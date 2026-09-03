package com.sed.tachimetro.car

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import com.sed.tachimetro.BuildConfig

/**
 * Entry point bindato dall'host Android Auto via Binder IPC (categoria POI, D-00a). L'host
 * scopre questo servizio tramite il `<service>` dichiarato in AndroidManifest.xml con
 * intent-filter `androidx.car.app.CarAppService` / categoria `androidx.car.app.category.POI`.
 * `android:exported="true"` sul manifest e' un requisito di piattaforma: senza di esso l'host
 * non potrebbe bindare il servizio.
 */
class TachimetroCarAppService : CarAppService() {

    // Gate di binding, split debug/release voluto esplicitamente dall'utente (D-01):
    // - build di RELEASE (BuildConfig.DEBUG == false) -> allow-list reale, solo gli host ufficiali
    //   Android Auto / Automotive Templates; e' cio' che chiude le due disposizioni "accept"
    //   ereditate T-08-05 e T-09-10, per cui il servizio accettava il binding da qualunque host;
    // - build di DEBUG -> validatore permissivo di proposito, per non rompere il flusso di
    //   sviluppo con il Desktop Head Unit (si presenta con package/firma non in allow-list).
    //   I build di debug non vengono distribuiti: il Play Store riceve solo il ramo release.
    // BuildConfig.DEBUG resta QUI, al call site, e non dentro il seam: e' cio' che rende entrambi
    // i rami raggiungibili da CarHostValidationTest. applicationContext per WR-04 (mai una
    // Activity/CarContext in un componente a vita lunga). Dettaglio dell'allow-list e motivazione
    // della fonte scelta: CarHostValidation.kt.
    override fun createHostValidator(): HostValidator =
        createCarHostValidator(applicationContext, BuildConfig.DEBUG)

    override fun onCreateSession(): Session = TachimetroCarSession()
}
