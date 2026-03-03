package com.storystream.reader_app.network

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import com.storystream.reader_app.data.TokenProvider
import com.storystream.reader_app.util.JwtUtils
import kotlin.jvm.Synchronized

/**
 * Authenticator that performs a single-flight token refresh using the refresh token.
 * It uses an injected RefreshApi (Retrofit) backed by a dedicated client without auth
 * interceptor to avoid recursion. Concurrent 401s are deduplicated via a simple monitor.
 */
class TokenAuthenticator(
    private val tokenProvider: TokenProvider,
    private val refreshApi: RefreshApi
) : Authenticator {

    private val lock = Any()
    private var isRefreshing = false

    @Synchronized
    override fun authenticate(route: okhttp3.Route?, response: Response): Request? {
        // Prevent retry loops
        if (responseCount(response) >= 2) return null

        val path = response.request.url.encodedPath
        if (path.startsWith("/api/auth/")) return null

        // If another thread already refreshed, retry with latest token
        val currentToken = tokenProvider.getToken()
        val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")
        if (!currentToken.isNullOrBlank() && requestToken != null && currentToken != requestToken && !JwtUtils.isExpired(currentToken)) {
            return response.request.newBuilder()
                .header("Authorization", "Bearer $currentToken")
                .build()
        }

        synchronized(lock) {
            if (isRefreshing) {
                while (isRefreshing) {
                    try {
                        (lock as java.lang.Object).wait()
                    } catch (_: InterruptedException) {
                        // ignore
                    }
                }
                val tokenNow = tokenProvider.getToken()
                return if (!tokenNow.isNullOrBlank()) {
                    response.request.newBuilder().header("Authorization", "Bearer $tokenNow").build()
                } else {
                    null
                }
            } else {
                isRefreshing = true
            }
        }

        try {
            val refreshToken = tokenProvider.getRefreshToken() ?: return null

            val call = refreshApi.refresh(com.storystream.reader_app.network.RefreshRequest(refreshToken))
            val resp = call.execute()
            if (!resp.isSuccessful) {
                tokenProvider.updateToken(null)
                return null
            }

            val body = resp.body() ?: run {
                tokenProvider.updateToken(null)
                return null
            }

            val newAccess = body.token
            val newRefresh = body.refreshToken

            if (newAccess.isNullOrBlank()) {
                tokenProvider.updateToken(null)
                return null
            }

            tokenProvider.updateTokens(newAccess, newRefresh)

            val finalToken = tokenProvider.getToken() ?: return null
            return response.request.newBuilder()
                .header("Authorization", "Bearer $finalToken")
                .build()
        } catch (e: Exception) {
            tokenProvider.updateToken(null)
            return null
        } finally {
            synchronized(lock) {
                isRefreshing = false
                (lock as java.lang.Object).notifyAll()
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
