# FloraFlow UI Audit — Pre-Ship (2026-07-03)

> **Re-audit (same day, after fix commit `4da6692`): see [Re-Audit Results](#re-audit-results--2026-07-03-post-fix) at the bottom.
> Verdict: all four P0s verified fixed, code compiles, one leftover demo string found ([BillingDialog.kt:711](app/src/main/java/com/example/ui/screens/BillingDialog.kt)), rest is polish-level.**

Scope: all Compose UI (~19,400 lines across 40 files). Method: full read of design system,
shared components, app shell, and monetization screens; pattern-scan + targeted reads of the
five large screens and dashboard components.

Verdict: **the design system is solid, but the screens routinely bypass it.** 540 hardcoded
color literals and 294 hardcoded font sizes are the root cause of most of what "doesn't look
right." Below, ordered by ship impact.

---

## P0 — Fix before ship (visible bugs)

### 1. Dark mode: theme text on fixed pastel backgrounds (unreadable)
Several components paint a **fixed light pastel background** but put **theme-aware text** on it.
In dark mode the text becomes light sage/oatmeal on pale pink/green — effectively invisible.

- `CompanionSynergyCard.kt:192` / `:226` — fixed `0xFFE8F5E9` / `0xFFFFEBEE` backgrounds with
  `onSurfaceVariant` body text (`:216`, `:249`).
- `DashboardScreen.kt:289-303` — Neural Load zone chips use fixed pastels with no dark variant,
  while lines 90–142 of the *same file* do it correctly with `isDark` branches. Inconsistent.
- `PlannerScreen.kt:1519-1565` — synergy/conflict cards: fixed pastel + fixed dark-green text.
  Readable in dark mode but looks like a light-mode fragment pasted into a dark screen.

**Fix:** these are all semantic success/warning/error surfaces. The theme already provides
`MaterialTheme.extendedColors` (success/warning/error with correct dark variants) — use it.
This one change resolves dozens of instances.

### 2. Bottom nav labels will clip on normal phones
`MainActivity.kt:353-536` — five tabs, labels like **"Garden Counsel"** rendered at 10sp with
`maxLines = 1, softWrap = false` and **no overflow handling**. On a 360dp-wide phone each tab is
~72dp; "Garden Counsel" doesn't fit and gets hard-clipped mid-letter. Also:
- `MainActivity.kt:293` — indicator geometry uses `LocalConfiguration.screenWidthDp`, which
  diverges from actual layout width in landscape, split-screen, and foldables → indicator
  drifts off the selected tab.
- Raw `IntOffset(x = …)` offset breaks in RTL locales.
- `indication = null` on all tabs removes touch feedback entirely.

**Fix:** shorten labels ("Counsel" or "Advisor"), add `overflow = Ellipsis` as a guard, and
measure the bar width via `onGloballyPositioned`/`BoxWithConstraints` instead of configuration.

### 3. Demo/placeholder data shown to paying users
`SubscriptionManagementDialog.kt:163-167` — when real values are null the dialog renders
`"GPA.DEMO-8791-0312"` as the order ID and `"PRO_VERIFIED_SECURE"` as an "Entitlement Key."
A paying subscriber can see a fake order reference. Show "—" / "Syncing with Google Play…"
instead, and drop the invented "Entitlement Key" row entirely.

### 4. Paywall comparison table never renders on phones
`PremiumUpsellScreen.kt:314-375` — `comparisonGrid()` ("Compare Plan Benefits", the strongest
conversion element) is only composed in the `isWideScreen` branch. Portrait phone users — the
overwhelming majority — never see it. Add it to the portrait column (collapsible if it feels long).

---

## P1 — Should fix (quality/accessibility)

### 5. Illegible micro-typography (below Android's 12sp floor)
294 hardcoded `fontSize` overrides fight the type scale; the worst are genuinely illegible:
- **7sp**: `PlannerScreen.kt:1307` ("Eraser")
- **8sp**: `AiStudioScreen.kt:1455-1567`, `LibraryScreen.kt:906`
  ("⚠️ Thirsty"), `PlannerScreen.kt:658`
- **9–10sp**: ~40 more across TherapyChart, CircularBotanicalRhythm, ScoreHistoryChart,
  PremiumUpsellScreen, SubscriptionManagementDialog, nav labels.

**Fix:** floor at 10sp for chart axis labels only; everything user-readable goes to
`labelSmall` (11sp) or `bodySmall` (12sp). Delete redundant overrides where a style already matches.

### 6. Touch targets under 48dp
- `PremiumLockOverlay.kt:66` — the **"Unlock PRO" upsell button is 36dp tall** (your money button).
- `PremiumUpsellScreen.kt:385` — paywall close button 36dp.
- `PlannerScreen.kt:1282-1308` — Clear/Eraser tray card relies on tiny text in a 76dp card.

**Fix:** minimum 48dp interactive height (`Modifier.heightIn(min = 48.dp)` or `minimumInteractiveComponentSize()`).

### 7. Brand accent drift — four different "golds," Tailwind blues, Material greens
- Brand gold is `BiophilicSecondary` `0xFFD4AF37`, but the UI ships `0xFFFFB300`
  (MainActivity crown), `0xFFFFD54F` (PremiumLockOverlay, AiStudio sparkles, celebration),
  `0xFFFFB74D` (gradients) — the premium identity looks different on every screen.
- `PlannerScreen.kt:1319` — seed tray accents are `0xFF0284C7` / `0xFF10B981` (Tailwind sky/emerald),
  visibly foreign to the biophilic palette.
- `0xFF4CAF50`/`0xFF2E7D32` stock Material greens appear ~30× where `primary` or
  `extendedColors.success` should be.

**Fix:** add `PremiumGold` to the theme (one value, light+dark), replace the strays.

### 8. Fixed-height dialog clips at large font scale
`CelebrationDialog.kt:72` — `height(420.dp)` card with center-arranged text. At 1.3–2.0× system
font scale the title/subtitle/buttons overflow and clip. Use `heightIn(min=…)` + scrollable column.
Card is also fixed-light (`0xFFFCF9F1`) — self-consistent, but jarring in dark mode.

### 9. Restoration tab vs. system bars in light theme
`RestorationJournalScreen.kt` is an intentional fixed-dark immersive screen (fine as a concept),
but when the app is in **light theme** the status bar keeps dark icons → dark-on-dark over the
forest gradient. Force light status-bar icons while this tab is active (or scope it via
`WindowInsetsControllerCompat`).

---

## P2 — Polish / accepted debt

- **No string resources**: `strings.xml` contains only `app_name`; every UI string is inline
  Kotlin. Acceptable for an English-only launch; blocks localization later.
- **Theme shape scale is fought, not used**: `Shapes.medium` = 24dp radius, but screens override
  with ad-hoc `RoundedCornerShape(10–12dp)` almost everywhere. Either lower the scale to match
  reality (12dp) or stop overriding.
- **`contentDescription = null` on meaningful icons** (35 hits): mostly decorative (fine), but the
  synergy check / conflict cancel icons (`CompanionSynergyCard.kt:201`, `:235`) convey status and
  should be described for TalkBack.
- **WeatherSyncCard** is a fully fixed light-mode design (light gradients + fixed dark text).
  Readable in dark mode but visually loud; consider dark gradient variants.
- Nav tab selection has no ripple (`indication = null`) — intentional, but combined with the
  tiny scale animation, taps feel unacknowledged.

---

## Recommended fix order

1. Route all semantic colors through `MaterialTheme.extendedColors` (kills P0-1 and half of P1-7).
2. Nav labels + indicator measurement (P0-2).
3. Strip demo strings from subscription dialog (P0-3, trivial).
4. Add comparison grid to portrait paywall (P0-4, trivial).
5. Typography floor + touch-target pass (P1-5, P1-6).
6. Single `PremiumGold` token + Tailwind color removal (P1-7).
7. CelebrationDialog height + Restoration status bar (P1-8, P1-9).

Estimated effort: P0 items are a focused half-day; P0+P1 roughly a day and a half.

---

# Re-Audit Results — 2026-07-03 (post-fix)

Verified fix commit `4da6692` line-by-line against every finding, then re-ran all pattern
scans across the full UI source. `:app:compileDebugKotlin` passes.

## P0 verification — all fixed ✅

| # | Finding | Status |
|---|---------|--------|
| 1 | Dark-mode pastel/text breakage | ✅ CompanionSynergyCard, DashboardScreen zone chips, and Planner synergy/conflict cards all route through `extendedColors` with alpha tints now |
| 2 | Nav label clipping / indicator drift | ✅ "Garden Counsel" → "Counsel", ellipsis guards added, indicator measured via `BoxWithConstraints`, RTL offset negated, ripple restored |
| 3 | Demo data in subscription console | ✅ `GPA.DEMO-8791-0312` → "Syncing with Google Play...", fake "Entitlement Key" row deleted |
| 4 | Comparison grid missing on phones | ✅ Added as collapsible "Compare Plan Benefits" section in the portrait layout |

## P1 verification — all addressed ✅

- Touch targets: Unlock PRO button and paywall close now 48dp; Planner tray cards `heightIn(min = 48.dp)`.
- Worst micro-type fixed (7sp Eraser → 10sp, 8sp instances → 10–11sp in the flagged spots).
- `PremiumGold` (`0xFFD4AF37`) added to `ExtendedColors` and applied at every premium touchpoint
  (crown, lock overlay, subscription console, upsell crown, restoration paywall).
- Tailwind sky/emerald removed from the Planner seed tray (now `primary`/`tertiary`).
- CelebrationDialog: themed surface, `heightIn(min)` + scrollable column.
- Restoration tab now forces light status-bar icons via `forceDarkStatusBar` in MainActivity.

## New/leftover findings

**Worth fixing before ship (one line):**
1. **`BillingDialog.kt:711`** — the restore-purchase receipt still falls back to
   `"GPA.DEMO-RESTORED"` as the Transaction Order ID. Same bug class as the fixed P0-3;
   use "Synced from Google Play" or "—".

**Polish-level (fine to ship, note for next pass):**
2. `PlannerScreen.kt:784-787` — "FILTER ACTIVE 🎯" badge still uses fixed `0xFFC62828` on
   `0xFFFFEBEE` (self-consistent/readable, but the file's siblings were migrated to `extendedColors.error`).
3. `PlannerScreen.kt:1306-1307` — Clear/Eraser font sizes were fixed, but selected color is
   still hardcoded `0xFFC62828` (mediocre contrast on dark `surfaceVariant`).
4. `BillingDialog.kt:667` — fixed `0xFFE8F5E9` success circle (self-consistent pair, just unmigrated).
5. `LogMoodDialog.kt:96` — 8sp mood labels remain; also `Color.White` where `onPrimary` belongs.
6. ~20 remaining 7–9sp instances, nearly all chart axis labels / in-grid-cell captions
   (TherapyChart, ScoreHistoryChart, CircularBotanicalRhythm, Planner grid cells, AiStudio badges).
   Defensible for dense data displays; bump to 10sp minimum when convenient.
7. `CircularBotanicalRhythm.kt:73-75` and `MoodLogItemCard.kt:143-149` still use Tailwind
   `0x0284C7`/`0x10B981` as data-category colors — same off-palette blue removed from the Planner tray.
8. Decorative `0xFFFFD54F`/`0xFFFFB74D` golds remain (~25×) in Onboarding tints, AiStudio sparkle
   gradients, Planner rings, MindfulBreathingCard. Premium *identity* spots are unified; these are
   ambient decoration and lower-stakes.

## Ship verdict

**Clear to ship after the one-line `BillingDialog.kt:711` fix.** Everything else remaining is
cosmetic debt suitable for a post-launch polish pass.
