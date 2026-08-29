# Requirements: Tachimetro

**Defined:** 2026-08-29
**Core Value:** La velocità attuale deve essere sempre visibile, corretta e leggibile istantaneamente in ogni condizione di luce

## v1.1 Requirements

Requirements for the v1.1 milestone (ricarica e distanza). Each maps to roadmap phases.

### Ricarica

- [x] **CHRG-01**: L'utente vede un'icona a fulmine accanto al toggle "sempre acceso" solo quando il telefono è in carica
- [x] **CHRG-02**: L'icona anima un riempimento progressivo dal basso verso l'alto, bianco → lime → bianco, in loop continuo (~2-3s per ciclo) per tutta la durata della ricarica

### Distanza

- [ ] **DIST-01**: L'utente vede la distanza percorsa dall'ultimo azzeramento in un'area in basso a destra, con font più grande dell'area velocità massima
- [ ] **DIST-02**: La distanza si accumula solo mentre l'app è attiva e riceve aggiornamenti GPS, senza tracking in background
- [ ] **DIST-03**: La distanza persiste su disco e sopravvive a chiusura app e riavvio del telefono

### Velocità Massima

- [ ] **MAX-04**: Il pulsante "Azzera massimo" azzera sia la velocità massima sia la distanza percorsa in un'unica azione

## v2 Requirements

(Nessuno — tutto quanto specificato dall'utente rientra nello scope v1.1)

## Out of Scope

| Feature | Reason |
|---------|--------|
| Cambio unità km/h ↔ mph | Non richiesto, l'app mostra solo km/h |
| Tracciamento percorso / mappa / cronologia velocità | L'app è un tachimetro istantaneo, non un GPS tracker |
| Menu, impostazioni avanzate, grafici | Contrario alla filosofia di interfaccia minimale del prodotto |
| Altre animazioni o colori oltre all'icona di ricarica | L'eccezione lime/animazione è mirata solo allo stato di ricarica, non un cambio generale di stile |
| Supporto Android precedente alla versione 11 (minSdk < 30) | Il progetto fissa già minSdk 30, nessun device più vecchio da supportare |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| CHRG-01 | Phase 6 | Complete |
| CHRG-02 | Phase 6 | Complete |
| DIST-01 | Phase 7 | Pending |
| DIST-02 | Phase 7 | Pending |
| DIST-03 | Phase 7 | Pending |
| MAX-04 | Phase 7 | Pending |

**Coverage:**
- v1.1 requirements: 6 total
- Mapped to phases: 6
- Unmapped: 0 ✓

---
*Requirements defined: 2026-08-29*
*Last updated: 2026-08-29 after v1.1 roadmap creation (Phase 6-7, 6/6 requirements mapped)*
