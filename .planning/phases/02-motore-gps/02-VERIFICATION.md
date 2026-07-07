---
phase: 02-motore-gps
verified: 2026-07-07T21:00:00Z
status: passed
score: 13/13 must-haves verified
overrides_applied: 0
---

# Phase 2: Motore GPS Verification Report

**Phase Goal:** L'app legge la velocità corrente del dispositivo tramite GPS in modo affidabile ed efficiente.
**Verified:** 2026-07-07T21:00:00Z
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Il progetto compila (assembleDebug) con le nuove dipendenze aggiunte | VERIFIED | `./gradlew.bat assembleDebug` → BUILD SUCCESSFUL (re-run by verifier) |
| 2 | FusedLocationProviderClient, LocationRequest.Builder, callbackFlow, repeatOnLifecycle risolvibili dal codice Kotlin | VERIFIED | Imported and used in `GpsSpeedProvider.kt` (lines 8-29) and `MainActivity.kt` (lines 17-22); compiles clean |
| 3 | Da fermo o hasSpeed()==false → 0 km/h (D-04) | VERIFIED | `SpeedMapping.kt:26` `val kmh = if (hasSpeed) speedMetersPerSecond * 3.6 else 0.0`; test `hasSpeedFalse_returnsZero` passes |
| 4 | Piccole velocità sotto soglia rumore (~2 km/h) arrotondate a 0 (D-03) | VERIFIED | `SpeedMapping.kt:29` `if (kmh < noiseFloorKmh) return 0`; test `belowNoiseFloor_returnsZero` passes |
| 5 | Letture con accuratezza scarsa (oltre ~50m) scartate (D-05) | VERIFIED | `SpeedMapping.kt:21` `if (hasAccuracy && accuracyMeters > accuracyThresholdMeters) return null`; test `poorAccuracy_returnsNull` passes |
| 6 | Velocità m/s convertita in km/h intero, nessun decimale (D-09) | VERIFIED | `SpeedMapping.kt:34` `kmh.roundToInt()`; test `fractionalSpeed_roundsToNearestInt` (18.97→19) passes |
| 7 | Il motore espone StateFlow<SpeedState> che parte in Searching e passa a NoSignal se >5s senza update (D-06/D-01/D-02) | VERIFIED | `GpsSpeedProvider.kt:105-114` `val state: StateFlow<SpeedState>` initial `SpeedState.Searching`; `deriveSpeedState()` (line 135-139) unit-tested in `GpsSpeedProviderStateTest` (5 cases incl. exact 5s boundary and >5s → NoSignal) |
| 8 | All'avvio (permesso concesso) l'utente vede "Ricerca segnale GPS..." finché non arriva un fix valido (D-01) | VERIFIED | `MainActivity.kt:169-176` `updatePlaceholder` maps `Searching`→`R.string.searching_gps_signal`; human-confirmed via emulator Route Playback checkpoint ("approvato") |
| 9 | Quando il GPS legge una velocità, il placeholder mostra il numero intero + " km/h" (D-09) | VERIFIED | `MainActivity.kt:174` `is SpeedState.Reading -> getString(R.string.speed_kmh_format, state.kmh)`; `strings.xml:9` `%1$d km/h`; human-confirmed |
| 10 | Se il segnale si perde per >5s, il placeholder torna a "Ricerca segnale GPS..." (D-02) | VERIFIED | `deriveSpeedState` NoSignal branch maps to same string as Searching (line 172); human-confirmed via Route Playback signal-loss step |
| 11 | Gli aggiornamenti GPS partono onStart e si fermano onStop (D-07) | VERIFIED | `MainActivity.kt:62-74` sole collector is `lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { ... } }`; no manual onStart/onStop overrides; `GpsSpeedProvider` uses `SharingStarted.WhileSubscribed()` so `awaitClose{ removeLocationUpdates }` fires on unsubscribe |
| 12 | Il flusso permesso della Fase 1 continua a funzionare invariato | VERIFIED | `checkAndRequestPermission`/`showReady`/`showDenied`/`onRetryClicked`/`openAppSettings` unchanged in logic; additionally hardened post-review (CR-01) with a reactive `permissionGranted` StateFlow refreshed from 3 call sites so a grant is never missed |
| 13 | Il checkpoint umano verifica la velocità variabile tramite Route Playback (D-10) | VERIFIED | 02-03-SUMMARY.md documents user response "approvato" via Pixel 10 Pro AVD Extended Controls → Routes → Play Route, confirming varying whole-number km/h ~1/sec, 0 km/h when stopped, revert to search message on loss |

