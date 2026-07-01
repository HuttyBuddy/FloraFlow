# Design System Audit — FloraFlow Garden Designer (Android/Compose)

## Summary
**Files reviewed:** 24 screen files (128 composables) + 4 theme files | **Issues found:** 7 major | **Score: 42/100**

There is no component library — every screen builds its own buttons, cards, and chips inline. Design tokens exist (`Color.kt`, `Type.kt`, `Shape.kt`) but are bypassed constantly: 401 hardcoded `Color(0x...)` literals, 1,683 raw `.dp` values, 315 raw `.sp` values, and 303 inline `RoundedCornerShape(...)` calls live in screen files instead of referencing the theme.

### Token Coverage
| Category | Defined | Hardcoded Instances Found |
|----------|---------|---------------------------|
| Colors | 24 tokens (`Color.kt`) + Material3 scheme | 401 raw hex colors across screens |
| Corner radius | 5 shape tokens (`Shapes`) | 303 inline `RoundedCornerShape(Ndp)` calls, using 12 different radii not in the scale (2, 3, 6, 14, 18, 28, 32dp) |
| Typography | 15 Material3 type styles (2 font families) | 315 raw `.sp` sizes set directly on `Text()`, bypassing `MaterialTheme.typography` |
| Spacing | No spacing scale defined | 1,683 raw `.dp` values; top values (16, 8, 12, 4, 6, 10dp) suggest an implicit 2/4dp grid that was never formalized as tokens |

### Naming & Structural Consistency
| Issue | Where | Recommendation |
|-------|-------|-----------------|
| Dark theme colors duplicated as raw hex | `Theme.kt` `DarkColorScheme` re-types hex values (`0xFF141511`, `0xFF1B1D17`, etc.) instead of referencing the `Soil*Dark` constants already defined in `Color.kt` | Replace literals in `Theme.kt` with `SoilBgDark`, `SoilSurfaceDark`, etc. Currently the two files can drift out of sync silently. |
| Stale default-template colors | `res/values/colors.xml` still has unused `purple_200/500/700`, `teal_200/700` from the Android Studio starter template | Delete — they contradict the Biophilic Minimalism palette and aren't referenced anywhere in Compose. |
| No shared component layer | 128 composables spread across 24 screen files; no `components/` package. Buttons are built 3 different ways: Material3 `Button`/`OutlinedButton`/`TextButton` (77 uses), `IconButton` (38 uses), and 71 raw `Modifier.clickable` boxes/rows standing in for buttons | Extract a `ui/components/` package with `FloraFlowButton`, `FloraFlowCard`, `FloraFlowChip` etc. so variant/state logic isn't reimplemented per screen. |
| Ad-hoc semantic colors | Status/feedback colors are hardcoded per-screen rather than tokenized, e.g. `AiStudioScreen.kt:1613-1615` maps score ranges to `Color(0xFF4CAF50)/0xFFFFC107/0xFFF44336` (green/amber/red) — a pattern likely needed elsewhere too | Add `success`/`warning`/`error` semantic tokens to the theme (Material3 supports custom extended color schemes). |
| Inconsistent accent green | Multiple distinct greens used ad hoc alongside the theme's `BiophilicPrimary` (`0xFF1B4D3E`): `0xFF386641`/`0xFF6A994E` (AiStudioScreen), `0xFF4CAF50`/`0xFF2E7D32` (BillingDialog), `0xFF2E6F40` (BillingDialog) | Consolidate into a small accent/gradient token set rather than picking a new green per screen. |

### Component Completeness (informal — no formal component API exists)
| Component | States covered | Variants | Docs | Score |
|-----------|----------------|----------|------|-------|
| Buttons | Ad hoc per screen (no shared disabled/loading pattern) | 4 competing implementations (Button/Outlined/Text/clickable-box) | ❌ | 3/10 |
| Cards/Dialogs | Custom per screen (BillingDialog, CommunityDialog, LegalDialog, etc. each reimplement layout/elevation) | Not standardized | ❌ | 4/10 |
| Color tokens | Full light/dark palette defined | ✅ semantic naming (Bg/Text/Muted/Primary/etc.) | ⚠️ inline comments only | 6/10 |
| Typography scale | Full Material3 type scale defined, 2 font families | ✅ | ⚠️ inline comments only | 7/10 |
| Shape scale | 5-step scale defined | ✅ | ⚠️ inline comments only | 6/10 |

### Accessibility Notes
- 170 `Icon()` uses; 37 explicitly set `contentDescription = null` (decorative — fine if intentional), the rest set a description. 146 uses of `Modifier.semantics`/`testTag`/`Role`, which is a reasonable baseline — worth a full `design:accessibility-review` pass if this hasn't been done, since it wasn't checked for contrast/touch-target size here.

### Priority Actions
1. **Build a `ui/components/` package** for buttons, cards, and chips — this is the single highest-leverage fix; it would eliminate most of the 401/303/71 duplication counts above in one move.
2. **Fix the `Theme.kt`/`Color.kt` drift** — point `DarkColorScheme` at the existing `Soil*Dark` constants instead of re-hardcoded hex, and delete the unused starter-template colors in `colors.xml`.
3. **Introduce a spacing scale** (e.g. 4/8/12/16/24/32dp tokens) and a semantic status-color set (success/warning/error), then migrate the highest-frequency raw values (16dp, 8dp, 12dp — 725 combined instances) first for maximum impact.
