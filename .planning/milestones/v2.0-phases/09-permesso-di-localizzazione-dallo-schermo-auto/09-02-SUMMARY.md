---
phase: 09-permesso-di-localizzazione-dallo-schermo-auto
plan: 02
subsystem: android-auto-permissions
tags: [android-auto, car-app-library, permissions, kotlin, statemachine, reactive-flow]

# Dependency graph
requires:
  - phase: 09-permesso-di-localizzazione-dallo-schermo-auto (Plan 01)
    provides: "CarPermissionState sealed model, resolveCarPermissionState() pure resolver, CarPermissionDenialStore persisted counter, three Italian car-screen strings"
provides:
  - "SpeedScreen reactive permission state machine (permissionState: MutableStateFlow<CarPermissionState>) replacing the T-08-08 passive gate"
  - "Automatic CarContext.requestPermissions() trigger on first Android Auto connection (AA-04, D-05)"
  - "requestInFlight in-memory guard against concurrent permission requests (Pitfall 2)"
  - "onRetryOrSettingsClicked()/openAppSettingsFromCar() manual retry/settings flow (D-03/D-04)"
  - "buildTemplate(permission, speed): PaneTemplate public seam rendering all four permission states, with retry/settings Action wrapped in ParkedOnlyOnClickListener"
affects: [09-03]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Reactive permission-state StateFlow + collectLatest inside repeatOnLifecycle(STARTED), mirroring MainActivity.setupGpsCollection()/permissionGranted exactly, adapted to CarContext"
    - "Public buildTemplate(permission, speed) seam extracted from onGetTemplate() override for instrumented-test injectability (Plan 03)"
    - "Two-state Action label switch (Riprova/Apri impostazioni) mirroring MainActivity.showDenied(), wrapped in ParkedOnlyOnClickListener instead of a plain OnClickListener"

key-files:
  created: []
  modified:
    - app/src/main/java/com/sed/tachimetro/car/SpeedScreen.kt

key-decisions:
  - "requestInFlight set to true before calling carContext.requestPermissions() and CarPermissionState.Waiting assigned AFTER the call returns, per plan's exact ordering, so collectLatest's cancellation of the previous collector can never race ahead of the request itself"
  - "Permission grant/denial in the requestPermissions() callback is re-derived from ContextCompat.checkSelfPermission() rather than trusted from the approved/rejected lists, mirroring MainActivity's own discipline of never trusting the raw launcher callback flag"
  - "wasAlreadyDenied captured from denialStore.denialCount() BEFORE calling recordDenial(), so the very first denial can never appear as permanent (D-04 ordering constraint)"
  - "buildTemplate() made public (not private) specifically as a test seam for Plan 03's instrumented tests, per the plan's explicit interface contract"

patterns-established:
  - "CarPermissionState and CarSpeedContent remain two orthogonal sealed models: buildTemplate() branches on permission first, only falling through to carSpeedContent(speed) in the Granted case"

requirements-completed: [AA-04]

# Metrics
duration: ~10min
completed: 2026-09-02
---

# Phase 09 Plan 02: Richiesta del permesso dallo schermo auto Summary

**SpeedScreen ora richiede automaticamente ACCESS_FINE_LOCATION al primo collegamento Android Auto (CarContext.requestPermissions()), transita reattivamente a Granted senza riavvii (SC2), e rende tutti e quattro gli stati di CarPermissionState nel PaneTemplate con un'Action di retry/impostazioni eseguibile solo a veicolo fermo.**

## Performance

- **Duration:** ~10 min
- **Started:** 2026-09-02T11:50:51Z (subito dopo la chiusura del Piano 01)
- **Completed:** 2026-09-02T11:59:30Z
- **Tasks:** 2 completed
- **Files modified:** 1

