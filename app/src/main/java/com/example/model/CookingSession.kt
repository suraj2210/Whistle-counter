package com.example.model

data class CookingSession(
    val id: String = System.currentTimeMillis().toString(),
    val dishName: String,
    val targetWhistles: Int,
    val actualWhistles: Int,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val whistles: List<WhistleRecord> = emptyList(),
    val completedSuccessfully: Boolean = true
) {
    val durationMinutes: Long
        get() = ((endTimeMs - startTimeMs) / 1000) / 60

    val durationSeconds: Long
        get() = ((endTimeMs - startTimeMs) / 1000) % 60
}
