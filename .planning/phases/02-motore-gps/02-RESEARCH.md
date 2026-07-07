# Phase 2: Motore GPS - Research

**Researched:** 2026-07-07
**Domain:** Android location services (FusedLocationProviderClient), Kotlin coroutines/Flow integration
**Confidence:** HIGH

## Summary

This phase wraps Google Play Services' `FusedLocationProviderClient` in a Kotlin `Flow`/`StateFlow` that emits speed readings (km/h) once per second, with quality filtering and a "no signal" state. The core API (`LocationRequest.Builder`, `Priority.PRIORITY_HIGH_ACCURACY`, `LocationCallback`) is stable, well-documented, and unchanged in shape for several years — confidence here is HIGH.

One important correction to a premise in `02-CONTEXT.md` (D-06): `kotlinx-coroutines-play-services` does **not** provide a Flow adapter for continuous location updates. It only converts single-shot `Task<T>` objects (e.g. `getCurrentLocation()`) into coroutine-friendly `await()`/`asDeferred()` calls. Continuous updates via `LocationCallback` must be wrapped manually with `callbackFlow { ... }` from `kotlinx-coroutines-core` (this is the standard, officially-recommended pattern — see Code Examples). `kotlinx-coroutines-play-services` is optional for this phase; it only becomes useful if a one-shot `getCurrentLocation()` call is added later. This does not change the locked outcome (speed exposed via Flow/StateFlow) — it changes which dependency actually does the work.

A second finding worth flagging early: the project already transitively depends on `androidx.lifecycle:lifecycle-runtime-ktx` (via the existing `libs.activity` / `androidx.activity:activity-ktx:1.9.3` dependency), which provides `lifecycleScope` and `repeatOnLifecycle(Lifecycle.State.STARTED)`. This is the current Android-recommended mechanism for collecting a Flow exactly between `onStart()`/`onStop()` — it directly implements D-07 without hand-written lifecycle callback overrides for the collection side. No new dependency is required to get this; adding an explicit version-catalog entry is a clarity improvement, not a functional requirement.

**Primary recommendation:** Wrap `FusedLocationProviderClient.requestLocationUpdates()` in a `callbackFlow<Location>` inside a small `GpsSpeedProvider` class (own package, e.g. `com.sed.tachimetro.gps`), apply filtering/mapping as Flow operators (accuracy filter → `hasSpeed()` fallback → m/s→km/h conversion → noise floor rounding), merge with a 1-second ticker to detect "no update for 5s" without relying on the still-`@FlowPreview` `Flow.timeout()` operator, and collect the resulting `StateFlow` from `MainActivity` via `lifecycleScope.launch { repeatOnLifecycle(STARTED) { ... } }`.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Reading raw GPS fixes (position + speed) | Device / OS (Google Play Services) | — | `FusedLocationProviderClient` is a Play Services process outside the app; the app only receives callbacks |
| Wrapping callbacks into Flow, filtering/quality logic | App layer — dedicated GPS engine class | — | Pure Kotlin logic, no Android UI dependency; should be unit-testable in isolation from `MainActivity` |
| Lifecycle-bound start/stop of updates | App layer — `MainActivity` (or a `LifecycleObserver` it owns) | — | Ties GPS engine start/stop to Activity visibility per D-07; single native Activity, no ViewModel/Fragment layer in this project |
| Displaying speed / "no signal" text | App layer — `MainActivity` (placeholder for this phase; real UI in Phase 3) | — | Placeholder reuses existing `messageText` TextView from Phase 1 |
| Permission state | App layer — `MainActivity` (already implemented Phase 1) | — | No changes needed this phase; GPS engine assumes permission already granted when started |

This is a single-module, single-Activity native Android app (no client/server split) — all tiers above collapse into "the app process," but the split between *GPS engine* (testable Kotlin class) and *Activity* (lifecycle glue + display) is the meaningful boundary for this phase.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Soglia "nessun segnale"**
- D-01: Il messaggio "Ricerca segnale GPS..." appare subito all'avvio finché non arriva il primo fix valido.
- D-02: Durante l'uso, se non arriva nessun aggiornamento di posizione per più di 5 secondi, il segnale è considerato perso e il messaggio "Ricerca segnale GPS..." ricompare.
- D-03: Da fermo, il GPS può riportare piccole velocità (0.3-1.5 km/h) per rumore. Applicare una soglia minima (es. ~2 km/h) sotto la quale il valore mostrato viene arrotondato a 0.
- D-04: Se la location arriva ma `hasSpeed()` è `false` (comune da fermo), trattare il valore come 0 km/h, non come "nessun segnale".
- D-05: Scartare le letture di posizione con accuratezza scarsa (raggio di incertezza elevato, indicativamente oltre ~30-50 metri) per evitare picchi di velocità fasulli da GPS impreciso — non aggiornare il valore mostrato con letture sotto la soglia di accuratezza accettabile.

**Architettura del dato velocità**
- D-06: Il motore GPS espone il valore di velocità tramite Kotlin Flow/StateFlow (richiede la dipendenza `kotlinx-coroutines-play-services` per collegare i callback di FusedLocationProviderClient a un Flow), non tramite callback/listener tradizionali.
  - **Research correction (see Summary):** `kotlinx-coroutines-play-services` does not provide this Flow adapter; a manual `callbackFlow` (from `kotlinx-coroutines-core`) is the correct mechanism. The Flow/StateFlow outcome itself is unaffected.

