package com.storystream.reader_app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.storystream.reader_app.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepo: AuthRepository
) : ViewModel() {
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun login(email: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            loading = true
            error = null
            val res = withContext(Dispatchers.IO) { authRepo.login(email, password) }
            loading = false
            if (res.isSuccess) {
                onResult(true)
            } else {
                error = res.exceptionOrNull()?.localizedMessage ?: "Login failed"
                onResult(false)
            }
        }
    }

    fun register(email: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            loading = true
            error = null
            val res = withContext(Dispatchers.IO) { authRepo.register(email, password) }
            loading = false
            if (res.isSuccess) {
                onResult(true)
            } else {
                error = res.exceptionOrNull()?.localizedMessage ?: "Register failed"
                onResult(false)
            }
        }
    }
}

