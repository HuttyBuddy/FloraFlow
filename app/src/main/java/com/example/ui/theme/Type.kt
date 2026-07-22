package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val PlayfairDisplayFont = GoogleFont("Playfair Display")
val PlusJakartaSansFont = GoogleFont("Plus Jakarta Sans")

val PlayfairDisplayFontFamily = FontFamily(
    Font(googleFont = PlayfairDisplayFont, fontProvider = provider)
)

val PlusJakartaSansFontFamily = FontFamily(
    Font(googleFont = PlusJakartaSansFont, fontProvider = provider)
)

val Typography = Typography(
    // Display styles (Playfair Display)
    displayLarge = TextStyle(
        fontFamily = PlayfairDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 60.sp // Slightly tighter for impact
    ),
    displayMedium = TextStyle(
        fontFamily = PlayfairDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp
    ),
    displaySmall = TextStyle(
        fontFamily = PlayfairDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp
    ),
    
    // Headline styles (Playfair Display)
    headlineLarge = TextStyle(
        fontFamily = PlayfairDisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 38.sp // More relaxed
    ),
    headlineMedium = TextStyle(
        fontFamily = PlayfairDisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = PlayfairDisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    
    // Title styles (Playfair Display for large/medium, Plus Jakarta Sans for small)
    titleLarge = TextStyle(
        fontFamily = PlayfairDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PlayfairDisplayFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    
    // Body styles (Plus Jakarta Sans)
    bodyLarge = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.2.sp // Slightly wider for readability
    ),
    bodyMedium = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.3.sp
    ),
    bodySmall = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp, // Slightly taller for small text
        letterSpacing = 0.4.sp
    ),
    
    // Label styles (Plus Jakarta Sans)
    labelLarge = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

// The supervised validation APK is fully offline and has a strict first-frame budget.
// System families preserve its editorial serif/sans hierarchy without starting the
// downloadable-font provider before the promise or saved-corner screen is readable.
val RestorativeValidationTypography = Typography.copy(
    displayLarge = Typography.displayLarge.copy(fontFamily = FontFamily.Serif),
    displayMedium = Typography.displayMedium.copy(fontFamily = FontFamily.Serif),
    displaySmall = Typography.displaySmall.copy(fontFamily = FontFamily.Serif),
    headlineLarge = Typography.headlineLarge.copy(fontFamily = FontFamily.Serif),
    headlineMedium = Typography.headlineMedium.copy(fontFamily = FontFamily.Serif),
    headlineSmall = Typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
    titleLarge = Typography.titleLarge.copy(fontFamily = FontFamily.Serif),
    titleMedium = Typography.titleMedium.copy(fontFamily = FontFamily.Serif),
    titleSmall = Typography.titleSmall.copy(fontFamily = FontFamily.SansSerif),
    bodyLarge = Typography.bodyLarge.copy(fontFamily = FontFamily.SansSerif),
    bodyMedium = Typography.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
    bodySmall = Typography.bodySmall.copy(fontFamily = FontFamily.SansSerif),
    labelLarge = Typography.labelLarge.copy(fontFamily = FontFamily.SansSerif),
    labelMedium = Typography.labelMedium.copy(fontFamily = FontFamily.SansSerif),
    labelSmall = Typography.labelSmall.copy(fontFamily = FontFamily.SansSerif),
)
