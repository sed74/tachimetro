# Phase 2: Motore GPS - Pattern Map

**Mapped:** 2026-07-07
**Files analyzed:** 6 (2 new, 4 modified)
**Analogs found:** 4 / 6 (2 modified-in-place self-analogs are exact; 2 new files have no in-repo analog — codebase is only Phase 1, no service/model layer exists yet)

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `gradle/libs.versions.toml` | config | batch (dependency declarations) | itself (extend existing `[versions]`/`[libraries]` tables) | exact (self) |
| `app/build.gradle.kts` | config | batch (dependency wiring) | itself (extend existing `dependencies {}` block) | exact (self) |
| `app/src/main/java/com/sed/tachimetro/gps/SpeedState.kt` | model | transform | none in repo | no analog — use RESEARCH.md Code Examples |
| `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt` | service | streaming/event-driven | none in repo | no analog — use RESEARCH.md Code Examples |
| `app/src/main/java/com/sed/tachimetro/MainActivity.kt` | controller (Activity) | event-driven (lifecycle + Flow collection) | itself (`MainActivity.kt`, Phase 1, permission flow) | exact (self) — same file, extend existing structure |
| `app/src/main/res/values/strings.xml` | config (resources) | — | itself (extend existing `<resources>` list) | exact (self) |

**Why no in-repo analog for the two new Kotlin files:** the codebase currently contains exactly one Activity and no `service`/`model`/`gps`/`data` package. Phase 1 established only the Activity + permission-flow + XML-layout precedent. There is no prior "wrap an Android callback API in a Flow" or "sealed-class state model" file to copy structurally. The planner should use `02-RESEARCH.md`'s "Code Examples" section (`callbackFlow` bridge, `SpeedState` sealed class sketch) as the primary source pattern for these two files, cross-checked against the conventions extracted below (package placement, import grouping, Suppress usage) from the existing codebase.

## Pattern Assignments

### `gradle/libs.versions.toml` (config, batch)

**Analog:** itself — current file, full contents already read.

**Current structure** (`gradle/libs.versions.toml` lines 1-26):
```toml
[versions]
agp = "9.1.1"
junit = "4.13.2"
junitVersion = "1.1.5"
espressoCore = "3.5.1"
appcompat = "1.6.1"
material = "1.10.0"
constraintlayout = "2.2.1"
activity = "1.9.3"

[libraries]
junit = { group = "junit", name = "junit", version.ref = "junit" }
ext-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
material = { group = "com.google.android.material", name = "material", version.ref = "material" }
constraintlayout = { group = "androidx.constraintlayout", name = "constraintlayout", version.ref = "constraintlayout" }
activity = { group = "androidx.activity", name = "activity-ktx", version.ref = "activity" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
# NOTE: no separate Kotlin Gradle Plugin declared here. ...
```

**Pattern to follow:** camelCase key in `[versions]` (e.g. `playServicesLocation`), matching kebab/dot-free `group:name` alias in `[libraries]` using `version.ref`. Add new entries at the bottom of each existing table (do not reorder existing entries). Per RESEARCH.md Standard Stack, add:
```toml
[versions]
playServicesLocation = "21.4.0"
kotlinxCoroutines = "1.10.2"
lifecycleRuntimeKtx = "2.11.0"

[libraries]
play-services-location = { group = "com.google.android.gms", name = "play-services-location", version.ref = "playServicesLocation" }
kotlinx-coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "kotlinxCoroutines" }
lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
```

**Constraint (from CLAUDE.md/RESEARCH.md):** do NOT add `org.jetbrains.kotlin.android` to `[plugins]` — AGP 9.1.1 built-in Kotlin support already active, adding the classic plugin breaks the build (documented failure in `01-01-SUMMARY.md`).

---

### `app/build.gradle.kts` (config, batch)

**Analog:** itself — current file, full contents already read (37 lines total).

**Current dependencies block** (lines 44-51):
```kotlin
dependencies {
    implementation(libs.appcompat)
    implementation(libs.activity)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
```

**Pattern to follow:** all app-facing (non-test) deps use `implementation(libs.*)` with the dot-separated alias matching the toml key (`play-services-location` → `libs.play.services.location`). Add new lines inside the existing `dependencies {}` block, grouped with the other `implementation(...)` lines above the `testImplementation`/`androidTestImplementation` lines:
```kotlin
implementation(libs.play.services.location)
implementation(libs.kotlinx.coroutines.core)
implementation(libs.lifecycle.runtime.ktx)
```
No other block in this file needs changes (namespace, compileSdk, minSdk/targetSdk, compileOptions, kotlin{} block all already correct/unrelated to this phase).

