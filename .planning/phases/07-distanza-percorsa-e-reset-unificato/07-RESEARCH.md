# Phase 7: Distanza Percorsa e Reset Unificato - Research

**Researched:** 2026-08-29
**Domain:** Android GPS distance accumulation (Kotlin, FusedLocationProviderClient, Flow/StateFlow), SharedPreferences persistence, ConstraintLayout window insets
**Confidence:** HIGH

## Summary

This phase adds a foreground-only, disk-persistent "distance since last reset" counter and folds it into the
existing max-speed reset button. The codebase already has every pattern needed — `MaxSpeedStore`/`MaxSpeedReducer`
for persistence, `GpsSpeedProvider`/`SpeedMapping.kt` for the GPS-filtering pipeline, and four precedent
`applyXWindowInsets()` functions in `MainActivity.kt` for corner-pinned views — so this is an extension exercise,
not new-pattern invention.

The one real architecture decision (D-07 in CONTEXT.md) is **how to get filtered position deltas out of
`GpsSpeedProvider` without duplicating `mapSpeedToKmh()`'s accuracy/noise-floor logic**. Having read the actual
`GpsSpeedProvider.kt`/`SpeedMapping.kt` source, the recommended approach is: compute `Location.distanceTo()`
between consecutive **accuracy-accepted** fixes *inside* `GpsSpeedProvider` (the only class that ever touches
`android.location.Location`), thread the resulting `deltaMeters: Float` through the *existing* `acceptedKmh`
pipeline as a second field, and add it to `SpeedState.Reading` as `Reading(val kmh: Int, val deltaMeters: Float)`.
No second flow, no `shareIn`, no duplicated filter logic — `mapSpeedToKmh()` stays the single source of truth for
D-04/D-05 (noise floor / accuracy), and the noise-floor **accumulation** gate (D-04's "don't add distance below
2 km/h") is applied downstream by a new pure `reduceDistance()` function, exactly mirroring how `reduceMax()`
consumes `state.kmh` today.

A critical, non-obvious correctness property that this design depends on and that the plan must preserve:
`SpeedState.Reading` must remain a Kotlin `data class` (structural equality), because `GpsSpeedProvider.state` is
a `StateFlow`, and `StateFlow` **conflates consecutive equal values** — this is what prevents the once-per-second
`ticker` (which co-drives `combine()` alongside the GPS flow) from re-triggering a duplicate distance addition for
a fix that hasn't actually changed. Adding any field to `Reading` that changes every tick regardless of a new fix
(e.g., a raw timestamp) would break this dedup and cause runaway distance overcounting. This is flagged as the
top pitfall below.

**Primary recommendation:** Extend `GpsSpeedProvider`'s existing single `acceptedKmh`/`state` pipeline with a
`deltaMeters: Float` computed via `Location.distanceTo()` against an internally-tracked `lastAcceptedLocation`;
carry it on `SpeedState.Reading`; consume it in `MainActivity.updatePlaceholder()` via a new pure `reduceDistance()`
function and a new `DistanceStore`/persist-every-update pattern that mirrors `MaxSpeedStore` exactly (same prefs
file, own key, `Float` not `Int`).

## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** Formato adattivo — metri interi sotto 1 km (es. "850 m"), poi km con una decimale sopra 1 km (es. "1,2 km"). Il formato cambia a runtime superata la soglia di 1 km.
- **D-02:** L'unità di misura va in una view separata, piccola, accanto al numero — stesso pattern già usato per `unitText` (non una stringa combinata come `maxSpeedText`/"MAX %d").
- **D-03:** Testo della distanza a **32sp** (vs 22sp dell'area MAX — deve essere visibilmente più grande, SC1).
- **D-04:** Nessun accumulo di distanza sotto la soglia di rumore già usata per la velocità (`noiseFloorKmh = 2.0` in `GpsSpeedProvider.kt`) — evita deriva quando il veicolo è fermo (es. semaforo).
- **D-05:** Stesso filtro di accuratezza della velocità (`accuracyThresholdMeters = 50f`) applicato anche alle letture usate per il calcolo della distanza — nessuna soglia separata.
- **D-06:** La distanza è la somma di `Location.distanceTo()` tra fix GPS consecutivi accettati — **non** un'integrazione della velocità istantanea (kmh × tempo). Scelta esplicita dell'utente per maggiore fedeltà al percorso reale (curve, traiettoria), a fronte di più complessità implementativa.
- **D-07 (nota architetturale per il planner):** D-06 combinato con D-04/D-05 significa che i filtri di rumore/accuratezza — oggi dentro `mapSpeedToKmh()` (`SpeedMapping.kt`) e applicati solo al flusso kmh di `GpsSpeedProvider` — devono essere applicati **anche** al flusso di posizioni usato per la distanza, senza duplicare la logica di filtro. `GpsSpeedProvider` oggi espone solo `StateFlow<SpeedState>` (kmh), non le `Location` grezze accettate — serve decidere come esporre/derivare i delta di posizione filtrati. Questa è una decisione di architettura per il ricercatore/planner, non ridiscussa con l'utente. **→ Risolto sotto in "Architecture Patterns".**
- **D-08:** Il pulsante esistente (`resetMaxButton`, string `reset_max_button`) cambia testo da "Azzera massimo" a **"Azzera"** (generico) — azzera sia il massimo sia la distanza nella stessa azione (MAX-04, requisito già bloccato).

### Claude's Discretion

- Tipo/precisione dei dati persistiti per la distanza (es. metri come Int vs altro) — dettaglio tecnico, nessuna preferenza espressa dall'utente. **→ Recommendation: `Float` meters, see below.**
- Come `GpsSpeedProvider` espone i delta di posizione filtrati (nuovo flow, campo aggiuntivo su `SpeedState`, provider separato) — vedi D-07, lasciato al planner/ricercatore. **→ Recommendation: field on `SpeedState.Reading`, see below.**
- Gestione dei window insets per il nuovo angolo bottom-right (nessun listener esistente per quella posizione). **→ Recommendation: new `applyDistanceAreaWindowInsets()`, mirrors `applyMaxAreaWindowInsets()`, see below.**
- Se l'area distanza resta nascosta a "0" come l'area MAX, oppure resta sempre visibile. **→ Recommendation: always visible, see Pitfalls/Open Questions.**
- Nomi delle string resources per i nuovi formati (es. chiave per "%1$d m" / "%1$.1f km"). **→ Suggested names below.**

### Deferred Ideas (OUT OF SCOPE)

None — discussion stayed within phase scope.

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| DIST-01 | L'utente vede la distanza percorsa dall'ultimo azzeramento in un'area in basso a destra, con font più grande dell'area velocità massima | Layout/insets pattern (`applyDistanceAreaWindowInsets`), 32sp text spec, `distanceText`/`distanceUnitText` view pair mirroring `maxSpeedText`/`unitText` |
| DIST-02 | La distanza si accumula solo mentre l'app è attiva e riceve aggiornamenti GPS, senza tracking in background | Confirmed via `repeatOnLifecycle(STARTED)` + `WhileSubscribed()` analysis — no new logic needed, same mechanism that already gates `state` collection |
| DIST-03 | La distanza persiste su disco e sopravvive a chiusura app e riavvio del telefono | `DistanceStore` mirrors `MaxSpeedStore` (SharedPreferences, `apply()`, sanitize-on-read), write-on-every-accepted-fix pattern |
| MAX-04 | Il pulsante "Azzera massimo" azzera sia la velocità massima sia la distanza percorsa in un'unica azione | Extend `onResetMaxClicked()` to also zero `DistanceStore`; button text change (D-08) |
</phase_requirements>

## Project Constraints (from CLAUDE.md)

- **Tech stack:** Kotlin, traditional XML layouts (no Compose) — new views go in `activity_main.xml`, not a new Compose surface.
- **GPS:** FusedLocationProviderClient only — no new location API/library.
- **Compatibility:** minSdk 30, targetSdk 36 — `Location.distanceTo()` has been available since API 1, no version gating needed.
- **Performance:** 1 GPS update/sec — distance accumulation piggybacks on the same cadence, no new polling.
- **UX:** No new animations, no new colors beyond the already-approved charging-icon lime exception (PROJECT.md explicit constraint: "la distanza NON deve introdurre nuove animazioni o colori"). Visibility toggles (if used) must be instant `GONE`↔`VISIBLE`, no fade.
- **No Room/DataStore:** a single Float doesn't justify a new persistence dependency — SharedPreferences only, matching `MaxSpeedStore`.
- **No third-party logging.**
- **KDoc for public classes/functions; inline comments reference decision tags** (`D-XX`, `DIST-XX`, `MAX-04`, `WR-XX`) — new code must follow this citation convention.
- **Pure functions for testable logic** — any new decision logic (`reduceDistance`, `sanitizePersistedDistance`, distance formatting) must be framework-free top-level functions, unit-tested on plain JVM (no Robolectric), matching `reduceMax`/`mapSpeedToKmh`/`deriveSpeedState`.
- **`applicationContext` only for long-lived components (WR-04)** — `DistanceStore(context: Context)` must follow the exact `MaxSpeedStore` constructor pattern.
- **Naming conventions:** PascalCase classes (`DistanceStore`, `DistanceReducer` file), camelCase functions/properties, SCREAMING_SNAKE_CASE constants, `[condition]_returns[Outcome]` test method names.
- **GSD workflow enforcement:** implementation must go through `/gsd:execute-phase`, not ad-hoc edits (process note for the planner, not a code constraint).

## Architectural Responsibility Map

Single-module native Android app (no client/server split) — "tiers" here are internal architectural layers, not network tiers.

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| GPS fix acquisition + accuracy/noise filtering | GPS engine (`GpsSpeedProvider`) | — | Sole owner of `android.location.Location`; already filters for kmh, extending it to also compute `distanceTo()` keeps the Location API boundary in one place |
| Distance delta computation (`Location.distanceTo()`) | GPS engine (`GpsSpeedProvider`) | — | Requires two `Location` objects; must live where `Location` objects exist (never expose raw `Location` further out per existing architecture, which only exposes derived primitives via `SpeedState`) |
| Noise-floor accumulation gate + running total (`reduceDistance`) | Domain/reducer layer (new `distance` package, pure function) | — | Pure, testable, framework-free — mirrors `reduceMax`/`MaxSpeedReducer.kt` exactly |
| Distance persistence | Persistence layer (new `DistanceStore`) | — | Mirrors `MaxSpeedStore` exactly: SharedPreferences, sanitize-on-read, `apply()` |
| Distance display + formatting + reset wiring | UI/Activity (`MainActivity`) | — | Same activity that already owns `currentMax`/`updateMaxArea()`/`onResetMaxClicked()` |
| Adaptive unit formatting (m vs km) | Domain/pure function (new, colocated near distance reducer or in `MainActivity` companion) | UI (`MainActivity` calls `getString()` with locale-aware format) | Threshold decision is pure/testable; final localized string rendering stays in Activity (matches `max_speed_format` pattern) |
| Window insets for new bottom-right corner | UI (`MainActivity`) | — | New `applyDistanceAreaWindowInsets()`, same shape as the other three `applyXWindowInsets()` functions |

## Standard Stack

No new external dependencies are required for this phase. `Location.distanceTo()` is part of `android.location.Location`,
already available via the existing `com.google.android.gms:play-services-location` dependency (fixes returned by
`FusedLocationProviderClient` are plain `android.location.Location` objects). SharedPreferences (`Context.getSharedPreferences`)
is core Android, already used by `MaxSpeedStore`/`ScreenOnPreferenceStore`.

### Core (already present, verified in `gradle/libs.versions.toml`)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `com.google.android.gms:play-services-location` | 21.4.0 `[VERIFIED: gradle/libs.versions.toml]` | Supplies the `Location` objects this phase's distance math consumes | Already the project's sole GPS source (Phase 2); no alternative under consideration |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` | 1.10.2 `[VERIFIED: gradle/libs.versions.toml]` | `Flow`/`StateFlow`/`combine` — the pipeline the new `deltaMeters` field flows through | Already the project's reactive-state mechanism (Phase 2); StateFlow conflation behavior (see Pitfalls) is load-bearing for this phase's correctness |
| Android SDK `android.location.Location` | API 1+ (minSdk 30 easily covers it) `[CITED: Android platform, stable since API 1]` | `distanceTo(Location)` — geodesic distance in meters | Built-in, no library needed; avoids hand-rolling Haversine/Vincenty math (see "Don't Hand-Roll") |
| Android SDK `SharedPreferences` | API 1+ | Distance persistence | Matches `MaxSpeedStore`/`ScreenOnPreferenceStore`; project convention explicitly rejects Room/DataStore for single scalar values |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `Location.distanceTo()` (Vincenty-based geodesic distance) | Manual Haversine formula on lat/lng primitives | Rejected — D-06 already locks `Location.distanceTo()` explicitly; hand-rolling would also violate "Don't Hand-Roll" (Android's implementation is a well-tested geodetic formula on the WGS84 ellipsoid, a manual Haversine implementation is a simpler, less accurate great-circle approximation) |
| Integrating kmh × elapsed-time for distance | `Location.distanceTo()` between consecutive fixes | Rejected by user (D-06) explicitly, despite being simpler to implement — user wants path fidelity (turns/trajectory), not straight-line speed integration |
| Storing distance as `Int` meters | `Float` meters (raw, unrounded) | `Int` would lose sub-meter precision on every fix; over a long drive, repeated truncation could visibly under-count. `Float` matches `distanceTo()`'s own return type exactly — no precision loss, no rounding needed until display time |
| A second `shareIn()`-based `Flow<Float>` for deltas, collected independently in `MainActivity` | Adding `deltaMeters` as a field on the existing `SpeedState.Reading` | Two independently-collected shared flows from one cold upstream adds `shareIn()` coordination complexity (replay/buffering, subscription-order concerns) for zero benefit in a single-collector app with no ViewModel/DI layer. Single-flow approach is simpler and lower-risk (see Architecture Patterns) |

**Installation:** none — no new Gradle dependencies for this phase.

**Version verification:** Versions above read directly from `gradle/libs.versions.toml` (2026-08-29) — `agp = "9.3.2"` (newer than the `9.1.1` referenced in CLAUDE.md; CLAUDE.md documentation appears to trail an uncommitted AGP bump visible in `git status`, does not affect this phase). `playServicesLocation = "21.4.0"` and `kotlinxCoroutines = "1.10.2"` are unchanged from CLAUDE.md.

## Package Legitimacy Audit

Not applicable — this phase installs no external packages. All work uses APIs already present in the project
(`android.location.Location`, `android.content.SharedPreferences`, existing `kotlinx.coroutines.flow.*`).

## Architecture Patterns

### Current pipeline (read from actual source, `GpsSpeedProvider.kt`)

```
FusedLocationProviderClient
        │  (callbackFlow, requestLocationUpdates)
        ▼
  rawLocations: Flow<Location>
        │  .map { loc -> mapSpeedToKmh(loc.hasAccuracy(), loc.accuracy, loc.hasSpeed(), loc.speed, ...) }
        │      (mapSpeedToKmh is PURE: primitives in, Int? out — null = accuracy-dropped, D-05)
        ▼
  Flow<Int?>
        │  .filterNotNull()   ← drops accuracy-failed fixes entirely
        │  .map { kmh -> lastAcceptedUpdateAtMs = now(); kmh }
        ▼
  acceptedKmh: Flow<Int>
        │
        ▼
  combine(acceptedKmh.onStart{null}, ticker(1s)) { lastKmh, now -> deriveSpeedState(...) }
        │  (deriveSpeedState is PURE: Searching / Reading(kmh) / NoSignal)
        ▼
  state: StateFlow<SpeedState>  ← the ONLY thing exposed publicly today
        │
        ▼
  MainActivity.updatePlaceholder(state)  →  messageText, reduceMax(currentMax, state.kmh), maxSpeedStore.write()
```

### Recommended pipeline (this phase — resolves D-07)

The change is additive to the SAME pipeline, not a parallel one. `mapSpeedToKmh()` is untouched (still the single
source of truth for D-04/D-05 filtering); `GpsSpeedProvider` additionally tracks the last accepted `Location` and
computes a delta whenever a new fix passes the SAME accuracy filter `mapSpeedToKmh()` already applies:

```
FusedLocationProviderClient
        │
        ▼
  rawLocations: Flow<Location>
        │  .map { loc ->
        │      val kmh = mapSpeedToKmh(...)               // UNCHANGED — D-04/D-05 logic, no duplication
        │      if (kmh == null) null else {
        │          val delta = lastAcceptedLocation?.distanceTo(loc) ?: 0f   // D-06: distanceTo() between
        │                                                                     // consecutive ACCEPTED fixes
        │          lastAcceptedLocation = loc            // update reference point on EVERY accepted fix,
        │                                                  // even ones below the noise floor (see Pitfall 1)
        │          AcceptedReading(kmh, delta)
        │      }
        │    }
        ▼
  Flow<AcceptedReading?>       (AcceptedReading = private data class(kmh: Int, deltaMeters: Float))
        │  .filterNotNull()    ← same accuracy gate as today, just carries deltaMeters alongside kmh
        │  .onEach { lastAcceptedUpdateAtMs = now() }
        ▼
  acceptedReadings: Flow<AcceptedReading>
        │
        ▼
  combine(acceptedReadings.onStart{null}, ticker(1s)) { last, now ->
      deriveSpeedState(last?.kmh, last?.deltaMeters ?: 0f, now, lastAcceptedUpdateAtMs)
  }
        ▼
  state: StateFlow<SpeedState>   ← SpeedState.Reading now carries (kmh: Int, deltaMeters: Float)
        │
        ▼
  MainActivity.updatePlaceholder(state)
        │  is Reading -> reduceMax(currentMax, state.kmh)               // UNCHANGED
        │             -> reduceDistance(currentDistance, state.deltaMeters, state.kmh)   // NEW, mirrors reduceMax
        ▼
  maxSpeedStore.write() / distanceStore.write()   ← both immediate, every accepted fix (D-07 pattern: no batching)
```

### Recommended Project Structure

```
app/src/main/java/com/sed/tachimetro/
├── gps/
│   ├── GpsSpeedProvider.kt      # MODIFY: add lastAcceptedLocation, AcceptedReading, deltaMeters plumbing
│   ├── SpeedMapping.kt          # UNCHANGED — mapSpeedToKmh() stays the single filter source of truth
│   └── SpeedState.kt            # MODIFY: Reading(val kmh: Int, val deltaMeters: Float) — see Pitfall 1
├── maxspeed/
│   ├── MaxSpeedStore.kt         # UNCHANGED
│   └── MaxSpeedReducer.kt       # UNCHANGED
├── distance/                     # NEW package — mirrors maxspeed/ structure exactly
│   ├── DistanceStore.kt         # NEW — mirrors MaxSpeedStore.kt (Float instead of Int)
│   └── DistanceReducer.kt       # NEW — mirrors MaxSpeedReducer.kt (reduceDistance, sanitizePersistedDistance,
│                                  #        + adaptive display formatting function, see Code Examples)
├── screen/
│   └── ScreenOnPreferenceStore.kt  # UNCHANGED
└── MainActivity.kt               # MODIFY: distanceText/distanceUnitText views, distanceStore, currentDistance,
                                    #         applyDistanceAreaWindowInsets(), extend onResetMaxClicked()
```

### Pattern 1: Pure Location-delta reducer (mirrors `reduceMax`/`MaxSpeedReducer.kt`)

**What:** A framework-free function that decides how much of a computed `deltaMeters` to add to the running total,
applying D-04's noise-floor gate.
**When to use:** Called once per `SpeedState.Reading` emission, exactly where `reduceMax()` is called today.
**Example:**
```kotlin
// Source: mirrors app/src/main/java/com/sed/tachimetro/maxspeed/MaxSpeedReducer.kt (read directly, 2026-08-29)
package com.sed.tachimetro.distance

/**
 * D-04/D-06: adds [deltaMeters] to [currentTotalMeters] only when [kmh] is at or above the
 * shared noise floor (mirrors GpsSpeedProvider.noiseFloorKmh) — otherwise the vehicle is
 * considered stationary and the (likely GPS-jitter) delta is discarded, not accumulated.
 * Pure, unit-testable: no Location/Android dependency — the caller (GpsSpeedProvider) already
 * computed deltaMeters via Location.distanceTo() before this function ever sees it.
 */
fun reduceDistance(
    currentTotalMeters: Float,
    deltaMeters: Float,
    kmh: Int,
    noiseFloorKmh: Double = 2.0,
): Float {
    val safeCurrent = if (currentTotalMeters < 0f) 0f else currentTotalMeters
    if (kmh < noiseFloorKmh) return safeCurrent // D-04: no accumulation below noise floor
    val safeDelta = if (deltaMeters < 0f) 0f else deltaMeters // distanceTo() is always >= 0; defensive only
    return safeCurrent + safeDelta
}

/** Mirrors sanitizePersistedMax — a tampered/negative persisted value resets to 0. */
fun sanitizePersistedDistance(raw: Float): Float = if (raw < 0f) 0f else raw
```

### Pattern 2: Location-touching logic stays inside `GpsSpeedProvider`

**What:** `distanceTo()` is called only where `Location` objects already exist — never pass a raw `Location` out
to `MainActivity` or a pure function.
**When to use:** Any time new GPS-derived data needs computing.
**Example:**
```kotlin
// Source: extends app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt (read directly, 2026-08-29)
@Volatile
private var lastAcceptedLocation: Location? = null

private data class AcceptedReading(val kmh: Int, val deltaMeters: Float)

@Suppress("MissingPermission")
private val acceptedReadings: Flow<AcceptedReading> = rawLocations
    .map { loc ->
        val kmh = mapSpeedToKmh(
            hasAccuracy = loc.hasAccuracy(),
            accuracyMeters = loc.accuracy,
            hasSpeed = loc.hasSpeed(),
            speedMetersPerSecond = loc.speed,
            accuracyThresholdMeters = accuracyThresholdMeters,
            noiseFloorKmh = noiseFloorKmh,
        )
        if (kmh == null) {
            null
        } else {
            // D-06: distanceTo() between this accepted fix and the previous one; null previous
            // (first-ever accepted fix) means delta 0f -- nothing to add yet.
            val delta = lastAcceptedLocation?.distanceTo(loc) ?: 0f
            lastAcceptedLocation = loc // always refresh, even if reduceDistance() below the
                                        // noise floor won't add this delta -- keeps the NEXT
                                        // delta anchored to the freshest accurate position
                                        // instead of drifting from a stale reference point.
            AcceptedReading(kmh, delta)
        }
    }
    .filterNotNull()
    .onEach { lastAcceptedUpdateAtMs = SystemClock.elapsedRealtime() }
```

### Pattern 3: Adaptive distance display formatting (mirrors D-01)

**What:** Pure function deciding meters-vs-km display branch, framework-free (no `Context`/`getString` inside it).
**When to use:** Called from `MainActivity` on every distance update, right before setting `distanceText`/`distanceUnitText`.
**Example:**
```kotlin
// New — colocate in DistanceReducer.kt or a small DistanceFormat.kt, same package
package com.sed.tachimetro.distance

sealed class DistanceDisplay {
    data class Meters(val value: Int) : DistanceDisplay()
    data class Kilometers(val value: Float) : DistanceDisplay()
}

/** D-01: below 1000m shows whole meters; at/above 1000m shows km with one decimal. */
fun formatDistanceDisplay(meters: Float): DistanceDisplay =
    if (meters < 1000f) {
        DistanceDisplay.Meters(kotlin.math.roundToInt(meters))
    } else {
        DistanceDisplay.Kilometers(meters / 1000f)
    }
```
Consumption in `MainActivity` (mirrors `updateMaxArea()`'s `getString(R.string.max_speed_format, currentMax)` call):
```kotlin
when (val display = formatDistanceDisplay(currentDistanceMeters)) {
    is DistanceDisplay.Meters -> {
        distanceText.text = getString(R.string.distance_meters_format, display.value) // "%1$d"
        distanceUnitText.text = getString(R.string.unit_meters) // "m"
    }
    is DistanceDisplay.Kilometers -> {
        // getString()/Resources.getString() formats %f using the DEVICE'S current locale
        // (Resources internally calls String.format(configuration.locale, format, args)) --
        // on an it-IT device this correctly renders "1,2" with a comma, matching D-01's
        // example, with NO manual locale handling required. See Pitfall 3.
        distanceText.text = getString(R.string.distance_km_format, display.value) // "%1$.1f"
        distanceUnitText.text = getString(R.string.unit_km) // "km"
    }
}
```

### Pattern 4: `DistanceStore` (mirrors `MaxSpeedStore.kt` exactly)

```kotlin
// Source: mirrors app/src/main/java/com/sed/tachimetro/maxspeed/MaxSpeedStore.kt (read directly, 2026-08-29)
package com.sed.tachimetro.distance

import android.content.Context

/**
 * DIST-03: persistenza della distanza (un solo Float, metri) via SharedPreferences app-private.
 * Stesso file "tachimetro_prefs" di MaxSpeedStore/ScreenOnPreferenceStore, chiave distinta.
 */
class DistanceStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun read(): Float = sanitizePersistedDistance(prefs.getFloat(KEY_DISTANCE_METERS, 0f))

    fun write(value: Float) {
        prefs.edit().putFloat(KEY_DISTANCE_METERS, value).apply()
    }

    companion object {
        const val PREFS_NAME = "tachimetro_prefs"
        const val KEY_DISTANCE_METERS = "distance_meters"
    }
}
```

### Pattern 5: `applyDistanceAreaWindowInsets()` (new bottom-right corner)

Confirmed by reading `MainActivity.kt` directly: three insets listeners exist today —
`applyUnitTextWindowInsets()` (top+right, lines 495-510), `applyMaxAreaWindowInsets()` (top+left, two views, lines
517-537), `applyBottomLeftWindowInsets()` (bottom+left, two views, lines 571-589). **No bottom+right combination
exists yet** — it is a trivial fourth combination of the same `maxOf(systemBars.X, cutout.X)` pattern:

```kotlin
// New — mirrors applyMaxAreaWindowInsets() shape exactly, bottom+right instead of top+left
private fun applyDistanceAreaWindowInsets() {
    val textParams = distanceText.layoutParams as ConstraintLayout.LayoutParams
    val textBaseBottom = textParams.bottomMargin
    val textBaseEnd = textParams.marginEnd
    ViewCompat.setOnApplyWindowInsetsListener(distanceText) { view, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
        val extraBottom = maxOf(systemBars.bottom, cutout.bottom)
        val extraEnd = maxOf(systemBars.right, cutout.right)
        val lp = view.layoutParams as ConstraintLayout.LayoutParams
        lp.bottomMargin = textBaseBottom + extraBottom
        lp.marginEnd = textBaseEnd + extraEnd
        view.layoutParams = lp
        insets
        // If distanceUnitText is a separate view pinned independently (D-02), give it its own
        // listener OR chain its margin update inside this same listener, exactly as
        // applyMaxAreaWindowInsets() updates resetMaxButton's margin inside maxSpeedText's listener.
    }
}
```

### Anti-Patterns to Avoid

- **Adding a per-tick-changing field to `SpeedState.Reading` (e.g., a raw `SystemClock.elapsedRealtime()`
  timestamp):** `SpeedState.Reading` must stay a `data class` whose fields are ONLY genuinely-new-per-fix values
  (`kmh`, `deltaMeters`). `GpsSpeedProvider.state` is a `StateFlow`, which conflates (drops) consecutive emissions
  that are `equals()`-equal `[CITED: kotlinlang.org/api/kotlinx.coroutines StateFlow docs]`. The `combine()` block
  re-runs on every 1s `ticker` tick even when no new GPS fix arrived; if `Reading` carried a field that changes on
  every tick regardless of a real fix, `state` would re-emit every second, `MainActivity.updatePlaceholder()` would
  re-run, and `reduceDistance()` would add the SAME `deltaMeters` repeatedly — a severe, silent distance-overcounting
  bug. See Pitfall 1.
- **Duplicating `mapSpeedToKmh()`'s accuracy/noise-floor logic in a second, parallel filter** for the distance
  flow — D-07 explicitly warns against this; the recommended pipeline reuses `mapSpeedToKmh()` as-is for the
  accuracy gate and only adds a NEW, separate noise-floor-for-accumulation gate downstream in `reduceDistance()`.
- **Exposing raw `Location` objects outside `GpsSpeedProvider`** — breaks the existing architectural boundary
  (only `GpsSpeedProvider` imports `android.location.Location` today); keep `distanceTo()` calls inside the
  provider and only expose derived primitives (`Float deltaMeters`), matching how `kmh: Int` is already exposed
  instead of the raw `Location`.
- **Rounding/truncating the persisted distance to `Int` meters:** would compound rounding error across potentially
  thousands of small per-second deltas on a long drive. Persist raw `Float` meters; round only for display.
- **Introducing a new animation or accent color for the distance area:** PROJECT.md explicitly restricts the
  lime/animation exception to the charging icon only — the distance feature must use the same plain
  white-text/instant-visibility-toggle style as `maxSpeedText`.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Distance between two GPS coordinates | Manual Haversine/spherical-law-of-cosines formula on raw lat/lng doubles | `Location.distanceTo(Location)` / `Location.distanceBetween()` | Built into the Android platform (`android.location.Location`), uses a geodesic formula on the WGS84 ellipsoid (more accurate than a simple great-circle Haversine approximation), already imported transitively via `play-services-location`'s `Location` objects — zero new code, zero new dependency `[CITED: WebSearch cross-verified, MEDIUM confidence — see Sources]` |
| Locale-correct decimal formatting ("1,2" vs "1.2") | Manual locale detection + string building for the km decimal | `Resources.getString(R.string.distance_km_format, value)` with a `%1$.1f` format string | `Resources.getString()` formats using the app's current configuration locale automatically — same mechanism already implicitly relied on by every other `getString(..., arg)` call in `MainActivity.kt` (e.g. `max_speed_format`) `[ASSUMED — standard, long-stable Android Resources behavior, not independently re-verified this session; see Assumptions Log A1]` |
| Debouncing/batching SharedPreferences writes | A custom write-coalescing/timer mechanism to avoid "too many writes" | Plain `prefs.edit().putFloat(...).apply()` on every accepted fix, same as `MaxSpeedStore.write()` today | `apply()` is already asynchronous (doesn't block the caller) and is the exact pattern the project uses for every other persisted value; introducing batching adds complexity and risk of losing the last few meters if the process dies before a batched write fires |

**Key insight:** every piece of new logic this phase needs already has a working analog in the codebase
(`MaxSpeedReducer`/`MaxSpeedStore` for persistence+reduction, `mapSpeedToKmh`/`GpsSpeedProvider` for GPS filtering,
three existing `applyXWindowInsets()` functions for corner placement) — the only genuinely new piece of domain
logic is the `Location.distanceTo()` call itself, and that is a one-line platform API call, not something to build.

## Common Pitfalls

### Pitfall 1: Breaking StateFlow conflation causes runaway distance overcounting

**What goes wrong:** If `SpeedState.Reading` stops being a plain `data class` with only `kmh`/`deltaMeters`
(e.g., someone adds a timestamp field "for debugging", or changes it to a regular `class`), `GpsSpeedProvider.state`
(a `StateFlow`) stops conflating identical consecutive emissions. Since `combine()` re-runs its lambda on every 1s
`ticker` tick regardless of whether a NEW GPS fix arrived, every re-run would produce a "new" (non-equal) `Reading`
object even when nothing changed, `MainActivity.updatePlaceholder()` would re-run every second, and
`reduceDistance()` would add the same `deltaMeters` again and again — silently inflating the displayed/persisted
distance far beyond the real value.
**Why it happens:** The current architecture relies on `StateFlow`'s documented equals()-based conflation
`[CITED: kotlinlang.org/api/kotlinx.coroutines StateFlow docs]` as an implicit "only notify on genuinely new data"
guarantee — this is not enforced by the type system, only by convention (keep `Reading` a data class with only
per-fix-meaningful fields).
**How to avoid:** Keep `SpeedState.Reading` a `data class` with exactly `(kmh: Int, deltaMeters: Float)` — no
additional fields that vary independently of a genuine new accepted fix. Add a unit test asserting
`deriveSpeedState(...)` called twice with identical inputs produces `==`-equal `Reading` instances (locks the
conflation contract, not just the state-machine transitions already tested).
**Warning signs:** Displayed distance climbing noticeably while the vehicle is stationary at a red light with GPS
signal held (kmh legitimately 0, but if `Reading` carried a repeating-but-unequal field, `reduceDistance`'s
`kmh < noiseFloorKmh` gate would still correctly reject it — so this specific pitfall manifests more subtly while
MOVING at a constant reported speed with a repeating identical fix, which is rarer but not impossible on flat,
straight roads with a slow-changing GPS fix).

### Pitfall 2: Reference-point drift during extended stationary periods

**What goes wrong:** If the "last accepted location" reference is NOT updated on fixes below the noise floor
(only updated when a delta is actually added), a long stop (e.g., 10 minutes at a red light with GPS jitter of a
few meters) could leave the reference anchored to a stale position; when movement resumes, the single
`distanceTo()` jump from that stale point to the new position could include several meters of accumulated GPS
jitter as if it were real travel.
**Why it happens:** Conflating "don't add to the total" with "don't update the reference point" — they are two
separate decisions.
**How to avoid:** Always update `lastAcceptedLocation` on every accuracy-accepted fix (see Pattern 2 code), even
when `reduceDistance()` will not add that fix's delta to the total. This keeps the reference point continuously
fresh, so any future delta is computed against the most recent known-accurate position, minimizing single-jump
error.
**Warning signs:** Distance jumping by several meters right as the vehicle starts moving again after a long stop.

### Pitfall 3: Relying on `String.format()` instead of `getString()` for the km decimal

**What goes wrong:** `String.format("%1$.1f km", value)` (no explicit `Locale`) uses the JVM default locale, which
is not guaranteed to match the device/app locale in all Android configurations (e.g., per-app language override,
some OEM skins). This is a well-known Android footgun distinct from `Resources.getString(id, args)`, which always
formats against the current configuration's locale.
**Why it happens:** Both approaches "work" during casual testing on a single device with system locale == app
locale, so the bug is easy to miss until a user changes the app's per-app language.
**How to avoid:** Always call `getString(R.string.distance_km_format, display.value)` (Activity's own `getString`,
which delegates to `Resources.getString`) rather than a bare `String.format()` — matches the pattern already used
for `max_speed_format` and `speed_kmh_format` in this codebase.
**Warning signs:** Decimal separator showing "." instead of "," on an Italian-locale device, or vice versa after
a locale/language override.

### Pitfall 4: Distance area visibility inconsistent with MAX area's "hide at 0" convention

**What goes wrong:** If the distance area copies `updateMaxArea()`'s hide-at-zero behavior verbatim, "0 m" would
never be shown even right after a reset — which is misleading in the opposite direction from MAX's rationale
(MAX hides at 0 because a max of exactly 0 before any reading is ambiguous with "not yet measured"; a distance of
"0 m" right after reset is unambiguous and accurate).
**Why it happens:** Copy-pasting `updateMaxArea()`'s pattern without re-deriving the rationale for the new field.
**How to avoid:** This is explicitly left to Claude's Discretion in CONTEXT.md — recommendation is to keep the
distance area **always visible** (shows "0 m" immediately after install/reset), unlike the MAX area. Document
this divergence explicitly in the plan so it isn't mistaken for an oversight during review.
**Warning signs:** N/A (a design decision, not a bug) — flagged here so the planner makes it deliberately, not by
accident.

### Pitfall 5: 1000m display-threshold boundary rounding

**What goes wrong:** A raw value like `999.6f` meters is `< 1000f` so `formatDistanceDisplay()` routes it to the
`Meters` branch, then rounds to `1000` for display — showing "1000 m" instead of the arguably more natural
"1,0 km". This is a genuine edge case in the adaptive-format spec (D-01 only gave qualitative examples, "850 m"
and "1,2 km", not an exact boundary rule).
**Why it happens:** The branch decision uses the raw (unrounded) meters value, but the meters branch itself rounds
for display — the two operations can disagree right at the boundary.
**How to avoid:** Not a locked requirement; flagged in Open Questions below for the planner/user to confirm. The
literal implementation in Pattern 3 (branch-then-round) is a reasonable default and matches the qualitative spec
for the vast majority of values; only the ~1-meter window around exactly 1000m is affected.
**Warning signs:** QA/checkpoint testing near exactly 1km showing "1000 m" instead of "1,0 km".

## Code Examples

### Existing test convention to mirror (read from `MaxSpeedReducerTest.kt`)

```kotlin
// Source: app/src/test/java/com/sed/tachimetro/maxspeed/MaxSpeedReducerTest.kt (read directly, 2026-08-29)
class DistanceReducerTest {
    @Test
    fun aboveNoiseFloor_addsDeltaToTotal() {
        assertEquals(114.2f, reduceDistance(100f, 14.2f, kmh = 20), 0.001f)
    }

    @Test
    fun belowNoiseFloor_doesNotAddDelta() {
        // D-04: kmh below noiseFloorKmh (2.0) -> delta discarded even if deltaMeters > 0
        assertEquals(100f, reduceDistance(100f, 3.5f, kmh = 1), 0.001f)
    }

    @Test
    fun exactlyAtNoiseFloorBoundary_addsDelta() {
        // mirrors mapSpeedToKmh's "< noiseFloorKmh" (strict) -- kmh == 2 is NOT below the floor
        assertEquals(105f, reduceDistance(100f, 5f, kmh = 2), 0.001f)
    }

    @Test
    fun sanitizePersistedDistance_negativeValue_isClampedToZero() {
        assertEquals(0f, sanitizePersistedDistance(-1f), 0.001f)
    }
}
```

### `deriveSpeedState()` extended signature (test updates required)

```kotlin
// Source: extends app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt (read directly, 2026-08-29)
fun deriveSpeedState(
    lastKmh: Int?,
    lastDeltaMeters: Float,
    now: Long,
    lastAcceptedAtMs: Long,
): SpeedState = when {
    lastKmh == null -> SpeedState.Searching
    now - lastAcceptedAtMs > 5000L -> SpeedState.NoSignal
    else -> SpeedState.Reading(lastKmh, lastDeltaMeters)
}
```
**Required follow-up:** every existing call site in `GpsSpeedProviderStateTest.kt` (`deriveSpeedState(lastKmh = 42, now = ..., lastAcceptedAtMs = ...)` and `SpeedState.Reading(42)`) must be updated to pass/expect the new
`deltaMeters` parameter — this is a deliberate breaking change to a tested pure function, not an oversight; the
plan must include updating these five existing test methods.

## State of the Art

Not materially applicable — `Location.distanceTo()`, `SharedPreferences`, and Kotlin `StateFlow` conflation are
all long-stable APIs (`Location.distanceTo()` since Android API 1; `StateFlow` conflation has been the documented
behavior since `kotlinx.coroutines` 1.3's `StateFlow` introduction). No deprecated/replaced approach to flag for
this phase's scope.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `Resources.getString(id, floatArg)` with a `%1$.1f` format string renders using the app/device's current configuration locale (e.g., comma decimal separator on it-IT), matching D-01's "1,2 km" example, with no extra locale handling code needed | Don't Hand-Roll, Pattern 3, Pitfall 3 | If wrong, the km display could show "1.2" (period) instead of "1,2" (comma) on the target device/locale, requiring an explicit `String.format(Locale.getDefault(), ...)` call instead of relying on `getString()`. Low risk: this is long-standing, widely-documented Android `Resources` behavior and the exact mechanism the project's existing `max_speed_format`/`speed_kmh_format` calls already implicitly rely on (those are integer formats so the bug would not have surfaced there) — should be spot-checked on-device during the phase's human checkpoint. |
| A2 | Reference-point-always-updates-but-total-only-adds-above-noise-floor (Pitfall 2's resolution) is the correct interpretation of D-04, beyond its literal text | Architecture Patterns Pattern 2, Pitfall 2 | If the user intended a stricter "freeze the reference point entirely while below noise floor" interpretation, the plan would need a one-line change (move `lastAcceptedLocation = loc` inside the `kmh >= noiseFloorKmh` branch instead of unconditionally). Low risk: both interpretations satisfy D-04's literal wording ("no accumulation below noise floor"); the recommended version is more robust against reference-point drift (Pitfall 2) and doesn't change any user-visible behavior mentioned in CONTEXT.md. |
| A3 | Distance area should stay **always visible** (not hidden at 0, unlike the MAX area) | Pitfall 4, Open Questions | Explicitly left to Claude's Discretion in CONTEXT.md — if the user actually prefers hide-at-zero consistency with MAX, this is a one-line change to mirror `updateMaxArea()`'s visibility branch. No functional risk, only a cosmetic/consistency preference. |

## Open Questions

1. **Exact 1000m boundary display behavior (Pitfall 5)**
   - What we know: D-01 gives qualitative examples ("850 m" below 1km, "1,2 km" above) but no exact boundary spec.
   - What's unclear: whether "999.6m" should round to "1000 m" or "1,0 km".
   - Recommendation: implement Pattern 3 as-is (branch on raw unrounded meters, round only within the chosen
     branch); this is a ~1-meter-wide edge case with negligible real-world impact — flag for the planner to
     decide/confirm during checkpoint review rather than blocking on it now.

2. **Visibility of the distance area at exactly 0 (Pitfall 4 / Assumption A3)**
   - What we know: explicitly deferred to Claude's Discretion in CONTEXT.md; no requirement specifies either way.
   - What's unclear: whether the user has an implicit preference for visual consistency with the MAX area (hidden
     at 0) despite the accuracy argument for always-visible.
   - Recommendation: default to always-visible (see rationale in Pitfall 4); easy to flip to hide-at-zero if the
     human checkpoint disagrees.

## Environment Availability

Skipped — this phase introduces no new external dependency, tool, or service beyond what Phases 1-2 already
verified and integrated (FusedLocationProviderClient / Google Play Services, already required and working in this
codebase since Phase 2; SharedPreferences is core Android). No new probe needed.

## Security Domain

This is a fully local, offline, single-user Android app with no authentication, no network calls beyond the OS's
own Google Play Services location stack, and no server component — most ASVS categories do not apply.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-------------------|
| V2 Authentication | No | No accounts/auth in this app |
| V3 Session Management | No | No sessions |
| V4 Access Control | No | Single-user local app, no multi-tenant data |
| V5 Input Validation | Yes (narrow) | `sanitizePersistedDistance()` — clamps a tampered/negative persisted `Float` to 0, mirroring the existing `sanitizePersistedMax()` pattern for `MaxSpeedStore`. The only "input" here is on-disk `SharedPreferences` data, which a rooted device or backup-restore could tamper with. |
| V6 Cryptography | No | SharedPreferences here stores only a non-sensitive scalar (distance in meters); no secrets, no need for `EncryptedSharedPreferences` — consistent with `MaxSpeedStore`'s existing (unencrypted) precedent for the same-sensitivity data |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|----------------------|
| Tampered/corrupted SharedPreferences value (e.g., a negative Float written via a rooted-device editor or a bad restore) | Tampering | `sanitizePersistedDistance()` clamps to 0 on read, exactly mirroring `sanitizePersistedMax()`'s existing handling of `MaxSpeedStore` |

## Sources

### Primary (HIGH confidence)
- `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt` — read directly, current pipeline structure (rawLocations → mapSpeedToKmh → acceptedKmh → combine/ticker → state)
- `app/src/main/java/com/sed/tachimetro/gps/SpeedMapping.kt` — read directly, `mapSpeedToKmh()` full implementation
- `app/src/main/java/com/sed/tachimetro/gps/SpeedState.kt` — read directly, sealed `SpeedState` model
- `app/src/main/java/com/sed/tachimetro/maxspeed/MaxSpeedStore.kt`, `MaxSpeedReducer.kt` — read directly, persistence/reducer pattern to mirror
- `app/src/main/java/com/sed/tachimetro/screen/ScreenOnPreferenceStore.kt` — read directly, confirms shared `PREFS_NAME = "tachimetro_prefs"` convention (each store class redeclares its own constant)
- `app/src/main/java/com/sed/tachimetro/MainActivity.kt` — read directly, full file (590 lines): `onResetMaxClicked()`, `updateMaxArea()`, `applyUnitTextWindowInsets()`, `applyMaxAreaWindowInsets()`, `applyBottomLeftWindowInsets()`, lifecycle/`repeatOnLifecycle(STARTED)` wiring
- `app/src/main/res/layout/activity_main.xml` — read directly, confirms bottom-right corner is currently unused
- `app/src/main/res/values/strings.xml` — read directly, existing format strings (`max_speed_format`, `speed_kmh_format`) confirm the `getString(id, args)` convention
- `app/src/test/java/com/sed/tachimetro/gps/GpsSpeedProviderStateTest.kt`, `app/src/test/java/com/sed/tachimetro/maxspeed/MaxSpeedReducerTest.kt` — read directly, test naming/assertion convention
- `gradle/libs.versions.toml` — read directly, verified current dependency versions
- [StateFlow | kotlinx.coroutines](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-state-flow/) — official Kotlin API docs, confirms equals()-based conflation behavior

### Secondary (MEDIUM confidence)
- WebSearch cross-verification of `Location.distanceTo()` — confirms it uses a Vincenty-inverse-style geodesic formula on the WGS84 ellipsoid, returns meters as `float`. Training knowledge of this specific, long-stable Android API is treated as MEDIUM (not HIGH) because the official `developer.android.com/reference/android/location/Location` page could not be rendered via WebFetch this session (JS-heavy doc site) — cross-verified instead via WebSearch summary. [Measuring GNSS accuracy on Android devices](https://barbeau.medium.com/measuring-gnss-accuracy-on-android-devices-6824492a1389)

### Tertiary (LOW confidence)
None — all claims above were either read directly from this repository's source, confirmed via official Kotlin API docs, or cross-verified via WebSearch and flagged accordingly in the Assumptions Log.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — no new dependencies; all versions read directly from `gradle/libs.versions.toml`
- Architecture: HIGH — core D-07 recommendation is grounded in the actual, directly-read source of `GpsSpeedProvider.kt`/`SpeedMapping.kt`/`MainActivity.kt`, and the load-bearing `StateFlow` conflation claim is confirmed against official Kotlin API docs
- Pitfalls: HIGH — Pitfall 1 (conflation) is derived from reading the real `combine()`/`stateIn()` code plus official docs, not speculation; Pitfalls 2-5 are direct consequences of the locked D-04/D-06 decisions

**Research date:** 2026-08-29
**Valid until:** 90 days (stable, single-module native Android app with no fast-moving dependencies; all APIs involved — `Location`, `SharedPreferences`, `StateFlow` — have been stable for years)
