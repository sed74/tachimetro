---
quick_id: 260822-huf
status: complete
---

# Quick Task 260822-huf: Asset e testi per pubblicazione Play Store — Summary

## Cosa e' stato fatto

1. **Signing config infra**: `keystore.properties.example` (template), `keystore.properties` in `.gitignore` (root e `app/.gitignore` per `/release`), `signingConfigs.release` in `app/build.gradle.kts` che legge da `keystore.properties` se presente — release build resta unsigned senza il file, nessun blocco.
2. **Build**: `assembleRelease` + `bundleRelease` completati con successo (unsigned, in attesa che l'utente compili `keystore.properties`). Copiati in `playstore/apk/`.
3. **Screenshot reali**: emulatore Pixel_10_Pro avviato, APK debug installato, permesso concesso via `pm grant`. Iniezione GPS mock via `adb emu geo fix <lon> <lat> <alt> <sat> <velocita_nodi>` con sequenze di fix a 1Hz a posizione crescente (necessario: l'algoritmo Fused Location calcola la velocita' dagli spostamenti di posizione, il campo velocita' NMEA/geo-fix da solo viene ignorato se la posizione resta statica). 5 stati catturati: avvio/0km-h, lettura ~82km/h con area MAX, lettura ~41km/h con switch "Sempre acceso" attivo, stato "Pronto" post-permesso, stato permesso negato.
4. **Icona**: copiata da `app/src/main/res/playstore-icon.png` (gia' 512x512).
5. **Feature graphic**: generata via Python/PIL (non serviva claude-in-chrome — font di sistema Arial Bold), fedele alla palette bianco/nero dell'app reale (colors.xml non ha colori accent).
6. **Testi listing** IT/EN scritti e verificati entro i limiti Play Console (titolo <=30, breve <=80, completa <=4000 char).
7. **Privacy policy** HTML bilingue, enfatizza l'assenza del permesso INTERNET (nessun dato lascia il device).
8. **Data safety / content rating**: bozze markdown basate sul comportamento reale del codice (unico permesso ACCESS_FINE_LOCATION, nessun INTERNET, nessuna pubblicita').
9. **Release notes** IT/EN.
10. **README.md** in `playstore/` con indice e passi manuali rimanenti.

## Deviazioni dal piano

- Non spawnati planner/executor GSD standard: task troppo eterogeneo (build Gradle + automazione emulatore/adb + generazione immagine + copywriting bilingue) per il vincolo "1-3 task atomici" e per un executor senza tool browser. Eseguito direttamente dall'orchestratore, con tracking GSD manuale (questa directory).
- Feature graphic generata con Python/PIL invece di claude-in-chrome (piu' affidabile in questo contesto, nessuna dipendenza da un browser).
- Non e' stato possibile catturare lo stato "Ricerca segnale GPS..." (staleness 5s): l'emulatore ripete l'ultimo fix noto a ~1Hz anche senza nuovi comandi espliciti, impedendo lo scadere della soglia nel codice reale. Documentato in `playstore/README.md`. Sostituito con lo stato "Pronto" (altrettanto reale e riproducibile).

## File creati/modificati

- `app/build.gradle.kts` (signing config)
- `.gitignore`, `app/.gitignore` (keystore.properties, /release)
- `keystore.properties.example` (nuovo)
- `playstore/` (nuovo, ~20 file: apk/, graphics/, screenshots/, listing/it+en/, release_notes/, privacy_policy.html, data_safety.md, content_rating.md, README.md)

## Verifica

- `assembleRelease` e `bundleRelease`: BUILD SUCCESSFUL.
- 5 screenshot reali (non placeholder), stati distinti confermati visivamente.
- Scan secret pattern su `playstore/` e `app/build.gradle.kts`: nessun segreto trovato; `keystore.properties` non tracciato da git.
- Tutti i deliverable richiesti presenti in `playstore/`.

## Prossimi passi (per l'utente, vedi playstore/README.md)

1. Compilare `keystore.properties` con le credenziali reali del keystore esistente.
2. Rilanciare `assembleRelease`/`bundleRelease` per ottenere APK/AAB firmati.
3. Ospitare `privacy_policy.html` su un URL pubblico (dopo aver sostituito il placeholder email).
4. Compilare manualmente i form di Play Console (Data Safety, Content Rating, Store Listing) usando i file come riferimento.
