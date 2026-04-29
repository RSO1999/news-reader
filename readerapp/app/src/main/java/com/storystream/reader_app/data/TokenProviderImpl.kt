package com.storystream.reader_app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenProviderImpl @Inject constructor() : TokenProvider {
    private val _tokenFlow = MutableStateFlow<String?>(SecureTokenStore.getAccessToken())
    override val tokenFlow: StateFlow<String?> = _tokenFlow.asStateFlow()

    override fun getToken(): String? {
        return _tokenFlow.value ?: SecureTokenStore.getAccessToken()
    }

    override fun getRefreshToken(): String? {
        return SecureTokenStore.getRefreshToken()
    }

    override fun updateToken(token: String?) {
        if (token == null) {
            SecureTokenStore.clearTokens()
            _tokenFlow.value = null
        } else {
            SecureTokenStore.saveToken(token)
            _tokenFlow.value = token
        }
    }

    override fun updateTokens(accessToken: String, refreshToken: String?) {
        if (refreshToken != null) {
            SecureTokenStore.saveTokens(accessToken, refreshToken)
        } else {
            SecureTokenStore.saveToken(accessToken)
        }
        _tokenFlow.value = accessToken
    }
}
