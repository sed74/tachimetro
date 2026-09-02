---
phase: 09-permesso-di-localizzazione-dallo-schermo-auto
reviewed: 2026-09-02T14:32:59+02:00
depth: standard
files_reviewed: 6
files_reviewed_list:
  - app/src/main/java/com/sed/tachimetro/car/CarPermissionState.kt
  - app/src/main/java/com/sed/tachimetro/car/CarPermissionDenialStore.kt
  - app/src/test/java/com/sed/tachimetro/car/CarPermissionStateTest.kt
  - app/src/main/res/values/strings.xml
  - app/src/main/java/com/sed/tachimetro/car/SpeedScreen.kt
  - app/src/androidTest/java/com/sed/tachimetro/car/SpeedScreenTemplateTest.kt
findings:
  critical: 0
  warning: 3
  info: 2
  total: 5
status: issues_found
---

# Phase 09: Code Review Report

**Reviewed:** 2026-09-02T14:32:59+02:00
**Depth:** standard
**Files Reviewed:** 6
**Status:** issues_found

## Summary

Reviewed the car-side location-permission state machine (`CarPermissionState`, `CarPermissionDenialStore`, `SpeedScreen`), its resource strings, and its JVM/instrumented tests. The implementation is deliberate and unusually well documented: the permission state machine, the `denialCount`-based "permanent denial" heuristic, the `collectLatest`-driven reactive template rebuilding, and the ordering guarantees around `requestInFlight`/`Waiting` are all traced through carefully and backed by tests that exercise every `CarPermissionState` branch of `buildTemplate()`. Cross-checking against `MainActivity.kt`, `GpsSpeedProvider.kt`, `MaxSpeedStore.kt`, `ScreenOnPreferenceStore.kt` and `DistanceStore.kt` did not surface a crash, data-loss, or security-severity defect in the reviewed files.

The findings below are all maintainability/robustness issues: an unnecessary cross-domain coupling between the `car` and `maxspeed` packages, a permission-staleness gap relative to `MainActivity`'s equivalent handling, and duplicated "permanent denial" threshold logic that could silently drift if changed in only one place. No Critical/Blocker issues were found.

## Warnings

### WR-01: `CarPermissionDenialStore` couples the `car` package to the unrelated `maxspeed` domain for a shared-preferences file name

**File:** `app/src/main/java/com/sed/tachimetro/car/CarPermissionDenialStore.kt:5,21`
**Issue:** `CarPermissionDenialStore` imports `com.sed.tachimetro.maxspeed.MaxSpeedStore` solely to reuse the string literal `MaxSpeedStore.PREFS_NAME`:
```kotlin
import com.sed.tachimetro.maxspeed.MaxSpeedStore
...
private val prefs = context.getSharedPreferences(MaxSpeedStore.PREFS_NAME, Context.MODE_PRIVATE)
```
This is a real (if functionally harmless) violation of the codebase's own established pattern: every other store in this project (`ScreenOnPreferenceStore.kt:25`, `DistanceStore.kt:23`) independently declares its own `const val PREFS_NAME = "tachimetro_prefs"` rather than importing another store's companion object. The car-permission domain now has a compile-time dependency on the max-speed domain for something that has nothing to do with max speed, which is confusing for future readers/maintainers and makes `car` -> `maxspeed` show up as a real dependency edge in the module graph despite the two features being unrelated.
**Fix:**
```kotlin
class CarPermissionDenialStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun denialCount(): Int = sanitizeDenialCount(prefs.getInt(KEY_DENIAL_COUNT, 0))

    fun recordDenial() {
        prefs.edit().putInt(KEY_DENIAL_COUNT, denialCount() + 1).apply()
    }

    companion object {
        // Matches ScreenOnPreferenceStore/DistanceStore's own PREFS_NAME declaration --
        // same underlying file, but no import-time coupling to an unrelated domain.
        private const val PREFS_NAME = "tachimetro_prefs"
        private const val KEY_DENIAL_COUNT = "car_location_denial_count"
    }
}
```

### WR-02: Permission revocation is never re-detected while the car `Screen` stays continuously `STARTED`

