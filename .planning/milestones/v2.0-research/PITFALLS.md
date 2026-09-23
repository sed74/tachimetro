# Pitfalls Research

**Domain:** Adding Android Auto (Car App Library) support to an existing phone-only GPS speedometer app
**Researched:** 2026-08-31
**Confidence:** MEDIUM-HIGH (category/quota/manifest mechanics are HIGH confidence from official docs; a few conclusions about "best fit" for a non-map speedometer are reasoned inferences and flagged LOW/MEDIUM)

## Critical Pitfalls

### Pitfall 1: No Car App Library category cleanly permits a bare "big number" speed display — this can block the whole milestone at Play Store review

**What goes wrong:**
The Car App Library does not have a generic "show custom full-screen content" category. Every `CarAppService` must declare exactly one of a small, fixed set of categories in its manifest intent-filter (`androidx.car.app.category.NAVIGATION`, `POI`, `IOT`, `WEATHER`, `MEDIA`, `MESSAGING`/`CALLING`). Raw `Surface`/`Canvas` drawing access (`androidx.car.app.ACCESS_SURFACE` permission + `SurfaceCallback`) is only granted to **Navigation**, **POI**, and **Weather** categories, via `NavigationTemplate`/`MapWithContentTemplate`. Tachimetro's "no menu, huge number, nothing else" concept does not fit any category's intended purpose:
- **Navigation** requires the app to actually *be* a turn-by-turn navigation app to pass Google Play review (see Pitfall 2's quality criteria: NF-1 turn-by-turn directions, NF-2 "draw only map content" on the navigation Surface, NF-6 handle nav intents, NF-7 test-drive/simulation mode). A Surface that only shows a speed number, with no map and no routing, directly violates NF-2 and would very likely fail the category-specific quality review that gates the **open testing** and **production** tracks.
- **POI** and **Weather** categories exist to show places/weather *on a map*, not an arbitrary number — a plain speedometer doesn't satisfy "meaningful functionality relevant to driving" (PF-1) or actual weather content (WE-1).
- **IoT** apps are restricted to `GridTemplate`/`ListTemplate`/`PaneTemplate` only — no `NavigationTemplate`/`MapWithContentTemplate`, hence **no Surface/Canvas access at all**. There is no evidence a plain GPS speed reading qualifies as "IoT device monitoring" either.
- In practice, industry examples confirm this: apps that show a live speedometer on Android Auto today (Google Maps, Waze) are full navigation apps that render the speed as a supplement to their nav Surface — no standalone "speedometer-only" app was found successfully shipping a custom Android Auto screen outside the Navigation category.

**Why it happens:**
It's easy to assume "Car App Library = a generic canvas I can draw on" by analogy with the existing phone `Surface`/`View` code. In reality the library is templated-and-categorized by design (driver-distraction policy), and category selection has *both* a technical gate (which templates/permissions you're allowed to request) and a Play Store review gate (your app must actually behave like that category).

