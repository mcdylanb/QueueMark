package com.bookmarkapp.queuemark.di

import android.content.Context
import androidx.room.Room
import com.bookmarkapp.queuemark.data.local.BookmarkDao
import com.bookmarkapp.queuemark.data.local.BookmarkDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BookmarkDatabase =
        Room.databaseBuilder(context, BookmarkDatabase::class.java, "queuemark.db").build()

    @Provides
    fun provideBookmarkDao(database: BookmarkDatabase): BookmarkDao = database.bookmarkDao()
}
