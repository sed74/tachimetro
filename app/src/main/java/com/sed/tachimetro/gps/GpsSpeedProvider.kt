package com.sed.tachimetro.gps

import android.content.Context
import android.location.Location
import android.os.Looper
import android.os.SystemClock

import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

/**
 * Wraps continuous FusedLocationProviderClient updates in a [callbackFlow] and exposes a
 * [StateFlow] of [SpeedState], applying the tested [mapSpeedToKmh] filters and detecting
 * startup/loss "no signal" per D-01/D-02.
 *
 * Permission note: this class does NOT check ACCESS_FINE_LOCATION itself. MainActivity
 * (Phase 1) is the single source of truth for that permission and only starts collecting
 * [state] once it has been granted — see RESEARCH.md Pitfall 1 / Anti-Patterns.
 */
class GpsSpeedProvider(context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    private val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
        .setMinUpdateIntervalMillis(1000L)
        .build()

    // D-05: tunable within the locked ~30-50m range.
    private val accuracyThresholdMeters = 50f

    // D-03: locked example value.
    private val noiseFloorKmh = 2.0

    // Owns the StateFlow sharing; scoped to this provider's own lifetime (mirrors the
    // Activity that owns it — no ViewModel/DI layer in this project).
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile
    private var lastAcceptedUpdateAtMs: Long = 0L

    // MainActivity guarantees ACCESS_FINE_LOCATION is granted before `state` is ever
    // collected (permission check lives solely there — see class doc above).
    @Suppress("MissingPermission")
    private val rawLocations: Flow<Location> = callbackFlow {
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it) }
            }
        }
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { client.removeLocationUpdates(callback) }
    }

    private val acceptedKmh: Flow<Int> = rawLocations
        .map { loc ->
            mapSpeedToKmh(
                hasAccuracy = loc.hasAccuracy(),
                accuracyMeters = loc.accuracy,
                hasSpeed = loc.hasSpeed(),
                speedMetersPerSecond = loc.speed,
                accuracyThresholdMeters = accuracyThresholdMeters,
                noiseFloorKmh = noiseFloorKmh,
            )
        }
        .filterNotNull() // D-05: drop poor-accuracy readings, do not update the shown value
        .map { kmh ->
            // WR-01: SystemClock.elapsedRealtime() is monotonic and unaffected by wall-clock
            // adjustments (NTP/GPS time sync, manual clock changes), unlike
            // System.currentTimeMillis().
            lastAcceptedUpdateAtMs = SystemClock.elapsedRealtime()
            kmh
        }

    // 1-second ticker: also drives the once-per-second staleness check (GPS-01 cadence).
    private val ticker: Flow<Long> = flow {
        while (true) {
            emit(SystemClock.elapsedRealtime()) // WR-01: monotonic clock, see note above.
            delay(1000)
        }
    }

    val state: StateFlow<SpeedState> = combine(
        acceptedKmh.map { it as Int? }.onStart { emit(null) }, // null until first fix (D-01)
        ticker,
    ) { lastKmh, now ->
        when {
            lastKmh == null -> SpeedState.Searching // D-01: no accepted fix yet
            now - lastAcceptedUpdateAtMs > 5000L -> SpeedState.NoSignal // D-02
            else -> SpeedState.Reading(lastKmh)
        }
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = SpeedState.Searching,
    )
}
