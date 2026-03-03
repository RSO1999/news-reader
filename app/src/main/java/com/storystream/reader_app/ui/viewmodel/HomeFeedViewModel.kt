package com.storystream.reader_app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storystream.reader_app.data.ArticleResponse
import com.storystream.reader_app.repository.ArticlesRepository
import com.storystream.reader_app.data.SavedRefreshManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeFeedUiState(
    val articles: List<ArticleResponse> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val page: Int = 0,
    val totalPages: Int = 1,
    val personalized: Boolean = false,
    val lastSaveStatus: String? = null
)

@HiltViewModel
class HomeFeedViewModel @Inject constructor(private val repo: ArticlesRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeFeedUiState())
    val uiState: StateFlow<HomeFeedUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun loadFirstPage(personalizedFlag: Boolean = false) {
        _uiState.update { it.copy(personalized = personalizedFlag, page = 0, articles = emptyList(), error = null) }
        loadNextPage()
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.loading) return
        if (state.page >= state.totalPages) return

        _uiState.update { it.copy(loading = true, error = null) }

        viewModelScope.launch {
            val res = repo.getArticles(state.page, 20, state.personalized)
            if (res.isSuccess) {
                val (content, total) = res.getOrNull()!!
                val existingIds = _uiState.value.articles.map { it.id }.toSet()
                val newItems = content.filter { it.id !in existingIds }
                _uiState.update {
                    it.copy(
                        articles = it.articles + newItems,
                        totalPages = total,
                        page = it.page + 1,
                        loading = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        loading = false,
                        error = res.exceptionOrNull()?.localizedMessage ?: "Failed to load"
                    )
                }
            }
        }
    }

    fun togglePersonalized() {
        val next = !_uiState.value.personalized
        loadFirstPage(next)
    }

    fun saveArticle(id: String) {
        viewModelScope.launch {
            val res = repo.saveArticle(id)
            if (res.isSuccess) {
                _uiState.update { it.copy(lastSaveStatus = "ok") }
                SavedRefreshManager.triggerRefresh()
                _events.emit("save_ok")
            } else {
                _uiState.update { it.copy(lastSaveStatus = "failed") }
                _events.emit("save_failed")
            }
        }
    }
}
