package com.sed.tachimetro.distance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Plain JVM unit tests for [formatDistanceDisplay] -- no Android runtime.
 * Locks D-01 (whole meters below 1 km, one-decimal km at/above 1 km) including the
 * accepted ~1-meter edge case at 999.6m documented in 07-UI-SPEC.md.
 */
class DistanceFormatTest {

    @Test
    fun zeroMeters_returnsMetersZero() {
        // formatDistanceDisplay(0f) == DistanceDisplay.Meters(0)
        assertEquals(DistanceDisplay.Meters(0), formatDistanceDisplay(0f))
    }

    @Test
    fun belowOneKilometer_returnsWholeMeters() {
        // formatDistanceDisplay(850f) == DistanceDisplay.Meters(850)
        assertEquals(DistanceDisplay.Meters(850), formatDistanceDisplay(850f))
    }

    @Test
    fun fractionalMeters_areRoundedToNearestMeter() {
        // formatDistanceDisplay(849.6f) == DistanceDisplay.Meters(850) -- rounded to the nearest meter in the meters branch.
        assertEquals(DistanceDisplay.Meters(850), formatDistanceDisplay(849.6f))
    }

    @Test
    fun justBelowOneKilometer_staysInMetersBranch() {
        // formatDistanceDisplay(999.6f) == DistanceDisplay.Meters(1000) -- Pitfall 5: branch decided on the
        // raw unrounded value -- accepted behavior, locked by this test.
        assertEquals(DistanceDisplay.Meters(1000), formatDistanceDisplay(999.6f))
    }

    @Test
    fun exactlyOneKilometer_returnsKilometers() {
        // formatDistanceDisplay(1000f) == DistanceDisplay.Kilometers(1.0f) -- inclusive threshold: 1000m is already km.
        val result = formatDistanceDisplay(1000f)
        assertTrue(result is DistanceDisplay.Kilometers)
        assertEquals(1.0f, (result as DistanceDisplay.Kilometers).value, 0.0001f)
    }

    @Test
    fun aboveOneKilometer_returnsRawKilometersValue() {
        // formatDistanceDisplay(1234f) == DistanceDisplay.Kilometers(1.234f) -- raw km value,
        // one-decimal rounding happens only at display time via "%1$.1f".
        val result = formatDistanceDisplay(1234f)
        assertTrue(result is DistanceDisplay.Kilometers)
        assertEquals(1.234f, (result as DistanceDisplay.Kilometers).value, 0.0001f)
    }
}
