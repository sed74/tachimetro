# Architecture Research — v2.1 Velocità a tutto schermo su Android Auto

**Domain:** Rendering diretto sulla Surface Android Auto (Car App Library 1.7.0) integrato nell'architettura car esistente (Session → SpeedScreen → GpsSpeedProvider Application-scoped)
**Researched:** 2026-09-23
**Confidence:** HIGH per API Surface/threading/lifecycle (sorgente AOSP androidx-main + sample ufficiale navigation); MEDIUM per la coesistenza template/surface negli stati di permesso; LOW per il comportamento host-specifico (dissolvenza action strip, surface durante MessageTemplate)

---

## Fatti verificati che vincolano l'architettura

| Fatto | Fonte | Conseguenza architetturale |
|-------|-------|----------------------------|
| `AppManager.setSurfaceCallback()` richiede il permesso manifest `androidx.car.app.ACCESS_SURFACE`, altrimenti `SecurityException` | Javadoc `AppManager.java` (androidx-main) | Il manifest va modificato PRIMA di qualunque codice di rendering |
| "the listener relates to UI events and will be executed on the main thread using `Looper.getMainLooper()`" | Javadoc `AppManager.setSurfaceCallback` | Tutto il pipeline (callback surface, `lifecycleScope`, `GpsSpeedProvider` su `Dispatchers.Main.immediate`) gira sul main thread: nessuna sincronizzazione necessaria |
| `onSurfaceAvailable` "may be called multiple times if the surface changes characteristics" ed "è garantito prima di ogni altro metodo" | Javadoc `SurfaceCallback.java` | Il renderer deve essere idempotente su `onSurfaceAvailable` e gestire il cambio dimensione/DPI |
| "every instance of Surface received through this method must be released by calling `Surface#release()`" | Javadoc `SurfaceCallback.onSurfaceAvailable` | Rilascio esplicito in `onSurfaceDestroyed` e alla sostituzione |
| `visibleArea`/`stableArea` possono essere `Rect.isEmpty()` = "currently unknown" | Javadoc `SurfaceCallback` | La funzione pura di scelta dell'area deve avere un fallback all'intera surface |
| `NavigationTemplate.Builder.build()` lancia `IllegalStateException` se manca l'`ActionStrip` | `NavigationTemplate.java` | Con categoria NAVIGATION almeno un'azione è sempre visibile: l'area utile per il numero è la `stableArea`/`visibleArea`, non l'intera surface |
| `NavigationTemplate` "supports any content changes as refreshes" e "the host will reset the template quota once an app reaches this template" | `NavigationTemplate.java` | Il passaggio MessageTemplate (permesso) → NavigationTemplate (velocità) non esaurisce la quota di step |
| `MapWithContentTemplate` è `@RequiresCarApi(7)`, richiede `NAVIGATION_TEMPLATES` **oppure** `MAP_TEMPLATES`, e `build()` valida che il content sia List/Pane/Grid/Message (Sectioned da API 8) | `MapWithContentTemplate.java`, `ContentTemplateConstraints` | Percorso POI possibile ma: `minCarApiLevel` va alzato da 1 a 7 e un template di contenuto **sovrapposto** alla surface è obbligatorio (riduce l'area visibile) |
| Sample ufficiale `navigation/SurfaceRenderer`: `DefaultLifecycleObserver` sul lifecycle della Session, `setSurfaceCallback` in `onCreate`, `lockCanvas(null)`/`unlockCanvasAndPost` sul main thread, redraw coalescato via `Handler(mainLooper)`, `isValid()` prima di disegnare, `onCarConfigurationChanged()` → redraw | androidx-main `car/app/app-samples/navigation/.../SurfaceRenderer.java` | Pattern di riferimento da replicare in forma ridotta (nessuna VirtualDisplay/Presentation) |
| `TestAppManager.getSurfaceCallback()` esiste in `app-testing`; `SurfaceContainer(Surface?, w, h, dpi)` ha costruttore pubblico | `TestAppManager.java`, `SurfaceContainer.java` | Il renderer è testabile in androidTest invocando i callback a mano (anche con `surface = null`) |

La scelta di categoria (NAVIGATION + `NavigationTemplate` vs POI + `MapWithContentTemplate`) spetta a STACK.md. **Questa architettura è progettata per essere indifferente alla scelta**: il template "con surface" è costruito in un unico punto sostituibile (vedi Pattern 4).

---

## Standard Architecture

### System Overview

```
┌──────────────────────────────── Processo app (telefono) ─────────────────────────────────┐
│                                                                                           │
│  TachimetroApplication                                                                    │
│   └── gpsSpeedProvider: StateFlow<SpeedState>  (Main.immediate, ticker 1 Hz, WhileSubscribed)
│                │                                                                          │
├────────────────┼──────────────────────── Lato auto (car/) ────────────────────────────────┤
│                │                                                                          │
│  TachimetroCarAppService ──► TachimetroCarSession  [MOD]                                  │
│                               │  lifecycle ──observer──► SpeedSurfaceRenderer [NUOVO]     │
│                               │  onCarConfigurationChanged ─► renderer.requestRender()    │
│                               └─ onCreateScreen ─► SpeedScreen(carContext, renderer) [MOD]│
│                                                                                           │
│  SpeedScreen [MOD]                                                                        │
│   ├── permissionState: MutableStateFlow<CarPermissionState>   (invariato)                 │
│   ├── collect GPS SOLO nel ramo Granted (T-08-08 invariato)                               │
│   ├── ogni SpeedState ─► renderer.submit(surfaceFrame(permission, state))   (NO invalidate)│
│   └── onGetTemplate() ─► resolveCarScreenMode(permission)                                 │
│          ├── SurfaceMode      ─► template con surface (Navigation / MapWithContent)       │
│          └── PermissionMode   ─► MessageTemplate (+ Action ParkedOnly se Denied)          │
│                                                                                           │
│  SpeedSurfaceRenderer [NUOVO]  implements SurfaceCallback                                 │
│   ├── surface: Surface?, surfaceSize, dpi, stableArea, visibleArea, latestFrame           │
│   ├── requestRender() ─► coalescing su main Handler ─► drawFrame()                        │
│   └── drawFrame(): lockCanvas ─► palette + layout (funzioni pure) ─► drawText ─► post     │
│                                                                                           │
│  Funzioni pure [NUOVE]  (JVM unit test, nessun import android.graphics)                  │
│   ├── surfaceFrame(permission, speed): SurfaceFrame                                       │
│   ├── resolveCarScreenMode(permission): CarScreenMode                                     │
│   ├── resolveDrawArea(surfaceW, surfaceH, stable, visible): AreaPx                        │
│   ├── computeSpeedLayout(area, dpi, metrics): SpeedLayout                                 │
│   └── surfacePalette(isDarkMode): SurfacePalette                                          │
│                                                                                           │
│  Riusati invariati: CarSpeedContent/carSpeedContent, CarPermissionState,                  │
│                     CarPermissionDenialStore, CarHostValidation, TachimetroCarAppService   │
└───────────────────────────────────────────────────────────────────────────────────────────┘
                 ▲ Surface (SurfaceContainer) + Rect visible/stable        │ Template
                 │                                                          ▼
           ┌──────────────────────── Host Android Auto ────────────────────────┐
```

### Component Responsibilities

| Componente | Stato | Responsabilità | Implementazione |
|------------|-------|----------------|-----------------|
| `SpeedSurfaceRenderer` | **NUOVO** (`car/SpeedSurfaceRenderer.kt`) | Possiede la `Surface` corrente, le aree e l'ultimo `SurfaceFrame`; disegna su richiesta; rilascia la Surface | `SurfaceCallback` + `DefaultLifecycleObserver`; `Paint` pre-allocati; solo main thread |
| `SurfaceLayout.kt` | **NUOVO** (`car/SurfaceLayout.kt`) | Matematica di layout pura: area utile, dimensione cifre, posizione unità in basso a destra | Data class `AreaPx`, `TextMetricsRatio`, `SpeedLayout`; funzioni top-level |
| `SurfaceFrame.kt` | **NUOVO** (`car/SurfaceFrame.kt`) | Contenuto da disegnare: `Speed(kmh)`, `Searching`, `Blank` + mapping puro da permesso/velocità | Sealed class + `surfaceFrame()`; riusa `carSpeedContent()` |
| `CarScreenMode.kt` | **NUOVO** (`car/CarScreenMode.kt`) | Decisione pura "template con surface" vs "template di permesso" | Sealed + `resolveCarScreenMode()` |
| `SurfacePalette` | **NUOVO** (in `SurfaceLayout.kt` o file proprio) | Colori sfondo/testo in funzione di `isDarkMode` | Funzione pura che restituisce ARGB `Int` |
| `SpeedScreen` | **MODIFICATO** | Mantiene la macchina a stati del permesso (invariata); nel ramo Granted inoltra lo stato GPS al renderer invece di `invalidate()`; `onGetTemplate()` sceglie il template via `CarScreenMode` | Rimozione di `PaneTemplate`/`Row` del ramo Granted; `buildTemplate()` resta seam di test con tipo di ritorno `Template` |
| `TachimetroCarSession` | **MODIFICATO** | Crea il renderer (una volta per Session), lo aggancia al proprio lifecycle, inoltra `onCarConfigurationChanged`, lo passa allo `SpeedScreen` | ~15 righe |
| `AndroidManifest.xml` | **MODIFICATO** | `ACCESS_SURFACE` + `NAVIGATION_TEMPLATES` o `MAP_TEMPLATES`; categoria; `minCarApiLevel` (7 se `MapWithContentTemplate`) | Dipende dalla decisione di STACK.md |
| `SpeedScreenTemplateTest` (androidTest) | **MODIFICATO** | Le asserzioni su `PaneTemplate`/`Row` diventano asserzioni su tipo di template per stato | Riscrittura parziale |
| `CarSpeedContent` / `carSpeedContent()` | invariato | Resta il mapping SpeedState → contenuto; `surfaceFrame()` lo compone | — |
| `MainActivity` | **MODIFICATO** (pulizie) | `isDeviceCharging()` → `deriveChargingState()`; finestra transitoria di `carLink` | Vedi "Pulizie minori" |

---

## Recommended Project Structure

```
app/src/main/java/com/sed/tachimetro/car/
├── TachimetroCarAppService.kt     # invariato
├── CarHostValidation.kt           # invariato
├── TachimetroCarSession.kt        # MOD: crea/possiede SpeedSurfaceRenderer, inoltra config change
├── SpeedScreen.kt                 # MOD: submit al renderer, scelta template via CarScreenMode
├── SpeedSurfaceRenderer.kt        # NUOVO: SurfaceCallback + draw (unico file con android.graphics)
├── SurfaceFrame.kt                # NUOVO: sealed SurfaceFrame + surfaceFrame() puro
├── SurfaceLayout.kt               # NUOVO: AreaPx, SpeedLayout, resolveDrawArea, computeSpeedLayout, surfacePalette
├── CarScreenMode.kt               # NUOVO: sealed CarScreenMode + resolveCarScreenMode() puro
├── CarSpeedContent.kt             # invariato
├── CarPermissionState.kt          # invariato
├── CarPermissionDenialStore.kt    # invariato
└── CarLinkState.kt                # eventualmente MOD (pulizia carLink, lato telefono)

app/src/test/java/com/sed/tachimetro/car/
├── SurfaceLayoutTest.kt           # NUOVO: area fallback, centratura, unità bottom-right, no overlap
├── SurfaceFrameTest.kt            # NUOVO: tabella permesso × SpeedState → frame
├── CarScreenModeTest.kt           # NUOVO
└── (esistenti invariati)

app/src/androidTest/java/com/sed/tachimetro/car/
├── SpeedScreenTemplateTest.kt     # MOD: tipo di template per stato
└── SpeedSurfaceRendererTest.kt    # NUOVO: callback via TestAppManager, surface null/ImageReader, release
```

### Structure Rationale

- **Tutto resta in `car/`:** coerente con la convenzione feature-package; il renderer è l'unico file nuovo che importa `android.graphics.*`, così la superficie non testabile su JVM è minima e isolata.
- **Funzioni pure in file separati dal renderer:** stessa disciplina di `carSpeedContent()`/`resolveCarLinkState()` — testabili con JUnit puro senza Robolectric (che non è nello stack).
- **Niente `android.graphics.Rect` nelle funzioni pure:** `Rect` nei test JVM è uno stub di `android.jar` (costruttore non funzionante). Si usa una data class `AreaPx(left, top, right, bottom)`; la conversione `Rect → AreaPx` avviene solo nel renderer.

---

## Architectural Patterns

### Pattern 1: Renderer passivo "last-value + redraw on demand"

**What:** Il renderer non osserva flow né conosce `GpsSpeedProvider`. Riceve `submit(frame: SurfaceFrame)` dallo `SpeedScreen`, conserva l'ultimo valore e ridisegna quando cambia *qualunque* input: frame, surface, stableArea/visibleArea, dark mode.
**When to use:** Sempre, per questo progetto. La surface può essere distrutta e ricreata (resize, cambio template, background) in momenti scollegati dall'arrivo dei dati GPS: il renderer deve poter ridisegnare lo stato corrente senza attendere il prossimo tick.
**Trade-offs:** + T-08-08 resta intatto (la collect GPS vive solo nel ramo Granted dello Screen); + il renderer è testabile senza coroutine. − Un campo mutabile in più (`latestFrame`), accettabile perché tutto è main-thread.

```kotlin
class SpeedSurfaceRenderer(private val carContext: CarContext) :
    SurfaceCallback, DefaultLifecycleObserver {

    private var surface: Surface? = null
    private var surfaceW = 0; private var surfaceH = 0; private var dpi = 160
    private var stable = AreaPx.EMPTY; private var visible = AreaPx.EMPTY
    private var latestFrame: SurfaceFrame = SurfaceFrame.Blank
    private val handler = Handler(Looper.getMainLooper())
    private var renderPending = false

    fun submit(frame: SurfaceFrame) {
        if (frame == latestFrame) return          // StateFlow conflate già, doppia guardia economica
        latestFrame = frame
        requestRender()
    }

    fun requestRender() {                          // coalescing: burst available+stable+visible = 1 frame
        if (renderPending) return
        renderPending = true
        handler.post { renderPending = false; drawFrame() }
    }

    override fun onCreate(owner: LifecycleOwner) {
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(this)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        handler.removeCallbacksAndMessages(null)
        releaseSurface()
    }

    override fun onSurfaceAvailable(c: SurfaceContainer) {
        val incoming = c.surface
        if (surface !== incoming) surface?.release()   // può essere richiamato con la STESSA surface
        surface = incoming; surfaceW = c.width; surfaceH = c.height; dpi = c.dpi
        requestRender()
    }
    override fun onStableAreaChanged(r: Rect) { stable = r.toAreaPx(); requestRender() }
    override fun onVisibleAreaChanged(r: Rect) { visible = r.toAreaPx(); requestRender() }
    override fun onSurfaceDestroyed(c: SurfaceContainer) { releaseSurface() }

    private fun releaseSurface() { surface?.release(); surface = null }

    private fun drawFrame() {
        val s = surface ?: return
        if (!s.isValid) return
        val canvas = try { s.lockCanvas(null) } catch (e: IllegalArgumentException) { return }
                     catch (e: IllegalStateException) { return }  // surface invalidata tra isValid e lock
        try {
            val palette = surfacePalette(carContext.isDarkMode)
            canvas.drawColor(palette.background)
            val area = resolveDrawArea(surfaceW, surfaceH, stable, visible)
            when (val f = latestFrame) {
                is SurfaceFrame.Speed -> drawSpeed(canvas, area, f.kmh, palette)
                SurfaceFrame.Searching -> drawMessage(canvas, area, R.string.car_searching_gps_signal, palette)
                SurfaceFrame.Blank -> Unit
            }
        } finally {
            s.unlockCanvasAndPost(canvas)
        }
    }
}
```

### Pattern 2: Layout come funzione pura su rapporti di metrica

**What:** Tutta la geometria (dove va il numero, quanto è grande, dove va "km/h") è calcolata da `computeSpeedLayout()` pura. L'unica dipendenza Android — la misura del testo — viene ridotta a **rapporti** misurati una sola volta dal renderer con `Paint` a una dimensione di riferimento (es. 100 px): larghezza del campione di cifre / textSize, ascent e descent / textSize. La larghezza del testo scala linearmente con `textSize` (con `isLinearText`/`isSubpixelText` attivi l'approssimazione è trascurabile), quindi la funzione pura può risolvere la dimensione ottimale senza `Paint`.
**When to use:** Per ogni calcolo che deciderebbe "cosa appare dove". Il renderer fa solo `Paint.textSize = layout.digitsSizePx; canvas.drawText(..., layout.digitsX, layout.digitsBaselineY, paint)`.
**Trade-offs:** + JVM-testabile (casi: area vuota, surface stretta, visible ≠ stable, DPI alto/basso, 1-2-3 cifre). − Un livello di indirezione in più rispetto a disegnare "a occhio" nel renderer; ripagato dai test.

