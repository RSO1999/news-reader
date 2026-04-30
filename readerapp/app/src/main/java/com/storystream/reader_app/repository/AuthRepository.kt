package com.storystream.reader_app.repository

import com.storystream.reader_app.data.SecureTokenStore
import com.storystream.reader_app.data.UserSession
import com.storystream.reader_app.data.TokenProvider
import com.storystream.reader_app.network.AuthApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class AuthState(
    val isAuthenticated: Boolean = false,
    val email: String? = null,
    val tier: String = "FREE",
    val token: String? = null,
    val isInitializing: Boolean = true
)

object AuthStateHolder {
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    @Volatile
    private var restoredFromStore = false

    fun initializeFromStoredToken() {
        if (restoredFromStore) return

        synchronized(this) {
            if (restoredFromStore) return
            restoredFromStore = true
        }

        val token = SecureTokenStore.getToken()
        val nextState = if (token != null) {
            val userSession = UserSession.restoreFromToken(token)
            AuthState(
                isAuthenticated = true,
                email = userSession.email,
                tier = userSession.tier,
                token = userSession.token,
                isInitializing = false
            )
        } else {
            AuthState(isInitializing = false)
        }

        _authState.value = nextState
    }

    fun updateState(newState: AuthState) {
        _authState.value = newState.copy(isInitializing = false)
    }
}

@Singleton
class AuthRepository @Inject constructor(
    private val api: AuthApi,
    private val tokenProvider: TokenProvider
) {
    suspend fun register(email: String, password: String): Result<Unit> {
        return try {
            val resp = withContext(Dispatchers.IO) {
                api.register(com.storystream.reader_app.network.AuthRequest(email, password))
            }
            tokenProvider.updateTokens(resp.token, resp.refreshToken)
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
            val resp = withContext(Dispatchers.IO) {
                api.login(com.storystream.reader_app.network.AuthRequest(email, password))
            }
            tokenProvider.updateTokens(resp.token, resp.refreshToken)
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
            val resp = withContext(Dispatchers.IO) { api.upgradeUser() }
            tokenProvider.updateToken(null)
            tokenProvider.updateToken(resp.token)
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
        tokenProvider.updateToken(null)
        AuthStateHolder.updateState(AuthState())
    }
}
