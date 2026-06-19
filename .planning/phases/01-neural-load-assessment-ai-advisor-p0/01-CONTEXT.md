# Phase 1: Neural Load Assessment & AI Advisor (P0) - Context

**Gathered:** 2026-06-17
**Status:** Ready for planning
**Source:** User Redesign Spec (PRD)

<domain>
## Phase Boundary

This phase delivers the core Neural Load Assessment onboarding flow, the score result screen, personalized next steps, the biophilic mode of the AI Advisor, and disables/hides temporary AR animated icons.

</domain>

<decisions>
## Implementation Decisions

### Welcome Screen (Splash)
- Centered top: FloraFlow logo
- Headline: "How much stress is your space creating?" (warm cream text)
- Subtext: "Take a 2-minute Neural Load assessment — find out if your environment is helping or hurting your nervous system."
- CTA: "Start My Assessment" (sandy warm background #E8C998)
- Secondary button: "Skip for now →" (links to Home, shows persistent gentle banner: "You haven't taken your Neural Load assessment yet.")
- Background color: Dark green (#1D3C28)

### Assessment Questions Screen
- Top progress bar: 10 segments, fills as user advances
- Question counter: "Question X of 10"
- Touch targets: Sage green badge category (e.g. NATURE VIEWS). Touch cards for options (not radios):
  - "Never or rarely → 0"
  - "Sometimes → 1"
  - "Always or almost always → 2"
- Swipe back allowed. Auto-saves progress. Tap option immediately advances to next question.
- Questions should match the 10 categories from the checklist.

### Calculating Screen
- Gentle leaf pulsing animation (2-3 seconds max, no spinner, no fake loading bar).
- Text cycles: "Analyzing your environment..." → "Calculating Neural Load..." → "Generating your results..."
- Background color: Dark green (#1D3C28)

### Score Result Screen
- Large score number: e.g. "9 / 20" (48pt+ bold)
- Zone label in pill badge:
  - Green Zone: Green background
  - Yellow Zone: Amber background
  - Red Zone: Red background
- Summary: "Your environment has meaningful biophilic gaps that are quietly costing you focus, mood, and resilience. The good news: the highest-impact fixes are specific and achievable."
- Primary CTA: "See My 3 Next Steps"
- Secondary CTA: "Share My Score" (generates shareable image card with logo and "Take your own assessment: floraflow.app")

### Next Steps Screen
- Personalized 3 next steps based on zone and lowest-scoring questions (e.g., Living Plants, Natural Materials, Natural Light).
- Tappable cards with numbered badges that expand to show details and a CTA that routes to existing features:
  - AI Advisor (e.g. "Find plants for my space")
  - Garden Planner (e.g. "Design my layout")
  - Plant Database (e.g. "Browse material ideas")

### AI Advisor (Biophilic Mode)
- Update default system prompt of the AI Advisor (`AiStudioScreen`) to act as the "Biophilic Design Advisor".
- System prompt must reference the user's Neural Load score/zone and lowest-scoring categories, and explain the biological rationale behind recommendations.
- Keep the care assistant mode as a secondary option.

### AR Icons
- Remove or hide AR animated icons in the bottom navigation/tabs since they are temporary/not premium.

### Existing Code Reference
- UI screens are in `com.example.ui.screens`.
- Main navigation is in `MainActivity.kt`.
- ViewModel is `GardenViewModel.kt`.

</decisions>

<canonical_refs>
## Canonical References

### MainActivity
- [MainActivity.kt](file:///f:/floraflow-garden-designer/app/src/main/java/com/example/MainActivity.kt)

### Screens
- [OnboardingScreen.kt](file:///f:/floraflow-garden-designer/app/src/main/java/com/example/ui/screens/OnboardingScreen.kt)
- [AiStudioScreen.kt](file:///f:/floraflow-garden-designer/app/src/main/java/com/example/ui/screens/AiStudioScreen.kt)
- [ArLensScreen.kt](file:///f:/floraflow-garden-designer/app/src/main/java/com/example/ui/screens/arlens/ArLensScreen.kt)
- [DashboardScreen.kt](file:///f:/floraflow-garden-designer/app/src/main/java/com/example/ui/screens/dashboard/DashboardScreen.kt)

</canonical_refs>

<specifics>
## Specific Ideas

### Neural Load Score Zone color mapping
- Green zone: bg = green-900, text = green-300, border = green-800, badge = green-800 text-green-200
- Yellow/Amber zone: bg = amber-900, text = amber-300, border = amber-800, badge = amber-800 text-amber-200
- Red zone: bg = red-900, text = red-300, border = red-800, badge = red-800 text-red-200

</specifics>

---
*Phase: 01-neural-load-assessment-ai-advisor-p0*
*Context gathered: 2026-06-17 via PRD Express Path*
