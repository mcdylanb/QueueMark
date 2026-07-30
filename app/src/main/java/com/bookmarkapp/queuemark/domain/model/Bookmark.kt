package com.bookmarkapp.queuemark.domain.model

data class Bookmark(
    val id: String,
    val url: String,
    val title: String,
    val description: String?,
    val content: String? = null,
    val estimatedReadTime: Int,
    val createdAt: Long,
    val reminderTime: Long?,
    val isCompleted: Boolean,
    val completedAt: Long?,
    val isSynced: Boolean,
    val isDeleted: Boolean
)
