package com.sed.tachimetro.gps

/** Sealed model of the GPS engine's exposed speed state (D-06). */
sealed class SpeedState {
    /** D-01: shown from startup until the first accepted fix arrives. */
    data object Searching : SpeedState()

    /** D-09: whole km/h, no decimals. */
    data class Reading(val kmh: Int) : SpeedState()

    /** D-02: no accepted update has arrived for more than 5 seconds. */
    data object NoSignal : SpeedState()
}
