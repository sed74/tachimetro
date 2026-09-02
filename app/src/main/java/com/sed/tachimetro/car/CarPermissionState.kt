package com.sed.tachimetro.car

/**
 * Modello sealed dello stato del permesso di localizzazione lato schermo Android Auto (D-04).
 *
 * `CarContext.requestPermissions()` non riporta alcun segnale che distingua un primo rifiuto da
 * un rifiuto permanente (09-RESEARCH.md, verificato leggendo il sorgente di
 * `CarAppPermissionActivity`): a differenza di `Activity.shouldShowRequestPermissionRationale()`,
 * disponibile solo da un'Activity, uno `Screen` non ha alcun modo diretto di interrogare il
 * sistema. Questo file sostituisce quel segnale mancante con un contatore persistito
 * ([CarPermissionDenialStore]) e una soglia nota del comportamento di Android 11+ (minSdk 30 del
 * progetto, nessun branching per API level): dal secondo "Nega" consecutivo per lo stesso
 * permesso il sistema smette di riproporre il dialogo, quindi `denialCount >= 2` equivale a "un
 * nuovo tentativo non mostrerebbe nulla".
 *
 * Limitazione nota (09-RESEARCH.md Pattern 1 "Known limitation" / Pitfall 3): se il primo
 * rifiuto e' avvenuto sul telefono via `MainActivity` prima di qualsiasi collegamento Android
 * Auto, il contatore lato auto e' sfasato di uno e mostrera' "Riprova" una volta di piu' prima
 * di correggersi da solo al tentativo successivo. Degrado limitato e auto-correttivo, accettato
 * consapevolmente: NON va "risolto" tenendo un riferimento a `MainActivity` (anti-pattern
 * esplicito di CLAUDE.md, "Retaining Activity Reference in Long-Lived Components").
 */
sealed class CarPermissionState {
    /** Permesso concesso; unico stato in cui `SpeedScreen` (Piano 02) puo' collezionare `GpsSpeedProvider.state`. */
    data object Granted : CarPermissionState()

    /** D-05: mai richiesto ne' mai rifiutato dallo schermo auto; innesca la richiesta automatica. */
    data object NotRequested : CarPermissionState()

    /** D-01: richiesta in corso, dialogo di sistema mostrato SUL TELEFONO. */
    data object Waiting : CarPermissionState()

    /** D-02/D-04: rifiutato almeno una volta; [permanent] = true quando il sistema non mostrera' piu' il dialogo. */
    data class Denied(val permanent: Boolean) : CarPermissionState()
}

/**
 * Funzione pura che deriva lo stato del permesso lato schermo auto da un flag di concessione e
 * dal contatore persistito dei rifiuti (D-04).
 *
 * NOTA: questa funzione non produce MAI [CarPermissionState.Waiting] -- e' uno stato
 * transitorio che solo `SpeedScreen` puo' impostare mentre attende il callback di
 * `CarContext.requestPermissions()`, prima che quel callback riporti un esito (concesso/negato)
 * da passare qui come [granted].
 *
 * Soglia `denialCount >= 2`: da Android 11+ (minSdk 30 del progetto), dal secondo rifiuto il
 * sistema smette di riproporre il dialogo per quel permesso -- un ulteriore tentativo non
 * mostrerebbe nulla, quindi va trattato come rifiuto permanente.
 */
fun resolveCarPermissionState(granted: Boolean, denialCount: Int): CarPermissionState = when {
    granted -> CarPermissionState.Granted
    denialCount == 0 -> CarPermissionState.NotRequested
    else -> CarPermissionState.Denied(permanent = denialCount >= 2)
}

/** Protegge dalla lettura di un valore persistito manomesso o corrotto (gemella di `sanitizePersistedMax`). */
fun sanitizeDenialCount(raw: Int): Int = if (raw < 0) 0 else raw
