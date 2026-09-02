package com.sed.tachimetro

import android.Manifest
import android.animation.ValueAnimator
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.car.app.connection.CarConnection
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.TextViewCompat

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import com.sed.tachimetro.car.CarLinkState
import com.sed.tachimetro.car.resolveCarLinkState
import com.sed.tachimetro.car.resolveEffectiveKeepScreenOn
import com.sed.tachimetro.charging.ChargingState
import com.sed.tachimetro.charging.ChargingStateProvider
import com.sed.tachimetro.distance.DistanceDisplay
import com.sed.tachimetro.distance.DistanceStore
import com.sed.tachimetro.distance.formatDistanceDisplay
import com.sed.tachimetro.distance.reduceDistance
import com.sed.tachimetro.gps.GpsSpeedProvider
import com.sed.tachimetro.gps.SpeedState
import com.sed.tachimetro.maxspeed.MaxSpeedStore
import com.sed.tachimetro.maxspeed.reduceMax
import com.sed.tachimetro.screen.ScreenOnPreferenceStore

class MainActivity : AppCompatActivity() {

    companion object {
        // Shared autosize floor/step for messageText regardless of content type.
        private const val AUTOSIZE_MIN_SP = 12
        private const val AUTOSIZE_STEP_SP = 4

        // Speed digits keep the full dominant range (UI-01/UI-SPEC Typography).
        private const val AUTOSIZE_MAX_SPEED_SP = 300

        // Status/error messages get a materially smaller cap so long strings
        // (e.g. permission_denied_permanent) stay compact/legible instead of
        // scaling up toward the speed digits' huge range and wrapping unreadably.
        private const val AUTOSIZE_MAX_MESSAGE_SP = 56

        // CHRG-02/UI-SPEC "Animation spec": a ClipDrawable's level range is always 0..10000
        // regardless of view size -- 10_000 means fully filled (100%).
        private const val CHARGING_FILL_LEVEL_MAX = 10_000

        // UI-SPEC revision (post-checkpoint, quick task 260829-tgw): lo svuotamento non è più
        // graduale (niente più due mezze fasi da 1250ms in modalità REVERSE). L'intero budget
        // di ~2,5s (dentro la finestra "~2-3 secondi" di CHRG-02) è ora assegnato alla sola
        // salita 0 -> CHARGING_FILL_LEVEL_MAX; il ritorno a 0 avviene nel frame successivo,
        // senza durata (repeatMode = RESTART riparte istantaneamente dal valore iniziale
        // dell'animatore invece di rifare il percorso all'indietro).
        private const val CHARGING_FILL_CYCLE_MS = 2500L

        // CONN-01/CONN-02: tag distinto da "TachimetroCar" (SpeedScreen) cosi' i log
        // diagnostici dei due lati (telefono/auto) sono filtrabili separatamente in logcat
        // durante la verifica DHU del Piano 03.
        private const val LOG_TAG = "TachimetroPhone"
    }

    private lateinit var messageText: TextView
    private lateinit var unitText: TextView
    private lateinit var retryButton: Button
    private lateinit var maxSpeedText: TextView
    private lateinit var resetMaxButton: Button
    private lateinit var maxSpeedStore: MaxSpeedStore
    private var currentMax: Int = 0
    private lateinit var distanceText: TextView
    private lateinit var distanceUnitText: TextView
    private lateinit var distanceStore: DistanceStore
    private var currentDistanceMeters: Float = 0f
    // CONN-01: unico punto in memoria che rappresenta "Android Auto sta proiettando" sul
    // telefono; il valore iniziale Disconnected garantisce che qualunque percorso eseguito
    // prima della prima emissione dell'observer (Task 2) si comporti esattamente come in v1.1.
    private var carLink: CarLinkState = CarLinkState.Disconnected
    private lateinit var carConnection: CarConnection
    // CONN-02: copia in memoria della PREFERENZA PERSISTITA (non del flag effettivo) -- e' il
    // valore che verra' riapplicato tale e quale alla disconnessione. Viene aggiornato solo in
    // due punti, la lettura iniziale in setupScreenOnSwitch() e il listener dello switch, mai
    // da una transizione di connessione.
    private var savedKeepOn: Boolean = false
    private lateinit var keepScreenOnSwitch: SwitchCompat
    private lateinit var screenOnStore: ScreenOnPreferenceStore
    private lateinit var gpsSpeedProvider: GpsSpeedProvider
    private lateinit var chargingStateProvider: ChargingStateProvider
    private lateinit var chargingIcon: ImageView
    private var chargingFillLayer: ClipDrawable? = null
    private var chargingFillAnimator: ValueAnimator? = null

