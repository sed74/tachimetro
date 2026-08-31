# Feature Research

**Domain:** Android Auto (Car App Library) integration for a single-purpose GPS speedometer
**Researched:** 2026-08-31
**Confidence:** MEDIUM-HIGH (official docs + API source verified for platform mechanics; MEDIUM on exact host-rendered visual fidelity and Play Store review outcome, which cannot be verified without an actual submission)

## Executive Framing (read first)

This is not a typical "what features exist in this product category" research — there is no category of "simple speedometer for Android Auto" apps distributed through Google Play. The single most important finding of this research is a **structural constraint**, not a feature list:

**The Car App Library has exactly four sanctioned app categories — `NAVIGATION`, `POI` (point of interest), `IOT`, `WEATHER` — and none of them cleanly describes "show current GPS speed as a big number, no interaction, no destination, no device to control."** This shapes every feature decision below. See "Critical Constraint" section before the feature tables.

## Critical Constraint: No Official Category Fits a Passive Speedometer

Verified against current official docs (developer.android.com/training/cars/apps, developer.android.com/docs/quality-guidelines/car-app-quality, last-updated 2026-06-18):

| Category | Intended purpose | Fit for Tachimetro | Review requirement |
|----------|-------------------|---------------------|----------------------|
| `NAVIGATION` | Turn-by-turn routing to a destination | Poor — app has no routing | **NF-1: must provide actual turn-by-turn directions.** Declaring this category without real navigation is a policy violation. |
| `POI` | Finding/navigating to parking, charging, gas stations | Weak but loosest fit | **PF-1: "must provide meaningful functionality relevant to driving."** Vague enough that a live speed readout could plausibly qualify, but it is not what POI review is designed to evaluate (locations/places), so outcome is uncertain. |
| `IOT` | Controlling/monitoring connected devices (garage door, lights) | Poor — no controllable device | **IT-1** describes viewing device state + one-touch controls; a GPS reading isn't a "device" in the intended sense. |
| `WEATHER` | Location-relevant weather data | Poor — no weather data | **WE-1 through WE-5** require actual weather content, forecast icons, map tiles. |

**Real-world precedent confirms this gap is not theoretical.** Two independent community projects that put OBD/gauge-cluster style live readouts on the Android Auto screen — `aa-torque` (github.com/agronick/aa-torque) and `obd2aa` (extension for Torque Pro) — are **explicitly distributed outside Google Play**, requiring the user to enable Android Auto's hidden **Developer Settings → Unknown sources** toggle to sideload the APK. The aa-torque README states plainly: *"You need to bypass restrictions put in place by Google to get this app working on Android Auto."* This is the closest real precedent to "big number/gauge on the Android Auto screen," and its distribution model is the direct consequence of the category gap above — a passive gauge display doesn't pass the review bar for any sanctioned category when built with a free-form custom canvas.

**Practical implication for this milestone (two viable paths, not a false binary):**

1. **Templated + POI category, Play-Store-safe path (recommended for MVP).** Use standard Car App Library templates (`MessageTemplate`, `PaneTemplate`) rather than a custom-drawn `Surface`. This avoids the `NAVIGATION_TEMPLATES`/`ACCESS_SURFACE` permissions entirely, keeps the manifest declaration to just `POI`, and is defensible under PF-1's loose "relevant to driving" bar. Tradeoff: the car's host app — not this app — controls exact typography/sizing of the displayed text, so the extreme edge-to-edge auto-sized digit from the phone screen (Core Value) cannot be replicated pixel-for-pixel on the car screen. It will still read as "a large, glanceable number" per the car UI's own large-title conventions, but visual fidelity to the phone is approximate, not identical.
2. **Custom Surface + sideload-only distribution.** Would allow closer visual fidelity (full canvas control, same auto-size logic as the phone), but requires the `NAVIGATION` category + `NAVIGATION_TEMPLATES` permission + implementing `NavigationManager.navigationStarted()/navigationEnded()` — which then requires either (a) genuinely misrepresenting the app's function during Play Store review (policy risk, likely rejection or future removal), or (b) never submitting for Play Store car review and relying on Android Auto's "Unknown sources" developer toggle, same as aa-torque/obd2aa. This is technically higher fidelity but abandons Play Store distribution for the Android Auto surface specifically (the phone app itself is unaffected either way — car app review is a separate Play Console declaration from the base app listing).

