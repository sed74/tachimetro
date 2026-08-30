package com.sed.tachimetro.distance

/**
 * D-04: accumulates a distance delta into the running total only while the vehicle is moving
 * above the noise floor (default `2.0` km/h, mirroring [com.sed.tachimetro.gps.mapSpeedToKmh]'s
 * `noiseFloorKmh`) -- this avoids drift accumulating while the vehicle is stopped and GPS jitter
 * produces small phantom deltas. D-06: [deltaMeters] arrives already computed by the caller via
 * `Location.distanceTo()`; this function has no knowledge of `Location` and stays framework-free.
 * Both [currentTotalMeters] and [deltaMeters] are defensively clamped to `0f` if negative/corrupted.
 */
fun reduceDistance(
    currentTotalMeters: Float,
    deltaMeters: Float,
    kmh: Int,
    noiseFloorKmh: Double = 2.0,
): Float {
    val safeCurrentTotal = if (currentTotalMeters < 0f) 0f else currentTotalMeters
    if (kmh < noiseFloorKmh) {
        return safeCurrentTotal
    }
    val safeDelta = if (deltaMeters < 0f) 0f else deltaMeters
    return safeCurrentTotal + safeDelta
}

/** T-07-01-T: un valore persistito manomesso/negativo viene riportato a 0 alla lettura. */
fun sanitizePersistedDistance(raw: Float): Float = if (raw < 0f) 0f else raw
