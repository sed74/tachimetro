---
phase: 09-permesso-di-localizzazione-dallo-schermo-auto
verified: 2026-09-02T15:10:00Z
status: passed
score: 17/17 must-haves verified
overrides_applied: 0
---

# Phase 9: Permesso di Localizzazione dallo Schermo Auto Verification Report

**Phase Goal:** Se il permesso di localizzazione non è ancora stato concesso, l'utente può concederlo direttamente dallo schermo Android Auto al primo collegamento, senza dover prima aprire l'app sul telefono.
**Verified:** 2026-09-02T15:10:00Z
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | (Roadmap SC1) Al primo collegamento senza permesso mai concesso, lo schermo auto mostra una richiesta esplicita invece di restare vuoto/bloccato | ✓ VERIFIED | `SpeedScreen.init` calls `refreshPermissionState()` then `requestLocationPermission()` from the `NotRequested` branch (`SpeedScreen.kt:100-107`); confirmed live on DHU (09-CONTEXT.md D-08, Scenario A: "Controlla il telefono" shown, system dialog appeared on phone) |
| 2 | (Roadmap SC2) Concedendo il permesso, lo schermo passa automaticamente alla velocità (o "Ricerca segnale...") senza riavvio app/collegamento | ✓ VERIFIED | `permissionState` is a `MutableStateFlow` collected with `collectLatest` inside `repeatOnLifecycle(STARTED)`; the callback of `requestLocationPermission()` re-derives grant via `ContextCompat.checkSelfPermission` and pushes `Granted`, which cancels/restarts the collector without a new lifecycle cycle (`SpeedScreen.kt:97-116, 153-181`); confirmed live on DHU (D-08, Scenario B) |
| 3 | (Roadmap SC3) Negando il permesso, viene mostrato un messaggio chiaro con distinzione rifiuto singolo/permanente | ✓ VERIFIED | `buildTemplate()`'s `Denied` branch renders `car_permission_denied`/`car_permission_denied_permanent` + Riprova/Apri impostazioni Action based on `permanent` (`SpeedScreen.kt:249-275`); locked by instrumented tests `deniedOnce_...`/`deniedPermanently_...`, both passing on a real connected device (see Behavioral Spot-Checks); confirmed live on DHU (D-08, Scenarios C/D) |
| 4 | Esiste un modello di stato (`CarPermissionState`) che distingue concesso/mai richiesto/in attesa/rifiutato singolo/rifiutato permanente | ✓ VERIFIED | `CarPermissionState.kt:23-35` — sealed class with `Granted`, `NotRequested`, `Waiting` (data objects) and `Denied(permanent: Boolean)` (data class) |
| 5 | La distinzione primo rifiuto/permanente è calcolabile senza Activity da una funzione pura testata a livello JVM | ✓ VERIFIED | `resolveCarPermissionState(granted, denialCount)` is a top-level function with zero Android imports (`CarPermissionState.kt:50-54`); `CarPermissionStateTest.kt` (10 `assertEquals`, plain JVM, no Robolectric) exercises every branch; `./gradlew.bat :app:testDebugUnitTest` re-run during verification: BUILD SUCCESSFUL |
| 6 | Il numero di rifiuti registrati sopravvive alla ricreazione dello Screen e alla riconnessione Android Auto | ✓ VERIFIED | `CarPermissionDenialStore` persists an `Int` in `SharedPreferences` (`tachimetro_prefs`, key `car_location_denial_count`), read via `denialCount()`, written via `recordDenial()` with `apply()` (`CarPermissionDenialStore.kt:19-33`) |
| 7 | Un valore persistito manomesso/negativo non produce uno stato incoerente | ✓ VERIFIED | `sanitizeDenialCount(raw) = if (raw < 0) 0 else raw` applied on every read (`CarPermissionState.kt:57`, `CarPermissionDenialStore.kt:24`); locked by test `notGranted_tamperedNegativeDenialCount_sanitizedToNotRequested` |
| 8 | Le tre stringhe italiane dedicate (attesa, rifiuto singolo, rifiuto permanente) esistono e sono più corte delle equivalenti telefono | ✓ VERIFIED | `strings.xml:12-16` — `car_check_your_phone`="Controlla il telefono" (D-01 exact text), `car_permission_denied`="Permesso GPS necessario" (23 chars < 37 for `permission_denied`), `car_permission_denied_permanent`="Permesso negato. Apri le impostazioni sul telefono" (49 chars < 56 for `permission_denied_permanent`) |
| 9 | Retry esplicito dopo un rifiuto rilancia la richiesta senza uscire dall'app auto | ✓ VERIFIED | `onRetryOrSettingsClicked()` calls `requestLocationPermission()` when not permanently denied (`SpeedScreen.kt:187-195`), wired to the Action via `ParkedOnlyOnClickListener.create { onRetryOrSettingsClicked() }` (`SpeedScreen.kt:271`) |
| 10 | Il dialogo non viene mai rilanciato automaticamente in loop dopo un rifiuto, né due volte in parallelo | ✓ VERIFIED | `Denied` branch of the `when` in `collectLatest` is `Unit` (no auto-relaunch, D-06, `SpeedScreen.kt:120-123`); `requestInFlight` guard prevents concurrent `requestPermissions()` calls (`SpeedScreen.kt:67, 104-107, 134-135, 188`) |
| 11 | Lo schermo auto non colleziona mai `GpsSpeedProvider` senza permesso concesso (T-08-08 preservato) | ✓ VERIFIED | `provider?.gpsSpeedProvider?.state?.collect { ... }` appears exactly once in the file, only inside the `Granted` branch (`SpeedScreen.kt:111-116`); `Granted` is only ever produced from `ContextCompat.checkSelfPermission(...) == PERMISSION_GRANTED`, never from the persisted counter |
| 12 | Nessun branding/personalizzazione del dialogo di permesso o del template (D-07) | ✓ VERIFIED | `grep` for `androidx.car.app.theme`/`carPermissionActivityLayout` returns 0 hits in `AndroidManifest.xml` and `SpeedScreen.kt`; every `buildTemplate()` branch returns `PaneTemplate.Builder(pane.build()).setHeaderAction(Action.APP_ICON).build()` with no `setActionStrip`/`setTitle` |
| 13 | La forma del template per ognuno dei quattro stati del permesso è verificata automaticamente | ✓ VERIFIED | `SpeedScreenTemplateTest.kt` calls `SpeedScreen.buildTemplate(permission, speed)` directly with all four states; **re-executed live during this verification** on a connected device (`adb devices` → `359592a5 device`): `./gradlew.bat :app:connectedDebugAndroidTest` → `TEST-KB2003 - 14-_app-.xml` shows `tests="11" failures="0" errors="0"`, including all 6 permission-state tests |
| 14 | Lo switch a due stati messaggio+azione (Riprova/Apri impostazioni) è lockato da asserzioni | ✓ VERIFIED | `deniedOnce_showsShortMessageWithRetryAction` and `deniedPermanently_showsSettingsMessageWithOpenSettingsAction` assert exact row title + single Action title from `context.getString(...)` — both passed in the live re-run above |
| 15 | Una persona ha verificato dal vivo su DHU l'intero percorso (richiesta automatica, concessione, rifiuto+retry, rifiuto permanente+impostazioni) | ✓ VERIFIED | 09-CONTEXT.md D-08: user-run DHU session confirming Scenarios A–F point-by-point (A1-A2, B1-B2, C1-C3, D1-D3, E1-E3, F1-F2), recorded 2026-09-02. Treated as valid human-verification evidence per task instructions — not re-run |
| 16 | La transizione Pane-di-sola-Row ↔ Pane-con-Row+Action non fa chiudere l'app dall'host (Pitfall 4) | ✓ VERIFIED | 09-CONTEXT.md D-08, Scenario E: host never closed the app, no host error, stable PID across all transitions |
| 17 | Il limite noto di piattaforma (Pitfall 1 — richiesta ignorabile dall'host a veicolo in movimento) ha una disposizione esplicita | ✓ VERIFIED | 09-CONTEXT.md D-09: user explicitly answered "Accettato" for v2.0; recorded in STATE.md as a known, accepted concern; no change requested to D-05/D-06 |

**Score:** 17/17 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `app/src/main/java/com/sed/tachimetro/car/CarPermissionState.kt` | Sealed model + pure resolver + sanitizer | ✓ VERIFIED | 57 lines (≥40 required); `sealed class CarPermissionState` present; zero `android`/`androidx` imports; wired into `SpeedScreen.kt` and `CarPermissionStateTest.kt` |
| `app/src/main/java/com/sed/tachimetro/car/CarPermissionDenialStore.kt` | Persisted denial counter | ✓ VERIFIED | 34 lines; `class CarPermissionDenialStore(context: Context)` with exactly `denialCount()`/`recordDenial()`; wired into `SpeedScreen.kt` |
| `app/src/test/java/com/sed/tachimetro/car/CarPermissionStateTest.kt` | JVM tests locking resolver | ✓ VERIFIED | 86 lines, 10 `assertEquals`; re-run during verification: `BUILD SUCCESSFUL`, 0 failures |
| `app/src/main/res/values/strings.xml` | 3 new car-screen strings | ✓ VERIFIED | `car_check_your_phone`, `car_permission_denied`, `car_permission_denied_permanent` present, exact text matches D-01/D-02/D-04, existing phone strings untouched |
| `app/src/main/java/com/sed/tachimetro/car/SpeedScreen.kt` | Reactive permission state machine + template rendering | ✓ VERIFIED | 316 lines (≥150 required); contains `resolveCarPermissionState(`, `requestPermissions(`, `buildTemplate(`; wired to `CarPermissionState`/`CarPermissionDenialStore` |
| `app/src/androidTest/java/com/sed/tachimetro/car/SpeedScreenTemplateTest.kt` | Instrumented test of template shape per permission state | ✓ VERIFIED | 264 lines (≥150 required); contains `buildTemplate(`; **actually executed on a real device during this verification, 10/10 passed** |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| `CarPermissionDenialStore.kt` | `MaxSpeedStore.PREFS_NAME` | reuse of shared prefs file | ✓ WIRED | `context.getSharedPreferences(MaxSpeedStore.PREFS_NAME, ...)` present (flagged by code review as a cross-domain import — WR-01 — but functionally wired exactly as the plan specified) |
| `CarPermissionDenialStore.kt` | `sanitizeDenialCount` | sanitize on read | ✓ WIRED | `denialCount(): Int = sanitizeDenialCount(prefs.getInt(...))` |
| `CarPermissionStateTest.kt` | `resolveCarPermissionState` | assertEquals per branch | ✓ WIRED | 10 assertions covering all branches; re-run passes |
| `SpeedScreen.kt` | `CarContext.requestPermissions` | explicit request (AA-04) | ✓ WIRED | `carContext.requestPermissions(listOf(ACCESS_FINE_LOCATION), ...) { _, _ -> ... }` |
| `SpeedScreen.kt` | `CarPermissionDenialStore` | read/increment denial count | ✓ WIRED | `denialStore.denialCount()` (pre-read) then `denialStore.recordDenial()` in the callback |
| `SpeedScreen.kt` | `ParkedOnlyOnClickListener` | retry/settings action gated to parked-only | ✓ WIRED | `ParkedOnlyOnClickListener.create { onRetryOrSettingsClicked() }` |
| `SpeedScreen.kt` | `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` | open phone settings on permanent denial | ✓ WIRED | `openAppSettingsFromCar()` builds the intent with `FLAG_ACTIVITY_NEW_TASK` and `carContext.packageName` |
| `SpeedScreen.kt` | `Pane.Builder.addAction` | retry/settings Action in PaneTemplate | ✓ WIRED | Exactly one `pane.addAction(...)` call, only in the `Denied` branch |
| `SpeedScreenTemplateTest.kt` | `SpeedScreen.buildTemplate` | direct seam call with injected state | ✓ WIRED | `screen.buildTemplate(permission, speed)` invoked inside `runOnMainSync` |
| `SpeedScreenTemplateTest.kt` | `CarPermissionState` | injection of all four states | ✓ WIRED | All four states injected across 6 dedicated tests |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|---------------------|--------|
| `SpeedScreen.buildTemplate` (permission branch) | `permissionState.value` | `ContextCompat.checkSelfPermission()` (real OS permission check) + `CarPermissionDenialStore.denialCount()` (real `SharedPreferences`) | Yes | ✓ FLOWING |
| `SpeedScreen.buildTemplate` (speed branch, `Granted`) | `latestState` | `TachimetroApplication.gpsSpeedProvider.state` (real `FusedLocationProviderClient` StateFlow, unchanged from Phase 8) | Yes | ✓ FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Pure resolver/sanitizer unit tests | `./gradlew.bat :app:testDebugUnitTest --tests 'com.sed.tachimetro.car.CarPermissionStateTest'` | BUILD SUCCESSFUL | ✓ PASS |
| Full JVM unit test suite (no regressions) | `./gradlew.bat :app:testDebugUnitTest` | BUILD SUCCESSFUL | ✓ PASS |
| App compiles with new permission flow | `./gradlew.bat :app:assembleDebug` | BUILD SUCCESSFUL | ✓ PASS |
| Instrumented test compiles | `./gradlew.bat :app:compileDebugAndroidTestKotlin` | BUILD SUCCESSFUL | ✓ PASS |
| Template shape for all 4 permission states, on a real device | `./gradlew.bat :app:connectedDebugAndroidTest` (device `359592a5`/`KB2003 - 14`) | `tests="11" failures="0" errors="0"` per `TEST-KB2003 - 14-_app-.xml`; all 6 new/updated permission-state tests present and green | ✓ PASS |
| No new runtime permission or theme declaration introduced | `grep -c '<uses-permission' AndroidManifest.xml` → 1; `grep -c 'androidx.car.app.theme\|carPermissionActivityLayout'` → 0 | as expected | ✓ PASS |

### Probe Execution

No `scripts/*/tests/probe-*.sh` conventions and no probes declared in this phase's PLAN/SUMMARY files. Skipped: no runnable probes for this phase.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|--------------|--------|----------|
| AA-04 | 09-01, 09-02, 09-03 | Se il permesso non è ancora stato concesso al primo collegamento Android Auto, lo schermo auto lo richiede esplicitamente invece di restare vuoto/bloccato | ✓ SATISFIED | `SpeedScreen.requestLocationPermission()` calls `CarContext.requestPermissions()` automatically from the `NotRequested` state (D-05); full grant/deny/retry/permanent-denial flow implemented and confirmed live on DHU (D-08); marked `[x]` in REQUIREMENTS.md and ROADMAP.md |

No orphaned requirements: REQUIREMENTS.md maps only AA-04 to Phase 9, and it is claimed in all three plans' `requirements:` frontmatter.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `CarPermissionDenialStore.kt` | 5, 21 | Imports `MaxSpeedStore` solely to reuse `PREFS_NAME`, unlike `ScreenOnPreferenceStore`/`DistanceStore` which declare their own constant (09-REVIEW.md WR-01) | ℹ️ Info | Functionally harmless (same underlying prefs file); creates an unnecessary `car → maxspeed` dependency edge. Non-blocking, advisory. |
| `SpeedScreen.kt` | 78-128 | Permission revocation while the car `Screen` stays continuously `STARTED` is never re-detected — `refreshPermissionState()` runs once per `STARTED` entry, then the `Granted` branch collects `GpsSpeedProvider.state` indefinitely (09-REVIEW.md WR-02) | ℹ️ Info | Edge case (external revocation mid-session) not covered by any must-have or roadmap SC for this phase; does not cause a crash (T-08-08 gate still prevents unpermitted collection at the moment collection starts). Advisory, not a phase blocker. |
| `SpeedScreen.kt` | 170-174 vs `CarPermissionState.kt:53` | "Permanent denial" threshold duplicated as `wasAlreadyDenied = denialCount() > 0` instead of routing through `resolveCarPermissionState()` (09-REVIEW.md WR-03) | ℹ️ Info | Mathematically equivalent today; a future threshold change would need updating in two places. Advisory, not a phase blocker. |

No debt markers (`TBD`/`FIXME`/`XXX`) found in any file modified by this phase (one grep hit on `SpeedScreenTemplateTest.kt` was a false positive — the Italian word "metodo" contains the substring "todo").

No Critical/Blocker findings — consistent with `09-REVIEW.md` (0 critical, 3 warning, 2 info, all advisory).

### Human Verification Required

None. The single human-check gate declared in this phase's plans (09-03-PLAN.md Task 2, DHU live session) was already executed by the user and its results are formally recorded in `09-CONTEXT.md` as decisions D-08 (all scenarios A-F confirmed) and D-09 (Pitfall 1 explicitly accepted for v2.0). Per task instructions this is treated as valid, already-completed human-verification evidence and is not re-requested here.

### Gaps Summary

No gaps. All 17 merged must-haves (3 roadmap Success Criteria + 5 Plan 01 truths + 4 Plan 02 truths + 5 Plan 03 truths, minus duplicates folded into the roadmap wording) are verified against the actual codebase, not just against SUMMARY.md claims:

- Independently re-ran `testDebugUnitTest`, `assembleDebug`, `compileDebugAndroidTestKotlin`, and — critically — `connectedDebugAndroidTest` on a real connected device during this verification (not just trusting the SUMMARY's prior run), confirming 10/10 `SpeedScreenTemplateTest` cases pass with 0 failures.
- Confirmed the manifest carries no new permission and no theme/branding declarations, matching D-07.
- Confirmed `git diff --stat` for the phase touches only the 6 files declared across the three plans' `files_modified`, plus documentation.
- Code review findings (WR-01/WR-02/WR-03) are real but advisory-only maintainability notes, not must-have failures — none of them contradicts a declared truth, artifact, or key link for this phase.
- The three roadmap Success Criteria and the two platform-limitation decisions (Pitfall 1/4) rely on the user's own live DHU session, recorded as D-08/D-09, accepted per this verification's task instructions as valid evidence.

---

_Verified: 2026-09-02T15:10:00Z_
_Verifier: Claude (gsd-verifier)_
