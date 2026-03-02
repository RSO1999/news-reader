package com.storystream.reader_app.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.storystream.reader_app.ui.components.ArticleCard
import com.storystream.reader_app.ui.viewmodel.TrendingViewModel

@Composable
fun TrendingScreen(onOpenArticle: (String) -> Unit = {}, viewModel: TrendingViewModel) {
    LaunchedEffect(Unit) {
        viewModel.loadTrending()
    }
    val trendingArticles = viewModel.trendingArticles

    LazyColumn {
        items(trendingArticles) { article ->
            ArticleCard(article = article, onClick = { onOpenArticle(article.id) }, onSave = { id -> viewModel.saveArticle(id) })
        }
    }
}
