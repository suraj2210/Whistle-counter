package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioTelemetry
import com.example.model.DetectorSettings
import com.example.ui.theme.AcousticGold
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.SteamCyan

@Composable
fun LiveAudioWaveform(
    telemetry: AudioTelemetry,
    settings: DetectorSettings,
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    val isWhistling = telemetry.isWhistling
    val currentDb = telemetry.currentDb
    val thresholdDb = settings.sensitivity.thresholdDb

    val cardBorderColor by animateColorAsState(
        targetValue = when {
            isWhistling -> FlameOrange
            telemetry.cooldownRemainingSec > 0 -> AcousticGold
            isListening -> SteamCyan.copy(alpha = 0.5f)
            else -> Color.Transparent
        },
        label = "waveform_border"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("audio_waveform_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, cardBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Header: Status + dB Readout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isWhistling) Icons.Default.GraphicEq else Icons.Default.Sensors,
                        contentDescription = "Acoustic sensor",
                        tint = if (isWhistling) FlameOrange else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.height(16.dp)
                    )
                    Text(
                        text = if (isWhistling) "Steam Whistle In Progress" else if (isListening) "Live Acoustic Monitor" else "Mic Paused",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isWhistling) FlameOrange else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Text(
                    text = "${currentDb.toInt()} dB / ${thresholdDb.toInt()} dB trigger",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (currentDb >= thresholdDb) FlameOrange else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Waveform bar visualizer (16 dynamic bars)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val samples = telemetry.waveAmplitudes
                samples.forEachIndexed { index, amp ->
                    val animatedHeight by animateFloatAsState(
                        targetValue = if (isListening) amp.coerceIn(0.08f, 1.0f) else 0.08f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "bar_$index"
                    )

                    val barColor = when {
                        isWhistling -> FlameOrange
                        currentDb >= thresholdDb -> AcousticGold
                        isListening -> SteamCyan
                        else -> Color.Gray.copy(alpha = 0.3f)
                    }

                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .fillMaxHeight(animatedHeight)
                            .clip(RoundedCornerShape(3.dp))
                            .background(barColor)
                    )
                }
            }

            // Duration indicator when actively whistling
            if (isWhistling) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Whistling duration: %.1fs".format(telemetry.ongoingDurationSec),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = FlameOrange
                        )
                    )
                    Text(
                        text = "Threshold: %.1fs min".format(settings.minWhistleDurationSec),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}
