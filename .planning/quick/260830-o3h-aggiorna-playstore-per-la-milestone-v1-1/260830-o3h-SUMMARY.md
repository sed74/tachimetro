---
phase: quick-260830-o3h
plan: 01
subsystem: infra
tags: [playstore, release-notes, versioning, listing]

# Dependency graph
requires:
  - phase: 06-indicatore-di-ricarica
    provides: icona di ricarica animata, unico accento lime approvato in PROJECT.md
  - phase: 07-distanza-percorsa-e-reset-unificato
    provides: campo distanza percorsa, pulsante "Azzera" con reset unificato
provides:
  - versionCode 2 / versionName 1.1 in app/build.gradle.kts
  - Note di rilascio v1.1 (IT, EN, file bilingue release_notes_v1.1.txt)
  - Descrizioni complete IT/EN aggiornate con distanza percorsa e indicatore di ricarica
  - playstore/README.md sincronizzato: artefatti APK/AAB e screenshot segnalati come obsoleti (1.0) da rigenerare
affects: [playstore-submission, release-process]

# Tech tracking
tech-stack:
  added: []
  patterns: []

key-files:
  created:
    - playstore/release_notes/release_notes_v1.1.txt
  modified:
    - app/build.gradle.kts
    - playstore/release_notes/it.txt
    - playstore/release_notes/en.txt
    - playstore/listing/it/full_description.txt
    - playstore/listing/en/full_description.txt
    - playstore/README.md

key-decisions:
  - "Mantenuti i due file per locale (it.txt/en.txt) e aggiunto un file bilingue unico (release_notes_v1.1.txt) con tag <it-IT>/<en-US>, per soddisfare la preferenza utente di un copia-incolla in un'unica azione in Play Console"
  - "La deroga UX v1.1 (unica animazione = icona di ricarica, unico accento = lime) è stata riformulata nelle descrizioni complete invece di lasciare l'affermazione assoluta 'nessuna animazione', ora non più vera"

patterns-established: []

requirements-completed: [QUICK-260830-o3h]

# Metrics
duration: ~4min
completed: 2026-08-30
---

# Quick Task 260830-o3h: Aggiorna playstore/ per la milestone v1.1 Summary

**Version bump a 1.1 (versionCode 2) e sincronizzazione completa del pacchetto playstore/ (note di rilascio bilingue, descrizioni IT/EN, README) con le due funzionalità della milestone v1.1: indicatore di ricarica e distanza percorsa con reset unificato.**

## Performance

- **Duration:** ~4 min
- **Started:** 2026-08-30T17:25:57+02:00
- **Completed:** 2026-08-30T17:29:24+02:00
- **Tasks:** 3
- **Files modified:** 7 (6 modificati, 1 creato)

## Accomplishments
- `app/build.gradle.kts` portato da versionCode 1 / versionName "1.0" a versionCode 2 / versionName "1.1"
- Note di rilascio IT ed EN riscritte per descrivere indicatore di ricarica, distanza percorsa e reset unificato (entrambe entro il limite di 500 caratteri Play Console); creato il file bilingue `release_notes_v1.1.txt` con tag `<it-IT>`/`<en-US>` per il copia-incolla in un'unica azione
- Descrizioni complete IT/EN aggiornate: aggiunte le due nuove funzionalità fra i punti elenco, corretta l'affermazione assoluta "nessuna animazione" secondo la deroga approvata in PROJECT.md (unica animazione ammessa = icona di ricarica, unico accento = lime)
- `playstore/README.md` sincronizzato: intestazione aggiornata con versione corrente, APK/AAB e screenshot segnalati esplicitamente come obsoleti (build 1.0) da rigenerare, aggiunto riferimento al file bilingue delle note di rilascio, sezione versionamento allineata a 1.1

## Task Commits

Ogni task è stato committato atomicamente:

