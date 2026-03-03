package com.storystream.reader_app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storystream.reader_app.data.ArticleResponse
import com.storystream.reader_app.data.ContextEntity
import com.storystream.reader_app.repository.ArticlesRepository
import com.storystream.reader_app.repository.AuthRepository
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.storystream.reader_app.data.SavedRefreshManager

class ArticleDetailViewModel(
    private val repo: ArticlesRepository = ArticlesRepository(),
    private val authRepo: AuthRepository = AuthRepository()
) : ViewModel() {

    var article by mutableStateOf<ArticleResponse?>(null)
        private set
    var contextEntities by mutableStateOf<List<ContextEntity>>(emptyList())
        private set
    var loading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var gated by mutableStateOf(false)
        private set
    var contextLoading by mutableStateOf(false)
        private set
    var contextLocked by mutableStateOf(false)
        private set
    var isSaved by mutableStateOf(false)
        private set

    fun loadArticle(articleId: String) {
        viewModelScope.launch {
            loading = true
            error = null
            gated = false
            // Reset all per-article state so stale data from previous article is cleared
            contextEntities = emptyList()
            contextLoading = false
            contextLocked = false
            isSaved = false
            article = null

            val res = repo.getArticle(articleId)
            loading = false
            if (res.isSuccess) {
                val resp = res.getOrNull()!!
                if (resp.isSuccessful) {
                    article = resp.body()
                } else {
                    if (resp.code() == 403) {
                        gated = true
                    } else {
                        error = "Failed to load article: ${resp.code()}"
                    }
                }
            } else {
                error = res.exceptionOrNull()?.localizedMessage ?: "Failed to load article"
            }
        }
    }

    fun loadContext(articleId: String) {
        viewModelScope.launch {
            contextLoading = true
            contextLocked = false
            val cres = repo.getContext(articleId)
            contextLoading = false
            if (cres.isSuccess) {
                val r = cres.getOrNull()!!
                if (r.isSuccessful) {
                    val body = r.body()
                    if (body != null) {
                        contextEntities = body.entities
                    }
                } else if (r.code() == 403) {
                    contextLocked = true
                } else {
                    // optional: set error
                }
            } else {
                // network failure
            }
        }
    }

    fun saveArticle(id: String) {
        viewModelScope.launch {
            val res = repo.saveArticle(id)
            if (res.isSuccess) {
                isSaved = true
                SavedRefreshManager.triggerRefresh() // Trigger a refresh after saving
            } else {
                isSaved = false
            }
        }
    }

    fun upgradeUser(articleId: String) {
        viewModelScope.launch {
            val res = authRepo.upgradeUser()
            if (res.isSuccess) {
                // Reload article to unlock gated content
                loadArticle(articleId)
            }
        }
    }
}
