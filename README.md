# FloraFlow

> *Cultivating calm through mindful gardening.*

---

**FloraFlow** is a therapeutic gardening application designed to bring the calming, restorative benefits of nature into the palm of your hand. Whether you are managing a small indoor balcony or sketching out a large backyard oasis, FloraFlow helps you plan your botanical spaces, catalog your plants, track your mood, and receive personalized advice from an AI gardening counselor.

---

## 🌸 Key Features

### 1. Daily Mindfulness Dashboard
*   **Mood Logging:** Record your emotional state (Peaceful, Energized, Refreshed, Stressed, Overwhelmed, Happy) to track how gardening correlates with your mental wellness.
*   **Therapeutic Tasks:** Engage in daily mindful gardening challenges curated to relieve stress and foster presence.
*   **Community Board:** View active community highlights and discussions centered around wellness and sustainable planting.

### 2. 2D Interactive Garden Planner
*   **Grid-Based Layouts:** Draft layouts on a customizable 5x5 grid.
*   **Design Styles:** Tailor your gardens to distinct themes, such as Zen, Cottage, Desert, Urban Balcony, or Vegetable.
*   **Plant Layouts:** Place, arrange, and manage individual plant types directly on the digital plot.

### 3. Greenhouse Botanical Database
*   **Botanical Specifications:** Access a rich database detailing requirements for soil type, sunlight needs, mature sizes, bloom times, and pest/disease management.
*   **Seasonal Care Checklists:** Follow step-by-step care lists tailored to Spring, Summer, Autumn, and Winter.

### 4. Gemini-Powered AI Advisor
*   **Intelligent Q&A:** Chat with an AI assistant powered by the Gemini API via Google AI Studio.
*   **Personalized Consultations:** Get instant recommendations, layout design ideas, plant care tips, and diagnostic help for ailing plants.

### 5. AR Simulator Lens (Premium 👑)
*   **Augmented Reality Previews:** Utilize your device's camera via `ARSceneview` to simulate plants in physical spaces.
*   **Atmospheric Climate Filters:** Premium subscribers can activate immersive overlays, such as "Cherry Blossoms" and "Fireflies," to cultivate a relaxing ambiance.

### 6. Dynamic Walkthrough Tour
*   **Guided Onboarding:** Step-by-step interactive overlay highlights core features, guiding new users through the Planner, Advisor, and AR Lens.

---

## 🛠️ Tech Stack & Architecture

FloraFlow is built as a native Android application using modern, performance-oriented tooling:

*   **Language:** [Kotlin 2.3.10](https://kotlinlang.org/) with [Kotlin Symbol Processing (KSP) 2.3.6](https://kotlinlang.org/docs/ksp-overview.html)
*   **UI Toolkit:** [Jetpack Compose](https://developer.android.com/compose) with [Material Design 3](https://m3.material.io/)
*   **Local Storage:** [Room Database (2.7.0)](https://developer.android.com/training/data-storage/room) for offline support, local garden layouts, plant progress, and wellness logs.
*   **Network & Serialization:** [Retrofit (2.12.0)](https://square.github.io/retrofit/) and [Moshi (1.15.2)](https://github.com/square/moshi) for communicating with the Gemini AI services.
*   **AR Engine:** [ARSceneview (4.16.8)](https://github.com/SceneView/arsceneview) for high-performance 3D and augmented reality rendering.
*   **Testing:** [Robolectric (4.16.1)](http://robolectric.org/) and [Roborazzi (1.59.0)](https://github.com/takahirom/roborazzi) for screenshot validation and automated UI test assertions.

### Project Directory Layout

```
floraflow-garden-designer/
├── app/
│   ├── src/main/java/com/example/
│   │   ├── MainActivity.kt               # Entrypoint & main tab scaffolding
│   │   ├── data/                         # Data Layer
│   │   │   ├── api/                      # GeminiApiClient
│   │   │   ├── database/                 # Room database configuration & DAOs
│   │   │   ├── model/                    # Data entities & database tables
│   │   │   └── repository/               # Repositories mediating local/remote access
│   │   └── ui/                           # UI Layer
│   │       ├── screens/                  # Compose Screens (Dashboard, Planner, Greenhouse, etc.)
│   │       ├── theme/                    # Material 3 Color Schemes & Typography
│   │       └── viewmodel/                # GardenViewModel managing application state
│   └── build.gradle.kts                  # Module dependencies and SDK configurations
├── gradle/
│   └── libs.versions.toml                # Centralized dependency catalog
└── build.gradle.kts                      # Root Gradle configuration
```

---

## 🚀 Getting Started

### Prerequisites
*   [Android Studio](https://developer.android.com/studio) (Koala or newer recommended)
*   Android SDK 35 (Target & Compile SDK)
*   JDK 17 or higher

### Installation & Local Setup

1.  **Clone the Repository** and open it in Android Studio.
2.  **Allow Gradle to Sync:** Allow the IDE to resolve all dependencies from the `libs.versions.toml` catalog.
3.  **Configure Environment Variables:**
    *   Create a file named `.env` in the root project directory.
    *   Add your Gemini API Key using the key name `GEMINI_API_KEY`:
        ```env
        GEMINI_API_KEY=your_actual_gemini_api_key_here
        ```
    *   *Note:* The project utilizes the **Secrets Gradle Plugin** to securely bind variables in `.env` into build configurations without exposing keys.
4.  **Run the Application:** Build and deploy to an Android Emulator or a physical device running API Level 24 (Android 7.0) or higher.

---

## 🧪 Testing

To run the suite of automated tests (including Robolectric unit tests and Roborazzi screenshot verification):

```bash
# Run unit and screenshot tests
./gradlew testDebugUnitTest
```

A Play Store Crawler verification script is configured inside `Robo Test Runs/` for testing the main UI navigation, billing dialog, and library onboarding flows automatically.

---

## 📄 License & Terms
Terms of Service, Privacy Policies, and open-source licenses are accessible directly in the application under **Settings** ➡️ **Legal Information**.
