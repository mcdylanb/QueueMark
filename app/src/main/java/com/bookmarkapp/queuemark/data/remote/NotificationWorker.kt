package com.bookmarkapp.queuemark.data.remote

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bookmarkapp.queuemark.MainActivity
import com.bookmarkapp.queuemark.R
import com.bookmarkapp.queuemark.data.local.BookmarkDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val dao: BookmarkDao
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val bookmarkId = inputData.getString(KEY_BOOKMARK_ID) ?: return Result.failure()
        val bookmark = dao.getById(bookmarkId) ?: return Result.success()

        // Reading it before the reminder fired is the happy path — stay silent.
        if (bookmark.isCompleted) return Result.success()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        val manager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Reading reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )

        val tapIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_BOOKMARK_ID, bookmarkId)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            bookmarkId.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Time to read")
            .setContentText(bookmark.title)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(bookmarkId.hashCode(), notification)
        return Result.success()
    }

    companion object {
        const val KEY_BOOKMARK_ID = "bookmarkId"
        private const val CHANNEL_ID = "reminders"
    }
}
