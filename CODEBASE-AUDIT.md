# FloraFlow Codebase Audit — Pre-Ship (2026-07-03)

Scope: everything beyond the UI (covered in [UI-AUDIT.md](UI-AUDIT.md)) — build config, manifest,
secrets, Gemini client + proxy, billing, Room database, repositories, analytics, services, repo
hygiene, and test state.

Overall: **the engineering fundamentals are solid** — real applicationId, externalized signing,
minification on, careful billing revocation logic, no committed secrets, 78 passing unit tests
with migration and screenshot test infrastructure. But there are three genuine ship risks below,
and one honest correction about verification.

---

## ⚠️ Correction to the previous re-audit

The re-audit claimed "`:app:compileDebugKotlin` passes." **That was wrong.** The Gradle run
actually failed with `Unable to establish loopback connection` and the piped `tail` masked the
exit code. Gradle currently cannot run on this machine at all — even `gradlew help` fails —
because something (firewall / security software) is blocking localhost sockets for the daemon.

Consequences:
- The UI fix commit `4da6692` and the `BillingDialog.kt` demo-string fix are **diff-verified but
  not compile-verified**.
- The last known-good full verification is today 12:23 (**78 tests, 0 failures, 0 skipped**) —
  which predates the fix commit (18:01).

**Action: build and run the test suite from Android Studio** (its embedded Gradle usually
survives firewall weirdness), or allow Java through the firewall, before generating the release AAB.

---

## P0 — Resolve before shipping

### 1. `APP_SHARED_SECRET` is missing from `.env`
`.env` has real values for `GEMINI_API_KEY` and `GEMINI_PROXY_URL`, but **no `APP_SHARED_SECRET`
line at all** — so `BuildConfig.APP_SHARED_SECRET` falls back to the `.env.example` placeholder
and [GeminiApiClient.kt:140](app/src/main/java/com/example/data/api/GeminiApiClient.kt) sends
**no `X-App-Secret` header**. Two possible states, both bad:

- If the deployed proxy has `APP_SHARED_SECRET` set → every production AI request returns 401
  → **all AI features silently dead in the shipped app**.
- If the proxy doesn't have it set → the endpoint accepts requests from anyone who finds the URL
  (the proxy code itself logs a warning about exactly this), and your Gemini quota is scrapeable.

Fix: generate a secret, set it in `.env` *and* on the Cloud Run service, rebuild. The proxy's own
comment is right that Play Integrity is the real answer later; the shared secret is fine for launch.

### 2. Release build is minified with empty ProGuard rules — never verified
[proguard-rules.pro](app/proguard-rules.pro) is the untouched template, while release has
`isMinifyEnabled = true` + `isShrinkResources = true` + R8. Retrofit/Moshi/OkHttp bundle their own
consumer rules and all DTOs use Moshi **codegen** (`@JsonClass(generateAdapter = true)`), which is
R8-safe — so this *probably* works. But `GeminiApiClient` also installs the reflection-based
`KotlinJsonAdapterFactory` as a fallback, which is exactly the thing R8 breaks.

Fix (both cheap):
1. Delete `KotlinJsonAdapterFactory` from `GeminiApiClient` — every serialized type already has a
   generated adapter, so the reflection path is dead weight and pure risk.
2. Install the **release** AAB (or a minified local build) on a device and smoke-test the three
   network paths: AI chat, paywall pricing (billing), weather sync. Ten minutes, catches every
   R8 stripping issue at once.

### 3. Room upgrade path: verify no earlier release shipped a DB version below 7
[GardenDatabase.kt](app/src/main/java/com/example/data/database/GardenDatabase.kt) is at
`version = 7` with `ALL_MIGRATIONS` empty and — deliberately, per the comment — no destructive
fallback. That design is correct going forward, but `versionCode = 19` means there have been
prior releases. **If any previously shipped build used DB version 1–6, every upgrading user
crashes on first launch** (Room throws when a migration is missing). Only schema `7.json` is in
`app/schemas/`, so the history isn't recoverable from the repo.

Fix: install the currently-live Play build on a device, then update to the new build over it and
launch. If it opens, you're clean. If prior releases really did ship v7 from the start, this is a
non-issue — but verify it once, on a device, before upload.

---

## P1 — Should fix / verify

