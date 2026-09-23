# Feature Research

**Domain:** Schermo Android Auto disegnato direttamente sulla `Surface` (Car App Library, `SurfaceCallback`) per un tachimetro GPS a numero singolo. Milestone v2.1.
**Researched:** 2026-09-23
**Confidence:** MEDIUM-HIGH. Meccanica API verificata sui sorgenti `androidx/androidx` (branch `androidx-main`: `NavigationTemplate.java`, `MapWithContentTemplate.java`, `MapController.java`, `SurfaceCallback.java`, `ActionStrip.java`, `Action.java`, `MessageTemplate.java`, `Screen.java`) e sulla documentazione ufficiale (draw-maps, car-app-quality, Design for Driving). MEDIUM sul comportamento visivo reale dell'host (auto-hide dell'action strip, Coolwalk/split-screen, risoluzioni delle head unit). LOW sull'esito della revisione Play Store per un'app che disegna "non-mappa".

> Ambito: solo ciò che serve per le NUOVE feature v2.1 (rendering su Surface). Le feature v2.0 già consegnate (GPS condiviso Application-scoped, stato "Ricerca segnale...", flusso permesso dall'auto, stato neutro sul telefono) sono trattate come dipendenze esistenti, non da ricercare di nuovo.

## Inquadramento (da leggere prima delle tabelle)

Uno schermo "disegnato sulla Surface" in Android Auto **non è una tela libera a schermo intero**. È sempre un **template di tipo mappa** (l'host resta proprietario dello schermo) sotto il quale l'app disegna dentro una `Surface`, con obblighi e zone occupate dall'host. Tutto il resto del documento discende da cinque fatti verificati.

1. **Solo due template danno la Surface (non deprecati):**
   - `NavigationTemplate`: solo categoria NAVIGATION, permesso `androidx.car.app.NAVIGATION_TEMPLATES`. `build()` lancia `IllegalStateException("Action strip for this template must be set")`, quindi **l'action strip è obbligatorio**, e `ActionStrip.build()` richiede almeno un'azione. Nessuna icona app forzata e nessun titolo obbligatorio. L'area mappa è di fatto tutto lo schermo meno l'action strip.
   - `MapWithContentTemplate` (`@RequiresCarApi(7)`): categorie Navigation, **POI** e Weather, con permesso `NAVIGATION_TEMPLATES` **oppure** `MAP_TEMPLATES`. Qui però **il template di contenuto è obbligatorio**: `setContentTemplate()` accetta List/Pane/Grid/Message (più SectionedItem da API 8) e viene validato in `build()`. L'host lo disegna come **card sovrapposta alla mappa**, e le aree visible/stable si restringono di conseguenza. Il `MessageTemplate` richiede un messaggio non vuoto. In pratica un'app POI può disegnare sulla Surface, ma **sempre con una card host sopra**, per cui "numero a tutto schermo senza altri elementi" non è ottenibile. Questo risponde alla domanda aperta in PROJECT.md: POI + Surface è tecnicamente possibile, ma non rispetta la visual spec D-14.
   - Entrambi richiedono anche `androidx.car.app.ACCESS_SURFACE` e la registrazione `AppManager.setSurfaceCallback(...)`.
2. **La Surface si ridisegna fuori dalla quota template.** Il ridisegno sul Canvas a 1 Hz non passa da `invalidate()`/`onGetTemplate()`, quindi il rischio quota (Pitfall 2 di v2.0) sparisce per l'aggiornamento della velocità. La quota torna in gioco **solo quando cambia il template** (per esempio se si passa a un `MessageTemplate` per il permesso). `NavigationTemplate` inoltre "resetta la quota" quando lo si raggiunge (javadoc).
3. **Visible area e stable area** (`SurfaceCallback.onVisibleAreaChanged` / `onStableAreaChanged`): la stable area è "il rettangolo più piccolo sempre visibile secondo il template corrente", la visible area si allarga quando l'action strip scompare. L'action strip delle mappe **si nasconde dopo 10 s senza interazione** e riappare a qualsiasi tocco (Design for Driving). Le azioni con `Action.FLAG_IS_PERSISTENT` (API 5) non scompaiono. Se il layout segue la visible area, il numero "salta" ogni volta che l'utente tocca lo schermo.
4. **Pan/zoom non sono obbligatori.** Tutti i metodi di `SurfaceCallback` (`onScroll`, `onFling`, `onScale`, `onClick`) sono `default` vuoti. Il pan si attiva solo se l'app mette `Action.PAN` nella map action strip, che è opzionale su `NavigationTemplate` e su `MapController`. Un tachimetro non li dichiara e non riceve gesture.
5. **Day/night a carico dell'app.** Con una Surface l'host non tematizza più nulla: bisogna leggere `carContext.isDarkMode` e ridisegnare in `onCarConfigurationChanged`. La regola MR-1 (Navigation/POI/Weather) chiede di disegnare in tema chiaro o scuro "quando istruiti", e la guida draw-maps dice "by default, apps must draw with light colors and support the dark theme". Il tema fisso è ammesso solo come scelta dell'utente.

**Obblighi di contenuto (qualità/policy):**
- **NF-2** (Navigation): sulla Surface dei template di navigazione si disegna "solo contenuto mappa", ma "Additional information relevant to the drive, speed limit, ... can be drawn on the safe area of the map". Design for Driving (Navigation template): le app "MAY draw driving-related details and alerts on the map, such as **current speed**, speed limit, camera ahead". La velocità corrente è quindi contenuto esplicitamente ammesso. Una Surface che contiene *solo* la velocità e nessuna mappa resta però in tensione con NF-1 ("must provide turn-by-turn navigation", Tier 2) in revisione produzione. Questo è un tema di categoria e distribuzione (STACK/PITFALLS), non di feature.
- **SA-1** (tutte le categorie templated): niente elementi animati. Il ridisegno a 1 Hz di un numero statico va bene, transizioni o contatori animati no.
- **VD-1**: colori e icone devono rispettare i requisiti di contrasto di Android Auto.
- **UX-3** (24 sp minimo) è formalmente per Video/Games/Browsers, ma è un utile riferimento minimo per le scritte secondarie ("km/h", messaggi di stato).

## Feature Landscape

### Table Stakes (obbligatori, senza questi lo schermo è rotto o non passa l'host)

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| **Template mappa con `SurfaceCallback` registrato** (`NavigationTemplate`, visto che la spec vieta card e icone) | Senza Surface non c'è rendering custom; è il cuore della milestone | MEDIUM | `AppManager.setSurfaceCallback()` nel `Screen`/`Session`; permessi `NAVIGATION_TEMPLATES` + `ACCESS_SURFACE`; categoria NAVIGATION nel manifest. Sostituisce il `PaneTemplate` di `SpeedScreen.buildTemplate()`. |
| **Action strip con almeno un'azione** | `NavigationTemplate.build()` lancia un'eccezione senza strip, e `ActionStrip.build()` senza azioni | LOW | Si nasconde da sola dopo 10 s, quindi per la maggior parte del tempo non copre il numero. Serve comunque un'azione sensata (vedi "Dipendenze", decisione aperta). Nessun `Action.APP_ICON` necessario, e questo soddisfa la parte "nessuna icona" di D-14. |
| **Ciclo di vita della Surface gestito** (`onSurfaceAvailable` può arrivare più volte, `onSurfaceDestroyed`, nuova Surface = nuove dimensioni/dpi) | Se si disegna su una Surface distrutta o si ignora un cambio di dimensioni si ottengono crash o schermo nero | MEDIUM | Tenere l'ultimo `SurfaceContainer` e l'ultimo stato; ridisegnare subito quando arriva una Surface nuova, senza aspettare il tick GPS successivo. Disegno diretto `surface.lockHardwareCanvas()`/`unlockCanvasAndPost()`, più semplice di `VirtualDisplay`+`Presentation` e coerente con il vincolo "no Compose". |
| **Numero grande centrato nella stable area** | Visual spec D-14 e Core Value (leggibile a colpo d'occhio) | MEDIUM | Layout calcolato su `onStableAreaChanged`, non sulla visible area, così il numero non si sposta quando l'action strip appare o scompare. Ricalcolo a ogni cambio di stable area o di Surface. |
| **"km/h" in basso a destra, piccolo** | Visual spec D-14, speculare a `unitText` sul telefono | LOW | Ancorato all'angolo in basso a destra della stable area con margine. Dimensione minima ragionevole (circa 24 sp convertiti con `SurfaceContainer.dpi`). |
| **Dimensionamento testo in pixel dalla geometria** (non in sp fissi) | Le head unit vanno da 800x480@160dpi a 1920x1080 e oltre, con aspect ratio molto diversi (wide 1920x720 via margini); uno sp fisso è troppo piccolo sugli schermi larghi o non ci sta su quelli piccoli | MEDIUM | Equivalente Canvas dell'autosize uniform del telefono: ricerca della dimensione massima per cui la stringa di riferimento sta in una frazione della stable area (sia in larghezza sia in altezza). Font di sistema Bold/Black come sul telefono. |
| **Day/night: tema scuro e chiaro** | MR-1/VD-1; la guida draw-maps: "by default ... light colors and support the dark theme"; uno schermo bloccato nel tema sbagliato sembra rotto | LOW-MEDIUM | `carContext.isDarkMode` più ridisegno in `onCarConfigurationChanged`. Scuro: sfondo nero e cifre bianche (identico al telefono). Chiaro: sfondo bianco e cifre nere. Nessun colore accento (vincolo di progetto). |
| **Stato "Ricerca segnale..." disegnato sulla Surface** | Già esistente in v2.0 (D-02); con la Surface non c'è più l'host a mostrarlo | LOW | Stesso `carSpeedContent(SpeedState)` già esistente (`CarSpeedContent.kt`), cambia solo il renderer: testo centrato di dimensione ridotta (cap separato, come i 56 sp massimi dei messaggi sul telefono), nessun "km/h". |
| **Stato permesso (richiesta, rifiuto, rifiuto permanente) con azione cliccabile** | Già esistente in v2.0 (Fase 9); `CarContext.requestPermissions()` richiede un gesto dell'utente e la Surface non è interattiva (nessun `onClick` senza pan) | MEDIUM | Consigliato: **restare su `NavigationTemplate`**, disegnare il messaggio sulla Surface e mettere "Concedi"/"Apri impostazioni" come azione nell'action strip con `FLAG_IS_PERSISTENT` (non scompare dopo 10 s). In alternativa si fa push di un `MessageTemplate`, che però costa quota e alterna Surface/non-Surface. Riusa `CarPermissionState`/`resolveCarPermissionState`/`CarPermissionDenialStore` senza modifiche. |
| **Ridisegno a 1 Hz sulla stessa pipeline `StateFlow<SpeedState>`** | AA-03 già validato; parità telefono/auto | LOW | Collector nel lifecycle del `Screen`, `draw()` a ogni emissione; niente `invalidate()` del template quando cambia solo la velocità. Il test di quota v2.0 (586 refresh/608 s) non è più il gate: lo diventa un conteggio dei frame disegnati. |
| **Nessuna animazione** | SA-1 + vincolo di progetto | LOW | Sostituzione secca del numero a ogni tick. |

### Differentiators (allineati al Core Value, costo marginale basso)

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| **Dimensione del numero stabile tra 1, 2 e 3 cifre** | Sul telefono l'autosize uniform fa cambiare dimensione al numero quando si passa da 9 a 10 o da 99 a 100 km/h. In auto una dimensione che salta distrae | LOW | Calcolare la dimensione una sola volta per stable area sulla stringa peggiore ("888") e poi centrare. Con cifre tabulari (`Paint.fontFeatureSettings = "tnum"`) il numero non "balla" tra 1 e 7. |
| **Layout sulla stable area invece che sulla visible area** | Il numero non si muove mai, nemmeno quando l'action strip compare dopo un tocco | LOW | Scelta deliberata: la visible area più grande si ignora. Vale un piccolo sacrificio di dimensione in cambio della stabilità. |
| **Margine di sicurezza per head unit con cornici e angoli arrotondati** | Alcune head unit ritagliano i bordi (content insets); una "km/h" attaccata all'angolo può finire tagliata | LOW | Padding proporzionale (per esempio circa il 3-4% del lato minore) dentro la stable area. Verificabile con la configurazione DHU (`margins`/`content_insets`). |
| **Funzioni pure di layout testabili in JVM** | Coerente con le convenzioni del progetto (`mapSpeedToKmh`, `deriveSpeedState`); permette di bloccare con test la geometria su 800x480, 1280x720, 1920x1080 e aspect ratio larghi | LOW-MEDIUM | Per esempio `computeSpeedLayout(stableArea, dpi, measure: (textSize) -> width)` restituisce posizione e dimensione. Il `Canvas` resta un sottile strato di I/O. |
| **Stessa semantica di stato di telefono e auto** | La correttezza (il numero dell'auto non contraddice mai il telefono) è già garantita dall'architettura v2.0 | LOW (già esistente) | Nessun lavoro nuovo: il renderer consuma `CarSpeedContent`/`CarPermissionState` già esistenti. |

### Anti-Features (sembrano utili, creano problemi)

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| **`MapWithContentTemplate` in categoria POI "per evitare NAVIGATION"** | Tiene la categoria POI, con revisione meno severa | La card di contenuto è obbligatoria (validata in `build()`) e si sovrappone alla Surface, restringendo stable e visible area. Non si ottiene né il "tutto schermo" né l'"assenza di elementi" di D-14, e un `PaneTemplate` o `MessageTemplate` nella card riporta titolo, header o icona | Da considerare solo come fallback esplicito se NAVIGATION risultasse impraticabile per la distribuzione. In quel caso la spec visiva va rinegoziata con l'utente e non degradata in silenzio |
| **Pan/zoom, `Action.PAN`, map action strip** | "I template mappa li hanno" | Non c'è nessuna mappa da spostare. Aggiunge interazione, stati (pan mode nasconde parti del template) e superficie di revisione per valore zero | Nessuna map action strip e nessun `PanModeListener` |
| **Disegnare una mappa finta o tile decorative per "sembrare navigazione"** | Mitigare NF-2 ("only map content") | Tenta di aggirare la policy (rischio di rimozione peggiore di un rifiuto), aggiunge rumore visivo contro il Core Value e richiede tile e licenze | La decisione di distribuzione si prende apertamente (vedi STACK/PITFALLS); la Surface contiene solo velocità e stato |
| **Chiamare `NavigationManager.navigationStarted()`** | "Siamo un'app NAVIGATION" | Senza guida turn-by-turn reale fa sì che l'host tratti l'app come navigazione attiva (NF-5: sospende le indicazioni di altre app di navigazione come Google Maps). Danneggia l'utente che usa Maps in parallelo | Non chiamarlo mai. L'app è solo un display |
| **Supporto cluster display** (`FEATURE_CLUSTER`, NF-9) | "La velocità nel quadro strumenti sarebbe perfetta" | Opt-in separato, sul cluster NF-9 vuole solo tile di mappa, e il veicolo ha già il suo tachimetro nel quadro | Fuori scope |
| **Velocità massima o distanza sullo schermo auto** | "Ora c'è spazio" | Fuori scope di milestone già deciso; più numeri riducono la leggibilità a colpo d'occhio | Restano solo sul telefono |
| **Tema sempre scuro fisso senza scelta utente** | Il telefono è sempre nero su bianco | La guida richiede tema chiaro di default più supporto scuro; il tema fisso è ammesso solo come preferenza utente, cioè un'impostazione che l'app non vuole avere | Seguire `isDarkMode`. Nella pratica molte installazioni AA stanno quasi sempre in scuro (LOW confidence), quindi l'aspetto "telefono" è quello più visto |
| **Transizioni animate tra valori, gauge circolari, lancette** | "Più bello" | SA-1 (niente animazioni) e vincolo di progetto (niente grafici) | Numero secco |
| **Toccare la Surface per azioni (`onClick`)** | Scorciatoia per l'azione permesso | `onClick` richiede Car API 5, "may not be called in some car systems", e non funziona con i controller rotativi | Azioni solo nell'action strip |
| **`VirtualDisplay` + `Presentation` con layout XML** | Riusare le `View` del telefono | Più parti in movimento (display virtuale, lifecycle, dismiss) per disegnare due stringhe; un autosize di `TextView` su un display virtuale non risolve la geometria della stable area | Canvas diretto con misura via `Paint` |

## Feature Dependencies

```
Rendering Surface (numero centrato + km/h)
    └──requires──> Template mappa con SurfaceCallback (NavigationTemplate)
                       └──requires──> Categoria NAVIGATION + permessi NAVIGATION_TEMPLATES, ACCESS_SURFACE
                                          (decisione di categoria/distribuzione: STACK/PITFALLS)
                       └──requires──> ActionStrip con >=1 Action (obbligo build())
    └──requires──> Stable area + dpi della Surface (SurfaceCallback)
    └──requires──> GpsSpeedProvider Application-scoped + StateFlow<SpeedState>   [ESISTENTE v2.0]
    └──requires──> carSpeedContent(SpeedState) / CarSpeedContent                  [ESISTENTE v2.0]

Stato "Ricerca segnale..." su Surface
    └──requires──> Rendering Surface (stesso renderer, ramo testuale)
    └──requires──> stringa car_searching_gps_signal                               [ESISTENTE v2.0]

Stato permesso su Surface
    └──requires──> Rendering Surface (messaggio disegnato)
    └──requires──> ActionStrip (azione "Concedi"/"Impostazioni", FLAG_IS_PERSISTENT, Car API 5)
    └──requires──> CarPermissionState / CarPermissionDenialStore / requestPermissions flow [ESISTENTE Fase 9]

Day/night
    └──requires──> Rendering Surface + onCarConfigurationChanged

Dimensione stabile / layout su stable area / margini ──enhances──> Rendering Surface

MapWithContentTemplate (POI) ──conflicts──> visual spec D-14 (card obbligatoria sovrapposta)
NavigationManager.navigationStarted() ──conflicts──> Google Maps in parallelo (NF-5)
Push di MessageTemplate per il permesso ──conflicts (debolmente)──> "Surface sempre visibile" + quota template
```

### Dependency Notes

- **Il renderer Surface richiede prima la scelta del template, che richiede prima la scelta della categoria.** È la dipendenza di ordinamento più forte per la roadmap: la decisione NAVIGATION vs POI (con `MapWithContentTemplate`) va chiusa prima di scrivere il renderer, perché cambia la geometria disponibile (tutto schermo contro schermo meno la card).
- **L'action strip obbligatoria serve anche al permesso.** In stato normale basta un'azione minima. Candidati da decidere in requirements: (a) un'azione "Esci" (`carContext.finishCarApp()`); (b) un'azione informativa innocua. Con il permesso mancante l'azione diventa "Concedi"/"Apri impostazioni" persistente. Unificare i due casi in una sola `ActionStrip` costruita dallo stato evita cambi di template e quindi consumo di quota.
- **Il riuso del modello di stato è totale.** `SpeedState`, `CarSpeedContent`, `CarPermissionState` non cambiano; cambia solo il "sink" (Canvas invece di `PaneTemplate`). Il rischio di regressione logica è basso e i test JVM esistenti restano validi.
- **Pulizie minori** (`isDeviceCharging()` e `deriveChargingState()`, finestra transitoria di `carLink`) sono indipendenti dal renderer e possono stare in qualunque fase.

## MVP Definition

### Launch With (v2.1)

- [ ] `NavigationTemplate` più `SurfaceCallback` al posto del `PaneTemplate`. Essenziale perché è l'unica via verificata alla visual spec D-14 (nessuna card, nessuna icona forzata)
- [ ] Action strip con una sola azione, stabile nel tempo. Essenziale perché senza di essa `build()` lancia un'eccezione
- [ ] Numero centrato nella stable area, dimensione calcolata dalla geometria, stabile tra 1 e 3 cifre. Essenziale per il Core Value
- [ ] "km/h" in basso a destra. Essenziale per la visual spec
- [ ] Ridisegno a 1 Hz più ridisegno immediato su nuova Surface, cambio di stable area e cambio day/night. Essenziale per evitare uno schermo nero o obsoleto
- [ ] Tema scuro e chiaro da `isDarkMode`. Essenziale per MR-1 e perché altrimenti lo schermo sembra rotto
- [ ] "Ricerca segnale..." disegnato sulla Surface. Essenziale per non regredire AA-02
- [ ] Flusso permesso preservato (messaggio sulla Surface più azione persistente nella strip). Essenziale per non regredire AA-04
- [ ] Verifica DHU su almeno 800x480@160, 1280x720 e 1920x1080 (più un profilo largo con margini). Essenziale perché il layout in pixel è il rischio principale della milestone

### Add After Validation (v2.1.x)

- [ ] Cifre tabulari e affinamento dei margini dopo una prova su head unit reale. Trigger: numero che "balla" o "km/h" tagliata sull'auto dell'utente
- [ ] Rivalutare il fallback POI con `MapWithContentTemplate`. Trigger: la categoria NAVIGATION si rivela impraticabile in revisione produzione

### Future Consideration

- [ ] Cluster display. Rimandato perché è opt-in, NF-9 vuole solo mappa e il veicolo ha già il suo tachimetro
- [ ] Android Automotive OS nativo. Resta fuori scope come in v2.0

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| NavigationTemplate + SurfaceCallback | HIGH | MEDIUM | P1 |
| Action strip minima obbligatoria | LOW (obbligo) | LOW | P1 |
| Numero centrato su stable area, dimensionamento geometrico | HIGH | MEDIUM | P1 |
| "km/h" in basso a destra | MEDIUM | LOW | P1 |
| Gestione lifecycle Surface + ridisegni | HIGH (evita schermo nero) | MEDIUM | P1 |
| Day/night | MEDIUM | LOW-MEDIUM | P1 |
| "Ricerca segnale..." su Surface | HIGH | LOW | P1 |
| Permesso su Surface + azione persistente | MEDIUM (edge case) | MEDIUM | P1 |
| Dimensione stabile tra 1-3 cifre / cifre tabulari | MEDIUM | LOW | P2 |
| Funzioni pure di layout con test JVM | MEDIUM (qualità) | LOW-MEDIUM | P2 |
| Fallback POI + MapWithContentTemplate | LOW (degrada la spec) | MEDIUM | P3 |
| Cluster display | LOW | HIGH | P3 |

## Competitor / Precedent Analysis

Non esiste un tachimetro "solo numero" su Android Auto distribuito sul Play Store che sia verificabile. I precedenti utili sono indiretti.

| Aspetto | Google Maps / Waze su AA | aa-torque / obd2aa | Tachimetro v2.1 |
|---------|--------------------------|--------------------|-----------------|
| Dove sta la velocità | Badge piccolo in un angolo della mappa (Maps l'ha aggiunto sul modello di Waze). È il pattern "velocità come info accessoria sulla mappa" che NF-2 e Design for Driving esplicitamente permettono | Gauge custom a tutto schermo | Numero come contenuto principale e unico della Surface |
| Template | Host o nativi (app Google) | Surface custom, fuori dalle regole Play | `NavigationTemplate` + Surface |
| Distribuzione | Play | **Solo sideload** ("Unknown sources" di AA) | Play (canale test aperto già attivo); revisione produzione NAVIGATION da valutare |
| Action strip / chrome | Ricchi (ricerca, impostazioni, pan) | Minimali | Una sola azione, auto-hide |

**Conclusione:** l'approccio v2.1 riprende la tecnica di rendering di aa-torque (Surface custom) dentro un template sanzionato. Il contenuto "velocità corrente" è esplicitamente citato come ammesso sulla Surface dalle guide ufficiali. Il rischio residuo è di categoria (NF-1), non di feature.

## Sources

- Sorgenti `androidx/androidx` (branch `androidx-main`, scaricati 2026-09-23): `car/app/app/src/main/java/androidx/car/app/navigation/model/NavigationTemplate.java` (action strip obbligatorio, pan opzionale, refresh fuori quota e reset quota), `MapWithContentTemplate.java` (`@RequiresCarApi(7)`, `NAVIGATION_TEMPLATES` o `MAP_TEMPLATES`, content template validato in `build()`), `MapController.java`, `SurfaceCallback.java` (visible/stable area, gesture `default` vuote, `onClick` API 5), `model/ActionStrip.java` ("must contain at least one action"), `model/Action.java` (`FLAG_IS_PERSISTENT`, API 5), `model/MessageTemplate.java` (message non vuoto), `Screen.java` (quota di 5 template, refresh). HIGH
- [Draw maps (Android for Cars)](https://developer.android.com/training/cars/apps/library/draw-maps): tabella template/permessi/categorie, `ACCESS_SURFACE`, `SurfaceContainer` (width/height/dpi), visible e stable area, dark theme. HIGH
- [Car app quality guidelines](https://developer.android.com/docs/quality-guidelines/car-app-quality): NF-1, NF-2, NF-5, NF-9, MR-1, VD-1, TH-1, SA-1, PF-1, UX-3. HIGH
- [Build a POI app](https://developer.android.com/training/cars/apps/poi): `MapWithContentTemplate` e `MAP_TEMPLATES` per POI. HIGH
- [Design for Driving: Navigation template](https://developers.google.com/cars/design/create-apps/apps-for-drivers/templates/navigation-template): action strip con almeno un'azione; "MAY draw ... current speed, speed limit". MEDIUM-HIGH
- [Design for Driving: Action strip](https://developers.google.com/cars/design/create-apps/apps-for-drivers/components/action-strip): auto-hide dopo 10 s, riapparizione al tocco, azioni persistenti. MEDIUM-HIGH
- [Test using the Desktop Head Unit](https://developer.android.com/training/cars/testing/dhu): configurazione di risoluzione e dpi per i test. MEDIUM
- [XDA: Headunit resolution](https://xdaforums.com/t/headunit-resolution.4397519/): risoluzioni AA 800x480, 1280x720, 1920x1080. LOW-MEDIUM (forum)
- [Android Authority: Google Maps on Android Auto live speedometer](https://www.androidauthority.com/google-maps-android-auto-live-speedometer-3689236/): precedente del badge velocità. MEDIUM
- [github.com/agronick/aa-torque](https://github.com/agronick/aa-torque): precedente Surface custom solo sideload (da ricerca v2.0). HIGH
- [Community AA: MapWithContentTemplate in POI](https://support.google.com/androidauto/thread/284127264/can-i-use-mapwithcontenttemplate-in-poi-category-app-in-android-auto?hl=en): contenuto del thread non recuperabile. Nessuna evidenza di esito

---
*Feature research for: Tachimetro v2.1 — Velocità a tutto schermo su Android Auto (rendering su Surface)*
*Researched: 2026-09-23*
