---
phase: 10-comportamento-del-telefono-alla-connessione-android-auto
plan: 02
subsystem: android-auto-integration
tags: [kotlin, androidx-car-app, car-connection, livedata, main-activity]

# Dependency graph
requires:
  - phase: 10-comportamento-del-telefono-alla-connessione-android-auto
    provides: "CarLinkState/resolveCarLinkState/resolveEffectiveKeepScreenOn (Piano 01) e la stringa android_auto_connected"
provides:
  - "MainActivity osserva CarConnection(applicationContext).type con il lifecycle dell'Activity e deriva carLink (CONN-01)"
  - "renderSpeedArea(state): unico punto di rendering dell'area velocita', a conoscenza di carLink, con ramo neutro 'Connesso ad Android Auto' e return anticipato"
  - "setupScreenOnSwitch()/onCarLinkChanged() derivano SEMPRE il flag FLAG_KEEP_SCREEN_ON tramite resolveEffectiveKeepScreenOn(savedKeepOn, carLink), mai una seconda variabile di stato indipendente (CONN-02)"
  - "Log diagnostico gated su BuildConfig.DEBUG (tag TachimetroPhone) con solo stato del collegamento e booleani di preferenza"
affects: ["10-03 (verifica DHU dal vivo: connessione/disconnessione Android Auto sul telefono fisico, log TachimetroPhone/TachimetroCar filtrabili separatamente)"]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Rendering separato dall'accumulo: renderSpeedArea() e' priva di effetti collaterali (solo View), l'accumulo di massimo/distanza vive in updatePlaceholder() sotto una guardia esplicita -- permette di ridisegnare da un observer esterno (onCarLinkChanged) senza contare due volte gli stessi metri"
    - "Preferenza persistita come unica sorgente di verita': lo switch/onCarLinkChanged() non tengono mai un secondo stato del flag effettivo, lo ricalcolano sempre da resolveEffectiveKeepScreenOn(savedKeepOn, carLink)"
    - "CarConnection osservato con il lifecycle dell'Activity (LiveData.observe(this)): nessuna deregistrazione manuale, la libreria gestisce onActive()/onInactive() internamente"

key-files:
  created: []
  modified:
    - app/src/main/java/com/sed/tachimetro/MainActivity.kt

key-decisions:
  - "Nessuna deviazione funzionale dal piano: entrambi i task eseguiti come scritto. Due discrepanze nelle asserzioni automatiche del <verify> del piano (grep grezzo non spogliato dai commenti) sono state trattate come bug del piano stesso (Rule 1) e documentate sotto, non come cambi di comportamento del codice."

patterns-established:
  - "onCarLinkChanged() come unico punto che aggiorna carLink e ne applica le due conseguenze (flag schermo + ridisegno), con guardie esplicite (changed, permissionGranted.value) commentate inline -- pattern riusabile per qualunque futuro segnale esterno che debba influenzare il rendering senza duplicare lo stato"

requirements-completed: [CONN-01, CONN-02]

# Metrics
duration: ~10min
completed: 2026-09-02
---

# Phase 10 Plan 02: Comportamento del telefono alla connessione Android Auto Summary

**`MainActivity` osserva `CarConnection` con il lifecycle dell'Activity, mostra lo stato neutro "Connesso ad Android Auto" e rilascia `FLAG_KEEP_SCREEN_ON` durante la proiezione, riapplicando esattamente la preferenza salvata alla disconnessione senza mai scriverla.**

## Performance

- **Duration:** ~10 min
- **Started:** 2026-09-02T16:38:25+02:00 (base del piano)
- **Completed:** 2026-09-02T16:46:11+02:00
- **Tasks:** 2 completate
- **Files modified:** 1 (`MainActivity.kt`)