**Ciclo di vita aggiornamenti GPS**
- D-07: Gli aggiornamenti di posizione partono in `onStart()` e si fermano in `onStop()` dell'Activity (non `onResume`/`onPause`) — continuano a funzionare finché l'app è visibile (anche in multitasking/split-screen), si fermano solo quando l'app non è più visibile. Bilancia batteria e continuità di lettura.
- D-08 [informational, rilevante per Fase 4]: Se l'app va in background mentre l'utente guida, la velocità massima (Fase 4) continua a essere registrata solo mentre l'app è visibile — nessun tracciamento in background/Foreground Service previsto per v1.

**Verificabilità e formato del placeholder di test**
- D-09: Per rendere questa fase verificabile prima che la Fase 3 costruisca la UI reale, il placeholder nero "Pronto" della Fase 1 viene temporaneamente sostituito dal valore di velocità numerico intero (es. "42 km/h", nessun decimale) quando disponibile, e torna a mostrare il messaggio di ricerca segnale quando il segnale è perso. Questo comportamento placeholder verrà rimpiazzato dalla vera UI in Fase 3.
- D-10 [verifica]: Il checkpoint umano per questa fase va testato usando la funzione "Route playback" nei controlli estesi dell'emulatore Android (Extended Controls → Location → Routes), che simula un percorso GPS con velocità variabile — non solo una verifica statica da fermo.

### Claude's Discretion
- Dettagli implementativi di conversione m/s → km/h, gestione dei permessi già negati (già coperta dalla Fase 1), struttura interna delle classi/file del motore GPS.

### Deferred Ideas (OUT OF SCOPE)
- Toggle "Schermo sempre acceso" / "Schermo automatico" — già pianificato in Fase 5 (Gestione Schermo, SCRN-01/02/03), menzionato dall'utente durante la discussione di questa fase ma fuori scope qui.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| GPS-01 | L'utente vede la velocità attuale in km/h letta dal GPS del dispositivo, aggiornata 1 volta al secondo | `LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L).setMinUpdateIntervalMillis(1000L)` gives a steady ~1 Hz cadence; see Code Examples for m/s→km/h conversion and rounding |
| GPS-02 | L'utente vede un messaggio semplice quando il GPS non ha segnale (es. "Ricerca segnale GPS...") | Startup-state (D-01) handled by initial `StateFlow` value; runtime-loss (D-02) handled by a 1s ticker comparing `now - lastUpdateTimestamp` against a 5000ms threshold (see Common Pitfalls: avoid `Flow.timeout()`, still `@FlowPreview`) |
| GPS-03 | L'app legge la velocità tramite FusedLocationProviderClient (Google Play Services) | `com.google.android.gms:play-services-location:21.4.0` — verified current via Google's Maven `group-index.xml`; see Standard Stack |
</phase_requirements>

## Project Constraints (from CLAUDE.md)

- **Language:** Kotlin for application code — but **no separate Kotlin Gradle plugin**. AGP 9.1.1's built-in Kotlin support (`android.builtInKotlin`, default `true`) is already active and proven working (Phase 1). Do **not** add `org.jetbrains.kotlin.android` to `gradle/libs.versions.toml` or `app/build.gradle.kts` — it breaks the build in this AGP version (documented, tested failure in `01-01-SUMMARY.md`).
- **UI toolkit:** Traditional XML layouts (no Jetpack Compose) — the placeholder update in this phase must keep using `findViewById`/`TextView`, consistent with `activity_main.xml`.
- **GPS:** `FusedLocationProviderClient` (Google Play Services) is a project-level constraint (`PROJECT.md`), not just a phase decision — no `LocationManager`/raw GNSS fallback.
- **minSdk 30 / targetSdk 36 / compileSdk 36** — all APIs discussed in this research (including `getSpeedAccuracyMetersPerSecond()`, API 26+) are safely available.
- **Dependency management:** version-catalog-first — every new dependency goes into `gradle/libs.versions.toml` first, referenced via `libs.*` aliases in `app/build.gradle.kts`. Never hardcode a version string directly in `app/build.gradle.kts`.
- **Package convention:** new classes under `com.sed.tachimetro`, sub-package per feature area (e.g. `com.sed.tachimetro.gps`) — no such sub-package exists yet, this phase establishes it.
- **Workflow enforcement:** file changes must go through a GSD command (`/gsd-execute-phase` etc.) — not relevant to research output itself, but the planner should be aware execution won't bypass this.
- **JVM target:** Java 11 bytecode (`compileOptions.sourceCompatibility/targetCompatibility = VERSION_11`, Kotlin `jvmTarget = JVM_11`). The recommended dependencies (Play Services Location, kotlinx-coroutines-core) all target JVM 8 minimum bytecode and are compatible.

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|---------------|
| `com.google.android.gms:play-services-location` | 21.4.0 | Provides `FusedLocationProviderClient`, `LocationRequest`, `LocationCallback`, `Priority` | The only Google-supported way to read fused (GPS+network+sensor) location/speed on Android; locked by `PROJECT.md` |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` | 1.10.2 | Provides `callbackFlow`, `Flow`, `StateFlow`, structured concurrency primitives | Standard Kotlin async/reactive toolkit; required to build the callback→Flow bridge itself |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.11.0 (already transitively present at 2.6.1 via `activity-ktx:1.9.3`) | Provides `lifecycleScope` + `repeatOnLifecycle(Lifecycle.State.STARTED)` | Collecting the GPS `StateFlow` from `MainActivity` exactly between `onStart()`/`onStop()` (implements D-07 on the collection side) — add an explicit version-catalog entry for clarity even though it already resolves transitively |
| `org.jetbrains.kotlinx:kotlinx-coroutines-play-services` | 1.10.2 | Adds `Task<T>.await()` / `.asDeferred()` | **Optional for this phase.** Only useful if a one-shot `getCurrentLocation()` call is added (e.g. to warm up the first fix faster). Not needed to build the continuous-updates Flow — see Summary correction to D-06. Recommend planner confirm with user whether to include it now (future-proofing) or add it only when actually used, to avoid an unused dependency. |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `callbackFlow` (manual wrapping) | `Flow.timeout()` operator for "no signal after 5s" | `timeout()` is still `@FlowPreview` (weak compatibility guarantees) as of kotlinx-coroutines 1.10.2 — a manual 1s ticker comparing timestamps is simpler, has no experimental-API risk, and naturally reuses the "update once per second" requirement (GPS-01) as the same tick |
| `FusedLocationProviderClient` (Play Services) | `LocationManager` (raw Android GNSS API) | Rejected — `PROJECT.md` locks Play Services; `LocationManager` also lacks the fused/blended accuracy and battery optimizations FusedLocationProviderClient provides |
| Manual `onStart()`/`onStop()` overrides calling `collect`/`cancel` by hand | `lifecycleScope.launch { repeatOnLifecycle(STARTED) { flow.collect {...} } }` | The `repeatOnLifecycle` pattern is fewer lines, avoids manual `Job` bookkeeping, and is the current official guidance for exactly this "collect while visible" scenario — recommended over hand-rolled overrides |

**Installation:**
```kotlin
// gradle/libs.versions.toml
[versions]
playServicesLocation = "21.4.0"
kotlinxCoroutines = "1.10.2"
lifecycleRuntimeKtx = "2.11.0"

