package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// --- Spacing Scale ---
data class Spacing(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val mediumSmall: Dp = 12.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp
)

val LocalSpacing = compositionLocalOf { Spacing() }

val MaterialTheme.spacing: Spacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSpacing.current

// --- Extended Semantic Status Colors ---
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val error: Color,
    val onError: Color,
    val premiumGold: Color
)

val LocalExtendedColors = staticCompositionLocalOf<ExtendedColors> {
    error("No ExtendedColors provided")
}

val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedColors.current

private val DarkColorScheme = darkColorScheme(
    primary = SoilSageDark,
    secondary = SoilMutedGreenDark,
    tertiary = SoilWoodDark,
    background = SoilBgDark,
    surface = SoilSurfaceDark,
    onPrimary = SoilBgDark,
    onSecondary = SoilBgDark,
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
    onSurfaceVariant = SoilMutedDark,
    error = ErrorRedDark,
    onError = OnErrorRedDark
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
    onSurfaceVariant = BiophilicMuted,
    error = ErrorRed,
    onError = OnErrorRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep false by default to show our custom gorgeous brand colors consistently!
    typography: androidx.compose.material3.Typography = Typography,
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

    val extendedColors = if (darkTheme) {
        ExtendedColors(
            success = SuccessGreenDark,
            onSuccess = OnSuccessGreenDark,
            warning = WarningAmberDark,
            onWarning = OnWarningAmberDark,
            error = ErrorRedDark,
            onError = OnErrorRedDark,
            premiumGold = PremiumGold
        )
    } else {
        ExtendedColors(
            success = SuccessGreen,
            onSuccess = OnSuccessGreen,
            warning = WarningAmber,
            onWarning = OnWarningAmber,
            error = ErrorRed,
            onError = OnErrorRed,
            premiumGold = PremiumGold
        )
    }

    val spacing = Spacing()

    CompositionLocalProvider(
        LocalSpacing provides spacing,
        LocalExtendedColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = Shapes,
            content = content
        )
    }
}

