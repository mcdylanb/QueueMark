package com.bookmarkapp.queuemark.domain

fun interface TimeProvider {
    fun now(): Long
}
