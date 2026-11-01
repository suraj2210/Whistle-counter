package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = FlameOrange,
    onPrimary = Color.White,
    primaryContainer = FlameOrangeDark,
    onPrimaryContainer = Color.White,
    secondary = AcousticGold,
    onSecondary = Color.Black,
    secondaryContainer = AcousticGoldDark,
    onSecondaryContainer = Color.White,
    tertiary = SteamCyan,
    onTertiary = Color.Black,
    background = CharcoalDark,
    onBackground = TextPrimaryDark,
    surface = CharcoalSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = CharcoalSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    error = AlertRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = FlameOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEDD5),
    onPrimaryContainer = FlameOrangeDark,
    secondary = AcousticGoldDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF92400E),
    tertiary = SteamCyan,
    onTertiary = Color.Black,
    background = CreamBackground,
    onBackground = TextPrimaryLight,
    surface = CreamSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = CreamSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    error = AlertRed,
    onError = Color.White
)

@Composable
fun WhistleCounterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep warm signature culinary colors for consistency
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
