package com.storystream.reader_app.data

import android.util.Base64
import org.json.JSONObject

data class UserSession(
    val token: String? = null,
    val email: String? = null,
    val tier: String = "FREE"
) {
    companion object {
        fun login(userEmail: String, jwt: String = "mock-token"): UserSession {
            val session = UserSession(token = jwt, email = userEmail)
            return decodeClaims(session, jwt)
        }

        /**
         * Restore session state from an existing token (used at startup). This does not imply a fresh login action.
         */
        fun restoreFromToken(jwt: String): UserSession {
            val session = UserSession(token = jwt)
            return decodeClaims(session, jwt)
        }

        private fun decodeClaims(session: UserSession, jwt: String): UserSession {
            return try {
                val parts = jwt.split(".")
                if (parts.size >= 2) {
                    val payload = parts[1]
                    // base64url decode
                    val decoded = String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP))
                    val obj = JSONObject(decoded)
                    val email = if (obj.has("sub")) obj.getString("sub") else session.email
                    val tier = if (obj.has("tier")) obj.getString("tier") else session.tier
                    session.copy(email = email, tier = tier)
                } else {
                    session
                }
            } catch (_: Exception) {
                // ignore parsing errors
                session
            }
        }
    }
}
