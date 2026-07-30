package com.bookmarkapp.queuemark.di

import com.bookmarkapp.queuemark.data.local.GuestSessionStore
import com.bookmarkapp.queuemark.data.local.PrefsGuestSessionStore
import com.bookmarkapp.queuemark.data.remote.AuthRepository
import com.bookmarkapp.queuemark.data.remote.AuthRepositoryImpl
import com.bookmarkapp.queuemark.data.remote.JsoupUrlMetadataService
import com.bookmarkapp.queuemark.data.remote.SyncScheduler
import com.bookmarkapp.queuemark.data.remote.UrlMetadataService
import com.bookmarkapp.queuemark.data.remote.WorkManagerSyncScheduler
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
    abstract fun bindSyncScheduler(impl: WorkManagerSyncScheduler): SyncScheduler

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUrlMetadataService(impl: JsoupUrlMetadataService): UrlMetadataService

    @Binds
    @Singleton
    abstract fun bindGuestSessionStore(impl: PrefsGuestSessionStore): GuestSessionStore
}
