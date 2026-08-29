---
phase: quick-260829-tgw
plan: 01
subsystem: ui
tags: [android, charging-indicator, animation, manual-verification]

# Dependency graph
requires:
  - phase: 06-indicatore-di-ricarica
    provides: ChargingStateProvider + chargingIcon wiring in MainActivity, 2 follow-up richieste raccolte in 06-04-SUMMARY.md
provides:
  - chargingIcon ingrandita da 24dp a 36dp, stessa posizione/vincoli
  - Animazione di riempimento con svuotamento istantaneo (RESTART invece di REVERSE), ciclo 2500ms
  - Verifica umana su dispositivo reale che chiude i 2 follow-up di 06-04
affects: [06-indicatore-di-ricarica]

# Tech tracking
tech-stack:
  added: []
  patterns: []

key-files:
  created: []
  modified:
    - app/src/main/res/layout/activity_main.xml
    - app/src/main/java/com/sed/tachimetro/MainActivity.kt
    - .planning/phases/06-indicatore-di-ricarica/06-UI-SPEC.md

key-decisions:
  - "36dp confermato come dimensione finale dell'icona (nessuna richiesta di misura diversa in checkpoint)"
  - "CHARGING_FILL_CYCLE_MS = 2500L confermato come durata finale del ciclo di riempimento (nessuna richiesta di durata diversa in checkpoint)"

patterns-established: []

requirements-completed: [CHRG-01, CHRG-02]

# Metrics
duration: 20min
completed: 2026-08-29
---

# Quick Task 260829-tgw: Icona di ricarica più grande e animazione con svuotamento istantaneo Summary

**chargingIcon ingrandita a 36dp e animazione di riempimento passata da REVERSE simmetrico a RESTART con svuotamento istantaneo (2500ms), entrambe approvate su dispositivo reale**

## Performance

- **Duration:** 20 min
- **Started:** 2026-08-29
- **Completed:** 2026-08-29
- **Tasks:** 3 (2 auto + 1 checkpoint:human-verify)
- **Files modified:** 3

## Accomplishments
- `chargingIcon` ingrandita da 24dp a 36dp, mantenendo posizione, vincoli e riga condivisa con `keepScreenOnSwitch` invariati
- Animazione di riempimento riscritta: `CHARGING_FILL_HALF_CYCLE_MS` (1250L, REVERSE) sostituita da `CHARGING_FILL_CYCLE_MS` (2500L, RESTART) — riempimento graduale morbido seguito da azzeramento istantaneo, non animato
- `06-UI-SPEC.md` aggiornato con sezione "Revisions" che registra i due valori superati, evitando contraddizioni tra spec e codice
- Verifica manuale sul dispositivo reale dell'utente: tutti gli 8 punti della checklist PASS, con risposta esplicita "approvato"
- I 2 follow-up aperti in `06-04-SUMMARY.md` (icona più grande, svuotamento istantaneo) sono ora chiusi

## Task Commits

1. **Task 1: Ingrandire l'icona di ricarica da 24dp a 36dp** - `a1437c6` (feat)
2. **Task 2: Riempimento graduale con svuotamento istantaneo (RESTART invece di REVERSE)** - `3901585` (feat)
3. **Task 3: Checkpoint umano su dispositivo reale** - nessun commit di codice (solo verifica umana, esito registrato qui sotto)

**Plan metadata:** commit successivo a questo file (docs: complete quick task)

## Files Created/Modified
- `app/src/main/res/layout/activity_main.xml` - `chargingIcon` width/height 24dp -> 36dp, tutto il resto invariato
- `app/src/main/java/com/sed/tachimetro/MainActivity.kt` - `CHARGING_FILL_CYCLE_MS = 2500L` sostituisce `CHARGING_FILL_HALF_CYCLE_MS`; `startChargingFillAnimation()` usa `repeatMode = ValueAnimator.RESTART` e `duration = CHARGING_FILL_CYCLE_MS`; commenti aggiornati; `freezeChargingFillAtFull()`, `stopChargingFillAnimation()`, `updateChargingIcon()` invariati
- `.planning/phases/06-indicatore-di-ricarica/06-UI-SPEC.md` - aggiunta sezione "Revisions (post-checkpoint, quick task 260829-tgw)" con i due valori superati (24dp -> 36dp; REVERSE 1250+1250ms -> RESTART 2500ms con svuotamento istantaneo)

