package com.storystream.reader_app.data

import kotlinx.coroutines.flow.StateFlow

/**
 * Small abstraction that provides synchronous token access for interceptors
 * and a Flow to observe token changes.
 */
interface TokenProvider {
    /** Synchronous token fetch for use inside OkHttp interceptors */
    fun getToken(): String?

    /** Synchronous refresh token fetch (used by authenticators) */
    fun getRefreshToken(): String?

    /** Flow of current access token (useful for UI or other consumers) */
    val tokenFlow: StateFlow<String?>

    /** Update the access token (and internal cache) */
    fun updateToken(token: String?)

    /** Update both access + refresh tokens */
    fun updateTokens(accessToken: String, refreshToken: String?)
}

