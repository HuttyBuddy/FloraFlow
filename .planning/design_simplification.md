# Design Specification: FloraFlow App Simplification (Swipeable Card Deck)

## 1. Executive Summary & Purpose

Simplify FloraFlow by retiring the 5-tab bottom navigation structure (My Plot grid blueprint, Greenhouse species catalog, standalone Restoration audio tab) in favor of a single-screen, highly focused **Swipeable Card Deck Architecture**. The entire app experience centers around **living plants as the biophilic anchor** for indoor/outdoor restorative meditation corners.

---

## 2. Decision Log

| # | Decision | Alternatives Considered | Rationale |
|:--|:---|:---|:---|
| **1** | **UI Architecture**: Single-Screen 3-Card Swipeable Pager (`HorizontalPager`) | 5-Tab Navigation, 2-Tab Navigation | Eliminates cognitive overload and tab switching friction; focuses user immediately on their plant sanctuary. |
| **2** | **Plant Centrality**: Plants promoted on every card (Corner setup, Companion plant recommendations, Daily plant care) | Generic wellness quotes, Abstract room design | Plants are the core differentiator and biophilic value driver for downloads and retention. |
| **3** | **AI Counsel Integration**: Slide-up `ModalBottomSheet` launched from a global Floating Action Button (FAB) | Dedicated chat tab, Inline chat widget | Provides instant AI plant assistance on any card without displacing the user from their main card flow. |
| **4** | **Data Persistence**: Retain existing Room DB schema and `AssessmentResult` history | Full database rewrite / wiping legacy models | Ensures 100% backward compatibility for existing users while simplifying the UI layer. |

---

## 3. Detailed Card Deck UI Design

### Header & Navigation
* **Top App Bar**: Minimalist header containing the FloraFlow logo, active layout title (*"Restorative Meditation Corner"*), and a gear icon for Settings.
* **3-Dot Pager Indicator**: Displays current active card (`Card 1: Sanctuary Corner` | `Card 2: Plant Match` | `Card 3: Daily Tend & Audio`).

---

### Card 1: My Restorative Plant Corner
* **Visual Corner Display**: Shows active living plants in the user's space (*Bonsai Juniper*, *Lavender*, *Peace Lily*) and natural light levels.
* **Score & Zone Badge**: Displays active Neural Load & Sanctuary score (e.g. `16/20 - Green Zone`).
* **Corner Action Button**: `"Retake Assessment"` button that opens the 6-stage Restorative Corner Assessment modal to update space light, dimensions, or plant arrangements.

---

### Card 2: Plant Match & Placement Guide
* **Plant Recommendation Carousel**: Displays 3 curated living plant species tailored to the user's corner assessment (e.g. *Snake Plant for low light*, *Lavender for scent*, *Peace Lily for acoustic calm*).
* **Biophilic Benefit Chips**: Highlights key plant advantages (*Air Purification*, *Stress Relief*, *Aromatic*).
* **1-Tap Action**: `"Add to Corner"` button that directly integrates the plant into the user's active corner layout.

---

### Card 3: Daily Plant Tend & Eco-Acoustics
* **Daily Plant Tend Checklist**: 1-tap interactive card (*"Water Bonsai & Inhale Lavender Scent for 60s"*).
* **Integrated Soundscape Mini-Player**: Built-in play/pause & frequency selector (Alpha, Theta, Delta binaural beats & nature rain audio) to listen to while tending plants.

---

### Global Floating Action Button (FAB)
* Located in bottom-right corner across all 3 cards.
* Tapping opens **Dr. Julian’s AI Plant Counsel** in a smooth slide-up bottom sheet for instant plant care advice, pest troubleshooting, and biophilic guidance.

---

## 4. Implementation & Data Strategy

* **UI Layer**: Create `SanctuaryCardDeckScreen.kt` using Jetpack Compose `HorizontalPager`.
* **State Management**: Consumed via `GardenViewModel` (`activeLayout`, `activePlants`, `assessmentScore`, `isPremium`, `aiChatHistory`).
* **Database**: Read/write from existing `GardenDatabase` DAOs (`GardenDao`, `AssessmentResult`).

---

## 5. Verification Plan

* **Automated Unit Tests**: Compose UI test in `SanctuaryCardDeckScreenTest.kt` verifying pager swipe, FAB modal launch, and card actions.
* **Build Health**: `./gradlew assembleDebug` and `./gradlew testDebugUnitTest`.
