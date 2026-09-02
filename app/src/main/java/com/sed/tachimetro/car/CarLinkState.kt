package com.sed.tachimetro.car

import androidx.car.app.connection.CarConnection

/**
 * Modello sealed dello stato del collegamento Android Auto osservato LATO TELEFONO (CONN-01,
 * CONN-02).
 *
 * La connessione viene osservata con `androidx.car.app.connection.CarConnection`, indipendente da
 * `TachimetroCarAppService`/`TachimetroCarSession`/`SpeedScreen` (Fase 8-9): nessuna comunicazione
 * fra i due processi/superfici e' necessaria, `CarConnection` interroga direttamente il provider
 * esposto dall'app Android Auto. L'elemento `<queries>` richiesto dal package visibility di
 * Android 11+ per interrogare quel provider e' gia' dichiarato nel manifest della libreria
 * `androidx.car.app:app` e viene unito automaticamente al manifest finale: questa fase non
 * aggiunge alcun `<queries>` ne' alcun permesso al manifest dell'app.
 */
sealed class CarLinkState {
    /** Nessuna proiezione Android Auto in corso; il telefono si comporta esattamente come in v1.0/v1.1 (CONN-02). */
    data object Disconnected : CarLinkState()

    /** Android Auto sta proiettando su un display auto (`CONNECTION_TYPE_PROJECTION`); il telefono passa allo stato neutro e rilascia `FLAG_KEEP_SCREEN_ON` (CONN-01). */
    data object Connected : CarLinkState()
}

/**
 * Funzione pura che deriva [CarLinkState] dal tipo di connessione riportato da
 * `CarConnection.getType()` (CONN-01).
 *
 * Il parametro e' NULLABLE di proposito: `CarConnection.getType()` e' un `LiveData<Integer>` che
 * in Kotlin arriva come piattaforma-type e puo' non avere ancora emesso alcun valore; assorbire il
 * `null` qui evita un `?:` con valore magico nel chiamante (Piano 02).
 *
 * `CONNECTION_TYPE_NATIVE` NON e' trattato come "connesso" ai fini di questa fase: su Android
 * Automotive OS l'app gira nativamente sull'head unit, non esiste un secondo schermo telefono da
 * mettere in stato neutro -- trattarlo come connesso oscurerebbe l'unica interfaccia esistente.
 *
 * Il default e' [CarLinkState.Disconnected] (fail-safe, T-10-02): un valore spurio, manomesso o
 * futuro/sconosciuto non deve MAI poter sostituire il tachimetro con un messaggio neutro sul
 * telefono. L'unico ramo che produce [CarLinkState.Connected] e' il confronto con la costante
 * simbolica `CarConnection.CONNECTION_TYPE_PROJECTION` (mai un letterale numerico duplicato: la
 * costante e' un `static final int` compile-time, quindi il compilatore Kotlin la inlinea e questo
 * file resta eseguibile da un test JVM puro senza runtime Android).
 */
fun resolveCarLinkState(connectionType: Int?): CarLinkState =
    if (connectionType == CarConnection.CONNECTION_TYPE_PROJECTION) {
        CarLinkState.Connected
    } else {
        CarLinkState.Disconnected
    }

/**
 * Funzione pura che deriva il valore EFFETTIVO di "schermo sempre acceso" dalla preferenza salvata
 * e dallo stato del collegamento Android Auto (CONN-01 + CONN-02 in un solo punto).
 *
 * Contratto architetturale su cui poggia CONN-02: questa funzione non ha alcun accesso a
 * `ScreenOnPreferenceStore`; riceve [savedPreference] come parametro e restituisce un valore
 * DERIVATO. Non esiste quindi alcun percorso in cui una transizione di connessione possa scrivere
 * la preferenza persistita (T-10-03). Il chiamante (Piano 02) deve chiamare
 * `screenOnStore.write()` SOLO dal listener dello switch, mai da qui.
 *
 * Lo switch dell'interfaccia continua a riflettere la PREFERENZA SALVATA e non il flag effettivo:
 * mentre Android Auto e' connesso lo switch puo' restare su "acceso" mentre il flag e' rilasciato,
 * ed e' il comportamento voluto (CONN-02: "senza alterare la preferenza memorizzata").
 */
fun resolveEffectiveKeepScreenOn(savedPreference: Boolean, link: CarLinkState): Boolean =
    when (link) {
        CarLinkState.Connected -> false
        CarLinkState.Disconnected -> savedPreference
    }
