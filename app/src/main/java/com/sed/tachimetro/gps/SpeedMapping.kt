package com.sed.tachimetro.gps

import kotlin.math.roundToInt

/**
 * Pure, unit-testable m/s -> km/h mapping with accuracy/noise/hasSpeed filters.
 * Framework-free: takes only primitives, no android.* or com.google.android.gms.* imports.
 *
 * @return null when the reading is DROPPED (poor accuracy, D-05); otherwise the km/h
 *   value to display (whole number, no decimals — D-09).
 */
fun mapSpeedToKmh(
    hasAccuracy: Boolean,
    accuracyMeters: Float,
    hasSpeed: Boolean,
    speedMetersPerSecond: Float,
    accuracyThresholdMeters: Float = 50f,
    noiseFloorKmh: Double = 2.0,
): Int? {
    // D-05: discard readings with a known accuracy worse than the threshold.
    if (hasAccuracy && accuracyMeters > accuracyThresholdMeters) {
        return null
    }

    // D-04: hasSpeed()==false ("fermo") is treated as 0 km/h, not "no signal".
    val kmh = if (hasSpeed) speedMetersPerSecond * 3.6 else 0.0

    // D-03: round small noise-floor speeds down to 0.
    if (kmh < noiseFloorKmh) {
        return 0
    }

    // GPS-01/D-09: whole km/h, no decimals.
    return kmh.roundToInt()
}
