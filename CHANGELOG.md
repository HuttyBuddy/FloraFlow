# Changelog

All notable changes to the FloraFlow project will be documented in this file.

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
