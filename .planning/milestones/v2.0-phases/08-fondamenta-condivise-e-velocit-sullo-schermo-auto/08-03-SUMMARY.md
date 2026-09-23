---
phase: 08-fondamenta-condivise-e-velocit-sullo-schermo-auto
plan: 03
subsystem: infra
tags: [android-auto, car-app-library, kotlin, dhu, instrumented-test, verification, pane-template]

# Dependency graph
requires:
  - phase: 08-fondamenta-condivise-e-velocit-sullo-schermo-auto (Piano 02)
    provides: "TachimetroCarAppService/Session/SpeedScreen con PaneTemplate a 1 Hz, log diagnostico onGetTemplate #<n> content=<...> sotto BuildConfig.DEBUG"
provides:
  - "SpeedScreenTemplateTest: forma del PaneTemplate (header action, assenza titolo/icona in Row, stato Searching, ripetibilita' su 60 rebuild) lockata da asserzioni strumentate"
  - "scripts/dhu-quota-check.ps1 + docs/dhu-quota-verification.md: procedura riproducibile e in gran parte automatizzata di misura refresh/quota su DHU, con soglie PASS/FAIL/INCONCLUSIVO esplicite"
  - "Gate SC4 chiuso: sessione DHU dal vivo (telefono fisico) conferma che l'host non chiude l'app sotto refresh continuo a 1Hz"
  - "Decisione formale D-13/D-14 (08-CONTEXT.md): layout host-controlled del PaneTemplate accettato per v2.0; passaggio a NavigationTemplate+SurfaceCallback rimandato a milestone v2.1 dedicata"
affects: [09-permesso-localizzazione-schermo-auto, 10-comportamento-telefono-connessione-android-auto, 11-hardening-produzione, v2.1-surface-navigationtemplate]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Rischio tecnico con esito incerto in documentazione chiuso con verifica empirica scriptata + checkpoint umano (script produce esito euristico, la persona conferma a occhio), non assunto per difetto"
    - "Limite strutturale di API scoperto solo dal rendering reale su host (DHU) documentato come decisione esplicitamente accettata (non bug, non fix cosmetico tentato), con l'alternativa architetturale rimandata a una milestone dedicata invece di essere aperta ad-hoc dentro la fase corrente"

key-files:
  created:
    - app/src/androidTest/java/com/sed/tachimetro/car/SpeedScreenTemplateTest.kt
    - scripts/dhu-quota-check.ps1
    - docs/dhu-quota-verification.md
  modified:
    - .gitignore

key-decisions:
  - "SC4 (quota refresh) CONFERMATO PASS via sessione DHU dal vivo: 586 refresh in 608s, cadenza media 0.964/s, gap massimo osservato 3.1s, PID mai cambiato/sparito, host non ha mai chiuso l'app (euristica script + conferma visiva utente)"
  - "SC5 (nessuna regressione telefono) CONFERMATO dall'utente: velocita', MAX, distanza, icona ricarica, switch 'sempre acceso' invariati rispetto a v1.1"
  - "SC1 (numero grande/leggibile, coerente col telefono) FALLITO come letteralmente formulato -- causa strutturale: PaneTemplate/Row non espone API per font size/allineamento/posizione, rendering interamente host-controlled, icona app in alto a sinistra obbligatoria con questo template (D-12). Accettato per v2.0 via decisione esplicita in sede di discuss-phase (D-13): AA-01 resta soddisfatto nell'accezione gia' scritta in REQUIREMENTS.md ('stile/tipografia gestiti dall'host'), non nell'accezione 'grande come sul telefono'"
  - "SC2 (perdita segnale -> 'Ricerca segnale...') NON testato dal vivo in nessuna sessione DHU -- accettato senza verifica live su istruzione esplicita dell'utente ('segna SC2 come superata e vai avanti'), motivato da copertura indiretta gia' esistente (CarSpeedContentTest per la mappatura NoSignal->Searching, GpsSpeedProviderStateTest per la transizione di staleness a 5s, collector di SpeedScreen agnostico allo stato)"
  - "Sessione DHU eseguita su telefono fisico (OnePlus 8T, non AVD) -- deviazione consapevole da D-09, motivata da instabilita' infrastrutturale dell'emulatore (moriva da solo pochi minuti dopo il boot, 2/2 tentativi falliti in sessione precedente), non un cambio di scope o di piano di verifica"
  - "Passaggio a NavigationTemplate+SurfaceCallback rimandato esplicitamente a una milestone v2.1 dedicata (D-14), dopo il completamento delle Fasi 9-11 di v2.0 -- non implementato, non pianificato come fase aggiuntiva qui"

