# Phase 9: Permesso di Localizzazione dallo Schermo Auto - Research

**Researched:** 2026-09-02
**Domain:** Android Auto Car App Library — `CarContext.requestPermissions()`, permission-denial state
tracking without an `Activity`, `PaneTemplate`/`Pane` action model
**Confidence:** HIGH for platform mechanics (verified directly against `androidx.car.app` source code,
not just docs); MEDIUM for the exact recommended denial-tracking mechanism (a reasoned, source-grounded
design — no official Google-documented precedent for this specific problem was found); LOW/flagged for
the "automatic request while driving" edge case (a genuine, documented platform gap)

## Summary

The central open question from CONTEXT.md D-04 is now answered with source-level certainty:
**`CarContext.requestPermissions()`'s callback (`approved`/`rejected` string lists) carries no signal
distinguishing a first denial from a permanent ("don't ask again") denial, and there is no
`PackageManager`-level or `CarContext`-level equivalent of `Activity.shouldShowRequestPermissionRationale()`.**
This was confirmed by reading the actual `androidx.car.app` library source (`CarContext.java`,
`CarAppPermissionActivity.java`) rather than relying on documentation summaries alone:
`CarAppPermissionActivity` — the real phone-side `Activity` that `CarContext.requestPermissions()`
silently launches — is a bare 30-line wrapper around `ActivityResultContracts.RequestMultiplePermissions()`
with no rationale check, no denial-count tracking, and no Settings redirect of its own. Whatever D-04
needs, this app must build itself.

