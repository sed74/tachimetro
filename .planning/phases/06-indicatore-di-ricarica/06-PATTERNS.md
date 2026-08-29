# Phase 6: Indicatore di Ricarica - Pattern Map

**Mapped:** 2026-08-29
**Files analyzed:** 9 (2 new Kotlin, 1 new test, 2 new drawables, 1 modified layout, 2 modified resource files, 1 modified Activity)
**Analogs found:** 7 / 9 (2 have no analog — see "No Analog Found")

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `app/src/main/java/com/sed/tachimetro/MainActivity.kt` (modify) | controller | event-driven | itself — existing `permissionGranted`/`gpsSpeedProvider` wiring + `applyScreenSwitchWindowInsets()` | exact (self-extend) |
| `app/src/main/java/com/sed/tachimetro/charging/ChargingStateProvider.kt` (new) | provider | event-driven | `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt` | exact |
| `app/src/main/java/com/sed/tachimetro/charging/ChargingState.kt` (new) | model | event-driven | `app/src/main/java/com/sed/tachimetro/gps/SpeedState.kt` | exact |
| `app/src/test/java/com/sed/tachimetro/charging/ChargingStateProviderStateTest.kt` (new) | test | transform | `app/src/test/java/com/sed/tachimetro/gps/GpsSpeedProviderStateTest.kt` | exact |
| `app/src/main/res/layout/activity_main.xml` (modify) | component | request-response (declarative UI) | itself — existing `keepScreenOnSwitch`/`maxSpeedText` blocks | exact (self-extend) |
| `app/src/main/res/values/colors.xml` (modify) | config | n/a | itself | exact (self-extend) |
| `app/src/main/res/values/strings.xml` (modify, optional) | config | n/a | itself | exact (self-extend) |
| `app/src/main/res/drawable/ic_charging_flash.xml` (new) | config/asset | n/a | `app/src/main/res/drawable/ic_launcher_foreground.xml` (only existing vector drawable) | role-match (XML format only, not content) |
| `app/src/main/res/drawable/*_fill.xml` (new layer-list/clip drawable, name TBD by planner) | config/asset | n/a | none | no analog |

## Pattern Assignments

### `app/src/main/java/com/sed/tachimetro/MainActivity.kt` (controller, event-driven, modify)

**Analog:** itself (this phase extends existing wiring, not a different file)

**Imports pattern** (lines 1-39) — add to this existing block, same grouping style (android.* first, then androidx.*, then kotlinx.coroutines.*, then com.sed.tachimetro.*):
```kotlin
import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
...
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import com.sed.tachimetro.gps.GpsSpeedProvider
import com.sed.tachimetro.gps.SpeedState
import com.sed.tachimetro.maxspeed.MaxSpeedStore
import com.sed.tachimetro.maxspeed.reduceMax
import com.sed.tachimetro.screen.ScreenOnPreferenceStore
```
The new `ChargingStateProvider`/`ChargingState` imports belong in this same block, alphabetically alongside `com.sed.tachimetro.gps.*`.

**View declaration pattern** (lines 57-66):
```kotlin
private lateinit var messageText: TextView
private lateinit var unitText: TextView
private lateinit var retryButton: Button
private lateinit var maxSpeedText: TextView
private lateinit var resetMaxButton: Button
private lateinit var maxSpeedStore: MaxSpeedStore
private var currentMax: Int = 0
private lateinit var keepScreenOnSwitch: SwitchCompat
private lateinit var screenOnStore: ScreenOnPreferenceStore
private lateinit var gpsSpeedProvider: GpsSpeedProvider
```
Add `private lateinit var chargingIcon: ImageView` and `private lateinit var chargingStateProvider: ChargingStateProvider` here, same style.

