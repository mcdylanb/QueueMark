package com.bookmarkapp.queuemark.testutil

import com.bookmarkapp.queuemark.data.remote.AuthRepository
import com.bookmarkapp.queuemark.data.remote.AuthUser
import com.bookmarkapp.queuemark.data.remote.UrlMetadata
import com.bookmarkapp.queuemark.data.remote.UrlMetadataService
import com.bookmarkapp.queuemark.data.repository.BookmarkRepository
import com.bookmarkapp.queuemark.domain.TimeProvider
import com.bookmarkapp.queuemark.domain.model.Bookmark
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeBookmarkRepository : BookmarkRepository {
    val rows = MutableStateFlow<Map<String, Bookmark>>(emptyMap())
    var failNextWrite = false

    fun seed(vararg bookmarks: Bookmark) {
        rows.value = bookmarks.associateBy { it.id }
    }

    private fun sorted(predicate: (Bookmark) -> Boolean) =
        rows.map { map -> map.values.filter(predicate).sortedBy { it.createdAt } }

    override fun observeActive(): Flow<List<Bookmark>> = sorted { !it.isCompleted && !it.isDeleted }
    override fun observeCompleted(): Flow<List<Bookmark>> = sorted { it.isCompleted && !it.isDeleted }
    override fun observeQuickWins(): Flow<List<Bookmark>> =
        sorted { !it.isCompleted && !it.isDeleted && it.estimatedReadTime < 5 }

    override fun search(query: String): Flow<List<Bookmark>> = sorted {
        !it.isDeleted && (it.title.contains(query, true) ||
            it.description?.contains(query, true) == true ||
            it.url.contains(query, true))
    }

    override fun observeById(id: String): Flow<Bookmark?> = rows.map { it[id] }

    override suspend fun add(bookmark: Bookmark): Result<Unit> = write {
        rows.value += bookmark.id to bookmark.copy(isSynced = false)
    }

    override suspend fun update(bookmark: Bookmark): Result<Unit> = add(bookmark)

    override suspend fun delete(id: String): Result<Unit> = write {
        rows.value[id]?.let {
            rows.value += id to it.copy(isDeleted = true, isSynced = false)
        }
    }

    override suspend fun setCompleted(id: String, completed: Boolean): Result<Unit> = write {
        rows.value[id]?.let {
            rows.value += id to it.copy(
                isCompleted = completed,
                completedAt = if (completed) 999L else null,
                isSynced = false
            )
        }
    }

    override suspend fun setReminder(id: String, reminderTime: Long?): Result<Unit> = write {
        rows.value[id]?.let {
            rows.value += id to it.copy(reminderTime = reminderTime, isSynced = false)
        }
    }

    override suspend fun getUnsynced(): List<Bookmark> =
        rows.value.values.filter { !it.isSynced }

    override suspend fun markSynced(id: String): Result<Unit> = write {
        rows.value[id]?.let { rows.value += id to it.copy(isSynced = true) }
    }

    override suspend fun clearAll() {
        rows.value = emptyMap()
    }

    private inline fun write(block: () -> Unit): Result<Unit> =
        if (failNextWrite) {
            failNextWrite = false
            Result.failure(RuntimeException("boom"))
        } else {
            block()
            Result.success(Unit)
        }
}

class FakeAuthRepository : AuthRepository {
    val user = MutableStateFlow<AuthUser?>(null)
    var failAnonymous = false
    var failEmailSignIn = false
    var signUpCalls = 0
    var linkCalls = 0

    override val authState: Flow<AuthUser?> = user
    override val currentUserId: String? get() = user.value?.uid

    override suspend fun signInAnonymously(): Result<Unit> {
        if (failAnonymous) return Result.failure(RuntimeException("offline"))
        user.value = AuthUser(uid = "anon", email = null, isAnonymous = true)
        return Result.success(Unit)
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<Unit> {
        if (failEmailSignIn) return Result.failure(RuntimeException("wrong password"))
        user.value = AuthUser(uid = "user", email = email, isAnonymous = false)
        return Result.success(Unit)
    }

    override suspend fun signUpWithEmail(email: String, password: String): Result<Unit> {
        signUpCalls++
        return signInWithEmail(email, password)
    }

    override fun signOut() {
        user.value = null
    }

    override suspend fun linkWithEmail(email: String, password: String): Result<Unit> {
        linkCalls++
        val current = user.value ?: return Result.failure(IllegalStateException("No user"))
        user.value = current.copy(email = email, isAnonymous = false)
        return Result.success(Unit)
    }
}

class FakeSyncScheduler : com.bookmarkapp.queuemark.data.remote.SyncScheduler {
    var syncRequests = 0
    var cancelAllCalled = false
    val reminders = mutableListOf<Pair<String, Long>>()

    override fun requestSync() {
        syncRequests++
    }

    override fun scheduleReminder(bookmarkId: String, triggerAtMillis: Long) {
        reminders += bookmarkId to triggerAtMillis
    }

    override fun cancelReminder(bookmarkId: String) {
        reminders.removeAll { it.first == bookmarkId }
    }

    override fun cancelAllReminders() {
        cancelAllCalled = true
        reminders.clear()
    }
}

class FakeGuestSessionStore : com.bookmarkapp.queuemark.data.local.GuestSessionStore {
    override var isGuest: Boolean = false
}

class FakeUrlMetadataService : UrlMetadataService {
    var result: Result<UrlMetadata> = Result.success(
        UrlMetadata(
            title = "Scraped Title",
            description = "Desc",
            wordCount = 1000,
            content = "Para one.\n\nPara two."
        )
    )

    override suspend fun fetch(url: String): Result<UrlMetadata> = result
}

class MutableTimeProvider(var nowMillis: Long = 0L) : TimeProvider {
    override fun now(): Long = nowMillis

    fun advanceMinutes(minutes: Double) {
        nowMillis += (minutes * 60_000).toLong()
    }
}

fun testBookmark(
    id: String = "b1",
    readTime: Int = 5,
    isCompleted: Boolean = false,
    createdAt: Long = 0L,
    content: String? = null
) = Bookmark(
    id = id,
    url = "https://example.com/$id",
    title = "Title $id",
    description = null,
    content = content,
    estimatedReadTime = readTime,
    createdAt = createdAt,
    reminderTime = null,
    isCompleted = isCompleted,
    completedAt = null,
    isSynced = true,
    isDeleted = false
)
