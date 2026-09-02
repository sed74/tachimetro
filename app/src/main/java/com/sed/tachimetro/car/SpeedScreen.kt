package com.sed.tachimetro.car

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.ParkedOnlyOnClickListener
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
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

    // WR-04: il chiamante passa applicationContext, mai il CarContext -- stessa convenzione di
    // `provider` sopra (CarPermissionDenialStore, Piano 01).
    private val denialStore = CarPermissionDenialStore(carContext.applicationContext)

    // AA-04/D-04/D-05/D-06/SC2: sorgente di verita' reattiva dello stato del permesso lato auto,
    // mirror esatto di MainActivity.permissionGranted. Guida sia la richiesta automatica (ramo
    // NotRequested sotto) sia il rendering del PaneTemplate (Piano 02, onGetTemplate()).
    private val permissionState =
        MutableStateFlow<CarPermissionState>(CarPermissionState.NotRequested)

    // Pitfall 2 (09-RESEARCH.md): repeatOnLifecycle(STARTED) riparte a ogni rientro in STARTED
    // (es. Screen brevemente STOPPED e poi di nuovo STARTED mentre una richiesta e' ancora
    // pendente). Il contatore persistito da solo non copre questo caso, perche' nessun rifiuto
    // e' ancora stato registrato quando cio' accade: senza questa guardia in memoria si
    // rischierebbe di lanciare una seconda CarContext.requestPermissions() mentre la prima e'
    // ancora in attesa del suo callback.
    private var requestInFlight = false

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
                // T-08-08: il gate difensivo sul permesso resta valido e NON viene rimosso -- la
                // collect di GpsSpeedProvider.state avviene ESCLUSIVAMENTE nel ramo Granted del
                // when sotto, mai altrove. GpsSpeedProvider e' annotato
                // @Suppress("MissingPermission") e chiama requestLocationUpdates senza controlli
                // propri: collezionare senza permesso solleverebbe una SecurityException dentro
                // il callbackFlow e farebbe crashare lo schermo auto. La Fase 9 non sostituisce
                // questo gate: lo ACCOMPAGNA con la richiesta esplicita del permesso (AA-04),
                // cosi' il gate smette di essere passivo e lo schermo non resta piu' bloccato per
                // sempre su "Ricerca segnale...".
                refreshPermissionState()

                // SC2: collectLatest (non collect) e' obbligatorio -- quando lo stato cambia da
                // Granted a qualcos'altro la raccolta GPS in corso deve essere cancellata, e
                // quando cambia da Waiting a Granted la raccolta deve partire senza attendere un
                // nuovo ciclo di lifecycle. E' questo che garantisce la transizione automatica
                // alla velocita' (o a "Ricerca segnale...") senza riavviare app o collegamento.
                permissionState.collectLatest { state ->
                    invalidate()
                    when (state) {
                        // D-05: mai richiesto ne' mai rifiutato da qui -- richiesta automatica al
                        // primo collegamento, nessuna azione preliminare richiesta all'utente
                        // sull'auto.
                        CarPermissionState.NotRequested -> {
                            if (!requestInFlight) {
                                requestLocationPermission()
                            }
                        }
                        // Permesso concesso: unico ramo in cui GpsSpeedProvider.state viene
                        // collezionato (T-08-08 preservato). Cadenza 1 Hz ereditata dal ticker
                        // interno del provider, nessun timer separato lato auto.
                        CarPermissionState.Granted -> {
                            provider?.gpsSpeedProvider?.state?.collect { gpsState ->
                                latestState = gpsState
                                invalidate()
                            }
                        }
                        // D-01: il dialogo e' aperto sul telefono, il template mostra gia'
                        // "Controlla il telefono" -- nessuna azione da fare qui.
                        CarPermissionState.Waiting -> Unit
                        // D-06: dopo un rifiuto gia' ricevuto il dialogo NON viene MAI rilanciato
                        // automaticamente -- si riparte solo dal tocco esplicito sull'Action
                        // (onRetryOrSettingsClicked(), Piano 02).
                        is CarPermissionState.Denied -> Unit
                    }
                }
            }
        }
    }

    // CR-01-equivalente (mirror di MainActivity.refreshPermissionState()): unica funzione che
    // spinge lo stato corrente dentro permissionState. Se una richiesta e' ancora pendente
    // (requestInFlight) non fa nulla -- il callback di requestLocationPermission() e' la sola
    // autorita' sull'esito finche' non risponde (Pitfall 2).
    private fun refreshPermissionState() {
        if (requestInFlight) return
        permissionState.value = resolveCarPermissionState(
            granted = ContextCompat.checkSelfPermission(
                carContext,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED,
            denialCount = denialStore.denialCount(),
        )
    }

    // Pitfall 1 (09-RESEARCH.md): limite di piattaforma noto e accettato per questa milestone --
    // se Android Auto si collega mentre il veicolo e' gia' in movimento, la Javadoc di
    // CarContext.requestPermissions() dichiara che l'host puo' ignorare silenziosamente la
    // richiesta ("when the host deems it is unsafe, for example when the user is driving"), e
    // non e' documentato se il callback venga comunque invocato in quel caso. Lo schermo puo'
    // restare su "Controlla il telefono" finche' il veicolo non e' fermo e un nuovo ingresso in
    // STARTED non rivaluta lo stato (es. disconnessione/riconnessione del collegamento). Non e'
    // un bug da correggere qui: e' un limite documentato, accettato per questa milestone.
    private fun requestLocationPermission() {
        requestInFlight = true
        carContext.requestPermissions(
            listOf(Manifest.permission.ACCESS_FINE_LOCATION),
            ContextCompat.getMainExecutor(carContext),
        ) { _, _ ->
            requestInFlight = false
            // Si rilegge la verita' dal sistema invece di fidarsi delle liste approved/rejected
            // del callback -- stessa disciplina di MainActivity, il cui callback richiama
            // refreshPermissionState() invece di usare il flag grezzo ricevuto dal launcher.
            val granted = ContextCompat.checkSelfPermission(
                carContext,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                permissionState.value = CarPermissionState.Granted
            } else {
                // D-04: l'ordine e' vincolante -- leggere denialCount() PRIMA di
                // recordDenial(), altrimenti il primo rifiuto risulterebbe gia' permanente.
                val wasAlreadyDenied = denialStore.denialCount() > 0
                denialStore.recordDenial()
                permissionState.value = CarPermissionState.Denied(permanent = wasAlreadyDenied)
            }
        }
        // AA-04: impostato DOPO la chiamata, cosi' la cancellazione di collectLatest (che
        // avverrebbe alla prossima emissione di permissionState) non puo' mai precedere la
        // richiesta effettiva.
        permissionState.value = CarPermissionState.Waiting
    }

    // Invocata dall'Action di retry/impostazioni (Piano 02). Mirror esatto di
    // MainActivity.onRetryClicked(), con current.permanent al posto di
    // shouldShowRequestPermissionRationale() -- quell'API richiede un'Activity, che uno Screen
    // non ha (D-04).
    private fun onRetryOrSettingsClicked() {
        if (requestInFlight) return
        val current = permissionState.value
        if (current is CarPermissionState.Denied && current.permanent) {
            openAppSettingsFromCar()
        } else {
            requestLocationPermission()
        }
    }

    // D-04: apre la scheda dell'app nelle impostazioni del telefono. FLAG_ACTIVITY_NEW_TASK e'
    // obbligatorio perche' il chiamante (CarContext) non e' un'Activity. Il package proviene
    // sempre da carContext.packageName (l'app stessa), mai da un dato ricevuto dall'host
    // (T-09-07).
    private fun openAppSettingsFromCar() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", carContext.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        carContext.startActivity(intent)
    }

    /**
     * Costruisce il [PaneTemplate] per lo stato di permesso e velocita' correnti. Pubblica come
     * seam di test: permette al test strumentato del Piano 03 di verificare la forma del
     * template per ogni stato del permesso senza dipendere dallo stato reale del permesso sul
     * dispositivo di test. Nessun effetto collaterale, non legge campi mutabili -- riceve tutto
     * per parametro.
     */
    fun buildTemplate(permission: CarPermissionState, speed: SpeedState): PaneTemplate {
        val pane = Pane.Builder()

        when (permission) {
            // Ramo invariato dalla Fase 8: il contenuto della Row proviene dal contratto puro
            // carSpeedContent(). Nessuna Action in questo ramo.
            CarPermissionState.Granted -> {
                val content = carSpeedContent(speed)
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
                pane.addRow(row)
            }
            // D-01: il dialogo e' sul telefono, sull'auto non c'e' nulla da toccare. D-06:
            // nessun retry offerto prima che un rifiuto sia arrivato.
            CarPermissionState.NotRequested, CarPermissionState.Waiting -> {
                pane.addRow(
                    Row.Builder()
                        .setTitle(carContext.getString(R.string.car_check_your_phone))
                        .build()
                )
            }
            // D-02/D-03/D-04: mirror esatto dello switch a due stati di
            // MainActivity.showDenied() -- messaggio e titolo dell'Action cambiano insieme sullo
            // stesso booleano permanent.
            is CarPermissionState.Denied -> {
                val messageRes = if (permission.permanent) {
                    R.string.car_permission_denied_permanent
                } else {
                    R.string.car_permission_denied
                }
                pane.addRow(
                    Row.Builder().setTitle(carContext.getString(messageRes)).build()
                )
                val actionTitleRes = if (permission.permanent) {
                    R.string.open_settings
                } else {
                    R.string.retry
                }
                // ParkedOnlyOnClickListener non e' opzionale: e' quanto prescrive la Javadoc di
                // CarContext.requestPermissions() per le azioni che rimandano l'utente al
                // telefono -- l'host mostra da solo il messaggio "solo da fermi" (T-09-08), nessuna
                // logica di driving-state fatta in casa.
                pane.addAction(
                    Action.Builder()
                        .setTitle(carContext.getString(actionTitleRes))
                        .setOnClickListener(
                            ParkedOnlyOnClickListener.create { onRetryOrSettingsClicked() }
                        )
                        .build()
                )
            }
        }

        // D-03 (Fase 8) / D-07 (Fase 9): nessun titolo testuale ne' branding dentro lo schermo,
        // in nessuno stato. PaneTemplate richiede almeno uno tra titolo e header action;
        // Action.APP_ICON soddisfa il vincolo senza mostrare testo.
        return PaneTemplate.Builder(pane.build())
            .setHeaderAction(Action.APP_ICON)
            .build()
    }

    // T-08-07/T-09-09: etichetta del contenuto per il solo log diagnostico (BuildConfig.DEBUG) --
    // nessun valore di velocita' ne' dato di posizione oltre a quanto gia' presente.
    private fun templateLogLabel(permission: CarPermissionState, speed: SpeedState): String =
        when (permission) {
            CarPermissionState.Granted -> when (carSpeedContent(speed)) {
                is CarSpeedContent.Speed -> "Speed"
                is CarSpeedContent.Searching -> "Searching"
            }
            CarPermissionState.NotRequested -> "PermissionNotRequested"
            CarPermissionState.Waiting -> "PermissionWaiting"
            is CarPermissionState.Denied -> if (permission.permanent) {
                "PermissionDeniedPermanent"
            } else {
                "PermissionDenied"
            }
        }

    override fun onGetTemplate(): Template {
        templateBuildCount++
        val permission = permissionState.value

        if (BuildConfig.DEBUG) {
            Log.d(
                LOG_TAG,
                "onGetTemplate #$templateBuildCount content=${templateLogLabel(permission, latestState)}",
            )
        }

        return buildTemplate(permission, latestState)
    }
}
