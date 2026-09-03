---
phase: 11-hardening-di-produzione-e-verifica-su-dispositivo-reale
plan: 01
subsystem: security
tags: [android-auto, car-app-library, hostvalidator, buildconfig, instrumented-test]

# Dependency graph
requires:
  - phase: 08-fondamenta-condivise-e-velocit-sullo-schermo-auto
    provides: TachimetroCarAppService con HostValidator.ALLOW_ALL_HOSTS_VALIDATOR e il marcatore di debito "Fase 11"
  - phase: 09-permesso-di-localizzazione-dallo-schermo-auto
    provides: pattern del seam pubblico per testabilita' (SpeedScreen.buildTemplate) e disposizione accept T-09-10
  - phase: 10-comportamento-del-telefono-alla-connessione-android-auto
    provides: CarLinkState.kt, analogo canonico della allow-list fail-safe come funzione pura top-level
provides:
  - "createCarHostValidator(context, allowAllHosts) — seam puro che produce il HostValidator del car service"
  - "Allow-list reale nei build di release, limitata ai due host ufficiali Android Auto / Automotive Templates"
  - "Validatore permissivo mantenuto nei soli build di debug (D-01), per non rompere il flusso DHU"
  - "CarHostValidationTest — 6 test strumentati che lockano entrambi i rami e il contenuto dell'allow-list"
affects: [11-02, 11-03, playstore-release, android-auto]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Seam a parametri iniettati per rendere testabile un componente non istanziabile in test (CarAppService non ha controller in app-testing)"
    - "Allow-list fail-safe: un solo ramo produce l'esito fiducioso, guardato da un flag esplicito, mai da un catch"
    - "@SuppressLint stretto al call site con motivazione scritta (disciplina di @Suppress(\"MissingPermission\"))"

key-files:
  created:
    - app/src/main/java/com/sed/tachimetro/car/CarHostValidation.kt
    - app/src/androidTest/java/com/sed/tachimetro/car/CarHostValidationTest.kt
  modified:
    - app/src/main/java/com/sed/tachimetro/car/TachimetroCarAppService.kt

key-decisions:
  - "Flag di build: com.sed.tachimetro.BuildConfig.DEBUG letto al call site, nessun nuovo build type ne' product flavor (app/build.gradle.kts non toccato)"
  - "Fonte dell'allow-list: risorsa della libreria androidx.car.app.R.array.hosts_allowlist_sample, non una copia locale dei 6 digest SHA-256 (evita trascrizione a mano e segue le rotazioni delle chiavi Google)"
  - "Nessun try/catch attorno alla costruzione del validator: un'allow-list malformata deve crashare rumorosamente, mai degradare a ALLOW_ALL (T-11-04)"
  - "Test strumentato e non JVM: HostValidator.Builder legge context.getResources().getStringArray()"

patterns-established:
  - "Gate di trust boundary come funzione top-level a parametri espliciti, con il flag di build valorizzato dal chiamante"
  - "Asserzioni sull'allow-list per struttura e package, mai sui digest letterali (resistono alle rotazioni delle chiavi di firma)"

requirements-completed: [AA-01, AA-02, AA-03, AA-04]

# Metrics
duration: 6min
completed: 2026-09-03
---

# Phase 11 Plan 01: HostValidator reale con split debug/release Summary

**Il car service ora rifiuta il binding da host non legittimi nei build di release, usando l'allow-list ufficiale `androidx.car.app.R.array.hosts_allowlist_sample`, e resta permissivo solo in debug (D-01) — chiuse le due disposizioni "accept" ereditate T-08-05 e T-09-10.**

## Performance

- **Duration:** 6 min
- **Started:** 2026-09-03T08:41:36Z
- **Completed:** 2026-09-03T08:47:26Z
- **Tasks:** 3
- **Files modified:** 3 (2 creati, 1 modificato)

## Accomplishments

- `createCarHostValidator(context, allowAllHosts)` estratto come seam puro: unico punto in cui l'app decide chi puo' bindare la sua superficie auto via Binder IPC.
- Ramo release: allow-list reale limitata a `com.google.android.projection.gearhead` e `com.google.android.apps.automotive.templates.host`, con i tre digest di firma Google ciascuno.
- Ramo debug: `ALLOW_ALL_HOSTS_VALIDATOR` mantenuto di proposito e documentato (D-01), il DHU di sviluppo continua a funzionare.
- Marcatore di debito greppabile "scope esplicito della Fase 11" sostituito da documentazione dello split che nomina D-01, T-08-05, T-09-10.
- 6 test strumentati verdi su dispositivo fisico (KB2003, Android 14) che lockano entrambi i rami, la non-vuotezza dell'allow-list e l'inversione `<package>,<digest>`.

## Task Commits

Each task was committed atomically:

1. **Task 1: Creare il seam createCarHostValidator in CarHostValidation.kt** — `3792bfd` (feat)
2. **Task 2: Collegare TachimetroCarAppService al seam e ritirare il marcatore di debito** — `55a8f77` (feat)
3. **Task 3: Test strumentato CarHostValidationTest su entrambi i rami** — `66c9cc0` (test)

_Nota sul ciclo TDD del Task 3: il piano colloca il test dopo il seam (Task 1/2 lo costruiscono), quindi non esiste una fase RED separata — `CarHostValidationTest` e' un lock di caratterizzazione, non un test guida. Vedi "TDD Gate Compliance"._

## Files Created/Modified

