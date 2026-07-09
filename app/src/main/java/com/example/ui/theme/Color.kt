package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

// --- Natural Tones Light Palette ---
val NaturalBg = Color(0xFFFCF9F1)          // Sunlit linen canvas
val NaturalText = Color(0xFF1B1C17)        // Onyx charcoal text
val NaturalMuted = Color(0xFF43493E)       // Darker sage-tinted graphite for clear body copy and subtext
val NaturalSage = Color(0xFF1F483E)        // Crisp deep therapeutic forest green (Primary) - high contrast
val NaturalMutedGreen = Color(0xFF384F45)  // Rich deep pine green (Secondary) - high contrast for icons & secondary details
val NaturalWood = Color(0xFF633B0D)        // Strong deep timber tone (Tertiary) - high contrast accent

val NaturalCardBg = Color(0xFFE2E6D5)      // Elegant organic container green-wash
val NaturalPillActive = Color(0xFFD7DEC6)  // Solid legible interactive background
val NaturalArBg = Color(0xFFEADFCD)        // Warm pine wood preview background
val NaturalBorder = Color(0xFFCBD0BE)      // Well-defined sand border line
val NaturalNavBg = Color(0xFFECEFE0)       // Clay wash bottom nav with high-contrast text support
val NaturalWhite = Color(0xFFFFFFFF)       // Crisp white container elements

// --- Natural Tones Dark Palette (Eye-Safe Night Soil) ---
val SoilBgDark = Color(0xFF141511)         // Dark earth night ground
val SoilTextDark = Color(0xFFE5E2D9)       // Oatmeal cream text
val SoilMutedDark = Color(0xFF8D9280)      // Foggy sage green/gray
val SoilSageDark = Color(0xFFACCFC6)       // Glowing moonlight sage (Primary Dark)
val SoilMutedGreenDark = Color(0xFF54594D) // Dark moss secondary
val SoilWoodDark = Color(0xFFE2C4A2)       // Light natural wicker wood (Tertiary Dark)

val SoilCardBgDark = Color(0xFF23261F)     // Tonal night wood container
val SoilPillActiveDark = Color(0xFF2E3228) // Deep night sprout active pill
val SoilArBgDark = Color(0xFF2A261D)       // Evening campfire shadow
val SoilBorderDark = Color(0xFF2E3228)     // Deep mud border
val SoilNavBgDark = Color(0xFF1A1C16)      // Dark clay navigation
val SoilSurfaceDark = Color(0xFF1B1D17)    // Dark sand surface

// --- Premium Redesign Gradients & Brushes ---
val PremiumGoldGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFFE5C060), Color(0xFFC59F3F))
)

val PremiumEmeraldGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF1F483E), Color(0xFF384F45))
)

val PremiumDarkEmeraldGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFFACCFC6), Color(0xFF88BDB3))
)

val PremiumGoldBorderBrush = Brush.linearGradient(
    colors = listOf(Color(0xFFFFDF00), Color(0xFFD4AF37), Color(0xFFFFDF00))
)
