# Codebase Structure

**Analysis Date:** 2026-08-22

## Directory Layout

```
Tachimetro/
├── app/                           # Single Android module
│   ├── build.gradle.kts           # Module build config; dependencies, SDK versions
│   ├── proguard-rules.pro         # ProGuard rules (minify disabled, not used)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml                # App manifest; MAIN/LAUNCHER intent filter
│       │   ├── java/com/sed/tachimetro/
│       │   │   ├── MainActivity.kt                # Single entry point; orchestrates all UI
│       │   │   ├── gps/
│       │   │   │   ├── GpsSpeedProvider.kt        # Reactive GPS state provider
│       │   │   │   ├── SpeedState.kt              # Sealed model: Searching, Reading, NoSignal
│       │   │   │   └── SpeedMapping.kt            # Pure function: m/s → km/h filtering
│       │   │   ├── maxspeed/
│       │   │   │   ├── MaxSpeedStore.kt           # SharedPreferences persistence
│       │   │   │   └── MaxSpeedReducer.kt         # Pure function: monotonic max update
│       │   │   └── screen/
│       │   │       └── ScreenOnPreferenceStore.kt # SharedPreferences: keep-screen-on toggle
│       │   └── res/
│       │       ├── layout/
│       │       │   └── activity_main.xml          # Single ConstraintLayout; speed/message display
│       │       ├── values/
│       │       │   ├── colors.xml                 # App color palette (black background, white text)
│       │       │   ├── strings.xml                # String resources (UI labels, messages)
│       │       │   ├── themes.xml                 # Light theme: Theme.Tachimetro
│       │       │   └── ic_launcher_background.xml # Launcher icon background color
│       │       ├── values-night/
│       │       │   └── themes.xml                 # Dark theme variant
│       │       ├── color/
│       │       │   ├── switch_thumb_tint.xml      # Switch component tint
│       │       │   └── switch_track_tint.xml      # Switch component tint
│       │       ├── drawable/
│       │       │   ├── ic_launcher_background.xml # Launcher icon background
│       │       │   └── ic_launcher_foreground.xml # Launcher icon foreground
│       │       ├── mipmap-anydpi-v26/
│       │       │   ├── ic_launcher.xml            # Adaptive icon definition
│       │       │   └── ic_launcher_round.xml      # Round icon definition
│       │       ├── xml/
│       │       │   ├── backup_rules.xml           # Android backup rules
│       │       │   └── data_extraction_rules.xml  # Data extraction rules (targetSdk 36)
│       │       └── mipmap-*/                      # Raster launcher icons (multiple densities)
│       ├── test/
│       │   └── java/com/sed/tachimetro/
│       │       ├── gps/
│       │       │   ├── SpeedMappingTest.kt        # Unit tests: mapSpeedToKmh filtering
│       │       │   └── GpsSpeedProviderStateTest.kt # Unit tests: deriveSpeedState logic
│       │       └── maxspeed/
│       │           └── MaxSpeedReducerTest.kt     # Unit tests: reduceMax logic
│       └── androidTest/
│           └── java/com/sed/tachimetro/
│               └── ExampleInstrumentedTest.java   # Placeholder instrumented test (not implemented)
├── gradle/
│   ├── wrapper/
│   │   ├── gradle-wrapper.jar     # Gradle wrapper JAR
│   │   └── gradle-wrapper.properties # Gradle 9.3.1 distribution URL
│   ├── gradle-daemon-jvm.properties # JVM 21 toolchain for daemon
│   └── libs.versions.toml         # Version catalog; dependency versions centralized
├── build.gradle.kts               # Root build script; applies AGP plugin
├── settings.gradle.kts            # Gradle settings; declares :app module, repositories
├── gradle.properties              # Gradle JVM args, file encoding
└── gradlew / gradlew.bat          # Gradle wrapper scripts

```

## Directory Purposes