patterns-established:
  - "Pattern: quando la documentazione ufficiale di una piattaforma non chiarisce un rischio (qui: quota di refresh dei template non-Surface), chiuderlo con uno script di misura oggettiva + soglie di esito scritte in anticipo + checkpoint umano finale, invece di procedere sulla sola lettura della documentazione o di un'assunzione prudente non verificata"

requirements-completed: [AA-01, AA-02, AA-03]

# Metrics
duration: "~12min esecuzione automatizzata (Task 1-2, 2026-08-31 10:34-10:46) + verifica DHU dal vivo e decisione su piu' sessioni umane (2026-08-31 -> 2026-09-02, Task 3)"
completed: 2026-09-02
---

# Phase 08 Plan 03: Gate SC4 — Verifica Quota Refresh su DHU e Chiusura Fase Summary

**Gate SC4 chiuso con sessione DHU dal vivo su telefono fisico (586 refresh in 608s, cadenza 0.964/s, PID mai cambiato): quota di refresh e assenza di regressioni sul telefono confermate, ma il rendering reale del `PaneTemplate` ha rivelato un limite strutturale su SC1 (numero piccolo in alto a sinistra, non "grande e centrato"), formalmente accettato per v2.0 e rimandato a una milestone v2.1 dedicata (NavigationTemplate+SurfaceCallback); SC2 accettato senza verifica live su decisione esplicita dell'utente.**

## Performance

- **Duration:** ~12 min di esecuzione automatizzata per i Task 1-2; il Task 3 (checkpoint umano bloccante) si e' esteso su piu' sessioni reali tra il 2026-08-31 e il 2026-09-02 (installazione/debug build, due sessioni DHU — la prima inconcludente per permesso mancante, la seconda risolutiva — e una sessione di decisione tipo discuss-phase per SC1)
- **Started:** 2026-08-31T10:34:00+02:00 (Task 1)
- **Completed:** 2026-09-02 (chiusura Task 3 e del piano)
- **Tasks:** 3/3 completati
- **Files modified:** 4 nei Task 1-2 (3 creati, 1 modificato) + file di tracking chiusi in questa sessione (nessuna modifica a codice applicativo)

## Accomplishments
- `SpeedScreenTemplateTest` blocca con asserzioni strumentate la forma del `PaneTemplate` prodotto da `SpeedScreen` (header action, Row senza icona/action strip, copy dello stato "Ricerca segnale...", ripetibilita' su 60 rebuild consecutivi)
- `scripts/dhu-quota-check.ps1` + `docs/dhu-quota-verification.md` rendono la verifica di quota riproducibile con un comando, con soglie PASS/FAIL/INCONCLUSIVO esplicite e un runbook che documenta prerequisiti, procedura e il protocollo di contingenza D-07 prima che servisse
- Sessione DHU dal vivo eseguita con successo (telefono fisico OnePlus 8T via DHU, non l'AVD previsto da D-09): **SC4 confermato PASS** (586 refresh/608s, cadenza media 0.964/s, gap massimo 3.1s, processo mai terminato, host mai chiuso l'app) e **SC5 confermato** (nessuna regressione visibile sul telefono)
- La stessa sessione ha rivelato che **SC1 fallisce** con l'implementazione `PaneTemplate` attuale per un limite strutturale dell'API (non un bug) — gestito con una decisione esplicita dell'utente (D-13 in `08-CONTEXT.md`) invece di un tentativo di fix cosmetico, con l'alternativa architetturale (`NavigationTemplate`+`SurfaceCallback`) rimandata a una milestone v2.1 dedicata (D-14)
- **SC2 accettato senza verifica live** su istruzione esplicita dell'utente, con motivazione onesta registrata (copertura indiretta via test unitari esistenti, non un'osservazione diretta su DHU)

