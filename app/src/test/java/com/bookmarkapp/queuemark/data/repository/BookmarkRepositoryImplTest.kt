package com.bookmarkapp.queuemark.data.repository

import com.bookmarkapp.queuemark.data.local.BookmarkDao
import com.bookmarkapp.queuemark.data.local.BookmarkEntity
import com.bookmarkapp.queuemark.data.remote.SyncScheduler
import com.bookmarkapp.queuemark.domain.TimeProvider
import com.bookmarkapp.queuemark.domain.model.Bookmark
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeBookmarkDao : BookmarkDao {
    val rows = MutableStateFlow<Map<String, BookmarkEntity>>(emptyMap())

    private fun sorted(predicate: (BookmarkEntity) -> Boolean) =
        rows.map { map -> map.values.filter(predicate).sortedBy { it.createdAt } }

    override fun observeActive(): Flow<List<BookmarkEntity>> = sorted { !it.isCompleted && !it.isDeleted }

    override fun observeCompleted(): Flow<List<BookmarkEntity>> = sorted { it.isCompleted && !it.isDeleted }

    override fun observeQuickWins(): Flow<List<BookmarkEntity>> =
        sorted { !it.isCompleted && !it.isDeleted && it.estimatedReadTime < 5 }

    override fun search(query: String): Flow<List<BookmarkEntity>> = sorted {
        !it.isDeleted && (it.title.contains(query, true) ||
            it.description?.contains(query, true) == true ||
            it.url.contains(query, true))
    }

    override fun observeById(id: String): Flow<BookmarkEntity?> = rows.map { it[id] }

    override suspend fun getById(id: String): BookmarkEntity? = rows.value[id]

    override suspend fun getUnsynced(): List<BookmarkEntity> =
        rows.value.values.filter { !it.isSynced }

    override suspend fun upsert(bookmark: BookmarkEntity) {
        rows.value = rows.value + (bookmark.id to bookmark)
    }

    override suspend fun softDelete(id: String) {
        rows.value[id]?.let {
            rows.value = rows.value + (id to it.copy(isDeleted = true, isSynced = false))
        }
    }

    override suspend fun permanentlyDelete(id: String) {
        rows.value = rows.value - id
    }

    override suspend fun setCompleted(id: String, isCompleted: Boolean, completedAt: Long?) {
        rows.value[id]?.let {
            rows.value = rows.value + (id to it.copy(
                isCompleted = isCompleted,
                completedAt = completedAt,
                isSynced = false
            ))
        }
    }

    override suspend fun markSynced(id: String) {
        rows.value[id]?.let {
            rows.value = rows.value + (id to it.copy(isSynced = true))
        }
    }

    override suspend fun setReminder(id: String, reminderTime: Long?) {
        rows.value[id]?.let {
            rows.value = rows.value + (id to it.copy(
                reminderTime = reminderTime,
                isSynced = false
            ))
        }
    }

    override suspend fun clearAll() {
        rows.value = emptyMap()
    }
}

private class RecordingSyncScheduler : SyncScheduler {
    var syncRequests = 0
    override fun requestSync() {
        syncRequests++
    }

    override fun scheduleReminder(bookmarkId: String, triggerAtMillis: Long) = Unit
    override fun cancelReminder(bookmarkId: String) = Unit
    override fun cancelAllReminders() = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class BookmarkRepositoryImplTest {

    private val dao = FakeBookmarkDao()
    private val syncScheduler = RecordingSyncScheduler()
    private val fixedNow = 1_700_000_000_000L
    private val repository = BookmarkRepositoryImpl(
        dao = dao,
        syncScheduler = syncScheduler,
        timeProvider = TimeProvider { fixedNow },
        ioDispatcher = UnconfinedTestDispatcher()
    )

    private fun bookmark(id: String = "b1", readTime: Int = 3) = Bookmark(
        id = id,
        url = "https://example.com/$id",
        title = "Title $id",
        description = null,
        estimatedReadTime = readTime,
        createdAt = fixedNow,
        reminderTime = null,
        isCompleted = false,
        completedAt = null,
        isSynced = true,
        isDeleted = false
    )

    @Test
    fun `add stores row dirty and requests sync`() = runTest {
        val result = repository.add(bookmark())

        assertTrue(result.isSuccess)
        assertFalse(dao.rows.value.getValue("b1").isSynced)
        assertEquals(1, syncScheduler.syncRequests)
    }

    @Test
    fun `setCompleted stamps completedAt from injected clock`() = runTest {
        repository.add(bookmark())
        repository.setCompleted("b1", completed = true)

        val row = dao.rows.value.getValue("b1")
        assertTrue(row.isCompleted)
        assertEquals(fixedNow, row.completedAt)
        assertFalse(row.isSynced)
    }

    @Test
    fun `setCompleted false clears completedAt`() = runTest {
        repository.add(bookmark())
        repository.setCompleted("b1", completed = true)
        repository.setCompleted("b1", completed = false)

        val row = dao.rows.value.getValue("b1")
        assertFalse(row.isCompleted)
        assertNull(row.completedAt)
    }

    @Test
    fun `delete marks row as deleted and requests sync`() = runTest {
        repository.add(bookmark())
        repository.delete("b1")

        val row = dao.rows.value.getValue("b1")
        assertTrue(row.isDeleted)
        assertFalse(row.isSynced)
        assertEquals(2, syncScheduler.syncRequests)
    }

    @Test
    fun `markSynced flips flag without requesting sync`() = runTest {
        repository.add(bookmark())
        val requestsAfterAdd = syncScheduler.syncRequests

        repository.markSynced("b1")

        assertTrue(dao.rows.value.getValue("b1").isSynced)
        assertEquals(requestsAfterAdd, syncScheduler.syncRequests)
    }

    @Test
    fun `observeActive maps entities to domain`() = runTest {
        repository.add(bookmark("b1"))
        repository.add(bookmark("b2"))
        repository.setCompleted("b2", completed = true)

        val active = repository.observeActive().first()
        assertEquals(listOf("b1"), active.map { it.id })
    }
}
