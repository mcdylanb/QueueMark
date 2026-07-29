package com.bookmarkapp.queuemark.data.remote

interface SyncScheduler {
    fun requestSync()
    fun scheduleReminder(bookmarkId: String, triggerAtMillis: Long)
    fun cancelReminder(bookmarkId: String)

    // Account switches must clear device-global reminder state: pending
    // WorkManager jobs AND already-posted notifications, or the next user
    // inherits the previous user's reminders.
    fun cancelAllReminders()
}
