# Tachimetro — pacchetto pubblicazione Play Store

Materiale per pubblicare Tachimetro su Google Play, generato il 2026-08-22.
Testi verificati e riallineati alla **versione 1.1 (`versionCode = 2`)** il 2026-09-02.

**Stato in una riga:** i testi e la grafica sono pronti; i binari e gli screenshot no.

## Attenzione — questo pacchetto riguarda la sola v1.1

Il tag git `v1.1` è la versione descritta da tutti i testi in questa cartella. Da allora il
ramo principale ha aggiunto **lavoro Android Auto non ancora rilasciato** (milestone v2.0,
Fasi 8-10: `TachimetroCarAppService`, `SpeedScreen`, `TachimetroApplication`, un service
`exported="true"` nel manifest) **senza incrementare `versionCode`/`versionName`**: `HEAD`
dichiara ancora 1.1/2.

Conseguenze pratiche per chi costruisce l'artefatto:

- **Costruire dal tag `v1.1`** (o comunque da una sorgente priva del codice Android Auto).
  Una build da `HEAD` produrrebbe un binario etichettato "1.1" contenente funzionalità **non
  annunciate** nei testi di questa cartella — una discrepanza fra binario e materiale
  dichiarato è un problema di compliance su Play Console.
- Il codice Android Auto **non è ancora indurito**: `TachimetroCarAppService` usa tuttora
  `HostValidator.ALLOW_ALL_HOSTS_VALIDATOR`. Sostituirlo con un validator reale è il lavoro
  della Fase 11, non ancora iniziata. Non va spedito così.
- Nessun testo di questa cartella menziona Android Auto, ed è intenzionale: quella milestone
  non è rilasciata. Non aggiungerne finché non lo è.

## Contenuto

