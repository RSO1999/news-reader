package com.storystream.reader_app.network

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response

class TokenAuthenticator : Authenticator {
    @Synchronized
    override fun authenticate(route: okhttp3.Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null

        val path = response.request.url.encodedPath
        if (path.startsWith("/api/auth/")) return null

        val refreshToken = com.storystream.reader_app.data.SecureTokenStore.getRefreshToken() ?: return null

        val currentToken = com.storystream.reader_app.data.SecureTokenStore.getAccessToken()
        val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")
        if (!currentToken.isNullOrBlank() && requestToken != null && currentToken != requestToken &&
            !com.storystream.reader_app.util.JwtUtils.isExpired(currentToken)
        ) {
            return response.request.newBuilder()
                .header("Authorization", "Bearer $currentToken")
                .build()
        }

        return try {
            val refreshCall = NetworkModule.authApi.refresh(RefreshRequest(refreshToken))
            val refreshResp = refreshCall.execute()
            if (!refreshResp.isSuccessful) {
                com.storystream.reader_app.data.SecureTokenStore.clearTokens()
                return null
            }
            val body = refreshResp.body() ?: return null
            val newAccess = body.token
            if (newAccess.isBlank()) return null

            if (body.refreshToken != null) {
                com.storystream.reader_app.data.SecureTokenStore.saveTokens(newAccess, body.refreshToken)
            } else {
                com.storystream.reader_app.data.SecureTokenStore.replaceToken(newAccess)
            }

            response.request.newBuilder()
                .header("Authorization", "Bearer $newAccess")
                .build()
        } catch (_: Exception) {
            null
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
