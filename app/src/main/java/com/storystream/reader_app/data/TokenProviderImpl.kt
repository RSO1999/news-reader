package com.storystream.reader_app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation that delegates persistence to SecureTokenStore but keeps a
 * small in-memory cache (MutableStateFlow) for synchronous reads and observers.
 *
 * IMPORTANT: After you adopt this provider, update places that directly call
 * SecureTokenStore.saveToken/saveTokens to call tokenProvider.updateTokens(...) instead,
 * so the in-memory cache stays in sync.
 */
@Singleton
class TokenProviderImpl @Inject constructor() : TokenProvider {
    private val _tokenFlow = MutableStateFlow<String?>(SecureTokenStore.getAccessToken())
    override val tokenFlow: StateFlow<String?> = _tokenFlow.asStateFlow()

    override fun getToken(): String? {
        // Prefer fast cached value; fall back to storage if null (defensive)
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

