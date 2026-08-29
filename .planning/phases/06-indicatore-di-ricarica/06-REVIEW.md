---
phase: 06-indicatore-di-ricarica
reviewed: 2026-08-29T00:00:00Z
depth: standard
files_reviewed: 10
files_reviewed_list:
  - app/src/main/java/com/sed/tachimetro/MainActivity.kt
  - app/src/main/java/com/sed/tachimetro/charging/ChargingState.kt
  - app/src/main/java/com/sed/tachimetro/charging/ChargingStateProvider.kt
  - app/src/main/res/drawable/charging_flash_fill.xml
  - app/src/main/res/drawable/ic_charging_flash.xml
  - app/src/main/res/drawable/ic_charging_flash_lime.xml
  - app/src/main/res/layout/activity_main.xml
  - app/src/main/res/values/colors.xml
  - app/src/main/res/values/strings.xml
  - app/src/test/java/com/sed/tachimetro/charging/ChargingStateProviderStateTest.kt
findings:
  critical: 0
  warning: 1
  info: 3
  total: 4
status: issues_found
---

# Phase 06: Code Review Report

**Reviewed:** 2026-08-29T00:00:00Z
**Depth:** standard
**Files Reviewed:** 10
**Status:** issues_found

## Summary

Reviewed the charging-indicator feature: `ChargingState`/`ChargingStateProvider` (sticky-broadcast-backed StateFlow with a pure `deriveChargingState` reducer), the `MainActivity` wiring that resolves a `ClipDrawable` fill layer and drives a `ValueAnimator` pulse/freeze/stop cycle, the three charging drawables, layout/color/string resources, and the JVM unit test for the pure reducer.

The core logic is sound: the sealed `ChargingState` model is exhaustively handled with no `else` branch, the `ClipDrawable` fill/level math and 2500ms full-cycle timing check out, lifecycle teardown (`onStop()` cancelling the animator, `WhileSubscribed()` unregistering the receiver, `onDestroy()` closing the provider's scope) is correctly reasoned through and matches the documented rationale in comments, and `mutate()` is used correctly to avoid `ConstantState` bleed. Safe casts throughout `resolveChargingFillLayer()` degrade gracefully instead of crashing if drawable structure ever changes. The unit test suite for `deriveChargingState` is complete (all `BatteryManager.BATTERY_STATUS_*` values plus the missing-extra `-1` fallback).

No critical/security issues were found. The main quality concern is that the new `ChargingState`/`deriveChargingState` logic duplicates (rather than reuses) an existing ad-hoc "is device charging" check already present in `MainActivity`, creating two independently-maintained sources of truth for the same broadcast data. A few minor resource/accessibility nits round out the findings.

## Warnings

### WR-01: Duplicate "is charging" derivation logic between MainActivity and ChargingStateProvider

**File:** `app/src/main/java/com/sed/tachimetro/MainActivity.kt:547-552` (compare with `app/src/main/java/com/sed/tachimetro/charging/ChargingStateProvider.kt:89-93`)
**Issue:** `MainActivity.isDeviceCharging()` re-implements, via its own independent `registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))` sticky-broadcast read, the exact same "is this device connected to power" decision that `deriveChargingState()` now encapsulates as a pure, tested function:

```kotlin
// MainActivity.kt:547-552
private fun isDeviceCharging(): Boolean {
    val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    return status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
}
```
is logically equivalent to `deriveChargingState(status) != ChargingState.Hidden`, but the equivalence is implicit and unenforced. If a future change extends `deriveChargingState()` (e.g. to also treat `BATTERY_STATUS_NOT_CHARGING` as "connected" for some devices) the two call sites will silently diverge, since nothing ties them together and no test guards the pairing.

**Fix:** Reuse the pure reducer instead of a second parallel implementation, e.g.:
```kotlin
private fun isDeviceCharging(): Boolean {
    val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    return deriveChargingState(status) != ChargingState.Hidden
}
```

## Info

### IN-01: Unused string resource `speed_kmh_format`

**File:** `app/src/main/res/values/strings.xml:9`
**Issue:** `<string name="speed_kmh_format">%1$d km/h</string>` is not referenced anywhere in `app/src/main/java` (confirmed via project-wide search — only the declaration itself matches). The speed digits and the "km/h" unit are rendered separately via `messageText`/`unitText` (`R.string.unit_kmh`), so this combined-format string appears to be dead.
**Fix:** Remove the unused resource, or if it is intentionally kept for a future use case, add a comment explaining why it is retained.

### IN-02: Charging content description does not distinguish "Full" from "Pulsing"

**File:** `app/src/main/res/values/strings.xml:14`, `app/src/main/java/com/sed/tachimetro/MainActivity.kt:362-380`
**Issue:** `charging_indicator_description` ("In carica") is a single static string applied via `android:contentDescription` in the layout and never updated by `updateChargingIcon()`. A TalkBack user hears "In carica" (charging) even when `ChargingState.Full` is active (battery full, plugged in, icon frozen solid) — which is a different, arguably more useful, piece of information ("carica completa").
**Fix:** Add a second string (e.g. `charging_full_description` = "Carica completa") and set `chargingIcon.contentDescription` dynamically inside `updateChargingIcon()`'s `Full` branch, falling back to the existing string for `Pulsing`.

### IN-03: `chargingStateProvider.close()` in `onDestroy()` is not defensive against a partially-initialized Activity

**File:** `app/src/main/java/com/sed/tachimetro/MainActivity.kt:227-238`
**Issue:** `chargingStateProvider` (like `gpsSpeedProvider`) is a `lateinit var` assigned partway through `onCreate()`. If `onCreate()` were ever to throw before reaching `chargingStateProvider = ChargingStateProvider(applicationContext)` (line 172), the unconditional `chargingStateProvider.close()` call in `onDestroy()` would raise `UninitializedPropertyAccessException`, masking the original failure with an unrelated crash. This mirrors a pre-existing pattern already present for `gpsSpeedProvider`, so it is not a new class of risk introduced by this phase, but the new field extends the same fragile pattern.
**Fix:** Guard with `if (::chargingStateProvider.isInitialized) chargingStateProvider.close()` (and likewise for `gpsSpeedProvider`) for defensiveness, or accept the current trade-off given how unlikely the failure window is.

---

_Reviewed: 2026-08-29T00:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
