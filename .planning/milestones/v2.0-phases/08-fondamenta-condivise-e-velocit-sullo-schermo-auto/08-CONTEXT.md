# Phase 8: Fondamenta Condivise e Velocità sullo Schermo Auto - Context

**Gathered:** 2026-08-31
**Updated:** 2026-09-02 (dopo verifica DHU dal vivo — vedi D-11..D-14)
**Status:** Ready for planning

<domain>
## Phase Boundary

La velocità corrente e lo stato "nessun segnale" vengono mostrati sullo schermo Android Auto (categoria `POI`, template standard della Car App Library), aggiornati alla stessa cadenza del telefono (1/sec), condividendo un'unica sottoscrizione GPS Application-scoped tra telefono e auto — nessuna sottoscrizione duplicata, nessuna regressione visibile sul telefono.

Requirements coperti: AA-01, AA-02, AA-03 (v. REQUIREMENTS.md).

Fuori scope per questa fase (già deciso a livello di milestone/roadmap, non riaperto qui): permesso di localizzazione dallo schermo auto (Fase 9), comportamento del telefono alla connessione/disconnessione (Fase 10), `HostValidator` reale e verifica su dispositivo fisico in background (Fase 11), velocità massima/distanza sullo schermo auto (esplicitamente out of scope di milestone), categoria/percorso di distribuzione (già risolto — vedi sotto).

</domain>

<decisions>
## Implementation Decisions

### Categoria e architettura (già lockate prima di questa discussione — non riaperte)
- **D-00a:** Categoria `POI` + template standard della Car App Library, distribuzione Play-Store-safe. Deciso in fase di ricerca/roadmap (v. PROJECT.md Key Decisions, STATE.md Decisions), non un gray area di questa discussione.
- **D-00b:** `GpsSpeedProvider` promosso da Activity-scoped ad Application-scoped tramite una nuova `TachimetroApplication` (una proprietà `by lazy`, non un framework DI) — sia `MainActivity` sia il nuovo `SpeedScreen` diventano puri collector dello stesso `StateFlow<SpeedState>`. Pattern raccomandato da `.planning/research/ARCHITECTURE.md` (Pattern 1) e già registrato come decisione in STATE.md.

### Contenuto schermo auto
- **D-01:** La velocità si presenta nel template con il numero come testo principale della Row e "km/h" come titolo/sottotitolo separato — rispecchia la separazione `messageText`/`unitText` già usata sul telefono (MainActivity.kt:342, 384), pur sapendo che lo stile/dimensione effettiva è host-controlled (niente autosize custom sul template).
- **D-02:** Lo stato "nessun segnale" sullo schermo auto usa un testo dedicato, più breve di quello del telefono: **"Ricerca segnale..."** (non riusa `searching_gps_signal` = "Ricerca segnale GPS..." — tronca solo "GPS"). Richiede una nuova risorsa stringa separata da `searching_gps_signal`.
- **D-03:** Nessun titolo/branding (es. "Tachimetro") in cima al template — solo il valore/stato, coerente con la filosofia "nessun elemento non necessario" del progetto (PROJECT.md Constraints).
- **D-04:** L'icona dell'app nella lista app di Android Auto riusa `ic_launcher` esistente (`app/src/main/res/mipmap-*`, `drawable/ic_launcher_*.xml`) — nessun asset grafico dedicato per l'auto.

### Mitigazione rischio quota refresh (Pitfall 2)
- **D-05:** Si implementa il refresh del template alla piena cadenza 1Hz fin da subito (coerente con SC3 "stessa cadenza del telefono, senza salti né disallineamenti"). Non si introduce una cadenza più prudente in via preventiva.
- **D-06:** La verifica empirica del rischio quota (SC4) è un gate esplicito prima di considerare il refresh 1Hz definitivo — non un dettaglio da scoprire dopo.
- **D-07:** Se la verifica empirica rivela che la quota si esaurisce davvero durante una sessione continua, la mitigazione è: **throttle del refresh SOLO sul lato auto** (es. ogni 2-3s), lasciando il telefono a 1Hz invariato. Questo significa accettare, solo in quello scenario, di rinegoziare esplicitamente SC3 con l'utente — non un compromesso silenzioso. Non si sceglie invece di fermarsi e rivalutare l'intero approccio Surface/NAVIGATION (opzione scartata).

