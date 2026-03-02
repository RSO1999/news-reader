package com.storystream.reader_app.data

import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject

object UserSession {
    var token by mutableStateOf<String?>(null)
    var email by mutableStateOf<String?>(null)
    var tier by mutableStateOf("FREE") // "FREE" or "PREMIUM"

    fun login(userEmail: String, jwt: String = "mock-token") {
        email = userEmail
        token = jwt
        // try decode jwt payload for tier claim
        decodeClaims(jwt)
    }

    /**
     * Restore session state from an existing token (used at startup). This does not imply a fresh login action.
     */
    fun restoreFromToken(jwt: String) {
        token = jwt
        decodeClaims(jwt)
    }

    fun setPremium(newToken: String = "mock-premium-token") {
        tier = "PREMIUM"
        token = newToken
        decodeClaims(newToken)
    }

    fun logout() {
        token = null
        email = null
        tier = "FREE"
    }

    private fun decodeClaims(jwt: String) {
        try {
            val parts = jwt.split(".")
            if (parts.size >= 2) {
                val payload = parts[1]
                // base64url decode
                val decoded = String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP))
                val obj = JSONObject(decoded)
                if (obj.has("sub")) {
                    email = obj.getString("sub")
                }
                if (obj.has("tier")) {
                    tier = obj.getString("tier")
                }
            }
        } catch (_: Exception) {
            // ignore parsing errors
        }
    }
}
