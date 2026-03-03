package com.storystream.reader_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.storystream.reader_app.ui.theme.AppTheme
import com.storystream.reader_app.ui.theme.LocalAppColors

// Import the screens we created
import com.storystream.reader_app.ui.screens.HomeFeedScreen
import com.storystream.reader_app.ui.screens.ArticleDetailScreen
import com.storystream.reader_app.ui.screens.SavedScreen
import com.storystream.reader_app.ui.screens.InsightsScreen
import com.storystream.reader_app.ui.screens.LoginScreen
import com.storystream.reader_app.ui.screens.CreateAccountScreen
import com.storystream.reader_app.ui.screens.TrendingScreen

// User session
import com.storystream.reader_app.data.SecureTokenStore

// Trending VM
import com.storystream.reader_app.ui.viewmodel.TrendingViewModel
import com.storystream.reader_app.ui.viewmodel.InsightsViewModel
import com.storystream.reader_app.ui.viewmodel.ArticleDetailViewModel
import com.storystream.reader_app.repository.AuthStateHolder

// Custom trending-up line graph icon (no extended icons dependency needed)
private val TrendingUpIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "TrendingUp",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        addPath(
            pathData = androidx.compose.ui.graphics.vector.PathParser().parsePathString(
                "M16,6l2.29,2.29l-4.88,4.88l-4,-4L2,16.59L3.41,18l6,-6l4,4l6.3,-6.29L22,12V6z"
            ).toNodes(),
            fill = SolidColor(androidx.compose.ui.graphics.Color.Black)
        )
    }.build()
}

// Styled bottom tab bar with icons and active/inactive states
@Composable
fun BottomTabBar(selectedTab: String, onSelect: (String) -> Unit) {
    val appColors = LocalAppColors.current
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    data class TabItem(val label: String, val activeIcon: ImageVector, val inactiveIcon: ImageVector)

    val tabs = listOf(
        TabItem("Home", Icons.Filled.Star, Icons.Filled.Star),
        TabItem("Trending", TrendingUpIcon, TrendingUpIcon),
        TabItem("Saved", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
        TabItem("Insights", Icons.Filled.Person, Icons.Filled.Person)
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = borderColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            for (tab in tabs) {
                val isSelected = tab.label == selectedTab
                val tint = if (isSelected) appColors.tabActive else appColors.tabInactive

                Column(
                    modifier = Modifier
                        .clickable { onSelect(tab.label) }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isSelected) tab.activeIcon else tab.inactiveIcon,
                        contentDescription = tab.label,
                        tint = tint,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = tint
                    )
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Tink-backed secure token store
        try {
            SecureTokenStore.init(this)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        setContent {
            AppTheme {
                val authState by AuthStateHolder.authState.collectAsState()

                if (!authState.isAuthenticated) {
                    // Show auth screens
                    val authMode = rememberSaveable { mutableStateOf("login") } // "login" or "create"

                    if (authMode.value == "login") {
                        LoginScreen(onLogin = { _, _ ->
                            // AuthRepository handles state update
                        }, onCreateAccount = { authMode.value = "create" }, modifier = Modifier.fillMaxSize())
                    } else {
                        CreateAccountScreen(modifier = Modifier.fillMaxSize(), onCreate = { _, _ ->
                            // AuthRepository handles state update
                        }, onBack = { authMode.value = "login" })
                    }

                } else {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        // state: current tab and optional selected article id
                        val currentTab = rememberSaveable { mutableStateOf("Home") }
                        val currentArticleId = rememberSaveable { mutableStateOf<String?>(null) }

                        // create VMs at host level
                        val trendingVm = remember { TrendingViewModel() }
                        val insightsVm = remember { InsightsViewModel() }
                        val articleDetailVm = remember { ArticleDetailViewModel() }

                        Column(modifier = Modifier.padding(innerPadding)) {
                            Box(modifier = Modifier.weight(1f)) {
                                val articleId = currentArticleId.value
                                if (articleId != null) {
                                    ArticleDetailScreen(articleId = articleId, onBack = { currentArticleId.value = null }, viewModel = articleDetailVm, userTier = authState.tier)
                                } else {
                                    when (currentTab.value) {
                                        "Home" -> HomeFeedScreen(onOpenArticle = { id: String -> currentArticleId.value = id })
                                        "Trending" -> TrendingScreen(onOpenArticle = { id: String -> currentArticleId.value = id }, viewModel = trendingVm)
                                        "Saved" -> SavedScreen(onOpenArticle = { id: String -> currentArticleId.value = id })
                                        "Insights" -> InsightsScreen(
                                            onOpenArticle = { id: String -> currentArticleId.value = id },
                                            viewModel = insightsVm,
                                            userEmail = authState.email,
                                            userTier = authState.tier
                                        )
                                        else -> HomeFeedScreen(onOpenArticle = { id: String -> currentArticleId.value = id })
                                    }
                                }
                            }

                            // Bottom tab bar
                            BottomTabBar(selectedTab = currentTab.value, onSelect = { tab ->
                                currentArticleId.value = null
                                currentTab.value = tab
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AppTheme {
        Greeting("Android")
    }
}