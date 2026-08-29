package com.sed.tachimetro.charging

import android.os.BatteryManager

/**
 * WR-02: pure, unit-testable extraction of the [ChargingStateProvider.state] decision logic
 * (D-01/D-02/D-03), independent of Flow/coroutines machinery.
 *
 * @param batteryStatus one of `BatteryManager.BATTERY_STATUS_*`, or `-1` when `EXTRA_STATUS`
 *   is absent from the `ACTION_BATTERY_CHANGED` intent.
 */
fun deriveChargingState(batteryStatus: Int): ChargingState = when (batteryStatus) {
    BatteryManager.BATTERY_STATUS_CHARGING -> ChargingState.Pulsing // D-02
    BatteryManager.BATTERY_STATUS_FULL -> ChargingState.Full // D-03
    else -> ChargingState.Hidden // D-01: not connected / unknown / missing extra
}
