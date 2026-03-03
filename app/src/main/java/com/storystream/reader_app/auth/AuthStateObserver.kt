package com.storystream.reader_app.auth

import com.storystream.reader_app.data.TokenProvider
import com.storystream.reader_app.repository.AuthState
import com.storystream.reader_app.repository.AuthStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Observes TokenProvider.tokenFlow and updates AuthStateHolder so UI reacts to token clears.
 */
@Singleton
class AuthStateObserver @Inject constructor(
    tokenProvider: TokenProvider
) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    init {
        scope.launch {
            tokenProvider.tokenFlow.collect { token ->
                if (token == null) {
                    AuthStateHolder.updateState(AuthState())
                } else {
                    // Optionally restore claims from token
                    val session = com.storystream.reader_app.data.UserSession.restoreFromToken(token)
                    AuthStateHolder.updateState(AuthState(isAuthenticated = true, email = session.email, tier = session.tier, token = session.token))
                }
            }
        }
    }
}
