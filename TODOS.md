# FloraFlow TODOs

## Product and design

### Create a product-wide DESIGN.md after Gate 2 validation

**What:** Document FloraFlow's typography, colors, spacing, surface hierarchy, component rules, botanical imagery, motion, responsive behavior, and accessibility conventions.

**Why:** The repository has audits, theme tokens, and shared components, but no single binding design-system reference. Later screens can otherwise drift toward dense cards, inconsistent hierarchy, and ad hoc styling.

**Context:** The FloraFlow Validation-First Conversion Sprint defines a layout-led, editorial botanical direction only for its Gate 2 meditation-corner route. Codifying it before user testing could preserve visual decisions that lack evidence.

**Effort:** M
**Priority:** P2
**Depends on:** Complete Gate 2 visual validation and incorporate what participants understand, trust, and find motivating.

### Productize and release the saved-corner journey before acquisition testing

**What:** If the validation gates pass, turn the saved-corner journey into a release-safe production experience, verify that production installers receive the promised route, and only then run the Gate 3 Play Store listing experiment. Integrate Room persistence, analytics, restoration, and care only where the evidence justifies them.

**Why:** The ten-person Gate 2 build intentionally uses isolated local JSON persistence so engineering integration does not substitute for demand evidence. Its screenshots and acquisition promise must not reach Play before the matching journey is available to every installer exposed to the experiment.

**Context:** The validation route stores one versioned journey record and a separate local event ledger in a checksum-verified, sideloaded debug APK. It does not create `GardenLayout`, `Plant`, or `CareTask` records, call Firebase, modify `SoundscapeService`, or ship in release builds. After Gate 2, use the observed completion, commitment, return, and interview evidence to decide which integrations are justified and redesign the production data contract around the validated behavior rather than copying the prototype blindly. Gate 3 begins only after the release-safe journey is implemented, verified, published, and available to production installers; then test one truthful listing asset at a time.

**Effort:** L
**Priority:** P2
**Depends on:** Gate 1 passes and every Gate 2 threshold demonstrates meaningful completion, commitment, physical intent, and unprompted deliberate return.
