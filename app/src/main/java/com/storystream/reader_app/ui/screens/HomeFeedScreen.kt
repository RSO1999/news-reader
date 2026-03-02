package com.storystream.reader_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.storystream.reader_app.ui.components.ArticleCard
import com.storystream.reader_app.ui.components.FeatureCard
import com.storystream.reader_app.ui.components.Masthead
import com.storystream.reader_app.ui.theme.AppTheme
import com.storystream.reader_app.ui.viewmodel.HomeFeedViewModel

@Composable
fun HomeFeedScreen(onOpenArticle: (String) -> Unit = {}, viewModel: HomeFeedViewModel = remember { HomeFeedViewModel() }) {
    val articles by remember { derivedStateOf { viewModel.articles } }
    val loading by remember { derivedStateOf { viewModel.loading } }
    val error by remember { derivedStateOf { viewModel.error } }
    val listState = rememberLazyListState()

    // load first page on composition
    LaunchedEffect(Unit) {
        viewModel.loadFirstPage()
    }

    AppTheme {
        Column(modifier = Modifier
            .fillMaxSize()) {

            Masthead(title = "NewsReader")

            // Personalization toggle
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Personalized")
                Spacer(modifier = Modifier.width(8.dp))
                Switch(checked = viewModel.personalized, onCheckedChange = { viewModel.togglePersonalized() })
            }

            if (articles.isEmpty() && loading) {
                // initial loading
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (articles.isEmpty() && error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Failed to load feed")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadFirstPage() }) {
                            Text(text = "Retry")
                        }
                    }
                }
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    item {
                        FeatureCard(title = "Top Story: Placeholder", onClick = { onOpenArticle("feature") })
                    }

                    itemsIndexed(articles) { index, article ->
                        ArticleCard(article = article, onClick = { onOpenArticle(article.id) }, onSave = { id -> viewModel.saveArticle(id) })

                        // trigger load when near end
                        if (index >= articles.lastIndex - 3 && !loading && viewModel.page <= viewModel.totalPages) {
                            viewModel.loadNextPage()
                        }
                    }

                    item {
                        if (loading) {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}
