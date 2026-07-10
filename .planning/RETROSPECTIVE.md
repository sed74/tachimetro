# Project Retrospective

*A living document updated after each milestone. Lessons feed forward into future planning.*

## Milestone: v1.0 — MVP

**Shipped:** 2026-07-10
**Phases:** 5 | **Plans:** 10 | **Timeline:** 4 giorni (2026-07-07 → 2026-07-10)

### What Was Built
- Motore GPS (`FusedLocationProviderClient` → `callbackFlow`/`StateFlow`) con filtro accuratezza, soglia rumore e timeout segnale
- Interfaccia tachimetro a schermo intero: numero auto-size dominante, sfondo nero alto contrasto, layout unico adattivo portrait/landscape, fullscreen immersivo
- Velocità massima persistente (`MaxSpeedReducer` funzioni pure TDD + `MaxSpeedStore` SharedPreferences), resistente a chiusura app e riavvio telefono
- Toggle "Schermo sempre acceso" monocromatico con default derivato dallo stato di ricarica e preferenza persistente
- Flusso permesso `ACCESS_FINE_LOCATION` completo (concessione/rifiuto/rifiuto permanente)

### What Worked
- **Struttura roadmap a strati orizzontali** (Fondamenta → GPS → UI → Max → Schermo): ogni fase produceva un incremento tecnico completo e verificabile eseguendo l'app
- **Checkpoint umani su emulatore** per ogni fase con superficie visiva/hardware: hanno intercettato problemi reali (window insets edge-to-edge su targetSdk 36, autosize dei messaggi di stato) impossibili da cogliere solo con test JVM
- **Logica pura isolata dal framework** (`SpeedMapping`, `MaxSpeedReducer`): testabile in TDD su JVM senza device/emulatore
- **Verifica persistenza con `adb reboot`**: ha dato confidenza reale sulle SharedPreferences oltre la semplice chiusura app

### What Was Inefficient
- **Fase 3 UI ha richiesto 4 round di fix da checkpoint** (cap autosize, unitText separata, window insets, fullscreen immersivo): parte scopribile solo a schermo, ma un UI-SPEC più dettagliato a monte avrebbe potuto anticipare gli insets edge-to-edge
- **Window insets come debito trasversale scoperto tardi** (Fase 3): ha vincolato ogni elemento ai bordi nelle fasi successive (MAX in Fase 4, toggle in Fase 5); meritava una decisione architetturale esplicita prima di posizionare elementi ai margini

### Patterns Established
- **Package per feature** sotto `com.sed.tachimetro` (`gps/`, `maxspeed/`, `screen/`) — la prima feature ha stabilito la convenzione
- **Store SharedPreferences dedicato per preferenza** (`MaxSpeedStore`, `ScreenOnPreferenceStore`) con reducer/funzioni pure separate e testate
- **Gestione esplicita dei window insets** per ogni elemento ancorato ai bordi (pattern nato in Fase 3, riusato in Fasi 4-5)
- Kotlin abilitato via **supporto built-in AGP 9.1.1** (non il plugin classico `org.jetbrains.kotlin.android`, incompatibile con questa AGP)

### Key Lessons
1. Le fasi con superficie visiva/hardware vanno sempre chiuse con un checkpoint umano su device: i test JVM non coprono insets, autosize, spegnimento schermo o stato di ricarica.
2. Decisioni architetturali trasversali (come la gestione dei window insets in edge-to-edge) vanno prese e documentate appena emergono, perché vincolano tutte le fasi successive.
3. Isolare la logica pura dal framework Android ripaga: `SpeedMapping` e `MaxSpeedReducer` sono stati sviluppati in TDD senza toccare l'emulatore.

### Cost Observations
- Model mix: prevalentemente opus/sonnet (profilo `balanced`)
- Notable: granularità `coarse` (5 fasi per l'intero MVP) ha tenuto basso l'overhead di planning mantenendo comunque incrementi verificabili

---

## Cross-Milestone Trends

### Process Evolution

| Milestone | Phases | Key Change |
|-----------|--------|------------|
| v1.0 | 5 | Baseline: roadmap a strati orizzontali, checkpoint umani su device per fasi UI/hardware |

### Cumulative Quality

| Milestone | Test Suites (JVM) | Zero-Dep Additions |
|-----------|-------------------|--------------------|
| v1.0 | 3 (SpeedMapping, GpsProviderState, MaxSpeedReducer) | play-services-location, kotlinx-coroutines, lifecycle-runtime-ktx, ConstraintLayout |

### Top Lessons (Verified Across Milestones)

1. (da confermare in milestone successive) Checkpoint umano su device indispensabile per fasi con superficie visiva/hardware.
