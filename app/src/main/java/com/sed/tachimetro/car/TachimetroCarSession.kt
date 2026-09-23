package com.sed.tachimetro.car

import android.content.Intent
import android.content.res.Configuration

import androidx.car.app.Screen
import androidx.car.app.Session

/**
 * Session creata dall'host Android Auto (via [TachimetroCarAppService.onCreateSession]) per
 * ogni connessione. L'app ha un solo schermo, nessun deep link: nessuna logica di routing
 * sull'`intent` ricevuto.
 *
 * Fase 12 spike (D-03): la Session possiede lo [SpeedSurfaceRenderer] (vive quanto lei, WR-04)
 * e lo registra come observer del proprio lifecycle, cosi' il callback della Surface viene
 * registrato in `onCreate` e i frame pendenti fermati in `onDestroy`.
 */
class TachimetroCarSession : Session() {

    private var renderer: SpeedSurfaceRenderer? = null

    override fun onCreateScreen(intent: Intent): Screen {
        // Un observer aggiunto a un lifecycle gia' CREATED riceve comunque onCreate.
        renderer = SpeedSurfaceRenderer(carContext).also { lifecycle.addObserver(it) }
        return SpeedScreen(carContext)
    }

    // Cambio tema/densita' dell'host: ridisegno della prova con la nuova configurazione.
    override fun onCarConfigurationChanged(newConfiguration: Configuration) {
        renderer?.requestRender()
    }
}