```kotlin
data class AreaPx(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width get() = right - left; val height get() = bottom - top
    val isEmpty get() = width <= 0 || height <= 0
    companion object { val EMPTY = AreaPx(0, 0, 0, 0) }
}

/** Rapporti misurati una volta dal renderer: valore / textSize. */
data class TextMetricsRatio(val widthPerPx: Float, val ascentPerPx: Float, val descentPerPx: Float)

data class SpeedLayout(
    val digitsSizePx: Float, val digitsCenterX: Float, val digitsBaselineY: Float,
    val unitSizePx: Float, val unitRightX: Float, val unitBaselineY: Float,
)

/** stable (conservativa, non salta quando l'action strip compare/scompare) → visible → intera surface. */
fun resolveDrawArea(surfaceW: Int, surfaceH: Int, stable: AreaPx, visible: AreaPx): AreaPx = when {
    !stable.isEmpty -> stable
    !visible.isEmpty -> visible
    else -> AreaPx(0, 0, surfaceW, surfaceH)
}

fun computeSpeedLayout(
    area: AreaPx, dpi: Int, digits: TextMetricsRatio, unit: TextMetricsRatio,
    unitSizeDp: Float = 28f, marginDp: Float = 12f, fillRatio: Float = 0.9f,
): SpeedLayout { /* banda unità in basso riservata simmetricamente in alto → cifre centrate
                    senza sovrapposizione; size = min(boxW / widthPerPx, boxH / (ascent+descent)) */ }
```

