package com.storystream.reader_app.data

object LocalStore {
    // Using title as temporary ID for prototype
    private val saved = mutableSetOf<String>()
    private val views = mutableMapOf<String, Int>()

    fun toggleSave(id: String) {
        if (saved.contains(id)) saved.remove(id) else saved.add(id)
    }

    fun isSaved(id: String): Boolean = saved.contains(id)

    fun getSavedArticles(): List<String> = saved.toList()

    fun recordView(id: String) {
        views[id] = (views[id] ?: 0) + 1
    }

    fun getTrending(limit: Int = 10): List<String> {
        return views.entries.sortedByDescending { it.value }.map { it.key }.take(limit)
    }

    // New helper: total views count (simple proxy for daily reads in prototype)
    fun getTotalViews(): Int {
        return views.values.sum()
    }

    // Optional: reset counts (not used currently)
    fun resetCounts() {
        views.clear()
        saved.clear()
    }
}
