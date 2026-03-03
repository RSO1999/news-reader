package com.storystream.reader_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.storystream.reader_app.ui.theme.AppTheme
import com.storystream.reader_app.ui.components.Masthead
import com.storystream.reader_app.ui.components.ContextEntityCard
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import com.storystream.reader_app.ui.viewmodel.ArticleDetailViewModel
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box

@Composable
fun ArticleDetailScreen(articleId: String, onBack: () -> Unit = {}, onRequireAuth: () -> Unit = {}, viewModel: ArticleDetailViewModel = remember { ArticleDetailViewModel() }, userTier: String = "FREE") {
    val article by remember { derivedStateOf { viewModel.article } }
    val contextEntities by remember { derivedStateOf { viewModel.contextEntities } }
    val loading by remember { derivedStateOf { viewModel.loading } }
    val error by remember { derivedStateOf { viewModel.error } }
    val gated by remember { derivedStateOf { viewModel.gated } }
    val contextLoading by remember { derivedStateOf { viewModel.contextLoading } }
    val contextLocked by remember { derivedStateOf { viewModel.contextLocked } }
    val isSaved by remember { derivedStateOf { viewModel.isSaved } }

    // Fetch article on load
    LaunchedEffect(articleId) {
        viewModel.loadArticle(articleId)
    }

    val isPremium = userTier == "PREMIUM"

    AppTheme {
        Column(modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 24.dp)) {

            Masthead(title = "Article", showBack = true, onBack = onBack)

            if (loading) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            if (gated) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Read limit reached. Please upgrade or log in to continue.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onRequireAuth) {
                        Text(text = "Upgrade / Login")
                    }
                }
                return@Column
            }

            if (error != null) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = error!!)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.loadArticle(articleId) }) {
                        Text(text = "Retry")
                    }
                }
                return@Column
            }

            // article should be non-null here
            article?.let { art ->
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = art.title, style = MaterialTheme.typography.displayLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "${art.sourceName} • ${art.publishedAt}", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = art.snippet, style = MaterialTheme.typography.bodyLarge)

                    Spacer(modifier = Modifier.height(16.dp))




                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        IconButton(onClick = {
                            viewModel.saveArticle(articleId)
                        }) {
                            Icon(
                                imageVector = if (isSaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (isSaved) "Saved" else "Save",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))


                    // Context panel — LOAD ON DEMAND for premium users only
                    if (isPremium) {
                        if (contextEntities.isEmpty()) {
                            if (contextLoading) {
                                Text(text = "Loading AI context...")
                            } else if (contextLocked) {
                                Text(text = "AI Context locked. Upgrade to Premium.")
                            } else {
                                // Show a button to load context on demand
                                Button(onClick = {
                                    viewModel.loadContext(articleId)
                                }) {
                                    Text(text = "Load AI Context")
                                }
                            }
                        }

                        if (contextEntities.isNotEmpty()) {
                            Column {
                                contextEntities.forEach { e ->
                                    ContextEntityCard(entity = e)
                                }
                            }
                        }
                    } else {
                        Text(text = "AI Context is a Premium feature. Upgrade to view.")
                    }
                }
            }
        }
    }
}
