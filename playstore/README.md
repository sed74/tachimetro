# Tachimetro — pacchetto pubblicazione Play Store

Materiale per pubblicare Tachimetro su Google Play, generato il 2026-08-22.
Testi verificati e riallineati alla **versione 2.0 (`versionCode = 3`)** il 2026-09-02.

**Stato in una riga:** i testi e la grafica sono pronti; i binari e gli screenshot no.

## Questo pacchetto descrive la v2.0 con supporto Android Auto

Tutti i testi in questa cartella descrivono la **versione 2.0 (`versionCode = 3`)**, che
include il supporto Android Auto (milestone v2.0, Fasi 8-10). Il bump di versione è già
committato in `app/build.gradle.kts`.

Conseguenze pratiche per chi costruisce l'artefatto:

- **L'artefatto va costruito da `HEAD`** (o dal tag `2.0` quando esisterà). La vecchia
  istruzione di costruire dal tag `v1.1` è stata rimossa: era corretta finché Android Auto non
  era annunciato, ma ora produrrebbe un binario **privo** delle funzionalità descritte nei
  testi di questa cartella — cioè esattamente la discrepanza fra binario e materiale dichiarato
  che quell'avvertenza voleva evitare, e un problema di compliance su Play Console.
- I testi di questa cartella **menzionano Android Auto**, ed è intenzionale: le funzionalità
  descritte (AA-01..AA-04, CONN-01, CONN-02) sono tutte implementate e verificate.

## Rischio noto accettato per questo rilascio (nota interna)

