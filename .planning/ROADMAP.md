# Roadmap: Tachimetro

## Overview

Tachimetro nasce da uno scaffold Android Studio vuoto e arriva a un'app completa: si parte dalle fondamenta (avvio diretto, permesso GPS), si costruisce il motore di lettura della velocità, poi l'interfaccia a schermo intero che la mostra, quindi le funzionalità di velocità massima con persistenza, e infine il controllo dello schermo sempre acceso. Ogni fase aggiunge uno strato tecnico completo e verificabile eseguendo l'app.

## Phases

**Phase Numbering:**

- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [x] **Phase 1: Fondamenta, Permessi e Avvio** - L'app si apre direttamente sulla schermata principale e gestisce correttamente il permesso GPS (completed 2026-07-07)
- [x] **Phase 2: Motore GPS** - L'app legge la velocità reale dal dispositivo tramite FusedLocationProviderClient (completed 2026-07-07)
- [x] **Phase 3: Interfaccia Tachimetro** - L'utente vede la velocità a schermo intero, leggibile in ogni orientamento e condizione di luce (completed 2026-07-10)
- [x] **Phase 4: Velocità Massima e Persistenza** - L'utente monitora e azzera la velocità massima, con valore persistente su disco (completed 2026-07-10)
- [ ] **Phase 5: Gestione Schermo** - L'utente controlla se lo schermo resta sempre acceso durante l'uso

## Phase Details

### Phase 1: Fondamenta, Permessi e Avvio

**Goal**: L'app si avvia direttamente sulla schermata principale (nessun menu iniziale) e gestisce correttamente la richiesta del permesso ACCESS_FINE_LOCATION, incluso il caso di rifiuto.
**Depends on**: Nothing (first phase)
**Requirements**: APP-01, PERM-01, PERM-02
**Success Criteria** (what must be TRUE):

  1. L'app si apre direttamente sulla schermata della velocità, senza schermate iniziali o menu
  2. Al primo avvio, l'app richiede all'utente il permesso ACCESS_FINE_LOCATION
  3. Se l'utente nega il permesso, vede un messaggio appropriato che spiega l'impossibilità di leggere il GPS
  4. Se l'utente concede il permesso, l'app procede senza ulteriori richieste non necessarie

**Plans**: 2 plans
Plans:
**Wave 1**

- [x] 01-01-PLAN.md — Abilitazione Kotlin + dipendenza ConstraintLayout nella build (version catalog + app/build.gradle.kts)

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 01-02-PLAN.md — MainActivity LAUNCHER + flusso permesso ACCESS_FINE_LOCATION (manifest, stringhe italiane, layout placeholder)

### Phase 2: Motore GPS

**Goal**: L'app legge la velocità corrente del dispositivo tramite GPS in modo affidabile ed efficiente.
**Depends on**: Phase 1
**Requirements**: GPS-01, GPS-02, GPS-03
**Success Criteria** (what must be TRUE):

  1. L'app legge la velocità attuale in km/h tramite FusedLocationProviderClient (Google Play Services)
  2. Il valore di velocità disponibile si aggiorna una volta al secondo
  3. Quando il GPS non ha ancora un segnale valido, è disponibile un messaggio di stato (es. "Ricerca segnale GPS...") anziché un valore numerico errato

**Plans**: 3 plans
Plans:
**Wave 1**

- [x] 02-01-PLAN.md — Dipendenze GPS via version catalog (play-services-location, kotlinx-coroutines-core, lifecycle-runtime-ktx) + gate compatibilità build

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 02-02-PLAN.md — Motore GPS: SpeedState + funzione pura mapSpeedToKmh (test) + GpsSpeedProvider (callbackFlow → StateFlow<SpeedState>)

**Wave 3** *(blocked on Wave 2 completion)*

- [x] 02-03-PLAN.md — Wiring MainActivity (collector lifecycle-scoped, placeholder "N km/h" / "Ricerca segnale GPS...") + checkpoint Route Playback emulatore

### Phase 3: Interfaccia Tachimetro