## Task Commits

Each task was committed atomically:

1. **Task 1: Test strumentato della forma del template** - `76904a9` (test)
2. **Task 2: Automazione e runbook della verifica quota su DHU** - `2745314` (feat)
3. **Task 3: Gate SC4 — sessione DHU e conferma assenza regressioni sul telefono** - checkpoint umano bloccante, nessun commit di codice (nessuna modifica prevista): risolto tramite sessione DHU dal vivo + decisione esplicita dell'utente su SC1, registrate in `08-CONTEXT.md` (D-11..D-14) e in questo SUMMARY

**Plan metadata:** commit di chiusura di questo SUMMARY (vedi commit successivo)

_Nessun task TDD in questo piano: Task 1 e' un test strumentato senza fase RED/GREEN separata (asserisce comportamento gia' implementato dal Piano 02), Task 2 e' automazione/documentazione, Task 3 e' un checkpoint umano senza codice._

## Files Created/Modified
- `app/src/androidTest/java/com/sed/tachimetro/car/SpeedScreenTemplateTest.kt` - test strumentato della forma del `PaneTemplate` (commit `76904a9`)
- `scripts/dhu-quota-check.ps1` - script PowerShell di misura riproducibile della quota/cadenza su DHU (commit `2745314`)
- `docs/dhu-quota-verification.md` - runbook della procedura di verifica e del protocollo di contingenza D-07 (commit `2745314`)
- `.gitignore` - esclusione di `build/dhu-quota/` (output di log logcat, T-08-12) (commit `2745314`)
- `.planning/phases/08-fondamenta-condivise-e-velocit-sullo-schermo-auto/08-CONTEXT.md` - D-11..D-14 aggiunte in una sessione precedente (discuss-phase) dopo la sessione DHU dal vivo; e' il record canonico dell'esito di Task 3, referenziato ma non modificato in questa chiusura
- `.planning/phases/08-fondamenta-condivise-e-velocit-sullo-schermo-auto/08-03-SUMMARY.md` - questo file

## Decisions Made

Tutte le decisioni rilevanti per la chiusura del Task 3/piano sono gia' registrate formalmente in `.planning/phases/08-fondamenta-condivise-e-velocit-sullo-schermo-auto/08-CONTEXT.md` (D-11..D-14, sezione `<deferred>`) a seguito di una sessione dal vivo su DHU e di una successiva discussione di fase. Riepilogo qui per riferimento immediato:

