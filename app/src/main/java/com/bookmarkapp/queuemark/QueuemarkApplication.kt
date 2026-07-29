package com.bookmarkapp.queuemark

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.bookmarkapp.queuemark.data.local.GuestSessionStore
import com.bookmarkapp.queuemark.data.remote.AuthRepository
import com.bookmarkapp.queuemark.data.remote.RemoteMirror
import com.bookmarkapp.queuemark.di.ApplicationScope
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class QueuemarkApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var hiltWorkerFactory: HiltWorkerFactory

    @Inject
    lateinit var remoteMirror: RemoteMirror

    @Inject
    lateinit var guestStore: GuestSessionStore

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    // @HiltWorker workers need this factory; requires removing the default
    // WorkManager initializer in the manifest.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(hiltWorkerFactory).build()

    override fun onCreate() {
        super.onCreate()
        remoteMirror.start()

        // Lazy account for offline-created guests: retry on every app start
        // until it succeeds. Once it does, the SyncWorker's retry-until-auth
        // loop uploads everything saved while accountless.
        if (guestStore.isGuest && authRepository.currentUserId == null) {
            appScope.launch {
                authRepository.signInAnonymously() // best-effort; offline is fine
            }
        }
    }
}
