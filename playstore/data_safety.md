# Data Safety form — bozza risposte per Play Console

Sezione Play Console: **Criteri per i contenuti > Sicurezza dei dati / Data safety**.
Basato sul comportamento reale del codice (vedi `AndroidManifest.xml`: unico permesso `ACCESS_FINE_LOCATION`, nessun permesso `INTERNET`; `.planning/codebase/INTEGRATIONS.md`).

Alla versione 2.0, che introduce il supporto Android Auto, il manifest dell'app continua a dichiarare il solo `ACCESS_FINE_LOCATION` e nessun `INTERNET`: le funzionalità Android Auto non hanno richiesto alcun permesso nuovo.

## Raccolta e condivisione dati

- **L'app raccoglie dati utente?** Sì.
- **L'app condivide dati utente con terze parti?** No.

## Tipi di dati

| Categoria | Tipo | Raccolto? | Condiviso? | Note |
|---|---|---|---|---|
| Posizione | Posizione precisa | Sì | No | Usata solo per calcolare la velocità mostrata a schermo; elaborata in memoria, mai trasmessa (nessun permesso INTERNET). **Nessuna coordinata GPS viene mai salvata su disco**: l'unico valore derivato dagli spostamenti che viene persistito localmente è la distanza percorsa, un singolo numero aggregato (metri totali) da cui non è ricostruibile alcun percorso né alcuna posizione. Quando l'app è connessa ad Android Auto, la stessa posizione GPS già raccolta alimenta anche lo schermo dell'auto: nessuna raccolta aggiuntiva, nessun permesso nuovo, nessuna trasmissione — la sorgente GPS è condivisa fra telefono e auto invece di essere duplicata. |
| Posizione | Posizione approssimativa | No | No | Non richiesta (solo ACCESS_FINE_LOCATION, nessun fallback coarse — vedi commento in AndroidManifest.xml). |

Tutte le altre categorie (informazioni personali, informazioni finanziarie, contatti, cronologia di navigazione, cronologia ricerche, identificativi dispositivo/altri, ecc.) → **non raccolte**.

## Per il dato "Posizione precisa"

- **Finalità della raccolta:** Funzionalità dell'app (mostrare la velocità corrente).
- **Il dato è facoltativo o obbligatorio?** Obbligatorio (l'app non ha funzione senza).
- **Il dato viene elaborato in modo effimero?** Sì — la posizione è usata al volo per il calcolo e non è persistita; l'unico residuo su disco è la distanza percorsa, un totale aggregato in metri, non una posizione. Vale anche quando l'app è connessa ad Android Auto: la stessa posizione già raccolta viene usata al volo anche per lo schermo dell'auto, senza raccolta aggiuntiva, senza permessi nuovi e senza alcuna trasmissione.
- **Il dato viene trasmesso in modo crittografato in transito?** Non applicabile — non lascia mai il dispositivo.
- **Gli utenti possono richiedere la cancellazione dei dati?** Non applicabile — nessun dato viene trasmesso da cancellare; disinstallare l'app rimuove ogni traccia locale (max velocità sessione, distanza percorsa, preferenza schermo, contatore dei rifiuti del permesso lato auto). Il pulsante "Azzera" nell'app riporta a zero sia la velocità massima sia la distanza percorsa in qualsiasi momento.

## Pratiche di sicurezza

- **Dati crittografati in transito:** Non applicabile (nessuna trasmissione di rete — l'app non dichiara il permesso `INTERNET`).
- **Gli utenti possono richiedere la cancellazione dei dati:** Sì, implicitamente (nessun dato lascia il device; la disinstallazione cancella tutto).

## Nota per chi compila il form in Play Console

Alla versione 2.0 l'app persiste localmente **quattro** sole voci, tutte in SharedPreferences app-private:

1. **Velocità massima di sessione** — un numero intero in km/h (`MaxSpeedStore`);
2. **Distanza percorsa dall'ultimo azzeramento** — un singolo numero in metri (`DistanceStore`), un totale aggregato, non un tracciato di posizioni;
3. **Preferenza "schermo sempre acceso"** — un valore vero/falso (`ScreenOnPreferenceStore`);
4. **Contatore dei rifiuti del permesso di localizzazione registrati dallo schermo Android Auto** — un singolo numero intero (`CarPermissionDenialStore`) che conta quante volte l'utente ha rifiutato la richiesta di permesso mostrata sullo schermo auto. Serve solo a decidere quale messaggio mostrare (riprova vs. apri le impostazioni). Non è un identificativo, non è collegabile all'identità dell'utente e non lascia mai il dispositivo.

Sono semplici valori funzionali, non raccolgono identificatori, non lasciano mai il dispositivo e non vengono dichiarate come "raccolta dati" nel form standard di Play Console (che riguarda dati raccolti/trasmessi, non le SharedPreferences locali) — ma è corretto menzionarle qui per completezza e trasparenza. Le prime due sono azzerabili dall'utente in qualsiasi momento con il pulsante "Azzera" nell'app; tutte e quattro spariscono con la disinstallazione.
