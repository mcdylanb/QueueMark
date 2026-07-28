package com.bookmarkapp.queuemark.data.remote

import javax.inject.Inject

interface SyncScheduler {
    fun requestSync()
    fun scheduleReminder(bookmarkId: String, triggerAtMillis: Long)
    fun cancelReminder(bookmarkId: String)
}

// Placeholder binding until Phase 5 swaps in the WorkManager-backed scheduler.
class NoOpSyncScheduler @Inject constructor() : SyncScheduler {
    override fun requestSync() = Unit
    override fun scheduleReminder(bookmarkId: String, triggerAtMillis: Long) = Unit
    override fun cancelReminder(bookmarkId: String) = Unit
}
