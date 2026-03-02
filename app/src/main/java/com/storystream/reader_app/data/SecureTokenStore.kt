package com.storystream.reader_app.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.core.content.edit
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.KeysetHandle
import java.nio.charset.StandardCharsets

object SecureTokenStore {
    private const val PREFS_NAME = "secure_token_prefs"
    private const val KEYSET_NAME = "master_keyset"
    private const val PREF_SECRET_KEY = "secure_token"
    private const val PREF_REFRESH_KEY = "secure_refresh_token"
    private const val MASTER_KEY_URI = "android-keystore://readerapp_master_key"

    private var aead: Aead? = null
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        // Register AEAD config
        try {
            AeadConfig.register()
            // Build or load the keyset in SharedPreferences, backed by Android Keystore
            val manager = AndroidKeysetManager.Builder()
                .withSharedPref(context, PREFS_NAME, KEYSET_NAME)
                .withKeyTemplate(AeadKeyTemplates.AES128_GCM)
                .withMasterKeyUri(MASTER_KEY_URI)
                .build()

            val keysetHandle: KeysetHandle = manager.keysetHandle

            @Suppress("DEPRECATION")
            aead = keysetHandle.getPrimitive(Aead::class.java)
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        } catch (e: Exception) {
            // If Tink initialization fails, clear state and rethrow
            aead = null
            prefs = null
            throw e
        }
    }

    fun saveToken(token: String) {
        saveAccessToken(token)
    }

    fun saveTokens(accessToken: String, refreshToken: String) {
        saveAccessToken(accessToken)
        val localPrefs = prefs ?: return
        val localAead = aead ?: return
        val ciphertext = localAead.encrypt(refreshToken.toByteArray(StandardCharsets.UTF_8), null)
        val encoded = Base64.encodeToString(ciphertext, Base64.DEFAULT)
        localPrefs.edit {
            putString(PREF_REFRESH_KEY, encoded)
        }
    }

    private fun saveAccessToken(token: String) {
        val localPrefs = prefs ?: return
        val localAead = aead ?: return
        val ciphertext = localAead.encrypt(token.toByteArray(StandardCharsets.UTF_8), null)
        val encoded = Base64.encodeToString(ciphertext, Base64.DEFAULT)
        localPrefs.edit {
            putString(PREF_SECRET_KEY, encoded)
        }
    }

    fun getToken(): String? {
        return getAccessToken()
    }

    fun getAccessToken(): String? {
        val localPrefs = prefs ?: return null
        val localAead = aead ?: return null
        val encoded = localPrefs.getString(PREF_SECRET_KEY, null) ?: return null
        return try {
            val cipher = Base64.decode(encoded, Base64.DEFAULT)
            val plain = localAead.decrypt(cipher, null)
            val token = String(plain, StandardCharsets.UTF_8)
            if (com.storystream.reader_app.util.JwtUtils.isExpired(token)) {
                localPrefs.edit {
                    remove(PREF_SECRET_KEY)
                }
                null
            } else {
                token
            }
        } catch (_: Exception) {
            localPrefs.edit {
                remove(PREF_SECRET_KEY)
            }
            null
        }
    }

    fun getRefreshToken(): String? {
        val localPrefs = prefs ?: return null
        val localAead = aead ?: return null
        val encoded = localPrefs.getString(PREF_REFRESH_KEY, null) ?: return null
        return try {
            val cipher = Base64.decode(encoded, Base64.DEFAULT)
            val plain = localAead.decrypt(cipher, null)
            String(plain, StandardCharsets.UTF_8)
        } catch (_: Exception) {
            localPrefs.edit {
                remove(PREF_REFRESH_KEY)
            }
            null
        }
    }

    fun clearToken() {
        clearTokens()
    }

    fun clearTokens() {
        prefs?.edit {
            remove(PREF_SECRET_KEY)
            remove(PREF_REFRESH_KEY)
        }
    }

    fun replaceToken(token: String) {
        saveAccessToken(token)
    }
}
