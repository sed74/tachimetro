# Coding Conventions

**Analysis Date:** 2026-07-07

## Project State

This is a newly generated Android Studio project (default "Empty Views Activity" / basic template) for package `com.sed.tachimetro`. No custom application code has been written yet — the codebase currently contains only the stock files Android Studio generates when a new project is created (manifest, default resources, and the two boilerplate test classes). There is no `MainActivity`, no custom classes, no Kotlin source files, and no established in-house convention to document yet.

Because there is no first-party source code to observe, the sections below record the project's declared/tooling-level conventions (build config, package naming, Java version) and provide **prescriptive defaults** to follow once real code is added, based on standard Android/Kotlin conventions and the toolchain already configured in this repo.

## Language

- Project is configured as a Java/Android project (`app/build.gradle.kts` has no `kotlin-android` plugin applied, and the only existing sources are `.java` files: `app/src/test/java/com/sed/tachimetro/ExampleUnitTest.java`, `app/src/androidTest/java/com/sed/tachimetro/ExampleInstrumentedTest.java`).
- No Kotlin plugin, `kotlin-stdlib` dependency, or `.kt` files are present. If future work is expected to use Kotlin (common for modern Android apps), the Kotlin Android Gradle plugin must be added to `app/build.gradle.kts` and `gradle/libs.versions.toml` first.
- Java source/target compatibility is pinned to Java 11 in `app/build.gradle.kts:32-35` (`sourceCompatibility`/`targetCompatibility = JavaVersion.VERSION_11`). Any new code must remain compatible with Java 11 language features unless this is bumped.

## Naming Patterns

**Package:**
- Root package: `com.sed.tachimetro` (declared in `app/build.gradle.kts:6` as `namespace` and used consistently in `AndroidManifest.xml` and both test classes).
- All new classes should live under `com.sed.tachimetro` or a sub-package of it (e.g. `com.sed.tachimetro.ui`, `com.sed.tachimetro.data`) — no sub-package structure exists yet, so the first real feature establishes the pattern.

**Files:**
- Test classes follow the stock Android Studio template naming: `Example<Type>Test.java` (`ExampleUnitTest.java`, `ExampleInstrumentedTest.java`). This prefix (`Example`) is a placeholder and should be replaced with the real class-under-test name once actual code exists (e.g. `SpeedCalculatorTest.java`).
- Android resource files follow standard Android naming (`ic_launcher_background.xml`, `colors.xml`, `themes.xml`) under `app/src/main/res/`.

**Classes:**
- PascalCase, matching standard Java/Android convention, as seen in `ExampleUnitTest`, `ExampleInstrumentedTest`.

**Methods:**
- Test methods use `snake_case`-style descriptive names with an underscore separating subject and expectation, e.g. `addition_isCorrect()` in `app/src/test/java/com/sed/tachimetro/ExampleUnitTest.java:14` and `useAppContext()` in `app/src/androidTest/java/com/sed/tachimetro/ExampleInstrumentedTest.java:21`. Follow this `subject_expectedBehavior` pattern for new test methods.
- No production methods exist yet to establish a convention for non-test method naming; default to standard Java camelCase (`doSomething()`).

## Code Style

**Formatting:**
- No `.editorconfig`, checkstyle, ktlint, or detekt configuration is present anywhere in the repo.
- No formatter (Spotless, ktlint, google-java-format) is configured in `app/build.gradle.kts` or `build.gradle.kts`.
- Default Android Studio / IntelliJ Java formatting conventions apply (4-space indentation, braces on same line), as observed in the two existing Java files.

**Linting:**
- No custom lint configuration file (`lint.xml`) or `android.lintOptions` block is present in `app/build.gradle.kts`.
- Only the default Android Gradle Plugin lint checks apply (whatever `com.android.application` runs out of the box).

## Import Organization

- Existing files use plain, ungrouped imports with a blank line separating framework imports from static imports, e.g. in `app/src/androidTest/java/com/sed/tachimetro/ExampleInstrumentedTest.java:3-11`:
  ```java
  import android.content.Context;

  import androidx.test.platform.app.InstrumentationRegistry;
  import androidx.test.ext.junit.runners.AndroidJUnit4;

  import org.junit.Test;
  import org.junit.runner.RunWith;

  import static org.junit.Assert.*;
  ```
- Pattern observed: `android.*` imports first, then `androidx.*`, then third-party (`org.junit.*`), then static imports last, each group separated by a blank line. Follow this grouping for new files.
- No path aliases apply (not applicable to Java/Android).

## Error Handling

- No error-handling code exists in the codebase yet (no try/catch, no custom exceptions, no Result-style wrappers). No convention to document — establish one (e.g. checked vs. unchecked exceptions, `Result`/sealed-class style for Kotlin) when the first real feature is implemented.

## Logging

- No logging framework or `Log.*` calls exist anywhere in the codebase yet. No convention to document.

## Comments

- Existing files use standard Javadoc-style block comments for class-level documentation, e.g. `app/src/test/java/com/sed/tachimetro/ExampleUnitTest.java:7-11`:
  ```java
  /**
   * Example local unit test, which will execute on the development machine (host).
   *
   * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
   */
  ```
- No inline comments exist in production code (none exists). Follow Javadoc conventions for public class/method documentation when added.

## Function Design

- No production functions exist. No size, parameter, or return-value conventions to observe yet.

## Module Design

- Single Gradle module: `app` (declared in `settings.gradle.kts`). No multi-module structure, no `core`/`feature` split.
- No custom Gradle convention plugins beyond the version-catalog-based plugin aliases (`libs.plugins.android.application`) referenced in `app/build.gradle.kts:2`. Dependency versions are centralized via a Gradle version catalog (`gradle/libs.versions.toml` — referenced as `libs.appcompat`, `libs.material`, `libs.junit`, `libs.ext.junit`, `libs.espresso.core` in `app/build.gradle.kts:39-43`). Add new dependencies to the catalog rather than hardcoding versions inline.

---

*Convention analysis: 2026-07-07*