**app/:**
- Purpose: Single Android application module; contains all source, resources, tests.
- Contains: Source code, layout files, resource files, build configuration.
- Key files: `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`

**app/src/main/java/com/sed/tachimetro/:**
- Purpose: Production source root; all Kotlin application code lives here.
- Contains: MainActivity (entry point), GPS provider, state models, data stores.
- Organized by feature/responsibility: `gps/`, `maxspeed/`, `screen/`.
- Current structure is flat by responsibility; no feature-based sub-modules.

**app/src/main/java/com/sed/tachimetro/gps/:**
- Purpose: GPS data layer; continuous location updates, state derivation, filtering.
- Contains: 
  - `GpsSpeedProvider.kt` — wraps FusedLocationProviderClient, exposes StateFlow<SpeedState>
  - `SpeedState.kt` — sealed model representing GPS engine state
  - `SpeedMapping.kt` — pure filter function (m/s → km/h, accuracy/noise filtering)

**app/src/main/java/com/sed/tachimetro/maxspeed/:**
- Purpose: Session max speed tracking and persistence.
- Contains:
  - `MaxSpeedStore.kt` — SharedPreferences wrapper for max speed (read/write)
  - `MaxSpeedReducer.kt` — pure reducer (monotonically increase max only)

**app/src/main/java/com/sed/tachimetro/screen/:**
- Purpose: Screen-on preference persistence.
- Contains:
  - `ScreenOnPreferenceStore.kt` — SharedPreferences wrapper for keep-screen-on toggle

**app/src/main/res/:**
- Purpose: Android resources (layout, strings, colors, icons).
- Contains: Layout XML, string resources, color palette, themes, launcher icons.
- Organized by resource type (standard Android convention).

**app/src/main/res/layout/:**
- Purpose: UI layout definitions.
- Contains: `activity_main.xml` (single screen: messageText, unitText, maxSpeedText, buttons, switch in ConstraintLayout).

**app/src/main/res/values/:**
- Purpose: Default (light) theme and resource values.
- Contains:
  - `colors.xml` — App color palette (black, white, grays)
  - `strings.xml` — UI labels, error messages, status messages
  - `themes.xml` — Light theme definition (Theme.Tachimetro parent: NoActionBar)
  - `ic_launcher_background.xml` — Launcher icon background color

**app/src/main/res/values-night/:**
- Purpose: Dark theme variant (for devices with system dark mode enabled).
- Contains: `themes.xml` — Dark theme definition (inherits Theme.Tachimetro but night variant).

**app/src/main/res/color/:**
- Purpose: Color state lists (for state-aware tinting).
- Contains: `switch_thumb_tint.xml`, `switch_track_tint.xml` — tint colors for SwitchCompat component.

**app/src/main/res/drawable/:**
- Purpose: Vector drawables for launcher icon.
- Contains: `ic_launcher_foreground.xml`, `ic_launcher_background.xml` (adaptive icon assets).

**app/src/main/res/mipmap-anydpi-v26/:**
- Purpose: Adaptive icon definitions (Android 8+).
- Contains: `ic_launcher.xml`, `ic_launcher_round.xml` — point to foreground/background/monochrome vectors.

**app/src/main/res/mipmap-*/:**
- Purpose: Raster (bitmap) launcher icons for multiple screen densities (mdpi, hdpi, xhdpi, etc.).
- Contains: Pre-rendered PNG icon files (auto-generated by Android Studio adaptive icon tool).

**app/src/main/res/xml/:**
- Purpose: Android system configuration (backup, data extraction rules).
- Contains: `backup_rules.xml`, `data_extraction_rules.xml` (targetSdk 36 requirement).

**app/src/test/:**
- Purpose: JVM-only unit tests (run on development machine, no Android runtime needed).
- Contains: Pure function tests for GpsSpeedProvider logic, SpeedMapping, MaxSpeedReducer.
- Organized by feature (mirrors production structure: `gps/`, `maxspeed/`).

