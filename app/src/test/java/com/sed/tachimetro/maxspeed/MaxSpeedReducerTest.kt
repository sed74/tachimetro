package com.sed.tachimetro.maxspeed

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Plain JVM unit tests for [reduceMax] and [sanitizePersistedMax] -- no Android runtime.
 * Locks D-07 (monotonic growth of the session max) and T-04-01 (sanitization of a
 * tampered/negative persisted value).
 */
class MaxSpeedReducerTest {

    @Test
    fun firstReading_growsFromZero() {
        // reduceMax(0, 50) == 50 -- first record: grows from 0.
        assertEquals(50, reduceMax(0, 50))
    }

    @Test
    fun lowerReading_doesNotLowerMax() {
        // reduceMax(120, 80) == 120 -- D-07: a lower reading must not lower the max.
        assertEquals(120, reduceMax(120, 80))
    }

    @Test
    fun higherReading_updatesMax() {
        // reduceMax(120, 121) == 121 -- a reading one unit higher updates the max.
        assertEquals(121, reduceMax(120, 121))
    }

    @Test
    fun equalReading_staysUnchanged() {
        // reduceMax(100, 100) == 100 -- equal: stays unchanged.
        assertEquals(100, reduceMax(100, 100))
    }

    @Test
    fun negativeReading_treatedAsZero_doesNotLowerMax() {
        // reduceMax(120, -5) == 120 -- negative/anomalous reading treated as 0, does not lower.
        assertEquals(120, reduceMax(120, -5))
    }

    @Test
    fun sanitizePersistedMax_validValue_passesThrough() {
        // sanitizePersistedMax(150) == 150 -- valid value passes through unchanged.
        assertEquals(150, sanitizePersistedMax(150))
    }

    @Test
    fun sanitizePersistedMax_zero_isValid() {
        // sanitizePersistedMax(0) == 0 -- zero is a valid value.
        assertEquals(0, sanitizePersistedMax(0))
    }

    @Test
    fun sanitizePersistedMax_negativeValue_isClampedToZero() {
        // sanitizePersistedMax(-1) == 0 -- T-04-01: a tampered/negative persisted value is reset to 0.
        assertEquals(0, sanitizePersistedMax(-1))
    }
}
