package com.storystream.reader_app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.storystream.reader_app.data.SecureTokenStore

@HiltAndroidApp
class ReaderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            SecureTokenStore.init(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
