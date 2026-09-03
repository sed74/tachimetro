# Phase 11: Hardening di Produzione e Verifica su Dispositivo Reale - Context

**Gathered:** 2026-09-03
**Status:** Ready for planning

<domain>
## Phase Boundary

L'integrazione Android Auto viene messa in sicurezza e validata per l'uso reale, senza introdurre nuovi requisiti funzionali: sostituzione di `HostValidator.ALLOW_ALL_HOSTS_VALIDATOR` con un validatore reale che accetta solo host Android Auto legittimi (SC1), verifica su dispositivo fisico che la velocità sullo schermo auto continui ad aggiornarsi con il telefono bloccato/in background durante un tragitto reale (SC2), e verifica di robustezza a connessioni/disconnessioni rapide ripetute (SC3).

Requirements coperti: nessun nuovo requirement ID — verifica e messa in sicurezza di AA-01, AA-02, AA-03, AA-04, CONN-01, CONN-02 (v. REQUIREMENTS.md).

Fuori scope per questa fase (già deciso a livello di milestone/roadmap, non riaperto qui): passaggio a `NavigationTemplate`+`SurfaceCallback` (rimandato esplicitamente a milestone v2.1, D-14 in `08-CONTEXT.md`), velocità massima/distanza sullo schermo auto (out of scope di milestone), qualunque nuova capability non elencata nei 3 Success Criteria di roadmap.

Punto di partenza noto: `TachimetroCarAppService.createHostValidator()` restituisce oggi `HostValidator.ALLOW_ALL_HOSTS_VALIDATOR` con un commento che marca esplicitamente il debito verso questa fase (`app/src/main/java/com/sed/tachimetro/car/TachimetroCarAppService.kt:19`). Il comportamento GPS in background a telefono bloccato non è mai stato testato empiricamente (gap di piattaforma non documentato da Google, `.planning/research/PITFALLS.md` Pitfall 6) — è il rischio più incerto della fase.

</domain>

<decisions>
## Implementation Decisions

### HostValidator reale (SC1)
- **D-01:** Split debug/release: i build di **debug** (incluso lo sviluppo/test su DHU) restano su `HostValidator.ALLOW_ALL_HOSTS_VALIDATOR` per comodità di sviluppo; solo i build di **release** usano l'allow-list reale. Motivazione dell'utente: comodità di sviluppo prevale sulla dev/prod parity totale in questo caso — la differenza va documentata esplicitamente (commento inline + eventualmente `docs/`), non lasciata implicita.
- **D-02:** Il meccanismo esatto per distinguere debug/release (`BuildConfig.DEBUG`, build type dedicato, o altro) e l'allow-list reale specifica da usare (host/signature ufficiali Google Android Auto) sono lasciati alla ricerca/pianificazione — nessuna preferenza dell'utente su questo dettaglio tecnico.

### Fallback se il GPS in background non funziona (SC2)
- **D-03:** Se il test empirico su dispositivo reale rivela che gli aggiornamenti GPS si fermano quando il telefono è bloccato, la fase si chiude **documentando il limite di piattaforma come noto e accettato**, SENZA aggiungere `ACCESS_BACKGROUND_LOCATION`. Motivazione esplicita dell'utente: coerenza con la filosofia di minimizzazione dei permessi già seguita dal progetto (Phase 2 threat model T-02-EP), evitare l'onere aggiuntivo di disclosure data-safety su Play Store. SC2 stesso ammette questo esito ("o il limite di piattaforma viene documentato esplicitamente se non risolvibile") — non è un fallimento della fase, è un esito valido.
- **D-04:** Questa decisione preclude esplicitamente l'opzione "aggiungere `ACCESS_BACKGROUND_LOCATION`" come piano di contingenza per questa fase — se in futuro (milestone successiva) il gap si rivelasse inaccettabile per l'uso reale, andrebbe riaperta come decisione a sé, non anticipata qui.