- `app/src/main/java/com/sed/tachimetro/car/CarHostValidation.kt` (nuovo) — seam `createCarHostValidator(context, allowAllHosts)`; ramo permissivo raggiungibile SOLO via `allowAllHosts == true`, ramo release costruito da `HostValidator.Builder(context).addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)`. `@SuppressLint("PrivateResource")` stretto con motivazione scritta.
- `app/src/main/java/com/sed/tachimetro/car/TachimetroCarAppService.kt` (modificato) — `createHostValidator()` delega a `createCarHostValidator(applicationContext, BuildConfig.DEBUG)`; commento di debito sostituito da documentazione dello split.
- `app/src/androidTest/java/com/sed/tachimetro/car/CarHostValidationTest.kt` (nuovo) — 6 test strumentati: `debugBranch_returnsAllowAllValidator`, `releaseBranch_isNotTheAllowAllValidator`, `releaseBranch_allowListIsNotEmpty`, `releaseBranch_allowListContainsAndroidAutoHost`, `releaseBranch_allowListContainsAutomotiveTemplatesHost`, `releaseBranch_digestsAreNotPackageNames`.

## Decisions Made

Nessuna decisione nuova: il `<decision_record>` del piano fissava gia' `BuildConfig.DEBUG` come meccanismo e la risorsa della libreria come fonte dell'allow-list (D-02), ed entrambe sono state applicate senza rinegoziazione. `app/build.gradle.kts` e `app/src/main/res/` non sono stati toccati, come richiesto (verificato con `git status --short`).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] `local.properties` mancante nel worktree**
- **Found during:** Task 1 (prima esecuzione di `:app:compileDebugKotlin`)
- **Issue:** `local.properties` non e' sotto controllo di versione, quindi il worktree parallelo non lo eredita: Gradle falliva con "SDK location not found" e nessun task del piano poteva essere verificato.
- **Fix:** Ricreato `local.properties` nel worktree con lo stesso `sdk.dir=D:\Android\SDK` del checkout principale.
- **Files modified:** `local.properties` (gitignored, NON committato — non entra nel repo)
- **Verification:** `:app:compileDebugKotlin`, `:app:assembleRelease`, `:app:assembleDebugAndroidTest`, `:app:connectedDebugAndroidTest` e `:app:test` eseguiti tutti con esito 0.
- **Committed in:** nessun commit (file ignorato da git per design)

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Nessuno scope creep. Il fix riguarda solo l'ambiente di build del worktree, non il codice dell'app; nessun file versionato ne' risulta modificato.

## Issues Encountered

- Nessuno. Il check lint `PrivateResource` temuto in `11-PATTERNS.md` (fatto verificato #4) non ha bloccato: `:app:lintVitalRelease` e' passato con la soppressione stretta gia' in posizione.

## Verification Results

| # | Verifica | Esito |
|---|----------|-------|
| 1 | `./gradlew.bat :app:assembleRelease` | PASS (exit 0, APK non firmato — atteso senza `keystore.properties`) |
| 2 | `ALLOW_ALL_HOSTS_VALIDATOR` non commentato in `app/src/main/java/` | PASS — 1 sola riga, `CarHostValidation.kt:67`, dentro `if (allowAllHosts)` |
| 3 | `:app:connectedDebugAndroidTest` filtrato su `CarHostValidationTest` | PASS — 6 test avviati, 6 completati, 0 falliti su KB2003 (Android 14) |
| 4 | `./gradlew.bat :app:test` | PASS (nessuna regressione JVM) |
| 5 | `git status --short app/build.gradle.kts app/src/main/res/` | PASS (output vuoto) |

Gate aggiuntivi dei task: `NO_FALLBACK_OK` (nessun `try`/`catch` nel seam) e `SINGLE_PERMISSIVE_SITE_OK` (una sola occorrenza non commentata nel package `car/`) entrambi soddisfatti.

## TDD Gate Compliance

Il Task 3 e' marcato `tdd="true"` ma il piano lo colloca dopo i Task 1 e 2, che costruiscono l'implementazione. Non esiste quindi un commit RED (`test(...)`) precedente al GREEN (`feat(...)`): la sequenza effettiva e' `feat` → `feat` → `test`. E' la sequenza voluta dal piano — `CarHostValidationTest` e' un lock di caratterizzazione dei due rami (un test scritto prima avrebbe potuto solo fallire per assenza del simbolo `createCarHostValidator`, non per comportamento). Nessun test e' stato cancellato o marcato `@Ignore`.

## Known Stubs

Nessuno. Entrambi i rami del validator sono funzionanti; nessun valore hardcoded, placeholder o TODO introdotto.

## Threat Flags

Nessuna nuova superficie di sicurezza introdotta: il piano riduce la superficie esistente (il confine Binder IPC gia' mappato in `<threat_model>`) invece di aggiungerne. Nessuna nuova dipendenza (T-11-SC resta chiuso: `androidx.car.app:app:1.7.0` era gia' risolta).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- SC1 e' chiuso **lato codice**. La conferma con un head unit reale (release/staging build su un host Android Auto legittimo) resta delegata al Piano 03: nessun test automatico puo' dimostrare che l'host reale supera il gate, perche' i digest della sua firma non sono verificabili senza il dispositivo.
- Attenzione per il Piano 03 (nota gia' in `11-PATTERNS.md`): il log diagnostico `onGetTemplate #` di `SpeedScreen` esiste solo sotto `BuildConfig.DEBUG`, cioe' nella stessa build che mantiene il validatore permissivo. La verifica di SC1 e le misure SC2/SC3 **non possono essere esercitate dallo stesso binario** — SC1 va confermato visivamente su una build release.
- `playstore/README.md` contiene ancora la nota "Rischio noto accettato per questo rilascio" che descrive il validatore permissivo: ora superata dal codice, va ritirata (sostituita da una spiegazione del cambiamento, non cancellata) — fuori dallo scope di questo piano, tracciata da `11-CONTEXT.md`.

---
*Phase: 11-hardening-di-produzione-e-verifica-su-dispositivo-reale*
*Completed: 2026-09-03*
