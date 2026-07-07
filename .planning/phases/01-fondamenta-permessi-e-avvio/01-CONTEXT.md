# Phase 1: Fondamenta, Permessi e Avvio - Context

**Gathered:** 2026-07-07
**Status:** Ready for planning

<domain>
## Phase Boundary

Questa fase costruisce le fondamenta dell'app: aggiunge il plugin Kotlin al progetto (attualmente scaffold Java vuoto), crea la prima Activity che si apre direttamente all'avvio (nessun menu/schermata iniziale), e implementa il flusso completo di richiesta/gestione del permesso ACCESS_FINE_LOCATION (concesso, negato, negato permanentemente). Non include ancora la lettura effettiva del GPS (Fase 2) né l'interfaccia finale a schermo intero (Fase 3).

</domain>

<decisions>
## Implementation Decisions

L'utente ha scelto di non discutere aree specifiche per questa fase e ha delegato le decisioni di dettaglio a Claude ("niente, passa alla fase successiva").

### Claude's Discretion

- **Tempo richiesta permesso**: Il popup di sistema per ACCESS_FINE_LOCATION viene richiesto subito al primo avvio dell'app, senza schermate o messaggi intermedi prima del popup — coerente con la filosofia "nessun elemento non necessario" del progetto. Se il sistema richiede una rationale (`shouldShowRequestPermissionRationale`), mostrare un messaggio semplice in italiano che spiega perché serve il GPS, poi ripresentare il popup.
- **Comportamento su rifiuto**: Se l'utente nega il permesso, mostrare un messaggio semplice ("Permesso GPS necessario per funzionare") con un pulsante "Riprova" che ri-richiede il permesso.
- **Rifiuto permanente**: Se il rifiuto è permanente (`shouldShowRequestPermissionRationale` torna `false` dopo un rifiuto), il pulsante deve invece aprire le Impostazioni dell'app (`Settings.ACTION_APPLICATION_DETAILS_SETTINGS`) perché richiedere di nuovo il permesso via popup non avrebbe effetto — pattern standard Android.
- **Schermata placeholder**: Poiché il motore GPS (Fase 2) e l'interfaccia finale (Fase 3) arrivano in fasi successive, quando il permesso è concesso questa fase mostra uno schermo nero minimale con un testo segnaposto neutro in italiano (es. "Pronto"), sostituito interamente dalla UI reale nella Fase 3. Nessun elemento grafico oltre al testo.
- **Setup Kotlin**: Aggiungere il plugin `org.jetbrains.kotlin.android` a `app/build.gradle.kts` e alla version catalog (`gradle/libs.versions.toml`), dato che il progetto attuale non ha Kotlin configurato (solo Java, vedi CONVENTIONS.md).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Stato attuale del codice
- `.planning/codebase/ARCHITECTURE.md` — conferma che non esiste ancora nessun codice applicativo, nessuna Activity, nessun intent-filter LAUNCHER
- `.planning/codebase/CONVENTIONS.md` — conferma progetto Java (no Kotlin plugin), package `com.sed.tachimetro`, Java 11
- `.planning/codebase/STACK.md` — minSdk 30, targetSdk 36, AGP 9.1.1, dipendenze attuali (AppCompat 1.6.1, Material 1.10.0)

### Requisiti e progetto
- `.planning/PROJECT.md` — Key Decisions: Kotlin invece di Java, layout XML invece di Compose
- `.planning/REQUIREMENTS.md` — APP-01, PERM-01, PERM-02 sono i requisiti di questa fase

No external specs — requirements fully captured in decisions above.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- Nessun asset di codice riutilizzabile — il progetto è uno scaffold vuoto (nessuna Activity, nessun layout, nessuna classe Java/Kotlin in `app/src/main/java/com/sed/tachimetro/`)
- Risorse esistenti riutilizzabili: `app/src/main/res/values/themes.xml` (tema base `Theme.Tachimetro`), `app/src/main/res/values/strings.xml` (solo `app_name` definito)

### Established Patterns
- Nessun pattern stabilito nel codice — questa fase stabilisce il primo pattern (package structure, naming) per tutte le fasi successive
- Convenzione da seguire: nuove classi sotto `com.sed.tachimetro` o sotto-pacchetti (es. `com.sed.tachimetro.ui`)

### Integration Points
- `app/src/main/AndroidManifest.xml` — deve dichiarare la nuova Activity con intent-filter `LAUNCHER`/`MAIN` e il permesso `ACCESS_FINE_LOCATION`
- `app/build.gradle.kts` e `gradle/libs.versions.toml` — richiedono l'aggiunta del plugin Kotlin prima di scrivere qualunque file `.kt`

</code_context>

<specifics>
## Specific Ideas

Nessuna idea specifica aggiuntiva oltre a quanto già catturato in PROJECT.md e REQUIREMENTS.md — l'utente ha delegato i dettagli implementativi di questa fase a Claude.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 1-Fondamenta, Permessi e Avvio*
*Context gathered: 2026-07-07*