### Robustezza a connessioni/disconnessioni rapide (SC3)
- **D-05:** Approccio "testa prima, patcha solo se si rompe davvero": si esegue il test di connessione/disconnessione rapida ripetuta di Android Auto e si aggiungono guardie difensive (debounce, guard su race condition) SOLO se il test rivela concretamente un crash o uno stato incoerente sullo schermo auto. Nessun hardening preventivo scritto per uno scenario non ancora osservato. Motivazione: coerente con CLAUDE.md ("non aggiungere error handling per scenari che non possono accadere", "non progettare per requisiti futuri ipotetici").

### Test su strada reale (SC2)
- **D-06:** Durata target del test: **5-10 minuti**, stessa soglia già validata empiricamente in Fase 8 per il test di quota refresh (D-08 in `08-CONTEXT.md`), riusata qui per coerenza — ma con un setup diverso: telefono fisicamente bloccato/schermo spento durante un tragitto reale (non solo sessione DHU con schermo acceso). Motivazione dell'utente: riusare una soglia già validata piuttosto che introdurne una nuova senza precedenti nel progetto.
- **D-07:** Il test resta un checkpoint umano bloccante (pattern già stabilito in Fasi 8/9/10): nessun test automatico può osservare il comportamento reale del sistema operativo a schermo bloccato durante una guida reale.

### Claude's Discretion
- Meccanismo tecnico esatto per il debug/release split del `HostValidator` (D-02) — quale flag/build-type e quale risorsa di allow-list usare.
- Contenuto esatto dell'allow-list reale (quali host/signature Android Auto legittimi includere) — ricerca tecnica, non una preferenza di prodotto.
- Meccanismo tecnico per rilevare/prevenire race condition di connessione/disconnessione rapida, SE il test D-05 rivela un problema reale — l'approccio ("solo se rotto") è deciso, l'implementazione del fix (se necessario) no.
- Formato esatto della documentazione del limite di piattaforma (D-03), se il test SC2 fallisce — dove registrarlo (STATE.md? ROADMAP.md? un file dedicato?), coerente con il pattern già usato per SC1/SC2 di Fase 8 e Scenario G di Fase 9.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Ricerca di progetto (v2.0 Android Auto)
- `.planning/research/PITFALLS.md` §Pitfall 6 — "Assuming location updates keep flowing to the car screen when the phone is locked/backgrounded" — riferimento primario per SC2, descrive il gap di documentazione Google e le tre opzioni di fallback (background permission / screen-on / foreground exemption non verificata)
- `.planning/research/PITFALLS.md` §"Looks Done But Isn't" Checklist — punto su "Speed updates keep flowing when the phone is locked/backgrounded" e §"Milestone Gate Checklist" — riga "Background location staleness (Pitfall 6)"
- `.planning/research/SUMMARY.md` — contesto generale della ricerca v2.0
- `.planning/research/STACK.md` — eventuali riferimenti a `HostValidator`/allow-list ufficiale della Car App Library

### Roadmap e requisiti
- `.planning/ROADMAP.md` §"Phase 11: Hardening di Produzione e Verifica su Dispositivo Reale" — goal, 3 success criteria, dipendenze (Fase 9, Fase 10)
- `.planning/REQUIREMENTS.md` — nessun nuovo requirement ID; nota di traceability in fondo al file conferma che Fase 11 valida AA-01..AA-04/CONN-01/CONN-02 sotto condizioni di produzione

### Contesto Fasi precedenti (debito esplicitamente marcato per questa fase)
- `app/src/main/java/com/sed/tachimetro/car/TachimetroCarAppService.kt:16-19` — `createHostValidator()` con `ALLOW_ALL_HOSTS_VALIDATOR` e commento inline che marca il debito verso "Fase 11" (introdotto in Fase 8, `08-02-PLAN.md` Task 2)
- `.planning/phases/08-fondamenta-condivise-e-velocit-sullo-schermo-auto/08-CONTEXT.md` — T-08-05 (disposition "accept" del validatore permissivo, con riferimento esplicito a questa fase come rimedio), D-08 (soglia 5-10 minuti riusata in D-06 qui)
- `.planning/phases/09-permesso-di-localizzazione-dallo-schermo-auto/09-CONTEXT.md` — T-09-10 (rischio ereditato invariato, stesso riferimento a Fase 11)
- `playstore/README.md` — nota di rischio interna sull'`ALLOW_ALL_HOSTS_VALIDATOR`, da rimuovere/aggiornare una volta chiusa questa fase (fuori scope diretto di questa fase, ma downstream deve saperlo)

