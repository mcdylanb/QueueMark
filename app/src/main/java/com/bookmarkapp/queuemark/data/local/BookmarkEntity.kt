package com.bookmarkapp.queuemark.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bookmarkapp.queuemark.domain.model.Bookmark
import java.util.UUID

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val url: String,
    val title: String,
    val description: String?,
    // Readable article text extracted at save time; local-only, never synced.
    val content: String? = null,
    val estimatedReadTime: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val reminderTime: Long? = null,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)

fun BookmarkEntity.toDomain(): Bookmark = Bookmark(
    id = id,
    url = url,
    title = title,
    description = description,
    content = content,
    estimatedReadTime = estimatedReadTime,
    createdAt = createdAt,
    reminderTime = reminderTime,
    isCompleted = isCompleted,
    completedAt = completedAt,
    isSynced = isSynced,
    isDeleted = false
)

fun Bookmark.toEntity(): BookmarkEntity = BookmarkEntity(
    id = id,
    url = url,
    title = title,
    description = description,
    content = content,
    estimatedReadTime = estimatedReadTime,
    createdAt = createdAt,
    reminderTime = reminderTime,
    isCompleted = isCompleted,
    completedAt = completedAt,
    isSynced = isSynced,
    isDeleted = false
)
