---
name: floraflow-ad-batch
description: Multi-variant visual batch engine for FloraFlow A/B paid ad creative — invokes higgsfield product-photoshoot --mode ad_creative_pack to algorithmically generate 3, 5, or 10 lighting/color/framing variants from a single brief without hand-authored per-variant prompts.
---

# FloraFlow Multi-Variant Visual Batch Engine (A/B Paid Ad Matrix)

## Description
Generates programmatic format expansions for paid ad creative testing without human intervention. Maps directly to the native `--mode ad_creative_pack` photoshoot command, passing control parameters back to the Higgsfield prompt enhancer so GPT Image 2 algorithmically varies lighting arrays, color palettes, and framing compositions simultaneously across a batch.

## Trigger Conditions
* Triggered when the user requests a batch of A/B ad creative variants, paid ad matrices, or programmatic format expansions for FloraFlow.
* Triggered via: `/floraflow:ad-batch`

## Hard Rules
1. **Never Hand-Author Per-Variant Prompts:** Do not write separate prompts for each variant. Supply a single base prompt/brief and let the `ad_creative_pack` enhancer generate the lighting, color palette, and framing variations across the batch.
2. **Batch Count Constraint:** The `--count` flag must be one of `3`, `5`, or `10`. Default to `5` if the user does not specify a count.
3. **Single Native Command:** Always invoke `higgsfield product-photoshoot create --mode ad_creative_pack` — do not chain multiple separate `product_shot` calls to simulate a batch.
4. **Reference Image Fidelity:** When a reference layout or placeholder UI is attached, pass it via `--image` and inject the same literal safety constraint used in `floraflow-photoshoot`:
   "The Android phone UI and layout must appear exactly as shown in this reference image—same colors, same elements, do not change anything or hallucinate placeholder text."
5. **Formatting Output:** Print only the finalized asset URLs as a clean Markdown bulleted list. Do not surface internal job IDs, preset strings, or compilation logs to the user.

## Execution Instructions
1. Identify the target platform and ad format (e.g. Meta feed, Instagram story, Pinterest) from the user context.
2. Determine the batch count (3, 5, or 10), defaulting to 5.
3. Verify if a local UI screenshot or device frame exists in the workspace to use as a reference.
4. Construct and run a single `ad_creative_pack` command with the base brief and batch count.

### Example: A/B Ad Creative Batch
```bash
higgsfield product-photoshoot create \
  --mode ad_creative_pack \
  --aspect_ratio 4:5 \
  --count 5 \
  --image ./assets/floraflow_ui_reference.png \
  --prompt "An Android smartphone displaying the FloraFlow app's plant health scan in progress, surrounded by lush houseplants in a bright modern living room. The Android phone UI and layout must appear exactly as shown in this reference image—same colors, same elements, do not change anything or hallucinate placeholder text."
```
