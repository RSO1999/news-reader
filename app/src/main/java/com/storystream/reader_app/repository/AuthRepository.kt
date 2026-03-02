package com.storystream.reader_app.repository

import com.storystream.reader_app.network.NetworkModule
import com.storystream.reader_app.data.SecureTokenStore
import com.storystream.reader_app.data.UserSession

class AuthRepository {
    private val api = NetworkModule.authApi

    suspend fun register(email: String, password: String): Result<Unit> {
        return try {
            val resp = api.register(com.storystream.reader_app.network.AuthRequest(email, password))
            // save token
            if (resp.refreshToken != null) {
                SecureTokenStore.saveTokens(resp.token, resp.refreshToken)
            } else {
                SecureTokenStore.saveToken(resp.token)
            }
            // decode minimal claims (email/tier) client-side
            UserSession.login(email, resp.token)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            val resp = api.login(com.storystream.reader_app.network.AuthRequest(email, password))
            if (resp.refreshToken != null) {
                SecureTokenStore.saveTokens(resp.token, resp.refreshToken)
            } else {
                SecureTokenStore.saveToken(resp.token)
            }
            UserSession.login(email, resp.token)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        SecureTokenStore.clearToken()
        UserSession.logout()
    }
}