**File:** `app/src/main/java/com/sed/tachimetro/car/SpeedScreen.kt:78-128,134-143`
**Issue:** `refreshPermissionState()` is only invoked once, at the top of `repeatOnLifecycle(Lifecycle.State.STARTED)` (line 90), each time the `Screen` transitions into `STARTED`. Once `permissionState` becomes `Granted`, the code enters `provider?.gpsSpeedProvider?.state?.collect { ... }` (lines 111-116), which suspends indefinitely and never calls `refreshPermissionState()` again. Unlike `MainActivity`, which re-checks the permission on every `onResume()` (`MainActivity.kt:248-255`), a `Screen` has no equivalent callback for "came back into focus without a STOP/START cycle." If `ACCESS_FINE_LOCATION` is revoked externally (e.g. from the phone's Settings) while the car screen remains uninterruptedly `STARTED` (a plausible scenario for a screen mounted in a car for a long drive), `permissionState` never leaves `Granted`, so the screen keeps trying to collect the shared `GpsSpeedProvider.state` and never surfaces the `Denied` UI/retry action — it will just silently sit on a frozen last reading and, after 5s of no fresh location callbacks, degrade to the generic "Ricerca segnale..." message instead of correctly reporting "permesso negato". This differs from the parity the file's own docs claim elsewhere ("mirror esatto di MainActivity...").
**Fix:** Add a periodic re-validation of the underlying OS permission while `Granted` is being collected, e.g. race the GPS collection against a slow ticker that calls `refreshPermissionState()`, or explicitly document this gap next to the other accepted limitations (Pitfall 1/2/3) in the class KDoc if it is intentionally out of scope for this milestone:
```kotlin
CarPermissionState.Granted -> {
    launch {
        while (isActive) {
            delay(5_000)
            refreshPermissionState()
        }
    }
    provider?.gpsSpeedProvider?.state?.collect { gpsState ->
        latestState = gpsState
        invalidate()
    }
}
```

### WR-03: "Permanent denial" threshold logic is duplicated in two places with different expressions

**File:** `app/src/main/java/com/sed/tachimetro/car/SpeedScreen.kt:170-174` vs `app/src/main/java/com/sed/tachimetro/car/CarPermissionState.kt:53`
**Issue:** The canonical rule for "this denial is permanent" lives in `resolveCarPermissionState()`: `denialCount >= 2`. But `SpeedScreen.requestLocationPermission()`'s callback computes the same concept independently and differently:
```kotlin
val wasAlreadyDenied = denialStore.denialCount() > 0   // pre-increment count
denialStore.recordDenial()
permissionState.value = CarPermissionState.Denied(permanent = wasAlreadyDenied)
```
`wasAlreadyDenied` (`count_before > 0`) is mathematically equivalent to `count_after >= 2` only because the threshold happens to be exactly 2 and the two expressions are algebraically related by the `+1` from `recordDenial()`. There is no shared constant and no shared function between the two computations — if the threshold in `resolveCarPermissionState()` is ever changed (e.g. tuned after further research into platform behavior), this second, independently-written formula in `SpeedScreen` will silently fall out of sync, producing incorrect `permanent` flags without any compiler or test signal pointing at the actual divergence (existing tests only exercise `resolveCarPermissionState` and `SpeedScreen.buildTemplate`, not this specific inline call site's arithmetic relationship to it).
**Fix:** Route the callback through the same pure function instead of re-deriving the threshold inline:
```kotlin
} else {
    denialStore.recordDenial()
    permissionState.value = resolveCarPermissionState(
        granted = false,
        denialCount = denialStore.denialCount(),
    )
}
```
This also removes the ordering-sensitive "read before write" comment (D-04) entirely, since `resolveCarPermissionState` is now the single, already-tested source of truth for the threshold.

## Info

### IN-01: `CarPermissionDenialStore` has no dedicated test coverage

**File:** `app/src/main/java/com/sed/tachimetro/car/CarPermissionDenialStore.kt:19-34`
**Issue:** `CarPermissionStateTest.kt` thoroughly covers the pure functions `resolveCarPermissionState()`/`sanitizeDenialCount()`, but the actual read-increment-write round trip in `CarPermissionDenialStore` (`denialCount()` / `recordDenial()`) has no Robolectric/instrumented test verifying that two consecutive `recordDenial()` calls actually persist `2`, or that a freshly-installed store starts at `0`.
**Fix:** Add a small Robolectric (or instrumented) test for `CarPermissionDenialStore` asserting `denialCount() == 0` on a fresh `SharedPreferences`, and that `recordDenial()` called N times makes `denialCount() == N`.

### IN-02: Known-limitation KDoc doesn't cover the "granted-then-externally-revoked" desync case

**File:** `app/src/main/java/com/sed/tachimetro/car/CarPermissionState.kt:16-21`
**Issue:** The class doc calls out one specific desync scenario for the persisted `denialCount` counter (first denial happening on the phone via `MainActivity` before any car connection). The counter can desync from the platform's actual "can ask again" flag in at least one more way: a user can be granted (via car or phone), later manually revoke the permission from Settings (which does not touch `denialCount`), and then deny again from the car dialog — at which point `CarPermissionDenialStore`'s persisted count (carried over from before the grant) can cause `Denied(permanent = true)` to be reported for what the OS actually treats as a fresh "can still ask again" state, since manually toggling a permission via Settings resets the platform's own rationale-tracking flag.
**Fix:** No code change required, but the KDoc's "Limitazione nota" paragraph should be broadened to describe the counter desync as a general class of issue (any external permission change that doesn't go through this store's `recordDenial()`), not just the single MainActivity-first-denial case, so a future reader/debugger recognizes it as an accepted risk rather than assuming the code is fully correct outside that one documented case.

---

_Reviewed: 2026-09-02T14:32:59+02:00_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
