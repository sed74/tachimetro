# Phase 4: Velocità Massima e Persistenza - Context

**Gathered:** 2026-07-10
**Status:** Ready for planning

<domain>
## Phase Boundary

Questa fase aggiunge il monitoraggio della velocità massima raggiunta dall'ultimo azzeramento, mostrata in un'area secondaria dello schermo, con un pulsante per azzerarla e persistenza su disco che sopravvive a chiusura app e riavvio del telefono. Non include il toggle "Schermo sempre acceso" (Fase 5).

</domain>

<decisions>
## Implementation Decisions

### Posizione e formato area velocità massima
- **D-01:** L'area della velocità massima va posizionata in alto a sinistra, speculare a `unitText` ("km/h") che occupa già l'alto a destra — layout simmetrico, numero principale libero al centro.
- **D-02:** Formato con etichetta testuale + numero, es. "MAX 120" (non solo il numero nudo) — evita ambiguità con la velocità attuale.

### Comportamento pulsante "Azzera massimo"
- **D-03:** Il pulsante "Azzera massimo" è visibile solo quando il valore massimo corrente è > 0 — nascosto quando non c'è ancora un record da azzerare.
- **D-04:** Azzeramento immediato al tap, nessun dialog/conferma richiesta (coerente con UI-04, nessun elemento non necessario; azzerare il record non è un'azione distruttiva di dati critici).
- **D-05:** Il pulsante va posizionato sotto l'etichetta MAX, in alto a sinistra — vicino al valore che controlla, zona simmetrica a `unitText`/area MAX a destra. Non condivide posizione/ID con `retryButton` (quest'ultimo visibile solo negli stati di errore permesso, mai insieme al pulsante Azzera).

### Timing della persistenza su disco
- **D-06:** Persistenza via SharedPreferences (già la decisione di default in PROJECT.md — Key Decisions, ora confermata attiva per questa fase).
- **D-07:** Il nuovo record massimo viene scritto su disco immediatamente ad ogni aggiornamento che supera il massimo corrente (non solo su `onPause`/`onStop`) — scrittura economica (un solo intero), evita perdita dati se l'app viene terminata bruscamente (kill di sistema, batteria scarica) senza passare da `onPause`.
- **D-08:** Anche l'azzeramento (nuovo valore 0) viene scritto su disco immediatamente al tap del pulsante — se l'app si chiude subito dopo l'azzeramento, alla riapertura non deve ricomparire il vecchio massimo.

### Comportamento iniziale / valore assente
- **D-09:** Prima che sia mai stata registrata una lettura valida (max = 0, nessun dato precedente salvato su disco), l'intera area MAX (etichetta + numero + pulsante Azzera) resta nascosta — nessun "MAX 0" fuorviante mostrato all'avvio. Coerente con D-03 (pulsante visibile solo se massimo > 0).

### Claude's Discretion
- Nome/struttura esatta della chiave SharedPreferences, stile/dimensione esatta del testo "MAX 120" (fisso o autosize, in analogia a `unitText`), esatto padding/margini dell'area MAX e del pulsante Azzera, gestione del ciclo di vita per la lettura del valore salvato all'avvio (`onCreate` vs altro punto), formato interno del dato persistito (Int diretto vs altra rappresentazione).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requisiti e progetto
- `.planning/PROJECT.md` — Core Value, Constraints (font di sistema Bold/Black, sfondo nero, nessun elemento grafico non necessario), Key Decisions (SharedPreferences per persistenza, "Pending" → confermata attiva in questa fase)
- `.planning/REQUIREMENTS.md` — MAX-01, MAX-02, MAX-03 sono i requisiti di questa fase

### Fasi precedenti
- `.planning/phases/03-interfaccia-tachimetro/03-CONTEXT.md` — D-01..D-05: layout unico adattivo, autosize uniform, `messageText` condiviso per numero e messaggi
- `.planning/phases/03-interfaccia-tachimetro/03-01-SUMMARY.md` — storico dei 4 round di deviazione: introduzione di `unitText` (top-right, piccolo, window-insets-aware), fix edge-to-edge/targetSdk 36, modalità fullscreen immersiva (`enableImmersiveFullscreen()`) — l'area MAX (D-01, top-left) deve applicare lo stesso pattern di gestione dei window insets di `unitText` per restare libera da status bar/cutout in entrambi gli orientamenti
- `app/src/main/java/com/sed/tachimetro/MainActivity.kt` — `updatePlaceholder(state: SpeedState)` è il punto di integrazione naturale per confrontare `state.kmh` col massimo corrente; `applyUnitTextWindowInsets()` è il pattern di riferimento per il posizionamento dell'area MAX
- `app/src/main/res/layout/activity_main.xml` — layout attuale con `messageText`, `unitText` (top-right), `retryButton`

No external specs — requirements fully captured in decisions above.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `app/src/main/java/com/sed/tachimetro/gps/SpeedState.kt` (Fase 2) — `SpeedState.Reading(kmh: Int)` è la fonte del valore corrente da confrontare col massimo
- `MainActivity.applyUnitTextWindowInsets()` — pattern già pronto da riusare/adattare per il nuovo elemento in alto a sinistra (window insets su targetSdk 36 edge-to-edge)
- `MainActivity.applySpeedAutosize()`/`applyMessageAutosize()` — pattern di autosize a due configurazioni runtime, riferimento se l'area MAX necessita di ridimensionamento

### Established Patterns
- Tutto il testo passa da `getString(R.string.*)` — nessuna stringa hardcoded (nuova stringa "MAX %1$d km/h" o simile da aggiungere a `strings.xml`, in italiano)
- `ConstraintLayout` con vincoli a `parent` per il posizionamento (pattern di `unitText`: `layout_constraintEnd_toEndOf="parent"` + `layout_constraintTop_toTopOf="parent"`, da specchiare con `layout_constraintStart_toStartOf="parent"` per il lato sinistro)
- Nessuna dipendenza Gradle aggiuntiva prevista: SharedPreferences è API Android nativa, nessun bisogno di Room/DataStore per un singolo intero

### Integration Points
- `MainActivity.updatePlaceholder(state: SpeedState)` — punto dove confrontare `state.kmh` col massimo persistito e aggiornare la UI/il valore salvato
- `MainActivity.onCreate()` — punto dove leggere il valore massimo salvato all'avvio, prima di iniziare la raccolta GPS

</code_context>

<specifics>
## Specific Ideas

Nessuna idea specifica aggiuntiva oltre alle decisioni sopra.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 04-velocit-massima-e-persistenza*
*Context gathered: 2026-07-10*
