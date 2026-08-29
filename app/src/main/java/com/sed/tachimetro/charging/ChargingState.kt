package com.sed.tachimetro.charging

/** Sealed model of the device's exposed charging state (D-01/D-02/D-03). */
sealed class ChargingState {
    /** D-01: not connected to power — the icon is not shown at all. */
    data object Hidden : ChargingState()

    /** D-02: `BATTERY_STATUS_CHARGING` — the fill animation loops indefinitely. */
    data object Pulsing : ChargingState()

    /** D-03: `BATTERY_STATUS_FULL` while still connected — icon frozen solid lime. */
    data object Full : ChargingState()
}
