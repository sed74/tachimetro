---
phase: 07-distanza-percorsa-e-reset-unificato
fixed_at: 2026-08-30T00:00:00Z
review_path: .planning/phases/07-distanza-percorsa-e-reset-unificato/07-REVIEW.md
iteration: 1
findings_in_scope: 3
fixed: 3
skipped: 0
status: all_fixed
---

# Phase 07: Code Review Fix Report

**Fixed at:** 2026-08-30T00:00:00Z
**Source review:** .planning/phases/07-distanza-percorsa-e-reset-unificato/07-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 3 (fix_scope: critical_warning -- 0 critical/blocker, 3 warning; the 3 Info findings from REVIEW.md were out of scope and left untouched)
- Fixed: 3
- Skipped: 0

## Fixed Issues

### WR-01: `lastAcceptedLocation` is never reset across GPS collection restarts, risking a spurious distance jump

**Files modified:** `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt`
**Commit:** 7e4c5d5
**Applied fix:** Added `lastAcceptedLocation = null` as the first statement inside the `rawLocations` `callbackFlow { ... }` block, so every fresh subscription (first launch, or a resume after `WhileSubscribed()` tore the previous collection down on background/permission-revoke) starts from a clean reference point instead of reusing a stale, pre-gap `Location`. This matches the fix suggested in REVIEW.md exactly; the surrounding code (`client.requestLocationUpdates`, `awaitClose`) was unchanged.

### WR-02: `MainActivity.onCreate()` has grown to ~90 lines mixing five unrelated setup concerns

**Files modified:** `app/src/main/java/com/sed/tachimetro/MainActivity.kt`
**Commit:** d9d7503
**Applied fix:** Extracted `onCreate()`'s inline setup code into six private helper functions -- `setupPermissionViews()`, `setupDistanceArea()`, `setupMaxSpeedArea()`, `setupScreenOnSwitch()`, `setupGpsCollection()`, `setupChargingIndicator()` -- called in sequence from `onCreate()`, which is now a short, readable list of initialization steps. Preserved every cross-concern ordering dependency present in the original inline code (e.g. `currentDistanceMeters` must be set before the first `updateMaxArea()` call, so `setupDistanceArea()` runs before `setupMaxSpeedArea()`; `chargingIcon`'s view lookup stays bundled with `setupScreenOnSwitch()` because `applyBottomLeftWindowInsets()` updates both `keepScreenOnSwitch` and `chargingIcon` margins in a single listener). Each extraction is documented with a `WR-02` comment explaining its origin and any ordering constraint relative to sibling functions. Verified with `./gradlew.bat compileDebugKotlin`, which succeeded (`BUILD SUCCESSFUL`).

### WR-03: Noise-floor threshold (`2.0` km/h) duplicated with no shared source of truth

**Files modified:** `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt`, `app/src/main/java/com/sed/tachimetro/MainActivity.kt`
**Commit:** 4f606b7
**Applied fix:** Added a `companion object { const val NOISE_FLOOR_KMH = 2.0 }` to `GpsSpeedProvider` and changed the existing `private val noiseFloorKmh` field to reference it (`= NOISE_FLOOR_KMH`) instead of redeclaring the literal. Updated `MainActivity.updatePlaceholder()`'s `reduceDistance(...)` call to explicitly pass `noiseFloorKmh = GpsSpeedProvider.NOISE_FLOOR_KMH` instead of relying on `DistanceReducer.reduceDistance()`'s separate default parameter. `DistanceReducer.kt`'s own default value (`2.0`) was left unchanged (kept as a sane fallback for direct/test calls) since REVIEW.md's suggested fix only asked for both real call sites to reference a single named constant, not for a cross-package default-parameter dependency. Existing `DistanceReducerTest.kt` assertions (which call `reduceDistance` without passing `noiseFloorKmh` and rely on the `2.0` default) are unaffected.

## Skipped Issues

None -- all in-scope findings were fixed.

---

_Fixed: 2026-08-30T00:00:00Z_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
