---
phase: 07-distanza-percorsa-e-reset-unificato
reviewed: 2026-08-30T00:00:00Z
depth: standard
files_reviewed: 11
files_reviewed_list:
  - app/src/main/java/com/sed/tachimetro/MainActivity.kt
  - app/src/main/java/com/sed/tachimetro/distance/DistanceFormat.kt
  - app/src/main/java/com/sed/tachimetro/distance/DistanceReducer.kt
  - app/src/main/java/com/sed/tachimetro/distance/DistanceStore.kt
  - app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt
  - app/src/main/java/com/sed/tachimetro/gps/SpeedState.kt
  - app/src/main/res/layout/activity_main.xml
  - app/src/main/res/values/strings.xml
  - app/src/test/java/com/sed/tachimetro/distance/DistanceFormatTest.kt
  - app/src/test/java/com/sed/tachimetro/distance/DistanceReducerTest.kt
  - app/src/test/java/com/sed/tachimetro/gps/GpsSpeedProviderStateTest.kt
findings:
  critical: 0
  warning: 3
  info: 3
  total: 6
status: issues_found
---

# Phase 07: Code Review Report

**Reviewed:** 2026-08-30T00:00:00Z
**Depth:** standard
**Files Reviewed:** 11
**Status:** issues_found

## Summary

Reviewed the distance-tracking feature (`DistanceFormat`, `DistanceReducer`, `DistanceStore`, the extended `GpsSpeedProvider`/`SpeedState` pipeline) and the unified reset button in `MainActivity`. The pure-function layer (`formatDistanceDisplay`, `reduceDistance`, `sanitizePersistedDistance`, `deriveSpeedState`) is well-tested and correctly implements the documented noise-floor/threshold semantics, including the deliberately-accepted `999.6m` branch edge case.

The most significant finding is a **latent correctness gap** in `GpsSpeedProvider`: the `lastAcceptedLocation` reference point used to compute `deltaMeters` is never reset when the GPS collection pipeline restarts after being torn down (app backgrounded/foregrounded, or location permission revoked and re-granted). This creates a code path where the very next accepted fix after such a gap could add a large, spurious one-off jump to the accumulated distance. The team's own on-device verification (07-04-SUMMARY.md, criterion 5) exercised exactly this scenario and reported a pass, which is why this is filed as a WARNING rather than a BLOCKER — but the protection that made that test pass is not implemented explicitly anywhere in the reviewed code, so it is not guaranteed to hold across GPS chipsets/OS versions. No unit test exercises this integration path (it lives inside the `Flow`/`Location` plumbing, which isn't easily testable without Robolectric), so a regression here would not be caught by the existing suite.

Two further maintainability findings (an overgrown `onCreate()`, and an un-shared noise-floor constant duplicated between the speed-display and distance-accumulation code paths) and three minor Info items round out the review. No critical/security issues were found.

## Warnings

### WR-01: `lastAcceptedLocation` is never reset across GPS collection restarts, risking a spurious distance jump

**File:** `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt:68-69, 74-107`
**Issue:**
`lastAcceptedLocation` (line 68-69) is a plain instance field on `GpsSpeedProvider`, which is constructed **once** in `MainActivity.onCreate()` (`MainActivity.kt:177`) and lives for the whole Activity lifetime. GPS collection itself, however, starts and stops repeatedly: `repeatOnLifecycle(Lifecycle.State.STARTED)` cancels the `gpsSpeedProvider.state.collect{...}` collector on every `onStop()` (`MainActivity.kt:180-192`), and since `state` is shared via `SharingStarted.WhileSubscribed()` (`GpsSpeedProvider.kt:125-134`, default `stopTimeoutMillis = 0`), the upstream `combine(...)` / `acceptedReadings` / `rawLocations` collection — and therefore the actual `client.requestLocationUpdates(...)` subscription — is torn down immediately when the subscriber count drops to zero, and rebuilt from scratch (`awaitClose { client.removeLocationUpdates(callback) }` at line 81) the next time the app is foregrounded (or permission is re-granted after being revoked).

`lastAcceptedLocation`, however, is **not** reset when this happens — only `rawLocations`/`acceptedReadings` are cold flows that get freshly re-collected; the class-level field they close over persists unchanged. So the first accepted fix after any such gap computes:

```kotlin
val delta = lastAcceptedLocation?.distanceTo(loc) ?: 0f   // line 101
```

