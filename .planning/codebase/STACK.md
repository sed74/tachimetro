# Technology Stack

**Analysis Date:** 2026-08-22

## Languages

**Primary:**
- Kotlin - Application code, build scripts via Gradle Kotlin DSL (`build.gradle.kts`, `app/build.gradle.kts`, `settings.gradle.kts`)
- Java 11 - Compilation target; legacy test stubs present in `app/src/test/java/` and `app/src/androidTest/java/`

**Secondary:**
- XML - Android resources (layouts, themes, strings, colors) in `app/src/main/res/`
- TOML - Dependency version management in `gradle/libs.versions.toml`

## Runtime

**Environment:**
- Android 11+ (API 30) to Android 16 (API 36)
- Compilation SDK: 36 with minorApiLevel = 1
- Target SDK: 36
- JVM target: Java 11 (`compileOptions.sourceCompatibility/targetCompatibility = VERSION_11`)

**Package Manager:**
- Gradle 9.3.1 (wrapper: `gradle/wrapper/gradle-wrapper.properties`)
- Gradle Kotlin DSL for all build scripts
- Gradle version catalog (centralized dependency versions in `gradle/libs.versions.toml`)
- Toolchain: JDK 21 for Gradle daemon (auto-resolved via foojay-resolver-convention v1.0.0)

## Frameworks

**Core:**
- Android Gradle Plugin 9.1.1 - App compilation, build configuration
- AndroidX AppCompat 1.6.1 - Activity/UI base compatibility layer
- Material Components for Android 1.10.0 - Material Design theming and components
- AndroidX Activity 1.9.3 (activity-ktx) - Activity lifecycle and coroutine integration
- AndroidX ConstraintLayout 2.2.1 - Flexible view layout management
- AndroidX Lifecycle Runtime 2.11.0 - Lifecycle-aware component support

**Runtime Support:**
- Kotlin Coroutines Core 1.10.2 - Asynchronous programming and threading
- Google Play Services Location 21.4.0 - FusedLocationProviderClient for GPS speed data

**Testing:**
- JUnit 4.13.2 - Local unit test framework
- AndroidX Test Ext JUnit 1.1.5 - Instrumented test runner integration
- Espresso Core 3.5.1 - UI/instrumented testing framework
- Test runner: `androidx.test.runner.AndroidJUnitRunner` (configured in `app/build.gradle.kts`)

**Build/Dev:**
- Gradle Kotlin DSL (all `.kts` files)
- org.gradle.toolchains.foojay-resolver-convention v1.0.0 - Auto-provisions JDK toolchains

## Key Dependencies

**Critical:**
- `com.google.android.gms:play-services-location:21.4.0` - Provides FusedLocationProviderClient for real-time GPS speed data; core to app's primary feature
- `androidx.appcompat:appcompat:1.6.1` - Provides Activity and AppCompatActivity base classes needed for any screen
- `com.google.android.material:material:1.10.0` - Material Design theming support

**Runtime Support:**
- `org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2` - Enables async location updates and non-blocking UI
- `androidx.lifecycle:lifecycle-runtime-ktx:2.11.0` - Lifecycle-aware coroutine scoping for location providers
- `androidx.activity:activity-ktx:1.9.3` - Activity extension functions for coroutine support
- `androidx.constraintlayout:constraintlayout:2.2.1` - Flexible view layout for speed display

**Testing:**
- `junit:junit:4.13.2` - Unit test assertions
- `androidx.test.ext:junit:1.1.5` - AndroidX test runner
- `androidx.test.espresso:espresso-core:3.5.1` - Instrumented test UI assertions

## Configuration

**Environment:**
- `local.properties` - Contains local Android SDK path (standard Android Studio file, not version-controlled secrets)
- No `.env` files or custom environment-specific config files
- No `BuildConfig` fields or `buildConfigField` entries defined

**Build:**
- Root build file: `build.gradle.kts` (declares AGP plugin version)
- Module build file: `app/build.gradle.kts` (namespace `com.sed.tachimetro`, versionCode 1, versionName "1.0")
- Settings: `settings.gradle.kts` (single module `:app`; repositories: Google, Maven Central, Gradle Plugin Portal)
- Version catalog: `gradle/libs.versions.toml` - Centralized dependency/plugin versions
- Gradle properties: `gradle.properties` (`-Xmx2048m -Dfile.encoding=UTF-8`; parallel mode disabled)
- Daemon JVM: `gradle/gradle-daemon-jvm.properties` (toolchainVersion=21; foojay URLs for auto-resolution)
- ProGuard rules: `app/proguard-rules.pro` (default template present; `isMinifyEnabled = false` in release build type)

**Android Manifest:**
- Location: `app/src/main/AndroidManifest.xml`
- Permissions: `android.permission.ACCESS_FINE_LOCATION` (fine-grained GPS required for speedometer accuracy; no coarse-location fallback per project constraint)
- Entry point: `.MainActivity` (exported, MAIN/LAUNCHER intent filter)
- Theme: `@style/Theme.Tachimetro`
- Backup/data extraction rules: `@xml/backup_rules`, `@xml/data_extraction_rules` (in `app/src/main/res/xml/`)

**Resources:**
- Theme definitions: `app/src/main/res/values/themes.xml`, `app/src/main/res/values-night/themes.xml`
- Colors: `app/src/main/res/values/colors.xml`
- Strings: `app/src/main/res/values/strings.xml`
- Launcher icons: `app/src/main/res/mipmap-*` directories and `app/src/main/res/drawable/ic_launcher_*.xml` (adaptive launcher icon)

## Platform Requirements

**Development:**
- Android Studio compatible with AGP 9.1.1
- JDK 21 (auto-provisioned via foojay if not locally installed)
- Windows environment (project path: `C:\Users\fedes\AndroidStudioProjects\Tachimetro`); gradlew.bat available for CLI builds

**Production/Runtime:**
- Android device/emulator running Android 11+ (API 30–36)
- Google Play Services installed (required for FusedLocationProviderClient)
- Location permission granted by user at runtime (Android 6+)

**Distribution:**
- Format: Native APK
- Build variants: debug (default), release (ProGuard disabled)
- Package name: `com.sed.tachimetro`
- Version: 1.0 (versionCode 1)

---

*Stack analysis: 2026-08-22*