    // CR-01: reactive permission state, refreshed from every place permission state can
    // change (checkAndRequestPermission(), requestPermissionLauncher callback, onResume()),
    // so the collector below reacts to a grant immediately -- independent of whether the
    // system permission dialog happens to drive the activity through a STOP/START cycle.
    private val permissionGranted = MutableStateFlow(false)

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            refreshPermissionState()
            if (granted) {
                showReady()
            } else {
                showDenied()
            }
        }

    // WR-02: onCreate() delegates each independent setup concern to its own private
    // setupXxx() function, called here in the exact sequence the original inline code used --
    // this preserves every cross-concern ordering dependency documented in the functions below
    // (distance-before-max, screen-switch-before-collectors, etc.) while keeping onCreate()
    // itself a short, readable list of initialization steps (see CLAUDE.md "Function Design").
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        enableImmersiveFullscreen()

        setupPermissionViews()
        setupDistanceArea()
        setupMaxSpeedArea()
        setupScreenOnSwitch()
        setupGpsCollection()
        setupChargingIndicator()
        // CONN-01/CONN-02: deve venire DOPO setupScreenOnSwitch() (che valorizza savedKeepOn) e
        // DOPO setupGpsCollection() (che valorizza gpsSpeedProvider), entrambi letti da
        // onCarLinkChanged() -- ultima delle chiamate setupXxx().
        setupCarConnectionObserver()

        checkAndRequestPermission()
    }

    // WR-02: extracted from onCreate() -- wires messageText/unitText/retryButton, the
    // permission-denied retry click handler, and unitText's window-insets listener.
    private fun setupPermissionViews() {
        messageText = findViewById(R.id.messageText)
        unitText = findViewById(R.id.unitText)
        retryButton = findViewById(R.id.retryButton)
        retryButton.setOnClickListener { onRetryClicked() }
        applyUnitTextWindowInsets()
    }

    // WR-02: extracted from onCreate() -- DIST-01/DIST-03: leggere la distanza persistita PRIMA
    // di costruire gpsSpeedProvider evita il flash di "0 m" all'avvio (stesso motivo di D-09 in
    // setupMaxSpeedArea() per il massimo). Deve essere chiamata PRIMA di setupMaxSpeedArea():
    // currentDistanceMeters deve essere valorizzato prima della prima chiamata a
    // updateMaxArea(), perche' la visibilita' di resetMaxButton dipende anche da questo campo
    // (Piano 03, MAX-04).
    private fun setupDistanceArea() {
        distanceText = findViewById(R.id.distanceText)
        distanceUnitText = findViewById(R.id.distanceUnitText)
        applyDistanceAreaWindowInsets()
        // WR-04: il chiamante passa applicationContext, mai l'Activity.
        distanceStore = DistanceStore(applicationContext)
        currentDistanceMeters = distanceStore.read()
        updateDistanceArea()
    }

    // WR-02: extracted from onCreate() -- D-09: leggere il massimo salvato PRIMA di avviare la
    // raccolta GPS, cosi' l'area MAX appare gia' con lo stato corretto senza flash di "MAX 0".
    // Deve essere chiamata DOPO setupDistanceArea() -- vedi commento li'.
    private fun setupMaxSpeedArea() {
        maxSpeedText = findViewById(R.id.maxSpeedText)
        resetMaxButton = findViewById(R.id.resetMaxButton)
        resetMaxButton.setOnClickListener { onResetClicked() }
        applyMaxAreaWindowInsets()
        maxSpeedStore = MaxSpeedStore(applicationContext)
        currentMax = maxSpeedStore.read()
        updateMaxArea()
    }

    // WR-02: extracted from onCreate() -- wires the "keep screen on" switch and its charging-
    // derived default. chargingIcon's view lookup + resolveChargingFillLayer() live here too
    // (rather than in setupChargingIndicator()) because applyBottomLeftWindowInsets() sets up a
    // single listener that updates BOTH keepScreenOnSwitch's and chargingIcon's margins
    // together (see that function's own comment) -- both views must already be bound before it
    // runs.
    private fun setupScreenOnSwitch() {
        screenOnStore = ScreenOnPreferenceStore(applicationContext)
        keepScreenOnSwitch = findViewById(R.id.keepScreenOnSwitch)
        chargingIcon = findViewById(R.id.chargingIcon)
        // Resolves the ClipDrawable inside chargingIcon's LayerDrawable so the fill
        // animation helpers below have something to animate -- without this call
        // chargingFillLayer stays null and the icon would show but never fill (CHRG-02).
        resolveChargingFillLayer()
        // D-04/D-05: se nessuna preferenza è salvata (primo avvio), il default deriva dallo stato di
        // ricarica; quel valore derivato viene persistito UNA sola volta, così dagli avvii successivi
        // conta solo la scelta salvata (lo stato di ricarica non viene più ricontrollato).
        val savedPreference = screenOnStore.read()
        val keepOn = savedPreference ?: isDeviceCharging()
        if (savedPreference == null) {
            screenOnStore.write(keepOn)
        }
        savedKeepOn = keepOn
        // Impostare checked PRIMA del listener: nessun trigger in init, nessun flash (UI-SPEC).
        keepScreenOnSwitch.isChecked = keepOn
        applyKeepScreenOn(resolveEffectiveKeepScreenOn(savedKeepOn, carLink))
        // D-06/D-07: il cambio applica immediatamente il flag e persiste la preferenza su disco.
        // CONN-02: la persistenza della preferenza resta immediata e incondizionata anche
        // mentre Android Auto e' connesso -- l'utente puo' cambiare idea durante la proiezione e
        // la scelta verra' onorata alla disconnessione; e' solo l'APPLICAZIONE del flag a essere
        // derivata da resolveEffectiveKeepScreenOn().
        keepScreenOnSwitch.setOnCheckedChangeListener { _, isChecked ->
            savedKeepOn = isChecked
            screenOnStore.write(isChecked)
            applyKeepScreenOn(resolveEffectiveKeepScreenOn(savedKeepOn, carLink))
        }
        applyBottomLeftWindowInsets()
    }

    // WR-02: extracted from onCreate() -- reads the shared gpsSpeedProvider and starts the
    // permission-gated, lifecycle-scoped collector that drives updatePlaceholder().
    private fun setupGpsCollection() {
        // WR-04/D-00b: GpsSpeedProvider is now Application-scoped (TachimetroApplication),
        // constructed with applicationContext exactly once per process and shared with the
        // future car screen -- MainActivity reads the existing instance instead of
        // constructing its own, so the two surfaces never open independent GPS subscriptions.
        gpsSpeedProvider = (application as TachimetroApplication).gpsSpeedProvider
        // D-07: start/stop is entirely driven by repeatOnLifecycle(STARTED) below -- no
        // manual onStart()/onStop() overrides call collect/cancel by hand (see 02-PATTERNS.md).
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // CR-01: collectLatest on the reactive permissionGranted flow instead of a
                // one-shot check -- a grant that arrives without a STOP/START cycle (e.g. the
                // system permission dialog only triggering onPause()/onResume()) now restarts
                // gpsSpeedProvider.state.collect() as soon as refreshPermissionState() fires.
                permissionGranted.collectLatest { granted ->
                    if (granted) {
                        gpsSpeedProvider.state.collect { state -> updatePlaceholder(state) }
                    }
                }
            }
        }
    }

    // WR-02: extracted from onCreate() -- constructs chargingStateProvider and starts its
    // lifecycle-scoped collector that drives updateChargingIcon(). Must run after
    // setupScreenOnSwitch(), which binds the chargingIcon view this collector updates.
    private fun setupChargingIndicator() {
        // WR-04: application context only, same rationale as gpsSpeedProvider above.
        chargingStateProvider = ChargingStateProvider(applicationContext)
        // Charging observation has no permission gate (unlike GPS), so this is a plain
        // collect() in its own repeatOnLifecycle(STARTED) launch -- separate from the GPS
        // block above, which is wrapped by permissionGranted.collectLatest.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                chargingStateProvider.state.collect { state -> updateChargingIcon(state) }
            }
        }
    }

    // WR-02: extracted from onCreate() -- osserva CarConnection e ridisegna/rilascia il flag
    // schermo-sempre-acceso a ogni cambio di collegamento (CONN-01, CONN-02).
    private fun setupCarConnectionObserver() {
        // WR-04: applicationContext, mai l'Activity -- il costruttore e' annotato @MainThread e
        // onCreate() soddisfa il vincolo.
        carConnection = CarConnection(applicationContext)
        // Il LiveData registra il proprio BroadcastReceiver in onActive() e lo deregistra in
        // onInactive(): osservandolo con il lifecycle dell'Activity la registrazione segue
        // automaticamente START/STOP e l'observer viene rimosso a DESTROY. Per questo
        // onDestroy() NON va toccata: nessuna deregistrazione manuale, nessun rischio di leak,
        // coerente con repeatOnLifecycle(STARTED) usato dagli altri collector.
        carConnection.type.observe(this) { connectionType ->
            onCarLinkChanged(resolveCarLinkState(connectionType))
        }
    }

    // CONN-01/CONN-02: unico punto che aggiorna carLink e ne applica le conseguenze -- flag
    // schermo-sempre-acceso e rendering dell'area velocita'.
    private fun onCarLinkChanged(link: CarLinkState) {
        val changed = link != carLink
        carLink = link
        if (BuildConfig.DEBUG) {
            // T-10-05: solo stato del collegamento e booleani di preferenza, MAI velocita' ne'
            // dati di posizione.
            Log.d(
                LOG_TAG,
                "carLink=$carLink savedKeepOn=$savedKeepOn effectiveKeepOn=" +
                    "${resolveEffectiveKeepScreenOn(savedKeepOn, carLink)}",
            )
        }
        // Chiamata incondizionata: e' idempotente (addFlags/clearFlags) e cosi' si auto-ripara
        // anche se il LiveData riemette lo stesso valore.
        applyKeepScreenOn(resolveEffectiveKeepScreenOn(savedKeepOn, carLink))
        // Ridisegno immediato senza aspettare il prossimo tick del GPS. Due guardie, entrambe
        // obbligatorie: (a) changed evita che la prima emissione dell'observer sovrascriva il
        // "Pronto" iniziale con "Ricerca segnale GPS..."; (b) permissionGranted.value protegge
        // la precedenza del messaggio di permesso, che showDenied() ha gia' scritto in
        // messageText.
        if (changed && permissionGranted.value) {
            // NON updatePlaceholder(): quella funzione accumula la distanza da
            // state.deltaMeters, e invocarla con il valore gia' consumato di
            // gpsSpeedProvider.state.value conterebbe due volte gli stessi metri.
            // renderSpeedArea() e' priva di effetti collaterali ed e' l'unica chiamata corretta
            // in questo punto.
            renderSpeedArea(gpsSpeedProvider.state.value)
        }
        // CONN-02: questa funzione NON chiama screenOnStore.write() in nessun ramo. La
        // preferenza persistita e' immutata dalle transizioni di connessione -- e' il requisito
        // "senza alterare la preferenza memorizzata".
    }

    override fun onResume() {
        super.onResume()
        // Re-check permission state whenever the activity comes back to the
        // foreground (e.g. returning from the system Settings screen opened
        // by openAppSettings()). Without this, granting the permission
        // externally leaves the UI stuck on the "denied" screen until the
        // app is force-killed and relaunched.
        refreshPermissionState()
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            showReady()
        } else if (retryButton.visibility == View.VISIBLE) {
            // Refresh the denial message/button label in case the
            // "can ask again" state changed while we were away.
            showDenied()
        }
    }

    // T-06-03-D: repeatOnLifecycle(STARTED) stops collecting chargingStateProvider.state on
    // stop, but that alone does NOT cancel an already-running ValueAnimator -- it would keep
    // ticking at 60fps in the background, draining battery, unless explicitly cancelled here.
    // When STARTED resumes, the StateFlow immediately re-emits the current state to the new
    // collector and the animation restarts cleanly from level 0.
    override fun onStop() {
        stopChargingFillAnimation()
        super.onStop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Re-apply immersive fullscreen whenever this window regains focus. System bars can
        // reappear behind our back after e.g. returning from openAppSettings() (Settings app
        // covers/uncovers our window) or after a swipe-reveal (BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // only auto-hides them again on the *next* interaction, not deterministically) -- this is
        // the documented pattern for keeping WindowInsetsControllerCompat.hide() sticky.
        if (hasFocus) {
            enableImmersiveFullscreen()
        }
    }

    override fun onDestroy() {
        // D-00b: gpsSpeedProvider is no longer closed here. It is now Application-scoped
        // (TachimetroApplication) and outlives this Activity by design -- the future
        // Android Auto host can keep SpeedScreen alive as the sole collector even after
        // MainActivity is destroyed (e.g. the phone screen closed while still driving).
        // Cancelling the shared scope from here would kill GPS updates for the car screen
        // too. SharingStarted.WhileSubscribed() remains the sole mechanism that stops the
        // upstream location updates once no collector (phone or car) is active.
        // WR-04: same rationale as before -- cancels
        // chargingStateProvider's own CoroutineScope for symmetry/defensiveness.
        chargingStateProvider.close()
        super.onDestroy()
    }

    // CR-01: single source of truth that pushes the current permission state into
    // permissionGranted, so the STARTED-scoped collector above can react to it.
    private fun refreshPermissionState() {
        permissionGranted.value = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkAndRequestPermission() {
        refreshPermissionState()
        val granted = permissionGranted.value

        // Note: shouldShowRequestPermissionRationale() is intentionally not
        // checked here -- both the "show rationale" and "first ask" cases
        // launch the same system permission request. A rationale-specific
        // explanation (if ever added) belongs in onRetryClicked()/showDenied(),
        // not here.
        if (granted) {
            showReady()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun onRetryClicked() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            openAppSettings()
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    private fun showReady() {
        retryButton.visibility = View.GONE
        unitText.visibility = View.GONE
        applyMessageAutosize()
        // CONN-01: showReady() e' invocata anche da onResume(), quindi senza questa condizione
        // il ritorno in foreground con Android Auto gia' connesso mostrerebbe "Pronto" al posto
        // del messaggio neutro.
        messageText.text = if (carLink is CarLinkState.Connected) {
            getString(R.string.android_auto_connected)
        } else {
            getString(R.string.status_ready)
        }
        updateMaxArea()
        updateDistanceArea()
    }

    private fun showDenied() {
        retryButton.visibility = View.VISIBLE
        unitText.visibility = View.GONE
        maxSpeedText.visibility = View.GONE
        resetMaxButton.visibility = View.GONE
        distanceText.visibility = View.GONE
        distanceUnitText.visibility = View.GONE
        applyMessageAutosize()
        // CONN-01: precedenza deliberata -- quando il permesso di localizzazione manca, il
        // messaggio di permesso e il pulsante Riprova hanno la precedenza sullo stato neutro
        // di Android Auto, perche' sono l'unica via per rendere di nuovo utilizzabile l'app;
        // sostituirli con "Connesso ad Android Auto" toglierebbe all'utente l'unica azione
        // disponibile. Per questo showDenied() non consulta mai carLink.
        val permanentlyDenied =
            !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
        messageText.text = if (permanentlyDenied) {
            getString(R.string.permission_denied_permanent)
        } else {
            getString(R.string.permission_denied)
        }
        retryButton.text = if (permanentlyDenied) {
            getString(R.string.open_settings)
        } else {
            getString(R.string.retry)
        }
    }

    // CONN-01: unico punto di decisione del rendering dell'area velocita', a conoscenza dello
    // stato del collegamento auto (carLink). Nessun aggiornamento di massimo/distanza qui
    // dentro -- quelli restano in updatePlaceholder() (vedi commento li').
    private fun renderSpeedArea(state: SpeedState) {
        retryButton.visibility = View.GONE
        // CONN-01: quando Android Auto e' connesso, il telefono mostra lo stato neutro al
        // posto del numero di velocita' -- return anticipato, il when sottostante non viene
        // eseguito.
        if (carLink is CarLinkState.Connected) {
            unitText.visibility = View.GONE
            // messageText puo' arrivare da applySpeedAutosize(), che lascia maxLines = 1 e un
            // tetto di 300sp: senza il ripristino qui il messaggio verrebbe compresso su una
            // riga sola invece di andare a capo (stesso motivo gia' documentato nel commento di
            // applyMessageAutosize()).
            applyMessageAutosize()
            messageText.text = getString(R.string.android_auto_connected)
            return
        }
        when (state) {
            is SpeedState.Searching, is SpeedState.NoSignal -> {
                unitText.visibility = View.GONE
                applyMessageAutosize()
                messageText.text = getString(R.string.searching_gps_signal)
            }
            is SpeedState.Reading -> {
                // D-03/checkpoint feedback round 2: the unit label now lives in its own
                // small, fixed-size, top-end-pinned view (unitText) instead of being a
                // RelativeSizeSpan inside messageText -- messageText shows only the
                // digits so it stays the single dominant, centered element.
                unitText.visibility = View.VISIBLE
                applySpeedAutosize()
                messageText.text = state.kmh.toString()
            }
        }
    }

    private fun updatePlaceholder(state: SpeedState) {
        renderSpeedArea(state)

        // CONN-01: l'accumulo di massimo e distanza avviene FUORI dal ramo di rendering, quindi
        // continua a funzionare identico mentre Android Auto e' connesso -- lo stato neutro
        // sostituisce solo il numero della velocita', non sospende la registrazione del viaggio.
        // Per lo stesso motivo updateMaxArea()/updateDistanceArea() NON vengono saltate nello
        // stato neutro: le aree MAX e distanza restano visibili e aggiornate. Le scritture su
        // disco restano esattamente le stesse di prima del refactor: maxSpeedStore.write() qui
        // sotto e distanceStore.write() poco piu' in basso, mai spostate dentro renderSpeedArea().
        if (state is SpeedState.Reading) {
            // D-07: update and persist the session max immediately whenever the current
            // reading exceeds it -- no batching to onPause()/onStop().
            val newMax = reduceMax(currentMax, state.kmh)
            if (newMax != currentMax) {
                currentMax = newMax
                maxSpeedStore.write(currentMax)
            }

            // DIST-03: scrittura immediata su disco ad ogni incremento, nessun batching su
            // onPause()/onStop() -- cosi' un kill del processo non perde gli ultimi metri.
            // D-04: il gate della soglia di rumore vive dentro reduceDistance(), non qui: a
            // veicolo fermo state.kmh vale 0 e la funzione restituisce il totale invariato,
            // quindi in quel caso non c'e' nemmeno una scrittura su disco.
            // WR-03: passa esplicitamente GpsSpeedProvider.NOISE_FLOOR_KMH invece di
            // affidarsi al default duplicato di reduceDistance(), cosi' le due soglie non
            // possono divergere silenziosamente se quella di GpsSpeedProvider viene tarata.
            val newDistance = reduceDistance(
                currentDistanceMeters, state.deltaMeters, state.kmh,
                noiseFloorKmh = GpsSpeedProvider.NOISE_FLOOR_KMH,
            )
            if (newDistance != currentDistanceMeters) {
                currentDistanceMeters = newDistance
                distanceStore.write(currentDistanceMeters)
            }
        }

        updateMaxArea()
        // DIST-02: chiamata incondizionatamente ad ogni emissione, esattamente come
        // updateMaxArea(), cosi' l'area resta visibile e coerente anche nei rami
        // Searching/NoSignal (dove mostra congelato l'ultimo totale, senza testo di errore ne'
        // indicatore di pausa -- 07-UI-SPEC.md "States").
        updateDistanceArea()
    }

    // MAX-04: unico punto di reset dell'app -- un solo tocco azzera sia il massimo sia la
    // distanza, nessun dialog di conferma, entrambe le scritture immediate su disco, cosi' una
    // riapertura subito dopo il reset non resuscita ne' il vecchio massimo ne' la vecchia
    // distanza.
    private fun onResetClicked() {
        currentMax = 0
        maxSpeedStore.write(0)
        updateMaxArea()
        currentDistanceMeters = 0f
        distanceStore.write(0f)
        updateDistanceArea()
    }

    // D-03/D-09: maxSpeedText resta nascosta finche' il massimo e' 0 -- mai renderizzare un
    // fuorviante "MAX 0". MAX-04/07-UI-SPEC.md: resetMaxButton invece resta raggiungibile
    // finche' ALMENO UNA delle due metriche ha qualcosa da azzerare, dato che l'area distanza
    // e' sempre visibile e indipendente dallo stato dell'area MAX. Plain visibility toggle,
    // nessuna animazione (UI-04).
    private fun updateMaxArea() {
        if (currentMax > 0) {
            maxSpeedText.text = getString(R.string.max_speed_format, currentMax)
            maxSpeedText.visibility = View.VISIBLE
        } else {
            maxSpeedText.visibility = View.GONE
        }
        resetMaxButton.visibility =
            if (currentMax > 0 || currentDistanceMeters > 0f) View.VISIBLE else View.GONE
    }

    // D-01: soglia adattiva metri/km via formatDistanceDisplay(). D-02: l'unita' di misura
    // vive SEMPRE in distanceUnitText, mai concatenata dentro distanceText. Pitfall 3: si usa
    // sempre getString(...) e MAI la formattazione nuda della classe standard Java/Kotlin,
    // cosi' il ramo km formatta la virgola decimale secondo la locale corrente del
    // dispositivo (es. "1,2" su it-IT) invece di produrre sempre il punto invariante della
    // locale di default della JVM.
    // Divergenza deliberata da updateMaxArea(): l'area distanza resta SEMPRE visibile, anche
    // a "0 m" dopo un azzeramento -- a differenza di "MAX 0", che sarebbe fuorviante prima di
    // qualsiasi lettura, "0 m" e' un valore accurato e non va nascosto.
    private fun updateDistanceArea() {
        when (val display = formatDistanceDisplay(currentDistanceMeters)) {
            is DistanceDisplay.Meters -> {
                distanceText.text = getString(R.string.distance_meters_format, display.value)
                distanceUnitText.text = getString(R.string.unit_meters)
            }
            is DistanceDisplay.Kilometers -> {
                distanceText.text = getString(R.string.distance_km_format, display.value)
                distanceUnitText.text = getString(R.string.unit_km)
            }
        }
        distanceText.visibility = View.VISIBLE
        distanceUnitText.visibility = View.VISIBLE
    }

    // CHRG-01/CHRG-02/D-01/D-02/D-03: StateFlow conflates equal values, so the continuous
    // stream of ACTION_BATTERY_CHANGED broadcasts (which also fire on every percentage/
    // temperature change) does NOT re-trigger this when the derived state hasn't actually
    // changed -- the fill animation is never restarted mid-cycle by unrelated battery ticks.
    // Exhaustive when (no else): the compiler enforces every ChargingState branch is handled.
    private fun updateChargingIcon(state: ChargingState) {
        when (state) {
            is ChargingState.Hidden -> {
                // Roadmap SC2: disappears immediately, without waiting for the fill cycle to
                // finish.
                chargingIcon.visibility = View.GONE
                stopChargingFillAnimation()
            }
            is ChargingState.Pulsing -> {
                chargingIcon.visibility = View.VISIBLE
                startChargingFillAnimation()
            }
            is ChargingState.Full -> {
                // D-03: frozen solid lime, no motion.
                chargingIcon.visibility = View.VISIBLE
                freezeChargingFillAtFull()
            }
        }
    }

    // CHRG-01/UI-SPEC: resolves the ClipDrawable buried inside chargingIcon's LayerDrawable
    // (R.drawable.charging_flash_fill) so startChargingFillAnimation()/freezeChargingFillAtFull()/
    // stopChargingFillAnimation() have a level (0..10000) to drive. mutate() is required before
    // touching level -- without it, changing this drawable's state could bleed into any other
    // View sharing the same drawable resource via Android's ConstantState caching (T-06-03-T).
    // Safe casts throughout: an unexpected drawable structure leaves chargingFillLayer null
    // rather than crashing.
    private fun resolveChargingFillLayer() {
        val layerDrawable = chargingIcon.drawable?.mutate() as? LayerDrawable
        chargingFillLayer = layerDrawable?.findDrawableByLayerId(R.id.chargingIconFill) as? ClipDrawable
    }

    // D-02/CHRG-02/UI-SPEC "Re-plug": always restarts the loop from empty/white (level = 0),
    // never resumes from a previously cached phase. UI-SPEC revision (post-checkpoint, quick
    // task 260829-tgw): il lime sale gradualmente dal basso in CHARGING_FILL_CYCLE_MS (2500ms,
    // interpolatore invariato per il movimento morbido), ma una volta pieno si azzera DI COLPO
    // invece di scendere di nuovo -- repeatMode = RESTART fa ripartire ofInt() dal suo valore
    // iniziale (0) nel frame successivo, quindi lo svuotamento non è animato: è esattamente lo
    // "svuotamento istantaneo" richiesto dall'utente al posto della precedente modalità
    // simmetrica riempi/svuota.
    private fun startChargingFillAnimation() {
        chargingFillAnimator?.cancel()
        chargingFillAnimator = null
        chargingFillLayer?.level = 0
        chargingFillAnimator = ValueAnimator.ofInt(0, CHARGING_FILL_LEVEL_MAX).apply {
            duration = CHARGING_FILL_CYCLE_MS
            repeatMode = ValueAnimator.RESTART
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator -> chargingFillLayer?.level = animator.animatedValue as Int }
            start()
        }
    }

    // D-03: BATTERY_STATUS_FULL freezes the icon fully lime and motionless -- cancel() (not
    // pause()) per UI-SPEC "Loop", since a paused animator would still hold system resources
    // and could resume from a mid-cycle phase instead of a clean solid-full frame.
    private fun freezeChargingFillAtFull() {
        chargingFillAnimator?.cancel()
        chargingFillAnimator = null
        chargingFillLayer?.level = CHARGING_FILL_LEVEL_MAX
    }

    // D-01/roadmap SC2: chargingIcon disappears immediately on unplug, regardless of animation
    // phase -- cancel() stops the loop right away instead of waiting for the current iteration
    // to finish, and resets level to 0 so a future re-plug always starts from empty/white.
    private fun stopChargingFillAnimation() {
        chargingFillAnimator?.cancel()
        chargingFillAnimator = null
        chargingFillLayer?.level = 0
    }

    private fun applySpeedAutosize() {
        // Impedisce che le cifre della velocità vadano mai a capo su una 2ª riga: con
        // maxLines = 1 l'autosize uniform le rimpicciolisce per stare su una sola riga
        // invece di wrapparle (visibile altrimenti con numeri a 2 cifre in portrait).
        messageText.maxLines = 1
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
            messageText,
            AUTOSIZE_MIN_SP,
            AUTOSIZE_MAX_SPEED_SP,
            AUTOSIZE_STEP_SP,
            TypedValue.COMPLEX_UNIT_SP
        )
    }

    private fun applyMessageAutosize() {
        // messageText è la STESSA TextView riusata per le cifre e per i messaggi di
        // stato/errore: ripristina esplicitamente il wrapping libero, altrimenti un
        // precedente applySpeedAutosize() lascerebbe maxLines = 1 e bloccherebbe il
        // wrapping dei messaggi multi-parola (es. "Ricerca segnale GPS...").
        messageText.maxLines = Integer.MAX_VALUE
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
            messageText,
            AUTOSIZE_MIN_SP,
            AUTOSIZE_MAX_MESSAGE_SP,
            AUTOSIZE_STEP_SP,
            TypedValue.COMPLEX_UNIT_SP
        )
    }

    // Post-completion enhancement (round 4 user feedback): "vorrei che l'app fosse a tutto
    // schermo, senza la barra del titolo". The title/ActionBar is removed via the
    // NoActionBar theme parent (themes.xml / values-night/themes.xml). This function hides
    // the system status bar and navigation bar entirely for a distraction-free, at-a-glance
    // speedometer display (Core Value / UI-04: nessun elemento grafico non necessario),
    // using the modern WindowInsetsControllerCompat API (the current best-practice
    // replacement for the deprecated SYSTEM_UI_FLAG_FULLSCREEN/SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    // flags) with swipe-to-reveal behavior so the bars remain reachable without leaving them
    // permanently on screen. setDecorFitsSystemWindows(false) is set explicitly rather than
    // relying on the targetSdk 36 edge-to-edge default, because that default is only enforced
    // from API 35+ -- this app's minSdk is 30, so API 30-34 devices need it set explicitly for
    // consistent immersive behavior across the whole supported SDK range.
    private fun enableImmersiveFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    // Fix (checkpoint round 3 feedback): targetSdk 36 (Android 15+) enforces
    // edge-to-edge by default with no opt-out, so content draws under the status bar
    // unless window insets are explicitly handled. unitText is pinned to the top-end
    // corner with only a fixed 16dp base margin (declared in activity_main.xml) and
    // no insets handling existed anywhere in the app, so it rendered directly behind
    // the status bar (clock/battery/signal icons) -- invisible, not a visibility-flag
    // bug. This listener adds the live system bars / display cutout inset on top of
    // the XML-declared base margin so unitText always renders fully clear of the
    // status bar and any right-side cutout, in both portrait and landscape, without
    // touching messageText's or retryButton's existing (checkpoint-approved)
    // constraints/behavior.
    private fun applyUnitTextWindowInsets() {
        val baseParams = unitText.layoutParams as ConstraintLayout.LayoutParams
        val baseTopMargin = baseParams.topMargin
        val baseEndMargin = baseParams.marginEnd
        ViewCompat.setOnApplyWindowInsetsListener(unitText) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val extraTop = maxOf(systemBars.top, cutout.top)
            val extraEnd = maxOf(systemBars.right, cutout.right)
            val params = view.layoutParams as ConstraintLayout.LayoutParams
            params.topMargin = baseTopMargin + extraTop
            params.marginEnd = baseEndMargin + extraEnd
            view.layoutParams = params
            insets
        }
    }

    // Mirror of applyUnitTextWindowInsets() for the top-left MAX area (D-01, UI-SPEC Window
    // insets row): adds live systemBars/displayCutout top+left inset on top of the XML base
    // margins so maxSpeedText/resetMaxButton never render behind the status bar or a left-side
    // display cutout, in either orientation. Not shared with unitText's listener instance --
    // insets differ per side (start vs end).
    private fun applyMaxAreaWindowInsets() {
        val labelParams = maxSpeedText.layoutParams as ConstraintLayout.LayoutParams
        val labelBaseTop = labelParams.topMargin
        val labelBaseStart = labelParams.marginStart
        val buttonParams = resetMaxButton.layoutParams as ConstraintLayout.LayoutParams
        val buttonBaseStart = buttonParams.marginStart
        ViewCompat.setOnApplyWindowInsetsListener(maxSpeedText) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val extraTop = maxOf(systemBars.top, cutout.top)
            val extraStart = maxOf(systemBars.left, cutout.left)
            val lp = view.layoutParams as ConstraintLayout.LayoutParams
            lp.topMargin = labelBaseTop + extraTop
            lp.marginStart = labelBaseStart + extraStart
            view.layoutParams = lp
            val bp = resetMaxButton.layoutParams as ConstraintLayout.LayoutParams
            bp.marginStart = buttonBaseStart + extraStart
            resetMaxButton.layoutParams = bp
            insets
        }
    }

    // SCRN-02/D-06: applica o rimuove FLAG_KEEP_SCREEN_ON sulla finestra corrente. Immediato,
    // nessun riavvio richiesto. FLAG_KEEP_SCREEN_ON è la via documentata e leggera per impedire
    // lo spegnimento schermo senza permessi WAKE_LOCK.
    private fun applyKeepScreenOn(keepOn: Boolean) {
        if (keepOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // D-04: legge lo stato di ricarica corrente dal broadcast sticky ACTION_BATTERY_CHANGED
    // (registerReceiver(null, ...) restituisce subito l'ultimo Intent sticky, nessun receiver da
    // deregistrare). Usato SOLO al primo avvio per derivare il default dello switch.
    private fun isDeviceCharging(): Boolean {
        val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
    }

    // Specchio di applyMaxAreaWindowInsets() per l'angolo bottom-left, ora esteso al gruppo
    // chargingIcon + keepScreenOnSwitch (D-06): un solo listener, registrato sullo switch
    // (sempre VISIBLE, a differenza dell'icona che è GONE la maggior parte del tempo -- così
    // non dipendiamo dal comportamento di dispatch degli insets verso figli GONE), aggiorna i
    // margini di ENTRAMBE le view con una ripartizione asimmetrica dettata dai vincoli del
    // Piano 01: l'inset inferiore va sullo switch, che è l'unica delle due ancorata a `parent`
    // in basso -- chargingIcon lo eredita indirettamente perché è vincolata verticalmente al
    // top/bottom dello switch e si sposta insieme a lui. L'inset sinistro va invece
    // sull'icona, che ora è l'elemento più a sinistra del gruppo e l'unica ancorata a `parent`
    // sul lato start; il marginStart da 8dp dello switch resta un gap interno al gruppo, non
    // una distanza dal bordo schermo, quindi non riceve extraStart.
    private fun applyBottomLeftWindowInsets() {
        val switchParams = keepScreenOnSwitch.layoutParams as ConstraintLayout.LayoutParams
        val baseSwitchBottom = switchParams.bottomMargin
        val iconParams = chargingIcon.layoutParams as ConstraintLayout.LayoutParams
        val baseIconStart = iconParams.marginStart
        ViewCompat.setOnApplyWindowInsetsListener(keepScreenOnSwitch) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val extraBottom = maxOf(systemBars.bottom, cutout.bottom)
            val extraStart = maxOf(systemBars.left, cutout.left)
            val lp = view.layoutParams as ConstraintLayout.LayoutParams
            lp.bottomMargin = baseSwitchBottom + extraBottom
            view.layoutParams = lp
            val iconLp = chargingIcon.layoutParams as ConstraintLayout.LayoutParams
            iconLp.marginStart = baseIconStart + extraStart
            chargingIcon.layoutParams = iconLp
            insets
        }
    }

    // Specchio di applyUnitTextWindowInsets() per il nuovo angolo bottom-right (DIST-01):
    // aggiunge il live inset bottom+end di system bars/display cutout sopra ai margini base
    // dichiarati in XML per distanceUnitText. Registrato SOLO su distanceUnitText, l'unica
    // view del gruppo ancorata direttamente a `parent` -- a differenza di
    // applyMaxAreaWindowInsets(), dove entrambe le view sono ancorate a `parent` e serve
    // aggiornare due layoutParams nello stesso listener, qui distanceText e' agganciata a
    // distanceUnitText su entrambi gli assi (end + baseline) e si sposta di conseguenza senza
    // bisogno di un secondo listener.
    private fun applyDistanceAreaWindowInsets() {
        val unitParams = distanceUnitText.layoutParams as ConstraintLayout.LayoutParams
        val baseBottomMargin = unitParams.bottomMargin
        val baseEndMargin = unitParams.marginEnd
        ViewCompat.setOnApplyWindowInsetsListener(distanceUnitText) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val extraBottom = maxOf(systemBars.bottom, cutout.bottom)
            val extraEnd = maxOf(systemBars.right, cutout.right)
            val params = view.layoutParams as ConstraintLayout.LayoutParams
            params.bottomMargin = baseBottomMargin + extraBottom
            params.marginEnd = baseEndMargin + extraEnd
            view.layoutParams = params
            insets
        }
    }
}