**Reactive StateFlow consumption pattern** (lines 130-142) — the binding template for the new charging observer:
```kotlin
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        // CR-01: collectLatest on the reactive permissionGranted flow instead of a
        // one-shot check -- a grant that arrives without a STOP/START cycle (e.g. the
        // system permission dialog only triggering onPause()/onResume()) now restarts
        // gpsSpeedProvider.state.collect() as soon as refreshPermissionState() fires.
        permissionGranted.collectLatest { granted ->
            if (granted) {
                gpsSpeedProvider.state.collect { state -> updatePlaceholder(state) }
            }
        }
    }
}
```
Copy the `repeatOnLifecycle(Lifecycle.State.STARTED) { chargingStateProvider.state.collect { state -> updateChargingIcon(state) } }` shape directly — charging observation needs no permission gate (unlike GPS), so it can be a plain `collect` in its own `repeatOnLifecycle(STARTED)` block (or added as a second `launch {}` alongside the existing one in `onCreate()`), started right after `gpsSpeedProvider = GpsSpeedProvider(applicationContext)` is instantiated (~line 127).

**`onCreate()` initialization block pattern for a lateinit view + store pairing** (lines 105-123, the `keepScreenOnSwitch` setup):
```kotlin
screenOnStore = ScreenOnPreferenceStore(applicationContext)
keepScreenOnSwitch = findViewById(R.id.keepScreenOnSwitch)
// ... derive default, set state BEFORE listener (no flash) ...
keepScreenOnSwitch.isChecked = keepOn
applyKeepScreenOn(keepOn)
keepScreenOnSwitch.setOnCheckedChangeListener { _, isChecked ->
    applyKeepScreenOn(isChecked)
    screenOnStore.write(isChecked)
}
applyScreenSwitchWindowInsets()
```
Mirror this shape for `chargingIcon`: `findViewById`, instantiate `ChargingStateProvider(applicationContext)` (WR-04 — application context, not Activity), then wire the window-insets function (extended, see below).

**`onDestroy()` teardown pattern** (lines 179-187):
```kotlin
override fun onDestroy() {
    // WR-04: tear down gpsSpeedProvider's own CoroutineScope for symmetry/defensiveness
    // when this Activity instance is going away for good (e.g. a configuration change
    // recreates it with a fresh GpsSpeedProvider). D-07's repeatOnLifecycle(STARTED)
    // already stops collection on stop, so this is a secondary safety net, not the
    // primary stop/start mechanism.
    gpsSpeedProvider.close()
    super.onDestroy()
}
```
Add `chargingStateProvider.close()` alongside `gpsSpeedProvider.close()` here — same symmetry rationale (the new provider will own a `CoroutineScope` per the `GpsSpeedProvider` analog below).

**Plain visibility toggle (no animation) pattern** (lines 293-304, `updateMaxArea()`) — directly reusable shape for the Hidden/Visible instant toggle (UI-SPEC "Transitions": `View.VISIBLE` ↔ `View.GONE`, no fade):
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
The new `updateChargingIcon(state: ChargingState)` should follow this exact if/else visibility-toggle shape: `ChargingState.Hidden -> chargingIcon.visibility = View.GONE` (also stop/cancel the fill animator here); `ChargingState.Pulsing -> chargingIcon.visibility = View.VISIBLE` + (re)start the loop animator; `ChargingState.Full -> chargingIcon.visibility = View.VISIBLE` + cancel animator and freeze the drawable at 100% level.

**One-shot sticky-broadcast read pattern — do NOT reuse as-is, but shows the underlying API** (lines 421-429):
```kotlin
// D-04: legge lo stato di ricarica corrente dal broadcast sticky ACTION_BATTERY_CHANGED
// (registerReceiver(null, ...) restituisce subito l'ultimo Intent sticky, nessun receiver da
// deregistrare). Usato SOLO al primo avvio per derivare il default dello switch.
private fun isDeviceCharging(): Boolean {
    val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    return status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
}
```
Per CONTEXT.md D-01, this function stays exactly as-is (still used only for the one-shot screen-on default at line 111) and must NOT be repurposed for the continuous charging indicator — the new `ChargingStateProvider` needs its own **registered** (not `null`-registered) `BroadcastReceiver`/`callbackFlow`, see next section.

