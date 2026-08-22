# Data Safety form — bozza risposte per Play Console

Sezione Play Console: **Criteri per i contenuti > Sicurezza dei dati / Data safety**.
Basato sul comportamento reale del codice (vedi `AndroidManifest.xml`: unico permesso `ACCESS_FINE_LOCATION`, nessun permesso `INTERNET`; `.planning/codebase/INTEGRATIONS.md`).

## Raccolta e condivisione dati

- **L'app raccoglie dati utente?** Sì.
- **L'app condivide dati utente con terze parti?** No.

## Tipi di dati

| Categoria | Tipo | Raccolto? | Condiviso? | Note |
|---|---|---|---|---|
| Posizione | Posizione precisa | Sì | No | Usata solo per calcolare la velocità mostrata a schermo; elaborata in memoria, mai salvata su disco, mai trasmessa (nessun permesso INTERNET). |
| Posizione | Posizione approssimativa | No | No | Non richiesta (solo ACCESS_FINE_LOCATION, nessun fallback coarse — vedi commento in AndroidManifest.xml). |

Tutte le altre categorie (informazioni personali, informazioni finanziarie, contatti, cronologia di navigazione, cronologia ricerche, identificativi dispositivo/altri, ecc.) → **non raccolte**.

## Per il dato "Posizione precisa"

- **Finalità della raccolta:** Funzionalità dell'app (mostrare la velocità corrente).
- **Il dato è facoltativo o obbligatorio?** Obbligatorio (l'app non ha funzione senza).
- **Il dato viene elaborato in modo effimero?** Sì — usato al volo per il calcolo, non persistito.
- **Il dato viene trasmesso in modo crittografato in transito?** Non applicabile — non lascia mai il dispositivo.
- **Gli utenti possono richiedere la cancellazione dei dati?** Non applicabile — nessun dato viene salvato o trasmesso da cancellare; disinstallare l'app rimuove ogni traccia locale (max velocità sessione, preferenza schermo).

## Pratiche di sicurezza

- **Dati crittografati in transito:** Non applicabile (nessuna trasmissione di rete — l'app non dichiara il permesso `INTERNET`).
- **Gli utenti possono richiedere la cancellazione dei dati:** Sì, implicitamente (nessun dato lascia il device; la disinstallazione cancella tutto).

## Nota per chi compila il form in Play Console

Le uniche due voci persistite localmente (max velocità sessione, preferenza "schermo sempre acceso") sono semplici preferenze funzionali, non raccolgono identificatori e non vengono mai dichiarate come "raccolta dati" nel form standard di Play Console (che riguarda dati raccolti/trasmessi, non le SharedPreferences locali) — ma è corretto menzionarle qui per completezza e trasparenza.
