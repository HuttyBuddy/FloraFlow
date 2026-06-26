# Changelog

All notable changes to the FloraFlow project will be documented in this file.

## [9.0.1.1] - 2026-06-26

### Added
- **Dashboard Widgets**: Introduced Biophilic Profile, Companion Synergy, and Weather Sync cards for a more data-rich landing experience.
- **Enhanced Testing**: Added comprehensive unit tests for CareScheduler and repository layers.

### Changed
- **UI Refinement**: Polished AiStudio, Garden Planner, and Dashboard layouts for better usability and modern aesthetics.
- **Project Documentation**: Updated README with the latest build instructions and project goals.

### Fixed
- **Startup Stability**: Resolved a critical startup crash by correctly bumping the Room database version following schema changes.
- **Build Infrastructure**: Fixed KSP compilation failures on Windows environments by programmatically configuring the SQLite temporary directory.

## [9.0.1.0] - 2026-06-11

### Added
- **Soundscape Foundation**: Introduced new Soundscape service architectures for enhanced therapeutic audio experiences.

### Changed
- **UI Polishing**: Refined layouts for Onboarding, Dashboard, and Restoration Journal screens for better visual consistency.
- **Merge Sync**: Synchronized with the latest upstream changes including performance optimizations for LazyLists and secure logging.

### Fixed
- **Screen Flow**: Resolved navigation state issues in the onboarding assessment path.

## [9.0.0.0] - 2026-06-11

### Added
- **Modern Build Stack**: Upgraded Gradle to 9.5.1 and migrated all dependencies (Retrofit 3, OkHttp 5, Firebase 34.14, etc.) to the latest stable versions.
- **Android 37 Support**: Updated compile and target SDK to API Level 37 (Android 15+) for long-term compatibility.
- **Project Governance**: Added `AGENTS.md` with skill routing and health stack rules for AI-assisted development.

### Changed
- **UI Architecture**: Migrated deprecated components to modern Jetpack Compose standards (HorizontalDivider, BasicAlertDialog, Icons.AutoMirrored).
- **Performance Optimizations**: Implemented primitive state holders (`mutableIntStateOf`, `mutableFloatStateOf`) across all interactive screens to reduce autoboxing overhead.
- **Code Hygiene**: Resolved 40+ high-priority lint warnings, including observable locale checks and SharedPreferences KTX migration.
- **Internal Tooling**: Integrated automated content pipeline scripts and specialized developer skills.

## [8] - 2026-06-11

### Added
- **Community Hub**: New interactive screen for garden enthusiasts to connect and share tips.
- **Support Center**: Integrated Help screen with FAQs and direct support channels.
- **Engagement Tools**: Added In-App Rating prompt to collect valuable user feedback.
- **AR Plant Rendering**: Completed the AR Lens experience with high-fidelity 3D plant models.

### Changed
- **UI Refinement**: Significant layout improvements to Dashboard, Settings, and the Garden Planner.
- **Compatibility Boost**: Optimized hardware requirements and NDK settings to restore support for 1,125+ older devices.
- **Stability Upgrade**: Added a comprehensive test suite for all major screens and interaction flows.

## [7] - 2026-06-02

### Changed
- **Kotlin Update**: Upgraded to Kotlin 2.3.10 and KSP 2.3.6 for improved build performance and modern language features.
- **Build System**: Optimized Room compiler configuration for better stability on Windows development environments.
- **Version Increment**: Updated version to 7 (7.0) for Play Console Closed testing.

## [6] - 2026-05-30

### Changed
- **SDK Compatibility**: Downgraded target SDK and compile SDK to 35 for broader stability on the current Google Play Store track.
- **Version Increment**: Updated version to 6 (6.0) for new Play Store submission.

## [5] - 2026-05-29

### Added
- **Garden Library**: Botanical database with detailed requirements for pH, sunlight, and companion planting.
- **Billing System**: Implementation of billing screens for premium feature access.
- **Privacy Policy**: Added comprehensive privacy policy and updated support email.

### Fixed
- **Signing Config**: Corrected release signing configuration for Play Store submission.
- **Project Structure**: Cleaned up unused dependencies and optimized build configuration.

### Enhanced
- **AR Simulator**: Added interactive climate filters ("Cherry Blossoms" and "Fireflies") to the AR lens.
- **UI/UX**: Refined the library and billing screen layouts.

### Testing
- **Robo Test**: Successfully passed automated crawler testing for core navigation.
- **Multi-Device Validation**: Verified stability across 4 diverse device profiles (S21 Ultra, Tablet, Pixel 8/8 Pro) on API levels 30-36.
- **Robo Script**: Added `MainActivity_robo_script.json` to guide future crawlers through onboarding, billing, and the botanical encyclopedia.
