# Codebase Concerns

**Analysis Date:** 2026-08-22

## Tech Debt

**Incomplete Data Extraction Rules:**
- Issue: `app/src/main/res/xml/data_extraction_rules.xml:8` contains a TODO placeholder for backup/data-extraction configuration
- Files: `app/src/main/res/xml/data_extraction_rules.xml`
- Impact: The backup framework (enabled via `android:allowBackup="true"` in `AndroidManifest.xml`) currently backs up all app data by default without explicit inclusion/exclusion rules. If sensitive data is added (e.g., user preferences, GPS history), it could be exposed unintentionally.
- Fix approach: Define explicit `<include>` and `<exclude>` tags for the data that must be backed up vs. protected. At minimum, exclude `SharedPreferences` if it ever stores sensitive data; currently only `max_speed_kmh` and `keep_screen_on` are stored (non-sensitive).

**MainActivity Monolith:**
- Issue: Single Activity class handles permission management (CR-01), GPS lifecycle (D-07), full UI rendering (speed/unit/max/message display), window insets for three separate views (unitText, maxSpeedText, keepScreenOnSwitch), screen keep-on state (D-06), max speed persistence, and immersive fullscreen toggling.
- Files: `app/src/main/java/com/sed/tachimetro/MainActivity.kt` (451 lines)
- Impact: Any change to permission logic, GPS lifecycle, or UI layout risks breaking multiple unrelated features. The file has reached a cognitive complexity ceiling where adding features (e.g., a toolbar, night mode toggle, trip duration display) becomes increasingly fragile.
- Fix approach: Extract reusable components: (1) PermissionManager — handles CR-01 reactive permission state and request/denial dialogs; (2) WindowInsetsHelper — centralizes the three applyXXXWindowInsets() methods into a single, parameterized system; (3) UiStateReducer — decouples the GPS/speed/max state transitions from direct view updates. None of these require a ViewModel, but they reduce MainActivity to 200-250 lines focused solely on lifecycle and the main collect() loop.

**Tunable but Hardcoded Accuracy Threshold:**
- Issue: GPS accuracy threshold (50 meters, D-05) is a constant `accuracyThresholdMeters = 50f` in `GpsSpeedProvider.kt:52`, marked as "tunable within the locked ~30-50m range" but there is no UI, preferences, or startup argument to adjust it at runtime.
- Files: `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt:52`
- Impact: Users in poor GPS environments (e.g., urban canyons, tunnels) cannot relax the threshold to get more frequent readings; conversely, users in suburban areas with multipath reflections cannot tighten it. A future phase may need to expose this as a preference.
- Fix approach: (Phase 2 or later) Add an optional preference store (similar to `ScreenOnPreferenceStore`) to persist user-selected accuracy tolerance, and pass it to `GpsSpeedProvider` on init. For now, document this as a known limitation and reserve the range 30-50m for future configuration.

**Manual Coroutine Scope Cleanup Required:**
- Issue: `GpsSpeedProvider.close()` must be called from `MainActivity.onDestroy()` to cancel the provider's `SupervisorJob`-scoped `CoroutineScope`. If omitted (e.g., in a future refactor where the provider is injected), the scope leaks for the lifetime of the process.
- Files: `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt:122-124`, `app/src/main/java/com/sed/tachimetro/MainActivity.kt:179-187`
- Impact: Coroutine scope leak → background job never stops → FusedLocationProviderClient continues requesting updates even after Activity destroy → battery drain, location info leaks.
- Fix approach: (1) Add an explicit safeguard: if `GpsSpeedProvider` is not closed within 500ms of Activity destroy, log a warning. (2) If DI is added later, use a factory or lifecycle-aware scope provider (e.g., ViewModel, viewModelScope) to manage scope lifetime automatically. (3) Document the `close()` requirement clearly in the KDoc and in `CLAUDE.md`.

## Known Bugs

