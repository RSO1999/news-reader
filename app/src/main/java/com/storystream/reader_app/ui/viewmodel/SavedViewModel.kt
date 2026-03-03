package com.storystream.reader_app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storystream.reader_app.data.ArticleResponse
import com.storystream.reader_app.repository.ArticlesRepository
import com.storystream.reader_app.data.SavedRefreshManager
import com.storystream.reader_app.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SavedUiState(
    val saved: List<ArticleResponse> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

sealed class SavedEvent {
    object RefreshSucceeded : SavedEvent()
    object RefreshFailed : SavedEvent()
    object SaveSucceeded : SavedEvent()
    object SaveFailed : SavedEvent()
}

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val repo: ArticlesRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavedUiState())
    val uiState: StateFlow<SavedUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SavedEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<SavedEvent> = _events.asSharedFlow()

    init {
        refresh()
        viewModelScope.launch {
            SavedRefreshManager.refreshFlow.collectLatest {
                refresh()
            }
        }
    }

    fun refresh(limit: Int = 50) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val res = withContext(ioDispatcher) { repo.getSaved(limit) }
            if (res.isSuccess) {
                _uiState.update { it.copy(saved = res.getOrNull() ?: emptyList(), loading = false) }
                _events.emit(SavedEvent.RefreshSucceeded)
            } else {
                _uiState.update {
                    it.copy(
                        loading = false,
                        error = res.exceptionOrNull()?.localizedMessage ?: "Failed to load saved"
                    )
                }
                _events.emit(SavedEvent.RefreshFailed)
            }
        }
    }

    fun saveArticle(id: String) {
        viewModelScope.launch {
            val res = withContext(ioDispatcher) { repo.saveArticle(id) }
            if (res.isSuccess) {
                SavedRefreshManager.triggerRefresh()
                _events.emit(SavedEvent.SaveSucceeded)
            } else {
                _events.emit(SavedEvent.SaveFailed)
            }
        }
    }
}
