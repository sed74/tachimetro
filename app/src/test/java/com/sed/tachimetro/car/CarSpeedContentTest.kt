package com.sed.tachimetro.car

import com.sed.tachimetro.gps.SpeedState

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Plain JVM unit tests for [carSpeedContent] -- no Android runtime.
 * Locks D-01 (digits/unit separation), D-02 (Searching/NoSignal unified under one copy) and
 * AA-02 (car screen never stuck on a stale reading).
 */
class CarSpeedContentTest {

    @Test
    fun reading_returnsSpeedWithKmh() {
        // carSpeedContent(SpeedState.Reading(87, 12f)) == CarSpeedContent.Speed(87)
        assertEquals(CarSpeedContent.Speed(87), carSpeedContent(SpeedState.Reading(87, 12f)))
    }

    @Test
    fun readingZeroKmh_returnsSpeedZero() {
        // carSpeedContent(SpeedState.Reading(0, 0f)) == CarSpeedContent.Speed(0) -- 0 km/h is a
        // valid reading, it must NOT degrade to Searching.
        assertEquals(CarSpeedContent.Speed(0), carSpeedContent(SpeedState.Reading(0, 0f)))
    }

    @Test
    fun searching_returnsSearching() {
        // carSpeedContent(SpeedState.Searching) == CarSpeedContent.Searching
        assertEquals(CarSpeedContent.Searching, carSpeedContent(SpeedState.Searching))
    }

    @Test
    fun noSignal_returnsSearching() {
        // carSpeedContent(SpeedState.NoSignal) == CarSpeedContent.Searching -- AA-02: unified
        // with Searching (same copy, D-02), the car screen never stays stuck on an old value.
        assertEquals(CarSpeedContent.Searching, carSpeedContent(SpeedState.NoSignal))
    }

    @Test
    fun deltaMeters_doesNotAffectContent() {
        // carSpeedContent(SpeedState.Reading(50, 0f)) == carSpeedContent(SpeedState.Reading(50, 999f))
        // -- distance/max speed remain phone-only (out of milestone scope).
        assertEquals(
            carSpeedContent(SpeedState.Reading(50, 0f)),
            carSpeedContent(SpeedState.Reading(50, 999f)),
        )
    }
}
