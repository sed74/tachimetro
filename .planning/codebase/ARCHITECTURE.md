<!-- refreshed: 2026-08-22 -->
# Architecture

**Analysis Date:** 2026-08-22

## System Overview

```text
┌──────────────────────────────────────────────────────────────────┐
│                        MainActivity (UI Layer)                    │
│              `app/src/main/java/com/sed/tachimetro/`              │
├────────────────────────────┬────────────────────────────────────┤
│    GPS Speed Provider      │   Permission & State Management    │
│  `gps/GpsSpeedProvider.kt` │   `MainActivity` lifecycle-aware   │
└────────┬───────────────────┴────────────┬───────────────────────┘
         │                                │
         │                                │
         ▼                                ▼
┌──────────────────────────────┐   ┌────────────────────────────────┐
│   SpeedState Model           │   │   MaxSpeed & Screen Stores     │
│  `gps/SpeedState.kt`         │   │  `maxspeed/*`, `screen/*`      │
│  `gps/SpeedMapping.kt`       │   │  SharedPreferences backed      │
└──────────────────────────────┘   └────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────────────────────────────┐
│           Google Play Services (FusedLocationProvider)           │
│                    GPS Raw Location Updates                      │
└──────────────────────────────────────────────────────────────────┘
```

## Component Responsibilities

| Component | Responsibility | File |
|-----------|----------------|------|
| **MainActivity** | Single entry point; orchestrates permission flow, UI state updates, layout window insets, speed/message display, max speed tracking, screen-on toggle | `app/src/main/java/com/sed/tachimetro/MainActivity.kt` |
| **GpsSpeedProvider** | Wraps FusedLocationProviderClient; exposes reactive StateFlow of SpeedState (Searching, Reading, NoSignal); applies accuracy filtering, noise floor filtering, 1-second staleness detection | `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt` |
| **SpeedState** | Sealed model representing GPS engine state: Searching (no fix yet), Reading (valid km/h), NoSignal (stale) | `app/src/main/java/com/sed/tachimetro/gps/SpeedState.kt` |
| **mapSpeedToKmh** | Pure function: filters raw GPS (m/s, accuracy) to km/h; drops poor accuracy readings, applies noise floor, converts to whole number | `app/src/main/java/com/sed/tachimetro/gps/SpeedMapping.kt` |
| **MaxSpeedStore** | Persists session max speed as single Int via SharedPreferences; sanitizes corrupted reads | `app/src/main/java/com/sed/tachimetro/maxspeed/MaxSpeedStore.kt` |
| **reduceMax** | Pure function: updates session max only if reading exceeds current max (monotonic increase only) | `app/src/main/java/com/sed/tachimetro/maxspeed/MaxSpeedReducer.kt` |
| **ScreenOnPreferenceStore** | Persists "keep screen on" toggle preference via SharedPreferences; returns null on first launch so MainActivity can derive default from charging state | `app/src/main/java/com/sed/tachimetro/screen/ScreenOnPreferenceStore.kt` |

## Pattern Overview

**Overall:** Reactive, single-Activity, Flow-based state management with immediate UI updates and async persistence.

**Key Characteristics:**
- **Reactive data flow:** GPS updates drive a StateFlow of SpeedState; MainActivity collects on lifecycle STARTED, updating UI synchronously on each emission.
- **Single source of truth:** Permission state lives in permissionGranted MutableStateFlow; GPS collection only starts when permission is granted. Current max speed is held in-memory in MainActivity, persisted to disk immediately on change.
- **No ViewModel/DI layer:** GpsSpeedProvider and stores are instantiated directly in MainActivity; scope is tied to Activity lifecycle (WhileSubscribed StateFlow, explicit close() on destroy).
- **Immersive fullscreen:** System bars hidden via WindowInsetsControllerCompat; content draws edge-to-edge with window insets handled explicitly to avoid overlap with status bar / display cutouts.
- **Asynchronous persistence:** SharedPreferences.edit().apply() is called (off main thread) for max speed and screen-on preference, never blocking UI.

## Layers

**UI/Presentation Layer:**
- Purpose: Render current speed/state, handle permission UI, manage fullscreen/immersive display, listen for user interaction (retry, reset max, toggle screen-on).
- Location: `app/src/main/java/com/sed/tachimetro/MainActivity.kt`, `app/src/main/res/layout/activity_main.xml`
- Contains: Activity lifecycle, layout binding, TextView autosize configuration, window insets handling, button click handlers.
- Depends on: GpsSpeedProvider, MaxSpeedStore, ScreenOnPreferenceStore, AndroidX AppCompat/Lifecycle/Constraintlayout.
- Used by: Android framework (launched from manifest intent filter).

**GPS/State Derivation Layer:**
- Purpose: Continuous FusedLocationProviderClient updates; accuracy/noise filtering; staleness detection; derive SpeedState model.
- Location: `app/src/main/java/com/sed/tachimetro/gps/`
- Contains: GpsSpeedProvider (wraps callback into Flow, combines with 1-second ticker), SpeedState (sealed model), mapSpeedToKmh (pure filter), deriveSpeedState (pure state machine).
- Depends on: Google Play Services Location, Kotlin coroutines/Flow.
- Used by: MainActivity to collect reactive speed updates.

**Persistence Layer:**
- Purpose: Read/write simple configuration (max speed, screen-on preference) to SharedPreferences; sanitize corrupted values.
- Location: `app/src/main/java/com/sed/tachimetro/maxspeed/`, `app/src/main/java/com/sed/tachimetro/screen/`
- Contains: MaxSpeedStore, MaxSpeedReducer (pure), ScreenOnPreferenceStore.
- Depends on: Android SharedPreferences, Context.
- Used by: MainActivity to persist state across sessions.

## Data Flow

### Primary Request Path: GPS Signal → Speed Display

1. **Permission gate** (`MainActivity.onCreate()`, `onCreate:144-210`) — checkAndRequestPermission() checks ACCESS_FINE_LOCATION; if denied, show denied UI; if granted, proceed to step 2.

2. **Permission reactive collection** (`MainActivity.onCreate()`, `onCreate:130-142`) — lifecycleScope.launch collects permissionGranted StateFlow; on each grant (or resume re-check), starts collecting gpsSpeedProvider.state.

3. **GPS raw location capture** (`GpsSpeedProvider.kt:67-75`) — callbackFlow wraps FusedLocationProviderClient.requestLocationUpdates(); emits each Location received via callback.

4. **Accuracy/noise filtering** (`GpsSpeedProvider.kt:77-95`) — map rawLocations through mapSpeedToKmh(); filter out poor-accuracy readings (> 50m); apply 2.0 km/h noise floor; convert m/s to whole km/h; filterNotNull() drops filtered results; record lastAcceptedUpdateAtMs.

5. **Staleness detection** (`GpsSpeedProvider.kt:98-103`, `GpsSpeedProvider.kt:105-114`) — combine acceptedKmh with 1-second ticker; derive SpeedState (Searching if no fix, Reading if fresh, NoSignal if stale > 5 sec).

6. **UI update** (`MainActivity.kt:256-283`, `updatePlaceholder()`) — on each SpeedState emission:
   - If Searching/NoSignal: show "Ricerca segnale GPS..." message
   - If Reading: display km/h number, set unitText visibility, check if new max reached
   - Call updateMaxArea() to show/hide MAX label and reset button

7. **Max speed update** (`MainActivity.kt:275-279`) — if reading > currentMax, call reduceMax(), persist via maxSpeedStore.write(), refresh MAX label.

### Secondary Flow: Permission Change (Runtime)

1. **checkAndRequestPermission()** (`MainActivity.kt:197-211`) — initial check in onCreate().

2. **Permission grant via system dialog** → requestPermissionLauncher callback (`MainActivity.kt:74-82`) → refreshPermissionState() → emit to permissionGranted → collectLatest restarts gpsSpeedProvider collection.

3. **Resume after Settings app** (`MainActivity.kt:147-165`, `onResume()`) — re-check permission state; if still denied, refresh denial UI; if granted externally, show ready UI.

### Tertiary Flow: User Interaction

**Reset max:** `onResetMaxClicked()` (`MainActivity.kt:287-291`) — set currentMax = 0, write to store, updateMaxArea() hides MAX label.

**Toggle screen-on:** `keepScreenOnSwitch.setOnCheckedChangeListener()` (`MainActivity.kt:119-122`) — call applyKeepScreenOn() to set/clear FLAG_KEEP_SCREEN_ON, persist toggle via screenOnStore.write().

**State Management:**
- **In-memory state:** currentMax (Int), permissionGranted (MutableStateFlow<Boolean>), keepOn (Boolean).
- **Persisted state:** max speed (SharedPreferences), screen-on preference (SharedPreferences).
- **Reactive state:** gpsSpeedProvider.state (StateFlow<SpeedState>) drives UI continuously on STARTED lifecycle.

## Key Abstractions

**SpeedState (Sealed Class):**
- Purpose: Represents the GPS engine's public state — searching for initial fix, actively reading speed, or stale (no update for 5+ sec).
- Examples: `app/src/main/java/com/sed/tachimetro/gps/SpeedState.kt`
- Pattern: Sealed class with three subtypes (Searching, Reading(kmh: Int), NoSignal); used in when-expression matching in MainActivity.updatePlaceholder().

**GpsSpeedProvider (Reactive Wrapper):**
- Purpose: Isolates GPS machinery (callback-based FusedLocationProviderClient) from MainActivity; exposes pure reactive StateFlow<SpeedState>.
- Examples: `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt`
- Pattern: Encapsulates callbackFlow, combines with ticker, shares state via WhileSubscribed() (stops upstream on no collectors). Passed applicationContext to prevent Activity leak.

**Pure Functions (mapSpeedToKmh, deriveSpeedState, reduceMax, sanitizePersistedMax):**
- Purpose: Isolate testable logic from Android/coroutines machinery.
- Examples: `app/src/main/java/com/sed/tachimetro/gps/SpeedMapping.kt`, `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt:135-139`, `app/src/main/java/com/sed/tachimetro/maxspeed/MaxSpeedReducer.kt`
- Pattern: Framework-free, take primitives, return primitives/sealed models; unit-testable in isolation.

## Entry Points

**MainActivity:**
- Location: `app/src/main/java/com/sed/tachimetro/MainActivity.kt`
- Triggers: Launched by Android framework via intent-filter in `app/src/main/AndroidManifest.xml:28-31` (MAIN/LAUNCHER).
- Responsibilities: 
  - Initialize GpsSpeedProvider, MaxSpeedStore, ScreenOnPreferenceStore on onCreate().
  - Bind UI views (messageText, unitText, maxSpeedText, retryButton, resetMaxButton, keepScreenOnSwitch).
  - Manage permission lifecycle (request, retry, settings fallback).
  - Collect gpsSpeedProvider.state reactively, update messageText/unitText/max area on each SpeedState change.
  - Handle button clicks (retry, reset max).
  - Apply window insets for status bar / display cutout edge-to-edge safety.
  - Restore max speed and screen-on preference from SharedPreferences on startup.

## Architectural Constraints

- **Threading:** 
  - Main thread: UI updates, permission checks, SharedPreferences writes (via apply(), off main thread).
  - Coroutine scope tied to Activity: GpsSpeedProvider.scope runs on Dispatchers.Main.immediate; location callbacks arrive on Looper.getMainLooper(); no background workers.
  - SystemClock.elapsedRealtime() used for staleness detection (monotonic, unaffected by NTP/manual clock changes).

- **Global state:** 
  - No singletons; no Application subclass.
  - MainActivity holds currentMax (in-memory) and reads/writes persisted max via MaxSpeedStore.
  - GpsSpeedProvider instantiated fresh per MainActivity instance; scope cancelled in onDestroy().
  - permissionGranted MutableStateFlow is local to MainActivity, single source of truth for permission state.

- **Circular imports:** None — clean dependency graph: MainActivity → GpsSpeedProvider/Stores; GpsSpeedProvider → none (just Android/coroutines); Stores → none (just Android SharedPreferences).

- **Min/Target SDK:** minSdk 30 (Android 11), targetSdk/compileSdk 36 (Android 15 with edge-to-edge). All window insets handling explicitly set (WindowCompat.setDecorFitsSystemWindows(false)) to ensure consistent behavior on API 30-34.

- **Permission model:** ACCESS_FINE_LOCATION requested at runtime; MainActivity is single source of truth. GpsSpeedProvider does NOT check permission itself (enforced via @Suppress("MissingPermission")); only collects state if MainActivity has confirmed grant.

- **Lifecycle:** Activities tied to Flow/StateFlow collection via repeatOnLifecycle(STARTED); GpsSpeedProvider.close() called in onDestroy() for defensive scope cleanup. No manual onStart()/onStop() collection management.

## Anti-Patterns

### Callback-Based Permission Dialogs Without Reactive Re-collection

**What happens:** Initial permission check passes, but if user grants later (without STOP/START cycle), UI remains stuck on "denied" screen until activity recreates.

**Why it's wrong:** System permission dialogs sometimes only trigger onPause()/onResume(), not STOP/START. One-shot checks in onCreate() miss late grants.

**Do this instead:** 
- Use MutableStateFlow (permissionGranted) to hold reactive permission state.
- Call refreshPermissionState() in onCreate(), onResume(), and the requestPermissionLauncher callback.
- collectLatest on permissionGranted in a repeatOnLifecycle(STARTED) block; restart GpsSpeedProvider collection immediately on grant, independent of lifecycle cycles.
- (See `MainActivity.kt:68-72`, `MainActivity.kt:130-142`, `MainActivity.kt:147-165`, `MainActivity.kt:191-195`, `MainActivity.kt:74-82`)

### Retaining Activity Reference in Long-Lived Components

**What happens:** GpsSpeedProvider passed Activity context; component holds it in a field; Activity instance leaks even after destroy because GpsSpeedProvider.scope never cancels.

**Why it's wrong:** Activity garbage collection blocked; memory leak; multiple instances accumulate if Activity recreates frequently (orientation change).

**Do this instead:**
- Accept applicationContext, not Activity, in component constructors (GpsSpeedProvider, MaxSpeedStore, ScreenOnPreferenceStore).
- If component owns a CoroutineScope tied to Activity lifetime, explicitly cancel it in Activity.onDestroy().
- (See `MainActivity.kt:125-127` comment WR-04, `GpsSpeedProvider.kt:42-45`, `MainActivity.kt:179-187`)

### Naive "No Signal" Detection Without Staleness Timestamp

**What happens:** GPS stops emitting; last reading remains on screen; user sees stale speed indefinitely until app restarts or re-enables location.

**Why it's wrong:** Silent failure; no indication that GPS signal is lost; dangerous for real-time speedometer use.

**Do this instead:**
- Record monotonic timestamp (SystemClock.elapsedRealtime()) of each accepted reading.
- Run a 1-second ticker alongside the readings; combine into state derivation.
- Emit NoSignal if now - lastAcceptedAtMs > 5 seconds.
- (See `GpsSpeedProvider.kt:90-103`, `deriveSpeedState()` lines 135-139, `MainActivity.kt:259`)

## Error Handling

**Strategy:** Fail gracefully with user-visible state; no exceptions propagate to crash the app.

**Patterns:**
- **Poor GPS accuracy:** Silently dropped in mapSpeedToKmh() (no reading shown, staleness detection triggers "no signal" after 5 sec). (`GpsSpeedProvider.kt:77-88`)
- **Permission denied:** Show user-facing message (permission_denied or permission_denied_permanent); offer Retry or Open Settings. No exception. (`MainActivity.kt:236-254`)
- **No GPS signal (startup/loss):** Show "Ricerca segnale GPS..." message; automatically clears when signal returns. No retry button. (`MainActivity.kt:259-262`)
- **Corrupted SharedPreferences:** Sanitize on read (negative max → 0). (`MaxSpeedStore.kt:14`, `MaxSpeedReducer.kt:12-13`)
- **Location callback null/missing:** Silently skipped (result.lastLocation?.let). (`GpsSpeedProvider.kt:70`)

## Cross-Cutting Concerns

**Logging:** None — no logging framework integrated. Silent fail pattern used instead.

**Validation:**
- GPS accuracy checked per reading (mapSpeedToKmh).
- Speed values sanitized (reduceMax, sanitizePersistedMax check >= 0).
- Permission checked in MainActivity, not delegated.

**Window Insets (Edge-to-Edge):** Three explicit listeners (applyUnitTextWindowInsets, applyMaxAreaWindowInsets, applyScreenSwitchWindowInsets) handle system bars and display cutout insets for each UI element's safe area. (`MainActivity.kt:366-449`)

**Authentication:** N/A — single-user device app; no user login.

**Immersive Fullscreen:** WindowCompat.setDecorFitsSystemWindows(false) + WindowInsetsControllerCompat with BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE. Re-applied in onWindowFocusChanged() to survive Settings app transitions. (`MainActivity.kt:347-353`, `MainActivity.kt:167-177`)

---

*Architecture analysis: 2026-08-22*