This is a scoping decision, not something this research resolves unilaterally — it should be an explicit decision point in requirements/roadmap definition, not discovered mid-implementation. Given the project's existing simplicity philosophy (no menus, no unnecessary UI, minimal footprint) and that failing car-category review has no blast radius on the existing phone app listing, **Path 1 (templated, POI category) is the pragmatic recommendation** for a first Android Auto milestone, with Path 2 as a possible future differentiator only if Path 1's visual fidelity proves unsatisfying after real device testing.

## The Phone-Screen-Off Question — Concrete Verdict

**Verdict: releasing `FLAG_KEEP_SCREEN_ON` + showing a neutral "Connesso ad Android Auto" state IS the ceiling of what this app can do. Forcing the phone screen off/locked is not achievable by a normal app, and should not be attempted.**

Grounded in:
- **There is no public Android API to turn the display off.** The only OS-level primitive (`android.permission.DEVICE_POWER`) is a signature-level permission granted exclusively to system/platform apps; third-party apps cannot hold it, full stop.
- **`DevicePolicyManager.lockNow()`** (Device Admin API) can lock the screen, but requires the user to explicitly grant the app Device Administrator status via a dedicated system settings flow — a heavyweight, security-sensitive permission grant that is completely disproportionate to this app's scope and directly conflicts with the project's existing "no unnecessary permissions/UI" constraint. Not recommended under any circumstance for this app.
- **Some OEM Android Auto stacks do auto-dim/blank the phone screen during active projection** (anecdotally reported for some Pixel devices, inconsistent on others per user reports), but this is entirely **OS/OEM-controlled system behavior outside any app's reach** — it cannot be triggered, requested, or relied upon by a third-party app, varies by device manufacturer and Android Auto version, and must not be assumed or designed around.
- **What the app legitimately controls:** its own `FLAG_KEEP_SCREEN_ON` flag on its own window. Releasing it when Android Auto connects simply hands control back to the phone's normal system display-timeout setting — identical to what happens with any other backgrounded/idle app. This is a real, immediate, in-app-controllable behavior change, just not a forced screen-off.

