package com.sed.tachimetro.gps

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Plain JVM unit tests for [deriveSpeedState] (WR-02) — no Android runtime, no
 * coroutines-test. Locks the state-machine decisions D-01/D-02 that
 * [GpsSpeedProvider.state]'s `combine(...)` block delegates to.
 */
class GpsSpeedProviderStateTest {

    @Test
    fun noAcceptedFixYet_returnsSearching() {
        // D-01: lastKmh == null means no accepted fix has arrived yet, regardless of clocks.
        val result = deriveSpeedState(lastKmh = null, now = 10_000L, lastAcceptedAtMs = 0L)
        assertEquals(SpeedState.Searching, result)
    }

    @Test
    fun recentAcceptedFix_returnsReading() {
        // Within the 5s window (D-02) -> Reading with the last accepted value.
        val result = deriveSpeedState(lastKmh = 42, now = 4_000L, lastAcceptedAtMs = 1_000L)
        assertEquals(SpeedState.Reading(42), result)
    }

    @Test
    fun exactlyAtFiveSecondBoundary_returnsReading() {
        // D-02 uses a strict ">" so exactly 5000ms elapsed is still "fresh".
        val result = deriveSpeedState(lastKmh = 42, now = 6_000L, lastAcceptedAtMs = 1_000L)
        assertEquals(SpeedState.Reading(42), result)
    }

    @Test
    fun justOverFiveSeconds_returnsNoSignal() {
        // D-02: no accepted update for more than 5s -> NoSignal.
        val result = deriveSpeedState(lastKmh = 42, now = 6_001L, lastAcceptedAtMs = 1_000L)
        assertEquals(SpeedState.NoSignal, result)
    }

    @Test
    fun wellOverFiveSeconds_returnsNoSignal() {
        val result = deriveSpeedState(lastKmh = 0, now = 60_000L, lastAcceptedAtMs = 1_000L)
        assertEquals(SpeedState.NoSignal, result)
    }
}
