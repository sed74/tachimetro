# Milestones

## v2.0 Android Auto Support (Shipped: 2026-09-23)

**Phases completed:** 4 phases, 13 plans, 25 tasks

**Key accomplishments:**

- CarPermissionState sealed model + resolveCarPermissionState() pure resolver (denialCount >= 2 = permanent) + CarPermissionDenialStore persisting the car-screen denial counter in the shared tachimetro_prefs file, plus three Italian car-screen strings for D-01/D-02/D-04.
- SpeedScreen ora richiede automaticamente ACCESS_FINE_LOCATION al primo collegamento Android Auto (CarContext.requestPermissions()), transita reattivamente a Granted senza riavvii (SC2), e rende tutti e quattro gli stati di CarPermissionState nel PaneTemplate con un'Action di retry/impostazioni eseguibile solo a veicolo fermo.
- SpeedScreenTemplateTest esteso a tutti e quattro gli stati di CarPermissionState (6 nuovi/aggiornati test) più chiusura del gate umano di fase: sessione DHU dal vivo conferma i tre Success Criteria di roadmap della Fase 9, chiude empiricamente Pitfall 4 (transizione Row-sola↔Row+Action), e l'utente accetta esplicitamente il limite noto di Pitfall 1 (Scenario G) per v2.0.
- Test di sequenza JVM che locka l'assenza di deriva su connessioni/disconnessioni ripetute, seguito da sessione DHU dal vivo con conferma punto per punto di A1-G1 (SC1/SC2/SC3 di roadmap).

---

## v1.1 Ricarica e distanza (Shipped: 2026-08-30)

**Phases completed:** 2 phases, 8 plans, 18 tasks

**Key accomplishments:**

- Verifica manuale su dispositivo reale approvata su tutti gli 8 punti della checklist; CHRG-01 e CHRG-02 confermati funzionanti come da 06-UI-SPEC.md, con 2 richieste di rifinitura estetica rinviate a un quick task successivo

---

## v1.0 MVP (Shipped: 2026-07-10)

**Phases completed:** 5 phases, 10 plans, 20 tasks
**Timeline:** 2026-07-07 → 2026-07-10 (4 giorni)
**Codebase:** ~695 LOC Kotlin (8 file `.kt`), 3 suite di test JVM
**Requirements:** 17/17 validati con checkpoint umani su device

**Delivered:** App Android nativa che mostra la velocità GPS in tempo reale a schermo intero, con interfaccia minimale ad altissimo contrasto, velocità massima persistente e controllo dello schermo sempre acceso.

**Key accomplishments:**

- **Fondamenta & permessi** (Fase 1) — App LAUNCHER diretta sulla schermata velocità, flusso completo permesso `ACCESS_FINE_LOCATION` (concessione/rifiuto/rifiuto permanente); Kotlin abilitato via supporto built-in AGP 9.1.1
- **Motore GPS** (Fase 2) — Lettura velocità via `FusedLocationProviderClient` con `callbackFlow`/`StateFlow`, filtro accuratezza ~50m, soglia rumore ~2 km/h, timeout segnale 5s, aggiornamento 1/sec
- **Interfaccia tachimetro** (Fase 3) — Numero auto-size dominante (12-300sp), sfondo nero alto contrasto, layout unico adattivo portrait/landscape, fullscreen immersivo, tutti i testi in italiano
- **Velocità massima persistente** (Fase 4) — `MaxSpeedReducer` (funzioni pure TDD) + `MaxSpeedStore` (SharedPreferences); sopravvive a chiusura app e riavvio telefono (verificato con `adb reboot`)
- **Gestione schermo** (Fase 5) — Toggle "Sempre acceso" monocromatico con default derivato dallo stato di ricarica, `FLAG_KEEP_SCREEN_ON` immediato, preferenza persistente tra sessioni

---
