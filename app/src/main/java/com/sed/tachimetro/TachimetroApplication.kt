package com.sed.tachimetro

import android.app.Application

import com.sed.tachimetro.gps.GpsSpeedProvider

/**
 * D-00b: process-wide `Application` subclass that owns the single, shared [GpsSpeedProvider]
 * instance. Before this milestone, [GpsSpeedProvider] was constructed directly by
 * `MainActivity` and scoped to its lifetime; now it is scoped to the process so it can be
 * the ONE source of GPS truth shared between `MainActivity` (phone screen) and the future
 * `SpeedScreen` (Android Auto car screen, Piano 02) — a second independent
 * `FusedLocationProviderClient` subscription would double battery drain and risk the two
 * screens showing momentarily disagreeing speed values (RESEARCH PITFALLS.md Pitfall 5).
 *
 * No collection is started here: [GpsSpeedProvider.state]'s `SharingStarted.WhileSubscribed()`
 * already decides on its own when to start/stop the upstream location updates, based purely on
 * whether any collector (phone, car, or both) is currently active.
 */
class TachimetroApplication : Application() {

    // WR-04: applicationContext is exactly what GpsSpeedProvider's constructor expects --
    // no Activity is ever passed. `by lazy` (not a DI framework, D-00b explicit) defers
    // constructing the location client until the first collector actually needs it, instead
    // of paying that cost on every process start regardless of whether any surface is active.
    val gpsSpeedProvider: GpsSpeedProvider by lazy { GpsSpeedProvider(applicationContext) }
}