Decisione UX da confermare in FEATURES/fase: dimensionare le cifre sul campione **"888"** (dimensione stabile, nessun "salto" a 99→100 km/h) invece che sul valore corrente (autosize per valore, come il telefono). Raccomandazione: campione a 3 cifre — a colpo d'occhio un numero che cambia dimensione è una distrazione. Il parametro è già nel contratto (`digits: TextMetricsRatio` misurato sul campione scelto).

### Pattern 3: Stato "cosa disegnare" come sealed puro (`SurfaceFrame`)

**What:** `surfaceFrame(permission: CarPermissionState, speed: SpeedState): SurfaceFrame` — `Granted + Reading → Speed(kmh)`, `Granted + Searching/NoSignal → Searching` (via `carSpeedContent()`), qualunque stato non-Granted → `Blank`.
**When to use:** È il contratto fra `SpeedScreen` e renderer; `when` esaustivo senza `else`, come `carSpeedContent()`.
**Trade-offs:** + la regola "mai un valore vecchio sullo schermo auto" (AA-02) resta codificata in una funzione pura testata; + all'uscita da Granted lo Screen invia esplicitamente `Blank`, così una surface che resta visibile non mostra una velocità congelata.

### Pattern 4: Coesistenza stati di permesso — template switching ibrido

