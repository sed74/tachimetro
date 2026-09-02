package com.sed.tachimetro.car

import androidx.car.app.connection.CarConnection
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Plain JVM unit tests for [resolveEffectiveKeepScreenOn] -- no Android runtime, nessun mocking,
 * nessun Robolectric. [CarLinkStateTest] copre la tabella di verita' punto per punto; questo file
 * copre la dimensione TEMPORALE, cioe' la proprieta' che CONN-02 chiama "senza reset indesiderati":
 * il valore riapplicato alla disconnessione deve dipendere SOLO dalla preferenza e dallo stato
 * corrente, mai dal numero o dall'ordine delle transizioni precedenti. E' anche la copertura a
 * costo zero, a livello logico, del Success Criterion 3 della Fase 11 (cicli rapidi di
 * connessione/disconnessione).
 */
class CarLinkSequenceTest {

    @Test
    fun alternatingSequence_withPreferenceOn_restoresPreferenceOnEveryDisconnect() {
        // pref = true, sequenza Disconnected,Connected,Disconnected,Connected,Connected,Disconnected
        // -> true,false,true,false,false,true
        val sequence = listOf(
            CarLinkState.Disconnected,
            CarLinkState.Connected,
            CarLinkState.Disconnected,
            CarLinkState.Connected,
            CarLinkState.Connected,
            CarLinkState.Disconnected,
        )
        val expected = listOf(true, false, true, false, false, true)
        assertEquals(expected, sequence.map { resolveEffectiveKeepScreenOn(savedPreference = true, link = it) })
    }

    @Test
    fun alternatingSequence_withPreferenceOff_staysFalseThroughout() {
        // pref = false, stessa sequenza -> false,false,false,false,false,false
        val sequence = listOf(
            CarLinkState.Disconnected,
            CarLinkState.Connected,
            CarLinkState.Disconnected,
            CarLinkState.Connected,
            CarLinkState.Connected,
            CarLinkState.Disconnected,
        )
        val expected = listOf(false, false, false, false, false, false)
        assertEquals(expected, sequence.map { resolveEffectiveKeepScreenOn(savedPreference = false, link = it) })
    }

    @Test
    fun twentyCycles_leavePreferenceUnchanged() {
        // Dopo 20 alternanze Connected/Disconnected il valore effettivo su Disconnected e' ancora
        // esattamente pref: la funzione e' senza stato, nessuna sequenza per quanto lunga puo'
        // produrre un valore diverso da quello che produrrebbe la stessa transizione isolata.
        val sequence = List(40) { index ->
            if (index % 2 == 0) CarLinkState.Connected else CarLinkState.Disconnected
        }
        for (pref in listOf(true, false)) {
            val lastOnDisconnected = sequence
                .map { resolveEffectiveKeepScreenOn(savedPreference = pref, link = it) }
                .last()
            assertEquals(pref, lastOnDisconnected)
        }
    }

    @Test
    fun sameLinkTwice_producesSameResult() {
        // Due invocazioni consecutive con lo stesso link producono lo stesso risultato
        // (idempotenza: la funzione non ha memoria).
        for (pref in listOf(true, false)) {
            for (link in listOf(CarLinkState.Connected, CarLinkState.Disconnected)) {
                val first = resolveEffectiveKeepScreenOn(savedPreference = pref, link = link)
                val second = resolveEffectiveKeepScreenOn(savedPreference = pref, link = link)
                assertEquals(first, second)
            }
        }
    }

    @Test
    fun rawConnectionType_toEffectiveFlag_neverAltersPreference() {
        // La catena completa "tipo di connessione grezzo -> flag effettivo" non introduce alcun
        // passaggio che possa alterare pref.
        for (pref in listOf(true, false)) {
            assertEquals(
                false,
                resolveEffectiveKeepScreenOn(pref, resolveCarLinkState(CarConnection.CONNECTION_TYPE_PROJECTION)),
            )
            assertEquals(
                pref,
                resolveEffectiveKeepScreenOn(pref, resolveCarLinkState(CarConnection.CONNECTION_TYPE_NOT_CONNECTED)),
            )
        }
    }
}
