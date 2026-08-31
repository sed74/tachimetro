---
phase: 08-fondamenta-condivise-e-velocit-sullo-schermo-auto
plan: 01
subsystem: infra
tags: [android-auto, car-app-library, kotlin, gps, tdd, application-scope]

# Dependency graph
requires:
  - phase: 07-distanza-e-reset-condiviso
    provides: GpsSpeedProvider con SpeedState (Searching/Reading/NoSignal), NOISE_FLOOR_KMH condiviso
provides:
  - "TachimetroApplication: GpsSpeedProvider ora Application-scoped, condiviso tra telefono e futuro schermo auto"
  - "androidx.car.app:app 1.7.0 / app-testing pinnati nel version catalog"
  - "BuildConfig abilitato (buildFeatures.buildConfig = true)"
  - "R.string.car_searching_gps_signal, distinto da searching_gps_signal"
  - "CarSpeedContent (Speed/Searching) + carSpeedContent(SpeedState): contratto puro testabile SpeedState -> contenuto Row auto"
affects: [08-02-schermo-velocita-auto, 08-03]

# Tech tracking
tech-stack:
  added: ["androidx.car.app:app:1.7.0", "androidx.car.app:app-testing:1.7.0"]
  patterns:
    - "Application-scoped shared service (by lazy, no DI framework) per una sorgente dati unica per processo condivisa tra piu' superfici (telefono + auto)"
    - "Contratto di contenuto puro sealed-class + funzione top-level (mirror di DistanceDisplay/formatDistanceDisplay) per isolare la logica di stato dal rendering, testabile senza runtime Android"

key-files:
  created:
    - app/src/main/java/com/sed/tachimetro/TachimetroApplication.kt
    - app/src/main/java/com/sed/tachimetro/car/CarSpeedContent.kt
    - app/src/test/java/com/sed/tachimetro/car/CarSpeedContentTest.kt
  modified:
    - gradle/libs.versions.toml
    - app/build.gradle.kts
    - app/src/main/res/values/strings.xml
    - app/src/main/AndroidManifest.xml
    - app/src/main/java/com/sed/tachimetro/MainActivity.kt
    - app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt

key-decisions:
  - "androidx.car.app pinnato a 1.7.0 esatto (prima release stable con fix per CVE-2024-10382), niente app-automotive/app-projected"
  - "GpsSpeedProvider promosso a Application scope via TachimetroApplication.gpsSpeedProvider (by lazy), MainActivity legge invece di costruire; niente collector eager a livello di Application"
  - "MainActivity.onDestroy() non chiama piu' gpsSpeedProvider.close() -- il provider condiviso deve sopravvivere all'Activity per il futuro schermo auto"
  - "car_searching_gps_signal = 'Ricerca segnale...' come risorsa separata da searching_gps_signal (D-02)"
  - "CarSpeedContent framework-free: nessun import Android, nessun R.string, when esaustivo senza else"

patterns-established:
  - "Pattern: servizio condiviso a livello di processo esposto da una Application subclass con by lazy, mai un collector eager in Application.onCreate()"
  - "Pattern: mirror di DistanceDisplay/formatDistanceDisplay per ogni nuovo contratto di contenuto puro (sealed class + funzione top-level + test JUnit plain)"

requirements-completed: [AA-01, AA-02]

# Metrics
duration: ~20min
completed: 2026-08-31
---

# Phase 08 Plan 01: Fondamenta condivise per lo schermo auto Summary

**GpsSpeedProvider promosso a scope Application (unica sorgente GPS condivisa telefono/auto), dipendenza androidx.car.app 1.7.0 aggiunta, e contratto di contenuto puro CarSpeedContent (SpeedState -> Speed/Searching) coperto da 5 test JUnit TDD.**

## Performance

- **Duration:** ~20 min
- **Started:** 2026-08-31T09:59:37+02:00 (base commit)
- **Completed:** 2026-08-31T10:07:31+02:00
- **Tasks:** 3 completed (Task 3 con ciclo TDD RED -> GREEN)
- **Files modified:** 9 (3 creati, 6 modificati)

