package com.bookmarkapp.queuemark.data.remote

import com.bookmarkapp.queuemark.data.local.BookmarkEntity
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

// Mirror of /users/{userId}/bookmarks/{bookmarkId}. Times are stored as epoch
// millis (Long) rather than Firestore Timestamps to round-trip losslessly
// with the Room columns.
@Singleton
class FirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    suspend fun upsertBookmark(userId: String, bookmark: BookmarkEntity): Result<Unit> =
        runCatching {
            bookmarks(userId).document(bookmark.id).set(
                mapOf(
                    "url" to bookmark.url,
                    "title" to bookmark.title,
                    "description" to bookmark.description,
                    "estimatedReadTime" to bookmark.estimatedReadTime,
                    "createdAt" to bookmark.createdAt,
                    "reminderTime" to bookmark.reminderTime,
                    "isCompleted" to bookmark.isCompleted,
                    "completedAt" to bookmark.completedAt
                )
            ).await()
            Unit
        }

    fun observeRemoteBookmarks(userId: String): Flow<List<BookmarkEntity>> = callbackFlow {
        val registration = bookmarks(userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val entities = snapshot?.documents.orEmpty().mapNotNull { doc ->
                val url = doc.getString("url") ?: return@mapNotNull null
                val title = doc.getString("title") ?: return@mapNotNull null
                BookmarkEntity(
                    id = doc.id,
                    url = url,
                    title = title,
                    description = doc.getString("description"),
                    estimatedReadTime = doc.getLong("estimatedReadTime")?.toInt() ?: 1,
                    createdAt = doc.getLong("createdAt") ?: 0L,
                    reminderTime = doc.getLong("reminderTime"),
                    isCompleted = doc.getBoolean("isCompleted") ?: false,
                    completedAt = doc.getLong("completedAt"),
                    isSynced = true
                )
            }
            trySend(entities)
        }
        awaitClose { registration.remove() }
    }

    private fun bookmarks(userId: String) =
        firestore.collection("users").document(userId).collection("bookmarks")
}
