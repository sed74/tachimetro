# Phase 12: Spike Surface e Decisione Categoria/Template - Context

**Gathered:** 2026-09-23
**Status:** Ready for planning

<domain>
## Phase Boundary

Spike su DHU (telefono fisico OnePlus 8T) dei due percorsi che danno accesso alla Surface di Android Auto — **POI + `MapWithContentTemplate`** e **NAVIGATION + `NavigationTemplate`** — con un disegno minimale di prova, misura concreta di stable area / visible area / Car API level, decisione esplicita dell'utente su categoria e template (registrata in PROJECT.md Key Decisions), e manifest finale con `minCarApiLevel` 7, solo i permessi Car App del percorso scelto e nessun ramo di fallback `PaneTemplate`. A fine fase l'app si apre ancora regolarmente su DHU.

NON in questa fase: velocità GPS reale sulla Surface, tipografia definitiva, tema giorno/notte, "Ricerca segnale..." disegnato, adattamento del flusso permesso al nuovo rendering (tutto Fase 13); pulizie telefono (Fase 14); caricamenti Play Store (Fase 15).

Requisiti: REL-01, REL-02.

</domain>

<decisions>
## Implementation Decisions

### Struttura del codice dello spike
- **D-01:** I due percorsi convivono come **due product flavor Gradle temporanei** (es. `spikePoi` / `spikeNav`), ciascuno con il proprio manifest (categoria + permessi `ACCESS_SURFACE` + `MAP_TEMPLATES` oppure `NAVIGATION_TEMPLATES`) e il proprio builder di template. Si installano alternativamente su DHU; il confronto deve essere ripetibile con un comando. Nessun artefatto dichiara mai `MAP_TEMPLATES` e `NAVIGATION_TEMPLATES` insieme.
- **D-02:** Dopo la decisione i flavor vengono **rimossi**: resta nel codice solo il percorso scelto (renderer minimale + `SurfaceCallback` + builder del template), come scheletro per la Fase 13. Il codice dell'altro percorso viene eliminato. La fase chiude con un'unica configurazione di build e l'app funzionante su DHU (SC4).
- **D-03:** Disegno di prova sulla Surface: **sfondo pieno + "888" fisso centrato nella stable area + contorni della stable area e della visible area in due colori distinti**, così che a colpo d'occhio si veda cosa l'host sovrappone. Nessun collegamento a `GpsSpeedProvider` nello spike.
- **D-04:** Flusso permesso della Fase 9 **invariato**: la macchina a stati di `SpeedScreen` (`CarPermissionState`, `requestLocationPermission()`, azioni riprova/impostazioni) resta com'è; solo il ramo `Granted` passa al template con Surface. Gli altri stati continuano a usare i template attuali. L'adattamento completo del flusso permesso al nuovo rendering è AA-12 in Fase 13.
- **D-05:** Le build dello spike **non vanno mai sul Play Store** (nessun canale, nemmeno test interno): solo APK debug locali su DHU.

### Card POI e action strip
- **D-06:** Nel percorso POI si provano **più varianti del template di contenuto obbligatorio** di `MapWithContentTemplate` — almeno `MessageTemplate` e `ListTemplate` con contenuto minimo (eventualmente anche `PaneTemplate`/`GridTemplate` se economico) — e si registra quale lascia la stable area più grande. La decisione si prende sul caso migliore di POI.
- **D-07:** Il testo della card POI nello spike è un **segnaposto minimo** (es. "Tachimetro"). Il contenuto definitivo della card si decide in Fase 13 solo se vince POI. La card non deve mai ripetere la velocità (niente numeri in più sullo schermo auto — Out of Scope).
- **D-08:** Action strip: vedi Claude's Discretion. Per ogni percorso si registra ingombro dell'action strip e se/quando si nasconde (atteso ~10s su `NavigationTemplate`).

