package com.sed.tachimetro.car

import android.content.Intent

import androidx.car.app.Screen
import androidx.car.app.Session

/**
 * Session creata dall'host Android Auto (via [TachimetroCarAppService.onCreateSession]) per
 * ogni connessione. L'app ha un solo schermo, nessun deep link: nessuna logica di routing
 * sull'`intent` ricevuto.
 */
class TachimetroCarSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen = SpeedScreen(carContext)
}
