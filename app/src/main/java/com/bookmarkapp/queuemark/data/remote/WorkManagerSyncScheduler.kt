package com.bookmarkapp.queuemark.data.remote

import android.app.NotificationManager
import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.bookmarkapp.queuemark.domain.TimeProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class WorkManagerSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timeProvider: TimeProvider
) : SyncScheduler {

    override fun requestSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            SYNC_WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }

    override fun scheduleReminder(bookmarkId: String, triggerAtMillis: Long) {
        val delayMillis = max(0L, triggerAtMillis - timeProvider.now())
        val request = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(NotificationWorker.KEY_BOOKMARK_ID to bookmarkId))
            .addTag(REMINDER_TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            reminderWorkName(bookmarkId),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    override fun cancelReminder(bookmarkId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(reminderWorkName(bookmarkId))
    }

    override fun cancelAllReminders() {
        WorkManager.getInstance(context).cancelAllWorkByTag(REMINDER_TAG)
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancelAll()
    }

    private fun reminderWorkName(bookmarkId: String) = "reminder_$bookmarkId"

    private companion object {
        const val SYNC_WORK_NAME = "bookmark_sync"
        const val REMINDER_TAG = "reminder"
    }
}
