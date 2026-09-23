# Project Research Summary

**Project:** Tachimetro v2.0 — Android Auto Support
**Domain:** Android Auto (Car App Library) integration into an existing single-Activity, phone-only GPS speedometer app
**Researched:** 2026-08-31
**Confidence:** MEDIUM-HIGH (platform mechanics, manifest requirements, and reuse architecture are HIGH confidence from official docs and direct code reading; the category/compliance question and template-quota behavior for 1Hz updates are MEDIUM/LOW and require empirical validation before committing to a build path)

## Executive Summary

Tachimetro's Core Value — a giant, self-drawn, always-legible number — has no clean home in the Android Auto Car App Library. All four research passes converge independently on the same structural finding: the Car App Library has a fixed set of app categories (NAVIGATION, POI, IOT, WEATHER, MEDIA, MESSAGING/CALLING), and raw `Surface`/`Canvas` drawing access — the only mechanism capable of reproducing the phone's autosize digit — is gated behind NAVIGATION (or, at a higher API-level cost, POI/WEATHER via `MapWithContentTemplate`). Declaring NAVIGATION requires the app to genuinely provide turn-by-turn routing (NF-1/NF-2 quality guidelines) to pass Play Store review on the open-testing/production tracks; a passive speedometer structurally cannot satisfy this. The non-Surface fallback (standard templates like `PaneTemplate`/`MessageTemplate`) is Play-Store-safe under a loosely-fitting category (POI) but is subject to a 5-template-per-task refresh quota whose exemption behavior for a continuously-changing 1Hz numeric value is not clearly documented — this is a real risk for the milestone's core requirement, not a style nitpick.

The recommended approach, consistent across STACK, FEATURES, ARCHITECTURE, and PITFALLS research: resolve the category/rendering-path decision as an explicit, dedicated pre-implementation spike — not something discovered mid-build. Two viable paths exist: (1) templated + POI category, Play-Store-safe, with degraded visual fidelity (host controls text styling; template quota risk for continuous updates needs empirical DHU verification); or (2) NavigationTemplate + Surface, full visual fidelity matching the phone screen, but restricted to internal/closed testing distribution tracks (which are not blocked by category-quality review) since declaring NAVIGATION without real navigation risks rejection or removal on open/production tracks. Given the project's existing Play Store presence and deploy-ready `playstore/` workflow, this decision has real product consequences and should be surfaced to the user before roadmap phases lock in a build order.

Independent of that open question, the architecture work is well-understood and low-risk: promote the existing `GpsSpeedProvider` from Activity-scoped to Application-scoped (a new `TachimetroApplication` class with one `by lazy` property), so both `MainActivity` and the new car `Screen` collect the same `StateFlow<SpeedState>` via the identical `repeatOnLifecycle(STARTED)` pattern already used today. This avoids duplicate GPS subscriptions, battery drain, and state divergence between the two screens — the single most consistently flagged pitfall across all four research files. `CarAppService`/`Session`/`Screen` run in-process (no IPC needed), and both implement `LifecycleOwner`, so no new coroutine idiom is required. The riskiest non-obvious edge cases are: permission requests must use `CarContext.requestPermissions()` (not the Activity-based flow), the car screen must handle "permission not yet granted" as a first-class state (it can be reached before `MainActivity` ever runs), and background-location behavior when the phone is locked/backgrounded while Android Auto is active is genuinely undocumented and needs real-device verification, not just DHU testing.

## Key Findings

### Recommended Stack

