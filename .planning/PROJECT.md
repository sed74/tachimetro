# Tachimetro

## Status

**v1.1 Ricarica e distanza — SHIPPED (2026-08-30).** Nessuna milestone attiva. Prossima da definire via `/gsd-new-milestone`.

## What This Is

App Android nativa che mostra la velocità GPS in tempo reale a schermo intero, con un'interfaccia minimale ad altissimo contrasto pensata per essere letta a colpo d'occhio mentre l'app è montata su un supporto in auto o in moto. Nessun menu, nessun grafico: solo il numero della velocità, con due indicatori secondari opzionali (stato di ricarica, distanza percorsa) che compaiono ai margini dello schermo senza mai competere con il numero principale.

## Core Value

La velocità attuale deve essere sempre visibile, corretta e leggibile istantaneamente in ogni condizione di luce — se questo non funziona alla perfezione, il resto non conta.

## Requirements

### Validated

- ✓ App che apre direttamente la schermata principale, nessuna schermata iniziale/menu — Fase 1
- ✓ Richiesta del permesso ACCESS_FINE_LOCATION con gestione corretta di concessione/rifiuto/rifiuto permanente — Fase 1
- ✓ Lettura della velocità via GPS usando FusedLocationProviderClient (Google Play Services) — Fase 2
- ✓ Aggiornamento della velocità a schermo 1 volta al secondo — Fase 2
- ✓ Messaggio semplice quando manca il segnale GPS (es. "Ricerca segnale GPS...") — Fase 2
- ✓ Velocità attuale come numero enorme, sempre centrato, elemento dominante dello schermo (auto-size uniform) — Fase 3
- ✓ Sfondo nero, testo ad alto contrasto, font di sistema in versione Bold/Black — Fase 3
- ✓ Supporto completo portrait e landscape, con il numero che si adatta e resta il più grande possibile (layout unico adattivo) — Fase 3
- ✓ Nessun menu, animazione o elemento grafico non necessario — Fase 3
- ✓ Testi e messaggi dell'interfaccia interamente in italiano — Fase 3 (già rispettato dalle Fasi 1-2, validato formalmente in Fase 3)
- ✓ Area secondaria con la velocità massima raggiunta dall'ultimo azzeramento — Fase 4
- ✓ Pulsante "Azzera massimo" che azzera il valore e avvia una nuova misurazione — Fase 4
- ✓ Velocità massima persistente su disco (sopravvive a chiusura app e riavvio del telefono, verificato con `adb reboot`) — Fase 4
- ✓ Toggle "Schermo sempre acceso" / "Schermo automatico", con preferenza salvata tra sessioni — Fase 5
- ✓ Quando "sempre acceso" è attivo, impedito lo spegnimento schermo durante l'uso (FLAG_KEEP_SCREEN_ON) — Fase 5
- ✓ Icona a fulmine animata (riempimento bianco → lime → bianco) accanto al toggle "sempre acceso", visibile solo quando il telefono è in carica — Fase 6 (verificato su dispositivo reale)
- ✓ Campo distanza percorsa in basso a destra, calcolata solo mentre l'app è attiva, persistente su disco — Fase 7 (verificato su strada, dispositivo reale)
- ✓ Il pulsante "Azzera massimo" (ora "Azzera") azzera sia velocità massima sia distanza insieme — Fase 7 (verificato su strada, dispositivo reale)

### Active

_Nessun requisito attivo — milestone v1.1 completa (Fasi 6-7)._

### Out of Scope

- Cambio unità km/h ↔ mph — non richiesto per v1, l'app mostra solo km/h
- Tracciamento percorso, mappa o cronologia velocità — l'app è un tachimetro istantaneo, non un GPS tracker
- Menu, impostazioni avanzate, grafici o animazioni ulteriori — l'interfaccia deve restare estremamente semplice per definizione di prodotto (vedi Constraints per l'unica eccezione ammessa, l'icona di ricarica)
- Supporto Android precedente alla versione 11 (minSdk < 30) — il progetto fissa già minSdk 30, nessun device più vecchio da supportare

## Context