1. **Task 1: Bump versione a 1.1 e riscrivi le note di rilascio** - `9cf4404` (feat)
2. **Task 2: Aggiorna le descrizioni complete IT ed EN alla v1.1** - `0982a3a` (feat)
3. **Task 3: Sincronizza playstore/README.md e segnala gli artefatti da rigenerare** - `a276712` (docs)

_Nessun task TDD in questo piano (solo testi e file di configurazione)._

## Files Created/Modified
- `app/build.gradle.kts` - versionCode 1→2, versionName "1.0"→"1.1"
- `playstore/release_notes/it.txt` - note di rilascio v1.1 in italiano (409 byte)
- `playstore/release_notes/en.txt` - note di rilascio v1.1 in inglese (365 byte)
- `playstore/release_notes/release_notes_v1.1.txt` - nuovo file bilingue con blocchi `<it-IT>`/`<en-US>`
- `playstore/listing/it/full_description.txt` - aggiunte distanza percorsa e indicatore di ricarica, corretta la frase sull'animazione (1849 byte)
- `playstore/listing/en/full_description.txt` - traduzione speculare (1552 byte)
- `playstore/README.md` - artefatti 1.0 segnalati come obsoleti, riferimento al file bilingue, sezione versionamento aggiornata

## Decisions Made
- Mantenuti entrambi i file per locale più il file bilingue unico, come da preferenza utente già registrata in memoria (`feedback_playstore_release_notes`)
- Il file `playstore/listing/en/full_description.txt` non presentava già la riga vuota iniziale spuria descritta nei fatti di planning (probabilmente corretta in una modifica precedente non tracciata separatamente): nessuna azione necessaria oltre a verificare che il file cominci correttamente con "Tachimetro is a no-frills GPS speedometer:"

## Deviations from Plan

None - plan executed exactly as written. L'unica discrepanza rispetto ai "facts" di planning (riga vuota iniziale spuria in `full_description.txt` EN) risultava già assente al momento dell'esecuzione (working tree pulito, nessuna modifica non committata trovata); il file è stato comunque verificato per confermare che inizia correttamente.

## Issues Encountered

Il worktree era inizialmente sul commit HEAD sbagliato (precedente al piano pre-dispatch `4f88bcb`). Riallineato con `git reset --hard 4f88bcb1b71656444d91a3d8ac0b1980432c5c9f` come da protocollo `<worktree_branch_check>` prima di iniziare qualunque modifica — operazione sicura perché il worktree era pulito e HEAD era già confermato sul branch per-agente corretto.

## User Setup Required

None - no external service configuration required.

## Da fare prima della sottomissione reale allo store

- **Rigenerare APK/AAB firmati come `tachimetro-1.1-*`**: richiede `keystore.properties` con le credenziali reali (fuori dal repo, vedi `keystore.properties.example`), poi `./gradlew.bat assembleRelease bundleRelease`. Sostituire `playstore/apk/tachimetro-1.0-unsigned.apk` e `playstore/apk/tachimetro-1.0.aab` con le versioni 1.1 firmate.
- **Ricatturare gli screenshot su dispositivo reale**: quelli attuali risalgono alla v1.0 e non mostrano l'area distanza (in basso a destra) né l'icona di ricarica. Serve una cattura manuale su device reale (non automatizzabile via emulatore/GPS mock come per gli screenshot 1.0): il telefono deve essere realmente in carica per l'icona, e serve movimento GPS reale per la distanza percorsa.

## Next Phase Readiness
Il pacchetto `playstore/` è ora testualmente allineato alla v1.1 e pronto per la revisione. Restano solo gli artefatti binari (APK/AAB firmati, screenshot) da rigenerare manualmente prima della sottomissione reale, come segnalato esplicitamente in `playstore/README.md`.

---
*Quick task: 260830-o3h*
*Completed: 2026-08-30*

## Self-Check: PASSED

All 7 declared files verified present on disk; all 3 task commit hashes (`9cf4404`, `0982a3a`, `a276712`) verified present in git history.