---

### `app/src/main/java/com/sed/tachimetro/gps/SpeedState.kt` (model, transform)

**Analog:** none in repo. Source: `02-RESEARCH.md` Code Examples ("Full GpsSpeedProvider sketch").

**Pattern to follow** (sealed class modeling three states, per D-01/D-02/D-09):
```kotlin
package com.sed.tachimetro.gps

sealed class SpeedState {
    data object Searching : SpeedState()           // D-01: shown until first valid fix
    data class Reading(val kmh: Int) : SpeedState() // D-09: whole km/h, no decimals
    data object NoSignal : SpeedState()             // D-02: no update for >5s
}
```

**Codebase convention alignment:** package `com.sed.tachimetro.gps` (new sub-package, per CONTEXT.md code_context "nuove classi in sotto-pacchetti coerenti"). No existing sealed-class or data-model file exists in this repo to compare naming/style against — this is a greenfield pattern for the project, follow the RESEARCH.md sketch as-is.

---

### `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt` (service, streaming/event-driven)

**Analog:** none in repo. Source: `02-RESEARCH.md` Code Examples + Architecture Patterns "Pattern 1: callbackFlow bridge".

**callbackFlow bridge pattern** (RESEARCH.md lines 211-225):
```kotlin
@Suppress("MissingPermission") // permission already verified by caller before starting collection
fun FusedLocationProviderClient.locationFlow(request: LocationRequest): Flow<Location> =
    callbackFlow {
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it) }
            }
        }
        requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { removeLocationUpdates(callback) }
    }
```

**Full sketch with filters** (RESEARCH.md lines 290-350) — filters/mapping order: accuracy filter (D-05) → hasSpeed() fallback to 0 (D-04) → m/s→km/h conversion + noise-floor rounding (D-03) → merge with 1s ticker for staleness detection (D-02):
```kotlin
class GpsSpeedProvider(context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    private val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
        .setMinUpdateIntervalMillis(1000L)
        .build()

    private val accuracyThresholdMeters = 50f  // D-05
    private val noiseFloorKmh = 2.0            // D-03

    @Suppress("MissingPermission") // caller (MainActivity) verifies ACCESS_FINE_LOCATION first
    private val rawLocations: Flow<Location> = callbackFlow {
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it) }
            }
        }
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { client.removeLocationUpdates(callback) }
    }

    private val filteredKmh: Flow<Int> = rawLocations
        .filter { !it.hasAccuracy() || it.accuracy <= accuracyThresholdMeters } // D-05
        .map { loc ->
            val kmh = if (loc.hasSpeed()) loc.speed * 3.6 else 0.0 // D-04
            if (kmh < noiseFloorKmh) 0 else kmh.roundToInt()        // D-03
        }
    // ... ticker + combine + stateIn -> StateFlow<SpeedState> (see RESEARCH.md for full sketch)
}
```

**Error handling pattern:** none needed beyond `awaitClose { removeLocationUpdates(callback) }` — this is the sole cleanup path (Anti-Pattern warning in RESEARCH.md: never call `removeLocationUpdates` from a manual lifecycle override, only from `awaitClose`).

**Permission pattern (do NOT duplicate):** `MainActivity.kt`'s `checkAndRequestPermission()`/`ContextCompat.checkSelfPermission` (lines 61-76, see MainActivity excerpt below) is the single source of truth for `ACCESS_FINE_LOCATION` state. `GpsSpeedProvider` must not re-check permission — suppress the lint warning at the call site instead (`@Suppress("MissingPermission")`), per RESEARCH.md Pitfall 1 and CONTEXT.md D-notes ("gestione dei permessi già negati, già coperta dalla Fase 1").

---

### `app/src/main/java/com/sed/tachimetro/MainActivity.kt` (controller/Activity, event-driven)

**Analog:** itself — current file (113 lines), full contents already read.

