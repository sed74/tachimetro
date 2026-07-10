package com.sed.tachimetro.maxspeed

import android.content.Context

/**
 * D-06/D-07/D-08: persistenza del massimo (un solo Int) via SharedPreferences app-private.
 * Niente Room/DataStore: un intero non giustifica una dipendenza (CONTEXT Established Patterns).
 */
class MaxSpeedStore(context: Context) {
    // WR-04: il chiamante passa applicationContext, mai un'Activity.
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Legge il massimo persistito, azzerando qualsiasi valore malformato/negativo (T-04-01). */
    fun read(): Int = sanitizePersistedMax(prefs.getInt(KEY_MAX_SPEED, 0))

    /** D-07/D-08: scrittura immediata asincrona (apply(), fuori dal main thread) — un solo Int economico. */
    fun write(value: Int) {
        prefs.edit().putInt(KEY_MAX_SPEED, value).apply()
    }

    companion object {
        const val PREFS_NAME = "tachimetro_prefs"
        const val KEY_MAX_SPEED = "max_speed_kmh"
    }
}
