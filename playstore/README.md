# Tachimetro — pacchetto pubblicazione Play Store

Tutto il necessario per pubblicare Tachimetro su Google Play, generato il 2026-08-22.
Aggiornato a **versione 1.1 (versionCode 2)** il 2026-08-30.

## Contenuto

| Percorso | Cosa contiene |
|---|---|
| `apk/tachimetro-1.0-unsigned.apk` | **OBSOLETO (build 1.0)** — APK release, **non firmato**; va rigenerato come `tachimetro-1.1-unsigned.apk` |
| `apk/tachimetro-1.0.aab` | **OBSOLETO (build 1.0)** — Android App Bundle release, **formato richiesto da Play Console** per il canale di produzione; va rigenerato come `tachimetro-1.1.aab` |
| `graphics/icon-512.png` | Icona 512×512 (copia di `app/src/main/res/playstore-icon.png`) |
| `graphics/feature-graphic-1024x500.png` | Feature graphic per la scheda dello store |
| `screenshots/` | 5 screenshot reali, catturati su emulatore Pixel_10_Pro con GPS mock (vedi sotto) — **risalgono alla v1.0** |
| `listing/it/`, `listing/en/` | Titolo, descrizione breve, descrizione completa (entro i limiti Play Console) — aggiornati alla v1.1 |
| `privacy_policy.html` | Informativa privacy bilingue IT/EN, pronta per l'hosting |
| `data_safety.md` | Bozza risposte per il form "Sicurezza dei dati" di Play Console |
| `content_rating.md` | Bozza risposte per il questionario di classificazione contenuti (IARC) |
| `release_notes/it.txt`, `release_notes/en.txt` | Note di rilascio per la versione 1.1 |
| `release_notes/release_notes_v1.1.txt` | File unico bilingue con tag `<it-IT>`/`<en-US>`, per il copia-incolla in un'unica azione in Play Console |

## Screenshot — stati catturati

Catturati realmente su emulatore (Pixel_10_Pro, API 36) iniettando fix GPS mock via `adb emu geo fix ... <velocità in nodi>` per simulare movimento continuo (il calcolo velocità di Android deriva dagli spostamenti di posizione, non dal solo campo velocità NMEA — richiede una sequenza di fix a ~1 Hz con posizione che avanza in modo coerente).

1. **01_avvio_0kmh** — avvio con posizione GPS statica, lettura "0 km/h"
2. **02_lettura_82kmh_max** — lettura in movimento (~82 km/h) con area MAX velocità e pulsante "Azzera massimo" visibili
3. **03_lettura_41kmh_sempre_acceso** — lettura in movimento (~41 km/h) con switch "Sempre acceso" attivato
4. **04_pronto** — stato "Pronto" mostrato subito dopo la concessione del permesso, prima del primo fix GPS
5. **05_permission_denied** — stato "Permesso GPS necessario per funzionare" con pulsante "Riprova", dopo aver negato il permesso di localizzazione

Non è stato possibile catturare in modo affidabile lo stato "Ricerca segnale GPS..." (5+ secondi senza fix con GPS di sistema attivo): l'emulatore ripete l'ultimo fix noto a ~1 Hz anche senza nuovi comandi, impedendo lo scadere della soglia di staleness di 5s nel codice (`GpsSpeedProvider.kt`). Non è un problema dell'app — è una particolarità del GPS simulato dell'emulatore.

**Nota v1.1:** questi screenshot risalgono alla v1.0 e non mostrano né l'area distanza (in basso a destra) né l'icona di ricarica introdotte con la milestone v1.1. Vanno rigenerati prima della sottomissione reale. A differenza della cattura v1.0 (interamente automatizzabile con GPS mock), la rigenerazione richiede una cattura manuale su device reale: l'icona di ricarica compare solo con il telefono realmente in carica, e la distanza percorsa richiede movimento GPS reale (non simulabile in modo affidabile con `adb emu geo fix` per questi due stati specifici).

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
5. Sostituisci `apk/tachimetro-1.0-unsigned.apk` e `apk/tachimetro-1.0.aab` con le versioni rigenerate `tachimetro-1.1-unsigned.apk` e `tachimetro-1.1.aab`, firmate. Per il canale di produzione su Play Console carica il file **.aab**, non l'APK.

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

`app/build.gradle.kts` ha ora `versionCode = 2`, `versionName = "1.1"` (aggiornato dalla milestone v1.1: indicatore di ricarica + distanza percorsa). Ricordarsi di incrementare `versionCode` a ogni release successiva.
