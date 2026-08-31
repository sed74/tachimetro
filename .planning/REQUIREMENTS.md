# Requirements: Tachimetro

**Defined:** 2026-08-31
**Core Value:** La velocità attuale deve essere sempre visibile, corretta e leggibile istantaneamente in ogni condizione di luce

## v2.0 Requirements

Requirements for the v2.0 milestone (Android Auto Support). Each maps to roadmap phases.

### Schermo Android Auto

- [ ] **AA-01**: L'utente vede la velocità attuale come testo grande sullo schermo Android Auto mentre l'app è connessa (categoria POI, template standard Car App Library — stile/typografia gestiti dall'host)
- [ ] **AA-02**: Quando manca il segnale GPS, lo schermo Android Auto mostra uno stato equivalente al messaggio "Ricerca segnale GPS..." del telefono, invece di restare bloccato su un valore vecchio
- [ ] **AA-03**: Lo schermo Android Auto si aggiorna alla stessa cadenza del telefono (1 volta al secondo)
- [ ] **AA-04**: Se il permesso di localizzazione non è ancora stato concesso quando l'utente si connette per la prima volta via Android Auto, lo schermo auto lo richiede esplicitamente (`CarContext.requestPermissions()`) invece di restare vuoto/bloccato

### Connessione Android Auto

- [ ] **CONN-01**: Quando Android Auto si connette, il telefono rilascia lo schermo sempre acceso (se attivo) e mostra uno stato neutro "Connesso ad Android Auto"
- [ ] **CONN-02**: Alla disconnessione di Android Auto, il telefono ripristina esattamente il comportamento "sempre acceso" precedente (dalla preferenza salvata), senza reset indesiderati

## Future Requirements (v1.x/v2+, non in questa milestone)

- Rivalutare la fedeltà visiva del template su un head unit reale/DHU; se insufficiente, valutare una Surface personalizzata (rendering identico al telefono) solo per distribuzione test interno/chiuso — non Play Store pubblico, perché richiederebbe la categoria NAVIGATION e navigazione reale

## Out of Scope

| Feature | Reason |
|---------|--------|
| Rendering a schermo pieno personalizzato (Canvas/Surface) identico al telefono | Richiede categoria NAVIGATION + navigazione turn-by-turn reale per passare la review Play Store sui canali pubblici; non pubblicabile per l'MVP. Decisione presa in fase di ricerca (v. `.planning/research/SUMMARY.md`) |
| Velocità massima e distanza percorsa sullo schermo auto | Restano solo sul telefono, come deciso nello scope della milestone — più numeri su uno schermo auto è una regressione per il Core Value, non una funzionalità |
| Pulsanti interattivi o schermata impostazioni sullo schermo auto | Nessuna interazione prevista, coerente con la filosofia "nessun menu" dell'app; niente da configurare lato auto |
| Supporto Android Automotive OS nativo (non proiettato) | Fuori scope esplicito della milestone — impegno molto più grande (build OS nativo separato), non richiesto |
| Spegnimento/blocco forzato dello schermo del telefono | Non esiste un'API pubblica per farlo senza permessi Device Admin sproporzionati e in conflitto con la filosofia "nessun permesso non necessario" del progetto; il rilascio di `FLAG_KEEP_SCREEN_ON` (CONN-01) è il massimo realisticamente ottenibile |

## Traceability

_Da compilare dal roadmapper._
