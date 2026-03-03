package com.storystream.reader_app.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

// Reuse existing RefreshRequest/AuthResponse defined in AuthApi.kt to avoid duplicate types
interface RefreshApi {
    @POST("/api/auth/refresh")
    fun refresh(@Body req: RefreshRequest): Call<AuthResponse>
}
