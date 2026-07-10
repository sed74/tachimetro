---
phase: 04-velocit-massima-e-persistenza
verified: 2026-07-10T00:00:00Z
status: passed
score: 7/7 must-haves verified
---

# Phase 4: Velocità Massima e Persistenza Verification Report

**Phase Goal:** L'utente può monitorare la velocità massima raggiunta dall'ultimo azzeramento e il valore sopravvive alla chiusura dell'app e al riavvio del telefono.
**Verified:** 2026-07-10
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | L'utente vede la velocità massima raggiunta dall'ultimo azzeramento in un'area secondaria dello schermo (Roadmap SC1 / MAX-01) | ✓ VERIFIED | `maxSpeedText` (top-left, mirrors `unitText`) added to `activity_main.xml`, populated via `getString(R.string.max_speed_format, currentMax)` in `updateMaxArea()`, `MainActivity.kt:268-277`. Confirmed on-device in 04-02-SUMMARY.md (case 2). |
| 2 | Premendo "Azzera massimo" il valore si azzera e inizia una nuova misurazione (Roadmap SC2 / MAX-02) | ✓ VERIFIED | `resetMaxButton.setOnClickListener { onResetMaxClicked() }` (`MainActivity.kt:90`); `onResetMaxClicked()` sets `currentMax = 0`, writes 0 to disk, calls `updateMaxArea()` (no confirmation dialog, D-04) — `MainActivity.kt:260-264`. Confirmed on-device (case 4). |
| 3 | Chiudendo/riaprendo l'app o riavviando il telefono, il massimo precedente resta visibile (Roadmap SC3 / MAX-03) | ✓ VERIFIED | `MaxSpeedStore` persists via `SharedPreferences...apply()`; `onCreate()` reads `maxSpeedStore.read()` before GPS collection starts (`MainActivity.kt:94-96`). Persistence across app-close AND phone reboot explicitly confirmed via `adb reboot` on Pixel 10 Pro emulator, approved by user ("approvato") — 04-02-SUMMARY.md cases 5 & 6. |
| 4 | Il massimo cresce con la velocità e non diminuisce quando la velocità corrente scende (D-07) | ✓ VERIFIED | `reduceMax(currentMax, reading)` uses `max(safeCurrent, safeReading)`, clamps negatives to 0 — `MaxSpeedReducer.kt:6-10`. Locked by 8 passing JVM unit tests in `MaxSpeedReducerTest.kt` (`./gradlew.bat :app:testDebugUnitTest --tests "com.sed.tachimetro.maxspeed.MaxSpeedReducerTest"` → BUILD SUCCESSFUL, re-run during this verification). Confirmed on-device (case 3). |
| 5 | Nessun "MAX 0" è mai mostrato — area nascosta finché il massimo è 0 (D-03/D-09) | ✓ VERIFIED | `maxSpeedText`/`resetMaxButton` default `android:visibility="gone"` in XML; `updateMaxArea()` only sets `VISIBLE` when `currentMax > 0`, else `GONE` — `MainActivity.kt:268-277`. Read-before-GPS-start ordering in `onCreate()` avoids startup flash. Confirmed on-device (case 1). |
| 6 | L'area MAX rispetta gli insets di status bar/cutout in portrait e landscape (top+left), speculare a `unitText` | ✓ VERIFIED | `applyMaxAreaWindowInsets()` mirrors `applyUnitTextWindowInsets()` pattern, adding live `systemBars`/`displayCutout` top+left insets on top of XML base margins — `MainActivity.kt:352-372`. Confirmed on-device (case 7). |
| 7 | Il pulsante Azzera e il pulsante Riprova non compaiono mai insieme | ✓ VERIFIED | `showDenied()` explicitly sets `maxSpeedText.visibility = View.GONE` and `resetMaxButton.visibility = View.GONE` alongside `retryButton.visibility = View.VISIBLE` — `MainActivity.kt:209-213`. Confirmed on-device (case 8). |

