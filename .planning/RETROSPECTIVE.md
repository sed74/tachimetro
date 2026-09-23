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

## Milestone: v1.1 — Ricarica e distanza

**Shipped:** 2026-08-30
**Phases:** 2 | **Plans:** 8 | **Timeline:** 2 giorni (2026-08-29 → 2026-08-30)

### What Was Built
- Indicatore di ricarica: `ChargingStateProvider` (rilevamento continuo via `BroadcastReceiver` su `ACTION_BATTERY_CHANGED`), `ChargingState` sealed (Hidden/Pulsing/Full), animazione di riempimento lime via `ValueAnimator`+`ClipDrawable` — prima animazione e primo colore accento del progetto (deroga esplicita)
- Distanza percorsa: `DistanceReducer`/`DistanceFormat`/`DistanceStore` (funzioni pure TDD), `GpsSpeedProvider` esteso con `deltaMeters` per fix accettato via `Location.distanceTo()`, formato adattivo metri/km con virgola decimale italiana
- Reset unificato: il pulsante "Azzera massimo" diventa "Azzera" e azzera velocità massima e distanza in un solo tocco

### What Worked
- **Riuso di pattern consolidati in v1.0**: `DistanceStore` è un mirror quasi esatto di `MaxSpeedStore`, `deltaMeters` viaggia sulla stessa pipeline accepted-fix già usata per `kmh` in `GpsSpeedProvider` — ha compresso 2 fasi complete in 2 giorni (contro i 4 giorni dell'intero MVP v1.0, che stabiliva i pattern da zero)
- **Esecuzione parallela via worktree isolation** su Wave 1 di Fase 7 (07-01 dominio distanza + 07-02 pipeline GPS, nessun file in comune) — due executor in background contemporaneamente
- **TDD rigoroso** con ciclo RED→GREEN esplicito per `reduceDistance`/`formatDistanceDisplay`
- **Checkpoint umano su strada** (Fase 7) invece che solo emulatore — ha permesso di verificare il comportamento GPS reale, incluso un falso allarme diagnosticato correttamente durante l'attesa (un singolo punto GPS impostato manualmente su emulatore, senza route playback, non porta un valore di `speed` valido: comportamento atteso del gate soglia rumore, non un bug)
- **Audit di sicurezza retroattivo evidence-based** (Fase 7) — verifica con grep/lettura reale del codice invece di fidarsi delle dichiarazioni di piano, 22/22 minacce chiuse con prove concrete

### What Was Inefficient
- Il `SECURITY.md` generato dall'agente auditor non seguiva il template canonico (mancava frontmatter `threats_open`, Accepted Risks Log, Security Audit Trail, Sign-Off) — ha richiesto una riscrittura manuale dell'orchestratore per allinearlo al contratto atteso dagli altri comandi GSD
- Diversi `SUMMARY.md` non avevano un campo `one_liner` parsabile per l'estrazione automatica degli accomplishment a fine milestone
- Modifiche non committate preesistenti nella working tree (bump AGP, `HANDOFF.json` consumato) sono rimaste per giorni prima di essere ripulite in una sessione successiva
- L'audit pre-chiusura milestone (`audit-open`) ha segnalato 4 falsi positivi sui quick task: nessuna colonna "Status" nella tabella `STATE.md` quando i quick task non girano con `--validate`, quindi lo scanner non riesce a determinarne lo stato pur essendo tutti completi e committati

### Patterns Established
- **Mirror pattern per nuove metriche persistite**: nuovo store che rispecchia uno store esistente validato (stesso `PREFS_NAME`, stessa firma read/write) invece di progettarne uno da zero
- **Estensione della pipeline esistente invece di duplicarla**: nuovo dato derivato (`deltaMeters`) aggiunto alla stessa pipeline accepted-fix già filtrata per accuratezza/rumore, mai una pipeline parallela
- **Costante condivisa tra domini correlati**: `GpsSpeedProvider.NOISE_FLOOR_KMH` ora unica fonte di verità per il filtro velocità e il gate di accumulo distanza (introdotta come fix post-review — da applicare fin dall'inizio in futuro)

### Key Lessons
1. Riusare pattern consolidati da una milestone precedente comprime drasticamente i tempi di sviluppo — la milestone che stabilisce i pattern costa di più della milestone che li riusa.
2. Un checkpoint umano su dispositivo reale **in movimento** (non solo emulatore) è indispensabile per feature che dipendono da velocità/accelerazione GPS reali: il mock location dell'emulatore non genera un valore di `speed` valido senza route playback esplicito.
3. Verificare template e frontmatter attesi PRIMA che un agente scriva un artefatto di governance (SECURITY.md, REVIEW.md) evita una riscrittura manuale a valle.

### Cost Observations
- Model mix: planner opus, executor/verifier/reviewer/auditor sonnet (profilo `balanced`)
- Sessions: 1 sessione continua per l'intera milestone (execute Fase 7 → code review → fix → security audit → complete-milestone)
- Notable: l'esecuzione in background degli agenti (worktree isolation, checkpoint su strada) ha permesso di gestire altre richieste dell'utente nel frattempo, senza bloccare la sessione

---

## Milestone: v2.0 — Android Auto Support

**Shipped:** 2026-09-23
**Phases:** 4 | **Plans:** 13

### What Was Built
- Fonte GPS unica Application-scoped (`TachimetroApplication`) condivisa tra telefono e auto; `TachimetroCarAppService`/`Session`/`SpeedScreen` su `PaneTemplate` aggiornato a 1Hz (Fase 8)
- Richiesta del permesso di localizzazione direttamente dallo schermo auto, macchina a stati reattiva con distinzione rifiuto singolo/permanente (Fase 9)
- Telefono che rilascia lo schermo sempre acceso e mostra uno stato neutro con Android Auto connesso, ripristino alla disconnessione (Fase 10)
- `HostValidator` reale con allow-list ufficiale in release, runbook di verifica su hardware reale (Fase 11)

### What Worked
- Verifiche empiriche dal vivo (DHU su telefono fisico) per i rischi di piattaforma: la quota di refresh dei template sotto 1Hz, temuta in ricerca, è stata chiusa con misure oggettive (586 refresh/608s)
- Funzioni pure come seam di test anche per codice legato alla Car App Library (`resolveCarLinkState`, `resolveEffectiveKeepScreenOn`, `createCarHostValidator(allowAllHosts)`)
- Criteri di esito PASS/FAIL/INCONCLUSIVO scritti prima delle sessioni di verifica

### What Was Inefficient
- Il limite strutturale di `PaneTemplate` (numero piccolo, host-controlled) è emerso solo al checkpoint umano di Fase 8, non in ricerca: l'obiettivo "numero grande come sul telefono" era irraggiungibile con il template scelto e ha generato una milestone v2.1 intera
- La Fase 11 è rimasta ferma ~3 settimane su checkpoint hardware; la procedura prevedeva una sessione da scrivania con logcat, ma l'uso reale (head unit in auto, USB occupata) non permette la cattura dei log — le verifiche si sono chiuse come PASS visivi
- Merge di worktree con SUMMARY non committati: un SUMMARY perso e ricostruito a posteriori

### Patterns Established
- Split debug/release esplicito passato come parametro (non letto da `BuildConfig` dentro la funzione) per rendere entrambi i rami testabili
- Nota di rischio interna confinata a `playstore/README.md`, ritirata sostituendola con la spiegazione di cosa l'ha chiusa
- Copia dei `*-SUMMARY.md` fuori dalla worktree prima di `git worktree remove`

### Key Lessons
1. Per le UI su superfici host-controlled (Android Auto), verificare in ricerca cosa il template permette di controllare prima di scrivere success criteria visivi
2. Pianificare le verifiche su hardware reale per come verranno davvero eseguite (in auto, senza adb), non per l'ambiente ideale da scrivania
3. Le verifiche umane bloccanti a fine milestone vanno rese rimandabili esplicitamente (Deferred Items) invece di congelare la milestone

### Cost Observations
- Model mix: prevalentemente opus (profilo `quality`)
- Sessioni: numerose, con checkpoint umani distribuiti su più giorni
- Notable: Fasi 8-10 in 3 giorni; la Fase 11 ha occupato il resto della timeline in attesa di hardware

---

## Cross-Milestone Trends

### Process Evolution

| Milestone | Phases | Key Change |
|-----------|--------|------------|
| v1.0 | 5 | Baseline: roadmap a strati orizzontali, checkpoint umani su device per fasi UI/hardware |
| v1.1 | 2 | Esecuzione wave-based parallela via worktree isolation; checkpoint umano su strada (non solo emulatore); audit di sicurezza retroattivo evidence-based; riuso di pattern v1.0 invece di stabilirne di nuovi |
| v2.0 | 4 | Verifiche empiriche dal vivo su DHU con misure oggettive; runbook con criteri di esito prefissati; verifiche su hardware reale chiuse come PASS visivi e voci rimandabili come Deferred Items |

### Cumulative Quality

| Milestone | Test Suites (JVM) | Zero-Dep Additions |
|-----------|-------------------|--------------------|
| v1.0 | 3 (SpeedMapping, GpsProviderState, MaxSpeedReducer) | play-services-location, kotlinx-coroutines, lifecycle-runtime-ktx, ConstraintLayout |
| v1.1 | +3 (ChargingStateProvider, DistanceReducer, DistanceFormat — GpsProviderState esteso, non nuovo) — 6 totali, 44 test | Nessuna — solo API di piattaforma (`BroadcastReceiver`, `Location.distanceTo()`) |
| v2.0 | +4 JVM (CarLinkState, CarLinkSequence, CarPermissionState, CarSpeedContent) + 2 strumentati (SpeedScreenTemplate, CarHostValidation) — 10 JVM totali | `androidx.car.app` (Car App Library) — unica nuova dipendenza |

### Top Lessons (Verified Across Milestones)

1. Checkpoint umano su device indispensabile per fasi con superficie visiva/hardware — confermato in v1.1 (Fase 6 su emulatore, Fase 7 su strada reale; il mock GPS di un emulatore senza route playback non basta per feature che dipendono dalla velocità).
2. Riusare pattern consolidati da una milestone precedente comprime i tempi di sviluppo — v1.1 (2 fasi in 2 giorni, mirror di store/pipeline esistenti) contro v1.0 (5 fasi in 4 giorni, pattern stabiliti da zero).
3. Le superfici gestite dall'host (Android Auto) vanno verificate dal vivo presto: il limite di `PaneTemplate` e la quota di refresh sono emersi/chiusi solo con DHU reale, non in ricerca (v2.0).
