package com.sed.tachimetro.car

import android.content.Context

import androidx.car.app.model.Action
import androidx.car.app.model.PaneTemplate
import androidx.car.app.testing.ScreenController
import androidx.car.app.testing.TestCarContext
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import com.sed.tachimetro.R

/**
 * Test strumentato della FORMA del [PaneTemplate] prodotto da [SpeedScreen] (Piano 02) -- il
 * contratto di CONTENUTO (mappatura [com.sed.tachimetro.gps.SpeedState] -> [CarSpeedContent]) e'
 * gia' lockato a livello JVM puro da `CarSpeedContentTest`; qui si verifica solo la traduzione in
 * oggetti `PaneTemplate`/`Pane`/`Row`/`Action` che nessun test JVM puo' coprire (D-01/D-02/D-03,
 * 08-UI-SPEC.md Content Contract).
 *
 * Costruzione sempre tramite [TestCarContext] (mai un `CarContext` reale ottenuto da un host),
 * come richiesto da PLAN.md e reso obbligatorio dalla libreria stessa: il costruttore di
 * [ScreenController] lancia `IllegalArgumentException` se `screen.carContext` non e' un
 * `TestCarContext` (verificato leggendo il bytecode di `androidx.car.app:app-testing:1.7.0`,
 * l'unica documentazione disponibile per questa versione della classe).
 *
 * Nota sulla strada scelta per pilotare lo Screen (richiesta da PLAN.md Task 1 punto 2):
 * [ScreenController.moveToState] viene usato SOLO per portare lo Screen a
 * [Lifecycle.State.CREATED], mai a STARTED. E' una scelta deliberata: `SpeedScreen.init` avvia un
 * collector guardato da `repeatOnLifecycle(Lifecycle.State.STARTED)` (vedi `SpeedScreen.kt`) che,
 * se il permesso `ACCESS_FINE_LOCATION` risultasse gia' concesso sul device/emulatore di test
 * (residuo di un'installazione manuale precedente, es. Piano 03 Task 3), inizierebbe a
 * collezionare lo `StateFlow` reale di `GpsSpeedProvider` in modo non deterministico rispetto al
 * momento dell'assert. Restando a CREATED quel blocco non viene mai eseguito (ON_START non e'
 * mai dispatchato), quindi `latestState` resta deterministicamente `SpeedState.Searching` -- il
 * valore di inizializzazione del campo -- per ogni test qui, indipendentemente dallo stato del
 * permesso sul dispositivo di test.
 *
 * Il template viene poi letto chiamando direttamente `screen.onGetTemplate()` (metodo pubblico di
 * `androidx.car.app.Screen`, verificato via bytecode) invece di
 * `ScreenController.getTemplatesReturned()`: quest'ultimo riflette solo i template
 * effettivamente pushati verso l'`AppManager` host (es. da un `invalidate()` mentre lo Screen e'
 * gia' STARTED e in cima allo stack), lista che a CREATED resterebbe vuota. La chiamata diretta a
 * `onGetTemplate()` e' la strada di ripiego esplicitamente prevista da PLAN.md per questo caso.
 *
 * Sia la costruzione dello Screen sia `moveToState` toccano una `LifecycleRegistry` standard
 * (verificato via bytecode: sia `Screen` sia `TestLifecycleOwner` usano il costruttore
 * `LifecycleRegistry(LifecycleOwner)`, che impone il thread principale), quindi ogni operazione
 * che tocca lo Screen o il suo `ScreenController` gira su `runOnMainSync`.
 */
@RunWith(AndroidJUnit4::class)
class SpeedScreenTemplateTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    /** Costruisce uno SpeedScreen fresco, lo porta a CREATED e ne legge il template corrente. */
    private fun createTemplate(): PaneTemplate {
        lateinit var template: PaneTemplate
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val testCarContext = TestCarContext.createCarContext(context)
            val screen = SpeedScreen(testCarContext)
            ScreenController(screen).moveToState(Lifecycle.State.CREATED)
            template = screen.onGetTemplate() as PaneTemplate
        }
        return template
    }

    @Test
    fun initialState_showsSearchingRowWithoutUnit() {
        val template = createTemplate()
        val rows = template.pane.rows
        assertEquals(1, rows.size)

        val row = rows[0]
        assertEquals(context.getString(R.string.car_searching_gps_signal), row.title.toString())
        assertTrue(row.texts.isEmpty())
    }

    @Test
    fun template_hasAppIconHeaderActionAndNoTitle() {
        val template = createTemplate()

        assertEquals(Action.APP_ICON, template.headerAction)

        val title = template.title
        assertTrue(title == null || title.isEmpty)
    }

    @Test
    fun template_hasNoActionStripAndRowHasNoImage() {
        val template = createTemplate()

        assertNull(template.actionStrip)

        val row = template.pane.rows[0]
        assertNull(row.image)
    }

    @Test
    fun repeatedInvalidate_keepsProducingValidTemplate() {
        // NON verifica la quota dell'host (che vive nel processo host, non testabile qui, v.
        // Task 3) -- esclude solo che sia l'app stessa a degradare/accumulare stato interno dopo
        // molti rebuild consecutivi del template.
        val templates = mutableListOf<PaneTemplate>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val testCarContext = TestCarContext.createCarContext(context)
            val screen = SpeedScreen(testCarContext)
            ScreenController(screen).moveToState(Lifecycle.State.CREATED)

            repeat(60) {
                templates.add(screen.onGetTemplate() as PaneTemplate)
            }
        }

        assertEquals(60, templates.size)
        templates.forEach { template ->
            assertEquals(1, template.pane.rows.size)
        }
    }
}
