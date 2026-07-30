package com.bookmarkapp.queuemark.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [BookmarkEntity::class], version = 3, exportSchema = false)
abstract class BookmarkDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        // v2 adds the offline reader content column (local-only; never synced).
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bookmarks ADD COLUMN content TEXT")
            }
        }

        // v3 adds the delete tombstone (isDeleted) so deletions sync to
        // Firestore instead of resurrecting via the remote mirror.
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE bookmarks ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
    }
}