| Percorso | Cosa contiene | Stato |
|---|---|---|
| `apk/tachimetro-1.0-unsigned.apk` | APK release **1.0**, non firmato | **OBSOLETO** — da rigenerare come `tachimetro-1.1-unsigned.apk` (a carico dell'utente, vedi passo 1) |
| `apk/tachimetro-1.0.aab` | Android App Bundle release **1.0** — formato richiesto da Play Console per il canale di produzione | **OBSOLETO** — da rigenerare come `tachimetro-1.1.aab` (a carico dell'utente, vedi passo 1) |
| `graphics/icon-512.png` | Icona 512×512 (copia di `app/src/main/res/playstore-icon.png`) | Pronto |
| `graphics/feature-graphic-1024x500.png` | Feature graphic per la scheda dello store | Pronto |
| `screenshots/` | 5 screenshot reali, catturati su emulatore Pixel_10_Pro con GPS mock (vedi sotto) | **Fermi alla v1.0** — da ricatturare a mano (passo 3) |
| `listing/it/`, `listing/en/` | Titolo, descrizione breve, descrizione completa (entro i limiti Play Console) | Verificati contro il codice v1.1 il 2026-09-02 |
| `privacy_policy.html` | Informativa privacy bilingue IT/EN | Testo pronto; **2 placeholder email da sostituire** e URL pubblico da creare (passi 2 e 4) |
| `data_safety.md` | Bozza risposte per il form "Sicurezza dei dati" di Play Console | Verificata contro il codice v1.1 il 2026-09-02 (elenca le tre voci persistite) |
| `content_rating.md` | Bozza risposte per il questionario di classificazione contenuti (IARC) | Verificata contro il codice v1.1 il 2026-09-02 |
| `release_notes/it.txt`, `release_notes/en.txt` | Note di rilascio per la versione 1.1 | Pronte |
| `release_notes/release_notes_v1.1.txt` | File unico bilingue con tag `<it-IT>`/`<en-US>`, per il copia-incolla in un'unica azione in Play Console | Pronto |

### Dati persistiti dichiarati nei testi (v1.1)

`data_safety.md` e `privacy_policy.html` dichiarano le **tre** voci che la v1.1 salva davvero
in SharedPreferences: velocità massima di sessione (`MaxSpeedStore`), distanza percorsa
dall'ultimo azzeramento (`DistanceStore`), preferenza "schermo sempre acceso"
(`ScreenOnPreferenceStore`). Nessuna coordinata GPS viene mai scritta su disco. Se una futura
versione aggiunge o rimuove un valore persistito, **entrambi** i file vanno aggiornati.

## Screenshot — stati catturati

Catturati realmente su emulatore (Pixel_10_Pro, API 36) iniettando fix GPS mock via
`adb emu geo fix ... <velocità in nodi>` per simulare movimento continuo (il calcolo velocità
di Android deriva dagli spostamenti di posizione, non dal solo campo velocità NMEA — richiede
una sequenza di fix a ~1 Hz con posizione che avanza in modo coerente).

1. **01_avvio_0kmh** — avvio con posizione GPS statica, lettura "0 km/h"
2. **02_lettura_82kmh_max** — lettura in movimento (~82 km/h) con area MAX velocità e pulsante "Azzera massimo" visibili
3. **03_lettura_41kmh_sempre_acceso** — lettura in movimento (~41 km/h) con switch "Sempre acceso" attivato
4. **04_pronto** — stato "Pronto" mostrato subito dopo la concessione del permesso, prima del primo fix GPS
5. **05_permission_denied** — stato "Permesso GPS necessario per funzionare" con pulsante "Riprova", dopo aver negato il permesso di localizzazione

Non è stato possibile catturare in modo affidabile lo stato "Ricerca segnale GPS..." (5+ secondi
senza fix con GPS di sistema attivo): l'emulatore ripete l'ultimo fix noto a ~1 Hz anche senza
nuovi comandi, impedendo lo scadere della soglia di staleness di 5s nel codice
(`GpsSpeedProvider.kt`). Non è un problema dell'app — è una particolarità del GPS simulato
dell'emulatore.

## Passi manuali rimanenti prima della pubblicazione

Nessuno dei passi seguenti è stato eseguito: sono tutti **aperti**.

### 1. Rigenerazione APK/AAB alla 1.1 (a carico dell'utente)

I due file in `apk/` sono ancora build **1.0**. La rigenerazione è stata esplicitamente presa
in carico dall'utente e **non** è stata eseguita da alcun task automatico: nessun file sotto
`playstore/apk/` è stato creato, rinominato o rimosso.

1. Costruire dal tag `v1.1` (vedi l'avvertenza in cima a questo file):
   ```
   ./gradlew.bat assembleRelease bundleRelease
   ```
2. Output attesi dalla build:
   - APK: `app/build/outputs/apk/release/app-release-unsigned.apk`
   - AAB: `app/build/outputs/bundle/release/app-release.aab`
3. Depositarli qui come `apk/tachimetro-1.1-unsigned.apk` e `apk/tachimetro-1.1.aab`, e
   rimuovere i due file 1.0 obsoleti (sono tracciati da git: usare `git rm`, non `rm`).

Per il canale di produzione su Play Console si carica il file **.aab**, non l'APK.

### 2. Firma release (obbligatorio per pubblicare)

Senza `keystore.properties` nella radice del repo, `app/build.gradle.kts` produce una release
**non firmata** — senza però fallire la build. Il keystore esiste già in
`C:\Users\fedes\AndroidStudioProjects\keystore\keystore`, ma le credenziali non sono nel repo
(giustamente — sono un segreto).

1. Copiare `keystore.properties.example` (nella radice del repo) in `keystore.properties`
2. Compilare `storePassword`, `keyAlias`, `keyPassword` con i valori reali
3. `keystore.properties` è già in `.gitignore` — non verrà mai committato
4. Rilanciare la build del passo 1: con `keystore.properties` presente, `signingConfigs.release`
   firma automaticamente sia l'APK che l'AAB

### 3. Screenshot alla v1.1

Gli screenshot in `screenshots/` risalgono alla v1.0 e non mostrano né l'area distanza (in
basso a destra) né l'icona di ricarica introdotte con la milestone v1.1. Vanno rigenerati prima
della sottomissione reale.

A differenza della cattura v1.0 (interamente automatizzabile con GPS mock), la rigenerazione
richiede una **cattura manuale su device reale**: l'icona di ricarica compare solo con il
telefono realmente in carica, e la distanza percorsa richiede movimento GPS reale (non
simulabile in modo affidabile con `adb emu geo fix` per questi due stati specifici).

### 4. Hosting privacy policy + email di contatto

Play Console richiede un **URL pubblico**, non un file. Opzioni più semplici:

- GitHub Pages da questo stesso repo (es. abilitare Pages sulla cartella `playstore/` o su un
  branch dedicato)
- Qualsiasi hosting statico gratuito (Netlify, Vercel, ecc.)

Prima di pubblicare l'URL, sostituire i due placeholder in `privacy_policy.html` con un
indirizzo email di contatto reale: `[inserire indirizzo email di contatto]` nella sezione
italiana e `[insert contact email address]` in quella inglese. Sono ancora entrambi presenti.

### 5. Play Console — form da compilare a mano

Questi contenuti sono bozze basate sul comportamento reale del codice, ma vanno inseriti
manualmente nei form di Play Console (non sono automatizzabili via file):

- **Scheda Store** → incollare i testi da `listing/it/` e `listing/en/`, caricare
  `graphics/icon-512.png`, `graphics/feature-graphic-1024x500.png` e gli screenshot da
  `screenshots/` (rigenerati al passo 3)
- **Sicurezza dei dati** → usare `data_safety.md` come riferimento
- **Classificazione dei contenuti** → usare `content_rating.md` come riferimento (categoria
  suggerita: Auto e veicoli)
- **Privacy policy** → incollare l'URL pubblico del passo 4
- **Contenuti** → dichiarare nessuna pubblicità, nessun acquisto in-app, nessun contenuto
  generato dagli utenti

### 6. Versionamento

La versione corrente dichiarata in `app/build.gradle.kts` è `versionName = "1.1"` con
`versionCode = 2` (milestone v1.1: indicatore di ricarica + distanza percorsa con reset
unificato). È la versione che questo pacchetto descrive e che va pubblicata.

Il **prossimo** rilascio dovrà incrementare `versionCode` (a 3) e aggiornare `versionName`:
Play Console rifiuta un caricamento con un `versionCode` già usato. In particolare, quando la
milestone v2.0 (Android Auto) sarà completa e indurita, servirà un bump di versione **e** un
aggiornamento di tutti i testi di questa cartella per annunciare il supporto Android Auto.