### Checkpoint di verifica DHU (SC4)
- **D-08:** La sessione di test empirico dura **5-10 minuti** di refresh continuo simulato a 1Hz — sufficiente a rivelare un esaurimento rapido della quota, coerente con "alcuni minuti" del goal di fase.
- **D-09:** La verifica gira su un **AVD emulator di Android Studio** connesso a DHU (Desktop Head Unit) con Developer Mode/debug overlay attivo — non richiede un telefono fisico. Questo è un cambio rispetto all'assunzione iniziale di `.planning/research/PITFALLS.md` (Pitfall 7: "richiede un telefono reale via USB") — la documentazione ufficiale DHU supporta anche un emulatore (API 23+, Google Play). Il telefono fisico resta necessario solo per la Fase 11 (comportamento background-location a telefono bloccato durante una guida reale), fuori scope qui.
- **D-10:** L'esecuzione va automatizzata dove possibile (script/istruzioni riproducibili per lanciare l'AVD + DHU + il conteggio dei refresh), ma la conferma finale di superamento resta un checkpoint umano — coerente con il pattern già usato in tutte le fasi precedenti (v1.0/v1.1), dato che il rendering visivo e il comportamento dell'host DHU non sono verificabili solo da codice/test automatici.

### Esito verifica DHU e decisione layout (aggiunto 2026-09-02, dopo sessione DHU dal vivo)
- **D-11:** Sessione DHU dal vivo eseguita su **telefono fisico** (OnePlus 8T via USB) invece dell'AVD instabile — deviazione da D-09 solo per problemi infrastrutturali di quella sessione, non una revisione della decisione. Esito: **SC4 (quota refresh) CONFERMATO PASS** (586 refresh in 608s, cadenza media 0.964/s, nessun gap oltre 3.1s, processo mai terminato/cambiato, host non ha mai chiuso l'app — confermato sia dallo script sia dall'utente a occhio) e **SC5 (nessuna regressione telefono) CONFERMATO**. **SC2 (perdita segnale GPS) NON ANCORA TESTATO** — resta da verificare prima di poter chiudere il Task 3/piano 08-03.
- **D-12:** **SC1 (numero grande e leggibile) osservato FALLIRE** nell'implementazione `PaneTemplate` attuale — il numero appare piccolo, allineato in alto a sinistra, con l'icona dell'app anch'essa forzata in alto a sinistra (screenshot utente, sessione 2026-09-02). Causa strutturale, non un bug: `PaneTemplate`/`Row` non espone alcuna API per font size, allineamento o posizione del testo (rendering interamente host-controlled); e richiede un titolo testuale O un header action per costruirsi (D-03 esclude il titolo testuale, quindi `Action.APP_ICON` resta l'unica opzione valida, non rimovibile restando su questo template).
- **D-13 (decisione presa in questa discussione):** Per v2.0 **si accetta il layout host-controlled di `PaneTemplate` così com'è** — nessun'altra modifica di codice va tentata su questo fronte per la Fase 8 (non esistono leve API per ottenerlo). `AA-01` resta soddisfatto nell'accezione già scritta in `REQUIREMENTS.md` ("stile/tipografia gestiti dall'host"), non nell'accezione "grande come sul telefono" — questa tensione è nota e accettata consapevolmente, non un compromesso silenzioso.
- **D-14 (decisione presa in questa discussione):** Il passaggio a **`NavigationTemplate` + `SurfaceCallback`** (disegno Canvas custom: numero grande e centrato, unità di misura posizionabile, nessuna icona forzata) è rimandato esplicitamente a una **milestone v2.1 dedicata**, DOPO il completamento delle Fasi 9-11 di v2.0 — non inserito come fase aggiuntiva dentro v2.0. Questo riapre la decisione di categoria (`NAVIGATION` invece di `POI`), con revisione Play Store più severa in produzione (ma testabile su canali interni/Internal Test Track senza quella revisione) — da affrontare esplicitamente in fase di roadmap v2.1, non anticipato qui. Vedi `<deferred>` per la visual spec raccolta.

### Claude's Discretion
- Nome esatto della nuova risorsa stringa per "Ricerca segnale..." (es. `car_searching_gps_signal`) — solo il testo è deciso, non l'identificatore.
- Scelta del template specifico della Car App Library (`PaneTemplate` vs altro) per comporre Row principale + titolo separato — vincolo funzionale deciso (D-01), implementazione tecnica lasciata al planner/ricerca.
- Meccanismo esatto di automazione del test DHU (script batch/PowerShell, tooling ADB) — il vincolo è "automatizzato dove possibile + conferma umana finale" (D-10), non lo strumento specifico.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Ricerca di progetto (v2.0 Android Auto)
- `.planning/research/SUMMARY.md` — sintesi esecutiva, fasi suggerite, architettura Application-scoped, gap noti (quota refresh, background location)
- `.planning/research/PITFALLS.md` — Pitfall 1 (categoria/distribuzione, già risolto), Pitfall 2 (quota refresh — rilevante per D-05/D-06/D-07), Pitfall 3 (Automotive OS accidentale), Pitfall 4 (permessi da `Screen`, rilevante per Fase 9), Pitfall 5 (GPS duplicato — rilevante per D-00b), Pitfall 6 (background location, Fase 11), Pitfall 7 (DHU su Windows — nota: D-09 diverge dall'assunzione "telefono reale" per l'uso su emulatore)
- `.planning/research/ARCHITECTURE.md` — Pattern 1 (Application-scoped `GpsSpeedProvider`), struttura `TachimetroCarAppService`/`Session`/`SpeedScreen`
- `.planning/research/FEATURES.md` — feature table stakes/differentiators per il car screen
- `.planning/research/STACK.md` — `androidx.car.app:app:1.7.0`, manifest scaffolding (`automotive_app_desc.xml`, `minCarApiLevel`)

### Roadmap e requisiti
- `.planning/ROADMAP.md` §"Phase 8: Fondamenta Condivise e Velocità sullo Schermo Auto" — goal, 5 success criteria (SC1-SC5), dipendenze
- `.planning/REQUIREMENTS.md` §"Schermo Android Auto" — AA-01, AA-02, AA-03 (requisiti coperti da questa fase)
- `.planning/PROJECT.md` §"Key Decisions" — categoria POI/distribuzione/architettura già lockate; §"Constraints" — filosofia "nessun elemento non necessario" (rilevante per D-03)

No external specs (nessun SPEC.md per questa fase) — requisiti pienamente catturati in ROADMAP.md/REQUIREMENTS.md e nelle decisioni sopra.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `GpsSpeedProvider` (`app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt`): espone già `state: StateFlow<SpeedState>` via `WhileSubscribed()` — pronto per essere condiviso da più collector (telefono + auto) una volta spostato a livello Application, senza modifiche interne.
- `SpeedState` (sealed class: `Searching`, `Reading(kmh, deltaMeters)`, `NoSignal`) e `deriveSpeedState()` — riusabili as-is dal nuovo `SpeedScreen` per lo stato "nessun segnale" (D-02).
- `mapSpeedToKmh()` — funzione pura, nessuna modifica necessaria.
- `R.string.searching_gps_signal` = "Ricerca segnale GPS..." (`app/src/main/res/values/strings.xml:8`) — NON riusata per D-02 (serve una stringa nuova più breve), ma è il riferimento di partenza per il tono/wording.
- `R.mipmap.ic_launcher` / `drawable/ic_launcher_*.xml` — icona da riusare per D-04 (CarIcon del service).

### Established Patterns
- `MainActivity` collega `gpsSpeedProvider.state` dentro `repeatOnLifecycle(Lifecycle.State.STARTED)` con `permissionGranted.collectLatest` (MainActivity.kt:215-228) — lo stesso pattern di collection va replicato in `SpeedScreen`/`Session` (che implementano `LifecycleOwner` nella Car App Library, nessun nuovo idioma di coroutine necessario).
- Costruzione con `applicationContext`, mai `Activity` (WR-04, vedi `GpsSpeedProvider(context: Context)` — usa già `context.applicationContext` internamente) — lo stesso vincolo si applica a `TachimetroApplication`.
- `GpsSpeedProvider.close()` chiamato oggi in `MainActivity.onDestroy()` (MainActivity.kt:294) — questa chiamata diventa scorretta una volta che il provider è Application-scoped e va rimossa quando si sposta la costruzione.

### Integration Points
- `AndroidManifest.xml` attuale non ha alcuna dichiarazione Car App Library — serve aggiungere `res/xml/automotive_app_desc.xml`, il meta-data `com.google.android.gms.car.application`, `androidx.car.app.minCarApiLevel`, e la dichiarazione `<service>` per `TachimetroCarAppService` con intent-filter categoria `POI`.
- `app/build.gradle.kts` non ha ancora `androidx.car.app:app` — da aggiungere (v.1.7.0 stabile) insieme a `androidx.car.app:app-testing` per i test.
- Nuovo package `car/` (mirror di `gps/`, `maxspeed/`, `screen/`) per `TachimetroCarAppService`/`TachimetroCarSession`/`SpeedScreen`, coerente con la struttura a feature-package del progetto.

</code_context>

<specifics>
## Specific Ideas

- Il numero sul template auto deve rispecchiare la separazione visiva phone (digit dominante + unità piccola separata, D-01), anche sapendo che lo stile finale è host-controlled.
- Testo "nessun segnale" per l'auto: esattamente **"Ricerca segnale..."** (D-02) — non "Nessun segnale" (opzione scartata) né il testo identico al telefono.
- Nessun titolo "Tachimetro" o branding testuale sul template (D-03).
- Verifica quota via emulatore Android Studio + DHU, non telefono fisico, con automazione dove possibile (D-09/D-10) — deviazione esplicita e consapevole dall'assunzione "telefono reale" di PITFALLS.md Pitfall 7 per questo specifico test (rendering/quota), non per i test di background-location della Fase 11. **Aggiornamento 2026-09-02:** la verifica effettivamente eseguita ha usato un telefono fisico via DHU (non l'AVD) per aggirare un'instabilità infrastrutturale — vedi D-11.
- **Visual spec per l'eventuale schermo Surface/NavigationTemplate (v2.1, NON v2.0)**, raccolta dall'utente in questa sessione: numero grande e centrato nello schermo (come sul telefono); unità di misura ("km/h") allineata in basso a destra; nessuna icona app visibile. Non applicabile a `PaneTemplate`/v2.0 (D-13) — riferimento per quando si pianificherà v2.1 (D-14).

