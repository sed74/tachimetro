package com.sed.tachimetro.car

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Plain JVM unit tests for [resolveCarPermissionState] and [sanitizeDenialCount] -- no Android
 * runtime, nessun mocking, nessun Robolectric. Locks D-04 (distinzione primo rifiuto / rifiuto
 * permanente) e la sanitizzazione di un contatore persistito manomesso.
 */
class CarPermissionStateTest {

    @Test
    fun granted_zeroDenials_returnsGranted() {
        // resolveCarPermissionState(granted = true, denialCount = 0) == Granted
        assertEquals(CarPermissionState.Granted, resolveCarPermissionState(granted = true, denialCount = 0))
    }

    @Test
    fun granted_withPriorDenials_stillReturnsGranted() {
        // resolveCarPermissionState(granted = true, denialCount = 3) == Granted -- il permesso
        // concesso vince sempre sul contatore.
        assertEquals(CarPermissionState.Granted, resolveCarPermissionState(granted = true, denialCount = 3))
    }

    @Test
    fun notGranted_zeroDenials_returnsNotRequested() {
        // resolveCarPermissionState(granted = false, denialCount = 0) == NotRequested
        assertEquals(CarPermissionState.NotRequested, resolveCarPermissionState(granted = false, denialCount = 0))
    }

    @Test
    fun notGranted_oneDenial_returnsDeniedNotPermanent() {
        // resolveCarPermissionState(granted = false, denialCount = 1) == Denied(permanent = false)
        assertEquals(
            CarPermissionState.Denied(permanent = false),
            resolveCarPermissionState(granted = false, denialCount = 1),
        )
    }

    @Test
    fun notGranted_twoDenials_returnsDeniedPermanent() {
        // resolveCarPermissionState(granted = false, denialCount = 2) == Denied(permanent = true)
        assertEquals(
            CarPermissionState.Denied(permanent = true),
            resolveCarPermissionState(granted = false, denialCount = 2),
        )
    }

    @Test
    fun notGranted_fiveDenials_returnsDeniedPermanent() {
        // resolveCarPermissionState(granted = false, denialCount = 5) == Denied(permanent = true)
        assertEquals(
            CarPermissionState.Denied(permanent = true),
            resolveCarPermissionState(granted = false, denialCount = 5),
        )
    }

    @Test
    fun sanitizeDenialCount_negativeValue_isClampedToZero() {
        // sanitizeDenialCount(-1) == 0
        assertEquals(0, sanitizeDenialCount(-1))
    }

    @Test
    fun sanitizeDenialCount_zero_isValid() {
        // sanitizeDenialCount(0) == 0
        assertEquals(0, sanitizeDenialCount(0))
    }

    @Test
    fun sanitizeDenialCount_validValue_passesThrough() {
        // sanitizeDenialCount(2) == 2
        assertEquals(2, sanitizeDenialCount(2))
    }

    @Test
    fun notGranted_tamperedNegativeDenialCount_sanitizedToNotRequested() {
        // resolveCarPermissionState(granted = false, denialCount = sanitizeDenialCount(-7)) ==
        // NotRequested -- un valore persistito manomesso non produce uno stato "rifiutato" fantasma.
        assertEquals(
            CarPermissionState.NotRequested,
            resolveCarPermissionState(granted = false, denialCount = sanitizeDenialCount(-7)),
        )
    }
}
