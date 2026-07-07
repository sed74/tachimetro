# Technology Stack

**Analysis Date:** 2026-07-07

## Languages

**Primary:**
- Kotlin - Gradle build scripts (`build.gradle.kts`, `app/build.gradle.kts`, `settings.gradle.kts`) via Kotlin DSL. No `.kt` application source files exist yet under `app/src/main/`.

**Secondary:**
- Java 11 - Test sources: `app/src/test/java/com/sed/tachimetro/ExampleUnitTest.java`, `app/src/androidTest/java/com/sed/tachimetro/ExampleInstrumentedTest.java` (default template test stubs, not yet customized).

**Note:** `app/src/main/` contains only Android resources (`res/`) and `AndroidManifest.xml` — no Kotlin/Java application code (no `MainActivity`, no packages under `app/src/main/java/`). This is an unmodified/near-unmodified Android Studio project scaffold.

## Runtime

**Environment:**
- Android (native APK), targeting Android SDK 36 (compileSdk/targetSdk), minSdk 30 (Android 11+).
- JVM target: Java 11 (`compileOptions.sourceCompatibility/targetCompatibility = VERSION_11` in `app/build.gradle.kts`).
- Gradle JVM toolchain: Java 21 (`gradle/gradle-daemon-jvm.properties`, `toolchainVersion=21`).

**Package Manager:**
- Gradle 9.3.1 (`gradle/wrapper/gradle-wrapper.properties`, `distributionUrl` → gradle-9.3.1-bin.zip)
- Lockfile: none (Gradle version catalog used instead — see below); no `gradle.lockfile`.
- Dependency versions centralized in `gradle/libs.versions.toml` (Gradle Version Catalog).

## Frameworks

**Core:**
- Android Gradle Plugin (AGP) 9.1.1 (`com.android.application`, applied in `app/build.gradle.kts` via `libs.plugins.android.application`)
- AndroidX AppCompat 1.6.1 (`androidx.appcompat:appcompat`) - base Activity/UI compatibility
- Material Components for Android 1.10.0 (`com.google.android.material:material`) - UI theming/components

**Testing:**
- JUnit 4.13.2 (`junit:junit`) - local unit tests (`testImplementation`)
- AndroidX Test Ext JUnit 1.1.5 (`androidx.test.ext:junit`) - instrumented test runner integration
- Espresso Core 3.5.1 (`androidx.test.espresso:espresso-core`) - UI/instrumented testing
- Test instrumentation runner: `androidx.test.runner.AndroidJUnitRunner` (set in `app/build.gradle.kts`)

**Build/Dev:**
- Gradle Kotlin DSL (`.kts` build files) throughout (`build.gradle.kts`, `app/build.gradle.kts`, `settings.gradle.kts`)
- `org.gradle.toolchains.foojay-resolver-convention` plugin v1.0.0 (`settings.gradle.kts`) - auto-provisions JDK toolchains
- ProGuard (config present but disabled): `app/proguard-rules.pro`, `isMinifyEnabled = false` in release build type (`app/build.gradle.kts`)

## Key Dependencies

**Critical:**
- None beyond AndroidX AppCompat and Material — this is a minimal template dependency set. No networking, database, DI, or reactive libraries (e.g., Retrofit, Room, Hilt, Coroutines, Compose) are declared in `gradle/libs.versions.toml` or `app/build.gradle.kts`.

**Infrastructure:**
- Not applicable — no infrastructure-related dependencies present.

## Configuration

**Environment:**
- No `.env` files detected.
- `local.properties` exists at repo root (standard Android Studio file for local SDK path; not read per policy — contains local machine config, not secrets in typical setups).
- No custom `BuildConfig` fields or `buildConfigField` entries defined.

**Build:**
- Root build file: `build.gradle.kts` (applies AGP plugin at root, `apply false`)
- Module build file: `app/build.gradle.kts` (namespace `com.sed.tachimetro`, applicationId `com.sed.tachimetro`, versionCode 1, versionName "1.0")
- Settings: `settings.gradle.kts` (single module `:app`; repositories: Google, Maven Central, Gradle Plugin Portal)
- Version catalog: `gradle/libs.versions.toml`
- Gradle properties: `gradle.properties` (JVM args `-Xmx2048m -Dfile.encoding=UTF-8`; parallel mode commented out)
- ProGuard rules: `app/proguard-rules.pro` (default template, no custom keep rules)
- Android manifest: `app/src/main/AndroidManifest.xml` (no permissions declared, no activities/services/receivers registered)

## Platform Requirements

**Development:**
- Android Studio with AGP 9.1.1 support
- JDK 21 for Gradle daemon/toolchain (auto-resolved via foojay-resolver-convention if not locally available)
- Windows environment (project path `C:\Users\fedes\AndroidStudioProjects\Tachimetro`), using `gradlew.bat` for CLI builds

**Production:**
- Target: Android devices/emulators running Android 11 (API 30) through Android 16 (API 36)
- Distribution: Native APK (no Play Store metadata, no App Bundle config beyond AGP defaults)

---

*Stack analysis: 2026-07-07*
