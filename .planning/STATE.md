---
gsd_state_version: 1.0
milestone: v2.0
milestone_name: Android Auto Support
status: executing
stopped_at: Completed 09-02-PLAN.md
last_updated: "2026-09-02T12:03:59.826Z"
last_activity: 2026-09-02
progress:
  total_phases: 4
  completed_phases: 1
  total_plans: 6
  completed_plans: 5
  percent: 25
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-09-02)

**Core value:** La velocità attuale deve essere sempre visibile, corretta e leggibile istantaneamente in ogni condizione di luce
**Current focus:** Phase 09 — permesso-di-localizzazione-dallo-schermo-auto

## Current Position

Phase: 09 (permesso-di-localizzazione-dallo-schermo-auto) — EXECUTING
Plan: 3 of 3
Status: Ready to execute
Last activity: 2026-09-02

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
| 08 | 3 | - | - |
| 09 | TBD | - | - |
| 10 | TBD | - | - |
| 11 | TBD | - | - |

**Recent Trend:**

- Last 5 plans: 08-01 (~20min), 08-02 (~15min), 08-03 (checkpoint spans multiple human sessions)
- Trend: -

*Updated after each plan completion*
| Phase 09 P01 | 12min | 3 tasks | 4 files |
| Phase 09 P02 | 10min | 2 tasks | 1 files |

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

- Roadmap v1.0: Horizontal-layer structure (Fondamenta → GPS → UI → Max Speed → Schermo), coarse granularity, 5 phases
- Roadmap v1.1: Due fasi indipendenti per feature (coarse granularity) — Fase 6 Indicatore di Ricarica (CHRG-01/02), Fase 7 Distanza Percorsa e Reset Unificato (DIST-01/02/03, MAX-04); Fase 7 dipende da Fase 4 per il pattern di reset condiviso
- Roadmap v2.0: Categoria/percorso di distribuzione già risolti dall'utente (template standard Car App Library, categoria POI, Play-Store-safe — non serve una fase dedicata). Quattro fasi sequenziali (coarse granularity): Fase 8 Fondamenta Condivise e Velocità sullo Schermo Auto (AA-01/02/03 — `GpsSpeedProvider` promosso ad Application-scoped, scaffold `TachimetroCarAppService`/`Session`/`SpeedScreen`, verifica empirica della quota di refresh dei template sotto aggiornamento continuo 1Hz); Fase 9 Permesso di Localizzazione dallo Schermo Auto (AA-04, dipende dallo scaffold di Fase 8); Fase 10 Comportamento del Telefono alla Connessione Android Auto (CONN-01/02, estende `ScreenOnPreferenceStore` v1.0, indipendente dal lavoro sullo schermo auto ma sequenziata dopo per coerenza); Fase 11 Hardening di Produzione e Verifica su Dispositivo Reale (nessun nuovo requisito — `HostValidator` reale al posto di `ALLOW_ALL_HOSTS_VALIDATOR`, verifica su strada del comportamento background-location a telefono bloccato)
- [Phase 08]: SC4 (quota refresh) confermato PASS via sessione DHU dal vivo su telefono fisico (586 refresh/608s, cadenza 0.964/s)
- [Phase 08]: SC1 fallito come formulato (limite strutturale PaneTemplate); accettato per v2.0, NavigationTemplate+SurfaceCallback rimandato a milestone v2.1 (08-CONTEXT.md D-12..D-14)
- [Phase 08]: SC2 (perdita segnale) accettato senza test live, su richiesta esplicita dell utente; copertura solo indiretta via test unitari gia esistenti
- [Phase 09]: Piano 02: SpeedScreen sostituisce il gate T-08-08 con una macchina a stati reattiva (permissionState + requestInFlight); requestPermissions() automatico su NotRequested, nessun rilancio automatico dopo un rifiuto (D-06), Denied distingue permanente da singolo via denialCount letto PRIMA di recordDenial() (D-04)

### Pending Todos

None yet.

### Blockers/Concerns

- ~~Fase 8/11: il comportamento della quota di refresh dei template Android Auto...~~ — **Risolto in Fase 8 (08-03)**: verifica empirica dal vivo su DHU conferma che l'host non chiude l'app sotto refresh continuo a 1Hz (586 refresh/608s, cadenza 0.964/s, PID mai cambiato). Vedi `08-CONTEXT.md` D-11 e `08-03-SUMMARY.md`.
- Fase 11: il comportamento del GPS in background quando il telefono è bloccato durante una sessione Android Auto attiva non è documentato — genuino gap di piattaforma da verificare su dispositivo reale, non solo su DHU (v. `.planning/research/SUMMARY.md`, Pitfall 5)
- v2.1 (milestone futura, non v2.0): `PaneTemplate` non permette un numero grande/centrato ne' unita' posizionabile ne' rimozione dell'icona app (limite strutturale, D-12 in `08-CONTEXT.md`) — accettato per v2.0, valutare `NavigationTemplate`+`SurfaceCallback` in una milestone v2.1 dedicata (D-14, visual spec gia' raccolta in `08-CONTEXT.md` sezione `<deferred>`)
- Fase 9+: SC2 (comportamento schermo auto alla perdita di segnale GPS) non è mai stato verificato dal vivo su DHU — solo copertura indiretta via test unitari (`CarSpeedContentTest`, `GpsSpeedProviderStateTest`). Se un problema di visualizzazione emergesse in una fase futura (es. verifica su strada in Fase 11), non assumere che sia già stato escluso empiricamente qui

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

Last session: 2026-09-02T12:03:59.815Z
Stopped at: Completed 09-02-PLAN.md
Resume file: None

## Operator Next Steps

- Fase 8 completa. Avviare `/gsd:discuss-phase 9` o `/gsd:plan-phase 9` per iniziare la pianificazione della Fase 9 (Permesso di Localizzazione dallo Schermo Auto, AA-04)
- Passo di transizione/verifica di fase (convenzione osservata in questo progetto per le Fasi 4/6/7: `XX-VERIFICATION.md` + evoluzione di `PROJECT.md` via `/gsd-transition`) non ancora eseguito per la Fase 8 — valutare se eseguirlo prima di iniziare la Fase 9
