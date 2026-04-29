package com.storystream.reader_app.data

import kotlinx.coroutines.flow.StateFlow

interface TokenProvider {
    fun getToken(): String?
    fun getRefreshToken(): String?
    val tokenFlow: StateFlow<String?>
    fun updateToken(token: String?)
    fun updateTokens(accessToken: String, refreshToken: String?)
}
