# Implementation Plan - Fix Weather Location Change

Fix the "Edit Location" functionality in the weather widget by ensuring the Zip Code dialog is available and functional in both the `DashboardScreen` and `SanctuaryCardDeckScreen`.

## User Review Required

> [!NOTE]
> I am extracting the inline dialog logic from `DashboardScreen.kt` into a standalone `ZipCodeDialog.kt` to allow reuse in the Card Deck view. This will ensure consistent behavior across both dashboard layouts.

## Proposed Changes

### Dashboard Components

#### [NEW] [ZipCodeDialog.kt](file:///F:/floraflow-garden-designer/app/src/main/java/com/example/ui/screens/dashboard/ZipCodeDialog.kt)
- Create a reusable `ZipCodeDialog` composable.
- Parameters: `initialZip`, `onDismiss`, `onUpdate`.
- Style: Consistent with the app's biophilic design (rounded corners, primary colors).

### Dashboard Screens

#### [MODIFY] [DashboardScreen.kt](file:///F:/floraflow-garden-designer/app/src/main/java/com/example/ui/screens/dashboard/DashboardScreen.kt)
- Remove the inline `// --- DIALOG: Change Zip Code ---` code.
- Replace with a call to the new `ZipCodeDialog`.

#### [MODIFY] [SanctuaryCardDeckScreen.kt](file:///F:/floraflow-garden-designer/app/src/main/java/com/example/ui/screens/dashboard/SanctuaryCardDeckScreen.kt)
- Add `var showZipDialog by remember { mutableStateOf(false) }` state.
- Update `WeatherSyncCard`'s `onWeatherClick` lambda to set `showZipDialog = true`.
- Render `ZipCodeDialog` when `showZipDialog` is true.

## Verification Plan

### Manual Verification
- **Device Test**:
    1. Open the **Sanctuary Card Deck** screen.
    2. Tap **Edit Location** on the Weather card.
    3. Enter a valid US Zip Code (e.g., `90210`).
    4. Tap **Update** and confirm the weather card refreshes with "Beverly Hills, CA" (or corresponding city).
    5. Switch to the main **Dashboard** and verify the same functionality works there.
