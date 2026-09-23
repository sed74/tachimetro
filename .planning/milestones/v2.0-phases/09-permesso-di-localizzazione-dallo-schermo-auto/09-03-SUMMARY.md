---
phase: 09-permesso-di-localizzazione-dallo-schermo-auto
plan: 03
subsystem: testing
tags: [android-auto, car-app-library, kotlin, instrumented-test, dhu, verification, pane-template, permissions]

# Dependency graph
requires:
  - phase: 09-permesso-di-localizzazione-dallo-schermo-auto (Piano 02)
    provides: "SpeedScreen.buildTemplate(permission, speed) public seam, permissionState state machine, requestPermissions() automatico, Action Riprova/Apri impostazioni"
provides:
  - "SpeedScreenTemplateTest esteso: forma del PaneTemplate lockata da asserzioni per tutti e quattro gli stati di CarPermissionState (Granted+Reading, Granted+Searching, NotRequested, Waiting, Denied(false), Denied(true))"
  - "Gate di chiusura Fase 9: sessione DHU dal vivo conferma i tre Success Criteria di roadmap (richiesta automatica, transizione automatica alla concessione, messaggio+retry al rifiuto)"
  - "Pitfall 4 chiuso empiricamente: la transizione di forma del template (Row-sola <-> Row+Action) non provoca chiusura dell'app da parte dell'host"
  - "Decisione formale D-08/D-09 (09-CONTEXT.md): esito completo della sessione DHU e accettazione esplicita del limite Pitfall 1 (Scenario G) per v2.0"
affects: [10-comportamento-telefono-connessione-android-auto, 11-hardening-produzione]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Seam di test pubblico (buildTemplate(permission, speed)) iniettato direttamente dentro runOnMainSync per verificare ogni stato del permesso in modo deterministico, senza dipendere dal permesso reale del dispositivo di test"
    - "Rischio non verificabile da codice (comportamento di un host reale, dialogo di sistema) chiuso con un checkpoint umano DHU puntuale (18 sotto-verifiche A1-F2) invece di essere assunto o dedotto dalla sola lettura della documentazione"
    - "Limite di piattaforma noto (Pitfall 1) reso esplicito con una domanda diretta all'utente in fase di checkpoint, non silenziosamente accettato o ignorato"

key-files:
  created: []
  modified:
    - app/src/androidTest/java/com/sed/tachimetro/car/SpeedScreenTemplateTest.kt
    - .planning/phases/09-permesso-di-localizzazione-dallo-schermo-auto/09-CONTEXT.md

key-decisions:
  - "D-08: sessione DHU dal vivo eseguita dall'utente, tutti gli scenari A-F confermati punto per punto -- i tre Success Criteria di roadmap della Fase 9 sono confermati dal vivo, Pitfall 4 (transizione di forma del template) chiuso empiricamente"
  - "D-09: limite di piattaforma noto (Pitfall 1 -- CarContext.requestPermissions() puo' essere ignorato silenziosamente dall'host a veicolo in movimento) ACCETTATO esplicitamente dall'utente per v2.0, nessuna modifica a D-05/D-06 richiesta (nessuna azione di sblocco manuale aggiunta allo stato Waiting)"
  - "Verifica DHU non ripetuta/simulata da questo esecutore: i risultati documentati in questo SUMMARY e in 09-CONTEXT.md D-08/D-09 riflettono la sessione dal vivo eseguita personalmente dall'utente in una sessione precedente a questa chiusura"

patterns-established:
  - "Checklist di verifica DHU con sotto-punti enumerati (A1, A2, B1...) invece di una singola conferma generica, per rendere tracciabile quale comportamento specifico e' stato osservato"

requirements-completed: [AA-04]

# Metrics
duration: "~4min esecuzione automatizzata (Task 1, 2026-09-02 14:04-14:08) + sessione DHU dal vivo e decisione su Scenario G su una sessione umana separata (Task 2) + ~9min di chiusura documentale (2026-09-02 14:08-14:17)"
completed: 2026-09-02
---

# Phase 09 Plan 03: Verifica — Test Strumentato Completo e Chiusura Fase su DHU Summary

**SpeedScreenTemplateTest esteso a tutti e quattro gli stati di CarPermissionState (6 nuovi/aggiornati test) più chiusura del gate umano di fase: sessione DHU dal vivo conferma i tre Success Criteria di roadmap della Fase 9, chiude empiricamente Pitfall 4 (transizione Row-sola↔Row+Action), e l'utente accetta esplicitamente il limite noto di Pitfall 1 (Scenario G) per v2.0.**

## Performance

