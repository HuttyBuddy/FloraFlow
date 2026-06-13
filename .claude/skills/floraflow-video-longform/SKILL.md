---
name: floraflow-video-longform
description: Long-form video synthesizer for FloraFlow YouTube explainers — chains consecutive 15-second seedance_2_0/kling3_0 generations with a persistent Soul ID persona and Popcorn storyboard keyframes to bypass single-clip duration caps while preventing character/clothing drift.
---

# FloraFlow Long-Form Video Synthesizer (YouTube Explainer)

## Description
Bypasses standard generative video clip constraints (which cap out at 15 seconds) by programmatically leveraging multi-image continuity. Produces extended YouTube explainer/walkthrough videos for the FloraFlow Android app by chaining consecutive Higgsfield generations into a single coherent sequence.

## Trigger Conditions
* Triggered when the user requests long-form explainer, walkthrough, or tutorial-style videos for FloraFlow that exceed a single 15-second clip.
* Triggered via: `/floraflow:video-longform`

## Hard Rules
1. **Persona Initialization (Once):** Call `higgsfield-soul-id` exactly once at the start of the run to initialize a static persona. Reuse the returned persona/character ID across every chained segment — never re-initialize mid-chain.
2. **Script/Audio Parsing:** Parse the driving audio track or script via Speak 2.0, or via an imported ElevenLabs audio file, before generating any video segments. Segment the audio/script into 15-second chunks to drive each generation step.
3. **Keyframe Continuity (Popcorn Storyboard):** Each chained 15-second generation (`seedance_2_0` or `kling3_0`) must reference the expanding sequence of keyframe images from all prior segments via the Popcorn storyboard layout. This is non-negotiable — it is the mechanism that prevents character and clothing drift across segments.
4. **Engine Allocation:** Use `seedance_2_0` for segments requiring native audio/sound synchronization with the parsed track. Use `kling3_0` for segments prioritizing stable cinematic motion paths.
5. **Formatting Output:** Print only the final stitched asset URL(s) as a clean Markdown bulleted list. Do not surface persona IDs, intermediate segment URLs, or compilation logs to the user unless explicitly asked.

## Execution Instructions
1. Parse the user's script/audio source and split it into sequential ~15-second segments.
2. Call `higgsfield-soul-id` to create the persistent persona for the run.
3. For each segment in order, invoke `higgsfield generate create`, passing the persona ID and the accumulated keyframe references (Popcorn layout) from all previously generated segments.
4. After the final segment completes, stitch/return the final asset URL(s).

### Example: Initializing the persona
```bash
higgsfield-soul-id create \
  --name "floraflow_presenter" \
  --reference-image ./assets/presenter_ref.png
```

### Example: Chained segment generation
```bash
higgsfield generate create \
  --model seedance_2_0 \
  --persona-id <persona_id_from_soul_id> \
  --storyboard-layout popcorn \
  --keyframes ./segments/seg01_keyframe.png,./segments/seg02_keyframe.png \
  --duration 15 \
  --wait \
  --prompt "$Model: \text{seedance\_2\_0} \cdot Camera: \text{Steady medium shot, slow push-in} \cdot Subject: \text{The FloraFlow presenter persona standing beside a sunlit plant shelf} \cdot Look: \text{Bright, friendly, brand-consistent greens} \cdot Action: \text{They gesture toward the FloraFlow app on an Android phone, explaining the watering reminder feature as the audio segment plays.}$"
```
