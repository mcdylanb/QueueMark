package com.bookmarkapp.queuemark.domain

import kotlin.math.max
import kotlin.math.roundToInt

object ReadTimeCalculator {
    private const val WORDS_PER_MINUTE = 200.0

    fun estimateMinutes(wordCount: Int): Int =
        max(1, (wordCount / WORDS_PER_MINUTE).roundToInt())
}
