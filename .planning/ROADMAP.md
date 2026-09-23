# Roadmap: Tachimetro

## Overview

Tachimetro nasce da uno scaffold Android Studio vuoto e arriva a un'app completa: si parte dalle fondamenta (avvio diretto, permesso GPS), si costruisce il motore di lettura della velocità, poi l'interfaccia a schermo intero che la mostra, quindi le funzionalità di velocità massima con persistenza, e infine il controllo dello schermo sempre acceso. Con la milestone v1.1 si aggiungono due indicatori secondari indipendenti: uno stato di ricarica riconoscibile a colpo d'occhio (unica animazione e unico colore accento ammessi nell'interfaccia) e una distanza percorsa persistente, azzerabile nella stessa azione del record di velocità massima già esistente. Con la milestone v2.0 la velocità viene proiettata anche sullo schermo Android Auto dell'auto/moto: prima si condivide la fonte GPS tra telefono e auto senza duplicarla, poi si mostra la velocità (e lo stato di assenza di segnale) sul display auto con i template standard della Car App Library, si gestisce il permesso di localizzazione richiesto direttamente dallo schermo auto, si adatta il comportamento del telefono quando Android Auto è connesso, e infine si mette in sicurezza il tutto per l'uso reale su strada. Con la milestone v2.1 il numero sul display auto diventa grande e centrato come sul telefono: prima uno spike su DHU misura lo spazio reale offerto dai due percorsi possibili (POI + `MapWithContentTemplate` oppure NAVIGATION + `NavigationTemplate`) e porta l'utente a una scelta esplicita, poi si costruisce il rendering diretto sulla Surface preservando stati e flusso permesso della v2.0, si chiudono due piccole pulizie lato telefono e infine si verifica tutto su head unit reale e si rilascia passando dal canale di test chiuso.

## Milestones

- ✅ **v1.0 MVP** — Fasi 1-5 (shipped 2026-07-10) → [archivio completo](milestones/v1.0-ROADMAP.md)
- ✅ **v1.1 Ricarica e distanza** — Fasi 6-7 (shipped 2026-08-30) → [archivio completo](milestones/v1.1-ROADMAP.md)
- ✅ **v2.0 Android Auto Support** — Fasi 8-11 (shipped 2026-09-23) → [archivio completo](milestones/v2.0-ROADMAP.md)
- 🚧 **v2.1 Velocità a tutto schermo su Android Auto** — Fasi 12-15 (in corso)

## Phases

**Phase Numbering:**

- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

<details>
<summary>✅ v1.0 MVP (Fasi 1-5) — SHIPPED 2026-07-10</summary>

- [x] Phase 1: Fondamenta, Permessi e Avvio (2/2 plans) — completed 2026-07-07
- [x] Phase 2: Motore GPS (3/3 plans) — completed 2026-07-07
- [x] Phase 3: Interfaccia Tachimetro (1/1 plan) — completed 2026-07-10
- [x] Phase 4: Velocità Massima e Persistenza (2/2 plans) — completed 2026-07-10
- [x] Phase 5: Gestione Schermo (2/2 plans) — completed 2026-07-10

Dettagli completi delle fasi: [milestones/v1.0-ROADMAP.md](milestones/v1.0-ROADMAP.md)

</details>

<details>
<summary>✅ v1.1 Ricarica e distanza (Fasi 6-7) — SHIPPED 2026-08-30</summary>

- [x] Phase 6: Indicatore di Ricarica (4/4 plans) — completed 2026-08-29
- [x] Phase 7: Distanza Percorsa e Reset Unificato (4/4 plans) — completed 2026-08-30

Dettagli completi delle fasi: [milestones/v1.1-ROADMAP.md](milestones/v1.1-ROADMAP.md)

</details>

<details>
<summary>✅ v2.0 Android Auto Support (Fasi 8-11) — SHIPPED 2026-09-23</summary>

- [x] Phase 8: Fondamenta Condivise e Velocità sullo Schermo Auto (3/3 plans) — completed 2026-09-02
- [x] Phase 9: Permesso di Localizzazione dallo Schermo Auto (3/3 plans) — completed 2026-09-02
- [x] Phase 10: Comportamento del Telefono alla Connessione Android Auto (3/3 plans) — completed 2026-09-02
- [x] Phase 11: Hardening di Produzione e Verifica su Dispositivo Reale (4/4 plans) — completed 2026-09-23

Dettagli completi delle fasi: [milestones/v2.0-ROADMAP.md](milestones/v2.0-ROADMAP.md)

</details>

### 🚧 v2.1 Velocità a tutto schermo su Android Auto (Fasi 12-15)

