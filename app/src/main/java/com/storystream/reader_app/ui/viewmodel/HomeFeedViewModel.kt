package com.storystream.reader_app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.storystream.reader_app.data.ArticleResponse
import com.storystream.reader_app.repository.ArticlesRepository
import com.storystream.reader_app.data.SavedRefreshManager
import kotlinx.coroutines.launch

class HomeFeedViewModel(private val repo: ArticlesRepository = ArticlesRepository()) : ViewModel() {
    var articles by mutableStateOf<List<ArticleResponse>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var page by mutableStateOf(0)
        private set
    var totalPages by mutableStateOf(1)
        private set
    var personalized by mutableStateOf(false)
        private set

    // simple status for last save attempt: "ok"/"failed"/null
    var lastSaveStatus by mutableStateOf<String?>(null)
        private set

    fun loadFirstPage(personalizedFlag: Boolean = false) {
        personalized = personalizedFlag
        page = 0
        articles = emptyList()
        loadNextPage()
    }

    fun loadNextPage() {
        if (loading) return
        if (page >= totalPages) return

        loading = true
        error = null
        viewModelScope.launch {
            val res = repo.getArticles(page, 20, personalized)
            loading = false
            if (res.isSuccess) {
                val (content, total) = res.getOrNull()!!
                // append while avoiding duplicates
                val existingIds = articles.map { it.id }.toSet()
                val newItems = content.filter { it.id !in existingIds }
                articles = articles + newItems
                totalPages = total
                page = page + 1
            } else {
                error = res.exceptionOrNull()?.localizedMessage ?: "Failed to load"
            }
        }
    }

    fun togglePersonalized() {
        personalized = !personalized
        loadFirstPage(personalized)
    }

    fun saveArticle(id: String) {
        // optimistic UI handled by caller (ArticleCard) via onSave; backend call here
        viewModelScope.launch {
            val res = repo.saveArticle(id)
            if (res.isSuccess) {
                lastSaveStatus = "ok"
                // notify saved list to refresh from server
                SavedRefreshManager.triggerRefresh()
            } else {
                lastSaveStatus = "failed"
            }
        }
    }
}
