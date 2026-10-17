# FloraFlow — Marketing & Branding Sheet

**Version:** 2.0 · **Date:** 2026-07-25 · **Status:** Current — supersedes the positioning in `brand-voice.md`, `marketing-goals.md`, `product-spec.md`, `DESIGN.md`, and `.claude/brand-voice-guidelines.md`

This sheet reflects the app as it actually ships today, after the Restorative Corner pivot, the 3-Card Sanctuary refactor, and the FloraFlow PRO paywall build. Where an older doc contradicts this one, this one wins. Section 12 lists every stale doc and what's wrong with it.

---

## 1. What changed since the last sheet

The product moved twice in quick succession. Marketing built against the first version is now wrong.

| Area | Was | Is now |
|---|---|---|
| **Category** | Outdoor garden planner (layouts, companion planting, USDA zones, AR lens) | Indoor restorative-corner designer — one corner, not a garden |
| **Scope** | Gardens, balconies, backyards, rooftops | 100% indoor houseplants and indoor rooms. All outdoor crop/farming references removed from DB, climate templates, planner tray, and AI prompts |
| **Core promise** | "Design your dream garden" | "Turn one indoor corner into a place you can actually restore in" |
| **App architecture** | 5-tab dashboard with an AI FAB | **3-Card swipeable Sanctuary Deck** with a persistent top-of-card AI button |
| **Monetization** | Billing screen, soft upsell | **FloraFlow PRO paywall dialog** — hard modal, annual-default, 3-day free trial |
| **Growth engine** | Organic content + ASO | In-app **viral share surfaces**: Vibe Check story card, 15s Reels exporter, Plant Parent Archetype badges, Co-Care Duet widget |
| **Accent color** | Warm Terracotta `#D97724` | **Soft Gold / Amber `#D4AF37`** — terracotta is dead, do not use it |
| **AI persona name** | "Garden Counsel" | **"AI Plant Counsel"** — Dr. Julian Greenleaf |
| **Hero metric for acquisition** | First binaural soundscape session | Restorative Corner assessment completed → Vibe Check score shared |

---

## 2. Positioning

### One-liner
**FloraFlow turns one indoor corner into a restorative space, using your actual light, your actual room, and plants that will survive there.**

### Category
Indoor biophilic wellness — sits between plant-care apps and meditation apps, and is neither. Plant apps tell you *what* to water. Meditation apps put calm in your headphones. FloraFlow engineers calm into the physical room you're already sitting in.

### Positioning statement
For urban and suburban renters and homeowners aged 25–45 who feel drained by their indoor environment, FloraFlow is an Android app that diagnoses the cognitive stress in a specific corner of their home and gives them a small, doable plan to fix it — combining biophilic design science, real daylight sensing, and an AI plant advisor. Unlike generic plant-care apps, FloraFlow starts from the room, not the plant.

### The three-word version
**Diagnose. Prescribe. Tend.**

### Competitive frames
- **vs. plant care apps (Planta, Picture This):** they identify and remind. We diagnose the *space* and prescribe placement. The plant is the intervention, not the product.
- **vs. meditation apps (Calm, Headspace):** their calm ends when the headphones come off. Ours is built into the room and persists.
- **vs. interior design apps:** we're evidence-led, not taste-led. Every recommendation traces to attention restoration theory, stress recovery theory, or daylight measurement.
- **vs. doing nothing:** a plant put in the wrong place is decoration. Placed against measured light and acoustic goals, it's an intervention.

### What FloraFlow is *not* — hold this line in every asset
- Not outdoor gardening, farming, crops, or landscaping
- Not a professional landscape-architecture tool
- Not a social feed (sharing is via native Android share intents only)
- Not a generic meditation app — audio and video output are tied to the user's own corner score

---

## 3. Product truth — what we can actually claim

Claims are only usable if they map to shipped code. This table is the source of truth for ad copy, store copy, and creator briefs.

