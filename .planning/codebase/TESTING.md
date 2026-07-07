# Testing Patterns

**Analysis Date:** 2026-07-07

## Project State

This project contains only the two boilerplate test files that Android Studio scaffolds automatically when creating a new project. Neither test exercises any application logic — they verify arithmetic (`2 + 2 == 4`) and the instrumentation package name. There is no real test suite, no test utilities, no fixtures, and no mocking library configured yet. The sections below document the testing infrastructure that IS present (framework/tooling wired up via Gradle) and provide prescriptive guidance for adding real tests going forward.

## Test Framework

**Runner:**
- JUnit 4 (via `libs.junit` in the Gradle version catalog, referenced at `app/build.gradle.kts:41`: `testImplementation(libs.junit)`).
- Instrumented/on-device tests use AndroidX Test with `AndroidJUnit4` runner (`androidx.test.ext.junit.runners.AndroidJUnit4`), configured via `testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"` in `app/build.gradle.kts:20`.
- No config file beyond the Gradle build script — standard Android Gradle Plugin test tasks (`test`, `connectedAndroidTest`) apply.

**Assertion Library:**
- `org.junit.Assert` static imports (`assertEquals`), as seen in both existing test files. No AssertJ, Truth, or other fluent assertion library is present.

**Run Commands:**
```bash
./gradlew test                    # Run all local (JVM) unit tests
./gradlew testDebugUnitTest       # Run unit tests for debug variant only
./gradlew connectedAndroidTest    # Run instrumented tests on a connected device/emulator
./gradlew test --continue         # Run tests, don't stop at first failure
```
No coverage tool (Jacoco) is configured in `app/build.gradle.kts` or `build.gradle.kts` — no coverage command is available yet.

## Test File Organization

**Location:**
- Local (JVM) unit tests: `app/src/test/java/com/sed/tachimetro/` — run on the host JVM, no Android framework dependencies available.
- Instrumented (on-device) tests: `app/src/androidTest/java/com/sed/tachimetro/` — run on an emulator/device, has access to Android framework and `Context`.
- This mirrors the standard Android Gradle Plugin source-set convention; continue placing tests in these two source sets based on whether they need the Android framework.

**Naming:**
- `<ClassUnderTest>Test.java` pattern, e.g. `ExampleUnitTest.java`, `ExampleInstrumentedTest.java`. Replace the `Example` placeholder with the real subject name for new tests (e.g. `SpeedCalculatorTest.java`, `MainActivityTest.java`).

**Structure:**
```
app/src/
├── test/java/com/sed/tachimetro/          # JVM unit tests
│   └── ExampleUnitTest.java
└── androidTest/java/com/sed/tachimetro/   # Instrumented/on-device tests
    └── ExampleInstrumentedTest.java
```

## Test Structure

**Suite Organization (current pattern from codebase):**

`app/src/test/java/com/sed/tachimetro/ExampleUnitTest.java`:
```java
package com.sed.tachimetro;

import org.junit.Test;
import static org.junit.Assert.*;

public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }
}
```

`app/src/androidTest/java/com/sed/tachimetro/ExampleInstrumentedTest.java`:
```java
package com.sed.tachimetro;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.sed.tachimetro", appContext.getPackageName());
    }
}
```

**Patterns:**
- Instrumented tests are annotated with `@RunWith(AndroidJUnit4.class)`; plain unit tests have no class-level runner annotation (default JUnit4 runner).
- No `@Before`/`@After` setup/teardown methods exist yet in either file — no established fixture-lifecycle pattern.
- Method-naming convention for test cases: `subject_expectedOutcome()` (e.g. `addition_isCorrect`, `useAppContext`).

## Mocking

**Framework:** None configured. No Mockito, MockK, or similar dependency exists in `app/build.gradle.kts`.

**Recommendation for new tests:** Add `testImplementation(libs.mockito.core)` (or Mockito-Kotlin/MockK if Kotlin is introduced) to the version catalog and `app/build.gradle.kts` `dependencies` block before writing tests that need to isolate collaborators. No existing mocking pattern to follow — establish one with the first non-trivial unit test.

## Fixtures and Factories

- No test data builders, factories, or fixture files exist anywhere in the repo. No `resources` test directory (`app/src/test/resources` or `app/src/androidTest/assets`) is present.

## Coverage

**Requirements:** None enforced. No Jacoco plugin, no coverage threshold configuration in `app/build.gradle.kts` or root `build.gradle.kts`.

**View Coverage:**
Not available until a coverage tool is added. To enable, apply the Jacoco plugin in `app/build.gradle.kts` and configure a `jacocoTestReport` task.

## Test Types

**Unit Tests:**
- Scope: host-JVM tests under `app/src/test/`, currently a single trivial arithmetic assertion (`ExampleUnitTest.addition_isCorrect`). No real application logic is under test.

**Integration Tests:**
- None present. No test currently exercises multiple collaborating classes.

**Instrumented/E2E Tests:**
- Scope: on-device tests under `app/src/androidTest/`, using AndroidX Test (`androidx.test.ext:junit`, `androidx.test.espresso:espresso-core` per `app/build.gradle.kts:42-43`). Currently only verifies the application package name (`ExampleInstrumentedTest.useAppContext`). Espresso is available as a dependency but no UI interaction test has been written yet (no `MainActivity` exists to test against).

## Common Patterns

**Async Testing:**
- Not applicable yet — no asynchronous code exists in the codebase.

**Error Testing:**
- Not applicable yet — no exception-throwing code exists to test. When added, use `assertThrows` (JUnit 4.13+, available via the `libs.junit` dependency) rather than manual try/catch-fail patterns.

---

*Testing analysis: 2026-07-07*
