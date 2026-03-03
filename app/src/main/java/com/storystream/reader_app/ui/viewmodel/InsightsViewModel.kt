package com.storystream.reader_app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storystream.reader_app.data.ReadingInsightsResponse
import com.storystream.reader_app.repository.ArticlesRepository
import com.storystream.reader_app.repository.AuthRepository
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class InsightsViewModel(
    private val articlesRepo: ArticlesRepository = ArticlesRepository(),
    private val authRepo: AuthRepository = AuthRepository()
) : ViewModel() {

    var insights by mutableStateOf<ReadingInsightsResponse?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init {
        loadInsights()
    }

    fun loadInsights() {
        viewModelScope.launch {
            loading = true
            error = null
            val res = articlesRepo.getUserInsights()
            loading = false
            if (res.isSuccess) {
                insights = res.getOrNull()
            } else {
                error = res.exceptionOrNull()?.localizedMessage ?: "Failed to load insights"
            }
        }
    }

    fun upgradeUser(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val res = authRepo.upgradeUser()
            if (res.isSuccess) {
                onSuccess()
            }
        }
    }

    fun logout(onLogout: () -> Unit) {
        authRepo.logout()
        onLogout()
    }
}