- [ ] **Phase 12: Spike Surface e Decisione Categoria/Template** - Misurare su DHU entrambi i percorsi con disegno sulla Surface e far scegliere all'utente categoria e template
- [ ] **Phase 13: Velocità a Tutto Schermo sulla Surface** - Numero grande e centrato disegnato sulla Surface, tema giorno/notte, stati "Ricerca segnale..." e flusso permesso preservati
- [ ] **Phase 14: Pulizie Lato Telefono** - Un'unica fonte di verità per lo stato di ricarica e nessun lampo del tachimetro all'avvio con Android Auto già connesso
- [ ] **Phase 15: Verifica su Head Unit Reale e Rilascio** - Build v2.1 verificata in auto, passata dal test chiuso e promossa al test aperto con `playstore/` deploy-ready

## Phase Details

### Phase 12: Spike Surface e Decisione Categoria/Template

**Goal**: L'utente sceglie, sulla base di misure reali osservate su DHU, quale percorso (POI + `MapWithContentTemplate` oppure NAVIGATION + `NavigationTemplate`) usare per disegnare la velocità sulla Surface, e l'app richiede Car API 7
**Depends on**: Phase 11 (v2.0 shippata)
**Requirements**: REL-01, REL-02
**Success Criteria** (what must be TRUE):

  1. Su DHU con il telefono fisico (OnePlus 8T) l'utente vede, per ciascuno dei due percorsi, un disegno minimale di prova sulla Surface (sfondo + testo fisso centrato) e cosa l'host sovrappone (action strip, card di contenuto, icona/titolo)
  2. Per entrambi i percorsi sono registrate le misure concrete di stable area e visible area su almeno una risoluzione DHU, insieme al Car API level riportato da DHU (e, se osservabile, dalla head unit reale)
  3. La scelta di categoria e template è una decisione esplicita dell'utente, registrata con motivazione in PROJECT.md Key Decisions, e il manifest contiene solo i permessi Car App del percorso scelto (mai `MAP_TEMPLATES` e `NAVIGATION_TEMPLATES` insieme)
  4. Il manifest dichiara `minCarApiLevel` 7 e non esiste alcun ramo di fallback a `PaneTemplate`; l'app si apre ancora regolarmente su DHU

**Plans:** 3/6 plans executed
Plans:
**Wave 1**

- [x] 12-01-PLAN.md — Flavor temporanei spikePoi/spikeNav, manifest disgiunti, minCarApiLevel 7, funzione pura di fit "888"
- [x] 12-03-PLAN.md — Script di misura DHU, ini 800x480/1280x720/1920x1080, runbook

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 12-02-PLAN.md — SpeedSurfaceRenderer (disegno di prova, log misure, api=), factory template per flavor, ramo Granted su Surface

**Wave 3** *(blocked on Wave 2 completion)*

- [ ] 12-04-PLAN.md — Sessione DHU sul telefono fisico (checkpoint) e 12-SPIKE-RESULTS.md con criterio D-12

**Wave 4** *(blocked on Wave 3 completion)*

- [ ] 12-05-PLAN.md — Gate decisione utente POI/NAVIGATION, registrazione in PROJECT.md Key Decisions

**Wave 5** *(blocked on Wave 4 completion)*

- [ ] 12-06-PLAN.md — Rimozione flavor, consolidamento sul percorso scelto, manifest finale, verifica DHU (SC4)

### Phase 13: Velocità a Tutto Schermo sulla Surface

