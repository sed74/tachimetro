package com.sed.tachimetro.maxspeed

import kotlin.math.max

/** D-07: il massimo di sessione cresce solo verso la lettura più alta vista; letture piu' basse o anomale non lo abbassano. */
fun reduceMax(currentMax: Int, reading: Int): Int {
    val safeCurrent = if (currentMax < 0) 0 else currentMax
    val safeReading = if (reading < 0) 0 else reading
    return max(safeCurrent, safeReading)
}

/** T-04-01: un valore persistito manomesso/negativo viene riportato a 0 alla lettura. */
fun sanitizePersistedMax(raw: Int): Int = if (raw < 0) 0 else raw
