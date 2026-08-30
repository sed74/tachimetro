# Roadmap: Tachimetro

## Overview

Tachimetro nasce da uno scaffold Android Studio vuoto e arriva a un'app completa: si parte dalle fondamenta (avvio diretto, permesso GPS), si costruisce il motore di lettura della velocità, poi l'interfaccia a schermo intero che la mostra, quindi le funzionalità di velocità massima con persistenza, e infine il controllo dello schermo sempre acceso. Con la milestone v1.1 si aggiungono due indicatori secondari indipendenti: uno stato di ricarica riconoscibile a colpo d'occhio (unica animazione e unico colore accento ammessi nell'interfaccia) e una distanza percorsa persistente, azzerabile nella stessa azione del record di velocità massima già esistente.

## Milestones

- ✅ **v1.0 MVP** — Fasi 1-5 (shipped 2026-07-10) → [archivio completo](milestones/v1.0-ROADMAP.md)
- 🚧 **v1.1 Ricarica e distanza** — Fasi 6-7 (in progress)

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

- [x] **Phase 6: Indicatore di Ricarica** - L'utente riconosce a colpo d'occhio quando il telefono è in carica tramite un'icona a fulmine animata (completed 2026-08-29)
- [x] **Phase 7: Distanza Percorsa e Reset Unificato** - L'utente monitora la distanza percorsa dall'ultimo azzeramento e la azzera insieme al massimo con un unico pulsante (completed 2026-08-30)

## Phase Details

### Phase 6: Indicatore di Ricarica

**Goal**: L'utente riconosce immediatamente quando il telefono è in carica, tramite un'icona a fulmine animata posizionata accanto al toggle "sempre acceso".
**Depends on**: Phase 5 (riusa la logica `isDeviceCharging()` e condivide l'area accanto a `keepScreenOnSwitch`)
**Requirements**: CHRG-01, CHRG-02
**Success Criteria** (what must be TRUE):

  1. Quando il telefono viene collegato alla ricarica, l'icona a fulmine appare accanto al toggle "sempre acceso"
  2. Quando il telefono viene scollegato dalla ricarica, l'icona a fulmine scompare immediatamente
  3. Mentre il telefono è in carica, l'icona anima un riempimento continuo dal basso verso l'alto, bianco → lime → bianco, in loop di circa 2-3 secondi, per tutta la durata della ricarica
  4. Nessun'altra icona, colore o animazione compare altrove nell'interfaccia al di fuori di questo indicatore

**Plans**: 4 plans

Plans:
**Wave 1**

- [x] 06-01-PLAN.md — Risorse: colore lime, stringa accessibilità, drawable del fulmine riempibile, ImageView nel layout
- [x] 06-02-PLAN.md — Dominio charging: ChargingState, deriveChargingState (test-first), ChargingStateProvider reattivo

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 06-03-PLAN.md — Wiring MainActivity: animazione di riempimento, collettore lifecycle-aware, window insets bottom-left

**Wave 3** *(blocked on Wave 2 completion)*

- [x] 06-04-PLAN.md — Verifica visiva su dispositivo (checkpoint umano)

**UI hint**: yes

### Phase 7: Distanza Percorsa e Reset Unificato

**Goal**: L'utente monitora la distanza percorsa dall'ultimo azzeramento, calcolata solo mentre l'app è in foreground e persistente su disco, e può azzerarla nella stessa azione con cui azzera la velocità massima.
**Depends on**: Phase 4 (estende il pattern `MaxSpeedStore`/`resetMaxButton` per la nuova metrica)
**Requirements**: DIST-01, DIST-02, DIST-03, MAX-04
**Success Criteria** (what must be TRUE):

  1. L'utente vede un'area in basso a destra con la distanza percorsa dall'ultimo azzeramento, in un font più grande dell'area velocità massima
  2. La distanza aumenta in tempo reale mentre l'app è in foreground e riceve letture GPS valide
  3. Mettendo l'app in background o perdendo il segnale GPS, la distanza smette di accumularsi (nessun tracking in background)
  4. Chiudendo e riaprendo l'app, o riavviando il telefono, la distanza precedentemente accumulata è ancora visibile
  5. Premendo il pulsante "Azzera massimo", sia la velocità massima sia la distanza percorsa si azzerano nella stessa azione

**Plans**: 4 plans

Plans:
**Wave 1** *(paralleli, nessun file in comune)*

- [x] 07-01-PLAN.md — Dominio distanza: reduceDistance/sanitizePersistedDistance, DistanceStore, formatDistanceDisplay (test-first)
- [x] 07-02-PLAN.md — Pipeline GPS: SpeedState.Reading porta deltaMeters calcolato con Location.distanceTo()

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 07-03-PLAN.md — Risorse, layout bottom-right e wiring MainActivity: accumulo, rendering adattivo, insets, reset unificato "Azzera"

**Wave 3** *(blocked on Wave 2 completion)*

- [x] 07-04-PLAN.md — Verifica su strada su dispositivo reale (checkpoint umano)

**UI hint**: yes

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4 → 5 → 6 → 7

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1. Fondamenta, Permessi e Avvio | v1.0 | 2/2 | Complete | 2026-07-07 |
| 2. Motore GPS | v1.0 | 3/3 | Complete | 2026-07-07 |
| 3. Interfaccia Tachimetro | v1.0 | 1/1 | Complete | 2026-07-10 |
| 4. Velocità Massima e Persistenza | v1.0 | 2/2 | Complete | 2026-07-10 |
| 5. Gestione Schermo | v1.0 | 2/2 | Complete | 2026-07-10 |
| 6. Indicatore di Ricarica | v1.1 | 4/4 | Complete   | 2026-08-29 |
| 7. Distanza Percorsa e Reset Unificato | v1.1 | 4/4 | Complete   | 2026-08-30 |
