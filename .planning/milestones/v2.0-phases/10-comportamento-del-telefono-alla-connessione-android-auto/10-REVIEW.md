---
phase: 10-comportamento-del-telefono-alla-connessione-android-auto
reviewed: 2026-09-02T15:23:26Z
depth: standard
files_reviewed: 5
files_reviewed_list:
  - app/src/main/java/com/sed/tachimetro/car/CarLinkState.kt
  - app/src/test/java/com/sed/tachimetro/car/CarLinkStateTest.kt
  - app/src/main/res/values/strings.xml
  - app/src/main/java/com/sed/tachimetro/MainActivity.kt
  - app/src/test/java/com/sed/tachimetro/car/CarLinkSequenceTest.kt
findings:
  critical: 0
  warning: 1
  info: 1
  total: 2
status: issues_found
---

# Phase 10: Code Review Report

**Reviewed:** 2026-09-02T15:23:26Z
**Depth:** standard
**Files Reviewed:** 5
**Status:** issues_found

## Summary

Reviewed the CONN-01/CONN-02 Android Auto phone-side behavior: the pure `CarLinkState`/`resolveCarLinkState`/`resolveEffectiveKeepScreenOn` functions, their JVM unit tests, the new `android_auto_connected` string, and the `MainActivity` wiring (`setupCarConnectionObserver()`, `onCarLinkChanged()`, `renderSpeedArea()`, the rewritten `setupScreenOnSwitch()`).

`CarLinkState.kt` and both test files are clean: the fail-safe default (`Disconnected` for null/negative/unknown/native connection types), the stateless truth table, and the sequence/idempotence tests all check out — traced by hand against the pure functions and found correct, including the 40-element alternation test and the raw-connection-type round trip. No hardcoded secrets, no dangerous APIs, no injection surfaces (this is local sealed-class/state logic, no external input parsing beyond an `Int?` from a platform LiveData). The diagnostic `Log.d()` call is correctly gated on `BuildConfig.DEBUG` and only logs `carLink`/`savedKeepOn`/the derived boolean — no speed or location data, consistent with the documented T-10-05 constraint.

One real correctness gap was found in `MainActivity.kt`: a transient stale-state window on `onResume()`/cold launch caused by `CarConnection`'s `LiveData` delivering its value asynchronously, which can briefly show the wrong phone-side message (and briefly leave `FLAG_KEEP_SCREEN_ON` in the wrong state) until the delayed emission self-corrects. This scenario is not covered by any of the automated tests or by the DHU checklist in `10-03-PLAN.md` (all scenarios there connect/disconnect while the app is already in the foreground with a settled `carLink`, never test cold-launch-already-connected or background-then-resume-after-a-change). See WR-01 below.

A minor duplication (INFO) was also found between `showReady()` and `renderSpeedArea()` for the "Connesso ad Android Auto" message-construction logic.

## Warnings

### WR-01: Transient stale phone-side state on resume/cold-launch due to async `CarConnection` LiveData delivery

**File:** `app/src/main/java/com/sed/tachimetro/MainActivity.kt:280-292` (`setupCarConnectionObserver`), `:296-327` (`onCarLinkChanged`), `:329-347` (`onResume`), `:393-407` (`checkAndRequestPermission`)

**Issue:** `carLink` is a plain instance field that is only updated when `carConnection.type.observe(this) { ... }` fires (line 289-291). That `LiveData` becomes active (and (re)delivers its current value) when the `Activity` reaches `STARTED`/`onActive()`, but the actual notification to the observer is not guaranteed to be synchronous with `onCreate()`/`onResume()` returning — it depends on the library's internal `BroadcastReceiver`/query implementation.

Meanwhile, `showReady()` (called from `checkAndRequestPermission()` at startup and from `onResume()` on every foreground transition) reads `carLink` synchronously to decide between `"Pronto"` and `"Connesso ad Android Auto"` (lines 431-435), and `setupScreenOnSwitch()`/`onCarLinkChanged()` similarly derive `FLAG_KEEP_SCREEN_ON` from whatever `carLink` currently holds.

