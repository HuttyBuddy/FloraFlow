---
name: floraflow-content-pipeline
description: Two-routine content queue protocol for FloraFlow's Higgsfield marketing pipeline — a Sunday Planning routine drafts creative briefs into a Google Sheet tracking schema, and a Monday Execution routine shells out to the Higgsfield CLI per a model-routing matrix, with human-in-the-loop handling for moderation flags and network interruptions.
---

# FloraFlow Cross-Platform Content Pipeline (Two-Routine Database Protocol)

## Description
Systematically fills content queues for long-form channels and multi-tier ad campaigns by decoupling computational reasoning (planning) from rendering (execution), using a Google Sheet as the shared state/tracking layer.

```
 [ Sunday Planning Routine ]
            |
            v  (writes blank-status rows via Apps Script)
   +------------------+
   |   Google Sheet    |<------------------+
   |  Content Queue    |                   |
   +------------------+                   | (logs URLs / job_set_id / status)
            |                              |
            v  (queries QUEUED rows)       |
 [ Monday Execution Routine ] -------------+
            |
            v  (terminal subprocess)
   +------------------+
   |  Higgsfield CLI   |
   +------------------+
```

## Trigger Conditions
* Triggered when the user wants to batch-plan and batch-render a queue of FloraFlow marketing assets across multiple platforms (TikTok, Reels, brand video, static ads).
* Triggered via: `/floraflow:content-pipeline`
* This skill orchestrates `floraflow-photoshoot`, `floraflow-marketing-video`, `floraflow-ad-batch`, `floraflow-storyboard-grid`, and `floraflow-video-longform` — it does not replace them.

## 1. The Sunday Planning Routine (The Brain)
Runs under a high-reasoning pass to generate a creative backlog:

1. Read FloraFlow's brand parameters (tone, palette, app feature focus) and the attention-vector framework in [`advertising-masterclass.md`](./advertising-masterclass.md) (pattern interrupts, curiosity spikes, stat flashes, etc.).
2. Generate a list of creative briefs — one per row — covering the week's content needs across platforms.
3. Write each brief as a new row with `Status = QUEUED` into the **Content Queue** sheet via the Apps Script in [`flora_flow_content_queue.gs`](../../../.planning/scripts/flora_flow_content_queue.gs) (custom menu: "Generate Weekly Content Briefs").
4. Do not generate media in this routine — it only populates the queue.

## 2. The Monday Execution Routine (The Machine)
A low-token terminal loop that drains the queue:

1. Run [`flora_flow_execution_routine.js`](../../../.planning/scripts/flora_flow_execution_routine.js), which queries the Content Queue sheet for rows where `Status = QUEUED`.
2. For each row, map `Platform` + `Objective Constraint` to a CLI invocation using the **Smart Heuristic Router** below.
3. Shell out to the Higgsfield CLI with `--wait`, capture the JSON stdout, and write the resulting asset URL + `Status = DONE` back to the row.
4. If a row fails, apply the **Hard Rules / Exception Handling** below and write the appropriate status.

## Smart Heuristic Router (Funnel & Optimization Matrix)

| Targeted Funnel / Placement | Primary Objective Constraint | Engine | CLI Model Flag | Key Advantage |
|---|---|---|---|---|
| TikTok Hooks & Shorts Loops | Kinetic fluid stability & motion paths | Kling 3.0 | `kling3_0` | Highest stability on fast micro-motions and editing cuts |
| Instagram Reels & UGC Presenters | Audio-video syncing & talking avatars | Seedance 2.0 | `seedance_2_0` | Eliminates downstream voiceover/lip-sync pipelines |
| High-Impact Brand Reels | Absolute environmental physics & draping | Sora 2 | `sora2` | Unmatched fluid simulation and weight permanence |
| Static Ad Overlays & Graphic Assets | Crisp typographic and label text rendering | Nano Banana Pro | `nano_banana_2` | Resolves character blur, handles high-res text natively |

Use this table to pick `--model` / `--mode` for the execution step — do not hand-pick engines outside this matrix without explicit user direction.

## Hard Rules / Exception Handling

### Rule 1 — Reference Drift Barrier (Product Fidelity)
**Symptom:** The engine ingests a text-only prompt and fabricates a generic replacement product instead of FloraFlow's actual UI/hardware asset.
**Fix:** Never rely on generalized phrasing like "use as background reference." Always attach the explicit image file directly via `--image` and include this literal constraint string in the prompt:
```bash
--image ./assets/ui-frame.png --prompt "The app UI and hardware configuration must appear EXACTLY as shown in this reference image. Same dimensions, exact branding text, zero structural variations or hallucinated placeholder blocks."
```

### Rule 2 — Async Interruption Vault (Network Drops)
**Symptom:** A heavy cinematic render (`sora2` / `seedance_2_0`) stalls the local terminal connection mid-stream.
**Fix:** All execution state persists server-side. On a dropped connection, the execution routine's catch-block runs:
```bash
higgsfield generate list --json
```
It parses the payload with `jq` to locate the matching `job_set_id`, maps it back to the corresponding Content Queue row (matched by `Job ID` column), and resumes polling from there — it does not re-submit the job.

### Rule 3 — Moderation Flag (Human-in-the-Loop)
**Symptom:** A generation returns a `failed` or `nsfw` status from the model's safety filter.
**Fix:** Do **not** automatically reword or resubmit the prompt. Instead:
1. Set the row's `Status = NEEDS_REVIEW`.
2. Write the original prompt and the raw error/status string into the `Error / Notes` column.
3. Continue processing the remaining queue rows — one flagged row must never halt the batch.
4. Flagged rows are surfaced to the user for manual review and resubmission (e.g. by editing the prompt and resetting `Status` back to `QUEUED`).

## Content Queue Sheet Schema
| Column | Description |
|---|---|
| `Row ID` | Stable identifier, e.g. `Q-000123` |
| `Platform` | TikTok / Reels / Brand / Static |
| `Objective Constraint` | Free text from the planning routine, used to look up the routing matrix |
| `Engine` | Resolved `--model` value (e.g. `kling3_0`) |
| `Prompt` | Full MCSLA-style prompt or photoshoot prompt |
| `Reference Image` | Path to local reference asset, if any |
| `Status` | `QUEUED` / `IN_PROGRESS` / `DONE` / `NEEDS_REVIEW` |
| `Job ID` | Higgsfield `job_set_id`, for resumption |
| `Asset URL` | Final output URL once `DONE` |
| `Error / Notes` | Populated only when `Status = NEEDS_REVIEW` |

## Setup
See [`flora_flow_execution_routine.js`](../../../.planning/scripts/flora_flow_execution_routine.js) header comments for one-time Google Sheets API service-account setup, and [`flora_flow_content_queue.gs`](../../../.planning/scripts/flora_flow_content_queue.gs) for the Apps Script that creates/maintains the Content Queue sheet.