**No Workaround Documented for Rapid Permission Toggles:**
- Symptoms: If a user rapidly grants/denies permission in system Settings while the app is visible, the UI may flicker or get stuck on the wrong screen (e.g., stuck on "denied" after a late permission grant).
- Files: `app/src/main/java/com/sed/tachimetro/MainActivity.kt:73-82`, `app/src/main/java/com/sed/tachimetro/MainActivity.kt:147-165`
- Trigger: (1) Open app, wait for "searching GPS" screen. (2) Swipe to system Settings, deny permission, return to app. (3) Immediately re-open Settings, grant permission, return to app. (4) Watch for race condition between `onResume()` refreshing state and the permission-granted flow reacting.
- Workaround: None currently; the reactive `permissionGranted` flow (CR-01) and `onResume()` checks should converge, but under extreme race conditions (e.g., permission change happening between Activity pause and resume), one might race the other.

## Security Considerations

**Backup Framework Exposes App Data Without Filtering:**
- Risk: Android backup framework (enabled by default in manifest) backs up all app data (SharedPreferences, cache, etc.) unfiltered. If the app later stores location history, user preferences, or other sensitive data, it would be included in device backups (cloud or otherwise) without explicit consent.
- Files: `app/src/main/AndroidManifest.xml:14-15`, `app/src/main/res/xml/data_extraction_rules.xml`, `app/src/main/java/com/sed/tachimetro/maxspeed/MaxSpeedStore.kt`, `app/src/main/java/com/sed/tachimetro/screen/ScreenOnPreferenceStore.kt`
- Current mitigation: Only `max_speed_kmh` (session max speed) and `keep_screen_on` (user preference) are stored; neither is sensitive.
- Recommendations: (1) Define explicit backup rules in `data_extraction_rules.xml` to exclude SharedPreferences or app cache by default. (2) If location history or trip data is added, explicitly exclude it from backup. (3) Test restore behavior on a real device or emulator to ensure the intended exclusions work.

**GPS Permission is Never Revoked at Runtime:**
- Risk: Once the app obtains ACCESS_FINE_LOCATION permission, the permission check happens only at startup and in `onResume()`. If the system revokes permission while the app is running (background, or after user toggles it in Settings), the app does not detect the revocation until next `onResume()`.
- Files: `app/src/main/java/com/sed/tachimetro/MainActivity.kt:147-165`
- Current mitigation: The app is designed to be always-on (full-screen speedometer in a car mount); the user is unlikely to toggle permissions mid-drive. However, if the app is backgrounded and permission is revoked, GPS updates continue until `onResume()` detects the denial.
- Recommendations: (1) Listen for `ACTION_PACKAGE_FULLY_REMOVED` or similar system broadcasts if permission revocation broadcast exists (unlikely; check official Android docs). (2) Wrap `FusedLocationProviderClient.requestLocationUpdates()` in a try/catch to detect SecurityException if permission is revoked mid-stream. (3) Consider adding a background Service (future phase) to monitor permission state in real-time; for now, document this as acceptable risk for a car-mounted app.

## Performance Bottlenecks

**Location Updates at 1-Second Cadence:**
- Problem: GPS/FusedLocationProviderClient is requested to update every 1000ms (1 second), per `LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)` in `GpsSpeedProvider.kt:47`. This cadence is a design choice (GPS-01) but drives continuous battery drain.
- Files: `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt:47-49`
- Cause: PRIORITY_HIGH_ACCURACY + 1-second intervals = FusedLocationProviderClient activates GPS hardware and fuses data constantly. In urban areas or with weak signal, this causes more aggressive location computation than necessary.
- Improvement path: (Phase 2+) Implement adaptive update rate: reduce interval to 3-5 seconds when stationary (speed == 0 for >10 sec), revert to 1 second when moving. Monitor battery drain in real-world usage first before optimizing; the current 1-second cadence may be acceptable for a car-mounted display that is powered from vehicle 12V.

