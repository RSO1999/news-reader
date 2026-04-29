package com.storystream.reader_app.network

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

data class AuthRequest(val email: String, val password: String)
data class RefreshRequest(val refreshToken: String)
data class AuthResponse(val token: String, val refreshToken: String? = null)
data class UpgradeResponse(val token: String, val tier: String)

interface AuthApi {
    @Headers("Content-Type: application/json")
    @POST("/api/auth/register")
    suspend fun register(@Body req: AuthRequest): AuthResponse

    @Headers("Content-Type: application/json")
    @POST("/api/auth/login")
    suspend fun login(@Body req: AuthRequest): AuthResponse

    @Headers("Content-Type: application/json")
    @POST("/api/auth/refresh")
    fun refresh(@Body req: RefreshRequest): retrofit2.Call<AuthResponse>

    @Headers("Content-Type: application/json")
    @POST("/api/user/upgrade")
    suspend fun upgradeUser(): UpgradeResponse
}
