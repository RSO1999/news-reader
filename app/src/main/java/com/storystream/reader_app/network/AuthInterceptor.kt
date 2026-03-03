package com.storystream.reader_app.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import com.storystream.reader_app.data.TokenProvider

/**
 * Synchronous OkHttp interceptor that attaches Authorization header when a token exists.
 * This uses TokenProvider.getToken() to keep the interceptor sync-friendly.
 */
class AuthInterceptor @Inject constructor(
    private val tokenProvider: TokenProvider
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        // Skip auth endpoints (e.g., login/register/refresh)
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