**app/src/androidTest/:**
- Purpose: Instrumented tests (run on Android device/emulator).
- Contains: Placeholder `ExampleInstrumentedTest.java` (not yet implemented).

**gradle/:**
- Purpose: Gradle wrapper and build configuration.
- Contains: Gradle version pin, JVM toolchain config, version catalog.

**gradle/libs.versions.toml:**
- Purpose: Centralized dependency and plugin version declarations.
- Contains: All dependency versions (AppCompat, Material, Coroutines, Play Services, JUnit, Espresso), plugin versions (AGP 9.1.1).
- Usage: Referenced in `build.gradle.kts` as `libs.*` aliases.

**build.gradle.kts (root):**
- Purpose: Root-level build configuration.
- Contains: Applies AGP plugin (apply false), sets up plugin repositories.

**app/build.gradle.kts:**
- Purpose: Module-specific build configuration.
- Contains: Namespace, SDK versions (minSdk 30, targetSdk/compileSdk 36), Java/Kotlin compiler settings, dependency declarations, build types (release: minify disabled).

**settings.gradle.kts:**
- Purpose: Gradle settings and repository configuration.
- Contains: Module declarations (`:app`), repository list (Google, Maven Central), version catalog inclusion.

## Key File Locations

**Entry Points:**
- `app/src/main/java/com/sed/tachimetro/MainActivity.kt` — App entry point; launched by Android framework via MAIN/LAUNCHER intent.
- `app/src/main/AndroidManifest.xml` — Manifest declarations; app icon, label, theme, permissions, activity.

**Configuration:**
- `app/build.gradle.kts` — Module build config; dependency versions (via libs.versions.toml), SDK versions, Java/Kotlin compiler options.
- `gradle/libs.versions.toml` — Centralized dependency versions (single source of truth).
- `settings.gradle.kts` — Gradle settings; repository/module declarations.
- `gradle.properties` — Gradle daemon JVM args.

**Core Logic:**
- `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt` — GPS data provider; reactive state machine.
- `app/src/main/java/com/sed/tachimetro/gps/SpeedMapping.kt` — Pure filtering logic (m/s → km/h).
- `app/src/main/java/com/sed/tachimetro/maxspeed/MaxSpeedStore.kt` — Max speed persistence.
- `app/src/main/java/com/sed/tachimetro/screen/ScreenOnPreferenceStore.kt` — Screen-on preference persistence.

**UI:**
- `app/src/main/res/layout/activity_main.xml` — Single screen layout (speed display, buttons, switch).
- `app/src/main/res/values/strings.xml` — UI text (labels, messages, button labels).
- `app/src/main/res/values/colors.xml` — Color palette.
- `app/src/main/res/values/themes.xml` — Light theme; `values-night/themes.xml` — Dark theme.

**Testing:**
- `app/src/test/java/com/sed/tachimetro/gps/SpeedMappingTest.kt` — mapSpeedToKmh unit tests.
- `app/src/test/java/com/sed/tachimetro/gps/GpsSpeedProviderStateTest.kt` — deriveSpeedState unit tests.
- `app/src/test/java/com/sed/tachimetro/maxspeed/MaxSpeedReducerTest.kt` — reduceMax unit tests.

## Naming Conventions

**Files:**
- **Kotlin source:** PascalCase, one public class per file (e.g., `MainActivity.kt`, `GpsSpeedProvider.kt`, `SpeedState.kt`).
- **XML resources:** snake_case (e.g., `activity_main.xml`, `ic_launcher_background.xml`, `backup_rules.xml`).
- **Test files:** Mirror production class name + `Test` suffix (e.g., `SpeedMappingTest.kt` → tests `SpeedMapping.kt`).

**Directories:**
- **Packages:** com.sed.tachimetro.{feature} (lowercase, dot-separated). Feature-based: `gps`, `maxspeed`, `screen`.
- **Resource directories:** Android standard (values/, layout/, drawable/, mipmap-*, xml/, color/).

