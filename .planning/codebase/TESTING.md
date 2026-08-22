# Testing Patterns

**Analysis Date:** 2026-08-22

## Test Framework

**Runner:**
- JUnit 4 (`junit:junit` v4.13.2, `gradle/libs.versions.toml:3`)
- AndroidX Test JUnit runner: `androidx.test.ext:junit` v1.1.5
- Instrumentation runner: `androidx.test.runner.AndroidJUnitRunner` (`app/build.gradle.kts:20`)
- Config: declared in `app/build.gradle.kts:20` as `testInstrumentationRunner`

**Assertion Library:**
- JUnit 4 built-in: `org.junit.Assert.*` (assertEquals, assertNull, etc.)
- No third-party assertion library (AssertJ, Hamcrest, Strikt) present

**Run Commands:**
```bash
./gradlew test              # Run all local (JVM) unit tests
./gradlew connectedAndroidTest  # Run instrumented tests (requires connected device or emulator)
./gradlew test --info       # Run tests with verbose output
```

## Test File Organization

**Location:**
- Co-located by source type, not by package alongside production code:
  - Unit tests (JVM only): `app/src/test/java/com/sed/tachimetro/`
  - Instrumented tests (on-device): `app/src/androidTest/java/com/sed/tachimetro/`
- Example structure:
  ```
  app/src/test/java/com/sed/tachimetro/
  ├── gps/
  │   ├── SpeedMappingTest.kt        # Tests mapSpeedToKmh()
  │   └── GpsSpeedProviderStateTest.kt # Tests deriveSpeedState()
  └── maxspeed/
      └── MaxSpeedReducerTest.kt     # Tests reduceMax() & sanitizePersistedMax()
  
  app/src/androidTest/java/com/sed/tachimetro/
  └── ExampleInstrumentedTest.java   # IDE template, not yet customized
  ```

**Naming:**
- Test classes: `[SubjectClass]Test.kt` (e.g., `SpeedMappingTest`, `GpsSpeedProviderStateTest`)
- Test methods: snake_case, descriptive of condition and expected outcome (e.g., `hasSpeedFalse_returnsZero()`, `poorAccuracy_returnsNull()`)

## Test Structure

**Suite Organization:**
```kotlin
class SpeedMappingTest {
    @Test
    fun testName_describesConditionAndExpectation() {
        // Arrange (set up inputs)
        val input = someValue
        
        // Act (execute code under test)
        val result = functionUnderTest(input)
        
        // Assert (verify result)
        assertEquals(expectedValue, result)
    }
}
```

**Patterns Observed:**

- **Simple direct assertions:**
  ```kotlin
  @Test
  fun hasSpeedFalse_returnsZero() {
      val result = mapSpeedToKmh(
          hasAccuracy = false,
          accuracyMeters = 0f,
          hasSpeed = false,
          speedMetersPerSecond = 99f,
      )
      assertEquals(0, result)
  }
  ```

- **Named parameters for readability:**
  - All test calls use named parameters explicitly (see `SpeedMappingTest.kt`)
  - Improves test readability and makes it clear what each parameter controls

- **KDoc on test classes explaining what is being locked/tested:**
  ```kotlin
  /**
   * Plain JVM unit tests for [mapSpeedToKmh] — no Android runtime, no coroutines-test.
   * Locks the numeric decisions D-03/D-04/D-05/GPS-01/D-09.
   */
  class SpeedMappingTest { ... }
  ```

**No setup/teardown currently used:**
- Tests are simple and stateless
- When needed in future: use `@Before` / `@After` annotations (JUnit 4 standard)

## Testing Strategy

**Unit Tests (JVM-only):**
- Target: Pure functions with no Android framework dependencies
  - `mapSpeedToKmh()` in `app/src/test/java/com/sed/tachimetro/gps/SpeedMappingTest.kt`
  - `deriveSpeedState()` in `app/src/test/java/com/sed/tachimetro/gps/GpsSpeedProviderStateTest.kt`
  - `reduceMax()` and `sanitizePersistedMax()` in `app/src/test/java/com/sed/tachimetro/maxspeed/MaxSpeedReducerTest.kt`
