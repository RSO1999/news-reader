package com.storystream.reader_app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storystream.reader_app.data.ArticleResponse
import com.storystream.reader_app.data.ContextEntity
import com.storystream.reader_app.repository.ArticlesRepository
import com.storystream.reader_app.repository.AuthRepository
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

@Suppress("unused")
data class ArticleDetailUiState(
    val article: ArticleResponse? = null,
    val contextEntities: List<ContextEntity> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val gated: Boolean = false,
    val contextLoading: Boolean = false,
    val contextLocked: Boolean = false,
    val isSaved: Boolean = false
)

sealed class ArticleDetailEvent {
    object SaveOk : ArticleDetailEvent()
    object SaveFailed : ArticleDetailEvent()
    object UpgradeSuccess : ArticleDetailEvent()
    object UpgradeFailed : ArticleDetailEvent()
}

@HiltViewModel
class ArticleDetailViewModel @Inject constructor(
    private val repo: ArticlesRepository,
    private val authRepo: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArticleDetailUiState())
    val uiState: StateFlow<ArticleDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ArticleDetailEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ArticleDetailEvent> = _events.asSharedFlow()

    fun loadArticle(articleId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    loading = true,
                    error = null,
                    gated = false,
                    contextEntities = emptyList(),
                    contextLoading = false,
                    contextLocked = false,
                    isSaved = false,
                    article = null
                )
            }

            val res = repo.getArticle(articleId)
            if (res.isSuccess) {
                val resp = res.getOrNull()!!
                if (resp.isSuccessful) {
                    _uiState.update { it.copy(article = resp.body(), loading = false) }
                } else {
                    if (resp.code() == 403) {
                        _uiState.update { it.copy(gated = true, loading = false) }
                    } else {
                        _uiState.update { it.copy(error = "Failed to load article: ${resp.code()}", loading = false) }
                    }
                }
            } else {
                _uiState.update { it.copy(error = res.exceptionOrNull()?.localizedMessage ?: "Failed to load article", loading = false) }
            }
        }
    }

    fun loadContext(articleId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(contextLoading = true, contextLocked = false) }
            val cres = repo.getContext(articleId)
            if (cres.isSuccess) {
                val r = cres.getOrNull()!!
                if (r.isSuccessful) {
                    val body = r.body()
                    if (body != null) {
                        _uiState.update { it.copy(contextEntities = body.entities, contextLoading = false) }
                    } else {
                        _uiState.update { it.copy(contextLoading = false) }
                    }
                } else if (r.code() == 403) {
                    _uiState.update { it.copy(contextLocked = true, contextLoading = false) }
                } else {
                    _uiState.update { it.copy(contextLoading = false) }
                }
            } else {
                _uiState.update { it.copy(contextLoading = false) }
            }
        }
    }

    fun saveArticle(id: String) {
        viewModelScope.launch {
            val res = repo.saveArticle(id)
            if (res.isSuccess) {
                _uiState.update { it.copy(isSaved = true) }
                SavedRefreshManager.triggerRefresh()
                _events.emit(ArticleDetailEvent.SaveOk)
            } else {
                _uiState.update { it.copy(isSaved = false) }
                _events.emit(ArticleDetailEvent.SaveFailed)
            }
        }
    }

    fun upgradeUser(articleId: String) {
        viewModelScope.launch {
            val res = authRepo.upgradeUser()
            if (res.isSuccess) {
                // Reload article to unlock gated content
                loadArticle(articleId)
                _events.emit(ArticleDetailEvent.UpgradeSuccess)
            } else {
                _events.emit(ArticleDetailEvent.UpgradeFailed)
            }
        }
    }
}