**Classes:**
- **PascalCase:** `MainActivity`, `GpsSpeedProvider`, `MaxSpeedStore`, `ScreenOnPreferenceStore`.
- **Sealed models:** `SpeedState` (with subtypes: Searching, Reading, NoSignal).
- **Pure functions:** camelCase in-file (e.g., `mapSpeedToKmh()`, `deriveSpeedState()`, `reduceMax()`, `sanitizePersistedMax()`).

**Functions/Methods:**
- **camelCase:** Standard Java/Kotlin convention (e.g., `onCreate()`, `checkAndRequestPermission()`, `updatePlaceholder()`, `applyKeepScreenOn()`).

**Test methods:**
- **subject_expectedBehavior:** Underscore-separated descriptive names (e.g., `addition_isCorrect()` in stock template, follow this pattern for new tests).

## Where to Add New Code

**New Feature (e.g., speedometer notifications, trip timer):**
- **Production code:** Create new package under `app/src/main/java/com/sed/tachimetro/{featureName}/` (e.g., `notifications/`, `tripTracking/`).
- **Tests:** Create corresponding directory under `app/src/test/java/com/sed/tachimetro/{featureName}/` with `*Test.kt` files.
- **UI (if needed):** Add new Activity/Fragment classes in production, register in `AndroidManifest.xml`.
- **Persistence (if needed):** Create new store class (pattern: `{Feature}Store.kt` with SharedPreferences read/write methods) alongside feature code.
- **Resources:** Add new layout/strings/colors to `res/layout/`, `res/values/strings.xml`, `res/values/colors.xml`.

**New Component/Module (e.g., data layer, API client):**
- **If feature-specific:** Add to feature package (e.g., `gps/LocationFetcher.kt` within `gps/`).
- **If shared infrastructure:** Create new top-level package (e.g., `app/src/main/java/com/sed/tachimetro/network/` for API client).
- **Coordinate with architecture:** Pure functions/models should be tested independently. Components that depend on Android context should be instantiated in MainActivity or via dependency injection if DI is added later.

**Utilities/Helpers:**
- **Shared pure functions:** Add to existing feature package or create new `utils/` package under `app/src/main/java/com/sed/tachimetro/utils/`.
- **Extensions:** Follow Kotlin convention (e.g., `app/src/main/java/com/sed/tachimetro/utils/AndroidExtensions.kt`).

**Tests:**
- **Unit tests for new code:** Always co-locate with source package (e.g., `SpeedMapping.kt` tests live in `app/src/test/java/com/sed/tachimetro/gps/SpeedMappingTest.kt`).
- **Test data/fixtures:** If shared across multiple test files, create `app/src/test/java/com/sed/tachimetro/testhelpers/Fixtures.kt`.

## Special Directories

**app/.gradle/ (generated):**
- Purpose: Gradle build cache.
- Generated: Yes — auto-created by Gradle daemon.
- Committed: No — ignored by `.gitignore`.

**app/build/ (generated):**
- Purpose: Build output (compiled classes, intermediate resources, APK/AAB).
- Generated: Yes — auto-created by Gradle build tasks.
- Committed: No — ignored by `.gitignore`.

**.idea/ (generated):**
- Purpose: Android Studio project metadata (run configs, editor settings, module structure).
- Generated: Yes — auto-created/updated by Android Studio.
- Committed: Partially — some config files tracked (e.g., `.idea/vcs.xml` for git integration), others ignored (`.idea/workspace.xml`).

**gradle/wrapper/:**
- Purpose: Gradle wrapper distribution.
- Generated: Partially — wrapper JAR is committed; distribution URL is pinned in `.properties`.
- Committed: Yes — ensures consistent Gradle version across all checkouts.

---

*Structure analysis: 2026-08-22*