**Score:** 7/7 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `app/src/main/res/values/strings.xml` | `max_speed_format` ("MAX %1$d"), `reset_max_button` ("Azzera massimo"), Italian, no hardcoded strings | ✓ VERIFIED | Both strings present exactly as specified (lines 11-12); existing strings untouched. |
| `app/src/main/res/layout/activity_main.xml` | `maxSpeedText` (top-left, 22sp, GONE) + `resetMaxButton` (below it, 48dp min, GONE) | ✓ VERIFIED | Both elements present with exact constraints (`layout_constraintStart_toStartOf="parent"`, `layout_constraintTop_toTopOf="parent"` / `layout_constraintTop_toBottomOf="@id/maxSpeedText"`), no autosize/textStyle on `maxSpeedText`, `minHeight="48dp"` on button. `messageText`/`unitText`/`retryButton` unchanged. |
| `app/src/main/java/com/sed/tachimetro/maxspeed/MaxSpeedReducer.kt` | Pure functions `reduceMax`, `sanitizePersistedMax` | ✓ VERIFIED | Both functions present, framework-free (no `android.*` imports), matches D-07 monotonic-growth + T-04-01 sanitization spec exactly. |
| `app/src/main/java/com/sed/tachimetro/maxspeed/MaxSpeedStore.kt` | SharedPreferences wrapper read()/write() for a single Int | ✓ VERIFIED | `getSharedPreferences(PREFS_NAME, MODE_PRIVATE)`, `putInt(KEY_MAX_SPEED, value).apply()` (no `.commit()`), `read()` sanitizes via `sanitizePersistedMax`. |
| `app/src/test/java/com/sed/tachimetro/maxspeed/MaxSpeedReducerTest.kt` | JVM tests covering all 8 behavior cases | ✓ VERIFIED | 8 tests present covering all specified cases (D-07 growth/non-decrease/negative-clamp, sanitize valid/zero/negative). Re-ran during verification: `BUILD SUCCESSFUL`. |
| `app/src/main/java/com/sed/tachimetro/MainActivity.kt` | Wiring: startup read, update-in-updatePlaceholder, reset handler, insets, visibility | ✓ VERIFIED | All wiring present (see Key Link Verification below). `./gradlew.bat :app:assembleDebug` → `BUILD SUCCESSFUL` (re-run during verification). |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|----|--------|---------|
| `MainActivity.updatePlaceholder(Reading)` | `reduceMax` + `MaxSpeedStore.write` | Comparison of `state.kmh` against current max, immediate persistence (D-07) | ✓ WIRED | `MainActivity.kt:248-252`: `reduceMax(currentMax, state.kmh)` computed, `maxSpeedStore.write(currentMax)` called only when the max actually changes. |
| `resetMaxButton` click | `MaxSpeedStore.write(0)` + `updateMaxArea()` | `setOnClickListener { onResetMaxClicked() }` (D-04/D-08) | ✓ WIRED | `MainActivity.kt:90, 260-264`. |
| `onCreate()` | `MaxSpeedStore.read()` | Read persisted max before GPS collection starts (D-09) | ✓ WIRED | `MainActivity.kt:94-96`, placed before `gpsSpeedProvider = GpsSpeedProvider(...)` and `lifecycleScope.launch { ... }`. |
| Device build | Observed behavior (display, reset, persistence post-close/reboot) | On-device human verification | ✓ WIRED | 04-02-SUMMARY.md documents all 9 checklist cases tested on Pixel 10 Pro emulator, including `adb reboot`, approved with "approvato". |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|---------------------|--------|
| `maxSpeedText` | `currentMax` (Int field) | `MaxSpeedStore.read()` at startup, then `reduceMax(currentMax, state.kmh)` on every GPS `Reading` | Yes — sourced from real SharedPreferences read + live GPS-driven `reduceMax` recompute, no hardcoded/static values | ✓ FLOWING |
| `resetMaxButton` visibility | `currentMax > 0` | Same `currentMax` field, updated by `onResetMaxClicked()` | Yes — tied to the same live state, no disconnected prop | ✓ FLOWING |

