package com.bookmarkapp.queuemark.data.repository

import com.bookmarkapp.queuemark.domain.model.Bookmark
import kotlinx.coroutines.flow.Flow

interface BookmarkRepository {
    fun observeActive(): Flow<List<Bookmark>>
    fun observeCompleted(): Flow<List<Bookmark>>
    fun observeQuickWins(): Flow<List<Bookmark>>
    fun search(query: String): Flow<List<Bookmark>>
    fun observeById(id: String): Flow<Bookmark?>

    suspend fun add(bookmark: Bookmark): Result<Unit>
    suspend fun update(bookmark: Bookmark): Result<Unit>
    suspend fun delete(id: String): Result<Unit>
    suspend fun setCompleted(id: String, completed: Boolean): Result<Unit>
    suspend fun setReminder(id: String, reminderTime: Long?): Result<Unit>

    suspend fun getUnsynced(): List<Bookmark>
    suspend fun markSynced(id: String): Result<Unit>
    suspend fun clearAll()
}
