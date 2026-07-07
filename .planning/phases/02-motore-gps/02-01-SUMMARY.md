---
phase: 02-motore-gps
plan: 01
subsystem: infra
tags: [gradle, version-catalog, play-services-location, kotlinx-coroutines, lifecycle-runtime-ktx, fusedlocationprovider]

# Dependency graph
requires:
  - phase: 01-fondamenta
    provides: Kotlin-enabled Android project scaffold (AGP 9.1.1 built-in Kotlin support), MainActivity with permission flow
provides:
  - Gradle version-catalog entries and build.gradle.kts wiring for play-services-location (FusedLocationProviderClient), kotlinx-coroutines-core (callbackFlow/Flow/StateFlow), lifecycle-runtime-ktx (lifecycleScope/repeatOnLifecycle)
  - Confirmed green assembleDebug build with kotlinx-coroutines 1.10.2 under AGP 9.1.1's bundled Kotlin 2.2.10 compiler (RESEARCH.md Assumption A2 resolved)
affects: [02-02, 02-03]

# Tech tracking
tech-stack:
  added: ["play-services-location 21.4.0", "kotlinx-coroutines-core 1.10.2", "lifecycle-runtime-ktx 2.11.0"]
  patterns: ["version-catalog-first dependency management (declare in toml, reference via libs.* alias)"]

key-files:
  created: []
  modified: ["gradle/libs.versions.toml", "app/build.gradle.kts"]

key-decisions:
  - "Followed RESEARCH.md exact coordinates/versions (playServicesLocation 21.4.0, kotlinxCoroutines 1.10.2, lifecycleRuntimeKtx 2.11.0) with no substitutions"
  - "Did not add org.jetbrains.kotlin.android to [plugins] — AGP 9.1.1 built-in Kotlin support remains the sole Kotlin toolchain, consistent with Phase 1 decision"
  - "Did not add kotlinx-coroutines-play-services — unused this phase per RESEARCH.md D-06 correction"

patterns-established:
  - "New Gradle deps always: [versions] entry appended to bottom of table -> [libraries] alias with version.ref -> implementation(libs.*) in app/build.gradle.kts dependencies{} block, grouped above testImplementation lines"

requirements-completed: [GPS-03]

# Metrics
duration: 6min
completed: 2026-07-07
---

# Phase 2 Plan 1: Gradle Dependencies for GPS Engine Summary

**Added play-services-location, kotlinx-coroutines-core, and lifecycle-runtime-ktx via version catalog; verified kotlinx-coroutines 1.10.2 compiles cleanly under AGP 9.1.1's built-in Kotlin 2.2.10 compiler with a green assembleDebug.**

## Performance

- **Duration:** 6 min
- **Started:** 2026-07-07T17:25:00Z
- **Completed:** 2026-07-07T17:31:49Z
- **Tasks:** 1
- **Files modified:** 2

## Accomplishments
- Extended `gradle/libs.versions.toml` [versions] and [libraries] tables with `playServicesLocation`, `kotlinxCoroutines`, `lifecycleRuntimeKtx` and their matching library aliases
- Wired all three as `implementation(libs.*)` in `app/build.gradle.kts`, grouped with existing implementation lines
- Verified `./gradlew.bat assembleDebug` succeeds (BUILD SUCCESSFUL in 1m 7s), confirming RESEARCH.md Assumption A2 (kotlinx-coroutines 1.10.2 vs Kotlin 2.2.10 compatibility) is resolved

## Task Commits

Each task was committed atomically:

1. **Task 1: Add play-services-location, kotlinx-coroutines-core, lifecycle-runtime-ktx via version catalog** - `c5d2301` (feat)

**Plan metadata:** SUMMARY commit pending (this file)

## Files Created/Modified
- `gradle/libs.versions.toml` - Added 3 version entries and 3 library aliases (play-services-location, kotlinx-coroutines-core, lifecycle-runtime-ktx)
- `app/build.gradle.kts` - Added 3 `implementation(libs.*)` lines in the dependencies block

## Decisions Made
- Used exact versions from RESEARCH.md's Package Legitimacy Audit (all first-party, Approved verdicts) — no substitutions
- Left `[plugins]` table untouched (no classic Kotlin plugin added), preserving the Phase 1 AGP-built-in-Kotlin decision
- Did not add `kotlinx-coroutines-play-services` since it's unused per RESEARCH.md D-06 correction

## Deviations from Plan

None - plan executed exactly as written. One environment-setup note (not a deviation, not committed): `local.properties` did not exist in this fresh worktree checkout (it's gitignored, machine-local) and was created locally with `sdk.dir` pointing at the existing Android SDK install, matching the main repo's checkout, purely to allow `assembleDebug` to run in this worktree. This file is untracked/gitignored and was not committed.

## Issues Encountered

None beyond the expected `local.properties` setup noted above (gitignored, not part of the repo state).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- `FusedLocationProviderClient`, `LocationRequest.Builder`, `Priority`, `callbackFlow`, `Flow`, `StateFlow`, `lifecycleScope`, and `repeatOnLifecycle` are all now resolvable from Kotlin source, unblocking Plan 02 (GPS engine implementation: `SpeedState.kt`, `GpsSpeedProvider.kt`) and Plan 03 (MainActivity wiring).
- No blockers or concerns.

---
*Phase: 02-motore-gps*
*Completed: 2026-07-07*

## Self-Check: PASSED

- FOUND: gradle/libs.versions.toml
- FOUND: app/build.gradle.kts
- FOUND: .planning/phases/02-motore-gps/02-01-SUMMARY.md
- FOUND commit: c5d2301 (Task 1)
- FOUND commit: accc81a (SUMMARY)
- FOUND: `playServicesLocation = "21.4.0"` in gradle/libs.versions.toml
- FOUND: `libs.play.services.location` wiring in app/build.gradle.kts