The recommended, code-grounded mechanism is a **persisted denial counter** (`CarPermissionDenialStore`,
one `SharedPreferences` int, mirroring the existing `MaxSpeedStore`/`ScreenOnPreferenceStore` pattern
already in this codebase) combined with a **pure state-resolution function**
(`resolveCarPermissionState(granted, denialCount)`), unit-testable exactly like `reduceMax`/`deriveSpeedState`.
Because Android 11+ (this app's `minSdk 30` floor, with no exceptions) auto-suppresses the system dialog
after a **second** denial regardless of which call site triggered it, a simple 0/1/2+ counter reliably
reproduces the OS's own "first ask vs. permanent" distinction for the resting UI state, without ever
needing to hold an `Activity` reference (which would violate this project's own established
"never retain an Activity in a long-lived component" convention).

Three concrete platform mechanics were newly discovered by reading source (not previously documented in
this project's Phase 8 research) and materially affect the plan: (1) `CarContext.requestPermissions()`'s
own Javadoc states it "should be called using a `ParkedOnlyOnClickListener`" and that the host will
silently skip the request "when the host deems it is unsafe (for example, when the user is driving)" —
this is a real risk to D-05's "fully automatic, no user action" requirement if the car connects while
already in motion; (2) `Pane.Builder.addAction(Action)` on `PaneTemplate` (already locked in Phase 8) is
fully supported, up to 2 actions, no icon required, no `@RequiresCarApi` above the project's existing
`minCarApiLevel=1` — the retry/open-settings action from D-03 is directly implementable on the current
template; (3) `CarContext.startActivity(Intent)` is a **plain, unrestricted `ContextWrapper.startActivity()`**
(confirmed: no override exists in `CarContext.java`) — opening the phone's Settings screen for D-04's
"Apri impostazioni" action works exactly like `MainActivity.openAppSettings()` today, just needs
`FLAG_ACTIVITY_NEW_TASK` added (the same flag `CarContext`'s own internal permission-request code uses).

**Primary recommendation:** Replace the T-08-08 defensive gate in `SpeedScreen` with a small
`CarPermissionState` sealed model (`Granted` / `NotRequested` / `Waiting` / `Denied(permanent: Boolean)`),
a pure `resolveCarPermissionState()` resolver driven by a persisted `CarPermissionDenialStore` counter,
and a `PaneTemplate` `Action` (wrapped in `ParkedOnlyOnClickListener`) for the manual retry/open-settings
step — no new library dependency, no manifest change, no `minCarApiLevel` bump.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Detect permission not granted | API/Backend equivalent (`Screen`, in-process, no server) | — | `Screen` runs in the app's own process; `ContextCompat.checkSelfPermission(carContext, ...)` reflects the real, app-UID-level grant state — same check already used by `MainActivity`. |
| Trigger system permission dialog | Browser/Client equivalent — phone-side `Activity` (`CarAppPermissionActivity`, library-internal) | Car `Screen` (triggers it) | The dialog itself always renders as a real Android `Activity` on the phone, launched internally by `CarContext.requestPermissions()`; the car `Screen` only triggers and observes the result, never renders the dialog itself. |
| Persist denial state across `Screen` recreations | Database/Storage tier (`SharedPreferences`, via new `CarPermissionDenialStore`) | — | `Screen` instances are recreated by the host across session lifecycle events; only persisted storage survives that, mirroring `MaxSpeedStore`/`ScreenOnPreferenceStore`. |
| Render "waiting"/"denied" car-screen state | Car `Screen` (template layer, closest analog to "Frontend Server/SSR" — host-rendered, app-controlled data) | — | `onGetTemplate()` decides content; the host (not the app) controls final pixel rendering, consistent with the already-locked `PaneTemplate` approach from Phase 8. |
| Redirect to phone Settings | Browser/Client equivalent — phone-side `Activity` (system Settings app) | Car `Screen` (triggers via `CarContext.startActivity()`) | Same shape as the permission dialog: the car screen only triggers, the phone renders. |

## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** The waiting state (dialog pending on the phone) shows exactly **"Controlla il telefono"** —
  a new string resource, distinct from `car_searching_gps_signal`/`searching_gps_signal`.
- **D-02:** On denial, the car screen shows a message inspired by `permission_denied` but shortened
  (mirroring the Phase 8 D-02 "auto copy always shorter than phone copy" pattern). Exact string text is
  Claude's Discretion; tone/content is locked (explains the missing permission, not a blank screen).
- **D-03:** The car screen offers an explicit retry action (row/`Action`) that relaunches
  `CarContext.requestPermissions()` without leaving the car app — conceptual mirror of the phone's
  `retryButton`/`MainActivity.onRetryClicked()`.
- **D-04:** The message/action MUST distinguish a first denial from a permanent ("don't ask again")
  denial, exactly like the phone (`permission_denied` vs. `permission_denied_permanent`, where the
  button switches from "Riprova" to "Apri impostazioni"). On the car, if relaunching the dialog has no
  further effect, the message/action must direct the user to open Settings on the phone instead of
  retrying uselessly. **The mechanism is explicitly open for research** (this document); the desired
  *behavior* is fixed.
- **D-05:** The permission request fires **automatically** on first connection (when the car screen
  detects the permission is not granted) — mirror of `MainActivity.checkAndRequestPermission()` called
  from `onCreate()`. No preliminary user action required before the dialog appears on the phone.
- **D-06:** Retry after a denial (D-03) remains an explicit tap-triggered action — never
  auto-relaunched in a loop after a denial has already been received.
- **D-07:** The system dialog on the phone keeps its **default** appearance — no
  `androidx.car.app.theme`/`carPermissionActivityLayout` customization, consistent with Phase 8 D-03
  (no branding in the car screen either).
- **SC2 (roadmap, not a gray area):** Granting the permission automatically transitions the screen to
  speed/searching without any app/connection restart. The exact reactive mechanism (e.g., a `StateFlow`
  observed reactively, mirroring `MainActivity`'s `permissionGranted`) is an implementation detail, not
  a UX gray area.

### Claude's Discretion

- Exact Italian text for "Controlla il telefono" wording is locked; exact strings/resource names for the
  single-denial and permanent-denial messages (D-02/D-04) are free (tone/content locked).
- Technical mechanism to detect "permanently denied" from the car `Screen` (D-04) — this document's
  primary deliverable.
- Exact reactive mechanism for the post-grant auto-transition (SC2).
- Exact shape of the retry action on the car template (`Action` in `PaneTemplate`, clickable row, etc.)
  — the functional requirement (D-03) is locked, the Car App Library component choice is not.

### Deferred Ideas (OUT OF SCOPE)

None — discussion stayed within phase scope (per 09-CONTEXT.md `<deferred>`).

## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| AA-04 | Se il permesso di localizzazione non è ancora stato concesso quando l'utente si connette per la prima volta via Android Auto, lo schermo auto lo richiede esplicitamente (`CarContext.requestPermissions()`) invece di restare vuoto/bloccato | Full API mechanics (signature, executor, callback shape), the D-04 denial-detection mechanism, the D-05/D-06 automatic-vs-manual trigger design, the SC2 reactive-transition design, and the D-03 `PaneTemplate` `Action` implementation are all covered below with source-verified code examples. |

## Project Constraints (from CLAUDE.md)

- Kotlin only, XML layouts (N/A here — no XML layout involved in Car App Library templates).
- `applicationContext` only in long-lived components, never an `Activity` — directly relevant: this
  rules out the "hold a `MainActivity` reference to call `shouldShowRequestPermissionRationale()`"
  alternative floated in CONTEXT.md's research note; see "Alternatives Considered" below for why it was
  rejected in favor of the persisted-counter approach.
  Comment style: inline comments reference decision tags (D-XX, AA-XX); KDoc for public classes/functions.
- Prefer small, pure, testable functions (`reduceMax`, `deriveSpeedState`, `mapSpeedToKmh` precedent) —
  `resolveCarPermissionState()` below follows this convention exactly and should get a
  `CarPermissionStateTest.kt` mirroring `MaxSpeedReducerTest.kt`.
- Sealed classes for state (`SpeedState`, `CarSpeedContent`) — `CarPermissionState` below follows this.
- SharedPreferences preferred over Room/DataStore for small scalar state (single Int) — matches
  `MaxSpeedStore`'s own stated rationale ("un intero non giustifica una dipendenza").
- No new permission scope: this phase does **not** request any permission beyond the already-declared
  `ACCESS_FINE_LOCATION` — consistent with the project's existing "no unnecessary permissions" posture.

## Standard Stack

No new dependency is required. `androidx.car.app:app:1.7.0` and `androidx.car.app:app-testing:1.7.0`
are already declared (`gradle/libs.versions.toml`, added in Phase 8) and already satisfy every API used
by this phase.

### Core (already present, verified sufficient)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `androidx.car.app:app` | 1.7.0 (pinned, `gradle/libs.versions.toml`) | `CarContext.requestPermissions()`, `Pane.addAction`, `ParkedOnlyOnClickListener` | [VERIFIED: androidx.car.app source, androidx-main branch] None of the APIs this phase needs (`Action.setOnClickListener`, `Pane.addAction`, `ParkedOnlyOnClickListener.create`, `CarContext.requestPermissions`) carry a `@RequiresCarApi` annotation above what the project's `minCarApiLevel=1` manifest declaration already supports — no version bump, no manifest change needed. |
| `androidx.car.app:app-testing` | 1.7.0 (pinned) | Screen/Session unit testing (if the planner extends test coverage into `Screen` itself) | Already added Phase 8; no change needed. |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Persisted `SharedPreferences` denial counter (recommended) | Hold a static/companion reference to `MainActivity` and call `ActivityCompat.shouldShowRequestPermissionRationale()` on it when alive | Rejected: contradicts this project's own documented Anti-Pattern ("Retaining Activity Reference in Long-Lived Components", CLAUDE.md), leaks/staleness risk, and does **not** help in the primary target scenario of this phase — car-first connection **before** `MainActivity` has ever run, where no `Activity` reference would exist to query in the first place. |
| Persisted `SharedPreferences` denial counter (recommended) | Timing heuristic ("if `requestPermissions()`'s callback fires within N ms, assume the OS silently auto-denied") | Rejected: fragile, device/latency-dependent, no official signal backs this; not recommended for inclusion in the plan even as a fallback. |

## Package Legitimacy Audit

No new external packages are introduced by this phase. `androidx.car.app:app`/`app-testing` were already
vetted and installed in Phase 8 (official first-party AndroidX/Google artifact — not subject to the
slopsquatting risk this gate targets). No `slopcheck`/registry verification is required here.

## Architecture Patterns

### System Architecture Diagram

```
Android Auto host binds CarAppService
        │
        ▼
TachimetroCarSession.onCreateScreen()
        │
        ▼
SpeedScreen (Screen, LifecycleOwner)
        │
        ├─ on STARTED: checkSelfPermission(ACCESS_FINE_LOCATION) ──┐
        │                                                          │
        │                    granted?  ───────────No───────────────┤
        │                       │                                  │
        │                      Yes                                 ▼
        │                       │                     denialStore.denialCount()
        │                       │                                  │
        │                       │                     count==0 ────┴──── count>=1
        │                       │                        │                   │
        │                       │                        ▼                   ▼
        │                       │           CarContext.requestPermissions() Denied(
        │                       │           (D-05, automatic)          permanent =
        │                       │                        │              count>=2)
        │                       │                        ▼                   │
        │                       │           phone-side CarAppPermissionActivity
        │                       │           (system dialog RENDERS ON PHONE, not car)
        │                       │                        │
        │                       │              approved ─┴─ rejected
        │                       │                 │            │
        │                       │                 ▼            ▼
        │                       │            Granted     denialStore.recordDenial()
        │                       │                              │
        │                       │                              ▼
        │                       │                         Denied(permanent =
        │                       │                         wasAlreadyDenied)
        │                       ▼                              │
        │            provider.gpsSpeedProvider.state ◄─────────┘ (D-06: waits for
        │            .collect { latestState = it; invalidate() }   explicit retry tap,
        │                       │                                  no auto-loop)
        ▼                       ▼
                onGetTemplate() reads current CarPermissionState + latestState,
                builds PaneTemplate(Pane(Row, optional retry/settings Action))
                       │
                       ▼
              Retry Action tap (ParkedOnlyOnClickListener)
                       │
                       ├─ permanent=false → CarContext.requestPermissions() again (D-03)
                       └─ permanent=true  → carContext.startActivity(Settings intent) (D-04)
```

### Recommended Project Structure

No new package needed — extend the existing `car/` package (mirrors `gps/`, `maxspeed/`, `screen/`):

```
app/src/main/java/com/sed/tachimetro/car/
├── SpeedScreen.kt                 # MODIFIED: replaces T-08-08 gate with the state machine below
├── CarSpeedContent.kt             # UNCHANGED: still pure SpeedState -> Row content, only used once Granted
├── CarPermissionState.kt          # NEW: sealed state (Granted/NotRequested/Waiting/Denied)
├── CarPermissionDenialStore.kt    # NEW: SharedPreferences-backed denial counter (mirrors MaxSpeedStore)
└── (optional) CarPermissionResolver.kt  # NEW: pure resolveCarPermissionState() — or inline in
                                          # CarPermissionState.kt companion, planner's call
```

### Pattern 1: Persisted denial counter as a `shouldShowRequestPermissionRationale()` substitute

**What:** A one-`Int` `SharedPreferences` counter, incremented every time the car screen's
`requestPermissions()` callback reports the permission in `rejected`. On any subsequent evaluation
(fresh `Screen`, `STARTED` re-entry), the counter alone determines the resting UI state without ever
calling `requestPermissions()` automatically again after the first denial (D-06).

**Why this works (verified via source, not assumption):** Android 11+ (this app's `minSdk 30` floor —
no API-level branching needed) auto-suppresses the system permission dialog after a user has tapped
"Deny" **twice**, regardless of which call site (`Activity.requestPermissions()` or the
`CarAppPermissionActivity` that `CarContext.requestPermissions()` launches internally) triggered either
request — the flag is tracked by the OS at the UID/permission level, not per-caller.
[VERIFIED: developer.android.com/about/versions/11/privacy/permissions — "Starting in Android 11, if the
user taps Deny for a specific permission more than once during your app's lifetime of installation on a
device, the user doesn't see the system permissions dialog if your app requests that permission again."]
A counter that reaches 2 therefore reliably means "the OS will not show a dialog on the next attempt
either" — exactly the state D-04 needs to detect.

**When to use:** Any Car App Library `Screen` that needs `shouldShowRequestPermissionRationale()`-like
behavior with no `Activity` available.

**Known limitation (documented honestly, not hidden):** If the user already denied once via
`MainActivity`'s own phone-UI flow **before ever touching Android Auto**, the car-side counter starts at
0 even though the OS is already one denial away from permanent. In that specific interleaving, the very
first car-side denial is actually the user's *second* denial overall (OS enters `USER_FIXED`/permanent
after this tap), but the car-side counter only reaches 1 — so the UI will show "Riprova" once more
before correctly flipping to "Apri impostazioni" on the *next* tap. This is a bounded, self-correcting
one-tap degradation (the retry tap after that silently produces no dialog, immediate `rejected` in the
callback, counter reaches 2, UI corrects itself) — not an infinite loop. See "Common Pitfalls" and
"Assumptions Log" below; this is the one honest gap in an otherwise fully source-grounded mechanism.

**Example:**
```kotlin
// Source: androidx.car.app CarContext.java (androidx-main, read directly), Android 11 permissions
// docs (developer.android.com/about/versions/11/privacy/permissions), applied to this project's
// MaxSpeedStore pattern (app/src/main/java/com/sed/tachimetro/maxspeed/MaxSpeedStore.kt).

// app/src/main/java/com/sed/tachimetro/car/CarPermissionDenialStore.kt
package com.sed.tachimetro.car

import android.content.Context
import com.sed.tachimetro.maxspeed.MaxSpeedStore

/**
 * D-04: sostituisce Activity.shouldShowRequestPermissionRationale() (non disponibile da uno
 * Screen, che non ha alcuna Activity dietro di se') con un contatore persistito dei rifiuti
 * registrati dallo schermo auto. Riusa il file SharedPreferences di MaxSpeedStore invece di
 * aprirne uno nuovo -- un solo file di preferenze per l'intera app.
 */
class CarPermissionDenialStore(context: Context) {
    private val prefs = context.getSharedPreferences(MaxSpeedStore.PREFS_NAME, Context.MODE_PRIVATE)

    /** Numero di rifiuti registrati finora dallo schermo auto (0 = mai rifiutato da qui). */
    fun denialCount(): Int = prefs.getInt(KEY_DENIAL_COUNT, 0)

    /** Incrementa il contatore. Chiamare SOLO quando rejected contiene il permesso richiesto. */
    fun recordDenial() {
        prefs.edit().putInt(KEY_DENIAL_COUNT, denialCount() + 1).apply()
    }

    companion object {
        private const val KEY_DENIAL_COUNT = "car_location_denial_count"
    }
}
```

### Pattern 2: Pure permission-state resolver (testable, framework-free)

**What:** A top-level pure function mapping `(granted, denialCount) -> CarPermissionState`, following
this project's established convention (`reduceMax`, `deriveSpeedState`, `mapSpeedToKmh`).

**When to use:** Every time `SpeedScreen` needs to decide what to render/do, on `STARTED` entry and
after every `requestPermissions()` callback.

**Example:**
```kotlin
// Source: derived from this codebase's own MaxSpeedReducer.kt / GpsSpeedProvider.kt deriveSpeedState()
// pure-function convention (CLAUDE.md "Function Design"), not an external reference.

// app/src/main/java/com/sed/tachimetro/car/CarPermissionState.kt
package com.sed.tachimetro.car

/** D-04: stato locale del flusso di richiesta permesso lato schermo auto -- valutato PRIMA di
 * leggere GpsSpeedProvider.state, che parte solo una volta raggiunto Granted. */
sealed class CarPermissionState {
    /** Permesso gia' concesso -- SpeedScreen collezione GpsSpeedProvider.state (SC2). */
    data object Granted : CarPermissionState()
    /** Mai richiesto da qui e mai rifiutato -- D-05: innesca subito requestPermissions(). */
    data object NotRequested : CarPermissionState()
    /** D-01: richiesta in corso, dialogo mostrato sul telefono -- "Controlla il telefono". */
    data object Waiting : CarPermissionState()
    /** D-02/D-04: rifiutato almeno una volta. permanent=true al secondo rifiuto registrato. */
    data class Denied(val permanent: Boolean) : CarPermissionState()
}

/**
 * D-04/D-05/D-06: unica funzione pura che decide lo stato -- nessuna chiamata a
 * requestPermissions() qui dentro, solo la decisione. denialCount viene da
 * [CarPermissionDenialStore.denialCount]. Esaustivo, nessun else: ogni combinazione e' esplicita.
 */
fun resolveCarPermissionState(granted: Boolean, denialCount: Int): CarPermissionState = when {
    granted -> CarPermissionState.Granted
    denialCount == 0 -> CarPermissionState.NotRequested
    else -> CarPermissionState.Denied(permanent = denialCount >= 2)
}
```

**Test analog** (mirrors `MaxSpeedReducerTest.kt` exactly):
```kotlin
package com.sed.tachimetro.car

import org.junit.Assert.assertEquals
import org.junit.Test

class CarPermissionStateTest {
    @Test
    fun granted_alwaysReturnsGranted_regardlessOfDenialCount() {
        assertEquals(CarPermissionState.Granted, resolveCarPermissionState(granted = true, denialCount = 3))
    }

    @Test
    fun notGranted_neverDenied_returnsNotRequested() {
        assertEquals(CarPermissionState.NotRequested, resolveCarPermissionState(granted = false, denialCount = 0))
    }

    @Test
    fun notGranted_deniedOnce_returnsDeniedNotPermanent() {
        assertEquals(CarPermissionState.Denied(permanent = false), resolveCarPermissionState(granted = false, denialCount = 1))
    }

    @Test
    fun notGranted_deniedTwice_returnsDeniedPermanent() {
        assertEquals(CarPermissionState.Denied(permanent = true), resolveCarPermissionState(granted = false, denialCount = 2))
    }
}
```

### Pattern 3: `SpeedScreen` wiring — automatic trigger (D-05), no auto-loop (D-06), reactive grant (SC2)

**What:** Replaces the T-08-08 gate. Re-checks fresh on every `STARTED` entry (same defensive shape as
today); the persisted counter itself prevents repeated automatic requests after the first denial — no
extra in-memory guard needed for that specific loop. An in-memory `requestInFlight` guard **is** still
needed for a narrower edge case (see Pitfall below: `STARTED`→`STOPPED`→`STARTED` cycling while a
request is already pending).

**Example:**
```kotlin
// Source: this project's own MainActivity.setupGpsCollection()/checkAndRequestPermission() pattern
// (permissionGranted: MutableStateFlow<Boolean>, repeatOnLifecycle(STARTED)), adapted to CarContext
// per androidx.car.app CarContext.java (androidx-main) requestPermissions() signature/Javadoc.

class SpeedScreen(carContext: CarContext) : Screen(carContext) {
    private val provider = carContext.applicationContext as? TachimetroApplication
    private val denialStore = CarPermissionDenialStore(carContext.applicationContext)
    private var latestState: SpeedState = SpeedState.Searching
    private var permissionState: CarPermissionState = CarPermissionState.NotRequested
    // Guards against double-firing requestPermissions() if STARTED is re-entered while a
    // previous request is still pending an answer (see Common Pitfalls).
    private var requestInFlight = false

    init {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val granted = ContextCompat.checkSelfPermission(
                    carContext, Manifest.permission.ACCESS_FINE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED

                permissionState = resolveCarPermissionState(granted, denialStore.denialCount())
                invalidate()

                if (permissionState is CarPermissionState.NotRequested && !requestInFlight) {
                    requestLocationPermission(carContext) // D-05: automatic, first time only
                }

                if (permissionState is CarPermissionState.Granted) {
                    provider?.gpsSpeedProvider?.state?.collect { state ->
                        latestState = state
                        invalidate()
                    }
                }
                // Waiting/Denied: nothing to collect here -- onGetTemplate() already reflects
                // permissionState; Denied waits for an explicit retry tap (D-06).
            }
        }
    }

    private fun requestLocationPermission(carContext: CarContext) {
        requestInFlight = true
        permissionState = CarPermissionState.Waiting
        invalidate()
        carContext.requestPermissions(
            listOf(Manifest.permission.ACCESS_FINE_LOCATION),
            ContextCompat.getMainExecutor(carContext),
        ) { _, rejected ->
            requestInFlight = false
            permissionState = if (rejected.isEmpty()) {
                CarPermissionState.Granted
            } else {
                val wasAlreadyDenied = denialStore.denialCount() > 0
                denialStore.recordDenial()
                CarPermissionState.Denied(permanent = wasAlreadyDenied)
            }
            invalidate()
        }
    }

    // Called from the retry/open-settings Action's ParkedOnlyOnClickListener -- see Pattern 4.
    fun onRetryOrSettingsClicked(carContext: CarContext) {
        val current = permissionState
        if (current is CarPermissionState.Denied && current.permanent) {
            openAppSettingsFromCar(carContext)
        } else {
            requestLocationPermission(carContext) // D-03/D-06: manual re-trigger only
        }
    }

    private fun openAppSettingsFromCar(carContext: CarContext) {
        // Mirror of MainActivity.openAppSettings(), called on carContext (a Context) instead of
        // an Activity -- CarContext.startActivity(Intent) is NOT overridden/restricted (verified
        // against androidx.car.app source: CarContext extends ContextWrapper with no
        // startActivity(Intent) override), so this is a plain phone-side Activity launch, exactly
        // like requestPermissions() itself does internally to show the system dialog.
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", carContext.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        carContext.startActivity(intent)
    }
}
```

**Note on `onGetTemplate()`:** keep `CarSpeedContent`/`carSpeedContent()` **unchanged** — it stays a pure
`SpeedState -> Row content` mapping, used only once `permissionState is Granted`. Do not fold permission
states into `CarSpeedContent`; they are an orthogonal axis (gates whether `SpeedState` collection even
starts), and conflating them would mix two independent state machines into one sealed class.
`onGetTemplate()` should branch on `permissionState` first, falling through to the existing
`carSpeedContent(latestState)` mapping only in the `Granted` case.

### Pattern 4: Retry/Open-Settings `Action` on the locked `PaneTemplate`

**What:** `Pane.Builder.addAction(Action)` — verified supported on `PaneTemplate` (validated against
`ACTIONS_CONSTRAINTS_BODY_WITH_PRIMARY_ACTION`: max 2 actions, up to 2 with custom titles, 1 may be
primary, `OnClickListener` explicitly allowed, **no icon required**). `CarContext.requestPermissions()`'s
own Javadoc requires wrapping the click handler in `ParkedOnlyOnClickListener` — this is not optional
guidance, it directly documents that calling `requestPermissions()` (or, by extension, any action that
"direct[s] the users to their phones") must only execute while parked; `ParkedOnlyOnClickListener` also
gives a free, host-rendered "only available while parked" message if tapped while driving, with zero
extra app code.

**Example:**
```kotlin
// Source: androidx.car.app.model Pane.java, Action.java, ParkedOnlyOnClickListener.java, and
// ActionsConstraints.java (ACTIONS_CONSTRAINTS_BODY_WITH_PRIMARY_ACTION) — all read directly from
// androidx/androidx (androidx-main branch), car/app/app module.
import androidx.car.app.model.ParkedOnlyOnClickListener

override fun onGetTemplate(): Template {
    val state = permissionState
    val pane = Pane.Builder()

    when (state) {
        is CarPermissionState.Granted -> {
            val row = /* existing carSpeedContent(latestState) branch, unchanged */
            pane.addRow(row)
        }
        is CarPermissionState.NotRequested, is CarPermissionState.Waiting -> {
            pane.addRow(
                Row.Builder().setTitle(carContext.getString(R.string.car_check_your_phone)).build()
            )
        }
        is CarPermissionState.Denied -> {
            val messageRes = if (state.permanent) {
                R.string.car_permission_denied_permanent
            } else {
                R.string.car_permission_denied
            }
            pane.addRow(Row.Builder().setTitle(carContext.getString(messageRes)).build())
            val actionTitleRes = if (state.permanent) R.string.open_settings else R.string.retry
            pane.addAction(
                Action.Builder()
                    .setTitle(carContext.getString(actionTitleRes))
                    .setOnClickListener(
                        ParkedOnlyOnClickListener.create { onRetryOrSettingsClicked(carContext) }
                    )
                    .build()
            )
        }
    }

    return PaneTemplate.Builder(pane.build())
        .setHeaderAction(Action.APP_ICON) // D-07/D-03 Phase 8: unchanged, no branding
        .build()
}
```

**Reused strings, no new resources for button labels:** `R.string.retry` ("Riprova") and
`R.string.open_settings` ("Apri impostazioni") already exist and are already short, generic 1-2 word
labels appropriate for the car screen as-is — no car-specific variant needed for these two (unlike the
longer descriptive messages, which do need shorter car-specific copy per the established D-02 pattern
from Phase 8).

### Anti-Patterns to Avoid

- **Holding a live `MainActivity` reference to query `shouldShowRequestPermissionRationale()`:**
  Contradicts this project's own "never retain an Activity in a long-lived component" convention
  (CLAUDE.md Anti-Patterns) and doesn't help the primary car-first scenario anyway (no `MainActivity`
  instance exists yet). Use the persisted counter (Pattern 1) instead.
- **Calling `CarContext.requestPermissions()` unconditionally from `onGetTemplate()`:** `onGetTemplate()`
  can be invoked repeatedly (every `invalidate()`); an unconditional call there would re-trigger the
  permission dialog every single template rebuild, violating D-06 and likely exhausting whatever quota
  behavior applies to rapid template pushes. Trigger only from the lifecycle-scoped check in `init`, and
  only when `resolveCarPermissionState()` returns `NotRequested`.
  [MEDIUM confidence, WebFetch-summarized from developer.android.com/training/cars/apps/library/request-permissions:
  "Avoid requesting in Screen constructor - Request permissions in onGetTemplate() or when actually
  needed" — this exact phrasing was not found verbatim in the source code itself, treat as a
  general best-practice signal, not a hard API constraint.]
- **Folding permission states into `CarSpeedContent`:** Keep `CarPermissionState` and `CarSpeedContent`
  as two separate, orthogonal sealed models (see Pattern 3 note above).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Showing a "only available while parked" message when the user taps retry while driving | A custom driving-state check + custom message | `ParkedOnlyOnClickListener.create { ... }` | [VERIFIED: androidx.car.app source] The host renders this message automatically — "When the car is not parked, the handler won't be executed and the host will display a message to the user indicating that the action can only be used while parked." Zero app code needed beyond the wrapper. |
| Detecting the current permission grant state | A custom permission-tracking flow | `ContextCompat.checkSelfPermission(carContext, ...)` (already used in the T-08-08 gate being replaced) | Permission grants are app-UID-level, not per-`Context`; the existing check is already correct and needs no car-specific variant. |
| Building a themed/branded permission dialog | Custom Activity UI overriding `CarAppPermissionActivity` | Leave the dialog at its default appearance (D-07, locked) | The library already exposes `androidx.car.app.theme`/`carPermissionActivityLayout` for this if ever needed later — not in scope here, and building a replacement `Activity` is not supported (the component name is pinned to `CarAppPermissionActivity` internally). |

**Key insight:** Every piece of "smart" permission UX this phase needs (parked-only gating, dialog
rendering, phone-side redirect) already has a first-party Car App Library primitive — the only genuinely
custom piece is the denial-count persistence, because Google's own library does not solve that problem
(confirmed by reading `CarAppPermissionActivity`'s full 30-line implementation — it does not attempt to
distinguish first vs. permanent denial at all).

## Common Pitfalls

### Pitfall 1: Automatic request (D-05) can silently no-op while driving, with an undocumented callback outcome

**What goes wrong:** `CarContext.requestPermissions()`'s own Javadoc states: "If this method is called
while the host deems it is unsafe (for example, when the user is driving), the permission(s) will not
be requested from the user." This applies to the *automatic* invocation (D-05) just as much as to a
`ParkedOnlyOnClickListener`-wrapped one — the safety gate is a property of the method itself, not of how
it's called. If a user connects Android Auto while **already driving** (e.g., a passenger connects it,
or a Bluetooth reconnect happens mid-drive), the automatic D-05 request could be silently swallowed by
the host, and the exact behavior of the callback in that scenario (does it fire at all? with what
content?) is **not documented anywhere found in this research** — the source code shows no explicit
"unsafe" check inside `CarContext.requestPermissions()` itself, meaning this gating most likely happens
at the host/OS level once the intent to launch `CarAppPermissionActivity` is dispatched, outside this
app's visibility.

**Why it happens:** The Car App Library's driver-distraction policy applies uniformly to "actions that
direct the user to their phone," regardless of whether the app-side trigger was a tap or an automatic
lifecycle event.

**How to avoid:** Do not assume the "Controlla il telefono" waiting state (D-01) is guaranteed to
resolve on its own. Consider adding a lightweight escape hatch even in the `Waiting` state — e.g., after
some seconds with no callback, allow a tap-to-retry — though CONTEXT.md's D-06 restricts explicit retry
to *after* a denial has been received, not during an unanswered wait. Flag this explicitly to the user
(CONTEXT.md author) as a scenario the current decision set does not fully cover: **if the automatic
request is silently dropped because the car isn't parked, the screen may show "Controlla il telefono"
indefinitely with no path back to a working state until the car is parked and something re-triggers a
fresh `STARTED` re-evaluation** (e.g., disconnecting/reconnecting Android Auto). This is a genuine,
documented platform constraint, not an implementation bug to fix.

**Warning signs:** DHU testing (where the "parked" state is typically simulated as always-parked) will
not reproduce this — it must be tested with the DHU/emulator's explicit "driving" simulation toggle, or
accepted as an untested edge case for this milestone (consistent with how Phase 8 already accepted SC2
GPS-loss as untested-live, per STATE.md Blockers/Concerns).

**Phase to address:** This phase's plan should at minimum document the gap; a full fix (e.g., detecting
"unsafe" host rejection specifically) was not found to be technically possible from the public API
surface in this research pass.

### Pitfall 2: `STARTED` → `STOPPED` → `STARTED` cycling while a request is still pending

**What goes wrong:** `repeatOnLifecycle(STARTED)` cancels and restarts its block on every `STOPPED`
transition. If the car screen briefly leaves `STARTED` (e.g., another screen is pushed transiently) while
a `requestPermissions()` call is awaiting its callback, re-entering `STARTED` would re-evaluate
`resolveCarPermissionState()`, see `NotRequested` still (denial count unchanged, permission still not
granted), and call `requestPermissions()` a **second time** — launching a second
`CarAppPermissionActivity` while the first is still pending.

**Why it happens:** The persisted-counter loop guard (Pattern 1) only prevents re-requesting *after* a
denial has been recorded — it does nothing to prevent a *concurrent* second request before the first one
has resolved.

**How to avoid:** The `requestInFlight` in-memory boolean guard shown in Pattern 3 covers exactly this
case — set `true` before calling `requestPermissions()`, reset in the callback, and skip the automatic
call if already `true`. This is a small, deliberate addition beyond what the persisted counter alone
provides; make sure the plan includes it explicitly rather than relying on the counter alone.

**Warning signs:** Two `CarAppPermissionActivity` instances briefly visible/flickering on the phone in
quick succession during a rapid car-screen navigation stress test.

### Pitfall 3: Denial-counter cross-surface gap (documented limitation, not a defect)

**What goes wrong:** See Pattern 1's "Known limitation" — if a denial happens on the phone
(`MainActivity`) first and a second denial happens on the car screen, the car-side counter only reaches
1 (not 2), so the UI briefly shows "Riprova" once more before self-correcting on the next tap. Already
covered in depth in Pattern 1; listed here for pitfall-tracking visibility.

**Phase to address:** Accept as a known, bounded, honestly-documented tradeoff for this phase — do not
attempt to "fix" it by holding an `Activity` reference (see Anti-Patterns).

### Pitfall 4: `PaneTemplate` content-shape change on state transitions — unverified quota interaction

**What goes wrong:** Phase 8 empirically verified (DHU, live session, D-11 in `08-CONTEXT.md`) that
refreshing a `PaneTemplate`'s **Row title text** every second via `invalidate()` does not exhaust the
5-templates-per-task quota (586 refreshes / 608s, host never closed the app). This phase introduces
transitions between **structurally different** `Pane` contents (a Row-only Pane for
waiting/speed/searching vs. a Row **+ Action** Pane for the denied states) — whether the host's
"refresh vs. step" quota-counting logic treats an `Action` appearing/disappearing on the same
`PaneTemplate` slot as a "refresh" (exempt) or a "step" (counted) was not found documented anywhere,
and Phase 8's verification only tested content-only changes on the same fixed shape (Row title text),
not action-list changes.

**Why it happens:** Same fundamental ambiguity already flagged in `.planning/research/PITFALLS.md`
Pitfall 2 ("whether same-type/same-content refreshes are exempt... is not clearly documented"), now
applied to a shape this project hasn't yet empirically tested.

**How to avoid:** Transitions through the permission states are infrequent per session (not a
continuous 1Hz stream like the GPS reading) — worst case a handful of transitions
(not-requested → waiting → denied → [retry] → waiting → granted). This is a low-frequency, low-risk
scenario relative to Phase 8's already-passed 1Hz stress test, but was not itself stress-tested. A light
manual DHU smoke test of the actual permission flow (deny once, deny twice, grant) is recommended before
considering this phase's DHU verification complete — this is much lighter than Phase 8's 5-10 minute SC4
stress session, just a walkthrough of each state transition.

**Phase to address:** This phase's own verification step, not deferred to Phase 11.

## Code Examples

### Full string resources needed

```xml
<!-- D-01: exact locked text -->
<string name="car_check_your_phone">Controlla il telefono</string>
<!-- D-02: single denial, shortened per the Phase 8 D-02 pattern (exact wording is Claude's
     Discretion; example below mirrors permission_denied's tone at car-appropriate length) -->
<string name="car_permission_denied">Permesso GPS necessario</string>
<!-- D-04: permanent denial, directs to phone Settings -->
<string name="car_permission_denied_permanent">Permesso negato. Apri le impostazioni sul telefono</string>
<!-- retry / open_settings: REUSE existing R.string.retry / R.string.open_settings for Action
     titles -- already short, no car-specific variant needed. -->
```

### Executor + SAM conversion note (Kotlin)

`OnRequestPermissionsListener` and `OnClickListener` (Car App Library) are both single-abstract-method
Java interfaces — Kotlin SAM conversion applies directly, no anonymous object boilerplate needed:
```kotlin
// Source: androidx.car.app.OnRequestPermissionsListener.java, androidx.car.app.model.OnClickListener
// (both confirmed single-abstract-method interfaces by direct source read).
carContext.requestPermissions(
    listOf(Manifest.permission.ACCESS_FINE_LOCATION),
    ContextCompat.getMainExecutor(carContext),
) { approved, rejected -> /* lambda form works directly, no object : OnRequestPermissionsListener */ }

ParkedOnlyOnClickListener.create { /* lambda form works directly here too */ }
```

## State of the Art

Nothing has changed since Phase 8's research — `androidx.car.app:app:1.7.0` remains current stable
(1.7.0, released 2025-07-16 per STACK.md; this research found no newer stable release referenced by
official docs as of this session). No deprecated API used by this phase's design.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | The persisted denial-counter mechanism (Pattern 1) is the best available substitute for `shouldShowRequestPermissionRationale()` from a `Screen` — no better first-party or community-documented alternative was found. | Architecture Patterns / Standard Stack "Alternatives Considered" | If a better mechanism exists but wasn't surfaced by this research (WebSearch found no community precedent for this exact problem), the plan may implement a more complex solution than necessary. Low risk — the mechanism is source-grounded and internally consistent even if not "the" canonical solution. |
| A2 | "Avoid requesting in Screen constructor - Request permissions in onGetTemplate() or when actually needed" (cited under Anti-Patterns) is real official guidance and not a WebFetch summarization artifact. | Architecture Patterns, Anti-Patterns | If this guidance doesn't actually exist as stated, the recommendation to trigger from the lifecycle-scoped `init` block (not raw constructor body, not `onGetTemplate()`) is still sound on its own architectural merits (avoids re-trigger loops from `onGetTemplate()` calls) — low risk either way. |
| A3 | Exact wording for `car_permission_denied` string (D-02) shown in Code Examples is illustrative, not the final locked text — CONTEXT.md explicitly leaves exact wording to Claude's Discretion. | Code Examples | None — explicitly flagged as discretionary in CONTEXT.md; planner/discuss-phase should confirm final copy, not treat the example string as locked. |

## Open Questions

1. **What happens to the `requestPermissions()` callback when the host silently blocks the request for
   "unsafe" driving conditions?**
   - What we know: the Javadoc confirms the block happens; the request activity is launched via a
     plain `startActivity()` call with no visible pre-check in `CarContext.java` itself.
   - What's unclear: whether the callback fires at all in that scenario, and if so with what content
     (empty/rejected?), or whether the `Waiting` state simply never resolves until the car is parked.
   - Recommendation: treat as an accepted, documented gap (Pitfall 1) rather than something this phase
     can fully solve; flag to the user during planning/discuss if not already acceptable as-is.
2. **Does a `Pane` content-shape change (Row-only → Row+Action) count against the 5-templates-per-task
   quota the same way a same-shape content refresh does?**
   - What we know: Phase 8 verified same-shape 1Hz refreshes are quota-safe; this phase's transitions
     are infrequent but structurally different.
   - What's unclear: exact host quota-counting behavior for structural (not just content) changes.
   - Recommendation: light manual DHU smoke test of the full permission flow (Pitfall 4) before
     considering this phase's verification complete.

## Sources

### Primary (HIGH confidence)
- `androidx/androidx` GitHub repository, `androidx-main` branch, `car/app/app/src/main/java/androidx/car/app/` — direct source reads (not summaries) of `CarContext.java`, `CarAppPermissionActivity.java`, `OnRequestPermissionsListener.java`, `model/ParkedOnlyOnClickListener.java`, `model/Pane.java`, `model/Row.java`, `model/PaneTemplate.java`, `model/Action.java`, `model/constraints/ActionsConstraints.java` — used for every API-mechanics claim in this document (requestPermissions signature/behavior, CarAppPermissionActivity's actual implementation, Pane/Action/PaneTemplate support and constraints, ParkedOnlyOnClickListener semantics, `@RequiresCarApi` levels). Fetched from `androidx-main` HEAD, not a pinned `1.7.0` tag (no such tag was resolvable) — flagged MEDIUM-HIGH rather than absolute HIGH for exact-version-match, though this is core, stable API unlikely to have changed materially across recent point releases.
- https://developer.android.com/about/versions/11/privacy/permissions — official Android 11 "deny twice = permanent, OS-enforced automatically" behavior; directly load-bearing for Pattern 1 since this app's `minSdk` is 30 (Android 11), so this applies to 100% of users with no API-level branching needed.
- `.planning/research/PITFALLS.md` Pitfall 4, `.planning/research/STACK.md`, `.planning/research/SUMMARY.md`, `.planning/research/FEATURES.md` — this project's own prior Android Auto research (Phase 8), re-verified and extended, not contradicted, by this session's direct source reads.
- Direct repository code reads: `SpeedScreen.kt`, `MainActivity.kt`, `CarSpeedContent.kt`, `strings.xml`, `MaxSpeedStore.kt`, `MaxSpeedReducerTest.kt`, `CarSpeedContentTest.kt`, `gradle/libs.versions.toml`, `08-CONTEXT.md`, `08-PATTERNS.md` — primary source, not research.

### Secondary (MEDIUM confidence)
- https://developer.android.com/training/cars/apps/library/request-permissions — official guide page, WebFetch-summarized (AI paraphrase, not verbatim quote); cross-checked against the direct source read above wherever the two overlap (signature, executor pattern, version warning) — no contradictions found, only the "don't call in constructor" guidance could not be independently verified in the source itself.
- https://developer.android.com/design/ui/cars/guides/flows/grant-permissions-in-car — Android Automotive OS (native)-focused UX flow guide, not Android Auto (projection)-specific; consulted for general pattern awareness only, explicitly noted to not cover denial/permanent-denial recovery flows.

### Tertiary (LOW confidence, superseded)
- Early WebSearch results suggesting `CarContext.startActivity(Intent)` restricts launches to `CATEGORY_APP_MAPS`/`CATEGORY_APP_MUSIC`/browser/Play-Store intents — **this was investigated and found to be incorrect for the single-argument `startActivity(Intent)` method** once the actual source was read: no such override exists in `CarContext.java` (it inherits the plain `ContextWrapper.startActivity()`); the restriction described by that WebSearch summary appears to actually describe `CarContext.startCarApp(Intent)`, a different method (verified: `startCarApp`'s Javadoc in the source does describe exactly those restrictions — navigate/phone-call/same-app intents only). Recorded here as an explicit correction, per this document's "honest reporting" requirement — do not carry the original (wrong) claim into the plan.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — no new dependency, every API verified present and compatible via direct source read against the already-pinned version.
- Architecture (permission state machine, denial detection): HIGH for the mechanics (source-verified), MEDIUM for "this is the best possible mechanism" (no official/community precedent found to compare against — a reasoned, internally-consistent design, not a copied best practice).
- Pitfalls: MEDIUM-HIGH — the `ParkedOnlyOnClickListener`/driving-safety-gate finding and the quota-interaction question are both genuinely under-documented by Google (confirmed by source reading, not just failure to find docs), correctly flagged as open risk rather than resolved.

**Research date:** 2026-09-02
**Valid until:** 30 days (stable, first-party library API; re-verify if `androidx.car.app` is upgraded past 1.7.0 before this phase is implemented)
