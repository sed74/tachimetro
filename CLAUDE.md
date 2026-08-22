# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

<!-- GSD:project-start source:PROJECT.md -->

## Project

**Tachimetro**

App Android nativa che mostra la velocità GPS in tempo reale a schermo intero, con un'interfaccia minimale ad altissimo contrasto pensata per essere letta a colpo d'occhio mentre l'app è montata su un supporto in auto o in moto. Nessun menu, nessuna animazione, nessun grafico: solo il numero della velocità.

**Core Value:** La velocità attuale deve essere sempre visibile, corretta e leggibile istantaneamente in ogni condizione di luce — se questo non funziona alla perfezione, il resto non conta.

### Constraints

- **Tech stack**: Kotlin per il codice applicativo, layout XML tradizionali (no Jetpack Compose) — coerente con AppCompat già presente e adeguato per una singola schermata statica
- **GPS**: FusedLocationProviderClient (Google Play Services) — richiede un device con Google Play Services installato
- **Compatibility**: minSdk 30 (Android 11+), targetSdk 36
- **Performance**: aggiornamento velocità 1 volta/sec — bilanciamento scelto tra fluidità percepita e consumo batteria
- **UX**: nessun elemento grafico non necessario, nessun menu, nessuna animazione — massima leggibilità in ogni condizione di luce

<!-- GSD:project-end -->

<!-- GSD:stack-start source:codebase/STACK.md -->

## Technology Stack

## Languages

- Kotlin - Application code, build scripts via Gradle Kotlin DSL (`build.gradle.kts`, `app/build.gradle.kts`, `settings.gradle.kts`)
- Java 11 - Compilation target; legacy test stubs present in `app/src/test/java/` and `app/src/androidTest/java/`
- XML - Android resources (layouts, themes, strings, colors) in `app/src/main/res/`
- TOML - Dependency version management in `gradle/libs.versions.toml`

## Runtime

- Android 11+ (API 30) to Android 16 (API 36)
- Compilation SDK: 36 with minorApiLevel = 1
- Target SDK: 36
- JVM target: Java 11 (`compileOptions.sourceCompatibility/targetCompatibility = VERSION_11`)
- Gradle 9.3.1 (wrapper: `gradle/wrapper/gradle-wrapper.properties`)
- Gradle Kotlin DSL for all build scripts
- Gradle version catalog (centralized dependency versions in `gradle/libs.versions.toml`)
- Toolchain: JDK 21 for Gradle daemon (auto-resolved via foojay-resolver-convention v1.0.0)

## Frameworks

- Android Gradle Plugin 9.1.1 - App compilation, build configuration
- AndroidX AppCompat 1.6.1 - Activity/UI base compatibility layer
- Material Components for Android 1.10.0 - Material Design theming and components
- AndroidX Activity 1.9.3 (activity-ktx) - Activity lifecycle and coroutine integration
- AndroidX ConstraintLayout 2.2.1 - Flexible view layout management
- AndroidX Lifecycle Runtime 2.11.0 - Lifecycle-aware component support
- Kotlin Coroutines Core 1.10.2 - Asynchronous programming and threading
- Google Play Services Location 21.4.0 - FusedLocationProviderClient for GPS speed data
- JUnit 4.13.2 - Local unit test framework
- AndroidX Test Ext JUnit 1.1.5 - Instrumented test runner integration
- Espresso Core 3.5.1 - UI/instrumented testing framework
- Test runner: `androidx.test.runner.AndroidJUnitRunner` (configured in `app/build.gradle.kts`)
- Gradle Kotlin DSL (all `.kts` files)
- org.gradle.toolchains.foojay-resolver-convention v1.0.0 - Auto-provisions JDK toolchains

## Key Dependencies

- `com.google.android.gms:play-services-location:21.4.0` - Provides FusedLocationProviderClient for real-time GPS speed data; core to app's primary feature
- `androidx.appcompat:appcompat:1.6.1` - Provides Activity and AppCompatActivity base classes needed for any screen
- `com.google.android.material:material:1.10.0` - Material Design theming support
- `org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2` - Enables async location updates and non-blocking UI
- `androidx.lifecycle:lifecycle-runtime-ktx:2.11.0` - Lifecycle-aware coroutine scoping for location providers
- `androidx.activity:activity-ktx:1.9.3` - Activity extension functions for coroutine support
- `androidx.constraintlayout:constraintlayout:2.2.1` - Flexible view layout for speed display
- `junit:junit:4.13.2` - Unit test assertions
- `androidx.test.ext:junit:1.1.5` - AndroidX test runner
- `androidx.test.espresso:espresso-core:3.5.1` - Instrumented test UI assertions

