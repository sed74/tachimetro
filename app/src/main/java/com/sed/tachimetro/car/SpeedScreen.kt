package com.sed.tachimetro.car

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import kotlinx.coroutines.launch

import com.sed.tachimetro.BuildConfig
import com.sed.tachimetro.R
import com.sed.tachimetro.TachimetroApplication
import com.sed.tachimetro.gps.SpeedState

/**
 * Unico collector lato auto dello `StateFlow` condiviso ([TachimetroApplication.gpsSpeedProvider]):
 * si iscrive una sola volta, ricostruisce il [PaneTemplate] a ogni emissione tramite
 * [invalidate], con cadenza ereditata dal ticker interno di `GpsSpeedProvider` (1 Hz, D-05) --
 * nessun timer separato lato auto. Il contenuto della Row e' delegato al contratto puro
 * [carSpeedContent].
 */
class SpeedScreen(carContext: CarContext) : Screen(carContext) {

    companion object {
        private const val LOG_TAG = "TachimetroCar"
    }

    // Cast SAFE (as?, non forzato): convenzione "Error Handling" di CLAUDE.md, preferire un
    // default sicuro a un'eccezione. Se il cast fallisce il provider resta null e lo schermo
    // mostra permanentemente "Ricerca segnale..." invece di andare in crash. carContext non
    // viene mai tenuto in una proprieta' che sopravviva allo Screen.
    private val provider = carContext.applicationContext as? TachimetroApplication

    // Valore piu' recente da cui onGetTemplate() costruisce il template. Inizializzato a
    // Searching cosi' che il primo template mostrato prima di qualsiasi fix sia gia' quello
    // corretto (AA-02).
    private var latestState: SpeedState = SpeedState.Searching

    // D-10: contatore dei refresh del template, usato solo per il log diagnostico di verifica
    // cadenza del Piano 03 (racchiuso in BuildConfig.DEBUG, T-08-07).
    private var templateBuildCount = 0L

    init {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // T-08-08: gate difensivo sul permesso, rivalutato ogni volta che lo schermo
                // torna in stato STARTED. GpsSpeedProvider e' annotato @Suppress("MissingPermission")
                // e chiama requestLocationUpdates senza controlli propri: collezionare senza
                // permesso solleverebbe una SecurityException dentro il callbackFlow e farebbe
                // crashare lo schermo auto. La richiesta esplicita del permesso via
                // CarContext.requestPermissions() e' scope della Fase 9 -- qui il gate e' solo
                // difensivo, nessuna UX di permesso viene inventata.
                val granted = ContextCompat.checkSelfPermission(
                    carContext,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED

                if (granted) {
                    provider?.gpsSpeedProvider?.state?.collect { state ->
                        latestState = state
                        invalidate()
                    }
                }
            }
        }
    }

    override fun onGetTemplate(): Template {
        templateBuildCount++
        val content = carSpeedContent(latestState)

        if (BuildConfig.DEBUG) {
            val contentLabel = when (content) {
                is CarSpeedContent.Speed -> "Speed"
                is CarSpeedContent.Searching -> "Searching"
            }
            Log.d(LOG_TAG, "onGetTemplate #$templateBuildCount content=$contentLabel")
        }

        val row = when (content) {
            // D-01: solo le cifre nel titolo, unita' in uno slot separato, mai concatenata.
            is CarSpeedContent.Speed -> Row.Builder()
                .setTitle(content.kmh.toString())
                .addText(carContext.getString(R.string.unit_kmh))
                .build()
            // D-02: nessun addText, l'unita' non va mostrata quando non c'e' un valore.
            is CarSpeedContent.Searching -> Row.Builder()
                .setTitle(carContext.getString(R.string.car_searching_gps_signal))
                .build()
        }

        // D-03: nessun titolo testuale ne' branding dentro lo schermo. PaneTemplate richiede
        // almeno uno tra titolo e header action; Action.APP_ICON soddisfa il vincolo senza
        // mostrare testo.
        return PaneTemplate.Builder(Pane.Builder().addRow(row).build())
            .setHeaderAction(Action.APP_ICON)
            .build()
    }
}