**Imports pattern** (lines 1-15) — `android.*` block first, blank line, then `androidx.*` block, both alphabetized within their group:
```kotlin
package com.sed.tachimetro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
```
New imports for this phase (`com.sed.tachimetro.gps.GpsSpeedProvider`, `com.sed.tachimetro.gps.SpeedState`, `androidx.lifecycle.lifecycleScope`, `androidx.lifecycle.Lifecycle`, `androidx.lifecycle.repeatOnLifecycle`, `kotlinx.coroutines.launch`) should be appended following this same two-group convention (project package first if any, then a third `androidx.lifecycle`/`kotlinx.coroutines` group after `androidx.*`).

**Field declaration pattern** (lines 19-20):
```kotlin
private lateinit var messageText: TextView
private lateinit var retryButton: Button
```
Follow this style for the new `GpsSpeedProvider` field — e.g. `private lateinit var gpsSpeedProvider: GpsSpeedProvider` initialized in `onCreate` alongside `messageText`/`retryButton`.

**onCreate wiring pattern** (lines 31-40):
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    messageText = findViewById(R.id.messageText)
    retryButton = findViewById(R.id.retryButton)
    retryButton.setOnClickListener { onRetryClicked() }

    checkAndRequestPermission()
}
```
This is the integration point: instantiate `GpsSpeedProvider(this)` here, and register the `lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { ... } }` collector here too (per RESEARCH.md Pattern 2 / Architecture Diagram), so it starts/stops with `onStart()`/`onStop()` per D-07 without hand-written lifecycle overrides.

**Existing lifecycle-override precedent** (lines 42-59, `onResume()`) — shows the established style for adding a lifecycle callback to this Activity (doc comment explaining *why*, then the logic):
```kotlin
override fun onResume() {
    super.onResume()
    // Re-check permission state whenever the activity comes back to the
    // foreground (e.g. returning from the system Settings screen opened
    // by openAppSettings()). Without this, granting the permission
    // externally leaves the UI stuck on the "denied" screen until the
    // app is force-killed and relaunched.
    if (ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        showReady()
    } else if (retryButton.visibility == View.VISIBLE) {
        showDenied()
    }
}
```
Note: per D-07, the new GPS-collection code must NOT be added as a manual `onStart()`/`onStop()` override pair calling `collect`/`cancel` by hand — use `repeatOnLifecycle` inside a `lifecycleScope.launch{}` started from `onCreate()` instead (RESEARCH.md Anti-Patterns explicitly warns against hand-rolled lifecycle callback registration).

**Display-update method pattern** (lines 93-112, `showReady()`/`showDenied()`) — small private methods that set `messageText.text` via `getString(R.string.*)`, no inline string literals:
```kotlin
private fun showReady() {
    retryButton.visibility = View.GONE
    messageText.text = getString(R.string.status_ready)
}

private fun showDenied() {
    retryButton.visibility = View.VISIBLE
    val permanentlyDenied =
        !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
    messageText.text = if (permanentlyDenied) {
        getString(R.string.permission_denied_permanent)
    } else {
        getString(R.string.permission_denied)
    }
    retryButton.text = if (permanentlyDenied) {
        getString(R.string.open_settings)
    } else {
        getString(R.string.retry)
    }
}
```
Follow this exact style for the new `updatePlaceholder(state: SpeedState)` method: a `when` on `SpeedState` (`Searching`/`Reading`/`NoSignal`) setting `messageText.text` to `getString(R.string.searching_gps_signal)` (new string) or a formatted `"$kmh km/h"` (no decimals, per D-09 — note this is the one case where a literal-formatted string, not `getString`, is appropriate since it's numeric interpolation, not a translatable phrase).

**Where `showReady()` currently gets called** (line 25, inside `requestPermissionLauncher` grant callback, and line 53/72, permission checks): per RESEARCH.md's Architectural Responsibility Map, `showReady()`'s current behavior (setting the black "Pronto" placeholder) is the exact spot Phase 3 will replace with real UI — for this phase, the GPS collector should only start once permission is confirmed granted (i.e., gate the `lifecycleScope.launch{...}` body's actual collection, or simply rely on `GpsSpeedProvider` never being asked to collect before `onCreate` runs `checkAndRequestPermission()` — planner's call per CONTEXT.md Claude's Discretion).

---

### `app/src/main/res/values/strings.xml` (config/resources)

**Analog:** itself — current file, full contents already read (8 lines).

**Current structure:**
```xml
<resources>
    <string name="app_name">Tachimetro</string>
    <string name="status_ready">Pronto</string>
    <string name="permission_denied">Permesso GPS necessario per funzionare</string>
    <string name="permission_denied_permanent">Permesso GPS negato. Aprire le impostazioni per abilitarlo</string>
    <string name="retry">Riprova</string>
    <string name="open_settings">Apri impostazioni</string>