[libraries]
play-services-location = { group = "com.google.android.gms", name = "play-services-location", version.ref = "playServicesLocation" }
kotlinx-coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "kotlinxCoroutines" }
lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
# Optional, only if a one-shot getCurrentLocation()/Task.await() call is added:
# kotlinx-coroutines-play-services = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-play-services", version.ref = "kotlinxCoroutines" }
```
```kotlin
// app/build.gradle.kts
dependencies {
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.lifecycle.runtime.ktx)
}
```

**Version verification:** Verified directly against authoritative source registries (not `npm view`/`pip` — this is a Gradle/Maven Android project):
- `play-services-location` 21.4.0 — confirmed as the highest listed version in Google's own Maven repository index: `https://dl.google.com/android/maven2/com/google/android/gms/group-index.xml` (fetched 2026-07-07)
- `kotlinx-coroutines-core` / `kotlinx-coroutines-play-services` 1.10.2 — confirmed via Maven Central's official search API (`search.maven.org/solrsearch/select`, `latestVersion` field, fetched 2026-07-07); release timestamp corresponds to ~April 2025
- `androidx.lifecycle:lifecycle-runtime-ktx` 2.11.0 — confirmed as the highest non-prerelease version in Google's Maven index: `https://dl.google.com/android/maven2/androidx/lifecycle/group-index.xml` (fetched 2026-07-07)
- Compatibility check: AGP 9.1.1's built-in Kotlin support bundles **Kotlin 2.2.10** as its compiler `[CITED: developer.android.com/build/migrate-to-built-in-kotlin, WebSearch cross-referenced against AGP 9.1 release notes]`. `kotlinx-coroutines` 1.10.2 predates Kotlin 2.2 (released ~April 2025) and was built against an older Kotlin API/language version; Kotlin maintains binary/source backward compatibility, so consuming it from a 2.2.10 compiler is expected to work, but this specific pairing was not build-verified in this research session — **flag for first-build verification** (see Assumptions Log A2).

## Package Legitimacy Audit

> This phase's dependencies are Gradle/Maven coordinates (Android/JVM ecosystem), not npm/PyPI/crates. `slopcheck` does not support this ecosystem, so the standard gate does not apply as written. Instead, each package was verified directly against its **authoritative first-party registry index** (Google's own `dl.google.com` Maven index for Google packages; Maven Central's official search API for JetBrains packages) rather than a general-purpose registry lookup — this is a stronger signal than an npm-style download/age heuristic would provide, since both publishers are the sole authoritative source for their own namespace (`com.google.android.gms.*` can only be published by Google; `org.jetbrains.kotlinx.*` can only be published by JetBrains).

| Package | Registry | Age | Publisher | Source Repo | Verification | Disposition |
|---------|----------|-----|-----------|--------------|--------------|-------------|
| `com.google.android.gms:play-services-location` | Google Maven (dl.google.com) | ~12 years (first released ~2013 as part of Play Services) | Google (first-party, closed-source SDK) | N/A — proprietary Google SDK, documented at developers.google.com/android/guides/setup | Confirmed present + latest version via Google's own `group-index.xml` | Approved |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` | Maven Central | ~8 years (project started 2017) | JetBrains (official Kotlin org) | github.com/Kotlin/kotlinx.coroutines | Confirmed via Maven Central official search API | Approved |
| `org.jetbrains.kotlinx:kotlinx-coroutines-play-services` | Maven Central | ~7 years | JetBrains (official Kotlin org, same repo/module as above) | github.com/Kotlin/kotlinx.coroutines | Confirmed via Maven Central official search API | Approved (optional — see Standard Stack note) |
| `androidx.lifecycle:lifecycle-runtime-ktx` | Google Maven (dl.google.com) | ~7 years (androidx.lifecycle since ~2018) | Google (AndroidX/Jetpack, first-party) | android.googlesource.com/platform/frameworks/support | Confirmed present + latest version via Google's own `group-index.xml` | Approved |

