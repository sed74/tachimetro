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
- Kotlin - Gradle build scripts (`build.gradle.kts`, `app/build.gradle.kts`, `settings.gradle.kts`) via Kotlin DSL. No `.kt` application source files exist yet under `app/src/main/`.
- Java 11 - Test sources: `app/src/test/java/com/sed/tachimetro/ExampleUnitTest.java`, `app/src/androidTest/java/com/sed/tachimetro/ExampleInstrumentedTest.java` (default template test stubs, not yet customized).
## Runtime
- Android (native APK), targeting Android SDK 36 (compileSdk/targetSdk), minSdk 30 (Android 11+).
- JVM target: Java 11 (`compileOptions.sourceCompatibility/targetCompatibility = VERSION_11` in `app/build.gradle.kts`).
- Gradle JVM toolchain: Java 21 (`gradle/gradle-daemon-jvm.properties`, `toolchainVersion=21`).
- Gradle 9.3.1 (`gradle/wrapper/gradle-wrapper.properties`, `distributionUrl` → gradle-9.3.1-bin.zip)
- Lockfile: none (Gradle version catalog used instead — see below); no `gradle.lockfile`.
- Dependency versions centralized in `gradle/libs.versions.toml` (Gradle Version Catalog).
## Frameworks
- Android Gradle Plugin (AGP) 9.1.1 (`com.android.application`, applied in `app/build.gradle.kts` via `libs.plugins.android.application`)
- AndroidX AppCompat 1.6.1 (`androidx.appcompat:appcompat`) - base Activity/UI compatibility
- Material Components for Android 1.10.0 (`com.google.android.material:material`) - UI theming/components
- JUnit 4.13.2 (`junit:junit`) - local unit tests (`testImplementation`)
- AndroidX Test Ext JUnit 1.1.5 (`androidx.test.ext:junit`) - instrumented test runner integration
- Espresso Core 3.5.1 (`androidx.test.espresso:espresso-core`) - UI/instrumented testing
- Test instrumentation runner: `androidx.test.runner.AndroidJUnitRunner` (set in `app/build.gradle.kts`)
- Gradle Kotlin DSL (`.kts` build files) throughout (`build.gradle.kts`, `app/build.gradle.kts`, `settings.gradle.kts`)
- `org.gradle.toolchains.foojay-resolver-convention` plugin v1.0.0 (`settings.gradle.kts`) - auto-provisions JDK toolchains
- ProGuard (config present but disabled): `app/proguard-rules.pro`, `isMinifyEnabled = false` in release build type (`app/build.gradle.kts`)
## Key Dependencies
- None beyond AndroidX AppCompat and Material — this is a minimal template dependency set. No networking, database, DI, or reactive libraries (e.g., Retrofit, Room, Hilt, Coroutines, Compose) are declared in `gradle/libs.versions.toml` or `app/build.gradle.kts`.
- Not applicable — no infrastructure-related dependencies present.
## Configuration
- No `.env` files detected.
- `local.properties` exists at repo root (standard Android Studio file for local SDK path; not read per policy — contains local machine config, not secrets in typical setups).
- No custom `BuildConfig` fields or `buildConfigField` entries defined.
- Root build file: `build.gradle.kts` (applies AGP plugin at root, `apply false`)
- Module build file: `app/build.gradle.kts` (namespace `com.sed.tachimetro`, applicationId `com.sed.tachimetro`, versionCode 1, versionName "1.0")
- Settings: `settings.gradle.kts` (single module `:app`; repositories: Google, Maven Central, Gradle Plugin Portal)
- Version catalog: `gradle/libs.versions.toml`
- Gradle properties: `gradle.properties` (JVM args `-Xmx2048m -Dfile.encoding=UTF-8`; parallel mode commented out)
- ProGuard rules: `app/proguard-rules.pro` (default template, no custom keep rules)
- Android manifest: `app/src/main/AndroidManifest.xml` (no permissions declared, no activities/services/receivers registered)
## Platform Requirements
- Android Studio with AGP 9.1.1 support
- JDK 21 for Gradle daemon/toolchain (auto-resolved via foojay-resolver-convention if not locally available)
- Windows environment (project path `C:\Users\fedes\AndroidStudioProjects\Tachimetro`), using `gradlew.bat` for CLI builds
- Target: Android devices/emulators running Android 11 (API 30) through Android 16 (API 36)
- Distribution: Native APK (no Play Store metadata, no App Bundle config beyond AGP defaults)
<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->
## Conventions