No external specs (nessun SPEC.md per questa fase) — requisiti pienamente catturati in ROADMAP.md/REQUIREMENTS.md e nelle decisioni sopra.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `TachimetroCarAppService.kt` (`app/src/main/java/com/sed/tachimetro/car/`): unico punto che istanzia `HostValidator` — `createHostValidator()` è l'unico metodo da modificare per SC1, nessuna propagazione altrove nel codebase (verificato: nessun altro riferimento a `HostValidator` nel progetto).
- `SpeedScreen.kt`: già osserva il lifecycle (`repeatOnLifecycle(Lifecycle.State.STARTED)`) e il gate difensivo sul permesso (T-08-08) — punto naturale dove eventuali sintomi di SC2 (aggiornamenti che si fermano) o SC3 (stato incoerente) si manifesterebbero.
- `docs/dhu-quota-verification.md` + `scripts/dhu-quota-check.ps1` (Fase 8): runbook e script di misura riproducibile già esistenti, riusabili come base/pattern per un eventuale script di verifica SC2 (misura su dispositivo reale invece che DHU) e per il logging diagnostico (tag `TachimetroCar`, formato `onGetTemplate #<n>`).
- `BuildConfig.DEBUG` (abilitato in Fase 8, `buildFeatures { buildConfig = true }`): già usato per gating del logging diagnostico — stesso meccanismo candidato naturale per il D-01/D-02 debug/release split del HostValidator.

### Established Patterns
- Pattern "verifica empirica con gate esplicito prima di considerare il rischio chiuso" (D-06/D-08 Fase 8, D-08/D-09 Fase 9) — stesso pattern da applicare qui per SC1 (verifica che DHU/head unit reale continuino a funzionare col nuovo validator) e SC2 (test su strada).
- Pattern "checkpoint umano bloccante per comportamenti non osservabili da test automatico" — già usato in ogni fase Android Auto precedente (08-03, 09-03, 10-03), da riapplicare per SC2/SC3.
- Pattern "limite di piattaforma documentato ed esplicitamente accettato invece di forzato" (SC1/SC2 di Fase 8, Scenario G/Pitfall 1 di Fase 9) — esattamente il pattern scelto per D-03/D-04 qui se SC2 fallisce.

### Integration Points
- Nessuna modifica prevista a `SpeedScreen.kt`, `TachimetroCarSession.kt`, `CarSpeedContent.kt`, `CarPermissionState.kt`, `CarPermissionDenialStore.kt` per SC1 (isolato a `TachimetroCarAppService.createHostValidator()`) — a meno che il test SC3 non riveli un problema reale in questi file (D-05).
- `AndroidManifest.xml`: nessuna nuova dichiarazione prevista per D-03 (nessun `ACCESS_BACKGROUND_LOCATION`); eventuale modifica solo se una futura milestone riaprisse D-04.

</code_context>

<specifics>
## Specific Ideas

- Il validatore reale deve continuare a far funzionare DHU/head unit reale in fase di sviluppo (D-01: DHU resta sotto il validatore permissivo di debug, non sotto quello reale — quindi non è un vincolo diretto su SC1, ma va verificato che il validator reale funzioni comunque con un head unit VERO in un build di release/staging).
- Nessuna richiesta di personalizzazione dell'allow-list oltre a "host Android Auto legittimi" — l'utente non ha espresso preferenze su OEM specifici o su un elenco custom.
- Se il gap di background location si conferma, la soluzione preferita dall'utente è la documentazione esplicita, non un permesso aggiuntivo — non riaprire questa decisione senza un nuovo giro di conversazione esplicito.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 11-Hardening di Produzione e Verifica su Dispositivo Reale*
*Context gathered: 2026-09-03*
