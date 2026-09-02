---
phase: 09-permesso-di-localizzazione-dallo-schermo-auto
plan: 01
subsystem: android-auto-permissions
tags: [android-auto, car-app-library, permissions, shared-preferences, kotlin, tdd, sealed-class]

# Dependency graph
requires:
  - phase: 08-fondamenta-condivise-e-velocit-sullo-schermo-auto
    provides: "SpeedScreen.kt scaffold with T-08-08 defensive permission gate (to be replaced in Plan 02), CarSpeedContent.kt sealed content model kept orthogonal"
provides:
  - "CarPermissionState sealed model (Granted/NotRequested/Waiting/Denied) framework-free"
  - "resolveCarPermissionState(granted, denialCount) pure resolver, denialCount >= 2 == permanent"
  - "sanitizeDenialCount() clamp for tampered/negative persisted values"
  - "CarPermissionDenialStore persisting the car-screen denial counter in tachimetro_prefs"
  - "Three Italian car-screen strings: car_check_your_phone, car_permission_denied, car_permission_denied_permanent"
affects: [09-02-wiring-speedscreen, 09-03]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Pure top-level resolver function mirroring MaxSpeedReducer.kt (no class wrapper, no Android imports, exhaustive when with a meaningful final branch)"
    - "Sealed state model mirroring SpeedState.kt (data object for payload-less states, data class for the one carrying data)"
    - "SharedPreferences counter store reusing MaxSpeedStore.PREFS_NAME with a dedicated key, following ScreenOnPreferenceStore precedent"

key-files:
  created:
    - app/src/main/java/com/sed/tachimetro/car/CarPermissionState.kt
    - app/src/main/java/com/sed/tachimetro/car/CarPermissionDenialStore.kt
    - app/src/test/java/com/sed/tachimetro/car/CarPermissionStateTest.kt
  modified:
    - app/src/main/res/values/strings.xml

key-decisions:
  - "No new SharedPreferences file: CarPermissionDenialStore reuses MaxSpeedStore.PREFS_NAME (tachimetro_prefs) with a dedicated KEY_DENIAL_COUNT key, per plan/pattern-map instruction"
  - "resolveCarPermissionState() never produces CarPermissionState.Waiting -- that is a transitional state only SpeedScreen (Plan 02) can set while awaiting the requestPermissions() callback"

patterns-established:
  - "Car-screen permission state is resolved purely from (granted flag, persisted denial count) -- no Activity reference held anywhere in this package"

requirements-completed: []

# Metrics
duration: 12min
completed: 2026-09-02
---

# Phase 09 Plan 01: Modello e persistenza del permesso lato schermo auto Summary

**CarPermissionState sealed model + resolveCarPermissionState() pure resolver (denialCount >= 2 = permanent) + CarPermissionDenialStore persisting the car-screen denial counter in the shared tachimetro_prefs file, plus three Italian car-screen strings for D-01/D-02/D-04.**

## Performance

- **Duration:** ~12 min
- **Started:** 2026-09-02T11:39:57Z
- **Completed:** 2026-09-02T11:45:09Z
- **Tasks:** 3 completed
- **Files modified:** 4 (3 created, 1 modified)

## Accomplishments
- Built the pure, testable foundation for D-04 (first-denial vs. permanent-denial distinction) with zero dependency on any Activity, replacing the unavailable `Activity.shouldShowRequestPermissionRationale()`
- Persisted the car-screen denial counter in the existing single `tachimetro_prefs` SharedPreferences file (no new file, no Room/DataStore), with mandatory sanitization on every read
- Added the three Italian car-screen strings required for the waiting/denied/permanently-denied states, each shorter than its phone equivalent per the established Phase 8 convention

## Task Commits

Each task was committed atomically:

1. **Task 1: Modello CarPermissionState e resolver puro (D-04)** - TDD (test -> feat)
   - `21a97b7` (test): add failing test for CarPermissionState resolver
   - `d3faf6c` (feat): implement CarPermissionState model and resolver
2. **Task 2: CarPermissionDenialStore -- contatore persistito dei rifiuti** - `40b3e98` (feat)
3. **Task 3: Stringhe italiane dedicate allo schermo auto (D-01, D-02, D-04)** - `c818459` (feat)

**Plan metadata:** committed separately after this SUMMARY.

## Files Created/Modified
- `app/src/main/java/com/sed/tachimetro/car/CarPermissionState.kt` - Sealed `CarPermissionState` model (`Granted`/`NotRequested`/`Waiting`/`Denied(permanent)`) + `resolveCarPermissionState()` + `sanitizeDenialCount()`, framework-free
- `app/src/main/java/com/sed/tachimetro/car/CarPermissionDenialStore.kt` - Persists the car-screen denial counter (single Int) in `tachimetro_prefs`, sanitizing on read, incrementing on `recordDenial()`
- `app/src/test/java/com/sed/tachimetro/car/CarPermissionStateTest.kt` - 10 plain-JVM `assertEquals` tests locking every branch of the resolver and the sanitizer
- `app/src/main/res/values/strings.xml` - Added `car_check_your_phone`, `car_permission_denied`, `car_permission_denied_permanent`

## Decisions Made
None beyond the plan itself - implementation followed the `MaxSpeedStore`/`MaxSpeedReducer`/`SpeedState`/`ScreenOnPreferenceStore` analogs exactly as directed by `09-PATTERNS.md`.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

While drafting the Task 3 XML comment for `car_permission_denied_permanent`, an inline comment initially contained a literal `--`, which Android's resource compiler rejects inside XML comments (`aapt` error: "The string \"--\" is not permitted within comments"). Caught immediately by the task's own `assembleDebug` verification step, before any commit was made; fixed by rewording the comment with a comma instead of `--`. No functional or string-content impact - the visible string values match the plan exactly.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- `CarPermissionState`, `resolveCarPermissionState()`, `sanitizeDenialCount()`, and `CarPermissionDenialStore` are ready to be wired into `SpeedScreen.onGetTemplate()` and the `CarContext.requestPermissions()` callback in Plan 02
- The three new strings (`car_check_your_phone`, `car_permission_denied`, `car_permission_denied_permanent`) are ready to be resolved by `SpeedScreen` in Plan 02; `R.string.retry`/`R.string.open_settings` remain the reused Action titles
- **AA-04 requirement remains Pending** - this plan built only the pure/persistence foundation; the requirement's actual user-facing behavior (explicit request from the car screen instead of staying blank) is delivered by Plan 02 (wiring `SpeedScreen`) and verified by Plan 03. `requirements-completed` intentionally left empty in this SUMMARY; do not mark AA-04 complete in REQUIREMENTS.md/ROADMAP.md until the wiring plan lands
- No blockers for Plan 02

---
*Phase: 09-permesso-di-localizzazione-dallo-schermo-auto*
*Completed: 2026-09-02*

## Self-Check: PASSED

- FOUND: app/src/main/java/com/sed/tachimetro/car/CarPermissionState.kt
- FOUND: app/src/main/java/com/sed/tachimetro/car/CarPermissionDenialStore.kt
- FOUND: app/src/test/java/com/sed/tachimetro/car/CarPermissionStateTest.kt
- FOUND: app/src/main/res/values/strings.xml contains car_check_your_phone
- FOUND: commit 21a97b7 (test: RED)
- FOUND: commit d3faf6c (feat: GREEN CarPermissionState)
- FOUND: commit 40b3e98 (feat: CarPermissionDenialStore)
- FOUND: commit c818459 (feat: strings.xml)
- FOUND: commit d5c2652 (docs: SUMMARY.md)
