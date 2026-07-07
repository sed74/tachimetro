---
phase: 02-motore-gps
plan: 03
subsystem: android-app
tags: [kotlin, coroutines, lifecycle, StateFlow, android-activity]

# Dependency graph
requires:
  - phase: 02-motore-gps (plan 02)
    provides: "com.sed.tachimetro.gps package (SpeedState sealed model, GpsSpeedProvider callbackFlow bridge exposing StateFlow<SpeedState>)"
  - phase: 01-fondamenta-permessi-e-avvio (plan 02)
    provides: "MainActivity with ACCESS_FINE_LOCATION permission flow (checkAndRequestPermission/showReady/showDenied) and the black placeholder screen (messageText/retryButton)"
provides:
  - "MainActivity wired to GpsSpeedProvider via lifecycleScope.launch { repeatOnLifecycle(STARTED) { ... } }, gated on permission grant"
  - "updatePlaceholder(SpeedState) rendering Searching/NoSignal as the Italian searching_gps_signal string and Reading as '<N> km/h'"
  - "New Italian string resource searching_gps_signal"
  - "Human-confirmed live GPS speed path via emulator Route Playback (D-10) -- GPS-01/GPS-02 validated end-to-end"
affects: [03-ui-tachimetro]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "repeatOnLifecycle(Lifecycle.State.STARTED) registered once in onCreate via lifecycleScope.launch{} as the sole GPS start/stop mechanism (D-07) -- no manual onStart()/onStop() overrides"
    - "Permission gate lives inside the repeatOnLifecycle block (checkSelfPermission == PERMISSION_GRANTED) so the existing Phase-1 permission flow stays the single source of truth for when GPS collection may run"

key-files:
  created: []
  modified:
    - app/src/main/res/values/strings.xml
    - app/src/main/java/com/sed/tachimetro/MainActivity.kt

key-decisions:
  - "Placed the GpsSpeedProvider instantiation and repeatOnLifecycle collector registration in onCreate() before checkAndRequestPermission(), matching 02-PATTERNS.md's onCreate wiring precedent; the collector's own internal grant check means it simply no-ops until permission is confirmed, so ordering relative to checkAndRequestPermission() has no functional effect."
  - "updatePlaceholder() also forces retryButton.visibility = View.GONE on every state change, mirroring showReady()'s existing behavior, so a stray visible retry button can never linger once GPS states start rendering."

patterns-established: []

requirements-completed: [GPS-01, GPS-02]

# Metrics
duration: ~20 min (Task 1 code + Task 2 human verification)
completed: 2026-07-07
---

# Phase 2 Plan 3: MainActivity GPS Wiring Summary

**MainActivity now instantiates GpsSpeedProvider and collects its StateFlow<SpeedState> through a lifecycle-scoped repeatOnLifecycle(STARTED) block, rendering the Phase-1 black placeholder as "Ricerca segnale GPS..." or "<N> km/h"; live Route Playback verification on a Pixel 10 Pro AVD emulator confirmed the full GPS speed path end-to-end.**

## Status: COMPLETE -- both tasks done, checkpoint approved by user

Task 1 (`type="auto"`) is implemented, build/lint-verified, and committed. Task 2 (`checkpoint:human-verify`, `gate="blocking"`) was verified by the user on a running Pixel 10 Pro AVD emulator via Extended Controls Route Playback: the response was "approvato" (approved), confirming speed tracks the route with varying whole-number km/h ~1/sec, shows "0 km/h" when stopped, and reverts to "Ricerca segnale GPS..." on signal loss.

## Performance

- **Duration:** ~20 min (Task 1 implementation + Task 2 human verification)
- **Started:** 2026-07-07 (worktree setup + context read)
- **Tasks:** 2 of 2 completed
- **Files modified:** 2

