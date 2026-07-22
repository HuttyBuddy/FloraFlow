# FloraFlow Design System (DESIGN.md)

## Overview & Philosophy
FloraFlow bridges biophilic spatial design, plant care, and personal restoration. The design system prioritizes a calm, layout-led, editorial aesthetic that reduces cognitive load, promotes focus, and transforms physical spaces into restorative sanctuaries.

---

## 1. Typography & Hierarchy

### Font Families
* **Primary Editorial / Headings**: `Playfair Display` (Serif) — used for emotional titles, sanctuary promises, and high-level card headers.
* **UI Utility / Body / Microcopy**: `Plus Jakarta Sans` (Sans-Serif) — used for form labels, action buttons, descriptions, and metrics.

### Type Scale
| Token | Font Family | Size / Line Height | Weight | Usage |
| :--- | :--- | :--- | :--- | :--- |
| `displayLarge` | Playfair Display | 32sp / 40sp | Bold (700) | Hero titles, primary onboarding promises |
| `displayMedium` | Playfair Display | 26sp / 34sp | SemiBold (600) | Section headers, score screens |
| `titleLarge` | Playfair Display | 20sp / 28sp | SemiBold (600) | Card titles, modal headers |
| `titleMedium` | Plus Jakarta Sans | 16sp / 24sp | Bold (700) | Sub-headers, form section titles |
| `bodyLarge` | Plus Jakarta Sans | 16sp / 24sp | Regular (400) | Primary body text, explanations |
| `bodyMedium` | Plus Jakarta Sans | 14sp / 20sp | Medium (500) | Standard UI text, list item titles |
| `bodySmall` | Plus Jakarta Sans | 12sp / 16sp | Regular (400) | Secondary info, tip descriptions |
| `labelLarge` | Plus Jakarta Sans | 14sp / 20sp | Bold (700) | Button labels, chip text |
| `labelSmall` | Plus Jakarta Sans | 11sp / 16sp | SemiBold (600) | Badges, tags, timestamp labels |

---

## 2. Color System & Palettes

### Primary Botanical Palette (HSL Tailored)
* **Deep Botanical Forest (`Primary`)**: `#1F483E` (HSL: 165°, 40%, 20%) — primary actions, active navigation icons, high-contrast titles.
* **Botanical Sage (`Secondary`)**: `#43493E` (HSL: 95°, 8%, 26%) — subheadings, borders, subtle indicators.
* **Warm Terracotta (`Accent / Coral`)**: `#D97724` (HSL: 28°, 71%, 50%) — CTAs, highlights, notification dots.
* **Sandstone Canvas (`Background Light`)**: `#FAF8F5` (HSL: 38°, 33%, 97%) — main light background surface.
* **Deep Night Forest (`Background Dark`)**: `#121B18` (HSL: 160°, 20%, 9%) — main dark background surface.

### Biophilic Zone Indicators
* **Green Zone (Optimal Calm)**:
  * Text/Icon: `#2E7D32`
  * Container: `#E8F5E9` (Dark: `#1B3B32`)
* **Yellow Zone (Moderate Awareness)**:
  * Text/Icon: `#F57F17`
  * Container: `#FFFDE7` (Dark: `#2D2517`)
* **Red Zone (Restorative Opportunity)**:
  * Text/Icon: `#C62828`
  * Container: `#FFFFEBEE` (Dark: `#3A1919`)

---

## 3. Surface & Spatial Hierarchy

### Elevation & Corner Radius
* **Base Cards**: `RoundedCornerShape(20.dp)` — container background surface with 1.5dp subtle border gradient.
* **Primary Buttons**: `RoundedCornerShape(12.dp)` — 48dp minimum touch target height.
* **Chips & Badges**: `RoundedCornerShape(8.dp)` or `CircleShape` for status pills.

### Spacing Scale
* `spacing.micro`: 4.dp
* `spacing.small`: 8.dp
* `spacing.medium`: 16.dp
* `spacing.large`: 24.dp
* `spacing.huge`: 32.dp

---

## 4. Component Rules & Contracts

### Buttons
* **Primary Action**: Solid `Primary` background with `OnPrimary` text. Bold weight, centered icon + text.
* **Secondary / Outlined**: 1.5dp border stroke using `Primary` color, transparent container.
* **Ghost / Icon Action**: Circular background overlay on hover/press (`Primary.copy(alpha = 0.1f)`).

### Interactive Next-Steps Checklist
* Interactive `Checkbox` paired with text line-through when checked.
* Direct action icon shortcuts on the right leading to corresponding feature tab (Planner, Database, Restoration).
* `LinearProgressIndicator` showing real-time `x/3 steps completed`.

---

## 5. Motion & Micro-Interactions

* **Expand/Collapse**: `AnimatedVisibility` with `expandVertically()` + `fadeIn()` (300ms cubic bezier).
* **State Transitions**: Smooth scale animations (`animateFloatAsState`) on interactive buttons.
* **Page Crossfade**: `Crossfade` navigation transitions between tab selections.

---

## 6. Accessibility & Responsiveness

* **TalkBack & Semantics**: All interactive components provide explicit `contentDescription` and `testTag`.
* **Minimum Touch Targets**: Every clickable surface guarantees at least `48dp x 48dp` tap area.
* **Responsive Layouts**: Supports adaptive two-column layouts on wide screens (`width >= 600.dp`) and scrollable column views on phones.
* **High Contrast**: Meets WCAG AAA standards for text readability in both Light and Dark theme modes.
