---
name: floraflow-photoshoot
description: Custom prompt compiler for generating high-converting, 2K static marketing and organic assets (App Store images, lifestyle imagery, Pinterest pins, website banners, static ads) for the FloraFlow Android app via the higgsfield-product-photoshoot backend.
---

# FloraFlow Product Photoshoot Studio Compiler

## Description
Custom prompt compiler for generating high-converting, 2K static marketing and organic assets for the FloraFlow Android app. This skill utilizes the `higgsfield-product-photoshoot` skill package wrapper running natively under GPT Image 2 to maintain strict design continuity across all variants.

## Trigger Conditions
* Triggered when the user requests App Store images, lifestyle imagery, Pinterest pins, website banners, or static ad assets for FloraFlow.
* Triggered via: `/floraflow:photoshoot`

## Hard Rules
1. **Never Write Raw Diffusion Prompts Manually:** The agent must strictly invoke the `higgsfield-product-photoshoot create` command using the precise `--mode` flag. The backend enhancer owns final prompt generation.
2. **Strict Resolution:** Always enforce 2K resolution metrics natively via the photoshoot backend.
3. **Reference Image Fidelity (Non-Negotiable):** When a reference layout or placeholder UI is attached, you must pass the file path via `--image` and strictly inject the literal safety constraint:
   "The Android phone UI and layout must appear exactly as shown in this reference image—same colors, same elements, do not change anything or hallucinate placeholder text."
4. **Formatting Output:** Print only the finalized asset URLs as a clean Markdown bulleted list. Do not surface internal job IDs, preset strings, or compilation logs to the user.

## Intent-to-Mode Mapping Routing
* App Store / Google Play Feature / Clean Studio Catalog -> Use `--mode product_shot` (Default Ratio `4:5`)
* Social Proof / Instagram / Pinterest Organic Scene -> Use `--mode lifestyle_scene` (Default Ratio `2:3`)
* Website Header / Landing Page Banner -> Use `--mode hero_banner` (Default Ratio `16:9`)

## Execution Instructions
1. Identify the targeted format, look, and layout from the user context.
2. Verify if a local UI screenshot or device frame exists in the workspace to utilize as a reference.
3. Construct and run the precise CLI command mapping to the selected intent matrix.

### Automated Variations Matrix

#### Option A: The App Store Feature
```bash
higgsfield product-photoshoot create \
  --mode product_shot \
  --aspect_ratio 4:5 \
  --count 1 \
  --prompt "An Android smartphone floating neatly on a clean, soft pastel sage-green background. The screen displays the FloraFlow app UI cleanly tracking a vibrant monstera plant. Bright, diffused studio lighting highlighting crisp phone textures and edge ergonomics."
```

#### Option B: The Social Proof Shot (Instagram/Pinterest Organic)
```bash
higgsfield product-photoshoot create \
  --mode lifestyle_scene \
  --aspect_ratio 2:3 \
  --count 1 \
  --prompt "A young woman's hands naturally holding an Android phone inside a bright, sunlit modern apartment kitchen filled with potted plants. The phone screen shows the FloraFlow watering reminder notification cleanly. Soft organic lighting, warm ambient depth, realistic plant leaves casting gentle shadows in the background."
```

#### Option C: The Website Header (High Negative Space)
```bash
higgsfield product-photoshoot create \
  --mode hero_banner \
  --aspect_ratio 16:9 \
  --count 1 \
  --prompt "A wide-format landing page header composition. An elegant Android smartphone angled cleanly on the right third of the frame. The left two-thirds remain clean, minimal, and open with soft volumetric negative space for website copy. The background features clean marble and soft green eucalyptus tones."
```
