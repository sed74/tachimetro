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
import com.sed.tachimetro.gps.SpeedState

/**
 * Test strumentato della FORMA del [PaneTemplate] prodotto da [SpeedScreen] (Piano 02) -- il
 * contratto di CONTENUTO (mappatura [com.sed.tachimetro.gps.SpeedState] -> [CarSpeedContent]) e'
 * gia' lockato a livello JVM puro da `CarSpeedContentTest`; qui si verifica solo la traduzione in
 * oggetti `PaneTemplate`/`Pane`/`Row`/`Action` che nessun test JVM puo' coprire (D-01/D-02/D-03,
 * 08-UI-SPEC.md Content Contract), estesa in questo Piano a TUTTI e quattro gli stati di
 * [CarPermissionState] (Piano 01/02), incluso lo switch a due stati messaggio+azione tra rifiuto
 * singolo e permanente.
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
 * valore di inizializzazione del campo -- per ogni test che usa [createTemplate], indipendentemente
 * dallo stato del permesso sul dispositivo di test.
 *
 * Il template viene poi letto chiamando direttamente `screen.onGetTemplate()` (metodo pubblico di
 * `androidx.car.app.Screen`, verificato via bytecode) invece di
 * `ScreenController.getTemplatesReturned()`: quest'ultimo riflette solo i template
 * effettivamente pushati verso l'`AppManager` host (es. da un `invalidate()` mentre lo Screen e'
 * gia' STARTED e in cima allo stack), lista che a CREATED resterebbe vuota. La chiamata diretta a
 * `onGetTemplate()` e' la strada di ripiego esplicitamente prevista da PLAN.md per questo caso.
 *
 * Per verificare gli altri stati del permesso (`Granted`, `Waiting`, `Denied(false)`,
 * `Denied(true)`) senza dover manipolare il permesso reale sul dispositivo di test o pilotare
 * l'intera macchina a stati asincrona di `SpeedScreen`, i test qui sotto usano invece l'helper
 * [buildTemplate], che chiama direttamente il seam pubblico `SpeedScreen.buildTemplate(permission,
 * speed)` esposto dal Piano 02 -- stesso problema di non determinismo gia' documentato sopra per
 * il collector GPS, qui risolto iniettando lo stato desiderato invece di lasciarlo derivare dal
 * permesso reale.
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

    /**
     * Costruisce uno SpeedScreen fresco (mai portato oltre CREATED) e invoca direttamente il
     * seam pubblico `SpeedScreen.buildTemplate(permission, speed)` con lo stato iniettato per
     * parametro. Motivazione: il seam permette di verificare ogni stato del permesso in modo
     * deterministico, senza dipendere dal fatto che `ACCESS_FINE_LOCATION` risulti concesso o
     * meno sul dispositivo di test -- lo stesso problema di non determinismo gia' documentato
     * nel KDoc di classe per il collector GPS.
     */
    private fun buildTemplate(
        permission: CarPermissionState,
        speed: SpeedState = SpeedState.Searching,
    ): PaneTemplate {
        lateinit var template: PaneTemplate
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val testCarContext = TestCarContext.createCarContext(context)
            val screen = SpeedScreen(testCarContext)
            template = screen.buildTemplate(permission, speed)
        }
        return template
    }

    @Test
    fun initialState_beforeStarted_showsCheckYourPhoneRow() {
        // Dopo il Piano 02 lo stato iniziale di permissionState e' NotRequested (non piu' un
        // valore neutro che porta al ramo Searching): a CREATED, prima che ON_START sia mai
        // dispatchato, il template atteso mostra il messaggio di attesa dialogo sul telefono,
        // senza alcuna Action.
        val template = createTemplate()
        val rows = template.pane.rows
        assertEquals(1, rows.size)

        val row = rows[0]
        assertEquals(context.getString(R.string.car_check_your_phone), row.title.toString())
        assertTrue(row.texts.isEmpty())
        assertTrue(template.pane.actions.isEmpty())
    }

    @Test
    fun granted_reading_showsDigitsAndUnitWithoutActions() {
        // D-01: solo le cifre nel titolo, unita' in uno slot separato, mai concatenata; nessuna
        // Action nel ramo Granted.
        val template = buildTemplate(
            CarPermissionState.Granted,
            SpeedState.Reading(kmh = 87, deltaMeters = 0f),
        )
        val rows = template.pane.rows
        assertEquals(1, rows.size)

        val row = rows[0]
        assertEquals("87", row.title.toString())
        assertEquals(1, row.texts.size)
        assertEquals(context.getString(R.string.unit_kmh), row.texts[0].toString())
        assertTrue(template.pane.actions.isEmpty())
    }

    @Test
    fun granted_searching_showsSearchingRowWithoutUnit() {
        // Copertura AA-02 che initialState_beforeStarted_showsCheckYourPhoneRow non copre piu'
        // dopo il rename: quel test verifica NotRequested, non Granted+Searching.
        val template = buildTemplate(CarPermissionState.Granted, SpeedState.Searching)
        val rows = template.pane.rows
        assertEquals(1, rows.size)

        val row = rows[0]
        assertEquals(context.getString(R.string.car_searching_gps_signal), row.title.toString())
        assertTrue(row.texts.isEmpty())
        assertTrue(template.pane.actions.isEmpty())
    }

    @Test
    fun waiting_showsCheckYourPhoneWithoutActions() {
        // D-01/D-06: nessun retry offerto prima che un rifiuto sia arrivato -- il dialogo di
        // sistema e' aperto sul telefono, l'auto resta passiva.
        val template = buildTemplate(CarPermissionState.Waiting)
        val rows = template.pane.rows
        assertEquals(1, rows.size)
        assertEquals(context.getString(R.string.car_check_your_phone), rows[0].title.toString())
        assertTrue(template.pane.actions.isEmpty())
    }

    @Test
    fun deniedOnce_showsShortMessageWithRetryAction() {
        val template = buildTemplate(CarPermissionState.Denied(permanent = false))
        val rows = template.pane.rows
        assertEquals(1, rows.size)
        assertEquals(context.getString(R.string.car_permission_denied), rows[0].title.toString())

        val actions = template.pane.actions
        assertEquals(1, actions.size)
        assertEquals(context.getString(R.string.retry), actions[0].title.toString())
    }

    @Test
    fun deniedPermanently_showsSettingsMessageWithOpenSettingsAction() {
        val template = buildTemplate(CarPermissionState.Denied(permanent = true))
        val rows = template.pane.rows
        assertEquals(1, rows.size)
        assertEquals(
            context.getString(R.string.car_permission_denied_permanent),
            rows[0].title.toString(),
        )

        val actions = template.pane.actions
        assertEquals(1, actions.size)
        assertEquals(context.getString(R.string.open_settings), actions[0].title.toString())
    }

    @Test
    fun everyPermissionState_keepsAppIconHeaderAndNoActionStrip() {
        // D-03/D-07: nessun branding, nessuna icona, nessuna action strip in nessuno stato --
        // verificato su tutti e quattro gli stati del permesso, non solo sullo stato iniziale.
        val states = listOf(
            CarPermissionState.Granted,
            CarPermissionState.NotRequested,
            CarPermissionState.Waiting,
            CarPermissionState.Denied(permanent = false),
            CarPermissionState.Denied(permanent = true),
        )

        states.forEach { state ->
            val template = buildTemplate(state)
            assertEquals(Action.APP_ICON, template.headerAction)

            val title = template.title
            assertTrue(title == null || title.isEmpty)

            assertNull(template.actionStrip)
            assertNull(template.pane.rows[0].image)
        }
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
        // Piano 03 Task 2) -- esclude solo che sia l'app stessa a degradare/accumulare stato
        // interno dopo molti rebuild consecutivi del template.
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