**MainActivity's `collectLatest` and `combine` Flow Overhead:**
- Problem: `MainActivity.onCreate()` launches a `collectLatest` coroutine on `permissionGranted` flow, which in turn collects from `gpsSpeedProvider.state.collect { updatePlaceholder() }`. Each state update triggers full screen re-render (visibility toggles, text updates). No render optimization (early return if state unchanged) exists.
- Files: `app/src/main/java/com/sed/tachimetro/MainActivity.kt:130-142`, `app/src/main/java/com/sed/tachimetro/MainActivity.kt:256-283`
- Cause: `updatePlaceholder(state: SpeedState)` is called on every state change, including duplicate state (same speed value twice). While Android's view system optimizes re-layout, the Kotlin code re-executes string conversions, text assignments, and visibility changes unnecessarily.
- Improvement path: (Low priority — current perf is acceptable) Add a `distinctUntilChanged()` filter upstream of `updatePlaceholder()`, or implement a local cache of the last rendered state to skip redundant updates. Profile battery drain before optimizing.

## Fragile Areas

**Permission State Machine (CR-01):**
- Files: `app/src/main/java/com/sed/tachimetro/MainActivity.kt:68-145`
- Why fragile: The reactive `permissionGranted` flow + `onResume()` check + `requestPermissionLauncher` callback form a distributed state machine. If the order of checks changes or a new lifecycle event is added without updating the state machine, permission state can become inconsistent (e.g., UI shows "denied" but permission is actually granted, or vice versa).
- Safe modification: (1) Before changing permission-related code, trace all paths that can modify `permissionGranted.value`: only `checkAndRequestPermission()` (line 198) and `refreshPermissionState()` (line 191) should update it. (2) Add a unit test covering: first-launch deny, retry-after-deny, grant-in-Settings-then-resume, grant-via-system-dialog. (3) Document the invariant: "permissionGranted reflects the current actual permission state immediately after any permission-changing event."
- Test coverage: None (no instrumented tests for the lifecycle + permission + launcher flow). This is the highest-risk area.

**Window Insets Handling (three separate listeners):**
- Files: `app/src/main/java/com/sed/tachimetro/MainActivity.kt:366-408`, `app/src/main/java/com/sed/tachimetro/MainActivity.kt:435-449`
- Why fragile: Three separate `ViewCompat.setOnApplyWindowInsetsListener()` lambdas each capture baseline margin values from XML (`baseParams`, `baseTopMargin`, etc.) and compute dynamic insets on-the-fly. If a new UI element is added to the top-left or bottom-left corner, a fourth listener is needed — copy-paste risk if not carefully aligned with the layout structure.
- Safe modification: (1) Document the invariant in a top-level comment: "Each corner region (top-right: unitText, top-left: max area, bottom-left: switch) must have its own listener to compute independent insets." (2) Extract a helper function `computeInsetParams(baseMargin: Int, insetType: WindowInsetsCompat.Type): Int` to reduce duplication. (3) If a new corner element is added, trace through all three listeners to ensure cutout/systemBars insets are correctly prioritized (maxOf pattern).
- Test coverage: None; only manual testing on emulators with notches/cutouts and system bar changes.

**GPS Signal Loss Detection (D-02 Timeout):**
- Files: `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt:105-114`, `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt:135-139`
- Why fragile: The 5-second timeout (`now - lastAcceptedAtMs > 5000L`) is hard-coded. If FusedLocationProviderClient takes > 5 seconds to return the first fix in a new session, the UI immediately shows "No Signal" even though the app is still searching. Conversely, in locations with poor signal (rural, tunnels), a 5-second gap might be normal, but the app shows "No Signal" instead of "Last reading".
- Safe modification: (1) Increase timeout to 10-15 seconds if users report false "No Signal" events during startup. (2) Log a timestamp each time the timeout is triggered to track real-world behavior. (3) If user feedback indicates frequent false timeouts, add a preference (D-11+) to adjust the timeout 5-30 second range.
- Test coverage: `GpsSpeedProviderStateTest` covers the state machine logic; no integration test of actual FusedLocationProviderClient timeouts.

