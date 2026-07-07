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

requirements-completed: []  # GPS-01/GPS-02 remain pending until the Task 2 human-verify checkpoint is approved on a Route Playback emulator session

# Metrics
duration: pending (Task 2 checkpoint not yet resolved)
completed: 2026-07-07
---

# Phase 2 Plan 3: MainActivity GPS Wiring Summary

**MainActivity now instantiates GpsSpeedProvider and collects its StateFlow<SpeedState> through a lifecycle-scoped repeatOnLifecycle(STARTED) block, rendering the Phase-1 black placeholder as "Ricerca segnale GPS..." or "<N> km/h"; live Route Playback verification on an emulator is the one remaining step (Task 2 checkpoint).**

## Status: IN PROGRESS -- Task 1 complete, Task 2 (checkpoint:human-verify) awaiting user verification

Task 1 (`type="auto"`) is implemented, build/lint-verified, and committed. Task 2 is a `checkpoint:human-verify` gate requiring a human to run the app on a Play-Store emulator image and drive an Extended Controls Route Playback session -- this cannot be automated or fabricated by the executor (per plan and worktree instructions) and is returned to the orchestrator as a checkpoint.

## Performance

- **Duration (Task 1 only):** ~15 min
- **Started:** 2026-07-07 (worktree setup + context read)
- **Tasks:** 1 of 2 completed (Task 2 pending human verification)
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

## Task Commits

Each task was committed atomically:

1. **Task 1: Add no-signal string + wire GpsSpeedProvider collector into MainActivity** - `4016d6e` (feat)
2. **Task 2: Verify live speed on emulator Route Playback (D-10)** - checkpoint:human-verify, no code commit (awaiting user verification, see Checkpoint below)

**Plan metadata:** not yet finalized -- will be completed once Task 2 is approved (per worktree policy, this executor does not touch STATE.md/ROADMAP.md; the orchestrator finalizes plan metadata after merge).

## Files Created/Modified
- `app/src/main/res/values/strings.xml` - Added `searching_gps_signal` Italian string ("Ricerca segnale GPS...")
- `app/src/main/java/com/sed/tachimetro/MainActivity.kt` - Added `GpsSpeedProvider` field, `repeatOnLifecycle(STARTED)`-scoped collector registered once in `onCreate()`, permission-gated collection, and `updatePlaceholder(SpeedState)` rendering method

## Decisions Made
- Followed the plan's locked interface contract exactly: `GpsSpeedProvider(this)` instantiated in `onCreate`, `state` collected via `lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { ... } }`.
- Registered the collector before calling `checkAndRequestPermission()` in `onCreate` (rather than after) -- since the collector's own internal permission check makes it a no-op until granted, this ordering choice has no behavioral effect but keeps all one-time `onCreate` setup grouped together before the permission-triggering call, matching the existing file's flow (view-binding -> provider setup -> permission check).
- Also cleared `retryButton.visibility` inside `updatePlaceholder()` (not just `showReady()`), so any transition into a GPS-driven state always leaves a clean placeholder with no stray retry button, consistent with the plan's acceptance criteria wording ("retryButton.visibility = View.GONE when showing a speed/searching state").

## Deviations from Plan

None - Task 1 executed exactly as written. No auto-fixes, no bugs found, no missing critical functionality, no blocking issues, and no architectural changes were required. `AndroidManifest.xml` was left untouched as required; no new permissions were added.

One environment-only action (not a plan deviation, consistent with how 02-02 and 01-02 handled the same file): `local.properties` (gitignored, machine-local SDK path) was created in this fresh worktree, copied from the main repo's `local.properties`, so `./gradlew.bat assembleDebug lintDebug` could run. It remains untracked/ignored and was never committed.

## Issues Encountered

None for Task 1.

## User Setup Required

None - no external service configuration required. Task 2 requires the user (not this executor) to run a live emulator Route Playback verification session -- see Checkpoint below.

## Threat Flags

None. This plan's own `<threat_model>` (T-02-ID, T-02-EP, T-02-D) was reviewed against the implementation:
- T-02-ID (Information Disclosure): confirmed -- `updatePlaceholder` only ever receives `SpeedState` (derived `Int` km/h or a sealed marker), never raw `Location`/lat-lng. No `Log.*` calls were added anywhere in `MainActivity.kt`.
- T-02-EP (Elevation of Privilege): confirmed -- `AndroidManifest.xml` is byte-for-byte unchanged; only the pre-existing `ACCESS_FINE_LOCATION` permission is checked, no coarse/background scope requested.
- T-02-D (Denial of Service / battery): confirmed -- collection is scoped entirely inside `repeatOnLifecycle(Lifecycle.State.STARTED)`, so `GpsSpeedProvider`'s `callbackFlow`/`awaitClose { removeLocationUpdates(...) }` teardown fires automatically once the Activity leaves `STARTED`. Full confirmation of this (background -> foreground behavior) is part of the Task 2 checkpoint's optional step 8.

No new/undocumented threat surface was introduced.

## Next Phase Readiness

- Task 1 is code-complete, build/lint-verified, and committed (`4016d6e`).
- Task 2 (`checkpoint:human-verify`, `gate="blocking"`) requires a human to launch a Play-Store-image emulator (API 30+), grant the location permission, and drive an Extended Controls -> Location -> Routes "Play Route" session to confirm: startup shows "Ricerca segnale GPS...", the display then switches to varying whole-number "<N> km/h" tracking the simulated route (~1 update/sec), reads "0 km/h" when stopped (not a small noise value), reverts to "Ricerca segnale GPS..." after >5s of signal loss, and (optionally) that GPS updates stop while the app is backgrounded and resume on return.
- This is NOT verifiable by the executor -- it requires a running Android emulator with Google Play Services and manual interaction with the Extended Controls UI, which is out of scope for this agent per the plan's own design (D-10) and the parallel-execution instructions.
- Requirements GPS-01/GPS-02 are implemented in code but remain **unconfirmed** until Task 2's checkpoint is approved; do not mark them complete in REQUIREMENTS.md until that happens.

---
*Phase: 02-motore-gps*
*Completed: pending Task 2 checkpoint approval*

## Self-Check: PASSED

- FOUND: app/src/main/res/values/strings.xml (contains `searching_gps_signal`)
- FOUND: app/src/main/java/com/sed/tachimetro/MainActivity.kt (contains `repeatOnLifecycle`, `GpsSpeedProvider`, `updatePlaceholder`, `km/h`)
- FOUND commit: 4016d6e (feat(02-03): wire GpsSpeedProvider into MainActivity)
- `./gradlew.bat assembleDebug lintDebug` -> BUILD SUCCESSFUL
