package com.sed.tachimetro.car

import android.content.Context

import androidx.car.app.validation.HostValidator
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test STRUMENTATO di [createCarHostValidator], il gate di binding del car service (Piano 11-01).
 *
 * Perche' strumentato e non un test JVM puro in `app/src/test/` come [CarLinkStateTest]:
 * `HostValidator.Builder` chiama `context.getResources().getStringArray(...)` per leggere
 * l'allow-list (verificato sul sources-jar di `androidx.car.app:app:1.7.0`). Serve quindi un
 * `Context` reale con le risorse mergiate dell'app, che un test JVM non puo' fornire -- stessa
 * ragione per cui [SpeedScreenTemplateTest] vive in `androidTest`.
 *
 * Diversamente da [SpeedScreenTemplateTest] qui NON serve `runOnMainSync`: quel wrapper esiste
 * la' solo perche' `Screen`/`LifecycleRegistry` impongono il thread principale, mentre
 * `HostValidator.Builder` non ha alcun vincolo di threading.
 *
 * Copertura esaustiva di entrambi i rami, nello stile fail-safe gia' usato da [CarLinkStateTest]:
 * il ramo permissivo (D-01) e il ramo con allow-list reale, piu' i due modi di fallire silenziosi
 * dell'allow-list (mappa vuota, coppie `<digest>,<package>` invertite). Le asserzioni non usano
 * mai i valori letterali dei digest SHA-256: fallirebbero a ogni rotazione delle chiavi di firma
 * di Google. Si asserisce la STRUTTURA e i package, mai i digest specifici.
 */
@RunWith(AndroidJUnit4::class)
class CarHostValidationTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    /** Il validator del ramo release, costruito una volta per test. */
    private fun releaseValidator(): HostValidator =
        createCarHostValidator(context, allowAllHosts = false)

    @Test
    fun debugBranch_returnsAllowAllValidator() {
        // D-01: nei build di debug il validatore resta permissivo di proposito, altrimenti il
        // Desktop Head Unit (package/firma di sviluppo) non riuscirebbe piu' a bindare il servizio.
        assertSame(
            HostValidator.ALLOW_ALL_HOSTS_VALIDATOR,
            createCarHostValidator(context, allowAllHosts = true),
        )
    }

    @Test
    fun releaseBranch_isNotTheAllowAllValidator() {
        // T-11-01: e' l'asserzione che fallisce se il flag venisse invertito al call site o se il
        // ramo release regredisse a ALLOW_ALL_HOSTS_VALIDATOR.
        assertNotSame(HostValidator.ALLOW_ALL_HOSTS_VALIDATOR, releaseValidator())
    }

    @Test
    fun releaseBranch_allowListIsNotEmpty() {
        // T-11-03: un res id sbagliato non lancia eccezione, produce una mappa VUOTA -- cioe' un
        // validator che rifiuta ogni host, incluso quello legittimo.
        assertFalse(releaseValidator().allowedHosts.isEmpty())
    }

    @Test
    fun releaseBranch_allowListContainsAndroidAutoHost() {
        // T-11-02: l'host Android Auto (proiezione su telefono) deve essere in allow-list con
        // almeno un digest di firma.
        val digests = releaseValidator().allowedHosts["com.google.android.projection.gearhead"]
        assertTrue(digests != null && digests.isNotEmpty())
    }

    @Test
    fun releaseBranch_allowListContainsAutomotiveTemplatesHost() {
        // T-11-02: idem per l'host Templates di Android Automotive OS.
        val digests =
            releaseValidator().allowedHosts["com.google.android.apps.automotive.templates.host"]
        assertTrue(digests != null && digests.isNotEmpty())
    }

    @Test
    fun releaseBranch_digestsAreNotPackageNames() {
        // T-11-03: addAllowedHosts accetta voci nel formato `<digest>,<package>` e chiama
        // addAllowedHost(parti[1], parti[0]). Invertire i due campi NON lancia eccezione: produce
        // un validator che compila, si installa e rifiuta ogni host reale. Un digest SHA-256 e'
        // esadecimale, quindi non contiene mai il carattere '.' di un package name.
        for ((packageName, digests) in releaseValidator().allowedHosts) {
            for (digest in digests) {
                assertTrue("digest vuoto per $packageName", digest.isNotEmpty())
                assertFalse(
                    "digest '$digest' di $packageName sembra un package name (contiene '.')",
                    digest.contains('.'),
                )
            }
        }
    }
}
