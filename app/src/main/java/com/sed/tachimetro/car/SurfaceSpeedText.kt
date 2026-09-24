package com.sed.tachimetro.car

import com.sed.tachimetro.gps.SpeedState

/**
 * Testo mostrato sulla Surface quando non c'e' una lettura valida (Searching o NoSignal).
 * E' anche lo stato iniziale del renderer, prima di qualsiasi emissione del provider GPS.
 */
const val SURFACE_SPEED_PLACEHOLDER = "--"

/**
 * Mappatura pura [SpeedState] -> testo disegnato sulla Surface di Android Auto.
 *
 * Delega a [carSpeedContent], cosi' la regola AA-02 (Searching e NoSignal unificati, lo schermo
 * auto non resta mai fermo su un valore vecchio) vive in un solo punto.
 *
 * La dimensione del testo NON dipende da questa stringa: il renderer misura sempre il campione
 * "888" (D-12), cosi' le cifre non cambiano grandezza al variare del numero di caratteri.
 *
 * @return le cifre in km/h interi per [SpeedState.Reading], [SURFACE_SPEED_PLACEHOLDER] altrimenti.
 */
fun surfaceSpeedText(state: SpeedState): String =
    when (val content = carSpeedContent(state)) {
        is CarSpeedContent.Speed -> content.kmh.toString()
        is CarSpeedContent.Searching -> SURFACE_SPEED_PLACEHOLDER
    }
