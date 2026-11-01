package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CookerUiState
import com.example.ui.theme.AcousticGold
import com.example.ui.theme.AlertRed
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.SteamCyan
import com.example.ui.theme.SuccessGreen

@Composable
fun WhistleProgressDial(
    uiState: CookerUiState,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = uiState.progressFraction,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "whistle_progress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_rings")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val isWhistlingNow = uiState.audioTelemetry.isWhistling
    val isCooldown = uiState.audioTelemetry.cooldownRemainingSec > 0
    val isTargetDone = uiState.isTargetReached

    val primaryAccent = when {
        isTargetDone -> AlertRed
        isWhistlingNow -> FlameOrange
        isCooldown -> AcousticGold
        else -> MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = modifier
            .size(270.dp)
            .testTag("whistle_progress_dial"),
        contentAlignment = Alignment.Center
    ) {
        // Background canvas for circular track and active arcs
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 18.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            val arcSize = Size(diameter, diameter)

            // Background full track circle
            drawArc(
                color = Color.Gray.copy(alpha = 0.15f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Dynamic Progress Arc
            if (animatedProgress > 0f) {
                val sweep = 270f * animatedProgress
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            AcousticGold,
                            FlameOrange,
                            if (isTargetDone) AlertRed else FlameOrange
                        )
                    ),
                    startAngle = 135f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // If actively whistling right now, draw glowing steam ripples
            if (isWhistlingNow) {
                drawCircle(
                    color = FlameOrange.copy(alpha = 0.12f),
                    radius = (diameter / 2) * pulseScale,
                    style = Stroke(width = 8.dp.toPx())
                )
            }
        }

        // Inner Content Box
        Surface(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Status pill badge
                StatusBadge(
                    uiState = uiState,
                    isWhistling = isWhistlingNow,
                    isCooldown = isCooldown,
                    isTargetDone = isTargetDone
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Large Whistle Count
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${uiState.currentWhistles}",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 62.sp,
                            fontWeight = FontWeight.Black,
                            color = primaryAccent
                        ),
                        modifier = Modifier.testTag("current_whistle_text")
                    )
                    Text(
                        text = "/${uiState.targetWhistles}",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .padding(bottom = 10.dp, start = 2.dp)
                            .testTag("target_whistle_text")
                    )
                }

                Text(
                    text = if (uiState.targetWhistles == 1) "Whistle" else "Whistles",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Bottom Indicator: Remaining or Timer
                if (isTargetDone) {
                    Text(
                        text = "TARGET REACHED!",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = AlertRed
                        )
                    )
                } else if (uiState.remainingWhistles > 0) {
                    Text(
                        text = "${uiState.remainingWhistles} more needed",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                    )
                } else {
                    Text(
                        text = "Cook Complete",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = SuccessGreen
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    uiState: CookerUiState,
    isWhistling: Boolean,
    isCooldown: Boolean,
    isTargetDone: Boolean
) {
    val (bgCol, textCol, labelText, icon) = when {
        isTargetDone -> Quadruple(
            AlertRed.copy(alpha = 0.2f),
            AlertRed,
            "TURN OFF STOVE",
            Icons.Default.CheckCircle
        )
        isWhistling -> Quadruple(
            FlameOrange.copy(alpha = 0.2f),
            FlameOrange,
            "WHISTLING %.1fs".format(uiState.audioTelemetry.ongoingDurationSec),
            Icons.AutoMirrored.Filled.VolumeUp
        )
        isCooldown -> Quadruple(
            AcousticGold.copy(alpha = 0.2f),
            AcousticGold,
            "COOLDOWN ${uiState.audioTelemetry.cooldownRemainingSec}s",
            Icons.Default.HourglassTop
        )
        uiState.isListening -> Quadruple(
            SteamCyan.copy(alpha = 0.18f),
            SteamCyan,
            "LISTENING",
            Icons.Default.Mic
        )
        else -> Quadruple(
            Color.Gray.copy(alpha = 0.12f),
            Color.Gray,
            "STANDBY",
            Icons.Default.HourglassTop
        )
    }

    Surface(
        shape = CircleShape,
        color = bgCol,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textCol,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = labelText,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = textCol
                )
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
