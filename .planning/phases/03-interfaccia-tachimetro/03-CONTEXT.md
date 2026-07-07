# Phase 3: Interfaccia Tachimetro - Context

**Gathered:** 2026-07-07
**Status:** Ready for planning

<domain>
## Phase Boundary

Questa fase costruisce l'interfaccia finale a schermo intero: il numero della velocità come elemento dominante, sempre centrato e il più grande possibile in ogni orientamento (portrait/landscape), su sfondo nero ad alto contrasto, sostituendo il layout placeholder minimale delle Fasi 1-2. Include anche l'integrazione dei messaggi di stato esistenti (ricerca segnale, permesso negato/rifiuto permanente) nello stesso schema visivo. Non include la velocità massima (Fase 4) né il toggle schermo sempre acceso (Fase 5).

</domain>

<decisions>
## Implementation Decisions

### Dimensionamento automatico del numero
- **D-01:** Usare `autoSizeTextType="uniform"` (o l'equivalente `TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration`) sul TextView esistente — nessuna dipendenza aggiuntiva, il testo si ridimensiona automaticamente per riempire lo spazio disponibile sia con 1 cifra ("5") che con 3 ("180").

### Layout portrait vs landscape
- **D-02:** Un unico layout XML adattivo (niente `res/layout-land/` separato) — il ConstraintLayout con vincoli e auto-size si adatta automaticamente a entrambi gli orientamenti senza duplicare file.

### Integrazione numero + messaggi di stato
- **D-03:** Riutilizzare lo stesso `TextView` (`messageText`) con auto-size sia per il numero della velocità sia per i messaggi di stato ("Ricerca segnale GPS...", permesso negato/permanente) — con `autoSizeTextType=uniform`, un messaggio testuale più lungo si ridimensiona naturalmente più piccolo del numero (1-3 cifre), ottenendo automaticamente la gerarchia visiva corretta senza bisogno di un secondo elemento di testo.
- **D-04:** Il pulsante "Riprova"/"Apri impostazioni" mantiene lo stesso posizionamento attuale (sotto il messaggio, centrato), semplicemente ricentrato nel nuovo layout dominato dal numero.

### Spazio per velocità massima (Fase 4)
- **D-05:** La Fase 3 costruisce solo il numero principale della velocità — nessuno spazio/area secondaria riservata in anticipo per la velocità massima. La Fase 4 modificherà il layout quando implementerà MAX-01/02/03.

### Claude's Discretion
- Valori esatti di `autoSizeMinTextSize`/`autoSizeMaxTextSize`/`autoSizeStepGranularity`, margini/padding esatti, eventuale uso di `Typeface.DEFAULT_BOLD` vs stile Black se disponibile nel tema.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requisiti e progetto
- `.planning/PROJECT.md` — Constraints: font di sistema Bold/Black, sfondo nero, nessun elemento grafico non necessario
- `.planning/REQUIREMENTS.md` — UI-01, UI-02, UI-03, UI-04, UI-05 sono i requisiti di questa fase

### Fasi precedenti
- `.planning/phases/01-fondamenta-permessi-e-avvio/01-CONTEXT.md` — placeholder nero originale, flusso permesso già implementato
- `.planning/phases/02-motore-gps/02-CONTEXT.md` — motore GPS e formato "N km/h" già implementati, questa fase sostituisce solo la presentazione visiva
- `app/src/main/res/layout/activity_main.xml` — layout attuale (ConstraintLayout, `messageText` 20sp bold, `retryButton`) da evolvere con auto-size
- `app/src/main/java/com/sed/tachimetro/MainActivity.kt` — `showReady()`, `showDenied()`, `updatePlaceholder()` già scrivono su `messageText`; questa fase non cambia la logica di stato, solo la resa visiva

No external specs — requirements fully captured in decisions above.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `app/src/main/res/layout/activity_main.xml` — ConstraintLayout esistente con `messageText`/`retryButton`, già su sfondo nero (`@android:color/black`), da estendere con auto-size invece di ricostruire da zero
- `app/src/main/java/com/sed/tachimetro/MainActivity.kt` — `showReady()`, `showDenied()`, `updatePlaceholder(state: SpeedState)` scrivono già tutto il testo via `getString()`; nessuna modifica di logica necessaria, solo del layout/stile visivo
- `app/src/main/res/values/strings.xml` — tutte le stringhe italiane già presenti (permesso, ricerca segnale, formato velocità)

### Established Patterns
- ConstraintLayout con vincoli percentuali/chain per centratura — pattern già in uso, da estendere per auto-size
- Tutto il testo passa da `getString(R.string.*)` — nessuna stringa hardcoded (eccetto il fix WR-03 già applicato in Fase 2 per il formato "N km/h")

### Integration Points
- `activity_main.xml` — il `TextView messageText` esistente è il punto di integrazione principale per l'auto-size
- Nessuna modifica a `MainActivity.kt` prevista oltre eventuali riferimenti a nuovi ID di layout, se il planner decide di rinominare/ristrutturare l'elemento

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

*Phase: 3-Interfaccia Tachimetro*
*Context gathered: 2026-07-07*