**How to avoid:**
Before writing any car UI code:
1. Explicitly decide and document which category will be declared, and verify against the current [car app quality guidelines](https://developer.android.com/develop/adaptive-apps/quality-guidelines/car-app-quality) for that category whether a speed-only Surface can pass review as declared.
2. If no category fits cleanly (most likely outcome), decide the actual distribution target for this milestone up front — this is a scope decision, not an implementation detail:
   - **(a)** Restrict the Android Auto feature to internal/closed testing tracks only (Google Play Console car-app review is **non-blocking** for Internal sharing, Internal testing, and Closed testing — only Open testing and Production block on category-quality compliance). If Tachimetro's Android Auto screen is for the developer's own use, shipping via internal/closed testing sidesteps the whole category-fit problem.
   - **(b)** Accept the Navigation category and scope creep into building minimal real navigation (large, likely disproportionate to this project's "minimal tachimeter" philosophy — probably not worth it).
   - **(c)** Fall back to a template-only screen (no custom Surface) inside a category the app can honestly claim (weakest option — see Pitfall 2, since templates can't sustain a smooth 1Hz "giant number" display anyway).
3. Do not assume "it's just a hobby feature" makes review requirements irrelevant if the plan is to publish through the same Play Store listing used today (`playstore/` directory in this repo is kept deploy-ready) — production/open-testing submissions of *any* app that opts into the Android Auto form factor are reviewed against category quality guidelines, even for a small personal app.

**Warning signs:**
- Discovering only at final Play Console submission that "Category not permitted" (a real, documented rejection reason developers hit — see Sources).
- Building the full Surface-drawing implementation before confirming which category/track it will ship under.

**Phase to address:**
**Before any roadmap phase is written** — this is a go/no-go research spike that determines the entire milestone's technical approach (Surface-based vs template-based vs internal-testing-only distribution). Do this before committing to a roadmap.

---

### Pitfall 2: Falling back to templates (List/Pane/Grid) can't sustain a smoothly-updating 1Hz numeric display — the host can kill the app for exceeding template quota

**What goes wrong:**
If Pitfall 1 forces the team away from `NavigationTemplate`'s Surface (e.g. because Navigation-category compliance isn't feasible), the natural fallback is a template like `PaneTemplate`, `ListTemplate`, or `GridTemplate` with a large text row for the speed value. But the Car App Library enforces a **template quota**: the host allows a maximum of 5 templates per "task", and template pushes that change type or "main content" count against that quota; if the quota is exhausted and the app pushes another template, **the host displays an error and closes the app**. Whether repeatedly refreshing a text value every second (same template type, same structure, only the number changes) counts as "same main content" (exempt) or as a new countable template is not clearly documented and is a real risk for a screen whose entire purpose is a number that changes every second, indefinitely, for the whole drive.

**Why it happens:**
Car App Library templates were designed for occasional state changes (route list, media queue, device toggle) — not for a continuously ticking readout. This is a fundamental mismatch between the milestone's core requirement (speed updates every second, indefinitely) and what non-Surface templates are built for.

**How to avoid:**
- Treat "template-only fallback" as a last resort, not a safe default if Surface access isn't available — validate empirically (own testing on DHU, watching the debug overlay counter under Developer Mode) whether same-type/same-content refreshes are actually exempt from quota before relying on this path for a whole milestone.
- If forced into templates, prefer the least frequent update cadence that's still useful (e.g. throttle car-screen refresh rate independently from the phone's 1Hz update, though this trades off the "instantly readable" core value that defines this app).
- Strongly prefer resolving Pitfall 1 (get real Surface access via a compliant Navigation-category implementation or an internal-testing-only distribution) over engineering around the quota.

**Warning signs:**
- App works fine in short DHU test sessions but the host abruptly closes the car screen after a few minutes of continuous driving simulation — check the Developer Mode debug overlay's template counter.

**Phase to address:**
Same research spike as Pitfall 1 (resolve category/Surface access first); if templates end up being the only option, this becomes a design constraint documented before implementation, not discovered during it.

---

### Pitfall 3: Car App Library boilerplate silently opts the app into Android Automotive OS distribution too, not just Android Auto

**What goes wrong:**
The Car App Library targets both **Android Auto** (phone projects UI to the car's screen; app keeps running on the phone) and **Android Automotive OS** (app runs natively inside the car's own Android system, no phone involved) with largely the same `CarAppService`/`Screen` code. Tutorials, codelabs, and starter templates often include manifest entries and `<uses-feature android:name="android.hardware.type.automotive" android:required="false"/>` declarations for both out of the box. If copied wholesale, this can register the app for Automotive OS distribution/review in Play Console even though this milestone (per `.planning/PROJECT.md`) is scoped to Android Auto only — pulling in a second, unplanned review/compatibility surface (different screen sizes, no phone fallback, different permission UX) with no product need.

**Why it happens:**
Most public documentation and codelabs deliberately cover both platforms together since the API surface is shared; it's easy to not notice which manifest pieces are Auto-specific vs AAOS-specific.

**How to avoid:**
- For Android Auto only, declare `<meta-data android:name="com.google.android.gms.car.application" android:resource="@xml/automotive_app_desc" />` with `automotive_app_desc.xml` containing `<automotiveApp><uses name="template" /></automotiveApp>` — this is the Android Auto (projected) declaration. Forgetting it means the app **silently never appears** in the Android Auto app list on the phone (no error, just absence) — a classic "looks done but isn't" failure.
- Do not add Automotive OS `<uses-feature android:name="android.hardware.type.automotive">` or opt into the Automotive OS form factor in Play Console unless AAOS support is an explicit, separate requirement.
- In Play Console, only opt into the **Android Auto** form factor (Advanced Settings → Form factors → Add form factor → Android Auto) — verify this is the only one selected.

**Warning signs:**
- App doesn't show up on the phone's Android Auto app list at all after install (missing `automotive_app_desc.xml` declaration) — check this early, don't wait until DHU testing to notice.
- Play Console shows an "Android Automotive OS" form factor section unexpectedly during submission.

**Phase to address:**
Manifest/scaffolding phase (first Android Auto implementation phase) — verify with a checklist item, not discovered at submission time.

---

### Pitfall 4: Reusing the phone's Activity-based permission-request flow for the car screen instead of `CarContext.requestPermissions()`

**What goes wrong:**
`MainActivity` already has a working `ACCESS_FINE_LOCATION` request flow built around `ActivityResultContracts.RequestPermission` (per this codebase's existing pattern). That flow cannot be reused as-is inside a `Screen`/`Session` in the car app process — a `Screen` has no `Activity` to call `requestPermissions()` on. The Car App Library provides a dedicated API, `CarContext.requestPermissions(permissions, executor, callback)`, which must be used instead. The resulting system dialog is shown **on the phone screen** (not on the car display, and by default with no themed background — must be branded via `androidx.car.app.theme` meta-data + `carPermissionActivityLayout` if a bare dialog is unacceptable). If the developer assumes the same `checkSelfPermission`/`requestPermissions` code from `MainActivity` "just works" from a `Screen`, the permission flow will not compile against the car-app APIs as-is, or (if adapted incorrectly, e.g. trying to launch `MainActivity` for the permission UI) will produce a confusing UX where the driver is bounced to the phone unexpectedly.

**Why it happens:**
Car App Library screens intentionally don't have direct `Activity` access (driver-distraction sandboxing) — this is a genuine API difference from normal Android permission handling, not just a refactor.

**How to avoid:**
- Write a small, car-specific permission-request helper around `CarContext.requestPermissions()` from day one; do not attempt to share `MainActivity`'s `ActivityResultContracts` flow with the car `Screen`.
- Handle the "already granted" case first with a normal permission check (same app, same UID, so `ContextCompat.checkSelfPermission(carContext, ...)` reflects the real grant state) before triggering `requestPermissions()`, so the common case (already granted from using the phone UI first) doesn't show any dialog on the car screen.
- Update to a recent `androidx.car.app` version — `1.7.0-rc01`+ contains fixes for permission-dialog display issues on Android 14+ phones and Automotive OS 15+ crashes; pin a current version rather than whatever a stale tutorial references.
- Design the car screen's first-run state explicitly for "permission not yet granted" — since the dialog appears on the phone, the car screen must show a clear "check your phone" message while waiting, consistent with this app's existing message-driven UI pattern (`SpeedState` sealed states).

**Warning signs:**
- Compile errors trying to reuse `ActivityResultContracts` inside a `Screen`/`Session`.
- Permission dialog never appears when testing purely on the car display (DHU/head unit) — remember to check the phone screen.

**Phase to address:**
Car-screen scaffolding/permission phase (early — this blocks any GPS integration on the car screen).

---

### Pitfall 5: Naively instantiating a second `GpsSpeedProvider`/`FusedLocationProviderClient` for the car `Session` causes duplicate location requests, lifecycle thrashing, and extra battery drain

**What goes wrong:**
`GpsSpeedProvider` currently exposes `state: StateFlow<SpeedState>` shared via `WhileSubscribed()`, scoped to a `CoroutineScope` that `MainActivity` explicitly `close()`s in `onDestroy()`. A `CarAppService`/`Session` has its own, independent lifecycle from `MainActivity` (they can start, stop, and be destroyed at different times — e.g. the phone screen turns off while the car session is still active and connected, or the driver disconnects the cable while the phone Activity is still in the foreground). If the car `Session` naively constructs its **own** `GpsSpeedProvider` instance (or its own `FusedLocationProviderClient.requestLocationUpdates()` call) instead of sharing the existing one, the app ends up with two independent GPS subscriptions running simultaneously at the same 1Hz interval — doubling location-related battery/CPU cost for no benefit, and risking two slightly different `SpeedState`s (e.g. different staleness timers) shown on phone vs car screen at the same instant.

Conversely, if a shared singleton is used but its `CoroutineScope` is tied to whichever lifecycle happens to stop first (e.g. still scoped to `MainActivity.lifecycleScope`), then backgrounding the phone Activity while Android Auto is still connected and actively displaying speed will kill the shared GPS flow entirely — the car screen goes stale/no-signal even though driving is still happening and the car display is the primary thing being looked at.

**Why it happens:**
The existing architecture (Activity-scoped `GpsSpeedProvider`, closed in `onDestroy()`) was correct for a single-screen phone app but was never designed to be shared across two independent consumers with different lifecycles.

**How to avoid:**
- Promote `GpsSpeedProvider`'s ownership to something that outlives both `MainActivity` and any single car `Session` — e.g. an application-scoped singleton (custom `Application` class or a simple object holder keyed off `applicationContext`) with its own `CoroutineScope` that is *not* tied to either UI lifecycle, started once and shared by both consumers via the existing `StateFlow`.
- Both `MainActivity` and the car `Screen`/`Session` should be pure *collectors* of the shared `StateFlow`, never independent owners of the location subscription — consistent with the existing `WhileSubscribed()` pattern, which already supports multiple collectors without extra location requests as long as they share the same upstream instance.
- Decide explicitly whether the shared provider should keep running (and thus keep draining battery) when neither the phone screen nor the car screen is actively visible (e.g. phone locked, Android Auto disconnected) — `WhileSubscribed()` naturally stops the upstream when collector count hits zero, which is desirable, but only if both consumers correctly stop collecting when they should.

**Warning signs:**
- Two separate `Log`/breakpoint traces of `requestLocationUpdates()` firing when only one is expected.
- Car screen shows "no signal" immediately after the phone screen turns off, even though GPS is fine and the car display is what's actually being watched.
- Noticeably higher battery drain during Android Auto sessions compared to phone-only use, beyond what continuous car-display rendering alone would explain.

**Phase to address:**
GPS/data-sharing integration phase — this is core architecture work that should happen right after the category/Surface decision (Pitfall 1) and before UI polish; get this wrong and both screens (existing phone screen and new car screen) risk regressing.

---

### Pitfall 6: Assuming location updates keep flowing to the car screen when the phone is locked/backgrounded — background location restrictions may apply differently than expected

**What goes wrong:**
Android 10+ restricts location access for apps running in the background unless the app holds `ACCESS_BACKGROUND_LOCATION` or is actively in the foreground (a visible Activity, or a foreground service with the `location` foreground-service type). This project currently only requests `ACCESS_FINE_LOCATION` (no background variant — an explicit, documented project constraint, "Phase 2 threat model T-02-EP: no additional permission scope"). It is *not officially documented* by Google exactly how a bound `CarAppService` (kept alive by the Android Auto host's binding while the car screen is actively displayed) is treated for background-location purposes when the phone's own Activity (`MainActivity`) is stopped/not visible (e.g. screen off, phone in a pocket, user on a different phone app). This is a real, currently under-documented gap — do not assume "the car screen is visible on the car's display, so the app counts as foreground" without verifying, since Android's foreground/background classification is about the *app process's* state on the *phone*, not what's visible on an external projected screen.

**Why it happens:**
Android Auto is a projection model — the car screen isn't a separate physical device with its own Android instance; it's rendered by the phone's app process. Standard Android background-location rules were written before this projection model existed and Google's official docs don't clearly cross-reference the two topics.

**How to avoid:**
- Explicitly test the real-world scenario early: phone screen locked/off, Android Auto still connected and the car screen showing the speed template, driving (or DHU-simulated) for several minutes — confirm speed updates keep arriving. Do this on a real device with a real drive or GPS mock, not just DHU (DHU is a rendering test, not necessarily a faithful background-location test).
- If updates stop when the phone is locked, the options are: keep the phone screen on/unlocked during Android Auto use (conflicts with the "release screen-on" behavior mentioned as an open question in `.planning/PROJECT.md`), add `ACCESS_BACKGROUND_LOCATION` (a real permission-scope expansion this project has so far deliberately avoided), or rely on whatever foreground exemption the Android Auto host binding provides (unverified — do not assume this without testing).
- Flag this explicitly as a requirement to validate with the Play Console's own guidance and real-device testing, not training-data assumptions — this is genuinely LOW confidence from documentation alone and needs empirical verification.

**Warning signs:**
- Car screen freezes on the last known speed or goes to "no signal" specifically when the phone screen times out/locks, but works fine while the phone screen stays on.

**Phase to address:**
GPS/data-sharing integration phase, verified with a dedicated real-device test task before considering the milestone done — this directly affects whether the existing "screen always on" toggle behavior needs to change when Android Auto is connected (an open question already flagged in `.planning/PROJECT.md`).

---

### Pitfall 7: DHU (Desktop Head Unit) setup friction blocks local development iteration on Windows

**What goes wrong:**
The DHU is the only way to test Android Auto's rendered UI on a development machine, and it requires a **real phone connected via USB** — there is no way to test Android Auto rendering purely on an emulator (the separate "Android Automotive OS emulator" is a different product for a different distribution target, not a substitute). On Windows specifically, common failure points include: WinUSB driver conflicts with ADB when using `--usb` accessory mode (requiring `adb kill-server` first, or falling back to ADB-tunnel mode via `adb forward tcp:5277 tcp:5277`), the phone screen needing to be **unlocked** for DHU to connect at all (a silent failure otherwise), and Android Auto's own "Developer Mode" needing to be enabled separately (tap the version number repeatedly in the Android Auto app's Settings, then explicitly toggle "Enable developer mode" and "Start head unit server").

**Why it happens:**
DHU is a lightly-maintained developer tool bridging a desktop process to a phone's Android Auto host app over USB/ADB — this is inherently more fragile than a normal emulator, and Windows-specific USB driver behavior isn't the primary platform Google tests DHU against.

**How to avoid:**
- Budget explicit setup time for DHU before the first "does it render" milestone checkpoint; don't assume it works on the first try.
- Prefer the ADB-tunnel connection method (`adb forward tcp:5277 tcp:5277` + `desktop-head-unit.exe`) as the more stable fallback if `--usb` mode fails on Windows.
- Keep the phone screen unlocked during DHU sessions; if DHU fails to discover the phone, try `adb kill-server` before retrying, and confirm `libusb` DLLs remain alongside `desktop-head-unit.exe` in the SDK's `extras\google\auto\` directory.
- Treat DHU as a **rendering/interaction** test tool, not a substitute for real-device testing of GPS behavior, background-location behavior (Pitfall 6), or Play Store review compliance (Pitfall 1) — those need a real head unit or real car (or at minimum a real device doing a real/mocked drive).

**Warning signs:**
- DHU window stays blank after connecting — often a stale head-unit-server state on the phone; stop the server, close DHU, restart the server, relaunch DHU.
- "Device not found" specifically with `--usb` — switch to ADB-tunnel mode rather than debugging USB drivers extensively.

**Phase to address:**
Very first Android Auto scaffolding/dev-environment phase — resolve this before writing any car-screen UI logic so later phases aren't blocked mid-implementation by tooling issues.

---

### Pitfall 8: Assuming Play Store submission-track behavior is uniform — internal/closed testing is a materially different (and much more forgiving) path than open testing/production

**What goes wrong:**
Teams sometimes design their whole rollout plan around "we'll have to pass the full car-app quality review," when in fact Google Play only **blocks** submission for category-quality non-compliance on the **Open testing** and **Production** tracks. **Internal sharing**, **Internal testing**, and **Closed testing** tracks are not blocked by car-app category review (non-compliance may be *noted* on closed testing, but doesn't reject the build). Conversely, some teams assume internal-testing-only distribution is a permanent workaround with no future obligations — but if this app is ever meant to reach production (public Play Store availability, which the existing `playstore/` deploy workflow in this repo implies is the long-term intent), the Pitfall 1 category problem must eventually be resolved for real, not just avoided during development.

**Why it happens:**
"Passed testing" is treated as one monolithic gate, when Play Console's car-app review is explicitly track-dependent.

**How to avoid:**
- Decide explicitly, as part of the Pitfall 1 research spike, whether this Android Auto feature is meant to ever reach the production track (public Play Store) or will remain on internal/closed testing indefinitely (e.g. because it's primarily for the developer's own car). This decision changes how urgently the category-fit problem needs a real solution vs. can be deferred.
- If production is the eventual goal, treat the category-quality requirements as a hard requirement to design against from the start, not a "fix it before submission" afterthought.

**Warning signs:**
- Milestone considered "done" after DHU/internal testing looks correct, without ever validating against the actual category quality guidelines that would apply at open/production submission.

**Phase to address:**
Same research spike as Pitfall 1 — this is a distribution-strategy decision, not an implementation phase.

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|-----------------|------------------|
| Declare `androidx.car.app.category.IOT` (or another loosely-fitting category) just to unlock a template, without matching functionality | Unblocks development quickly, no need to build real navigation | Review rejection risk at open/production track; possible removal from Play Store if flagged post-launch | Only for internal/closed testing tracks that will never be promoted to production as-is |
| Give the car `Session` its own independent `GpsSpeedProvider`/location client instead of sharing the phone's | Simpler to write in isolation, no shared-state coordination needed | Doubled location requests, battery drain, divergent state between phone and car screens (Pitfall 5) | Never — even an MVP car screen should share the existing provider |
| Skip explicit "phone locked / background" testing during development, rely only on DHU | Faster iteration loop | Silent GPS staleness in the exact real-world scenario (driving, phone in pocket) this app is built for | Never for the final milestone checkpoint; acceptable only for early rendering-only spikes |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|-----------------|-------------------|
| Car App Library manifest setup | Forgetting `automotive_app_desc.xml` (`<automotiveApp><uses name="template" /></automotiveApp>`) referenced via `com.google.android.gms.car.application` meta-data | App silently never appears in the Android Auto app list — verify this explicitly on first DHU run, don't assume manifest is complete |
| Permission requests from car `Screen` | Trying to reuse `MainActivity`'s `ActivityResultContracts` permission flow | Use `CarContext.requestPermissions()`; dialog renders on the phone screen, not the car display |
| Play Console Android Auto opt-in | Assuming Android Auto and Android Automotive OS are the same form-factor toggle | They are separate form factors in Play Console Advanced Settings; only opt into Android Auto for this milestone |
| Template refresh for the speed value | Assuming `Screen.invalidate()` can be called every second indefinitely without consequence on non-Surface templates | Verify template-quota behavior (5 templates/task cap) empirically via the Developer Mode debug overlay before relying on a template-only fallback |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|-----------------|
| Duplicate `FusedLocationProviderClient` subscriptions (phone + car each own one) | Elevated battery drain specifically during Android Auto sessions vs. phone-only use | Share one application-scoped `GpsSpeedProvider` instance/`StateFlow` between both consumers | Immediately noticeable on any real drive with Android Auto connected, not just at scale |
| Continuous per-second `Screen.invalidate()` on a non-Surface template | App abruptly closed by host mid-session ("template quota exceeded") | Confirm quota-exempt refresh behavior before committing to template-only fallback; prefer Surface-based rendering | Triggers after quota is exhausted — could be minutes into a drive, easy to miss in short manual tests |
| GPS scope tied to `MainActivity.lifecycleScope` shared with car `Session` | Car screen goes stale exactly when phone screen turns off, despite active driving | Move `GpsSpeedProvider` ownership to an application-scoped singleton, independent of either UI lifecycle | As soon as the phone screen times out or the user manually locks it while Android Auto stays connected |

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| Expanding to `ACCESS_BACKGROUND_LOCATION` reflexively to "fix" Pitfall 6 without confirming it's actually needed | Unnecessary permission-scope expansion (a deliberate anti-pattern this project has avoided since Phase 2's threat model), extra Play Store data-safety disclosure burden | Empirically test whether the existing foreground/bound-service behavior is sufficient before adding background location; treat it as a last resort, not a default fix |
| Declaring `ACCESS_SURFACE`/`NAVIGATION_TEMPLATES`-style permissions "just in case" for categories the app doesn't actually implement correctly | Increases the app's declared permission/category surface without matching functionality, raising review scrutiny for no benefit | Only declare the permissions and category that match the category actually implemented and validated against quality guidelines |

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-------------------|
| Assuming template-based rows can visually replicate the phone app's "huge, dominant number" identity | Car screen speed value ends up small/constrained by host-controlled row styling, breaking this app's Core Value (instant readability) on the one screen where it matters most while driving | Prioritize resolving Surface access (Pitfall 1) specifically because it's the only path to a large, custom-styled number; treat template fallback as a degraded experience, not equivalent |
| No explicit "waiting for permission on your phone" state on the car screen | Driver sees a blank/stuck car screen with no explanation while the permission dialog is actually showing on the phone | Reuse this app's existing message-driven state pattern (`SpeedState`-style) to show a clear car-screen message pointing at the phone during the permission wait |
| Not accounting for the host's forced dark-theme / safe-area rules when porting the phone's black-background high-contrast design | Custom Surface drawing that ignores `CarContext.isDarkMode()` or draws into the host's reserved "stable area" gets clipped or looks inconsistent with host chrome | Explicitly implement `onVisibleAreaChanged()`/`onStableAreaChanged()` handling and dark-theme awareness from the first Surface implementation, not as a later fix |

## "Looks Done But Isn't" Checklist

- [ ] **App appears in the Android Auto app list on the phone:** Often missing because `automotive_app_desc.xml` / the `com.google.android.gms.car.application` meta-data entry was never added — verify on a real DHU/head-unit run, not just by reading the manifest.
- [ ] **Speed updates keep flowing when the phone is locked/backgrounded:** Often missing — DHU sessions are typically run with the phone screen on and unlocked, masking a background-location gap (Pitfall 6) that only appears on a real unattended drive.
- [ ] **Only one location subscription is active during an Android Auto session:** Verify with logging/breakpoints that the car screen is a collector of the existing shared `GpsSpeedProvider`, not a second independent subscriber (Pitfall 5).
- [ ] **Category declared in the manifest matches what was actually implemented and reviewed against:** Verify against the current [car app quality guidelines](https://developer.android.com/develop/adaptive-apps/quality-guidelines/car-app-quality) for that specific category, not just "it compiles and renders in DHU."
- [ ] **Permission-request path works when permission was never granted before:** Test a fresh install where location permission hasn't been granted on the phone yet, launched car-screen-first — confirm the phone-side dialog appears and the car screen shows a sensible waiting state.

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|----------------|------------------|
| Category mismatch discovered late (app built, rejected at review) | HIGH | Re-scope: either restrict distribution to internal/closed testing tracks indefinitely, or redesign the car screen to genuinely satisfy a viable category's requirements — likely a significant rework, not a quick fix |
| Template quota exhaustion discovered late (app closes mid-drive) | MEDIUM | Reduce refresh frequency or restructure to a single, quota-exempt content-only refresh path; if that's insufficient, this forces the Pitfall 1 category/Surface decision to be revisited |
| Duplicate location subscriptions discovered late (battery complaints) | LOW-MEDIUM | Refactor `GpsSpeedProvider` ownership to an application-scoped singleton shared by both `MainActivity` and the car `Session`; contained, well-isolated change given the existing pure-function/StateFlow architecture |
| Background-location staleness discovered late (car screen freezes when phone locks) | MEDIUM | Decide between forcing "keep phone screen on during Android Auto" behavior vs. adding `ACCESS_BACKGROUND_LOCATION` — both are scoped, well-understood changes but require revisiting the existing screen-on preference logic |

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|-------------------|----------------|
| No category fits a bare speed display (Pitfall 1) | Pre-roadmap research spike | Written decision: target category + distribution track (internal/closed vs. open/production), checked against current official quality guidelines for that category |
| Template quota can't sustain 1Hz updates (Pitfall 2) | Same pre-roadmap spike | Empirical DHU test with Developer Mode debug overlay showing template count over several minutes of simulated driving, if template fallback is chosen |
| Accidental Android Automotive OS opt-in (Pitfall 3) | First Android Auto scaffolding phase | Manifest review: only Android Auto form factor declared in Play Console; `automotive_app_desc.xml` present and correct |
| Reused phone permission flow breaks in car `Screen` (Pitfall 4) | Car-screen scaffolding/permission phase | Fresh-install test with permission not yet granted, launched car-first; confirm phone-side dialog + car-screen waiting state |
| Duplicate location subscriptions / lifecycle mismatch (Pitfall 5) | GPS/data-sharing integration phase | Logging/instrumentation confirms exactly one active `requestLocationUpdates()` call regardless of how many collectors (phone + car) are active |
| Background location staleness (Pitfall 6) | GPS/data-sharing integration phase, verified before milestone close | Real-device test: phone locked/backgrounded, Android Auto connected, confirm speed keeps updating over several minutes |
| DHU/dev environment friction (Pitfall 7) | First Android Auto scaffolding phase | DHU renders the car screen successfully on the actual Windows dev machine before any further UI work proceeds |
| Track-dependent review assumptions (Pitfall 8) | Same pre-roadmap spike as Pitfall 1 | Written decision on target track, referenced by later phases so "done" criteria match the actual distribution goal |

## Sources

- [Draw maps | Android for Cars | Android Developers](https://developer.android.com/training/cars/apps/library/draw-maps) — Surface/`ACCESS_SURFACE` restricted to Navigation/POI/Weather categories; dark-theme and safe-area requirements. MEDIUM-HIGH confidence (AI-summarized fetch of official docs; core claims cross-checked against other official pages below).
- [Car app quality | Adaptive Apps | Android Developers](https://developer.android.com/develop/adaptive-apps/quality-guidelines/car-app-quality) and [alternate path](https://developer.android.com/docs/quality-guidelines/car-app-quality) — Tier 1/2/3 quality requirements, NF-1/NF-2/NF-6/NF-7 navigation criteria, category-specific review gating. HIGH confidence, official source.
- [Build a navigation app | Android for Cars | Android Developers](https://developer.android.com/training/cars/apps/navigation) — Navigation category manifest/permission requirements, test-drive simulation requirement. HIGH confidence.
- [Set up your project | Android for Cars | Android Developers](https://developer.android.com/training/cars/apps/library/set-up-project) — `CarAppService` manifest structure, category intent-filter strings, `minCarApiLevel` meta-data. HIGH confidence.
- [Build an internet of things app | Android for Cars | Android Developers](https://developer.android.com/training/cars/apps/iot) — IoT apps restricted to `GridTemplate`/`ListTemplate`/`PaneTemplate`, no Surface access. MEDIUM-HIGH confidence.
- [Distribute to cars | Android for Cars | Android Developers](https://developer.android.com/training/cars/distribute) — Play Console Android Auto form-factor opt-in, `automotive_app_desc.xml`, track-dependent review blocking (internal/closed non-blocking, open/production blocking). HIGH confidence.
- [Refresh the contents of a template](https://developer.android.com/training/cars/apps/library/refresh-template) and [Template restrictions](https://developer.android.com/training/cars/apps/library/template-restrictions) — 5-templates-per-task quota, host closes app on quota exhaustion, Developer Mode debug overlay for counting. HIGH confidence.
- [Request permissions | Android for Cars | Android Developers](https://developer.android.com/training/cars/apps/library/request-permissions) — `CarContext.requestPermissions()`, phone-side dialog rendering, `androidx.car.app.theme`/`carPermissionActivityLayout` customization, recommended `androidx.car.app:1.7.0-rc01`+. HIGH confidence.
- [Test using the Desktop Head Unit | Android for Cars | Android Developers](https://developer.android.com/training/cars/testing/dhu) — DHU requires a real phone via USB, Windows WinUSB/ADB gotchas, Developer Mode toggle steps, `adb forward tcp:5277 tcp:5277`. HIGH confidence.
- [Background Location Limits | Android Developers](https://developer.android.com/about/versions/oreo/background-location-limits) and [Request location permissions | Android Developers](https://developer.android.com/develop/sensors-and-location/location/permissions) — General Android background-location restrictions (`ACCESS_BACKGROUND_LOCATION`, foreground-service location type). HIGH confidence for general Android rules; LOW confidence for how they specifically interact with a bound `CarAppService`'s process state — this specific interaction was not found documented and should be verified empirically (flagged explicitly in Pitfall 6).
- [App rejected. Issue found: Category not permitted — Google Play Developer Community](https://support.google.com/googleplay/android-developer/thread/232683714/app-rejected-issue-found-category-not-permitted?hl=en) and [Android Auto Category Not Permitted thread](https://support.google.com/googleplay/android-developer/thread/318441322/android-auto-category-not-permitted?hl=en) — Real-world developer reports of category-related rejections (thread contents behind a login wall for full detail; titles/existence corroborate the category-fit risk described in Pitfall 1). LOW confidence on specifics, MEDIUM confidence that this rejection reason is a real, recurring one worth planning around.
- Industry-practice check (WebSearch): Google Maps and Waze are the notable examples of live speedometer-on-Android-Auto today, both as full Navigation-category apps rendering speed as part of their nav Surface, not as standalone speedometer apps. No standalone non-navigation speedometer app was found with a confirmed, compliant Android Auto Surface implementation. MEDIUM confidence (absence of counter-examples in search results, not an exhaustive audit).

---
*Pitfalls research for: Android Auto / Car App Library integration into an existing single-Activity Kotlin GPS speedometer app*
*Researched: 2026-08-31*
