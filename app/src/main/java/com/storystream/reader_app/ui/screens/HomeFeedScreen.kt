package com.storystream.reader_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.storystream.reader_app.ui.components.ArticleCard
import com.storystream.reader_app.ui.components.FeatureCard
import com.storystream.reader_app.ui.components.Masthead
import com.storystream.reader_app.ui.theme.AppTheme
import com.storystream.reader_app.ui.viewmodel.HomeFeedViewModel
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star

@Composable
fun HomeFeedScreen(onOpenArticle: (String) -> Unit = {}, viewModel: HomeFeedViewModel = remember { HomeFeedViewModel() }) {
    val articles by remember { derivedStateOf { viewModel.articles } }
    val loading by remember { derivedStateOf { viewModel.loading } }
    val error by remember { derivedStateOf { viewModel.error } }
    val topStory = articles.firstOrNull()
    val feedItems = if (articles.isNotEmpty()) articles.drop(1) else emptyList()
    val listState = rememberLazyListState()

    // load first page on composition
    LaunchedEffect(Unit) {
        viewModel.loadFirstPage()
    }

    AppTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            Masthead(title = "NewsReader")

            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp),
                state = listState
            ) {
                // Header with personalization toggle
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Top Stories",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        // Personalization toggle
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (viewModel.personalized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier
                                    .clickable { viewModel.togglePersonalized() }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = if (viewModel.personalized) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (viewModel.personalized) "For You" else "Personalize",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (viewModel.personalized) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Feature card for top story
                if (topStory != null) {
                    item {
                        FeatureCard(
                            title = topStory.title,
                            imageUrl = topStory.imageUrl,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            onClick = { onOpenArticle(topStory.id) }
                        )
                    }
                }

                // Latest divider
                item {
                    Text(
                        text = "Latest",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Article cards
                itemsIndexed(feedItems) { index, article ->
                    ArticleCard(
                        article = article,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        onClick = { onOpenArticle(article.id) },
                        onSave = { id -> viewModel.saveArticle(id) }
                    )

                    // Infinite scroll
                    if (index >= feedItems.lastIndex - 3 && !loading && viewModel.page <= viewModel.totalPages) {
                        viewModel.loadNextPage()
                    }
                }

                // Loading indicator
                item {
                    if (loading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }

            // Error state (if needed, overlay or replace)
            if (error != null && articles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Failed to load feed")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadFirstPage() }) {
                            Text(text = "Retry")
                        }
                    }
                }
            }
        }
    }
}
