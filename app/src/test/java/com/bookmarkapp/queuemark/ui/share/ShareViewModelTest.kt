package com.bookmarkapp.queuemark.ui.share

import com.bookmarkapp.queuemark.domain.ReminderPreset
import com.bookmarkapp.queuemark.domain.ReminderTimeCalculator
import com.bookmarkapp.queuemark.testutil.FakeBookmarkRepository
import com.bookmarkapp.queuemark.testutil.FakeSyncScheduler
import com.bookmarkapp.queuemark.testutil.FakeUrlMetadataService
import com.bookmarkapp.queuemark.testutil.MainDispatcherRule
import com.bookmarkapp.queuemark.testutil.MutableTimeProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ShareViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeBookmarkRepository()
    private val metadataService = FakeUrlMetadataService()
    private val syncScheduler = FakeSyncScheduler()
    private val clock = MutableTimeProvider(nowMillis = 1_000_000L)

    private lateinit var viewModel: ShareViewModel

    @Before
    fun setUp() {
        viewModel = ShareViewModel(
            repository = repository,
            metadataService = metadataService,
            syncScheduler = syncScheduler,
            timeProvider = clock
        )
    }

    @Test
    fun `shared text with surrounding noise extracts the url and saves`() = runTest {
        viewModel.onSharedText("Check this out! https://example.com/article so good")

        val saved = repository.rows.value.values.single()
        assertEquals("https://example.com/article", saved.url)
        assertEquals("Scraped Title", saved.title)
        assertEquals("Para one.\n\nPara two.", saved.content)
        assertFalse(saved.isSynced)
    }

    @Test
    fun `shared text without a url flags invalid`() = runTest {
        viewModel.onSharedText("just some words")

        assertFalse(viewModel.uiState.value.hasValidUrl)
        assertTrue(repository.rows.value.isEmpty())
    }

    @Test
    fun `scrape failure still saves with url as title`() = runTest {
        metadataService.result = Result.failure(RuntimeException("offline"))

        viewModel.onSharedText("https://example.com/a")

        val saved = repository.rows.value.values.single()
        assertEquals("https://example.com/a", saved.title)
        assertEquals(1, saved.estimatedReadTime)
    }

    @Test
    fun `preset selection persists reminder and schedules work`() = runTest {
        viewModel.onSharedText("https://example.com/a")
        viewModel.onAction(ShareUiAction.OnPresetSelected(ReminderPreset.TONIGHT))

        val expected = ReminderTimeCalculator.forPreset(ReminderPreset.TONIGHT, clock.nowMillis)
        val saved = repository.rows.value.values.single()
        assertEquals(expected, saved.reminderTime)
        assertEquals(saved.id to expected, syncScheduler.reminders.single())
    }

    @Test
    fun `custom pick replaces preset reminder`() = runTest {
        viewModel.onSharedText("https://example.com/a")
        viewModel.onAction(ShareUiAction.OnPresetSelected(ReminderPreset.TONIGHT))
        viewModel.onAction(ShareUiAction.OnCustomPicked(5_000_000L))

        val saved = repository.rows.value.values.single()
        assertEquals(5_000_000L, saved.reminderTime)
        assertTrue(viewModel.uiState.value.customPicked)
        assertEquals(5_000_000L, syncScheduler.reminders.last().second)
    }

    @Test
    fun `done finishes the flow`() = runTest {
        viewModel.onSharedText("https://example.com/a")
        viewModel.onAction(ShareUiAction.OnDone)
        assertTrue(viewModel.uiState.value.isFinished)
    }

    @Test
    fun `re-delivered intent does not create a second bookmark`() = runTest {
        viewModel.onSharedText("https://example.com/a")
        viewModel.onSharedText("https://example.com/a")
        assertEquals(1, repository.rows.value.size)
    }
}
