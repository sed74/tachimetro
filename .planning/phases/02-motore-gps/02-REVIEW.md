---
phase: 02-motore-gps
reviewed: 2026-07-07T00:00:00Z
depth: standard
files_reviewed: 9
files_reviewed_list:
  - app/build.gradle.kts
  - app/src/main/AndroidManifest.xml
  - app/src/main/java/com/sed/tachimetro/MainActivity.kt
  - app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt
  - app/src/main/java/com/sed/tachimetro/gps/SpeedMapping.kt
  - app/src/main/java/com/sed/tachimetro/gps/SpeedState.kt
  - app/src/main/res/values/strings.xml
  - app/src/test/java/com/sed/tachimetro/gps/SpeedMappingTest.kt
  - gradle/libs.versions.toml
findings:
  critical: 1
  warning: 4
  info: 2
  total: 7
status: issues_found
---

# Phase 02-motore-gps: Code Review Report

**Reviewed:** 2026-07-07T00:00:00Z
**Depth:** standard
**Files Reviewed:** 9
**Status:** issues_found

## Summary

Reviewed the GPS speed engine (`GpsSpeedProvider`, `SpeedMapping`, `SpeedState`) and its integration into `MainActivity`, plus the manifest, build config, version catalog, strings, and the unit test for the pure mapping function.

`mapSpeedToKmh` is well-isolated, well-documented, and correctly tested against its own design decisions (D-03/D-04/D-05/D-09). The bigger risk in this phase is in the *wiring*: `MainActivity`'s `repeatOnLifecycle` block only re-checks the location permission once per `STARTED` lifecycle entry, which creates a real path where the app never starts collecting GPS state after the user grants the permission for the first time. There is also no test coverage at all for the actual async state machine in `GpsSpeedProvider` (the ticker/`combine`/staleness logic) — only the pure conversion function is tested. A few smaller robustness and consistency issues round out the findings below.

## Critical Issues

### CR-01: GPS state collection may never start after first-time permission grant

**File:** `app/src/main/java/com/sed/tachimetro/MainActivity.kt:51-62`
**Issue:**
```kotlin
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        if (ContextCompat.checkSelfPermission(...) == PackageManager.PERMISSION_GRANTED) {
            gpsSpeedProvider.state.collect { state -> updatePlaceholder(state) }
        }
    }
}
```
`repeatOnLifecycle(STARTED)` re-runs its block only when the lifecycle transitions from below `STARTED` back up to `STARTED` (e.g. a `STOP` → `START` cycle). The permission check happens exactly once per such cycle: if permission is **not** granted at that moment, the `if` is false, the block has no suspension point left, and it simply completes — it will not run again until the next full `STOP`/`START` cycle.

On a first-time launch, `checkAndRequestPermission()` (line 64) fires the system permission dialog *after* the block above has already reached `STARTED` and found the permission denied. Whether the block ever re-executes and finally calls `.collect(...)` depends entirely on whether showing the permission dialog happens to drive the host activity through `onStop()`/`onStart()` — behavior that is not guaranteed across Android versions/OEMs (many devices only trigger `onPause()`/`onResume()` for the permission UI, not a full stop). If that stop/start transition doesn't happen, granting the permission calls `showReady()` (line 34/78, purely a text/visibility change) but the collector that would ever call `updatePlaceholder()` with a real `Reading`/`Searching`/`NoSignal` state never starts. The UI would then be stuck showing "Pronto" forever, with the speedometer never displaying a value — a total failure of the app's core value ("la velocità attuale deve essere sempre visibile, corretta").

