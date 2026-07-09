package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

// --- Biophilic Minimalism Light Palette (Product Sheet Aligned) ---
val BiophilicBg = Color(0xFFFDF5E6)          // Pale Cream - Sunlit linen canvas
val BiophilicText = Color(0xFF1B4D3E)        // Deep Forest Green - Primary text
val BiophilicMuted = Color(0xFF43493E)       // Muted Sage - Subtext
val BiophilicPrimary = Color(0xFF1B4D3E)     // Deep Forest Green - Primary Brand
val BiophilicSecondary = Color(0xFFD4AF37)   // Soft Gold/Amber - Accents & Streaks
val BiophilicWood = Color(0xFF633B0D)        // Deep Timber - Tertiary

val BiophilicCardBg = Color(0xFFFFFFFF)      // Pure White - Main container
val BiophilicPillActive = Color(0xFFD7DEC6)  // Solid interactive background
val BiophilicArBg = Color(0xFFF2E8CF)        // Warm wood preview
val BiophilicBorder = Color(0xFFE0E0E0)      // Subtle border
val BiophilicNavBg = Color(0xFFFFFFFF)       // Pure White - Bottom nav
val BiophilicWhite = Color(0xFFFFFFFF)       // Crisp white

val NaturalMuted = Color(0xFF43493E)       // Darker sage-tinted graphite for clear body copy and subtext
val NaturalMutedGreen = Color(0xFF384F45)  // Rich deep pine green (Secondary) - high contrast for icons & secondary details
val NaturalWood = Color(0xFF633B0D)        // Strong deep timber tone (Tertiary) - high contrast accent

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

// --- Brand Gold Color (Unified Premium Accent) ---
val PremiumGold = Color(0xFFD4AF37)

// --- Semantic Status Colors ---
val SuccessGreen = Color(0xFF2E7D32)
val OnSuccessGreen = Color(0xFFFFFFFF)
val WarningAmber = Color(0xFFF57C00)
val OnWarningAmber = Color(0xFFFFFFFF)
val ErrorRed = Color(0xFFD32F2F)
val OnErrorRed = Color(0xFFFFFFFF)

// Dark version
val SuccessGreenDark = Color(0xFF81C784)
val OnSuccessGreenDark = Color(0xFF0C3813)
val WarningAmberDark = Color(0xFFFFB74D)
val OnWarningAmberDark = Color(0xFF4E2600)
val ErrorRedDark = Color(0xFFE57373)
val OnErrorRedDark = Color(0xFF4A0000)

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
