---
quick_id: 260822-huf
status: complete
---

# Quick Task 260822-huf: Asset e testi per pubblicazione Play Store

**Descrizione:** Generare tutti gli asset e i testi necessari per pubblicare Tachimetro sul Google Play Store, nella cartella `playstore/` alla radice del repo.

**Nota di processo:** eseguito direttamente dall'orchestratore (non tramite planner/executor sub-agent standard) perche' il task combina build Gradle, automazione emulatore/adb per screenshot reali, generazione immagine (richiede claude-in-chrome, non disponibile al gsd-executor), e copywriting IT/EN — uno scope troppo eterogeneo per il vincolo "1-3 task atomici" del planner quick e per un singolo executor senza tool browser.

## Decisioni utente (raccolte via AskUserQuestion)

- **Firma APK:** keystore esistente in `C:\Users\fedes\AndroidStudioProjects\keystore\keystore`. L'utente compila lui le credenziali in `keystore.properties` (gitignored) — io predispongo solo il signing config in `build.gradle.kts` che lo legge, senza mai vedere/scrivere le password.
- **Screenshot:** reali, catturati avviando l'emulatore Pixel_10_Pro e iniettando NMEA mock GPS via adb per mostrare velocita' variabili.
- **Lingua testi listing:** italiano + inglese.

## Task

1. **Signing config infra**: aggiungere `keystore.properties` a `.gitignore`, aggiungere `keystore.properties.example` (template senza segreti), aggiungere `signingConfigs.release` in `app/build.gradle.kts` che legge da `keystore.properties` se presente (release build resta unsigned se il file manca — nessun errore bloccante).
2. **Build**: `assembleRelease` (APK) e `bundleRelease` (AAB) in `playstore/apk/`.
3. **Screenshot**: boot emulatore Pixel_10_Pro, install APK debug, grant permission, iniezione NMEA via `adb emu geo nmea`, cattura stati (ricerca segnale, lettura velocita' a varie km/h, max speed, permesso negato) in `playstore/screenshots/`.
4. **Icona**: copiare `app/src/main/res/playstore-icon.png` in `playstore/graphics/icon-512.png` (verificare dimensioni).
5. **Feature graphic** 1024x500: HTML + screenshot via claude-in-chrome, salvato in `playstore/graphics/`.
6. **Testi listing** IT/EN: titolo, descrizione breve, descrizione completa, in `playstore/listing/{it,en}/`.
7. **Privacy policy**: HTML (l'app non ha permesso INTERNET — nessun dato lascia il device) in `playstore/privacy_policy.html`.
8. **Data safety form**: bozza risposte in `playstore/data_safety.md`.
9. **Content rating questionnaire**: bozza risposte in `playstore/content_rating.md`.
10. **Release notes**: IT/EN in `playstore/release_notes/`.
11. **README.md** in `playstore/`: indice + passi manuali rimanenti (compilare keystore.properties, hosting privacy policy, form Play Console).

## Verifica

- `assembleRelease`/`bundleRelease` completano senza errori.
- Screenshot reali (non placeholder) mostrano stati distinti dell'app.
- Tutti i file elencati sopra esistono in `playstore/`.
- Nessun segreto (password keystore) committato nel repo.
