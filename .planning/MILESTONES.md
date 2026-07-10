# Milestones

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
