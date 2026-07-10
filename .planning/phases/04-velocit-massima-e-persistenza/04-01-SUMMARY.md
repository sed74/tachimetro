---
phase: 04-velocit-massima-e-persistenza
plan: 01
subsystem: ui
tags: [kotlin, android, sharedpreferences, tdd, constraintlayout]

# Dependency graph
requires:
  - phase: 03-interfaccia-tachimetro
    provides: "activity_main.xml con unitText top-right e pattern applyUnitTextWindowInsets() da specchiare; SpeedState.Reading come fonte del valore corrente"
provides:
  - "Funzioni pure reduceMax(currentMax, reading) e sanitizePersistedMax(raw) in com.sed.tachimetro.maxspeed, con test JVM completi"
  - "MaxSpeedStore: wrapper SharedPreferences app-private per la persistenza del massimo (read/write)"
  - "maxSpeedText + resetMaxButton cablati in MainActivity: lettura all'avvio, aggiornamento/persistenza immediata, reset senza dialog, visibilità condizionale, window insets top+left"
affects: [04-02-verifica-device]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Funzioni pure framework-free per logica di dominio testabile via JVM unit test (stesso pattern di SpeedMapping.kt/SpeedMappingTest.kt)"
    - "Wrapper SharedPreferences monouso per un singolo valore primitivo, niente Room/DataStore per dati minimi"
    - "Window insets speculari per elementi ai due angoli superiori (applyUnitTextWindowInsets / applyMaxAreaWindowInsets)"

key-files:
  created:
    - app/src/main/java/com/sed/tachimetro/maxspeed/MaxSpeedReducer.kt
    - app/src/main/java/com/sed/tachimetro/maxspeed/MaxSpeedStore.kt
    - app/src/test/java/com/sed/tachimetro/maxspeed/MaxSpeedReducerTest.kt
  modified:
    - app/src/main/res/values/strings.xml
    - app/src/main/res/layout/activity_main.xml
    - app/src/main/java/com/sed/tachimetro/MainActivity.kt

key-decisions:
  - "MaxSpeedStore.write() usa apply() (asincrono, off-main-thread), mai commit() bloccante, per non introdurre jank sull'aggiornamento 1/sec del GPS"
  - "sanitizePersistedMax applicato sia in lettura da disco (MaxSpeedStore.read()) sia difensivamente dentro reduceMax, per bloccare T-04-01 su entrambi i lati (disco e memoria)"

patterns-established:
  - "Pattern 1: logica di dominio pura (nessun import android.*) + test JVM in package parallela sotto src/test, seguendo esattamente lo stile di gps/SpeedMapping.kt"
  - "Pattern 2: elementi UI ancorati agli angoli devono avere un metodo applyXWindowInsets() dedicato che somma insets live ai margini XML base, senza riusare listener di altri elementi"

requirements-completed: [MAX-01, MAX-02, MAX-03]

# Metrics
duration: 5min
completed: 2026-07-10
---

# Phase 04 Plan 01: Velocità Massima e Persistenza (logica + wiring) Summary

**Monitoraggio velocità massima con crescita monotona (`reduceMax`) e persistenza immediata su SharedPreferences (`MaxSpeedStore`), cablati in MainActivity con area "MAX &lt;n&gt;" top-left nascosta finché il massimo è 0.**

## Performance

- **Duration:** ~5 min
- **Started:** 2026-07-10T15:44:41+02:00
- **Completed:** 2026-07-10T15:49:58+02:00
- **Tasks:** 3
- **Files modified:** 6 (3 modificati, 3 nuovi)