## Accomplishments
- `strings.xml`: added `searching_gps_signal` = "Ricerca segnale GPS..." (Italian, snake_case, matching the 6 existing entries' convention)
- `MainActivity.kt`: added a third import group (`androidx.lifecycle.*`, `kotlinx.coroutines.launch`, `com.sed.tachimetro.gps.*`) after the existing `androidx.*` group
- `MainActivity.kt`: added `private lateinit var gpsSpeedProvider: GpsSpeedProvider` field
- `MainActivity.kt`: `onCreate()` now instantiates `GpsSpeedProvider(this)` and registers `lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { ... } }` as the single start/stop mechanism (D-07) -- no manual `onStart()`/`onStop()` overrides added
- The collector body gates on `ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED` before calling `gpsSpeedProvider.state.collect { ... }`, keeping the Phase-1 permission flow authoritative
- Added `updatePlaceholder(state: SpeedState)`: `Searching`/`NoSignal` -> `getString(R.string.searching_gps_signal)`; `Reading(kmh)` -> `"$kmh km/h"` (whole-number interpolation only, per D-09); also hides `retryButton`
- `showReady()`, `showDenied()`, `onResume()`, `checkAndRequestPermission()`, `onRetryClicked()`, `openAppSettings()` all left untouched -- Phase-1 permission flow logic is unmodified
- `AndroidManifest.xml` unchanged -- no new permissions added
- `./gradlew.bat assembleDebug lintDebug` -> BUILD SUCCESSFUL (both tasks green)
- **Live Route Playback verification (Task 2, D-10):** app installed and run on a Pixel 10 Pro AVD (Play Store image). User confirmed via Extended Controls -> Location -> Routes -> Play Route: speed tracks the simulated route with varying whole-number km/h (~1 update/sec), reads "0 km/h" when stopped, and reverts to "Ricerca segnale GPS..." on signal loss/startup. GPS-01 and GPS-02 are now validated end-to-end.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add no-signal string + wire GpsSpeedProvider collector into MainActivity** - `4016d6e` (feat)
2. **Task 2: Verify live speed on emulator Route Playback (D-10)** - checkpoint:human-verify, no code commit (verification-only task; approved by user with "approvato" via Extended Controls Route Playback on a Pixel 10 Pro AVD)

## Files Created/Modified
- `app/src/main/res/values/strings.xml` - Added `searching_gps_signal` Italian string ("Ricerca segnale GPS...")
- `app/src/main/java/com/sed/tachimetro/MainActivity.kt` - Added `GpsSpeedProvider` field, `repeatOnLifecycle(STARTED)`-scoped collector registered once in `onCreate()`, permission-gated collection, and `updatePlaceholder(SpeedState)` rendering method

## Decisions Made
- Followed the plan's locked interface contract exactly: `GpsSpeedProvider(this)` instantiated in `onCreate`, `state` collected via `lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { ... } }`.
- Registered the collector before calling `checkAndRequestPermission()` in `onCreate` (rather than after) -- since the collector's own internal permission check makes it a no-op until granted, this ordering choice has no behavioral effect but keeps all one-time `onCreate` setup grouped together before the permission-triggering call, matching the existing file's flow (view-binding -> provider setup -> permission check).
- Also cleared `retryButton.visibility` inside `updatePlaceholder()` (not just `showReady()`), so any transition into a GPS-driven state always leaves a clean placeholder with no stray retry button, consistent with the plan's acceptance criteria wording ("retryButton.visibility = View.GONE when showing a speed/searching state").

## Deviations from Plan

None - both tasks executed exactly as written. No auto-fixes, no bugs found, no missing critical functionality, no blocking issues, and no architectural changes were required. `AndroidManifest.xml` was left untouched as required; no new permissions were added.

One environment-only action (not a plan deviation, consistent with how 02-02 and 01-02 handled the same file): `local.properties` (gitignored, machine-local SDK path) was created in this fresh worktree, copied from the main repo's `local.properties`, so `./gradlew.bat assembleDebug lintDebug` could run. It remains untracked/ignored and was never committed.

## Issues Encountered

None.

## User Setup Required

None. Task 2's checkpoint required a human to run a live emulator Route Playback verification session -- this was completed by the user directly on a Pixel 10 Pro AVD, with the outcome "approvato" (approved).

## Checkpoint: Task 2 Resolution

- **Type:** checkpoint:human-verify (gate="blocking")
- **What was verified:** Live speed reading via GPS on an emulator Route Playback session, per D-10.
- **Environment:** Pixel 10 Pro AVD (Play Store system image), app installed and run.
- **Method:** Extended Controls -> Location -> Routes tab -> Play Route.
- **Outcome:** User response "approvato". Confirmed: speed tracks the route (varying whole-number km/h ~1/sec), shows "0 km/h" when stopped (not a noise value), and reverts to "Ricerca segnale GPS..." on signal loss/startup.
- **Result:** GPS-01 and GPS-02 requirements validated end-to-end; checkpoint resolved, no code changes required.

## Threat Flags

None. This plan's own `<threat_model>` (T-02-ID, T-02-EP, T-02-D) was reviewed against the implementation:
- T-02-ID (Information Disclosure): confirmed -- `updatePlaceholder` only ever receives `SpeedState` (derived `Int` km/h or a sealed marker), never raw `Location`/lat-lng. No `Log.*` calls were added anywhere in `MainActivity.kt`.
- T-02-EP (Elevation of Privilege): confirmed -- `AndroidManifest.xml` is byte-for-byte unchanged; only the pre-existing `ACCESS_FINE_LOCATION` permission is checked, no coarse/background scope requested.
- T-02-D (Denial of Service / battery): confirmed -- collection is scoped entirely inside `repeatOnLifecycle(Lifecycle.State.STARTED)`, so `GpsSpeedProvider`'s `callbackFlow`/`awaitClose { removeLocationUpdates(...) }` teardown fires automatically once the Activity leaves `STARTED`. The Route Playback checkpoint session confirmed a working live GPS path consistent with this lifecycle scoping.

No new/undocumented threat surface was introduced.

## Next Phase Readiness

- Both tasks are code-complete, build/lint-verified, human-verified, and committed (`4016d6e`).
- Requirements GPS-01/GPS-02 are now fully implemented and confirmed by live Route Playback verification.
- Phase 2 (motore-gps) is complete and ready to hand off to Phase 3 (ui-tachimetro), which will build the real full-screen UI on top of the now-verified `SpeedState`-driven placeholder wiring.

---
*Phase: 02-motore-gps*
*Completed: 2026-07-07*

## Self-Check: PASSED

- FOUND: app/src/main/res/values/strings.xml (contains `searching_gps_signal`)
- FOUND: app/src/main/java/com/sed/tachimetro/MainActivity.kt (contains `repeatOnLifecycle`, `GpsSpeedProvider`, `updatePlaceholder`, `km/h`)
- FOUND commit: 4016d6e (feat(02-03): wire GpsSpeedProvider into MainActivity)
- `./gradlew.bat assembleDebug lintDebug` -> BUILD SUCCESSFUL
- Task 2 checkpoint approved by user ("approvato") via Pixel 10 Pro AVD Route Playback session
