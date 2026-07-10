# Requirements: Tachimetro

**Defined:** 2026-07-07
**Core Value:** La velocità attuale deve essere sempre visibile, corretta e leggibile istantaneamente in ogni condizione di luce

## v1 Requirements

Requirements for initial release. Each maps to roadmap phases.

### GPS

- [x] **GPS-01**: L'utente vede la velocità attuale in km/h letta dal GPS del dispositivo, aggiornata 1 volta al secondo
- [x] **GPS-02**: L'utente vede un messaggio semplice quando il GPS non ha segnale (es. "Ricerca segnale GPS...")
- [x] **GPS-03**: L'app legge la velocità tramite FusedLocationProviderClient (Google Play Services)

### Interfaccia

- [x] **UI-01**: Il numero della velocità è l'elemento dominante dello schermo, sempre centrato e il più grande possibile
- [x] **UI-02**: Sfondo nero e testo ad alto contrasto (font di sistema Bold/Black) per massima leggibilità in ogni condizione di luce
- [x] **UI-03**: L'app supporta sia orientamento portrait sia landscape, con il layout che si adatta mantenendo il numero centrato
- [x] **UI-04**: L'interfaccia non contiene menu, animazioni o elementi grafici non necessari
- [x] **UI-05**: Tutti i testi e i messaggi dell'app sono in italiano

### Velocità Massima

- [x] **MAX-01**: L'utente vede la velocità massima raggiunta dall'ultimo azzeramento in un'area secondaria dello schermo
- [x] **MAX-02**: L'utente può premere un pulsante "Azzera massimo" per azzerare il valore e iniziare una nuova misurazione
- [x] **MAX-03**: La velocità massima persiste su disco e sopravvive a chiusura app e riavvio del telefono

### Schermo

- [x] **SCRN-01**: L'utente può scegliere tra "Schermo sempre acceso" e "Schermo automatico" tramite un toggle
- [x] **SCRN-02**: Quando "Schermo sempre acceso" è attivo, lo schermo non si spegne durante l'utilizzo dell'app
- [x] **SCRN-03**: La preferenza scelta viene salvata e ripristinata nelle sessioni successive

### Permessi

- [x] **PERM-01**: L'app richiede solo il permesso ACCESS_FINE_LOCATION necessario per leggere il GPS
- [x] **PERM-02**: L'app gestisce correttamente sia la concessione sia il rifiuto del permesso GPS, mostrando un messaggio appropriato in caso di rifiuto

### Comportamento App

- [x] **APP-01**: L'app si apre direttamente sulla schermata della velocità, senza schermate iniziali o menu

## v2 Requirements

(Nessuno — tutto quanto specificato dall'utente rientra nello scope v1)

## Out of Scope

| Feature | Reason |
|---------|--------|
| Cambio unità km/h ↔ mph | Non richiesto per v1, riduce complessità UI |
| Tracciamento percorso / mappa / cronologia velocità | L'app è un tachimetro istantaneo, non un GPS tracker |
| Menu, impostazioni avanzate, grafici, animazioni | Contrario alla filosofia di interfaccia minimale del prodotto |
| Supporto Android precedente alla versione 11 (minSdk < 30) | Il progetto fissa già minSdk 30, nessun device più vecchio da supportare |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| GPS-01 | Phase 2 | Complete |
| GPS-02 | Phase 2 | Complete |
| GPS-03 | Phase 2 | Complete |
| UI-01 | Phase 3 | Complete |
| UI-02 | Phase 3 | Complete |
| UI-03 | Phase 3 | Complete |
| UI-04 | Phase 3 | Complete |
| UI-05 | Phase 3 | Complete |
| MAX-01 | Phase 4 | Complete |
| MAX-02 | Phase 4 | Complete |
| MAX-03 | Phase 4 | Complete |
| SCRN-01 | Phase 5 | Complete |
| SCRN-02 | Phase 5 | Complete |
| SCRN-03 | Phase 5 | Complete |
| PERM-01 | Phase 1 | Complete |
| PERM-02 | Phase 1 | Complete |
| APP-01 | Phase 1 | Complete |

**Coverage:**
- v1 requirements: 17 total
- Mapped to phases: 17
- Unmapped: 0 ✓

---
*Requirements defined: 2026-07-07*
*Last updated: 2026-07-10 after Phase 5 completion (v1 milestone complete — 17/17 requirements)*