| Shipped feature | In-app name (use exactly) | Claim you may make |
|---|---|---|
| 10-question biophilic assessment, scored /20, Green/Yellow/Red zone | **Restorative Corner Assessment** → **Neural Load Score** | "Score your corner in about 3 minutes" |
| 3-card swipeable pager | **Sanctuary Deck** — Card 1 *Restorative Corner*, Card 2 *Plant Companion Match*, Card 3 *Daily Tend & Soundscapes* | "Your whole sanctuary in three swipes" |
| Gemini-backed advisor with vision | **AI Plant Counsel** (Dr. Julian Greenleaf) | "Ask about placement, light, pests, companion synergy" |
| Hardware ambient light sensor, live lux readout + zone | **Real-Time Daylight Sensor** | "Measures your actual daylight in lux" — it is a real sensor read, not an estimate |
| Preset-driven room scan, 0–100 score, vibe tag, 3 upgrades | **AI Room Vibe Check** → **Biophilic Vitality Score** | "Instant space scan with a shareable score" — see §11, this is currently preset-driven, don't imply camera CV |
| Procedural binaural generator, 4–40Hz slider + Alpha 10 / Theta 6 / Gamma 40 presets | **Custom Binaural Studio** | "Tune your own 4–40Hz brainwave soundscape" |
| MediaCodec H.264 1080×1920 30fps MP4 encoder | **15-Second Ambient Reel** | "Export a 15-second reel for TikTok or Reels in seconds" |
| 1080×1920 story card generator | **Viral Vibe Card** | "Share your score as a story card" |
| 5 archetypes from score + lowest categories + streak | **Plant Parent Archetype** | "Find out which of five plant parents you are" |
| Home screen widget, shared streak + partner status | **Co-Care Duet Widget** | "Share a plant with your partner or roommate on your home screen" |
| Daily habit loop, streak + badge tiers | **Plant Care Streak** (Seedling Tender → Botanical Caregiver → Master Sanctuary Keeper) | "Build a daily tending habit" |
| 3-step onboarding: light → space → plants, with saved plan | **Restorative Validation** / starter plan | "A starter plan in about 3 minutes" |
| Weather-driven indoor advice by ZIP | **Weather Sync** | "Misting and placement advice that follows your local weather" |

### The five Plant Parent Archetypes
Shareable identity units — the core organic loop. Colors are locked; use them in creative.

| Archetype | Icon | Badge color | Trigger |
|---|---|---|---|
| Jungle Maximalist | 🌿 | `#2E7D32` | Foliage/greenery is a weak category, score ≥ 12 (also the default) |
| Cactus Survivor | 🌵 | `#E65100` | Score 1–7 |
| Serial Overwaterer | 💦 | `#0288D1` | Care streak > 5 days |
| Cyberpunk Botanist | ⚡ | `#7B1FA2` | Light/daylight is a weak category |
| Sanctuary Master | ✨ | `#D4AF37` | Score ≥ 16 |

---

## 4. Monetization — exact, do not paraphrase

**Product name:** FloraFlow PRO (always uppercase PRO)

| Tier | Price | Framing shown in-app | Play SKU |
|---|---|---|---|
| **Annual Pass** (default-selected, BEST VALUE) | **$49.99 / year** | "$4.16 / month — SAVE 58%" · 3-Day Free Trial | `floraflow_premium_yearly` |
| **Monthly Pass** | **$9.99 / month** | "Flexible billing, cancel anytime" | `floraflow_premium_monthly` |

**Paywall headline:** "Transform Your Sanctuary"
**Paywall subhead:** "Turn any dark indoor space into a calm, restorative biophilic haven with full AI power."
**Primary CTA:** "Start 3-Day Free Trial" (annual) / "Subscribe Now ($9.99/mo)" (monthly)
**Trust line:** "🔒 Secured by Google Play. No commitment, cancel anytime."

**The four PRO benefits — use this order and this wording:**
1. Unlimited AI Plant Counsel — instant Leaf Doctor vision diagnosis & 24/7 care guidance
2. Real-Time Spatial Lux Meter — hardware sensor daylight mapping & window placement advice
3. Custom 4–40Hz Binaural Audio — Alpha & Theta brainwave soundscapes for calm focus
4. Sanctuary Care Streak Rewards — botanical caregiver badges & streak progress