**What:** Due modalità di schermo, decise da funzione pura:

| Stato | Modalità | Template | Cosa si vede |
|-------|----------|----------|--------------|
| `Granted` + `Reading` | Surface | Navigation/MapWithContent | Numero disegnato |
| `Granted` + `Searching`/`NoSignal` | Surface (stesso template) | invariato | "Ricerca segnale..." **disegnato** sulla surface |
| `NotRequested` / `Waiting` | Permission | `MessageTemplate` | "Controlla il telefono" |
| `Denied(permanent)` | Permission | `MessageTemplate` + `Action` con `ParkedOnlyOnClickListener` | Messaggio + Riprova/Impostazioni |

**Perché ibrido e non "tutto sulla surface":** lo stato `Denied` richiede un'azione tappabile con `ParkedOnlyOnClickListener` (prescrizione della Javadoc di `CarContext.requestPermissions()`, già adottata in Fase 9). I tap sulla surface (`SurfaceCallback.onClick`) non danno la semantica "solo da fermi" gestita dall'host e dipendono dal template/pan mode — riprodurla a mano violerebbe la decisione T-09-08 ("nessuna logica di driving-state fatta in casa"). Gli stati di permesso sono rari e transitori, quindi uno switch di template lì costa poco.

**Perché "Ricerca segnale..." sulla surface e non come template:** la perdita di segnale è frequente (gallerie, parcheggi) e oscillante; alternare template a ogni perdita di fix causerebbe flicker e consumerebbe quota di step. Restando sullo stesso template con surface, la transizione Speed ↔ Searching è solo un ridisegno.

