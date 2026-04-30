package com.storystream.reader_app

import android.app.Application
import com.storystream.reader_app.data.SecureTokenStore
import com.storystream.reader_app.repository.AuthStateHolder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class ReaderApplication : Application() {
    private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        SecureTokenStore.prepare(this)

        startupScope.launch {
            try {
                SecureTokenStore.init(this@ReaderApplication)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            AuthStateHolder.initializeFromStoredToken()
        }
    }
}
