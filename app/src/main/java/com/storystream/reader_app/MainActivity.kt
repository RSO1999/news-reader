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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.storystream.reader_app.ui.theme.AppTheme

// Added minimal state imports for simple in-app navigation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable

// unit import needed
import androidx.compose.ui.unit.dp

// Import the screens we created
import com.storystream.reader_app.ui.screens.HomeFeedScreen
import com.storystream.reader_app.ui.screens.ArticleDetailScreen
import com.storystream.reader_app.ui.screens.SavedScreen
import com.storystream.reader_app.ui.screens.InsightsScreen
import com.storystream.reader_app.ui.screens.LoginScreen
import com.storystream.reader_app.ui.screens.CreateAccountScreen
import com.storystream.reader_app.ui.screens.TrendingScreen

// User session
import com.storystream.reader_app.data.UserSession
import com.storystream.reader_app.data.SecureTokenStore

// Trending VM
import com.storystream.reader_app.ui.viewmodel.TrendingViewModel
import androidx.compose.runtime.remember

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
            // if token exists, restore user session
            val token = SecureTokenStore.getToken()
            if (token != null) {
                UserSession.restoreFromToken(token)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        setContent {
            AppTheme {
                // Simple in-memory auth flow for UI prototype
                var isAuthenticated by rememberSaveable { mutableStateOf(SecureTokenStore.getToken() != null) }
                var authMode by rememberSaveable { mutableStateOf("login") } // "login" or "create"

                if (!isAuthenticated) {
                    // Show auth screens
                    if (authMode == "login") {
                        LoginScreen(onLogin = { _, _ ->
                            // minimal simulated success
                            isAuthenticated = true
                        }, onCreateAccount = { authMode = "create" }, modifier = Modifier.fillMaxSize())
                    } else {
                        CreateAccountScreen(modifier = Modifier.fillMaxSize(), onCreate = { _, _ ->
                            // simulated account creation success
                            isAuthenticated = true
                        }, onBack = { authMode = "login" })
                    }

                } else {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        // state: current tab and optional selected article id
                        var currentTab by rememberSaveable { mutableStateOf("Home") }
                        var currentArticleId by rememberSaveable { mutableStateOf<String?>(null) }

                        // create VMs at host level
                        val trendingVm = remember { TrendingViewModel() }

                        Column(modifier = Modifier.padding(innerPadding)) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (currentArticleId != null) {
                                    ArticleDetailScreen(articleId = currentArticleId!!, onBack = { currentArticleId = null })
                                } else {
                                    when (currentTab) {
                                        "Home" -> HomeFeedScreen(onOpenArticle = { id: String -> currentArticleId = id })
                                        "Trending" -> TrendingScreen(onOpenArticle = { id: String -> currentArticleId = id }, viewModel = trendingVm)
                                        "Saved" -> SavedScreen(onOpenArticle = { id: String -> currentArticleId = id })
                                        "Insights" -> InsightsScreen(
                                            onUpgrade = { UserSession.setPremium() },
                                            onLogout = {
                                                // clear auth state and return to login
                                                isAuthenticated = false
                                            },
                                            onOpenArticle = { id: String -> currentArticleId = id }
                                        )
                                        else -> HomeFeedScreen(onOpenArticle = { id: String -> currentArticleId = id })
                                    }
                                }
                            }

                            // Minimal bottom tab bar (keeps UI simple & dependency-free)
                            Spacer(modifier = Modifier.height(8.dp))
                            BottomTabBar(selectedTab = currentTab, onSelect = { tab ->
                                currentArticleId = null
                                currentTab = tab
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