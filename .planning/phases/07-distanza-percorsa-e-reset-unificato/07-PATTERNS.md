# Phase 7: Distanza Percorsa e Reset Unificato - Pattern Map

**Mapped:** 2026-08-29
**Files analyzed:** 9
**Analogs found:** 9 / 9

This phase is a pure extension exercise — every new/modified file has a direct, exact-match analog
already in the codebase (either a sibling file to mirror, or the file's own current content to extend
in place). No file lacks a precedent.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|--------------------|------|-----------|-----------------|----------------|
| `app/src/main/java/com/sed/tachimetro/distance/DistanceStore.kt` (NEW) | store/persistence | CRUD | `app/src/main/java/com/sed/tachimetro/maxspeed/MaxSpeedStore.kt` | exact |
| `app/src/main/java/com/sed/tachimetro/distance/DistanceReducer.kt` (NEW) | utility (pure function) | transform | `app/src/main/java/com/sed/tachimetro/maxspeed/MaxSpeedReducer.kt` | exact |
| `app/src/test/java/com/sed/tachimetro/distance/DistanceReducerTest.kt` (NEW) | test | transform | `app/src/test/java/com/sed/tachimetro/maxspeed/MaxSpeedReducerTest.kt` | exact |
| `app/src/main/java/com/sed/tachimetro/gps/SpeedState.kt` (MODIFY) | model | event-driven | itself (current content) | exact (in-place extension) |
| `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt` (MODIFY) | service | streaming/event-driven | itself (current content) | exact (in-place extension) |
| `app/src/test/java/com/sed/tachimetro/gps/GpsSpeedProviderStateTest.kt` (MODIFY) | test | transform | itself (current content) | exact (in-place extension) |
| `app/src/main/java/com/sed/tachimetro/MainActivity.kt` (MODIFY) | controller/activity | request-response (UI) + event-driven (state collection) | itself (current content — `maxSpeedText`/`resetMaxButton`/`updateMaxArea`/`onResetMaxClicked`/`applyMaxAreaWindowInsets` sections) | exact (mirror existing MAX-area code within the same file) |
| `app/src/main/res/layout/activity_main.xml` (MODIFY) | config (layout) | n/a | itself (current content — `maxSpeedText`/`resetMaxButton`/`unitText` blocks) | exact (mirror existing blocks) |
| `app/src/main/res/values/strings.xml` (MODIFY) | config | n/a | itself (current content — `max_speed_format`, `unit_kmh`, `reset_max_button`) | exact (mirror existing format-string conventions) |

## Pattern Assignments

### `app/src/main/java/com/sed/tachimetro/distance/DistanceStore.kt` (store, CRUD)

**Analog:** `app/src/main/java/com/sed/tachimetro/maxspeed/MaxSpeedStore.kt` (full file, 25 lines)

**Full pattern to mirror** (lines 1-25):
```kotlin
package com.sed.tachimetro.maxspeed

import android.content.Context

/**
 * D-06/D-07/D-08: persistenza del massimo (un solo Int) via SharedPreferences app-private.
 * Niente Room/DataStore: un intero non giustifica una dipendenza (CONTEXT Established Patterns).
 */
class MaxSpeedStore(context: Context) {
    // WR-04: il chiamante passa applicationContext, mai un'Activity.
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Legge il massimo persistito, azzerando qualsiasi valore malformato/negativo (T-04-01). */
    fun read(): Int = sanitizePersistedMax(prefs.getInt(KEY_MAX_SPEED, 0))

    /** D-07/D-08: scrittura immediata asincrona (apply(), fuori dal main thread) — un solo Int economico. */
    fun write(value: Int) {
        prefs.edit().putInt(KEY_MAX_SPEED, value).apply()
    }

    companion object {
        const val PREFS_NAME = "tachimetro_prefs"
        const val KEY_MAX_SPEED = "max_speed_kmh"
    }
}
```

