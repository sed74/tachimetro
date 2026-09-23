package com.sed.tachimetro.car

import androidx.car.app.CarContext
import androidx.car.app.model.CarIcon
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridTemplate
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.MapWithContentTemplate

import com.sed.tachimetro.BuildConfig

// D-07: segnaposto fisso della card. La velocita' non compare MAI nella card: vive solo sulla
// Surface (e nello spike nemmeno li', D-03).
private const val CARD_PLACEHOLDER = "Tachimetro"

/**
 * Fase 12 spike, percorso POI (D-06/D-07): template con Surface dello schermo auto quando il
 * permesso e' concesso.
 *
 * `MapWithContentTemplate` richiede una card di contenuto: la variante (message, list, pane,
 * grid) arriva da `BuildConfig.SPIKE_POI_CARD`, scelta a riga di comando con `-PpoiCard=...`
 * (Piano 01), cosi' lo spike misura quanta Surface resta libera con ciascuna card.
 *
 * Nessuna action strip (D-08: la configurazione piu' discreta possibile).
 *
 * REL-02: `MapWithContentTemplate` e' `@RequiresCarApi(7)`; nessun controllo a runtime perche'
 * il manifest dichiara `minCarApiLevel` 7 e l'host non avvia l'app sotto quel livello.
 *
 * @param carContext contesto della Session (non usato dalla card POI, presente per avere la
 *   stessa firma del flavor spikeNav)
 */
@Suppress("UNUSED_PARAMETER")
fun buildSurfaceTemplate(carContext: CarContext): Template {
    val card: Template = when (BuildConfig.SPIKE_POI_CARD) {
        "message" -> MessageTemplate.Builder(CARD_PLACEHOLDER).build()
        "list" -> ListTemplate.Builder()
            .setSingleList(
                ItemList.Builder()
                    .addItem(Row.Builder().setTitle(CARD_PLACEHOLDER).build())
                    .build()
            )
            .build()
        "pane" -> PaneTemplate.Builder(
            Pane.Builder()
                .addRow(Row.Builder().setTitle(CARD_PLACEHOLDER).build())
                .build()
        ).build()
        "grid" -> GridTemplate.Builder()
            .setSingleList(
                ItemList.Builder()
                    .addItem(
                        GridItem.Builder()
                            .setTitle(CARD_PLACEHOLDER)
                            .setImage(CarIcon.APP_ICON)
                            .build()
                    )
                    .build()
            )
            .build()
        // Non raggiungibile: -PpoiCard e' validato da Gradle in fase di configurazione (T-12-03).
        else -> throw IllegalStateException(
            "SPIKE_POI_CARD='${BuildConfig.SPIKE_POI_CARD}' non valido per il flavor spikePoi"
        )
    }

    return MapWithContentTemplate.Builder()
        .setContentTemplate(card)
        .build()
}
