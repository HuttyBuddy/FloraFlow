# FloraFlow: Indoor Sanctuaries

> *Breathe life into every corner of your indoor space.*

---

**FloraFlow** is an advanced mobile application for the Android ecosystem that revolutionizes indoor biophilic sanctuary design and houseplant care. By merging immersive eco-acoustic binaural soundscapes, hardware-accelerated 15-second Reels video generation, Plant Parent Personality Archetypes, and a shared Co-Care Duet AppWidget with an AI assistant grounded in biophilic design neuroscience, FloraFlow transforms indoor room styling from static blueprints into an immersive, emotionally grounding creative experience.

FloraFlow makes professional biophilic indoor styling accessible to everyday plant lovers. Users answer a short Neural Load assessment and FloraFlow helps them envision, plan, and bring a restorative indoor plant sanctuary to life.

---

## 🌸 Key Features

### 1. 100% Indoor Biophilic Profile & Plant Parent Archetype System
*   **10-Category Biophilic Assessment:** Scores the user's room space across Nature Views, Living Plants, Window Light, Acoustic Calm, Natural Materials, Air & Ventilation, Organic Forms, Water Features, Sensory Richness, and Seasonal Awareness.
*   **Plant Parent Personality Archetypes:** Automatically calculates 5 shareable personality archetypes from room lighting, plant count, and assessment results:
    *   🌿 **Jungle Maximalist** *(High foliage density & natural daylight)*
    *   🌵 **Cactus Survivor** *(Low-maintenance, resilient species)*
    *   💦 **Serial Overwaterer** *(High daily care frequency & soil moisture tracking)*
    *   ⚡ **Cyberpunk Botanist** *(Artificial LED light + binaural focus)*
    *   ✨ **Sanctuary Master** *(Top-tier biophilic score 85%+)*
*   **Neural Load Score & Zone:** Results map to a Green/Yellow/Red zone with a score out of 20, plus a shareable score card.
*   **Personalized Restoration Plan:** The three lowest-scoring categories drive tailored next steps and deep-links into the relevant part of the app (Planner, Greenhouse, Restoration).

### 2. 🎬 15-Second Ambient Reels Video Exporter
*   **Hardware-Accelerated Video Encoding:** Uses native Android `MediaCodec` (H.264) + `MediaMuxer` to generate a 1080x1920 30fps MP4 video clip in ~1.5 seconds.
*   **Social-Ready Templates:** Merges pulsing soundwave visualizer graphics, biophilic scores, and binaural audio tracks into an `.mp4` clip built for TikTok, Instagram Reels, and Snapchat sound templates.

### 3. 🪴 Co-Care Duet Home Screen AppWidget & Shared Hub
*   **Native AppWidget:** An Android home screen widget powered by `AppWidgetProvider` displaying the shared blooming plant node, care streak counter, and partner status.
*   **Real-Time Co-Care Hub:** Synchronizes plant care routines between partners or roommates, allowing plants to bloom in real time upon watering or mindfulness sessions.

### 4. 📸 AI Room Vibe Check Screen
*   **Biophilic Environment Diagnostics:** Camera-assisted diagnostic screen assessing indoor window light levels, room greenery density, and air flow balance.

### 5. Daily Mindfulness & Indoor Botanical Rhythm Dashboard
*   **Mood Logging:** Record emotional states (Peaceful, Energized, Refreshed, Stressed, Overwhelmed, Happy) to track how tending indoor plants correlates with mental wellness.
*   **Therapeutic Tasks & Leaf Misting Habits:** Engage in daily mindful houseplant care challenges, including Leaf Misting (`mistCompleted`) and habit streaks computed in local time.
*   **Indoor Climate Weather Sync:** Window placement, temperature shielding, and humidity misting recommendations adjust automatically for heatwaves, rain, and winter frost drafts.

### 6. 2D Interactive Indoor Sanctuary Planner
*   **Grid-Based Layouts:** Draft indoor sanctuary layouts on a customizable 5x5 room grid.
*   **Indoor Light Filtering:** Filter houseplant seeds by indoor daylight levels (*All Indoor*, *Bright Light*, *Low Light*).
*   **Indoor Substrates & Themes:** Custom indoor soil mixes (Aroid blend, Peat perlite, Succulent sand, Clay pebble) and room themes (Indoor Area, Botanical Corner, Zen Sanctuary).

