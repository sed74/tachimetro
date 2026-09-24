package com.sed.tachimetro.car

import com.sed.tachimetro.gps.SpeedState

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Test JVM puri per [surfaceSpeedText] -- nessun runtime Android.
 * Blocca la regola AA-02 sulla Surface: Reading -> cifre, Searching/NoSignal -> placeholder,
 * mai un valore vecchio fermo sullo schermo.
 */
class SurfaceSpeedTextTest {

    @Test
    fun reading_returnsKmhAsText() {
        assertEquals("87", surfaceSpeedText(SpeedState.Reading(87, 12f)))
    }

    @Test
    fun reading_zero_returnsZeroText() {
        assertEquals("0", surfaceSpeedText(SpeedState.Reading(0, 0f)))
    }

    @Test
    fun reading_threeDigits_returnsKmhAsText() {
        assertEquals("123", surfaceSpeedText(SpeedState.Reading(123, 30f)))
    }

    @Test
    fun searching_returnsPlaceholder() {
        assertEquals(SURFACE_SPEED_PLACEHOLDER, surfaceSpeedText(SpeedState.Searching))
    }

    @Test
    fun noSignal_returnsPlaceholder() {
        assertEquals(SURFACE_SPEED_PLACEHOLDER, surfaceSpeedText(SpeedState.NoSignal))
    }

    @Test
    fun placeholder_isDoubleDash() {
        assertEquals("--", SURFACE_SPEED_PLACEHOLDER)
    }
}
