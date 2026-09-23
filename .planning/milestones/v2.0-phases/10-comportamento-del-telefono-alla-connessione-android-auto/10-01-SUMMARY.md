---
phase: 10-comportamento-del-telefono-alla-connessione-android-auto
plan: 01
subsystem: android-auto-integration
tags: [kotlin, androidx-car-app, car-connection, tdd, pure-functions]

# Dependency graph
requires:
  - phase: 08-fondamenta-condivise-e-velocit-sullo-schermo-auto
    provides: dipendenza androidx.car.app:app gia' presente in app/build.gradle.kts
provides:
  - "CarLinkState (sealed: Disconnected/Connected) — modello dello stato del collegamento Android Auto lato telefono"
  - "resolveCarLinkState(connectionType: Int?): CarLinkState — mappatura fail-safe del tipo di connessione riportato da CarConnection.getType()"
  - "resolveEffectiveKeepScreenOn(savedPreference: Boolean, link: CarLinkState): Boolean — derivazione pura senza stato del flag effettivo schermo-sempre-acceso (CONN-01 + CONN-02 in un solo punto)"
  - "stringa android_auto_connected = \"Connesso ad Android Auto\" per lo stato neutro lato telefono"
affects: ["10-02 (cablaggio in MainActivity: CarConnection.getType().observe(), applicazione del flag FLAG_KEEP_SCREEN_ON, sostituzione del testo con android_auto_connected)"]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Sealed class + funzioni pure top-level nello stesso file (stessa forma di CarPermissionState.kt/MaxSpeedReducer.kt)"
    - "Preferenza persistita come unica sorgente di verita'; il valore effettivo e' sempre DERIVATO da una funzione pura senza accesso a SharedPreferences, mai una seconda variabile di stato che potrebbe divergere"

key-files:
  created:
    - app/src/main/java/com/sed/tachimetro/car/CarLinkState.kt
    - app/src/test/java/com/sed/tachimetro/car/CarLinkStateTest.kt
  modified:
    - app/src/main/res/values/strings.xml

key-decisions:
  - "Nessuna deviazione dal piano: CarConnection.CONNECTION_TYPE_PROJECTION/NOT_CONNECTED/NATIVE risolti tramite le costanti simboliche della libreria (compile-time inlined), test JVM eseguiti senza runtime Android senza bisogno del fallback a letterali numerici previsto dal piano come piano B"

patterns-established:
  - "resolveEffectiveKeepScreenOn come punto unico di derivazione CONN-01/CONN-02: il chiamante (Piano 02) scrivera' la preferenza SOLO dal listener dello switch, mai da un cambio di stato della connessione"

requirements-completed: [CONN-01, CONN-02]

# Metrics
duration: ~12min
completed: 2026-09-02
---

# Phase 10 Plan 01: Fondamenta pure CarLinkState Summary

**Modello sealed `CarLinkState` (Disconnected/Connected) con mappatura fail-safe del tipo di connessione Android Auto e funzione pura `resolveEffectiveKeepScreenOn` che deriva CONN-01/CONN-02 in un solo punto senza toccare la preferenza persistita.**

## Performance

- **Duration:** ~12 min
- **Started:** 2026-09-02T14:24:00Z (stimato)
- **Completed:** 2026-09-02T14:36:37Z
- **Tasks:** 2 completate
- **Files modified:** 3 (2 creati, 1 modificato)

## Accomplishments
- `CarLinkState.kt`: sealed class a due sottotipi (`Disconnected`, `Connected`) + `resolveCarLinkState(Int?)` che tratta come "connesso" SOLO `CarConnection.CONNECTION_TYPE_PROJECTION`, con `NATIVE`/`null`/negativi/valori futuri sconosciuti tutti fail-safe verso `Disconnected` (T-10-01, T-10-02)
- `resolveEffectiveKeepScreenOn(savedPreference, link)`: funzione pura senza stato che copre l'intera tabella di verita' CONN-01 (rilascio del flag quando connesso) e CONN-02 (ripristino esatto della preferenza salvata alla disconnessione), senza alcun accesso a `ScreenOnPreferenceStore` (T-10-03)
- 11 test JVM in `CarLinkStateTest.kt` che lockano ogni caso del `<behavior>` del piano, incluso il round-trip Connected -> Disconnected che dimostra l'assenza di stato interno
- Stringa `android_auto_connected` = "Connesso ad Android Auto" aggiunta a `strings.xml`, testo esatto di CONN-01 e del Success Criterion 1 della roadmap

