package com.sed.tachimetro.car

import android.content.Context

import com.sed.tachimetro.maxspeed.MaxSpeedStore

/**
 * D-04: contatore persistito dei rifiuti del permesso di localizzazione registrati dallo schermo
 * auto. Sostituisce `Activity.shouldShowRequestPermissionRationale()`, non disponibile da uno
 * `Screen` (09-RESEARCH.md, verificato leggendo il sorgente di `CarAppPermissionActivity`).
 *
 * Questo contatore NON e' MAI autorevole sullo stato di concessione del permesso: quello si
 * legge sempre e solo con `ContextCompat.checkSelfPermission()` (Piano 02, `SpeedScreen`).
 * Questo store espone solo QUANTE volte l'utente ha rifiutato, non SE il permesso e' concesso.
 *
 * `recordDenial()` va chiamato SOLO dal callback di `CarContext.requestPermissions()` quando il
 * permesso risulta non concesso, mai in altri punti.
 */
class CarPermissionDenialStore(context: Context) {
    // WR-04: il chiamante passa applicationContext, mai un'Activity ne' un CarContext conservato.
    private val prefs = context.getSharedPreferences(MaxSpeedStore.PREFS_NAME, Context.MODE_PRIVATE)

    /** Legge il contatore persistito, azzerando qualsiasi valore malformato/negativo. */
    fun denialCount(): Int = sanitizeDenialCount(prefs.getInt(KEY_DENIAL_COUNT, 0))

    /** Incrementa di uno il contatore persistito (scrittura asincrona, mai bloccante sul main thread). */
    fun recordDenial() {
        prefs.edit().putInt(KEY_DENIAL_COUNT, denialCount() + 1).apply()
    }

    companion object {
        private const val KEY_DENIAL_COUNT = "car_location_denial_count"
    }
}
