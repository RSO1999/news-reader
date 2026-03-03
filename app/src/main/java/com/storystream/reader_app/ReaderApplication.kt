package com.storystream.reader_app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.storystream.reader_app.data.SecureTokenStore

@HiltAndroidApp
class ReaderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize secure token store early so DI singletons that read tokens
        // (TokenProviderImpl, AuthInterceptor, TokenAuthenticator) can rely on it.
        try {
            SecureTokenStore.init(this)
        } catch (e: Exception) {
            // Fail open: if Tink initialization fails, we clear and continue; apps may handle this.
            e.printStackTrace()
        }
    }
}
