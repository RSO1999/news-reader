package com.storystream.reader_app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storystream.reader_app.data.ArticleResponse
import com.storystream.reader_app.repository.ArticlesRepository
import com.storystream.reader_app.data.SavedRefreshManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class SavedViewModel(private val repo: ArticlesRepository = ArticlesRepository()) : ViewModel() {
    var saved by mutableStateOf<List<ArticleResponse>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init {
        // initial load
        refresh()
        // listen for refresh signals
        viewModelScope.launch {
            SavedRefreshManager.refreshFlow.collectLatest {
                refresh()
            }
        }
    }

    fun refresh(limit: Int = 50) {
        viewModelScope.launch {
            loading = true
            error = null
            val res = repo.getSaved(limit)
            loading = false
            if (res.isSuccess) {
                saved = res.getOrNull() ?: emptyList()
            } else {
                error = res.exceptionOrNull()?.localizedMessage ?: "Failed to load saved"
            }
        }
    }
}