**Packages removed due to slopcheck [SLOP] verdict:** none (slopcheck not applicable to this ecosystem)
**Packages flagged as suspicious [SUS]:** none — all four packages are first-party publications from the two entities (Google, JetBrains) that own their respective namespaces; no third-party/community package of unknown provenance is recommended in this phase

## Architecture Patterns

### System Architecture Diagram

```
┌─────────────────────────────┐        ┌──────────────────────────────┐
│  Google Play Services        │        │  Android OS                   │
│  (external process)          │        │  (Lifecycle events)           │
│  - GNSS chip fusion           │        │  onStart() / onStop()         │
│  - LocationCallback delivery  │        └───────────────┬────────────────┘
└───────────────┬───────────────┘                        │
                │ onLocationResult(LocationResult)         │ drives
                ▼                                          ▼
┌───────────────────────────────────────────────────────────────────────┐
│ GpsSpeedProvider (com.sed.tachimetro.gps)                              │
│                                                                         │
│  callbackFlow<Location> {                                              │
│    requestLocationUpdates(request, callback, Looper.getMainLooper())   │
│    awaitClose { removeLocationUpdates(callback) }                      │
│  }                                                                      │
│      │                                                                  │
│      ▼ filter: accuracy > threshold (D-05) → drop                      │
│      ▼ map: hasSpeed()==false → 0 km/h (D-04)                          │
│      ▼ map: m/s → km/h, round; below noise floor → 0 (D-03)            │
│      ▼ merge with 1s ticker → no update in 5s → SpeedState.NoSignal    │
│                                                                          │
│  exposes: StateFlow<SpeedState>                                        │
│    sealed: Searching | Reading(kmh: Int) | NoSignal                    │
└───────────────────────────────┬─────────────────────────────────────┘
                                 │ collected via
                                 ▼
┌───────────────────────────────────────────────────────────────────────┐
│ MainActivity (existing, Phase 1)                                       │
│                                                                         │
│  onCreate(): create GpsSpeedProvider, register lifecycleScope collector│
│  lifecycleScope.launch {                                                │
│    repeatOnLifecycle(Lifecycle.State.STARTED) {                        │
│      gpsSpeedProvider.state.collect { state -> updatePlaceholder(state)│
│    }                                                                    │
│  }                                                                      │
│  → starts on onStart(), auto-cancels on onStop() (implements D-07)     │
│                                                                         │
│  updatePlaceholder(state): sets messageText.text to                    │
│    "Ricerca segnale GPS..." | "<N> km/h" per D-09                      │
└───────────────────────────────────────────────────────────────────────┘
```

A reader can trace the primary path: Play Services delivers a `Location` → `GpsSpeedProvider` filters/converts it into a `SpeedState` → `MainActivity` collects the `StateFlow` (only while visible) and writes it to the placeholder `TextView`.

### Recommended Project Structure
```
app/src/main/java/com/sed/tachimetro/
├── MainActivity.kt          # existing — add GpsSpeedProvider wiring + placeholder update
└── gps/
    ├── GpsSpeedProvider.kt  # wraps FusedLocationProviderClient in a Flow, applies filters
    └── SpeedState.kt        # sealed class: Searching / Reading(kmh: Int) / NoSignal
```

