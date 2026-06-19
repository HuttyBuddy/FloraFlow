---
name: floraflow-storyboard-grid
description: 3x3 pre-visualization grid generator for FloraFlow Pinterest pins and storyboards — invokes Higgsfield Shots on a single master reference image to auto-generate a 9-frame contact sheet with varied cinematic camera perspectives while locking face/text fidelity.
---

# FloraFlow 3x3 Pre-Visualization Grid (Pinterest & Storyboards)

## Description
Zero-prompt blueprint generation for visual curation. Invokes Higgsfield Shots on a single master reference image, and the server-side mapping layer automatically structuralizes a perfect 9-frame (3x3) storyboard contact sheet with auto-calculated cinematic camera perspectives, while locking near-perfect detail permanency on text overlays and faces.

## Trigger Conditions
* Triggered when the user requests a 3x3 storyboard, contact sheet, pre-visualization grid, or Pinterest curation board for FloraFlow built from a single reference image.
* Triggered via: `/floraflow:storyboard`

## Hard Rules
1. **Single Reference Image Required:** This skill requires exactly one master reference image, passed via `--image`. Do not proceed without it — ask the user for a reference image if none exists in the workspace.
2. **Zero-Prompt by Default:** Do not author a descriptive scene prompt. Invoke Higgsfield Shots with only the reference image and grid configuration; the server-side mapping layer determines camera perspectives automatically.
3. **Fixed 3x3 Grid Output:** Always enforce a 9-frame (3x3) contact sheet layout (`--grid 3x3`). Do not request other grid sizes.
4. **Detail Permanence:** Apply the same reference-fidelity lock used in `floraflow-photoshoot` — faces and any text overlays present in the reference image must remain consistent and unaltered across all 9 frames.
5. **Formatting Output:** Print only the finalized contact sheet asset URL as a clean Markdown bulleted list. Do not surface internal job IDs, preset strings, or compilation logs to the user.

## Execution Instructions
1. Verify a local master reference image exists in the workspace; if none, ask the user to provide one.
2. Invoke Higgsfield Shots with the reference image and a fixed 3x3 grid configuration.
3. Return the resulting contact sheet asset URL.

### Example: 3x3 Storyboard Grid
```bash
higgsfield shots create \
  --grid 3x3 \
  --image ./assets/floraflow_master_reference.png \
  --lock-faces \
  --lock-text-overlays \
  --wait
```
