package com.bookmarkapp.queuemark.data.remote

import com.bookmarkapp.queuemark.data.local.BookmarkDao
import com.bookmarkapp.queuemark.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

// Mirrors Firestore back into Room while a user is signed in.
// Echo suppression (DATABASE_SCHEMA.md rule 2): a remote doc never overwrites
// a local row that is still dirty (isSynced = false) — the local edit wins
// until SyncWorker pushes it; last-write-wins after that.
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class RemoteMirror @Inject constructor(
    private val authRepository: AuthRepository,
    private val firestoreService: FirestoreService,
    private val dao: BookmarkDao,
    @ApplicationScope private val scope: CoroutineScope
) {

    fun start() {
        scope.launch {
            authRepository.authState
                .flatMapLatest { user ->
                    if (user == null) emptyFlow()
                    else firestoreService.observeRemoteBookmarks(user.uid)
                }
                .catch { /* listener errors are non-fatal; next sign-in re-attaches */ }
                .collect { remote ->
                    remote.forEach { entity ->
                        val local = dao.getById(entity.id)
                        if (local == null || local.isSynced) {
                            dao.upsert(entity)
                        }
                    }
                }
        }
    }
}
