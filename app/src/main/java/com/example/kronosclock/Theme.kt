package com.example.kronosanalogclock.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF3A86FF),
    secondary = Color(0xFFFF006E),
    tertiary = Color(0xFF8338EC),
    background = Color(0xFFF7F7F7),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF111111),
    onSurface = Color(0xFF111111)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7AB8FF),
    secondary = Color(0xFFFF5A9C),
    tertiary = Color(0xFFB18CFF),
    background = Color(0xFF0E0E10),
    surface = Color(0xFF16161A),
    onPrimary = Color(0xFF0E0E10),
    onSecondary = Color(0xFF0E0E10),
    onTertiary = Color(0xFF0E0E10),
    onBackground = Color(0xFFEDEDED),
    onSurface = Color(0xFFEDEDED)
)

@Composable
fun KronosClockTheme(
    darkTheme: Boolean,
    useDynamicColor: Boolean,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colors = when {
        useDynamicColor && Build.VERSION.SDK_INT >= 31 -> {
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        content = content
    )
}