**Adaptation notes:**
- Same `PREFS_NAME = "tachimetro_prefs"` shared file (confirmed also in `ScreenOnPreferenceStore.kt:25`) — each store class redeclares the constant, does not import it from another store.
- Swap `Int`/`getInt`/`putInt` for `Float`/`getFloat`/`putFloat` (distance persisted as raw unrounded meters — RESEARCH.md's `Don't Hand-Roll` table explicitly rejects truncating to `Int`).
- New key: `KEY_DISTANCE_METERS = "distance_meters"`.
- `sanitizePersistedMax` → `sanitizePersistedDistance` (from `DistanceReducer.kt`, see below) — same clamp-negative-to-0 shape.
- Constructor takes `context: Context` and the caller passes `applicationContext` (WR-04) — verify at the `MainActivity` call site, not inside this class.

---

### `app/src/main/java/com/sed/tachimetro/distance/DistanceReducer.kt` (utility, transform)

**Analog:** `app/src/main/java/com/sed/tachimetro/maxspeed/MaxSpeedReducer.kt` (full file, 13 lines)

**Full pattern to mirror** (lines 1-13):
```kotlin
package com.sed.tachimetro.maxspeed

import kotlin.math.max

/** D-07: il massimo di sessione cresce solo verso la lettura più alta vista; letture piu' basse o anomale non lo abbassano. */
fun reduceMax(currentMax: Int, reading: Int): Int {
    val safeCurrent = if (currentMax < 0) 0 else currentMax
    val safeReading = if (reading < 0) 0 else reading
    return max(safeCurrent, safeReading)
}

/** T-04-01: un valore persistito manomesso/negativo viene riportato a 0 alla lettura. */
fun sanitizePersistedMax(raw: Int): Int = if (raw < 0) 0 else raw
```

**Adaptation notes (three functions needed, not one — per RESEARCH.md Pattern 1 and Pattern 3):**
- `reduceDistance(currentTotalMeters: Float, deltaMeters: Float, kmh: Int, noiseFloorKmh: Double = 2.0): Float` — same clamp-negatives-defensively shape as `reduceMax`, but gate is D-04's noise floor on `kmh`, not a `max()` — mirrors `mapSpeedToKmh`'s `noiseFloorKmh: Double = 2.0` default (`SpeedMapping.kt:18`) so the constant stays consistent across both filter and accumulation, not hardcoded twice.
- `sanitizePersistedDistance(raw: Float): Float = if (raw < 0f) 0f else raw` — direct mirror of `sanitizePersistedMax`, `Float` instead of `Int`.
- `formatDistanceDisplay(meters: Float): DistanceDisplay` — new pure function, no direct prior analog in this codebase (the closest conceptual sibling is `mapSpeedToKmh()` in `SpeedMapping.kt:12-35`: framework-free, primitives/sealed-model in and out, no `Context`/`getString` inside it). Implements D-01's meters-vs-km branch as a `sealed class DistanceDisplay { data class Meters(val value: Int); data class Kilometers(val value: Float) }`.
- KDoc must cite decision tags per project convention (see CLAUDE.md "Comments"): `D-04`, `D-06`, `T-04-01`-style tag for sanitize, `D-01` for the formatter.

---

### `app/src/test/java/com/sed/tachimetro/distance/DistanceReducerTest.kt` (test, transform)

**Analog:** `app/src/test/java/com/sed/tachimetro/maxspeed/MaxSpeedReducerTest.kt` (full file, 60 lines)

**Full pattern to mirror** (lines 1-60):
```kotlin
package com.sed.tachimetro.maxspeed

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Plain JVM unit tests for [reduceMax] and [sanitizePersistedMax] -- no Android runtime.
 * Locks D-07 (monotonic growth of the session max) and T-04-01 (sanitization of a
 * tampered/negative persisted value).
 */
class MaxSpeedReducerTest {

    @Test
    fun firstReading_growsFromZero() {
        assertEquals(50, reduceMax(0, 50))
    }

    @Test
    fun lowerReading_doesNotLowerMax() {
        assertEquals(120, reduceMax(120, 80))
    }

    // ... higherReading_updatesMax, equalReading_staysUnchanged,
    //     negativeReading_treatedAsZero_doesNotLowerMax,
    //     sanitizePersistedMax_validValue_passesThrough,
    //     sanitizePersistedMax_zero_isValid,
    //     sanitizePersistedMax_negativeValue_isClampedToZero
}
```

**Test naming convention confirmed:** `[condition]_returns/does[Outcome]` snake_case-ish camelCase, one behavior per test, no test class setup/teardown needed (pure functions, no mocks). `assertEquals(expected, actual)` two-arg form for `Int`; **note the `Float` overload requires a delta**, e.g. `assertEquals(114.2f, reduceDistance(100f, 14.2f, kmh = 20), 0.001f)` (RESEARCH.md Code Examples already spells out the exact four test cases: `aboveNoiseFloor_addsDeltaToTotal`, `belowNoiseFloor_doesNotAddDelta`, `exactlyAtNoiseFloorBoundary_addsDelta`, `sanitizePersistedDistance_negativeValue_isClampedToZero`).

---

### `app/src/main/java/com/sed/tachimetro/gps/SpeedState.kt` (model, event-driven) — MODIFY

**Analog:** itself, current full content (13 lines)

**Current content to extend:**
```kotlin
package com.sed.tachimetro.gps

/** Sealed model of the GPS engine's exposed speed state (D-06). */
sealed class SpeedState {
    /** D-01: shown from startup until the first accepted fix arrives. */
    data object Searching : SpeedState()

    /** D-09: whole km/h, no decimals. */
    data class Reading(val kmh: Int) : SpeedState()

    /** D-02: no accepted update has arrived for more than 5 seconds. */
    data object NoSignal : SpeedState()
}
```

**Required change:** `Reading(val kmh: Int)` → `Reading(val kmh: Int, val deltaMeters: Float)`.

**CRITICAL correctness constraint (RESEARCH.md Pitfall 1, HIGH confidence, verified against official `StateFlow` docs):**
`Reading` MUST remain a plain Kotlin `data class` with ONLY `kmh` and `deltaMeters` as fields — no timestamp or
any other per-tick-changing field. `GpsSpeedProvider.state` is a `StateFlow`, which conflates (drops) consecutive
`equals()`-equal emissions; the once-per-second `ticker` re-runs `combine()` every tick regardless of whether a
new GPS fix arrived, so a field that changes independently of a genuine new fix would break dedup and cause
`reduceDistance()` to add the same `deltaMeters` repeatedly (silent distance overcounting). Update the KDoc on
`Reading` to note this constraint explicitly for future maintainers.

---

### `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt` (service, streaming/event-driven) — MODIFY

**Analog:** itself, current full content (139 lines)

**Imports pattern** (lines 1-29) — unchanged, no new imports needed (`Location.distanceTo()` is a method on
the already-imported `android.location.Location`, line 4).

**Core pattern to extend — current pipeline** (lines 77-114):
```kotlin
private val acceptedKmh: Flow<Int> = rawLocations
    .map { loc ->
        mapSpeedToKmh(
            hasAccuracy = loc.hasAccuracy(),
            accuracyMeters = loc.accuracy,
            hasSpeed = loc.hasSpeed(),
            speedMetersPerSecond = loc.speed,
            accuracyThresholdMeters = accuracyThresholdMeters,
            noiseFloorKmh = noiseFloorKmh,
        )
    }
    .filterNotNull() // D-05: drop poor-accuracy readings, do not update the shown value
    .map { kmh ->
        lastAcceptedUpdateAtMs = SystemClock.elapsedRealtime()
        kmh
    }

// 1-second ticker: also drives the once-per-second staleness check (GPS-01 cadence).
private val ticker: Flow<Long> = flow {
    while (true) {
        emit(SystemClock.elapsedRealtime())
        delay(1000)
    }
}

val state: StateFlow<SpeedState> = combine(
    acceptedKmh.map { it as Int? }.onStart { emit(null) },
    ticker,
) { lastKmh, now ->
    deriveSpeedState(lastKmh, now, lastAcceptedUpdateAtMs)
}.stateIn(
    scope = scope,
    started = SharingStarted.WhileSubscribed(),
    initialValue = SpeedState.Searching,
)
```

**Required change (D-06/D-07):** rename `acceptedKmh: Flow<Int>` to `acceptedReadings: Flow<AcceptedReading>`
where `private data class AcceptedReading(val kmh: Int, val deltaMeters: Float)`; add a `@Volatile private var
lastAcceptedLocation: Location? = null` field (mirrors the existing `@Volatile private var lastAcceptedUpdateAtMs:
Long = 0L` at line 61-62); compute `val delta = lastAcceptedLocation?.distanceTo(loc) ?: 0f` inside the SAME
`.map { loc -> ... }` block that already calls `mapSpeedToKmh()` (do NOT duplicate the accuracy/noise filter —
`mapSpeedToKmh()` at `SpeedMapping.kt:12-35` stays the single source of truth, per D-07 and the "Anti-Patterns to
Avoid" section of RESEARCH.md). Update `lastAcceptedLocation = loc` unconditionally on every accuracy-accepted fix
(RESEARCH.md Pitfall 2 — reference-point drift — requires this even for fixes the noise floor will later reject
in `reduceDistance()`). Update `combine(...)` and `deriveSpeedState(...)` call to thread `deltaMeters` through.

**Function signature pattern to extend** (lines 135-139):
```kotlin
fun deriveSpeedState(lastKmh: Int?, now: Long, lastAcceptedAtMs: Long): SpeedState = when {
    lastKmh == null -> SpeedState.Searching // D-01: no accepted fix yet
    now - lastAcceptedAtMs > 5000L -> SpeedState.NoSignal // D-02
    else -> SpeedState.Reading(lastKmh)
}
```
→ becomes `deriveSpeedState(lastKmh: Int?, lastDeltaMeters: Float, now: Long, lastAcceptedAtMs: Long): SpeedState`
with `SpeedState.Reading(lastKmh, lastDeltaMeters)` in the `else` branch — this is a pure, testable top-level
function (WR-02 convention), unchanged in shape, only the parameter list and final `Reading(...)` call grow.

**Permission/suppression pattern** (line 66, unchanged, applies to the extended `.map` block too):
```kotlin
@Suppress("MissingPermission")
private val rawLocations: Flow<Location> = callbackFlow { ... }
```

**Error handling pattern:** none needed beyond what already exists — `result.lastLocation?.let { trySend(it) }`
(line 70) silently skips null locations; `mapSpeedToKmh()` returning `null` is the existing "drop this reading"
signal, reused unchanged for the distance delta too (if `kmh == null`, no `AcceptedReading` is emitted at all,
`.filterNotNull()` drops it exactly like today).

---

### `app/src/test/java/com/sed/tachimetro/gps/GpsSpeedProviderStateTest.kt` (test, transform) — MODIFY

**Analog:** itself, current full content (46 lines)

**Current pattern** (lines 20-25, one of five call sites needing the same edit):
```kotlin
@Test
fun recentAcceptedFix_returnsReading() {
    val result = deriveSpeedState(lastKmh = 42, now = 4_000L, lastAcceptedAtMs = 1_000L)
    assertEquals(SpeedState.Reading(42), result)
}
```

**Required change:** every one of the five existing `@Test` methods in this file calls either `deriveSpeedState(
lastKmh = ..., now = ..., lastAcceptedAtMs = ...)` or constructs `SpeedState.Reading(42)` — both now require the
extra `deltaMeters`/second positional argument. RESEARCH.md explicitly flags this as "a deliberate breaking change
to a tested pure function, not an oversight" and requires the plan to update all five test methods (lines 14-45),
e.g. `deriveSpeedState(lastKmh = 42, lastDeltaMeters = 5.0f, now = 4_000L, lastAcceptedAtMs = 1_000L)` and
`SpeedState.Reading(42, 5.0f)`. Consider adding a new test asserting `deriveSpeedState(...)` called twice with
identical inputs produces `==`-equal `Reading` instances, to lock the `StateFlow` conflation contract (Pitfall 1).

---

### `app/src/main/java/com/sed/tachimetro/MainActivity.kt` (controller/activity) — MODIFY

**Analog:** itself, current full content (590 lines) — new distance code mirrors the existing MAX-area code
section by section.

**Imports pattern** (lines 40-46):
```kotlin
import com.sed.tachimetro.charging.ChargingState
import com.sed.tachimetro.charging.ChargingStateProvider
import com.sed.tachimetro.gps.GpsSpeedProvider
import com.sed.tachimetro.gps.SpeedState
import com.sed.tachimetro.maxspeed.MaxSpeedStore
import com.sed.tachimetro.maxspeed.reduceMax
import com.sed.tachimetro.screen.ScreenOnPreferenceStore
```
Add: `import com.sed.tachimetro.distance.DistanceStore`, `import com.sed.tachimetro.distance.reduceDistance`,
`import com.sed.tachimetro.distance.formatDistanceDisplay`, `import com.sed.tachimetro.distance.DistanceDisplay`
(or however the sealed type is named — see `DistanceReducer.kt` above).

**View field + store field pattern** (lines 76-85):
```kotlin
private lateinit var messageText: TextView
private lateinit var unitText: TextView
private lateinit var retryButton: Button
private lateinit var maxSpeedText: TextView
private lateinit var resetMaxButton: Button
private lateinit var maxSpeedStore: MaxSpeedStore
private var currentMax: Int = 0
```
Add: `distanceText: TextView`, `distanceUnitText: TextView`, `distanceStore: DistanceStore`,
`currentDistanceMeters: Float = 0f` — same `lateinit var`/`private var` split as the MAX fields.

**"Read persisted value before starting GPS collection" pattern (D-09)** (lines 118-126):
```kotlin
maxSpeedText = findViewById(R.id.maxSpeedText)
resetMaxButton = findViewById(R.id.resetMaxButton)
resetMaxButton.setOnClickListener { onResetMaxClicked() }
applyMaxAreaWindowInsets()
// D-09: leggere il massimo salvato PRIMA di avviare la raccolta GPS, cosi' l'area MAX
// appare gia' con lo stato corretto senza flash di "MAX 0".
maxSpeedStore = MaxSpeedStore(applicationContext)
currentMax = maxSpeedStore.read()
updateMaxArea()
```
Mirror exactly: `distanceText = findViewById(...)`, `distanceUnitText = findViewById(...)`,
`applyDistanceAreaWindowInsets()`, `distanceStore = DistanceStore(applicationContext)`,
`currentDistanceMeters = distanceStore.read()`, `updateDistanceArea()` — placed in `onCreate()` alongside the
existing MAX block (both run before `gpsSpeedProvider = GpsSpeedProvider(applicationContext)` at line 155).

**State-consumption pattern in the GPS collector** (lines 308-335, `updatePlaceholder`):
```kotlin
is SpeedState.Reading -> {
    unitText.visibility = View.VISIBLE
    applySpeedAutosize()
    messageText.text = state.kmh.toString()

    // D-07: update and persist the session max immediately whenever the current
    // reading exceeds it -- no batching to onPause()/onStop().
    val newMax = reduceMax(currentMax, state.kmh)
    if (newMax != currentMax) {
        currentMax = newMax
        maxSpeedStore.write(currentMax)
    }
}
```
Add, in the same `is SpeedState.Reading ->` branch, right after the max-update block:
```kotlin
val newDistance = reduceDistance(currentDistanceMeters, state.deltaMeters, state.kmh)
if (newDistance != currentDistanceMeters) {
    currentDistanceMeters = newDistance
    distanceStore.write(currentDistanceMeters)
}
```
Then call `updateDistanceArea()` — mirrors the unconditional `updateMaxArea()` call at line 334 (called every
`updatePlaceholder()` invocation, not just on change, same as MAX).

**Reset pattern** (lines 337-343, `onResetMaxClicked`):
```kotlin
// D-04/D-08: reset tap zeroes the in-memory max immediately (no confirmation dialog) and
// persists 0 to disk right away, so a re-open right after reset never resurrects the old max.
private fun onResetMaxClicked() {
    currentMax = 0
    maxSpeedStore.write(0)
    updateMaxArea()
}
```
**D-08 requires extending this SAME function** (not adding a second button/handler — MAX-04 mandates one
unified reset action):
```kotlin
private fun onResetMaxClicked() {
    currentMax = 0
    maxSpeedStore.write(0)
    updateMaxArea()
    currentDistanceMeters = 0f
    distanceStore.write(0f)
    updateDistanceArea()
}
```
Consider renaming the function (e.g. `onResetClicked()`) to match the button's new generic label — see string
change below — but keep it as the single reset entry point.

**Show/hide-at-zero visibility pattern** (lines 345-356, `updateMaxArea` — the pattern to consciously DIVERGE
from, per RESEARCH.md Pitfall 4):
```kotlin
// D-03/D-09: the whole MAX area (label + reset button) stays hidden while the max is 0 --
// never renders a misleading "MAX 0". Plain visibility toggle, no animation (UI-04).
private fun updateMaxArea() {
    if (currentMax > 0) {
        maxSpeedText.text = getString(R.string.max_speed_format, currentMax)
        maxSpeedText.visibility = View.VISIBLE
        resetMaxButton.visibility = View.VISIBLE
    } else {
        maxSpeedText.visibility = View.GONE
        resetMaxButton.visibility = View.GONE
    }
}
```
`updateDistanceArea()` should call `formatDistanceDisplay(currentDistanceMeters)` (from `DistanceReducer.kt`) and
set `distanceText`/`distanceUnitText.text` via `getString(R.string.distance_meters_format, ...)` or
`getString(R.string.distance_km_format, ...)` (mirrors `updateMaxArea()`'s `getString(R.string.max_speed_format,
currentMax)` call, `Resources.getString()` locale-formats the km decimal automatically — RESEARCH.md Pitfall 3).
**Divergence (Claude's Discretion / CONTEXT.md):** keep `distanceText`/`distanceUnitText` **always `VISIBLE`**
(no `if (currentDistanceMeters > 0)` hide branch) — "0 m" after a reset is accurate, unlike "MAX 0" before any
reading. Document this divergence inline with a comment referencing the decision, matching the project's
citation convention.

**Window insets pattern for a NEW corner** (lines 512-537, `applyMaxAreaWindowInsets` — top+left, two views —
closest structural analog for a new bottom+right, two-view corner):
```kotlin
private fun applyMaxAreaWindowInsets() {
    val labelParams = maxSpeedText.layoutParams as ConstraintLayout.LayoutParams
    val labelBaseTop = labelParams.topMargin
    val labelBaseStart = labelParams.marginStart
    val buttonParams = resetMaxButton.layoutParams as ConstraintLayout.LayoutParams
    val buttonBaseStart = buttonParams.marginStart
    ViewCompat.setOnApplyWindowInsetsListener(maxSpeedText) { view, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
        val extraTop = maxOf(systemBars.top, cutout.top)
        val extraStart = maxOf(systemBars.left, cutout.left)
        val lp = view.layoutParams as ConstraintLayout.LayoutParams
        lp.topMargin = labelBaseTop + extraTop
        lp.marginStart = labelBaseStart + extraStart
        view.layoutParams = lp
        val bp = resetMaxButton.layoutParams as ConstraintLayout.LayoutParams
        bp.marginStart = buttonBaseStart + extraStart
        resetMaxButton.layoutParams = bp
        insets
    }
}
```
Also see `applyBottomLeftWindowInsets()` (lines 571-589) for the `bottom`/`marginEnd` (right-side) axis
combination already used once for `keepScreenOnSwitch`/`chargingIcon` — combine `bottomMargin +=
maxOf(systemBars.bottom, cutout.bottom)` (from `applyBottomLeftWindowInsets`) with `marginEnd +=
maxOf(systemBars.right, cutout.right)` (from `applyUnitTextWindowInsets`, lines 499-509) to get the new
bottom+right `applyDistanceAreaWindowInsets()`. If `distanceUnitText` is pinned independently of `distanceText`
(D-02: separate small unit view, same pattern as `unitText`), register its own listener OR update its margins
inside the same listener as `distanceText`, exactly as `applyMaxAreaWindowInsets()` updates `resetMaxButton`'s
margin inside `maxSpeedText`'s listener callback.

**`onCreate()` wiring order:** call `applyDistanceAreaWindowInsets()` immediately after `applyMaxAreaWindowInsets()`
(line 121), before `maxSpeedStore = MaxSpeedStore(...)` — matches the existing ordering convention (bind views →
set click listener → apply insets → construct store → read persisted value → render).

---

### `app/src/main/res/layout/activity_main.xml` (layout config) — MODIFY

**Analog:** itself, current full content (122 lines) — `maxSpeedText`/`resetMaxButton` block for the large
number + label pair, `unitText` block for the small separate unit view (D-02).

**Large-value + reset-button block to mirror** (lines 60-87):
```xml
<TextView
    android:id="@+id/maxSpeedText"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_marginStart="16dp"
    android:layout_marginTop="16dp"
    android:maxLines="1"
    android:singleLine="true"
    android:textColor="@android:color/white"
    android:textSize="22sp"
    android:visibility="gone"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintTop_toTopOf="parent"
    tools:text="MAX 120"
    tools:visibility="visible" />

<Button
    android:id="@+id/resetMaxButton"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_marginStart="16dp"
    android:layout_marginTop="8dp"
    android:minHeight="48dp"
    android:text="@string/reset_max_button"
    android:visibility="gone"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintTop_toBottomOf="@id/maxSpeedText"
    tools:visibility="visible" />
```

**Small separate unit-label view to mirror for `distanceUnitText`** (D-02, lines 33-45):
```xml
<TextView
    android:id="@+id/unitText"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_marginTop="16dp"
    android:layout_marginEnd="16dp"
    android:text="@string/unit_kmh"
    android:textColor="@android:color/white"
    android:textSize="22sp"
    android:visibility="gone"
    app:layout_constraintEnd_toEndOf="parent"
    app:layout_constraintTop_toTopOf="parent"
    tools:visibility="visible" />
```

**Adaptation notes:**
- New `distanceText` anchors `app:layout_constraintBottom_toBottomOf="parent"` +
  `app:layout_constraintEnd_toEndOf="parent"` (bottom-right corner — confirmed free in this file; no existing
  view uses `constraintBottom` + `constraintEnd` together) with `android:textSize="32sp"` (D-03, hardcoded, not
  an autosize range — unlike `messageText`) instead of `22sp`.
- `distanceUnitText` sits "accanto al numero" (D-02) — small, separate view like `unitText`, NOT concatenated
  into `distanceText` like `max_speed_format`'s `"MAX %1$d"` combined string.
- Per Divergence note above (Claude's Discretion), do NOT set `android:visibility="gone"` as the base XML state
  for `distanceText`/`distanceUnitText` if the always-visible approach is adopted — `maxSpeedText`/`unitText`/
  `resetMaxButton` all start `gone` in XML because they're hidden until a real reading/max exists; the distance
  views may instead start `visible` in XML (showing "0 m" pre-GPS) if `MainActivity.onCreate()` sets text from
  `distanceStore.read()` before any GPS state arrives (mirrors D-09's persisted-value-read-before-GPS-start
  pattern already used for `updateMaxArea()`).
- `tools:text="850 m"` / `tools:text="m"` placeholders for layout-editor preview, matching the `tools:text="MAX
  120"` convention.

---

### `app/src/main/res/values/strings.xml` (config) — MODIFY

**Analog:** itself, current full content (15 lines)

**Format-string convention to mirror** (lines 9, 11-12):
```xml
<string name="speed_kmh_format">%1$d km/h</string>
...
<string name="max_speed_format">MAX %1$d</string>
<string name="reset_max_button">Azzera massimo</string>
```

**Required changes:**
- `reset_max_button` value changes from `"Azzera massimo"` to `"Azzera"` (D-08) — key name unchanged (no need
  to rename the resource, only its value, since `MainActivity.kt` and `activity_main.xml` both reference it by
  key `@string/reset_max_button` / `R.string.reset_max_button`).
- New keys needed (naming left to Claude's Discretion per CONTEXT.md, suggested to match existing
  `max_speed_format`/`unit_kmh` naming shape):
  - `distance_meters_format` → `"%1$d m"` (mirrors `speed_kmh_format`'s `%1$d` integer placeholder)
  - `distance_km_format` → `"%1$.1f km"` (new — one-decimal float placeholder, no existing analog in this file,
    but `%1$.1f` is standard Android `Resources.getString()` format syntax, same mechanism as the `%1$d` calls)
  - `unit_meters` → `"m"` (mirrors `unit_kmh`'s bare-unit-string shape, line 10)
  - `unit_km` → `"km"`
- Values are Italian (`it` is the app's working locale per existing strings like `"Ricerca segnale GPS..."`) —
  keep new strings in Italian too, consistent with every existing string in this file.

---

## Shared Patterns

### SharedPreferences single-value store (persistence)
**Source:** `app/src/main/java/com/sed/tachimetro/maxspeed/MaxSpeedStore.kt` (full file), confirmed shared
`PREFS_NAME` constant convention via `app/src/main/java/com/sed/tachimetro/screen/ScreenOnPreferenceStore.kt:25`
**Apply to:** `DistanceStore.kt`
```kotlin
class MaxSpeedStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    fun read(): Int = sanitizePersistedMax(prefs.getInt(KEY_MAX_SPEED, 0))
    fun write(value: Int) { prefs.edit().putInt(KEY_MAX_SPEED, value).apply() }
    companion object {
        const val PREFS_NAME = "tachimetro_prefs"
        const val KEY_MAX_SPEED = "max_speed_kmh"
    }
}
```
Every store class: (1) takes `context: Context`, caller passes `applicationContext` (WR-04); (2) opens the SAME
`"tachimetro_prefs"` file; (3) `read()` sanitizes via a paired pure function; (4) `write()` uses `apply()`
(async, never `commit()`); (5) constants live in a `companion object`.

### Pure reducer / sanitizer functions (domain logic)
**Source:** `app/src/main/java/com/sed/tachimetro/maxspeed/MaxSpeedReducer.kt` (full file)
**Apply to:** `DistanceReducer.kt` (`reduceDistance`, `sanitizePersistedDistance`, `formatDistanceDisplay`)
```kotlin
fun reduceMax(currentMax: Int, reading: Int): Int {
    val safeCurrent = if (currentMax < 0) 0 else currentMax
    val safeReading = if (reading < 0) 0 else reading
    return max(safeCurrent, safeReading)
}
fun sanitizePersistedMax(raw: Int): Int = if (raw < 0) 0 else raw
```
Top-level (not class-member) functions; framework-free (no `android.*`/`com.google.android.gms.*` imports);
defensively clamp negative inputs to a safe floor before applying the real logic; unit-tested on plain JVM
(matches `SpeedMapping.kt`'s `mapSpeedToKmh()` shape too).

### Corner-pinned window insets listener
**Source:** `app/src/main/java/com/sed/tachimetro/MainActivity.kt` — four instances of the same shape:
`applyUnitTextWindowInsets()` (lines 495-510, top+right), `applyMaxAreaWindowInsets()` (lines 517-537,
top+left, 2 views), `applyBottomLeftWindowInsets()` (lines 571-589, bottom+left, 2 views).
**Apply to:** new `applyDistanceAreaWindowInsets()` (bottom+right)
```kotlin
ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
    val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
    val extra = maxOf(systemBars.SIDE, cutout.SIDE)
    val lp = v.layoutParams as ConstraintLayout.LayoutParams
    lp.MARGIN = base + extra
    v.layoutParams = lp
    insets
}
```
Capture each view's XML-declared base margin BEFORE registering the listener (`val baseX = params.xMargin`);
listener adds the live inset on top of that base, never replaces it; always `return insets` unconsumed (other
listeners/children still need it); multi-view groups update all views' `layoutParams` inside one listener
registered on the "anchor" view (the one always `VISIBLE`, not one that toggles `GONE`).

### Decision-tag comment citation
**Source:** every file in `app/src/main/java/com/sed/tachimetro/` (e.g. `GpsSpeedProvider.kt:88`,
`MainActivity.kt:325-326`, `MaxSpeedReducer.kt:5`)
**Apply to:** all new/modified files
Inline comments prefix with the CONTEXT.md/RESEARCH.md decision tag they implement, e.g. `// D-04: ...`,
`// D-06: ...`, `// DIST-03: ...`. KDoc on public classes/functions also cites tags in the first sentence.

## No Analog Found

None. Every file in this phase's scope has a close, concrete precedent — either a sibling file to mirror
(`MaxSpeedStore.kt` → `DistanceStore.kt`, `MaxSpeedReducer.kt` → `DistanceReducer.kt`,
`MaxSpeedReducerTest.kt` → `DistanceReducerTest.kt`) or its own current content to extend in place
(`GpsSpeedProvider.kt`, `SpeedState.kt`, `GpsSpeedProviderStateTest.kt`, `MainActivity.kt`, `activity_main.xml`,
`strings.xml`). The only genuinely new logic — `formatDistanceDisplay()`'s meters/km branch — has a strong
structural analog in `mapSpeedToKmh()` (`SpeedMapping.kt`) even though no prior function does unit-format
branching specifically.

## Metadata

**Analog search scope:** `app/src/main/java/com/sed/tachimetro/` (all packages: `gps/`, `maxspeed/`, `screen/`,
`charging/`, root), `app/src/main/res/layout/`, `app/src/main/res/values/`, `app/src/test/java/com/sed/tachimetro/`
**Files scanned:** `GpsSpeedProvider.kt`, `SpeedState.kt`, `SpeedMapping.kt`, `MaxSpeedStore.kt`,
`MaxSpeedReducer.kt`, `ScreenOnPreferenceStore.kt`, `MainActivity.kt`, `activity_main.xml`, `strings.xml`,
`MaxSpeedReducerTest.kt`, `GpsSpeedProviderStateTest.kt` (11 files read directly for this pattern map)
**Pattern extraction date:** 2026-08-29