### Protocollo di misura
- **D-09:** Risoluzioni DHU: **800×480, 1280×720, 1920×1080**, per entrambi i percorsi (e per ogni variante di card POI). Sono le stesse di SC1 della Fase 13: le misure diventano casi di test per le funzioni pure di layout della Fase 13.
- **D-10:** Registrazione: **log DEBUG con tag dedicato** (solo `BuildConfig.DEBUG`) a ogni callback (`onSurfaceAvailable`, `onStableAreaChanged`, `onVisibleAreaChanged`) con dimensione Surface, dpi, stableArea, visibleArea e `carContext.carAppApiLevel`; raccolta tramite **script PowerShell sul modello di `scripts/dhu-quota-check.ps1`**, più **uno screenshot DHU per variante/risoluzione**. Tutto confluisce in `12-SPIKE-RESULTS.md` nella cartella della fase.
- **D-11:** Car API level dell'head unit reale: **opzionale**. Se l'utente vuole, sideload dell'APK debug in auto (Android Auto → impostazioni sviluppatore → origini sconosciute). Perché sia leggibile senza logcat (USB occupata), il disegno di prova mostra anche il `carAppApiLevel` come piccolo testo sulla Surface (solo build di spike). Se non si fa, SC2 è soddisfatto dal solo DHU; il rischio (head unit con Car API < 7 → app non visibile) viene annotato nei risultati.

### Criteri di decisione
- **D-12:** Criterio fissato ora: **POI vince se l'altezza delle cifre "888" ottenibile in POI (caso migliore di D-06) è ≥ 70% di quella ottenibile in NAVIGATION alla stessa risoluzione**, verificato su tutte e tre le risoluzioni. Il 70% è un valore di partenza che l'utente conferma o corregge guardando i numeri; la decisione finale resta sua (gate umano esplicito). Se vince POI, la spec D-14 (v2.0) viene aggiornata per convivere con la card host-obbligatoria.
- **D-13:** Se vince NAVIGATION: si **tenta**, ma la build passa **prima solo dal canale di test chiuso** (non bloccante) e si legge l'esito della revisione Android Auto; con esito negativo si rientra su POI senza mai toccare il canale aperto. La Fase 15 deve prevedere questo piano di rientro. Il rischio NF-1 (turn-by-turn obbligatorio) e NF-6 va citato esplicitamente nella motivazione.
- **D-14:** Registrazione: `12-SPIKE-RESULTS.md` (misure, screenshot, calcolo del criterio D-12, motivazione) + riga in **PROJECT.md Key Decisions** che rimanda al documento (aggiornando anche la riga "Passaggio a `NavigationTemplate`+`SurfaceCallback` rimandato a v2.1" da Pending a esito). Se vince POI si aggiornano anche i testi di AA-05/AA-08 in REQUIREMENTS.md e la nota D-14 per riflettere la card.

