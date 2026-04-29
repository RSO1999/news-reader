package com.storystream.reader_app.network

import okhttp3.Interceptor
import okhttp3.Response
import com.storystream.reader_app.data.SecureTokenStore

class TokenInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        if (path.startsWith("/api/auth/")) {
            return chain.proceed(request)
        }
        val token = SecureTokenStore.getAccessToken()
        return if (token != null) {
            val newReq = request.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
            chain.proceed(newReq)
        } else {
            chain.proceed(request)
        }
    }
}
