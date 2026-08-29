---
phase: 06-indicatore-di-ricarica
plan: 02
subsystem: charging-state
tags: [kotlin, coroutines, stateflow, broadcastreceiver, battery, tdd]

# Dependency graph
requires:
  - phase: 02-motore-gps
    provides: "GpsSpeedProvider pattern (callbackFlow + StateFlow + WR-04 applicationContext + close()) replicated 1:1 for the charging domain"
provides:
  - "com.sed.tachimetro.charging.ChargingState sealed model (Hidden/Pulsing/Full)"
  - "com.sed.tachimetro.charging.deriveChargingState pure function, 6 JVM unit tests"
  - "com.sed.tachimetro.charging.ChargingStateProvider — continuous StateFlow<ChargingState> fed by ACTION_BATTERY_CHANGED"
affects: ["06-03 (MainActivity wiring + fill animation)", "06-01 (layout/drawables, no code dependency)"]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Sticky broadcast wrapped in callbackFlow (no onStart{emit(null)}/ticker needed — ACTION_BATTERY_CHANGED delivers current value immediately on registration, unlike GPS's fix-based flow)"
    - "ContextCompat.registerReceiver(..., RECEIVER_NOT_EXPORTED) for dynamic, unexported receiver registration (Android 13+ compliant, no manifest declaration)"

key-files:
  created:
    - app/src/main/java/com/sed/tachimetro/charging/ChargingState.kt
    - app/src/main/java/com/sed/tachimetro/charging/ChargingStateProvider.kt
    - app/src/test/java/com/sed/tachimetro/charging/ChargingStateProviderStateTest.kt
  modified: []

key-decisions:
  - "ChargingState.kt and deriveChargingState were split into a RED commit (model + failing test) and a GREEN commit (pure function), matching the plan's Task 1 TDD sequencing"
  - "No staleness/ticker logic in ChargingStateProvider (unlike GpsSpeedProvider) — ACTION_BATTERY_CHANGED is sticky, so there is no 'no signal' concept for this broadcast"

patterns-established:
  - "Charging domain (charging/ package) mirrors the gps/ domain 1:1: sealed state model, pure derive function with top-level placement + KDoc, provider class with WR-04 applicationContext, callbackFlow-wrapped Android callback API, stateIn(WhileSubscribed()), symmetric close()"

requirements-completed: [CHRG-01]

# Metrics
duration: ~15min
completed: 2026-08-29
---

# Phase 6 Plan 2: Charging Domain (ChargingState + ChargingStateProvider) Summary

**Continuous `StateFlow<ChargingState>` (Hidden/Pulsing/Full) fed by the sticky `ACTION_BATTERY_CHANGED` broadcast, registered as unexported and deregistered on `close()`, mirroring `GpsSpeedProvider`'s reactive pattern exactly.**

## Performance

- **Duration:** ~15 min
- **Completed:** 2026-08-29T17:24:24Z
- **Tasks:** 2 completed
- **Files modified:** 3 (all new)

## Accomplishments
- `ChargingState` sealed model (`Hidden`/`Pulsing`/`Full`) created test-first, mirroring `SpeedState`'s shape and KDoc style
- `deriveChargingState(batteryStatus: Int): ChargingState` — pure, top-level, fail-closed (any unrecognized/missing status maps to `Hidden`), covered by 6 green JVM unit tests (RED confirmed before implementation)
- `ChargingStateProvider(context: Context)` exposes `state: StateFlow<ChargingState>` fed by a `callbackFlow`-wrapped, dynamically-registered `BroadcastReceiver` (`RECEIVER_NOT_EXPORTED`), deregistered in `awaitClose`, with a symmetric `close()` cancelling its own `CoroutineScope`
- Zero changes to `MainActivity.kt` or `AndroidManifest.xml` — `isDeviceCharging()` one-shot check remains untouched, no new permission introduced

## Task Commits

Each task was committed atomically (Task 1 split into RED/GREEN per TDD protocol):

1. **Task 1 (RED): failing test + model** - `da26ff0` (test) — `ChargingState.kt` sealed model + `ChargingStateProviderStateTest.kt` referencing not-yet-implemented `deriveChargingState`; confirmed failing (compile error) before implementation
2. **Task 1 (GREEN): pure function** - `05ece10` (feat) — `deriveChargingState` implemented in `ChargingStateProvider.kt`; all 6 tests pass
3. **Task 2: ChargingStateProvider class** - `7d8db1c` (feat) — continuous `StateFlow<ChargingState>` via `callbackFlow` + `ContextCompat.registerReceiver(RECEIVER_NOT_EXPORTED)` + `stateIn(WhileSubscribed())` + `close()`

_TDD task: RED → GREEN sequence verified via `./gradlew.bat :app:testDebugUnitTest` before/after implementation, per plan Task 1 `tdd="true"`._

## Files Created/Modified
- `app/src/main/java/com/sed/tachimetro/charging/ChargingState.kt` - Sealed model: `Hidden` (not connected), `Pulsing` (`BATTERY_STATUS_CHARGING`), `Full` (`BATTERY_STATUS_FULL`)
- `app/src/main/java/com/sed/tachimetro/charging/ChargingStateProvider.kt` - `deriveChargingState` pure function + `ChargingStateProvider` class (StateFlow, BroadcastReceiver wiring, `close()`)
- `app/src/test/java/com/sed/tachimetro/charging/ChargingStateProviderStateTest.kt` - 6 JVM unit tests covering all `BATTERY_STATUS_*` values and missing `EXTRA_STATUS`

## Decisions Made
- Followed the plan's exact interface contract (`ChargingState`, `ChargingStateProvider(context: Context)`, `deriveChargingState`) verbatim — these are binding names consumed by Plan 03 (MainActivity wiring)
- Split Task 1 into two commits (RED test+model, GREEN implementation) rather than one, to make the TDD gate sequence (`test(...)` before `feat(...)`) explicit in git history for the plan-level TDD compliance check

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Created missing `local.properties` in the worktree**
- **Found during:** Task 1, first `./gradlew.bat` invocation
- **Issue:** `local.properties` is gitignored (per `.gitignore` lines 3/15) and therefore absent from the fresh git worktree, causing all Gradle tasks to fail with "SDK location not found"
- **Fix:** Created `local.properties` in the worktree root with the same `sdk.dir=D:\Android\SDK` value already present in the main repo checkout's `local.properties` (a local machine-specific file, not a secret/credential)
- **Files modified:** `local.properties` (gitignored, not committed — this is expected local dev environment setup, not a tracked plan artifact)
- **Verification:** Subsequent `./gradlew.bat :app:testDebugUnitTest` ran successfully
- **Committed in:** N/A — gitignored, intentionally not committed

---

**Total deviations:** 1 auto-fixed (1 blocking, environment-only, no tracked file changed)
**Impact on plan:** No impact on plan scope or deliverables — purely a local worktree environment fix required to run Gradle at all.

## Issues Encountered
- The worktree's git branch (`worktree-agent-a414dd99452f91717`) was initially based on a stale commit (`ec0295f`, same as `master`) rather than the expected phase-06 planning base (`1def349`, "docs(06): create phase plan"), so `.planning/phases/06-indicatore-di-ricarica/` was absent at the start of the session. Corrected via `git reset --hard 1def349be254299630fb07247cb1b2a354580190` per the mandatory `worktree_branch_check` step (working tree was clean, no local commits existed yet, so the reset was lossless).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Plan 03 (MainActivity wiring) can now instantiate `ChargingStateProvider(applicationContext)`, collect `.state` via `repeatOnLifecycle(STARTED)`, and drive the fill animation off `ChargingState.Hidden`/`Pulsing`/`Full` exactly as specified in the plan's `<interfaces>` contract
- No blockers. `ChargingState`/`ChargingStateProvider`/`deriveChargingState` names and signatures match the plan's binding interface contract verbatim

---
*Phase: 06-indicatore-di-ricarica*
*Completed: 2026-08-29*