</resources>
```

**Pattern to follow:** flat list, one `<string name="snake_case_or_lower_camel">` per line, Italian text only (per UI-05, already an established constraint from Phase 1). Add one new entry, e.g.:
```xml
<string name="searching_gps_signal">Ricerca segnale GPS...</string>
```
Naming convention observed: existing keys use `snake_case` (`status_ready`, `permission_denied_permanent`, `open_settings`) — follow the same casing for the new key.

---

## Shared Patterns

### Permission gating (do not duplicate)
**Source:** `app/src/main/java/com/sed/tachimetro/MainActivity.kt` lines 61-76 (`checkAndRequestPermission()`)
**Apply to:** `GpsSpeedProvider.kt` — must never re-check `ACCESS_FINE_LOCATION` itself; relies entirely on `MainActivity` having already confirmed grant before the Flow is collected. Use `@Suppress("MissingPermission")` at the `requestLocationUpdates()` call site instead of a redundant runtime check.

### Italian-only, string-resource-driven UI text
**Source:** `app/src/main/res/values/strings.xml` (all 6 existing entries) + `MainActivity.kt` `showReady()`/`showDenied()` (`getString(R.string.*)` usage)
**Apply to:** the new `updatePlaceholder(state: SpeedState)` method in `MainActivity.kt` and the new `searching_gps_signal` string — every user-facing phrase must be a string resource, in Italian, except the numeric `"$kmh km/h"` interpolation itself (D-09).

### Version-catalog-first dependency management
**Source:** `gradle/libs.versions.toml` + `app/build.gradle.kts` (existing `[versions]`/`[libraries]`/`dependencies{}` structure)
**Apply to:** all three new dependencies (`play-services-location`, `kotlinx-coroutines-core`, `lifecycle-runtime-ktx`) — declare in toml first, reference via `libs.*` alias in `build.gradle.kts`, never hardcode a version string directly in the `.kts` file.

### callbackFlow + repeatOnLifecycle (no manual onStart/onStop overrides for GPS)
**Source:** `02-RESEARCH.md` Architecture Patterns, Pattern 1 (lines 207-225) and Pattern 2 (lines 227-243); Anti-Patterns (lines 245-249)
**Apply to:** `GpsSpeedProvider.kt` (owns `requestLocationUpdates`/`removeLocationUpdates` exclusively via `awaitClose{}`) and `MainActivity.kt` (owns start/stop exclusively via `lifecycleScope.launch { repeatOnLifecycle(STARTED) { ... } }`, registered once in `onCreate()` — implements D-07 without hand-written `onStart()`/`onStop()` overrides).

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| `app/src/main/java/com/sed/tachimetro/gps/SpeedState.kt` | model | transform | First model/sealed-class file in the project; no `gps`/`data`/`model` package exists yet. Use `02-RESEARCH.md` Code Examples as the primary pattern source. |
| `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt` | service | streaming/event-driven | First non-Activity, non-UI class in the project; no prior "wrap an Android callback API in a Flow" precedent exists in this codebase. Use `02-RESEARCH.md` Code Examples (callbackFlow bridge + full sketch) as the primary pattern source, cross-checked against `MainActivity.kt`'s permission-gating convention (see Shared Patterns above). |

**Testing note:** `app/src/test/java/com/sed/tachimetro/ExampleUnitTest.java` (default AGP template, JUnit4, Java) is the only existing test file and establishes no real project convention (no Kotlin test dependency, no coroutines-test/Turbine for Flow testing configured in `libs.versions.toml`/`app/build.gradle.kts`). If the planner decides to unit-test `GpsSpeedProvider`'s filtering logic, that will require adding new test dependencies (e.g. `kotlinx-coroutines-test`) not currently declared — flag this as a planning decision, not a pattern to copy.

## Metadata

**Analog search scope:** `app/src/main/java/com/sed/tachimetro/` (entire package tree), `app/src/main/res/` (layout, values), `app/src/test/`, `app/src/androidTest/`, `gradle/libs.versions.toml`, `app/build.gradle.kts`
**Files scanned:** 21 (full `app/src/main` tree) + 2 config files + 2 default test files
**Pattern extraction date:** 2026-07-07
