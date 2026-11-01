package com.example.ui

import com.example.audio.AudioTelemetry
import com.example.model.CookingPreset
import com.example.model.CookingSession
import com.example.model.DetectorSettings
import com.example.model.WhistleRecord

enum class CookerTab {
    COUNTER,
    PRESETS,
    HISTORY,
    SETTINGS
}

data class CookerUiState(
    val dishName: String = "White Rice / Basmati",
    val targetWhistles: Int = 3,
    val currentWhistles: Int = 0,
    val isListening: Boolean = false,
    val isTargetReached: Boolean = false,
    val showTargetAlarmDialog: Boolean = false,
    val cookingStartTimeMs: Long? = null,
    val elapsedCookingTimeSec: Long = 0L,
    val whistleLog: List<WhistleRecord> = emptyList(),
    val audioTelemetry: AudioTelemetry = AudioTelemetry(),
    val activePreset: CookingPreset? = null,
    val settings: DetectorSettings = DetectorSettings(),
    val hasRecordPermission: Boolean = false,
    val selectedTab: CookerTab = CookerTab.COUNTER,
    val presets: List<CookingPreset> = emptyList(),
    val pastSessions: List<CookingSession> = emptyList(),
    val isSimulating: Boolean = false
) {
    val remainingWhistles: Int
        get() = (targetWhistles - currentWhistles).coerceAtLeast(0)

    val progressFraction: Float
        get() = if (targetWhistles > 0) (currentWhistles.toFloat() / targetWhistles).coerceIn(0f, 1f) else 0f

    val formattedElapsedTime: String
        get() {
            val mins = elapsedCookingTimeSec / 60
            val secs = elapsedCookingTimeSec % 60
            return "%02d:%02d".format(mins, secs)
        }
}
