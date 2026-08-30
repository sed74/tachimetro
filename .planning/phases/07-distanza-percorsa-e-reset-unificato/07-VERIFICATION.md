---
phase: 07-distanza-percorsa-e-reset-unificato
verified: 2026-08-30T12:54:51Z
status: passed
score: 14/14 must-haves verified
overrides_applied: 0
---

# Phase 7: Distanza Percorsa e Reset Unificato Verification Report

**Phase Goal:** L'utente monitora la distanza percorsa dall'ultimo azzeramento, calcolata solo mentre l'app è in foreground e persistente su disco, e può azzerarla nella stessa azione con cui azzera la velocità massima.
**Verified:** 2026-08-30T12:54:51Z
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | L'utente vede un'area in basso a destra con la distanza percorsa dall'ultimo azzeramento, in un font più grande dell'area velocità massima (Roadmap SC1 / DIST-01) | ✓ VERIFIED | `activity_main.xml:89-113`: `distanceText` at `android:textSize="32sp"` vs `maxSpeedText` at `android:textSize="22sp"` (line 69), both anchored bottom-right via `distanceUnitText` (`layout_constraintBottom_toBottomOf="parent"`, `layout_constraintEnd_toEndOf="parent"`). Confirmed on real hardware: 07-04-SUMMARY.md checklist point 1 = PASS |
| 2 | La distanza aumenta in tempo reale mentre l'app è in foreground e riceve letture GPS valide (Roadmap SC2 / DIST-01) | ✓ VERIFIED | `MainActivity.updatePlaceholder()` (`MainActivity.kt:363-367`) calls `reduceDistance(currentDistanceMeters, state.deltaMeters, state.kmh)` on every `SpeedState.Reading` emission and writes to disk when changed; `state.deltaMeters` is computed by `GpsSpeedProvider` via `Location.distanceTo()` between consecutive accepted fixes (`GpsSpeedProvider.kt:101`). Confirmed on real hardware: 07-04-SUMMARY.md checklist point 2 = PASS |
| 3 | Mettendo l'app in background o perdendo il segnale GPS, la distanza smette di accumularsi — nessun tracking in background (Roadmap SC3 / DIST-02) | ✓ VERIFIED (see WARNING below) | GPS collection is scoped inside `repeatOnLifecycle(Lifecycle.State.STARTED)` (`MainActivity.kt:180-192`) and `GpsSpeedProvider.state` uses `SharingStarted.WhileSubscribed()` (`GpsSpeedProvider.kt:132`), so the upstream `client.requestLocationUpdates(...)` subscription is torn down on `onStop()` and no accumulation runs while backgrounded. `reduceDistance()` also gates accumulation below the noise floor while stationary. Confirmed on real hardware: 07-04-SUMMARY.md checklist points 3 (stationary) and 5 (background) = PASS. **07-REVIEW.md WR-01 flags a related, non-blocking latent risk** — see Anti-Patterns section |
| 4 | Chiudendo e riaprendo l'app, o riavviando il telefono, la distanza precedentemente accumulata è ancora visibile (Roadmap SC4 / DIST-03) | ✓ VERIFIED | `DistanceStore` persists a raw `Float` to `SharedPreferences("tachimetro_prefs")` key `distance_meters` (`DistanceStore.kt:10-26`); `MainActivity.onCreate()` calls `currentDistanceMeters = distanceStore.read()` and `updateDistanceArea()` *before* `gpsSpeedProvider` is constructed (`MainActivity.kt:140-142`), so the persisted value renders before any GPS fix arrives. Confirmed on real hardware: 07-04-SUMMARY.md checklist point 6 = PASS (both app-restart and phone-reboot cases) |
| 5 | Premendo il pulsante "Azzera massimo" ("Azzera"), sia la velocità massima sia la distanza percorsa si azzerano nella stessa azione (Roadmap SC5 / MAX-04) | ✓ VERIFIED | `onResetClicked()` (`MainActivity.kt:382-389`) zeroes and persists both `currentMax`/`maxSpeedStore` and `currentDistanceMeters`/`distanceStore` in one handler, bound to the single `resetMaxButton.setOnClickListener` (line 128); `grep -c 'setOnClickListener { onResetClicked() }'` = 1 (single reset point, no `AlertDialog`). Confirmed on real hardware: 07-04-SUMMARY.md checklist point 7 = PASS |
| 6 | Un delta di distanza viene sommato al totale solo quando la lettura di velocità è pari o superiore alla soglia di rumore di 2 km/h (D-04) | ✓ VERIFIED | `reduceDistance()` (`DistanceReducer.kt:11-23`): `if (kmh < noiseFloorKmh) return safeCurrentTotal`. Covered by 9 JVM unit tests in `DistanceReducerTest.kt`, independently re-run: `tests="9" failures="0" errors="0"` |
| 7 | Un valore di distanza persistito negativo o corrotto viene riportato a 0 al momento della lettura | ✓ VERIFIED | `sanitizePersistedDistance()` (`DistanceReducer.kt:26`) called inside `DistanceStore.read()` (`DistanceStore.kt:15`); test `sanitizePersistedDistance_negativeValue_isClampedToZero` passes |
| 8 | Sotto i 1000 m il valore è espresso in metri interi, da 1000 m in su in chilometri con una decimale (D-01) | ✓ VERIFIED | `formatDistanceDisplay()` (`DistanceFormat.kt:27-33`), threshold `meters < 1000f`; 6 JVM tests in `DistanceFormatTest.kt` independently re-run: `tests="6" failures="0" errors="0"`. Display formatting uses `getString(R.string.distance_km_format, ...)` (`"%1$.1f"`) — never bare `String.format` (`grep -c 'String.format' MainActivity.kt` = 0), so the decimal comma renders per device locale. Confirmed on real hardware: 07-04-SUMMARY.md checklist point 4 = PASS (Italian comma decimal) |
| 9 | Ogni lettura GPS accettata espone, oltre ai km/h, i metri percorsi rispetto al fix accettato precedente, calcolati con `Location.distanceTo()` (D-06) | ✓ VERIFIED | `SpeedState.Reading(val kmh: Int, val deltaMeters: Float)` (`SpeedState.kt:23`); `GpsSpeedProvider.kt:101` `lastAcceptedLocation?.distanceTo(loc) ?: 0f` inside the same `acceptedReadings` pipeline that computes `kmh` |
| 10 | Il primo fix accettato di una sessione espone un delta di 0 metri | ✓ VERIFIED | `lastAcceptedLocation?.distanceTo(loc) ?: 0f` — `null` on first fix produces `0f` (`GpsSpeedProvider.kt:101`); no explicit unit test exercises the Flow itself (untestable without Robolectric per plan's own scope), but the logic is a direct, unambiguous Elvis-operator fallback |
| 11 | I filtri di accuratezza e soglia di rumore non sono duplicati: `mapSpeedToKmh` resta l'unica sorgente di verità (D-05) | ✓ VERIFIED | `grep -c 'mapSpeedToKmh' GpsSpeedProvider.kt` = 1 (single call site, `GpsSpeedProvider.kt:91-98`); `SpeedMapping.kt` untouched by this phase (`git log` shows no phase-07 commits touching it) |
| 12 | Nessun oggetto `Location` grezzo esce da `GpsSpeedProvider` | ✓ VERIFIED | `grep -rl 'android.location.Location' app/src/main/java/com/sed/tachimetro/` returns only `GpsSpeedProvider.kt` |
| 13 | Due invocazioni di `deriveSpeedState` con input identici producono istanze `Reading` uguali secondo `equals()` (StateFlow conflation contract) | ✓ VERIFIED | Test `identicalInputs_produceEqualReadings` and `differentDeltaMeters_produceDifferentReadings` in `GpsSpeedProviderStateTest.kt`; independently re-run: `tests="7" failures="0" errors="0"` |
| 14 | L'area distanza è nascosta quando il permesso GPS è negato e torna visibile quando viene concesso | ✓ VERIFIED | `showDenied()` sets `distanceText.visibility = View.GONE` / `distanceUnitText.visibility = View.GONE` (`MainActivity.kt:316-317`); `showReady()` calls `updateDistanceArea()` which sets both back to `View.VISIBLE` (`MainActivity.kt:308`, `427-428`). Confirmed on real hardware: 07-04-SUMMARY.md checklist point 11 = PASS |

**Score:** 14/14 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `app/src/main/java/com/sed/tachimetro/distance/DistanceReducer.kt` | `reduceDistance` (D-04 gate) + `sanitizePersistedDistance`, top-level, framework-free | ✓ VERIFIED | Both functions present exactly as specified; `grep -c '^import android' DistanceReducer.kt` = 0 |
| `app/src/main/java/com/sed/tachimetro/distance/DistanceStore.kt` | SharedPreferences persistence, `Float`, mirror of `MaxSpeedStore` | ✓ VERIFIED | `class DistanceStore(context: Context)`, `"tachimetro_prefs"`, `"distance_meters"`, `putFloat`/`getFloat`/`.apply()`, calls `sanitizePersistedDistance`; no `commit()` |
| `app/src/main/java/com/sed/tachimetro/distance/DistanceFormat.kt` | `DistanceDisplay` sealed class + `formatDistanceDisplay` (D-01) | ✓ VERIFIED | `sealed class DistanceDisplay` with `Meters`/`Kilometers`, threshold `1000f`; no `getString`/`String.format` (unit-agnostic per D-02) |
| `app/src/test/java/com/sed/tachimetro/distance/DistanceReducerTest.kt` | 9 tests | ✓ VERIFIED | 9 `@Test` methods present; re-run confirms `tests="9" failures="0"` |
| `app/src/test/java/com/sed/tachimetro/distance/DistanceFormatTest.kt` | 6 tests | ✓ VERIFIED | 6 `@Test` methods present; re-run confirms `tests="6" failures="0"` |
| `app/src/main/java/com/sed/tachimetro/gps/SpeedState.kt` | `Reading(kmh, deltaMeters)` | ✓ VERIFIED | Exact 2-field data class with conflation-contract KDoc |
| `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt` | `lastAcceptedLocation` + `distanceTo()`, `deriveSpeedState` at 4 params | ✓ VERIFIED | All present exactly as specified |
| `app/src/test/java/com/sed/tachimetro/gps/GpsSpeedProviderStateTest.kt` | 7 tests incl. conflation contract | ✓ VERIFIED | 7 `@Test` methods; re-run confirms `tests="7" failures="0"` |
| `app/src/main/res/values/strings.xml` | Distance formats, unit strings, "Azzera" label | ✓ VERIFIED | `reset_max_button` = "Azzera"; `distance_meters_format`, `distance_km_format`, `unit_meters`, `unit_km` all present |
| `app/src/main/res/layout/activity_main.xml` | `distanceText` (32sp) + `distanceUnitText` (16sp), bottom-right | ✓ VERIFIED | Both views present with correct sizes/constraints, no `visibility="gone"` (always-visible by design), no `lime` |
| `app/src/main/java/com/sed/tachimetro/MainActivity.kt` | Distance wiring, reset unification, insets | ✓ VERIFIED | `updateDistanceArea()`, `applyDistanceAreaWindowInsets()`, `onResetClicked()`, accumulation in `updatePlaceholder()` all present and correctly wired |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| `DistanceStore.kt` | `sanitizePersistedDistance` | call inside `read()` | ✓ WIRED | `fun read(): Float = sanitizePersistedDistance(prefs.getFloat(...))` |
| `DistanceStore.kt` | `SharedPreferences "tachimetro_prefs"` | `getSharedPreferences` + `putFloat`/`apply` | ✓ WIRED | Confirmed literal strings present |
| `GpsSpeedProvider.kt` | `android.location.Location.distanceTo` | delta computation between accepted fixes | ✓ WIRED | `lastAcceptedLocation?.distanceTo(loc) ?: 0f` (line 101) |
| `GpsSpeedProvider.kt` | `mapSpeedToKmh` | single accuracy/noise gate | ✓ WIRED | One call site, `SpeedMapping.kt` untouched |
| `MainActivity.kt` | `com.sed.tachimetro.distance.reduceDistance` | call in `SpeedState.Reading` branch of `updatePlaceholder()` | ✓ WIRED | `reduceDistance(currentDistanceMeters, state.deltaMeters, state.kmh)` (line 363) |
| `MainActivity.kt` | `com.sed.tachimetro.distance.DistanceStore` | `read()` in `onCreate`, `write()` on every increment + reset | ✓ WIRED | `distanceStore.read()` (141), `distanceStore.write(currentDistanceMeters)` (366), `distanceStore.write(0f)` (387) |
| `MainActivity.kt` | `SpeedState.Reading.deltaMeters` | consumed in `updatePlaceholder()` | ✓ WIRED | `state.deltaMeters` passed directly into `reduceDistance` |
| `activity_main.xml` | `distanceUnitText` | sole `parent`-anchored view of the group, receives insets listener | ✓ WIRED | `layout_constraintEnd_toStartOf="@id/distanceUnitText"` on `distanceText`; `applyDistanceAreaWindowInsets()` registers listener only on `distanceUnitText` |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|---------------------|--------|
| `distanceText`/`distanceUnitText` | `currentDistanceMeters` | `distanceStore.read()` at startup, then `reduceDistance(currentDistanceMeters, state.deltaMeters, state.kmh)` on every `SpeedState.Reading` where `state.deltaMeters` comes from real `Location.distanceTo()` between live FusedLocationProviderClient fixes | Yes | ✓ FLOWING |
| `maxSpeedText`/`resetMaxButton` visibility | `currentMax`, `currentDistanceMeters` | Same real accumulation path plus `maxSpeedStore`/`distanceStore` reads | Yes | ✓ FLOWING |

No hardcoded/static fallback found in the distance rendering path — confirmed both by static analysis and by 07-04-SUMMARY.md's real-device road test (≥1.5 km walked/driven, distance value tracked visually).

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Full test suite compiles and passes | `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest` (independently re-run by verifier) | `BUILD SUCCESSFUL` | ✓ PASS |
| Distance domain unit tests pass | `./gradlew.bat :app:testDebugUnitTest --tests "com.sed.tachimetro.distance.*"` | `DistanceFormatTest`: `tests="6" failures="0"`; `DistanceReducerTest`: `tests="9" failures="0"` | ✓ PASS |
| GPS state-derivation unit tests pass | `./gradlew.bat :app:testDebugUnitTest --tests "com.sed.tachimetro.gps.GpsSpeedProviderStateTest"` | `tests="7" failures="0"` | ✓ PASS |
| Full project unit test count matches SUMMARY claim (44) | sum of all `TEST-*.xml` `tests=` attributes in `app/build/test-results/testDebugUnitTest/` | 1+6+6+9+7+7+8 = 44, 0 failures across all | ✓ PASS |
| No `String.format`/`AlertDialog`/`lime` leak into distance code | `grep -c` on `MainActivity.kt` / `activity_main.xml` | All `0` | ✓ PASS |
| `Location` confined to `GpsSpeedProvider.kt` | `grep -rl 'android.location.Location' app/src/main/java/com/sed/tachimetro/` | Only `GpsSpeedProvider.kt` | ✓ PASS |

### Probe Execution

SKIPPED — no `scripts/*/tests/probe-*.sh` files exist in this repository, and no PLAN/SUMMARY file for this phase references any probe script. Not a migration/CLI tooling phase.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|--------------|--------|----------|
| DIST-01 | 07-01, 07-03, 07-04 | L'utente vede la distanza percorsa dall'ultimo azzeramento in un'area in basso a destra, con font più grande dell'area velocità massima | ✓ SATISFIED | Truths 1, 8 above; REQUIREMENTS.md marked `[x]` |
| DIST-02 | 07-02, 07-03, 07-04 | La distanza si accumula solo mentre l'app è attiva e riceve aggiornamenti GPS, senza tracking in background | ✓ SATISFIED | Truths 2, 3, 9-13 above; REQUIREMENTS.md marked `[x]`; see WR-01 non-blocking note below |
| DIST-03 | 07-01, 07-03, 07-04 | La distanza persiste su disco e sopravvive a chiusura app e riavvio del telefono | ✓ SATISFIED | Truths 4, 7 above; REQUIREMENTS.md marked `[x]` |
| MAX-04 | 07-03, 07-04 | Il pulsante "Azzera massimo" azzera sia la velocità massima sia la distanza percorsa in un'unica azione | ✓ SATISFIED | Truth 5 above; REQUIREMENTS.md marked `[x]` |

No orphaned requirements: REQUIREMENTS.md traceability table maps only DIST-01, DIST-02, DIST-03, MAX-04 to Phase 7, and all four appear in the `requirements:` frontmatter of at least one plan in this phase (07-01: DIST-01/DIST-03; 07-02: DIST-02; 07-03 and 07-04: all four).

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt` | 69, 74-107 | `lastAcceptedLocation` is never reset when the GPS collection `Flow` restarts after being torn down (background/foreground cycle, or permission revoke+re-grant). The first accepted fix after such a gap computes its delta against the stale pre-gap position rather than treating it as a fresh start. Documented in `07-REVIEW.md` WR-01 with a proposed fix (reset `lastAcceptedLocation = null` at the top of the `rawLocations` `callbackFlow` block) | ⚠️ Warning | Could theoretically cause a one-time spurious distance jump on resume after backgrounding, contrary to the spirit of DIST-02. Filed as WARNING not BLOCKER because: (a) the exact scenario (500m walked/driven in background, then foregrounded) was explicitly tested on real hardware in 07-04-SUMMARY.md checklist point 5 and passed — the first re-acquired fix typically arrives with `hasSpeed()==false`, gating it via `mapSpeedToKmh`/noise floor; (b) no unit test can exercise this Flow-restart path without Robolectric (out of this project's test scope); (c) the code reviewer explicitly notes this protection is incidental/undocumented-in-code and device/OS-dependent, not guaranteed. **Recommend a follow-up fix in a future phase or quick task** — this is a real latent correctness gap even though it did not manifest in the observed test |
| `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt`, `app/src/main/java/com/sed/tachimetro/distance/DistanceReducer.kt` | 55, 15 | `noiseFloorKmh = 2.0` duplicated as two independently-declared literals (no shared constant) — `07-REVIEW.md` WR-03 | ℹ️ Info | Coupling/maintainability risk only; both values are currently in sync and covered by tests against the current literal. Does not affect current goal achievement |
| `app/src/main/java/com/sed/tachimetro/MainActivity.kt` | 115-206 | `onCreate()` has grown to ~90 lines mixing five setup concerns (permission views, max-speed init, distance init, screen-on switch, GPS/charging collectors) — `07-REVIEW.md` WR-02 | ℹ️ Info | Maintainability concern only, does not affect DIST-01/02/03/MAX-04 behavior |

No TBD/FIXME/XXX/TODO/HACK/PLACEHOLDER markers found in any file created/modified by this phase (`grep -n -E "TBD|FIXME|XXX|TODO|HACK|PLACEHOLDER"` returned no matches across all 8 distance-related source/resource files).

### Human Verification Required

None outstanding. Plan 07-04 was a `checkpoint:human-verify` task (`gate="blocking"`) that already executed the full 11-point on-device/on-road checklist (presence/size, real-time growth, no accumulation while stationary, format switch at 1km with Italian decimal comma, no background tracking over 500m, persistence across app-close and phone-reboot, unified reset, reset-button reappearance, rotation/cutout, no stray colors/animations, permission revoke/re-grant) against real hardware over a ≥1.5 km road test. The user responded "sono riuscito a testare tutto: approvo!" (11/11 PASS), documented verbatim with per-point results in `07-04-SUMMARY.md`. An initial false-alarm report on an emulator (no movement without a route/waypoint sequence) was investigated against the actual source code and correctly attributed to the D-04 noise-floor gate working as designed, not a bug — no code change was made or needed. Per this verification's scope, the completed blocking checkpoint is accepted as genuine human verification evidence, not a pending gap.

### Gaps Summary

No gaps found. All 14 observable truths (5 roadmap Success Criteria + 9 plan-level supporting truths) are verified through a combination of: (1) direct code inspection matching every plan's binding `<interfaces>` contract exactly, byte-for-byte on function signatures; (2) a green `assembleDebug` + `testDebugUnitTest` build independently re-run by this verifier (not just trusted from SUMMARY.md), confirming 44/44 unit tests pass with 0 failures, including 9 `DistanceReducerTest`, 6 `DistanceFormatTest`, and 7 `GpsSpeedProviderStateTest`; (3) grep-based confirmation of every hard constraint (no `String.format`, no `AlertDialog`, no `lime` color leakage, `Location` confined to a single file, single reset click listener, zero debt-marker comments); and (4) genuine on-device human verification of all real-time/background/persistence/reset behavior documented point-by-point in 07-04-SUMMARY.md and approved by the actual user after a full physical road test.

One non-blocking WARNING is carried forward from `07-REVIEW.md` (WR-01: `lastAcceptedLocation` not reset across GPS collection restarts) — a real latent correctness risk for DIST-02 that did not manifest in the tested scenario but is not structurally prevented by the code. This does not block phase completion but is worth tracking as a follow-up.

---

_Verified: 2026-08-30T12:54:51Z_
_Verifier: Claude (gsd-verifier)_
