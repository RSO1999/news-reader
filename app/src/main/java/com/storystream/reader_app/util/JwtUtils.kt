package com.storystream.reader_app.util

object JwtUtils {
    fun decodeClaims(token: String): org.json.JSONObject? {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null
            val payload = parts[1]
            val decoded = String(
                android.util.Base64.decode(
                    payload,
                    android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP
                )
            )
            org.json.JSONObject(decoded)
        } catch (_: Exception) {
            null
        }
    }

    fun isExpired(token: String, nowSeconds: Long = System.currentTimeMillis() / 1000): Boolean {
        val obj = decodeClaims(token) ?: return true
        return try {
            if (!obj.has("exp")) return true
            val exp = obj.getLong("exp")
            nowSeconds >= exp
        } catch (_: Exception) {
            true
        }
    }
}