**Percorso POI (`MapWithContentTemplate`):** in alternativa al `MessageTemplate` standalone, gli stati di permesso possono diventare `MapWithContentTemplate(content = MessageTemplate)` con la surface `Blank` dietro. Resta però aperto cosa mettere come content **obbligatorio** nello stato Granted (il builder rifiuta un content vuoto): un `MessageTemplate`/`PaneTemplate` minimo sovrapposto riduce l'area visibile e potrebbe reintrodurre l'icona/titolo che v2.1 vuole eliminare. **Da verificare su DHU nella fase spike** — è il principale rischio architetturale del percorso POI.

```kotlin
sealed class CarScreenMode {
    data object Surface : CarScreenMode()
    data class Permission(val permission: CarPermissionState) : CarScreenMode()
}
fun resolveCarScreenMode(permission: CarPermissionState): CarScreenMode = when (permission) {
    CarPermissionState.Granted -> CarScreenMode.Surface
    CarPermissionState.NotRequested, CarPermissionState.Waiting,
    is CarPermissionState.Denied -> CarScreenMode.Permission(permission)
}

// SpeedScreen: unico punto dipendente dalla categoria (STACK.md)
private fun buildSurfaceTemplate(): Template =
    NavigationTemplate.Builder()
        .setActionStrip(/* azione minima obbligatoria, vedi PITFALLS */)
        .build()
    // oppure MapWithContentTemplate.Builder().setContentTemplate(...).build()
```

---

## Data Flow

### Flusso principale: GPS → pixel sulla surface

```
FusedLocationProviderClient (callback)
    ↓ callbackFlow (Main)
GpsSpeedProvider.state  — combine(ticker 1 Hz) + deriveSpeedState + stateIn(WhileSubscribed), conflate uguali
    ↓ collect  [SOLO ramo Granted di permissionState.collectLatest, dentro repeatOnLifecycle(STARTED)]
SpeedScreen: latestState = gpsState
    ↓ renderer.submit(surfaceFrame(Granted, gpsState))        ← NESSUN invalidate() qui
SpeedSurfaceRenderer.latestFrame
    ↓ requestRender() → handler.post (coalescing)
drawFrame(): isValid → lockCanvas → surfacePalette → resolveDrawArea → computeSpeedLayout → drawText → unlockCanvasAndPost
    ↓
Host Android Auto compone la surface sotto l'eventuale action strip
```

