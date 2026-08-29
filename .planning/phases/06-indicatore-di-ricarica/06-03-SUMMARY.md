---
phase: 06-indicatore-di-ricarica
plan: 03
subsystem: ui
tags: [android, kotlin, valueanimator, clipdrawable, layerdrawable, coroutines, stateflow, window-insets]

# Dependency graph
requires:
  - phase: 06-01
    provides: "chargingIcon ImageView in activity_main.xml, charging_flash_fill.xml LayerDrawable (chargingIconBase/chargingIconFill ids), lime_charging_accent color"
  - phase: 06-02
    provides: "ChargingState sealed model (Hidden/Pulsing/Full) and ChargingStateProvider (StateFlow<ChargingState>, close())"
provides:
  - "MainActivity wired end-to-end: chargingIcon bound, ChargingStateProvider collected reactively, fill animation driven by ChargingState"
  - "resolveChargingFillLayer/startChargingFillAnimation/freezeChargingFillAtFull/stopChargingFillAnimation helpers on ClipDrawable level (0..10000)"
  - "updateChargingIcon(state) exhaustive visibility + animation state machine"
  - "applyBottomLeftWindowInsets(): single listener positioning chargingIcon + keepScreenOnSwitch correctly against system bars/cutouts in both orientations"
affects: ["06-04"]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "ValueAnimator driving a ClipDrawable.level (0..10000) via addUpdateListener, REVERSE repeat mode for symmetric fill/drain -- first animation in the codebase (D-04 exception)"
    - "onStop() override to explicitly cancel a ValueAnimator that repeatOnLifecycle(STARTED) alone would not stop"

key-files:
  created: []
  modified:
    - app/src/main/java/com/sed/tachimetro/MainActivity.kt

key-decisions:
  - "duration = 1250L with ValueAnimator.REVERSE (not 06-PATTERNS.md's duration = 2500 suggestion) to match 06-UI-SPEC.md's binding 2500ms full white-lime-white cycle contract"
  - "Window insets bottom-left group split asymmetrically: bottom inset applied only to keepScreenOnSwitch (the only view anchored to parent at the bottom), left inset applied only to chargingIcon (the new leftmost element) -- listener stays registered on the switch since it is always VISIBLE while the icon is GONE most of the time"

patterns-established:
  - "Fill-animation lifecycle: start (always resets level to 0 first), freeze (cancel + set level to max), stop (cancel + reset level to 0) -- three distinct terminal states matching Hidden/Pulsing/Full 1:1"

requirements-completed: [CHRG-01, CHRG-02]

# Metrics
duration: ~18min
completed: 2026-08-29
---

# Phase 6 Plan 3: MainActivity Charging Indicator Wiring Summary

**`chargingIcon` bound and driven end-to-end by `ChargingStateProvider.state`: a `ValueAnimator`-fed `ClipDrawable` produces a 2500ms bianco→lime→bianco fill loop while charging, freezes solid lime at `BATTERY_STATUS_FULL`, and disappears instantly on unplug — plus a single migrated window-insets listener keeping the icon+switch group clear of navigation bar/cutouts in both orientations.**

## Performance

- **Duration:** ~18 min
- **Started:** 2026-08-29T17:16:00Z (approx, base commit correction + context reading)
- **Completed:** 2026-08-29T17:34:17Z
- **Tasks:** 3/3 completed
- **Files modified:** 1 (`MainActivity.kt`)