## Task Commits

Ogni task e' stato committato atomicamente secondo il ciclo TDD RED -> GREEN:

1. **Task 1: Modello CarLinkState e le due funzioni pure (CONN-01, CONN-02)**
   - `4766a3f` (test) — RED: 11 test falliscono in compilazione (CarLinkState non esiste ancora)
   - `c46bd93` (feat) — GREEN: implementazione minima, tutti i test passano
2. **Task 2: Stringa italiana dello stato neutro lato telefono (CONN-01)** - `36db92a` (feat)

**Plan metadata:** (in arrivo — commit di questo SUMMARY.md)

_Nessun commit `refactor` necessario: l'implementazione GREEN era gia' minimale e conforme allo stile del progetto._

## Files Created/Modified
- `app/src/main/java/com/sed/tachimetro/car/CarLinkState.kt` - sealed `CarLinkState` + `resolveCarLinkState()` + `resolveEffectiveKeepScreenOn()`, KDoc che motiva NATIVE non "connesso" e default fail-safe
- `app/src/test/java/com/sed/tachimetro/car/CarLinkStateTest.kt` - 11 test JVM puri (nessun mocking, nessun Robolectric), stile identico a `CarPermissionStateTest.kt`
- `app/src/main/res/values/strings.xml` - nuova stringa `android_auto_connected`, inserita dopo `searching_gps_signal` e prima del blocco `car_*`, con commento che ne chiarisce l'ambito (telefono, non schermo auto)

## Decisions Made
None - plan executed exactly as written. Il piano B previsto al punto 5 dell'`<action>` del Task 1 (sostituire le costanti simboliche con letterali numerici nel test se il caricamento classi di `CarConnection` fosse fallito in JVM puro) non e' stato necessario: le costanti `static final int` sono state inlineate correttamente dal compilatore Kotlin e i test girano senza runtime Android.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
- `local.properties` (file locale, non versionato) mancava nel worktree appena creato, causando il fallimento immediato di Gradle su "SDK location not found". Ricreato copiando `sdk.dir` dal repository principale (stesso valore, file gia' in `.gitignore`, nessun impatto sul commit). Non e' un deviation dal piano: e' un prerequisito di ambiente locale, non versionato in nessun caso.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Piano 02 puo' cablare `CarConnection(applicationContext).type.observe(...)` in `MainActivity`, chiamare `resolveCarLinkState()` sul valore emesso e poi `resolveEffectiveKeepScreenOn()` per determinare se applicare/rimuovere `FLAG_KEEP_SCREEN_ON`, sostituendo il contenuto testuale con `getString(R.string.android_auto_connected)` quando `Connected`
- Nessun blocco: `MainActivity.kt`, `ScreenOnPreferenceStore.kt`, `AndroidManifest.xml` e i file di build restano invariati come richiesto dal piano, verificato con `git diff --name-only` (esattamente i 3 file dichiarati) e diff vuoti sui due file esplicitamente esclusi

## TDD Gate Compliance

Sequenza commit verificata su `git log`: `test(10-01)` (4766a3f) precede `feat(10-01)` (c46bd93) che implementa `CarLinkState.kt`. Gate RED/GREEN rispettato.

## Self-Check: PASSED

- FOUND: app/src/main/java/com/sed/tachimetro/car/CarLinkState.kt
- FOUND: app/src/test/java/com/sed/tachimetro/car/CarLinkStateTest.kt
- FOUND: app/src/main/res/values/strings.xml
- FOUND commit: 4766a3f (test)
- FOUND commit: c46bd93 (feat)
- FOUND commit: 36db92a (feat)

---
*Phase: 10-comportamento-del-telefono-alla-connessione-android-auto*
*Completed: 2026-09-02*