### Pattern 1: callbackFlow bridge for continuous location updates
**What:** Wrap `FusedLocationProviderClient.requestLocationUpdates()`/`removeLocationUpdates()` in a `callbackFlow`, since there is no built-in Flow adapter for this callback shape.
**When to use:** Any continuous (not one-shot) Play Services callback API without a native coroutine/Flow surface.
**Example:**
```kotlin
// Source: pattern confirmed via WebSearch (Google Codelabs "while-in-use-location",
// community references) + kotlinx-coroutines-core callbackFlow official docs
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

### Pattern 2: repeatOnLifecycle for onStart/onStop-scoped Flow collection
**What:** `lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { flow.collect { ... } } }` — the block (and thus the Flow collection, and thus the `callbackFlow`'s `awaitClose`) starts when the Activity reaches `STARTED` (`onStart()`) and is cancelled when it drops below `STARTED` (`onStop()`), then restarts automatically if the Activity becomes visible again.
**When to use:** Exactly the D-07 requirement — implements "starts in onStart(), stops in onStop()" without manually overriding those two lifecycle methods to call `collect`/`cancel` by hand.
**Example:**
```kotlin
// Source: androidx.lifecycle-runtime-ktx official pattern (CITED: developer.android.com
// lifecycle-aware coroutine collection guidance — well-established since lifecycle 2.4.0)
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // ...existing permission setup...
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            gpsSpeedProvider.state.collect { state -> updatePlaceholder(state) }
        }
    }
}
```

### Anti-Patterns to Avoid
- **Registering `LocationCallback` in `onCreate()`/manually in `onStart()`/`onStop()` without tying it to Flow cancellation:** leads to callback leaks if the Activity is destroyed while a request is in flight. Let `awaitClose {}` inside `callbackFlow` own the `removeLocationUpdates()` call — it always runs when the Flow collector is cancelled.
- **Relying on `Flow.timeout()` for the 5-second "no signal" detection:** still `@FlowPreview` in kotlinx-coroutines 1.10.2 (weak compatibility guarantees per its own KDoc). Prefer a manual ticker/timestamp comparison (see Code Examples) — it also reuses the "once per second" cadence already required by GPS-01.
- **Computing speed by differentiating consecutive `Location` lat/lng fixes:** `Location.getSpeed()` (when `hasSpeed()` is true) is computed by the GNSS chip directly (often via Doppler shift), which is materially more accurate and lower-latency than finite-differencing two position fixes — do not hand-roll this.
- **Checking permission only for `ACCESS_FINE_LOCATION` inside the GPS engine class:** the check already lives in `MainActivity` (Phase 1); duplicating it in `GpsSpeedProvider` invites the two to drift. Suppress the lint warning at the call site instead (see Common Pitfalls).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|--------------|-----|
| Instantaneous speed calculation | Manual haversine distance / time-delta speed calculator across consecutive fixes | `Location.getSpeed()` (guarded by `hasSpeed()`) | GNSS-chip-computed speed (via Doppler) is more accurate and has less noise/lag than differentiating position fixes at 1 Hz; also what D-04 already assumes |
| Location "freshness"/staleness detection | Custom `Handler`/`postDelayed` timers | A single 1-second ticker `Flow` (`flow { while (true) { emit(Unit); delay(1000) } }`) merged with the location Flow, or `kotlinx.coroutines.flow.combine` | Simpler mental model, no `Handler` boilerplate, consistent with structured concurrency and automatically cancelled by `repeatOnLifecycle` |
| Play Services connection/availability handling | Manual `GoogleApiClient` connection callbacks (legacy pre-2018 pattern) | `LocationServices.getFusedLocationProviderClient(context)` (modern client, connection-less) | The legacy `GoogleApiClient` API this project might be tempted to reference from old tutorials was deprecated years ago; the modern `FusedLocationProviderClient` has no explicit connect/disconnect step |

**Key insight:** Nearly every "pitfall" in this domain (stale docs referencing `LocationRequest.create()` or `GoogleApiClient`, hand-rolled speed math, hand-rolled timeout logic) stems from copying pre-2018 tutorials. The current API surface (`LocationRequest.Builder`, `Priority`, connection-less `FusedLocationProviderClient`) removes the need for almost all custom plumbing except the Flow bridge itself.

## Common Pitfalls

### Pitfall 1: Compile-time lint failure on `requestLocationUpdates()` — "Missing permission"
**What goes wrong:** Android Lint statically enforces the `@RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])` annotation on `requestLocationUpdates()`. If the permission check isn't visible to Lint at the exact call site, the build fails with a "Missing permission" lint error (or a red squiggle blocking `assembleDebug` if lint errors are treated as fatal).
**Why it happens:** Lint's permission-check detector traces a limited set of patterns (an `if (checkSelfPermission(...) == GRANTED)` guard, or a try/catch around a `SecurityException`) within the same method or a small set of recognized call chains. Since the permission check for this app already happened earlier in `MainActivity` (Phase 1) — separated by class/method boundaries from where `requestLocationUpdates()` will actually be called inside `GpsSpeedProvider` — Lint cannot see that guarantee.
**How to avoid:** Add `@Suppress("MissingPermission")` on the function/class that calls `requestLocationUpdates()`, with a comment explaining the permission is guaranteed by the caller (`MainActivity` only starts collecting the Flow after `ACCESS_FINE_LOCATION` is confirmed granted). This is the standard, widely-used pattern for this exact situation — do not duplicate the permission check inside the GPS engine class just to satisfy Lint.
**Warning signs:** `./gradlew.bat assembleDebug` or `lintDebug` fails citing `MissingPermission` even though the app logically can't reach that code path without the permission.

### Pitfall 2: LocationCallback leak across configuration changes / Activity recreation
**What goes wrong:** If `removeLocationUpdates()` is never called (or called on the wrong callback instance after an Activity recreation), Play Services keeps delivering updates to a `LocationCallback` referencing a destroyed Activity, and/or GPS keeps running (battery drain) after the app is no longer visible.
**Why it happens:** Manual lifecycle wiring (calling `requestLocationUpdates`/`removeLocationUpdates` from ad-hoc places) is easy to get wrong across `onCreate`/`onStart`/`onStop`/`onDestroy` and configuration-change recreation.
**How to avoid:** Let the `callbackFlow`'s `awaitClose {}` be the single place `removeLocationUpdates()` is called, and let `repeatOnLifecycle(STARTED)` be the single mechanism that starts/stops collection. This guarantees "started exactly once per visible period, stopped exactly once per invisible period" without manual bookkeeping.
**Warning signs:** GPS icon stays active in the status bar after backgrounding the app; battery usage attributed to the app while it's not visible.

### Pitfall 3: Testing speed on the emulator without route playback shows constant 0
**What goes wrong:** A developer verifies this phase by opening the emulator with a single static point set in Extended Controls → Location, and speed always reads 0 (correctly, per D-03/D-04, but this doesn't verify the actual speed-reading path at all).
**Why it happens:** A single static point never produces a non-zero `Location.getSpeed()` — there's no movement for the GNSS simulation to derive a speed from.
**How to avoid:** Use Extended Controls → Location → **Routes** tab, either drawing a route on the map or importing a GPX/KML file, then "Play Route" — the emulator will simulate movement along the route at realistic speeds and feed genuinely varying speed values to `LocationCallback`. This is exactly D-10's required verification method.
**Warning signs:** Human checkpoint testing only ever shows "0 km/h" or "Ricerca segnale GPS..." even though the code appears correct.

### Pitfall 4: `Priority` is a separate top-level class, not a `LocationRequest` nested enum
**What goes wrong:** Code written from memory or an old tutorial references `LocationRequest.PRIORITY_HIGH_ACCURACY` (an `int` constant that existed on the class itself in the deprecated `LocationRequest.create()`-based API) instead of `com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY`.
**Why it happens:** The API was reshaped when `LocationRequest.Builder` replaced the deprecated `LocationRequest.create()` factory method; priority constants moved to their own `Priority` class.
**How to avoid:** Import `com.google.android.gms.location.Priority` explicitly; use `LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)`.
**Warning signs:** Compile error "unresolved reference: PRIORITY_HIGH_ACCURACY" on `LocationRequest`.

## Code Examples

### Full GpsSpeedProvider sketch (filters + no-signal detection combined)
```kotlin
// Illustrative pattern combining verified API calls (Android Developers "Request location
// updates" guide + kotlinx-coroutines-core callbackFlow docs) with the locked decisions
// D-01 through D-05. Not a literal drop-in file — the planner should size this into tasks.

