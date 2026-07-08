# FloraFlow

> *Cultivating calm through mindful gardening.*

---

**FloraFlow** is an advanced mobile application for the Android ecosystem that revolutionizes residential and urban landscape planning. By merging immersive eco-acoustic binaural soundscapes and a Neural Restoration Journal with an AI assistant grounded in biophilic design neuroscience, FloraFlow transforms garden planning from static blueprints into an immersive, emotionally grounding creative experience.

FloraFlow makes professional-grade landscape design accessible to everyday home gardeners. Users don't need design training or horticultural expertise — they answer a short Neural Load assessment and FloraFlow helps them envision, plan, and bring a restorative garden to life.

---

## 🌸 Key Features

### 1. Neural Load Assessment & Personalized Onboarding
*   **10-Category Biophilic Assessment:** A guided questionnaire scores the user's space across Nature Views, Living Plants, Natural Light, Acoustic Calm, Natural Materials, Air & Ventilation, Organic Forms, Water Features, Sensory Richness, and Seasonal Awareness.
*   **Neural Load Score & Zone:** Results map to a Green/Yellow/Red zone with a score out of 20, plus a shareable score card.
*   **Personalized Restoration Plan:** The three lowest-scoring categories drive tailored next steps and deep-links into the relevant part of the app (Planner, Greenhouse, Restoration).
*   **30-Day Reassessment:** Users are gently prompted to retake the assessment a month later to track how their space — and their score — has changed.

### 2. Daily Mindfulness Dashboard
*   **Mood Logging:** Record your emotional state (Peaceful, Energized, Refreshed, Stressed, Overwhelmed, Happy) to track how gardening correlates with your mental wellness.
*   **Therapeutic Tasks & Habit Streaks:** Engage in daily mindful gardening challenges, with streaks computed in the device's local time zone.
*   **Weather-Aware Care Scheduling:** Watering, fertilizing, and pruning reminders adjust automatically for rain, heatwaves, and frost, with deduplicated notifications so the same alert never fires twice.

### 3. 2D Interactive Garden Planner
*   **Grid-Based Layouts:** Draft layouts on a customizable 5x5 grid.
*   **Design Styles:** Tailor your gardens to distinct themes, such as Indoor Area, Cottage, Desert, Urban Balcony, or Botanical Sanctuary.
*   **Plant Layouts:** Place, arrange, and manage individual plant types directly on the digital plot, with climate set explicitly at creation and never silently rewritten by the app.

### 4. Greenhouse Botanical Database
*   **Botanical Specifications:** Access a rich database detailing requirements for soil type, sunlight needs, mature sizes, bloom times, and pest/disease management.
*   **Seasonal Care Checklists:** Follow step-by-step care lists tailored to Spring, Summer, Autumn, and Winter.

### 5. Gemini-Powered AI Advisor ("Garden Counsel")
*   **Intelligent Q&A:** Chat with an AI assistant powered by the Gemini API, routed through a companion proxy service (see [`gemini-proxy/`](gemini-proxy/)) so API keys never ship inside the app.
*   **Personalized Consultations:** Get instant recommendations, layout design ideas, plant care tips, and diagnostic help (including photo-based plant diagnosis) for ailing plants.
*   **Free Trial Quota:** Free-tier users get exactly 3 free AI queries in total, persisted client-side and never consumed by a failed request.

### 6. Eco-Acoustic Restoration Journal (Premium 👑)
*   **Neural Restoration Index (NRI):** Automatically analyze your garden's biophilic design, unique plant types, and companion synergies to calculate a stress-relief recovery percentage.
*   **Binaural Soundscapes & Nature Loops:** Play procedural, continuous Alpha (focus), Theta (meditation), and Delta (sleep) brainwave entrainment frequencies overlaid with natural wind chimes via a persistent foreground service.
*   **Weekly Free Trial:** Free-tier users get 3 restoration sessions per week (reset on a rolling 7-day window), so the feature stays discoverable instead of being a one-time trial.