**Goal**: Sul display Android Auto l'utente legge la velocità come un numero grande e centrato, disegnato direttamente sulla Surface con il template scelto in Fase 12, senza perdere nessuno degli stati e dei comportamenti della v2.0
**Depends on**: Phase 12
**Requirements**: AA-05, AA-06, AA-07, AA-08, AA-09, AA-10, AA-11, AA-12
**Success Criteria** (what must be TRUE):

  1. Su DHU (almeno 800×480, 1280×720 e 1920×1080) l'utente vede la velocità come numero grande centrato nell'area garantita libera dall'host, con "km/h" piccolo in basso a destra e nessuna icona app né titolo sopra il numero (nei limiti del template scelto in Fase 12)
  2. Guidando/simulando da 9 a 10 e da 99 a 100 km/h il numero non cambia dimensione né "salta" di posizione
  3. La velocità si aggiorna una volta al secondo per una sessione prolungata senza che l'host chiuda l'app per quota template esaurita, e dopo una ricreazione della Surface (es. cambio risoluzione/rotazione DHU, ritorno all'app) l'ultimo valore ricompare subito, mai uno schermo nero
  4. Passando l'auto/DHU da giorno a notte e viceversa lo schermo si ridisegna subito nel tema corrispondente, con alto contrasto in entrambe le modalità
  5. Senza fix GPS l'utente vede "Ricerca segnale..." sullo schermo auto, e l'intero flusso permesso della Fase 9 (richiesta automatica, negato, negato permanente, riprova/impostazioni solo a veicolo fermo) funziona su DHU come in v2.0

**Plans**: TBD
**UI hint**: yes

### Phase 14: Pulizie Lato Telefono

**Goal**: Il telefono si comporta esattamente come in v2.0 ma con un'unica fonte di verità per lo stato di ricarica e senza il lampo del tachimetro quando Android Auto è già connesso all'apertura
**Depends on**: Nessuna dipendenza tecnica (indipendente dal renderer); sequenziata dopo Phase 13 per non mescolare regressioni del telefono con regressioni del rendering auto
**Requirements**: CLEAN-01, CLEAN-02
**Success Criteria** (what must be TRUE):

  1. Al primo avvio con telefono in carica lo switch "Sempre acceso" risulta attivo, e a telefono non in carica risulta disattivo, esattamente come prima del consolidamento
  2. L'icona di ricarica compare, anima e si ferma su "piena" come in v1.1, e il codice non contiene più una seconda logica di rilevamento della ricarica separata da `deriveChargingState()`
  3. Aprendo a freddo o riprendendo l'app con Android Auto già connesso (DHU), il telefono mostra direttamente "Connesso ad Android Auto" senza far apparire, nemmeno per un istante, il numero del tachimetro
  4. Alla disconnessione di Android Auto il telefono torna al tachimetro e alla preferenza "Sempre acceso" salvata, senza regressioni su MAX, distanza e toggle

**Plans**: TBD

### Phase 15: Verifica su Head Unit Reale e Rilascio

**Goal**: La v2.1 arriva ai tester sul canale di test aperto solo dopo aver funzionato su una head unit reale e aver superato la revisione Android Auto sul canale di test chiuso
**Depends on**: Phase 13, Phase 14
**Requirements**: REL-03
**Success Criteria** (what must be TRUE):

  1. Su una head unit Android Auto reale, con la build di release, l'utente vede la velocità grande e centrata aggiornarsi durante la guida e "Ricerca segnale..." quando il fix manca (PASS visivo accettato, senza `adb logcat` con la USB occupata)
  2. La build v2.1 (nuovo versionCode) è caricata prima sul canale di test chiuso ed è promossa al test aperto solo dopo esito positivo della revisione Android Auto
  3. `playstore/` è allineato alla v2.1 (versione, note di rilascio in un unico file con tag `<it-IT>`/`<en-US>`, listing, dichiarazioni dei permessi Car App del percorso scelto) e resta deploy-ready

**Plans**: TBD

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 → 10 → 11 → 12 → 13 → 14 → 15

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1. Fondamenta, Permessi e Avvio | v1.0 | 2/2 | Complete | 2026-07-07 |
| 2. Motore GPS | v1.0 | 3/3 | Complete | 2026-07-07 |
| 3. Interfaccia Tachimetro | v1.0 | 1/1 | Complete | 2026-07-10 |
| 4. Velocità Massima e Persistenza | v1.0 | 2/2 | Complete | 2026-07-10 |
| 5. Gestione Schermo | v1.0 | 2/2 | Complete | 2026-07-10 |
| 6. Indicatore di Ricarica | v1.1 | 4/4 | Complete | 2026-08-29 |
| 7. Distanza Percorsa e Reset Unificato | v1.1 | 4/4 | Complete | 2026-08-30 |
| 8. Fondamenta Condivise e Velocità sullo Schermo Auto | v2.0 | 3/3 | Complete   | 2026-09-02 |
| 9. Permesso di Localizzazione dallo Schermo Auto | v2.0 | 3/3 | Complete   | 2026-09-02 |
| 10. Comportamento del Telefono alla Connessione Android Auto | v2.0 | 3/3 | Complete    | 2026-09-02 |
| 11. Hardening di Produzione e Verifica su Dispositivo Reale | v2.0 | 4/4 | Complete    | 2026-09-23 |
| 12. Spike Surface e Decisione Categoria/Template | v2.1 | 3/6 | In Progress|  |
| 13. Velocità a Tutto Schermo sulla Surface | v2.1 | 0/TBD | Not started | - |
| 14. Pulizie Lato Telefono | v2.1 | 0/TBD | Not started | - |
| 15. Verifica su Head Unit Reale e Rilascio | v2.1 | 0/TBD | Not started | - |
