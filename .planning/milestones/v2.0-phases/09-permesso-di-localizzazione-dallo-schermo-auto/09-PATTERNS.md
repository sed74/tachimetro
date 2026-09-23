# Phase 9: Permesso di Localizzazione dallo Schermo Auto - Pattern Map

**Mapped:** 2026-09-02
**Files analyzed:** 5 (2 modified, 3 new)
**Analogs found:** 5 / 5

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|--------------------|------|-----------|-----------------|----------------|
| `app/src/main/java/com/sed/tachimetro/car/SpeedScreen.kt` | screen/controller (Car App Library `Screen`) | event-driven (lifecycle + permission callback) | `app/src/main/java/com/sed/tachimetro/MainActivity.kt` (permission flow: `checkAndRequestPermission()`/`onRetryClicked()`/`refreshPermissionState()`, lines 103-113, 304-334) + itself (existing T-08-08 gate, lines 54-77) | role-match (self, extended with MainActivity's permission-flow shape) |
| `app/src/main/java/com/sed/tachimetro/car/CarPermissionState.kt` | model (sealed state) + utility (pure resolver function) | transform | `app/src/main/java/com/sed/tachimetro/gps/SpeedState.kt` (sealed class shape) + `app/src/main/java/com/sed/tachimetro/maxspeed/MaxSpeedReducer.kt` (pure resolver-function shape) | exact (composite of two exact analogs) |
| `app/src/main/java/com/sed/tachimetro/car/CarPermissionDenialStore.kt` | store (SharedPreferences persistence) | CRUD (single Int read/write) | `app/src/main/java/com/sed/tachimetro/maxspeed/MaxSpeedStore.kt` | exact |
| `app/src/test/java/com/sed/tachimetro/car/CarPermissionStateTest.kt` | test (plain JVM unit test) | transform | `app/src/test/java/com/sed/tachimetro/maxspeed/MaxSpeedReducerTest.kt` | exact |
| `app/src/main/res/values/strings.xml` | config/resource | N/A (static resource) | itself — existing `car_searching_gps_signal` entry (line 10) establishes the "shorter car copy" pattern | exact |

## Pattern Assignments

### `app/src/main/java/com/sed/tachimetro/car/SpeedScreen.kt` (screen/controller, event-driven)

**Analogs:** `app/src/main/java/com/sed/tachimetro/car/SpeedScreen.kt` itself (structure to extend) and `app/src/main/java/com/sed/tachimetro/MainActivity.kt` (permission-flow shape to mirror, adapted to `CarContext`).

**Imports pattern — existing file, lines 1-24** (extend, do not replace):
```kotlin
package com.sed.tachimetro.car

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import kotlinx.coroutines.launch

import com.sed.tachimetro.BuildConfig
import com.sed.tachimetro.R
import com.sed.tachimetro.TachimetroApplication
import com.sed.tachimetro.gps.SpeedState
```
New imports needed: `android.content.Intent`, `android.net.Uri`, `android.provider.Settings`, `androidx.car.app.model.ParkedOnlyOnClickListener`.

**Gate to replace (existing, lines 54-77)** — the T-08-08 defensive gate being superseded, kept here as the exact removal target:
```kotlin
init {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            // T-08-08: gate difensivo sul permesso ...
            val granted = ContextCompat.checkSelfPermission(
                carContext,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED

            if (granted) {
                provider?.gpsSpeedProvider?.state?.collect { state ->
                    latestState = state
                    invalidate()
                }
            }
        }
    }
}
```

**Auth/permission-trigger pattern to mirror** — `MainActivity.kt` lines 103-113 (`permissionGranted` reactive flow) and lines 306-334 (`refreshPermissionState()`, `checkAndRequestPermission()` unconditional auto-trigger on first check — D-05 mirror):
```kotlin
// MainActivity.kt:103-113
private val permissionGranted = MutableStateFlow(false)

private val requestPermissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        refreshPermissionState()
        if (granted) { showReady() } else { showDenied() }
    }

// MainActivity.kt:306-326
private fun refreshPermissionState() {
    permissionGranted.value = ContextCompat.checkSelfPermission(
        this, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

private fun checkAndRequestPermission() {
    refreshPermissionState()
    val granted = permissionGranted.value
    if (granted) {
        showReady()
    } else {
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}
```
`CarContext` has no `ActivityResultContracts` launcher (Pitfall 4) — replace `requestPermissionLauncher.launch(...)` with `carContext.requestPermissions(listOf(...), executor, callback)` per RESEARCH.md Pattern 3. Reuse `ContextCompat.checkSelfPermission(carContext, ...)` verbatim — same call, same signature, just a different `Context` argument, no car-specific variant needed (RESEARCH.md "Don't Hand-Roll" table).

**Retry / permanent-denial branch pattern to mirror** — `MainActivity.kt` lines 328-334 (`onRetryClicked()`) and lines 336-341 (`openAppSettings()`):
```kotlin
private fun onRetryClicked() {
    if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    } else {
        openAppSettings()
    }
}

private fun openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
    }
    startActivity(intent)
}
```
`SpeedScreen` has no `Activity` to call `shouldShowRequestPermissionRationale()` on — replace the condition with `CarPermissionState.Denied(permanent).permanent`, sourced from `CarPermissionDenialStore`/`resolveCarPermissionState()` (see `CarPermissionState.kt` below). `openAppSettings()` becomes `carContext.startActivity(intent)` with `FLAG_ACTIVITY_NEW_TASK` added (required from a non-Activity `Context`) — see RESEARCH.md Pattern 3 `openAppSettingsFromCar()` for the exact adapted form.

**Error handling pattern (existing, line 39-42):** keep the same safe-cast convention already in the file —
```kotlin
private val provider = carContext.applicationContext as? TachimetroApplication
```
Never force-cast; a null provider must degrade gracefully (no crash), consistent with CLAUDE.md "Error Handling" (`?.let`, safe defaults over exceptions).

**`onGetTemplate()` extension point (existing, lines 79-109):** keep the existing `Speed`/`Searching` `Row` branches for the `Granted` case (calling `carSpeedContent(latestState)` unchanged, per RESEARCH.md's explicit note not to fold permission states into `CarSpeedContent`); add new branches for `NotRequested`/`Waiting` (single `Row`, no action) and `Denied` (`Row` + `Action` wrapped in `ParkedOnlyOnClickListener`, mirroring the `retryButton`/`resetMaxButton` two-state text switch already used on the phone at `MainActivity.kt:367-371`):
```kotlin
// MainActivity.kt:367-371 -- button label switches on the same permanent/non-permanent axis
retryButton.text = if (permanentlyDenied) {
    getString(R.string.open_settings)
} else {
    getString(R.string.retry)
}
```

---

### `app/src/main/java/com/sed/tachimetro/car/CarPermissionState.kt` (model + utility, transform)

**Analogs:** `app/src/main/java/com/sed/tachimetro/gps/SpeedState.kt` (sealed class shape) and `app/src/main/java/com/sed/tachimetro/maxspeed/MaxSpeedReducer.kt` (pure top-level resolver-function shape).

**Sealed class pattern to copy** — `SpeedState.kt` (full file, 27 lines):
```kotlin
package com.sed.tachimetro.gps

/** Sealed model of the GPS engine's exposed speed state (D-06). */
sealed class SpeedState {
    /** D-01: shown from startup until the first accepted fix arrives. */
    data object Searching : SpeedState()

    data class Reading(val kmh: Int, val deltaMeters: Float) : SpeedState()

    /** D-02: no accepted update has arrived for more than 5 seconds. */
    data object NoSignal : SpeedState()
}
```
Copy this exact shape: `data object` for singleton states, `data class` for states carrying a payload, one-line KDoc per case referencing the driving decision tag (D-XX).

**Pure resolver-function pattern to copy** — `MaxSpeedReducer.kt` (full file, 13 lines):
```kotlin
package com.sed.tachimetro.maxspeed

import kotlin.math.max

/** D-07: il massimo di sessione cresce solo verso la lettura più alta vista; ... */
fun reduceMax(currentMax: Int, reading: Int): Int {
    val safeCurrent = if (currentMax < 0) 0 else currentMax
    val safeReading = if (reading < 0) 0 else reading
    return max(safeCurrent, safeReading)
}

/** T-04-01: un valore persistito manomesso/negativo viene riportato a 0 alla lettura. */
fun sanitizePersistedMax(raw: Int): Int = if (raw < 0) 0 else raw
```
Copy this shape for `resolveCarPermissionState(granted: Boolean, denialCount: Int): CarPermissionState` — top-level, no class wrapper, no Android imports, primitives in / sealed model out, one-line KDoc citing the decision tag (D-04/D-05/D-06 per RESEARCH.md Pattern 2). Use an exhaustive `when` with no `else` branch, exactly like `carSpeedContent()` in `CarSpeedContent.kt:36-40` does for `SpeedState`.

**Error handling / sanitization pattern:** none needed here beyond what `resolveCarPermissionState()` itself expresses via its exhaustive `when` — no external I/O, no exceptions possible.

---

### `app/src/main/java/com/sed/tachimetro/car/CarPermissionDenialStore.kt` (store, CRUD)

**Analog:** `app/src/main/java/com/sed/tachimetro/maxspeed/MaxSpeedStore.kt` (full file, 25 lines) — exact structural match (single scalar in `SharedPreferences`, no Room/DataStore).

**Full pattern to copy:**
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
Deviation required by RESEARCH.md Pattern 1: `CarPermissionDenialStore` must NOT declare its own `PREFS_NAME` — it opens the same file via `MaxSpeedStore.PREFS_NAME` (import `com.sed.tachimetro.maxspeed.MaxSpeedStore`), so the whole app keeps a single `SharedPreferences` file, and exposes `denialCount(): Int` (read) + `recordDenial()` (increment-by-one write) instead of `read()`/`write(value)` — the shape is "counter", not "last value", so the write method takes no parameter and increments internally. `ScreenOnPreferenceStore.kt` (same directory family) is the reference for reusing `PREFS_NAME` across stores with a distinct `KEY_*` constant — see its own docstring: "stesso file 'tachimetro_prefs' di MaxSpeedStore ma con chiave distinta."

**Constructor/context pattern (WR-04 convention, applies identically):**
```kotlin
// MaxSpeedStore.kt:9-11
class MaxSpeedStore(context: Context) {
    // WR-04: il chiamante passa applicationContext, mai un'Activity.
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
```
`SpeedScreen` must construct `CarPermissionDenialStore(carContext.applicationContext)` — never `carContext` itself held past construction, and never a bare `CarContext` reference stored as a long-lived field beyond what `Screen` already retains internally (mirrors `provider = carContext.applicationContext as? TachimetroApplication` at `SpeedScreen.kt:43`).

---

### `app/src/test/java/com/sed/tachimetro/car/CarPermissionStateTest.kt` (test, transform)

**Analog:** `app/src/test/java/com/sed/tachimetro/maxspeed/MaxSpeedReducerTest.kt` (full file, 60 lines) — exact structural match for testing a pure resolver function.

**Pattern to copy:**
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
        // reduceMax(0, 50) == 50 -- first record: grows from 0.
        assertEquals(50, reduceMax(0, 50))
    }

    @Test
    fun lowerReading_doesNotLowerMax() {
        // reduceMax(120, 80) == 120 -- D-07: a lower reading must not lower the max.
        assertEquals(120, reduceMax(120, 80))
    }
    // ... one @Test per boundary case, each with an inline comment stating the exact
    // call/expected-value pair before the assertion.
}
```
Copy exactly: package-matches-source-under-test, `org.junit.Assert.assertEquals` + `org.junit.Test` only (no mocking framework, no Robolectric — these are plain JVM tests), one `@Test` per branch of the `when` in `resolveCarPermissionState()` (granted regardless of count; not-granted+count=0; not-granted+count=1; not-granted+count=2; not-granted+count>2 if distinguishable), inline comment above each `assertEquals` stating the literal call and expected value (see `CarPermissionStateTest` example already drafted in RESEARCH.md "Pattern 2", which follows this exact shape).

---

### `app/src/main/res/values/strings.xml` (config/resource)

**Analog:** itself — the existing `car_searching_gps_signal` entry (line 10) is the established precedent for "shorter, car-specific copy" (Phase 8 D-02), directly reused for this phase's D-01/D-02/D-04 strings.

**Existing pattern to copy (lines 4-10):**
```xml
<string name="permission_denied">Permesso GPS necessario per funzionare</string>
<string name="permission_denied_permanent">Permesso GPS negato. Aprire le impostazioni per abilitarlo</string>
<string name="retry">Riprova</string>
<string name="open_settings">Apri impostazioni</string>
<string name="searching_gps_signal">Ricerca segnale GPS...</string>
<!-- D-02: copy dedicata allo schermo auto, distinta e piu' corta di searching_gps_signal -->
<string name="car_searching_gps_signal">Ricerca segnale...</string>
```
Add new entries immediately after `car_searching_gps_signal`, following the same "inline XML comment citing the decision tag" convention:
```xml
<!-- D-01: testo esatto bloccato, distinto da car_searching_gps_signal (attesa dialogo permesso vs attesa fix GPS) -->
<string name="car_check_your_phone">Controlla il telefono</string>
<!-- D-02: copy dedicata allo schermo auto, piu' corta di permission_denied -->
<string name="car_permission_denied">[Claude's Discretion -- tone/content locked, exact wording free]</string>
<!-- D-04: rifiuto permanente -- indirizza alle impostazioni sul telefono -->
<string name="car_permission_denied_permanent">[Claude's Discretion -- tone/content locked, exact wording free]</string>
```
`R.string.retry` and `R.string.open_settings` are REUSED as-is for the retry/settings `Action` titles (RESEARCH.md: "already short, generic 1-2 word labels appropriate for the car screen as-is — no car-specific variant needed"). Do not create `car_retry`/`car_open_settings` duplicates.

---

## Shared Patterns

### Safe-cast / no-Activity-retention convention
**Source:** `app/src/main/java/com/sed/tachimetro/car/SpeedScreen.kt:39-43` and CLAUDE.md "Anti-Patterns" §"Retaining Activity Reference in Long-Lived Components"
**Apply to:** `SpeedScreen.kt` (already applies), `CarPermissionDenialStore.kt` (constructor must take `Context`, called with `carContext.applicationContext`).
```kotlin
// Cast SAFE (as?, non forzato): convenzione "Error Handling" di CLAUDE.md, preferire un
// default sicuro a un'eccezione.
private val provider = carContext.applicationContext as? TachimetroApplication
```

### Reactive permission-state flow, re-evaluated on every `STARTED` entry
**Source:** `app/src/main/java/com/sed/tachimetro/MainActivity.kt:217-230` (`setupGpsCollection()`, `permissionGranted.collectLatest`) and the existing `SpeedScreen.kt:54-76` `repeatOnLifecycle(STARTED)` block.
**Apply to:** `SpeedScreen.kt` — the permission check + `resolveCarPermissionState()` call must live inside the same `repeatOnLifecycle(Lifecycle.State.STARTED)` block already collecting GPS state, not a second parallel mechanism (CONTEXT.md "Established Patterns").
```kotlin
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        permissionGranted.collectLatest { granted ->
            if (granted) {
                gpsSpeedProvider.state.collect { state -> updatePlaceholder(state) }
            }
        }
    }
}
```

### SharedPreferences store convention (single file, per-feature key)
**Source:** `app/src/main/java/com/sed/tachimetro/maxspeed/MaxSpeedStore.kt`, `app/src/main/java/com/sed/tachimetro/screen/ScreenOnPreferenceStore.kt`, `app/src/main/java/com/sed/tachimetro/distance/DistanceStore.kt` — all three share `PREFS_NAME = "tachimetro_prefs"` with a distinct `KEY_*` constant, no Room/DataStore.
**Apply to:** `CarPermissionDenialStore.kt` — reuse `MaxSpeedStore.PREFS_NAME` directly (import, don't redeclare), add `KEY_DENIAL_COUNT = "car_location_denial_count"`.

### Pure top-level resolver/reducer function convention
**Source:** `app/src/main/java/com/sed/tachimetro/maxspeed/MaxSpeedReducer.kt` (`reduceMax`, `sanitizePersistedMax`), `app/src/main/java/com/sed/tachimetro/car/CarSpeedContent.kt:36-40` (`carSpeedContent`), `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt` (`deriveSpeedState`, per CONTEXT.md reference).
**Apply to:** `CarPermissionState.kt` (`resolveCarPermissionState`) — framework-free, exhaustive `when`, no `else`, primitives/sealed-model I/O only, paired with a `*Test.kt` mirroring `MaxSpeedReducerTest.kt`.

### Sealed-class state model convention
**Source:** `app/src/main/java/com/sed/tachimetro/gps/SpeedState.kt`, `app/src/main/java/com/sed/tachimetro/car/CarSpeedContent.kt:12-18`.
**Apply to:** `CarPermissionState.kt` — `data object` for payload-less states (`Granted`, `NotRequested`, `Waiting`), `data class` for the one state carrying data (`Denied(val permanent: Boolean)`), one-line KDoc per case citing D-XX.

### Two-state action label switch (retry vs. open settings)
**Source:** `app/src/main/java/com/sed/tachimetro/MainActivity.kt:367-371`.
**Apply to:** `SpeedScreen.kt`'s `onGetTemplate()` `Denied` branch — same `if (permanent) R.string.open_settings else R.string.retry` shape, reused verbatim (no car-specific string needed for these two labels, per RESEARCH.md).

## No Analog Found

None. All 5 files have a strong (exact or role-match) analog in the existing codebase; no file requires falling back to RESEARCH.md's illustrative code alone.

## Metadata

**Analog search scope:** `app/src/main/java/com/sed/tachimetro/` (all packages: `car/`, `maxspeed/`, `screen/`, `distance/`, `gps/`), `app/src/main/java/com/sed/tachimetro/MainActivity.kt`, `app/src/test/java/com/sed/tachimetro/` (all test packages), `app/src/main/res/values/strings.xml`.
**Files scanned:** 17 main-source `.kt` files, 7 test `.kt` files, 1 `strings.xml` (full directory listing via Glob; targeted `Read`/`Grep` on 10 of them where a permission/store/sealed-state/test pattern was plausible).
**Pattern extraction date:** 2026-09-02
