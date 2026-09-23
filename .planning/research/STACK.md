# Stack Research — v2.1 Velocità a tutto schermo su Android Auto

**Domain:** Rendering diretto su `Surface` Android Auto (Car App Library) per un'app tachimetro già esistente (Kotlin, XML, categoria `POI`, `PaneTemplate` a 1 Hz)
**Researched:** 2026-09-23
**Confidence:** HIGH sui fatti di libreria/API/manifest (verificati su sorgente AndroidX e documentazione ufficiale); MEDIUM sulla decisione di categoria (dipende dalla revisione manuale di Google, non deterministica); MEDIUM-LOW sull'aspetto visivo esatto di `MapWithContentTemplate` su Android Auto (va verificato su DHU con uno spike)

> Base: `.planning/milestones/v2.0-research/STACK.md` (confronto PaneTemplate vs NavigationTemplate, quota dei template, integrazione `GpsSpeedProvider` Application-scoped). Qui NON si ripete: si corregge e si estende solo ciò che serve per la v2.1.

## Risposta breve alla domanda chiave

**Sì, un'app `POI` può disegnare sulla `Surface`**, senza passare a `NAVIGATION`, usando `MapWithContentTemplate` + `SurfaceCallback`, con i permessi `androidx.car.app.MAP_TEMPLATES` + `androidx.car.app.ACCESS_SURFACE`, su host con Car API level >= 7. È tutto già dentro `androidx.car.app:app:1.7.0`, che il progetto usa già: **nessuna nuova dipendenza.**

**Il prezzo da pagare (strutturale, non aggirabile):** `MapWithContentTemplate` **richiede obbligatoriamente** un template di contenuto (`MessageTemplate`/`PaneTemplate`/`ListTemplate`/`GridTemplate`) che l'host disegna come pannello sovrapposto alla Surface. Quindi con POI si ottiene "numero grande e centrato **nell'area visibile** della Surface, accanto a una card host-controlled", non "numero a tutto schermo senza nient'altro". Il tutto-schermo quasi puro (solo una piccola action strip) è ottenibile solo con `NavigationTemplate` → categoria `NAVIGATION`, che fallisce in modo deterministico la revisione Play (NF-1: navigazione turn-by-turn obbligatoria).

