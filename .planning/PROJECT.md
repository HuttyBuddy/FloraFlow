# FloraFlow

## What This Is

An Android application designed to help users design therapeutic gardens, manage plant care, and reduce their environment's Neural Load. It features a Garden Planner, an AI Advisor, a Botanical Database, and a newly-introduced Neural Load Assessment to bridge the gap between content promise and app experience.

## Core Value

Empower users to understand and reduce their space-induced stress (Neural Load) through personalized biophilic design and plant care advice.

## Requirements

### Validated

- ✓ Garden Planner (2D Layout) — v1.0
- ✓ Botanical Database — v1.0
- ✓ AI Advisor (Plant Care) — v1.0

### Active

- [ ] **ASSESS-01**: 10-question Neural Load Assessment flow in-app.
- [ ] **RESULT-01**: Score result screen with zone color and sharing capabilities.
- [ ] **STEPS-01**: Personalized 3 next steps with deep links to existing features (Planner, Advisor, Database).
- [ ] **ADVISOR-01**: AI Advisor Biophilic Design mode (updated system prompt referencing Neural Load).
- [ ] **AR-01**: Remove or hide AR animated icons until high-quality AR rendering is implemented.
- [ ] **WIDGET-01**: Home screen score widget and step progress tracking.
- [ ] **DIAG-01**: Conversational Space Diagnosis mode (guided consultation).
- [ ] **REASSESS-01**: 30-day reassessment prompt.
- [ ] **HISTORY-01**: Score history and progress tracking over time.

### Out of Scope

- [AR-02] Real AR plant visualization — Deferred to P2 (60-90 days post-launch) to ensure premium quality.
- [PHOTO-01] Photo-based space analysis — Deferred to P2 due to high technical complexity.
- [COMM-01] Community share feed — Deferred to P2 to build user base first.

## Context

The app has existing screens for Onboarding, 2D Planner, Library (Database), AI Advisor (AI Studio), and a basic AR Lens. To drive engagement, the onboarding must lead directly to a Neural Load Assessment rather than dropping the user into an empty planner. The assessment will direct users to existing tools.

## Constraints

- **Tech Stack**: Kotlin, Jetpack Compose, Android SDK.
- **Project ID**: agentic-allies-v1 (never use urban-nest-2).

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Neural Load Assessment Onboarding | Bridge content promise directly to app tools. | — Pending |
| AI Advisor Reframe | Support plant care, biophilic design, and space diagnosis. | — Pending |
| Hide AR Animated Icons | Bad AR hurts premium credibility. | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition:**
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone:**
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-06-17 after initialization*
