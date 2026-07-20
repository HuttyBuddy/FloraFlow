# FloraFlow TODOs

## Product and design

### Create a product-wide DESIGN.md after Gate 2 validation

**What:** Document FloraFlow's typography, colors, spacing, surface hierarchy, component rules, botanical imagery, motion, responsive behavior, and accessibility conventions.

**Why:** The repository has audits, theme tokens, and shared components, but no single binding design-system reference. Later screens can otherwise drift toward dense cards, inconsistent hierarchy, and ad hoc styling.

**Context:** The FloraFlow Validation-First Conversion Sprint defines a layout-led, editorial botanical direction only for its Gate 2 meditation-corner route. Codifying it before user testing could preserve visual decisions that lack evidence.

**Effort:** M
**Priority:** P2
**Depends on:** Complete Gate 2 visual validation and incorporate what participants understand, trust, and find motivating.

### Productize the saved-corner journey after validation

**What:** Integrate the validated saved-corner journey with production Room persistence, analytics, restoration, and care only if the validation gates pass.

**Why:** The ten-person Gate 2 build intentionally uses isolated local JSON persistence so engineering integration does not substitute for demand evidence.

**Context:** The validation route stores one versioned journey record and a separate local event ledger. It does not create `GardenLayout`, `Plant`, or `CareTask` records, call Firebase, or modify `SoundscapeService`. After Gate 2, use the observed completion, commitment, return, and interview evidence to decide which integrations are justified and redesign the production data contract around the validated behavior rather than copying the prototype blindly.

**Effort:** L
**Priority:** P3
**Depends on:** Gate 1 passes and Gate 2 demonstrates meaningful completion, commitment, and unprompted deliberate return.
