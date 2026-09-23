# Project Research Summary

**Project:** Tachimetro — v2.1 Velocità a tutto schermo su Android Auto
**Domain:** Rendering diretto sulla `Surface` di Android Auto (Car App Library 1.7.0) per un'app tachimetro GPS già pubblicata (categoria POI, `PaneTemplate` in v2.0)
**Researched:** 2026-09-23
**Confidence:** MEDIA-ALTA sulle meccaniche tecniche (API, permessi, lifecycle — verificate sui sorgenti AndroidX/AOSP); BASSA-MEDIA sull'esito della revisione Play Store e sul comportamento reale dell'host

## Executive Summary

La v2.1 sostituisce il `PaneTemplate` host-controlled di v2.0 con un rendering diretto sulla `Surface` di Android Auto, usando la Car App Library già presente (`androidx.car.app:app:1.7.0`, nessuna nuova dipendenza). Le meccaniche sono ben documentate e verificate direttamente sui sorgenti AndroidX: solo due template danno accesso alla Surface — `NavigationTemplate` (categoria NAVIGATION, quasi tutto-schermo, solo un'action strip obbligatoria che si nasconde dopo 10s) e `MapWithContentTemplate` (categorie Navigation/POI/Weather, ma con un template di contenuto **sempre obbligatorio**, validato in `build()`, che l'host sovrappone come card riducendo l'area disponibile). Entrambi richiedono i permessi manifest `ACCESS_SURFACE` più il permesso di template appropriato, e disegnano su Canvas con `SurfaceCallback` sul main thread, seguendo il pattern del sample ufficiale `navigation/SurfaceRenderer`.

L'approccio raccomandato dalla ricerca è pragmatico: una fase di spike su DHU per misurare concretamente cosa mostra l'host in entrambi i percorsi (categoria/template) prima di scrivere codice di rendering definitivo; funzioni pure testabili in JVM per tutta la geometria e la logica di stato (`computeSpeedLayout`, `surfaceFrame`, `resolveCarScreenMode`), seguendo la convenzione già in uso nel progetto (`mapSpeedToKmh`, `deriveSpeedState`); riuso totale della pipeline dati esistente (`GpsSpeedProvider` Application-scoped, `CarPermissionState`, `CarSpeedContent`) cambiando solo il "sink" da template a Canvas. **Due decisioni di prodotto/rischio non tecniche restano esplicitamente aperte e i quattro documenti di ricerca non convergono su un'unica risposta** — sono descritte in dettaglio più sotto e vanno prese dall'utente prima di bloccare la struttura definitiva della roadmap.

I rischi principali sono tre: (1) un aggiornamento con nuovi permessi Car App/categoria torna in revisione Play Store, e sul canale test aperto (dove oggi vive la v2.0) la revisione form factor Android Auto è **bloccante** — un rifiuto rischia di congelare anche gli aggiornamenti lato telefono; mitigazione: caricare sempre prima sul canale test chiuso (non bloccante) e promuovere solo dopo un esito pulito. (2) Bug di ciclo di vita della Surface (leak per `Surface.release()` mancato, crash su Surface distrutta, schermo nero dopo ricreazione perché `StateFlow` non riemette valori uguali) — mitigazione: un renderer unico con cache dell'ultimo stato e ridisegno su ogni evento, non solo sui tick GPS. (3) Tipografia tarata sui pixel/dpi della Surface (non sp/densità del telefono), con dimensione calcolata su un campione a 3 cifre per evitare che il numero "balli" — mitigazione: funzioni pure testate su più risoluzioni DHU (800×480, 1280×720, 1920×1080, profili wide).

## Key Findings

### Recommended Stack

Nessuna nuova libreria: `androidx.car.app:app:1.7.0` (già nel progetto) contiene già `MapWithContentTemplate`, `SurfaceCallback`, `AppManager.setSurfaceCallback`. Il disegno usa `android.graphics.Canvas`/`Paint` del framework via `Surface.lockCanvas()`/`lockHardwareCanvas()`, con `Typeface.create(Typeface.DEFAULT, 900, false)` per lo stesso peso Black del telefono. L'unica modifica di configurazione è nel manifest: nuovi permessi `androidx.car.app.ACCESS_SURFACE` e `androidx.car.app.MAP_TEMPLATES` (percorso POI) oppure `androidx.car.app.NAVIGATION_TEMPLATES` (percorso NAVIGATION) — mai entrambi insieme in un artefatto caricato su Play, pena rischio di rifiuto.

**Core technologies:**
- `androidx.car.app:app` 1.7.0 (invariata) — `MapWithContentTemplate`/`NavigationTemplate`, `SurfaceCallback`, permessi Car App — già presente, nessuna nuova dipendenza necessaria
- `android.graphics.Canvas`/`Paint`/`Surface.lockHardwareCanvas()` (framework, API 30+) — disegno diretto del numero e di "km/h" — più semplice di `VirtualDisplay`+`Presentation` per due sole stringhe, coerente col divieto di Compose
- `Typeface.create(Typeface.DEFAULT, 900, false)` (framework, API 28+) — peso Black coerente con l'identità visiva del telefono, nessun asset font aggiuntivo

### Expected Features

Il rendering sulla Surface non è mai un canvas libero: è sempre un template "a mappa" con obblighi host (action strip e/o card di contenuto), quindi "numero a tutto schermo senza altro" è raggiungibile solo con `NavigationTemplate`.

**Must have (table stakes):**
- Template con `SurfaceCallback` registrato (permessi + categoria coerenti), sostituisce il `PaneTemplate` attuale
- Action strip con almeno un'azione (obbligo di `build()` su entrambi i template candidati)
- Gestione robusta del ciclo di vita della Surface (`onSurfaceAvailable` ripetibile, release, resize/dpi)
- Numero centrato sulla `stableArea` (non sulla `visibleArea`, che cambia quando l'action strip appare/scompare), dimensionato in pixel dalla geometria reale, non in sp fissi
- "km/h" piccolo in basso a destra della stessa area
- Ridisegno event-driven a 1 Hz (nessun `invalidate()` per tick, solo su cambio contenuto della card/template)
- Tema chiaro/scuro da `carContext.isDarkMode`, ridisegnato su `onCarConfigurationChanged`
- Stato "Ricerca segnale..." e flusso permesso (Fase 9: azione persistente Concedi/Impostazioni) preservati sul nuovo rendering

**Should have (differenziatori a basso costo):**
- Dimensione del numero stabile tra 1-3 cifre (calcolata sul campione "888", non sul valore corrente) per evitare che "salti" a 9→10 o 99→100
- Cifre tabulari (`fontFeatureSettings = "tnum"`) per eliminare il tremolio orizzontale
- Margine di sicurezza per head unit con bordi/angoli arrotondati
- Funzioni pure di layout testabili in JVM su più risoluzioni/dpi

**Defer (v2.1.x o oltre):**
- Fallback esplicito a POI + `MapWithContentTemplate` se NAVIGATION si rivela impraticabile in revisione produzione
- Cluster display (`FEATURE_CLUSTER`) — fuori scope, opt-in separato
- Android Automotive OS nativo — fuori scope come in v2.0

### Architecture Approach

Tutto il nuovo codice resta nel package `car/` esistente. Un `SpeedSurfaceRenderer` (unico file che importa `android.graphics.*`) implementa `SurfaceCallback` in modo passivo: riceve `submit(frame: SurfaceFrame)` da `SpeedScreen` invece di osservare direttamente `GpsSpeedProvider`, mantiene l'ultimo stato in cache e ridisegna a ogni evento (nuovo frame, nuova Surface, cambio area, cambio tema) — questo evita sia lo schermo nero dopo ricreazione della Surface sia la duplicazione del collector GPS. Tutta la geometria e la logica di stato sono estratte in funzioni pure e testabili su JVM (`computeSpeedLayout`, `resolveDrawArea`, `surfaceFrame`, `resolveCarScreenMode`, `surfacePalette`), usando data class proprie (`AreaPx`) invece di `android.graphics.Rect`, che nei test JVM è uno stub non funzionante. `SpeedScreen` mantiene invariata la macchina a stati del permesso e sceglie il template in un unico punto sostituibile (`buildSurfaceTemplate()`), reso deliberatamente indifferente alla decisione categoria/template.

**Major components:**
1. `SpeedSurfaceRenderer` — possiede la Surface corrente, disegna su richiesta, gestisce release/lifecycle
2. `SurfaceLayout.kt` / `SurfaceFrame.kt` / `CarScreenMode.kt` — funzioni pure per geometria, contenuto da disegnare, scelta template-vs-permesso
3. `TachimetroCarSession` (modificato) — crea e possiede il renderer per tutta la durata della Session, inoltra i cambi di configurazione
4. `SpeedScreen` (modificato) — inoltra lo stato GPS al renderer invece di chiamare `invalidate()` per tick; sceglie fra template con Surface e `MessageTemplate` di permesso

### Critical Pitfalls

1. **Revisione Play Store bloccante sul canale test aperto** — nuovi permessi Car App e/o categoria diversa rimettono l'app in revisione form factor Android Auto, bloccante su test aperto/produzione; un rifiuto rischia di congelare anche gli update solo-telefono. Evitare caricando prima sul canale test chiuso (non bloccante) e promuovendo solo dopo un esito pulito.
2. **Card di contenuto obbligatoria in `MapWithContentTemplate`** — riduce strutturalmente l'area disponibile e impedisce il "numero a tutto schermo senza altro" della spec D-14; non è un bug risolvibile via codice.
3. **Livello Car API disallineato** — `MapWithContentTemplate` richiede Car API 7, ma il manifest attuale dichiara `minCarApiLevel=1`; serve una decisione esplicita (vedi sezione Gap sotto) prima di scrivere il branch di selezione template.
4. **Bug di ciclo di vita della Surface** — leak per `release()` mancato, crash su Surface distrutta/non valida, schermo nero dopo ricreazione (perché `StateFlow` non riemette valori uguali). Prevenzione: un solo renderer con cache dello stato e ridisegno su ogni evento, non solo sui tick GPS.
5. **Tipografia tarata sul telefono invece che sui pixel/dpi della Surface** — sp/densità del telefono producono testo minuscolo o tagliato su head unit reali; serve calcolo in pixel su un campione a 3 cifre, testato su più risoluzioni.

## Implications for Roadmap

Basandosi sulla numerazione fasi suggerita da PITFALLS.md (F12-F15 + pulizie) e sul "Suggested Build Order" di ARCHITECTURE.md, la struttura di fase proposta è:

### Phase 1: Spike Surface + decisione categoria/template/canale (F12)
**Rationale:** è il rischio più alto e blocca tutto il resto — la geometria disponibile per il numero dipende interamente da quale template/categoria si sceglie, e questa è anche una decisione di prodotto non solo tecnica (vedi Gap sotto).
**Delivers:** manifest aggiornato (permessi + categoria secondo l'opzione scelta), renderer minimale (sfondo + testo fisso centrato) verificato su DHU in entrambi i percorsi candidati, misure concrete di `stableArea`/`visibleArea` per POI+MapWithContentTemplate e per NAVIGATION+NavigationTemplate, decisione scritta e motivata su categoria/canale di distribuzione.
**Addresses:** i due decision point esplicitamente irrisolti tra i documenti di ricerca (categoria/template, `minCarApiLevel`)
**Avoids:** Pitfall "revisione bloccante", "card obbligatoria", "livello Car API disallineato"

### Phase 2: Funzioni pure di layout e stato (F13a)
**Rationale:** nessuna dipendenza Android, parallelizzabile subito dopo l'esito dello spike; stabilisce il nucleo testabile prima di collegare il rendering reale.
**Delivers:** `SurfaceLayout.kt`, `SurfaceFrame.kt`, `CarScreenMode.kt` con test JVM che usano le aree osservate nello spike come casi di test.
**Uses:** convenzione di progetto delle funzioni pure testabili (`mapSpeedToKmh`, `deriveSpeedState`)

### Phase 3: Integrazione GPS → renderer reale (F13b)
**Rationale:** dipende dalla Phase 1 (scheletro renderer/Session) e dalla Phase 2 (funzioni pure); collega `GpsSpeedProvider` attraverso `SpeedScreen` al renderer definitivo.
**Delivers:** `SpeedSurfaceRenderer` completo con gestione lifecycle-safe della Surface, coalescing del redraw, tema chiaro/scuro via `onCarConfigurationChanged`; rimozione di `invalidate()` per tick e del ramo `PaneTemplate` in stato Granted.
**Implements:** Pattern "renderer passivo last-value + redraw on demand" di ARCHITECTURE.md
**Avoids:** bug di ciclo di vita della Surface, redraw eccessivo, threading sbagliato

### Phase 4: Stati permesso/ricerca segnale + tema (F14)
**Rationale:** dipende dalla Phase 3; gli stati rari/transitori (permesso, ricerca segnale) richiedono attenzione per non consumare la quota dei template e non perdere le azioni della Fase 9 (v2.0).
**Delivers:** `buildPermissionTemplate()` con azione persistente Concedi/Impostazioni, "Ricerca segnale..." disegnato sulla Surface, verifica completa giorno/notte, aggiornamento di `SpeedScreenTemplateTest` + nuovo `SpeedSurfaceRendererTest`.
**Addresses:** requisiti AA-02/AA-04 (v2.0) da non regredire
**Avoids:** regressione del flusso permesso, quota template esaurita, sfondo nel tema sbagliato

### Phase 5: Verifica hardware reale + rilascio (F15)
**Rationale:** ultima — richiede la feature completa per essere verificata su head unit reale e per attraversare la strategia di canale Play Store.
**Delivers:** checklist multi-risoluzione DHU, verifica su head unit reale, caricamento su canale test chiuso → lettura esito → promozione a test aperto, aggiornamento `playstore/` (listing + release notes it-IT/en-US).
**Avoids:** effetti su release già pubblicata, DHU non rappresentativo della head unit reale

### Phase 6: Pulizie minori (indipendente, in parallelo o a inizio/fine)
**Rationale:** scollegata dal lavoro sul renderer; l'architettura raccomanda esplicitamente di tenerla separata perché un eventuale rework dello spike non si mescoli con modifiche lato telefono.
**Delivers:** consolidamento `isDeviceCharging()` → `deriveChargingState()` con test di equivalenza; chiusura della finestra transitoria di `carLink` (stato `Unknown` esplicito) con estensione di `CarLinkSequenceTest`.
**Avoids:** regressioni sul telefono introdotte dal consolidamento

### Phase Ordering Rationale

- La decisione di categoria/template va presa prima di qualunque riga di codice di rendering, perché determina la geometria disponibile (schermo pieno contro area ridotta dalla card) — è la dipendenza di ordinamento più forte trovata in tutti e quattro i documenti.
- Le funzioni pure precedono il codice Android-wired, coerentemente con la convenzione TDD già in uso nel progetto.
- Gli stati rari (permesso, ricerca segnale) arrivano dopo che il percorso principale di rendering è stabile, perché riusano interamente la logica esistente della Fase 9 e cambiano solo il "sink".
- Verifica hardware e rilascio restano sempre per ultimi.
- Le pulizie lato telefono sono completamente disaccoppiate e possono slittare senza impattare il resto.

### Research Flags

Fasi che probabilmente necessitano `/gsd:plan-phase --research-phase`:
- **Phase 1:** l'esito dello spike DHU determina quale percorso (POI o NAVIGATION) proseguire; inoltre la strategia `minCarApiLevel` (tensione STACK vs ARCHITECTURE, vedi Gap) va risolta con osservazione diretta, non assunta.
- **Phase 4:** se si sceglie il percorso POI, il comportamento di reset/non-reset della quota template per `MapWithContentTemplate` non è documentato (MEDIUM confidence) e va verificato empiricamente con l'overlay Developer Mode prima di finalizzare il design degli stati permesso.

Fasi con pattern standard (research-phase non necessaria):
- **Phase 2:** convenzione di funzioni pure già consolidata nel progetto (stesso pattern di `mapSpeedToKmh`/`MaxSpeedReducer`).
- **Phase 3:** pattern Canvas/`SurfaceCallback` documentato dal sample ufficiale AOSP `navigation/SurfaceRenderer`.
- **Phase 5:** gating per canale Play Store documentato ufficialmente (`training/cars/distribute`).
- **Phase 6:** refactoring locale isolato, nessuna API nuova coinvolta.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | MEDIA-ALTA | HIGH su fatti API/manifest (sorgenti AndroidX + doc ufficiale); MEDIUM sulla decisione di categoria (dipende da revisione manuale Google, non deterministica); MEDIUM-LOW sull'aspetto visivo esatto di `MapWithContentTemplate` (da verificare con spike DHU) |
| Features | MEDIA-ALTA | Meccanica API verificata su sorgenti AndroidX + doc ufficiale (draw-maps, car-app-quality); MEDIUM sul comportamento visivo reale dell'host (auto-hide action strip, Coolwalk, risoluzioni head unit); LOW sull'esito della revisione Play Store per un'app che disegna "non-mappa" |
| Architecture | MEDIA-ALTA | HIGH per API Surface/threading/lifecycle (sorgente AOSP + sample ufficiale navigation); MEDIUM per la coesistenza template/surface negli stati di permesso; LOW per comportamento host-specifico (dissolvenza action strip, surface durante MessageTemplate) |
| Pitfalls | MEDIA-ALTA | HIGH sulle meccaniche API (verificate sui sorgenti `androidx.car.app:app:1.7.0` nella cache Gradle del progetto); HIGH sul testo delle regole di revisione Play Store, LOW-MEDIUM su come un revisore giudicherà in pratica un tachimetro su Surface |

**Overall confidence:** MEDIA-ALTA sulle meccaniche tecniche, BASSA sull'esito della revisione Play Store e sul comportamento visivo esatto dell'host — per questo la fase di spike (Phase 1) è un prerequisito obbligatorio, non solo consigliato, prima di qualunque lavoro di rendering definitivo.

### Gaps to Address

**1. Categoria e template (POI + `MapWithContentTemplate` vs NAVIGATION + `NavigationTemplate`) — i documenti di ricerca non convergono su un'unica raccomandazione di default:**

- Tutti e quattro i documenti concordano sui fatti: POI + `MapWithContentTemplate` disegna sulla Surface **senza** cambiare categoria, ma impone **sempre** una card di contenuto host-obbligatoria sovrapposta (validata in `build()`: solo `MessageTemplate`/`PaneTemplate`/`ListTemplate`/`GridTemplate`). Questo significa che la spec visiva D-14 ("numero da solo, a schermo intero, nessuna icona") **non è raggiungibile alla lettera** in POI — si ottiene "numero grande in un'area ridotta accanto a una card", non uno schermo pulito. `NavigationTemplate` dà invece uno schermo quasi completamente pulito (solo action strip che si nasconde dopo 10s), ma richiede categoria NAVIGATION, che secondo NF-1 (turn-by-turn obbligatorio) e NF-6 (gestione intent di navigazione da altre app) un tachimetro verosimilmente non può soddisfare — rifiuto quasi certo in revisione bloccante su canale test aperto/produzione, con rischio di congelare anche gli aggiornamenti lato telefono.
- **Il conflitto è su quale sia il default raccomandato:** STACK.md, PITFALLS.md e ARCHITECTURE.md raccomandano esplicitamente di **restare POI + `MapWithContentTemplate`** e rinegoziare/aggiornare la spec D-14 per accettare la card ("centrato nella stable area", non "centrato nello schermo"), trattando NAVIGATION come extrema ratio distribuibile al massimo su canale interno/test non-aperto. FEATURES.md invece, valutando la spec D-14 come vincolo non negoziabile ("nessuna card, nessuna icona forzata"), propone `NavigationTemplate` come **P1/MVP primario** — perché è l'unica via tecnicamente verificata per soddisfare D-14 alla lettera — relegando il fallback POI + `MapWithContentTemplate` a "Add After Validation" con trigger "la categoria NAVIGATION si rivela impraticabile in revisione produzione". Nessuno dei quattro documenti risolve la tensione: è un compromesso tra fedeltà alla spec visiva originale e rischio di distribuzione, non una questione tecnica.
- **Decisione da richiedere esplicitamente all'utente prima di bloccare la roadmap definitiva:** (a) accettare di aggiornare la spec D-14 per convivere con la card host-obbligatoria e restare POI, con rischio di distribuzione più basso; oppure (b) tentare NAVIGATION per lo schermo pulito, accettando un rischio concreto di rifiuto bloccante su canale aperto/produzione (o distribuendo quella build solo su canale interno/test chiuso, mai promuovendola). Lo spike DHU (Phase 1) deve produrre le misure concrete (i `Rect` stable/visible osservati in entrambi i percorsi) per informare la scelta, ma la decisione finale resta di prodotto/rischio e va presa dall'utente, non assunta implicitamente durante la pianificazione.

**2. `minCarApiLevel`: alzare a 7 vs mantenere 1 con fallback runtime — tensione tra STACK.md e ARCHITECTURE.md:**

- STACK.md raccomanda esplicitamente di **mantenere `minCarApiLevel=1` invariato** e fare un check a runtime (`carContext.carAppApiLevel >= CarAppApiLevels.LEVEL_7`), con fallback al `PaneTemplate` v2.0 esistente per host con Car API level inferiore a 7, per non escludere head unit/versioni Android Auto datate "per un guadagno solo estetico".
- ARCHITECTURE.md, descrivendo la conseguenza architetturale del fatto che `MapWithContentTemplate` è `@RequiresCarApi(7)`, afferma che "il percorso POI è possibile ma: `minCarApiLevel` va alzato da 1 a 7", presentandolo quasi come requisito automatico del percorso POI — salvo poi, nel "Suggested Build Order", qualificarlo con "minCarApiLevel se POI", lasciando la porta aperta all'alternativa runtime senza sciogliere esplicitamente la contraddizione.
- PITFALLS.md (Pitfall 4) inquadra le due strade come una scelta esplicita da fare in fase di spike: (a) alzare `minCarApiLevel` a 7 accettando la perdita di host più vecchi; (b) mantenere basso con branch a runtime e fallback `PaneTemplate` — le cita entrambe senza raccomandare in modo netto, ma nota che l'opzione (b) ha un costo di recovery più basso ("codice già collaudato").
- **Raccomandazione di sintesi per la roadmap:** la posizione meglio supportata (STACK.md + costo di recovery più basso secondo PITFALLS.md) è mantenere `minCarApiLevel=1` con branch a runtime e fallback al `PaneTemplate` esistente, così l'app non sparisce dal launcher Android Auto per i tester con versioni meno aggiornate. Va comunque validato nello spike (Phase 1) quale Car API level riportano realmente DHU e la head unit reale dell'utente: se fosse già ≥7 su entrambi, la questione è teorica per l'uso attuale ma resta rilevante per la distribuzione pubblica su dispositivi non controllati.

**3. Altri gap minori (impatto più contenuto, da tenere presenti in fase di esecuzione):**
- Aspetto reale della card POI su Android Auto (posizione, dimensione, eventuale icona/titolo forzato) non documentato con precisione — risolvibile solo con lo spike DHU.
- Esito della revisione form factor della v2.0 sul canale test aperto non verificato da questa ricerca: se l'app è già passata come POI, è un segnale forte a favore di restare POI.
- Nessun precedente pubblico verificabile di un tachimetro "solo Surface" approvato in categoria POI su Play Store.
- Comportamento non documentato di Play Console su (a) rifiuto di un update quando la release precedente è già live, e (b) cambio di categoria su un'app esistente — trattare un eventuale cambio di categoria come decisione irreversibile a livello di listing, mai caricare direttamente sul canale aperto.

## Sources

### Primary (HIGH confidence)
- Sorgenti AndroidX/AOSP (`androidx-main`, incluso `-sources.jar` in cache Gradle locale): `MapWithContentTemplate.java`, `NavigationTemplate.java`, `SurfaceCallback.java`, `AppManager.java`, `SurfaceContainer.java`, `CarAppPermission.java`, `ContentTemplateConstraints.java`, `ActionStrip.java`, `Action.java`, `MessageTemplate.java`, `Screen.java`, `TestAppManager.java`, sample ufficiale `car/app/app-samples/navigation/.../SurfaceRenderer.java` — meccaniche API, permessi, threading, lifecycle
- https://developer.android.com/training/cars/apps/library/draw-maps — categorie, tabella template/permessi, visible/stable area, dark mode
- https://developer.android.com/training/cars/apps/poi — `MapWithContentTemplate` disponibile per POI con `MAP_TEMPLATES`
- https://developer.android.com/training/cars/apps/navigation — obblighi categoria NAVIGATION
- https://developer.android.com/docs/quality-guidelines/car-app-quality — PC-1, PF-1, NF-1/2/5/6/9, MR-1, SA-1, VD-1, UX-3
- https://developer.android.com/training/cars/distribute — gating di revisione per canale (chiuso non bloccante, aperto/produzione bloccanti)
- https://developer.android.com/training/cars — categorie supportate

### Secondary (MEDIUM confidence)
- https://developers.google.com/cars/design/create-apps/apps-for-drivers/templates/navigation-template e .../components/action-strip — comportamento action strip, contenuto ammesso sulla safe area
- https://android-developers.googleblog.com/2026/05/... — Car App Library 1.8.0-beta01/1.9.0-alpha01, widget Android Auto annunciati (fuori scope attuale)
- https://developer.android.com/training/cars/testing/dhu — configurazione risoluzioni DHU
- https://www.androidauthority.com/google-maps-android-auto-live-speedometer-3689236/ — precedente badge velocità su mappa

### Tertiary (LOW confidence)
- Thread community Play Console su rifiuti per categoria ("Category not permitted") — esistenza confermata, dettagli non leggibili
- Thread community Android Auto su `MapWithContentTemplate` in categoria POI — contenuto non recuperabile, nessuna evidenza di esito
- github.com/agronick/aa-torque — precedente Surface custom solo sideload, non distribuito su Play

---
*Research completed: 2026-09-23*
*Ready for roadmap: sì, con due decisioni esplicite da richiedere all'utente prima della Phase 1 (vedi Gaps to Address) — la roadmap può includerle come parte della Phase 1 stessa*
