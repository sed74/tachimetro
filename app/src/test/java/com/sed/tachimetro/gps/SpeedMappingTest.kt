package com.sed.tachimetro.gps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Plain JVM unit tests for [mapSpeedToKmh] — no Android runtime, no coroutines-test.
 * Locks the numeric decisions D-03/D-04/D-05/GPS-01/D-09.
 */
class SpeedMappingTest {

    @Test
    fun hasSpeedFalse_returnsZero() {
        // D-04: hasSpeed()==false means "fermo" -> 0 km/h, not dropped.
        val result = mapSpeedToKmh(
            hasAccuracy = false,
            accuracyMeters = 0f,
            hasSpeed = false,
            speedMetersPerSecond = 99f,
        )
        assertEquals(0, result)
    }

    @Test
    fun belowNoiseFloor_returnsZero() {
        // 0.4 m/s ~= 1.44 km/h, below the 2.0 km/h noise floor (D-03).
        val result = mapSpeedToKmh(
            hasAccuracy = false,
            accuracyMeters = 0f,
            hasSpeed = true,
            speedMetersPerSecond = 0.4f,
        )
        assertEquals(0, result)
    }

    @Test
    fun tenMetersPerSecond_returnsThirtySix() {
        // 10 m/s * 3.6 = 36 km/h (GPS-01 conversion).
        val result = mapSpeedToKmh(
            hasAccuracy = false,
            accuracyMeters = 0f,
            hasSpeed = true,
            speedMetersPerSecond = 10f,
        )
        assertEquals(36, result)
    }

    @Test
    fun fractionalSpeed_roundsToNearestInt() {
        // 5.27 m/s ~= 18.97 km/h -> rounds to 19, no decimals (D-09).
        val result = mapSpeedToKmh(
            hasAccuracy = false,
            accuracyMeters = 0f,
            hasSpeed = true,
            speedMetersPerSecond = 5.27f,
        )
        assertEquals(19, result)
    }

    @Test
    fun poorAccuracy_returnsNull() {
        // accuracy 60m > 50m threshold -> reading dropped (D-05).
        val result = mapSpeedToKmh(
            hasAccuracy = true,
            accuracyMeters = 60f,
            hasSpeed = true,
            speedMetersPerSecond = 10f,
        )
        assertNull(result)
    }

    @Test
    fun goodAccuracy_isAccepted() {
        // accuracy 20m <= 50m threshold -> reading accepted.
        val result = mapSpeedToKmh(
            hasAccuracy = true,
            accuracyMeters = 20f,
            hasSpeed = true,
            speedMetersPerSecond = 10f,
        )
        assertEquals(36, result)
    }

    @Test
    fun unknownAccuracy_isNotDropped() {
        // hasAccuracy=false -> accuracy unknown, must not be treated as poor (D-05 filter
        // only applies when accuracy is actually known).
        val result = mapSpeedToKmh(
            hasAccuracy = false,
            accuracyMeters = 999f,
            hasSpeed = true,
            speedMetersPerSecond = 10f,
        )
        assertEquals(36, result)
    }
}