**SharedPreferences Synchronous Writes:**
- Files: `app/src/main/java/com/sed/tachimetro/maxspeed/MaxSpeedStore.kt:17-18`, `app/src/main/java/com/sed/tachimetro/screen/ScreenOnPreferenceStore.kt:20-21`
- Why fragile: Both store classes use `prefs.edit().putInt(...).apply()` (async) or `.commit()` (sync). If a burst of speed readings arrives (e.g., highway passing multiple cars), each new max triggers a write. Under very rare conditions (device crash/reboot during apply()), a write might be lost. The async `apply()` is safer than sync `commit()`, but still carries tiny risk.
- Safe modification: (1) Do not switch to sync `commit()` without a strong reason (current async is correct). (2) If max speed ever becomes critical data (e.g., logged for safety analysis), migrate to DataStore (replacement for SharedPreferences, more robust). (3) Add defensive read-on-startup: if persisted max is corrupted/negative, `sanitizePersistedMax()` already handles this (T-04-01).
- Test coverage: Unit tests cover the logic; no stress test of rapid writes under simulated crash conditions.

## Scaling Limits

**Single Activity Design Limits Modularity:**
- Current capacity: 1 activity, ~200 layout views across 1 XML file, up to ~10-15 UI elements, 1 GPS provider, 1 permission request, 1 coroutine scope.
- Limit: Adding a second screen (e.g., Trip Details, History, Settings) requires either fragmenting MainActivity or creating a second Activity. The current flow-based architecture (collectLatest on GPS provider state) scales linearly but not modularly.
- Scaling path: (Phase 3+) Refactor to multi-Activity or multi-Fragment structure with navigation component, or add a simple local state store (non-reactive, just an in-memory cache of GPS readings) to enable offline access to speed history without live GPS.

**GPS History Not Persisted:**
- Current capacity: Session-max speed only (~4 bytes, 1 integer).
- Limit: Users cannot view trip history, average speed, or duration — only the session's peak speed. A future feature to log location trail or trip summary would require database (Room, DataStore, or local SQLite).
- Scaling path: (Phase 4+) Add optional trip logging: Record Location objects (lat/lon/accuracy) on a configurable frequency (e.g., every 5 seconds) to local database. Expose a simple trip list and detail view. Requires: Room/DataStore dependency, second Activity/Fragment for history UI, potentially a broadcast receiver for app crash recovery.

## Dependencies at Risk

**Play Services Location 21.4.0 — FusedLocationProviderClient Coverage:**
- Risk: Depends on Google Play Services for FusedLocationProviderClient. If a device lacks Google Play Services (e.g., custom ROMs, some Chinese handsets), the app crashes on startup with `NoClassDefFoundError`.
- Impact: Blocks launch, app completely non-functional.
- Current mitigation: The manifest does not declare `<uses-library android:name="com.google.android.gms" android:required="false" />`, so the app can be installed on devices without Play Services but will crash if GPS is requested. The UX is poor (blank screen, possible ANR).
- Migration plan: (Phase 2) Add `android:required="false"` metadata for Play Services in the manifest, or implement a safe fallback: show a "Play Services not available" error message in UI instead of crashing. For a v1.0 car-mount speedometer, requiring Play Services is acceptable, but document this as a hard requirement.

**AndroidX Lifecycle Runtime 2.11.0 — Coroutine Integration:**
- Risk: Uses `lifecycle.repeatOnLifecycle()` (Jetpack Lifecycle + Coroutines integration). No forward-compatibility concern at 2.11.0, but future versions may change the API.
- Impact: Low; Jetpack lifecycle is stable and unlikely to break. The pattern is well-documented and widely used.
- Current mitigation: Locked to 2.11.0 in the version catalog; semver allows patch updates only.
- Migration plan: Monitor AndroidX release notes; no action needed at present.

## Missing Critical Features

