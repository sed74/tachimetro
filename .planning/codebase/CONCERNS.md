# Codebase Concerns

**Analysis Date:** 2026-07-07

## Overall State

This repository is an unmodified Android Studio project scaffold ("Empty Activity" style template) for an app named **Tachimetro** (Italian for "speedometer"), applicationId `com.sed.tachimetro`. There is **no application code** — no Kotlin/Java source files under `app/src/main/java/com/sed/tachimetro/` (the package directory exists but is empty), no `MainActivity`, no layout XML, no launcher `<activity>` entry in `app/src/main/AndroidManifest.xml`. Git has never been committed (`git log` reports "no commits yet"; all files are untracked). Every concern below stems from this "day zero" state rather than from defects in working logic, since no working logic exists yet.

## Tech Debt

**No application entry point:**
- Issue: `AndroidManifest.xml` declares only the `<application>` tag with no `<activity>` elements, so the app has no launcher activity and cannot currently be installed/run in any meaningful way.
- Files: `app/src/main/AndroidManifest.xml`
- Impact: The project will build but produce an app with no UI; running it from Android Studio will fail to find a launchable activity.
- Fix approach: Add a `MainActivity` (or navigation host) under `app/src/main/java/com/sed/tachimetro/` and register it with an `intent-filter` for `MAIN`/`LAUNCHER` in the manifest.

**No source code / package directory is empty:**
- Issue: `app/src/main/java/com/sed/tachimetro/` exists as an empty directory tree with nothing inside.
- Files: `app/src/main/java/com/sed/tachimetro/` (empty)
- Impact: There is no domain logic, UI, or business logic implemented yet — this is purely a build-system skeleton.
- Fix approach: Implement the speedometer/tachometer feature set (likely GPS/location-based speed tracking or vehicle sensor integration, per the "Tachimetro" name) starting with a clear architecture (e.g., MVVM) before adding more files.

**Only placeholder/template tests exist:**
- Issue: `app/src/test/java/com/sed/tachimetro/ExampleUnitTest.java` and `app/src/androidTest/java/com/sed/tachimetro/ExampleInstrumentedTest.java` are the stock Android Studio template tests (`assertEquals(4, 2+2)` and a package-name check). No real test coverage exists.
- Files: `app/src/test/java/com/sed/tachimetro/ExampleUnitTest.java`, `app/src/androidTest/java/com/sed/tachimetro/ExampleInstrumentedTest.java`
- Impact: Zero confidence in correctness once real code is added; no regression safety net.
- Fix approach: Delete or replace template tests as real modules are implemented; establish a testing convention (unit tests for view models/use cases, instrumented tests for UI) from the first feature onward.

**Minification disabled in release builds:**
- Issue: `app/build.gradle.kts` sets `isMinifyEnabled = false` for the `release` build type, even though ProGuard files are already wired up.
- Files: `app/build.gradle.kts` (lines 24-30)
- Impact: Release APKs will be larger and unobfuscated; no R8 shrinking/optimization benefit even though the plumbing (`proguard-rules.pro`) is present but empty (all rules commented out).
- Fix approach: Enable `isMinifyEnabled = true` for release once the app has real code, and populate `app/proguard-rules.pro` with keep rules for any reflection-based libraries introduced later.

**No dependency injection, architecture, or navigation libraries declared:**
- Issue: `gradle/libs.versions.toml` only declares `appcompat`, `material`, `junit`, `androidx.test.ext.junit`, and `espresso-core` — the bare minimum added by the Android Studio wizard. No lifecycle/viewmodel, navigation, coroutines, or location/sensor libraries are present.
- Files: `gradle/libs.versions.toml`, `app/build.gradle.kts`
- Impact: Any speedometer functionality (GPS-based speed calculation, sensor fusion, etc.) will require adding `play-services-location` or `LocationManager`/`SensorManager` usage, plus lifecycle-aware components — none of this groundwork exists yet.
- Fix approach: Add dependencies incrementally as features are planned (e.g., `androidx.lifecycle:lifecycle-viewmodel-ktx`, `com.google.android.gms:play-services-location` or plain `android.location`, `androidx.core:core-ktx`) rather than bulk-adding unused libraries.

**Project uses Java-only template even though app name suggests Kotlin is likely intended:**
- Issue: The two template test files are `.java`, and `compileOptions` targets Java 11 with no Kotlin plugin (`org.jetbrains.kotlin.android`) applied in `app/build.gradle.kts` or `build.gradle.kts`.
- Files: `app/build.gradle.kts`, `build.gradle.kts`
- Impact: If the intended implementation language is Kotlin (standard for modern Android development), the Kotlin Gradle plugin, `kotlin-stdlib`, and `kotlinOptions`/`jvmTarget` are not yet configured, and no `.kt` files exist.
- Fix approach: Decide language strategy before adding source files. If Kotlin, add `alias(libs.plugins.kotlin.android)` (or equivalent) to `app/build.gradle.kts` and the corresponding version to `libs.versions.toml`.

