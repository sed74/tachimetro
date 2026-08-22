# Tachimetro — pacchetto pubblicazione Play Store

Tutto il necessario per pubblicare Tachimetro su Google Play, generato il 2026-08-22.

## Contenuto

| Percorso | Cosa contiene |
|---|---|
| `apk/tachimetro-1.0-unsigned.apk` | APK release, **non firmato** (nessun keystore.properties presente al momento della build) |
| `apk/tachimetro-1.0.aab` | Android App Bundle release — **formato richiesto da Play Console** per il canale di produzione |
| `graphics/icon-512.png` | Icona 512×512 (copia di `app/src/main/res/playstore-icon.png`) |
| `graphics/feature-graphic-1024x500.png` | Feature graphic per la scheda dello store |
| `screenshots/` | 5 screenshot reali, catturati su emulatore Pixel_10_Pro con GPS mock (vedi sotto) |
| `listing/it/`, `listing/en/` | Titolo, descrizione breve, descrizione completa (entro i limiti Play Console) |
| `privacy_policy.html` | Informativa privacy bilingue IT/EN, pronta per l'hosting |
| `data_safety.md` | Bozza risposte per il form "Sicurezza dei dati" di Play Console |
| `content_rating.md` | Bozza risposte per il questionario di classificazione contenuti (IARC) |
| `release_notes/it.txt`, `release_notes/en.txt` | Note di rilascio per la prima versione |

## Screenshot — stati catturati

Catturati realmente su emulatore (Pixel_10_Pro, API 36) iniettando fix GPS mock via `adb emu geo fix ... <velocità in nodi>` per simulare movimento continuo (il calcolo velocità di Android deriva dagli spostamenti di posizione, non dal solo campo velocità NMEA — richiede una sequenza di fix a ~1 Hz con posizione che avanza in modo coerente).

1. **01_avvio_0kmh** — avvio con posizione GPS statica, lettura "0 km/h"
2. **02_lettura_82kmh_max** — lettura in movimento (~82 km/h) con area MAX velocità e pulsante "Azzera massimo" visibili
3. **03_lettura_41kmh_sempre_acceso** — lettura in movimento (~41 km/h) con switch "Sempre acceso" attivato
4. **04_pronto** — stato "Pronto" mostrato subito dopo la concessione del permesso, prima del primo fix GPS
5. **05_permission_denied** — stato "Permesso GPS necessario per funzionare" con pulsante "Riprova", dopo aver negato il permesso di localizzazione

Non è stato possibile catturare in modo affidabile lo stato "Ricerca segnale GPS..." (5+ secondi senza fix con GPS di sistema attivo): l'emulatore ripete l'ultimo fix noto a ~1 Hz anche senza nuovi comandi, impedendo lo scadere della soglia di staleness di 5s nel codice (`GpsSpeedProvider.kt`). Non è un problema dell'app — è una particolarità del GPS simulato dell'emulatore.

## Passi manuali rimanenti prima della pubblicazione

### 1. Firma release (obbligatorio per pubblicare)

Il keystore esiste già in `C:\Users\fedes\AndroidStudioProjects\keystore\keystore`, ma le credenziali non sono nel repo (giustamente — sono un segreto).

1. Copia `keystore.properties.example` (nella radice del repo) in `keystore.properties`
2. Compila `storePassword`, `keyAlias`, `keyPassword` con i valori reali
3. `keystore.properties` è già in `.gitignore` — non verrà mai committato
4. Rilancia la build:
   ```
   ./gradlew.bat assembleRelease bundleRelease
   ```
   Con `keystore.properties` presente, `app/build.gradle.kts` firma automaticamente sia l'APK che l'AAB con il keystore release (vedi `signingConfigs.release` nel file).
5. Sostituisci `apk/tachimetro-1.0-unsigned.apk` e `apk/tachimetro-1.0.aab` con le versioni firmate. Per il canale di produzione su Play Console carica il file **.aab**, non l'APK.

### 2. Hosting privacy policy

Play Console richiede un **URL pubblico**, non un file. Opzioni più semplici:
- GitHub Pages da questo stesso repo (es. abilitare Pages sulla cartella `playstore/` o su un branch dedicato)
- Qualsiasi hosting statico gratuito (Netlify, Vercel, ecc.)

Prima di pubblicare l'URL, sostituisci il placeholder `[inserire indirizzo email di contatto]` / `[insert contact email address]` in `privacy_policy.html` con un indirizzo email di contatto reale.

### 3. Play Console — form da compilare a mano

Questi contenuti sono bozze basate sul comportamento reale del codice, ma vanno inseriti manualmente nei form di Play Console (non sono automatizzabili via file):

- **Scheda Store** → incolla i testi da `listing/it/` e `listing/en/`, carica `graphics/icon-512.png`, `graphics/feature-graphic-1024x500.png` e gli screenshot da `screenshots/`
- **Sicurezza dei dati** → usa `data_safety.md` come riferimento
- **Classificazione dei contenuti** → usa `content_rating.md` come riferimento (categoria suggerita: Auto e veicoli)
- **Privacy policy** → incolla l'URL pubblico del passo 2
- **Contenuti** → dichiara nessuna pubblicità, nessun acquisto in-app, nessun contenuto generato dagli utenti

### 4. Versionamento

`app/build.gradle.kts` ha attualmente `versionCode = 1`, `versionName = "1.0"`. Va bene per la prima pubblicazione; ricordarsi di incrementare `versionCode` a ogni release successiva.