**Free tier — say this accurately, it's a compliance surface:**
- 3 AI Plant Counsel consultations, **total** (lifetime, not daily). A failed request never consumes one.
- 3 restoration/soundscape sessions per week, on a rolling 7-day window.
- Plant placement, daily habit tracking, and the Restorative Corner Assessment are free forever.

**Paywall trigger points (where PRO gets sold):** binaural preset chips and frequency slider, Vibe Check "Unlock Deep AI Room Transformation," AI quota exhaustion, post-assessment score screen ("Start 3-Day Free Trial" vs. "Continue with Basic Free Tips").

---

## 5. Visual identity

### Logo
Circular deep-forest medallion with a two-leaf heart — sage-green leaf left, warm-sand leaf right, cream dashed inner ring. Files: `play_store_assets/FloraFlow Logo.png`, `FloraFlow icon.svg`, in-app `drawable/ic_logo_heart.png`.

**Rules:** never recolor the leaves; never separate the mark from its circular field; never place on a mid-green background (the medallion disappears). Minimum clear space = 25% of the medallion diameter. Minimum size 36dp in-app, 48px in web/social.

### Color palette — canonical

**Light (primary brand expression)**
| Role | Hex | Name |
|---|---|---|
| Primary / text | `#1B4D3E` | Deep Forest Green |
| Secondary / accent | `#D4AF37` | Soft Gold-Amber |
| Tertiary | `#633B0D` | Deep Timber |
| Background | `#FDF5E6` | Pale Cream (sunlit linen) |
| Surface / cards | `#FFFFFF` | Pure White |
| Muted body text | `#43493E` | Muted Sage |
| Border | `#E0E0E0` | Subtle Border |

**Dark**
| Role | Hex | Name |
|---|---|---|
| Primary | `#ACCFC6` | Moonlight Sage |
| Tertiary | `#E2C4A2` | Natural Wicker |
| Background | `#141511` | Night Ground |
| Surface | `#1B1D17` | Dark Sand |
| Text | `#E5E2D9` | Oatmeal Cream |
| Muted | `#8D9280` | Foggy Sage |

**Premium accent (PRO surfaces only):** `#D4AF37` gold, gold gradient `#E5C060 → #C59F3F`, gold border brush `#FFDF00 → #D4AF37 → #FFDF00`. The paywall also uses `#E5A93C` for its badge and border.

**Share-card palette (social exports only — deliberately darker and more saturated than the app):** background gradient `#061A12 → #0B2B1D → #030D08`, brand mint `#7FE3B5`, text `#E0F7ED`, delta green `#52E09B`, body `#C3EBD9`.

**Zone indicators:** Green `#2E7D32` · Yellow `#F57F17` · Red `#C62828`.

**Retired:** Warm Terracotta `#D97724` and Sandstone `#FAF8F5` from the old `DESIGN.md`. Pull them from any live creative.

### Typography
- **Display / headline / title:** Playfair Display — editorial serif, all emotional and score-facing copy
- **Body / label / UI:** Plus Jakarta Sans — everything functional
- **Marketing lockups only:** the feature graphic uses Cinzel for the wordmark and Space Grotesk for the tagline; keep that treatment for the store banner and out-of-app hero art, not for in-product screens
- Scale in-app: displayLarge 57sp → labelSmall 11sp; card radius 20dp, button radius 12–16dp, paywall card 28dp

### Taglines
- **Primary:** "Breathe life into every corner of your indoor space."
- **Store-experiment short:** "Design a calming indoor meditation corner shaped around your light and space."
- **Onboarding promise:** "Design a space that helps you thrive"
- **Share-card footer:** "FloraFlow: Indoor Sanctuaries 🌿 • Breathe life into your space"

---

## 6. Voice