</specifics>

<deferred>
## Deferred Ideas

### Passaggio a Surface/NavigationTemplate per lo schermo auto (milestone v2.1, non v2.0)
- Emerso il 2026-09-02 dopo la sessione DHU dal vivo: `PaneTemplate` (categoria POI) non può dare un numero grande/centrato con posizione custom dell'unità e nessuna icona — limite strutturale dell'API (D-12), non un bug.
- Decisione presa: rimandare esplicitamente a una milestone v2.1 dedicata, dopo il completamento delle Fasi 9-11 di v2.0 (D-14) — non una fase aggiuntiva dentro v2.0.
- Visual spec già raccolta dall'utente (vedi `<specifics>`): numero grande e centrato, unità in basso a destra, nessuna icona.
- Implica riaprire la decisione di categoria da `POI` a `NAVIGATION` — revisione Play Store più severa in produzione, ma testabile su canali interni (Internal Test Track/Internal App Sharing) senza quella revisione. Da riconfermare esplicitamente quando si pianificherà v2.1.
- Riferimento tecnico già in archivio: `.planning/research/STACK.md` (sezioni "What NOT to Use" e "Alternatives Considered") — confronto PaneTemplate vs NavigationTemplate+SurfaceCallback già documentato.

</deferred>

---

*Phase: 8-Fondamenta Condivise e Velocità sullo Schermo Auto*
*Context gathered: 2026-08-31*
