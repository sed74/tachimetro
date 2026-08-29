package com.sed.tachimetro.charging

import android.os.BatteryManager

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Plain JVM unit tests for [deriveChargingState] — no Android runtime, no
 * coroutines-test. Locks the state-machine decisions D-01/D-03 that
 * [ChargingStateProvider.state]'s `map(...)` block delegates to.
 */
class ChargingStateProviderStateTest {

    @Test
    fun charging_returnsPulsing() {
        // D-02: BATTERY_STATUS_CHARGING -> Pulsing (fill animation loops).
        val result = deriveChargingState(BatteryManager.BATTERY_STATUS_CHARGING)
        assertEquals(ChargingState.Pulsing, result)
    }

    @Test
    fun full_returnsFull() {
        // D-03: BATTERY_STATUS_FULL while still connected -> Full (frozen solid lime).
        val result = deriveChargingState(BatteryManager.BATTERY_STATUS_FULL)
        assertEquals(ChargingState.Full, result)
    }

    @Test
    fun discharging_returnsHidden() {
        // D-01: not connected to power -> Hidden.
        val result = deriveChargingState(BatteryManager.BATTERY_STATUS_DISCHARGING)
        assertEquals(ChargingState.Hidden, result)
    }

    @Test
    fun notCharging_returnsHidden() {
        val result = deriveChargingState(BatteryManager.BATTERY_STATUS_NOT_CHARGING)
        assertEquals(ChargingState.Hidden, result)
    }

    @Test
    fun unknownStatus_returnsHidden() {
        val result = deriveChargingState(BatteryManager.BATTERY_STATUS_UNKNOWN)
        assertEquals(ChargingState.Hidden, result)
    }

    @Test
    fun missingStatusExtra_returnsHidden() {
        // EXTRA_STATUS absent from the Intent -> caller defaults to -1.
        val result = deriveChargingState(-1)
        assertEquals(ChargingState.Hidden, result)
    }
}