**Detection mechanism (verified from Car App Library source):** `androidx.car.app.connection.CarConnection` is a general-purpose class — its constructor takes a plain `Context` (confirmed via source: `CarConnection.java` in `androidx/androidx` queries a content provider at `content://androidx.car.app.connection` through a standard `ContentResolver`, not something scoped to `CarContext`/`Session`). This means **`MainActivity` can construct `CarConnection(applicationContext)` directly** and observe its `type` `LiveData<Int>` (convertible to a `Flow` for consistency with this codebase's existing Flow-based patterns) without needing to be inside a `CarAppService`. Values: `CONNECTION_TYPE_NOT_CONNECTED`, `CONNECTION_TYPE_NATIVE` (Android Automotive OS), `CONNECTION_TYPE_PROJECTION` (Android Auto — the relevant one here). **Version note:** for apps targeting API 34+ (this project targets 36), `androidx.car.app` must be `1.3.0-beta01` or later, or the app throws on Android 14+ due to unregistered exported-receiver behavior.

## Feature Landscape

### Table Stakes (Users Expect These)

Minimum for the Android Auto integration to feel complete and not broken, given the milestone's stated scope (current speed + no-signal state on the car screen; sensible phone behavior on connect).

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Current speed shown on car screen as large text | This is the entire point of the milestone | MEDIUM | `PaneTemplate` or `MessageTemplate` with a large-title row; host controls exact rendering. Reuses `GpsSpeedProvider`/`mapSpeedToKmh` unchanged. |
| "No GPS signal" state replicated on car screen | Phone already has this state (`SpeedState.NoSignal`/`Searching`); a car screen frozen on a stale number is worse than the phone equivalent, since the driver can't dismiss/refresh it easily | LOW-MEDIUM | `MessageTemplate` is purpose-built for this: icon + short text, no action required. Directly reuses the existing `SpeedState` sealed model — same `when` branch as `MainActivity.updatePlaceholder()`. |
| Car screen updates on same cadence as phone (1/sec) | Consistency with existing product behavior; no reason to diverge | LOW | Car screen calls `invalidate()`/re-issues template on the same `StateFlow<SpeedState>` collection already built for the phone. |
| Location-permission-not-yet-granted handling on the car screen | Edge case but realistic: user could connect Android Auto for the first time before ever opening the phone app and granting `ACCESS_FINE_LOCATION` | MEDIUM | Car App Library has a purpose-built mechanism: `CarContext.requestPermissions()` (standard runtime dialogs cannot render on the head unit). Needs `androidx.car.app` ≥ `1.7.0-rc01` to avoid crashes on Android 14+/Automotive OS 15+. Without this, a first-time-on-car-only user gets a silently broken screen. |
| Declare a car app category + minimum manifest scaffolding | Required by the platform for the app to appear on Android Auto at all | LOW | `POI` category (see Critical Constraint above), `androidx.car.app.minCarApiLevel` meta-data, `CarAppService` + `Session` + `Screen` boilerplate. New dependency: `androidx.car.app:app` (not currently in this project's `libs.versions.toml`). |
| Phone-side: release `FLAG_KEEP_SCREEN_ON` and show neutral connected state when Android Auto connects | Explicit milestone requirement; avoids two screens fighting for "the number" and avoids pointless battery drain keeping the phone lit face-down/in a pocket while the car screen is the one being read | LOW-MEDIUM | Driven by `CarConnection(applicationContext)` observed from `MainActivity` (see verdict above). Revert to prior toggle-derived `FLAG_KEEP_SCREEN_ON` state when connection drops. |
| Day/night theme on the car screen | Car screens auto-switch between light/dark based on ambient light/ignition state; a screen frozen in the wrong theme looks broken next to every other car app | LOW (if templates) / MEDIUM (if custom Surface) | **Effectively free** if using standard templates (Path 1) — the host renders day/night automatically, no app code needed. Only becomes work if a custom `Surface` is used (Path 2), which would require listening for `CarContext` configuration/dark-mode changes and redrawing. Another point in favor of Path 1. |

### Differentiators (Competitive Advantage)

Not required by the platform, but align with the project's existing Core Value and could be worth doing given how small the marginal cost is.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Reuse of exact same pure functions (`mapSpeedToKmh`, `deriveSpeedState`) between phone and car screen | Guarantees the car screen number is never inconsistent with what the phone would show at the same instant — a correctness guarantee, not just code reuse | LOW (given existing architecture) | These are already framework-free pure functions per this project's conventions; nothing car-specific needs to touch them. This is the strongest argument for keeping `CarAppService` in the **default process** (no `android:process` override) so `GpsSpeedProvider` can be a single shared instance rather than needing IPC (AIDL/Messenger) to duplicate GPS state across two processes — a real complexity avoidance, not just a nice-to-have. |
| Minimal, ad-free, single-glance car screen (no branding, no extra rows) | Every real competitor found (Torque, Car Scanner, OBD Fusion, Car Gauge Pro) is a multi-gauge dashboard-configuration tool; none of them is "just the speed, nothing else" | LOW | Directly matches the project's existing anti-menu, anti-clutter philosophy — this is differentiation by *subtraction*, consistent with how the phone screen already works. |
| Custom-drawn, phone-matching auto-size digit on the car screen (Path 2 from Critical Constraint) | Closest possible visual fidelity to the Core Value ("velocità... leggibile istantaneamente") | HIGH | Only viable as sideload-only distribution (see Critical Constraint). Treat as a *future* consideration, not MVP — revisit only if Path 1's host-rendered text proves genuinely hard to read at a glance on a real head unit. |

### Anti-Features (Commonly Requested, Often Problematic)

| Feature | Why it seems appealing | Why problematic | Alternative |
|---------|---------------------------|------------------|-------------|
| Custom `Surface`/canvas drawing declared under `NAVIGATION` category for Play Store distribution | Would give pixel-perfect control matching the phone's auto-size design | Violates `NF-1` (navigation category requires real turn-by-turn routing); real risk of Play Store review rejection or later takedown of the car app declaration; misrepresents the app's function | Use standard templates under `POI` (Path 1); if higher fidelity is truly wanted later, do it as an explicit sideload-only build, not through Play Store |
| Forcing the phone screen off/locked via Device Admin `lockNow()` | Would feel like "true" hand-off to the car screen | Requires Device Administrator grant — a heavyweight, security-sensitive permission flow wildly disproportionate to a speedometer app; conflicts with this project's own "no unnecessary permissions" constraint; Device Admin APis are increasingly restricted/deprecated by Google for non-MDM use cases | Release `FLAG_KEEP_SCREEN_ON` + show neutral connected message; let the phone's own display timeout do its job, same as any backgrounded app |
| Max speed / distance traveled shown on the car screen | "Why not show everything the phone shows?" | Explicitly out of scope per milestone context; also goes against driver-distraction minimization built into the Car App Library's own quality guidelines (glanceable info only) — more numbers on a car screen is a regression, not a feature, for this product's Core Value | Keep max speed and distance phone-only, exactly as scoped |
| Interactive action buttons on the car screen (e.g. a "refresh"/"reset" button) | Feels more "complete" as an app, matches other car apps that always show at least one action | Not required by the platform for `MessageTemplate`/`PaneTemplate` (both can be built with zero actions — informational-only use is a supported, common pattern, e.g. loading/error screens); adds interaction surface and driver-distraction review scrutiny for zero product value in a passive display | Ship a purely informational template; no actions, matching the phone's own "no menu, no interaction" philosophy |
| Full settings screen on the car surface (units, theme, thresholds) | Other car apps often have one | Car App Library's parked-app exception (`PE-1`) *permits* settings/setup activities while parked, it does not *require* them; this app has zero settings to expose on the car screen (everything configurable — like the "always on" toggle — is phone-only and irrelevant to what's shown on the car screen) | No settings screen; nothing to configure on the car side |

## Feature Dependencies

```
Car screen speed display (PaneTemplate)
    └──requires──> GpsSpeedProvider reused in-process (no android:process override on CarAppService)
                       └──requires──> CarAppService declared in same default process as MainActivity

Car screen "no signal" state (MessageTemplate)
    └──requires──> Existing SpeedState sealed model (Searching/Reading/NoSignal) — already built, phase-4/2 era

Car-screen permission handling (CarContext.requestPermissions)
    └──requires──> androidx.car.app >= 1.7.0-rc01
    └──enhances──> Car screen speed display (prevents broken first-run-on-car-only state)

Phone-side "Android Auto connected" neutral state
    └──requires──> CarConnection(applicationContext) observed from MainActivity
    └──requires──> androidx.car.app >= 1.3.0-beta01 (mandatory once targeting API 34+; this project targets 36)
    └──enhances──> existing FLAG_KEEP_SCREEN_ON toggle logic (Phase 5) — must not fight it, must layer on top

Path 2 (custom Surface, phone-matching fidelity)
    └──requires──> NAVIGATION category + NAVIGATION_TEMPLATES permission
    └──conflicts──> Play Store car-app review (NF-1) unless real navigation is added
    └──conflicts──> Path 1 (POI + templates) — pick one distribution strategy, not both, for a single milestone
```

### Dependency Notes

- **Car screen display requires GpsSpeedProvider reused in-process:** the single biggest architecture decision this milestone introduces. Declaring `CarAppService` without `android:process` keeps it in the app's default process, letting a shared `GpsSpeedProvider` instance (or a small app-scoped holder) serve both `MainActivity` and the car `Screen` directly via the same `StateFlow`, with zero IPC code. Declaring a separate process (`android:process=":car"`, sometimes seen in Car App Library samples) would force AIDL/Messenger plumbing to duplicate GPS state across two processes for no benefit in this app's scope — actively avoid this pattern here.
- **Car-screen permission handling enhances car screen speed display:** without it, a user who connects Android Auto before ever launching/granting on the phone gets an app that silently can't read GPS on the car screen with no way to fix it from the car (the car has no runtime permission UI of its own).
- **Phone-side connected state enhances the existing Phase 5 keep-screen-on toggle:** must be additive, not a replacement — when Android Auto disconnects, the toggle's prior phone-only state (from `ScreenOnPreferenceStore`) must resume exactly as before, not reset.
- **Path 2 conflicts with Path 1:** these are alternative distribution/implementation strategies for the same table-stakes feature (car screen speed display), not features to build in parallel. The milestone should pick one (Path 1 recommended) rather than hedge on both.

## MVP Definition

### Launch With (v1 of this milestone)

- [ ] Car screen shows current speed via a standard template (`PaneTemplate`/`MessageTemplate`), `POI` category — why essential: this is the entire stated milestone goal
- [ ] Car screen shows a "no signal" equivalent state — why essential: the phone already models this state; skipping it on the car screen means the car freezes on stale data, a regression vs. the phone
- [ ] `GpsSpeedProvider` reused in-process (no separate `:car` process) — why essential: avoids an entire class of IPC complexity for zero product benefit
- [ ] `CarContext.requestPermissions()` handling for not-yet-granted location — why essential: realistic first-connection edge case, otherwise silently broken
- [ ] Phone releases `FLAG_KEEP_SCREEN_ON` + shows "Connesso ad Android Auto" (or equivalent neutral state) on `CONNECTION_TYPE_PROJECTION` — why essential: explicit milestone requirement, and the concrete, technically-grounded answer to the "can we turn the phone off" question above

### Add After Validation (v1.x)

- [ ] Reassess visual fidelity of the templated car screen on a real head unit / Desktop Head Unit — trigger: if the host-rendered text turns out too small/low-contrast in practice despite `POI` + template approach being policy-safe
- [ ] Revisit whether `PF-1` ("meaningful functionality relevant to driving") actually clears Play Store's car-app review for `POI` — trigger: only knowable after a real submission; if rejected, this becomes an active blocker requiring a distribution-strategy decision, not a silent one

### Future Consideration (v2+)

- [ ] Path 2 custom-`Surface` phone-matching rendering, sideload-only distribution — why defer: high complexity, abandons Play Store distribution for the car surface specifically, only worth it if Path 1's fidelity is proven insufficient after real-world use
- [ ] Android Automotive OS (native, not projected) support — out of scope signal: milestone context and `PROJECT.md` scope this explicitly to Android Auto (projection); Automotive OS is a materially different, much larger commitment (full native OS build, different hardware constraints) not implied by current wording

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| Car screen speed display (templated, POI) | HIGH | MEDIUM | P1 |
| Car screen "no signal" state | HIGH | LOW | P1 |
| In-process GpsSpeedProvider reuse | HIGH (enables everything else cheaply) | LOW | P1 |
| CarContext.requestPermissions() handling | MEDIUM (edge case, but silent failure otherwise) | MEDIUM | P1 |
| Phone-side CarConnection detection + FLAG_KEEP_SCREEN_ON release | HIGH (explicit milestone ask) | LOW-MEDIUM | P1 |
| Day/night on car screen | MEDIUM | LOW (free with templates) | P1 (comes for free, no reason to defer) |
| Path 2 custom Surface fidelity | LOW-MEDIUM (nice, not required) | HIGH | P3 |
| Automotive OS native support | LOW (not requested) | HIGH | P3 (not currently in scope) |

**Priority key:**
- P1: Must have for this milestone's launch
- P2: Should have, add when possible
- P3: Nice to have, future consideration

## Competitor / Precedent Analysis

No true competitor exists in "minimal single-purpose speedometer on Android Auto." The closest precedents are OBD/gauge-cluster apps, all of which are materially different in scope (multi-gauge, configurable dashboards, require external Bluetooth/WiFi OBD hardware) and, critically, differ in distribution model:

| App | How they show live numbers on the car screen | Distribution | Relevance |
|-----|------------------------------------------------|---------------|------------|
| aa-torque (github.com/agronick/aa-torque) | Custom gauge rendering pulling data from Torque Pro | **Sideload only** — requires enabling Android Auto's "Unknown sources" developer setting; explicitly states it bypasses Google's restrictions | Strongest direct precedent for "gauge/number display on AA screen" and for the category-gap problem described above |
| obd2aa (Torque Pro plugin) | Displays Torque Pro PIDs + speed camera warnings on the AA screen | Commercial, distributed via XDA Labs, not confirmed on Google Play | Second independent confirmation of the same pattern — live-data car-screen apps trend toward non-Play distribution |
| OBDLink app | Lists PIDs/values (text list, not graphic gauges) natively on Android Auto | Google Play | Only found example of "native, on-Play, live data on Android Auto" — but uses a plain list, not a large glanceable number; still supports that Play-compliant live data display is achievable when kept simple/templated rather than custom-canvas |
| dashOBD | Described (secondhand, unverified) as first OBD2 app with "full support for Android Auto" | Google Play (unverified categorization) | Not independently confirmed which car category/template it uses; treat as LOW confidence, worth a spot-check if the team wants one more real-world data point before committing to Path 1 |

**Our approach:** closest to OBDLink's pattern (native, Play-compliant, templated, no custom canvas) rather than aa-torque/obd2aa's pattern (custom canvas, sideload-only) — consistent with the Path 1 recommendation above.

## Sources

- [Use the Android for Cars App Library](https://developer.android.com/training/cars/apps) — category list, last updated 2026-06-18 (HIGH confidence)
- [Car app quality guidelines](https://developer.android.com/docs/quality-guidelines/car-app-quality) — NF-1, PF-1, IT-1, WE-1–5, TH-1, MR-1, PE-1 requirement IDs (HIGH confidence)
- [Draw maps / SurfaceCallback](https://developer.android.com/training/cars/apps/library/draw-maps) — Surface access via `MapWithContentTemplate` (Navigation/POI/Weather) and `ACCESS_SURFACE` permission (MEDIUM-HIGH confidence, WebFetch-summarized)
- [Build a navigation app](https://developer.android.com/training/cars/apps/navigation) — NAVIGATION category requirements, `NavigationManager` (MEDIUM-HIGH confidence)
- [Connection API](https://developer.android.com/training/cars/apps/library/connection-api) — `CarConnection`, `CONNECTION_TYPE_*` values, version requirements for API 34+ (HIGH confidence)
- `androidx/androidx` GitHub source via Context7 — `CarConnection.java`, `CarConnectionTypeLiveData.java` (content-provider-based, plain-`Context`-compatible), `MapWithContentTemplate.java` (`@RequiresCarApi(7)`) (HIGH confidence — primary source)
- [Request permissions](https://developer.android.com/training/cars/apps/library/request-permissions) — `CarContext.requestPermissions()`, version requirement `androidx.car.app:1.7.0-rc01+` (MEDIUM-HIGH confidence)
- [github.com/agronick/aa-torque](https://github.com/agronick/aa-torque) — direct precedent, sideload-only distribution model (HIGH confidence, primary source)
- [puderty/obd2aa](https://github.com/puderty/obd2aa) — second independent precedent (MEDIUM confidence, secondhand description)
- General web search on "no public API to turn screen off" / `DEVICE_POWER` permission scope and `DevicePolicyManager.lockNow()` requirements (MEDIUM confidence, multiple independent developer-forum sources agreeing, consistent with well-established Android platform behavior)
- Play Store listings for GPS Speedometer, Torque Pro, Car Scanner, Car Gauge Pro, OBDLink, dashOBD — competitor scan (LOW-MEDIUM confidence, listing descriptions only, not independently verified for exact car-category declarations except where noted)

---
*Feature research for: Tachimetro v2.0 — Android Auto Support*
*Researched: 2026-08-31*