**Fix:** Don't gate the collector with a one-shot permission check inside `repeatOnLifecycle`. Drive it off a reactive permission-state flow instead, e.g.:
```kotlin
private val permissionGranted = MutableStateFlow(false)

private fun refreshPermissionState() {
    permissionGranted.value = ContextCompat.checkSelfPermission(
        this, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

// in onCreate:
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
Call `refreshPermissionState()` from `checkAndRequestPermission()`, the `requestPermissionLauncher` callback, and `onResume()` so the collector reacts immediately to a grant regardless of whether a stop/start cycle occurred.

## Warnings

### WR-01: Staleness detection uses wall-clock time, not a monotonic clock

**File:** `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt:85, 92, 100-105`
**Issue:** `lastAcceptedUpdateAtMs` and the `ticker` both use `System.currentTimeMillis()`, and the D-02 "no signal for 5s" check is `now - lastAcceptedUpdateAtMs > 5000L`. `System.currentTimeMillis()` is wall-clock time and can jump — due to NTP sync, manual clock changes, time zone/DST changes, or (ironically, for a GPS app) Android's "use GPS/network-provided time" auto-sync, which can adjust the system clock right when a GPS fix is (re)acquired. A backward jump can make the difference negative indefinitely (the app would never show `NoSignal` even though GPS is actually lost); a forward jump can make it spuriously huge (flashing `NoSignal` even though updates are current).
**Fix:** Use `android.os.SystemClock.elapsedRealtime()` for both `lastAcceptedUpdateAtMs` and the ticker's timestamp — it is monotonic and unaffected by wall-clock adjustments:
```kotlin
lastAcceptedUpdateAtMs = SystemClock.elapsedRealtime()
...
emit(SystemClock.elapsedRealtime())
```

### WR-02: No test coverage for the actual GPS state machine

**File:** `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt` (whole file); `app/src/test/java/com/sed/tachimetro/gps/SpeedMappingTest.kt`
**Issue:** `SpeedMappingTest` thoroughly covers the pure `mapSpeedToKmh` function, but there is no test at all for `GpsSpeedProvider.state` — the `combine(acceptedKmh, ticker)` logic that decides between `Searching`, `Reading`, and `NoSignal` (D-01/D-02), including the 5-second staleness window and the `lastAcceptedUpdateAtMs` bookkeeping. This is the most behaviorally complex part of the phase (async, time-dependent, stateful) and is currently the least verified.
**Fix:** Extract the `when { ... }` state-decision logic (lines 101-105) into a small pure function (e.g. `deriveSpeedState(lastKmh: Int?, now: Long, lastAcceptedAtMs: Long): SpeedState`) that can be unit tested the same way `mapSpeedToKmh` is, independent of Flow/`combine`/coroutines machinery.

### WR-03: Hardcoded, non-localized speed unit string

**File:** `app/src/main/java/com/sed/tachimetro/MainActivity.kt:144`
**Issue:** `messageText.text = "${state.kmh} km/h"` builds the displayed text via raw string interpolation, while every other user-visible string in this app goes through `res/values/strings.xml` (`status_ready`, `permission_denied`, `searching_gps_signal`, etc.). This is inconsistent with the rest of the codebase and blocks future localization (e.g. a `mph` variant, or locale-specific formatting).
**Fix:**
```xml
<string name="speed_kmh_format">%1$d km/h</string>
```
```kotlin
is SpeedState.Reading -> getString(R.string.speed_kmh_format, state.kmh)
```

### WR-04: Activity context passed into GpsSpeedProvider, whose scope is never torn down

**File:** `app/src/main/java/com/sed/tachimetro/MainActivity.kt:48`, `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt:38, 40, 54`
**Issue:** `GpsSpeedProvider(this)` passes the `Activity` context directly (not `applicationContext`), and `GpsSpeedProvider` creates its own `CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)` (line 54) with no `close()`/`cancel()` method exposed. On every configuration change (e.g. screen rotation — no `configChanges` handling is declared in the manifest, so the activity is destroyed/recreated), a new `GpsSpeedProvider` (and a new scope) is created while the previous instance has no explicit teardown hook. `FusedLocationProviderClient` itself doesn't need an `Activity` context, so there's no reason to retain one.
**Fix:** Pass `context.applicationContext` into `LocationServices.getFusedLocationProviderClient(...)`, and consider exposing a `fun close() = scope.cancel()` called from `MainActivity.onDestroy()` for symmetry/defensiveness, even though `WhileSubscribed()` already stops the upstream location updates when unsubscribed.

## Info

### IN-01: `trySend` result silently ignored

**File:** `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt:65`
**Issue:** `result.lastLocation?.let { trySend(it) }` discards the `ChannelResult` from `trySend`. This is acceptable best-effort behavior for a location stream, but silently dropping a failed send (e.g. channel closed/full) with no logging makes it harder to diagnose missed updates during development.
**Fix:** Optional — log on failure in debug builds, e.g. `trySend(it).onFailure { /* log */ }`.

### IN-02: Magic timing constants could be named

**File:** `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt:42-43, 93, 103`
**Issue:** `1000L` (location update interval / ticker cadence) and `5000L` (no-signal timeout) are inline literals. They are documented via comments (D-02/GPS-01) but appear multiple times without a single named source of truth.
**Fix:**
```kotlin
private val updateIntervalMs = 1000L
private val noSignalTimeoutMs = 5000L
```

---

_Reviewed: 2026-07-07T00:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