FloraFlow speaks like a knowledgeable friend with an environmental-psychology background who genuinely lights up explaining why a plant in the right corner changes how a room feels. Warm, science-grounded, unhurried, never clinical, never hyped.

**We are / we are not**

| We are | We are not |
|---|---|
| Warm — specific enthusiasm, not generic cheer | Saccharine |
| Grounded in science — ART, SRT, cortisol, lux, biophilia | Academic — science supports the story, never leads it |
| Encouraging — small wins count, imperfect corners count | Condescending — no beginner/expert tiers |
| Clear — translate "neural load" into plain language | Oversimplified — the audience can handle real ideas |
| Restorative — copy reads like a breath out | Passive — always "you," always active voice |
| Honest about scope — one corner, indoors, doable | Hyperbolic — no "transform your life" |

**Two registers — this is the most important voice rule right now.**

The app currently ships two very different tones, and they must be used deliberately, never mixed in one asset:

- **Restorative register** (onboarding, Restorative Validation, assessment, store listing): plain, quiet, unhurried. *"Turn the light and space you already have into a small, nature-supported place to pause and restore."* · *"A calm corner can begin with a single surface."* · *"Start with one small move; the space does not need to be perfect."* This is the acquisition voice.
- **Viral register** (Vibe Check, share cards, Reels, archetypes): playful, energetic, screenshot-bait. *"Cyberpunk Sanctuary (Low Oxygen Zone)"* · *"+34% Potential Upgrade Unlocked 🌿"* · *"Your Viral Vibe Card"* This is the sharing voice — it earns the screenshot, but it is not the brand's first impression.

Rule: **restorative on the way in, viral on the way out.** Store listing, paid social, and onboarding use the restorative register. In-app share surfaces and creator-facing content use the viral register.

### Voice in practice
| Context | We say | We don't say |
|---|---|---|
| Onboarding | "Let's figure out what your space needs." | "Please complete the setup wizard." |
| Error | "Hmm, something didn't load — let's try again." | "An error occurred. Error code: 403." |
| Plant suggestion | "This one thrives in spots just like yours." | "Optimal species selection for your USDA zone." |
| Soundscape | "Put on your headphones and let the wind chimes settle your mind." | "Activate the background audio playback system." |
| Paywall | "Turn any dark indoor space into a calm, restorative haven." | "Upgrade now to unlock premium features!" |

### Language rules
Use "you." Active voice. Contractions. Common plant names first, scientific in parentheses. Both imperial and metric where space allows.

---

## 7. Terminology — canonical vs. retired

| Use | Never use | Why |
|---|---|---|
| Restorative Corner Assessment | Neural Load Assessment, Biophilic Profile, garden assessment | Renamed in the pivot |
| AI Plant Counsel / Dr. Julian Greenleaf | Garden Counsel, AI advisor, chatbot | Renamed |
| Sanctuary Deck, Card 1/2/3 | dashboard, tabs, home screen | Architecture changed |
| Corner, space, room | garden, yard, plot, bed, backyard, balcony | Indoor-only, no exceptions |
| Houseplant, living plant | crop, produce, seedling tray | Indoor-only |
| FloraFlow PRO | premium, pro tier, paid version | Product name |
| Neural Load Score (/20) | wellness score, stress score | Assessment metric |
| Biophilic Vitality Score (%) | vibe score, room score | Vibe Check metric |
| Restorative Corner / sanctuary | dream garden, paradise, oasis (except "Mindful Rest Oasis" vibe tag) | Category discipline |
| Beginner-friendly framing without the word | "beginner," "advanced," "optimize," "hack," "transform your life" | Off-voice |

---

## 8. Audience

**Primary:** 25–45, urban and suburban, apartments and homes — **indoor spaces**, including renters with no outdoor space at all. Drawn to biophilic design instinctively. Value intentionality in their home. Already interested in mental wellness; many use meditation or ambient-sound apps. Comfortable with Android and expect polish. Want to understand *why* they feel better, not just be told they will.

