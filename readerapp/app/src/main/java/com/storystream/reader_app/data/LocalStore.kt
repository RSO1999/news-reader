package com.storystream.reader_app.data

@Suppress("unused")
object LocalStore {
    fun toggleSave(id: String) { /* no-op */ }
    fun isSaved(id: String): Boolean = false
    fun getSavedArticles(): List<String> = emptyList()
    fun recordView(id: String) { /* no-op */ }
    fun getTrending(limit: Int = 10): List<String> = emptyList()
    fun getTotalViews(): Int = 0
    fun resetCounts() { /* no-op */ }
}
