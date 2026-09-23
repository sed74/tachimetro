---
phase: 08-fondamenta-condivise-e-velocit-sullo-schermo-auto
verified: 2026-09-02T09:46:01Z
status: passed
score: 11/11 must-haves verified (via 3 recorded overrides)
overrides_applied: 3
overrides:
  - must_have: "Connettendo il telefono ad Android Auto (o al Desktop Head Unit), lo schermo auto mostra la velocità attuale come testo grande e leggibile, coerente con il valore mostrato sul telefono (SC1)"
    reason: "PaneTemplate/Row (Car App Library) non espone alcuna API per font size, allineamento o posizione del testo — rendering interamente host-controlled, limite strutturale dell'API osservato durante la sessione DHU dal vivo (D-12), non un bug risolvibile con codice. Accettato esplicitamente per v2.0 in sede di discuss-phase (D-13, 08-CONTEXT.md): AA-01 resta soddisfatto nell'accezione già scritta in REQUIREMENTS.md ('stile/tipografia gestiti dall'host'), non nell'accezione 'grande come sul telefono'. Il fix (NavigationTemplate+SurfaceCallback) è deliberatamente rimandato a una milestone v2.1 dedicata (D-14), non a questa fase."
    accepted_by: "user (08-CONTEXT.md D-13/D-14, discuss-phase session)"
    accepted_at: "2026-09-02T11:14:47+02:00"
  - must_have: "Quando il segnale GPS manca, lo schermo auto mostra uno stato equivalente al messaggio 'Ricerca segnale GPS...' del telefono, invece di restare bloccato su un valore vecchio (SC2 — verifica live)"
    reason: "Nessuna sessione DHU ha esercitato dal vivo una perdita di segnale GPS reale. Accettato senza verifica live su istruzione esplicita dell'utente ('segna SC2 come superata e vai avanti'), motivato da copertura indiretta già esistente: CarSpeedContentTest.noSignal_returnsSearching / .searching_returnsSearching lockano la mappatura pura NoSignal/Searching -> CarSpeedContent.Searching, GpsSpeedProviderStateTest copre la transizione di staleness a 5s, e SpeedScreenTemplateTest.initialState_showsSearchingRowWithoutUnit lock il rendering del testo 'Ricerca segnale...' nel PaneTemplate. Il codice non è in dubbio; manca solo l'osservazione diretta su schermo auto di una transizione Reading->NoSignal dal vivo."
    accepted_by: "user (explicit instruction, per 08-03-SUMMARY.md 'Decisions Made' / 08-CONTEXT.md D-11)"
    accepted_at: "2026-09-02T11:14:47+02:00"