### Già decisi prima della fase (non rinegoziare)
- `minCarApiLevel` = 7, nessun fallback `PaneTemplate` né check runtime del livello (REL-02 — scelta utente che chiude il gap #2 di research/SUMMARY.md).
- Verifiche su head unit reale accettate come PASS visivi senza logcat (Fase 11).

### Claude's Discretion
- Azione dell'action strip nello spike: la più discreta che `build()` accetta su ciascun template (es. singola icona senza titolo); nessuna funzione reale richiesta.
- Nomi dei flavor, del tag di log, della classe renderer e struttura interna del codice di spike (coerente con le convenzioni del package `car/` e con ARCHITECTURE.md della ricerca: un unico file che importa `android.graphics.*`).
- Meccanismo di cambio variante della card POI (costante debug, flavor dimension aggiuntiva, ecc.).
- Uso di `lockCanvas()` vs `lockHardwareCanvas()` nel renderer minimale.
- Come cambiare risoluzione DHU (file `.ini`) e struttura dello script di raccolta.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Ricerca v2.1
- `.planning/research/SUMMARY.md` — sintesi; §"Gaps to Address" #1 (POI vs NAVIGATION) e #2 (minCarApiLevel, risolto da REL-02 a favore di 7)
- `.planning/research/STACK.md` — permessi manifest, `MapWithContentTemplate` `@RequiresCarApi(7)`, Canvas/Typeface
- `.planning/research/ARCHITECTURE.md` — `SpeedSurfaceRenderer` passivo, funzioni pure, punto unico `buildSurfaceTemplate()`
- `.planning/research/FEATURES.md` — obblighi action strip / card, comportamento visible vs stable area
- `.planning/research/PITFALLS.md` — revisione bloccante canale aperto, card obbligatoria, lifecycle Surface, Car API level

### Requisiti e roadmap
- `.planning/REQUIREMENTS.md` — REL-01, REL-02 (e AA-05..AA-12 per capire cosa servirà alla Fase 13)
- `.planning/ROADMAP.md` §"Phase 12" — success criteria SC1-SC4
- `.planning/PROJECT.md` §"Key Decisions" — righe PaneTemplate v2.0 / rinvio NavigationTemplate da aggiornare

### Visual spec e storia
- `.planning/milestones/v2.0-phases/08-fondamenta-condivise-e-velocit-sullo-schermo-auto/08-CONTEXT.md` — D-12..D-14 e `<specifics>` (visual spec: numero grande centrato, km/h in basso a destra, nessuna icona)

### Tooling DHU esistente
- `docs/dhu-quota-verification.md` — runbook sessione DHU su telefono fisico (modello per il runbook di spike)
- `scripts/dhu-quota-check.ps1` — modello per lo script di raccolta misure
- `docs/android-auto-hardening-verification.md` — verifiche su head unit reale senza logcat

### Documentazione esterna
- https://developer.android.com/training/cars/apps/library/draw-maps — template/permessi, visible/stable area
- https://developer.android.com/training/cars/apps/poi — `MapWithContentTemplate` in POI
- https://developer.android.com/docs/quality-guidelines/car-app-quality — NF-1, NF-6
- https://developer.android.com/training/cars/testing/dhu — configurazione risoluzioni DHU

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `car/SpeedScreen.kt` (316 righe): macchina a stati del permesso (`permissionState`, `requestLocationPermission()`, `onRetryOrSettingsClicked()`, `openAppSettingsFromCar()`) da preservare intatta; `onGetTemplate()` è il punto in cui il ramo `Granted` passa dal `PaneTemplate` al template con Surface.
- `car/TachimetroCarSession.kt` (15 righe): punto naturale in cui creare/possedere il renderer e registrare `AppManager.setSurfaceCallback`.
- `car/CarHostValidation.kt`: `ALLOW_ALL` solo in debug — le build di spike su DHU funzionano già.
- `BuildConfig.DEBUG` già usato in `SpeedScreen` per log diagnostici (convenzione T-08-07) — riusabile per il log di misura.

### Established Patterns
- Commenti con tag decisionali (D-xx, AA-xx, WR-xx) e KDoc in italiano.
- Funzioni pure testabili in JVM per la logica (utile se lo spike estrae già il calcolo dell'altezza cifre per il criterio D-12).
- Script PowerShell + runbook markdown per le sessioni DHU (Fase 8, Fase 11).

### Integration Points
- `app/src/main/AndroidManifest.xml`: `minCarApiLevel` 1 → 7; categoria `androidx.car.app.category.POI` nel service; aggiunta `ACCESS_SURFACE` + permesso template per flavor (manifest per flavor in `src/spikePoi/`, `src/spikeNav/`).
- `app/src/main/res/xml/automotive_app_desc.xml`: `<uses name="template" />` — verificare se il percorso scelto richiede modifiche.
- `app/build.gradle.kts`: oggi solo `buildTypes`, nessun flavor — i flavor temporanei vanno aggiunti e poi rimossi.
- Test esistenti su `SpeedScreen`/template (es. `SpeedScreenTemplateTest`) da mantenere verdi o adattare al ramo Granted.

</code_context>

<specifics>
## Specific Ideas

- Contorni di stable area e visible area disegnati sulla Surface in due colori distinti: l'utente deve *vedere* cosa copre l'host, non solo leggerlo nei log.
- Confronto finale affiancato POI vs NAV per ogni risoluzione, con il rapporto delle altezze cifre calcolato (criterio D-12).
- `carAppApiLevel` scritto piccolo sulla Surface di spike per poterlo leggere in auto senza USB.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope. (Restano validi i Future Requirements già in REQUIREMENTS.md, incluso il flavor NAVIGATION solo per uso interno se vince POI.)

</deferred>

---

*Phase: 12-spike-surface-e-decisione-categoria-template*
*Context gathered: 2026-09-23*
