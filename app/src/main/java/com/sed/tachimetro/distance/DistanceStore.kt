package com.sed.tachimetro.distance

import android.content.Context

/**
 * DIST-03: persistenza della distanza percorsa (un solo Float in metri) via SharedPreferences
 * app-private. Niente Room/DataStore: uno scalare non giustifica una dipendenza (CONTEXT
 * Established Patterns), mirror esatto di [com.sed.tachimetro.maxspeed.MaxSpeedStore].
 */
class DistanceStore(context: Context) {
    // WR-04: il chiamante passa applicationContext, mai un'Activity.
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Legge la distanza persistita, azzerando qualsiasi valore malformato/negativo (T-07-01-T). */
    fun read(): Float = sanitizePersistedDistance(prefs.getFloat(KEY_DISTANCE_METERS, 0f))

    /** Scrittura immediata asincrona (apply(), fuori dal main thread). */
    fun write(value: Float) {
        prefs.edit().putFloat(KEY_DISTANCE_METERS, value).apply()
    }

    companion object {
        const val PREFS_NAME = "tachimetro_prefs"
        const val KEY_DISTANCE_METERS = "distance_meters"
    }
}
