package com.example.audio

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import java.util.Locale

class AlertManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private var toneGenerator: ToneGenerator? = null
    private var alarmRingtone: Ringtone? = null
    private var isAlarmPlaying = false

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    companion object {
        const val CHANNEL_ID = "cooker_whistle_alerts"
        const val NOTIFICATION_ID = 1001
    }

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        } catch (_: Exception) {}

        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (_: Exception) {}

        createNotificationChannel()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
            isTtsReady = true
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pressure Cooker Whistle Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when pressure cooker target whistles are reached"
                enableVibration(true)
                setBypassDnd(true)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun playWhistleCountFeedback(whistleCount: Int, targetWhistles: Int, voiceEnabled: Boolean, vibrateEnabled: Boolean) {
        // Haptic pulse
        if (vibrateEnabled) {
            vibrateShort()
        }

        // Audio Beep
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 250)
        } catch (_: Exception) {}

        // Voice announcement
        if (voiceEnabled && isTtsReady) {
            val remaining = targetWhistles - whistleCount
            val message = if (remaining > 0) {
                "Whistle $whistleCount counted. $remaining remaining."
            } else {
                "Whistle $whistleCount counted."
            }
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "whistle_$whistleCount")
        }
    }

    fun startTargetReachedAlarm(dishName: String, totalWhistles: Int, voiceEnabled: Boolean, vibrateEnabled: Boolean) {
        if (isAlarmPlaying) return
        isAlarmPlaying = true

        // Play alarm ringtone
        try {
            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            alarmRingtone = RingtoneManager.getRingtone(context, alertUri)?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    isLooping = true
                }
                play()
            }
        } catch (_: Exception) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 4000)
            } catch (_: Exception) {}
        }

        // Heavy SOS vibration pattern
        if (vibrateEnabled) {
            vibrateTargetAlert()
        }

        // Voice announcement
        if (voiceEnabled && isTtsReady) {
            val alertMsg = "Target reached! $totalWhistles whistles recorded for $dishName. Please turn off the stove now!"
            tts?.speak(alertMsg, TextToSpeech.QUEUE_FLUSH, null, "target_alert")
        }

        // System notification
        postTargetNotification(dishName, totalWhistles)
    }

    fun stopAlarm() {
        isAlarmPlaying = false
        try {
            if (alarmRingtone?.isPlaying == true) {
                alarmRingtone?.stop()
            }
            alarmRingtone = null
        } catch (_: Exception) {}

        try {
            toneGenerator?.stopTone()
        } catch (_: Exception) {}

        vibrator?.cancel()
        tts?.stop()
        notificationManager?.cancel(NOTIFICATION_ID)
    }

    private fun vibrateShort() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(180, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(180)
            }
        } catch (_: Exception) {}
    }

    private fun vibrateTargetAlert() {
        try {
            val timings = longArrayOf(0, 500, 200, 500, 200, 800, 300)
            val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(timings, amplitudes, 1) // loop index 1
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(timings, 1)
            }
        } catch (_: Exception) {}
    }

    @SuppressLint("NotificationPermission")
    private fun postTargetNotification(dishName: String, whistles: Int) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("🚨 Turn Off Stove: $whistles Whistles Reached!")
                .setContentText("Cooking complete for $dishName. Switch off burner immediately to avoid overcooking.")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager?.notify(NOTIFICATION_ID, notification)
        } catch (_: Exception) {}
    }

    fun release() {
        stopAlarm()
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {}

        try {
            tts?.shutdown()
            tts = null
        } catch (_: Exception) {}
    }
}