## Accomplishments
- `GpsSpeedProvider` ora costruito in un solo punto del codebase (`TachimetroApplication`), mai piu' in `MainActivity` -- elimina il rischio di due sottoscrizioni GPS parallele (batteria doppia, valori disallineati) quando il Piano 02 aggiungera' lo schermo auto
- Dipendenza `androidx.car.app:app:1.7.0` (+ `app-testing`) pinnata e risolta, `BuildConfig` abilitato per il gating del logging diagnostico del Piano 02
- Contratto puro `carSpeedContent(SpeedState) -> CarSpeedContent` che unifica `Searching`/`NoSignal` sotto un'unica copia piu' corta per l'auto (D-02, AA-02), bloccato da 5 test JUnit senza runtime Android

## Task Commits

Each task was committed atomically:

1. **Task 1: Dipendenza Car App Library, buildConfig e stringa di stato auto** - `ada5f8f` (feat)
2. **Task 2: Promuovere GpsSpeedProvider a scope Application** - `e7983ba` (feat)
3. **Task 3: Contratto di contenuto puro per la Row dello schermo auto** - `15f664c` (test, RED) + `6d110de` (feat, GREEN)

_Task 3 e' TDD: nessun commit refactor separato, l'implementazione GREEN e' passata al primo tentativo senza bisogno di refactoring._

## Files Created/Modified
- `gradle/libs.versions.toml` - alias `car-app`/`car-app-testing` pinnati a `carApp = "1.7.0"`
- `app/build.gradle.kts` - dipendenze `libs.car.app`/`libs.car.app.testing`, `buildFeatures { buildConfig = true }`
- `app/src/main/res/values/strings.xml` - nuova stringa `car_searching_gps_signal`
- `app/src/main/AndroidManifest.xml` - `android:name=".TachimetroApplication"` sul tag `<application>`
- `app/src/main/java/com/sed/tachimetro/TachimetroApplication.kt` - Application subclass, proprieta' `gpsSpeedProvider` by lazy
- `app/src/main/java/com/sed/tachimetro/MainActivity.kt` - `setupGpsCollection()` legge il provider condiviso, `onDestroy()` non lo chiude piu'
- `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt` - solo commenti aggiornati (scope ownership, KDoc di `close()`), nessuna modifica di logica
- `app/src/main/java/com/sed/tachimetro/car/CarSpeedContent.kt` - `sealed class CarSpeedContent` + `carSpeedContent()`
- `app/src/test/java/com/sed/tachimetro/car/CarSpeedContentTest.kt` - 5 test JUnit del contratto

## Decisions Made
- Nessuna deviazione dal piano: tutte le decisioni (D-00b, D-01, D-02, AA-02, versione `androidx.car.app` pinnata a 1.7.0 per CVE-2024-10382) erano gia' bloccate in `08-CONTEXT.md`/`08-UI-SPEC.md` ed eseguite come specificato.
- Aggiustamento minore in fase di verifica: la KDoc iniziale di `CarSpeedContent.kt` citava letteralmente `R.string` come riferimento testuale nel commento, facendo scattare il grep di acceptance criteria (`grep -c 'R\.string'` doveva restituire 0). Riformulato il commento senza quella stringa letterale -- nessun cambio di comportamento, solo wording del commento.

## Deviations from Plan

None - plan executed exactly as written (a parte l'aggiustamento di wording del commento sopra, non un cambio di comportamento o scope).

## Issues Encountered
- Il worktree non aveva `local.properties` (file gitignored, non versionato): creato localmente con lo stesso `sdk.dir` del repo principale per poter eseguire `./gradlew.bat`. File non tracciato da git, nessun impatto sul commit.
- Il worktree branch era ancorato a un commit precedente alla creazione del piano di fase 8 (`d671c2c`, ramo v1.1 milestone archival); verificato che il commit base atteso (`3263fc84`) fosse un discendente fast-forward e corretto con `git reset --hard` come da protocollo `worktree_branch_check`, senza perdita di lavoro.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Il Piano 02 puo' ora costruire `SpeedScreen` (Car App Library) leggendo `(application as TachimetroApplication).gpsSpeedProvider.state` e mappando ogni emissione con `carSpeedContent()` per popolare la Row del template, senza aprire una seconda sottoscrizione GPS
- `BuildConfig.DEBUG` disponibile per gating-are il log di conteggio refresh richiesto dal Piano 02 (T-08-03)
- Nessun blocco noto per il Piano 02/03

---
*Phase: 08-fondamenta-condivise-e-velocit-sullo-schermo-auto*
*Completed: 2026-08-31*
