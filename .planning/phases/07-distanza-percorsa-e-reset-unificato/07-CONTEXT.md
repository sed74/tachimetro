# Phase 7: Distanza Percorsa e Reset Unificato - Context

**Gathered:** 2026-08-29
**Status:** Ready for planning

<domain>
## Phase Boundary

Questa fase aggiunge un contatore di distanza percorsa (area in basso a destra, persistente su disco, calcolato solo mentre l'app è in foreground con GPS valido) e unifica il reset: il pulsante esistente che azzera la velocità massima azzera anche la distanza nella stessa azione. Nessuna nuova capability oltre DIST-01/02/03 e MAX-04 (già bloccati da REQUIREMENTS.md).

</domain>

<decisions>
## Implementation Decisions

### Formato e stile della distanza
- **D-01:** Formato adattivo — metri interi sotto 1 km (es. "850 m"), poi km con una decimale sopra 1 km (es. "1,2 km"). Il formato cambia a runtime superata la soglia di 1 km.
- **D-02:** L'unità di misura va in una view separata, piccola, accanto al numero — stesso pattern già usato per `unitText` (non una stringa combinata come `maxSpeedText`/"MAX %d").
- **D-03:** Testo della distanza a **32sp** (vs 22sp dell'area MAX — deve essere visibilmente più grande, SC1).

### Comportamento a fermo / GPS instabile
- **D-04:** Nessun accumulo di distanza sotto la soglia di rumore già usata per la velocità (`noiseFloorKmh = 2.0` in `GpsSpeedProvider.kt`) — evita deriva quando il veicolo è fermo (es. semaforo).
- **D-05:** Stesso filtro di accuratezza della velocità (`accuracyThresholdMeters = 50f`) applicato anche alle letture usate per il calcolo della distanza — nessuna soglia separata.

### Metodo di calcolo della distanza
- **D-06:** La distanza è la somma di `Location.distanceTo()` tra fix GPS consecutivi accettati — **non** un'integrazione della velocità istantanea (kmh × tempo). Scelta esplicita dell'utente per maggiore fedeltà al percorso reale (curve, traiettoria), a fronte di più complessità implementativa.
- **D-07 (nota architetturale per il planner):** D-06 combinato con D-04/D-05 significa che i filtri di rumore/accuratezza — oggi dentro `mapSpeedToKmh()` (`SpeedMapping.kt`) e applicati solo al flusso kmh di `GpsSpeedProvider` — devono essere applicati **anche** al flusso di posizioni usato per la distanza, senza duplicare la logica di filtro. `GpsSpeedProvider` oggi espone solo `StateFlow<SpeedState>` (kmh), non le `Location` grezze accettate — serve decidere come esporre/derivare i delta di posizione filtrati (es. un flusso di `Location` accettate condiviso, da cui derivano sia il valore kmh sia i delta di distanza). Questa è una decisione di architettura per il ricercatore/planner, non ridiscussa con l'utente.

### Reset unificato
- **D-08:** Il pulsante esistente (`resetMaxButton`, string `reset_max_button`) cambia testo da "Azzera massimo" a **"Azzera"** (generico) — azzera sia il massimo sia la distanza nella stessa azione (MAX-04, requisito già bloccato).

### Claude's Discretion
- Tipo/precisione dei dati persistiti per la distanza (es. metri come Int vs altro) — dettaglio tecnico, nessuna preferenza espressa dall'utente.
- Come `GpsSpeedProvider` espone i delta di posizione filtrati (nuovo flow, campo aggiuntivo su `SpeedState`, provider separato) — vedi D-07, lasciato al planner/ricercatore.
- Gestione dei window insets per il nuovo angolo bottom-right (nessun listener esistente per quella posizione — vedi Code Context sotto).
- Se l'area distanza resta nascosta a "0" come l'area MAX (`updateMaxArea()` nasconde a 0 perché "MAX 0" prima di una lettura sarebbe fuorviante), oppure resta sempre visibile (mostrare "0 m" dopo un reset è accurato, non fuorviante, a differenza di MAX) — non discusso esplicitamente con l'utente, nessun requisito lo specifica.
- Nomi delle string resources per i nuovi formati (es. chiave per "%1$d m" / "%1$.1f km").

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requisiti e roadmap
- `.planning/ROADMAP.md` — sezione "Phase 7: Distanza Percorsa e Reset Unificato" (Goal, Depends on, Success Criteria SC1-SC5)
- `.planning/REQUIREMENTS.md` — DIST-01, DIST-02, DIST-03, MAX-04 (requisiti bloccati, sezione "Distanza" e "Velocità Massima")
- `.planning/PROJECT.md` — Key Decisions ("Reset unico per massimo e distanza"), Constraints ("UX (eccezione v1.1): unica animazione/colore ammessi sono quelli dell'icona di ricarica — la distanza NON deve introdurre nuove animazioni o colori")

### Pattern esistenti da estendere
- `app/src/main/java/com/sed/tachimetro/maxspeed/MaxSpeedStore.kt` — pattern SharedPreferences (singolo valore, sanitize on read) da replicare per la persistenza della distanza
- `app/src/main/java/com/sed/tachimetro/maxspeed/MaxSpeedReducer.kt` — pattern di funzioni pure testabili (`reduceMax`, `sanitizePersistedMax`) da replicare per la distanza
- `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt` (righe 51-55: `accuracyThresholdMeters`, `noiseFloorKmh`) — filtri esistenti da riusare/condividere per il calcolo distanza (D-04/D-05/D-07)
- `app/src/main/java/com/sed/tachimetro/gps/SpeedMapping.kt` — `mapSpeedToKmh()`, logica di filtro testata da cui derivare il filtro per la distanza
- `app/src/main/java/com/sed/tachimetro/MainActivity.kt` — `onResetMaxClicked()` (da estendere), `applyMaxAreaWindowInsets()`/`applyBottomLeftWindowInsets()` (pattern da specchiare per il nuovo angolo bottom-right), `updateMaxArea()` (pattern show/hide a valore 0)
- `app/src/main/res/layout/activity_main.xml` — layout ConstraintLayout; angolo bottom-right attualmente libero
- `app/src/main/res/values/strings.xml` — `max_speed_format`, `reset_max_button` (stringa da modificare per D-08)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `MaxSpeedStore`/`MaxSpeedReducer`: pattern diretto da copiare per un nuovo `DistanceStore`/`DistanceReducer` (SharedPreferences singolo valore int, sanitize negativi a 0, funzioni pure testabili).
- `onResetMaxClicked()`: già l'unico punto di reset nell'app — estenderlo per azzerare anche la distanza soddisfa MAX-04 senza un secondo pulsante.
- `unitText` (TextView piccola, ancorata, pattern D-02): riusare lo stesso approccio per l'unità della distanza.
- D-09 pattern (`MainActivity.kt:122-126`): leggere il valore persistito PRIMA di avviare la raccolta GPS, per evitare un flash "0" all'avvio — applicabile identico alla distanza.

### Established Patterns
- Reset immediato e persistito subito su disco, nessuna conferma, nessun batching (`onResetMaxClicked`) — stesso pattern atteso per la distanza.
- GPS engine esposto come `StateFlow<SpeedState>` (solo kmh, nessuna `Location` grezza/lat-lng esposta) — D-06/D-07 richiede di estendere questo confine per esporre anche i delta di posizione filtrati.
- Window insets gestiti esplicitamente per ogni angolo (`applyUnitTextWindowInsets` top-right, `applyMaxAreaWindowInsets` top-left, `applyBottomLeftWindowInsets` bottom-left) — nessun listener esiste ancora per bottom-right; ne serve uno nuovo sullo stesso modello.
- Le aree secondarie (MAX) restano nascoste finché il valore è 0 — vedi "Claude's Discretion" sopra per se applicare lo stesso alla distanza.

### Integration Points
- `MainActivity.onCreate()`: lettura `DistanceStore` prima di avviare `gpsSpeedProvider` (mirror di `maxSpeedStore.read()`/`currentMax`).
- Collettore dello stato GPS (`gpsSpeedProvider.state.collect { ... }` in `updatePlaceholder()`, o un nuovo collettore parallelo se il provider viene esteso con un flow di posizioni): punto in cui la distanza si accumula ad ogni fix accettato.
- `onResetMaxClicked()`: estendere per azzerare anche `DistanceStore`.

</code_context>

<specifics>
## Specific Ideas

- Formato distanza: "850 m" sotto 1 km, poi "1,2 km" (una decimale) sopra — esempi concreti dati durante la discussione.
- Dimensione testo distanza: 32sp esatti (non un range autosize).
- Testo pulsante reset: "Azzera" (non "Azzera tutto", non "Azzera massimo").
- Calcolo distanza: esplicitamente `Location.distanceTo()` tra fix consecutivi, non integrazione della velocità — l'utente ha scelto la precisione sul percorso reale pur sapendo che richiede più lavoro di filtro (D-07).

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 7-Distanza Percorsa e Reset Unificato*
*Context gathered: 2026-08-29*
