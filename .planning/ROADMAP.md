# Roadmap: Tachimetro

## Overview

Tachimetro nasce da uno scaffold Android Studio vuoto e arriva a un'app completa: si parte dalle fondamenta (avvio diretto, permesso GPS), si costruisce il motore di lettura della velocità, poi l'interfaccia a schermo intero che la mostra, quindi le funzionalità di velocità massima con persistenza, e infine il controllo dello schermo sempre acceso. Con la milestone v1.1 si aggiungono due indicatori secondari indipendenti: uno stato di ricarica riconoscibile a colpo d'occhio (unica animazione e unico colore accento ammessi nell'interfaccia) e una distanza percorsa persistente, azzerabile nella stessa azione del record di velocità massima già esistente. Con la milestone v2.0 la velocità viene proiettata anche sullo schermo Android Auto dell'auto/moto: prima si condivide la fonte GPS tra telefono e auto senza duplicarla, poi si mostra la velocità (e lo stato di assenza di segnale) sul display auto con i template standard della Car App Library, si gestisce il permesso di localizzazione richiesto direttamente dallo schermo auto, si adatta il comportamento del telefono quando Android Auto è connesso, e infine si mette in sicurezza il tutto per l'uso reale su strada.

## Milestones

- ✅ **v1.0 MVP** — Fasi 1-5 (shipped 2026-07-10) → [archivio completo](milestones/v1.0-ROADMAP.md)
- ✅ **v1.1 Ricarica e distanza** — Fasi 6-7 (shipped 2026-08-30) → [archivio completo](milestones/v1.1-ROADMAP.md)
- ✅ **v2.0 Android Auto Support** — Fasi 8-11 (shipped 2026-09-23) → [archivio completo](milestones/v2.0-ROADMAP.md)

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
| 9. Permesso di Localizzazione dallo Schermo Auto | v2.0 | 3/3 | Complete   | 2026-09-02 |
| 10. Comportamento del Telefono alla Connessione Android Auto | v2.0 | 3/3 | Complete    | 2026-09-02 |
| 11. Hardening di Produzione e Verifica su Dispositivo Reale | v2.0 | 4/4 | Complete    | 2026-09-23 |