### 7. Premium Subscriptions & Billing
*   **Real Google Play Billing:** Native subscription purchase flow for FloraFlow PRO (Monthly and Annual tiers). Pricing and free-trial length are read live from Play Console — the paywall never advertises a trial that isn't actually configured.
*   **Debug-Only Mock Mode:** A simulated checkout flow exists strictly behind `BuildConfig.DEBUG` for local development and testing; it is fully unreachable in release builds.
*   **Entitlement Integrity:** Premium is only granted or revoked based on Play's authoritative purchase state — an in-flight or pending purchase can never strip an existing subscriber's access.
*   **Subscription Management:** Users can view their current tier, billing date, and transaction details from Settings; canceling deep-links to Google Play's own subscription management page in production.

### 8. Dynamic Immersive Experience
*   **Guided Exploration:** Seamless interactive overlays highlight core features, guiding new users through the Planner, Advisor, and Restoration Journal.
*   **Splash Screen:** Uses the Android 12+ SplashScreen API for an instant, native cold-start experience.
*   **Home Screen Widget:** A glanceable widget surfaces streak, next care task, weather, and Neural Load status without opening the app.

---

## 🛠️ Tech Stack & Architecture

FloraFlow is built as a native Android application using modern, performance-oriented tooling:

*   **Language:** [Kotlin 2.3.10](https://kotlinlang.org/) with [Kotlin Symbol Processing (KSP) 2.3.6](https://kotlinlang.org/docs/ksp-overview.html)
*   **UI Toolkit:** [Jetpack Compose](https://developer.android.com/compose) with [Material Design 3](https://m3.material.io/)
*   **Local Storage:** [Room Database (2.8.4)](https://developer.android.com/training/data-storage/room) for offline support, local garden layouts, plant progress, and wellness logs. Schema history is exported and version-controlled under `app/schemas/`, with a migration guardrail test (`GardenDatabaseMigrationTest`) — the app intentionally does **not** use destructive migrations, so a future schema change fails a test instead of silently wiping user data.
*   **Network & Serialization:** [Retrofit (3.0.0)](https://square.github.io/retrofit/) and [Moshi (1.15.2)](https://github.com/square/moshi) for communicating with the Gemini AI services.
*   **Billing:** [Google Play Billing Library (7.0.0)](https://developer.android.com/google/play/billing) for real subscription purchases, with a debug-only mock fallback.
*   **Analytics & Crash Reporting:** [Firebase Analytics & Crashlytics](https://firebase.google.com/) for funnel visibility (paywall views, trial starts, purchases, AI usage) and production stability monitoring.
*   **Testing:** [Robolectric (4.16.1)](http://robolectric.org/) and [Roborazzi (1.64.0)](https://github.com/takahirom/roborazzi) for JVM-based unit tests, screenshot validation, and Room migration verification.

### Project Directory Layout

```
floraflow-garden-designer/
├── app/
│   ├── src/main/java/com/example/
│   │   ├── MainActivity.kt               # Entrypoint & main tab scaffolding
│   │   ├── analytics/                    # AnalyticsHelper (Firebase event logging)
│   │   ├── billing/                      # BillingManager (Play Billing + debug mock mode)
│   │   ├── data/                         # Data Layer
│   │   │   ├── api/                      # GeminiApiClient
│   │   │   ├── database/                 # Room database configuration & DAOs
│   │   │   ├── model/                    # Data entities & database tables
│   │   │   └── repository/               # Repositories mediating local/remote access
│   │   └── ui/                           # UI Layer
│   │       ├── components/               # Shared design-system primitives (FloraFlowButton, FloraFlowCard, FloraFlowChip)
│   │       ├── screens/                  # Compose Screens (Dashboard, Planner, Greenhouse, AI Studio, Billing, etc.)
│   │       │   ├── dashboard/components/ # Dashboard-specific composables (habit cards, mood log, charts, celebration dialog)
│   │       │   └── restoration/          # Neural Restoration Journal & SoundscapeService
│   │       ├── theme/                    # Material 3 Color Schemes, Typography, Spacing scale & semantic status colors
│   │       └── viewmodel/                # GardenViewModel managing application state
│   ├── schemas/                          # Exported Room schema history (committed; used by migration tests)
│   └── build.gradle.kts                  # Module dependencies and SDK configurations
├── gemini-proxy/                         # Optional Node.js proxy that keeps the Gemini API key server-side
├── gradle/
│   └── libs.versions.toml                # Centralized dependency catalog
└── build.gradle.kts                      # Root Gradle configuration
```

### Design System

The `ui/theme` and `ui/components` packages form a small internal design system:

*   **Spacing scale:** A `Spacing` token set (`extraSmall`–`extraLarge`, 4dp–32dp) exposed via `MaterialTheme.spacing`, replacing ad hoc `.dp` literals in new screens.
*   **Semantic status colors:** An `ExtendedColors` set (`success`, `warning`, `error` + their `on*` pairs) exposed via `MaterialTheme.extendedColors`, themed for both light and dark mode.
*   **Shared components:** `FloraFlowButton` (Filled/Outlined/Text variants with loading and icon states), `FloraFlowCard`, and `FloraFlowChip` live in `ui/components/` for reuse across screens instead of being reimplemented per screen.
*   **Dark theme parity:** `Theme.kt`'s dark color scheme is built directly from the `Soil*Dark` constants in `Color.kt`, so light/dark tokens stay in sync by construction.

---

## 🚀 Getting Started

### Prerequisites
*   [Android Studio](https://developer.android.com/studio) (Koala or newer recommended)
*   Android SDK 37 (Target & Compile SDK)
*   JDK 17 or higher
*   Node.js 18+ (only if you want to run the optional [`gemini-proxy`](gemini-proxy/) locally)

### Installation & Local Setup

1.  **Clone the Repository** and open it in Android Studio.
2.  **Allow Gradle to Sync:** Allow the IDE to resolve all dependencies from the `libs.versions.toml` catalog.
3.  **Configure Environment Variables:**
    *   Create a file named `.env` in the root project directory (see `.env.example` for the full list).
    *   Add your Gemini API Key:
        ```env
        GEMINI_API_KEY=your_actual_gemini_api_key_here
        ```
    *   *Optional — recommended before any public release:* point the app at the [`gemini-proxy`](gemini-proxy/) service instead of calling Gemini directly, so the API key never ships inside the APK:
        ```env
        GEMINI_PROXY_URL=your-proxy-host.example.com
        APP_SHARED_SECRET=a-secret-shared-with-the-proxy
        ```
        Leave `GEMINI_PROXY_URL` as `YOUR_PROXY_URL` to call Gemini directly during local development.
    *   *Note:* The project utilizes the **Secrets Gradle Plugin** to securely bind variables in `.env` into build configurations without exposing keys.
4.  **Run the Application:** Build and deploy to an Android Emulator or a physical device running API Level 24 (Android 7.0) or higher.

### Optional: Running the Gemini Proxy Locally

```bash
cd gemini-proxy
npm install
GEMINI_API_KEY=your_key APP_SHARED_SECRET=your_secret npm start
```

The proxy forwards requests to the Gemini API, keeps the real API key server-side, and rejects requests that don't present the matching `X-App-Secret` header. A `Dockerfile` is included for deployment to any container host (e.g. Cloud Run).

---

## 🧪 Testing

To run the suite of automated tests (Robolectric unit tests, Roborazzi screenshot verification, and Room migration checks):

```bash
# Run unit, screenshot, and migration tests
./gradlew testDebugUnitTest
```

Notable test coverage includes `GardenDatabaseMigrationTest`, a guardrail that fails the build if a future `@Database` version bump doesn't ship a matching `Migration` — the app deliberately avoids destructive migrations, so this is the safety net that prevents a schema change from wiping user data in production.

---

## 📄 License & Terms
Terms of Service, Privacy Policies, and open-source licenses are accessible directly in the application under **Settings** ➡️ **Legal Information**.
