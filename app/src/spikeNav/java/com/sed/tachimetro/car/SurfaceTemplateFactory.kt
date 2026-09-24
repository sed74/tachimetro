package com.sed.tachimetro.car

import androidx.car.app.CarContext
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate

/**
 * Fase 12 spike, percorso NAVIGATION (D-08): template con Surface dello schermo auto quando il
 * permesso e' concesso.
 *
 * `NavigationTemplate` lancia `IllegalStateException` a `build()` senza action strip: se ne
 * mette una minima, con una sola azione icona senza titolo e senza funzione reale.
 *
 * Rischio NF-1/NF-6: un'app di categoria NAVIGATION che non naviga non passerebbe la revisione
 * Play. Questa variante esiste solo per misurare la Surface e non viene mai distribuita (D-05).
 * La deroga D-05 del 2026-09-24 (test chiuso Play Store) riguarda solo spikePoi: spikeNav
 * resta mai distribuito.
 *
 * @param carContext contesto della Session (non usato dalla variante con `CarIcon.APP_ICON`,
 *   presente per avere la stessa firma del flavor spikePoi)
 */
@Suppress("UNUSED_PARAMETER")
fun buildSurfaceTemplate(carContext: CarContext): Template {
    // D-08: icona senza titolo, click senza effetti (T-12-09, build mai distribuita).
    val placeholderAction = Action.Builder()
        .setIcon(CarIcon.APP_ICON)
        .setOnClickListener { }
        .build()

    return NavigationTemplate.Builder()
        .setActionStrip(ActionStrip.Builder().addAction(placeholderAction).build())
        .build()
}
