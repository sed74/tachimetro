# Coding Conventions

**Analysis Date:** 2026-08-22

## Naming Patterns

**Files:**
- Kotlin files: PascalCase matching the primary class name (e.g., `MainActivity.kt`, `SpeedMapping.kt`, `MaxSpeedStore.kt`)
- Package structure: reverse-domain + feature/domain split (e.g., `com.sed.tachimetro.gps`, `com.sed.tachimetro.maxspeed`, `com.sed.tachimetro.screen`)
- Test files: `[SubjectClass]Test.kt` (e.g., `SpeedMappingTest.kt`, `GpsSpeedProviderStateTest.kt`)

**Classes & Types:**
- PascalCase for all classes: `MainActivity`, `GpsSpeedProvider`, `MaxSpeedStore`, `ScreenOnPreferenceStore`
- Sealed classes and data classes: PascalCase (e.g., `SpeedState` sealed class with `Searching`, `Reading`, `NoSignal` subclasses)
- Companions and top-level constants: SCREAMING_SNAKE_CASE (e.g., `PREFS_NAME`, `KEY_MAX_SPEED`, `AUTOSIZE_MIN_SP`)

**Functions:**
- Top-level functions: camelCase (e.g., `mapSpeedToKmh()`, `deriveSpeedState()`, `reduceMax()`, `sanitizePersistedMax()`)
- Private/internal functions: camelCase (e.g., `checkAndRequestPermission()`, `updatePlaceholder()`, `applySpeedAutosize()`)
- Test method names: snake_case describing the condition and expected outcome (e.g., `hasSpeedFalse_returnsZero()`, `belowNoiseFloor_returnsZero()`, `poorAccuracy_returnsNull()`)

**Variables & Parameters:**
- camelCase for local variables and properties (e.g., `permissionGranted`, `messageText`, `lastAcceptedUpdateAtMs`, `currentMax`)
- Private/internal properties: camelCase with `private val` or `private var` (e.g., `private val scope`, `private var lastAcceptedUpdateAtMs`)
- Companion object constants: SCREAMING_SNAKE_CASE (e.g., `const val PREFS_NAME = "tachimetro_prefs"`)

## Code Style

**Formatting:**
- No `.editorconfig` or automated formatter (Spotless, ktlint) configured
- Default Kotlin/Android Studio formatting conventions apply: 4-space indentation, braces on same line
- Line length: pragmatic, following Android Studio defaults (typically 100-120 characters based on observed code)
- Spacing: blank lines between logical method groups, especially between public API and private helpers

**Linting:**
- No detekt, ktlint, or custom lint configuration present
- Default Android Gradle Plugin lint checks apply
- Suppression annotations used sparingly: `@Suppress("MissingPermission")` when intentionally bypassing Android permission checks (e.g., `GpsSpeedProvider.kt:66` where permission is guaranteed by MainActivity)

**Idioms:**
- Prefer Kotlin idioms: data classes over Java POJOs, sealed classes for sum types (`SpeedState`), destructuring when useful
- Avoid unnecessary null-safety: use `?.let {}` and `?:` operators freely, avoid nested ifs
- Top-level pure functions for testability when logic can be isolated from Android framework (see `mapSpeedToKmh()`, `deriveSpeedState()`, `reduceMax()`)

## Import Organization

