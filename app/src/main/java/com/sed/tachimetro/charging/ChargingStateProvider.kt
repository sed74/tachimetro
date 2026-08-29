package com.sed.tachimetro.charging

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

import androidx.core.content.ContextCompat

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Wraps the sticky `ACTION_BATTERY_CHANGED` broadcast in a [callbackFlow] and exposes a
 * continuous [StateFlow] of [ChargingState], applying [deriveChargingState] (D-01/D-02/D-03).
 *
 * Unlike [com.sed.tachimetro.gps.GpsSpeedProvider], this provider needs no staleness/ticker
 * logic: `ACTION_BATTERY_CHANGED` is a sticky broadcast, so the current status is delivered
 * immediately on registration and there is no "no signal" concept for it.
 */
class ChargingStateProvider(context: Context) {

    // WR-04: BroadcastReceiver registration doesn't need an Activity context; use
    // applicationContext defensively so this provider never retains/leaks an Activity,
    // regardless of what the caller passes in.
    private val appContext = context.applicationContext

    // Owns the StateFlow sharing; scoped to this provider's own lifetime (mirrors the
    // Activity that owns it — no ViewModel/DI layer in this project).
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // ACTION_BATTERY_CHANGED is sticky: registering the receiver immediately delivers an
    // Intent with the current battery state, so — unlike GpsSpeedProvider — no
    // onStart { emit(null) } or ticker/staleness handling is needed here.
    private val rawBatteryStatus: Flow<Int> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1))
            }
        }
        ContextCompat.registerReceiver(
            appContext,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        awaitClose { appContext.unregisterReceiver(receiver) }
    }

    // StateFlow conflates equal values: ACTION_BATTERY_CHANGED also fires on every
    // percentage/temperature change, but as long as the derived state stays e.g. Pulsing the
    // collector receives no new emission, so a downstream fill animation is never restarted.
    val state: StateFlow<ChargingState> = rawBatteryStatus
        .map { status -> deriveChargingState(status) }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = ChargingState.Hidden,
        )

    // WR-04: explicit teardown for the provider's own scope, for symmetry/defensiveness.
    // repeatOnLifecycle(STARTED) already stops collecting `state` on onStop()/activity
    // destroy, which (via WhileSubscribed()) stops the upstream receiver registration; this
    // additionally cancels the SupervisorJob scope itself. Call from
    // MainActivity.onDestroy() when the Activity (and this provider instance) is being torn
    // down for good, e.g. on a configuration change that recreates the Activity.
    fun close() {
        scope.cancel()
    }
}

/**
 * WR-02: pure, unit-testable extraction of the [ChargingStateProvider.state] decision logic
 * (D-01/D-02/D-03), independent of Flow/coroutines machinery.
 *
 * @param batteryStatus one of `BatteryManager.BATTERY_STATUS_*`, or `-1` when `EXTRA_STATUS`
 *   is absent from the `ACTION_BATTERY_CHANGED` intent.
 */
fun deriveChargingState(batteryStatus: Int): ChargingState = when (batteryStatus) {
    BatteryManager.BATTERY_STATUS_CHARGING -> ChargingState.Pulsing // D-02
    BatteryManager.BATTERY_STATUS_FULL -> ChargingState.Full // D-03
    else -> ChargingState.Hidden // D-01: not connected / unknown / missing extra
}