## Configuration

- `local.properties` - Contains local Android SDK path (standard Android Studio file, not version-controlled secrets)
- No `.env` files or custom environment-specific config files
- No `BuildConfig` fields or `buildConfigField` entries defined
- Root build file: `build.gradle.kts` (declares AGP plugin version)
- Module build file: `app/build.gradle.kts` (namespace `com.sed.tachimetro`, versionCode 1, versionName "1.0")
- Settings: `settings.gradle.kts` (single module `:app`; repositories: Google, Maven Central, Gradle Plugin Portal)
- Version catalog: `gradle/libs.versions.toml` - Centralized dependency/plugin versions
- Gradle properties: `gradle.properties` (`-Xmx2048m -Dfile.encoding=UTF-8`; parallel mode disabled)
- Daemon JVM: `gradle/gradle-daemon-jvm.properties` (toolchainVersion=21; foojay URLs for auto-resolution)
- ProGuard rules: `app/proguard-rules.pro` (default template present; `isMinifyEnabled = false` in release build type)
- Location: `app/src/main/AndroidManifest.xml`
- Permissions: `android.permission.ACCESS_FINE_LOCATION` (fine-grained GPS required for speedometer accuracy; no coarse-location fallback per project constraint)
- Entry point: `.MainActivity` (exported, MAIN/LAUNCHER intent filter)
- Theme: `@style/Theme.Tachimetro`
- Backup/data extraction rules: `@xml/backup_rules`, `@xml/data_extraction_rules` (in `app/src/main/res/xml/`)
- Theme definitions: `app/src/main/res/values/themes.xml`, `app/src/main/res/values-night/themes.xml`
- Colors: `app/src/main/res/values/colors.xml`
- Strings: `app/src/main/res/values/strings.xml`
- Launcher icons: `app/src/main/res/mipmap-*` directories and `app/src/main/res/drawable/ic_launcher_*.xml` (adaptive launcher icon)

## Platform Requirements

- Android Studio compatible with AGP 9.1.1
- JDK 21 (auto-provisioned via foojay if not locally installed)
- Windows environment (project path: `C:\Users\fedes\AndroidStudioProjects\Tachimetro`); gradlew.bat available for CLI builds
- Android device/emulator running Android 11+ (API 30–36)
- Google Play Services installed (required for FusedLocationProviderClient)
- Location permission granted by user at runtime (Android 6+)
- Format: Native APK
- Build variants: debug (default), release (ProGuard disabled)
- Package name: `com.sed.tachimetro`
- Version: 1.0 (versionCode 1)

<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->

## Conventions

## Naming Patterns

- Kotlin files: PascalCase matching the primary class name (e.g., `MainActivity.kt`, `SpeedMapping.kt`, `MaxSpeedStore.kt`)
- Package structure: reverse-domain + feature/domain split (e.g., `com.sed.tachimetro.gps`, `com.sed.tachimetro.maxspeed`, `com.sed.tachimetro.screen`)
- Test files: `[SubjectClass]Test.kt` (e.g., `SpeedMappingTest.kt`, `GpsSpeedProviderStateTest.kt`)
- PascalCase for all classes: `MainActivity`, `GpsSpeedProvider`, `MaxSpeedStore`, `ScreenOnPreferenceStore`
- Sealed classes and data classes: PascalCase (e.g., `SpeedState` sealed class with `Searching`, `Reading`, `NoSignal` subclasses)
- Companions and top-level constants: SCREAMING_SNAKE_CASE (e.g., `PREFS_NAME`, `KEY_MAX_SPEED`, `AUTOSIZE_MIN_SP`)
- Top-level functions: camelCase (e.g., `mapSpeedToKmh()`, `deriveSpeedState()`, `reduceMax()`, `sanitizePersistedMax()`)
- Private/internal functions: camelCase (e.g., `checkAndRequestPermission()`, `updatePlaceholder()`, `applySpeedAutosize()`)
- Test method names: snake_case describing the condition and expected outcome (e.g., `hasSpeedFalse_returnsZero()`, `belowNoiseFloor_returnsZero()`, `poorAccuracy_returnsNull()`)
- camelCase for local variables and properties (e.g., `permissionGranted`, `messageText`, `lastAcceptedUpdateAtMs`, `currentMax`)
- Private/internal properties: camelCase with `private val` or `private var` (e.g., `private val scope`, `private var lastAcceptedUpdateAtMs`)
- Companion object constants: SCREAMING_SNAKE_CASE (e.g., `const val PREFS_NAME = "tachimetro_prefs"`)

## Code Style

