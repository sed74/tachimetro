# Phase 9: Permesso di Localizzazione dallo Schermo Auto - Context

**Gathered:** 2026-09-02
**Status:** Ready for planning

<domain>
## Phase Boundary

Se il permesso `ACCESS_FINE_LOCATION` non è ancora stato concesso, l'utente può concederlo direttamente dallo schermo Android Auto al primo collegamento, senza dover prima aprire l'app sul telefono. Copre l'intero ciclo: richiesta esplicita via `CarContext.requestPermissions()`, transizione automatica alla velocità dopo la concessione (senza riavvio app/collegamento), e un messaggio chiaro con possibilità di riprovare se l'utente nega.

Requirements coperti: AA-04 (v. REQUIREMENTS.md).

Fuori scope per questa fase (già deciso a livello di milestone/roadmap, non riaperto qui): comportamento del telefono alla connessione/disconnessione Android Auto (Fase 10), `HostValidator` reale e verifica su dispositivo fisico in background (Fase 11), velocità massima/distanza sullo schermo auto (out of scope di milestone), categoria/template Car App Library (già lockati in Fase 8 — `PaneTemplate`/categoria `POI`, non riaperti qui).

Punto di partenza noto: `SpeedScreen.kt` ha già un gate difensivo (T-08-08, Fase 8) che verifica il permesso ad ogni `STARTED` e, se non concesso, salta semplicemente la collection — lo schermo resta bloccato per sempre su "Ricerca segnale..." senza mai richiedere il permesso. Questa fase sostituisce quel gate passivo con una richiesta attiva.

</domain>

<decisions>
## Implementation Decisions

### Attesa "controlla il telefono" (mentre il dialogo è sul telefono)
- **D-01:** Il dialogo di sistema per il permesso appare SUL TELEFONO, non sull'auto — limite strutturale di `CarContext.requestPermissions()` (Pitfall 4, `PITFALLS.md`). Mentre è in attesa della risposta, lo schermo auto mostra un testo dedicato: **"Controlla il telefono"** — breve e diretto, stesso tono/lunghezza di `car_searching_gps_signal` (D-02 Fase 8). Richiede una nuova risorsa stringa, non riusa `car_searching_gps_signal` né `searching_gps_signal`.

### Rifiuto: messaggio e azione
- **D-02:** Se l'utente nega il permesso, lo schermo auto mostra un messaggio ispirato a `permission_denied` del telefono ma accorciato, coerente con il pattern già usato per D-02 di Fase 8 (stringa auto sempre più breve di quella telefono). Testo esatto lasciato a Claude's Discretion (vedi sotto), tono/contenuto: spiega che manca il permesso, non uno schermo vuoto.
- **D-03:** Lo schermo auto offre un'azione di retry esplicita (riga/`Action`) che rilancia `CarContext.requestPermissions()` senza dover uscire dall'app auto — mirror concettuale del `retryButton` sul telefono (`MainActivity.onRetryClicked()`).
- **D-04:** Il messaggio/azione DEVE distinguere un primo rifiuto da un rifiuto permanente ("non chiedere più"), esattamente come fa il telefono (`permission_denied` vs `permission_denied_permanent`, dove il bottone passa da "Riprova" a "Apri impostazioni"). Sull'auto: se il rilancio del dialogo non ha più effetto, il messaggio/azione deve indirizzare l'utente ad aprire le impostazioni sul telefono invece di ritentare a vuoto.
  - **Nota per la ricerca (Claude's Discretion sul COME, non sul COSA):** `CarContext` non espone un equivalente diretto di `Activity.shouldShowRequestPermissionRationale()` (API legata a un'Activity). Il researcher deve investigare come rilevare lo stato "permanentemente negato" per lo schermo auto — possibili strade: tenere traccia lato app del numero di tentativi/esiti riportati dal callback di `requestPermissions()` (permessi approvati/rifiutati), oppure interrogare `shouldShowRequestPermissionRationale()` tramite un'Activity comunque presente nel processo (es. `MainActivity`, se mai avviata) con fallback ragionevole se l'app non è mai stata aperta sul telefono. Il comportamento desiderato (D-04) è fissato; il meccanismo tecnico è aperto.

### Quando scatta la richiesta
- **D-05:** La richiesta di permesso parte **automaticamente** al primo collegamento (quando lo schermo auto rileva permesso non concesso), mirror di `MainActivity.checkAndRequestPermission()` chiamato in `onCreate()` — coerente con la filosofia "nessun menu" del progetto. Nessuna azione preliminare richiesta all'utente sull'auto prima che il dialogo appaia sul telefono.
- **D-06:** Il retry dopo un rifiuto (D-03) resta invece un'azione esplicita a tocco — non si rilancia automaticamente il dialogo in loop dopo un rifiuto già ricevuto.