**Window insets pattern to extend** (lines 431-450, `applyScreenSwitchWindowInsets()`):
```kotlin
// Specchio di applyMaxAreaWindowInsets() per l'angolo bottom-left: somma l'inset live
// systemBars/displayCutout bottom+left sui margini base XML, così lo switch non finisce mai
// dietro la navigation bar o un cutout inferiore/sinistro, in entrambi gli orientamenti.
// Listener dedicato: gli insets differiscono per angolo (non riusare quello di maxSpeedText).
private fun applyScreenSwitchWindowInsets() {
    val baseParams = keepScreenOnSwitch.layoutParams as ConstraintLayout.LayoutParams
    val baseBottom = baseParams.bottomMargin
    val baseStart = baseParams.marginStart
    ViewCompat.setOnApplyWindowInsetsListener(keepScreenOnSwitch) { view, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
        val extraBottom = maxOf(systemBars.bottom, cutout.bottom)
        val extraStart = maxOf(systemBars.left, cutout.left)
        val lp = view.layoutParams as ConstraintLayout.LayoutParams
        lp.bottomMargin = baseBottom + extraBottom
        lp.marginStart = baseStart + extraStart
        view.layoutParams = lp
        insets
    }
}
```
Per UI-SPEC "Window insets (critical)": `chargingIcon` becomes the new leftmost bottom-left element (taking the role `keepScreenOnSwitch` currently plays — start+bottom inset), while `keepScreenOnSwitch` now chains its start constraint from `chargingIcon` (no longer from `parent`) so it only needs the inset applied once, on the icon. Rename/extend this function to apply `bottomMargin += extraBottom` and `marginStart += extraStart` to `chargingIcon`'s `LayoutParams` instead of (or in addition to) the switch's — mirror the exact `maxOf(systemBars.X, cutout.X)` computation shown here, do not invent a new inset-merging strategy.

---

### `app/src/main/java/com/sed/tachimetro/charging/ChargingStateProvider.kt` (provider, event-driven, new)

**Analog:** `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt`

**Imports pattern** (lines 1-29) — same three-group style (android.*, google-play-services or android.content for the receiver, kotlinx.coroutines.*):
```kotlin
package com.sed.tachimetro.gps

import android.content.Context
import android.location.Location
import android.os.Looper
import android.os.SystemClock

import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
```
For the new file, replace the Play Services location imports with `android.content.BroadcastReceiver`, `android.content.Intent`, `android.content.IntentFilter`, `android.os.BatteryManager`; keep the same coroutines import group (`callbackFlow`, `awaitClose`, `StateFlow`, `stateIn`, `SharingStarted`).

**WR-04 application-context constructor pattern + class doc pattern** (lines 31-45):
```kotlin
/**
 * Wraps continuous FusedLocationProviderClient updates in a [callbackFlow] and exposes a
 * [StateFlow] of [SpeedState], applying the tested [mapSpeedToKmh] filters and detecting
 * startup/loss "no signal" per D-01/D-02.
 *
 * Permission note: this class does NOT check ACCESS_FINE_LOCATION itself. MainActivity
 * (Phase 1) is the single source of truth for that permission and only starts collecting
 * [state] once it has been granted — see RESEARCH.md Pitfall 1 / Anti-Patterns.
 */
class GpsSpeedProvider(context: Context) {

    // WR-04: FusedLocationProviderClient doesn't need an Activity context; use
    // applicationContext defensively so this provider never retains/leaks an Activity,
    // regardless of what the caller passes in.
    private val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)
```
`ChargingStateProvider(context: Context)` must follow the identical WR-04 shape: store `context.applicationContext` (or accept only `Context` and immediately call `.applicationContext` when registering the receiver), never the raw Activity.

**`callbackFlow` wrapping a callback/receiver-based Android API** (lines 61-75) — this is the core pattern to copy for wrapping `BroadcastReceiver` registration instead of `LocationCallback`:
```kotlin
@Volatile
private var lastAcceptedUpdateAtMs: Long = 0L

// MainActivity guarantees ACCESS_FINE_LOCATION is granted before `state` is ever
// collected (permission check lives solely there — see class doc above).
@Suppress("MissingPermission")
private val rawLocations: Flow<Location> = callbackFlow {
    val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { trySend(it) }
        }
    }
    client.requestLocationUpdates(request, callback, Looper.getMainLooper())
    awaitClose { client.removeLocationUpdates(callback) }
}
```
Copy this exact shape for a `rawBatteryStatus: Flow<Int>` built with `callbackFlow { val receiver = object : BroadcastReceiver() { override fun onReceive(...) { trySend(intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) } }; context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED)); awaitClose { context.unregisterReceiver(receiver) } }` — note `ACTION_BATTERY_CHANGED` is itself sticky and fires immediately on registration with the current status, so no separate "read current value first" step is needed (unlike GPS, which has no initial value until the first fix — see `onStart { emit(null) }` below, which the charging provider does NOT need).