## Known Bugs

Not applicable — no functional code exists to exhibit bugs.

## Security Considerations

**`allowBackup="true"` with default data extraction/backup rules:**
- Risk: `AndroidManifest.xml` sets `android:allowBackup="true"` and references `@xml/data_extraction_rules` / `@xml/backup_rules`, both of which are the unmodified Android Studio defaults (no exclusions configured).
- Files: `app/src/main/AndroidManifest.xml`, `app/src/main/res/xml/data_extraction_rules.xml`, `app/src/main/res/xml/backup_rules.xml`
- Current mitigation: None — default templates include no exclude rules.
- Recommendations: Once the app stores any location history, user preferences, or sensitive data, explicitly configure `data_extraction_rules.xml`/`backup_rules.xml` to exclude sensitive files/shared-prefs from cloud backup and device-to-device transfer.

**Anticipated location/sensor permissions not yet modeled:**
- Risk: A speedometer app will almost certainly need `ACCESS_FINE_LOCATION` (and possibly `ACCESS_BACKGROUND_LOCATION` for continuous tracking) or raw sensor access. None of these permissions, runtime permission request flows, or privacy-disclosure UX exist yet.
- Files: `app/src/main/AndroidManifest.xml` (no `<uses-permission>` entries present)
- Current mitigation: None (feature not yet built).
- Recommendations: When location tracking is implemented, request only the minimum permission needed (foreground `ACCESS_FINE_LOCATION` unless background tracking is a hard requirement), implement the Android 6.0+ runtime permission flow, and add a privacy policy if publishing to Play Store (required for location-permission apps).

## Performance Bottlenecks

Not applicable — no functional code exists to profile.

## Fragile Areas

**Entire codebase is pre-implementation:**
- Files: whole `app/src/main/` tree
- Why fragile: Because there is no existing structure, the first set of architectural decisions (package layout, DI approach, threading model for sensor/location updates) will set precedent for everything after. Getting this wrong early is costly to unwind later.
- Safe modification: Establish `.planning/codebase/ARCHITECTURE.md` and `STRUCTURE.md` conventions (via `/gsd:map-codebase` arch focus, or manual planning) before writing the first feature, so subsequent phases build on a deliberate structure rather than ad hoc growth.
- Test coverage: 0% — no real code to cover.

## Scaling Limits

Not applicable at this stage — no runtime behavior to scale.

## Dependencies at Risk

**Android Gradle Plugin 9.1.1 with `compileSdk` 36 (minor 1):**
- Risk: `gradle/libs.versions.toml` pins `agp = "9.1.1"` and `app/build.gradle.kts` targets `compileSdk` version 36 with `minorApiLevel = 1` — very recent/bleeding-edge versions as of this analysis. Such recent AGP/SDK combinations can have compatibility issues with some third-party libraries or Android Studio versions still catching up.
- Impact: Adding third-party dependencies later may surface version-resolution conflicts or require waiting for libraries to publish compatible artifacts.
- Migration plan: When adding new dependencies, verify compatibility with AGP 9.1.1 / compileSdk 36 first; keep the Gradle wrapper (`gradle/wrapper/gradle-wrapper.properties`) version-aligned with AGP requirements.

## Missing Critical Features

**No feature implementation of any kind:**
- Problem: The app has no speed calculation, no sensor/location integration, no UI screens, no data persistence, and no settings — i.e., none of the functionality implied by the "Tachimetro" name exists yet.
- Blocks: All downstream planning phases depend on a first vertical slice (e.g., "display current speed from GPS on a single screen") being implemented before architecture, conventions, or testing patterns can be meaningfully analyzed in future codebase-mapping passes.

**No CI/CD or build verification:**
- Problem: No GitHub Actions/CI config, no lint-check automation, and no pre-commit hooks are present anywhere in the repo.
- Blocks: Regressions can be committed without any automated build/test/lint gate as soon as real code is added.

## Test Coverage Gaps

**100% of prospective functionality is untested:**
- What's not tested: Everything — there is no functionality yet to test beyond the stock template assertions.
- Files: `app/src/test/java/com/sed/tachimetro/ExampleUnitTest.java`, `app/src/androidTest/java/com/sed/tachimetro/ExampleInstrumentedTest.java`
- Risk: If real code is added without accompanying tests, the project starts its life with zero safety net for regressions.
- Priority: High — establish testing conventions (unit test naming, fixtures, mocking approach for `LocationManager`/`SensorManager`) as part of the very first implementation phase, not as an afterthought.

---

*Concerns audit: 2026-07-07*