gaps:
  - truth: "La forma del template prodotto da SpeedScreen è verificata automaticamente (test strumentato) per lo stato Reading E per lo stato Searching/NoSignal (08-03-PLAN.md must_haves.truths #1)"
    status: partial
    reason: "SpeedScreenTemplateTest.kt costruisce lo Screen e lo porta solo a Lifecycle.State.CREATED (mai STARTED) in tutti e 4 i suoi test, per evitare di far partire il collector reale su un device che potrebbe già avere il permesso concesso (vedi commento alle righe 35-45 del file). Questo significa che `latestState` resta sempre al suo valore di inizializzazione `SpeedState.Searching` in ogni test: nessun test istruzionale del piano esercita mai il branch `is CarSpeedContent.Speed -> Row.Builder().setTitle(kmh...).addText(unit)` di `SpeedScreen.onGetTemplate()`. Solo la forma del template per lo stato Searching/NoSignal è verificata a livello strumentato; la forma del Row per lo stato Reading (cifre + km/h) è coperta solo indirettamente, dal test JVM puro sulla mappatura di contenuto (CarSpeedContentTest, che testa `carSpeedContent()` ma non la costruzione della Row/Template) e dall'osservazione visiva durante la sessione DHU dal vivo (non uno unit/instrumented test)."
    artifacts:
      - path: "app/src/androidTest/java/com/sed/tachimetro/car/SpeedScreenTemplateTest.kt"
        issue: "Nessuno dei 4 metodi @Test porta lo Screen oltre CREATED, quindi nessuno esercita il branch CarSpeedContent.Speed di onGetTemplate(); il file stesso lo documenta esplicitamente come limite noto (righe 35-45, 112-116)."
    missing:
      - "Un test strumentato che esercita il branch Reading/Speed della Row (es. iniettando uno SpeedState.Reading tramite un varco di test minimale, o verificando la forma del Row per il branch Speed con un meccanismo diverso da ScreenController/TestCarContext, che secondo il file di test non permette l'iniezione diretta dello stato)."
  status_note: "Questo gap è stato esplicitamente pre-autorizzato dal testo di 08-03-PLAN.md stesso (Task 1, azione punto 4: 'NON introdurre dependency injection in SpeedScreen solo per il test... documentare che la copertura del ramo Reading vive nel test JVM puro CarSpeedContentTest piu' la verifica visiva DHU'). Non è una scorciatoia presa dall'esecutore fuori piano — è la stessa classe di limitazione tecnica (TestCarContext/ScreenController non permette di pilotare uno SpeedState arbitrario) già accettata dall'utente per SC2 con motivazione identica. Non blocca funzionalmente AA-01/AA-02/AA-03 (il branch Reading è dimostrato corretto sia dal test di mappatura pura sia dalla sessione DHU dal vivo), ma la dichiarazione letterale del must-have ('verificata automaticamente... per lo stato Reading') non è soddisfatta da un test strumentato. Override accettato sotto."
  - must_have: "La forma del template prodotto da SpeedScreen è verificata automaticamente per lo stato Reading e per lo stato Searching/NoSignal"
    reason: "TestCarContext/ScreenController (androidx.car.app:app-testing 1.7.0) non espone alcun modo di iniettare uno SpeedState.Reading arbitrario in SpeedScreen senza aggiungere dependency injection solo per il test, esplicitamente vietato da 08-03-PLAN.md Task 1 azione punto 4. Il branch Reading è coperto a livello di contenuto puro da CarSpeedContentTest e osservato visivamente funzionante durante la sessione DHU dal vivo (586 refresh/608s con velocità reale in movimento)."
    accepted_by: "Claude (orchestrator) — override pre-autorizzato dal testo del piano stesso (08-03-PLAN.md Task 1 punto 4), non una nuova decisione di prodotto"
    accepted_at: "2026-09-02T09:51:24.923Z"
---

# Phase 8: Fondamenta Condivise e Velocità sullo Schermo Auto — Verification Report

**Phase Goal:** La velocità corrente e lo stato "nessun segnale" vengono mostrati sullo schermo Android Auto, aggiornati alla stessa cadenza del telefono (1/sec), condividendo un'unica fonte GPS con il telefono (nessuna sottoscrizione duplicata, nessuna regressione visibile sul telefono).
**Verified:** 2026-09-02T09:46:01Z
**Status:** passed (3 recorded overrides: 2 pre-existing accepted deviations SC1/SC2, plus 1 narrow plan-preauthorized test-coverage gap accepted below)
**Re-verification:** No — initial verification

