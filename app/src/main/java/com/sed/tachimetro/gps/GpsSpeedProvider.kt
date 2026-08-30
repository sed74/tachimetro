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
import kotlinx.coroutines.cancel
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
 * [StateFlow] of [SpeedState], applying the tested speed-mapping filters (see the single
 * call site below) and detecting startup/loss "no signal" per D-01/D-02.
 *
 * Permission note: this class does NOT check ACCESS_FINE_LOCATION itself. MainActivity
 * (Phase 1) is the single source of truth for that permission and only starts collecting
 * [state] once it has been granted — see RESEARCH.md Pitfall 1 / Anti-Patterns.
 */
class GpsSpeedProvider(context: Context) {

    // WR-04: FusedLocationProviderClient doesn't need an Activity context; use
    // applicationContext defensively so this provider never retains/leaks an Activity,
    // regardless of what the caller passes in.
    private val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)

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

    // D-06: reference point for the next distance delta (Location.distanceTo()). Updated on
    // EVERY accuracy-accepted fix, including ones the noise floor later rejects for kmh
    // display — Pitfall 2: otherwise, after a long stop, this would stay anchored to a stale
    // position and the resumed trip would add accumulated jitter in a single jump.
    @Volatile
    private var lastAcceptedLocation: Location? = null

    // MainActivity guarantees ACCESS_FINE_LOCATION is granted before `state` is ever
    // collected (permission check lives solely there — see class doc above).
    @Suppress("MissingPermission")
    private val rawLocations: Flow<Location> = callbackFlow {
        // WR-01: defensive reset -- every fresh subscription (first launch, or a resume after
        // WhileSubscribed() tore the previous one down on background/permission-revoke) must
        // not reuse a reference point from before the gap, otherwise the first fix after the
        // gap would compute a spurious one-off distance jump against a stale position.
        lastAcceptedLocation = null
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it) }
            }
        }
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { client.removeLocationUpdates(callback) }
    }

    // D-06/D-07: payload traveling the pipeline alongside kmh — the distance delta rides the
    // SAME accepted-fix pipeline that already produces kmh, so the shared accuracy/noise
    // filter (below) is never duplicated for distance.
    private data class AcceptedReading(val kmh: Int, val deltaMeters: Float)

    private val acceptedReadings: Flow<AcceptedReading> = rawLocations
        .map { loc ->
            val kmh = mapSpeedToKmh(
                hasAccuracy = loc.hasAccuracy(),
                accuracyMeters = loc.accuracy,
                hasSpeed = loc.hasSpeed(),
                speedMetersPerSecond = loc.speed,
                accuracyThresholdMeters = accuracyThresholdMeters,
                noiseFloorKmh = noiseFloorKmh,
            ) ?: return@map null // D-05: drop poor-accuracy readings, do not update the shown value

            // D-06: meters since the last accuracy-accepted fix, 0f if this is the first one.
            val delta = lastAcceptedLocation?.distanceTo(loc) ?: 0f
            // Pitfall 2: update unconditionally, even for fixes under the noise floor (kmh
            // == 0) — the noise-floor accumulation gate lives downstream in reduceDistance()
            // (Piano 01), not here.
            lastAcceptedLocation = loc
            AcceptedReading(kmh, delta)
        }
        .filterNotNull() // D-05: drop poor-accuracy readings, do not update the shown value
        .map { reading ->
            // WR-01: SystemClock.elapsedRealtime() is monotonic and unaffected by wall-clock
            // adjustments (NTP/GPS time sync, manual clock changes), unlike
            // System.currentTimeMillis().
            lastAcceptedUpdateAtMs = SystemClock.elapsedRealtime()
            reading
        }

    // 1-second ticker: also drives the once-per-second staleness check (GPS-01 cadence).
    private val ticker: Flow<Long> = flow {
        while (true) {
            emit(SystemClock.elapsedRealtime()) // WR-01: monotonic clock, see note above.
            delay(1000)
        }
    }

    val state: StateFlow<SpeedState> = combine(
        acceptedReadings.map { it as AcceptedReading? }.onStart { emit(null) }, // null until first fix (D-01)
        ticker,
    ) { last, now ->
        deriveSpeedState(last?.kmh, last?.deltaMeters ?: 0f, now, lastAcceptedUpdateAtMs)
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = SpeedState.Searching,
    )

    // WR-04: explicit teardown for the provider's own scope, for symmetry/defensiveness.
    // D-07 already has repeatOnLifecycle(STARTED) stop collecting `state` on
    // onStop()/activity destroy, which (via WhileSubscribed()) stops the upstream location
    // updates; this additionally cancels the SupervisorJob scope itself. Call from
    // MainActivity.onDestroy() when the Activity (and this provider instance) is being torn
    // down for good, e.g. on a configuration change that recreates the Activity.
    fun close() {
        scope.cancel()
    }
}

/**
 * WR-02: pure, unit-testable extraction of the [GpsSpeedProvider.state] decision logic
 * (D-01/D-02), independent of Flow/`combine`/coroutines machinery.
 *
 * @param lastKmh the latest accepted km/h reading, or `null` if no fix has been accepted yet.
 * @param lastDeltaMeters D-06: meters traveled since the previous accepted fix, carried
 *   through unchanged into [SpeedState.Reading] when the reading is fresh.
 * @param now the current monotonic timestamp ([android.os.SystemClock.elapsedRealtime]).
 * @param lastAcceptedAtMs the monotonic timestamp of the last accepted reading.
 */
fun deriveSpeedState(
    lastKmh: Int?,
    lastDeltaMeters: Float,
    now: Long,
    lastAcceptedAtMs: Long,
): SpeedState = when {
    lastKmh == null -> SpeedState.Searching // D-01: no accepted fix yet
    now - lastAcceptedAtMs > 5000L -> SpeedState.NoSignal // D-02
    else -> SpeedState.Reading(lastKmh, lastDeltaMeters)
}
