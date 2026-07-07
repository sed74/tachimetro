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

class MainActivity : AppCompatActivity() {

    private lateinit var messageText: TextView
    private lateinit var retryButton: Button

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
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

        checkAndRequestPermission()
    }

    override fun onResume() {
        super.onResume()
        // Re-check permission state whenever the activity comes back to the
        // foreground (e.g. returning from the system Settings screen opened
        // by openAppSettings()). Without this, granting the permission
        // externally leaves the UI stuck on the "denied" screen until the
        // app is force-killed and relaunched.
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

    private fun checkAndRequestPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> showReady()

            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }

            else -> requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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
}
