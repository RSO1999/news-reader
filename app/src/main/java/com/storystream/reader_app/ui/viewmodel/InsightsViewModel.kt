package com.storystream.reader_app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storystream.reader_app.data.ReadingInsightsResponse
import com.storystream.reader_app.repository.ArticlesRepository
import com.storystream.reader_app.repository.AuthRepository
import com.storystream.reader_app.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Suppress("unused")
data class InsightsUiState(
    val insights: ReadingInsightsResponse? = null,
    val loading: Boolean = false,
    val error: String? = null
)

sealed class InsightsEvent {
    object UpgradeSuccess : InsightsEvent()
    object UpgradeFailed : InsightsEvent()
}

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val articlesRepo: ArticlesRepository,
    private val authRepo: AuthRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<InsightsEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<InsightsEvent> = _events.asSharedFlow()

    fun loadInsights() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val res = withContext(ioDispatcher) { articlesRepo.getUserInsights() }
            if (res.isSuccess) {
                _uiState.update { it.copy(insights = res.getOrNull(), loading = false) }
            } else {
                _uiState.update { it.copy(loading = false, error = res.exceptionOrNull()?.localizedMessage ?: "Failed to load insights") }
            }
        }
    }

    fun upgradeUser() {
        viewModelScope.launch {
            val res = withContext(ioDispatcher) { authRepo.upgradeUser() }
            if (res.isSuccess) {
                // Reload insights from server to get updated premium state
                loadInsights()
                _events.emit(InsightsEvent.UpgradeSuccess)
            } else {
                _events.emit(InsightsEvent.UpgradeFailed)
            }
        }
    }

    fun logout(onLogout: () -> Unit) {
        authRepo.logout()
        onLogout()
    }
}
