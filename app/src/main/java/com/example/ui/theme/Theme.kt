package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SoilSageDark,
    secondary = SoilMutedGreenDark,
    tertiary = SoilWoodDark,
    background = SoilBgDark,
    surface = SoilSurfaceDark,
    onPrimary = SoilBgDark,
    onSecondary = SoilTextDark,
    onTertiary = SoilBgDark,
    onBackground = SoilTextDark,
    onSurface = SoilTextDark,
    primaryContainer = SoilCardBgDark,
    secondaryContainer = SoilPillActiveDark,
    tertiaryContainer = SoilArBgDark,
    surfaceVariant = SoilNavBgDark,
    outline = SoilBorderDark,
    onPrimaryContainer = SoilTextDark,
    onSecondaryContainer = SoilTextDark,
    onTertiaryContainer = SoilTextDark,
    onSurfaceVariant = SoilMutedDark
)

private val LightColorScheme = lightColorScheme(
    primary = NaturalSage,
    secondary = NaturalMutedGreen,
    tertiary = NaturalWood,
    background = NaturalBg,
    surface = NaturalWhite,
    onPrimary = NaturalWhite,
    onSecondary = NaturalText,
    onTertiary = NaturalWhite,
    onBackground = NaturalText,
    onSurface = NaturalText,
    primaryContainer = NaturalCardBg,
    secondaryContainer = NaturalPillActive,
    tertiaryContainer = NaturalArBg,
    surfaceVariant = NaturalNavBg,
    outline = NaturalBorder,
    onPrimaryContainer = NaturalText,
    onSecondaryContainer = NaturalText,
    onTertiaryContainer = NaturalText,
    onSurfaceVariant = NaturalMuted
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep false by default to show our custom gorgeous brand colors consistently!
    content: @Composable () -> Unit,
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
