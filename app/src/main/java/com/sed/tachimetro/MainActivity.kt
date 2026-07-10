package com.sed.tachimetro

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
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
    }

    private lateinit var messageText: TextView
    private lateinit var unitText: TextView
    private lateinit var retryButton: Button
    private lateinit var maxSpeedText: TextView
    private lateinit var resetMaxButton: Button
    private lateinit var maxSpeedStore: MaxSpeedStore
    private var currentMax: Int = 0
    private lateinit var keepScreenOnSwitch: SwitchCompat
    private lateinit var screenOnStore: ScreenOnPreferenceStore
    private lateinit var gpsSpeedProvider: GpsSpeedProvider

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        enableImmersiveFullscreen()

        messageText = findViewById(R.id.messageText)
        unitText = findViewById(R.id.unitText)
        retryButton = findViewById(R.id.retryButton)
        retryButton.setOnClickListener { onRetryClicked() }
        applyUnitTextWindowInsets()

        maxSpeedText = findViewById(R.id.maxSpeedText)
        resetMaxButton = findViewById(R.id.resetMaxButton)
        resetMaxButton.setOnClickListener { onResetMaxClicked() }
        applyMaxAreaWindowInsets()
        // D-09: leggere il massimo salvato PRIMA di avviare la raccolta GPS, cosi' l'area MAX
        // appare gia' con lo stato corretto senza flash di "MAX 0".
        maxSpeedStore = MaxSpeedStore(applicationContext)
        currentMax = maxSpeedStore.read()
        updateMaxArea()

        screenOnStore = ScreenOnPreferenceStore(applicationContext)
        keepScreenOnSwitch = findViewById(R.id.keepScreenOnSwitch)
        // D-04/D-05: se nessuna preferenza è salvata (primo avvio), il default deriva dallo stato di
        // ricarica; quel valore derivato viene persistito UNA sola volta, così dagli avvii successivi
        // conta solo la scelta salvata (lo stato di ricarica non viene più ricontrollato).
        val savedKeepOn = screenOnStore.read()
        val keepOn = savedKeepOn ?: isDeviceCharging()
        if (savedKeepOn == null) {
            screenOnStore.write(keepOn)
        }
        // Impostare checked PRIMA del listener: nessun trigger in init, nessun flash (UI-SPEC).
        keepScreenOnSwitch.isChecked = keepOn
        applyKeepScreenOn(keepOn)
        // D-06/D-07: il cambio applica immediatamente il flag e persiste la preferenza su disco.
        keepScreenOnSwitch.setOnCheckedChangeListener { _, isChecked ->
            applyKeepScreenOn(isChecked)
            screenOnStore.write(isChecked)
        }
        applyScreenSwitchWindowInsets()

        // WR-04: pass applicationContext, not the Activity, so GpsSpeedProvider (and the
        // FusedLocationProviderClient it wraps) never retains an Activity reference.
        gpsSpeedProvider = GpsSpeedProvider(applicationContext)
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

        checkAndRequestPermission()
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
        // WR-04: tear down gpsSpeedProvider's own CoroutineScope for symmetry/defensiveness
        // when this Activity instance is going away for good (e.g. a configuration change
        // recreates it with a fresh GpsSpeedProvider). D-07's repeatOnLifecycle(STARTED)
        // already stops collection on stop, so this is a secondary safety net, not the
        // primary stop/start mechanism.
        gpsSpeedProvider.close()
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
        messageText.text = getString(R.string.status_ready)
        updateMaxArea()
    }

    private fun showDenied() {
        retryButton.visibility = View.VISIBLE
        unitText.visibility = View.GONE
        maxSpeedText.visibility = View.GONE
        resetMaxButton.visibility = View.GONE
        applyMessageAutosize()
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

    private fun updatePlaceholder(state: SpeedState) {
        retryButton.visibility = View.GONE
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

                // D-07: update and persist the session max immediately whenever the current
                // reading exceeds it -- no batching to onPause()/onStop().
                val newMax = reduceMax(currentMax, state.kmh)
                if (newMax != currentMax) {
                    currentMax = newMax
                    maxSpeedStore.write(currentMax)
                }
            }
        }
        updateMaxArea()
    }

    // D-04/D-08: reset tap zeroes the in-memory max immediately (no confirmation dialog) and
    // persists 0 to disk right away, so a re-open right after reset never resurrects the old max.
    private fun onResetMaxClicked() {
        currentMax = 0
        maxSpeedStore.write(0)
        updateMaxArea()
    }

    // D-03/D-09: the whole MAX area (label + reset button) stays hidden while the max is 0 --
    // never renders a misleading "MAX 0". Plain visibility toggle, no animation (UI-04).
    private fun updateMaxArea() {
        if (currentMax > 0) {
            maxSpeedText.text = getString(R.string.max_speed_format, currentMax)
            maxSpeedText.visibility = View.VISIBLE
            resetMaxButton.visibility = View.VISIBLE
        } else {
            maxSpeedText.visibility = View.GONE
            resetMaxButton.visibility = View.GONE
        }
    }

    private fun applySpeedAutosize() {
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
            messageText,
            AUTOSIZE_MIN_SP,
            AUTOSIZE_MAX_SPEED_SP,
            AUTOSIZE_STEP_SP,
            TypedValue.COMPLEX_UNIT_SP
        )
    }

    private fun applyMessageAutosize() {
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

    // Specchio di applyMaxAreaWindowInsets() per l'angolo bottom-left: somma l'inset live
    // systemBars/displayCutout bottom+left sui margini base XML, così lo switch non finisce mai
    // dietro la navigation bar o un cutout inferiore/sinistro, in entrambi gli orientamenti.
    // Listener dedicato: gli insets differiscono per angolo (non riusare quello di maxSpeedText).
    private fun applyScreenSwitchWindowInsets() {
        val baseParams = keepScreenOnSwitch.layoutParams as ConstraintLayout.LayoutParams
        val baseBottom = baseParams.bottomMargin
        val baseStart = baseParams.marginStart
        ViewCompat.setOnApplyWindowInsetsListener(keepScreenOnSwitch) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val extraBottom = maxOf(systemBars.bottom, cutout.bottom)
            val extraStart = maxOf(systemBars.left, cutout.left)
            val lp = view.layoutParams as ConstraintLayout.LayoutParams
            lp.bottomMargin = baseBottom + extraBottom
            lp.marginStart = baseStart + extraStart
            view.layoutParams = lp
            insets
        }
    }
}