Two concrete scenarios produce an observably wrong (self-correcting, but real) state for a brief window:
1. **Cold launch with Android Auto already connected and permission already granted** (returning user): `checkAndRequestPermission()` → `showReady()` runs while `carLink` is still its default `Disconnected` (the observer hasn't fired yet), so the phone briefly shows `"Pronto"` and briefly re-applies the saved `FLAG_KEEP_SCREEN_ON` state instead of the neutral message, until `onCarLinkChanged(Connected)` arrives and corrects it via the `changed`-gated `renderSpeedArea()` call.
2. **App backgrounded while Android Auto's connection state changes, then resumed**: the `CarConnection` `LiveData`'s internal receiver is unregistered in `onInactive()` (during `STOP`) and only re-queries on `onActive()` (during the next `STARTED`). `onResume()` calls `refreshPermissionState()` + `showReady()`/`showDenied()` using the *stale* pre-background `carLink` before the `LiveData` has a chance to re-fire, so the phone can briefly show the wrong message (e.g. still "Connesso ad Android Auto" right after AA was disconnected while backgrounded, or vice versa) until the delayed emission self-corrects.

Neither scenario is exercised by `CarLinkSequenceTest.kt` (which only tests the pure function, not the `Activity` wiring) nor by the DHU checklist in `10-03-PLAN.md` (all scenarios A-F connect/disconnect with the app already in the foreground and `carLink` already settled; none test cold-launch-already-connected or resume-after-background-change). The window is self-correcting and low-impact (a `FLAG_KEEP_SCREEN_ON` misapplication for a fraction of a second is harmless in practice), which is why this is a Warning rather than a Blocker — but it is a real, provable gap in an app whose stated Core Value is that on-screen information must be "always correct."

**Fix:** Synchronously reconcile `carLink` from the `LiveData`'s current value at the points that currently trust a possibly-stale field, instead of only reacting to the next async emission. For example, in `onResume()`:

```kotlin
override fun onResume() {
    super.onResume()
    // CONN-01: reconcile carLink synchronously before showReady()/showDenied() run --
    // CarConnection's LiveData re-delivers asynchronously on the next onActive(), which
    // can otherwise leave carLink stale for a foreground-visible frame after backgrounding
    // while Android Auto's connection state changed off-screen.
    if (::carConnection.isInitialized) {
        onCarLinkChanged(resolveCarLinkState(carConnection.type.value))
    }
    refreshPermissionState()
    ...
}
```

and equivalently seed `carLink` right after registering the observer in `setupCarConnectionObserver()` (reading `carConnection.type.value`, falling back to the existing default when still `null`). This removes the dependency on emission timing for the states that are read synchronously elsewhere in the Activity lifecycle.

## Info

### IN-01: Duplicated "Connesso ad Android Auto" message logic between `showReady()` and `renderSpeedArea()`

**File:** `app/src/main/java/com/sed/tachimetro/MainActivity.kt:431-435`, `:475-483`

**Issue:** Both `showReady()` and `renderSpeedArea()` independently branch on `carLink is CarLinkState.Connected` to decide whether to display `getString(R.string.android_auto_connected)`. The two call sites are currently consistent, but the duplication is a maintenance risk: a future change to the neutral-state message (extra formatting, a different string, additional view-visibility handling) only needs to be applied in one place to silently desync the two render paths.

**Fix:** Extract a small private helper, e.g.:

```kotlin
private fun androidAutoConnectedMessage(): String = getString(R.string.android_auto_connected)
```

or a `private fun showAndroidAutoConnectedState()` that both call sites invoke, so the neutral-state text has a single source of truth.

---

_Reviewed: 2026-09-02T15:23:26Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