**`StateFlow` exposure + lifecycle-scoped sharing pattern** (lines 57-59, 105-124):
```kotlin
// Owns the StateFlow sharing; scoped to this provider's own lifetime (mirrors the
// Activity that owns it — no ViewModel/DI layer in this project).
private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

...

val state: StateFlow<SpeedState> = combine(
    acceptedKmh.map { it as Int? }.onStart { emit(null) }, // null until first fix (D-01)
    ticker,
) { lastKmh, now ->
    deriveSpeedState(lastKmh, now, lastAcceptedUpdateAtMs)
}.stateIn(
    scope = scope,
    started = SharingStarted.WhileSubscribed(),
    initialValue = SpeedState.Searching,
)

// WR-04: explicit teardown for the provider's own scope, for symmetry/defensiveness.
// D-07 already has repeatOnLifecycle(STARTED) stop collecting `state` on
// onStop()/activity destroy, which (via WhileSubscribed()) stops the upstream location
// updates; this additionally cancels the SupervisorJob scope itself. Call from
// MainActivity.onDestroy() when the Activity (and this provider instance) is being torn
// down for good, e.g. on a configuration change that recreates the Activity.
fun close() {
    scope.cancel()
}
```
`ChargingStateProvider` is simpler than `GpsSpeedProvider` — no `combine()`/ticker/staleness needed (there is no "no signal" concept for a sticky broadcast; the value is always current). Use `val state: StateFlow<ChargingState> = rawBatteryStatus.map { status -> deriveChargingState(status) }.stateIn(scope, SharingStarted.WhileSubscribed(), initialValue = ChargingState.Hidden)`. Keep the same `private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)` field and the same `fun close() { scope.cancel() }`, called from `MainActivity.onDestroy()`.

**Pure, testable state-derivation function pattern** (lines 127-139):
```kotlin
/**
 * WR-02: pure, unit-testable extraction of the [GpsSpeedProvider.state] decision logic
 * (D-01/D-02), independent of Flow/`combine`/coroutines machinery.
 *
 * @param lastKmh the latest accepted km/h reading, or `null` if no fix has been accepted yet.
 * @param now the current monotonic timestamp ([android.os.SystemClock.elapsedRealtime]).
 * @param lastAcceptedAtMs the monotonic timestamp of the last accepted reading.
 */
fun deriveSpeedState(lastKmh: Int?, now: Long, lastAcceptedAtMs: Long): SpeedState = when {
    lastKmh == null -> SpeedState.Searching // D-01: no accepted fix yet
    now - lastAcceptedAtMs > 5000L -> SpeedState.NoSignal // D-02
    else -> SpeedState.Reading(lastKmh)
}
```
Write a top-level `fun deriveChargingState(batteryStatus: Int): ChargingState = when (batteryStatus) { BatteryManager.BATTERY_STATUS_CHARGING -> ChargingState.Pulsing; BatteryManager.BATTERY_STATUS_FULL -> ChargingState.Full; else -> ChargingState.Hidden }` as a top-level pure function outside the class body (D-01/D-03 state machine), exactly mirroring this placement and doc style so it is independently unit-testable without Android runtime/coroutines-test (see test analog below).

---

### `app/src/main/java/com/sed/tachimetro/charging/ChargingState.kt` (model, event-driven, new)

**Analog:** `app/src/main/java/com/sed/tachimetro/gps/SpeedState.kt` (full file, 13 lines — copy this shape wholesale):
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
`ChargingState` needs three `data object` variants (no payload needed, unlike `Reading(kmh)`): `Hidden` (D-01, not connected), `Pulsing` (D-02, `BATTERY_STATUS_CHARGING`), `Full` (D-03, `BATTERY_STATUS_FULL` — icon frozen solid lime). Package should be `com.sed.tachimetro.charging` per the project's domain-split convention (`gps`, `maxspeed`, `screen` are the existing precedents).

