# Requirements: Tachimetro

**Defined:** 2026-08-29
**Core Value:** La velocità attuale deve essere sempre visibile, corretta e leggibile istantaneamente in ogni condizione di luce

## v1.1 Requirements

Requirements for the v1.1 milestone (ricarica e distanza). Each maps to roadmap phases.

### Ricarica

- [ ] **CHRG-01**: L'utente vede un'icona a fulmine accanto al toggle "sempre acceso" solo quando il telefono è in carica
- [ ] **CHRG-02**: L'icona anima un riempimento progressivo dal basso verso l'alto, bianco → lime → bianco, in loop continuo (~2-3s per ciclo) per tutta la durata della ricarica

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
| CHRG-01 | TBD | Pending |
| CHRG-02 | TBD | Pending |
| DIST-01 | TBD | Pending |
| DIST-02 | TBD | Pending |
| DIST-03 | TBD | Pending |
| MAX-04 | TBD | Pending |

**Coverage:**
- v1.1 requirements: 6 total
- Mapped to phases: 0
- Unmapped: 6 ⚠️ (roadmap not yet created)

---
*Requirements defined: 2026-08-29*
*Last updated: 2026-08-29 after initial v1.1 definition*