**Goal**: L'utente vede la velocità corrente a schermo intero, come elemento dominante, leggibile a colpo d'occhio in ogni orientamento e condizione di luce, interamente in italiano.
**Depends on**: Phase 2
**Requirements**: UI-01, UI-02, UI-03, UI-04, UI-05
**Success Criteria** (what must be TRUE):

  1. Il numero della velocità è l'elemento dominante dello schermo, sempre centrato e il più grande possibile
  2. Lo sfondo è nero e il testo è ad alto contrasto in stile Bold/Black del font di sistema
  3. Ruotando il dispositivo tra portrait e landscape, il layout si adatta mantenendo il numero centrato e massimizzato
  4. Non sono presenti menu, animazioni o elementi grafici non necessari
  5. Tutti i testi e i messaggi visibili nell'app sono in italiano

**Plans**: 1 plan
Plans:
**Wave 1**

- [x] 03-01-PLAN.md — Auto-size dominante di messageText in activity_main.xml (0dp box, uniform 12/300/4sp, centratura, font weight 900) + checkpoint verifica visiva portrait/landscape × 1/2/3 cifre
**UI hint**: yes

### Phase 4: Velocità Massima e Persistenza

**Goal**: L'utente può monitorare la velocità massima raggiunta dall'ultimo azzeramento e il valore sopravvive alla chiusura dell'app e al riavvio del telefono.
**Depends on**: Phase 3
**Requirements**: MAX-01, MAX-02, MAX-03
**Success Criteria** (what must be TRUE):

  1. L'utente vede la velocità massima raggiunta dall'ultimo azzeramento in un'area secondaria dello schermo
  2. Premendo il pulsante "Azzera massimo", il valore si azzera e inizia una nuova misurazione
  3. Chiudendo e riaprendo l'app, o riavviando il telefono, la velocità massima precedentemente registrata è ancora visibile

**Plans**: 2 plans
Plans:
**Wave 1**

- [x] 04-01-PLAN.md — Stringhe + layout (maxSpeedText/resetMaxButton), logica pura reduceMax/sanitize (test JVM) + MaxSpeedStore (SharedPreferences), wiring MainActivity (lettura all'avvio, aggiornamento/persistenza D-07/D-08, reset, insets, visibilità D-09)

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 04-02-PLAN.md — Checkpoint verifica su device: display MAX, non-decrescita, azzeramento immediato, persistenza dopo chiusura app E riavvio telefono, insets portrait/landscape
**UI hint**: yes

### Phase 5: Gestione Schermo

**Goal**: L'utente può scegliere se mantenere lo schermo sempre acceso durante l'uso dell'app, con la preferenza salvata tra sessioni.
**Depends on**: Phase 3
**Requirements**: SCRN-01, SCRN-02, SCRN-03
**Success Criteria** (what must be TRUE):

  1. L'utente vede un toggle per scegliere tra "Schermo sempre acceso" e "Schermo automatico"
  2. Con "Schermo sempre acceso" attivo, lo schermo non si spegne mentre l'app è in uso
  3. Chiudendo e riaprendo l'app, la preferenza scelta in precedenza è ancora impostata

**Plans**: 2 plans
Plans:
**Wave 1**

- [ ] 05-01-PLAN.md — Stringa + 2 ColorStateList grayscale + ScreenOnPreferenceStore (SharedPreferences Boolean nullable), layout keepScreenOnSwitch (SwitchCompat bottom-left sempre visibile), wiring MainActivity (default da ricarica al primo avvio D-04/D-05, FLAG_KEEP_SCREEN_ON immediato D-06, persistenza D-07, insets bottom+left)

**Wave 2** *(blocked on Wave 1 completion)*

- [ ] 05-02-PLAN.md — Checkpoint verifica su device: switch sempre visibile in ogni stato, blocco spegnimento immediato/reversibile, persistenza chiusura app E riavvio telefono, default da ricarica al primo avvio, insets portrait/landscape, no overlap con Riprova
**UI hint**: yes

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4 → 5

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Fondamenta, Permessi e Avvio | 2/2 | Complete    | 2026-07-07 |
| 2. Motore GPS | 3/3 | Complete    | 2026-07-07 |
| 3. Interfaccia Tachimetro | 1/1 | Complete   | 2026-07-10 |
| 4. Velocità Massima e Persistenza | 2/2 | Complete   | 2026-07-10 |
| 5. Gestione Schermo | 0/2 | Not started | - |
