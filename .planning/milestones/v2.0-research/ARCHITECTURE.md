# Architecture Research

**Domain:** Android Auto (Car App Library) integration into an existing single-Activity Android app
**Researched:** 2026-08-31
**Confidence:** HIGH for platform mechanics (sourced from official `developer.android.com` docs and API reference); MEDIUM for the specific ownership-pattern recommendation below (a sound inference from documented StateFlow/process behavior, not a pattern Google's docs spell out verbatim); LOW/flagged explicitly where the public docs are genuinely ambiguous (template refresh-quota exemption scope)

## The One Finding That Changes the Plan

Before the "how to wire it up" content below: **the literal product requirement in PROJECT.md — "velocità attuale visualizzata sul display Android Auto come numero grande, ad alto contrasto," mirroring the phone's huge-digit display — is not straightforwardly compliant with Android Auto's platform rules, and this should be resolved as a product decision before a build-order phase is locked in.**

- Continuous, imperative Canvas drawing on a raw `Surface` (the only way to reproduce the phone's giant-autosized-digit look) is only available to apps that use `NavigationTemplate` (or the newer `MapWithContentTemplate`), both gated behind declaring `androidx.car.app.category.NAVIGATION` (or `.POI`) in the manifest and requesting `androidx.car.app.NAVIGATION_TEMPLATES` (or `.MAP_TEMPLATES`) plus `androidx.car.app.ACCESS_SURFACE`. — HIGH confidence, official docs.
- The official Car App quality guidelines (`developer.android.com/docs/quality-guidelines/car-app-quality`, rule `NF-1`) explicitly require: *"The app must provide turn-by-turn navigation directions"* to qualify for the `NAVIGATION` category, and `NF-2` requires that *"the app draws only map content on the surface of the navigation templates."* Tachimetro has no routing/destinations and would not be drawing a map. — HIGH confidence, this is a direct quote from Google's own review criteria, not an inference.
- Real-world corroboration: Google Maps itself only recently (mid-2026) started showing a live speedometer on Android Auto, and it only appears as an overlay *during active turn-by-turn navigation* — reinforcing that live speed-on-Surface is treated by the platform as a navigation-app feature, not a standalone capability. (MEDIUM confidence — journalism, not a spec, but consistent with the guideline above.)
- Every other supported category (POI, IoT, VoIP, Weather) either has no Surface access at all, or gets it only for genuine map/place content (`MapWithContentTemplate`).

**Practical implication:** there is no compliant path to a raw-Canvas, continuously-redrawn giant number on the Android Auto screen for an app whose whole purpose is "just show speed." The realistic compliant alternative is a **declarative template (`PaneTemplate`/`GridTemplate`) showing a custom-rendered `CarIcon` bitmap** (a bitmap you draw yourself with a `Canvas`-on-`Bitmap`, exactly like the weather-app pattern Google documents for gauges), refreshed via `Screen.invalidate()`. This avoids the Surface/category restriction entirely, but is subject to a **hard quota of 5 template pushes per task**, with same-type/same-content pushes exempted from the quota (see Anti-Pattern 1 and Pattern 3 below for the exact mechanics and the parts that remain genuinely uncertain).

This finding doesn't block architecture work — the component wiring below is identical either way — but it should reach the roadmap as a flagged decision point, not be silently resolved by whichever approach is easiest to prototype.

## Standard Architecture

### System Overview

```
┌──────────────────────────────────────────────────────────────────────┐
│                    Tachimetro app process (single process)            │
│                                                                        │
│  ┌────────────────────────┐        ┌─────────────────────────────┐   │
│  │   TachimetroApplication │        │   Android Auto host app      │   │
│  │  (NEW — owns singleton) │        │   (Google's app, phone-side  │   │
│  │                         │        │   process, binds over Binder)│   │
│  │  gpsSpeedProvider ──┐   │        └───────────────┬───────────────┘  │
│  └──────────────────────┼──┘                        │ binds to         │
│                          │                            ▼                │
│  ┌───────────────────┐   │            ┌──────────────────────────────┐│
│  │   MainActivity     │   │            │  TachimetroCarAppService     ││
│  │  (existing, phone  │   │            │  (NEW — extends CarAppService)││
│  │  screen)           │   │            └───────────────┬──────────────┘│
│  │  repeatOnLifecycle │   │                             │ onCreateSession│
│  │  (STARTED) {       │   │                             ▼               │
│  │   collect state }  │   │            ┌──────────────────────────────┐│
│  └──────────┬─────────┘   │            │  TachimetroCarSession        ││
│             │              │            │  (NEW — extends Session)     ││
│             │              │            └───────────────┬──────────────┘│
│             │              │                             │ onCreateScreen│
│             │              │                             ▼               │
│             │              │            ┌──────────────────────────────┐│
│             │              │            │  SpeedScreen                  ││
│             │              │            │  (NEW — extends Screen)       ││
│             │              │            │  repeatOnLifecycle(STARTED) { ││
│             │              │            │   collect state }             ││
│             │              │            └───────────────┬──────────────┘│
│             │              │                             │ draws via     │
│             │              │                             ▼               │
│             │              │            ┌──────────────────────────────┐│
│             │              │            │ SurfaceCallback / CarIcon     ││
│             │              │            │ renderer (NEW)                ││
│             │              │            └──────────────────────────────┘│
│             └──────────────┴──────────────────────► GpsSpeedProvider    │
│                                          (EXISTING — reused unmodified, │
│                                           now owned at Application      │
│                                           scope instead of Activity)   │
│                                                       │                 │
│                                                       ▼                 │
│                                      FusedLocationProviderClient        │
│                                      (Google Play Services)             │
└──────────────────────────────────────────────────────────────────────┘
```

Key structural fact (HIGH confidence, official docs + API reference): `CarAppService` runs in your app's **own process**, communicating with the Android Auto host app (a separate process on the phone) over Binder IPC — it is *not* the host's process, and it is *not* a special isolated process unless you explicitly set `android:process` on the `<service>` (there is no reason to for this app). This means `TachimetroCarAppService`/`Session`/`Screen` can call directly into existing Kotlin classes like `GpsSpeedProvider` — no IPC, no serialization, no cross-process contract needed for internal wiring.

Equally important: the host **can bind to `CarAppService` and create a `Session` even when `MainActivity` is not running** (e.g., the user connects to Android Auto without ever having opened the app on the phone first, or after swiping the phone app away from recents). `Session`/`Screen` lifecycles are therefore fully independent of `MainActivity`'s lifecycle — they are two separate entry points into the same process, not a parent/child relationship.

### Component Responsibilities

| Component | Responsibility | Typical Implementation |
|-----------|----------------|------------------------|
| `TachimetroApplication` (NEW) | Owns the single, process-scoped `GpsSpeedProvider` instance so both entry points share one GPS subscription | `Application` subclass, `val gpsSpeedProvider by lazy { GpsSpeedProvider(applicationContext) }`; declared via `<application android:name=".TachimetroApplication">` |
| `TachimetroCarAppService` (NEW) | Entry point the Android Auto host binds to; validates the host and creates a `Session` per connection | `class TachimetroCarAppService : CarAppService()`, overrides `onCreateSession()` and `createHostValidator()` |
| `TachimetroCarSession` (NEW) | One instance per active car connection (per display); provides the first `Screen` | `class TachimetroCarSession : Session()`, overrides `onCreateScreen(intent)` returning `SpeedScreen(carContext)` |
| `SpeedScreen` (NEW) | Owns the car-screen UI: collects `GpsSpeedProvider.state` on its own `Lifecycle`, drives either a `Surface` redraw loop or a template rebuild | `class SpeedScreen(carContext: CarContext) : Screen(carContext)`, overrides `onGetTemplate()`; uses `lifecycleScope`/`repeatOnLifecycle(STARTED)` exactly like `MainActivity` does today |
| Surface renderer (NEW, only if Surface path is chosen) | Implements `SurfaceCallback`; owns the retained `Surface` reference and performs the actual `Canvas` drawing | Small class registered via `carContext.getCarService(AppManager::class.java).setSurfaceCallback(...)` |
| Bitmap renderer (NEW, only if template path is chosen) | Draws the speed digits onto an offscreen `Bitmap` with a `Canvas` (no `Surface` involved), wraps it as a `CarIcon` for a `Row`/`Pane`/`GridItem` | Pure function-ish helper, same "draw big bold text" logic as `MainActivity`'s autosize handling, just targeting a `Bitmap` instead of a `TextView` |
| `GpsSpeedProvider` (EXISTING, unmodified internals) | Single source of truth for GPS speed, already exposed as `StateFlow<SpeedState>` with accuracy/noise filtering and 5s staleness detection | No code changes required inside the class; only its *construction site* moves from `MainActivity.setupGpsCollection()` to `TachimetroApplication` |
| `MainActivity` (EXISTING, minor change) | Phone screen; now reads the shared provider from `Application` instead of constructing its own | Change `gpsSpeedProvider = GpsSpeedProvider(applicationContext)` → `gpsSpeedProvider = (application as TachimetroApplication).gpsSpeedProvider`; drop the `gpsSpeedProvider.close()` call in `onDestroy()` (see Anti-Pattern 2) |

## Recommended Project Structure

```
app/src/main/java/com/sed/tachimetro/
├── TachimetroApplication.kt      # NEW — process-scoped GpsSpeedProvider owner
├── MainActivity.kt               # EXISTING — construction-site change only
├── gps/
│   ├── GpsSpeedProvider.kt       # EXISTING — reused as-is, zero internal changes
│   ├── SpeedState.kt             # EXISTING — reused as-is
│   └── SpeedMapping.kt           # EXISTING — reused as-is
├── maxspeed/, distance/, screen/, charging/   # EXISTING — untouched, phone-only per scope
└── car/                           # NEW package, mirrors existing feature-package convention
    ├── TachimetroCarAppService.kt # extends CarAppService
    ├── TachimetroCarSession.kt    # extends Session
    ├── SpeedScreen.kt             # extends Screen; collects GpsSpeedProvider.state
    └── SpeedSurfaceRenderer.kt    # SurfaceCallback impl (Surface path) OR
                                    # SpeedBitmapRenderer.kt (template/CarIcon path — see decision above)

app/src/main/res/xml/
└── automotive_app_desc.xml        # NEW — declares template-app support to Play/host
```

### Structure Rationale

- **`car/` as a sibling of `gps/`, `maxspeed/`, `screen/`, `charging/`:** matches the project's existing "package = feature/domain" convention (`com.sed.tachimetro.{gps,maxspeed,screen,charging}`) documented in CLAUDE.md — no new organizational pattern introduced.
- **`TachimetroApplication.kt` at the top level, not inside `car/`:** it is genuinely app-scoped infrastructure (needed the moment two independent entry points exist), not a car-specific concern — placing it in `car/` would misleadingly suggest `MainActivity` doesn't depend on it too.
- **`gps/GpsSpeedProvider.kt` untouched:** this is the central finding of this research (see Pattern 1) — the class's public contract (`context: Context` constructor, `val state: StateFlow<SpeedState>`) already supports exactly this reuse without modification.

## Architectural Patterns

### Pattern 1: Application-scoped shared `StateFlow` provider (reuse, don't duplicate)

**What:** Promote `GpsSpeedProvider`'s ownership from "constructed inside `MainActivity`" to "constructed once, at `Application` scope," and have both `MainActivity` and `SpeedScreen` collect the *same* `state: StateFlow<SpeedState>` instance.

**Why this is the right call, not just a convenient one:** `GpsSpeedProvider.state` is already built with `.stateIn(scope, started = SharingStarted.WhileSubscribed(), ...)`. `WhileSubscribed()` is inherently ref-counted across *any number* of independent collectors — it starts the upstream `FusedLocationProviderClient` subscription when the first collector attaches (from either `MainActivity` or `SpeedScreen`) and tears it down only when the *last* collector detaches. This is exactly the semantics needed when two independent, differently-lifecycled consumers (an `Activity` and a `Session`/`Screen`) need the same continuous GPS stream: no code in `GpsSpeedProvider` has to know or care how many consumers exist, and there is no double subscription to Play Services location updates (battery-relevant, since both surfaces could theoretically be visible at once — the car screen doesn't blank the phone screen by default).

**When to use:** Any time a new entry point (car screen, widget, future watch companion, etc.) needs the same reactive data the phone screen already has, and the source is process-local (no cross-process IPC needed, confirmed by the process model above).

**Trade-offs:** Requires one new small ownership object (`TachimetroApplication`) that didn't exist before — a real, if minor, departure from "no ViewModel/DI layer, everything instantiated directly in `MainActivity`." This is the smallest possible version of that departure: a plain `Application` subclass with one `by lazy` property, not a DI framework, not a service locator, not a singleton `object` reaching into a static `Context` (which the project's `WR-04` convention already avoids). The alternative (Pattern 1b below) avoids even this.

**Example:**
```kotlin
class TachimetroApplication : Application() {
    val gpsSpeedProvider: GpsSpeedProvider by lazy { GpsSpeedProvider(applicationContext) }
}

// MainActivity.setupGpsCollection() — construction-site change only:
gpsSpeedProvider = (application as TachimetroApplication).gpsSpeedProvider

// SpeedScreen — same reactive shape as MainActivity's existing collector:
class SpeedScreen(carContext: CarContext) : Screen(carContext) {
    private val provider = (carContext.applicationContext as TachimetroApplication).gpsSpeedProvider
    init {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                provider.state.collect { state -> /* redraw */ }
            }
        }
    }
}
```

### Pattern 1b (fallback alternative): Independent per-entry-point instances

**What:** Instead of a shared singleton, `SpeedScreen`/`TachimetroCarSession` constructs its own `GpsSpeedProvider(carContext.applicationContext)`, exactly mirroring how `MainActivity` instantiates it today.

**When to use:** If the team prefers to keep zero new ownership objects and accepts the trade-off below, this is a legitimate, simpler-to-reason-about fallback that requires no change to `MainActivity` at all.

**Trade-offs:** Two independent `FusedLocationProviderClient` subscriptions when both surfaces are simultaneously active (rare but not impossible — Android Auto phone projection does not always blank the phone screen); two independently-drifting `lastAcceptedLocation`/`lastAcceptedUpdateAtMs` internal states (invisible to the user today since the car screen doesn't show distance/max per this milestone's stated scope, but worth knowing if a future milestone adds distance to the car screen too). Recommend Pattern 1 unless there's a concrete reason to avoid the new `Application` subclass.

### Pattern 2: Mirror the existing `repeatOnLifecycle(STARTED)` discipline on `Session`/`Screen`

**What:** `Session` and `Screen` both implement `LifecycleOwner` (`Session.getLifecycle()`, `Screen.getLifecycle()`) and expose `lifecycleScope`, so the exact collection pattern `MainActivity` already uses (`lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { provider.state.collect { ... } } }`) ports over unchanged in shape.

**When to use:** Always, for both the Surface-drawing path and the template path — this keeps the car-side code idiomatically identical to the phone-side code already in the repo, rather than inventing a second reactive style.

**Trade-offs:** None significant — this is a direct, low-risk port of an existing, already-battle-tested pattern (D-07 in the existing codebase comments).

### Pattern 3: Category/template choice is a compliance decision, not just a technical one

**What:** Two concretely different implementation paths exist, gated by manifest declarations, with different trade-offs:

| | Surface (`NavigationTemplate`) path | Template (`CarIcon` bitmap) path |
|---|---|---|
| Manifest | `<category android:name="androidx.car.app.category.NAVIGATION"/>`, `<uses-permission android:name="androidx.car.app.NAVIGATION_TEMPLATES"/>`, `<uses-permission android:name="androidx.car.app.ACCESS_SURFACE"/>` | No special category needed for a plain `PaneTemplate`/`GridTemplate`; no `ACCESS_SURFACE` permission |
| Drawing | Register a `SurfaceCallback` via `carContext.getCarService(AppManager::class.java).setSurfaceCallback(...)`; draw with `surface.lockCanvas(null)` / `unlockCanvasAndPost(canvas)` on every emission — unthrottled by any template quota | Draw once per emission onto an offscreen `Bitmap` with a plain `Canvas`, wrap via `IconCompat.createWithBitmap(bitmap)` → `CarIcon`, place inside a `Row`/`GridItem`, call `Screen.invalidate()` to trigger a fresh `onGetTemplate()` |
| Fidelity to phone UI | Full imperative control — can genuinely reproduce a giant autosized digit like the phone screen | Host controls surrounding chrome (header, action strip) and row layout; the bitmap itself can still be large/high-contrast, but it's *content inside a template*, not a full-bleed custom screen |
| Compliance risk | **HIGH** — `NF-1` quality guideline requires actual turn-by-turn navigation for this category; a pure speedometer does not qualify, meaning Play review could reject the app or the category-appropriateness could be flagged later even if initial review passes | **LOW** — no category claim beyond what the app actually does |
| Refresh throttling | None (Surface drawing bypasses the template quota entirely — confirmed by official docs: "NavigationTemplate's Surface avoids this because it uses a custom drawing surface rather than declarative templates") | Hard quota of **5 template pushes per task** exists (HIGH confidence, official docs). Pushing a template of the *same type* with the *same main content* as the previous one does **not** count against the quota (HIGH confidence, documented, with the ListTemplate-row-toggle case given as the official example) — but the docs do not spell out precisely whether a Row's changing numeric text every second reliably qualifies as "same main content" indefinitely, or whether some other Play-review-level throttling expectation (`SA-1`: "apps must not display automatically scrolling text or animated elements") applies to a value that visibly changes once per second. **This specific point needs a short throwaway prototype/spike before committing to the template path for a 1 Hz-updating value — treat it as LOW confidence until verified hands-on.** |

**Recommendation for the roadmap:** don't silently pick one of these two paths inside an implementation phase. Surface the trade-off (illustrated above) explicitly, most likely as an early "spike/feasibility" phase that (a) prototypes both minimal paths against the Desktop Head Unit, and (b) gets an explicit product decision on whether declaring the `NAVIGATION` category for a non-navigation app is acceptable risk, before the "real" build-order phases below commit to one.

### Pattern 4: Reactive `StateFlow` collector as the bridge into an otherwise-imperative Surface

**What:** A `Surface` obtained via `SurfaceCallback` has no automatic redraw scheduling — unlike a `TextView` whose `.text =` assignment triggers measure/layout/draw automatically, every single frame on the Surface is drawn by your own code, imperatively, inside `lockCanvas()`/`unlockCanvasAndPost()`. The reconciliation with this codebase's existing reactive style: keep the *same* `collect { state -> ... }` shape used everywhere else in the project, and make the terminal action of that collector "draw this frame" instead of "set this TextView's text."

**Why this works cleanly for Tachimetro specifically:** `GpsSpeedProvider.state` already emits at most once per second — its internal `ticker` (`while (true) { emit(...); delay(1000) }`) already paces the whole pipeline, `combine`d with the accepted-reading stream. This means the car-side collector does **not** need its own separate redraw-pacing timer; it can safely call the draw function directly on every `state` emission, reusing a rate-limiting property the class already has for an unrelated reason (staleness detection), rather than introducing a second, independent 1 Hz loop that could drift out of phase with the phone screen's.

**Trade-off / pitfall:** the `Surface` and the `Screen`'s `Lifecycle` are two *separate* signals that don't necessarily change together — `onSurfaceDestroyed()` can fire (e.g., the user navigates to a different screen on the cluster display, or the action-strip chrome changes the visible area enough to warrant a new Surface) without the `Screen`/`Session` as a whole leaving `STARTED`. The draw function must guard against drawing after `onSurfaceDestroyed()` (hold a nullable `Surface` reference, null it out in that callback, and skip drawing when null) rather than relying solely on the `repeatOnLifecycle(STARTED)` gate — that gate stops the *GPS collection*, not necessarily every in-flight draw call racing against surface teardown.

**Example (Surface path):**
```kotlin
class SpeedSurfaceRenderer : SurfaceCallback {
    @Volatile private var surface: Surface? = null
    private var stableArea: Rect? = null

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        surface = surfaceContainer.surface
    }

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        surface = null // guards the in-flight draw() below
    }

    override fun onStableAreaChanged(stableArea: Rect) {
        this.stableArea = stableArea // keep the digit centered/clear of chrome, mirrors
                                      // the phone screen's WindowInsetsCompat handling
    }

    fun draw(state: SpeedState) {
        val s = surface ?: return
        val canvas = try { s.lockCanvas(null) } catch (e: IllegalStateException) { return } // surface torn down mid-race
        try {
            canvas.drawColor(Color.BLACK)
            // draw big bold text for state.kmh / "Ricerca segnale GPS..." within stableArea
        } finally {
            s.unlockCanvasAndPost(canvas)
        }
    }
}
```

## Data Flow

### Request Flow

```
FusedLocationProviderClient (Google Play Services)
    ↓ (LocationCallback, 1/sec)
GpsSpeedProvider.rawLocations (callbackFlow)
    ↓ mapSpeedToKmh() [accuracy + noise filter, EXISTING, unmodified]
GpsSpeedProvider.acceptedReadings
    ↓ combine() with 1s ticker → deriveSpeedState() [EXISTING, unmodified]
GpsSpeedProvider.state: StateFlow<SpeedState>  ← SINGLE SOURCE OF TRUTH, Application-scoped
    ├──→ MainActivity.updatePlaceholder(state)         → TextView.text (phone screen, unchanged)
    └──→ SpeedScreen's collector → renderer.draw(state) → Canvas-on-Surface or CarIcon-in-template (car screen, NEW)
```

### Key Data Flows

1. **GPS → phone screen:** unchanged from the existing architecture — `MainActivity` collects `state` inside `repeatOnLifecycle(STARTED)`, gated by `permissionGranted`.
2. **GPS → car screen:** new, but structurally identical — `SpeedScreen` collects the *same* `state` StateFlow inside its own `repeatOnLifecycle(STARTED)` (Session/Screen's Lifecycle, not Activity's). No permission gating needed on the car side: `ACCESS_FINE_LOCATION` is an app-level (not Activity-level) grant already established by `MainActivity`'s existing flow — if the user has denied it, `GpsSpeedProvider`'s upstream `callbackFlow` should not be relied upon to fail gracefully by itself (it is annotated `@Suppress("MissingPermission")` on the assumption that the caller already checked). **Concrete new requirement:** `SpeedScreen` needs its own answer to "permission not granted yet" (e.g., show a message template asking the user to grant permission on the phone), since it can be reached without `MainActivity` ever having run.
3. **State fan-out, not duplication:** because of Pattern 1 (shared singleton + `WhileSubscribed()`), this is a genuine one-producer/two-consumer fan-out at the `StateFlow` level, not two independent GPS pipelines — the "Key Data Flows" above are two *readers* of one *stream*.

## Scaling Considerations

Reframed for this domain (displays/entry-points, not user count — this is a single-user app):

| Scale | Architecture Adjustments |
|-------|---------------------------|
| 1 display (phone only, today) | Current architecture as-is. |
| 2 displays (phone + Android Auto projected screen) | Pattern 1 (shared `GpsSpeedProvider`) — this milestone's scope. No change to `GpsSpeedProvider` internals needed. |
| 3 displays (phone + Android Auto main display + car cluster display behind the wheel) | Out of scope per PROJECT.md, but architecturally: a second `Session` (cluster) would be created by the host if `FEATURE_CLUSTER` category is additionally declared — `Session` already supports this 1-service-to-N-sessions relationship natively; each `Session` would still collect the same shared `state` StateFlow, so Pattern 1 continues to scale to this case without modification if ever pursued. |

### Scaling Priorities

1. **First real constraint:** not a scale/performance one — it's the category-compliance question in Pattern 3, which gates *which* rendering approach is even legitimate to build before worrying about anything else.
2. **Second constraint (if Surface path chosen):** keeping the per-frame Canvas draw cheap enough to stay well under the ~500ms "recommended" refresh budget mentioned in Google's own refresh-template guidance — a single large bold string draw (what this app already does on the phone) is trivially fast enough; no measurable risk here.

## Anti-Patterns

### Anti-Pattern 1: Declaring the `NAVIGATION` category to get Surface access without providing real navigation

**What people do:** Reach for `NavigationTemplate` + `SurfaceCallback` purely because it's the only path to unrestricted custom Canvas drawing, without the app actually providing turn-by-turn route guidance.
**Why it's wrong:** Directly contradicts `NF-1`/`NF-2` in Google's own car-app quality guidelines (a pure speedometer has no routes, no map, and `NF-2` requires the Surface to draw *only* map content) — real risk of Play Store review rejection or later removal, not a style nitpick.
**Instead:** Treat this as a product decision to surface explicitly (see Pattern 3), and default to the compliant template+`CarIcon` path unless the team consciously accepts the compliance risk with eyes open.

### Anti-Pattern 2: Tying the shared `GpsSpeedProvider`'s teardown to `MainActivity.onDestroy()`

**What people do:** Keep the existing `gpsSpeedProvider.close()` call in `MainActivity.onDestroy()` after promoting the provider to `Application` scope (Pattern 1).
**Why it's wrong:** `Session`/`Screen` can be alive and actively collecting `state` independent of whether `MainActivity` exists at all (confirmed: the host can bind to `CarAppService` without any Activity running). Cancelling the shared scope from `MainActivity.onDestroy()` would kill GPS updates for an active car screen the moment the phone Activity happens to be destroyed (e.g., a configuration change, or the user swiping the phone app from recents while still driving with the car screen open).
**Instead:** Drop the manual `.close()` call once ownership moves to `TachimetroApplication` — the provider's `CoroutineScope` now naturally lives for the process's lifetime, which is what's wanted for infrastructure two independent entry points depend on. (`WhileSubscribed()` already handles stopping the *upstream* location updates correctly when both consumers are gone — the `.close()` call was always a secondary defensive teardown per the existing code comment, not the primary stop mechanism, so removing it doesn't reintroduce a leak.)

### Anti-Pattern 3: Treating a `Surface` like a self-invalidating `View`

**What people do:** Assume that once `onSurfaceAvailable()` fires and an initial frame is drawn, later state changes will "just show up" the way updating a `TextView.text` does.
**Why it's wrong:** There is no implicit redraw scheduling on a `Surface` obtained this way — nothing redraws until your code calls `lockCanvas()`/`unlockCanvasAndPost()` again. Skipping this leads to a car screen frozen on the first speed reading forever.
**Instead:** Pattern 4 above — bind the draw call to the same `state` collector already used everywhere else in this codebase, so a fresh draw happens on every emission (already paced at 1 Hz by `GpsSpeedProvider`'s own ticker).

### Anti-Pattern 4: Shipping with `HostValidator.ALLOW_ALL_HOSTS_VALIDATOR`

**What people do:** Leave the test-only `createHostValidator()` override (used in Google's own codelab and Hello World samples to simplify local Desktop Head Unit testing) in place for a production build.
**Why it's wrong:** It allows *any* app claiming to be a car host to bind to `TachimetroCarAppService` and drive the Session — a real, if narrow, security surface (untrusted app impersonating Android Auto).
**Instead:** Before any release build, replace it with a real `HostValidator` built from the known Android Auto/Automotive OS host allow-list (`HostValidator.Builder`), consistent with the codebase's existing "sanitize/validate at the boundary" ethos already applied elsewhere (e.g., `sanitizePersistedMax()`).

### Anti-Pattern 5: Racing a redraw against `onSurfaceDestroyed()`

**What people do:** Hold a non-nullable `Surface` reference captured once in `onSurfaceAvailable()` and keep calling `lockCanvas()` on it from a background timer without checking whether `onSurfaceDestroyed()` has since fired.
**Why it's wrong:** Produces `IllegalStateException`/"Surface has already been released"-style crashes when a draw call races a fast surface teardown (e.g., quick screen transitions on the cluster/head unit, or a fast disconnect).
**Instead:** Hold the `Surface` in a nullable, volatile field; null it in `onSurfaceDestroyed()`; guard every draw call with a null-check plus a defensive `try/catch` around `lockCanvas()` (see the `SpeedSurfaceRenderer` example in Pattern 4).

## Integration Points

### External Services

| Service | Integration Pattern | Notes |
|---------|---------------------|-------|
| Android Auto host app (Google's app, phone-side, separate process) | Binder IPC via `CarAppService.onBind()` (handled automatically by the base class — do not override) | This is a *separate process* from Tachimetro's own, but `TachimetroCarAppService`/`Session`/`Screen` still run inside Tachimetro's own process — only the host↔service boundary crosses processes, not anything internal to this app. |
| Desktop Head Unit (DHU) | Local dev-machine tool from the Android Auto desktop head unit package; connects to a phone (real or emulator) over `adb` and renders what the car display would show | The only practical way to test a `CarAppService` end-to-end without a physical vehicle or an in-car head unit — should be the very first thing set up in Phase A of the build order below, before any GPS wiring, purely to validate manifest/category/service-discovery plumbing. |
| Google Play (review) | Category eligibility review (`NAVIGATION`/`POI`/etc. quality guidelines) before/at listing | Directly relevant per the compliance finding above — this is a gate on *shipping*, not on local development/testing, which works regardless of category compliance. |

### Internal Boundaries

| Boundary | Communication | Notes |
|----------|---------------|-------|
| `TachimetroApplication` ↔ `GpsSpeedProvider` | Direct construction (`by lazy`), same process, no IPC | New ownership relationship (Pattern 1); `GpsSpeedProvider`'s constructor signature (`context: Context`) is unchanged — it already only needs `applicationContext`, which `Application` provides natively. |
| `MainActivity` ↔ `GpsSpeedProvider` | `StateFlow.collect` inside `repeatOnLifecycle(STARTED)` | Unchanged in shape; only the construction site moves (see Component Responsibilities table). |
| `SpeedScreen`/`TachimetroCarSession` ↔ `GpsSpeedProvider` | `StateFlow.collect` inside `repeatOnLifecycle(STARTED)`, same pattern, different `Lifecycle` owner (Session/Screen's, not Activity's) | New; structurally identical to the `MainActivity` boundary above — this symmetry is the main argument for Pattern 1 over introducing a bespoke car-specific data-access pattern. |
| `MainActivity` ↔ `androidx.car.app.connection.CarConnection` | `CarConnection(context).type` exposes a `LiveData<Int>` (`CONNECTION_TYPE_NOT_CONNECTED` / `CONNECTION_TYPE_PROJECTION` / `CONNECTION_TYPE_NATIVE`), observable from a plain `Context`/Activity, not just from inside a `Session` | Not required to make the car screen itself work, but this is the concrete, documented bridge point if a later phase needs `MainActivity` to know "Android Auto is currently connected" (referenced in PROJECT.md's still-undecided "phone screen-on behavior when Android Auto connects" note) — worth flagging to whichever phase owns that decision rather than re-discovering this API then. (MEDIUM confidence — documented API, but its use from a plain Activity context specifically wasn't shown in a first-party code sample during this research pass.) |

## Suggested Build Order (Phased)

Ordered by dependency — each phase should be genuinely demonstrable/testable before the next begins, per this project's existing "human checkpoint on device" discipline.

1. **Plumbing-only scaffold (no GPS yet).** Add the `androidx.car.app:app` + `androidx.car.app:app-projected` dependencies (NOT `app-automotive` — this milestone targets Android Auto phone-projection only, not Android Automotive OS, so no `CarAppActivity`/`uses-feature android.hardware.type.automotive`/automotive product flavor is needed). Create `TachimetroCarAppService`, `TachimetroCarSession`, and a `SpeedScreen` that returns a static `PaneTemplate` ("Ciao" or similar), with `HostValidator.ALLOW_ALL_HOSTS_VALIDATOR` for now. Wire the manifest (`<service>` + intent-filter + `minCarApiLevel` meta-data + `automotive_app_desc.xml` reference). Validate via DHU that the app is discoverable and the static screen renders. **This phase alone should resolve the Pattern 3 compliance question** — try both a template-only screen and (as a throwaway spike, not committed code) a `NavigationTemplate`+`Surface` screen, to make the category decision concretely rather than abstractly.
2. **Shared-ownership refactor (still no car-facing GPS behavior change).** Introduce `TachimetroApplication`, move `GpsSpeedProvider` construction there, update `MainActivity` to read the shared instance, remove the `MainActivity.onDestroy()` `.close()` call (Anti-Pattern 2). Regression-check the phone screen behaves identically — this phase touches shared infrastructure but should produce zero visible change on the phone.
3. **Wire real GPS data into `SpeedScreen`.** Add the `repeatOnLifecycle(STARTED) { provider.state.collect { ... } }` collector inside `SpeedScreen`/its renderer (Pattern 2/4), rendering `SpeedState.Reading(kmh)` via whichever path Phase 1's spike validated. Test on DHU with a mock location provider (or a route-playback-style mock, mirroring how the existing GPS engine was originally verified per PROJECT.md's Fase 2 history).
4. **`Searching`/`NoSignal` parity.** Mirror `MainActivity.updatePlaceholder()`'s handling of `SpeedState.Searching`/`SpeedState.NoSignal` (the "Ricerca segnale GPS..." message) into the car screen, so both surfaces degrade identically when GPS is unavailable — this is explicitly called out as an in-scope requirement in PROJECT.md.
5. **Permission-not-granted handling on the car side.** Since `SpeedScreen` can be reached without `MainActivity` ever running (see Data Flow point 2), add an explicit car-screen state for "location permission not yet granted," distinct from `Searching`/`NoSignal` — this has no phone-side equivalent to mirror, it's new.
6. **Production hardening.** Replace `HostValidator.ALLOW_ALL_HOSTS_VALIDATOR` with a real allow-list validator (Anti-Pattern 4); verify dark-mode/`onCarConfigurationChanged` handling is a no-op given the app is already permanently black/high-contrast; verify no crash on rapid connect/disconnect cycling (Anti-Pattern 5); real-vehicle or DHU-based checkpoint before considering the milestone done.

## Sources

- [Use the Android for Cars App Library](https://developer.android.com/training/cars/apps) — HIGH, official overview
- [Create your CarAppService and Session](https://developer.android.com/training/cars/apps/library/carappservice-session) — HIGH, official
- [CarAppService, Session, and Screen lifecycles](https://developer.android.com/training/cars/apps/library/lifecycles) — HIGH, official (lifecycle model confirmed; process-model detail cross-verified via the codelab below)
- [Learn Car App Library fundamentals (codelab)](https://developer.android.com/codelabs/car-app-library-fundamentals) — HIGH, official, most detailed source used for manifest structure, process model ("host can bind without any Activity running," same-process confirmation), and minimal build steps
- [Draw maps](https://developer.android.com/training/cars/apps/library/draw-maps) — HIGH, official, `SurfaceCallback`/threading/`VirtualDisplay`+`Presentation` alternative, dark-mode handling
- [Build a navigation app](https://developer.android.com/training/cars/apps/navigation) — HIGH, official, `NAVIGATION_TEMPLATES`/`ACCESS_SURFACE` permission requirements
- [Build a point of interest app](https://developer.android.com/training/cars/apps/poi) — HIGH, official, confirms Surface access boundary (POI apps do not get raw Surface; `PlaceListMapTemplate` vs `MapWithContentTemplate` distinction)
- [Build a weather app](https://developer.android.com/training/cars/apps/weather) — MEDIUM-HIGH, official, `CarIcon`+custom `Bitmap` pattern for non-navigation custom graphics
- [Car app quality guidelines](https://developer.android.com/docs/quality-guidelines/car-app-quality) — HIGH, official, source of the `NF-1`/`NF-2`/`SA-1` rules underpinning the central compliance finding
- [Refresh the contents of a template](https://developer.android.com/training/cars/apps/library/refresh-template) / [Template restrictions](https://developer.android.com/training/cars/apps/library/template-restrictions) — HIGH for the "5 templates per task" quota and same-type/same-content exemption existing; LOW/flagged for whether continuously-changing numeric text reliably qualifies for the exemption long-term (not spelled out in the fetched content)
- [androidx.car.app releases](https://developer.android.com/jetpack/androidx/releases/car-app) — MEDIUM, version numbers/artifact names (`app`, `app-projected`, `app-automotive`, `app-testing`) confirmed as of the last-checked stable (1.7.0); re-verify current version at implementation time
- [Connection API (`CarConnection`)](https://developer.android.com/training/cars/apps/library/connection-api) — MEDIUM, confirms `CarConnection(context).type` LiveData is usable from outside a `Session`, relevant to the phone-side "is Android Auto connected" question referenced in PROJECT.md
- 9to5Google, "Android Auto finally starts rolling out a speedometer in Google Maps" (2026-07-19) and Android Authority coverage of the same — MEDIUM, corroborating (not authoritative) evidence that live-speed-on-Surface is treated as a navigation-app feature by the platform's biggest first-party example
- `app/src/main/java/com/sed/tachimetro/MainActivity.kt`, `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt`, `.planning/PROJECT.md` — this repository, read directly for the existing architecture and stated milestone scope

---
*Architecture research for: Android Auto (Car App Library) integration into an existing single-Activity Android app*
*Researched: 2026-08-31*