## Project State
## Language
- Project is configured as a Java/Android project (`app/build.gradle.kts` has no `kotlin-android` plugin applied, and the only existing sources are `.java` files: `app/src/test/java/com/sed/tachimetro/ExampleUnitTest.java`, `app/src/androidTest/java/com/sed/tachimetro/ExampleInstrumentedTest.java`).
- No Kotlin plugin, `kotlin-stdlib` dependency, or `.kt` files are present. If future work is expected to use Kotlin (common for modern Android apps), the Kotlin Android Gradle plugin must be added to `app/build.gradle.kts` and `gradle/libs.versions.toml` first.
- Java source/target compatibility is pinned to Java 11 in `app/build.gradle.kts:32-35` (`sourceCompatibility`/`targetCompatibility = JavaVersion.VERSION_11`). Any new code must remain compatible with Java 11 language features unless this is bumped.
## Naming Patterns
- Root package: `com.sed.tachimetro` (declared in `app/build.gradle.kts:6` as `namespace` and used consistently in `AndroidManifest.xml` and both test classes).
- All new classes should live under `com.sed.tachimetro` or a sub-package of it (e.g. `com.sed.tachimetro.ui`, `com.sed.tachimetro.data`) — no sub-package structure exists yet, so the first real feature establishes the pattern.
- Test classes follow the stock Android Studio template naming: `Example<Type>Test.java` (`ExampleUnitTest.java`, `ExampleInstrumentedTest.java`). This prefix (`Example`) is a placeholder and should be replaced with the real class-under-test name once actual code exists (e.g. `SpeedCalculatorTest.java`).
- Android resource files follow standard Android naming (`ic_launcher_background.xml`, `colors.xml`, `themes.xml`) under `app/src/main/res/`.
- PascalCase, matching standard Java/Android convention, as seen in `ExampleUnitTest`, `ExampleInstrumentedTest`.
- Test methods use `snake_case`-style descriptive names with an underscore separating subject and expectation, e.g. `addition_isCorrect()` in `app/src/test/java/com/sed/tachimetro/ExampleUnitTest.java:14` and `useAppContext()` in `app/src/androidTest/java/com/sed/tachimetro/ExampleInstrumentedTest.java:21`. Follow this `subject_expectedBehavior` pattern for new test methods.
- No production methods exist yet to establish a convention for non-test method naming; default to standard Java camelCase (`doSomething()`).
## Code Style
- No `.editorconfig`, checkstyle, ktlint, or detekt configuration is present anywhere in the repo.
- No formatter (Spotless, ktlint, google-java-format) is configured in `app/build.gradle.kts` or `build.gradle.kts`.
- Default Android Studio / IntelliJ Java formatting conventions apply (4-space indentation, braces on same line), as observed in the two existing Java files.
- No custom lint configuration file (`lint.xml`) or `android.lintOptions` block is present in `app/build.gradle.kts`.
- Only the default Android Gradle Plugin lint checks apply (whatever `com.android.application` runs out of the box).
## Import Organization
- Existing files use plain, ungrouped imports with a blank line separating framework imports from static imports, e.g. in `app/src/androidTest/java/com/sed/tachimetro/ExampleInstrumentedTest.java:3-11`:
- Pattern observed: `android.*` imports first, then `androidx.*`, then third-party (`org.junit.*`), then static imports last, each group separated by a blank line. Follow this grouping for new files.
- No path aliases apply (not applicable to Java/Android).
## Error Handling
- No error-handling code exists in the codebase yet (no try/catch, no custom exceptions, no Result-style wrappers). No convention to document — establish one (e.g. checked vs. unchecked exceptions, `Result`/sealed-class style for Kotlin) when the first real feature is implemented.
## Logging
- No logging framework or `Log.*` calls exist anywhere in the codebase yet. No convention to document.
## Comments
- Existing files use standard Javadoc-style block comments for class-level documentation, e.g. `app/src/test/java/com/sed/tachimetro/ExampleUnitTest.java:7-11`:
- No inline comments exist in production code (none exists). Follow Javadoc conventions for public class/method documentation when added.
## Function Design
- No production functions exist. No size, parameter, or return-value conventions to observe yet.
## Module Design
- Single Gradle module: `app` (declared in `settings.gradle.kts`). No multi-module structure, no `core`/`feature` split.
- No custom Gradle convention plugins beyond the version-catalog-based plugin aliases (`libs.plugins.android.application`) referenced in `app/build.gradle.kts:2`. Dependency versions are centralized via a Gradle version catalog (`gradle/libs.versions.toml` — referenced as `libs.appcompat`, `libs.material`, `libs.junit`, `libs.ext.junit`, `libs.espresso.core` in `app/build.gradle.kts:39-43`). Add new dependencies to the catalog rather than hardcoding versions inline.
<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->
## Architecture

