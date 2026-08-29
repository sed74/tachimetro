# Phase 6: Indicatore di Ricarica - Context

**Gathered:** 2026-08-29
**Status:** Ready for planning

<domain>
## Phase Boundary

L'utente riconosce immediatamente quando il telefono è in carica, tramite un'icona a fulmine animata posizionata accanto al toggle "sempre acceso". Copre: rilevamento continuo dello stato di ricarica, comparsa/scomparsa dell'icona, animazione di riempimento in loop. Non copre: la Fase 7 (distanza percorsa/reset unificato) né alcuna altra funzionalità.

</domain>

<decisions>
## Implementation Decisions

### Trigger e visibilità
- **D-01:** L'icona appare solo quando il telefono è collegato alla ricarica (`BATTERY_STATUS_CHARGING` o `BATTERY_STATUS_FULL`) e scompare immediatamente allo scollegamento. Serve rilevamento **continuo** durante la sessione app (non un controllo one-shot come l'attuale `isDeviceCharging()` in `MainActivity.kt:424`, che è usato solo all'avvio per il default del toggle "sempre acceso" — quella funzione NON va riusata così com'è, va estesa/affiancata da un meccanismo reattivo).

### Animazione
- **D-02:** Riempimento progressivo dal basso verso l'alto, bianco → lime → bianco, loop continuo di circa 2-3 secondi per ciclo, per tutta la durata della ricarica (decisione presa a livello di milestone, vedi PROJECT.md).
- **D-03:** Quando la batteria raggiunge lo stato `BATTERY_STATUS_FULL` (piena) mentre il telefono resta collegato, l'icona **smette di pulsare e resta fissa, completamente piena di lime solido** — segnala visivamente che la ricarica attiva è terminata pur restando il cavo collegato. Torna a pulsare (o scompare) se lo stato cambia di nuovo.
- **D-04:** Il lime è l'unico colore accento ammesso nell'intera interfaccia, riservato esclusivamente a questa icona. Nessun'altra animazione o colore va introdotto altrove (vincolo esplicito, vedi PROJECT.md Constraints).

### Stile e posizione
- **D-05:** Stile del fulmine: icona "flash" Material classica (zigzag pieno, tipo `ic_flash_on` di sistema) — non uno stile outline/sottile custom.
- **D-06:** Posizione: **a sinistra** dello switch "sempre acceso" (`keepScreenOnSwitch`), sulla stessa riga orizzontale — non sopra, non a destra verso il centro schermo.

### Claude's Discretion
- Dimensione esatta dell'icona in dp/sp relativa allo switch, meccanismo tecnico di rilevamento continuo dello stato di ricarica (BroadcastReceiver su `ACTION_POWER_CONNECTED`/`ACTION_POWER_DISCONNECTED` vs osservazione dello sticky broadcast `ACTION_BATTERY_CHANGED`), implementazione dell'animazione (AnimatedVectorDrawable, ValueAnimator con clip/layer, o Lottie — nessuna libreria di animazione è già presente nel progetto), esatto valore del colore lime (hex) da aggiungere a `colors.xml`.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Vincoli di progetto e requisiti
- `.planning/PROJECT.md` — sezione "Current Milestone: v1.1", Constraints (eccezione UX v1.1 su animazione/colore lime), Key Decisions (deroga mirata)
- `.planning/REQUIREMENTS.md` — CHRG-01, CHRG-02 (testo esatto dei requisiti di questa fase)
- `.planning/ROADMAP.md` — Phase 6, Success Criteria

Nessuna ADR o spec esterna esiste per questo progetto — i vincoli sono interamente catturati in PROJECT.md/REQUIREMENTS.md sopra.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `keepScreenOnSwitch` (`MainActivity.kt:64,106`) — view di riferimento per l'ancoraggio della nuova icona (stessa riga, a sinistra)
- `applyScreenSwitchWindowInsets()` (`MainActivity.kt:435-450`) — pattern esistente di gestione window insets per l'angolo bottom-left; la nuova icona condivide lo stesso angolo schermo e dovrà tenerne conto (stesso pattern, listener dedicato se necessario)
- `switch_thumb_tint.xml` / `switch_track_tint.xml` (`res/color/`) — pattern esistente per color state list, utile come riferimento se serve un tint state-based sull'icona

### Established Patterns
- Rilevamento ricarica esistente ma one-shot: `isDeviceCharging()` (`MainActivity.kt:424-429`) legge lo sticky broadcast `ACTION_BATTERY_CHANGED` via `registerReceiver(null, filter)`. Per il rilevamento continuo richiesto da questa fase, il pattern coerente col resto del codebase (vedi `GpsSpeedProvider` in `CONVENTIONS.md`) sarebbe un `callbackFlow`/`BroadcastReceiver` osservato con lo stesso stile `lifecycleScope` + `repeatOnLifecycle(STARTED)` già usato per GPS e permessi.
- Nessuna animazione o libreria di animazione presente nel codebase — questa fase introduce la prima eccezione esplicita (vedi PROJECT.md Constraints).
- Palette colori attuale (`colors.xml`) è solo bianco/nero/grigi — andrà aggiunto un colore lime.

### Integration Points
- `MainActivity.onCreate()` intorno a `MainActivity.kt:105-123` (blocco di setup di `keepScreenOnSwitch`) è il punto naturale di integrazione per l'inizializzazione della nuova icona e del suo listener di stato ricarica.
- `activity_main.xml` — nuovo elemento `ImageView`/vettore da aggiungere al `ConstraintLayout` esistente, vincolato relativamente a `keepScreenOnSwitch`.

</code_context>

<specifics>
## Specific Ideas

- L'utente ha descritto l'animazione come un "riempimento" — non un semplice crossfade di colore, ma un livello di lime che sale dentro la sagoma del fulmine come un liquido, poi si svuota tornando bianco, in loop continuo.
- Icona "flash" Material classica, non un design custom.
- Batteria piena → icona fissa e completamente lime, non più pulsante.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 6-Indicatore di Ricarica*
*Context gathered: 2026-08-29*
