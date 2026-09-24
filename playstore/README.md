# Tachimetro — pacchetto pubblicazione Play Store

Materiale per pubblicare Tachimetro su Google Play, generato il 2026-08-22.
Testi verificati contro il codice della **versione 2.0** il 2026-09-02; i riferimenti di
versione sono stati riallineati a `versionCode = 4` il 2026-09-22 (il `versionName` resta
invariato a "2.0").
Aggiornato il 2026-09-24 per la build di test chiuso `versionCode = 5` / `versionName = "2.1-beta"`
(vedi la sezione successiva).

**Stato in una riga:** i testi e la grafica sono pronti; i binari e gli screenshot no.

## Build di test chiuso 2.1-beta (Fase 12, flavor spikePoi)

Su richiesta esplicita dell'utente (2026-09-24) la build del flavor **spikePoi** va caricata
sul canale di **test chiuso** di Play Store. È una **deroga a D-05** della Fase 12 (che
prevedeva flavor mai distribuiti) e vale **solo** per spikePoi con card `message`: il flavor
**spikeNav** (categoria NAVIGATION) resta un APK debug locale per il DHU e non viene mai
distribuito.

- **Flavor:** `spikePoi`, card `message` — `MapWithContentTemplate` con `MessageTemplate`
  "Tachimetro", nessuna action strip; la velocità GPS reale è disegnata sulla Surface.
- **Categoria:** POI (`androidx.car.app.category.POI`), `minCarApiLevel` 7.
- **Versione:** `versionCode = 5`, `versionName = "2.1-beta"`.
- **Canale:** solo test chiuso.
- **Comando:**
  ```
  ./gradlew.bat :app:bundleSpikePoiRelease -PpoiCard=message
  ```
- **Output:** `app/build/outputs/bundle/spikePoiRelease/app-spikePoi-release.aab`
- **Firma:** automatica se `keystore.properties` è presente nella radice del repo (vedi passo
  2); altrimenti l'AAB esce non firmato e Play Console lo rifiuta.
- **Aspetto in release:** solo cifre bianche su nero. I contorni verde/magenta e le righe di
  debug (`api=...`, variante, risoluzione) compaiono solo nelle build debug usate sul DHU.
- **Note di rilascio:** `release_notes/release_notes_v2.1-beta.txt` (file unico bilingue).
- **Dati persistiti:** invariati nella 2.1-beta — la Surface usa lo stesso `GpsSpeedProvider` e
  non scrive nulla su disco, quindi `data_safety.md` e `privacy_policy.html` restano validi.

La **Fase 12 resta aperta**: le misure DHU del piano 12-04 e il gate di decisione 12-05 sono
ancora da completare. La build e il caricamento su Play Console sono **a carico dell'utente**;
nessun task automatico ha caricato nulla.

## Questo pacchetto descrive la v2.0 con supporto Android Auto

Tutti i testi in questa cartella descrivono la **versione 2.0 (`versionCode = 4`)**, che
include il supporto Android Auto (milestone v2.0, Fasi 8-10). Il bump di versione è già
committato in `app/build.gradle.kts`: il `versionName` resta **"2.0"**, mentre il
`versionCode` è stato portato a 4 (commit `b62b879`) per poter caricare il pacchetto su Play
Console.

Conseguenze pratiche per chi costruisce l'artefatto:

- **L'artefatto va costruito da `HEAD`** (o dal tag `2.0` quando esisterà). La vecchia
  istruzione di costruire dal tag `v1.1` è stata rimossa: era corretta finché Android Auto non
  era annunciato, ma ora produrrebbe un binario **privo** delle funzionalità descritte nei
  testi di questa cartella — cioè esattamente la discrepanza fra binario e materiale dichiarato
  che quell'avvertenza voleva evitare, e un problema di compliance su Play Console.
- I testi di questa cartella **menzionano Android Auto**, ed è intenzionale: le funzionalità
  descritte (AA-01..AA-04, CONN-01, CONN-02) sono tutte implementate e verificate.
- **Il build 2.0 / `versionCode` 4 è destinato al canale di test aperto di Play Store.**
  L'utente genera il build di release **firmato** e lo pubblica lì (decisione registrata in
  `.planning/HANDOFF.json` e nel `.continue-here.md` della Fase 11), invece di installarlo
  localmente: lo stesso binario serve anche come artefatto per i checkpoint SC1/SC3 della
  Fase 11 e resta un binario release (`DEBUGGABLE=false`) anche se ri-firmato da Play App
  Signing. La pubblicazione sul canale di **produzione** è un passo successivo, non ancora
  eseguito.

## Rischio host validation: chiuso in Fase 11 (nota interna)

