# Phase 11: Hardening di Produzione e Verifica su Dispositivo Reale - Pattern Map

**Mapped:** 2026-09-03
**Files analyzed:** 8 (3-4 new, 2 modified, 2 conditional)
**Analogs found:** 7 / 8

**Scope note carried from CONTEXT.md:** only SC1 (`HostValidator` reale, debug/release split per D-01/D-02) produces guaranteed code changes. SC2 (test su strada, D-06/D-07) and SC3 (connect/disconnect rapido, D-05) are **human-checkpoint + documentation** work by default; they produce code changes *only if* the empirical test reveals a real defect. Patterns for both eventualities are mapped below, with the conditional ones clearly marked.

**No RESEARCH.md for this phase.** Where a Car App Library fact was needed, it was verified by direct inspection of the resolved artifact `androidx.car.app:app:1.7.0` (AAR from the Gradle cache: `res/values/values.xml`, `public.txt`, `classes.jar` bytecode via `javap`) rather than assumed. Those verified facts are recorded inline below and are the highest-value part of this document.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `app/src/main/java/com/sed/tachimetro/car/TachimetroCarAppService.kt` (MODIFY) | service (entry point / IPC boundary) | request-response (host binds via Binder, validated once per bind) | itself before/after + `MainActivity.kt:299-307` (`BuildConfig.DEBUG` gate) | exact |
| `app/src/main/java/com/sed/tachimetro/car/CarHostValidation.kt` (NEW, discretionary seam — D-02) | utility (pure-ish factory + fail-safe allow-list) | transform (build-flag + Context → `HostValidator`) | `app/src/main/java/com/sed/tachimetro/car/CarLinkState.kt:44-49` (`resolveCarLinkState`, fail-safe allow-list of one value) | role-match |
| `app/src/androidTest/java/com/sed/tachimetro/car/CarHostValidationTest.kt` (NEW) | test (instrumented — needs a real `Context`) | transform | `app/src/androidTest/java/com/sed/tachimetro/car/SpeedScreenTemplateTest.kt` | exact |
| `app/src/main/res/values/car_hosts_allowlist.xml` (NEW, **conditional** — only if the planner rejects the library's private resource, see "Verified library facts") | config (resource) | — | `app/src/main/res/values/strings.xml` (existing `<resources>` file) | role-match |
| `docs/android-auto-hardening-verification.md` (NEW) | doc (human-checkpoint runbook for SC2 + SC3) | — | `docs/dhu-quota-verification.md` | exact |
| `scripts/aa-background-check.ps1` (NEW, **conditional** — only if the planner wants a scripted logcat/PID measurement for SC2/SC3) | script (measurement harness) | batch | `scripts/dhu-quota-check.ps1` | exact |
| `playstore/README.md` (MODIFY — remove/close the accepted-risk note once SC1 lands) | doc | — | itself, lines 24-40 | exact |
| `app/build.gradle.kts` | config | — | itself, lines 64-69 | **no change expected** — see below |
| `app/src/main/java/com/sed/tachimetro/car/SpeedScreen.kt` (**conditional** — D-05, only if SC3 reveals a crash/incoherent state) | component/screen | streaming | itself (`SpeedScreen.kt:61-67`, the existing `requestInFlight` race guard) | exact |

**`app/build.gradle.kts` needs no edit for D-01/D-02.** `buildFeatures { buildConfig = true }` is already enabled (lines 64-69) and `com.sed.tachimetro.BuildConfig.DEBUG` is already imported and used in two production files. A dedicated build type / product flavor would be new machinery for zero added capability — the "no new mechanism when an existing one fits" bias of this codebase argues for `BuildConfig.DEBUG`. The one alternative worth naming (because it is what Google's own sample uses) is `applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0`; it is equivalent in outcome here and *less* consistent with the codebase, so `BuildConfig.DEBUG` is the recommended default.

## Verified library facts (androidx.car.app:app 1.7.0, read from the AAR)

These are not assumptions — each was read out of the resolved artifact in `~/.gradle/caches/.../androidx.car.app/app/1.7.0/app-1.7.0.aar`.

**1. `HostValidator` public API** (`javap` on `classes.jar`):
```
public final class androidx.car.app.validation.HostValidator {
  public static final androidx.car.app.validation.HostValidator ALLOW_ALL_HOSTS_VALIDATOR;
  public boolean isValidHost(androidx.car.app.HostInfo);
  public java.util.Map<java.lang.String, java.util.List<java.lang.String>> getAllowedHosts();
}
public final class androidx.car.app.validation.HostValidator$Builder {
  public Builder(android.content.Context);
  public Builder addAllowedHost(java.lang.String, java.lang.String);   // (packageName, sha256Signature)
  public Builder addAllowedHosts(int);                                  // @ArrayRes string-array
  public HostValidator build();
}
```
`getAllowedHosts()` being **public** is the test seam: it lets an instrumented test assert the release allow-list content without ever needing a real host.

**2. `addAllowedHosts(int)` entry format is `<sha256Digest>,<packageName>`** — confirmed from the bytecode: the method does `entry.split(",", -1)`, requires exactly 2 parts, then calls `addAllowedHost(parts[1], parts[0])` (i.e. package name second in the string, first in the method). Getting this order backwards silently produces a validator that rejects every host.

**3. The library ships the official allow-list as `androidx.car.app.R.array.hosts_allowlist_sample`** (`res/values/values.xml` in the AAR), containing 6 entries: three signatures for `com.google.android.projection.gearhead` (Android Auto host) and three for `com.google.android.apps.automotive.templates.host` (Automotive OS templates host) — dev, beta and release signing keys of each.

**4. Pitfall the planner must decide on:** `hosts_allowlist_sample` is **not** listed in the AAR's `public.txt` (which contains only 5 `carColor*`/`carPermissionActivityLayout` attrs). Under AGP's resource-visibility rules a non-empty `public.txt` makes every unlisted resource *private*, so referencing `androidx.car.app.R.array.hosts_allowlist_sample` can raise the `PrivateResource` lint check even though Google's own documentation sample uses exactly that reference. Two acceptable resolutions, both consistent with this codebase:
   - use the library resource and, if lint complains, suppress narrowly at the call site with a comment explaining *why* (same discipline as the existing `@Suppress("MissingPermission")` in `GpsSpeedProvider.kt`);
   - or copy the 6 entries into the app's own `app/src/main/res/values/car_hosts_allowlist.xml` (`translatable="false"`), which removes the lint question but creates a copy that will not track library updates — that trade-off must be written down, not left implicit.

## Pattern Assignments

### `app/src/main/java/com/sed/tachimetro/car/TachimetroCarAppService.kt` (service, request-response)

**Analog:** itself (the whole file is 22 lines) plus the established `BuildConfig.DEBUG` gate pattern from `MainActivity.kt`/`SpeedScreen.kt`.

**Current state to replace** (`TachimetroCarAppService.kt:14-22`, full file body):
```kotlin
class TachimetroCarAppService : CarAppService() {

    // Validatore permissivo accettabile solo per lo scaffold e per il test DHU di questa fase;
    // la sostituzione con una allow-list reale e' scope esplicito della Fase 11
    // (ROADMAP.md Phase 11 SC1) e NON va anticipata qui.
    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR

    override fun onCreateSession(): Session = TachimetroCarSession()
}
```
Note for the planner: the string `Fase 11` in that comment is a **greppable debt marker** deliberately planted by `08-02-PLAN.md` Task 2 and asserted by `08-VERIFICATION.md`. Closing SC1 means replacing that comment with a *documentation* comment about the debug/release split (D-01 requires the difference be explicit), not simply deleting it.

**`BuildConfig.DEBUG` gate pattern to copy** (`app/src/main/java/com/sed/tachimetro/car/SpeedScreen.kt:27` and `:307-312`):
```kotlin
import com.sed.tachimetro.BuildConfig
...
if (BuildConfig.DEBUG) {
    Log.d(
        LOG_TAG,
        "onGetTemplate #$templateBuildCount content=${templateLogLabel(permission, latestState)}",
    )
}
```
Same shape in `MainActivity.kt:299-307` — an `if (BuildConfig.DEBUG) { ... }` block with an inline comment naming the threat-model ID it satisfies (`T-10-05` there, `T-08-07` in `SpeedScreen`). The Phase 11 equivalent must name `T-08-05` / `T-09-10` (the two inherited "accept" dispositions this phase closes).

**KDoc pattern to preserve** (`TachimetroCarAppService.kt:7-13`) — the existing class KDoc explains *why* `exported="true"` is a platform requirement. The new `createHostValidator()` documentation must sit alongside it in the same register: explain the split, name D-01, and state that debug builds are intentionally permissive.

**Context source:** `CarAppService` is a `Service`, so `applicationContext` is available directly — no Activity/`CarContext` is involved. This matches the codebase-wide WR-04 rule ("`applicationContext`, mai l'Activity") already stated at `MainActivity.kt:281-282` and `SpeedScreen.kt:51-53`. `HostValidator.Builder` only uses the Context to reach `getResources()` and `getPackageManager()`.

---

### `app/src/main/java/com/sed/tachimetro/car/CarHostValidation.kt` (utility, transform) — discretionary seam

**Analog:** `app/src/main/java/com/sed/tachimetro/car/CarLinkState.kt:44-49` — the codebase's canonical "fail-safe allow-list expressed as a documented top-level function" pattern.

**Analog excerpt** (`CarLinkState.kt:25-49`, KDoc + function):
```kotlin
/**
 * Funzione pura che deriva [CarLinkState] dal tipo di connessione riportato da
 * `CarConnection.getType()` (CONN-01).
 * ...
 * Il default e' [CarLinkState.Disconnected] (fail-safe, T-10-02): un valore spurio, manomesso o
 * futuro/sconosciuto non deve MAI poter sostituire il tachimetro con un messaggio neutro sul
 * telefono. L'unico ramo che produce [CarLinkState.Connected] e' il confronto con la costante
 * simbolica `CarConnection.CONNECTION_TYPE_PROJECTION` ...
 */
fun resolveCarLinkState(connectionType: Int?): CarLinkState =
    if (connectionType == CarConnection.CONNECTION_TYPE_PROJECTION) {
        CarLinkState.Connected
    } else {
        CarLinkState.Disconnected
    }
```

**Why extract a seam at all:** `androidx.car.app:app-testing:1.7.0` ships `ScreenController`, `SessionController`, `TestCarContext` — but **no** `CarAppService` controller (verified by listing `classes.jar`). There is therefore no supported way to instantiate `TachimetroCarAppService` in a test and call `createHostValidator()` on it. A top-level function taking the two inputs explicitly is the only testable shape, and it mirrors exactly the seam trick already used in Phase 9 (`SpeedScreen.buildTemplate(permission, speed)` made public purely for testability — see its KDoc at `SpeedScreen.kt:209-215`, "Pubblica come seam di test").

**Target shape** (parameters injected, no hidden reads — same contract discipline as `resolveEffectiveKeepScreenOn`, `CarLinkState.kt:65-69`):
```kotlin
fun createCarHostValidator(context: Context, allowAllHosts: Boolean): HostValidator =
    if (allowAllHosts) {
        HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    } else {
        HostValidator.Builder(context)
            .addAllowedHosts(<allow-list array res>)
            .build()
    }
```
with the service reduced to `override fun createHostValidator() = createCarHostValidator(applicationContext, BuildConfig.DEBUG)`. Keeping `BuildConfig.DEBUG` at the *call site* (not inside the function) is what makes both branches reachable from a single instrumented test — the same reason `buildTemplate` takes `permission` as a parameter instead of reading `permissionState.value`.

**Naming conventions to follow:** file `PascalCase.kt` matching its domain (`CarLinkState.kt`, `CarSpeedContent.kt`), top-level function `camelCase` with a `resolve`/`create` verb prefix, KDoc naming the requirement/threat ID it locks.

---

### `app/src/androidTest/java/com/sed/tachimetro/car/CarHostValidationTest.kt` (test, transform)

**Analog:** `app/src/androidTest/java/com/sed/tachimetro/car/SpeedScreenTemplateTest.kt` — instrumented because a real `Context`/`Resources` is required (`HostValidator.Builder` calls `context.getResources().getStringArray(...)`, so a plain JVM test cannot run it; note this is why the analog is the **androidTest** one, not `CarLinkStateTest.kt`).

**Harness pattern** (`SpeedScreenTemplateTest.kt:70-90`):
```kotlin
@RunWith(AndroidJUnit4::class)
class SpeedScreenTemplateTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }
    ...
```
(`runOnMainSync` is **not** needed here — that wrapper exists in the analog only because `Screen`/`LifecycleRegistry` require the main thread. `HostValidator.Builder` has no threading constraint.)

**Assertion style to copy** (`SpeedScreenTemplateTest.kt:129-145`) — one behaviour per test, a comment naming the decision ID being locked, `assertEquals`/`assertTrue` from `org.junit.Assert`, snake_case test names describing condition and outcome:
```kotlin
@Test
fun granted_reading_showsDigitsAndUnitWithoutActions() {
    // D-01: solo le cifre nel titolo, unita' in uno slot separato, mai concatenata; nessuna
    // Action nel ramo Granted.
    val template = buildTemplate(...)
    ...
    assertTrue(template.pane.actions.isEmpty())
}
```

**Exhaustive fail-safe coverage pattern** (`app/src/test/java/com/sed/tachimetro/car/CarLinkStateTest.kt:24-59`) — Phase 10 locked its allow-list by asserting *every* rejected input, not just the accepted one (`notConnected_`, `native_`, `nullValue_`, `negativeValue_`, `unknownFutureValue_` all `returnsDisconnected`). The Phase 11 equivalent, using the verified-public `getAllowedHosts()` seam:
- `debugBuild_returnsAllowAllValidator` → `assertSame(HostValidator.ALLOW_ALL_HOSTS_VALIDATOR, createCarHostValidator(context, allowAllHosts = true))`
- `releaseBuild_allowListContainsGearheadHost` → `assertTrue(validator.allowedHosts.containsKey("com.google.android.projection.gearhead"))`
- `releaseBuild_allowListIsNotEmpty` → guards against the silent `IllegalArgumentException`-free failure mode where a wrong array res id yields an empty map
- (optional but high-value, given verified fact #2) assert the signature list for a package is non-empty — this is what would catch a `<packageName>,<digest>` ordering mistake, which otherwise produces a validator that compiles, ships, and rejects every real host.

---

### `docs/android-auto-hardening-verification.md` (doc, human checkpoint for SC2 + SC3)

**Analog:** `docs/dhu-quota-verification.md` — the phase-8 runbook for the identical "empirical gate that no automated test can replace" pattern (D-06/D-07 here mirror D-08/D-10 there).

**Section skeleton to copy verbatim** (`docs/dhu-quota-verification.md`, headings in order):
```markdown
# <Titolo>
<paragrafo: quale gate di quale fase questo runbook serve, con riferimento al SC>
## Perche' questa verifica esiste
## Prerequisiti
## Procedura passo-passo
## Cosa osservare a occhio durante la sessione
## Criteri di esito        <- tabella PASS / FAIL / INCONCLUSIVO
## Contingenza se FAIL     <- la decisione gia' presa, non una da prendere sul momento
## Cosa e' fuori scope di questa verifica
```

**"Cosa osservare a occhio" pattern** (`docs/dhu-quota-verification.md:63-74`) — a bulleted list explicitly framed as the input to the blocking human checkpoint:
```markdown
Questi punti sono l'input diretto del checkpoint umano del Task 3 -- nessuno di questi e'
verificabile da codice:

- Il numero sullo schermo auto **cambia visibilmente** e corrisponde al numero mostrato
  sull'AVD/telefono nello stesso istante.
- ... lo schermo auto passa a **"Ricerca segnale..."** -- mai bloccato su un valore ormai scaduto.
- **Nessun ritorno improvviso** alla lista app dell'head unit durante la sessione.
```
For SC2 the equivalent observations are: phone physically locked/screen off for the whole run, the car screen number keeps changing, it never freezes on a stale value, and it never sits permanently on "Ricerca segnale...". For SC3: repeated fast connect/disconnect leaves no crash, no ANR, and no incoherent car screen.

**"Criteri di esito" table pattern** (`docs/dhu-quota-verification.md:80-84`) — three explicit rows, `FAIL` first, with the *measurable* condition spelled out rather than left to judgement:
```markdown
| Esito | Condizione |
|-------|------------|
| `FAIL` | Il PID di `com.sed.tachimetro` e' sparito o e' cambiato durante la sessione, oppure il conteggio dei refresh `onGetTemplate #` si e' fermato per piu' di 30 secondi consecutivi prima della fine |
| `PASS` | Il processo e' rimasto vivo per tutta la durata e la cadenza media osservata e' compresa tra 0,8 e 1,2 refresh/secondo |
| `INCONCLUSIVO` | Ogni altro caso |
```

**"Contingenza se FAIL" pattern** (`docs/dhu-quota-verification.md:89-107`) — this section states the already-decided fallback *and* explicitly re-closes the options that must not be reopened. The Phase 11 version writes D-03/D-04: document the platform limit as known and accepted, and **do not** add `ACCESS_BACKGROUND_LOCATION` (the phase-8 doc's equivalent sentence is "resta **esplicitamente scartata** e **non va riaperta**").

**Duration constant to reuse:** 5-10 minutes, `600` seconds at the high end — D-06 explicitly reuses the phase-8 threshold (`docs/dhu-quota-verification.md:48-49`, `-DurationSeconds 600`).

**Where to record the documented limit if SC2 fails (open in D-03/Discretion):** the in-repo precedent is *two-layered* — a durable technical note in `docs/` (the "Cosa e' fuori scope" section of the phase-8 runbook, lines 109-114, is exactly how phase 8 handed its unverified gap forward to phase 11) plus the phase's own `11-VERIFICATION.md`/`STATE.md` entry. `.planning/research/PITFALLS.md` Pitfall 6 (lines 130-148) is the statement of the gap and should be cross-referenced, not rewritten.

---

### `scripts/aa-background-check.ps1` (script, batch) — conditional

**Analog:** `scripts/dhu-quota-check.ps1` — same measurement problem (logcat on tag `TachimetroCar`, PID sampling of `com.sed.tachimetro`, heuristic verdict), different setup (real phone, locked screen, real drive).

**Header/param pattern to copy** (`scripts/dhu-quota-check.ps1:1-55`): a full comment-based-help block (`.SYNOPSIS` naming the decision IDs, `.DESCRIPTION` pointing at the `docs/` runbook, `.PARAMETER` per argument, `.EXAMPLE`), then:
```powershell
[CmdletBinding()]
param(
    [int]$DurationSeconds = 600,
    [string]$OutputDir = "build/dhu-quota",
    [string]$Serial = ""
)

$ErrorActionPreference = 'Stop'

# applicationId di app/build.gradle.kts -- valore letterale, non caricato dinamicamente:
# lo script non deve dipendere da un parsing del build script per restare semplice e
# ispezionabile a occhio.
$AppId = "com.sed.tachimetro"
$LogTag = "TachimetroCar"
```
Two constraints carried over verbatim from the analog's header: the script **installs and downloads nothing** (uses `adb` already on PATH), and its heuristic verdict **does not replace** the human confirmation. Output must go under `build/` (already gitignored — the analog cites `git check-ignore` verification for T-08-12) so captured logs never enter the repo.

**Reusable diagnostic already in place:** `SpeedScreen.onGetTemplate()` emits `Log.d("TachimetroCar", "onGetTemplate #<n> content=<label>")` on every rebuild (`SpeedScreen.kt:303-314`) — but **only under `BuildConfig.DEBUG`**. Planner must reconcile this with D-01: an SC2/SC3 run that needs this log is necessarily a *debug* build, which is also the build that keeps `ALLOW_ALL_HOSTS_VALIDATOR`. So the SC1 validator and the SC2/SC3 measurement cannot be exercised by the same binary through this log; the runbook should say so explicitly and route the SC1 verification (real head unit, release/staging build) through visual confirmation instead, per `11-CONTEXT.md` `<specifics>`.

---

### `playstore/README.md` (doc, MODIFY)

**Analog:** itself. Section to rewrite once SC1 lands (`playstore/README.md:24-40`), currently titled "## Rischio noto accettato per questo rilascio (nota interna)" and opening:
```markdown
`TachimetroCarAppService.createHostValidator()` restituisce ancora
`HostValidator.ALLOW_ALL_HOSTS_VALIDATOR`: il car service accetta quindi il binding da
**qualunque** host Android Auto, invece di limitarsi a quelli legittimi. Sostituirlo con un
validator reale è il lavoro della **Fase 11** ...
```
`11-CONTEXT.md` marks this as out of direct phase scope but flags that downstream must know. The precedent for retiring such a note is the same file's own line 16-20 (a superseded instruction was *replaced with an explanation of why it changed*, not silently deleted).

---

### `app/src/main/java/com/sed/tachimetro/car/SpeedScreen.kt` (component/screen) — conditional, D-05 only

**Do not pre-emptively harden.** D-05 is explicit: guards are added only if the SC3 test produces a concrete crash or incoherent state.

**If a guard turns out to be needed, the analog is in the same file** (`SpeedScreen.kt:61-67`) — the existing in-memory race guard against `repeatOnLifecycle(STARTED)` re-entry, which is precisely the class of bug a fast connect/disconnect cycle would surface:
```kotlin
// Pitfall 2 (09-RESEARCH.md): repeatOnLifecycle(STARTED) riparte a ogni rientro in STARTED
// (es. Screen brevemente STOPPED e poi di nuovo STARTED mentre una richiesta e' ancora
// pendente). Il contatore persistito da solo non copre questo caso, perche' nessun rifiuto
// e' ancora stato registrato quando cio' accade: senza questa guardia in memoria si
// rischierebbe di lanciare una seconda CarContext.requestPermissions() mentre la prima e'
// ancora in attesa del suo callback.
private var requestInFlight = false
```
Its consumers (`SpeedScreen.kt:135`, `:154`, `:159`, `:188`) show the full discipline: check-and-return at every entry point, set before the async call, clear in the callback.

## Shared Patterns

### Fail-safe allow-list at a trust boundary
**Source:** `app/src/main/java/com/sed/tachimetro/car/CarLinkState.kt:44-49`
**Apply to:** `createHostValidator()` / `CarHostValidation.kt`
The project already states this as an ethos (`.planning/research/ARCHITECTURE.md:280` ties the real `HostValidator` to the existing `sanitizePersistedMax()` "sanitize/validate at the boundary" habit). The rule expressed by the analog: **exactly one branch produces the permissive/trusting outcome, and it is guarded by a symbolic constant** — everything else falls through to the safe default. For Phase 11 that means the `ALLOW_ALL` branch must be reachable only through the build-flag check, never as a fallback (e.g. never `try { realValidator() } catch { ALLOW_ALL }`).

### `BuildConfig.DEBUG` gating with a threat-model comment
**Source:** `app/src/main/java/com/sed/tachimetro/MainActivity.kt:299-307`, `app/src/main/java/com/sed/tachimetro/car/SpeedScreen.kt:307-312`
**Apply to:** `TachimetroCarAppService.kt`
Every existing `BuildConfig.DEBUG` block in this codebase carries an inline comment naming the threat-model ID that motivates the gate (`T-10-05`, `T-08-07`). Phase 11's gate closes `T-08-05`/`T-09-10` and must say so; D-01 additionally requires the debug/release *difference* to be documented explicitly rather than left implicit.

### `applicationContext`, never an Activity/CarContext (WR-04)
**Source:** `app/src/main/java/com/sed/tachimetro/car/SpeedScreen.kt:51-53`, `app/src/main/java/com/sed/tachimetro/MainActivity.kt:281-282`
**Apply to:** the `HostValidator.Builder(context)` call site
Uniform across every long-lived component in the repo (`GpsSpeedProvider`, `MaxSpeedStore`, `ScreenOnPreferenceStore`, `CarPermissionDenialStore`, `CarConnection`).

### KDoc/inline comments tagged with decision IDs, in Italian
**Source:** every file under `app/src/main/java/com/sed/tachimetro/car/`
**Apply to:** all new/modified source in this phase
Comments explain *why*, are written in Italian, use ASCII apostrophes (`perche'`, `unita'`, `e'`) rather than accented characters in source comments, and are prefixed/suffixed with the requirement or decision tag they implement (`D-01`, `T-08-05`, `AA-04`, `WR-04`, `CONN-01`). Public functions get `/** ... */` KDoc; test-only seams document *why* they are public (see `SpeedScreen.kt:209-215`).

### Empirical gate = script (heuristic) + runbook (human eyes), neither substituting the other
**Source:** `docs/dhu-quota-verification.md:86-87`, `scripts/dhu-quota-check.ps1:26-32`
**Apply to:** SC2 and SC3
Phase 8's D-10 pattern, restated in both artifacts so neither can be mistaken for sufficient on its own. D-07 makes the same demand here.

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| `app/src/main/res/values/car_hosts_allowlist.xml` (only if the copy-the-array option is chosen) | config (resource) | — | No `string-array` resource exists anywhere in `app/src/main/res/values/` today; the only structural precedent is the plain `<resources>` wrapper of `strings.xml`. The content itself is fully specified by verified fact #3 above (the 6 `<digest>,<package>` entries of `androidx.car.app`'s `hosts_allowlist_sample`), so this is a transcription task, not a design task. |

## Metadata

**Analog search scope:** `app/src/main/java/com/sed/tachimetro/**`, `app/src/test/java/com/sed/tachimetro/car/**`, `app/src/androidTest/java/com/sed/tachimetro/car/**`, `app/src/main/res/**`, `app/src/main/AndroidManifest.xml`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `docs/`, `scripts/`, `playstore/README.md`, `.planning/research/`, `.planning/phases/08..10/`
**External artifact inspected (read-only):** `androidx.car.app:app:1.7.0` AAR from the Gradle module cache — `res/values/values.xml`, `public.txt`, `R.txt`, `classes.jar` (`javap` on `HostValidator` / `HostValidator$Builder`); `androidx.car.app:app-testing:1.7.0` AAR — `classes.jar` listing
**Files scanned:** 18
**Pattern extraction date:** 2026-09-03