- **v1.1 Ricarica e distanza SHIPPED (2026-08-30)** — 2 fasi, 8 piani, 6/6 requisiti validati con checkpoint umani su dispositivo reale. 1.631 LOC Kotlin (era ~695 a fine v1.0). Timeline 2026-08-29 → 2026-08-30 (2 giorni). Fase 7 con audit di sicurezza retroattivo completo (22/22 minacce chiuse). Archivio: `.planning/milestones/v1.1-*`. Prossima milestone da definire via `/gsd-new-milestone`.
- Fase 7 completa (2026-08-30) — **v1.1 completa**: distanza percorsa implementata — `DistanceReducer.kt`/`DistanceFormat.kt` (funzioni pure `reduceDistance`/`sanitizePersistedDistance`/`formatDistanceDisplay`, TDD, gate soglia rumore condiviso con `mapSpeedToKmh`), `DistanceStore.kt` (mirror di `MaxSpeedStore`), `GpsSpeedProvider` esteso con `deltaMeters` per fix accettato via `Location.distanceTo()`. Area distanza in basso a destra (`distanceText` 32sp + `distanceUnitText` 16sp), formato adattivo metri/km con virgola decimale italiana sopra 1 km. Pulsante "Azzera massimo" rinominato "Azzera" e ora azzera massimo e distanza nella stessa azione. Checkpoint umano su strada superato (11/11, movimento reale, GPS reale). Review non bloccante: `GpsSpeedProvider.lastAcceptedLocation` non viene resettato quando la pipeline di collection riparte (background/foreground) — rischio latente di un salto di distanza spurio al primo fix dopo la ripresa, non riprodotto nel test su strada ma non strutturalmente garantito; candidato per un fix futuro.
- Fase 6 completa (2026-08-29): indicatore di ricarica implementato — `ChargingStateProvider.kt` (rilevamento continuo via `BroadcastReceiver` su `ACTION_BATTERY_CHANGED`, sostituisce il precedente controllo one-shot `isDeviceCharging()` per questo scopo), `ChargingState.kt` (sealed Hidden/Pulsing/Full), icona `chargingIcon` (fulmine Material 24dp) a sinistra del toggle "sempre acceso", riempimento lime animato via `ValueAnimator`+`ClipDrawable` (loop 2500ms), stato "piena" congelato lime solido. Prima animazione e primo colore accento del progetto (deroga esplicita). Checkpoint umano su dispositivo reale superato (8/8). Review non bloccante: `isDeviceCharging()` in `MainActivity` andrebbe consolidato con `deriveChargingState()` (duplicazione minore, non blocca). L'utente ha richiesto 2 raffinamenti post-verifica (icona più grande, animazione con svuotamento istantaneo invece di simmetrico) — gestiti come quick task separato dopo la chiusura della fase.
- **v1.0 MVP SHIPPED (2026-07-10)** — 5 fasi, 10 piani, 17/17 requisiti validati con checkpoint umani su device. ~695 LOC Kotlin (8 file `.kt` in `main`: `MainActivity`, package `gps/`, `maxspeed/`, `screen/`), 3 suite di test JVM (SpeedMapping, GpsProviderState, MaxSpeedReducer). Timeline 2026-07-07 → 2026-07-10 (4 giorni). Archivio: `.planning/milestones/v1.0-*`. Nessun feedback utente reale ancora raccolto (app non ancora usata su strada). Prossima milestone da definire via `/gsd-new-milestone`.
- Fase 5 completa (2026-07-10) — **v1.0 completa**: toggle "Sempre acceso" (`keepScreenOnSwitch`, SwitchCompat monocromatico) in basso a sinistra, speculare all'area MAX, sempre visibile. `ScreenOnPreferenceStore.kt` persiste una preferenza booleana nullable (null = mai impostata); al primo avvio il default è derivato dallo stato di ricarica del telefono (ON se in carica) e scritto una sola volta. Cambio di stato applica/rimuove `FLAG_KEEP_SCREEN_ON` immediatamente. Checkpoint umano superato su emulatore (8/8 casi, incluso riavvio e stato di ricarica).
- Fase 4 completa (2026-07-10): monitoraggio velocità massima implementato — `maxSpeedText`/`resetMaxButton` in alto a sinistra (speculare a `unitText`), `MaxSpeedReducer.kt` (funzioni pure `reduceMax`/`sanitizePersistedMax`, TDD con 8 test JVM) e `MaxSpeedStore.kt` (wrapper SharedPreferences). Il massimo si aggiorna e si salva su disco immediatamente ad ogni nuovo record e ad ogni azzeramento; l'area resta nascosta finché il massimo è 0. Applica lo stesso pattern di window insets di `unitText` per restare libera da status bar/cutout. Checkpoint umano superato su emulatore, incluso il test critico di persistenza tramite `adb reboot`.
- Fase 3 completa (2026-07-10): interfaccia tachimetro implementata — `messageText` con `autoSizeTextType="uniform"` (12-300sp per il numero, 12-56sp per i messaggi di stato, cap distinti applicati a runtime), layout unico adattivo (nessun `res/layout-land`), unità "km/h" spostata in un `TextView` separato (`unitText`) piccolo e ancorato in alto a destra. Quattro round di fix da checkpoint umano, tutti approvati su emulatore: (1) cap autosize messaggi, (2) unitText separata, (3) fix window insets per targetSdk 36 edge-to-edge (la status bar copriva unitText), (4) modalità fullscreen immersiva su richiesta utente (tema NoActionBar + `WindowInsetsControllerCompat` per nascondere status/nav bar con swipe-to-reveal). Nota per fasi future: l'app ora gestisce attivamente i window insets — qualunque nuovo elemento posizionato ai bordi schermo (es. area velocità massima in Fase 4) deve tenerne conto.
- Fase 2 completa (2026-07-07): motore GPS implementato (SpeedState, mapSpeedToKmh con 7 unit test, GpsSpeedProvider via callbackFlow/StateFlow), collegato a MainActivity, verificato su emulatore con Route Playback. Filtro accuratezza (~50m), soglia rumore (~2 km/h), timeout "nessun segnale" 5s
- Fase 1 completa (2026-07-07): scaffold portato sotto controllo versione, Kotlin abilitato via supporto built-in AGP 9.1.1 (non il plugin classico, incompatibile con questa versione AGP), MainActivity LAUNCHER con flusso permesso GPS completo, verificato su emulatore reale
- Progetto Android Studio inizializzato (`com.sed.tachimetro`); vedi `.planning/codebase/` per la mappatura pre-esistente
- minSdk 30, targetSdk 36, AGP 9.3.2, Gradle 9.3.1, Kotlin DSL per i build script
- Uso previsto: app montata su supporto auto/moto, quindi priorità assoluta a leggibilità a distanza/in movimento e a basso consumo batteria durante sessioni prolungate