**Raccomandazione:** restare `POI`, passare a `MapWithContentTemplate` + Canvas sulla Surface, usare il pannello di contenuto obbligatorio come contenitore *statico* per il flusso permesso/stati (dove oggi c'è già un template host), fallback a runtime al `PaneTemplate` attuale se l'host ha Car API level < 7. **Non** passare a `NAVIGATION`.

## Fatti verificati che guidano la raccomandazione

1. **Categorie che possono avere una Surface: NAVIGATION, POI, WEATHER.** "Navigation, point of interest (POI), and weather apps using the following templates can draw maps by accessing a Surface." Tabella ufficiale: `MapWithContentTemplate` → permesso `NAVIGATION_TEMPLATES` **o** `MAP_TEMPLATES` → app Navigation, POI, Weather. In più serve sempre `ACCESS_SURFACE`. — [draw-maps](https://developer.android.com/training/cars/apps/library/draw-maps), HIGH.
   - Nota di correzione rispetto alla ricerca v2.0: lì `MapWithContentTemplate` era scartato come "non risolve il mismatch di categoria". Ma per la v2.1 il mismatch di categoria c'è **già** (l'app è `POI` in v2.0 ed è distribuita) — quindi `MapWithContentTemplate` non introduce un nuovo mismatch, mentre `NAVIGATION` sì.
2. **`MapWithContentTemplate` è `@RequiresCarApi(7)`**, introdotto in `1.7.0-alpha01` (2024-04-03), incluso nella 1.7.0 stabile. Javadoc sorgente: "In order to use this template your car app MUST declare ... EITHER the `NAVIGATION_TEMPLATES` permission OR the `MAP_TEMPLATES`". — sorgente AndroidX `car/app/app/.../navigation/model/MapWithContentTemplate.java`, HIGH.
3. **Il contenuto è obbligatorio.** `Builder.build()` esegue `ContentTemplateConstraints.MAP_WITH_CONTENT_TEMPLATE_CONSTRAINTS_API_8.validateOrThrow(mContentTemplate)` e lancia `IllegalArgumentException` se il contenuto non è uno fra `GridTemplate`, `MessageTemplate`, `ListTemplate`, `PaneTemplate` (+ `SectionedItemTemplate` da API 8). Il default del builder è un `Template` anonimo che NON passa la validazione → non esiste una modalità "solo mappa". "The content is usually rendered as an overlay on top of the map tiles, with the map visible and stable areas adjusting to the content." — sorgente AndroidX, HIGH.
4. **`MAP_TEMPLATES` è vincolato alla categoria**: "An app not in one of those categories requesting this permission may be rejected upon submission." — sorgente `CarAppPermission.java`, HIGH. POI è una di quelle categorie ([poi](https://developer.android.com/training/cars/apps/poi): POI "can use ... `MapWithContentTemplate` - For displaying lists and other content alongside an app-rendered map").
5. **`NavigationTemplate.build()` lancia `IllegalStateException` se manca l'`ActionStrip`** ("Action strip for this template must be set"), non ha card obbligatorie → è l'unico modo per un tutto-schermo quasi pulito, ma richiede `NAVIGATION_TEMPLATES` (solo categoria `NAVIGATION`). — sorgente AndroidX, HIGH.
6. **Linee guida di qualità (car-app-quality):**
   - `PC-1`: "The app must not include features outside the app types intended for cars."
   - `PF-1` (POI): "The app must provide meaningful functionality relevant to driving." — unico requisito funzionale POI; un tachimetro è difendibile qui.
   - `NF-1` (Navigation): "The app must provide turn-by-turn navigation directions." + `NF-6` (gestire intent di navigazione da altre app). **Tachimetro non può soddisfarli → rifiuto certo in revisione bloccante.**
   - `NF-2` (Navigation): "The app draws only map content on the surface of the navigation templates ... speed limit ... can be drawn on the safe area of the map." — vincolo esplicito "solo mappa" **solo per i template di navigazione**; nessuna regola equivalente trovata per POI.
   - `MR-1`: le app che disegnano mappe devono disegnare tema chiaro/scuro "when instructed" (→ rispettare `CarContext.isDarkMode`; ammesso anche lasciar scegliere all'utente un tema fisso).
   - `SA-1`: niente elementi animati. Aggiornare il numero 1 volta/sec non è un'animazione; **non** introdurre transizioni/interpolazioni del numero sull'auto.
   — [car-app-quality](https://developer.android.com/docs/quality-guidelines/car-app-quality), HIGH (testo), MEDIUM (applicazione reale da parte dei revisori).
7. **Revisione per track** — [distribute](https://developer.android.com/training/cars/distribute), HIGH:

   | Track | Revisione form factor |
   |---|---|
   | Internal sharing / Internal testing | Nessuna |
   | Closed testing | Non bloccante (notifica, ma approvata) |
   | **Open testing** | **Bloccante** |
   | Production | Bloccante |

   E dalla FAQ di car-app-quality: se l'app non passa, "Any subsequent updates are not available for distribution until the app is approved" — **anche per il telefono.** Poiché la 2.0 è oggi sul **canale test aperto** (bloccante), un passaggio a `NAVIGATION` rischierebbe di congelare anche gli aggiornamenti lato telefono.
8. **Nessuna categoria "strumenti di guida/tachimetro" esiste.** Le categorie supportate sono Media, Messaging, Calling, Navigation, POI, IoT, Weather (+ Video/Games/Browsers solo da fermi). — [training/cars](https://developer.android.com/training/cars), HIGH. WEATHER è peggiore di POI per semantica (l'app non mostra alcun dato meteo, requisiti `WE-*` specifici sul meteo).

## Recommended Stack

### Core Technologies

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| `androidx.car.app:app` | **1.7.0** (già presente, invariata) | `MapWithContentTemplate`, `SurfaceCallback`, `AppManager.setSurfaceCallback`, `SurfaceContainer`, `CarAppPermission.MAP_TEMPLATES`/`ACCESS_SURFACE` | Tutto ciò che serve è già nella 1.7.0 stabile (2025-07-16). Nessun motivo funzionale per cambiare versione in questa milestone. Vedi "Version Compatibility" per la nota di sicurezza su 1.8.0-rc01. |
| `android.graphics.Canvas` / `Paint` / `Surface.lockHardwareCanvas()` (framework) | API 30+ (minSdk) | Disegno del numero e di "km/h" sulla Surface | Canvas diretto è il percorso più semplice indicato dalla doc ufficiale (alternative: `VirtualDisplay`+`Presentation`, `ComposeView`). Per un numero e un'etichetta non serve un sistema di View. `lockHardwareCanvas()` (API 23+) evita il rendering software. |
| `Typeface.create(Typeface.DEFAULT, 900, false)` (framework, API 28+) | API 30+ | Peso Black del font di sistema, coerente con il telefono | Stessa identità visiva del telefono ("font di sistema Bold/Black") senza asset font aggiuntivi. |

### Supporting Libraries

**Nessuna nuova libreria.** Riutilizzo integrale di: Kotlin Coroutines 1.10.2, Lifecycle Runtime 2.11.0 (`Screen` è `LifecycleOwner`, il pattern `repeatOnLifecycle(STARTED)` di `SpeedScreen` resta invariato), `GpsSpeedProvider` Application-scoped, `CarPermissionState`/`CarPermissionDenialStore`, `CarHostValidation`.

### Manifest (unica modifica di configurazione)

| Declaration | Valore | Perché |
|-------------|--------|--------|
| `<uses-permission android:name="androidx.car.app.MAP_TEMPLATES"/>` | **NUOVO** | Richiesto per `MapWithContentTemplate` in categoria POI (in alternativa a `NAVIGATION_TEMPLATES`, che invece è riservato alla categoria NAVIGATION). |
| `<uses-permission android:name="androidx.car.app.ACCESS_SURFACE"/>` | **NUOVO** | Senza, `AppManager.setSurfaceCallback()` lancia `SecurityException`. |
| `<category android:name="androidx.car.app.category.POI"/>` | **INVARIATO** | Nessun cambio di categoria. |
| `androidx.car.app.minCarApiLevel` | **INVARIATO a `1`** | Non alzare a 7: si fa check a runtime `carContext.carAppApiLevel >= CarAppApiLevels.LEVEL_7` e si ricade sul `PaneTemplate` esistente per host più vecchi. Evita di escludere head unit/versioni di Android Auto datate per un guadagno solo estetico. |
| Nessun nuovo permesso Android (location, foreground service, ecc.) | — | La Surface non richiede altro; `ACCESS_FINE_LOCATION` resta l'unico permesso di sistema (vincolo v2.0 D-03/D-04 preservato). |

Nota Data safety / scheda Play: i permessi `androidx.car.app.*` non sono permessi di runtime Android e non toccano dati utente; nessun impatto sulla sezione Data safety (MEDIUM — deduzione, non trovata una dichiarazione esplicita).

### Development Tools

| Tool | Purpose | Notes |
|------|---------|-------|
| Desktop Head Unit (DHU) | Spike iniziale obbligatorio: vedere DOVE l'host posiziona la card di contenuto e quanto vale `onVisibleAreaChanged`/`onStableAreaChanged` | Provare più risoluzioni DHU (es. 800x480 e 1920x720 widescreen): la card cambia posizione/dimensione. Il flusso DHU su telefono fisico è già collaudato (Fase 8, D-11). |
| Play Console Internal testing / Internal App Sharing | Prova su head unit reale senza revisione | Come in v2.0. Non pubblicare esperimenti di categoria sul track aperto (revisione bloccante). |

## Installation

Nessuna nuova dipendenza Gradle. Solo manifest:

```xml
<!-- app/src/main/AndroidManifest.xml -->
<uses-permission android:name="androidx.car.app.MAP_TEMPLATES" />
<uses-permission android:name="androidx.car.app.ACCESS_SURFACE" />
<!-- service TachimetroCarAppService: category resta androidx.car.app.category.POI -->
<!-- meta-data androidx.car.app.minCarApiLevel resta 1 -->
```

## Punti di integrazione con il codice car esistente

- **`SpeedScreen.onGetTemplate()`**: ramo `carAppApiLevel >= 7` → `MapWithContentTemplate.Builder().setContentTemplate(<contenuto statico>).build()`; altrimenti il `PaneTemplate` attuale (codice v2.0 riusato così com'è come fallback).
- **Cadenza 1 Hz**: con la Surface il numero NON passa più da `invalidate()`. La collect su `GpsSpeedProvider.state` chiama un `redraw()` sul Canvas; `invalidate()` si usa solo quando cambia il *contenuto della card* (cambio di `CarPermissionState`, Searching ↔ Reading). Così il template resta praticamente statico e la quota dei template diventa irrilevante (la doc v2.0 sulla quota resta valida per i template, non per i frame della Surface).
- **`SurfaceCallback`** registrato con `carContext.getCarService(AppManager::class.java).setSurfaceCallback(cb)` in `Screen`/`Session` all'avvio; ridisegnare su `onSurfaceAvailable`, `onVisibleAreaChanged`, `onStableAreaChanged` e su ogni emissione di stato; rilasciare su `onSurfaceDestroyed` (non disegnare dopo). I callback arrivano sul main thread; a 1 Hz disegnare sul main thread è accettabile.
- **Geometria**: centrare il numero nel `stableArea` (sempre visibile) o nel `visibleArea`, non nell'intera Surface — la card lo coprirebbe. "km/h" in basso a destra dello stesso rettangolo. Usare `SurfaceContainer.dpi` per le dimensioni minime. Isolare il calcolo in una **funzione pura** (es. `computeCarSpeedLayout(area: RectLike, dpi: Int, text: String): CarSpeedLayout`) testabile su JVM come `CarSpeedContent` — convenzione di progetto.
- **Card di contenuto obbligatoria**: usarla per ciò che già oggi è host-controlled — stato "Ricerca segnale..." / "Controlla il telefono" / permesso negato con Action riprova/impostazioni (`MessageTemplate` o `PaneTemplate`). Il flusso permesso della Fase 9 (`CarContext.requestPermissions`, `ParkedOnlyOnClickListener`) si sposta dentro la card senza riscriverne la logica. Da decidere in fase di requisiti cosa mostra la card nello stato Reading (vincolo: `MessageTemplate` richiede un messaggio non vuoto; `PaneTemplate` richiede titolo o header action, come già scoperto in D-12).
- **Tema**: leggere `carContext.isDarkMode` (MR-1). Proposta coerente con il Core Value: nero/bianco in dark, bianco/nero in light, nessun colore accento.

## Alternatives Considered

| Recommended | Alternative | When to Use Alternative |
|-------------|-------------|-------------------------|
| POI + `MapWithContentTemplate` + Canvas | NAVIGATION + `NavigationTemplate` + Canvas (quasi tutto schermo, solo action strip obbligatoria) | Solo per uso personale via Internal testing / Internal App Sharing, mai su open/production: NF-1/NF-6 non soddisfacibili → rifiuto bloccante che congela anche gli aggiornamenti telefono. Se l'utente vuole *davvero* lo schermo 100% pulito, l'unica via "legale" è una build separata (flavor) distribuita solo su track interno. |
| POI + `MapWithContentTemplate` | WEATHER + `MapWithContentTemplate` | Mai: stessa capacità tecnica di POI, ma mismatch semantico più evidente (nessun dato meteo) e cambio di categoria da rivalutare in revisione. |
| Canvas diretto | `VirtualDisplay` + `Presentation` con layout XML | Solo se il contenuto sulla Surface diventasse complesso (più elementi, layout adattivo). Per numero + etichetta è sovradimensionato. |
| Canvas diretto | `ComposeView` sulla Surface | Mai: il progetto vieta Compose. |
| `app:1.7.0` | `app:1.8.0-rc01` (2026-08-26) | Valutare quando esce la 1.8.0 stabile: rc01 dichiara "includes a security fix. If you are using a lower version, please update". Non necessaria per le feature v2.1; minSdk della libreria sale a 23 (nessun impatto con minSdk 30). |
| Fallback `PaneTemplate` per API < 7 | Alzare `minCarApiLevel` a 7 | Solo se lo spike su head unit reale mostra che il fallback complica troppo `SpeedScreen`. |

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| Categoria `NAVIGATION` / permesso `NAVIGATION_TEMPLATES` nella build distribuita sul track aperto | NF-1 (turn-by-turn) e NF-6 (intent di navigazione) non soddisfacibili; revisione bloccante su open testing e produzione; un rifiuto blocca **tutti** gli aggiornamenti, anche quelli solo-telefono | POI + `MAP_TEMPLATES` |
| Google Maps SDK / Mapbox / qualsiasi SDK di mappe | L'app non disegna mappe; `MapWithContentTemplate` non richiede mappe reali, solo pixel sulla Surface. Aggiungerebbe dipendenze pesanti e API key | Canvas del framework |
| `androidx.car.app:app-projected` / `app-automotive` | Non servono per Android Auto con Surface (confermato in v2.0) | Solo `androidx.car.app:app` |
| `PlaceListMapTemplate` (POI) | Mappa disegnata dall'host, nessun accesso alla Surface | `MapWithContentTemplate` |
| `MapTemplate`, `PlaceListNavigationTemplate`, `RoutePreviewNavigationTemplate` | Deprecati da Car API 7 e comunque solo `NAVIGATION_TEMPLATES` | `MapWithContentTemplate` |
| `invalidate()` a 1 Hz per aggiornare il numero | Con la Surface è inutile e ripropone la quota dei template | Ridisegno della Surface; `invalidate()` solo su cambio di contenuto della card |
| Animazioni/transizioni del numero sull'auto | `SA-1` vieta elementi animati (eccezione solo da fermi) | Ridisegno secco del nuovo valore |
| Un secondo `GpsSpeedProvider` o un timer lato auto | Già vietato in v2.0 (sottoscrizione unica Application-scoped, cadenza dal ticker del provider) | `provider.gpsSpeedProvider.state` esistente |

## Stack Patterns by Variant

**Se l'host riporta Car API level >= 7 (caso atteso su Android Auto recente):**
- `MapWithContentTemplate` + `SurfaceCallback` + Canvas, card con stato/permesso.

**Se l'host riporta Car API level < 7:**
- `PaneTemplate` v2.0 invariato (layout host-controlled, già validato su DHU e head unit reale).

**Se l'utente, dopo lo spike DHU, giudica la card POI inaccettabile e vuole lo schermo 100% pulito:**
- Unica opzione: `NavigationTemplate` in un flavor separato distribuito solo su Internal testing/Internal App Sharing, mai sul track aperto/produzione. Decisione esplicita da registrare in Key Decisions, non implicita in un piano di fase.

## Version Compatibility

| Package A | Compatible With | Notes |
|-----------|-----------------|-------|
| `androidx.car.app:app:1.7.0` | `MapWithContentTemplate` (Car API 7), `MAP_TEMPLATES`, `ACCESS_SURFACE`, `SurfaceCallback.onClick` (Car API 5) | Tutto disponibile; requisito lato host = Car API 7 per il template, verificato a runtime. |
| `androidx.car.app:app:1.7.0` | minSdk 30, compileSdk 36, AGP 9.3.2 | Invariato rispetto a v2.0. |
| `androidx.car.app:app:1.8.0-rc01` | minSdk 30 | Libreria minSdk 23 da 1.8.0-alpha03: nessun conflitto. Aggiunge Car API level 8 (`SectionedItemTemplate` come contenuto) — non necessario. Contiene una fix di sicurezza non dettagliata nelle release notes (MEDIUM): tenerla d'occhio per la 1.8.0 stabile. |
| `Typeface.create(Typeface, Int, Boolean)` | API 28+ | Coperto da minSdk 30. |

## Gap aperti (da chiudere prima/durante la roadmap)

1. **Aspetto reale della card su Android Auto** (posizione, larghezza, presenza di icona/titolo app nella card, comportamento su schermi stretti): non documentato con precisione → spike DHU come primo piano della milestone. Confidence MEDIUM-LOW.
2. **Esito della revisione form factor della 2.0 sul track aperto**: verificare in Play Console se l'app è già passata come POI in revisione bloccante. Se sì, è il dato più forte a favore di POI + `MapWithContentTemplate` (stessa categoria, stessa funzionalità). Non verificabile da qui.
3. **Nessun precedente pubblico trovato** di un'app tachimetro approvata o rifiutata su Android Auto con Surface in categoria POI (thread "Category not permitted" sul forum Play Console esistono ma il contenuto non era leggibile). Rischio di rifiuto per `PC-1`/`PF-1` non azzerabile, ma non più alto di quello già accettato in v2.0.
4. Google ha annunciato (blog 2026-05) l'arrivo dei widget mobile su Android Auto "this year": possibile via alternativa futura per un tachimetro, oggi non documentata → fuori scope, da monitorare.

## Sources

- https://developer.android.com/training/cars/apps/library/draw-maps — categorie NAVIGATION/POI/WEATHER, tabella template→permesso, `ACCESS_SURFACE`, `onVisibleAreaChanged`/`onStableAreaChanged`, opzioni Canvas/VirtualDisplay/Compose — HIGH
- Sorgente AndroidX (`androidx-main`): `car/app/app/src/main/java/androidx/car/app/navigation/model/MapWithContentTemplate.java` (`@RequiresCarApi(7)`, permessi, contenuto obbligatorio), `navigation/model/constraints/ContentTemplateConstraints.java` (template ammessi, eccezione), `navigation/model/NavigationTemplate.java` (ActionStrip obbligatoria, refresh senza quota), `CarAppPermission.java` (`MAP_TEMPLATES` vincolato alla categoria), `SurfaceCallback.java` — HIGH
- https://developer.android.com/training/cars/apps/poi — POI può usare `MapWithContentTemplate` con `MAP_TEMPLATES` — HIGH
- https://developer.android.com/docs/quality-guidelines/car-app-quality — PC-1, PF-1, NF-1/2/6, MR-1, SA-1, FAQ sugli aggiornamenti bloccati — HIGH (testo) / MEDIUM (applicazione)
- https://developer.android.com/training/cars/distribute — revisione per track (open testing bloccante) — HIGH
- https://developer.android.com/training/cars — categorie supportate, nessuna categoria "strumenti di guida" — HIGH
- https://developer.android.com/jetpack/androidx/releases/car-app — 1.7.0 stabile (2025-07-16), 1.8.0-rc01 (2026-08-26, fix di sicurezza), 1.9.0-alpha02 (2026-09-09), `MapWithContent` introdotto in 1.7.0-alpha01 — HIGH
- https://android-developers.googleblog.com/2026/05/android-for-cars-unifying-platforms-premium-experiences.html — Maps SDK per POI/Weather con `MapWithContentTemplate`, widget su Android Auto annunciati — MEDIUM
- https://support.google.com/googleplay/android-developer/thread/318441322 e /thread/232683714 ("Category not permitted") — esistenza di rifiuti per categoria, contenuto non leggibile — LOW
- Lettura diretta: `app/src/main/AndroidManifest.xml`, `gradle/libs.versions.toml`, `app/src/main/java/com/sed/tachimetro/car/SpeedScreen.kt`, `08-CONTEXT.md` (D-12..D-14) — HIGH

---
*Stack research per: Tachimetro v2.1 — velocità a tutto schermo su Android Auto*
*Researched: 2026-09-23*
