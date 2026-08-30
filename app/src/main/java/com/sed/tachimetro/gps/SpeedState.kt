package com.sed.tachimetro.gps

/** Sealed model of the GPS engine's exposed speed state (D-06). */
sealed class SpeedState {
    /** D-01: shown from startup until the first accepted fix arrives. */
    data object Searching : SpeedState()

    /**
     * D-09: whole km/h, no decimals.
     *
     * @param deltaMeters D-06: meters traveled since the previous accepted fix, computed via
     *   `Location.distanceTo()`; `0f` for the first accepted fix of a session (no prior fix
     *   to measure from).
     *
     * CRITICAL (RESEARCH.md Pitfall 1): this MUST remain a plain `data class` with ONLY these
     * two fields. [GpsSpeedProvider.state] is a `StateFlow`, which conflates (drops)
     * consecutive `equals()`-equal emissions; the once-per-second ticker re-runs `combine()`
     * every tick regardless of whether a new GPS fix arrived. Adding a timestamp or any other
     * field that changes independently of a genuine new accepted fix would break that
     * deduplication and cause the distance accumulator (Piano 03, `reduceDistance()`) to add
     * the same `deltaMeters` repeatedly every tick — silent distance overcounting.
     */
    data class Reading(val kmh: Int, val deltaMeters: Float) : SpeedState()

    /** D-02: no accepted update has arrived for more than 5 seconds. */
    data object NoSignal : SpeedState()
}