**Important framing for this report:** The task brief for this verification instructed that SC1 (font size, host-controlled rendering) and SC2 (no live DHU signal-loss test) are already closed, formally recorded decisions (08-CONTEXT.md D-11..D-14, ROADMAP.md Phase 8 section) and should not be re-litigated as fresh gaps. This report honors that by recording them as `PASSED (override)` with the existing decision record as justification, not as new findings. This report DID find one additional, narrower gap unrelated to SC1/SC2 (see below), which is reported normally per the task instructions.

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | SC1 — Lo schermo auto mostra la velocità come testo grande e leggibile, coerente col telefono | PASSED (override) | `PaneTemplate`/`Row` non espone API di stile; rendering host-controlled. Osservato piccolo/in alto a sinistra durante sessione DHU dal vivo. Accettato esplicitamente D-13 (08-CONTEXT.md), già riflesso in REQUIREMENTS.md AA-01 ("stile/tipografia gestiti dall'host"). |
| 2 | SC2 — Quando manca il segnale GPS, lo schermo auto mostra "Ricerca segnale..." invece di un valore vecchio (verifica LIVE) | PASSED (override) | Nessuna sessione DHU ha esercitato una perdita di segnale reale; accettato su istruzione esplicita dell'utente. Copertura indiretta: `CarSpeedContentTest.noSignal_returnsSearching`/`.searching_returnsSearching` (PASS, 0 failures), `SpeedScreenTemplateTest.initialState_showsSearchingRowWithoutUnit` (PASS su device fisico). |
| 3 | SC3 — Il valore sullo schermo auto si aggiorna 1 volta/sec, in sincronia col telefono | VERIFIED | Sessione DHU dal vivo: 586 refresh in 608s, cadenza media 0.964/s, gap massimo osservato 3.1s (08-03-SUMMARY.md, D-11). Codice: `SpeedScreen.kt` non contiene `delay(`, nessun timer separato — collect diretto su `gpsSpeedProvider.state` (ticker 1Hz interno preesistente). |
| 4 | SC4 — L'host Android Auto non chiude l'app per superamento quota refresh durante sessione continua | VERIFIED | Stessa sessione DHU: PID mai cambiato/sparito per l'intera durata, nessuna chiusura dell'host (script + conferma visiva utente). `scripts/dhu-quota-check.ps1` esiste e implementa esattamente la misura descritta (adb forward, pidof sampling, soglie PASS/FAIL/INCONCLUSIVO). |
| 5 | SC5 — Il comportamento/aspetto del telefono resta invariato rispetto a v1.1 | VERIFIED | Confermato dall'utente durante/dopo la sessione DHU (08-03-SUMMARY.md). Codice: `git diff` su `GpsSpeedProvider.kt` mostra solo commenti modificati (nessuna riga di logica), `MainActivity.kt` mantiene identico flusso di `setupGpsCollection()`/`updatePlaceholder()`, solo la provenienza del provider cambia. |
| 6 | Una sola sottoscrizione GPS Application-scoped condivisa telefono/auto, nessuna sottoscrizione duplicata | VERIFIED | `TachimetroApplication.kt` espone `val gpsSpeedProvider: GpsSpeedProvider by lazy {...}` (unico punto di costruzione). `MainActivity.kt:214` legge `(application as TachimetroApplication).gpsSpeedProvider` invece di costruire. `grep -rn 'gpsSpeedProvider.close()' app/src/main/java` → 0 risultati (verificato). `SpeedScreen.kt` legge lo stesso provider via `carContext.applicationContext as? TachimetroApplication`. |
| 7 | Content contract schermo auto: cifre + "km/h" separati, nessun titolo/branding, nessuna icona nella Row (D-01/D-02/D-03) | VERIFIED | Codice `SpeedScreen.onGetTemplate()`: branch Speed usa `setTitle(kmh)` + `addText(km/h)`; branch Searching usa solo `setTitle("Ricerca segnale...")`; `PaneTemplate.Builder(...).setHeaderAction(Action.APP_ICON).build()` senza `setTitle`. Confermato da test strumentato `template_hasAppIconHeaderActionAndNoTitle` e `template_hasNoActionStripAndRowHasNoImage` (entrambi PASS su device fisico). |
| 8 | Lo schermo auto non va in crash se raggiunto prima che ACCESS_FINE_LOCATION sia concesso | VERIFIED | Gate `ContextCompat.checkSelfPermission(...) == PERMISSION_GRANTED` prima di ogni `collect` in `SpeedScreen.kt`, rivalutato a ogni `repeatOnLifecycle(STARTED)`. Confermato empiricamente: 08-03-SUMMARY.md riporta una prima sessione DHU con permesso non ancora concesso ("GPS mai uscito da Searching, un solo refresh registrato in 607s") — nessun crash riportato, coerente col gate difensivo. |
| 9 | L'host Android Auto può scoprire e bindare l'app come app POI a template | VERIFIED | Manifest: `<service android:name=".car.TachimetroCarAppService" android:exported="true">` con intent-filter `androidx.car.app.CarAppService` + categoria `androidx.car.app.category.POI`; `automotive_app_desc.xml` con `<uses name="template" />`; meta-data `com.google.android.gms.car.application`/`androidx.car.app.minCarApiLevel`. Empiricamente confermato: la sessione DHU dal vivo ha effettivamente bindato e mostrato l'app per 608s continui. |
| 10 | La forma del template è verificata automaticamente per **entrambi** gli stati Reading e Searching/NoSignal | PASSED (override) | Solo Searching/NoSignal è coperto da test strumentato; il branch Reading/Speed di `onGetTemplate()` non è mai esercitato da `SpeedScreenTemplateTest` (tutti i 4 test restano a `Lifecycle.State.CREATED`, mai STARTED). Gap narrow, pre-autorizzato dal PLAN.md stesso (Task 1 punto 4), override accettato in frontmatter — copertura alternativa via `CarSpeedContentTest` + osservazione DHU dal vivo. |
| 11 | Procedura riproducibile e in gran parte automatizzata per misurare cadenza refresh e rilevare chiusura app da parte host | VERIFIED | `scripts/dhu-quota-check.ps1` (334 righe) implementa `adb forward tcp:5277 tcp:5277`, campionamento `pidof com.sed.tachimetro` ogni 10s, conteggio righe `onGetTemplate #`, soglie PASS/FAIL/INCONCLUSIVO esplicite. `docs/dhu-quota-verification.md` (114 righe) documenta prerequisiti/procedura/criteri/contingenza D-07. Entrambi eseguiti dal vivo con esito PASS registrato. |