---

### `app/src/test/java/com/sed/tachimetro/charging/ChargingStateProviderStateTest.kt` (test, transform, new)

**Analog:** `app/src/test/java/com/sed/tachimetro/gps/GpsSpeedProviderStateTest.kt` (full file):
```kotlin
package com.sed.tachimetro.gps

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Plain JVM unit tests for [deriveSpeedState] (WR-02) — no Android runtime, no
 * coroutines-test. Locks the state-machine decisions D-01/D-02 that
 * [GpsSpeedProvider.state]'s `combine(...)` block delegates to.
 */
class GpsSpeedProviderStateTest {

    @Test
    fun noAcceptedFixYet_returnsSearching() {
        // D-01: lastKmh == null means no accepted fix has arrived yet, regardless of clocks.
        val result = deriveSpeedState(lastKmh = null, now = 10_000L, lastAcceptedAtMs = 0L)
        assertEquals(SpeedState.Searching, result)
    }
    ...
}
```
Note: `BatteryManager` constants (`BATTERY_STATUS_CHARGING`, `BATTERY_STATUS_FULL`, etc.) are plain `Int` constants from the Android SDK stub jar, resolvable in a plain JVM unit test without Robolectric (same as this file needs zero Android runtime). Test method naming follows the observed convention: `[condition]_returns[Outcome]` in `camelCase`/snake_case-per-word, e.g. `notConnected_returnsHidden()`, `charging_returnsPulsing()`, `full_returnsFull()`, `unknownStatus_returnsHidden()`.

---

### `app/src/main/res/layout/activity_main.xml` (component, declarative UI, modify)

**Analog:** itself — existing `keepScreenOnSwitch` block (lines 89-105):
```xml
<androidx.appcompat.widget.SwitchCompat
    android:id="@+id/keepScreenOnSwitch"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_marginStart="16dp"
    android:layout_marginBottom="16dp"
    android:minHeight="48dp"
    android:maxLines="1"
    android:singleLine="true"
    android:text="@string/keep_screen_on_label"
    android:textColor="@android:color/white"
    android:textSize="16sp"
    app:thumbTint="@color/switch_thumb_tint"
    app:trackTint="@color/switch_track_tint"
    app:layout_constraintBottom_toBottomOf="parent"
    app:layout_constraintStart_toStartOf="parent"
    tools:text="Sempre acceso" />
```
Per UI-SPEC "Layout / positioning": add `chargingIcon` (`ImageView`) BEFORE this element in the XML, then change `keepScreenOnSwitch`'s `app:layout_constraintStart_toStartOf="parent"` to `app:layout_constraintStart_toEndOf="@id/chargingIcon"` with a new `android:layout_marginStart="8dp"`. New `ImageView`:
```xml
<ImageView
    android:id="@+id/chargingIcon"
    android:layout_width="24dp"
    android:layout_height="24dp"
    android:layout_marginStart="16dp"
    android:layout_marginBottom="16dp"
    android:src="@drawable/ic_charging_flash.xml"
    android:clickable="false"
    android:focusable="false"
    android:visibility="gone"
    app:layout_constraintBottom_toBottomOf="@id/keepScreenOnSwitch"
    app:layout_constraintTop_toTopOf="@id/keepScreenOnSwitch"
    app:layout_constraintStart_toStartOf="parent"
    tools:visibility="visible" />
```
(exact `src`/drawable name is decided by whichever animated-fill resource the executor authors — see "No Analog Found" below.) The `android:visibility="gone"` default + `tools:visibility="visible"` preview-only override directly mirrors the existing `unitText` (lines 33-45), `maxSpeedText` (60-74), and `resetMaxButton` (76-87) blocks in this same file — same convention, reuse it verbatim.

**Reference: `maxLines`/`singleLine` + `minHeight="48dp"` touch-target convention** (lines 60-87) — NOT applicable to `chargingIcon` per UI-SPEC (icon is non-interactive, no 48dp minimum required), but shown here so the planner does not mistakenly copy the touch-target sizing onto the new `ImageView`.

---

### `app/src/main/res/values/colors.xml` (config, modify)

