---
gsd_state_version: 1.0
milestone: v2.0
milestone_name: Android Auto Support
status: planning
last_updated: "2026-08-31T09:00:00.000Z"
last_activity: 2026-08-31
progress:
  total_phases: 4
  completed_phases: 0
  total_plans: 0
  completed_plans: 0
  percent: 0
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-08-30)

**Core value:** La velocità attuale deve essere sempre visibile, corretta e leggibile istantaneamente in ogni condizione di luce
**Current focus:** Phase 8: Fondamenta Condivise e Velocità sullo Schermo Auto

## Current Position

Phase: 8 of 11 (Fondamenta Condivise e Velocità sullo Schermo Auto)
Plan: — (fase non ancora pianificata)
Status: Ready to plan
Last activity: 2026-08-31 — Roadmap v2.0 creato (Fasi 8-11), in attesa di approvazione utente

## Performance Metrics

**Velocity:**

- Total plans completed: 18
- Average duration: - min
- Total execution time: 0 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 1 | 2 | - | - |
| 2 | 3 | - | - |
| 03 | 1 | - | - |
| 04 | 2 | - | - |
| 05 | 2 | - | - |
| 06 | 4 | - | - |
| 07 | 4 | - | - |
| 08 | TBD | - | - |
| 09 | TBD | - | - |
| 10 | TBD | - | - |
| 11 | TBD | - | - |

**Recent Trend:**

- Last 5 plans: none yet (v2.0 non ancora pianificata)
- Trend: -

*Updated after each plan completion*

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

- Roadmap v1.0: Horizontal-layer structure (Fondamenta → GPS → UI → Max Speed → Schermo), coarse granularity, 5 phases
- Roadmap v1.1: Due fasi indipendenti per feature (coarse granularity) — Fase 6 Indicatore di Ricarica (CHRG-01/02), Fase 7 Distanza Percorsa e Reset Unificato (DIST-01/02/03, MAX-04); Fase 7 dipende da Fase 4 per il pattern di reset condiviso
- Roadmap v2.0: Categoria/percorso di distribuzione già risolti dall'utente (template standard Car App Library, categoria POI, Play-Store-safe — non serve una fase dedicata). Quattro fasi sequenziali (coarse granularity): Fase 8 Fondamenta Condivise e Velocità sullo Schermo Auto (AA-01/02/03 — `GpsSpeedProvider` promosso ad Application-scoped, scaffold `TachimetroCarAppService`/`Session`/`SpeedScreen`, verifica empirica della quota di refresh dei template sotto aggiornamento continuo 1Hz); Fase 9 Permesso di Localizzazione dallo Schermo Auto (AA-04, dipende dallo scaffold di Fase 8); Fase 10 Comportamento del Telefono alla Connessione Android Auto (CONN-01/02, estende `ScreenOnPreferenceStore` v1.0, indipendente dal lavoro sullo schermo auto ma sequenziata dopo per coerenza); Fase 11 Hardening di Produzione e Verifica su Dispositivo Reale (nessun nuovo requisito — `HostValidator` reale al posto di `ALLOW_ALL_HOSTS_VALIDATOR`, verifica su strada del comportamento background-location a telefono bloccato)

### Pending Todos

None yet.

### Blockers/Concerns

- Fase 8/11: il comportamento della quota di refresh dei template Android Auto per un valore numerico che cambia ogni secondo non è chiaramente documentato da Google — richiede verifica empirica (DHU + Developer Mode) prima di impegnarsi sul percorso a template per l'intera milestone (v. `.planning/research/SUMMARY.md`, Pitfall 2)
- Fase 11: il comportamento del GPS in background quando il telefono è bloccato durante una sessione Android Auto attiva non è documentato — genuino gap di piattaforma da verificare su dispositivo reale, non solo su DHU (v. `.planning/research/SUMMARY.md`, Pitfall 5)

### Quick Tasks Completed

| # | Description | Date | Commit | Directory |
|---|-------------|------|--------|-----------|
| 260710-tuh | Impedire wrapping numero velocità (maxLines dinamico su messageText) | 2026-07-10 | 3600992 | [260710-tuh-impedire-wrapping-del-numero-velocit-max](./quick/260710-tuh-impedire-wrapping-del-numero-velocit-max/) |
| 260822-huf | Genera asset e testi per pubblicazione Play Store in playstore/ | 2026-08-22 | 8bcae15 | [260822-huf-genera-asset-e-testi-per-pubblicazione-p](./quick/260822-huf-genera-asset-e-testi-per-pubblicazione-p/) |
| 260829-tgw | Icona di ricarica più grande e animazione con svuotamento istantaneo invece di simmetrico | 2026-08-29 | 6303338 | [260829-tgw-icona-di-ricarica-pi-grande-e-animazione](./quick/260829-tgw-icona-di-ricarica-pi-grande-e-animazione/) |
| 260830-o3h | Aggiorna playstore/ per la milestone v1.1 (bump versione, note di rilascio, descrizioni, README) | 2026-08-30 | a276712 | [260830-o3h-aggiorna-playstore-per-la-milestone-v1-1](./quick/260830-o3h-aggiorna-playstore-per-la-milestone-v1-1/) |

## Deferred Items

Items acknowledged and carried forward from previous milestone close:

| Category | Item | Status | Deferred At |
|----------|------|--------|-------------|
| *(none)* | | | |

## Session Continuity

Last session: 2026-08-31
Stopped at: Roadmap v2.0 creato (Fasi 8-11) e REQUIREMENTS.md aggiornato con la traceability — in attesa di approvazione utente
Resume file: none

## Operator Next Steps

- Rivedere il roadmap v2.0 (Fasi 8-11) e, se approvato, avviare `/gsd:discuss-phase 8` o `/gsd:plan-phase 8` per iniziare la pianificazione della Fase 8
