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
 *
 * La Session collega lo [SpeedScreen] (unico collector GPS lato auto) al renderer: ogni
 * `SpeedState` raccolto dallo Screen arriva a [SpeedSurfaceRenderer.updateSpeed]. Il riferimento
 * al renderer vive quanto la Session (WR-04), nessun Activity context.
 */
class TachimetroCarSession : Session() {

    private var renderer: SpeedSurfaceRenderer? = null

    override fun onCreateScreen(intent: Intent): Screen {
        // Un observer aggiunto a un lifecycle gia' CREATED riceve comunque onCreate.
        val surfaceRenderer = SpeedSurfaceRenderer(carContext).also { lifecycle.addObserver(it) }
        renderer = surfaceRenderer
        return SpeedScreen(carContext, surfaceRenderer::updateSpeed)
    }

    // Cambio tema/densita' dell'host: ridisegno della Surface con la nuova configurazione.
    override fun onCarConfigurationChanged(newConfiguration: Configuration) {
        renderer?.requestRender()
    }
}
