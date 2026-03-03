package com.storystream.reader_app.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storystream.reader_app.data.ArticleResponse
import com.storystream.reader_app.repository.ArticlesRepository
import kotlinx.coroutines.launch

class TrendingViewModel(private val repo: ArticlesRepository = ArticlesRepository()) : ViewModel() {
    var trendingArticles by mutableStateOf<List<ArticleResponse>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun loadTrending(limit: Int = 10) {
        if (loading) return
        loading = true
        error = null
        viewModelScope.launch {
            val res = repo.getTrending(limit)
            loading = false
            if (res.isSuccess) {
                trendingArticles = res.getOrNull() ?: emptyList()
            } else {
                error = res.exceptionOrNull()?.localizedMessage ?: "Failed to load trending"
            }
        }
    }

    fun saveArticle(id: String) {
        viewModelScope.launch {
            repo.saveArticle(id)
            // notify saved list to refresh
            // best effort: fire and forget; repository handles networking errors
            // trigger refresh (non-suspending)
            SavedRefreshManager.triggerRefresh()
        }
    }
}
