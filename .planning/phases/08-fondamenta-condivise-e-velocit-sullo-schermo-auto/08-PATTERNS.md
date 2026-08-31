# Phase 8: Fondamenta Condivise e Velocità sullo Schermo Auto - Pattern Map

**Mapped:** 2026-08-31
**Files analyzed:** 11 (7 new, 4 modified)
**Analogs found:** 7 / 11 (4 have no in-repo analog — new Car App Library domain; research docs substitute)

**Important scope note carried from CONTEXT.md/UI-SPEC.md:** the category is **POI** with a standard **template** (`PaneTemplate`/`Row`), **not** `NAVIGATION`/`SurfaceCallback` (D-00a, locked). `.planning/research/ARCHITECTURE.md` and `STACK.md` explore a Surface/NavigationTemplate path in depth (Pattern 3/4, "The One Finding That Changes the Plan") — that discussion is **superseded** for this phase. Only the parts of those two documents that are path-agnostic (Application-scoped `GpsSpeedProvider`, `Session`/`Screen` lifecycle mirroring, manifest scaffolding shape, gradle dependency) are used below; every Surface/`NAVIGATION`-specific snippet has been adapted to POI/template equivalents or dropped.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `app/src/main/java/com/sed/tachimetro/TachimetroApplication.kt` | provider/config (Application subclass) | event-driven (singleton lifecycle, lazy init) | `MainActivity.setupGpsCollection()` (construction site) + `GpsSpeedProvider` itself | role-match (no `Application` subclass exists yet; pattern ported from `.planning/research/ARCHITECTURE.md` Pattern 1, which was authored by reading this exact codebase) |
| `app/src/main/java/com/sed/tachimetro/car/TachimetroCarAppService.kt` | service (entry point) | event-driven (host binds via Binder IPC) | none in-repo | no-analog — new domain; use `.planning/research/ARCHITECTURE.md`/`STACK.md` scaffolding |
| `app/src/main/java/com/sed/tachimetro/car/TachimetroCarSession.kt` | controller/session | event-driven | none in-repo | no-analog — new domain |
| `app/src/main/java/com/sed/tachimetro/car/SpeedScreen.kt` | component/screen (template-driven UI) | streaming (1 Hz `StateFlow` → `invalidate()` → `onGetTemplate()`) | `MainActivity.setupGpsCollection()` + `MainActivity.updatePlaceholder()` | role-match — same reactive collection shape (`repeatOnLifecycle(STARTED)`), same `SpeedState` `when` branching, different rendering target |
| `app/src/main/res/xml/automotive_app_desc.xml` | config | — | none (new config file type) | no-analog — fixed boilerplate, see STACK.md |
| `app/src/main/AndroidManifest.xml` | config | — | existing `<activity>` declaration | role-match — same manifest declaration conventions |
| `app/src/main/java/com/sed/tachimetro/MainActivity.kt` | controller/activity (existing, modified) | request-response / lifecycle | itself (before/after) | exact |
| `app/build.gradle.kts` | config | — | existing `dependencies {}` block | exact |
| `gradle/libs.versions.toml` | config | — | existing `[versions]`/`[libraries]` entries | exact |
| `app/src/main/res/values/strings.xml` | config (resource) | — | existing `searching_gps_signal` entry | exact |
| `app/src/test/java/com/sed/tachimetro/car/*Test.kt` (only if planner extracts a pure content-mapping function, see below) | test | transform | `app/src/test/java/com/sed/tachimetro/gps/GpsSpeedProviderStateTest.kt` | exact |

## Pattern Assignments

### `app/src/main/java/com/sed/tachimetro/TachimetroApplication.kt` (provider/config, event-driven)

**Analog:** No `Application` subclass exists in the repo today (`AndroidManifest.xml` has no `android:name` on `<application>`, confirmed by direct read). The closest real precedent is how `MainActivity` already constructs `GpsSpeedProvider` defensively with `applicationContext` — this phase promotes that same instance one level up.

**Current construction site to change** (`app/src/main/java/com/sed/tachimetro/MainActivity.kt:209-214`):
```kotlin
private fun setupGpsCollection() {
    // WR-04: pass applicationContext, not the Activity, so GpsSpeedProvider (and the
    // FusedLocationProviderClient it wraps) never retains an Activity reference.
    gpsSpeedProvider = GpsSpeedProvider(applicationContext)
    ...
```