## Accomplishments
- Area "MAX 120" in alto a sinistra (speculare a `unitText`), nascosta di default, con pulsante "Azzera massimo" sotto (48dp min touch target), entrambi mai mostrati insieme a `retryButton`
- Logica pura `reduceMax`/`sanitizePersistedMax` con 8 test JVM verdi che bloccano D-07 (crescita monotona) e T-04-01 (sanitizzazione valore manomesso/negativo)
- `MaxSpeedStore` persiste il massimo su SharedPreferences app-private con scrittura `apply()` immediata a ogni nuovo record e all'azzeramento
- `MainActivity` legge il massimo salvato in `onCreate()` prima di avviare la raccolta GPS (nessun flash "MAX 0"), aggiorna/persiste il massimo in `updatePlaceholder(Reading)`, applica window insets top+left speculari a `unitText`

## Task Commits

Ciascun task è stato committato atomicamente:

1. **Task 1: Stringhe italiane e layout (maxSpeedText + resetMaxButton)** - `f2df651` (feat)
2. **Task 2: Logica pura del massimo con test JVM (TDD)**
   - RED - `396fefd` (test): test JVM falliti (funzioni non ancora esistenti)
   - GREEN - `edbb05f` (feat): `MaxSpeedReducer.kt` + `MaxSpeedStore.kt`, tutti i test verdi
3. **Task 3: Wiring MainActivity** - `2cd0668` (feat)

**Plan metadata:** (da aggiungere dall'orchestratore dopo il merge)

## Files Created/Modified
- `app/src/main/res/values/strings.xml` - Aggiunte `max_speed_format` ("MAX %1$d") e `reset_max_button` ("Azzera massimo")
- `app/src/main/res/layout/activity_main.xml` - Aggiunti `maxSpeedText` (top-left, 22sp, GONE) e `resetMaxButton` (sotto, 48dp min, GONE)
- `app/src/main/java/com/sed/tachimetro/maxspeed/MaxSpeedReducer.kt` - Funzioni pure `reduceMax`/`sanitizePersistedMax`
- `app/src/main/java/com/sed/tachimetro/maxspeed/MaxSpeedStore.kt` - Wrapper SharedPreferences read()/write() per un singolo Int
- `app/src/test/java/com/sed/tachimetro/maxspeed/MaxSpeedReducerTest.kt` - 8 test JVM (5 su reduceMax, 3 su sanitizePersistedMax)
- `app/src/main/java/com/sed/tachimetro/MainActivity.kt` - Campi/lettura all'avvio, aggiornamento in `updatePlaceholder`, `onResetMaxClicked()`, `updateMaxArea()`, `applyMaxAreaWindowInsets()`, occultamento in `showDenied()`, ripristino in `showReady()`

## Decisions Made
- Nessuna decisione architetturale nuova oltre a quanto già fissato dal CONTEXT (D-01..D-09) e dallo UI-SPEC — il piano ha specificato l'implementazione esatta e non sono state necessarie deviazioni.

## Deviations from Plan

None - plan executed exactly as written. L'unica estensione minima rispetto al testo letterale del piano è la sequenza esplicita RED/GREEN per il Task 2 (richiesta dal flag `tdd="true"` e dal workflow dell'executor): il test è stato committato separatamente allo stato fallito prima dell'implementazione, poi l'implementazione è stata committata a parte una volta verdi tutti i test. Non è una deviazione di contenuto, solo di granularità dei commit.

## Issues Encountered
- Il worktree non aveva `local.properties` (file gitignored, non presente nel checkout isolato) — creato localmente con lo stesso `sdk.dir` del repo principale per poter eseguire `gradlew.bat`. File non tracciato/non commitato (resta gitignored).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Logica e wiring pronti per la verifica comportamentale su device nel piano 04-02 (checkpoint human-verify): display "MAX &lt;n&gt;", tap "Azzera massimo", persistenza dopo chiusura app e dopo riavvio telefono, window insets in entrambi gli orientamenti.
- Nessun blocco noto. `./gradlew.bat :app:assembleDebug` e `./gradlew.bat :app:testDebugUnitTest` verdi.

---
*Phase: 04-velocit-massima-e-persistenza*
*Completed: 2026-07-10*

## Self-Check: PASSED

All created files found on disk; all task commit hashes (f2df651, 396fefd, edbb05f, 2cd0668) found in git log.
