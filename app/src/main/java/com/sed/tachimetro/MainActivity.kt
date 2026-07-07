package com.sed.tachimetro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import com.sed.tachimetro.gps.GpsSpeedProvider
import com.sed.tachimetro.gps.SpeedState

class MainActivity : AppCompatActivity() {

    private lateinit var messageText: TextView
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
        retryButton = findViewById(R.id.retryButton)
        retryButton.setOnClickListener { onRetryClicked() }

        gpsSpeedProvider = GpsSpeedProvider(this)
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
        messageText.text = getString(R.string.status_ready)
    }

    private fun showDenied() {
        retryButton.visibility = View.VISIBLE
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
        messageText.text = when (state) {
            is SpeedState.Searching, is SpeedState.NoSignal ->
                getString(R.string.searching_gps_signal)
            is SpeedState.Reading -> getString(R.string.speed_kmh_format, state.kmh)
        }
    }
}
