package com.bookmarkapp.queuemark

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.bookmarkapp.queuemark.data.remote.RemoteMirror
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class QueuemarkApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var hiltWorkerFactory: HiltWorkerFactory

    @Inject
    lateinit var remoteMirror: RemoteMirror

    // @HiltWorker workers need this factory; requires removing the default
    // WorkManager initializer in the manifest.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(hiltWorkerFactory).build()

    override fun onCreate() {
        super.onCreate()
        remoteMirror.start()
    }
}
