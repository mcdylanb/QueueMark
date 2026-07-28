package com.bookmarkapp.queuemark.data.remote

interface SyncScheduler {
    fun requestSync()
    fun scheduleReminder(bookmarkId: String, triggerAtMillis: Long)
    fun cancelReminder(bookmarkId: String)
}