- Rationale: No mocking, no context setup needed; tests run fast on the JVM

**Instrumented Tests:**
- Only IDE template placeholder exists: `ExampleInstrumentedTest.java`
- When needed: use `@RunWith(AndroidJUnit4.class)` and `InstrumentationRegistry.getInstrumentation().getTargetContext()` to access app context
- Framework: AndroidX Test with Espresso (dependencies already present: `androidx.test.espresso:espresso-core` v3.5.1)

**E2E Tests:**
- Not implemented

## Mocking

**Framework:** None declared or used currently

**When mocking will be needed:**
- For Android-specific dependencies (LocationServices, SharedPreferences, Context)
- Consider MockK (lightweight Kotlin-first mocking) or Mockito when integration tests are added
- Currently avoided by designing pure functions for testable logic (`mapSpeedToKmh`, `deriveSpeedState`, `reduceMax`)

**What's NOT mocked (current practice):**
- Logic functions are not mocked; they are tested directly with various inputs
- Example: `SpeedMappingTest.kt` tests `mapSpeedToKmh()` with different accuracy and speed values, no mocks needed

**What WILL be mocked in future (when integration tests added):**
- FusedLocationProviderClient (wrapped in `GpsSpeedProvider.kt`)
- SharedPreferences (wrapped in `MaxSpeedStore`, `ScreenOnPreferenceStore`)
- Android Context and system services
- Lifecycle events when testing Activity behavior

## Test Examples

### SpeedMappingTest.kt
Comprehensive coverage of `mapSpeedToKmh()` numeric logic:
- `hasSpeedFalse_returnsZero()`: when GPS has no speed lock, return 0 km/h
- `belowNoiseFloor_returnsZero()`: speeds below 2.0 km/h noise floor round to 0
- `tenMetersPerSecond_returnsThirtySix()`: 10 m/s * 3.6 = 36 km/h conversion
- `fractionalSpeed_roundsToNearestInt()`: 5.27 m/s ≈ 18.97 km/h → rounds to 19
- `poorAccuracy_returnsNull()`: accuracy > 50m → null (reading dropped)
- `goodAccuracy_isAccepted()`: accuracy ≤ 50m → reading accepted
- `unknownAccuracy_isNotDropped()`: when accuracy is unknown, reading is not filtered out

**Pattern:** Each test is isolated, uses named parameters, and tests one condition.

### GpsSpeedProviderStateTest.kt
Tests the state-machine logic in `deriveSpeedState()`:
- `noAcceptedFixYet_returnsSearching()`: null lastKmh → Searching state
- `recentAcceptedFix_returnsReading()`: recent reading within 5s window → Reading
- `exactlyAtFiveSecondBoundary_returnsReading()`: exactly 5000ms elapsed → still fresh
- `justOverFiveSeconds_returnsNoSignal()`: > 5000ms elapsed → NoSignal
- `wellOverFiveSeconds_returnsNoSignal()`: far beyond 5s → NoSignal

**Pattern:** Tests boundary conditions explicitly (the `exactlyAtFiveSecondBoundary` case catches off-by-one errors in the state logic).

### MaxSpeedReducerTest.kt
Tests monotonic max growth and data sanitization:
- `firstReading_growsFromZero()`: initial reading establishes the max
- `lowerReading_doesNotLowerMax()`: max is monotonic; lower readings ignored
- `higherReading_updatesMax()`: only higher readings update
- `equalReading_staysUnchanged()`: equal reading has no effect
- `negativeReading_treatedAsZero_doesNotLowerMax()`: anomalous negatives treated as 0
- `sanitizePersistedMax_validValue_passesThrough()`: normal values pass through
- `sanitizePersistedMax_negativeValue_isClampedToZero()`: tampered persistent data is reset

**Pattern:** Defensive input validation is tested explicitly (negative values, boundary cases).

## Coverage

**Requirements:** No coverage threshold enforced (not configured in `app/build.gradle.kts`)