`TachimetroCarAppService.createHostValidator()` restituisce ancora
`HostValidator.ALLOW_ALL_HOSTS_VALIDATOR`: il car service accetta quindi il binding da
**qualunque** host Android Auto, invece di limitarsi a quelli legittimi. Sostituirlo con un
validator reale è il lavoro della **Fase 11** ("Hardening di Produzione e Verifica su
Dispositivo Reale"), non ancora iniziata.

L'utente è stato informato di questo gap e ha scelto consapevolmente di pubblicare comunque la
2.0.

Questa nota resta **confinata a questo README**, che è un documento di lavoro interno per chi
gestisce il rilascio. Non è una voce di sicurezza dei dati — riguarda quali host Android Auto
possono collegarsi al car service dell'app, non la raccolta o la condivisione di dati — e non
ha alcun significato per l'utente finale né per chi esamina la scheda su Play Console. **Non va
copiata** in `listing/`, `release_notes/`, `data_safety.md`, `content_rating.md` o
`privacy_policy.html`.

## Contenuto

| Percorso | Cosa contiene | Stato |
|---|---|---|
| `apk/tachimetro-1.0-unsigned.apk` | APK release **1.0**, non firmato | **OBSOLETO** — da rigenerare come `tachimetro-2.0-unsigned.apk` (a carico dell'utente, vedi passo 1) |
| `apk/tachimetro-1.0.aab` | Android App Bundle release **1.0** — formato richiesto da Play Console per il canale di produzione | **OBSOLETO** — da rigenerare come `tachimetro-2.0.aab` (a carico dell'utente, vedi passo 1) |
| `graphics/icon-512.png` | Icona 512×512 (copia di `app/src/main/res/playstore-icon.png`) | Pronto |
| `graphics/feature-graphic-1024x500.png` | Feature graphic per la scheda dello store | Pronto |
| `screenshots/` | 5 screenshot reali, catturati su emulatore Pixel_10_Pro con GPS mock (vedi sotto) | **Fermi alla v1.0** — da ricatturare a mano (passo 3) |
| `listing/it/`, `listing/en/` | Titolo, descrizione breve, descrizione completa (entro i limiti Play Console) | Verificati contro il codice v2.0 il 2026-09-02 (con sezione Android Auto) |
| `privacy_policy.html` | Informativa privacy bilingue IT/EN | Testo allineato alla v2.0; **2 placeholder email da sostituire** e URL pubblico da creare (passi 2 e 4) |
| `data_safety.md` | Bozza risposte per il form "Sicurezza dei dati" di Play Console | Verificata contro il codice v2.0 il 2026-09-02 (elenca le quattro voci persistite) |
| `content_rating.md` | Bozza risposte per il questionario di classificazione contenuti (IARC) | Verificata contro il codice v2.0 il 2026-09-02 (risposte invariate) |
| `release_notes/it.txt`, `release_notes/en.txt` | Note di rilascio per la versione **2.0**, da incollare nel campo per-locale di Play Console | Pronte |
| `release_notes/release_notes_v2.0.txt` | File unico bilingue 2.0 con tag `<it-IT>`/`<en-US>`, per il copia-incolla in un'unica azione in Play Console | Pronto |
| `release_notes/release_notes_v1.1.txt` | File unico bilingue della 1.1 | **Archivio storico** — conservato per riferimento, non va caricato |

### Dati persistiti dichiarati nei testi (v2.0)

`data_safety.md` e `privacy_policy.html` dichiarano le **quattro** voci che la v2.0 salva
davvero in SharedPreferences: velocità massima di sessione (`MaxSpeedStore`), distanza percorsa
dall'ultimo azzeramento (`DistanceStore`), preferenza "schermo sempre acceso"
(`ScreenOnPreferenceStore`) e il contatore dei rifiuti del permesso di localizzazione
registrati dallo schermo Android Auto (`CarPermissionDenialStore`, un singolo intero usato solo
per scegliere quale messaggio mostrare). Nessuna coordinata GPS viene mai scritta su disco. Se
una futura versione aggiunge o rimuove un valore persistito, **entrambi** i file vanno
aggiornati.

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

### 1. Rigenerazione APK/AAB alla 2.0 (a carico dell'utente)

I due file in `apk/` sono ancora build **1.0**. La rigenerazione è stata esplicitamente presa
in carico dall'utente e **non** è stata eseguita da alcun task automatico: nessun file sotto
`playstore/apk/` è stato creato, rinominato o rimosso.

1. Costruire da `HEAD` (o dal tag `2.0` quando esisterà), che dichiara `versionCode = 3` /
   `versionName = "2.0"` e contiene il codice Android Auto descritto nei testi:
   ```
   ./gradlew.bat assembleRelease bundleRelease
   ```
2. Output attesi dalla build:
   - APK: `app/build/outputs/apk/release/app-release-unsigned.apk`
   - AAB: `app/build/outputs/bundle/release/app-release.aab`
3. Depositarli qui come `apk/tachimetro-2.0-unsigned.apk` e `apk/tachimetro-2.0.aab`, e
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

### 3. Screenshot alla v2.0

Gli screenshot in `screenshots/` risalgono alla v1.0 e non mostrano né l'area distanza (in
basso a destra) né l'icona di ricarica introdotte con la milestone v1.1. Con la v2.0 manca
inoltre **qualsiasi cattura dello schermo Android Auto**, che è la novità principale annunciata
nei testi. Vanno rigenerati prima della sottomissione reale.

A differenza della cattura v1.0 (interamente automatizzabile con GPS mock), la rigenerazione
richiede una **cattura manuale su device reale**: l'icona di ricarica compare solo con il
telefono realmente in carica, e la distanza percorsa richiede movimento GPS reale (non
simulabile in modo affidabile con `adb emu geo fix` per questi due stati specifici).

La cattura dello schermo Android Auto richiede in più una sessione **DHU** (Desktop Head Unit)
o un **head unit** fisico: non è automatizzabile in questo ambiente.

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

La versione corrente dichiarata in `app/build.gradle.kts` è `versionName = "2.0"` con
`versionCode = 3` (milestone v2.0: supporto Android Auto). È la versione che questo pacchetto
descrive e che va pubblicata.

Il **prossimo** rilascio dovrà incrementare `versionCode` (a 4) e aggiornare `versionName`:
Play Console rifiuta un caricamento con un `versionCode` già usato.