## Accomplishments
- `chargingIcon` bound in `onCreate()`, with `resolveChargingFillLayer()` invoked immediately after to resolve the `ClipDrawable` (`R.id.chargingIconFill`) inside the `LayerDrawable`, `mutate()`d first so the level change never bleeds into another instance sharing the drawable's `ConstantState`
- Three animation helpers (`startChargingFillAnimation`/`freezeChargingFillAtFull`/`stopChargingFillAnimation`) implement the exact UI-SPEC contract: 1250ms half-cycle with `ValueAnimator.REVERSE` = 2500ms full loop, `AccelerateDecelerateInterpolator`, always restarting from level 0 on re-plug, never resuming a cached phase
- `ChargingStateProvider(applicationContext)` collected in its own `repeatOnLifecycle(STARTED)` block, unguarded by `permissionGranted` (charging observation needs no runtime permission) — `updateChargingIcon()` is an exhaustive `when` over `Hidden`/`Pulsing`/`Full` with no `else` branch, so the compiler enforces coverage of all three states
- `onStop()` added to explicitly `cancel()` the fill animator — `repeatOnLifecycle(STARTED)` alone stops collection but not an already-running `ValueAnimator`, which would otherwise keep ticking at 60fps in the background (T-06-03-D)
- `chargingStateProvider.close()` added to `onDestroy()` alongside the existing `gpsSpeedProvider.close()`
- `applyScreenSwitchWindowInsets()` renamed to `applyBottomLeftWindowInsets()` and extended to reposition both `chargingIcon` and `keepScreenOnSwitch`: bottom inset only on the switch (the only view anchored to `parent` at the bottom; the icon inherits it via its vertical constraint to the switch), left inset only on the icon (the new leftmost element; the switch's 8dp `marginStart` is an inter-element gap, not a screen-edge distance)

## Task Commits

Each task was committed atomically:

1. **Task 1: Binding della view e helper dell'animazione di riempimento** - `0e06693` (feat)
2. **Task 2: Collettore dello stato di ricarica e teardown** - `0402ef5` (feat)
3. **Task 3: Migrare gli window insets dell'angolo in basso a sinistra al gruppo icona+switch** - `4936401` (refactor)

**Plan metadata:** commit pending (docs: complete plan, added by executor after this summary)

## Files Created/Modified
- `app/src/main/java/com/sed/tachimetro/MainActivity.kt` - Added `chargingIcon`/`chargingFillLayer`/`chargingFillAnimator`/`chargingStateProvider` fields, `CHARGING_FILL_LEVEL_MAX`/`CHARGING_FILL_HALF_CYCLE_MS` companion constants, four animation helpers, `updateChargingIcon()`, a second `repeatOnLifecycle(STARTED)` collector block, `onStop()` override, `onDestroy()` teardown addition, and `applyBottomLeftWindowInsets()` (renamed/extended from `applyScreenSwitchWindowInsets()`)

## Decisions Made
- No deviations from the plan's binding interface contract (`ChargingState`, `ChargingStateProvider`) or animation timing (1250ms half-cycle / `ValueAnimator.REVERSE` per UI-SPEC, correcting 06-PATTERNS.md's `duration = 2500` suggestion which would have produced an incorrect 5000ms cycle)
- Window insets: kept the listener registered on `keepScreenOnSwitch` rather than moving it to `chargingIcon`, exactly per the plan's rationale (switch is always `VISIBLE`, icon is `GONE` most of the time — avoids any dependency on insets dispatch behavior toward `GONE` children)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Created missing `local.properties` in the worktree**
- **Found during:** Pre-Task-1 build verification (`./gradlew.bat :app:assembleDebug`)
- **Issue:** `local.properties` is gitignored and therefore absent from this fresh git worktree, causing Gradle to fail with "SDK location not found" — same issue documented in both 06-01-SUMMARY.md and 06-02-SUMMARY.md for their respective worktrees
- **Fix:** Created `local.properties` in the worktree root with the same `sdk.dir=D:\Android\SDK` value from the main repo checkout
- **Files modified:** `local.properties` (gitignored, not committed)
- **Verification:** All three tasks' `./gradlew.bat :app:assembleDebug [:app:testDebugUnitTest]` invocations succeeded (`BUILD SUCCESSFUL`)
- **Committed in:** N/A — gitignored, intentionally not committed

---

**Total deviations:** 1 auto-fixed (1 blocking, environment-only, no tracked file changed)
**Impact on plan:** No impact on plan scope, code, or commit contents — purely a local worktree environment fix required to run Gradle at all.

## Issues Encountered
None beyond the deviation documented above. The worktree's base commit had drifted from the expected phase-06 base (`ffeca9a...`) to a stale `master`-equivalent commit at agent startup; corrected via the mandatory `worktree_branch_check` `git reset --hard` step before any file reads or edits (working tree was clean, no local commits existed yet, so the reset was lossless).

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- CHRG-01 and CHRG-02 are both fully implemented and wired: the charging icon appears/disappears instantly with power connection state, pulses with a 2500ms bianco→lime→bianco loop while charging, and freezes solid lime at 100% battery
- `git diff --name-only` across the whole plan shows only `app/src/main/java/com/sed/tachimetro/MainActivity.kt` modified, as required by the plan's `<verification>` block
- `showReady`, `showDenied`, `updatePlaceholder`, `updateMaxArea`, `isDeviceCharging`, `applyUnitTextWindowInsets`, `applyMaxAreaWindowInsets` are all untouched (confirmed via `git diff` hunk inspection)
- No blockers for Plan 06-04 (or phase closure) — build green, unit tests green, threat register mitigations (T-06-03-D, T-06-03-D2, T-06-03-T, T-06-03-E) all applied as specified
- Manual on-device verification (visual fill animation, actual charge/unplug/full-battery transitions, insets on a real cutout device) has NOT been performed in this worktree session — recommended before considering CHRG-01/CHRG-02 fully validated end-to-end on hardware

---
*Phase: 06-indicatore-di-ricarica*
*Completed: 2026-08-29*

## Self-Check: PASSED

- FOUND: app/src/main/java/com/sed/tachimetro/MainActivity.kt
- FOUND: .planning/phases/06-indicatore-di-ricarica/06-03-SUMMARY.md
- FOUND commit: 0e06693 (Task 1)
- FOUND commit: 0402ef5 (Task 2)
- FOUND commit: 4936401 (Task 3)
- FOUND commit: 2564e99 (SUMMARY.md)