## Accomplishments
- Sostituito il gate difensivo passivo T-08-08 (che lasciava lo schermo auto bloccato per sempre su "Ricerca segnale..." senza permesso) con una macchina a stati reattiva (`permissionState: MutableStateFlow<CarPermissionState>`) che richiede da sola il permesso al primo collegamento -- questo e' il deliverable della fase (AA-04)
- Implementate entrambe le guardie anti-loop richieste dal threat model: `requestInFlight` in memoria contro richieste concorrenti (T-09-06/Pitfall 2) e il contatore persistito che impedisce ogni rilancio automatico dopo un rifiuto gia' ricevuto (D-06)
- Estesa `onGetTemplate()` in un seam pubblico `buildTemplate(permission, speed)` che rende tutti e quattro gli stati del permesso, con l'Action di retry/impostazioni sempre avvolta in `ParkedOnlyOnClickListener` (T-09-08) -- nessuna logica di driving-state fatta in casa
- Preservata intatta la garanzia T-08-08: `GpsSpeedProvider.state` viene collezionato esclusivamente nel ramo `Granted`, mai altrove nel file

## Task Commits

Each task was committed atomically:

1. **Task 1: Macchina a stati del permesso e richiesta automatica in SpeedScreen** - `f046a2f` (feat)
2. **Task 2: Rendering degli stati di permesso nel PaneTemplate con Action di retry/impostazioni** - `e5d5d20` (feat)

**Plan metadata:** committed separately after this SUMMARY.

## Files Created/Modified
- `app/src/main/java/com/sed/tachimetro/car/SpeedScreen.kt` - Sostituito il gate T-08-08 con `permissionState`/`denialStore`/`requestInFlight`, `refreshPermissionState()`, `requestLocationPermission()`, `onRetryOrSettingsClicked()`, `openAppSettingsFromCar()`; estratto `buildTemplate(permission, speed): PaneTemplate` da `onGetTemplate()`, che ora rende tutti e quattro gli stati del permesso con l'Action di retry/impostazioni

## Decisions Made
Nessuna deviazione dalle decisioni gia' bloccate in CONTEXT.md/RESEARCH.md/PATTERNS.md -- l'implementazione segue esattamente la macchina a stati, l'ordine delle operazioni (denialCount prima di recordDenial, Waiting dopo la chiamata a requestPermissions) e la forma del template gia' specificati nel piano.

## Deviations from Plan

None - plan executed exactly as written. Tutti i grep di acceptance criteria (presenza di `carContext.requestPermissions(`, `resolveCarPermissionState(`, `collectLatest`, singolo `recordDenial()`, assenza di `delay()`/`MaxSpeedStore`/`DistanceStore`/`ScreenOnPreferenceStore`/`shouldShowRequestPermissionRationale` fuori dai commenti, `buildTemplate(permission: CarPermissionState, speed: SpeedState): PaneTemplate`, `ParkedOnlyOnClickListener.create`, singola `addAction(`, assenza di `setActionStrip`/`androidx.car.app.theme`/`carPermissionActivityLayout`) sono passati al primo tentativo, senza necessita' di fix iterativi.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- `SpeedScreen` espone ora `buildTemplate(permission: CarPermissionState, speed: SpeedState): PaneTemplate` come seam pubblico: pronto per i test strumentati del Piano 03, che potranno verificare la forma del `PaneTemplate` per ogni stato del permesso senza dover manipolare il permesso reale sul dispositivo di test
- Verifica manuale rapida sul telefono eseguita durante questo piano: `installDebug` + avvio dell'app + controllo logcat/processo -- nessun crash, nessuna regressione lato telefono (v1.1 invariata)
- **La verifica formale su DHU (Pitfall 4 di 09-RESEARCH.md: transizione di forma del Pane da Row-sola a Row+Action, quota refresh non testata per questo caso) resta scope del Piano 03** -- non eseguita in questo piano
- AA-04 marcato completo in questo Summary (`requirements-completed: [AA-04]`); il Piano 01 aveva lasciato il campo vuoto proprio in attesa di questo cablaggio
- Nessun blocco per il Piano 03

---
*Phase: 09-permesso-di-localizzazione-dallo-schermo-auto*
*Completed: 2026-09-02*

## Self-Check: PASSED

- FOUND: app/src/main/java/com/sed/tachimetro/car/SpeedScreen.kt
- FOUND: .planning/phases/09-permesso-di-localizzazione-dallo-schermo-auto/09-02-SUMMARY.md
- FOUND: commit f046a2f (feat: Task 1 permission state machine)
- FOUND: commit e5d5d20 (feat: Task 2 PaneTemplate rendering)