package com.sed.tachimetro.gps

sealed class SpeedState {
    data object Searching : SpeedState()          // D-01: shown until first valid fix
    data class Reading(val kmh: Int) : SpeedState() // D-09: whole km/h, no decimals
    data object NoSignal : SpeedState()             // D-02: no update for >5s
}

class GpsSpeedProvider(context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    private val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
        .setMinUpdateIntervalMillis(1000L)
        .build()

    private val accuracyThresholdMeters = 50f  // D-05: tune within the ~30-50m locked range
    private val noiseFloorKmh = 2.0             // D-03: locked example value

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
            val kmh = if (loc.hasSpeed()) loc.speed * 3.6 else 0.0 // D-04, m/s -> km/h
            if (kmh < noiseFloorKmh) 0 else kmh.roundToInt()        // D-03
        }

    private val ticker: Flow<Long> = flow {
        while (true) { emit(System.currentTimeMillis()); delay(1000) }
    }

    val state: StateFlow<SpeedState> = combine(
        filteredKmh.map { it as Int? }.onStart { emit(null) }, // null until first fix (D-01)
        ticker
    ) { lastKmh, _ -> lastKmh }
        .runningFold(Pair<Int?, Long>(null, 0L)) { acc, kmh ->
            if (kmh != acc.first) kmh to System.currentTimeMillis() else acc.first to acc.second
        }
        .map { (kmh, lastUpdate) ->
            when {
                kmh == null -> SpeedState.Searching
                System.currentTimeMillis() - lastUpdate > 5000 -> SpeedState.NoSignal // D-02
                else -> SpeedState.Reading(kmh)
            }
        }
        .stateIn(/* scope, SharingStarted.WhileSubscribed(), SpeedState.Searching */)
}
```
*(This sketch prioritizes showing every locked decision mapped to code over being a final polished implementation — the planner should decide exact operator composition/testability boundaries. In particular, the `runningFold`-based "last change timestamp" tracking above is one valid approach; a simpler alternative is a plain `var lastUpdateAtMs: Long` mutated inside `onLocationResult` before filtering, read by the ticker — either is acceptable and should be a planning-time choice, not a research-time one.)*

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|-------------------|---------------|--------|
| `LocationRequest.create()` + `setPriority()`/`setInterval()` setters (deprecated factory) | `LocationRequest.Builder(priority, intervalMillis)` | Deprecated for several years; `Builder` has been the sole recommended path in current official docs and codelabs | Old tutorials/StackOverflow answers using `.create()` will not compile against current `play-services-location` |
| `GoogleApiClient` connect/disconnect lifecycle | Connection-less `LocationServices.getFusedLocationProviderClient(context)` | Deprecated ~2018 | Old tutorials referencing `GoogleApiClient.Builder()...connect()` are obsolete; no connection step needed today |
| Separate `org.jetbrains.kotlin.android` Gradle plugin | AGP 9.1.1+ built-in Kotlin support (`android.builtInKotlin`) | AGP 9.0 (per `01-01-SUMMARY.md`, already adopted in this project) | Already handled in Phase 1 — do not reintroduce the classic plugin |

**Deprecated/outdated:**
- `LocationRequest.create()`: replaced by `LocationRequest.Builder`.
- `GoogleApiClient`: replaced by direct `LocationServices.getFusedLocationProviderClient()`.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|-----------------|
| A1 | Accuracy filter threshold recommended at 50 meters (upper bound of the locked ~30-50m range) | Code Examples, Standard Stack | Low — value is within the user-locked range either way; if 50m proves too lenient in testing (spurious speed spikes still visible), tightening to 30m is a one-line change, no architecture impact |
| A2 | `kotlinx-coroutines-core`/`-play-services` 1.10.2 (built against a pre-2.2 Kotlin) is fully binary/source compatible with AGP 9.1.1's bundled Kotlin 2.2.10 compiler | Standard Stack (Version verification) | Low-medium — Kotlin's stated backward-compatibility guarantees make this very likely to work, but it was not build-verified in this research session (no live Gradle sync was run against these exact coordinates); planner should treat first Gradle sync as the verification step, not assume success |
| A3 | `LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L).setMinUpdateIntervalMillis(1000L)` reliably yields ~1 update/sec on real hardware (not throttled slower by the OS/chipset under Doze-adjacent states while foregrounded) | Phase Requirements (GPS-01) | Low — app only runs updates while visible (D-07), which is outside Doze/standby restrictions; foregrounded apps are not throttled this way. Real-device cadence should still be confirmed during human verification (D-10) |
| A4 | AGP 9.1.1's built-in Kotlin bundles exactly Kotlin 2.2.10 | Standard Stack (Version verification) | Low — sourced from WebSearch cross-referencing AGP 9.1 release notes rather than a single primary citation with a fetched page; does not change any code recommendation in this document, only the compatibility risk note in A2 |

## Open Questions (RESOLVED)

1. **Exact accuracy threshold value (30m vs 50m, or a different point in the locked range)**
   - What we know: D-05 locks a *range* (~30-50m), not an exact number.
   - What's unclear: which end of the range gives the best subjective result (too strict → drops good readings often near obstructions/tunnels-adjacent driving; too lenient → still passes noisy readings).
   - Recommendation: implement with an easily-changeable constant (as in the Code Examples sketch) and let the human verification pass (D-10, route playback) decide if it needs tuning — do not over-engineer this into a user-facing setting (out of scope, `UI-04` forbids extra controls).
   - RESOLVED: plan 02-02 implements this as a tunable constant (50m) in `mapSpeedToKmh`, per the recommendation.

2. **Where exactly does the "no signal" ticker/timestamp-tracking logic live — inside `GpsSpeedProvider` or as a second collaborator class?**
   - What we know: Decisions leave "struttura interna delle classi/file del motore GPS" to Claude's discretion.
   - What's unclear: whether splitting "raw Flow filtering" and "no-signal timeout" into two classes improves testability enough to be worth the extra file, versus one cohesive `GpsSpeedProvider`.
   - Recommendation: planner's call; either is consistent with this research. A single class is likely sufficient for this phase's scope (no ViewModel/DI framework in this project).
   - RESOLVED: plan 02-02 keeps a single `GpsSpeedProvider` class, per the recommendation.

3. **Should the GPS engine explicitly check `GoogleApiAvailability.isGooglePlayServicesAvailable()` before starting?**
   - What we know: `PROJECT.md` already accepts "richiede un device con Google Play Services installato" as a constraint (i.e., devices without GMS are out of scope). If GMS is genuinely absent, `requestLocationUpdates()`'s underlying task simply never completes/fires — which surfaces identically to "no signal" (D-02's 5s timeout) with no crash.
   - What's unclear: whether that graceful-degradation-by-coincidence is good enough for v1, or whether an explicit check-and-message is worth the extra (out-of-scope-leaning) UI surface.
   - Recommendation: skip an explicit check for this phase — the timeout-based "no signal" state already covers this failure mode without additional code, consistent with `UI-04`'s "no unnecessary elements" and the project's minimal-surface philosophy. Flag to user only if it becomes an issue during real-device testing.
   - RESOLVED: no explicit check added, per the recommendation.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|--------------|-----------|---------|-----------|
| Google Play Services (on test device/emulator) | `FusedLocationProviderClient` to function at all | ✓ (confirmed on connected device) | GMS `26.25.31` on connected physical device (model KB2003, Android 14/API 34) | Emulator images with Play Store (the "Play Store" variant, not "Google APIs"-only) also ship current GMS — use a Play Store emulator image for D-10 route-playback testing |
| Android Studio Emulator "Extended Controls → Location → Routes" | D-10 verification method | Not directly probed (no emulator instance running in this session) | Standard Android Studio feature, present in all currently supported Studio versions `[CITED: developer.android.com/studio/run/emulator-extended-controls]` | None needed — this is the primary/required verification method per D-10, not one of several options |
| `adb` | General on-device debugging/log inspection during implementation | ✓ | functional in this environment (`/c/Android/adb`), connected to a physical device over Wi-Fi (`adb-tls-connect`) | — |
| Gradle / AGP 9.1.1 build toolchain | Compiling the new dependency additions | ✓ (already working per Phase 1: `assembleDebug` succeeded) | AGP 9.1.1, Gradle 9.3.1, JDK 21 toolchain | — |

**Missing dependencies with no fallback:** none identified.
**Missing dependencies with fallback:** none blocking — emulator route-playback capability itself was not directly exercised in this research session (no emulator was launched), but it is a standard, long-standing Android Studio feature and D-10 already specifies it as the required test method.

## Security Domain

> `security_enforcement` is not set in `.planning/config.json` — treated as enabled per default rule.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|-----------------|---------|---------------------|
| V2 Authentication | No | No authentication surface in this app (single-user, local-only, no accounts) |
| V3 Session Management | No | No sessions/network backend exists |
| V4 Access Control | No | No multi-user/access-control surface |
| V5 Input Validation | Marginal | GPS-derived `Location` data is validated for *quality* (accuracy/hasSpeed, per D-03/D-04/D-05) but this is a data-quality concern, not a security input-validation concern — there is no untrusted external input crossing a trust boundary in this phase (Play Services is a trusted OS-level API, not user/network input) |
| V6 Cryptography | No | No data persisted or transmitted in this phase (persistence arrives in Phase 4 for max speed; this phase is purely in-memory) |
| V8 Data Protection (sensitive data) | Yes, lightly | Location data is inherently sensitive (real-time GPS coordinates). This phase only *reads instantaneous speed* and never persists, logs, or transmits raw lat/lng — do not add `Log.d()` calls that print raw `Location` objects (which include lat/lng) in production code; if debug logging is added during implementation, log only the derived `kmh` value, not the underlying coordinates |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|-----------------------|
| Accidental logging/leakage of precise device coordinates (privacy, not just "security") | Information Disclosure | Never log full `Location` objects; this app's data model should carry only derived speed (`Int` km/h) past the GPS engine boundary — `SpeedState.Reading(kmh: Int)` already enforces this by construction (lat/lng never leaves `GpsSpeedProvider`) |
| Requesting more permission scope than needed (`ACCESS_COARSE_LOCATION` in addition to `ACCESS_FINE_LOCATION`, or background location) | Elevation of Privilege (excess permission) | Manifest already declares only `ACCESS_FINE_LOCATION` (Phase 1) — this phase must not add `ACCESS_COARSE_LOCATION` or `ACCESS_BACKGROUND_LOCATION`; `PRIORITY_HIGH_ACCURACY` works with fine-only permission |

## Sources

### Primary (HIGH confidence)
- `https://dl.google.com/android/maven2/com/google/android/gms/group-index.xml` — confirmed `play-services-location` 21.4.0 is the latest published version (fetched 2026-07-07)
- `https://search.maven.org/solrsearch/select?q=g:org.jetbrains.kotlinx+AND+a:kotlinx-coroutines-core&rows=1&wt=json` and same query for `kotlinx-coroutines-play-services` — confirmed `1.10.2` as `latestVersion` for both, via Maven Central's official search API (fetched 2026-07-07)
- `https://dl.google.com/android/maven2/androidx/lifecycle/group-index.xml` — confirmed `lifecycle-runtime-ktx` 2.11.0 as latest non-prerelease version (fetched 2026-07-07)
- `https://dl.google.com/android/maven2/androidx/activity/activity-ktx/1.9.3/activity-ktx-1.9.3.pom` — confirmed `activity-ktx:1.9.3` (already a project dependency) transitively pulls `androidx.lifecycle:lifecycle-runtime-ktx:2.6.1`, confirming `lifecycleScope`/`repeatOnLifecycle` are already reachable without new dependencies
- `https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-play-services/` — confirmed the module provides only `Task.await()`/`Task.asDeferred()`/`Deferred.asTask()`, no Flow adapter (the correction to D-06's premise)
- `https://developer.android.com/build/migrate-to-built-in-kotlin` — confirmed AGP built-in Kotlin support automatically adds the Kotlin stdlib dependency (no explicit `kotlin-stdlib` declaration needed) — consistent with Phase 1's working setup
- Local repo: `01-01-SUMMARY.md` — confirms AGP 9.1.1 built-in Kotlin support is already active and the classic Kotlin plugin must not be reintroduced; `MainActivity.kt`, `activity_main.xml`, `strings.xml`, `AndroidManifest.xml` — confirmed exact reusable assets for the placeholder update

### Secondary (MEDIUM confidence)
- `https://developer.android.com/develop/sensors-and-location/location/request-updates` — via WebFetch summary, confirmed current `LocationRequest.Builder`/`Priority`/`LocationCallback`/`requestLocationUpdates(request, callback, Looper)` shape and `hasSpeed()`/`hasAccuracy()` usage
- `https://developer.android.com/studio/run/emulator-extended-controls` — via WebSearch summary, confirmed the Routes tab / GPX-KML import / Play Route feature referenced by D-10 still exists in current Android Studio
- WebSearch cross-reference (multiple sources) confirming AGP 9.0/9.1 bundles Kotlin 2.2.10 as the built-in compiler version

### Tertiary (LOW confidence)
- WebSearch-derived `callbackFlow` example pattern for wrapping `LocationCallback` (Medium/dev-blog sources, e.g. Sean Barbeau's "Kotlin callbackFlow" article, Conor Smith's post) — pattern is consistent across multiple independent sources and matches the official `kotlinx.coroutines` `callbackFlow` API docs' own described usage (register → `trySend` → `awaitClose { unregister }`), so treated as reliable despite non-official origin
- `Flow.timeout()` `@FlowPreview` status — confirmed via WebSearch summary of the operator's own KDoc language ("weak guarantees"); not independently re-fetched from the primary KDoc page in this session

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — versions verified directly against Google's and Maven Central's own authoritative index/search endpoints, not secondary aggregators
- Architecture: HIGH — `callbackFlow` + `repeatOnLifecycle` are both well-established, officially-documented patterns for exactly this problem shape; the D-06 correction is backed by directly reading the `kotlinx-coroutines-play-services` API surface
- Pitfalls: MEDIUM-HIGH — permission-lint and callback-leak pitfalls are well-known, broadly documented issues; the exact Kotlin-2.2.10-vs-coroutines-1.10.2 compatibility (A2) is the one item not build-verified in this session

**Research date:** 2026-07-07
**Valid until:** 2026-08-06 (30 days — stable, mature API surface, but dependency versions should be re-checked if planning is delayed significantly, since `play-services-location` and `kotlinx-coroutines` both release updates every few months)

---
*Phase: 2-Motore GPS*
*Research completed: 2026-07-07*