Add `androidx.car.app:app:1.7.0` (stable) plus `androidx.car.app:app-testing:1.7.0` for unit/instrumented testing — no other new dependencies are needed; the existing stack (Kotlin Coroutines 1.10.2, Lifecycle Runtime 2.11.0, Play Services Location 21.4.0) is reused as-is. Do **not** add `androidx.car.app:app-automotive` (that's for Android Automotive OS, a different, out-of-scope distribution target). New manifest scaffolding is required: `res/xml/automotive_app_desc.xml`, `com.google.android.gms.car.application` meta-data, `androidx.car.app.minCarApiLevel` (value `1`, for broadest head-unit compatibility), and a `CarAppService` `<service>` declaration with an intent-filter declaring the chosen category. If the Surface path is chosen, also `androidx.car.app.NAVIGATION_TEMPLATES` and `androidx.car.app.ACCESS_SURFACE` permissions.

**Core technologies:**
- `androidx.car.app:app:1.7.0` — Car App Library core (`CarAppService`, `Session`, `Screen`, templates, `SurfaceCallback`) — latest stable, minSdk well below project floor, compatible with existing coroutines/lifecycle stack
- `androidx.car.app:app-testing:1.7.0` — test `Screen`/`Session` without a real host — matches project's existing JVM-test-first convention
- No new supporting libraries — existing `GpsSpeedProvider`, coroutines, and lifecycle infrastructure are reused unmodified

### Expected Features

The platform's driver-distraction category system, not user expectations, is the primary constraint shaping this feature set — there is no true competitor category ("minimal speedometer on Android Auto"); the closest precedents (aa-torque, obd2aa) are sideload-only distributions outside Google Play, confirming the category-gap problem is real-world, not theoretical.

**Must have (table stakes):**
- Current speed shown on car screen as large text (the entire point of the milestone)
- "No GPS signal" / "Searching" state parity with the phone's existing `SpeedState` model — a frozen car screen is worse than the phone equivalent
- Car screen updates on the same 1Hz cadence as the phone
- `CarContext.requestPermissions()` handling for first-connection-via-car (before `MainActivity` ever ran)
- Category + manifest scaffolding (`POI` recommended for MVP — see Architecture Approach)
- Phone releases `FLAG_KEEP_SCREEN_ON` and shows a neutral "Connesso ad Android Auto" state on `CONNECTION_TYPE_PROJECTION` (detected via `CarConnection(applicationContext)`, usable directly from `MainActivity`, no `Session` required)
- Day/night theme on the car screen — free with standard templates (host-rendered), extra work only if a custom Surface is used

**Should have (differentiators):**
- Reuse of the exact same pure functions (`mapSpeedToKmh`, `deriveSpeedState`) between phone and car — guarantees consistency, near-zero marginal cost given existing architecture
- Minimal, ad-free, single-glance car screen — differentiation by subtraction, consistent with the phone app's existing anti-menu philosophy

**Defer (v2+):**
- Custom-drawn, phone-matching auto-size digit via Surface (Path 2) — only worth it if the templated approach's fidelity proves genuinely insufficient on a real head unit, and only via internal/closed-testing distribution
- Android Automotive OS (native) support — explicitly out of scope per milestone framing, a materially larger commitment
- Max speed / distance traveled on the car screen — explicitly out of scope; more numbers on a car screen is a regression for this product's Core Value
- Interactive buttons or a settings screen on the car surface — nothing to configure on the car side, adds distraction-review scrutiny for no value

### Architecture Approach

`TachimetroCarAppService`/`Session`/`Screen` run in the app's own process (no IPC), so they can call directly into existing Kotlin classes. The single architectural change required is promoting `GpsSpeedProvider` from Activity-owned to Application-owned via a new, minimal `TachimetroApplication` class — both `MainActivity` and the new `SpeedScreen` then become pure collectors of one shared `StateFlow<SpeedState>`, relying on the existing `WhileSubscribed()` semantics to ref-count subscribers and start/stop the underlying `FusedLocationProviderClient` correctly regardless of which screen(s) are active. This is a deliberate, small departure from the project's "no DI/ViewModel layer" convention — one plain `by lazy` property, not a framework.

**Major components:**
1. `TachimetroApplication` (NEW) — owns the single, process-scoped `GpsSpeedProvider` instance so both entry points share one GPS subscription
2. `TachimetroCarAppService` / `TachimetroCarSession` / `SpeedScreen` (NEW, `car/` package) — entry point, per-connection session, and car-screen UI; `SpeedScreen` collects the shared `StateFlow` via the identical `repeatOnLifecycle(STARTED)` pattern already used in `MainActivity`
3. Surface renderer or Bitmap/`CarIcon` renderer (NEW, path-dependent) — the actual "draw the number" logic, either raw `Canvas`-on-`Surface` (Path 2, full fidelity, NAVIGATION category) or `Canvas`-on-`Bitmap` wrapped as `CarIcon` inside a template (Path 1, POI category, host-constrained styling)
4. `GpsSpeedProvider`, `SpeedState`, `mapSpeedToKmh` (EXISTING, unmodified) — only the construction site moves from `MainActivity` to `TachimetroApplication`; internal logic requires zero changes

### Critical Pitfalls

1. **No Car App Library category cleanly permits a bare "big number" speed display** — this can block the entire milestone at Play Store review if not resolved up front. Decide category + distribution track (internal/closed testing vs. open/production) as a pre-roadmap research spike, not an implementation detail.
2. **Template fallback (Path 1) may not sustain a smoothly-updating 1Hz numeric display** — the host enforces a 5-templates-per-task quota and closes the app on exhaustion; whether same-type/same-content refreshes are exempt for a continuously-changing number is undocumented and must be verified empirically via DHU + Developer Mode debug overlay before committing to this path for the whole milestone.
3. **Naively instantiating a second `GpsSpeedProvider` for the car session** duplicates GPS polling, drains battery, and risks phone/car showing subtly different values — always share one Application-scoped singleton (Pattern 1 in ARCHITECTURE.md); this is the most consistently flagged pitfall across all four files.
4. **Reusing the phone's Activity-based permission flow for the car screen** does not compile/work — `Screen` has no `Activity`; use `CarContext.requestPermissions()` instead, and design an explicit "check your phone" car-screen state for the wait.
5. **Assuming location updates keep flowing when the phone is locked/backgrounded** — Android's background-location restrictions predate the Android Auto projection model, and how a bound `CarAppService` is classified when `MainActivity` is stopped is not clearly documented; must be verified with a real unattended drive, not just DHU (which typically runs with the phone unlocked, masking this gap).

## Implications for Roadmap

Based on research, suggested phase structure:

### Phase 0 (pre-roadmap): Category & Distribution Decision Spike
**Rationale:** All four research files independently converge on this as a go/no-go decision that determines the entire technical approach — it must be resolved as an explicit product decision, not discovered mid-implementation or silently defaulted by whoever writes the first phase plan.
**Delivers:** A written decision: (a) which category to declare (POI recommended for Play-Store-safe MVP, NAVIGATION only if accepting internal/closed-testing-only distribution), and (b) which distribution track this feature targets (internal/closed testing is non-blocking on category-quality review; open/production is blocking).
**Addresses:** Resolves the "which rendering path" ambiguity blocking every feature in FEATURES.md's MVP list.
**Avoids:** Pitfall 1 (category mismatch discovered at Play Store review) and Pitfall 8 (assuming all submission tracks behave uniformly).

### Phase 1: Plumbing-Only Scaffold
**Rationale:** Validate manifest/category/service-discovery mechanics and DHU tooling before any GPS logic is written, so tooling friction (Pitfall 7) doesn't block later phases mid-stream.
**Delivers:** `TachimetroCarAppService`, `TachimetroCarSession`, a `SpeedScreen` returning a static template (or, as a throwaway spike per Phase 0's decision, a minimal Surface screen), manifest wiring (`automotive_app_desc.xml`, `minCarApiLevel`, category intent-filter), `HostValidator.ALLOW_ALL_HOSTS_VALIDATOR` for now. Verified via DHU that the app is discoverable and renders.
**Uses:** `androidx.car.app:app:1.7.0` from STACK.md.
**Implements:** `TachimetroCarAppService`/`Session`/`SpeedScreen` components from ARCHITECTURE.md.

### Phase 2: Shared-Ownership Refactor
**Rationale:** Core architecture work should land before any car-facing GPS behavior, and should be independently regression-tested against the phone screen (zero visible change expected) before car-side logic depends on it.
**Delivers:** `TachimetroApplication` introduced, `GpsSpeedProvider` construction moved there, `MainActivity` updated to read the shared instance, the now-incorrect `MainActivity.onDestroy()` `.close()` call removed.
**Addresses:** Pattern 1 from ARCHITECTURE.md (application-scoped shared `StateFlow`).
**Avoids:** Pitfall 5 (duplicate GPS subscriptions) and Pitfall 6's scope-tying variant (GPS scope dying with `MainActivity`).

### Phase 3: Real GPS Data on the Car Screen
**Rationale:** Depends on Phase 1 (scaffold exists) and Phase 2 (shared provider exists); this is where the milestone's actual value gets delivered.
**Delivers:** `SpeedScreen`/renderer collecting the shared `state` StateFlow, rendering `SpeedState.Reading(kmh)` via whichever path Phase 0/1 validated, tested on DHU.
**Addresses:** "Current speed shown on car screen" (table stakes, FEATURES.md).
**Uses:** Existing `mapSpeedToKmh`/`deriveSpeedState` pure functions, unmodified.

### Phase 4: Searching / No-Signal Parity + Permission Handling
**Rationale:** Two related "state parity with the phone" concerns that both depend on Phase 3's rendering pipeline being in place; grouped together since both are about degrading gracefully on the car screen.
**Delivers:** `Searching`/`NoSignal` states mirrored from `MainActivity.updatePlaceholder()`; explicit "permission not yet granted, check your phone" car-screen state using `CarContext.requestPermissions()`.
**Addresses:** Table-stakes features from FEATURES.md (no-signal parity, permission edge case).
**Avoids:** Pitfall 4 (reusing the wrong permission flow) and the associated UX pitfall (blank/stuck car screen during permission wait).

### Phase 5: Phone-Side Connection Awareness
**Rationale:** Independent of the car-screen rendering work (uses `CarConnection` directly from `MainActivity`, no `Session` involved) — can be built in parallel with Phases 3-4, but sequenced after Phase 2 since it's phone-side polish, not core car functionality.
**Delivers:** `MainActivity` observes `CarConnection(applicationContext).type`, releases `FLAG_KEEP_SCREEN_ON` and shows a neutral "Connesso ad Android Auto" state on `CONNECTION_TYPE_PROJECTION`, correctly restoring the prior `ScreenOnPreferenceStore`-derived state on disconnect.
**Addresses:** Explicit milestone requirement (phone screen-on behavior on Android Auto connect).
**Avoids:** Regressing the existing Phase 5 (v1.1) keep-screen-on toggle logic — must layer on top, not replace.

### Phase 6: Production Hardening & Real-Device Verification
**Rationale:** Must come last — depends on every prior phase being functionally complete; several of its checks (background-location behavior, host validator) are security/correctness gates that should not be skipped even for an internal-testing-only release.
**Delivers:** Real `HostValidator` (replacing `ALLOW_ALL_HOSTS_VALIDATOR`), real-device test of background-location behavior (phone locked, Android Auto connected, confirm speed keeps updating over several minutes — not just DHU), verification of no crash on rapid connect/disconnect cycling, final "Looks Done But Isn't" checklist pass from PITFALLS.md.
**Avoids:** Pitfall 6 (background-location staleness discovered late) and Anti-Pattern 4/5 from ARCHITECTURE.md (unvalidated host binding, Surface teardown races).

### Phase Ordering Rationale

- Phase 0 must precede everything else — it is a product/policy decision, not a technical one, and changes which templates/permissions every subsequent phase uses.
- Phases 1-2 (scaffold, shared ownership) are prerequisite infrastructure with no user-visible car-screen behavior yet — sequencing them before Phase 3 lets each be regression-tested in isolation (DHU discoverability; phone-screen-unchanged check) rather than debugging both scaffolding and shared-state bugs simultaneously.
- Phases 3-4 build up car-screen behavior incrementally (happy path, then degraded states), mirroring how the phone app's own `SpeedState` model was originally built out.
- Phase 5 is independent of the car-screen rendering pipeline (different API surface: `CarConnection` vs. `Session`/`Screen`) and could run in parallel with Phases 3-4 if the roadmap wants to parallelize, but is sequenced after Phase 2 since it's phone-side, lower-risk work.
- Phase 6 last, because its checks (background-location, host validator, rapid-cycling) are only meaningful once the full feature exists to test.

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 0 (category/distribution spike):** MEDIUM/LOW confidence area — the template-quota exemption behavior for continuously-changing text and the real-world Play Store review outcome for POI-category speed display are both empirically unverified; needs a DHU spike with the Developer Mode debug overlay before committing.
- **Phase 3 (real GPS data / rendering path):** Depends entirely on Phase 0's outcome — if Surface path is chosen, needs research into `SurfaceCallback` threading, `onStableAreaChanged`/dark-mode handling; if template path is chosen, needs research into `CarIcon`/`Bitmap` rendering and template-push semantics.
- **Phase 6 (background-location behavior):** LOW confidence from documentation alone (PITFALLS.md flags this explicitly) — how a bound `CarAppService` is treated for background-location purposes when `MainActivity` is stopped is not documented by Google; needs real-device empirical verification, potentially informing whether `ACCESS_BACKGROUND_LOCATION` becomes necessary (a permission-scope expansion this project has deliberately avoided).

Phases with standard patterns (skip research-phase):
- **Phase 1 (scaffold):** Well-documented, official codelab covers this exact setup step by step.
- **Phase 2 (shared ownership refactor):** Standard Kotlin/Android pattern (Application-scoped singleton, `WhileSubscribed()` StateFlow), already well-understood from the existing codebase's own conventions.
- **Phase 5 (CarConnection):** Documented, straightforward API usable directly from a plain `Context`.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Library/version/manifest facts verified against official Android Developers docs and direct code reading; only the Play Store category-policy conclusion is MEDIUM since real-world review enforcement isn't fully deterministic/published |
| Features | MEDIUM-HIGH | Platform mechanics (categories, permissions, quotas) verified via official docs; exact host-rendered visual fidelity and actual Play Store review outcome cannot be verified without a real submission |
| Architecture | HIGH for platform mechanics (official docs + API reference); MEDIUM for the specific shared-ownership recommendation (sound inference from documented StateFlow/process behavior, not spelled out verbatim in Google's docs); explicitly LOW/flagged on template refresh-quota exemption scope |
| Pitfalls | MEDIUM-HIGH | Category/quota/manifest mechanics are HIGH confidence (official docs); background-location-during-car-session interaction and "best fit" category conclusions are reasoned inferences, explicitly flagged LOW/MEDIUM in the source file |

**Overall confidence:** MEDIUM-HIGH — the technical/architectural "how to wire it up" work is HIGH confidence and low-risk; the "which category, which distribution track, does the template quota actually work for 1Hz updates" cluster is genuinely unresolved by documentation alone and is the correct thing to spend the first roadmap phase on, not to plan around by assumption.

### Gaps to Address

- **Template quota exemption for continuously-changing numeric text:** Not spelled out precisely in official docs whether a Row's changing number every second reliably counts as "same main content" (quota-exempt) indefinitely. Handle via an empirical DHU spike (Developer Mode debug overlay, multi-minute simulated session) in Phase 0/1 before committing to the template path for the whole milestone.
- **Background-location behavior for a bound `CarAppService`:** Google's docs don't cross-reference standard Android background-location restrictions with the Android Auto projection model. Handle via a dedicated real-device test (phone locked, Android Auto connected, multi-minute drive or GPS mock) in Phase 6, not just DHU (which usually runs with the phone unlocked and masks this).
- **Actual Play Store review outcome for POI category with a speed-only display:** Cannot be verified without a real submission; PF-1's "meaningful functionality relevant to driving" language is vague enough to plausibly qualify but not designed with this use case in mind. Handle by targeting internal/closed testing first (non-blocking track) and treating "submit to open/production" as a separate, later, explicit decision — do not assume it will pass.
- **Whether the milestone's eventual distribution goal is production (public Play Store) or personal/internal use only:** This is a genuine open product question flagged by PITFALLS.md (Pitfall 8) that changes how urgently the category-fit problem needs a durable solution. Should be resolved by the user during Phase 0, not assumed by the roadmap.

## Sources

### Primary (HIGH confidence)
- https://developer.android.com/jetpack/androidx/releases/car-app — version table, artifact list, minSdk history
- https://developer.android.com/training/cars/apps/library/set-up-project — manifest metadata, category list, `CarAppService` declaration
- https://developer.android.com/training/cars/apps/library/draw-maps — `SurfaceCallback`, `ACCESS_SURFACE`, category gating
- https://developer.android.com/training/cars/apps/navigation — `NAVIGATION_TEMPLATES` permission, `NavigationManager`
- https://developer.android.com/training/cars/apps/auto — `automotive_app_desc.xml`, `com.google.android.gms.car.application`
- https://developer.android.com/docs/quality-guidelines/car-app-quality (and adaptive-apps mirror) — NF-1/NF-2/NF-6/NF-7/PF-1/IT-1/WE-1-5 requirement IDs
- https://developer.android.com/training/cars/apps/library/request-permissions — `CarContext.requestPermissions()`, version requirements
- https://developer.android.com/training/cars/apps/library/carappservice-session and /lifecycles — process model, `LifecycleOwner` on Session/Screen
- https://developer.android.com/codelabs/car-app-library-fundamentals — most detailed manifest/process-model source
- https://developer.android.com/training/cars/apps/poi and /weather/iot — Surface access boundaries by category
- https://developer.android.com/training/cars/apps/library/connection-api — `CarConnection`, `CONNECTION_TYPE_*` values
- https://developer.android.com/training/cars/testing/dhu — DHU setup, Windows-specific gotchas
- https://developer.android.com/training/cars/distribute — Play Console form-factor opt-in, track-dependent review blocking
- `androidx/androidx` GitHub source (via Context7) — `CarConnection.java`, `MapWithContentTemplate.java` direct source verification
- Direct repository code reading: `MainActivity.kt`, `GpsSpeedProvider.kt`, `AndroidManifest.xml`, `PROJECT.md`

### Secondary (MEDIUM confidence)
- https://developers.google.com/cars/design/create-apps/apps-for-drivers/plan-task-flows — task-flow step quota, refresh-vs-step distinction
- https://developer.android.com/training/cars/apps/library/refresh-template and /template-restrictions — 5-templates-per-task quota mechanics; exemption scope for continuously-changing content explicitly under-documented
- 9to5Google/Android Authority coverage of Google Maps' Android Auto speedometer rollout (2026-07) — corroborating, not authoritative, evidence that live-speed-on-Surface is a navigation-app-only pattern in practice
- WebSearch on Android Auto sideloading/"Unknown sources" limitations for Car App Library apps — community sources (AndroidAuthority, XDA) corroborating each other

### Tertiary (LOW confidence)
- github.com/agronick/aa-torque and puderty/obd2aa — real precedent for sideload-only distribution, but secondhand/single-source description of obd2aa specifically
- Google Play Developer Community threads on "Category not permitted" rejections — titles/existence corroborate the risk; full thread content behind login wall
- Background Location Limits general Android docs cross-referenced against bound `CarAppService` process state — HIGH confidence for general Android rules, LOW confidence for this specific interaction, which is not documented anywhere found during research

---
*Research completed: 2026-08-31*
*Ready for roadmap: yes — with Phase 0 (category/distribution decision spike) as a mandatory first step*
