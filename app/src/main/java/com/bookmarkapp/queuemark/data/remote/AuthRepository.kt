package com.bookmarkapp.queuemark.data.remote

import kotlinx.coroutines.flow.Flow

data class AuthUser(
    val uid: String,
    val email: String?,
    val isAnonymous: Boolean
)

interface AuthRepository {
    val authState: Flow<AuthUser?>
    val currentUserId: String?

    suspend fun signInAnonymously(): Result<Unit>
    suspend fun signInWithEmail(email: String, password: String): Result<Unit>
    suspend fun signUpWithEmail(email: String, password: String): Result<Unit>
    fun signOut()
}
