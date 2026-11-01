package com.example.model

enum class SensitivityLevel(val displayName: String, val thresholdDb: Float, val description: String) {
    LOW("Low (Noisy Room)", 68f, "Best when exhaust chimney or mixer grinder is nearby"),
    MEDIUM("Medium (Standard)", 58f, "Balanced for most domestic kitchens"),
    HIGH("High (Quiet / Far)", 50f, "Detects distant cooker or soft whistling")
}

data class DetectorSettings(
    val sensitivity: SensitivityLevel = SensitivityLevel.MEDIUM,
    val minWhistleDurationSec: Float = 1.4f,
    val cooldownSec: Int = 15,
    val voiceAnnouncementsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val keepScreenAwake: Boolean = true,
    val alarmSound: String = "Loud Siren"
)
