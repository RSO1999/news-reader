package com.storystream.reader_app.network

import com.storystream.reader_app.data.ArticleResponse
import com.storystream.reader_app.data.ContextResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.Response
import retrofit2.http.POST

data class ArticlesPageResponse(val content: List<ArticleResponse>, val totalPages: Int)

data class SaveResponse(val status: String)

interface ArticlesApi {
    @GET("/api/articles")
    suspend fun getArticles(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("personalized") personalized: Boolean = false
    ): ArticlesPageResponse

    @GET("/api/articles/{id}")
    suspend fun getArticle(@Path("id") id: String): Response<ArticleResponse>

    @GET("/api/articles/{id}/context")
    suspend fun getContext(@Path("id") id: String): Response<ContextResponse>

    @GET("/api/articles/trending")
    suspend fun getTrending(@Query("limit") limit: Int = 10): List<ArticleResponse>

    @POST("/api/articles/{id}/save")
    suspend fun postSave(@Path("id") id: String): SaveResponse
}
