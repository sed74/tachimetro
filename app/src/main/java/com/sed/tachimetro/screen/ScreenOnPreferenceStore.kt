package com.sed.tachimetro.screen

import android.content.Context

/**
 * D-07: persistenza della preferenza "schermo sempre acceso" (un Boolean) via SharedPreferences
 * app-private, stesso file "tachimetro_prefs" di MaxSpeedStore ma con chiave distinta.
 * read() restituisce null quando NESSUNA preferenza è ancora stata salvata (primo avvio, D-05),
 * così il chiamante può derivare il default dallo stato di ricarica solo in quel caso.
 */
class ScreenOnPreferenceStore(context: Context) {
    // WR-04: il chiamante passa applicationContext, mai un'Activity.
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** null = nessuna preferenza salvata (D-05: primo avvio); altrimenti il Boolean persistito. */
    fun read(): Boolean? =
        if (prefs.contains(KEY_KEEP_SCREEN_ON)) prefs.getBoolean(KEY_KEEP_SCREEN_ON, false) else null

    /** D-06/D-07: scrittura immediata (apply(), fuori dal main thread) — un solo Boolean economico. */
    fun write(keepOn: Boolean) {
        prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, keepOn).apply()
    }

    companion object {
        const val PREFS_NAME = "tachimetro_prefs"
        const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
    }
}
