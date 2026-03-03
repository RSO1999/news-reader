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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.storystream.reader_app.ui.theme.AppTheme

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

// Minimal bottom bar component (simple Row of tappable labels)
@Composable
fun BottomTabBar(selectedTab: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val tabs = listOf("Home", "Trending", "Saved", "Insights")
        for (t in tabs) {
            val isSelected = t == selectedTab
            Text(
                text = t,
                modifier = Modifier
                    .clickable { onSelect(t) }
                    .padding(8.dp),
                style = if (isSelected) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
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

                            // Minimal bottom bar (keeps UI simple & dependency-free)
                            Spacer(modifier = Modifier.height(8.dp))
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