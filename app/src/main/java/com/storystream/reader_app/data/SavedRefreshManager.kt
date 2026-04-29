package com.storystream.reader_app.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

object SavedRefreshManager {
    val refreshFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun triggerRefresh() {
        scope.launch {
            refreshFlow.emit(Unit)
        }
    }
}
