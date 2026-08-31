package com.sed.tachimetro.car

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

/**
 * Entry point bindato dall'host Android Auto via Binder IPC (categoria POI, D-00a). L'host
 * scopre questo servizio tramite il `<service>` dichiarato in AndroidManifest.xml con
 * intent-filter `androidx.car.app.CarAppService` / categoria `androidx.car.app.category.POI`.
 * `android:exported="true"` sul manifest e' un requisito di piattaforma: senza di esso l'host
 * non potrebbe bindare il servizio.
 */
class TachimetroCarAppService : CarAppService() {

    // Validatore permissivo accettabile solo per lo scaffold e per il test DHU di questa fase;
    // la sostituzione con una allow-list reale e' scope esplicito della Fase 11
    // (ROADMAP.md Phase 11 SC1) e NON va anticipata qui.
    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR

    override fun onCreateSession(): Session = TachimetroCarSession()
}
