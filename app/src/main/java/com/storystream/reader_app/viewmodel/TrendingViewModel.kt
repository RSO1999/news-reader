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

data class TrendingUiState(
    val trendingArticles: List<ArticleResponse> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

sealed class TrendingEvent {
    object SaveOk : TrendingEvent()
    object SaveFailed : TrendingEvent()
}

@HiltViewModel
class TrendingViewModel @Inject constructor(
    private val repo: ArticlesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrendingUiState())
    val uiState: StateFlow<TrendingUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TrendingEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<TrendingEvent> = _events.asSharedFlow()

    fun loadTrending(limit: Int = 10) {
        val current = _uiState.value
        if (current.loading) return

        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val res = repo.getTrending(limit)
            if (res.isSuccess) {
                _uiState.update { it.copy(trendingArticles = res.getOrNull() ?: emptyList(), loading = false) }
            } else {
                _uiState.update { it.copy(loading = false, error = res.exceptionOrNull()?.localizedMessage ?: "Failed to load trending") }
            }
        }
    }

    fun saveArticle(id: String) {
        viewModelScope.launch {
            val res = repo.saveArticle(id)
            // notify saved list to refresh
            SavedRefreshManager.triggerRefresh()
            if (res.isSuccess) {
                _events.emit(TrendingEvent.SaveOk)
            } else {
                _events.emit(TrendingEvent.SaveFailed)
            }
        }
    }
}
