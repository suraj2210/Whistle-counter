package com.example.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.example.model.DetectorSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

data class AudioTelemetry(
    val currentDb: Float = 0f,
    val isWhistling: Boolean = false,
    val ongoingDurationSec: Float = 0f,
    val cooldownRemainingSec: Int = 0,
    val waveAmplitudes: List<Float> = List(16) { 0.05f }
)

class WhistleDetector(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var cooldownJob: Job? = null

    private val _telemetry = MutableStateFlow(AudioTelemetry())
    val telemetry: StateFlow<AudioTelemetry> = _telemetry.asStateFlow()

    private val _whistleDetectedEvents = MutableSharedFlow<Float>(extraBufferCapacity = 8)
    val whistleDetectedEvents: SharedFlow<Float> = _whistleDetectedEvents.asSharedFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    var settings: DetectorSettings = DetectorSettings()

    private var whistleCandidateStartTime = 0L
    private var isInCooldown = false
    private var cooldownRemaining = 0

    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun startListening() {
        if (!hasRecordPermission()) return
        if (_isListening.value) return

        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = max(minBufferSize, 2048)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                return
            }

            audioRecord?.startRecording()
            _isListening.value = true

            recordingJob = scope.launch(Dispatchers.Default) {
                val audioBuffer = ShortArray(1024)
                val recentWaveforms = FloatArray(16) { 0.05f }
                var waveIndex = 0

                var qualifiedWhistle = false
                var candidateDurationMs = 0L

                while (isActive && _isListening.value) {
                    val readSamples = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0
                    if (readSamples <= 0) {
                        delay(20)
                        continue
                    }

                    // Compute RMS Amplitude and approximate dB
                    var sumSquares = 0.0
                    var zeroCrossings = 0
                    var diffSquares = 0.0

                    for (i in 0 until readSamples) {
                        val sample = audioBuffer[i].toDouble()
                        sumSquares += sample * sample

                        if (i > 0) {
                            val prevSample = audioBuffer[i - 1].toDouble()
                            if ((sample > 0 && prevSample < 0) || (sample < 0 && prevSample > 0)) {
                                zeroCrossings++
                            }
                            val diff = sample - prevSample
                            diffSquares += diff * diff
                        }
                    }

                    val rms = sqrt(sumSquares / readSamples)
                    val diffRms = sqrt(diffSquares / max(1, readSamples - 1))

                    // Normalize dB between 0 and 100
                    val db = if (rms > 1.0) {
                        (20.0 * log10(rms)).toFloat().coerceIn(10f, 95f)
                    } else {
                        10f
                    }

                    val zcr = zeroCrossings.toFloat() / readSamples
                    val highFreqRatio = if (rms > 5.0) (diffRms / (rms * 2.0)).toFloat().coerceIn(0f, 1f) else 0f

                    // Update circular wave samples for visualizer
                    val normalizedAmp = (rms.toFloat() / 15000f).coerceIn(0.04f, 1f)
                    recentWaveforms[waveIndex % 16] = normalizedAmp
                    waveIndex++

                    // Whistle classification logic:
                    // Cooker whistle has high amplitude (>= thresholdDb) AND sustained high frequency (ZCR or high freq energy)
                    val thresholdDb = settings.sensitivity.thresholdDb
                    val isCandidate = db >= thresholdDb && (zcr >= 0.14f || highFreqRatio >= 0.35f)

                    val now = System.currentTimeMillis()

                    if (isCandidate && !isInCooldown) {
                        if (whistleCandidateStartTime == 0L) {
                            whistleCandidateStartTime = now
                        }
                        candidateDurationMs = now - whistleCandidateStartTime
                        val candidateDurationSec = candidateDurationMs / 1000f

                        if (candidateDurationSec >= settings.minWhistleDurationSec) {
                            qualifiedWhistle = true
                        }

                        _telemetry.value = AudioTelemetry(
                            currentDb = db,
                            isWhistling = true,
                            ongoingDurationSec = candidateDurationSec,
                            cooldownRemainingSec = cooldownRemaining,
                            waveAmplitudes = recentWaveforms.toList()
                        )
                    } else {
                        // Sound dropped below threshold or is in cooldown
                        if (whistleCandidateStartTime > 0L) {
                            val finalDurationSec = candidateDurationMs / 1000f
                            whistleCandidateStartTime = 0L

                            if (qualifiedWhistle && !isInCooldown) {
                                // Trigger Whistle Detected Event!
                                _whistleDetectedEvents.tryEmit(finalDurationSec)
                                triggerCooldown(settings.cooldownSec)
                            }
                            qualifiedWhistle = false
                        }

                        _telemetry.value = AudioTelemetry(
                            currentDb = db,
                            isWhistling = false,
                            ongoingDurationSec = 0f,
                            cooldownRemainingSec = cooldownRemaining,
                            waveAmplitudes = recentWaveforms.toList()
                        )
                    }

                    delay(30)
                }
            }
        } catch (_: Exception) {
            stopListening()
        }
    }

    fun stopListening() {
        _isListening.value = false
        recordingJob?.cancel()
        recordingJob = null
        whistleCandidateStartTime = 0L

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {
        } finally {
            audioRecord = null
        }

        _telemetry.value = AudioTelemetry(
            currentDb = 0f,
            isWhistling = false,
            ongoingDurationSec = 0f,
            cooldownRemainingSec = cooldownRemaining,
            waveAmplitudes = List(16) { 0.05f }
        )
    }

    private fun triggerCooldown(seconds: Int) {
        cooldownJob?.cancel()
        isInCooldown = true
        cooldownRemaining = seconds

        cooldownJob = scope.launch(Dispatchers.Default) {
            while (cooldownRemaining > 0) {
                delay(1000)
                cooldownRemaining--
                _telemetry.value = _telemetry.value.copy(cooldownRemainingSec = cooldownRemaining)
            }
            isInCooldown = false
        }
    }

    /**
     * Simulation method: simulates a cooker whistle lasting the given seconds.
     * Perfect for testing, demonstrations, or quiet kitchen preview.
     */
    fun simulateWhistle(durationSec: Float = 2.2f) {
        scope.launch(Dispatchers.Default) {
            val startTime = System.currentTimeMillis()
            val totalSteps = (durationSec * 20).toInt()

            for (step in 0..totalSteps) {
                val elapsed = (System.currentTimeMillis() - startTime) / 1000f
                val fakeDb = 72f + (kotlin.random.Random.nextFloat() * 6f)
                val fakeAmps = List(16) { 0.4f + kotlin.random.Random.nextFloat() * 0.5f }

                _telemetry.value = AudioTelemetry(
                    currentDb = fakeDb,
                    isWhistling = true,
                    ongoingDurationSec = elapsed,
                    cooldownRemainingSec = 0,
                    waveAmplitudes = fakeAmps
                )
                delay(50)
            }

            // Completed simulated whistle
            _whistleDetectedEvents.tryEmit(durationSec)
            triggerCooldown(settings.cooldownSec)
        }
    }
}