## Constraints

- **Tech stack**: Kotlin per il codice applicativo, layout XML tradizionali (no Jetpack Compose) — coerente con AppCompat già presente e adeguato per una singola schermata statica
- **GPS**: FusedLocationProviderClient (Google Play Services) — richiede un device con Google Play Services installato
- **Compatibility**: minSdk 30 (Android 11+), targetSdk 36
- **Performance**: aggiornamento velocità 1 volta/sec — bilanciamento scelto tra fluidità percepita e consumo batteria
- **UX**: nessun elemento grafico non necessario, nessun menu — massima leggibilità in ogni condizione di luce
- **UX (eccezione v1.1)**: unica animazione ammessa è il riempimento dell'icona di ricarica (bianco → lime → bianco); unico colore accento ammesso è il lime, riservato a quell'icona — non introdurre altre animazioni o colori altrove nell'interfaccia

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Kotlin invece di Java | Standard moderno per Android, build script già in Kotlin DSL | ✓ Good |
| Kotlin abilitato via supporto built-in AGP 9.1.1 (non plugin separato) | Il plugin `org.jetbrains.kotlin.android` classico è incompatibile con AGP 9.1.1 (conflitto verificato) | ✓ Good |
| Layout XML invece di Jetpack Compose | Schermata singola e statica, meno overhead di setup, coerente con AppCompat esistente | ✓ Good |
| FusedLocationProviderClient invece di LocationManager nativo | Più efficiente e preciso, accettato il vincolo di richiedere Google Play Services | ✓ Good |
| Velocità esposta via Kotlin Flow/StateFlow (callbackFlow manuale, non kotlinx-coroutines-play-services) | kotlinx-coroutines-play-services non offre un adapter Flow per aggiornamenti continui (verificato in ricerca Fase 2) | ✓ Good |
| Velocità massima persistente (SharedPreferences) | L'utente vuole confrontare sessioni di guida diverse senza perdere il record | ✓ Good |
| Aggiornamento GPS 1/sec | Bilancia fluidità e battery drain per uso prolungato in auto/moto | ✓ Good |
| Solo km/h, nessun toggle unità | Riduce complessità UI, non richiesto per v1 | ✓ Good (shippato in v1.0 senza attriti) |
| Deroga mirata a "nessuna animazione/nessun colore" per l'icona di ricarica (v1.1) | L'utente vuole un segnale di ricarica evidente e riconoscibile a colpo d'occhio; l'animazione di riempimento comunica lo stato meglio di un'icona statica | ✓ Good (verificato su dispositivo reale, Fase 6) |
| Reset unico per massimo e distanza (v1.1) | La distanza è definita come "percorsa dall'ultimo reset del massimo", quindi i due valori condividono lo stesso ciclo di vita — evita un secondo pulsante su uno schermo minimale | ✓ Good (verificato su strada, Fase 7) |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd:complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-08-30 after v1.1 milestone completion*