### Branding del dialogo
- **D-07:** Il dialogo di sistema mostrato sul telefono usa l'aspetto di **default**, nessuna personalizzazione via `androidx.car.app.theme`/`carPermissionActivityLayout` — coerente con D-03 di Fase 8 (nessun branding/titolo extra nello schermo auto) e con la filosofia generale di minimalismo del progetto.

### Transizione automatica dopo la concessione (già nei Success Criteria di roadmap, non riaperta come gray area)
- **SC2 (roadmap):** Concedendo il permesso, lo schermo passa automaticamente alla velocità (o a "Ricerca segnale...") senza riavvio app/collegamento — il meccanismo reattivo esatto (es. uno `StateFlow` di permesso osservato dal collector esistente in `SpeedScreen`, mirror del pattern `permissionGranted` già usato in `MainActivity`) è lasciato all'implementazione, non è un gray area di UX.

### Verifica DHU dal vivo (Piano 03, Task 2 — checkpoint umano risolto)

- **D-08:** Sessione DHU dal vivo eseguita dall'utente, percorrendo tutti gli scenari richiesti dal checkpoint di chiusura fase (`09-03-PLAN.md` Task 2). Esito: **tutti gli scenari A-F confermati "tutto ok"** dall'utente in risposta punto per punto:
  - **Scenario A** (richiesta automatica al primo collegamento, SC1 di roadmap): confermato — "Controlla il telefono" mostrato sull'auto, dialogo di sistema apparso sul telefono senza interazione sull'auto.
  - **Scenario B** (concessione e transizione automatica, SC2 di roadmap): confermato — passaggio automatico a velocità/"Ricerca segnale..." senza riavvio app/collegamento.
  - **Scenario C** (primo rifiuto e retry, SC3 di roadmap): confermato — "Permesso GPS necessario" + azione "Riprova" mostrati, nessun loop del dialogo, retry funzionante a veicolo fermo.
  - **Scenario D** (rifiuto permanente e apertura impostazioni): confermato — "Permesso negato. Apri le impostazioni sul telefono" + azione "Apri impostazioni" funzionante, transizione post-concessione dalle impostazioni confermata.
  - **Scenario E** (quota e forma del template, Pitfall 4 mai verificato prima su questa variazione strutturale Row-sola ↔ Row+Action): confermato — nessuna chiusura dell'app da parte dell'host, nessun errore host, PID stabile durante tutte le transizioni.
  - **Scenario F** (nessuna regressione): confermato — comportamento telefono (velocità/MAX/distanza/icona ricarica/switch "sempre acceso") invariato rispetto a v1.1, nessun crash in `adb logcat -b crash`.
  - I tre Success Criteria di roadmap della Fase 9 sono quindi **confermati dal vivo su DHU**, cosi' come Pitfall 4 (transizione di forma del template) e' chiuso empiricamente.

- **D-09:** **Scenario G / Pitfall 1 — limite di piattaforma esplicitamente accettato per v2.0.** La Javadoc di `CarContext.requestPermissions()` dichiara che l'host può ignorare silenziosamente la richiesta quando ritiene non sicuro mostrarla (es. veicolo già in movimento al momento del collegamento). Se questo accade, lo schermo auto può restare bloccato su "Controlla il telefono" finché il veicolo non si ferma e un nuovo ingresso in `STARTED` (es. scollegando/ricollegando) non rivaluta lo stato — le decisioni D-05/D-06 di questa fase non prevedono un'azione di sblocco manuale in quello stato. Alla domanda di chiarimento esplicita posta durante il checkpoint, l'utente ha risposto **"Accettato"**: il limite è accettato così com'è per la milestone v2.0, nessuna modifica alle decisioni D-05/D-06 richiesta (niente azione di retry aggiunta allo stato `Waiting`). Registrato anche in `STATE.md` come concern noto, sulla falsariga di SC2 in Fase 8 (D-11).

### Claude's Discretion
- Testo esatto italiano per i messaggi "Controlla il telefono" (D-01, wording bloccato), rifiuto singolo e rifiuto permanente (D-02/D-04, tono/contenuto bloccato ma stringa esatta libera) — nomi delle risorse stringa.
- Meccanismo tecnico per rilevare lo stato "permanentemente negato" dallo `Screen` auto (nota sotto D-04) — il researcher deve investigare le opzioni disponibili in `CarContext`/callback di `requestPermissions()`.
- Meccanismo reattivo esatto per la transizione automatica post-concessione (SC2) — pattern coerente con `permissionGranted` di `MainActivity`, dettaglio implementativo.
- Forma esatta dell'azione di retry sul template auto (es. `Action` in `PaneTemplate`, riga cliccabile) — vincolo funzionale deciso (D-03), scelta del componente Car App Library lasciata al planner.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Ricerca di progetto (v2.0 Android Auto)
- `.planning/research/PITFALLS.md` §Pitfall 4 — `CarContext.requestPermissions()`, dialogo phone-side, branding via `androidx.car.app.theme`/`carPermissionActivityLayout`, pattern "check your phone" — riferimento primario per questa fase
- `.planning/research/SUMMARY.md` — sezione permesso da schermo auto ("check your phone" car-screen state), fase P1 in `FEATURES.md`
- `.planning/research/STACK.md` — link `request-permissions`, pattern `checkSelfPermission()` check-first
- `.planning/research/FEATURES.md` — voce "Car-screen permission handling (CarContext.requestPermissions)", priorità P1

