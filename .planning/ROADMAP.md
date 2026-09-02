# Roadmap: Tachimetro

## Overview

Tachimetro nasce da uno scaffold Android Studio vuoto e arriva a un'app completa: si parte dalle fondamenta (avvio diretto, permesso GPS), si costruisce il motore di lettura della velocità, poi l'interfaccia a schermo intero che la mostra, quindi le funzionalità di velocità massima con persistenza, e infine il controllo dello schermo sempre acceso. Con la milestone v1.1 si aggiungono due indicatori secondari indipendenti: uno stato di ricarica riconoscibile a colpo d'occhio (unica animazione e unico colore accento ammessi nell'interfaccia) e una distanza percorsa persistente, azzerabile nella stessa azione del record di velocità massima già esistente. Con la milestone v2.0 la velocità viene proiettata anche sullo schermo Android Auto dell'auto/moto: prima si condivide la fonte GPS tra telefono e auto senza duplicarla, poi si mostra la velocità (e lo stato di assenza di segnale) sul display auto con i template standard della Car App Library, si gestisce il permesso di localizzazione richiesto direttamente dallo schermo auto, si adatta il comportamento del telefono quando Android Auto è connesso, e infine si mette in sicurezza il tutto per l'uso reale su strada.

## Milestones

- ✅ **v1.0 MVP** — Fasi 1-5 (shipped 2026-07-10) → [archivio completo](milestones/v1.0-ROADMAP.md)
- ✅ **v1.1 Ricarica e distanza** — Fasi 6-7 (shipped 2026-08-30) → [archivio completo](milestones/v1.1-ROADMAP.md)
- 🚧 **v2.0 Android Auto Support** — Fasi 8-11 (in progress)

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

- [x] **Phase 8: Fondamenta Condivise e Velocità sullo Schermo Auto** - La velocità e lo stato "nessun segnale" appaiono sullo schermo Android Auto, aggiornati al secondo, condividendo un'unica fonte GPS con il telefono (SC1 accettato con nuance — vedi dettagli sotto)
- [ ] **Phase 9: Permesso di Localizzazione dallo Schermo Auto** - Se il permesso non è ancora concesso, l'utente lo concede direttamente dallo schermo auto al primo collegamento
- [ ] **Phase 10: Comportamento del Telefono alla Connessione Android Auto** - Il telefono rilascia lo schermo sempre acceso e mostra uno stato neutro quando Android Auto è connesso, ripristinando tutto alla disconnessione
- [ ] **Phase 11: Hardening di Produzione e Verifica su Dispositivo Reale** - L'integrazione Android Auto è validata con un host reale e verificata su strada a telefono bloccato

## Phase Details

### Phase 8: Fondamenta Condivise e Velocità sullo Schermo Auto

