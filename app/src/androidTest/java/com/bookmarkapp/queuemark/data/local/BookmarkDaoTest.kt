package com.bookmarkapp.queuemark.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookmarkDaoTest {

    private lateinit var database: BookmarkDatabase
    private lateinit var dao: BookmarkDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BookmarkDatabase::class.java
        ).build()
        dao = database.bookmarkDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun entity(
        id: String,
        readTime: Int = 3,
        title: String = "Title $id",
        description: String? = null,
        url: String = "https://example.com/$id",
        createdAt: Long = 1000L
    ) = BookmarkEntity(
        id = id,
        url = url,
        title = title,
        description = description,
        estimatedReadTime = readTime,
        createdAt = createdAt
    )

    @Test
    fun insertedBookmarkAppearsInActiveQueue() = runTest {
        dao.upsert(entity("b1"))

        val active = dao.observeActive().first()
        assertEquals(listOf("b1"), active.map { it.id })
    }

    @Test
    fun activeQueueOrderedByCreationAge() = runTest {
        dao.upsert(entity("newer", createdAt = 2000L))
        dao.upsert(entity("older", createdAt = 1000L))

        val active = dao.observeActive().first()
        assertEquals(listOf("older", "newer"), active.map { it.id })
    }

    @Test
    fun completingMovesBetweenQueues() = runTest {
        dao.upsert(entity("b1"))
        dao.setCompleted("b1", isCompleted = true, completedAt = 5000L)

        assertTrue(dao.observeActive().first().isEmpty())
        val completed = dao.observeCompleted().first()
        assertEquals(listOf("b1"), completed.map { it.id })
        assertEquals(5000L, completed.first().completedAt)
    }

    @Test
    fun quickWinsBoundaryIsExclusiveAtFiveMinutes() = runTest {
        dao.upsert(entity("in", readTime = 4))
        dao.upsert(entity("out", readTime = 5))

        val quickWins = dao.observeQuickWins().first()
        assertEquals(listOf("in"), quickWins.map { it.id })
    }

    @Test
    fun completedBookmarksExcludedFromQuickWins() = runTest {
        dao.upsert(entity("b1", readTime = 2))
        dao.setCompleted("b1", isCompleted = true, completedAt = 5000L)

        assertTrue(dao.observeQuickWins().first().isEmpty())
    }

    @Test
    fun searchMatchesTitleDescriptionAndUrl() = runTest {
        dao.upsert(entity("t", title = "Kotlin Coroutines Guide"))
        dao.upsert(entity("d", description = "deep dive into kotlin flows"))
        dao.upsert(entity("u", url = "https://kotlinlang.org/docs"))
        dao.upsert(entity("none", title = "Swift Concurrency"))

        val results = dao.search("kotlin").first()
        assertEquals(setOf("t", "d", "u"), results.map { it.id }.toSet())
    }

    @Test
    fun unsyncedRoundTrip() = runTest {
        dao.upsert(entity("b1"))
        dao.upsert(entity("b2").copy(isSynced = true))

        assertEquals(listOf("b1"), dao.getUnsynced().map { it.id })

        dao.markSynced("b1")
        assertTrue(dao.getUnsynced().isEmpty())
    }

    @Test
    fun largeReaderContentRoundTrips() = runTest {
        val content = "paragraph ".repeat(20_000) // ~200KB
        dao.upsert(entity("b1").copy(content = content))

        assertEquals(content, dao.getById("b1")?.content)
    }

    @Test
    fun mutationsMarkRowDirty() = runTest {
        dao.upsert(entity("b1").copy(isSynced = true))
        dao.setReminder("b1", reminderTime = 9000L)

        val row = dao.getById("b1")!!
        assertEquals(9000L, row.reminderTime)
        assertEquals(false, row.isSynced)
    }
}
