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
    primary = androidx.compose.ui.graphics.Color(0xFF1B4D3E),           // Deep Forest Green
    secondary = androidx.compose.ui.graphics.Color(0xFFD4AF37),         // Soft Gold/Amber
    tertiary = androidx.compose.ui.graphics.Color(0xFF633B0D),           // Deep Timber
    background = androidx.compose.ui.graphics.Color(0xFF141511),         // Dark earth night ground
    surface = androidx.compose.ui.graphics.Color(0xFF1B1D17),           // Dark sand surface
    onPrimary = androidx.compose.ui.graphics.Color(0xFFFDF5E6),         // Pale Cream
    onSecondary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),       // Pure White
    onTertiary = androidx.compose.ui.graphics.Color(0xFFFDF5E6),         // Pale Cream
    onBackground = androidx.compose.ui.graphics.Color(0xFFFDF5E6),       // Pale Cream
    onSurface = androidx.compose.ui.graphics.Color(0xFFFDF5E6),         // Pale Cream
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF23261F),   // Tonal night wood
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF2E3228), // Deep night sprout
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFF2A261D),  // Evening campfire shadow
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF1A1C16),     // Dark clay navigation
    outline = androidx.compose.ui.graphics.Color(0xFF2E3228),           // Deep mud border
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFFDF5E6),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFFDF5E6),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFFFDF5E6),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF8D9280)
)

private val LightColorScheme = lightColorScheme(
    primary = BiophilicPrimary,
    secondary = BiophilicSecondary,
    tertiary = BiophilicWood,
    background = BiophilicBg,
    surface = BiophilicWhite,
    onPrimary = BiophilicWhite,
    onSecondary = BiophilicText,
    onTertiary = BiophilicWhite,
    onBackground = BiophilicText,
    onSurface = BiophilicText,
    primaryContainer = BiophilicCardBg,
    secondaryContainer = BiophilicPillActive,
    tertiaryContainer = BiophilicArBg,
    surfaceVariant = BiophilicNavBg,
    outline = BiophilicBorder,
    onPrimaryContainer = BiophilicText,
    onSecondaryContainer = BiophilicText,
    onTertiaryContainer = BiophilicText,
    onSurfaceVariant = BiophilicMuted
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
        shapes = Shapes,
        content = content
    )
}