**Score:** 13/13 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `gradle/libs.versions.toml` | version-catalog entries for play-services-location, kotlinx-coroutines-core, lifecycle-runtime-ktx | VERIFIED | Lines 10-12 (versions), 22-24 (libraries) — exact versions from RESEARCH.md |
| `app/build.gradle.kts` | `implementation(libs.*)` wiring | VERIFIED | Lines 49-51 |
| `app/src/main/java/com/sed/tachimetro/gps/SpeedState.kt` | sealed class Searching/Reading(kmh)/NoSignal | VERIFIED | Present, 3 members, mapped to D-01/D-09/D-02 |
| `app/src/main/java/com/sed/tachimetro/gps/SpeedMapping.kt` | pure `mapSpeedToKmh` | VERIFIED | Framework-free, exact signature match, no android.*/gms.* imports |
| `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt` | callbackFlow bridge exposing StateFlow<SpeedState> | VERIFIED | callbackFlow + awaitClose sole cleanup; `deriveSpeedState` extracted (post-review WR-02) |
| `app/src/test/java/com/sed/tachimetro/gps/SpeedMappingTest.kt` | unit tests for D-03/D-04/D-05/GPS-01 | VERIFIED | 7 tests, all pass |
| `app/src/test/java/com/sed/tachimetro/gps/GpsSpeedProviderStateTest.kt` | unit tests for state-machine (D-01/D-02) | VERIFIED | 5 tests added post-review (WR-02), all pass — not originally in PLAN must_haves but strengthens truth #7 |
| `app/src/main/res/values/strings.xml` | Italian no-signal string | VERIFIED | `searching_gps_signal` present; `speed_kmh_format` added post-review (WR-03) |
| `app/src/main/java/com/sed/tachimetro/MainActivity.kt` | GpsSpeedProvider wiring + updatePlaceholder + lifecycle-scoped collector | VERIFIED | All present and functioning |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| `app/build.gradle.kts` | `gradle/libs.versions.toml` | `libs.*` alias references | WIRED | `libs.play.services.location` etc. resolve; build succeeds |
| `GpsSpeedProvider.kt` | `FusedLocationProviderClient` | `requestLocationUpdates`/`removeLocationUpdates` inside `awaitClose` | WIRED | Line 73 (request) / Line 74 (`awaitClose { client.removeLocationUpdates(callback) }`) — `removeLocationUpdates` appears ONLY inside `awaitClose` |
| `GpsSpeedProvider.kt` | `SpeedMapping.kt` | `Flow.map` calls `mapSpeedToKmh` | WIRED | Lines 78-87 |
| `GpsSpeedProvider.kt` | `SpeedState` | `StateFlow<SpeedState>` exposed as `state` | WIRED | Lines 105-114 |
| `MainActivity.kt` | `GpsSpeedProvider.state` | `lifecycleScope.launch { repeatOnLifecycle(STARTED) { ...collect } }` | WIRED | Lines 62-74; post-review reactive-permission fix (CR-01) makes this more robust, not less |
| `MainActivity.kt` | `R.string.searching_gps_signal` | `updatePlaceholder` sets `messageText.text` | WIRED | Line 173 |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|---------------------|--------|
| `MainActivity.updatePlaceholder` | `state: SpeedState` collected from `gpsSpeedProvider.state` | `GpsSpeedProvider.state` = `combine(acceptedKmh, ticker) { ... deriveSpeedState }` | Yes — `acceptedKmh` derives from `rawLocations` = real `callbackFlow` over `FusedLocationProviderClient.requestLocationUpdates`, not a static/stub value | FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Project builds with new deps | `./gradlew.bat assembleDebug` | BUILD SUCCESSFUL | PASS |
| Unit tests pass (SpeedMapping + GpsSpeedProviderState) | `./gradlew.bat testDebugUnitTest` | BUILD SUCCESSFUL, all tests green | PASS |
| Lint clean (MissingPermission suppressed correctly) | `./gradlew.bat lintDebug` | BUILD SUCCESSFUL | PASS |
| No debt markers (TODO/FIXME/XXX/TBD/placeholder) in phase files | grep across `com.sed.tachimetro` sources | Only match is the legitimate function/variable name `updatePlaceholder`/`messageText` (the placeholder TextView) — no stub markers | PASS |

### Probe Execution

No `scripts/*/tests/probe-*.sh` convention or explicit probe declarations found in this project; PLAN/SUMMARY verification is build+unit-test based, not probe-script based. Skipped — no runnable probes declared for this phase.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|--------------|--------|----------|
| GPS-01 | 02-02, 02-03 | Velocità attuale in km/h dal GPS, aggiornata 1 volta/sec | SATISFIED | `mapSpeedToKmh` conversion/rounding tested; `GpsSpeedProvider` 1Hz ticker; human-confirmed live via Route Playback |
| GPS-02 | 02-03 | Messaggio "Ricerca segnale GPS..." quando GPS non ha segnale | SATISFIED | `updatePlaceholder` Searching/NoSignal → `searching_gps_signal`; human-confirmed |
| GPS-03 | 02-01, 02-02 | Velocità letta tramite FusedLocationProviderClient | SATISFIED | `GpsSpeedProvider.kt:45` `LocationServices.getFusedLocationProviderClient(...)`; `play-services-location` dependency wired |

