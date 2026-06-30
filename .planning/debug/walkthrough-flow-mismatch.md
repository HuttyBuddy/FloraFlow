# Debug Session: Walkthrough Flow Mismatch

## Symptoms
The onboarding walkthrough overlay shows "Finish" (with a check icon) on the Garden Counsel (AI Advisor) tab step. However, clicking it does not end the walkthrough; it instead transitions to the Restoration Journal tab step, where "Finish" is displayed again. Additionally, the bottom tab bar still has 5 tabs instead of the expected 4.

## Root Cause
In [WalkthroughOverlay.kt](file:///f:/floraflow-garden-designer/app/src/main/java/com/example/ui/screens/walkthrough/WalkthroughOverlay.kt#L330-L334), the button text and icon logic explicitly maps both `WalkthroughStep.AI_ADVISOR_TAB` and `WalkthroughStep.AR_LENS_TAB` (which represents the Restoration tab) to "Finish" and `Icons.Default.Check`:
```kotlin
val buttonText = if (currentStep == WalkthroughStep.AI_ADVISOR_TAB || currentStep == WalkthroughStep.AR_LENS_TAB) "Finish" else "Next"
```
This causes the "Finish" button to display prematurely on the Garden Counsel tab.

## Evidence
- In [MainActivity.kt](file:///f:/floraflow-garden-designer/app/src/main/java/com/example/MainActivity.kt#L485-L517), the bottom navigation is defined with 5 tabs, keeping the 5th tab index (`4`) mapped to `WalkthroughStep.AR_LENS_TAB` but swapping its presentation and screen destination to the Restoration Journal.
- Since the 5th tab is kept and mapped to `RestorationJournalScreen`, the walkthrough overlay must progress from AI Advisor (step 4) to Restoration (step 5) before finishing, rather than finishing at AI Advisor.

## Suggested Fix
1. Modify [WalkthroughOverlay.kt](file:///f:/floraflow-garden-designer/app/src/main/java/com/example/ui/screens/walkthrough/WalkthroughOverlay.kt#L330-L334) to remove `currentStep == WalkthroughStep.AI_ADVISOR_TAB` from the "Finish" label and checkmark icon condition.
2. Update the UAT test definition to reflect that the 5th tab is successfully replaced by the Restoration Journal tab (5 tabs in total) and ensure the walkthrough flow runs sequentially through all 5 tabs.