**Cambiamento chiave rispetto a v2.0:** oggi ogni emissione GPS chiama `invalidate()` → `onGetTemplate()` → nuovo `PaneTemplate` (soggetto al throttling dell'host sui refresh). Con la surface l'aggiornamento della velocità **non passa più dal template**: `invalidate()` resta solo nel ramo `permissionState.collectLatest` (cambio di permesso = cambio di modalità). Questo elimina la dipendenza dalla cadenza di refresh dei template decisa dall'host.

### Frame timing

- **Nessun loop di rendering, nessun Choreographer, nessun thread dedicato.** Il frame si produce solo a evento: (1) nuovo `SurfaceFrame` (max 1/s, ereditato dal ticker del provider — D-05 di Fase 8 preservato: nessun timer separato lato auto), (2) `onSurfaceAvailable`, (3) `onStableAreaChanged`/`onVisibleAreaChanged`, (4) `Session.onCarConfigurationChanged` (dark mode).
- **Coalescing** via flag `renderPending` + `handler.post`: al primo collegamento l'host tipicamente invia available + stable + visible in rapida sequenza → un solo `lockCanvas`.
- **Costo:** un `drawColor` + 1-2 `drawText` in software canvas è nell'ordine del millisecondo; a 1 Hz sul main thread è trascurabile. `lockHardwareCanvas()` non serve (e il sample ufficiale usa `lockCanvas`).
- Il buffer postato resta visualizzato dall'host finché non ne arriva un altro: se la velocità non cambia (StateFlow conflate) non serve ridisegnare.

### Threading

| Sorgente | Thread | Note |
|----------|--------|------|
| `SurfaceCallback.*` | Main (garantito da `AppManager`) | Javadoc verificata |
| `SpeedScreen.lifecycleScope` / `repeatOnLifecycle` | Main | invariato |
| `GpsSpeedProvider.scope` | `Dispatchers.Main.immediate` | invariato |
| `Session.onCarConfigurationChanged` | Main | — |
| `drawFrame()` | Main (via `Handler(mainLooper)`) | — |

Conclusione: **nessun `synchronized`, nessun `@Volatile`** (il sample li usa perché supporta anche input da altri thread; qui non esistono). Annotare `drawFrame()` con `@MainThread`.

### Lifecycle della surface

```
Session ON_CREATE ──► renderer.onCreate ──► AppManager.setSurfaceCallback(renderer)
Screen onGetTemplate (Granted) ──► template con surface
   host ──► onSurfaceAvailable(container)       [sempre per primo; ripetibile su resize/DPI]
   host ──► onStableAreaChanged / onVisibleAreaChanged   [Rect vuoto = sconosciuto → fallback]
   ...emissioni GPS 1 Hz ──► submit ──► drawFrame
Permesso revocato / Screen → template di permesso
   host ──► (probabile) onSurfaceDestroyed   [host-dependent: il renderer gestisce entrambi i casi]
   SpeedScreen ──► submit(Blank)             [nessuna velocità congelata se la surface resta]
App in background su AA / Screen STOPPED
   repeatOnLifecycle ferma la collect GPS (WhileSubscribed può fermare l'upstream)
   host ──► onSurfaceDestroyed ──► release
Ritorno in foreground ──► onSurfaceAvailable ──► drawFrame(latestFrame)  (subito, senza attendere il tick)
Session ON_DESTROY ──► removeCallbacks + release surface
```

Nota: il sample ufficiale **non** chiama `setSurfaceCallback(null)` in `onDestroy`; a Session distrutta il binder verso l'host può essere già chiuso (`HostException`). Raccomandazione: non chiamarlo, oppure solo dentro `runCatching` (confidenza LOW sul comportamento host).

### Stati di permesso (flusso secondario, invariato nella logica)

```
permissionState.collectLatest { state ->
    invalidate()                                   // cambia modalità: Surface ↔ Permission
    when (state) {
        Granted  -> gps.state.collect { renderer.submit(surfaceFrame(Granted, it)); latestState = it }
        else     -> renderer.submit(SurfaceFrame.Blank)  + logica esistente (request/Waiting/Denied)
    }
}
onGetTemplate() = when (resolveCarScreenMode(permissionState.value)) {
    Surface       -> buildSurfaceTemplate()
    is Permission -> buildPermissionTemplate(mode.permission)   // MessageTemplate, Action ParkedOnly
}
```

### Key Data Flows

1. **GPS → surface:** descritto sopra; unico canale dati Screen → renderer è `submit(SurfaceFrame)`.
2. **Host → renderer (geometria):** `SurfaceContainer` + `Rect` → campi del renderer → `resolveDrawArea`/`computeSpeedLayout` al prossimo frame.
3. **Configurazione auto → palette:** `Session.onCarConfigurationChanged` → `renderer.requestRender()` → `surfacePalette(carContext.isDarkMode)`.
4. **Permesso → template:** invariato rispetto a Fase 9 salvo il tipo di template restituito.

---

## Scaling Considerations

Non applicabile in senso utenti/server (app locale, una sola sessione AA per volta). Le "scale" rilevanti sono le head unit:

| Scenario | Adattamento |
|----------|-------------|
| Display 800×480 a basso DPI | `computeSpeedLayout` usa rapporti e dpi → nessuna costante in px |
| Display ultrawide / portrait (es. Tesla-like, Polestar) | Dimensione cifre = min(vincolo larghezza, vincolo altezza) sull'area stabile |
| Head unit con action strip/overlay grandi | `stableArea` conservativa → numero sempre fuori dagli overlay |

### Scaling Priorities

1. **Primo punto di rottura:** area utile sconosciuta (`Rect` vuoti) → fallback all'intera surface, già nella funzione pura.
2. **Secondo:** host che non chiama `onSurfaceDestroyed` passando al template di permesso → mitigato da `submit(Blank)`.

---

## Anti-Patterns

### Anti-Pattern 1: Tenere l'aggiornamento della velocità su `invalidate()`

**What people do:** continuare a chiamare `invalidate()` a ogni tick "per sicurezza" anche col template con surface.
**Why it's wrong:** rigenera template inutilmente, può incorrere nel throttling dei refresh e, sul percorso `MapWithContentTemplate`, non è documentato che i refresh non consumino quota.
**Do this instead:** `invalidate()` solo su cambio di `CarPermissionState`; la velocità passa solo da `renderer.submit()`.

### Anti-Pattern 2: VirtualDisplay + Presentation + layout XML per un numero

**What people do:** copiare lo snippet "Recommended Drawing Technique" della guida draw-maps e riusare `TextView` con autosize.
**Why it's wrong:** crea VirtualDisplay, una `Presentation` (Dialog) e un view tree per 2 stringhe; più punti di leak/crash su `onSurfaceDestroyed`, lifecycle più complesso, e il layout diventa non testabile su JVM.
**Do this instead:** `lockCanvas` diretto come nel sample `navigation/SurfaceRenderer`, geometria da funzioni pure.

### Anti-Pattern 3: Collezionare `GpsSpeedProvider.state` dentro il renderer

**What people do:** far osservare il flow direttamente al renderer "perché è lui che disegna".
**Why it's wrong:** aggira il gate T-08-08 (collect solo con permesso concesso → `SecurityException` nel `callbackFlow`), crea un secondo collector con lifecycle diverso dallo Screen.
**Do this instead:** renderer passivo; l'unico collector lato auto resta `SpeedScreen` nel ramo `Granted`.

### Anti-Pattern 4: Rilasciare la Surface ricevuta due volte / usare una Surface rilasciata

**What people do:** `surface?.release()` incondizionato in `onSurfaceAvailable` (come il sample) — se l'host ripassa lo **stesso** oggetto per un cambio di dimensione, si rilascia e poi si usa la surface corrente.
**Do this instead:** rilasciare solo se `previous !== incoming`; controllare `isValid` e proteggere `lockCanvas` con try/catch; `unlockCanvasAndPost` sempre in `finally`.

### Anti-Pattern 5: Disegnare i messaggi di permesso sulla surface con tap su canvas

**What people do:** unificare tutto sulla surface e gestire "Riprova" con `SurfaceCallback.onClick`.
**Why it's wrong:** perde `ParkedOnlyOnClickListener` (sicurezza alla guida gestita dall'host), `onClick` dipende da Car API/template e non è garantito su tutti gli host.
**Do this instead:** Pattern 4 (template switching per i soli stati di permesso).

