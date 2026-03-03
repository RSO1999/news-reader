package com.storystream.reader_app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storystream.reader_app.di.IoDispatcher
import com.storystream.reader_app.repository.AuthRepository
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

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null
)

sealed class AuthEvent {
    object LoginSucceeded : AuthEvent()
    object LoginFailed : AuthEvent()
    object RegisterSucceeded : AuthEvent()
    object RegisterFailed : AuthEvent()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    fun login(email: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val res = withContext(ioDispatcher) { authRepo.login(email, password) }
            _uiState.update { it.copy(loading = false) }
            if (res.isSuccess) {
                _events.emit(AuthEvent.LoginSucceeded)
                onResult(true)
            } else {
                _uiState.update { it.copy(error = res.exceptionOrNull()?.localizedMessage ?: "Login failed") }
                _events.emit(AuthEvent.LoginFailed)
                onResult(false)
            }
        }
    }

    fun register(email: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val res = withContext(ioDispatcher) { authRepo.register(email, password) }
            _uiState.update { it.copy(loading = false) }
            if (res.isSuccess) {
                _events.emit(AuthEvent.RegisterSucceeded)
                onResult(true)
            } else {
                _uiState.update { it.copy(error = res.exceptionOrNull()?.localizedMessage ?: "Register failed") }
                _events.emit(AuthEvent.RegisterFailed)
                onResult(false)
            }
        }
    }
}