Fino alla Fase 10 `TachimetroCarAppService.createHostValidator()` restituiva
`HostValidator.ALLOW_ALL_HOSTS_VALIDATOR`, accettando il binding da **qualunque** host Android
Auto. Dalla Fase 11 (piano 11-01) nei build di release restituisce invece un validator con
allow-list reale (`app/src/main/java/com/sed/tachimetro/car/CarHostValidation.kt`), basata
sull'allow-list ufficiale `androidx.car.app` e limitata a `com.google.android.projection.gearhead`
(Android Auto) e `com.google.android.apps.automotive.templates.host` (Automotive OS Templates).
I build di DEBUG restano permissivi di proposito (D-01, per il Desktop Head Unit) e non vengono
distribuiti.

La chiusura è stata verificata con un host Android Auto reale il 2026-09-23 sul build release
versionCode 4 installato dal Play Store (PASS visivo, senza cattura del log `CarApp.Val`) —
dettagli in `docs/android-auto-hardening-verification.md`, tabella "Esiti registrati".

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
| `apk/tachimetro-1.0.aab` | Android App Bundle release **1.0** — formato richiesto da Play Console sia per il canale di **test aperto** sia per la produzione | **OBSOLETO** — da rigenerare come `tachimetro-2.0.aab` (a carico dell'utente, vedi passo 1) |
| `graphics/icon-512.png` | Icona 512×512 (copia di `app/src/main/res/playstore-icon.png`) | Pronto |
| `graphics/feature-graphic-1024x500.png` | Feature graphic per la scheda dello store | Pronto |
| `screenshots/` | 5 screenshot reali, catturati su emulatore Pixel_10_Pro con GPS mock (vedi sotto) | **Fermi alla v1.0** — da ricatturare a mano (passo 3) |
| `listing/it/`, `listing/en/` | Titolo, descrizione breve, descrizione completa (entro i limiti Play Console) | Verificati contro il codice v2.0 il 2026-09-02 (con sezione Android Auto) |
| `privacy_policy.html` | Informativa privacy bilingue IT/EN | Testo allineato alla v2.0; **2 placeholder email da sostituire** e URL pubblico da creare (passi 2 e 4) |
| `data_safety.md` | Bozza risposte per il form "Sicurezza dei dati" di Play Console | Verificata contro il codice v2.0 il 2026-09-02 (elenca le quattro voci persistite) |
| `content_rating.md` | Bozza risposte per il questionario di classificazione contenuti (IARC) | Verificata contro il codice v2.0 il 2026-09-02 (risposte invariate) |
| `release_notes/release_notes_v2.1-beta.txt` | File unico bilingue **2.1-beta** con tag `<it-IT>`/`<en-US>`, per la build di test chiuso spikePoi | Pronto (`versionCode` 5) |
| `release_notes/it.txt`, `release_notes/en.txt` | Note di rilascio per la versione **2.0**, da incollare nel campo per-locale di Play Console | Riferite alla 2.0 (`versionCode` 4) |
| `release_notes/release_notes_v2.0.txt` | File unico bilingue 2.0 con tag `<it-IT>`/`<en-US>`, per il copia-incolla in un'unica azione in Play Console | Riferito alla 2.0 (`versionCode` 4) |
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

1. Costruire da `HEAD` (o dal tag `2.0` quando esisterà), che dichiara `versionCode = 4` /
   `versionName = "2.0"` e contiene il codice Android Auto descritto nei testi:
   ```
   ./gradlew.bat assembleRelease bundleRelease
   ```
2. Output attesi dalla build:
   - APK: `app/build/outputs/apk/release/app-release-unsigned.apk`
   - AAB: `app/build/outputs/bundle/release/app-release.aab`
3. Depositarli qui come `apk/tachimetro-2.0-unsigned.apk` e `apk/tachimetro-2.0.aab`, e
   rimuovere i due file 1.0 obsoleti (sono tracciati da git: usare `git rm`, non `rm`).

Su Play Console si carica il file **.aab**, non l'APK — sia per il canale di **test aperto**
sia per quello di produzione.

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

La versione corrente dichiarata in `app/build.gradle.kts` è `versionName = "2.1-beta"` con
`versionCode = 5` (2026-09-24): build di test chiuso del flavor spikePoi, vedi la sezione
"Build di test chiuso 2.1-beta" in alto.

Storico: la 2.0 (supporto Android Auto) è stata pubblicata con `versionCode = 4`, ottenuto
incrementando da 3 a 4 **senza** cambiare il `versionName` (commit `b62b879`), perché Play
Console rifiuta il caricamento di un `versionCode` già usato.

Il **prossimo** caricamento su Play Console dovrà usare `versionCode = 6`, e aggiornare anche
il `versionName` se il contenuto funzionale cambia.
