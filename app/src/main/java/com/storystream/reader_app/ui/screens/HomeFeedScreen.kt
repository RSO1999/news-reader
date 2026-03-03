package com.storystream.reader_app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storystream.reader_app.ui.components.AnimatedListItem
import com.storystream.reader_app.ui.components.ArticleCard
import com.storystream.reader_app.ui.components.FeatureCard
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {

            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp),
                state = listState
            ) {
                // Header with personalization toggle
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Top Stories",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.weight(1f))

                        // Personalization toggle with animated color
                        val toggleBg by animateColorAsState(
                            targetValue = if (viewModel.personalized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            label = "toggleBg"
                        )
                        val toggleFg by animateColorAsState(
                            targetValue = if (viewModel.personalized) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            label = "toggleFg"
                        )

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = toggleBg
                        ) {
                            Row(
                                modifier = Modifier
                                    .clickable { viewModel.togglePersonalized() }
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = toggleFg
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (viewModel.personalized) "For You" else "Personalize",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = toggleFg
                                )
                            }
                        }
                    }
                }

                // Feature card for top story
                if (topStory != null) {
                    item {
                        AnimatedListItem(index = 0) {
                            FeatureCard(
                                title = topStory.title,
                                imageUrl = topStory.imageUrl,
                                section = topStory.section,
                                sourceName = topStory.sourceName,
                                publishedAt = topStory.publishedAt,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                onClick = { onOpenArticle(topStory.id) },
                                onSave = { viewModel.saveArticle(topStory.id) }
                            )
                        }
                    }
                }

                // Section divider
                item {
                    val dividerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .drawBehind {
                                drawLine(
                                    color = dividerColor,
                                    start = Offset(0f, size.height),
                                    end = Offset(size.width, size.height),
                                    strokeWidth = 1f
                                )
                            }
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Latest",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                // Article cards with staggered animation
                itemsIndexed(feedItems) { index, article ->
                    AnimatedListItem(index = index + 1) {
                        ArticleCard(
                            article = article,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            onClick = { onOpenArticle(article.id) },
                            onSave = { id -> viewModel.saveArticle(id) }
                        )
                    }

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
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Error state
            if (error != null && articles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Failed to load feed",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadFirstPage() }) {
                            Text(text = "Retry")
                        }
                    }
                }
            }
        }
    }
}