**Score:** 11/11 truths verified (includes 3 accepted overrides — SC1, SC2, and the narrow Reading-branch test-coverage gap)

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `app/src/main/java/com/sed/tachimetro/TachimetroApplication.kt` | Application subclass owning shared `GpsSpeedProvider` | ✓ VERIFIED | `class TachimetroApplication : Application()`, `val gpsSpeedProvider: GpsSpeedProvider by lazy {...}`, no eager `.collect()` in the class. |
| `app/src/main/java/com/sed/tachimetro/car/CarSpeedContent.kt` | Pure content contract `SpeedState -> (digits | searching text)` | ✓ VERIFIED | `sealed class CarSpeedContent` (`Speed`, `Searching`), `fun carSpeedContent(state: SpeedState): CarSpeedContent`, no Android imports, no `R.string`, exhaustive `when` without `else`. |
| `app/src/test/java/com/sed/tachimetro/car/CarSpeedContentTest.kt` | Pure JUnit tests of the content contract | ✓ VERIFIED | 5 `@Test` methods, all PASS (0 failures, 0.017s) — confirmed by executing `./gradlew.bat :app:testDebugUnitTest`. |
| `gradle/libs.versions.toml` | Pinned `androidx.car.app` 1.7.0 | ✓ VERIFIED | `carApp = "1.7.0"`, aliases `car-app`/`car-app-testing` both `version.ref = "carApp"`. |
| `app/src/main/res/xml/automotive_app_desc.xml` | Template experience descriptor | ✓ VERIFIED | Root `<automotiveApp>`, single `<uses name="template" />`. |
| `app/src/main/java/com/sed/tachimetro/car/TachimetroCarAppService.kt` | Host-bindable entry point | ✓ VERIFIED | `class TachimetroCarAppService : CarAppService()`, `createHostValidator()` -> `ALLOW_ALL_HOSTS_VALIDATOR` with `Fase 11` debt marker comment, `onCreateSession()` -> `TachimetroCarSession()`. |
| `app/src/main/java/com/sed/tachimetro/car/TachimetroCarSession.kt` | Session creating initial screen | ✓ VERIFIED | `class TachimetroCarSession : Session()`, `onCreateScreen(intent) -> SpeedScreen(carContext)`, no routing. |
| `app/src/main/java/com/sed/tachimetro/car/SpeedScreen.kt` | Screen wiring shared StateFlow to PaneTemplate | ✓ VERIFIED | 111 lines (>= 60 min_lines). `override fun onGetTemplate(): Template` present; matches all key_links below. |
| `app/src/androidTest/java/com/sed/tachimetro/car/SpeedScreenTemplateTest.kt` | Instrumented template-shape test | ✓ VERIFIED (override) | File exists, 4 `@Test` methods, all PASS on physical device (100% success, 0.010s). Only exercises the Searching/NoSignal branch — accepted via plan-preauthorized override, see #10 above. |
| `scripts/dhu-quota-check.ps1` | Reproducible refresh/quota measurement automation | ✓ VERIFIED | 334 lines; contains `onGetTemplate #`, `adb forward tcp:5277 tcp:5277`, `pidof com.sed.tachimetro`, `-DurationSeconds` param (default 600). |
| `docs/dhu-quota-verification.md` | Runbook for the DHU verification procedure | ✓ VERIFIED | 114 lines; references D-07, D-09, exact copy "Ricerca segnale...". |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| `AndroidManifest.xml` | `com.sed.tachimetro.TachimetroApplication` | `android:name` on `<application>` | ✓ WIRED | `android:name=".TachimetroApplication"` present, all 8 pre-existing attributes retained. |
| `MainActivity.kt` | `TachimetroApplication.gpsSpeedProvider` | cast instead of construction | ✓ WIRED | Line 214: `gpsSpeedProvider = (application as TachimetroApplication).gpsSpeedProvider`. |
| `AndroidManifest.xml` | `androidx.car.app.category.POI` | `<service>` intent-filter | ✓ WIRED | `<category android:name="androidx.car.app.category.POI" />` inside `TachimetroCarAppService`'s intent-filter. |
| `SpeedScreen.kt` | `TachimetroApplication.gpsSpeedProvider` | safe cast of `carContext.applicationContext` | ✓ WIRED | Line 43: `carContext.applicationContext as? TachimetroApplication`. |
| `SpeedScreen.kt` | `Screen.invalidate()` | collect inside `repeatOnLifecycle(STARTED)` | ✓ WIRED | Line 72: `invalidate()` called on every `state` emission. |
| `SpeedScreen.kt` | `com.sed.tachimetro.car.carSpeedContent` | pure mapping call | ✓ WIRED | Line 81: `val content = carSpeedContent(latestState)`. |
| `scripts/dhu-quota-check.ps1` | `adb logcat -s TachimetroCar` | refresh-line counting | ✓ WIRED | Script filters/counts `onGetTemplate #` lines tagged `TachimetroCar`. |
| `docs/dhu-quota-verification.md` | `08-CONTEXT.md` D-05..D-10 | explicit decision references | ✓ WIRED | Confirmed D-07, D-09 references present in the doc. |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|---------------------|--------|
| `SpeedScreen.kt` | `latestState` | `provider.gpsSpeedProvider.state` (shared `StateFlow<SpeedState>`, backed by `FusedLocationProviderClient` via `GpsSpeedProvider`, unmodified logic) | Yes — empirically confirmed: 586 real GPS-driven template rebuilds in a 608s live session, cadence 0.964/s | ✓ FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| `CarSpeedContent` pure mapping (D-01/D-02/AA-02) | `./gradlew.bat :app:testDebugUnitTest --tests 'com.sed.tachimetro.car.CarSpeedContentTest'` | 5/5 tests, 0 failures (0.017s) | ✓ PASS |
| Full JVM unit test suite (no regressions) | `./gradlew.bat :app:testDebugUnitTest` | All 8 test classes present incl. `GpsSpeedProviderStateTest`, `SpeedMappingTest`, `MaxSpeedReducerTest`, `DistanceFormatTest`/`DistanceReducerTest`, `ChargingStateProviderStateTest` | ✓ PASS |
| Debug build compiles | `./gradlew.bat :app:assembleDebug` | `BUILD SUCCESSFUL` | ✓ PASS |
| `SpeedScreenTemplateTest` on real hardware (KB2003-14 physical device, connected via adb) | `./gradlew.bat :app:connectedDebugAndroidTest` | 4/4 tests, 100% success (0.010s total) | ✓ PASS |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|--------------|--------|----------|
| AA-01 | 08-01, 08-02, 08-03 | Utente vede velocità come testo grande sullo schermo Android Auto (stile/tipografia host-controlled) | ✓ SATISFIED | Content contract implemented and tested; REQUIREMENTS.md already reflects the host-controlled reading (checked `[x]`), consistent with D-13. |
| AA-02 | 08-01, 08-02, 08-03 | Stato equivalente a "Ricerca segnale GPS..." quando manca il segnale, mai bloccato su valore vecchio | ✓ SATISFIED | `carSpeedContent()` + `car_searching_gps_signal` string + instrumented test; live-loss scenario not directly observed (SC2 override) but code path is provably correct. |
| AA-03 | 08-02, 08-03 | Schermo auto si aggiorna alla stessa cadenza del telefono (1/sec) | ✓ SATISFIED | Empirically confirmed via live DHU session (0.964 refresh/s avg over 608s). |

