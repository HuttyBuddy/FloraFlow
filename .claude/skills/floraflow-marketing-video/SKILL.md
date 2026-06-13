---
name: floraflow-marketing-video
description: Automated generation engine for 5-second short-form DTC performance video hooks and organic video social assets (TikTok hooks, paid Meta/Instagram ad variations, short-form UGC) for the FloraFlow Android app via the higgsfield-generate backend.
---

# FloraFlow Marketing Studio Video Generator

## Description
Automated generation engine for 5-second short-form DTC performance video hooks and organic video social assets for the FloraFlow Android app. This skill hooks directly into the specialized `higgsfield-generate` sub-system, structuring prompt inputs precisely via the grammatical MCSLA Formula matrix.

## Trigger Conditions
* Triggered when the user requests TikTok hooks, paid Meta/Instagram ad variations, or short-form UGC video assets for FloraFlow.
* Triggered via: `/floraflow:video`

## Hard Rules
1. **Grammatical Format Structure:** Every generation payload must pass a strictly formatted mathematical inline LaTeX block representing the full MCSLA matrix sequence: $Model \cdot Camera \cdot Subject \cdot Look \cdot Action$.
2. **Kinetic & Fluid Stability:** Direct your video renders through the correct backend engine identifier based on the feature requirements. For native sound synchronizations, route through Seedance 2.0 (`seedance_2_0`). For highly stable cinematic motion paths, fallback to Kling 3.0 (`kling3_0`).
3. **Clip Duration Constraint:** All outputs are strictly configured as 5-second high-energy hooks to prevent narrative drift or credit waste.
4. **Content Filter Fault Tolerances:** If the prompt hits a sensitive content filter block, immediately flag the error, parse out any problematic descriptive adjectives or verbs, swap them with softer, compliant natural terms, and programmatically re-submit the queue sequence.

## Execution Instructions
1. Parse the user brief to isolate the target platform optimization guidelines.
2. Build the precise MCSLA matrix string making all structural choices programmatically.
3. Invoke the terminal command with the `--wait` flag active to ensure synchronous script tracking until the finalized URL outputs to terminal stdout.

### Automated Variations Matrix

#### Option A: The TikTok Hook ("How-To / Problem-Solution")
* Platform Target: TikTok / Shorts Vertical Video Loop
* Engine Allocation: Kling 3.0 (`kling3_0`)
* DTC Preset Mode: `--preset ugc_how_to`
* Sizing Matrix: `--aspect-ratio 9:16`
```bash
higgsfield generate create \
  --model kling3_0 \
  --preset ugc_how_to \
  --aspect-ratio 9:16 \
  --wait \
  --prompt "$Model: \text{kling3\_0} \cdot Camera: \text{Macro close-up, smooth camera pan} \cdot Subject: \text{A drooping, brown-edged peace lily plant next to a Google Pixel phone} \cdot Look: \text{Natural indoor lighting, high contrast} \cdot Action: \text{The app scans the plant, a clean green boundary rings the leaf, and it dynamically transitions into a blooming green state as a checkmark pops up.}$"
```