against the **stale, pre-gap** position, not `null`. If the device physically moved during the gap (user switched to another app, took a call, or the screen locked while driving), `distanceTo()` returns the straight-line distance covered during the entire gap, and `reduceDistance()` (`DistanceReducer.kt:11-23`) will add the whole thing in a single step as long as that first reading's `kmh` clears the noise floor.

The comment at `GpsSpeedProvider.kt:64-67` ("Pitfall 2") only documents the *continuously-running, vehicle-stopped* case (where fixes keep arriving and update the reference point even below the noise floor) — it does not address the *collection-stopped-and-restarted* case, and `07-RESEARCH.md` DIST-02 only analyzed "no accumulation **during** background," not "no jump **on return from** background." The one on-device test that covers this scenario (07-04-SUMMARY.md criterion 5, "Nessun tracking in background") passed, most likely because the first fix reacquired after a GPS gap tends to arrive with `hasSpeed() == false` (making `mapSpeedToKmh` return `0`, gating `reduceDistance` closed) — but that protection is incidental, undocumented, and not something the code enforces; it depends on FusedLocationProviderClient/GNSS-chipset timing that can vary by device and Android version.

**Fix:** Reset the reference point explicitly whenever a new `rawLocations` subscription starts, so the first fix after any gap is always treated the same way as the very first fix of the app's lifetime (`delta = 0f`):

```kotlin
@Suppress("MissingPermission")
private val rawLocations: Flow<Location> = callbackFlow {
    // Defensive reset (WR-01): every fresh subscription -- first launch, or a
    // resume after WhileSubscribed() tore the previous one down on background/
    // permission-revoke -- must not reuse a reference point from before the gap.
    lastAcceptedLocation = null
    val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { trySend(it) }
        }
    }
    client.requestLocationUpdates(request, callback, Looper.getMainLooper())
    awaitClose { client.removeLocationUpdates(callback) }
}
```

Consider also adding an integration-style test (or a Robolectric test) that simulates two back-to-back `rawLocations` collections with a large position jump between them, to lock this behavior in and catch future regressions that the current pure-function unit tests cannot reach.

### WR-02: `MainActivity.onCreate()` has grown to ~90 lines mixing five unrelated setup concerns

**File:** `app/src/main/java/com/sed/tachimetro/MainActivity.kt:115-206`
**Issue:** `onCreate()` now inlines: permission-view wiring, max-speed-area init/read, the new distance-area init/read (added this phase, lines 131-142), the screen-on switch + charging default derivation, and the GPS/charging `StateFlow` collectors — all in one function body. CLAUDE.md's own stated convention is "Private helper functions in Activities/classes break down responsibilities (e.g. `applySpeedAutosize()`, `applyMessageAutosize()`, `updateMaxArea()`)," and this function no longer follows it: the distance-area block (7 new lines plus 2 comment blocks) was appended in-place rather than extracted, pushing the function further past the "small, focused functions" guidance and the standard >50-line code-smell threshold.
**Fix:** Extract each concern into a private `setupXxx()` function called in sequence from `onCreate()`, e.g. `setupPermissionViews()`, `setupMaxSpeedArea()`, `setupDistanceArea()`, `setupScreenOnSwitch()`, `setupGpsCollection()`, `setupChargingIndicator()`. This keeps `onCreate()` as a short, readable list of initialization steps and matches the pattern already used elsewhere in the file (`applySpeedAutosize()`, `updateMaxArea()`, etc.).

### WR-03: Noise-floor threshold (`2.0` km/h) duplicated with no shared source of truth

