package com.storystream.reader_app.data

import java.time.Instant
import com.google.gson.annotations.SerializedName

data class ArticleResponse(
    val id: String,
    val title: String,
    val section: String,
    val snippet: String,
    val imageUrl: String? = null,
    val publishedAt: String = Instant.now().toString(),
    val sourceName: String = "NewsWire",
    val externalUrl: String = ""
)

data class TopSection(val section: String, val views: Int, val percent: Double)

data class DailyUsage(val date: String, val reads: Int, val limit: Int, val isUnlimited: Boolean)

data class ReadingInsights(
    val userEmail: String?,
    val tier: String,
    val dailyUsage: DailyUsage,
    val topSections: List<TopSection>,
    val recentHistory: List<ArticleResponse>
)

// Context models for AI context responses
data class ContextEntity(
    val title: String,
    @SerializedName("summary")
    val summary: String,
    val source: String,
    val imageUrl: String? = null,
    val metadata: Map<String, Any>? = null
)

data class ContextResponse(val entities: List<ContextEntity>)
