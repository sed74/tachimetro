package com.sed.tachimetro.gps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Plain JVM unit tests for [deriveSpeedState] (WR-02) — no Android runtime, no
 * coroutines-test. Locks the state-machine decisions D-01/D-02 that
 * [GpsSpeedProvider.state]'s `combine(...)` block delegates to, and the `equals()` contract
 * of [SpeedState.Reading] that the `StateFlow` conflation depends on (D-06/D-07).
 */
class GpsSpeedProviderStateTest {

    @Test
    fun noAcceptedFixYet_returnsSearching() {
        // D-01: lastKmh == null means no accepted fix has arrived yet, regardless of clocks.
        val result = deriveSpeedState(lastKmh = null, lastDeltaMeters = 0f, now = 10_000L, lastAcceptedAtMs = 0L)
        assertEquals(SpeedState.Searching, result)
    }

    @Test
    fun recentAcceptedFix_returnsReading() {
        // Within the 5s window (D-02) -> Reading with the last accepted value.
        val result = deriveSpeedState(lastKmh = 42, lastDeltaMeters = 5.0f, now = 4_000L, lastAcceptedAtMs = 1_000L)
        assertEquals(SpeedState.Reading(42, 5.0f), result)
    }

    @Test
    fun exactlyAtFiveSecondBoundary_returnsReading() {
        // D-02 uses a strict ">" so exactly 5000ms elapsed is still "fresh".
        val result = deriveSpeedState(lastKmh = 42, lastDeltaMeters = 5.0f, now = 6_000L, lastAcceptedAtMs = 1_000L)
        assertEquals(SpeedState.Reading(42, 5.0f), result)
    }

    @Test
    fun justOverFiveSeconds_returnsNoSignal() {
        // D-02: no accepted update for more than 5s -> NoSignal.
        val result = deriveSpeedState(lastKmh = 42, lastDeltaMeters = 5.0f, now = 6_001L, lastAcceptedAtMs = 1_000L)
        assertEquals(SpeedState.NoSignal, result)
    }

    @Test
    fun wellOverFiveSeconds_returnsNoSignal() {
        val result = deriveSpeedState(lastKmh = 0, lastDeltaMeters = 0f, now = 60_000L, lastAcceptedAtMs = 1_000L)
        assertEquals(SpeedState.NoSignal, result)
    }

    @Test
    fun identicalInputs_produceEqualReadings() {
        // Blocca il contratto su cui si regge la deduplicazione di GpsSpeedProvider.state
        // (StateFlow conflaziona le emissioni consecutive equals()-uguali); se Reading
        // smettesse di essere una data class o guadagnasse un campo variabile per-tick,
        // questo test fallirebbe e impedirebbe il bug di sovra-conteggio della distanza
        // (RESEARCH.md Pitfall 1).
        val first = deriveSpeedState(lastKmh = 42, lastDeltaMeters = 5.0f, now = 4_000L, lastAcceptedAtMs = 1_000L)
        val second = deriveSpeedState(lastKmh = 42, lastDeltaMeters = 5.0f, now = 4_000L, lastAcceptedAtMs = 1_000L)
        assertEquals(first, second)
    }

    @Test
    fun differentDeltaMeters_produceDifferentReadings() {
        // La conflation non deve mascherare un delta diverso: due Reading con lastDeltaMeters
        // differenti non sono equals(), quindi lo StateFlow li emette entrambi.
        val first = deriveSpeedState(lastKmh = 42, lastDeltaMeters = 5.0f, now = 4_000L, lastAcceptedAtMs = 1_000L)
        val second = deriveSpeedState(lastKmh = 42, lastDeltaMeters = 7.5f, now = 4_000L, lastAcceptedAtMs = 1_000L)
        assertNotEquals(first, second)
    }
}