### 7. Greenhouse Houseplant Database
*   **Indoor Botanical Specifications:** Access a rich catalog of 100% indoor houseplants (*Monstera Deliciosa*, *Snake Plant*, *Peace Lily*, *ZZ Plant*, *Pothos*, *Fiddle Leaf Fig*, *Calathea*, *Rubber Tree*, *Anthurium*, *Parlor Palm*, *Bonsai Ficus*, *Peperomia*, *Hoya*, *Spider Plant*).
*   **Seasonal Houseplant Care Checklists:** Follow step-by-step care lists tailored to seasonal indoor window shifts and room humidity changes.

### 8. Gemini-Powered AI Advisor ("Garden Counsel")
*   **Intelligent Indoor Q&A:** Chat with an AI assistant powered by the Gemini API, routed through a companion proxy service (see [`gemini-proxy/`](gemini-proxy/)) so API keys never ship inside the app.
*   **Biophilic Consultations:** Get instant recommendations for indoor plant selection, room lighting placement, and diagnostic help for yellowing indoor leaves.
*   **Free Trial Quota:** Free-tier users get 3 free AI queries in total, persisted client-side and never consumed by a failed request.

### 9. Eco-Acoustic Restoration Journal (Premium 👑)
*   **Neural Restoration Index (NRI):** Automatically analyze your indoor space's biophilic design, plant variety, and room lighting to calculate a stress-relief recovery percentage.
*   **Binaural Soundscapes:** Play procedural, continuous Alpha (focus 10Hz), Theta (meditation 6Hz), and Gamma (focus 40Hz) brainwave entrainment frequencies overlaid with natural wind chimes via a persistent foreground service.
*   **Weekly Free Trial:** Free-tier users get 3 restoration sessions per week (reset on a rolling 7-day window).

### 10. Premium Subscriptions & Billing
*   **Real Google Play Billing:** Native subscription purchase flow for FloraFlow PRO (Monthly and Annual tiers) via Google Play Billing Library 9.0.0.
*   **Subscription Management:** Users can view tier details, renewal dates, and manage subscriptions directly.

---

## 🛠️ Tech Stack & Architecture

FloraFlow is built as a native Android application using modern, performance-oriented tooling:

*   **Language:** [Kotlin 2.3.10](https://kotlinlang.org/) with [Kotlin Symbol Processing (KSP) 2.3.6](https://kotlinlang.org/docs/ksp-overview.html)
*   **UI Toolkit:** [Jetpack Compose](https://developer.android.com/compose) with [Material Design 3](https://m3.material.io/)
*   **Video Generation:** Native `MediaCodec` (H.264) + `MediaMuxer` 1080x1920 MP4 hardware encoding engine
*   **Local Storage:** [Room Database (2.8.4)](https://developer.android.com/training/data-storage/room) for offline support, room layouts, plant progress, and wellness logs with migration guardrail tests (`GardenDatabaseMigrationTest`).
*   **Network & Serialization:** [Retrofit (3.0.0)](https://square.github.io/retrofit/) and [Moshi (1.15.2)](https://github.com/square/moshi) communicating with Gemini AI proxy services.
*   **Billing:** [Google Play Billing Library (9.0.0)](https://developer.android.com/google/play/billing) for real subscription purchases.
*   **Analytics & Crash Reporting:** [Firebase Analytics & Crashlytics](https://firebase.google.com/).
*   **Testing:** [Robolectric (4.16.1)](http://robolectric.org/) and [Roborazzi (1.64.0)](https://github.com/takahirom/roborazzi) for JVM-based unit tests and screenshot validation.

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
    *   Create a file named `.env` in the root project directory (see `.env.example`).
    *   Add your Gemini API Key:
        ```env
        GEMINI_API_KEY=your_actual_gemini_api_key_here
        ```
4.  **Run the Application:** Build and deploy to an Android Emulator or physical device running API Level 24 (Android 7.0) or higher.

---

## 📄 License & Terms
Terms of Service, Privacy Policies, and open-source licenses are accessible directly in the application under **Settings** ➡️ **Legal Information**.
