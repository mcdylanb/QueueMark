package com.bookmarkapp.queuemark.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {

    @Query("SELECT * FROM bookmarks WHERE isCompleted = 0 ORDER BY createdAt ASC")
    fun observeActive(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE isCompleted = 1 ORDER BY completedAt DESC")
    fun observeCompleted(): Flow<List<BookmarkEntity>>

    @Query(
        "SELECT * FROM bookmarks WHERE isCompleted = 0 AND estimatedReadTime < 5 " +
            "ORDER BY estimatedReadTime ASC"
    )
    fun observeQuickWins(): Flow<List<BookmarkEntity>>

    @Query(
        "SELECT * FROM bookmarks WHERE title LIKE '%' || :query || '%' " +
            "OR description LIKE '%' || :query || '%' " +
            "OR url LIKE '%' || :query || '%' " +
            "ORDER BY createdAt DESC"
    )
    fun search(query: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE id = :id")
    fun observeById(id: String): Flow<BookmarkEntity?>

    @Query("SELECT * FROM bookmarks WHERE id = :id")
    suspend fun getById(id: String): BookmarkEntity?

    @Query("SELECT * FROM bookmarks WHERE isSynced = 0")
    suspend fun getUnsynced(): List<BookmarkEntity>

    @Upsert
    suspend fun upsert(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query(
        "UPDATE bookmarks SET isCompleted = :isCompleted, completedAt = :completedAt, " +
            "isSynced = 0 WHERE id = :id"
    )
    suspend fun setCompleted(id: String, isCompleted: Boolean, completedAt: Long?)

    @Query("UPDATE bookmarks SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE bookmarks SET reminderTime = :reminderTime, isSynced = 0 WHERE id = :id")
    suspend fun setReminder(id: String, reminderTime: Long?)
}