**File:** `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt:55`, `app/src/main/java/com/sed/tachimetro/distance/DistanceReducer.kt:15`
**Issue:** `GpsSpeedProvider.noiseFloorKmh` (line 55, `= 2.0`) is the value actually used to filter the km/h shown to the user (passed explicitly into `mapSpeedToKmh(...)` at line 97). `reduceDistance`'s `noiseFloorKmh: Double = 2.0` default parameter (`DistanceReducer.kt:15`) is a **separate, independently-declared** literal with the same value, connected to the first only by convention/comment ("mirroring ... `noiseFloorKmh`"), not by any shared constant or import. `MainActivity.updatePlaceholder()` (`MainActivity.kt:363`) calls `reduceDistance(currentDistanceMeters, state.deltaMeters, state.kmh)` without passing `noiseFloorKmh` explicitly, relying entirely on the default staying in sync with `GpsSpeedProvider`'s field. `07-RESEARCH.md` acknowledges this is a deliberate mirroring, but it is still a coupling risk: if `GpsSpeedProvider.noiseFloorKmh` is ever tuned (the class comment at line 54 calls it "tunable within the locked ~30-50m range" for the sibling accuracy threshold, and D-05 language suggests the noise floor is likewise a tunable "locked example value"), the distance accumulator will silently keep using the old threshold, and speed display vs. distance accumulation would disagree about what counts as "moving," with no compiler or test failure surfacing the drift (the existing `DistanceReducerTest` hardcodes `kmh` values against the current literal, not against `GpsSpeedProvider`'s field).
**Fix:** Expose the threshold as a single named constant and have both call sites reference it, e.g.:

```kotlin
// GpsSpeedProvider.kt
companion object {
    /** D-03/D-04: shared with DistanceReducer.reduceDistance()'s default -- single source of truth. */
    const val NOISE_FLOOR_KMH = 2.0
}
private val noiseFloorKmh = NOISE_FLOOR_KMH
```

```kotlin
// MainActivity.kt (updatePlaceholder)
val newDistance = reduceDistance(
    currentDistanceMeters, state.deltaMeters, state.kmh,
    noiseFloorKmh = GpsSpeedProvider.NOISE_FLOOR_KMH,
)
```

## Info

### IN-01: `resetMaxButton`/`reset_max_button` identifiers no longer describe the button's behavior

**File:** `app/src/main/java/com/sed/tachimetro/MainActivity.kt:84, 127-128, 382-389`; `app/src/main/res/values/strings.xml:12`; `app/src/main/res/layout/activity_main.xml:77`
**Issue:** The button now performs a unified reset of both max speed and distance (MAX-04), and its user-visible text was correctly generalized to "Azzera" (D-08), but the Kotlin field name (`resetMaxButton`), the click handler indirection, the XML id (`@+id/resetMaxButton`), and the string resource key (`reset_max_button`) still read as max-speed-specific. This is a pre-existing, apparently deliberate choice (per `07-RESEARCH.md` D-08) to avoid a sweeping rename, but it is worth flagging for future readers who will see `resetMaxButton` and reasonably assume it only affects the max-speed metric.
**Fix:** Low priority; if a future phase touches this area again, consider renaming to `resetButton`/`reset_button` for clarity. Not urgent enough to justify a rename-only change today.

### IN-02: `formatDistanceDisplay` has no defensive handling for non-finite input

**File:** `app/src/main/java/com/sed/tachimetro/distance/DistanceFormat.kt:27-33`
**Issue:** Unlike `DistanceStore.read()`/`sanitizePersistedDistance()`, which clamp negative/corrupted persisted values, `formatDistanceDisplay(meters: Float)` performs no validation. If `meters` were ever `NaN` or `Float.POSITIVE_INFINITY` (not currently reachable from any code path — `reduceDistance` only ever sums non-negative finite floats — but a future change could introduce one, e.g. a division), `meters < 1000f` evaluates `false` for `NaN`, routing it into the `Kilometers` branch and producing a literal `"NaN"`/`"Infinity"` string rendered to the user via `"%1$.1f"`.
**Fix:** Given current unreachability this is low priority, but for defense-in-depth consistent with the project's stated error-handling convention ("prefer returning a safe default"), consider guarding at the top: `if (!meters.isFinite()) return DistanceDisplay.Meters(0)`.

### IN-03: Four near-identical window-insets listener functions in `MainActivity`

**File:** `app/src/main/java/com/sed/tachimetro/MainActivity.kt:568-687`
**Issue:** `applyUnitTextWindowInsets()`, `applyMaxAreaWindowInsets()`, `applyBottomLeftWindowInsets()`, and the new `applyDistanceAreaWindowInsets()` (added this phase, mirroring the existing three per its own doc comment) each independently read `systemBars`/`displayCutout` insets, compute `maxOf(...)`, and reassign `ConstraintLayout.LayoutParams` margins. The new function follows the pre-existing convention faithfully, so this isn't a regression, but the duplication has now grown to four copies of essentially the same ~15-line pattern.
**Fix:** Low priority given it matches established project conventions; if a fifth corner/element needs insets handling in a future phase, consider extracting a small shared helper (e.g. `fun extraInsets(insets: WindowInsetsCompat): Insets` returning the `maxOf(systemBars, cutout)` per edge) to reduce the boilerplate on each call site.

---

_Reviewed: 2026-08-30T00:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
