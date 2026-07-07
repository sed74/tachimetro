---
phase: 02-motore-gps
fixed_at: 2026-07-07T18:45:00Z
review_path: .planning/phases/02-motore-gps/02-REVIEW.md
iteration: 1
findings_in_scope: 5
fixed: 5
skipped: 0
status: all_fixed
---

# Phase 02-motore-gps: Code Review Fix Report

**Fixed at:** 2026-07-07T18:45:00Z
**Source review:** .planning/phases/02-motore-gps/02-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 5 (CR-01, WR-01, WR-02, WR-03, WR-04 — Critical + Warning only; IN-01/IN-02 out of scope for this run)
- Fixed: 5
- Skipped: 0

**Build verification:** `./gradlew.bat assembleDebug testDebugUnitTest` — BUILD SUCCESSFUL, all unit tests pass (including the two new/updated test classes).

## Fixed Issues

### CR-01: GPS state collection may never start after first-time permission grant

**Files modified:** `app/src/main/java/com/sed/tachimetro/MainActivity.kt`
**Commit:** `ba032a5`
**Applied fix:** Replaced the one-shot `ContextCompat.checkSelfPermission(...)` check inside `repeatOnLifecycle(STARTED)` with a reactive `MutableStateFlow<Boolean> permissionGranted`, collected via `collectLatest` inside the same `repeatOnLifecycle(STARTED)` block (kept consistent with the existing lifecycle-scoped pattern per D-07, no new lifecycle mechanism introduced). Added `refreshPermissionState()` and wired it into `checkAndRequestPermission()`, the `requestPermissionLauncher` callback, and `onResume()` so a permission grant is reflected immediately regardless of whether a STOP/START cycle occurs.

**Verification note:** This finding involves lifecycle/state-handling logic (not pure syntax). Build and existing tests pass, but per the fixer's verification policy this class of fix is flagged as **"fixed: requires human verification"** — please manually exercise the deny-then-grant flow on a device/emulator (deny permission, then grant it from the system dialog without backgrounding the app) to confirm the GPS reading now appears without requiring an app restart.

### WR-01: Staleness detection uses wall-clock time, not a monotonic clock

**Files modified:** `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt`
**Commit:** `1d9b831`
**Applied fix:** Replaced both `System.currentTimeMillis()` call sites (in the `acceptedKmh` map that updates `lastAcceptedUpdateAtMs`, and in the `ticker` flow) with `android.os.SystemClock.elapsedRealtime()`, which is monotonic and unaffected by wall-clock adjustments (NTP/GPS time sync, manual clock changes). The `now - lastAcceptedUpdateAtMs > 5000L` D-02 staleness comparison itself is unchanged — only the two timestamp sources feeding it.

### WR-02: No test coverage for the actual GPS state machine

**Files modified:** `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt`, `app/src/test/java/com/sed/tachimetro/gps/GpsSpeedProviderStateTest.kt` (new)
**Commit:** `50dee26`
**Applied fix:** Extracted the `when { ... }` state-decision logic from the `combine(...)` block into a new pure top-level function `deriveSpeedState(lastKmh: Int?, now: Long, lastAcceptedAtMs: Long): SpeedState`, matching the reviewer's suggested lowest-risk approach (no `kotlinx-coroutines-test` infra added). Added `GpsSpeedProviderStateTest` with 5 cases: no accepted fix yet (Searching, D-01), a fresh reading (Reading), the exact 5-second boundary (still Reading — the check is a strict `>`), just-over 5 seconds (NoSignal, D-02), and well-over 5 seconds (NoSignal).

### WR-03: Hardcoded, non-localized speed unit string

**Files modified:** `app/src/main/res/values/strings.xml`, `app/src/main/java/com/sed/tachimetro/MainActivity.kt`
**Commit:** `6a5ba8c`
**Applied fix:** Added `<string name="speed_kmh_format">%1$d km/h</string>` to `strings.xml` (Italian-neutral — the unit label `km/h` is unaffected by UI-05's Italian-string requirement) and replaced `"${state.kmh} km/h"` with `getString(R.string.speed_kmh_format, state.kmh)` in `updatePlaceholder()`.

### WR-04: Activity context passed into GpsSpeedProvider, whose scope is never torn down

**Files modified:** `app/src/main/java/com/sed/tachimetro/MainActivity.kt`, `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt`
**Commit:** `c7487da`
**Applied fix:** `MainActivity` now constructs `GpsSpeedProvider(applicationContext)` instead of `GpsSpeedProvider(this)`. `GpsSpeedProvider` additionally uses `context.applicationContext` defensively when calling `LocationServices.getFusedLocationProviderClient(...)`. Added `fun close() = scope.cancel()` on `GpsSpeedProvider`, called from a new `MainActivity.onDestroy()` override, documented as a secondary safety net — D-07's `repeatOnLifecycle(STARTED)` remains the sole primary stop/start mechanism, unchanged.

## Skipped Issues

None — all 5 in-scope findings were fixed.

---

_Fixed: 2026-07-07T18:45:00Z_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