**No Adaptive Update Frequency:**
- Problem: GPS updates run constantly at 1-second intervals regardless of vehicle state (parked vs. highway). Optimal behavior: reduce to 5-10 seconds when stopped, revert to 1 second when moving.
- Blocks: Reducing battery drain for all-day use cases (e.g., fleet vehicles with always-on GPS).

**No Trip Duration or Average Speed Display:**
- Problem: The app shows only current speed and session max. Users cannot see how long they've been driving or average speed.
- Blocks: Compliance with common speedometer UX expectations (most car dash displays show trip time).

**No Night Mode / Display Theme Control:**
- Problem: The app uses a hardcoded dark theme (black background, white text). No light mode toggle despite having night resources.
- Blocks: Usability in bright sunlight (white text on black is dim); not a blocker for MVP but noted for Phase 2.

**No Error Logging or Telemetry:**
- Problem: If GPS stops working in production, there is no way to detect or diagnose why. No events are logged, no crash reporter is integrated.
- Blocks: Debugging real-world issues; users report "app stopped showing speed" with no way to investigate root cause.

## Test Coverage Gaps

**No Instrumented Tests for Activity Lifecycle + GPS State:**
- What's not tested: The end-to-end flow of (1) Activity start with no permission → show "denied" screen, (2) user grants permission via system dialog → show "searching GPS", (3) FusedLocationProviderClient delivers first fix → show speed reading, (4) Activity pause/resume → state is preserved.
- Files: `app/src/main/java/com/sed/tachimetro/MainActivity.kt` (entire file), `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt` (entire lifecycle)
- Risk: A regression in lifecycle handling (e.g., forgotten `close()` call, wrong lifecycle state in `collectLatest`) would not be caught by CI.
- Priority: High — this is the core app flow.

**No Tests for Permission Request Result Handling:**
- What's not tested: The `requestPermissionLauncher` callback (`MainActivity.kt:74-82`) never executes in CI. No test verifies that a deny result shows the "denied" screen, or that a grant result shows "searching GPS".
- Files: `app/src/main/java/com/sed/tachimetro/MainActivity.kt:74-82`
- Risk: Permission UI logic could break silently.
- Priority: High — permission denial is a critical path.

**No Tests for UI State Transitions:**
- What's not tested: The visibility changes for `messageText`, `unitText`, `retryButton`, `maxSpeedText`, `resetMaxButton` in response to `SpeedState` changes. Example: when state transitions from `Searching` to `Reading`, `unitText.visibility` must toggle to `View.VISIBLE` and `applySpeedAutosize()` must run. No test verifies this.
- Files: `app/src/main/java/com/sed/tachimetro/MainActivity.kt:256-304`
- Risk: UI could break (e.g., unit label stays hidden when speed is displayed).
- Priority: Medium — can be caught by manual testing, but automated tests are preferable.

**No Tests for Window Insets Calculations:**
- What's not tested: The three `applyXXXWindowInsets()` functions never execute in CI. No test verifies that `unitText` is correctly shifted down by `systemBars.top + displayCutout.top` on a notched device.
- Files: `app/src/main/java/com/sed/tachimetro/MainActivity.kt:366-408`, `app/src/main/java/com/sed/tachimetro/MainActivity.kt:435-449`
- Risk: UI elements render behind status bar or notch on certain devices.
- Priority: Medium — requires emulator with virtual notch or real device testing.

**No Stress Test for Rapid State Changes:**
- What's not tested: Behavior when GPS state changes rapidly (e.g., Searching → Reading → NoSignal → Reading in quick succession). No test verifies that UI remains consistent and no reads are dropped.
- Files: `app/src/main/java/com/sed/tachimetro/MainActivity.kt:130-142` (collectLatest), `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt:105-114` (combine)
- Risk: Race condition in state rendering under high-frequency GPS updates.
- Priority: Low — unlikely in practice (GPS updates are 1 second apart), but useful for regression prevention if update interval is ever reduced.

---

*Concerns audit: 2026-08-22*
