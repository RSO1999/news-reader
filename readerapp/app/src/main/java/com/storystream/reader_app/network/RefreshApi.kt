package com.storystream.reader_app.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface RefreshApi {
    @POST("/api/auth/refresh")
    fun refresh(@Body req: RefreshRequest): Call<AuthResponse>
}
