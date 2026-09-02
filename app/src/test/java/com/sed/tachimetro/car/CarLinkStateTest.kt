package com.sed.tachimetro.car

import androidx.car.app.connection.CarConnection
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Plain JVM unit tests for [resolveCarLinkState] and [resolveEffectiveKeepScreenOn] -- no Android
 * runtime, nessun mocking, nessun Robolectric. Lockano CONN-01/CONN-02: la mappatura fail-safe del
 * tipo di connessione riportato da `CarConnection` e la tabella di verita' del flag effettivo
 * "schermo sempre acceso".
 */
class CarLinkStateTest {

    @Test
    fun projection_returnsConnected() {
        // resolveCarLinkState(CarConnection.CONNECTION_TYPE_PROJECTION) == Connected
        assertEquals(
            CarLinkState.Connected,
            resolveCarLinkState(CarConnection.CONNECTION_TYPE_PROJECTION),
        )
    }

    @Test
    fun notConnected_returnsDisconnected() {
        // resolveCarLinkState(CarConnection.CONNECTION_TYPE_NOT_CONNECTED) == Disconnected
        assertEquals(
            CarLinkState.Disconnected,
            resolveCarLinkState(CarConnection.CONNECTION_TYPE_NOT_CONNECTED),
        )
    }

    @Test
    fun native_returnsDisconnected() {
        // resolveCarLinkState(CarConnection.CONNECTION_TYPE_NATIVE) == Disconnected -- esecuzione
        // nativa su Automotive OS, nessun secondo schermo telefono da neutralizzare.
        assertEquals(
            CarLinkState.Disconnected,
            resolveCarLinkState(CarConnection.CONNECTION_TYPE_NATIVE),
        )
    }

    @Test
    fun nullValue_returnsDisconnected() {
        // resolveCarLinkState(null) == Disconnected -- nessun valore ancora emesso dal provider.
        assertEquals(CarLinkState.Disconnected, resolveCarLinkState(null))
    }

    @Test
    fun negativeValue_returnsDisconnected() {
        // resolveCarLinkState(-1) == Disconnected -- valore spurio, fail-safe.
        assertEquals(CarLinkState.Disconnected, resolveCarLinkState(-1))
    }

    @Test
    fun unknownFutureValue_returnsDisconnected() {
        // resolveCarLinkState(99) == Disconnected -- valore futuro/sconosciuto, fail-safe.
        assertEquals(CarLinkState.Disconnected, resolveCarLinkState(99))
    }

    @Test
    fun disconnected_savedPreferenceTrue_returnsTrue() {
        // resolveEffectiveKeepScreenOn(savedPreference = true, link = Disconnected) == true
        assertEquals(
            true,
            resolveEffectiveKeepScreenOn(savedPreference = true, link = CarLinkState.Disconnected),
        )
    }

    @Test
    fun disconnected_savedPreferenceFalse_returnsFalse() {
        // resolveEffectiveKeepScreenOn(savedPreference = false, link = Disconnected) == false
        assertEquals(
            false,
            resolveEffectiveKeepScreenOn(savedPreference = false, link = CarLinkState.Disconnected),
        )
    }

    @Test
    fun connected_savedPreferenceTrue_returnsFalse() {
        // resolveEffectiveKeepScreenOn(savedPreference = true, link = Connected) == false -- CONN-01:
        // il flag viene rilasciato anche se la preferenza era attiva.
        assertEquals(
            false,
            resolveEffectiveKeepScreenOn(savedPreference = true, link = CarLinkState.Connected),
        )
    }

    @Test
    fun connected_savedPreferenceFalse_returnsFalse() {
        // resolveEffectiveKeepScreenOn(savedPreference = false, link = Connected) == false
        assertEquals(
            false,
            resolveEffectiveKeepScreenOn(savedPreference = false, link = CarLinkState.Connected),
        )
    }

    @Test
    fun roundTrip_afterConnectedThenDisconnected_returnsOriginalPreference() {
        // CONN-02: per ogni pref in [true, false], dopo un passaggio da Connected il ritorno a
        // Disconnected restituisce di nuovo esattamente pref -- la funzione e' senza stato.
        for (pref in listOf(true, false)) {
            resolveEffectiveKeepScreenOn(savedPreference = pref, link = CarLinkState.Connected)
            assertEquals(
                pref,
                resolveEffectiveKeepScreenOn(savedPreference = pref, link = CarLinkState.Disconnected),
            )
        }
    }
}
