package com.sed.tachimetro.distance

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Plain JVM unit tests for [reduceDistance] and [sanitizePersistedDistance] -- no Android runtime.
 * Locks D-04 (no accumulation below the noise floor, avoids drift while the vehicle is stopped)
 * and T-07-01-T/T-07-01-T2 (sanitization of tampered/negative totals and deltas).
 */
class DistanceReducerTest {

    @Test
    fun aboveNoiseFloor_addsDeltaToTotal() {
        // reduceDistance(100f, 14.2f, kmh = 20) == 114.2f -- above threshold: delta is added.
        assertEquals(114.2f, reduceDistance(100f, 14.2f, kmh = 20), 0.001f)
    }

    @Test
    fun belowNoiseFloor_doesNotAddDelta() {
        // reduceDistance(100f, 3.5f, kmh = 1) == 100f -- D-04: below threshold, delta discarded.
        assertEquals(100f, reduceDistance(100f, 3.5f, kmh = 1), 0.001f)
    }

    @Test
    fun zeroKmh_doesNotAddDelta() {
        // reduceDistance(100f, 3.5f, kmh = 0) == 100f -- stopped: mapSpeedToKmh returns exactly 0 below threshold.
        assertEquals(100f, reduceDistance(100f, 3.5f, kmh = 0), 0.001f)
    }

    @Test
    fun exactlyAtNoiseFloorBoundary_addsDelta() {
        // reduceDistance(100f, 5f, kmh = 2) == 105f -- strict comparison: 2 is NOT below the 2.0 threshold.
        assertEquals(105f, reduceDistance(100f, 5f, kmh = 2), 0.001f)
    }

    @Test
    fun negativeCurrentTotal_treatedAsZero() {
        // reduceDistance(-5f, 10f, kmh = 20) == 10f -- negative current total treated as 0.
        assertEquals(10f, reduceDistance(-5f, 10f, kmh = 20), 0.001f)
    }

    @Test
    fun negativeDelta_treatedAsZero() {
        // reduceDistance(100f, -3f, kmh = 20) == 100f -- negative delta treated as 0, defensive.
        assertEquals(100f, reduceDistance(100f, -3f, kmh = 20), 0.001f)
    }

    @Test
    fun sanitizePersistedDistance_validValue_passesThrough() {
        // sanitizePersistedDistance(123.4f) == 123.4f -- valid value passes through unchanged.
        assertEquals(123.4f, sanitizePersistedDistance(123.4f), 0.001f)
    }

    @Test
    fun sanitizePersistedDistance_zero_isValid() {
        // sanitizePersistedDistance(0f) == 0f -- zero is a valid value.
        assertEquals(0f, sanitizePersistedDistance(0f), 0.001f)
    }

    @Test
    fun sanitizePersistedDistance_negativeValue_isClampedToZero() {
        // sanitizePersistedDistance(-1f) == 0f -- T-07-01-T: a tampered/negative persisted value is reset to 0.
        assertEquals(0f, sanitizePersistedDistance(-1f), 0.001f)
    }
}