## System Overview
```text
```
## Component Responsibilities
| Component | Responsibility | File |
|-----------|----------------|------|
| Root Gradle build | Declares the AGP plugin version, applies no application logic itself | `build.gradle.kts` |
| App module build | Configures `com.sed.tachimetro` application module: SDK versions, Java 11 compatibility, dependencies | `app/build.gradle.kts` |
| Version catalog | Centralizes dependency versions (AGP, JUnit, Espresso, AppCompat, Material) | `gradle/libs.versions.toml` |
| App manifest | Declares application-level attributes (icon, label, theme, backup rules); currently declares zero components (no activities/services) | `app/src/main/AndroidManifest.xml` |
| Main source root | Intended home for all production Java/Kotlin classes; currently empty | `app/src/main/java/com/sed/tachimetro/` |
| Resources | Launcher icons, base theme (`Theme.Tachimetro`), color palette, string table (only `app_name` defined) | `app/src/main/res/` |
| Unit tests | JVM-only test source set; contains only the IDE-generated placeholder test | `app/src/test/java/com/sed/tachimetro/ExampleUnitTest.java` |
| Instrumented tests | On-device/emulator test source set; contains only the IDE-generated placeholder test | `app/src/androidTest/java/com/sed/tachimetro/ExampleInstrumentedTest.java` |
## Pattern Overview
- Single Gradle module (`:app`) — no `:core`, `:data`, `:feature` modules exist
- Java-based (not Kotlin) — `app/build.gradle.kts` sets `sourceCompatibility`/`targetCompatibility` to Java 11 and all existing `.java` files use Java syntax; no Kotlin plugin is applied
- No architectural layers implemented (no MVC/MVVM/MVI, no ViewModel, no Repository, no DI framework)
- No third-party libraries beyond AppCompat, Material Components, JUnit, and Espresso (see `gradle/libs.versions.toml`)
- No navigation component, no Jetpack Compose — the project is set up for traditional View-based UI (implied by `appcompat` + `material` deps) but no layouts or activities exist yet
## Layers
- Purpose: Declares module structure, SDK targets, and dependency versions
- Location: `build.gradle.kts`, `app/build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`
- Contains: Gradle Kotlin DSL scripts, version catalog TOML
- Depends on: Nothing (root of the build graph)
- Used by: Gradle build/compile/test tasks
- Purpose: Declares application metadata and static resources
- Location: `app/src/main/AndroidManifest.xml`, `app/src/main/res/`
- Contains: Theme XML (`values/themes.xml`, `values-night/themes.xml`), colors (`values/colors.xml`), strings (`values/strings.xml`), launcher icon assets (`mipmap-*`, `drawable/ic_launcher_*.xml`), backup/data-extraction rules (`xml/backup_rules.xml`, `xml/data_extraction_rules.xml`)
- Depends on: Nothing
- Used by: The (currently nonexistent) application code, and the OS at install/runtime for app metadata
- Purpose: Intended to hold all Java/Kotlin application code
- Location: `app/src/main/java/com/sed/tachimetro/`
- Contains: Nothing currently — directory exists but has zero files
- Depends on: AppCompat, Material Components (declared as dependencies but unused)
- Used by: N/A
- Purpose: JVM unit tests (`test`) and instrumented/on-device tests (`androidTest`)
- Location: `app/src/test/java/com/sed/tachimetro/`, `app/src/androidTest/java/com/sed/tachimetro/`
- Contains: Single IDE-generated placeholder test class each (`ExampleUnitTest.java`, `ExampleInstrumentedTest.java`)
- Depends on: JUnit4, AndroidX Test (Espresso, ext-junit)
- Used by: `./gradlew test` and `./gradlew connectedAndroidTest` respectively
## Data Flow
### Primary Request Path
- Not applicable — no state-holding classes exist yet.
## Key Abstractions
## Entry Points
- Not declared. `app/src/main/AndroidManifest.xml` has an `<application>` tag with no child `<activity>`, `<service>`, or `<receiver>` elements, and no `android:name` custom Application class.
- `./gradlew assembleDebug` / `./gradlew assembleRelease` — compiles the (currently empty) app module per `app/build.gradle.kts` build type definitions (`release` block only; debug uses AGP defaults)
- `./gradlew test` — runs `app/src/test/java/com/sed/tachimetro/ExampleUnitTest.java`
- `./gradlew connectedAndroidTest` — runs `app/src/androidTest/java/com/sed/tachimetro/ExampleInstrumentedTest.java`
## Architectural Constraints
- **Threading:** No threading model established — no background work, coroutines, or executors exist yet.
- **Global state:** None — no singletons, no `Application` subclass, no static mutable state.
- **Circular imports:** None possible — there is only one (empty) package.
- **Min/target SDK:** `minSdk = 30` (Android 11), `targetSdk = 36`, `compileSdk = 36` (with `minorApiLevel = 1`) as set in `app/build.gradle.kts`. Any new code must remain compatible with API 30 as the floor.
- **Language:** Java 11 source/target compatibility is configured; no Kotlin plugin is applied in `app/build.gradle.kts`, so introducing Kotlin files would require adding `id("org.jetbrains.kotlin.android")` to the plugins block first.
## Anti-Patterns
## Error Handling
## Cross-Cutting Concerns
<!-- GSD:architecture-end -->

<!-- GSD:skills-start source:skills/ -->
## Project Skills

No project skills found. Add skills to any of: `.claude/skills/`, `.agents/skills/`, `.cursor/skills/`, `.github/skills/`, or `.codex/skills/` with a `SKILL.md` index file.
<!-- GSD:skills-end -->

<!-- GSD:workflow-start source:GSD defaults -->
## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:
- `/gsd-quick` for small fixes, doc updates, and ad-hoc tasks
- `/gsd-debug` for investigation and bug fixing
- `/gsd-execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.
<!-- GSD:workflow-end -->



<!-- GSD:profile-start -->
## Developer Profile

> Profile not yet configured. Run `/gsd-profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.
<!-- GSD:profile-end -->