**Highest-intent segments now that the pivot landed:**
1. **WFH desk sufferers** — the "Cyberpunk Sanctuary (Low Oxygen Zone)" result is the single most screenshot-worthy moment in the app. Lead paid social here.
2. **Renters with one dark corner** — the constraint the product was rebuilt around.
3. **Couples and roommates** — Co-Care Duet widget is the only true two-person hook.

**Secondary:** wellness-first users who arrive for soundscapes and stay for plants; designers using the assessment as a client conversation tool.

**Retired audience language:** balconies, backyards, rooftop gardens, container gardening outdoors. Delete from all live copy.

---

## 9. Messaging by channel

| Channel | Register | Lead with | Primary CTA |
|---|---|---|---|
| Play Store listing | Restorative | One corner, your light, in 3 minutes | Install |
| Paid social (Meta/IG) | Viral hook → restorative payoff | The desk-score reveal | Install |
| Pinterest | Restorative, visual | Before/after corner, placement diagram | Save → install |
| TikTok / Shorts | Viral | Archetype reveal, vibe-tag roast | Watch full → install |
| YouTube long-form | Restorative, high depth | Corner audit teardown, the science | Subscribe → app mid-roll |
| ASO keywords | Restorative | indoor plants, biophilic design, calm corner, meditation space | — |
| In-app share | Viral | Score + archetype badge | Share |

### Message pillars
1. **The corner is the unit** (35%) — you don't need a garden, a yard, or a redesign. One corner. *"You don't need a green thumb. You need one corner and better information."*
2. **The science of why** (30%) — attention restoration theory, stress recovery theory, measured lux. *"The reason your home office drains you has nothing to do with your to-do list."*
3. **Your score, your archetype** (20%) — identity and shareability. *"Score your corner. Find your archetype. Screenshot the receipts."*
4. **Built in public** (15%) — the founder story stays a differentiator; keep it honest and unpolished.

---

## 10. Growth surfaces & viral loops

Every one of these is shipped. Brief creators against them specifically.

1. **Vibe Card loop** — run Vibe Check → get score, vibe tag, three upgrades → export 1080×1920 story card → share intent carries *"My indoor plant sanctuary score is X%! Discover your Plant Parent Archetype with FloraFlow: Indoor Sanctuaries 🌿✨"*
2. **Reels loop** — 15-second 1080×1920 30fps MP4 merging the soundwave visualizer, the score, the archetype badge, and the binaural track. Encodes on-device in roughly 1.5 seconds. Built for TikTok/Reels sound templates.
3. **Archetype loop** — five identity badges; the score card carries the badge.
4. **Score card loop** — the assessment result screen has its own "Share My Score Card" export, separate from Vibe Check.
5. **Co-Care Duet loop** — home-screen widget shared between two people; the only invite-a-human mechanic in the app.
6. **Streak loop** — Seedling Tender (< 7d) → Botanical Caregiver (< 30d) → Master Sanctuary Keeper (30d+).

**Suggested north star, replacing the old soundscape-based one:** new users who complete the Restorative Corner Assessment *and* export or share one card within 7 days. It's the only event that predicts both retention and organic reach under the current build.

---

## 11. Store listing

**Currently live in the Gate 3 experiment** (`play_store_assets/gate3_experiment.json`):
- Title: `FloraFlow: Garden & Biophilic Space Designer`
- Short: "Design a calming indoor meditation corner shaped around your light and space."
- Captions: "Turn one indoor corner into a place for calm." / "Get placement guidance shaped around your light and available room." / "Save one doable change. Return to your corner."

**Recommended next iteration** — the live title still says "Garden," which contradicts the indoor-only pivot and pulls the wrong install intent:
- Title: `FloraFlow: Indoor Plant Sanctuary` or `FloraFlow: Calm Corner Designer`
- Short: "Turn one indoor corner into a calm, restorative space — shaped around your real light."
- Full description leads with the corner and the 3-minute plan, then the AI Plant Counsel, then the daylight sensor, then soundscapes, then PRO.