**Target pattern** (adapted from `.planning/research/ARCHITECTURE.md` Pattern 1 — authored by reading this exact codebase, not generic boilerplate):
```kotlin
package com.sed.tachimetro

import android.app.Application
import com.sed.tachimetro.gps.GpsSpeedProvider

class TachimetroApplication : Application() {
    // WR-04 (extended to Application scope): applicationContext is already what
    // GpsSpeedProvider's constructor expects (context.applicationContext internally,
    // see GpsSpeedProvider.kt:53) -- no Activity ever passed in.
    val gpsSpeedProvider: GpsSpeedProvider by lazy { GpsSpeedProvider(applicationContext) }
}
```

**Manifest wiring** — add `android:name=".TachimetroApplication"` to the existing `<application>` tag in `app/src/main/AndroidManifest.xml:13`.

**Why no internal change to `GpsSpeedProvider` is needed:** its public contract (`class GpsSpeedProvider(context: Context)`, `val state: StateFlow<SpeedState>` built with `.stateIn(scope, SharingStarted.WhileSubscribed(), ...)`, `GpsSpeedProvider.kt:138-147`) already ref-counts multiple independent collectors correctly — see Shared Patterns below.

---

### `app/src/main/java/com/sed/tachimetro/car/TachimetroCarAppService.kt` (service, event-driven)

**Analog:** none in-repo (first `CarAppService` in the project). Source: `.planning/research/ARCHITECTURE.md` Component Responsibilities table + Anti-Pattern 4; `.planning/research/STACK.md` Installation section (manifest/service shape only — category and permissions corrected to POI, no Surface, per UI-SPEC.md/D-00a).

**Core pattern:**
```kotlin
package com.sed.tachimetro.car

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

class TachimetroCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator =
        HostValidator.ALLOW_ALL_HOSTS_VALIDATOR // scaffold/DHU-only for Phase 8;
        // Anti-Pattern 4 (ARCHITECTURE.md): replace with a real allow-list before any
        // release build -- explicit Phase 11 scope, do not front-run here.

    override fun onCreateSession(): Session = TachimetroCarSession()
}
```

**Manifest declaration** — analog is the existing `<activity>` block in `app/src/main/AndroidManifest.xml:23-32`, same declarative conventions (`android:exported`, `android:label`, intent-filter):
```xml
<!-- Existing MainActivity declaration for structural comparison (AndroidManifest.xml:23-32) -->
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:label="@string/app_name"
    android:theme="@style/Theme.Tachimetro">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>

<!-- NEW service, same structural pattern, POI category per D-00a (NOT NAVIGATION --
     STACK.md:84-93's example uses NAVIGATION; this project locked POI pre-phase) -->
<service
    android:name=".car.TachimetroCarAppService"
    android:exported="true"
    android:label="@string/app_name">
    <intent-filter>
        <action android:name="androidx.car.app.CarAppService" />
        <category android:name="androidx.car.app.category.POI" />
    </intent-filter>
</service>
```

**Required `<application>`-level meta-data** (STACK.md:76-82, category/permissions trimmed — no `NAVIGATION_TEMPLATES`/`ACCESS_SURFACE`, since Surface is explicitly not used, UI-SPEC.md "Surface/Canvas access: Not used"):
```xml
<meta-data
    android:name="com.google.android.gms.car.application"
    android:resource="@xml/automotive_app_desc" />
<meta-data
    android:name="androidx.car.app.minCarApiLevel"
    android:value="1" />
```

---

### `app/src/main/java/com/sed/tachimetro/car/TachimetroCarSession.kt` (controller/session, event-driven)

**Analog:** none in-repo. Source: `.planning/research/ARCHITECTURE.md` Component Responsibilities table.

**Core pattern:**
```kotlin
package com.sed.tachimetro.car

import androidx.car.app.Screen
import androidx.car.app.Session

class TachimetroCarSession : Session() {
    override fun onCreateScreen(intent: android.content.Intent): Screen =
        SpeedScreen(carContext)
}
```

---

### `app/src/main/java/com/sed/tachimetro/car/SpeedScreen.kt` (component/screen, streaming)

**Analog:** `MainActivity.setupGpsCollection()` (`MainActivity.kt:209-228`) for the reactive collection shape, and `MainActivity.updatePlaceholder()` (`MainActivity.kt:371-420`) for the `SpeedState` branching/content logic — role-match, not exact, because the terminal action differs (`Screen.invalidate()` + rebuilt template vs. `TextView.text =`).

**Imports pattern to mirror** (`MainActivity.kt:33-38`, coroutines/lifecycle section):
```kotlin
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

import com.sed.tachimetro.gps.GpsSpeedProvider
import com.sed.tachimetro.gps.SpeedState
```

