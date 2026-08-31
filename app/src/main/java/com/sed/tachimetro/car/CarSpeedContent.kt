package com.sed.tachimetro.car

import com.sed.tachimetro.gps.SpeedState

/**
 * D-01: modello sealed del contenuto della Row dello schermo auto. Porta solo il DATO -- l'unita'
 * di misura ("km/h") e la stringa di stato ("Ricerca segnale...") vivono in risorse stringa
 * risolte dal chiamante (Piano 02), esattamente come la separazione `messageText`/`unitText`
 * sul telefono. Nessun import Android qui: framework-free, come
 * `DistanceDisplay`/`formatDistanceDisplay`.
 */
sealed class CarSpeedContent {
    /** D-01: le sole cifre di velocita'; l'unita' "km/h" vive in uno slot separato. */
    data class Speed(val kmh: Int) : CarSpeedContent()

    /** D-02/AA-02: stato unificato Searching + NoSignal -- mai bloccati su un valore vecchio. */
    data object Searching : CarSpeedContent()
}

/**
 * D-01/D-02/AA-02: mappa lo stato del motore GPS ([SpeedState]) al contenuto puro della Row
 * dello schermo auto, senza risolvere risorse Android (quello avviene nel `SpeedScreen` del
 * Piano 02).
 *
 * - [SpeedState.Reading] -> [CarSpeedContent.Speed]: `deltaMeters` NON viene propagato --
 *   distanza e velocita' massima restano funzioni solo del telefono (REQUIREMENTS.md "Out of
 *   Scope" per questa milestone).
 * - [SpeedState.Searching] e [SpeedState.NoSignal] -> [CarSpeedContent.Searching]: AA-02, stessa
 *   copia piu' corta per l'auto (D-02) cosi' lo schermo auto non resta mai fermo su una lettura
 *   ormai scaduta, esattamente come `MainActivity.updatePlaceholder()` gia' unifica i due rami
 *   sotto un unico messaggio.
 *
 * `when` esaustivo senza `else`: il compilatore obbliga a gestire ogni futuro sottotipo di
 * [SpeedState].
 */
fun carSpeedContent(state: SpeedState): CarSpeedContent = when (state) {
    is SpeedState.Reading -> CarSpeedContent.Speed(state.kmh)
    is SpeedState.Searching -> CarSpeedContent.Searching
    is SpeedState.NoSignal -> CarSpeedContent.Searching
}