### Roadmap e requisiti
- `.planning/ROADMAP.md` §"Phase 9: Permesso di Localizzazione dallo Schermo Auto" — goal, 3 success criteria, dipendenze (Fase 8)
- `.planning/REQUIREMENTS.md` §"Schermo Android Auto" — AA-04 (requisito coperto da questa fase)
- `.planning/PROJECT.md` §"Constraints" — filosofia "nessun elemento non necessario" (rilevante per D-07); §"Active" — AA-04 elencato come requisito attivo di Fase 9

### Contesto Fase 8 (fondamenta condivise)
- `.planning/phases/08-fondamenta-condivise-e-velocit-sullo-schermo-auto/08-CONTEXT.md` — D-02 (pattern stringa auto più corta del telefono, riusato per D-01/D-02 qui), D-03 (nessun branding/titolo nel car screen, riusato per D-07 qui)

No external specs (nessun SPEC.md per questa fase) — requisiti pienamente catturati in ROADMAP.md/REQUIREMENTS.md e nelle decisioni sopra.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `SpeedScreen.kt` (`app/src/main/java/com/sed/tachimetro/car/SpeedScreen.kt`): contiene già il gate difensivo sul permesso (T-08-08, righe 54-77) da sostituire con la richiesta attiva — il commento inline al suo interno menziona esplicitamente che "la richiesta esplicita del permesso via `CarContext.requestPermissions()` è scope della Fase 9".
- `MainActivity.kt` — `checkAndRequestPermission()`, `onRetryClicked()`, `refreshPermissionState()`, `permissionGranted: MutableStateFlow<Boolean>` (righe 103-113, 304-334): pattern di riferimento diretto per trigger automatico (D-05), retry (D-03), distinzione rifiuto singolo/permanente (D-04) — NON riusabile as-is (Pitfall 4: `ActivityResultContracts` non funziona da uno `Screen`), ma il comportamento/UX va rispecchiato con le API `CarContext`.
- `strings.xml` — `permission_denied`, `permission_denied_permanent`, `retry`, `open_settings`: riferimento di tono/wording per le nuove stringhe auto (D-02/D-04), pattern già stabilito con `car_searching_gps_signal` (stringa auto sempre più corta della corrispondente telefono).
- `CarSpeedContent.kt` — modello sealed framework-free (`CarSpeedContent.Speed`/`Searching`) che mappa `SpeedState` al contenuto della Row: probabile punto di estensione per rappresentare i nuovi stati (attesa permesso, rifiuto) in modo puro/testabile, coerente con il pattern esistente.

### Established Patterns
- `SpeedScreen` colleziona `provider.gpsSpeedProvider.state` dentro `repeatOnLifecycle(Lifecycle.State.STARTED)` (righe 55-76) — il nuovo flusso di richiesta permesso deve integrarsi in questo stesso blocco lifecycle-aware, non introdurre un secondo meccanismo di collection parallelo.
- `TachimetroApplication.gpsSpeedProvider` è Application-scoped (D-00b, Fase 8) — nessuna modifica prevista qui, il permesso riguarda l'accesso a questo provider condiviso, non la sua costruzione.

### Integration Points
- `SpeedScreen.onGetTemplate()` (righe 79-109) costruisce oggi solo due varianti di `Row` (`Speed`/`Searching`) dentro un `PaneTemplate` — va esteso con le nuove varianti per "attesa permesso" e "rifiuto" (con eventuale `Action` di retry, D-03).
- Nessuna dichiarazione `androidx.car.app.theme` esiste oggi nel manifest — se in futuro si rivalutasse D-07, andrebbe aggiunta lì; per questa fase non serve (D-07: dialogo di default).

</code_context>

<specifics>
## Specific Ideas

- Testo di attesa: esattamente **"Controlla il telefono"** (D-01) — non "Permesso richiesto sul telefono" (opzione scartata, più lunga) né il riuso di "Ricerca segnale..." (scartato, ambiguo).
- Il retry dopo un rifiuto deve essere un'azione visibile sullo schermo auto stesso (D-03), non un rimando generico "vai sul telefono" — l'utente non deve uscire dall'app auto per riprovare.
- La distinzione rifiuto-singolo/rifiuto-permanente (D-04) è esplicitamente richiesta com'è già sul telefono — non una semplificazione accettabile per questa fase.
- Il dialogo di permesso resta quello di sistema, senza branding (D-07) — nessuna richiesta di investire tempo in personalizzazione visiva per questa fase.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 9-Permesso di Localizzazione dallo Schermo Auto*
*Context gathered: 2026-09-02*
