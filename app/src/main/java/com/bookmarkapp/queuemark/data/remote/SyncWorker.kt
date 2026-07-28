package com.bookmarkapp.queuemark.data.remote

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bookmarkapp.queuemark.data.local.BookmarkDao
import com.google.firebase.auth.FirebaseAuth
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

// Pushes every dirty (isSynced = false) row to Firestore and flips the flag.
// No signed-in user yet -> retry later (auth may still be initializing).
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val dao: BookmarkDao,
    private val firestoreService: FirestoreService,
    private val firebaseAuth: FirebaseAuth
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val userId = firebaseAuth.currentUser?.uid ?: return Result.retry()

        val dirty = dao.getUnsynced()
        var allPushed = true
        for (entity in dirty) {
            firestoreService.upsertBookmark(userId, entity)
                .onSuccess { dao.markSynced(entity.id) }
                .onFailure { allPushed = false }
        }

        return if (allPushed) Result.success() else Result.retry()
    }
}