## Accomplishments
- `renderSpeedArea(state)` estratta da `updatePlaceholder()`: unico punto di decisione del rendering dell'area velocita', con un ramo anticipato per `carLink is CarLinkState.Connected` che mostra "Connesso ad Android Auto" al posto del numero (CONN-01)
- L'accumulo di massimo/distanza spostato sotto una guardia `if (state is SpeedState.Reading)` in `updatePlaceholder()`, fuori dal ramo di rendering: continua identico anche con Android Auto connesso
- `showReady()` sceglie il messaggio neutro o "Pronto" in base a `carLink`; `showDenied()` documenta la precedenza deliberata del messaggio di permesso sullo stato neutro
- `setupCarConnectionObserver()`: `CarConnection(applicationContext).type.observe(this) { ... }`, nessuna deregistrazione manuale (la libreria gestisce il ciclo di vita del `BroadcastReceiver` interno tramite `onActive()`/`onInactive()`)
- `onCarLinkChanged(link)`: aggiorna `carLink`, applica sempre `applyKeepScreenOn(resolveEffectiveKeepScreenOn(savedKeepOn, carLink))` (idempotente), ridisegna con `renderSpeedArea(gpsSpeedProvider.state.value)` solo quando lo stato e' cambiato e il permesso e' concesso, non scrive mai la preferenza persistita (CONN-02)
- `setupScreenOnSwitch()` riscritta sulla derivazione pura: `savedKeepOn` tiene la preferenza salvata, il listener persiste incondizionatamente e riapplica il flag effettivo tramite la stessa funzione pura del Piano 01
- Log diagnostico `Log.d(LOG_TAG, ...)` gated su `BuildConfig.DEBUG`, tag `"TachimetroPhone"` (distinto da `"TachimetroCar"` di `SpeedScreen`), solo `carLink`/`savedKeepOn`/flag effettivo -- mai velocita' o posizione (T-10-10)

## Task Commits

Ogni task e' stato committato atomicamente:

1. **Task 1: Estrarre renderSpeedArea() e introdurre il ramo dello stato neutro** - `d65f91e` (refactor)
2. **Task 2: Osservare CarConnection e derivare il flag schermo-sempre-acceso (CONN-01, CONN-02)** - `8fb3d19` (feat)

**Plan metadata:** (in arrivo -- commit di questo SUMMARY.md)

## Files Created/Modified
- `app/src/main/java/com/sed/tachimetro/MainActivity.kt` - `renderSpeedArea()`, `setupCarConnectionObserver()`, `onCarLinkChanged()`, `setupScreenOnSwitch()` riscritta sulla derivazione pura, `carLink`/`carConnection`/`savedKeepOn` come nuovi campi, `LOG_TAG = "TachimetroPhone"` nel companion object

## Decisions Made
Nessuna deviazione funzionale dal piano: entrambi i task eseguiti esattamente come scritto nell'`<action>`. Vedi sotto per due discrepanze puramente nelle asserzioni automatiche del `<verify>`.

## Deviations from Plan

### Auto-fixed Issues (plan verify script bugs, no code behavior change)

**1. [Rule 1 - Bug nel piano] Asserzione `reduceDistance(` == 1 nel `<verify>` del Task 1 incompatibile con l'istruzione esplicita di preservare i commenti D-04/WR-03**
- **Found during:** Task 1, terzo blocco `<verify>`
- **Issue:** Il piano richiede testualmente (punto 4 dell'`<action>`) di mantenere integralmente i commenti esistenti "D-07, DIST-03, D-04, WR-03, DIST-02" quando si sposta il blocco di accumulo. I commenti D-04/WR-03 preesistenti citano letteralmente `reduceDistance()` due volte in prosa. Il terzo comando `<verify>` esegue pero' un `grep -c 'reduceDistance(' $F` sul file GREZZO (non spogliato dai commenti, a differenza del primo blocco che usa `$C=$(grep -v '^\s*//' $F)`), aspettandosi 1 sola occorrenza. Le due istruzioni del piano sono in conflitto diretto: seguire l'una rende impossibile soddisfare l'altra.
- **Fix:** Preservati i commenti come esplicitamente richiesto dall'`<action>` (requisito piu' specifico e piu' vincolante). Il conteggio grezzo risulta 3 (2 menzioni in commento + 1 chiamata reale), ma il conteggio sul codice reale (spogliato dai commenti, coerente con l'`<acceptance_criteria>` "reduceDistance(` ricorre ... esattamente una volta nel file") e' 1, verificato con `grep -v '^\s*//' | grep -c`.
- **Files modified:** nessuno oltre a quanto gia' pianificato (nessun file aggiuntivo)
- **Verification:** `grep -v '^\s*//' MainActivity.kt | grep -c 'reduceDistance('` → 1
- **Committed in:** `d65f91e` (Task 1 commit)

