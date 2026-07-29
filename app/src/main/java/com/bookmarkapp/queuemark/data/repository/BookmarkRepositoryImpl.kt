package com.bookmarkapp.queuemark.data.repository

import com.bookmarkapp.queuemark.data.local.BookmarkDao
import com.bookmarkapp.queuemark.data.local.BookmarkEntity
import com.bookmarkapp.queuemark.data.local.toDomain
import com.bookmarkapp.queuemark.data.local.toEntity
import com.bookmarkapp.queuemark.data.remote.SyncScheduler
import com.bookmarkapp.queuemark.di.IoDispatcher
import com.bookmarkapp.queuemark.domain.TimeProvider
import com.bookmarkapp.queuemark.domain.model.Bookmark
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class BookmarkRepositoryImpl @Inject constructor(
    private val dao: BookmarkDao,
    private val syncScheduler: SyncScheduler,
    private val timeProvider: TimeProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : BookmarkRepository {

    override fun observeActive(): Flow<List<Bookmark>> =
        dao.observeActive().map { list -> list.map(BookmarkEntity::toDomain) }

    override fun observeCompleted(): Flow<List<Bookmark>> =
        dao.observeCompleted().map { list -> list.map(BookmarkEntity::toDomain) }

    override fun observeQuickWins(): Flow<List<Bookmark>> =
        dao.observeQuickWins().map { list -> list.map(BookmarkEntity::toDomain) }

    override fun search(query: String): Flow<List<Bookmark>> =
        dao.search(query).map { list -> list.map(BookmarkEntity::toDomain) }

    override fun observeById(id: String): Flow<Bookmark?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun add(bookmark: Bookmark): Result<Unit> = writeAndSync {
        dao.upsert(bookmark.toEntity().copy(isSynced = false))
    }

    override suspend fun update(bookmark: Bookmark): Result<Unit> = writeAndSync {
        dao.upsert(bookmark.toEntity().copy(isSynced = false))
    }

    override suspend fun delete(id: String): Result<Unit> = writeAndSync {
        dao.deleteById(id)
    }

    override suspend fun setCompleted(id: String, completed: Boolean): Result<Unit> =
        writeAndSync {
            dao.setCompleted(
                id = id,
                isCompleted = completed,
                completedAt = if (completed) timeProvider.now() else null
            )
        }

    override suspend fun setReminder(id: String, reminderTime: Long?): Result<Unit> =
        writeAndSync {
            dao.setReminder(id, reminderTime)
        }

    override suspend fun getUnsynced(): List<Bookmark> = withContext(ioDispatcher) {
        dao.getUnsynced().map(BookmarkEntity::toDomain)
    }

    override suspend fun markSynced(id: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching { dao.markSynced(id) }
    }

    override suspend fun clearAll() = withContext(ioDispatcher) {
        dao.clearAll()
    }

    // Every local mutation leaves the row dirty (isSynced = false, enforced by the
    // DAO queries) and pokes the scheduler so Phase 5's SyncWorker can mirror it.
    private suspend fun writeAndSync(block: suspend () -> Unit): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                block()
                syncScheduler.requestSync()
            }
        }
}