### Behavioral Spot-Checks

Step 7b: SKIPPED (no automatable runnable entry point — this is a full-screen Android UI app requiring a device/emulator with GPS simulation; the equivalent behavioral verification was already performed as a mandatory human-verify checkpoint in plan 04-02, covering all 9 behavior cases including the critical `adb reboot` persistence test, and was approved by the user).

Automated checks that were run during this verification:
| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Unit logic (reduceMax/sanitizePersistedMax) correctness | `./gradlew.bat :app:testDebugUnitTest --tests "com.sed.tachimetro.maxspeed.MaxSpeedReducerTest"` | BUILD SUCCESSFUL, all 8 tests green | ✓ PASS |
| Full app compiles with new wiring | `./gradlew.bat :app:assembleDebug` | BUILD SUCCESSFUL | ✓ PASS |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|------------|-------------|--------|----------|
| MAX-01 | 04-01-PLAN.md, 04-02-PLAN.md | L'utente vede la velocità massima raggiunta dall'ultimo azzeramento in un'area secondaria dello schermo | ✓ SATISFIED | `maxSpeedText` wired + persisted + human-confirmed on-device. |
| MAX-02 | 04-01-PLAN.md, 04-02-PLAN.md | L'utente può premere un pulsante "Azzera massimo" per azzerare il valore e iniziare una nuova misurazione | ✓ SATISFIED | `resetMaxButton` click handler immediate reset + persist + human-confirmed. |
| MAX-03 | 04-01-PLAN.md, 04-02-PLAN.md | La velocità massima persiste su disco e sopravvive a chiusura app e riavvio del telefono | ✓ SATISFIED | `MaxSpeedStore` SharedPreferences persistence + human-confirmed via `adb reboot` test, approved. |

No orphaned requirements: REQUIREMENTS.md maps only MAX-01, MAX-02, MAX-03 to Phase 4, and all three are declared in both plans' frontmatter `requirements` fields.

Note (non-blocking, documentation housekeeping): `.planning/REQUIREMENTS.md` still shows MAX-01/02/03 as unchecked `[ ]` and traceability status "Pending" — this is a tracking-doc update that normally happens as part of phase closeout, not a code/behavior gap. No action needed from this verification.

### Anti-Patterns Found

None. Scanned `MainActivity.kt`, `MaxSpeedReducer.kt`, `MaxSpeedStore.kt`, `MaxSpeedReducerTest.kt`, `activity_main.xml`, `strings.xml` for TODO/FIXME/placeholder/stub patterns, empty handlers, hardcoded empty state, and console-log-only implementations. The only regex hits were the pre-existing method name `updatePlaceholder` (false positive on "Placeholder" substring, unrelated to phase 4 — inherited from Phase 2/3). No blockers, no warnings.

### Human Verification Required

None outstanding. The mandatory human-verify checkpoint (plan 04-02) was already executed and approved: all 9 behavior cases (initial-hidden state, MAX appearance, monotonic growth/non-decrease, immediate reset, persistence across app-close, persistence across phone reboot via `adb reboot`, insets in both orientations, mutual exclusivity with `retryButton`, Italian copy/no animation) were tested on a Pixel 10 Pro emulator and confirmed with "approvato" in 04-02-SUMMARY.md.

### Gaps Summary

No gaps found. All observable truths derived from the Roadmap Success Criteria and PLAN must_haves are verified at all four levels (exists, substantive, wired, data-flowing). Build and unit tests re-run clean during this verification (`assembleDebug` and `testDebugUnitTest` both `BUILD SUCCESSFUL`). All git commit hashes referenced in 04-01-SUMMARY.md exist in the repository. The critical reboot-persistence behavior — which cannot be verified by static code inspection alone — was already confirmed via a documented, approved human-verify checkpoint (04-02).

---

*Verified: 2026-07-10*
*Verifier: Claude (gsd-verifier)*
