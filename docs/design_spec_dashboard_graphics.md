# FloraFlow Dashboard & Sanctuary Card Graphic Enhancements Specification

## 1. Understanding Summary
* **Goal**: Transform basic-looking Dashboard & Sanctuary Cards (`BiophilicProfileCard`, `SanctuaryCardDeckScreen`, `DailyHabitCard`, `WeatherSyncCard`) into high-impact, biophilic visual components.
* **Aesthetic**: Illustration-heavy design using dynamic AGSL procedural shaders, hardware-accelerated Compose `Canvas` leaf/vine vector corner accents, and dynamic seasonal status badges.
* **Target Platforms**: Android (Jetpack Compose). AGSL RenderEffect shaders on API 31+ with smooth Compose Canvas fallback gradients on API < 31.
* **Performance Budget**: Target 60/120 FPS frame rate. Off-screen animation auto-pausing via lifecycle listeners to guarantee minimal battery/CPU usage.

---

## 2. Assumptions
1. Retains core design tokens from `DESIGN.md` (`#1F483E` Deep Botanical Forest, `#FAF8F5` Sandstone Canvas, `#D97724` Warm Terracotta, `#43493E` Botanical Sage).
2. Uses `Playfair Display` serif typography for card titles and `Plus Jakarta Sans` for labels/metrics.
3. Operates within existing architecture without altering core state management or business logic.

---

## 3. Decision Log

| # | Decision | Alternatives Considered | Rationale |
| :--- | :--- | :--- | :--- |
| **1** | AGSL Procedural Shaders + Compose `Canvas` vector overlays | Lottie JSON assets, static bitmap textures | Zero APK size impact, unlimited screen density scaling, full dynamic theme color mapping |
| **2** | Auto-pausing animation ticks via `DisposableEffect` & `LifecycleObserver` | Continuous frame loops | Prevents background CPU/GPU drain when cards are off-screen or app is minimized |
| **3** | Non-invasive Composable modifiers (`biophilicCard()`) | Rewriting card implementations | Clean separation of concerns, high reusability, zero risk to underlying business logic |

---

## 4. Final Design Specification

### A. New Components (`com.example.ui.components.graphics`)
1. **`BiophilicShaderModifier.kt`**:
   - AGSL runtime shader for ambient sunlight-through-foliage shifting light effect.
   - HSL multi-stop fallback brush for API < 31.
2. **`BotanicalCanvasAccents.kt`**:
   - Procedural vine/leaf vector accents rendered along card borders via `Canvas` drawing paths.
3. **`SeasonalBadgeChip.kt`**:
   - Seasonal indicator chip with gradient border stroke and vector leaf icon.

### B. Target Integrations
- **`BiophilicProfileCard.kt`**: Foliage shader background + top-right leaf border accent.
- **`SanctuaryCardDeckScreen.kt`**: Dynamic seasonal status badges + corner flourishes.
- **`DailyHabitCard.kt`**: Organic line-art border + progress glow.
- **`WeatherSyncCard.kt`**: Real-time atmospheric shader intensity syncing.

### C. Testing & Verification
- Unit test coverage in `EnhancementFeaturesTest.kt` verifying fallback shader paths, lifecycle listeners, and seasonal badge logic.
- `./gradlew testDebugUnitTest` and `./gradlew assembleDebug` verification.