**Analog:** itself (full file, 9 lines):
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="purple_200">#FFBB86FC</color>
    <color name="purple_500">#FF6200EE</color>
    <color name="purple_700">#FF3700B3</color>
    <color name="teal_200">#FF03DAC5</color>
    <color name="teal_700">#FF018786</color>
    <color name="black">#FF000000</color>
    <color name="white">#FFFFFFFF</color>
</resources>
```
Add `<color name="lime_charging_accent">#FFAEEA00</color>` before `</resources>`, same flat `<color name="...">#AARRGGBB</color>` format, 8-digit ARGB hex with explicit alpha (matching `black`/`white`'s `#FF...` prefix, not a bare 6-digit hex).

---

### `app/src/main/res/values/strings.xml` (config, modify, optional)

**Analog:** itself (full file, 13 lines) — flat `<string name="...">...</string>` list, one line per entry, no comments/grouping:
```xml
<resources>
    <string name="app_name">Tachimetro</string>
    ...
    <string name="keep_screen_on_label">Sempre acceso</string>
</resources>
```
If the optional content-description is adopted (UI-SPEC Copywriting Contract): add `<string name="charging_indicator_description">In carica</string>` in the same flat style, no new `<string-array>` or grouping needed.

---

### `app/src/main/res/drawable/ic_charging_flash.xml` (config/asset, new)

**Analog (format only, not content):** `app/src/main/res/drawable/ic_launcher_foreground.xml` — the only existing vector drawable in the project, shows the project's baseline `<vector>` XML shape (viewport, `pathData`, `fillColor`):
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#FFFFFF"
        android:fillType="nonZero"
        android:pathData="M65.3,45.828l3.8,-6.6c0.2,-0.4 ..." />
</vector>
```
This launcher icon is NOT a content analog (it's the Android-robot mascot, not a flash/bolt glyph) — it is only useful for the `<vector>` element attribute shape (`android:width`/`height`/`viewportWidth`/`viewportHeight`/`android:fillColor`). Per UI-SPEC, do not hand-author path data: generate `ic_charging_flash.xml` via Android Studio's Vector Asset Studio (New → Vector Asset → Clip Art → "flash on"/"bolt", Material filled style), 24×24dp viewport, per D-05.

---

## Shared Patterns

### Reactive StateFlow + `repeatOnLifecycle(STARTED)` consumption
**Source:** `MainActivity.kt:130-142` (GPS/permission wiring)
**Apply to:** `ChargingStateProvider.state` consumption in `MainActivity.onCreate()` — same `lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { chargingStateProvider.state.collect { ... } } }` shape. No permission gate needed (unlike GPS's `permissionGranted.collectLatest` wrapper) since `ACTION_BATTERY_CHANGED` requires no runtime permission.

### WR-04 — application-context-only for long-lived components
**Source:** `MainActivity.kt:125-127` comment, `GpsSpeedProvider.kt:42-45`
**Apply to:** `ChargingStateProvider(context: Context)` constructor — must store/use `context.applicationContext`, never the Activity, and be instantiated in `MainActivity.onCreate()` with `ChargingStateProvider(applicationContext)`.

### `callbackFlow` wrapping a callback/broadcast-based Android API
**Source:** `GpsSpeedProvider.kt:67-75`
**Apply to:** `ChargingStateProvider`'s internal `rawBatteryStatus` flow — wrap `BroadcastReceiver` registration/`unregisterReceiver` in `awaitClose {}`, exactly as `requestLocationUpdates`/`removeLocationUpdates` are wrapped.

### Pure, testable state-derivation function
**Source:** `GpsSpeedProvider.kt:135-139` (`deriveSpeedState`), tested by `GpsSpeedProviderStateTest.kt`
**Apply to:** `deriveChargingState(batteryStatus: Int): ChargingState` — top-level function outside the provider class, no Android runtime dependency beyond the `BatteryManager` `Int` constants, unit-tested the same way.

### Symmetric `close()` / `onDestroy()` teardown
**Source:** `GpsSpeedProvider.kt:122-124`, `MainActivity.kt:179-187`
**Apply to:** `ChargingStateProvider.close() { scope.cancel() }`, called from `MainActivity.onDestroy()` alongside the existing `gpsSpeedProvider.close()`.

### Plain visibility toggle, no fade (instant `GONE`↔`VISIBLE`)
**Source:** `MainActivity.kt:293-304` (`updateMaxArea()`), `activity_main.xml` `unitText`/`maxSpeedText`/`resetMaxButton` (`android:visibility="gone"` + `tools:visibility="visible"`)
**Apply to:** `chargingIcon`'s Hidden↔Charging/Full transitions (UI-SPEC "Transitions": instant toggle, no animation on appear/disappear — the ONLY animation permitted anywhere is the internal fill loop while `VISIBLE`).

### Window insets per screen corner, dedicated listener per element
**Source:** `MainActivity.kt:366-381` (`applyUnitTextWindowInsets`, top-right), `MainActivity.kt:388-408` (`applyMaxAreaWindowInsets`, top-left, chains a second view's margin off the first), `MainActivity.kt:435-450` (`applyScreenSwitchWindowInsets`, bottom-left)
**Apply to:** the bottom-left inset must move from `keepScreenOnSwitch` onto `chargingIcon` (the new leftmost element), using the exact `maxOf(systemBars.X, cutout.X)` merge shown in all three existing listeners — do not invent a new inset-merging strategy. `applyMaxAreaWindowInsets()` is the best template for "one listener updates two views' margins" (label + button) if `keepScreenOnSwitch`'s own margin also needs adjusting inside the same listener.

### Data classification of colors: `#AARRGGBB` hex, one flat list
**Source:** `colors.xml` (whole file)
**Apply to:** `lime_charging_accent` entry — 8-digit hex with explicit alpha, no grouping/comments needed.

