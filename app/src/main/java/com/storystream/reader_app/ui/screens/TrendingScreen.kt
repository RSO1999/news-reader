package com.storystream.reader_app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storystream.reader_app.ui.components.AnimatedListItem
import com.storystream.reader_app.ui.components.ArticleCard
import com.storystream.reader_app.ui.theme.AppTheme
import com.storystream.reader_app.ui.viewmodel.TrendingViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun TrendingScreen(onOpenArticle: (String) -> Unit = {}, viewModel: TrendingViewModel = viewModel()) {
    val context = LocalContext.current

    // lifecycle-aware collection of UiState
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val trendingArticles = uiState.trendingArticles
    val loading = uiState.loading
    val error = uiState.error

    // Trigger load on composition
    LaunchedEffect(Unit) {
        viewModel.loadTrending()
    }

    // Consume one-shot events (show toast for save result)
    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { ev ->
            when (ev) {
                "save_ok" -> Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                "save_failed" -> Toast.makeText(context, "Save failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AppTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {

            // Header
            Text(
                text = "Most Popular",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            )

            when {
                loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Failed to load trending",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { viewModel.loadTrending() }) {
                                Text(text = "Retry")
                            }
                        }
                    }
                }
                trendingArticles.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No trending articles right now",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        itemsIndexed(trendingArticles) { index, article ->
                            AnimatedListItem(index = index) {
                                Row(
                                    modifier = Modifier.padding(start = 16.dp, end = 0.dp, top = 6.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    // Rank badge
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(
                                                color = if (index < 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            ),
                                            color = if (index < 3) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    ArticleCard(
                                        article = article,
                                        modifier = Modifier.padding(start = 8.dp, end = 16.dp),
                                        onClick = { onOpenArticle(article.id) },
                                        onSave = { id -> viewModel.saveArticle(id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
