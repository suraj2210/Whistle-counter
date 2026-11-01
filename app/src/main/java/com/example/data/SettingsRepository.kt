package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.DetectorSettings
import com.example.model.SensitivityLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("whistle_settings_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<DetectorSettings> = _settings.asStateFlow()

    private fun loadSettings(): DetectorSettings {
        val sensName = prefs.getString("sensitivity", SensitivityLevel.MEDIUM.name) ?: SensitivityLevel.MEDIUM.name
        val sensitivity = try {
            SensitivityLevel.valueOf(sensName)
        } catch (_: Exception) {
            SensitivityLevel.MEDIUM
        }
        val duration = prefs.getFloat("min_duration", 1.4f)
        val cooldown = prefs.getInt("cooldown", 15)
        val voice = prefs.getBoolean("voice_enabled", true)
        val vibrate = prefs.getBoolean("vibrate_enabled", true)
        val keepScreen = prefs.getBoolean("keep_screen_awake", true)
        val alarmSound = prefs.getString("alarm_sound", "Loud Siren") ?: "Loud Siren"

        return DetectorSettings(
            sensitivity = sensitivity,
            minWhistleDurationSec = duration,
            cooldownSec = cooldown,
            voiceAnnouncementsEnabled = voice,
            vibrationEnabled = vibrate,
            keepScreenAwake = keepScreen,
            alarmSound = alarmSound
        )
    }

    fun updateSettings(newSettings: DetectorSettings) {
        prefs.edit()
            .putString("sensitivity", newSettings.sensitivity.name)
            .putFloat("min_duration", newSettings.minWhistleDurationSec)
            .putInt("cooldown", newSettings.cooldownSec)
            .putBoolean("voice_enabled", newSettings.voiceAnnouncementsEnabled)
            .putBoolean("vibrate_enabled", newSettings.vibrationEnabled)
            .putBoolean("keep_screen_awake", newSettings.keepScreenAwake)
            .putString("alarm_sound", newSettings.alarmSound)
            .apply()

        _settings.value = newSettings
    }
}