- **D-11**: Sessione DHU dal vivo eseguita su telefono fisico (OnePlus 8T, DHU) invece dell'AVD previsto da D-09 — deviazione solo infrastrutturale (AVD instabile, moriva da solo entro pochi minuti dal boot, 2/2 tentativi falliti in precedenza), non una revisione della decisione di merito. SC4 confermato PASS, SC5 confermato.
- **D-12**: Causa radice di SC1 identificata come limite strutturale dell'API `PaneTemplate`/`Row` (nessuna leva per font size/allineamento/posizione, rendering host-controlled; icona app in alto a sinistra obbligatoria quando non si usa un titolo testuale, D-03), non un bug risolvibile con una modifica minore di codice.
- **D-13**: Decisione esplicita presa in sede di discussione di fase — per v2.0 si accetta il layout host-controlled del `PaneTemplate` cosi' com'e'. Nessuna ulteriore modifica di codice tentata su questo fronte per la Fase 8. `AA-01` resta soddisfatto nell'accezione gia' scritta in `REQUIREMENTS.md` ("stile/tipografia gestiti dall'host"), non nell'accezione "grande come sul telefono" — tensione nota e accettata consapevolmente, non un compromesso silenzioso.
- **D-14**: Il passaggio a `NavigationTemplate`+`SurfaceCallback` (disegno Canvas custom: numero grande e centrato, unita' posizionabile, nessuna icona forzata) e' rimandato esplicitamente a una milestone v2.1 dedicata, dopo il completamento delle Fasi 9-11 di v2.0. Riapre la decisione di categoria (`NAVIGATION` invece di `POI`), con revisione Play Store piu' severa in produzione (ma testabile su canali interni senza quella revisione) — da affrontare esplicitamente in fase di roadmap v2.1, non anticipato qui. Visual spec gia' raccolta in `08-CONTEXT.md` `<deferred>`.
- **SC2**: per decisione esplicita dell'utente ("segna SC2 come superata e vai avanti"), accettato senza una sessione DHU che abbia effettivamente esercitato una perdita di segnale GPS. Non riportato come "confermato via sessione live" in nessun documento di tracking — la copertura effettiva e' indiretta (test unitari esistenti su `CarSpeedContentTest`/`GpsSpeedProviderStateTest`), non un'osservazione diretta sullo schermo auto.

## Deviations from Plan

### Auto-fixed Issues

Nessuna modifica di codice applicativo in questa chiusura (per esplicita indicazione: la risoluzione del Task 3 e' documentale, non implementativa). Le due deviazioni operative rispetto al piano scritto sono documentate come decisioni sopra, non come fix di codice:

**1. [Rule 4 - Architectural, risolta con decisione utente esplicita, non con fix automatico] SC1 fallito per limite strutturale dell'API**
- **Found during:** Task 3 (sessione DHU dal vivo)
- **Issue:** Il `PaneTemplate` non permette un numero grande e centrato come richiesto letteralmente da SC1; l'icona app e' forzata in alto a sinistra insieme al numero, entrambi piccoli.
- **Fix:** Nessun fix di codice tentato (nessuna leva API disponibile entro il vincolo `PaneTemplate`). Risolto con una decisione esplicita dell'utente in sede di discussione di fase: accettare il layout com'e' per v2.0 (D-13), rimandare l'alternativa strutturale (`NavigationTemplate`+`SurfaceCallback`) a una milestone v2.1 dedicata (D-14).
- **Files modified:** nessuno (solo `08-CONTEXT.md`, aggiornato in sessione precedente, non in questa chiusura)
- **Verification:** decisione registrata in `08-CONTEXT.md` D-12/D-13/D-14 e nella sezione `<deferred>`
- **Committed in:** decisione gia' presente su `master` prima di questa sessione di chiusura (commit `1bbc7ab`/`308014a`)

**2. [Deviazione procedurale, non una deviazione di codice] Verifica eseguita su telefono fisico invece che su AVD**
- **Found during:** Task 3 (preparazione della sessione DHU)
- **Issue:** L'AVD previsto da D-09 risultava instabile (moriva da solo pochi minuti dopo il boot, riprodotto 2/2 volte in una sessione precedente, causa mai identificata)
- **Fix:** Usato un telefono fisico (OnePlus 8T) connesso via USB come sostituto del solo AVD per la sessione DHU — il DHU supporta ufficialmente entrambi i percorsi. Nessuna modifica allo script necessaria, solo il parametro esplicito `-Serial 359592a5` (il default dello script seleziona solo device `emulator-*`, per design D-09).
- **Files modified:** nessuno
- **Verification:** sessione DHU completata con successo, SC4/SC5 confermati
- **Committed in:** n/a (nessuna modifica di codice)

---

**Total deviations:** 2, entrambe procedurali/decisionali, nessuna modifica di codice applicativo. Nessuno scope creep.
**Impact on plan:** Nessun cambiamento del codice prodotto dai Piani 01/02; l'unico impatto e' sulla lettura di SC1 (accettata con nuance, non un fallimento silenzioso) e sul metodo di verifica (telefono fisico invece di AVD).

## Issues Encountered
- **AVD Android Studio instabile**: moriva da solo pochi minuti dopo il boot in 2/2 tentativi in una sessione precedente, causa radice mai identificata. Aggirato usando un telefono fisico via DHU (vedi Deviazioni sopra); non serve piu' investigare a meno che il telefono fisico non sia disponibile in futuro.
- **Prima sessione DHU inconcludente**: il permesso `ACCESS_FINE_LOCATION` non era ancora stato concesso sul telefono fisico al primo tentativo — GPS mai uscito da `Searching`, un solo refresh registrato in 607s. Risolto concedendo il permesso; la seconda sessione ha prodotto l'esito PASS riportato sopra.
- **SC1 non raggiungibile con l'implementazione corrente**: vedi Decisioni Made / Deviations sopra — non un "issue" risolto con codice, ma una decisione di prodotto registrata esplicitamente.

## User Setup Required

None - la sessione DHU e la decisione su SC1 sono gia' state completate dall'utente in sessioni precedenti a questa chiusura; nessuna azione utente ulteriore richiesta per chiudere il piano/la fase.

## Next Phase Readiness

- **Fase 8 completa**: tutti e 3 i piani eseguiti, requisiti AA-01/AA-02/AA-03 coperti (AA-01 nell'accezione "stile host-controlled" scritta in REQUIREMENTS.md, non "grande come sul telefono" — vedi nuance SC1 sopra)
- **Fase 9** (Permesso di Localizzazione dallo Schermo Auto) puo' partire: lo scaffold `TachimetroCarAppService`/`TachimetroCarSession`/`SpeedScreen` esiste e funziona, nessun blocco noto
- **Follow-up non bloccante per la milestone v2.0 corrente**:
  - SC1: limitazione visiva nota e accettata (numero piccolo, in alto a sinistra, icona app forzata) — rimandata esplicitamente a milestone v2.1 (`NavigationTemplate`+`SurfaceCallback`, D-14, visual spec gia' raccolta in `08-CONTEXT.md` `<deferred>`)
  - SC2: non verificato dal vivo su DHU — copertura solo indiretta via test unitari esistenti (`CarSpeedContentTest`, `GpsSpeedProviderStateTest`); se emergesse un problema di segnale sullo schermo auto in una fase successiva (es. Fase 11, verifica su strada), va investigato li' senza assumere che questa fase l'abbia gia' escluso empiricamente
- **Verifica/transizione di fase**: come da convenzione osservata in questo progetto (vedi commit `docs(phase-XX): complete phase execution` per le Fasi 4/6/7, che producono un file `XX-VERIFICATION.md` e aggiornano `REQUIREMENTS.md`/`STATE.md`, seguiti da `docs(phase-XX): evolve PROJECT.md after phase completion` secondo la sezione "Evolution" di `PROJECT.md` — "Dopo ogni transizione di fase (via `/gsd-transition`)") — questa chiusura copre SUMMARY/ROADMAP/STATE ma **non** crea `08-VERIFICATION.md` ne' evolve `PROJECT.md`: quei due passaggi restano espliciti passi successivi, fuori dallo scope di questa chiusura di piano.

---
*Phase: 08-fondamenta-condivise-e-velocit-sullo-schermo-auto*
*Completed: 2026-09-02*

## Known Stubs

Nessuno stub rilevato. I file creati nei Task 1-2 (`SpeedScreenTemplateTest.kt`, `dhu-quota-check.ps1`, `docs/dhu-quota-verification.md`) sono test/tooling/documentazione, senza componenti UI con dati mock/placeholder. Nessuna modifica a codice applicativo e' avvenuta in questa sessione di chiusura.

## Threat Flags

Nessuna nuova superficie non coperta dal `<threat_model>` del piano. T-08-11 (quota refresh, mitigate) chiuso con esito PASS empirico; T-08-12 (log logcat catturati dallo script, mitigate) invariato; T-08-13/T-08-14 invariati. Nessun nuovo endpoint di rete, percorso di autenticazione o schema dati introdotto da questa chiusura documentale.

## Self-Check: PASSED

All files verified present on disk: `app/src/androidTest/java/com/sed/tachimetro/car/SpeedScreenTemplateTest.kt` (Task 1), `scripts/dhu-quota-check.ps1` and `docs/dhu-quota-verification.md` (Task 2), this `08-03-SUMMARY.md`. All commits verified present in `git log --oneline --all`: `76904a9` (Task 1), `2745314` (Task 2), `73c53e4` (this SUMMARY), `f842862` (ROADMAP.md/STATE.md tracking), `45b161f` (removal of consumed HANDOFF.json/.continue-here.md). Working tree clean, no untracked files.
