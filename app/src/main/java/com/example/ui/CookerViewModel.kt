package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AlertManager
import com.example.audio.WhistleDetector
import com.example.data.CookingRepository
import com.example.data.SettingsRepository
import com.example.model.CookingPreset
import com.example.model.CookingSession
import com.example.model.DefaultPresets
import com.example.model.DetectorSettings
import com.example.model.WhistleRecord
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class CookerViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val cookingRepo = CookingRepository(application)
    private val alertManager = AlertManager(application)
    private val whistleDetector = WhistleDetector(application, viewModelScope)

    private val _uiState = MutableStateFlow(
        CookerUiState(
            hasRecordPermission = whistleDetector.hasRecordPermission(),
            activePreset = DefaultPresets.list.firstOrNull(),
            targetWhistles = DefaultPresets.list.firstOrNull()?.defaultWhistles ?: 3,
            dishName = DefaultPresets.list.firstOrNull()?.name ?: "Custom Dish"
        )
    )
    val uiState: StateFlow<CookerUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var lastWhistleTimeMs = 0L

    init {
        // Collect persistent settings
        viewModelScope.launch {
            settingsRepo.settings.collectLatest { newSettings ->
                whistleDetector.settings = newSettings
                _uiState.update { it.copy(settings = newSettings) }
            }
        }

        // Collect presets
        viewModelScope.launch {
            cookingRepo.presets.collectLatest { list ->
                _uiState.update { it.copy(presets = list) }
            }
        }

        // Collect past sessions history
        viewModelScope.launch {
            cookingRepo.history.collectLatest { list ->
                _uiState.update { it.copy(pastSessions = list) }
            }
        }

        // Collect live audio telemetry
        viewModelScope.launch {
            whistleDetector.telemetry.collectLatest { telem ->
                _uiState.update { it.copy(audioTelemetry = telem) }
            }
        }

        // Collect detector listening state
        viewModelScope.launch {
            whistleDetector.isListening.collectLatest { listening ->
                _uiState.update { it.copy(isListening = listening) }
            }
        }

        // Collect detected whistles from the audio engine
        viewModelScope.launch {
            whistleDetector.whistleDetectedEvents.collectLatest { durationSec ->
                recordWhistleEvent(durationSec)
            }
        }
    }

    fun onPermissionResult(isGranted: Boolean) {
        _uiState.update { it.copy(hasRecordPermission = isGranted) }
    }

    fun selectTab(tab: CookerTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setDishName(name: String) {
        _uiState.update { it.copy(dishName = name) }
    }

    fun setTargetWhistles(target: Int) {
        val clamped = target.coerceIn(1, 30)
        _uiState.update { it.copy(targetWhistles = clamped) }
    }

    fun incrementTarget() {
        setTargetWhistles(_uiState.value.targetWhistles + 1)
    }

    fun decrementTarget() {
        setTargetWhistles(_uiState.value.targetWhistles - 1)
    }

    fun applyPreset(preset: CookingPreset) {
        _uiState.update {
            it.copy(
                activePreset = preset,
                dishName = preset.name,
                targetWhistles = preset.defaultWhistles,
                selectedTab = CookerTab.COUNTER
            )
        }
    }

    fun startListeningAndCooking() {
        val now = System.currentTimeMillis()
        if (_uiState.value.cookingStartTimeMs == null) {
            _uiState.update { it.copy(cookingStartTimeMs = now, elapsedCookingTimeSec = 0L) }
            startElapsedTimer()
        }

        whistleDetector.startListening()
    }

    fun pauseListening() {
        whistleDetector.stopListening()
    }

    private fun startElapsedTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val start = _uiState.value.cookingStartTimeMs
                if (start != null) {
                    val elapsed = (System.currentTimeMillis() - start) / 1000
                    _uiState.update { it.copy(elapsedCookingTimeSec = elapsed) }
                }
            }
        }
    }

    private fun recordWhistleEvent(durationSec: Float) {
        val now = System.currentTimeMillis()
        val interval = if (lastWhistleTimeMs > 0L) (now - lastWhistleTimeMs) / 1000 else 0L
        lastWhistleTimeMs = now

        val newCount = _uiState.value.currentWhistles + 1
        val target = _uiState.value.targetWhistles
        val record = WhistleRecord(
            whistleNumber = newCount,
            timestampMs = now,
            durationSec = durationSec,
            intervalSinceLastSec = interval
        )

        val updatedLogs = _uiState.value.whistleLog + record
        val isTargetReached = newCount >= target

        _uiState.update {
            it.copy(
                currentWhistles = newCount,
                whistleLog = updatedLogs,
                isTargetReached = isTargetReached,
                showTargetAlarmDialog = isTargetReached
            )
        }

        // Trigger Audio / Voice / Vibration feedback
        val settings = _uiState.value.settings
        if (isTargetReached) {
            alertManager.startTargetReachedAlarm(
                dishName = _uiState.value.dishName,
                totalWhistles = newCount,
                voiceEnabled = settings.voiceAnnouncementsEnabled,
                vibrateEnabled = settings.vibrationEnabled
            )
        } else {
            alertManager.playWhistleCountFeedback(
                whistleCount = newCount,
                targetWhistles = target,
                voiceEnabled = settings.voiceAnnouncementsEnabled,
                vibrateEnabled = settings.vibrationEnabled
            )
        }
    }

    fun manualAddWhistle() {
        recordWhistleEvent(durationSec = 2.0f)
    }

    fun manualSubtractWhistle() {
        val current = _uiState.value.currentWhistles
        if (current <= 0) return
        val newCount = current - 1
        val updatedLogs = _uiState.value.whistleLog.dropLast(1)
        val target = _uiState.value.targetWhistles

        _uiState.update {
            it.copy(
                currentWhistles = newCount,
                whistleLog = updatedLogs,
                isTargetReached = newCount >= target
            )
        }
    }

    fun simulateCookerWhistle() {
        if (!_uiState.value.isListening) {
            startListeningAndCooking()
        }
        whistleDetector.simulateWhistle(2.5f)
    }

    fun dismissAlarm() {
        alertManager.stopAlarm()
        _uiState.update { it.copy(showTargetAlarmDialog = false) }

        // Save session in history if whistles were counted
        val state = _uiState.value
        if (state.currentWhistles > 0 && state.cookingStartTimeMs != null) {
            val session = CookingSession(
                dishName = state.dishName,
                targetWhistles = state.targetWhistles,
                actualWhistles = state.currentWhistles,
                startTimeMs = state.cookingStartTimeMs,
                endTimeMs = System.currentTimeMillis(),
                whistles = state.whistleLog,
                completedSuccessfully = state.currentWhistles >= state.targetWhistles
            )
            cookingRepo.saveCompletedSession(session)
        }
    }

    fun resetSession() {
        alertManager.stopAlarm()
        whistleDetector.stopListening()
        timerJob?.cancel()
        timerJob = null
        lastWhistleTimeMs = 0L

        _uiState.update {
            it.copy(
                currentWhistles = 0,
                isListening = false,
                isTargetReached = false,
                showTargetAlarmDialog = false,
                cookingStartTimeMs = null,
                elapsedCookingTimeSec = 0L,
                whistleLog = emptyList()
            )
        }
    }

    fun updateSettings(newSettings: DetectorSettings) {
        settingsRepo.updateSettings(newSettings)
    }

    fun addCustomPreset(name: String, whistles: Int, category: String, tips: String) {
        val id = "custom_" + System.currentTimeMillis()
        val preset = CookingPreset(
            id = id,
            name = name,
            category = category.ifBlank { "Custom" },
            defaultWhistles = whistles.coerceIn(1, 30),
            flameAdvice = "Adjust flame to medium after 1st whistle",
            tips = tips
        )
        cookingRepo.addCustomPreset(preset)
    }

    fun clearHistory() {
        cookingRepo.clearHistory()
    }

    override fun onCleared() {
        super.onCleared()
        whistleDetector.stopListening()
        alertManager.release()
        timerJob?.cancel()
    }
}
