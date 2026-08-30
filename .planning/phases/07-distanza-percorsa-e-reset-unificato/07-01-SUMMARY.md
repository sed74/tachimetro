---
phase: 07-distanza-percorsa-e-reset-unificato
plan: 01
subsystem: domain
tags: [kotlin, sharedpreferences, tdd, distance, pure-functions]

# Dependency graph
requires: []
provides:
  - "package com.sed.tachimetro.distance con reduceDistance, sanitizePersistedDistance, DistanceStore, DistanceDisplay, formatDistanceDisplay"
  - "gate soglia rumore D-04 applicato all'accumulo distanza (mirror del gate già usato in mapSpeedToKmh)"
  - "persistenza Float in metri su SharedPreferences (tachimetro_prefs / distance_meters), mai troncata"
  - "logica di formattazione adattiva metri/km (soglia inclusiva 1000m, D-01)"
affects: [07-02, 07-03, 07-04]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Funzioni pure top-level framework-free per logica di dominio testabile su JVM puro (mirror di MaxSpeedReducer/SpeedMapping)"
    - "Store SharedPreferences 1:1 con MaxSpeedStore, con Float al posto di Int"
    - "Sealed class a due varianti (Meters/Kilometers) che porta solo il numero, unità di misura delegata alla view (D-02)"

key-files:
  created:
    - app/src/main/java/com/sed/tachimetro/distance/DistanceReducer.kt
    - app/src/main/java/com/sed/tachimetro/distance/DistanceStore.kt
    - app/src/main/java/com/sed/tachimetro/distance/DistanceFormat.kt
    - app/src/test/java/com/sed/tachimetro/distance/DistanceReducerTest.kt
    - app/src/test/java/com/sed/tachimetro/distance/DistanceFormatTest.kt
  modified: []

key-decisions:
  - "reduceDistance usa lo stesso default noiseFloorKmh=2.0 di mapSpeedToKmh, nessuna costante duplicata"
  - "DistanceStore riusa la stessa PREFS_NAME \"tachimetro_prefs\" di MaxSpeedStore/ScreenOnPreferenceStore (stesso file di preferenze, costante ridichiarata localmente per restare framework-free per package)"
  - "formatDistanceDisplay decide il ramo metri/km sul valore grezzo non arrotondato (edge case 999.6m -> \"1000\" nel ramo metri, accettato in 07-UI-SPEC.md)"

patterns-established:
  - "Pattern 3 (07-PATTERNS.md): dominio distanza isolato in funzioni pure prima di qualunque consumo UI/GPS, additivo al 100% rispetto al codice esistente"

requirements-completed: [DIST-01, DIST-03]

# Metrics
duration: 12min
completed: 2026-08-30
---

# Phase 07 Plan 01: Dominio Distanza Summary

**Package `com.sed.tachimetro.distance` completo: accumulo distanza con gate rumore D-04 (`reduceDistance`), sanitizzazione (`sanitizePersistedDistance`), persistenza `Float` su SharedPreferences (`DistanceStore`) e formattazione adattiva metri/km (`formatDistanceDisplay`, D-01) — 15 test JVM, tutti verdi.**

## Performance

- **Duration:** 12 min
- **Started:** 2026-08-30T09:59:xxZ
- **Completed:** 2026-08-30T10:12:04Z
- **Tasks:** 2 completati
- **Files modified:** 5 (tutti nuovi, nessun file esistente toccato)

## Accomplishments
- `reduceDistance()` applica il gate D-04 (nessun accumulo sotto 2 km/h) con clamp difensivo di totale e delta negativi, 9 test JVM
- `DistanceStore` mirror 1:1 di `MaxSpeedStore` con `Float` invece di `Int`, persiste metri grezzi mai troncati su `tachimetro_prefs`/`distance_meters`
- `formatDistanceDisplay()` implementa la soglia adattiva D-01 a 1000 m (inclusiva), con l'edge case 999.6m documentato e bloccato da test, 6 test JVM
- Ciclo TDD RED→GREEN rispettato per entrambi i task, verificato in git log

## Task Commits

Ciascun task è stato committato con il ciclo TDD completo (test → feat):

1. **Task 1: Dominio distanza — reduceDistance, sanitizePersistedDistance, DistanceStore**
   - `9dd6ed4` (test) - RED: 9 test falliscono in compilazione (riferimenti non risolti)
   - `15c4f07` (feat) - GREEN: 9/9 test passano
2. **Task 2: Formattazione adattiva metri/chilometri (D-01)**
   - `358b745` (test) - RED: 6 test falliscono in compilazione (riferimenti non risolti)
   - `4f1871f` (feat) - GREEN: 6/6 test passano, `assembleDebug` + `testDebugUnitTest` -> BUILD SUCCESSFUL

_Nota: entrambi i task sono TDD (`tdd="true"`), quindi due commit ciascuno (RED test → GREEN feat), nessun refactor necessario._

## Files Created/Modified
- `app/src/main/java/com/sed/tachimetro/distance/DistanceReducer.kt` - `reduceDistance()` (gate D-04) e `sanitizePersistedDistance()`, funzioni pure top-level
- `app/src/main/java/com/sed/tachimetro/distance/DistanceStore.kt` - Persistenza SharedPreferences della distanza in metri (`Float`)
- `app/src/main/java/com/sed/tachimetro/distance/DistanceFormat.kt` - `DistanceDisplay` sealed class e `formatDistanceDisplay()` (D-01)
- `app/src/test/java/com/sed/tachimetro/distance/DistanceReducerTest.kt` - 9 test JVM per gate D-04 e sanitizzazione
- `app/src/test/java/com/sed/tachimetro/distance/DistanceFormatTest.kt` - 6 test JVM per la soglia adattiva m/km

## Decisions Made
- Nessuna deviazione dal contratto `<interfaces>` del piano: firme di `reduceDistance`, `sanitizePersistedDistance`, `DistanceStore`, `DistanceDisplay` e `formatDistanceDisplay` rispettano esattamente quanto vincolato per il Piano 03
- `PREFS_NAME` ridichiarata localmente in `DistanceStore` (non importata da `MaxSpeedStore`) come esplicitamente richiesto dal piano, per mantenere i package indipendenti

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
- Il worktree non aveva `local.properties` (file gitignored, non presente in nessun worktree Git per definizione). Copiato da `sdk.dir` del repo principale per eseguire `gradlew.bat`; file resta non tracciato e non committato, coerente con `.gitignore`.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Il Piano 03 (consumo UI) può ora importare `reduceDistance`, `DistanceStore`, `formatDistanceDisplay` e `DistanceDisplay` con firme stabili e testate
- Nessun file esistente toccato (`MainActivity.kt`, `gps/*` invariati) — il Piano 02 può procedere in parallelo senza conflitti
- Nessun blocco noto

---
*Phase: 07-distanza-percorsa-e-reset-unificato*
*Completed: 2026-08-30*
