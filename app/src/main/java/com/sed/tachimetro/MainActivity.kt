package com.sed.tachimetro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.TextView

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import com.sed.tachimetro.gps.GpsSpeedProvider
import com.sed.tachimetro.gps.SpeedState

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

        messageText = findViewById(R.id.messageText)
        unitText = findViewById(R.id.unitText)
        retryButton = findViewById(R.id.retryButton)
        retryButton.setOnClickListener { onRetryClicked() }

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
    }

    private fun showDenied() {
        retryButton.visibility = View.VISIBLE
        unitText.visibility = View.GONE
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
            }
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
}
