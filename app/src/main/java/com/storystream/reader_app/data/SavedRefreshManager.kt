package com.storystream.reader_app.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

object SavedRefreshManager {
    // simple shared flow used as a signal; replay 0 is fine, emissions only trigger collectors
    val refreshFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun triggerRefresh() {
        // fire-and-forget wrapper for convenience
        scope.launch {
            refreshFlow.emit(Unit)
        }
    }
}
