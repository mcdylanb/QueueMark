package com.bookmarkapp.queuemark.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

// exportSchema is off, so instead of MigrationTestHelper we hand-build a v1
// database with the exact DDL Room generated for version 1, migrate it, and
// let Room's schema validation on open prove the result matches the v2 entity.
@RunWith(AndroidJUnit4::class)
class Migration1To2Test {

    @Test
    fun migrate1To2_preservesRowsAndAddsNullContent() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(DB_NAME)

        context.openOrCreateDatabase(DB_NAME, Context.MODE_PRIVATE, null).use { db ->
            db.execSQL(
                "CREATE TABLE bookmarks (" +
                    "id TEXT NOT NULL PRIMARY KEY, " +
                    "url TEXT NOT NULL, " +
                    "title TEXT NOT NULL, " +
                    "description TEXT, " +
                    "estimatedReadTime INTEGER NOT NULL, " +
                    "createdAt INTEGER NOT NULL, " +
                    "reminderTime INTEGER, " +
                    "isCompleted INTEGER NOT NULL, " +
                    "completedAt INTEGER, " +
                    "isSynced INTEGER NOT NULL)"
            )
            db.execSQL(
                "INSERT INTO bookmarks VALUES " +
                    "('b1', 'https://example.com/a', 'Kept Title', NULL, 3, 1000, NULL, 0, NULL, 1)"
            )
            db.version = 1
        }

        val database = Room.databaseBuilder(context, BookmarkDatabase::class.java, DB_NAME)
            .addMigrations(BookmarkDatabase.MIGRATION_1_2, BookmarkDatabase.MIGRATION_2_3)
            .build()
        try {
            // Opening validates the migrated schema against the v3 entity.
            val row = database.bookmarkDao().getById("b1")
            requireNotNull(row)
            assertEquals("Kept Title", row.title)
            assertEquals(3, row.estimatedReadTime)
            assertNull(row.content)
            assertEquals(false, row.isDeleted)
        } finally {
            database.close()
            context.deleteDatabase(DB_NAME)
        }
    }

    // The exact scenario that crashed after the tombstone merge: a device
    // already on v2 (has content, no isDeleted) upgrading to v3.
    @Test
    fun migrate2To3_addsTombstoneToExistingV2Database() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(DB_NAME)

        context.openOrCreateDatabase(DB_NAME, Context.MODE_PRIVATE, null).use { db ->
            db.execSQL(
                "CREATE TABLE bookmarks (" +
                    "id TEXT NOT NULL PRIMARY KEY, " +
                    "url TEXT NOT NULL, " +
                    "title TEXT NOT NULL, " +
                    "description TEXT, " +
                    "content TEXT, " +
                    "estimatedReadTime INTEGER NOT NULL, " +
                    "createdAt INTEGER NOT NULL, " +
                    "reminderTime INTEGER, " +
                    "isCompleted INTEGER NOT NULL, " +
                    "completedAt INTEGER, " +
                    "isSynced INTEGER NOT NULL)"
            )
            db.execSQL(
                "INSERT INTO bookmarks VALUES ('b2', 'https://example.com/b', 'V2 Row', " +
                    "NULL, 'Saved text.', 5, 2000, NULL, 0, NULL, 1)"
            )
            db.version = 2
        }

        val database = Room.databaseBuilder(context, BookmarkDatabase::class.java, DB_NAME)
            .addMigrations(BookmarkDatabase.MIGRATION_1_2, BookmarkDatabase.MIGRATION_2_3)
            .build()
        try {
            val row = database.bookmarkDao().getById("b2")
            requireNotNull(row)
            assertEquals("V2 Row", row.title)
            assertEquals("Saved text.", row.content)
            assertEquals(false, row.isDeleted)
        } finally {
            database.close()
            context.deleteDatabase(DB_NAME)
        }
    }

    private companion object {
        const val DB_NAME = "migration-test.db"
    }
}