## No Analog Found

Files/resources with no close match in the codebase (planner should follow UI-SPEC's recommended approach directly, since RESEARCH.md was skipped for this phase):

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| `app/src/main/res/drawable/*_fill.xml` (layer-list + `<clip>` combining a white base copy and a lime copy of `ic_charging_flash.xml`) | config/asset | n/a | No `<layer-list>`, `<clip>`, or any multi-layer/animatable drawable exists anywhere in the project — this is the first drawable beyond flat vectors and launcher assets. Follow UI-SPEC "Animation spec" verbatim: `ClipDrawable` (`android:clipOrientation="vertical"`, `android:gravity="bottom"`) wrapping a lime copy, layered over a white base copy via `<layer-list>`. |
| `ValueAnimator`-driven fill-level code (likely a small private helper in `MainActivity.kt`, e.g. `startChargingAnimation()`/`stopChargingAnimation()`) | utility | event-driven | No animation-driving code (`ValueAnimator`, `ObjectAnimator`, `AnimatedVectorDrawable`) exists anywhere in the project — CONTEXT.md explicitly confirms this is the first animation exception in the codebase (D-04, PROJECT.md Constraints). Follow UI-SPEC's non-binding recommendation: single `ValueAnimator` with `setIntValues(0, 10_000)`, `repeatMode = ValueAnimator.REVERSE`, `repeatCount = ValueAnimator.INFINITE`, `duration = 2500`, `interpolator = AccelerateDecelerateInterpolator()`, updating the `ClipDrawable`'s `level` on each `onAnimationUpdate`. Must be cancelled (not paused) on Hidden/Full transitions per UI-SPEC "Loop" row. |

## Metadata

**Analog search scope:** `app/src/main/java/com/sed/tachimetro/**` (all Kotlin source), `app/src/test/java/com/sed/tachimetro/**` (all unit tests), `app/src/main/res/layout/**`, `app/src/main/res/values/**`, `app/src/main/res/drawable/**`, `app/src/main/res/color/**`
**Files scanned:** MainActivity.kt, GpsSpeedProvider.kt, SpeedState.kt, SpeedMapping.kt (referenced not read — out of scope), MaxSpeedReducer.kt/MaxSpeedStore.kt (referenced not read — no persistence needed this phase), ScreenOnPreferenceStore.kt, GpsSpeedProviderStateTest.kt, activity_main.xml, colors.xml, strings.xml, switch_thumb_tint.xml, switch_track_tint.xml, ic_launcher_foreground.xml
**Pattern extraction date:** 2026-08-29
