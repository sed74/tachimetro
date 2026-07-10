# Phase 5: Gestione Schermo - Context

**Gathered:** 2026-07-10
**Status:** Ready for planning

<domain>
## Phase Boundary

Questa fase aggiunge un toggle per scegliere tra "Schermo sempre acceso" e "Schermo automatico" durante l'uso dell'app, con blocco effettivo dello spegnimento schermo quando attivo (`FLAG_KEEP_SCREEN_ON`) e preferenza persistita su disco tra sessioni. Non include altre impostazioni o schermate di configurazione.

</domain>

<decisions>
## Implementation Decisions

### Posizione e forma del toggle
- **D-01:** Switch (interruttore) con etichetta breve — non testo cliccabile — stato ON/OFF immediatamente chiaro a colpo d'occhio.
- **D-02:** Posizionato in basso a sinistra, speculare verticalmente all'area MAX (in alto a sinistra) — lato sinistro dello schermo bilanciato tra le due informazioni secondarie (velocità massima sopra, gestione schermo sotto), lato destro riservato a "km/h".

### Visibilità del toggle
- **D-03:** Sempre visibile (piccolo), non nascosto dietro un gesture/tap — coerente con `unitText`/area MAX già sempre visibili quando applicabile; l'utente deve poterlo controllare a colpo d'occhio anche montato su un supporto in movimento, dove i tap precisi/nascosti sono scomodi.

### Valore iniziale di default
- **D-04:** Al primo avvio (nessuna preferenza salvata su disco), il default iniziale dello switch è determinato dallo stato di ricarica del telefono in quel momento: **sempre acceso** se il telefono è in ricarica, **automatico** se non lo è. Rationale utente: uno schermo sempre acceso ha senso quando non c'è pressione sulla batteria (in ricarica, tipico durante l'uso in auto/moto con cavo), mentre di default si preferisce risparmiare batteria se non in ricarica.
- **D-05:** Questo rilevamento dello stato di ricarica si applica **solo al primissimo avvio** (quando non esiste ancora nessuna preferenza salvata). Una volta che l'utente ha interagito con lo switch (o è stato scritto un valore di default al primo avvio), quel valore persistito è sempre quello che conta ad ogni avvio successivo — lo stato di ricarica NON viene più ricontrollato per sovrascrivere la scelta dell'utente.

### Comportamento del blocco spegnimento
- **D-06:** Il cambio di stato dello switch applica immediatamente `FLAG_KEEP_SCREEN_ON` (attivazione) o lo rimuove (disattivazione) sulla finestra corrente — nessun riavvio o riapertura app richiesti.
- **D-07:** La preferenza viene scritta su disco (SharedPreferences, stesso pattern di `MaxSpeedStore` dalla Fase 4) immediatamente ad ogni cambio dello switch, così sopravvive a chiusura app e riavvio del telefono (SCRN-03).

### Claude's Discretion
- Nome/struttura esatta della chiave SharedPreferences (nuovo store dedicato o riuso/estensione del pattern `MaxSpeedStore`), API esatta per rilevare lo stato di ricarica al primo avvio (es. `BatteryManager`/`ACTION_BATTERY_CHANGED`), testo esatto dell'etichetta breve dello switch, stile/dimensioni esatte del widget Switch, gestione dei window insets per il nuovo elemento in basso a sinistra (se necessaria, in analogia al pattern già usato per `unitText`/area MAX).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requisiti e progetto
- `.planning/PROJECT.md` — Core Value, Constraints, Key Decisions (nessuna decisione precedente su SCRN-* — questa è la prima fase a introdurli)
- `.planning/REQUIREMENTS.md` — SCRN-01, SCRN-02, SCRN-03 sono i requisiti di questa fase

### Fasi precedenti
- `.planning/phases/04-velocit-massima-e-persistenza/04-CONTEXT.md` — pattern di persistenza SharedPreferences già stabilito e approvato (D-06/D-07/D-08 di quella fase)
- `app/src/main/java/com/sed/tachimetro/maxspeed/MaxSpeedStore.kt` — wrapper SharedPreferences esistente da cui replicare il pattern (o riusare/estendere)
- `app/src/main/java/com/sed/tachimetro/MainActivity.kt` — `applyUnitTextWindowInsets()`/`applyMaxAreaWindowInsets()` (se presente) sono il pattern di riferimento per il posizionamento di elementi ancorati agli angoli con gestione insets
- `app/src/main/res/layout/activity_main.xml` — layout attuale: `messageText` (centro), `unitText` (alto-dx), `maxSpeedText`/`resetMaxButton` (alto-sx), `retryButton` (basso-centro, solo stati errore)

No external specs — requirements fully captured in decisions above.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `app/src/main/java/com/sed/tachimetro/maxspeed/MaxSpeedStore.kt` (Fase 4) — pattern SharedPreferences già collaudato per un valore persistito, da replicare per il nuovo `ScreenOnPreferenceStore` (o nome equivalente) che persiste un Boolean invece di un Int
- `MainActivity.applyUnitTextWindowInsets()` (Fase 3) — pattern per ancorare un elemento a un angolo con margini che rispettano i window insets, riusabile per il nuovo switch in basso a sinistra
- `android.os.BatteryManager` / `Intent.ACTION_BATTERY_CHANGED` — API standard Android per rilevare lo stato di ricarica al primo avvio (D-04)

### Established Patterns
- Tutto il testo passa da `getString(R.string.*)` — nessuna stringa hardcoded (nuova stringa per l'etichetta breve dello switch, in italiano)
- ConstraintLayout con vincoli a `parent` per il posizionamento (pattern degli elementi agli angoli: `layout_constraintStart_toStartOf="parent"` + `layout_constraintBottom_toBottomOf="parent"` per basso-sinistra)
- Nessuna dipendenza Gradle aggiuntiva prevista: `Switch`/`SwitchCompat` è un widget AndroidX/platform già disponibile via `androidx.appcompat`

### Integration Points
- `MainActivity.onCreate()` — punto dove leggere la preferenza salvata (o determinare il default da stato di ricarica al primo avvio) e applicare/rimuovere `FLAG_KEEP_SCREEN_ON` di conseguenza
- Nuovo `OnCheckedChangeListener` sullo switch — punto dove applicare immediatamente il flag e scrivere la preferenza su disco

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

*Phase: 05-gestione-schermo*
*Context gathered: 2026-07-10*
