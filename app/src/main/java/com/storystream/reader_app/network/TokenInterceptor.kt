package com.storystream.reader_app.network

import okhttp3.Interceptor
import okhttp3.Response
import com.storystream.reader_app.data.SecureTokenStore
import com.storystream.reader_app.data.UserSession

class TokenInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        if (path.startsWith("/api/auth/")) {
            return chain.proceed(request)
        }
        val inMemory = UserSession.token
        val token = if (!inMemory.isNullOrBlank() && !com.storystream.reader_app.util.JwtUtils.isExpired(inMemory)) {
            inMemory
        } else {
            SecureTokenStore.getAccessToken()
        }
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