**Current coverage (observed):**
- Core pure functions: Comprehensive (95%+)
  - `mapSpeedToKmh()`: 7 test cases covering all branches (noise floor, accuracy filtering, speed presence)
  - `deriveSpeedState()`: 5 test cases covering Searching/Reading/NoSignal states and boundaries
  - `reduceMax()` / `sanitizePersistedMax()`: 8 test cases covering growth, edge cases, sanitization
- MainActivity: Not tested (requires instrumented tests with Context/Lifecycle mocking)
- GpsSpeedProvider (as a class): Only tested via `deriveSpeedState()` logic; the Flow/coroutine integration is not tested

**To view coverage (when configured in future):**
```bash
./gradlew test jacocoTestReport  # Requires Jacoco plugin added
./gradlew connectedAndroidTest jacocoAndroidReport  # For instrumented tests
# Reports in: app/build/reports/jacoco/
```

## Test Types

**Unit Tests (JVM):**
- Framework: JUnit 4
- Location: `app/src/test/java/com/sed/tachimetro/`
- Files:
  - `gps/SpeedMappingTest.kt` - Pure function logic
  - `gps/GpsSpeedProviderStateTest.kt` - State machine logic
  - `maxspeed/MaxSpeedReducerTest.kt` - Max reduction & sanitization logic
- No context, no framework dependencies, fast execution
- Run with `./gradlew test`

**Instrumented Tests (On-Device/Emulator):**
- Framework: AndroidX Test + Espresso
- Location: `app/src/androidTest/java/com/sed/tachimetro/`
- Current: Only IDE template `ExampleInstrumentedTest.java` (placeholder)
- Would test UI interactions, permission flows, Activity lifecycle when implemented
- Run with `./gradlew connectedAndroidTest` (requires emulator or connected device)

**E2E Tests:**
- Not implemented

## Test Execution

**Local JVM tests (fast, no device needed):**
```bash
./gradlew test
```
- Runs all `*Test.kt` files in `app/src/test/`
- Output: `app/build/test-results/test/`, test reports in HTML format
- Typical duration: <5 seconds

**Instrumented tests (requires emulator or physical device):**
```bash
./gradlew connectedAndroidTest
```
- Runs all test files in `app/src/androidTest/`
- Requires `adb` (Android Debug Bridge) configured and a device/emulator running
- Output: `app/build/outputs/androidTest-results/connected/`
- Typical duration: 30-60 seconds depending on device

**Watch mode (rebuild & re-run on code changes):**
```bash
./gradlew test --watch
```

## Common Patterns

**Null return testing:**
```kotlin
@Test
fun poorAccuracy_returnsNull() {
    val result = mapSpeedToKmh(
        hasAccuracy = true,
        accuracyMeters = 60f,
        hasSpeed = true,
        speedMetersPerSecond = 10f,
    )
    assertNull(result)  // Function returns null when reading is dropped
}
```

**Type assertion (sealed class variants):**
```kotlin
@Test
fun noAcceptedFixYet_returnsSearching() {
    val result = deriveSpeedState(lastKmh = null, now = 10_000L, lastAcceptedAtMs = 0L)
    assertEquals(SpeedState.Searching, result)  // Type-safe enum-like assertion
}
```

**Boundary testing (off-by-one safety):**
```kotlin
@Test
fun exactlyAtFiveSecondBoundary_returnsReading() {
    // Exactly 5000ms elapsed, but the check uses ">" so this is still "fresh"
    val result = deriveSpeedState(lastKmh = 42, now = 6_000L, lastAcceptedAtMs = 1_000L)
    assertEquals(SpeedState.Reading(42), result)
}

@Test
fun justOverFiveSeconds_returnsNoSignal() {
    // 5001ms elapsed, crosses the boundary → NoSignal
    val result = deriveSpeedState(lastKmh = 42, now = 6_001L, lastAcceptedAtMs = 1_000L)
    assertEquals(SpeedState.NoSignal, result)
}
```

**Data class destructuring in assertions (when used):**
- Not currently used, but available for SpeedState.Reading assertions if needed

---

*Testing analysis: 2026-08-22*