- No `.editorconfig` or automated formatter (Spotless, ktlint) configured
- Default Kotlin/Android Studio formatting conventions apply: 4-space indentation, braces on same line
- Line length: pragmatic, following Android Studio defaults (typically 100-120 characters based on observed code)
- Spacing: blank lines between logical method groups, especially between public API and private helpers
- No detekt, ktlint, or custom lint configuration present
- Default Android Gradle Plugin lint checks apply
- Suppression annotations used sparingly: `@Suppress("MissingPermission")` when intentionally bypassing Android permission checks (e.g., `GpsSpeedProvider.kt:66` where permission is guaranteed by MainActivity)
- Prefer Kotlin idioms: data classes over Java POJOs, sealed classes for sum types (`SpeedState`), destructuring when useful
- Avoid unnecessary null-safety: use `?.let {}` and `?:` operators freely, avoid nested ifs
- Top-level pure functions for testability when logic can be isolated from Android framework (see `mapSpeedToKmh()`, `deriveSpeedState()`, `reduceMax()`)

## Import Organization

## Error Handling

- Prefer returning a safe default or null over throwing exceptions: `prefs.getInt(KEY_MAX_SPEED, 0)` defaults to 0 when key is missing
- Use null-coalescing with `?:`: `val keepOn = savedKeepOn ?: isDeviceCharging()`
- Check before dereferencing: `result.lastLocation?.let { trySend(it) }` in `GpsSpeedProvider.kt:70`
- Sanitize on read: `fun sanitizePersistedMax(raw: Int): Int = if (raw < 0) 0 else raw` (`MaxSpeedReducer.kt:13`) validates persisted data at entry point
- Use `@Suppress("MissingPermission")` with a class-level comment explaining why permission is safe (e.g., `GpsSpeedProvider.kt:66` — MainActivity checks permission before calling)

## Logging

- No structured logging framework is currently used
- Comments document behavior instead (see "Comments" section below)
- When logging is needed in future: consider logcat-based approach or explicit `Log.d()` calls (Android standard), not a third-party library

## Comments

- **Javadoc/KDoc for public classes and functions:** Explain purpose, parameters, and return values
- **Inline comments reference design decision tags:** Prefix inline comments with document/requirement tags (e.g., "D-01", "WR-04", "CR-01")
- **Complex logic:** Document the "why", not the "what"
- Use `/**` for class and function documentation
- Document parameters, return values, and notable behaviors
- Include `@see` links to external documentation or related code when relevant

## Function Design