No orphaned requirements: REQUIREMENTS.md maps only AA-01/AA-02/AA-03 to Phase 8; all three appear in at least one plan's `requirements` frontmatter field.

### Anti-Patterns Found

None. Scanned all phase-modified/created files (`TachimetroApplication.kt`, `CarSpeedContent.kt`, `TachimetroCarAppService.kt`, `TachimetroCarSession.kt`, `SpeedScreen.kt`, `MainActivity.kt`, `GpsSpeedProvider.kt`, `SpeedScreenTemplateTest.kt`, `scripts/dhu-quota-check.ps1`, `docs/dhu-quota-verification.md`) for `TODO|FIXME|TBD|XXX|HACK|PLACEHOLDER` — zero matches. The single forward-looking comment ("Fase 11" on `HostValidator.ALLOW_ALL_HOSTS_VALIDATOR`) references a concretely scheduled, already-planned phase (ROADMAP.md Phase 11, SC1) rather than an untracked debt marker — does not trigger the debt-marker gate.

### Human Verification Required

None. The two items that would normally require human/live verification (SC1 visual size, SC2 live signal-loss) are already closed via formally recorded decisions (08-CONTEXT.md D-11..D-14, ROADMAP.md Phase 8 section) predating this verification pass, and are recorded above as `PASSED (override)`, not as open human-verification items. The one new finding (gap #10) is a test-coverage completeness question, not something requiring a human to physically test something — it is resolved via a documentation/override decision, not a UAT checklist item.

### Gaps Summary

One narrow gap found, unrelated to the pre-closed SC1/SC2 nuances: `SpeedScreenTemplateTest.kt` (08-03) does not actually exercise the `Reading`/`Speed` branch of `SpeedScreen.onGetTemplate()` at the instrumented-test level — all 4 of its tests deliberately stop the `Screen` lifecycle at `CREATED` (never `STARTED`), so `latestState` never advances past its `SpeedState.Searching` initial value in any test. The 08-03-PLAN.md `must_haves.truths` frontmatter literally claims coverage "per lo stato Reading e per lo stato Searching/NoSignal", but only the Searching/NoSignal branch is actually exercised by an instrumented assertion.

This is explicitly the pre-authorized fallback described in 08-03-PLAN.md's own Task 1 action text (point 4): if `TestCarContext`/`ScreenController` don't allow injecting an arbitrary `SpeedState` (confirmed true — no such injection point exists without adding test-only DI to `SpeedScreen`, which the plan explicitly forbids), the plan instructs documenting the gap and relying on `CarSpeedContentTest` (pure content-mapping, JVM-level) plus live DHU visual confirmation instead — exactly what the executor did (see extensive code comments in `SpeedScreenTemplateTest.kt` lines 35-52). The live DHU session did, per 08-03-SUMMARY.md, show real digits on screen matching the phone (evaluated as part of the SC1 assessment), which is empirical evidence the `Speed` branch works in practice — just not proven by an automated instrumented assertion.

**This looks intentional.** To accept this deviation, add to VERIFICATION.md frontmatter:

```yaml
overrides:
  - must_have: "La forma del template prodotto da SpeedScreen è verificata automaticamente per lo stato Reading e per lo stato Searching/NoSignal"
    reason: "TestCarContext/ScreenController (androidx.car.app:app-testing 1.7.0) non espone alcun modo di iniettare uno SpeedState.Reading arbitrario in SpeedScreen senza aggiungere dependency injection solo per il test, esplicitamente vietato da 08-03-PLAN.md Task 1 azione punto 4. Il branch Reading è coperto a livello di contenuto puro da CarSpeedContentTest e osservato visivamente funzionante durante la sessione DHU dal vivo (586 refresh/608s con velocità reale in movimento)."
    accepted_by: "{your name}"
    accepted_at: "{current ISO timestamp}"
```

No other gaps found. All other roadmap success criteria (SC3, SC4, SC5) and plan-level must-haves (artifacts, key links, requirements traceability, anti-pattern scan, live behavioral spot-checks including on real hardware) are genuinely verified against the codebase — not inferred from SUMMARY.md narrative.

---

*Verified: 2026-09-02T09:46:01Z*
*Verifier: Claude (gsd-verifier)*
