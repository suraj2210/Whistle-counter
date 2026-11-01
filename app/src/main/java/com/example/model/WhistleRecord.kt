package com.example.model

data class WhistleRecord(
    val whistleNumber: Int,
    val timestampMs: Long = System.currentTimeMillis(),
    val durationSec: Float = 2.0f,
    val intervalSinceLastSec: Long = 0L
)
