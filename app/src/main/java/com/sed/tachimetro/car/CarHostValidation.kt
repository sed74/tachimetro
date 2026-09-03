package com.sed.tachimetro.car

import android.annotation.SuppressLint
import android.content.Context
import androidx.car.app.validation.HostValidator

/**
 * Fabbrica del [HostValidator] usato da [TachimetroCarAppService] per decidere CHI puo' bindare il
 * car service via Binder IPC. E' l'unico controllo dell'app su quel confine di fiducia: chiunque
 * superi questo gate pilota la superficie auto del tachimetro (AA-01, AA-02, AA-03, AA-04).
 *
 * Questa funzione chiude le due disposizioni "accept" ereditate T-08-05 (Fase 8) e T-09-10
 * (Fase 9): fino alla Fase 10 compresa il servizio restituiva incondizionatamente
 * `HostValidator.ALLOW_ALL_HOSTS_VALIDATOR` e accettava quindi il binding da QUALUNQUE host.
 *
 * ## Split debug/release (D-01)
 * Lo split e' una scelta esplicita dell'utente e NON va lasciata implicita:
 * - [allowAllHosts] `true` (build di DEBUG) -> validatore permissivo. Serve a non rompere il flusso
 *   di sviluppo con il Desktop Head Unit, che si presenta con un package/firma di debug non
 *   presenti in nessuna allow-list ufficiale. I build di debug non vengono distribuiti (il Play
 *   Store riceve solo il ramo release), quindi la mancata dev/prod parity e' un rischio accettato
 *   e documentato (T-11-05).
 * - [allowAllHosts] `false` (build di RELEASE) -> allow-list reale, limitata ai due soli host
 *   ufficiali `com.google.android.projection.gearhead` (Android Auto) e
 *   `com.google.android.apps.automotive.templates.host` (Automotive OS Templates), ciascuno con i
 *   tre digest di firma di Google (dev, beta, release).
 *
 * Il valore di [allowAllHosts] e' un PARAMETRO e non una lettura diretta di `BuildConfig.DEBUG`
 * fatta qui dentro: il flag viene valorizzato al call site (vedi
 * [TachimetroCarAppService.createHostValidator]) esattamente come `resolveEffectiveKeepScreenOn`
 * riceve `savedPreference` e `SpeedScreen.buildTemplate` riceve `permission`. E' cio' che rende
 * ENTRAMBI i rami raggiungibili da un unico test strumentato (`CarHostValidationTest`), dato che
 * `androidx.car.app:app-testing` non offre alcun controller per istanziare un `CarAppService`.
 *
 * ## Nessun fallback permissivo (T-11-04)
 * Non esiste alcun `try`/`catch` attorno alla costruzione del validator e nessun ramo che
 * restituisca `ALLOW_ALL_HOSTS_VALIDATOR` come esito di un errore: il ramo permissivo e'
 * raggiungibile SOLO tramite `allowAllHosts == true`. E' la stessa regola "fail-safe allow-list"
 * gia' applicata da [resolveCarLinkState], dove un solo ramo produce l'esito fiducioso. Se la
 * risorsa dell'allow-list fosse malformata, `addAllowedHosts` lancia `IllegalArgumentException` e
 * il crash e' l'esito CORRETTO: un fallimento rumoroso e' preferibile a un car service che si apre
 * a chiunque.
 *
 * ## Fonte dell'allow-list (D-02)
 * Si usa la risorsa della libreria `androidx.car.app.R.array.hosts_allowlist_sample` invece di una
 * copia locale in `app/src/main/res/values/`: la copia richiederebbe la trascrizione a mano di 6
 * digest SHA-256 (errore silenzioso -- un digest sbagliato produce un validator che compila, si
 * installa e rifiuta ogni host reale) e non seguirebbe le rotazioni delle chiavi di firma di
 * Google.
 *
 * @param context Contesto usato da `HostValidator.Builder` solo per `getResources()` e
 *   `getPackageManager()`. Il chiamante deve passare `applicationContext` (WR-04): mai una
 *   Activity ne' un `CarContext`, perche' il validator vive quanto il servizio.
 * @param allowAllHosts `true` solo nei build di debug (D-01); vedi sopra.
 * @return il validator permissivo oppure quello con l'allow-list reale dei due host ufficiali.
 */
// @SuppressLint("PrivateResource"): hosts_allowlist_sample non compare nel public.txt dell'AAR
// androidx.car.app:app:1.7.0, quindi le regole di visibilita' delle risorse di AGP la considerano
// privata -- ma e' esattamente il riferimento usato dalla documentazione ufficiale Google, ed e'
// l'unica fonte che resta allineata alle rotazioni delle chiavi di firma. La copia locale dei 6
// digest e' stata valutata e scartata (D-02): trascrizione a mano soggetta a errore silenzioso.
// Soppressione stretta al solo call site, stessa disciplina di @Suppress("MissingPermission") in
// GpsSpeedProvider.kt.
@SuppressLint("PrivateResource")
fun createCarHostValidator(context: Context, allowAllHosts: Boolean): HostValidator =
    if (allowAllHosts) {
        HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    } else {
        HostValidator.Builder(context)
            .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
            .build()
    }