- Prefer small, focused functions with a single responsibility
- Pure functions (no side effects) are preferred when possible and are always unit-testable
- Private helper functions in Activities/classes break down responsibilities (e.g., `applySpeedAutosize()`, `applyMessageAutosize()`, `updateMaxArea()`)
- Use named parameters for clarity, especially in constructors and test assertions
- Keep parameter lists short (<=5 params); use data classes if more are needed
- Default parameters are acceptable for stable configuration values (e.g., `accuracyThresholdMeters: Float = 50f`)
- Prefer explicit typed returns (`Int?`, `Boolean?`) over throwing exceptions
- Use `Unit` implicitly (no explicit return needed for functions that don't return a value)
- Sealed classes (`SpeedState`) for representing multiple possible outcomes

## Module Design

- One class/interface per file typically, with supporting pure functions in the same file
- Public functions documented with KDoc
- Internal state kept private; only expose what's needed for consumers
- `GpsSpeedProvider` exports `val state: StateFlow<SpeedState>` as its public API; internal `rawLocations` and `acceptedKmh` flows are private
- `MaxSpeedStore` exports `fun read()` and `fun write(value: Int)` as the persistence API
- `mapSpeedToKmh()` is a top-level, testable pure function; not hidden in a class
- Not currently used; each module is imported directly
- If needed in future: create `package com.sed.tachimetro.gps.models` with `SpeedState`, then a barrel `index.kt` exporting it
- Group related constants in companion objects (e.g., `MaxSpeedStore.PREFS_NAME`, `MaxSpeedStore.KEY_MAX_SPEED`)
- Use `const val` for compile-time constants, `val` for runtime

## Coroutines & Async

- Example from `MainActivity.kt:130-142`:
- Use `collectLatest` to interrupt the previous collector when the source emits again (for reactive permission changes)
- Use `callbackFlow` to wrap callback-based APIs (e.g., FusedLocationProviderClient in `GpsSpeedProvider.kt:67-75`)
- Coroutine scopes should match the lifetime of their owner
- `MainActivity` uses `lifecycleScope` (built-in)
- `GpsSpeedProvider` owns its own `scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)` and explicitly `close()` it in `MainActivity.onDestroy()`

## Resource Management

- Pass `applicationContext` to utilities that hold references (e.g., `GpsSpeedProvider`, `MaxSpeedStore`, `ScreenOnPreferenceStore`)
- Never pass Activity context to long-lived objects to avoid leaks

<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->

## Architecture

## System Overview

```text

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

- **Reactive data flow:** GPS updates drive a StateFlow of SpeedState; MainActivity collects on lifecycle STARTED, updating UI synchronously on each emission.
- **Single source of truth:** Permission state lives in permissionGranted MutableStateFlow; GPS collection only starts when permission is granted. Current max speed is held in-memory in MainActivity, persisted to disk immediately on change.
- **No ViewModel/DI layer:** GpsSpeedProvider and stores are instantiated directly in MainActivity; scope is tied to Activity lifecycle (WhileSubscribed StateFlow, explicit close() on destroy).
- **Immersive fullscreen:** System bars hidden via WindowInsetsControllerCompat; content draws edge-to-edge with window insets handled explicitly to avoid overlap with status bar / display cutouts.
- **Asynchronous persistence:** SharedPreferences.edit().apply() is called (off main thread) for max speed and screen-on preference, never blocking UI.

## Layers

- Purpose: Render current speed/state, handle permission UI, manage fullscreen/immersive display, listen for user interaction (retry, reset max, toggle screen-on).
- Location: `app/src/main/java/com/sed/tachimetro/MainActivity.kt`, `app/src/main/res/layout/activity_main.xml`
- Contains: Activity lifecycle, layout binding, TextView autosize configuration, window insets handling, button click handlers.
- Depends on: GpsSpeedProvider, MaxSpeedStore, ScreenOnPreferenceStore, AndroidX AppCompat/Lifecycle/Constraintlayout.
- Used by: Android framework (launched from manifest intent filter).
- Purpose: Continuous FusedLocationProviderClient updates; accuracy/noise filtering; staleness detection; derive SpeedState model.
- Location: `app/src/main/java/com/sed/tachimetro/gps/`
- Contains: GpsSpeedProvider (wraps callback into Flow, combines with 1-second ticker), SpeedState (sealed model), mapSpeedToKmh (pure filter), deriveSpeedState (pure state machine).
- Depends on: Google Play Services Location, Kotlin coroutines/Flow.
- Used by: MainActivity to collect reactive speed updates.
- Purpose: Read/write simple configuration (max speed, screen-on preference) to SharedPreferences; sanitize corrupted values.
- Location: `app/src/main/java/com/sed/tachimetro/maxspeed/`, `app/src/main/java/com/sed/tachimetro/screen/`
- Contains: MaxSpeedStore, MaxSpeedReducer (pure), ScreenOnPreferenceStore.
- Depends on: Android SharedPreferences, Context.
- Used by: MainActivity to persist state across sessions.

## Data Flow

### Primary Request Path: GPS Signal → Speed Display

### Secondary Flow: Permission Change (Runtime)

### Tertiary Flow: User Interaction

- **In-memory state:** currentMax (Int), permissionGranted (MutableStateFlow<Boolean>), keepOn (Boolean).
- **Persisted state:** max speed (SharedPreferences), screen-on preference (SharedPreferences).
- **Reactive state:** gpsSpeedProvider.state (StateFlow<SpeedState>) drives UI continuously on STARTED lifecycle.

## Key Abstractions

- Purpose: Represents the GPS engine's public state — searching for initial fix, actively reading speed, or stale (no update for 5+ sec).
- Examples: `app/src/main/java/com/sed/tachimetro/gps/SpeedState.kt`
- Pattern: Sealed class with three subtypes (Searching, Reading(kmh: Int), NoSignal); used in when-expression matching in MainActivity.updatePlaceholder().
- Purpose: Isolates GPS machinery (callback-based FusedLocationProviderClient) from MainActivity; exposes pure reactive StateFlow<SpeedState>.
- Examples: `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt`
- Pattern: Encapsulates callbackFlow, combines with ticker, shares state via WhileSubscribed() (stops upstream on no collectors). Passed applicationContext to prevent Activity leak.
- Purpose: Isolate testable logic from Android/coroutines machinery.
- Examples: `app/src/main/java/com/sed/tachimetro/gps/SpeedMapping.kt`, `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt:135-139`, `app/src/main/java/com/sed/tachimetro/maxspeed/MaxSpeedReducer.kt`
- Pattern: Framework-free, take primitives, return primitives/sealed models; unit-testable in isolation.

## Entry Points

- Location: `app/src/main/java/com/sed/tachimetro/MainActivity.kt`
- Triggers: Launched by Android framework via intent-filter in `app/src/main/AndroidManifest.xml:28-31` (MAIN/LAUNCHER).
- Responsibilities: 

## Architectural Constraints

- **Threading:** 
- **Global state:** 
- **Circular imports:** None — clean dependency graph: MainActivity → GpsSpeedProvider/Stores; GpsSpeedProvider → none (just Android/coroutines); Stores → none (just Android SharedPreferences).
- **Min/Target SDK:** minSdk 30 (Android 11), targetSdk/compileSdk 36 (Android 15 with edge-to-edge). All window insets handling explicitly set (WindowCompat.setDecorFitsSystemWindows(false)) to ensure consistent behavior on API 30-34.
- **Permission model:** ACCESS_FINE_LOCATION requested at runtime; MainActivity is single source of truth. GpsSpeedProvider does NOT check permission itself (enforced via @Suppress("MissingPermission")); only collects state if MainActivity has confirmed grant.
- **Lifecycle:** Activities tied to Flow/StateFlow collection via repeatOnLifecycle(STARTED); GpsSpeedProvider.close() called in onDestroy() for defensive scope cleanup. No manual onStart()/onStop() collection management.

## Anti-Patterns

### Callback-Based Permission Dialogs Without Reactive Re-collection

- Use MutableStateFlow (permissionGranted) to hold reactive permission state.
- Call refreshPermissionState() in onCreate(), onResume(), and the requestPermissionLauncher callback.
- collectLatest on permissionGranted in a repeatOnLifecycle(STARTED) block; restart GpsSpeedProvider collection immediately on grant, independent of lifecycle cycles.
- (See `MainActivity.kt:68-72`, `MainActivity.kt:130-142`, `MainActivity.kt:147-165`, `MainActivity.kt:191-195`, `MainActivity.kt:74-82`)

### Retaining Activity Reference in Long-Lived Components

- Accept applicationContext, not Activity, in component constructors (GpsSpeedProvider, MaxSpeedStore, ScreenOnPreferenceStore).
- If component owns a CoroutineScope tied to Activity lifetime, explicitly cancel it in Activity.onDestroy().
- (See `MainActivity.kt:125-127` comment WR-04, `GpsSpeedProvider.kt:42-45`, `MainActivity.kt:179-187`)

### Naive "No Signal" Detection Without Staleness Timestamp

- Record monotonic timestamp (SystemClock.elapsedRealtime()) of each accepted reading.
- Run a 1-second ticker alongside the readings; combine into state derivation.
- Emit NoSignal if now - lastAcceptedAtMs > 5 seconds.
- (See `GpsSpeedProvider.kt:90-103`, `deriveSpeedState()` lines 135-139, `MainActivity.kt:259`)

## Error Handling

- **Poor GPS accuracy:** Silently dropped in mapSpeedToKmh() (no reading shown, staleness detection triggers "no signal" after 5 sec). (`GpsSpeedProvider.kt:77-88`)
- **Permission denied:** Show user-facing message (permission_denied or permission_denied_permanent); offer Retry or Open Settings. No exception. (`MainActivity.kt:236-254`)
- **No GPS signal (startup/loss):** Show "Ricerca segnale GPS..." message; automatically clears when signal returns. No retry button. (`MainActivity.kt:259-262`)
- **Corrupted SharedPreferences:** Sanitize on read (negative max → 0). (`MaxSpeedStore.kt:14`, `MaxSpeedReducer.kt:12-13`)
- **Location callback null/missing:** Silently skipped (result.lastLocation?.let). (`GpsSpeedProvider.kt:70`)

## Cross-Cutting Concerns

- GPS accuracy checked per reading (mapSpeedToKmh).
- Speed values sanitized (reduceMax, sanitizePersistedMax check >= 0).
- Permission checked in MainActivity, not delegated.

<!-- GSD:architecture-end -->

<!-- GSD:skills-start source:skills/ -->

## Project Skills

No project skills found. Add skills to any of: `.claude/skills/`, `.agents/skills/`, `.cursor/skills/`, `.github/skills/`, or `.codex/skills/` with a `SKILL.md` index file.
<!-- GSD:skills-end -->

<!-- GSD:workflow-start source:GSD defaults -->

## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:

- `/gsd:quick` for small fixes, doc updates, and ad-hoc tasks
- `/gsd:debug` for investigation and bug fixing
- `/gsd:execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.
<!-- GSD:workflow-end -->

<!-- GSD:profile-start -->

## Developer Profile

> Profile not yet configured. Run `/gsd-profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.
<!-- GSD:profile-end -->