### Anti-Pattern 6: `android.graphics.Rect`/`Paint` nelle funzioni "pure"

**Why it's wrong:** nei test JVM `android.jar` è uno stub: `Rect(1,2,3,4)` non inizializza i campi, `Paint.measureText` lancia "Method not mocked".
**Do this instead:** `AreaPx` e `TextMetricsRatio` propri; conversioni solo nel renderer.

---

## Integration Points

### External Services

| Servizio | Pattern di integrazione | Note |
|----------|-------------------------|------|
| Host Android Auto — `AppManager` | `setSurfaceCallback(renderer)` in Session `ON_CREATE` | Richiede `ACCESS_SURFACE` in manifest, altrimenti `SecurityException` |
| Host — template | `NavigationTemplate` (richiede `NAVIGATION_TEMPLATES`, ActionStrip obbligatoria) **o** `MapWithContentTemplate` (Car API 7, `MAP_TEMPLATES`, content obbligatorio) | Scelta in STACK.md; isolata in `buildSurfaceTemplate()` |
| Host — configurazione | `Session.onCarConfigurationChanged` → redraw | Dark mode: requisito di qualità per le app che disegnano sulla surface |
| `automotive_app_desc.xml` | resta `<uses name="template" />` | Nessun cambio previsto (verificare in STACK se la categoria NAVIGATION richiede altro) |

### Internal Boundaries

| Confine | Comunicazione | Note |
|---------|---------------|------|
| `TachimetroCarSession` ↔ `SpeedSurfaceRenderer` | Costruzione + `lifecycle.addObserver(renderer)` + `renderer.requestRender()` su config change | Il renderer vive quanto la Session (la surface è per-app, non per-Screen) |
| `SpeedScreen` ↔ `SpeedSurfaceRenderer` | `submit(SurfaceFrame)` — unica API | Iniettato via costruttore `SpeedScreen(carContext, renderer)`; nei test si passa un renderer con `TestCarContext` |
| `SpeedScreen` ↔ `GpsSpeedProvider` | invariato (`StateFlow` collect nel ramo Granted) | T-08-08 preservato |
| Renderer ↔ funzioni pure | chiamate dirette con primitivi/data class | Nessuna dipendenza inversa |
| Telefono (`MainActivity`) ↔ auto | nessuna (solo `CarConnection` lato telefono) | invariato |

---

## Pulizie minori (stesso milestone, indipendenti dalla surface)

### `isDeviceCharging()` → `deriveChargingState()`

- **Oggi:** `MainActivity.isDeviceCharging()` duplica la logica `CHARGING || FULL` già in `deriveChargingState()`.
- **Dopo:** mantenere la lettura dello sticky broadcast (serve un valore sincrono al primo avvio — non usare `chargingStateProvider.state.value`, che con `WhileSubscribed` può non essere ancora popolato prima del primo collector) ma delegare la decisione: `deriveChargingState(status) != ChargingState.Hidden`. Opzionale: estrarre `fun isChargingOrFull(state: ChargingState): Boolean` pura in `charging/` con test. Equivalenza verificata: `deriveChargingState` mappa CHARGING→Pulsing, FULL→Full, resto→Hidden.
- **Componenti:** MOD `MainActivity`, eventualmente MOD `ChargingState.kt`/`ChargingStateProvider.kt` + test.

### Finestra transitoria di `carLink` su cold-launch/resume con AA già connesso

- **Causa (dal codice):** `carLink` parte da `Disconnected`; `CarConnection.type` è un `LiveData` che riceve il primo valore in modo asincrono (query al provider di AA). Nel frattempo `onResume()` → `showReady()` mostra "Pronto", `applyKeepScreenOn(savedKeepOn)` accende il flag e una prima emissione GPS può mostrare il numero sul telefono prima che arrivi `Connected`.
- **Direzione architetturale (confidenza MEDIUM, da validare in fase):** introdurre uno stato iniziale esplicito `CarLinkState.Unknown` nella funzione pura (`resolveCarLinkState(null) → Unknown`), con regole pure dedicate: `resolveEffectiveKeepScreenOn(saved, Unknown)` e un rendering neutro/vuoto dell'area velocità finché il primo valore non arriva. Attenzione al vincolo T-10-02 (fail-safe: un valore spurio non deve mai nascondere il tachimetro in modo permanente): `Unknown` deve risolversi al primo valore del LiveData, e serve un timeout/fallback o la garanzia che `CarConnection` emetta sempre (verificare). Alternativa minima: nessun nuovo stato, ma rinviare `showReady()`/`applyKeepScreenOn` alla prima emissione dell'observer.
- **Componenti:** MOD `CarLinkState.kt` (+ `CarLinkStateTest`, `CarLinkSequenceTest`), MOD `MainActivity.onCarLinkChanged/showReady/onResume`.