4. **Fake order ID can reach real users** — [BillingManager.kt:151](app/src/main/java/com/example/billing/BillingManager.kt):
   when Play returns no `orderId` (happens with license-tester and promo purchases), the code
   fabricates `GPA.1234-5678-MOCK` and shows it in the subscription console — the same demo-data
   bug class we just removed from the UI. Use `"—"` instead. Also note the "Renewal Date" is
   computed locally (`now + 1 month/year`), not from Play — acceptable, but label it "estimated".
5. **Weather is US-only and silently fabricated offline** — [WeatherRepository.kt](app/src/main/java/com/example/data/repository/WeatherRepository.kt)
   geocodes via `zippopotam.us/us` (5-digit US zips only, default `90210`) and on any failure
   falls back to invented weather presented as real ("Home Haven", 71.6°F). Fine as a design
   choice for launch, but international users will always see fake weather. Consider a "simulated"
   label; at minimum know this before reading support mail.
6. **Version sanity** — `versionName = "9.0.1.0"`, `versionCode = 19`: confirm 19 is above the
   live Play versionCode, and that the 9.x name is intentional.
7. **Play Data Safety form** — Firebase Analytics + Crashlytics are initialized unconditionally
   at launch with no consent gate. That's allowed, but the Play Data Safety declaration must list
   the corresponding data collection, and the store listing needs a privacy policy URL.
8. **Camera/AR manifest leftovers** — the manifest declares `CAMERA`, seven camera features, and
   AR metadata (`tools:overrideLibrary` for `io.github.sceneview.*`), and CameraX dependencies are
   included — verify the camera flow is actually reachable in the shipped app. Play review asks
   about CAMERA if nothing visibly uses it, and the sceneview override references a library that
   is no longer a dependency (stale but harmless).

## P2 — Housekeeping

9. **Dead code**: [WeatherService.kt](app/src/main/java/com/example/data/api/WeatherService.kt)
   (OpenWeatherMap Retrofit interface) is referenced nowhere — the app uses Open-Meteo via
   `HttpURLConnection`. Delete it.
10. `debug.keystore.base64` is committed — it's the standard debug keystore (password "android"),
    so no secret leaked; presumably for CI. Fine, just deliberate clutter.
11. `app/release/app-release.aab` sits on disk but is correctly *not* tracked by git. Good —
    make sure the one you upload is rebuilt after the P0 fixes, not this stale artifact.
12. `WeatherRepository.isUnderTest` detects JUnit via classpath reflection — works, but a
    constructor flag would be cleaner than production code sniffing for test frameworks.

## What's in good shape (no action)

- **Secrets hygiene**: `.env` git-ignored, no API keys in history, signing config reads from
  `local.properties`/env vars, `google-services.json` committed (correct — it's not a secret).
- **Manifest**: only MainActivity exported, backup disabled with extraction rules, foreground
  service properly typed (`mediaPlayback`) with its permission and a real notification channel,
  widget receiver unexported.
- **Billing**: acknowledgement handled, revocation restricted to full reconciliation passes,
  pending purchases respected, mock mode locked behind `BuildConfig.DEBUG`, prices/trials pulled
  from live `ProductDetails` so the paywall can't over-promise.
- **Gemini proxy design**: key kept server-side when proxied, per-IP rate limiting, honest
  comments about its own limitations.
- **Concurrency**: no `GlobalScope`, no `runBlocking`, no `Thread.sleep` in app code; ViewModel
  work runs on `viewModelScope` with an injected IO dispatcher.
- **Tests**: 78 unit tests green as of today's pre-fix run, including Room migration-infrastructure
  and Roborazzi screenshot tests.

---

## Ship checklist (ordered)

1. Fix the Gradle/firewall issue or switch to Android Studio's build; re-run the 78-test suite
   against the current HEAD (post-fix code has never been compiled).
2. Set `APP_SHARED_SECRET` in `.env` + Cloud Run; confirm an AI request succeeds through the proxy.
3. Remove `KotlinJsonAdapterFactory`; build the release AAB; smoke-test AI, billing, weather on
   the minified build.
4. Device-test the upgrade path from the live Play build (Room v7 question).
5. One-line `orderId ?: "—"` fix in BillingManager.
6. Confirm versionCode/Data Safety/privacy policy in Play Console.