**Goal**: La velocità corrente e lo stato "nessun segnale" vengono mostrati sullo schermo Android Auto, aggiornati alla stessa cadenza del telefono (1/sec), condividendo un'unica fonte GPS con il telefono (nessuna sottoscrizione duplicata, nessuna regressione visibile sul telefono).
**Depends on**: Phase 7 (v1.1 — GpsSpeedProvider e MainActivity esistenti da cui parte il refactor Application-scoped)
**Requirements**: AA-01, AA-02, AA-03
**Status**: Complete (2026-09-02) — verificato con sessione DHU dal vivo su telefono fisico; vedi nuance SC1/SC2 sotto e `08-CONTEXT.md` D-11..D-14 per il record canonico delle decisioni
**Success Criteria** (what must be TRUE):

  1. ~~Connettendo il telefono ad Android Auto (o al Desktop Head Unit), lo schermo auto mostra la velocità attuale come testo grande e leggibile, coerente con il valore mostrato sul telefono~~ — **Accettato con limitazione nota**: osservato FALLIRE come letteralmente formulato (il `PaneTemplate` rende il numero piccolo, in alto a sinistra, con l'icona app anch'essa forzata in alto a sinistra — limite strutturale dell'API, non un bug). Formalmente accettato per v2.0 via decisione esplicita (D-13): `AA-01` resta soddisfatto nell'accezione "stile/tipografia gestiti dall'host" già scritta in REQUIREMENTS.md, non "grande come sul telefono". Alternativa (`NavigationTemplate`+`SurfaceCallback`) rimandata a milestone v2.1 dedicata (D-14). Vedi `08-CONTEXT.md` D-12/D-13/D-14.
  2. Quando il segnale GPS manca, lo schermo auto mostra uno stato equivalente a "Ricerca segnale GPS..." invece di restare bloccato su un valore vecchio — **Accettato senza verifica live**: nessuna sessione DHU ha esercitato una perdita di segnale reale; accettato su istruzione esplicita dell'utente, coperto solo indirettamente da test unitari esistenti (`CarSpeedContentTest`, `GpsSpeedProviderStateTest`). Vedi `08-CONTEXT.md` D-11.
  3. Il valore sullo schermo auto si aggiorna una volta al secondo, alla stessa cadenza del telefono, senza salti né disallineamenti tra i due schermi — **Confermato**: 586 refresh in 608s, cadenza media 0.964/s, gap massimo osservato 3.1s (sessione DHU dal vivo, D-11)
  4. Durante una sessione continua di alcuni minuti a cadenza 1Hz, l'host Android Auto non chiude l'app per superamento della quota di refresh dei template (verifica empirica preventiva del rischio quota, DHU + Developer Mode) — **Confermato PASS**: PID mai cambiato/sparito per l'intera sessione, host non ha mai chiuso l'app (euristica script + conferma visiva utente)
  5. Il comportamento e l'aspetto del telefono restano invariati rispetto alla v1.1 (nessuna regressione visibile), a conferma che il GPS è condiviso da un'unica sottoscrizione Application-scoped tra telefono e auto — **Confermato** dall'utente durante e dopo la sessione DHU

**Plans**: 3 plans

Plans:
**Wave 1**

- [x] 08-01-PLAN.md — Fondamenta condivise: GpsSpeedProvider Application-scoped, dipendenza Car App Library 1.7.0, stringa "Ricerca segnale..." e contratto di contenuto puro (wave 1)

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 08-02-PLAN.md — Servizio Car App categoria POI, Session e SpeedScreen con PaneTemplate aggiornato a 1 Hz (wave 2)

**Wave 3** *(blocked on Wave 2 completion)*

- [x] 08-03-PLAN.md — Gate SC4: test strumentato del template, automazione della misura refresh su DHU e checkpoint umano di verifica quota (wave 3) — completo 2026-09-02, vedi nuance SC1/SC2 sopra

### Phase 9: Permesso di Localizzazione dallo Schermo Auto

**Goal**: Se il permesso di localizzazione non è ancora stato concesso, l'utente può concederlo direttamente dallo schermo Android Auto al primo collegamento, senza dover prima aprire l'app sul telefono.
**Depends on**: Phase 8 (lo schermo auto deve già esistere e mostrare dati prima di gestire il caso "permesso non ancora concesso")
**Requirements**: AA-04
**Success Criteria** (what must be TRUE):

  1. Collegando Android Auto per la prima volta senza aver mai concesso il permesso di localizzazione sul telefono, lo schermo auto mostra una richiesta di permesso esplicita (`CarContext.requestPermissions()`) invece di restare vuoto o bloccato
  2. Concedendo il permesso dalla richiesta mostrata sullo schermo auto, lo schermo passa automaticamente alla velocità (o allo stato "Ricerca segnale") senza richiedere il riavvio dell'app o del collegamento
  3. Se l'utente nega il permesso dallo schermo auto, viene mostrato un messaggio chiaro che spiega l'impossibilità di leggere la velocità, invece di uno schermo vuoto

**Plans**: 3 plans

Plans:
**Wave 1**

- [ ] 09-01-PLAN.md — Fondamenta pure: modello `CarPermissionState`, resolver del rifiuto permanente, contatore persistito dei rifiuti e stringhe italiane dedicate all'auto (wave 1)

**Wave 2** *(blocked on Wave 1 completion)*

- [ ] 09-02-PLAN.md — `SpeedScreen`: richiesta automatica via `CarContext.requestPermissions()`, transizione reattiva alla concessione, Action Riprova/Apri impostazioni nel `PaneTemplate` (wave 2)

**Wave 3** *(blocked on Wave 2 completion)*

- [ ] 09-03-PLAN.md — Verifica: test strumentato della forma del template per ogni stato del permesso e checkpoint umano della sessione DHU sul flusso completo (wave 3)

### Phase 10: Comportamento del Telefono alla Connessione Android Auto

**Goal**: Quando Android Auto si connette, il telefono passa a uno stato neutro coerente e rilascia il controllo dello schermo sempre acceso; alla disconnessione, ripristina esattamente il comportamento precedente, senza reset indesiderati.
**Depends on**: Phase 5 (v1.0 — estende `ScreenOnPreferenceStore` esistente; indipendente dal lavoro sullo schermo auto delle Fasi 8-9, sequenziata qui per coerenza della milestone)
**Requirements**: CONN-01, CONN-02
**Success Criteria** (what must be TRUE):

  1. Quando Android Auto si connette, il telefono rilascia "schermo sempre acceso" (se era attivo) e mostra uno stato neutro "Connesso ad Android Auto" al posto della velocità
  2. Alla disconnessione di Android Auto, il telefono ripristina esattamente la preferenza "sempre acceso" salvata in precedenza (attiva se era attiva, automatica se era automatica), senza alterare la preferenza memorizzata
  3. Il toggle "Schermo sempre acceso" esistente continua a funzionare normalmente quando Android Auto non è connesso, senza regressioni rispetto al comportamento v1.0/v1.1

**Plans**: TBD

### Phase 11: Hardening di Produzione e Verifica su Dispositivo Reale

**Goal**: L'integrazione Android Auto è pronta per l'uso reale: l'host della connessione viene validato correttamente (non più permissivo per default) e il comportamento a schermo bloccato/in background durante una connessione attiva è verificato su un dispositivo reale, non solo in emulazione.
**Depends on**: Phase 9, Phase 10
**Requirements**: Nessun nuovo requisito — verifica e messa in sicurezza di AA-01, AA-02, AA-03, AA-04, CONN-01, CONN-02
**Success Criteria** (what must be TRUE):

  1. L'app usa un `HostValidator` reale (non più `ALLOW_ALL_HOSTS_VALIDATOR`) che accetta solo host Android Auto legittimi, verificato che il collegamento a un head unit reale/DHU continui a funzionare
  2. Su un dispositivo reale, con il telefono bloccato/in background e Android Auto connesso, la velocità sullo schermo auto continua ad aggiornarsi per diversi minuti consecutivi durante un tragitto reale (o il limite di piattaforma viene documentato esplicitamente se non risolvibile)
  3. Connettendo e disconnettendo Android Auto ripetutamente in rapida successione, l'app non va in crash e lo schermo auto non resta bloccato in uno stato incoerente

**Plans**: TBD

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 → 10 → 11

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
| 9. Permesso di Localizzazione dallo Schermo Auto | v2.0 | 0/3 | Planned | - |
| 10. Comportamento del Telefono alla Connessione Android Auto | v2.0 | 0/TBD | Not started | - |
| 11. Hardening di Produzione e Verifica su Dispositivo Reale | v2.0 | 0/TBD | Not started | - |
