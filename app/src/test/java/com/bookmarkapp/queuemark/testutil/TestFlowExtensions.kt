package com.bookmarkapp.queuemark.testutil

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

// Subscribes to a hot flow for the duration of the test. Must run on an
// UnconfinedTestDispatcher: backgroundScope's default dispatcher is queued,
// so a plain launch never starts collecting during the test body and
// stateIn(WhileSubscribed) state flows would silently stay at their initial
// value (the bug that made every uiState assertion vacuous).
@OptIn(ExperimentalCoroutinesApi::class)
fun TestScope.collectEagerly(flow: Flow<*>): Job =
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.collect() }
