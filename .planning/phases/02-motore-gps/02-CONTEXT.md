# Phase 2: Motore GPS - Context

**Gathered:** 2026-07-07
**Status:** Ready for planning

<domain>
## Phase Boundary

Questa fase costruisce il motore che legge la velocità reale dal GPS del dispositivo tramite FusedLocationProviderClient (Google Play Services), la converte in km/h, applica filtri di qualità/rumore, ed espone il valore aggiornato (o lo stato "nessun segnale") al resto dell'app. Non include ancora l'interfaccia finale a schermo intero (Fase 3): per questa fase, il placeholder nero "Pronto" della Fase 1 viene temporaneamente sostituito dal numero letto, solo per rendere il lavoro verificabile su device — la UI reale arriva in Fase 3.

</domain>

<decisions>
## Implementation Decisions

### Soglia "nessun segnale"
- **D-01:** Il messaggio "Ricerca segnale GPS..." appare subito all'avvio finché non arriva il primo fix valido.
- **D-02:** Durante l'uso, se non arriva nessun aggiornamento di posizione per più di 5 secondi, il segnale è considerato perso e il messaggio "Ricerca segnale GPS..." ricompare.
- **D-03:** Da fermo, il GPS può riportare piccole velocità (0.3-1.5 km/h) per rumore. Applicare una soglia minima (es. ~2 km/h) sotto la quale il valore mostrato viene arrotondato a 0.
- **D-04:** Se la location arriva ma `hasSpeed()` è `false` (comune da fermo), trattare il valore come 0 km/h, non come "nessun segnale".
- **D-05:** Scartare le letture di posizione con accuratezza scarsa (raggio di incertezza elevato, indicativamente oltre ~30-50 metri) per evitare picchi di velocità fasulli da GPS impreciso — non aggiornare il valore mostrato con letture sotto la soglia di accuratezza accettabile.

### Architettura del dato velocità
- **D-06:** Il motore GPS espone il valore di velocità tramite Kotlin Flow/StateFlow (richiede la dipendenza `kotlinx-coroutines-play-services` per collegare i callback di FusedLocationProviderClient a un Flow), non tramite callback/listener tradizionali.

### Ciclo di vita aggiornamenti GPS
- **D-07:** Gli aggiornamenti di posizione partono in `onStart()` e si fermano in `onStop()` dell'Activity (non `onResume`/`onPause`) — continuano a funzionare finché l'app è visibile (anche in multitasking/split-screen), si fermano solo quando l'app non è più visibile. Bilancia batteria e continuità di lettura.
- **D-08 [informational, rilevante per Fase 4]:** Se l'app va in background mentre l'utente guida, la velocità massima (Fase 4) continua a essere registrata solo mentre l'app è visibile — nessun tracciamento in background/Foreground Service previsto per v1.

### Verificabilità e formato del placeholder di test
- **D-09:** Per rendere questa fase verificabile prima che la Fase 3 costruisca la UI reale, il placeholder nero "Pronto" della Fase 1 viene temporaneamente sostituito dal valore di velocità numerico intero (es. "42 km/h", nessun decimale) quando disponibile, e torna a mostrare il messaggio di ricerca segnale quando il segnale è perso. Questo comportamento placeholder verrà rimpiazzato dalla vera UI in Fase 3.
- **D-10 [verifica]:** Il checkpoint umano per questa fase va testato usando la funzione "Route playback" nei controlli estesi dell'emulatore Android (Extended Controls → Location → Routes), che simula un percorso GPS con velocità variabile — non solo una verifica statica da fermo.

### Claude's Discretion
- Dettagli implementativi di conversione m/s → km/h, gestione dei permessi già negati (già coperta dalla Fase 1), struttura interna delle classi/file del motore GPS.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requisiti e progetto
- `.planning/PROJECT.md` — Constraints: FusedLocationProviderClient, aggiornamento 1/sec, Kotlin
- `.planning/REQUIREMENTS.md` — GPS-01, GPS-02, GPS-03 sono i requisiti di questa fase

### Fase precedente (Fase 1)
- `.planning/phases/01-fondamenta-permessi-e-avvio/01-CONTEXT.md` — decisioni sul permesso ACCESS_FINE_LOCATION già implementato, placeholder nero "Pronto" da estendere in questa fase
- `.planning/phases/01-fondamenta-permessi-e-avvio/01-02-SUMMARY.md` — MainActivity.kt esistente con gestione permesso (onResume incluso, commit 927e3c0) su cui costruire il motore GPS

### Stato del codice
- `.planning/codebase/STACK.md` — nessuna dipendenza Google Play Services/coroutines presente ancora, va aggiunta
- `.planning/codebase/INTEGRATIONS.md` — nessuna integrazione esterna presente ancora (da aggiornare dopo questa fase)

No external specs beyond the above — requirements fully captured in decisions above.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `app/src/main/java/com/sed/tachimetro/MainActivity.kt` — Activity esistente con gestione completa del permesso ACCESS_FINE_LOCATION (grant/deny/permanent-deny/onResume re-check), placeholder nero "Pronto" da estendere per mostrare il valore di velocità
- `app/src/main/res/layout/activity_main.xml` — layout placeholder con `messageText`/`retryButton` da riutilizzare o estendere per mostrare il numero
- `app/src/main/res/values/strings.xml` — stringhe italiane esistenti (messaggi permesso), da estendere con le nuove stringhe (es. "Ricerca segnale GPS...")

### Established Patterns
- Version-catalog-first: ogni nuova dipendenza (play-services-location, kotlinx-coroutines-play-services) va dichiarata prima in `gradle/libs.versions.toml`, poi referenziata via alias `libs.*` in `app/build.gradle.kts`
- Kotlin abilitato via supporto built-in AGP 9.1.1 — NON aggiungere il plugin Kotlin separato (vedi 01-01-SUMMARY.md)
- Package `com.sed.tachimetro` — nuove classi in sotto-pacchetti coerenti (es. `com.sed.tachimetro.gps` o simile)

### Integration Points
- `MainActivity.kt` — punto di integrazione tra il motore GPS (Flow/StateFlow) e l'aggiornamento del placeholder testuale
- `AndroidManifest.xml` — già dichiara `ACCESS_FINE_LOCATION`, nessuna modifica di permessi necessaria in questa fase

</code_context>

<specifics>
## Specific Ideas

Nessuna idea specifica aggiuntiva oltre alle decisioni sopra — il toggle "schermo sempre acceso" menzionato dall'utente durante la discussione appartiene alla Fase 5 (Gestione Schermo) ed è già pianificato lì, non fa parte dello scope di questa fase.

</specifics>

<deferred>
## Deferred Ideas

- Toggle "Schermo sempre acceso" / "Schermo automatico" — già pianificato in Fase 5 (Gestione Schermo, SCRN-01/02/03), menzionato dall'utente durante la discussione di questa fase ma fuori scope qui.

</deferred>

---

*Phase: 2-Motore GPS*
*Context gathered: 2026-07-07*
