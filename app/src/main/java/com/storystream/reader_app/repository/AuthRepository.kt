package com.storystream.reader_app.repository

import com.storystream.reader_app.network.NetworkModule
import com.storystream.reader_app.data.SecureTokenStore
import com.storystream.reader_app.data.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AuthState(
    val isAuthenticated: Boolean = false,
    val email: String? = null,
    val tier: String = "FREE",
    val token: String? = null
)

object AuthStateHolder {
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        // Initialize from stored token
        val token = SecureTokenStore.getToken()
        if (token != null) {
            val userSession = UserSession.restoreFromToken(token)
            _authState.value = AuthState(
                isAuthenticated = true,
                email = userSession.email,
                tier = userSession.tier,
                token = userSession.token
            )
        }
    }

    fun updateState(newState: AuthState) {
        _authState.value = newState
    }
}

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
            val session = UserSession.login(email, resp.token)
            AuthStateHolder.updateState(AuthState(
                isAuthenticated = true,
                email = session.email,
                tier = session.tier,
                token = session.token
            ))
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
            val session = UserSession.login(email, resp.token)
            AuthStateHolder.updateState(AuthState(
                isAuthenticated = true,
                email = session.email,
                tier = session.tier,
                token = session.token
            ))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upgradeUser(): Result<Unit> {
        return try {
            val resp = api.upgradeUser()
            // replace stored token with new premium token
            SecureTokenStore.clearTokens()
            SecureTokenStore.saveToken(resp.token)
            val session = UserSession.restoreFromToken(resp.token)
            AuthStateHolder.updateState(AuthState(
                isAuthenticated = true,
                email = session.email,
                tier = session.tier,
                token = session.token
            ))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        SecureTokenStore.clearToken()
        AuthStateHolder.updateState(AuthState())
    }
}