**Asset specs:** feature graphic 1024×500 (`feature_graphic_1024_500.svg/.png`, Cinzel wordmark on `#152B24`), phone screenshots in `play_store_assets/enhanced/`, icon in `mipmap-*` + `FloraFlow icon.svg`.

**Creative production:** the repo's Higgsfield skills generate brand-consistent assets — `floraflow-photoshoot` (2K statics, App Store images, Pinterest pins, banners), `floraflow-marketing-video` (5-second DTC hooks, TikTok, Meta variants), `floraflow-ad-batch` (3/5/10 A/B variants), `floraflow-storyboard-grid` (3×3 previs), `floraflow-video-longform` (YouTube explainers). Feed them this sheet's palette, type, and terminology sections.

---

## 12. Open issues to resolve before the next campaign

These are real inconsistencies in the current build and docs. Flagging, not fixing — several are product calls, not marketing calls.

1. **Two competing scores.** Neural Load Score (0–20, Green/Yellow/Red) and Biophilic Vitality Score (0–100%) both describe "how good is your space." Users will conflate them. Pick one public-facing metric before scaled acquisition; suggest keeping Neural Load Score in-product and using the % only on share cards.
2. **Vibe Check is preset-driven, not camera CV.** It maps four hardcoded presets ("My Work Desk," "Living Room Corner," "Bedroom Nightstand," "Balcony Garden") to fixed scores and upgrade lists. Copy must not imply photo analysis or camera scanning until that ships. Current in-app copy ("Instant Biophilic Space Scan," "Run AI Space Diagnostic") is already ahead of the implementation.
3. **"Balcony Garden" preset survives the indoor pivot.** It's an outdoor preset in a 100%-indoor app, and it carries the highest score (92). Either remove it or reframe as "Window Ledge."
4. **Store title still says "Garden."** Contradicts the pivot and every other surface.
5. **Version drift.** `versionName` is `9.0.1.3` / `versionCode 22`, while `CHANGELOG.md` and `release_notes.txt` both announce `10.0.0.0`. Release notes will not match the build users install.
6. **`metadata.json` is pre-pivot.** Still describes "design your dream garden," AR, and outdoor care. It's the shortest, most-copied description in the repo.
7. **Release notes are pre-pivot.** They lead with archetypes and Co-Care, not the Restorative Corner or PRO — the two things that actually changed for users.
8. **`DESIGN.md` palette is wrong.** Terracotta accent, `#1F483E` primary, and `#FAF8F5` background do not match `Color.kt` (`#D4AF37`, `#1B4D3E`, `#FDF5E6`).
9. **`brand-voice.md` demographics are outdoor.** "Balconies, backyards, rooftop gardens" in an indoor-only product.
10. **`marketing-goals.md` north star is obsolete.** Activation defined as "first binaural soundscape" — soundscapes are now a PRO-gated Card 3 feature, not the acquisition moment.
11. **Free-AI-quota framing.** 3 lifetime consultations is very tight for a product whose paywall headline promises "full AI power." Worth an experiment at 3/week before scaling paid spend.

### Doc status

| File | Status |
|---|---|
| `MARKETING-BRAND-SHEET.md` (this) | ✅ Current |
| `play_store_assets/gate3_experiment.json` | ✅ Current — title needs one more pass |
| `DESIGN.md` | ⚠️ Palette stale |
| `brand-voice.md` | ⚠️ Voice good, demographics and feature list stale |
| `.claude/brand-voice-guidelines.md` | ⚠️ Voice framework good, product claims pre-pivot |
| `README.md` | ⚠️ Feature-accurate, positioning pre-Restorative-Corner |
| `product-spec.md` | ⚠️ Missing 3-Card architecture and PRO paywall |
| `marketing-goals.md` | ❌ North star and channel plan obsolete |
| `metadata.json` | ❌ Pre-pivot outdoor description |
| `play_store_assets/release_notes.txt` | ❌ Pre-pivot, wrong version |
| `docs/whitepaper_science_of_floraflow.md` | ✅ Science holds; swap "AI Garden Counsel" → "AI Plant Counsel" |
