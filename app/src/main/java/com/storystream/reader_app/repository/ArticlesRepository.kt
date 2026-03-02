package com.storystream.reader_app.repository

import com.storystream.reader_app.data.ArticleResponse
import com.storystream.reader_app.data.ContextResponse
import com.storystream.reader_app.network.ArticlesApi
import com.storystream.reader_app.network.NetworkModule
import retrofit2.Response

class ArticlesRepository {
    private val api: ArticlesApi = NetworkModule.retrofit.create(ArticlesApi::class.java)

    suspend fun getArticles(page: Int, size: Int, personalized: Boolean): Result<Pair<List<ArticleResponse>, Int>> {
        return try {
            val resp = api.getArticles(page = page, size = size, personalized = personalized)
            Result.success(resp.content to resp.totalPages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getArticle(id: String): Result<Response<ArticleResponse>> {
        return try {
            val resp = api.getArticle(id)
            Result.success(resp)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getContext(id: String): Result<Response<ContextResponse>> {
        return try {
            val resp = api.getContext(id)
            Result.success(resp)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTrending(limit: Int): Result<List<ArticleResponse>> {
        return try {
            val resp = api.getTrending(limit)
            Result.success(resp)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveArticle(id: String): Result<Boolean> {
        return try {
            val resp = api.postSave(id)
            Result.success(resp.status == "saved")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