- **Duration:** ~4 min di esecuzione automatizzata per il Task 1 (2026-09-02 14:04-14:08); il Task 2 (checkpoint umano bloccante) si è risolto tramite una sessione DHU dal vivo eseguita dall'utente in una sessione precedente a questa chiusura, seguita da ~9 min di chiusura documentale (registrazione esiti in `09-CONTEXT.md` e in questo SUMMARY, 2026-09-02 14:08-14:17)
- **Started:** 2026-09-02T14:04:36+02:00 (Task 1, continuazione dopo 09-02)
- **Completed:** 2026-09-02T14:17:47+02:00 (chiusura Task 2 e del piano)
- **Tasks:** 2/2 completati
- **Files modified:** 2 (1 nel Task 1, 1 nella chiusura documentale del Task 2)

## Accomplishments
- `SpeedScreenTemplateTest` blocca ora con asserzioni strumentate la forma del `PaneTemplate` per ognuno dei quattro stati di `CarPermissionState` (`Granted`+`Reading`, `Granted`+`Searching`, `NotRequested`, `Waiting`, `Denied(false)`, `Denied(true)`), incluso il numero e il titolo esatto delle `Action` (Riprova/Apri impostazioni) — nessun letterale italiano hardcoded, tutti i valori attesi letti da `context.getString(...)`
- Sessione DHU dal vivo eseguita con successo dall'utente: **i tre Success Criteria di roadmap della Fase 9 sono confermati** (richiesta automatica al primo collegamento, transizione automatica alla velocità dopo la concessione, messaggio+retry al rifiuto con distinzione singolo/permanente)
- **Pitfall 4 chiuso empiricamente**: la transizione tra un `Pane` di sola `Row` e un `Pane` con `Row`+`Action` (variazione strutturale del template, mai messa alla prova contro la quota dell'host prima di questa fase) non ha mai provocato la chiusura dell'app da parte dell'host, nessun errore host, PID stabile durante tutte le transizioni
- **Scenario G / Pitfall 1 con disposizione esplicita**: il limite noto (`CarContext.requestPermissions()` può essere ignorato silenziosamente dall'host se il veicolo è già in movimento al collegamento, lasciando lo schermo bloccato su "Controlla il telefono" senza azione di sblocco manuale) è stato presentato esplicitamente all'utente, che ha risposto **"Accettato"** per la milestone v2.0 — nessuna modifica alle decisioni D-05/D-06

## Task Commits

Each task was committed atomically:

1. **Task 1: Estendere il test strumentato a tutti gli stati del permesso** - `95e9570` (test)
2. **Task 2: Verifica DHU dal vivo del flusso di permesso completo** - checkpoint umano bloccante, nessuna modifica a codice applicativo prevista dal piano (`<files>` = "nessun file modificato -- checkpoint di verifica"); risolto tramite sessione DHU dal vivo eseguita dall'utente + risposta esplicita "Accettato" allo Scenario G, registrati in `09-CONTEXT.md` (D-08/D-09, commit `9c96090`) e in questo SUMMARY

**Plan metadata:** commit di chiusura di questo SUMMARY (vedi commit successivo)

_Nessun task TDD in questo piano: Task 1 estende un test strumentato esistente asserendo comportamento già implementato dal Piano 02 (nessuna fase RED/GREEN separata), Task 2 è un checkpoint umano senza codice._

## Files Created/Modified
- `app/src/androidTest/java/com/sed/tachimetro/car/SpeedScreenTemplateTest.kt` - esteso da 4 a 264 righe: nuovo helper `buildTemplate(permission, speed)` che invoca direttamente il seam pubblico di `SpeedScreen`, test rinominato `initialState_beforeStarted_showsCheckYourPhoneRow` (stato iniziale post-Piano-02 è `NotRequested`, non più un fallback `Searching`), 6 nuovi test per gli stati `Granted`+`Reading`, `Granted`+`Searching`, `Waiting`, `Denied(false)`, `Denied(true)` e la copertura trasversale header/action-strip/icona su tutti gli stati (commit `95e9570`)
- `.planning/phases/09-permesso-di-localizzazione-dallo-schermo-auto/09-CONTEXT.md` - aggiunte D-08 (esito puntuale della sessione DHU, scenari A-F) e D-09 (accettazione esplicita dello Scenario G/Pitfall 1) (commit `9c96090`)
- `.planning/phases/09-permesso-di-localizzazione-dallo-schermo-auto/09-03-SUMMARY.md` - questo file

## Decisions Made

Tutte le decisioni rilevanti per la chiusura del Task 2/piano sono registrate formalmente in `.planning/phases/09-permesso-di-localizzazione-dallo-schermo-auto/09-CONTEXT.md` (D-08/D-09) a seguito della sessione DHU dal vivo dell'utente. Riepilogo qui per riferimento immediato:

- **D-08**: Sessione DHU dal vivo eseguita dall'utente, che ha confermato punto per punto tutti gli scenari A-F richiesti dal checkpoint (A1-A2, B1-B2, C1-C3, D1-D3, E1-E3, F1-F2). I tre Success Criteria di roadmap della Fase 9 sono confermati, Pitfall 4 è chiuso empiricamente.
- **D-09**: Lo Scenario G (Pitfall 1 — limite di piattaforma sulla possibile ignorazione silenziosa di `requestPermissions()` a veicolo in movimento) è stato presentato esplicitamente all'utente in una domanda di chiarimento durante il checkpoint. Risposta: **"Accettato"** per v2.0 — nessuna azione di sblocco manuale aggiunta allo stato `Waiting`, nessuna modifica a D-05/D-06.

## Deviations from Plan

None - plan eseguito esattamente come scritto. Il Task 1 ha superato tutti i grep di acceptance criteria (helper `buildTemplate(`, `CarPermissionState.Denied(permanent = true/false)`, `CarPermissionState.Waiting`, `R.string.car_check_your_phone`/`open_settings`/`retry`, assenza di letterali italiani hardcoded) al primo tentativo. Il Task 2 (checkpoint umano) è stato risolto interamente dall'utente con una sessione DHU dal vivo che ha prodotto risposte affermative su tutti i punti richiesti, senza necessità di iterazioni o correzioni di codice.

## Issues Encountered

None. Nessun problema tecnico incontrato in questo piano — sia il test strumentato (Task 1) sia la sessione DHU (Task 2) hanno prodotto l'esito atteso al primo tentativo.

## User Setup Required

None - la sessione DHU e la decisione sullo Scenario G sono già state completate personalmente dall'utente in una sessione precedente a questa chiusura; nessuna azione utente ulteriore richiesta per chiudere il piano/la fase.

## Next Phase Readiness

- **Fase 9 completa**: tutti e 3 i piani eseguiti, requisito AA-04 coperto e verificato dal vivo su DHU (richiesta automatica del permesso dallo schermo auto, transizione automatica alla concessione, messaggio+retry al rifiuto con distinzione singolo/permanente)
- **Fase 10** (Comportamento del Telefono alla Connessione Android Auto) può partire: nessun blocco noto, indipendente dal lavoro sul permesso di questa fase (estende `ScreenOnPreferenceStore` v1.0)
- **Follow-up non bloccante per la milestone v2.0 corrente**: il limite Pitfall 1 (Scenario G — richiesta di permesso silenziosamente ignorata dall'host a veicolo in movimento, nessuna azione di sblocco manuale nello stato `Waiting`) resta un concern noto e accettato, non un bug da correggere in una fase futura — registrato in `STATE.md`
- **Verifica/transizione di fase**: come da convenzione osservata in questo progetto (Fasi 4/6/7/8 producono `XX-VERIFICATION.md` + evoluzione di `PROJECT.md` via `/gsd-transition`) — questa chiusura copre SUMMARY/ROADMAP/STATE ma **non** crea `09-VERIFICATION.md` né evolve `PROJECT.md`; quei due passaggi restano espliciti passi successivi, fuori dallo scope di questa chiusura di piano

---
*Phase: 09-permesso-di-localizzazione-dallo-schermo-auto*
*Completed: 2026-09-02*

## Known Stubs

Nessuno stub rilevato. L'unico file di codice modificato in questo piano (`SpeedScreenTemplateTest.kt`) è un test strumentato, senza componenti UI con dati mock/placeholder. Nessuna modifica a codice applicativo (`app/src/main`) è avvenuta in questo piano — confermato anche dall'acceptance criteria del Task 2 (`git diff --name-only` non elenca file sotto `app/src/main` in seguito al checkpoint).

## Threat Flags

Nessuna nuova superficie non coperta dal `<threat_model>` del piano. T-09-12 (seam pubblico `buildTemplate()`, accept) invariato; T-09-13 (nessun dato personale nei comandi di verifica, mitigate) rispettato — nessun comando eseguito da questo esecutore ha stampato dati di posizione o preferenze persistite; T-09-14 (`pm clear` durante la verifica, accept) e T-09-15 (host DHU non validato, ereditato da T-08-05, accept) invariati, applicati dall'utente durante la propria sessione DHU. Nessun nuovo endpoint di rete, percorso di autenticazione o schema dati introdotto da questa chiusura.

## Self-Check: PASSED

- FOUND: app/src/androidTest/java/com/sed/tachimetro/car/SpeedScreenTemplateTest.kt
- FOUND: .planning/phases/09-permesso-di-localizzazione-dallo-schermo-auto/09-CONTEXT.md
- FOUND: .planning/phases/09-permesso-di-localizzazione-dallo-schermo-auto/09-03-SUMMARY.md (questo file)
- FOUND: commit 95e9570 (test: Task 1 - PaneTemplate shape for all permission states)
- FOUND: commit 9c96090 (docs: Task 2 - DHU verification results, D-08/D-09)
