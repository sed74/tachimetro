# Requirements: Tachimetro

**Defined:** 2026-09-23
**Core Value:** La velocità attuale deve essere sempre visibile, corretta e leggibile istantaneamente in ogni condizione di luce

## v2.1 Requirements

Requirements for the v2.1 milestone (Velocità a tutto schermo su Android Auto). Each maps to roadmap phases.
Research: `.planning/research/SUMMARY.md`. Visual spec di partenza: `.planning/milestones/v2.0-phases/08-*/08-CONTEXT.md` (D-14).

### Schermo Android Auto a tutto schermo

- [ ] **AA-05**: L'utente vede la velocità come numero grande, centrato nell'area dello schermo auto garantita libera dall'host (stable area), disegnato direttamente sulla Surface (non più `PaneTemplate` host-controlled)
- [ ] **AA-06**: La dimensione del numero è fissa, calcolata sul caso peggiore a 3 cifre ("888") — il numero non cambia dimensione passando da 99 a 100 km/h
- [ ] **AA-07**: L'unità "km/h" è mostrata piccola in basso a destra dell'area libera, separata dal numero
- [ ] **AA-08**: Nessuna icona app né titolo testuale sopra il numero, nei limiti del template scelto (REL-01)
- [ ] **AA-09**: Lo schermo auto segue il tema giorno/notte dell'auto (`carContext.isDarkMode`), con alto contrasto in entrambe le modalità e ridisegno immediato al cambio
- [ ] **AA-10**: La velocità si aggiorna 1 volta al secondo ridisegnando la Surface (senza `invalidate()` per tick, quindi senza consumare la quota template); dopo una ricreazione della Surface da parte dell'host l'ultimo stato ricompare subito — mai uno schermo nero
- [ ] **AA-11**: Lo stato "Ricerca segnale..." resta visibile sullo schermo auto con il nuovo rendering
- [ ] **AA-12**: Il flusso permesso della Fase 9 (richiesta automatica, negato, negato permanente, azioni riprova/impostazioni eseguibili solo a veicolo fermo) funziona come in v2.0 con il nuovo rendering

### Distribuzione

- [ ] **REL-01**: Categoria e template dello schermo auto (POI + `MapWithContentTemplate` oppure NAVIGATION + `NavigationTemplate`) vengono scelti dopo uno spike su DHU che misura lo spazio libero reale di entrambe le strade; la scelta è una decisione esplicita dell'utente, registrata in PROJECT.md Key Decisions
- [ ] **REL-02**: `minCarApiLevel` portato a 7 (nessun fallback `PaneTemplate`); le head unit con Car API < 7 non vedono più l'app — accettato consapevolmente dall'utente
- [ ] **REL-03**: La build v2.1 passa prima dal canale di test chiuso e viene promossa al canale aperto solo dopo esito positivo della revisione Android Auto; `playstore/` aggiornato (versione, note di rilascio, listing, permessi) e deploy-ready

### Pulizie

- [ ] **CLEAN-01**: Il rilevamento dello stato di ricarica sul telefono ha un'unica fonte di verità (`MainActivity.isDeviceCharging()` consolidato con `deriveChargingState()`), senza cambiamenti di comportamento visibili (default "sempre acceso" al primo avvio, icona di ricarica)
- [ ] **CLEAN-02**: Aprendo o riprendendo l'app con Android Auto già connesso, il telefono mostra direttamente lo stato neutro "Connesso ad Android Auto", senza far apparire per un istante il tachimetro (finestra transitoria di `carLink`)

## Future Requirements

- Fix del salto di distanza spurio al primo fix dopo la ripresa della pipeline GPS (`GpsSpeedProvider.lastAcceptedLocation` non resettato)
- Verifica rimandata Fase 11 SC3 — 10 cicli rapidi di connessione/disconnessione Android Auto su hardware reale
- Widget Android Auto, se Google li documenta (annuncio maggio 2026)
- Flavor NAVIGATION distribuito solo su test interno, se lo spike REL-01 sceglie POI e l'utente vuole comunque lo schermo pulito
- Aggiornamento a `androidx.car.app` 1.8.0 stabile (contiene una correzione di sicurezza), come attività separata

## Out of Scope

| Feature | Reason |
|---------|--------|
| Velocità massima e distanza sullo schermo auto | Restano solo sul telefono (decisione v2.0) — più numeri sul display auto peggiorano il Core Value |
| Animazioni sullo schermo auto | Regola di qualità SA-1 e vincolo UX del progetto |
| Mappe / Maps SDK / Mapbox sulla Surface | Il tachimetro non è un navigatore; la Surface serve solo al numero |
| Fallback `PaneTemplate` per host con Car API < 7 | Scelta utente: `minCarApiLevel` 7, codice più semplice (REL-02) |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| (filled by roadmap) | | |

---
*Requirements defined: 2026-09-23*