**2. [Rule 1 - Bug nel piano] Asserzioni `maxSpeedStore.write(` == 3, `screenOnStore.write(` == 3 (raw), `updatePlaceholder(` == 5 (raw) causate dagli stessi commenti esplicitamente richiesti dal piano**
- **Found during:** Task 1 e Task 2, blocchi `<verify>` non spogliati dai commenti
- **Issue:** Stesso pattern del punto 1. Il piano richiede esplicitamente (Task 1 punto 4, Task 2 punto 6) commenti che citano letteralmente `maxSpeedStore.write()`, `screenOnStore.write()` e `updatePlaceholder()` come testo (es. "questa funzione NON chiama `screenOnStore.write()`", "NON chiamare `updatePlaceholder()` qui"). Il `grep` grezzo del `<verify>` conta anche queste menzioni testuali nei commenti, producendo conteggi superiori a quelli attesi per le sole chiamate reali.
- **Fix:** Commenti scritti esattamente come richiesto dall'`<action>`. Verificato che il conteggio sul codice reale (spogliato dai commenti) coincide con l'intento dichiarato negli `<acceptance_criteria>`: `maxSpeedStore.write(` → 2 chiamate reali (D-07 + reset), `screenOnStore.write(` → 2 chiamate reali (default al primo avvio + listener, mai da `onCarLinkChanged()`), `updatePlaceholder(` → 2 (dichiarazione + unica chiamata in `setupGpsCollection()`, mai da `onCarLinkChanged()`).
- **Files modified:** nessuno oltre a quanto gia' pianificato
- **Verification:** `grep -v '^\s*//' MainActivity.kt | grep -c 'maxSpeedStore.write('` → 2 (2 chiamate reali; il conteggio grezzo di 3 include 1 menzione in commento); `screenOnStore.write(` spogliato → 2; `updatePlaceholder(` spogliato → 2. `assembleDebug` e `testDebugUnitTest` entrambi `BUILD SUCCESSFUL`.
- **Committed in:** `d65f91e` (Task 1), `8fb3d19` (Task 2)

---

**Total deviations:** 2 (entrambe bug nelle asserzioni grezze del `<verify>` del piano, nessuna deviazione di comportamento del codice)
**Impact on plan:** Nessuno sul comportamento consegnato. Il codice implementa esattamente quanto richiesto dall'`<action>`/`<acceptance_criteria>`; solo le due asserzioni raw-grep del `<verify>` erano miscalibrate rispetto ai propri stessi requisiti di preservare/aggiungere commenti che citano quei nomi di funzione. Tutti gli altri gate automatici del piano (build, test, conteggi comment-stripped, `git diff` su manifest/build/car/screen) passano invariati.

## Issues Encountered
- `local.properties` mancava in questo worktree appena creato (stesso problema gia' documentato nel Piano 01), causando il fallimento immediato di `assembleDebug` su "SDK location not found". Ricreato copiando `sdk.dir` dal repository principale (file gia' in `.gitignore`, nessun impatto sul commit). Non e' una deviation dal piano: e' un prerequisito di ambiente locale, non versionato in nessun caso.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Piano 03 puo' procedere con la verifica DHU dal vivo: connessione/disconnessione Android Auto sul telefono fisico, osservando i log `TachimetroPhone` (nuovo, questo piano) e `TachimetroCar` (Fase 8/9) separatamente in logcat
- Nessun blocco: `git diff --name-only` conferma che solo `MainActivity.kt` e' stato modificato; `git diff` su `app/src/main/java/com/sed/tachimetro/car/`, `app/src/main/java/com/sed/tachimetro/screen/ScreenOnPreferenceStore.kt`, `AndroidManifest.xml`, `app/build.gradle.kts` e `gradle/libs.versions.toml` sono tutti vuoti come richiesto
- `assembleDebug` e `testDebugUnitTest` (incluso `CarLinkStateTest` del Piano 01) entrambi `BUILD SUCCESSFUL`, nessuna regressione

## TDD Gate Compliance

Piano di tipo `execute` (non `tdd`), nessun gate RED/GREEN/REFACTOR applicabile.

## Self-Check: PASSED

- FOUND: app/src/main/java/com/sed/tachimetro/MainActivity.kt
- FOUND commit: d65f91e (refactor, Task 1)
- FOUND commit: 8fb3d19 (feat, Task 2)

---
*Phase: 10-comportamento-del-telefono-alla-connessione-android-auto*
*Completed: 2026-09-02*