**Note (documentation freshness, non-blocking):** `.planning/REQUIREMENTS.md` still shows GPS-01/GPS-02/GPS-03 as unchecked `[ ]` and "Pending" in the traceability table. This is a documentation-sync gap only — the codebase evidence above satisfies all three requirements. Recommend updating REQUIREMENTS.md checkboxes/traceability as a housekeeping step; it does not block phase goal achievement.

No orphaned requirements: all three IDs declared in this phase's REQUIREMENTS.md section (GPS-01, GPS-02, GPS-03) are claimed across the three plans' frontmatter `requirements:` fields (02-01: GPS-03; 02-02: GPS-01/02/03; 02-03: GPS-01/02).

### Anti-Patterns Found

None. No `TODO`/`FIXME`/`XXX`/`TBD` debt markers, no empty handlers, no hardcoded static returns, no `Log.*` of `Location` objects (verified by grep — only `kmh` Int values are ever logged/rendered).

### Code Review Findings — Resolution Status

A code review (`02-REVIEW.md`) ran after the phase's human checkpoint was approved and found 1 critical + 4 warning issues. All 5 were fixed (`02-REVIEW-FIX.md`) and independently re-verified here against the current source:

| ID | Finding | Fix Commit | Verifier Re-check |
|----|---------|-----------|--------------------|
| CR-01 | GPS collector may never start after first-time permission grant (one-shot check inside `repeatOnLifecycle`) | `ba032a5` | CONFIRMED — `MainActivity.kt` now uses reactive `permissionGranted: MutableStateFlow<Boolean>` collected via `collectLatest`, refreshed from `checkAndRequestPermission()`, `requestPermissionLauncher` callback, and `onResume()` |
| WR-01 | Staleness detection used wall-clock time | `1d9b831` | CONFIRMED — `SystemClock.elapsedRealtime()` used at both call sites (`GpsSpeedProvider.kt:93,100`) |
| WR-02 | No test coverage for GPS state machine | `50dee26` | CONFIRMED — `deriveSpeedState()` extracted as pure function, 5 unit tests in `GpsSpeedProviderStateTest.kt`, all pass |
| WR-03 | Hardcoded non-localized `"$kmh km/h"` string | `6a5ba8c` | CONFIRMED — `R.string.speed_kmh_format` in `strings.xml`, used via `getString(...)` |
| WR-04 | Activity context leaked into GpsSpeedProvider with no teardown | `c7487da` | CONFIRMED — `GpsSpeedProvider(applicationContext)`, `context.applicationContext` used internally, `close()`/`onDestroy()` teardown added |

Build/test verification after fixes: `assembleDebug testDebugUnitTest` green (re-confirmed independently by this verifier, not just trusted from SUMMARY).

**CR-01 fix manual re-verification:** The code review flagged CR-01 as lifecycle/state-handling logic warranting a manual device-level re-check of the deny→grant-via-Settings→auto-resume flow specifically. Per the task context, this manual re-verification was explicitly offered to the user after the fix, and the user chose to skip it, relying on the automated build/test pass instead. This is treated as a resolved, informed decision (not an open gap) — the static code change directly and correctly addresses the reviewed failure mode (one-shot check → reactive `collectLatest` over a `StateFlow` refreshed from all permission-change entry points), and the original blocking checkpoint (Task 2, Route Playback) had already been approved by the user prior to the review. No further human verification item is raised for this.

### Human Verification Required

None outstanding. The phase's blocking human checkpoint (Task 2: Route Playback, D-10) was approved by the user ("approvato") prior to code review. The one post-review item eligible for optional manual re-verification (CR-01 deny→grant-via-Settings flow) was explicitly offered to and declined by the user, who elected to rely on the green automated build/test suite instead — documented above, not re-raised here.

### Gaps Summary

No gaps found. All 13 must-have truths across the three plans (02-01, 02-02, 02-03) are verified directly against the current source code, independent of SUMMARY.md claims: dependencies resolve and build green, the pure `mapSpeedToKmh` filter enforces D-03/D-04/D-05/D-09 (locked by 7 passing unit tests), `GpsSpeedProvider` exposes a real `StateFlow<SpeedState>` driven by live `FusedLocationProviderClient` data (not a stub), the 5s no-signal state machine is now unit-tested (`deriveSpeedState`, 5 passing tests, added during review-fix), `MainActivity` wires the collector through `repeatOnLifecycle(STARTED)` and renders the Italian search message / whole-number km/h correctly, and the Phase-1 permission flow is preserved and hardened. The one critical code-review finding (CR-01) was fixed and the fix was independently re-inspected in the source (not just trusted from the fix report); the optional manual re-verification of that specific fix was offered to and knowingly declined by the user.

---

_Verified: 2026-07-07T21:00:00Z_
_Verifier: Claude (gsd-verifier)_