**Order:**
1. Android framework imports (`android.*`)
2. AndroidX imports (`androidx.*`)
3. Google/third-party imports (`com.google.*, kotlinx.*, org.junit.*)
4. Internal project imports (`com.sed.tachimetro.*`)
5. Blank line between groups

**Example from MainActivity.kt:**
```kotlin
import android.Manifest
import android.content.Intent
// ... more android.* ...

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
// ... more androidx.* ...

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableStateFlow
// ... kotlinx.* ...

import com.sed.tachimetro.gps.GpsSpeedProvider
// ... internal imports ...
```

**Path Aliases:** Not applicable (Java/Kotlin Android project, no custom path mapping)

## Error Handling

**Pattern: Defensive null coalescing and early returns**
- Prefer returning a safe default or null over throwing exceptions: `prefs.getInt(KEY_MAX_SPEED, 0)` defaults to 0 when key is missing
- Use null-coalescing with `?:`: `val keepOn = savedKeepOn ?: isDeviceCharging()`
- Check before dereferencing: `result.lastLocation?.let { trySend(it) }` in `GpsSpeedProvider.kt:70`
- Sanitize on read: `fun sanitizePersistedMax(raw: Int): Int = if (raw < 0) 0 else raw` (`MaxSpeedReducer.kt:13`) validates persisted data at entry point

**Suppress Android permission checks only when guaranteed:**
- Use `@Suppress("MissingPermission")` with a class-level comment explaining why permission is safe (e.g., `GpsSpeedProvider.kt:66` — MainActivity checks permission before calling)

**No exceptions thrown in current codebase** — maintain this pattern for core logic; use typed returns (`Int?`, `Boolean?`) instead

## Logging

**Framework:** Console/Android Logcat implicit only (no explicit logging dependency like Timber or kotlin-logging configured)

**Current practice:** Logging is minimal; the app is designed for silent, reliable operation
- No structured logging framework is currently used
- Comments document behavior instead (see "Comments" section below)
- When logging is needed in future: consider logcat-based approach or explicit `Log.d()` calls (Android standard), not a third-party library

## Comments

**When to Comment:**
- **Javadoc/KDoc for public classes and functions:** Explain purpose, parameters, and return values
  - Example: `GpsSpeedProvider.kt:31-39` documents the class purpose, permission contract, and state-flow model
  - Example: `mapSpeedToKmh()` at `SpeedMapping.kt:6-10` documents return value semantics (null means "dropped")
- **Inline comments reference design decision tags:** Prefix inline comments with document/requirement tags (e.g., "D-01", "WR-04", "CR-01")
  - `D-##` = Design decision (e.g., D-01: no accepted fix yet → Searching state)
  - `WR-##` = Work/review checkpoint (e.g., WR-04: memory management rule)
  - `CR-##` = Code review/reactive pattern rule (e.g., CR-01: reactive permission flow)
- **Complex logic:** Document the "why", not the "what"
  - Example: `MainActivity.kt:99-100` explains *why* maxSpeedStore is read before GPS collection starts (to avoid "MAX 0" flash)
  - Example: `MainActivity.kt:335-346` explains the transition from deprecated SYSTEM_UI_FLAG_IMMERSIVE_STICKY to modern WindowInsetsControllerCompat, including minSdk 30 compatibility reasoning

**KDoc/Javadoc Style:**
- Use `/**` for class and function documentation
- Document parameters, return values, and notable behaviors
- Include `@see` links to external documentation or related code when relevant

Example from `GpsSpeedProvider.kt:31-39`:
```kotlin
/**
 * Wraps continuous FusedLocationProviderClient updates in a [callbackFlow] and exposes a
 * [StateFlow] of [SpeedState], applying the tested [mapSpeedToKmh] filters and detecting
 * startup/loss "no signal" per D-01/D-02.
 *
 * Permission note: this class does NOT check ACCESS_FINE_LOCATION itself. MainActivity
 * (Phase 1) is the single source of truth for that permission and only starts collecting
 * [state] once it has been granted — see RESEARCH.md Pitfall 1 / Anti-Patterns.
 */
```

## Function Design

**Size & Complexity:**
- Prefer small, focused functions with a single responsibility
- Pure functions (no side effects) are preferred when possible and are always unit-testable
  - Example: `mapSpeedToKmh()`, `deriveSpeedState()`, `reduceMax()` are pure and have full test coverage
  - Example: `MainActivity` orchestrates UI but delegates speed logic and state derivation to pure functions
- Private helper functions in Activities/classes break down responsibilities (e.g., `applySpeedAutosize()`, `applyMessageAutosize()`, `updateMaxArea()`)

**Parameters:**
- Use named parameters for clarity, especially in constructors and test assertions
  - Example: `mapSpeedToKmh(hasAccuracy = false, accuracyMeters = 60f, ...)`
- Keep parameter lists short (<=5 params); use data classes if more are needed
- Default parameters are acceptable for stable configuration values (e.g., `accuracyThresholdMeters: Float = 50f`)

**Return Values:**
- Prefer explicit typed returns (`Int?`, `Boolean?`) over throwing exceptions
- Use `Unit` implicitly (no explicit return needed for functions that don't return a value)
- Sealed classes (`SpeedState`) for representing multiple possible outcomes

## Module Design

**Exports & Public API:**
- One class/interface per file typically, with supporting pure functions in the same file
- Public functions documented with KDoc
- Internal state kept private; only expose what's needed for consumers

**Examples:**
- `GpsSpeedProvider` exports `val state: StateFlow<SpeedState>` as its public API; internal `rawLocations` and `acceptedKmh` flows are private
- `MaxSpeedStore` exports `fun read()` and `fun write(value: Int)` as the persistence API
- `mapSpeedToKmh()` is a top-level, testable pure function; not hidden in a class

**Barrel Files (Re-exports):**
- Not currently used; each module is imported directly
- If needed in future: create `package com.sed.tachimetro.gps.models` with `SpeedState`, then a barrel `index.kt` exporting it

**Constant Organization:**
- Group related constants in companion objects (e.g., `MaxSpeedStore.PREFS_NAME`, `MaxSpeedStore.KEY_MAX_SPEED`)
- Use `const val` for compile-time constants, `val` for runtime

## Coroutines & Async

**Pattern:** `lifecycleScope.launch` for lifecycle-aware launching; `Flow`/`StateFlow` for reactive state

- Example from `MainActivity.kt:130-142`:
  ```kotlin
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
- Use `collectLatest` to interrupt the previous collector when the source emits again (for reactive permission changes)
- Use `callbackFlow` to wrap callback-based APIs (e.g., FusedLocationProviderClient in `GpsSpeedProvider.kt:67-75`)

**Scoping:**
- Coroutine scopes should match the lifetime of their owner
- `MainActivity` uses `lifecycleScope` (built-in)
- `GpsSpeedProvider` owns its own `scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)` and explicitly `close()` it in `MainActivity.onDestroy()`

## Resource Management

**Context usage:**
- Pass `applicationContext` to utilities that hold references (e.g., `GpsSpeedProvider`, `MaxSpeedStore`, `ScreenOnPreferenceStore`)
- Never pass Activity context to long-lived objects to avoid leaks

Example from `MainActivity.kt:125-127`:
```kotlin
// Pass applicationContext, not the Activity, so GpsSpeedProvider (and the
// FusedLocationProviderClient it wraps) never retains an Activity reference.
gpsSpeedProvider = GpsSpeedProvider(applicationContext)
```

---

*Convention analysis: 2026-08-22*