**Core reactive-collection pattern to port** (`MainActivity.kt:209-228`, `Session`/`Screen` both implement `LifecycleOwner` per ARCHITECTURE.md Pattern 2, so `lifecycleScope`/`repeatOnLifecycle` carry over unchanged in shape — no `permissionGranted` gate needed here per this phase's scope; UI-SPEC.md "Permission not yet granted" row explicitly defers that to Phase 9, this phase only needs to not crash if reached pre-permission):
```kotlin
// MainActivity.kt:209-228 -- pattern to port into SpeedScreen's init block
private fun setupGpsCollection() {
    gpsSpeedProvider = GpsSpeedProvider(applicationContext)
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            permissionGranted.collectLatest { granted ->
                if (granted) {
                    gpsSpeedProvider.state.collect { state -> updatePlaceholder(state) }
                }
            }
        }
    }
}
```

**Content/state-branching pattern to mirror** (`MainActivity.kt:371-386`, this exact `Searching`/`NoSignal` unification is called out by UI-SPEC.md States table as the thing to mirror):
```kotlin
// MainActivity.kt:371-386 -- the exact branch shape SpeedScreen's onGetTemplate() should mirror
when (state) {
    is SpeedState.Searching, is SpeedState.NoSignal -> {
        unitText.visibility = View.GONE
        applyMessageAutosize()
        messageText.text = getString(R.string.searching_gps_signal)
    }
    is SpeedState.Reading -> {
        unitText.visibility = View.VISIBLE
        applySpeedAutosize()
        messageText.text = state.kmh.toString()
        // ... max/distance side effects: NOT ported to SpeedScreen (car screen shows
        // only speed + no-signal per UI-SPEC.md Content Contract; max/distance stay
        // phone-only, out of milestone scope)
    }
}
```

**Adapted for `PaneTemplate`/`Row` per D-01/UI-SPEC "Content Contract"** (no code example exists in-repo or in research docs for the POI/`PaneTemplate` path specifically — ARCHITECTURE.md's examples are all Surface-path; this shape is derived directly from the `androidx.car.app.model.PaneTemplate`/`Row`/`Pane` API and UI-SPEC's locked content rules, not copied from an existing source):
```kotlin
class SpeedScreen(carContext: CarContext) : Screen(carContext) {
    private val provider = (carContext.applicationContext as TachimetroApplication).gpsSpeedProvider
    private var latestState: SpeedState = SpeedState.Searching

    init {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                provider.state.collect { state ->
                    latestState = state
                    invalidate() // D-05: 1 Hz, paced by GpsSpeedProvider's own ticker --
                                 // no separate car-side timer (Pattern 4, ARCHITECTURE.md)
                }
            }
        }
    }

    override fun onGetTemplate(): Template {
        val row = when (val state = latestState) {
            is SpeedState.Reading -> Row.Builder()
                .setTitle(state.kmh.toString()) // D-01: digits only, no unit appended
                .addText(carContext.getString(R.string.unit_kmh)) // D-01: separate unit slot
                .build()
            is SpeedState.Searching, is SpeedState.NoSignal -> Row.Builder()
                .setTitle(carContext.getString(R.string.car_searching_gps_signal)) // D-02
                .build()
        }
        return PaneTemplate.Builder(Pane.Builder().addRow(row).build())
            .setHeaderAction(Action.APP_ICON) // D-03: icon-only header, no text title/branding
            .build()
    }
}
```

---

### `app/src/main/res/xml/automotive_app_desc.xml` (config)

**Source:** `.planning/research/STACK.md:64-69` (fixed boilerplate, no in-repo analog possible — first file of this kind):
```xml
<automotiveApp>
    <uses name="template" />
</automotiveApp>
```

---

### `app/src/main/AndroidManifest.xml` (modified, config)

**Current full file for reference** (`app/src/main/AndroidManifest.xml:1-35`) — the `<application>` tag (line 13) needs `android:name=".TachimetroApplication"` added, and the new `<service>` block (see `TachimetroCarAppService` section above) inserted after the existing `<activity>` block (after line 32), plus the two `<meta-data>` entries inside `<application>`. No new `<uses-permission>` needed beyond the existing `ACCESS_FINE_LOCATION` (line 9-11) — POI/template path requires no `NAVIGATION_TEMPLATES`/`ACCESS_SURFACE` (UI-SPEC.md explicit: "Surface/Canvas access: Not used").

---

### `app/src/main/java/com/sed/tachimetro/MainActivity.kt` (modified, exact self-analog)

**Change 1 — construction site** (`MainActivity.kt:209-214`, `setupGpsCollection()`):
```kotlin
// BEFORE:
gpsSpeedProvider = GpsSpeedProvider(applicationContext)

// AFTER (Pattern 1, ARCHITECTURE.md — read the shared, Application-scoped instance
// instead of constructing a new one):
gpsSpeedProvider = (application as TachimetroApplication).gpsSpeedProvider
```

**Change 2 — remove now-incorrect teardown** (`MainActivity.kt:288-294`, `onDestroy()`):
```kotlin
// Current (to be removed per Anti-Pattern 2, ARCHITECTURE.md):
override fun onDestroy() {
    gpsSpeedProvider.close()
    chargingStateProvider.close() // stays -- ChargingStateProvider is NOT promoted
                                   // to Application scope this phase, still Activity-owned
    super.onDestroy()
}
```
Why: `Session`/`Screen` can be alive collecting `state` independent of `MainActivity`'s existence (host can bind to `CarAppService` without any Activity running — ARCHITECTURE.md, confirmed HIGH-confidence process-model fact). Cancelling the shared scope from `MainActivity.onDestroy()` would kill GPS updates for an active car screen the moment the phone Activity is destroyed. `WhileSubscribed()` already handles stopping the *upstream* location updates correctly when the last collector (from either surface) detaches — `.close()` was always a secondary defensive teardown, not the primary stop mechanism, per the existing code comment at `GpsSpeedProvider.kt:149-154`.

---

### `app/build.gradle.kts` + `gradle/libs.versions.toml` (modified, exact self-analog)

**Analog — existing dependency-addition pattern** (`app/build.gradle.kts:71-81`):
```kotlin
dependencies {
    implementation(libs.appcompat)
    implementation(libs.activity)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
```
**Addition** (same alphabetical/grouped convention, version per STACK.md — verify 1.7.0 is still latest stable at implementation time):
```kotlin
implementation(libs.car.app)
androidTestImplementation(libs.car.app.testing)
```

**`gradle/libs.versions.toml` analog** (existing `[versions]`/`[libraries]` entries, e.g. `playServicesLocation`/`play-services-location` at lines 10 and 22):
```toml
[versions]
carApp = "1.7.0"

[libraries]
car-app = { group = "androidx.car.app", name = "app", version.ref = "carApp" }
car-app-testing = { group = "androidx.car.app", name = "app-testing", version.ref = "carApp" }
```

---

### `app/src/main/res/values/strings.xml` (modified, exact self-analog)

**Analog** (`strings.xml:8`):
```xml
<string name="searching_gps_signal">Ricerca segnale GPS...</string>
```
**New entry to add** (D-02, exact locked text, suggested identifier per CONTEXT.md "Claude's Discretion"):
```xml
<string name="car_searching_gps_signal">Ricerca segnale...</string>
```
Also needed for the `Row` unit text (D-01) — `unit_kmh` already exists (`strings.xml:10`, `"km/h"`) and can be reused as-is; no new resource needed for that slot.

---

### Optional: pure content-mapping helper (only if planner extracts one)

**Analog:** `deriveSpeedState()` (`GpsSpeedProvider.kt:170-179`) and `MainActivity.updatePlaceholder()`'s `when` branch (`MainActivity.kt:371-386`) — the project's established "Function Design" convention (CLAUDE.md) prefers small, pure, framework-free functions for anything unit-testable. If the planner chooses to extract `SpeedScreen`'s `SpeedState → (title, subtitle)` mapping into a top-level pure function (e.g. `car/CarSpeedContent.kt`), the test analog is:

**Test pattern to mirror** (`app/src/test/java/com/sed/tachimetro/gps/GpsSpeedProviderStateTest.kt:1-20`):
```kotlin
package com.sed.tachimetro.gps

import org.junit.Assert.assertEquals
import org.junit.Test

class GpsSpeedProviderStateTest {
    @Test
    fun noAcceptedFixYet_returnsSearching() {
        val result = deriveSpeedState(lastKmh = null, lastDeltaMeters = 0f, now = 10_000L, lastAcceptedAtMs = 0L)
        assertEquals(SpeedState.Searching, result)
    }
    // ... plain JUnit, no Android runtime, no coroutines-test (WR-02 convention)
}
```
This is optional — the planner may instead keep the `when` inline in `SpeedScreen.onGetTemplate()` (as MainActivity does), trading testability for simplicity, consistent with how `updatePlaceholder()` itself is not extracted into a pure function today.

## Shared Patterns

### Application-scoped shared `StateFlow` (the phase's central pattern)
**Source:** `GpsSpeedProvider.kt:138-147` (`state: StateFlow<SpeedState>` built via `.stateIn(scope, SharingStarted.WhileSubscribed(), initialValue = SpeedState.Searching)`)
**Apply to:** `TachimetroApplication.kt` (owner), `MainActivity.kt` (existing collector, construction site changed), `car/SpeedScreen.kt` (new collector)
```kotlin
val state: StateFlow<SpeedState> = combine(
    acceptedReadings.map { it as AcceptedReading? }.onStart { emit(null) },
    ticker,
) { last, now ->
    deriveSpeedState(last?.kmh, last?.deltaMeters ?: 0f, now, lastAcceptedUpdateAtMs)
}.stateIn(
    scope = scope,
    started = SharingStarted.WhileSubscribed(),
    initialValue = SpeedState.Searching,
)
```
No changes needed inside this class — `WhileSubscribed()` is inherently ref-counted across any number of independent collectors, which is exactly what two entry points (phone Activity + car Session/Screen) need.

### `repeatOnLifecycle(STARTED)` collection discipline
**Source:** `MainActivity.kt:215-228` (D-07 in existing code comments)
**Apply to:** `car/SpeedScreen.kt` (new collector, same shape) — `Session`/`Screen` both implement `LifecycleOwner` and expose `lifecycleScope`, so the pattern ports unchanged.
```kotlin
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        gpsSpeedProvider.state.collect { state -> /* render */ }
    }
}
```

### `applicationContext`, never `Activity` (WR-04)
**Source:** `GpsSpeedProvider.kt:50-53` comment + constructor
**Apply to:** `TachimetroApplication.kt` (constructs `GpsSpeedProvider(applicationContext)`, natively available on `Application`), `car/SpeedScreen.kt` (reads `carContext.applicationContext as TachimetroApplication`, never holds `carContext` itself longer than the `Screen`'s own lifetime)

### Sealed-state `when` branching for degraded states
**Source:** `MainActivity.kt:371-386` (`SpeedState.Searching`/`SpeedState.NoSignal` unified under one message)
**Apply to:** `car/SpeedScreen.kt`'s `onGetTemplate()` — same two-branch shape, different terminal action (template `Row` instead of `TextView.text`), different (shorter) copy per D-02.

### Manifest declaration conventions
**Source:** `AndroidManifest.xml:23-32` (`<activity>` block: `android:exported`, `android:label="@string/app_name"`, intent-filter)
**Apply to:** the new `<service>` block for `TachimetroCarAppService` — same attribute style, `android:exported="true"` required for the host to bind.

## No Analog Found

| File | Role | Data Flow | Reason |
|---|---|---|---|
| `car/TachimetroCarAppService.kt` | service | event-driven | First `CarAppService` in the project — no prior Android service of any kind exists in this single-Activity codebase. Use `.planning/research/ARCHITECTURE.md`/`STACK.md` scaffolding (category/permissions adapted to POI, not NAVIGATION, per D-00a). |
| `car/TachimetroCarSession.kt` | controller/session | event-driven | Same reason — new Car App Library domain, no prior `Session` analog. |
| `car/SpeedScreen.kt` (template-building portion only; the reactive-collection portion has a strong analog, see above) | component/screen | streaming | The `PaneTemplate`/`Row` construction has no in-repo or research-doc code example — research docs (`ARCHITECTURE.md`) only worked examples for the Surface/`NavigationTemplate` path, which this phase does not use (D-00a). Derived directly from UI-SPEC.md's Content Contract and the `androidx.car.app.model` API shape instead. |
| `res/xml/automotive_app_desc.xml` | config | — | New file type, fixed boilerplate per STACK.md, no analog needed. |

## Metadata

**Analog search scope:** `app/src/main/java/com/sed/tachimetro/**`, `app/src/test/java/com/sed/tachimetro/**`, `app/src/main/AndroidManifest.xml`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `app/src/main/res/values/strings.xml`, `.planning/research/{ARCHITECTURE,STACK}.md` (substitute source for the new Car App Library domain, no in-repo precedent exists)
**Files scanned:** 12 Kotlin source files, 6 test files, 1 manifest, 2 build/version files, 1 strings resource, 2 research documents
**Pattern extraction date:** 2026-08-31
