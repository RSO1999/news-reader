package com.storystream.reader_app.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import com.storystream.reader_app.data.TokenProvider

class AuthInterceptor @Inject constructor(
    private val tokenProvider: TokenProvider
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        if (path.startsWith("/api/auth/")) {
            return chain.proceed(request)
        }

        val token = tokenProvider.getToken()
        return if (!token.isNullOrBlank()) {
            val newReq = request.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
            chain.proceed(newReq)
        } else {
            chain.proceed(request)
        }
    }
}
