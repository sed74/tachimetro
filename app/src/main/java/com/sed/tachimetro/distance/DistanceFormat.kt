package com.sed.tachimetro.distance

import kotlin.math.roundToInt

/**
 * D-01: modello sealed della distanza percorsa da mostrare a schermo. Porta solo il NUMERO --
 * l'unità di misura ("m"/"km") vive in una view separata (D-02), quindi qui non compaiono
 * stringhe di unità.
 */
sealed class DistanceDisplay {
    /** Sotto 1000 m: metri interi. */
    data class Meters(val value: Int) : DistanceDisplay()

    /** Da 1000 m in su: chilometri come valore grezzo (l'arrotondamento a una decimale è a display). */
    data class Kilometers(val value: Float) : DistanceDisplay()
}

/**
 * D-01: sotto 1000 m restituisce metri interi arrotondati, da 1000 m in su (soglia inclusiva)
 * restituisce chilometri come valore grezzo -- l'arrotondamento a una decimale avviene solo a
 * display via `%1$.1f` (D-02), non qui.
 *
 * La decisione del ramo usa il valore GREZZO non arrotondato: 999.6 m rende quindi "1000" nel
 * ramo metri anziché "1,0" nel ramo km. Questo è un edge case di ~1 metro accettato esplicitamente
 * in 07-UI-SPEC.md (Pitfall 5 di 07-RESEARCH.md) -- non è un difetto da correggere.
 */
fun formatDistanceDisplay(meters: Float): DistanceDisplay {
    return if (meters < 1000f) {
        DistanceDisplay.Meters(meters.roundToInt())
    } else {
        DistanceDisplay.Kilometers(meters / 1000f)
    }
}
