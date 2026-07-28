package com.bookmarkapp.queuemark.di

import com.bookmarkapp.queuemark.data.remote.NoOpSyncScheduler
import com.bookmarkapp.queuemark.data.remote.SyncScheduler
import com.bookmarkapp.queuemark.data.repository.BookmarkRepository
import com.bookmarkapp.queuemark.data.repository.BookmarkRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindBookmarkRepository(impl: BookmarkRepositoryImpl): BookmarkRepository

    @Binds
    @Singleton
    abstract fun bindSyncScheduler(impl: NoOpSyncScheduler): SyncScheduler
}