## Esito Task 3: Verifica visiva su dispositivo reale (checkpoint umano)

L'utente ha installato manualmente l'APK sul proprio dispositivo fisico, collegato e scollegato fisicamente un caricabatterie reale, e ha risposto **"approvato"** all'intera checklist.

Esito punto per punto (tutti PASS, per dichiarazione esplicita dell'utente "approvato"):

| # | Punto di verifica | Esito |
|---|--------------------|-------|
| 1 | Dimensione — il fulmine a sinistra di "Sempre acceso" è chiaramente più grande di prima (36dp), ben leggibile a colpo d'occhio | PASS |
| 2 | Riempimento — il lime sale dal basso verso l'alto in modo graduale e morbido, come prima | PASS |
| 3 | Svuotamento istantaneo — arrivato in cima, il fulmine torna bianco di colpo, senza scendere gradualmente, e riparte subito a riempirsi | PASS |
| 4 | Durata — un ciclo completo (bianco -> pieno lime -> scatto a bianco) dura circa 2,5 secondi | PASS |
| 5 | Layout intatto — "Sempre acceso" alla stessa altezza di prima, icona e switch interamente visibili sia in verticale che in orizzontale | PASS |
| 6 | Batteria piena — icona ferma e completamente lime, senza movimento | PASS |
| 7 | Scollegamento — icona sparisce all'istante staccando il cavo, anche a metà riempimento; ricollegando riparte da vuoto/bianco | PASS |
| 8 | Nessun lime altrove — il lime resta solo dentro il fulmine, nessuna altra animazione compare nella UI | PASS |

Requisiti coperti:
- CHRG-01 (comparsa/scomparsa dell'icona in base allo stato di carica) → **PASS** (invariato, non toccato da questo quick task, riconfermato al punto 7)
- CHRG-02 (animazione di riempimento loop bianco→lime→bianco, ~2-3s) → **PASS** (punti 2, 3, 4 — ora con svuotamento istantaneo come richiesto)

Nessuna dimensione o durata alternativa è stata richiesta in checkpoint: 36dp e 2500ms sono i valori finali approvati.

I 2 follow-up estetici aperti in `06-04-SUMMARY.md` sono ora chiusi. La Fase 6 (indicatore di ricarica) resta completa senza ulteriori azioni pendenti.

## Decisions Made
- 36dp confermato come dimensione finale dell'icona di ricarica (nessuna correzione richiesta)
- `CHARGING_FILL_CYCLE_MS = 2500L` con `RESTART` confermato come comportamento finale dell'animazione di riempimento (nessuna correzione richiesta)

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- I 2 follow-up di rifinitura estetica emersi da `06-04-SUMMARY.md` sono chiusi; nessun lavoro pendente collegato alla Fase 6
- Nessun blocco per l'avvio di lavoro futuro (es. Fase 7 - Distanza)

---
*Phase: quick-260829-tgw*
*Completed: 2026-08-29*

## Self-Check: PASSED
- FOUND: app/src/main/res/layout/activity_main.xml
- FOUND: app/src/main/java/com/sed/tachimetro/MainActivity.kt
- FOUND: .planning/phases/06-indicatore-di-ricarica/06-UI-SPEC.md
- FOUND: .planning/quick/260829-tgw-icona-di-ricarica-pi-grande-e-animazione/260829-tgw-SUMMARY.md
- FOUND: a1437c6
- FOUND: 3901585