---

## Suggested Build Order

Ordine guidato dalle dipendenze e dal rischio (il rischio più alto — cosa l'host mostra davvero — va eliminato per primo).

1. **Spike Surface + decisione template/categoria** (rischio massimo, sblocca tutto)
   - MOD manifest (`ACCESS_SURFACE`, permesso template, categoria, `minCarApiLevel` se POI), NUOVO `SpeedSurfaceRenderer` minimale (sfondo + testo fisso centrato), MOD `TachimetroCarSession` (creazione + observer), `buildSurfaceTemplate()` in `SpeedScreen` per il solo ramo Granted.
   - Verifica su DHU: la surface arriva? quali `stableArea`/`visibleArea`? l'action strip scompare? con `MapWithContentTemplate` quanto copre il content obbligatorio?
   - Esito: conferma della categoria (input a STACK.md/roadmap). **Research flag: SÌ.**
2. **Funzioni pure di layout e stato** (nessuna dipendenza Android, parallelizzabile con 1 dopo il primo esito)
   - NUOVI `SurfaceLayout.kt` (`AreaPx`, `resolveDrawArea`, `computeSpeedLayout`, `surfacePalette`), `SurfaceFrame.kt`, `CarScreenMode.kt` + test JVM. I valori delle aree osservati nello spike diventano casi di test.
3. **Integrazione dati GPS → renderer**
   - `SpeedScreen`: ramo Granted → `submit(surfaceFrame(...))`, rimozione di `invalidate()` per tick e del `PaneTemplate` del ramo Granted; renderer con layout reale, coalescing, dark mode via `onCarConfigurationChanged`, gestione robusta di release/isValid.
   - Dipende da 1 (renderer/Session) e 2 (funzioni pure).
4. **Coesistenza stati di permesso/searching**
   - `buildPermissionTemplate()` (MessageTemplate + Action ParkedOnly), `submit(Blank)` all'uscita da Granted, "Ricerca segnale..." disegnato sulla surface; aggiornamento `SpeedScreenTemplateTest`; NUOVO `SpeedSurfaceRendererTest` (TestAppManager, surface null, doppio `onSurfaceAvailable`, destroy).
   - Verifica DHU dell'intera matrice di stati (primo collegamento, rifiuto, rifiuto permanente, grant dalle impostazioni, perdita segnale, background/foreground AA).
5. **Pulizie telefono** (indipendenti: possono andare in una fase breve iniziale o finale)
   - `isDeviceCharging()` → `deriveChargingState()`; stato transitorio `carLink`. Nessuna dipendenza dalle fasi 1-4; toccano solo `MainActivity`/`CarLinkState`/`charging`. Raccomandazione: **per ultime o in parallelo**, così un eventuale rework dello spike non si mescola con modifiche lato telefono.

---

## Sources

- AOSP androidx-main `car/app/app/src/main/java/androidx/car/app/SurfaceCallback.java` — contratto dei callback, ordine garantito, `Surface#release()` obbligatorio, Rect vuoto = sconosciuto (HIGH)
- AOSP androidx-main `.../AppManager.java` — `setSurfaceCallback`, permesso `ACCESS_SURFACE`, esecuzione su main looper (HIGH)
- AOSP androidx-main `.../navigation/model/NavigationTemplate.java` — ActionStrip obbligatoria, refresh illimitati, reset quota (HIGH)
- AOSP androidx-main `.../navigation/model/MapWithContentTemplate.java` + `ContentTemplateConstraints` — `@RequiresCarApi(7)`, permessi, content obbligatorio e tipi ammessi (HIGH)
- AOSP androidx-main `car/app/app-samples/navigation/common/.../car/SurfaceRenderer.java` — pattern ufficiale lockCanvas/Handler/LifecycleObserver (HIGH)
- AOSP androidx-main `car/app/app-testing/.../TestAppManager.java`, `SurfaceContainer.java` — testabilità (HIGH)
- https://developer.android.com/training/cars/apps/library/draw-maps — permessi, tabella template/categorie (Navigation, POI, Weather per `MapWithContentTemplate`), VirtualDisplay+Presentation, dark mode (HIGH)
- https://developer.android.com/training/cars/apps/navigation — template con surface per app di navigazione (MEDIUM)
- Codice del progetto: `SpeedScreen.kt`, `TachimetroCarSession.kt`, `CarSpeedContent.kt`, `MainActivity.kt`, `GpsSpeedProvider.kt`, `ChargingStateProvider.kt`, `CarLinkState.kt`, `.planning/milestones/v2.0-phases/08-*/08-CONTEXT.md` (D-14)

---
*Architecture research for: rendering diretto sulla Surface Android Auto (Tachimetro v2.1)*
*Researched: 2026-09-23*
