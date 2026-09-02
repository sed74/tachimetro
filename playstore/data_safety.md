# Data Safety form — bozza risposte per Play Console

Sezione Play Console: **Criteri per i contenuti > Sicurezza dei dati / Data safety**.
Basato sul comportamento reale del codice (vedi `AndroidManifest.xml`: unico permesso `ACCESS_FINE_LOCATION`, nessun permesso `INTERNET`; `.planning/codebase/INTEGRATIONS.md`).

## Raccolta e condivisione dati

- **L'app raccoglie dati utente?** Sì.
- **L'app condivide dati utente con terze parti?** No.

## Tipi di dati

| Categoria | Tipo | Raccolto? | Condiviso? | Note |
|---|---|---|---|---|
| Posizione | Posizione precisa | Sì | No | Usata solo per calcolare la velocità mostrata a schermo; elaborata in memoria, mai trasmessa (nessun permesso INTERNET). **Nessuna coordinata GPS viene mai salvata su disco**: l'unico valore derivato dagli spostamenti che viene persistito localmente è la distanza percorsa, un singolo numero aggregato (metri totali) da cui non è ricostruibile alcun percorso né alcuna posizione. |
| Posizione | Posizione approssimativa | No | No | Non richiesta (solo ACCESS_FINE_LOCATION, nessun fallback coarse — vedi commento in AndroidManifest.xml). |

Tutte le altre categorie (informazioni personali, informazioni finanziarie, contatti, cronologia di navigazione, cronologia ricerche, identificativi dispositivo/altri, ecc.) → **non raccolte**.

## Per il dato "Posizione precisa"

- **Finalità della raccolta:** Funzionalità dell'app (mostrare la velocità corrente).
- **Il dato è facoltativo o obbligatorio?** Obbligatorio (l'app non ha funzione senza).
- **Il dato viene elaborato in modo effimero?** Sì — la posizione è usata al volo per il calcolo e non è persistita; l'unico residuo su disco è la distanza percorsa, un totale aggregato in metri, non una posizione.
- **Il dato viene trasmesso in modo crittografato in transito?** Non applicabile — non lascia mai il dispositivo.
- **Gli utenti possono richiedere la cancellazione dei dati?** Non applicabile — nessun dato viene trasmesso da cancellare; disinstallare l'app rimuove ogni traccia locale (max velocità sessione, distanza percorsa, preferenza schermo). Il pulsante "Azzera" nell'app riporta a zero sia la velocità massima sia la distanza percorsa in qualsiasi momento.

## Pratiche di sicurezza

- **Dati crittografati in transito:** Non applicabile (nessuna trasmissione di rete — l'app non dichiara il permesso `INTERNET`).
- **Gli utenti possono richiedere la cancellazione dei dati:** Sì, implicitamente (nessun dato lascia il device; la disinstallazione cancella tutto).

## Nota per chi compila il form in Play Console

Alla versione 1.1 l'app persiste localmente **tre** sole voci, tutte in SharedPreferences app-private:

1. **Velocità massima di sessione** — un numero intero in km/h (`MaxSpeedStore`);
2. **Distanza percorsa dall'ultimo azzeramento** — un singolo numero in metri (`DistanceStore`), un totale aggregato, non un tracciato di posizioni;
3. **Preferenza "schermo sempre acceso"** — un valore vero/falso (`ScreenOnPreferenceStore`).

Sono semplici valori funzionali, non raccolgono identificatori, non lasciano mai il dispositivo e non vengono dichiarate come "raccolta dati" nel form standard di Play Console (che riguarda dati raccolti/trasmessi, non le SharedPreferences locali) — ma è corretto menzionarle qui per completezza e trasparenza. Tutte e tre sono cancellabili dall'utente: le prime due con il pulsante "Azzera" nell'app, tutte con la disinstallazione.
