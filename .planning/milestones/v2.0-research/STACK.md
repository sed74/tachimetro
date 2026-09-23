# Stack Research

**Domain:** Android Auto integration (Car App Library) for an existing native Kotlin speedometer app
**Researched:** 2026-08-31
**Confidence:** HIGH (library/version/manifest facts verified against official Android Developers docs; MEDIUM on the Play Store category-policy conclusion since it depends on Google's manual review, which is not fully published/deterministic)

## The one decision that drives everything else

The milestone brief already suspected this correctly: **there is no generic "just show a number" template in the Car App Library, and the reason is deeper than a missing template — it's a driver-distraction quota system.**

Verified facts that lead to the recommendation below:

1. Every list/grid/pane/message template is part of a **task flow** with a hard **refresh/step quota** (Google's design docs: "Refreshes are updates to a template's content that don't increment the step count" vs. "Steps" which do, and flows are capped at 5 steps). For `MessageTemplate`/`PaneTemplate`/`ListTemplate`, updating the title/message text repeatedly counts as a new step unless "Adaptive task limits" applies (mainly a parked-state relaxation). **A 1×/second continuously-changing speed value will exhaust this quota almost immediately** — this rules out any plain-template approach as the *primary* speed display.
2. `NavigationTemplate` is explicitly exempt from this step-counting behavior for its content updates ("For Navigation... templates: any content update qualifies [as a refresh]"), and additionally exposes a raw `Surface` you draw on directly with `Canvas` — drawing frames to a `Surface` is **not** a templated object update at all, so it isn't quota-limited the same way. This is the only mechanism in the library that behaves like the phone screen's autosize `TextView`: you control pixels, font size, and redraw cadence yourself.
3. Surface/Canvas access (`SurfaceCallback` + `AppManager.setSurfaceCallback`) is **gated by category**: it is only available to apps declaring `androidx.car.app.category.NAVIGATION` (via `NavigationTemplate`, `NAVIGATION_TEMPLATES` permission) or, since library 1.7, to `POI`/`WEATHER` apps via the newer `MapWithContentTemplate` (`MAP_TEMPLATES` permission). In both cases you additionally need the `androidx.car.app.ACCESS_SURFACE` permission. **No other category (IOT, MEDIA, MESSAGING, CALLING) can obtain a `Surface` at all.**
4. Tachimetro is none of NAVIGATION, POI, or WEATHER — it has no routing, no points of interest, no weather data. Declaring `NAVIGATION` (the only mature, CarAppApiLevel-1-compatible option of the three) to get Surface access means **misrepresenting the app's category** against Google's own car-app quality bar, which requires real turn-by-turn directions, nav-intent handling, and an auto-drive/test-drive simulation (`NavigationManagerCallback.onAutoDriveEnabled`) for **production** track review.

This tension — "the only technical path to the Core Value (a giant, self-drawn, always-legible number) requires a category the app doesn't semantically belong to" — is the single most important thing this research surfaces. It is not a library question, it's a policy question, and it should be made an explicit decision in the roadmap/requirements step, not silently resolved. See **Play Console implications** below for the concrete, low-risk way to build and use it anyway.

## Recommended Stack

### Core Technologies

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| `androidx.car.app:app` | **1.7.0** (stable, released 2025-07-16) | Core Car App Library: `CarAppService`, `Session`, `Screen`, templates, `SurfaceCallback`/`AppManager` | Latest stable release; already includes `MapWithContentTemplate`, `MAP_TEMPLATES`, and `ACCESS_SURFACE` (all introduced across the 1.7.0 alpha/beta cycle, GA in 1.7.0). `androidx.car.app:app-projected` is explicitly documented as *optional* for Android Auto — the base `app` artifact alone is sufficient to build and ship an Android Auto (projected) experience. |
| `androidx.car.app:app-testing` | **1.7.0** | Unit/instrumented testing of `Screen`/`Session` without a real host | Matches the project's existing JVM-test-first convention (`SpeedMappingTest`, `GpsSpeedProviderStateTest`, `MaxSpeedReducerTest`) — lets `Screen.onGetTemplate()` and permission-flow logic be exercised without DHU/emulator. |

**Do not add** `androidx.car.app:app-automotive` — that artifact supplies `CarAppActivity`, which is only needed for **Android Automotive OS** (the built-in car OS, no phone involved). The milestone is explicitly Android Auto (phone-projected); Automotive OS is a different distribution/form factor and out of scope.

### Manifest & Permissions

| Declaration | Value | Purpose |
|-------------|-------|---------|
| `res/xml/automotive_app_desc.xml` | `<automotiveApp><uses name="template" /></automotiveApp>` | Tells the Android Auto host this app has a templated (Car App Library) experience. **Note the correct filename is `automotive_app_desc.xml`, not `car-app-desc.xml`** — the latter name doesn't exist in current docs; this is a common naming confusion worth flagging so nobody spends time searching for the wrong file. |
| `<meta-data android:name="com.google.android.gms.car.application" android:resource="@xml/automotive_app_desc"/>` | on `<application>` | Points the manifest at the descriptor above. |
| `<meta-data android:name="androidx.car.app.minCarApiLevel" android:value="1"/>` | on `<application>` | `NavigationTemplate` + `SurfaceCallback` have been available since **CarAppApiLevel 1** — declaring the lowest level maximizes compatibility with older Android Auto head units. (The newer `MapWithContentTemplate` needs a materially higher level, ~5–7, which is a reason *not* to use it here — see Alternatives.) |
| `<uses-permission android:name="androidx.car.app.NAVIGATION_TEMPLATES"/>` | manifest root | Required to use `NavigationTemplate` at all. |
| `<uses-permission android:name="androidx.car.app.ACCESS_SURFACE"/>` | manifest root | Required to receive a `Surface` via `SurfaceCallback`, in addition to the template-specific permission above. |
| `<service>` for the `CarAppService` with `<intent-filter><action android:name="androidx.car.app.CarAppService"/><category android:name="androidx.car.app.category.NAVIGATION"/></intent-filter>` | manifest | Registers the entry point and declares the app category (gates access to `NavigationTemplate`/Surface — see the policy discussion above). `android:exported="true"` is required for the host to bind to it. |
| Existing `android.permission.ACCESS_FINE_LOCATION` | already declared | No new location permission needed — GPS access is per-app (UID-level), not per-component. See integration section below for the one nuance (cold-start via car before ever opening the phone screen). |

### Supporting Libraries

No new supporting libraries are needed beyond `androidx.car.app:app`. The existing stack (Kotlin Coroutines Core 1.10.2, AndroidX Lifecycle Runtime 2.11.0, Play Services Location 21.4.0) is reused as-is — this is the main architectural win of this milestone (see below).

### Development Tools

| Tool | Purpose | Notes |
|------|---------|-------|
| Desktop Head Unit (DHU) | Early-stage local testing on a dev machine, no physical head unit needed | `developer.android.com/training/cars/testing/dhu`. Fastest iteration loop for template/Surface layout work. |
| Play Console **Internal Test Track** / **Internal App Sharing** | Real-hardware testing on an actual Android Auto head unit, and initial distribution | Confirmed important nuance: Android Auto's phone-side "Unknown sources" developer setting **does not apply to Car App Library apps** — sideloading a `.apk` and expecting Android Auto to run it will not work. The app must come through a trusted channel (Play Store track), and Internal Test Track / Internal App Sharing are the lowest-friction trusted channels that don't require production-level review. |

## Installation

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("androidx.car.app:app:1.7.0")
    androidTestImplementation("androidx.car.app:app-testing:1.7.0")
}
```

```xml
<!-- app/src/main/res/xml/automotive_app_desc.xml -->
<automotiveApp>
    <uses name="template" />
</automotiveApp>
```

```xml
<!-- app/src/main/AndroidManifest.xml additions -->
<uses-permission android:name="androidx.car.app.NAVIGATION_TEMPLATES" />
<uses-permission android:name="androidx.car.app.ACCESS_SURFACE" />

<application>
    <meta-data
        android:name="com.google.android.gms.car.application"
        android:resource="@xml/automotive_app_desc" />
    <meta-data
        android:name="androidx.car.app.minCarApiLevel"
        android:value="1" />

    <service
        android:name=".car.TachimetroCarAppService"
        android:exported="true"
        android:label="@string/app_name">
        <intent-filter>
            <action android:name="androidx.car.app.CarAppService" />
            <category android:name="androidx.car.app.category.NAVIGATION" />
        </intent-filter>
    </service>
</application>
```

## Integration with the existing GpsSpeedProvider/StateFlow architecture

This is the part most likely to go wrong if approached naively, so it's spelled out explicitly.

**Current state (verified by reading the code):** `GpsSpeedProvider` is instantiated *per-Activity-instance* inside `MainActivity.onCreate()` (`gpsSpeedProvider = GpsSpeedProvider(applicationContext)`, `MainActivity.kt:212`), owns its own `CoroutineScope`, and shares its `StateFlow<SpeedState>` via `SharingStarted.WhileSubscribed()` — meaning the underlying `FusedLocationProviderClient` subscription starts/stops with `MainActivity`'s lifecycle. There is currently **no `Application` subclass** in the project (`AndroidManifest.xml` has no `android:name` on `<application>`).

**Why this matters for Android Auto:** `CarAppService`/`Session`/`Screen` run as a separate component with its own lifecycle, independent of `MainActivity` (e.g., the phone screen can be off/backgrounded with only the car screen active, or vice versa). If the car `Screen` naively does `GpsSpeedProvider(carContext)` on its own, you get **two independent `FusedLocationProviderClient` subscriptions** running whenever both screens are visible — double GPS polling, double battery drain, and two independently-filtered speed values that could (rarely) disagree by a beat. This is exactly the kind of duplication the downstream consumer asked to flag as "what NOT to add."

**Recommended fix — promote `GpsSpeedProvider` to an application-scoped singleton:**
- Add a small custom `Application` subclass (new, e.g. `TachimetroApplication`) that lazily constructs a single `GpsSpeedProvider(applicationContext)` instance and exposes it (plain property is enough — no DI framework needed, consistent with the project's "no ViewModel/DI layer" convention).
- `MainActivity` and the new car `Screen` both collect from this **same** `StateFlow<SpeedState>` instead of each owning a provider. `WhileSubscribed()` then correctly reflects "is anyone (phone screen OR car screen) currently looking at this," and the location subscription starts/stops based on the union of both, not either alone.
- `androidx.car.app.Screen` and `androidx.car.app.Session` both implement `LifecycleOwner` (confirmed against the API surface), so the exact same pattern already used in `MainActivity` — `lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { gpsSpeedProvider.state.collectLatest { ... } } }` — carries over to the car `Screen` unchanged. No new coroutine pattern to learn.
- `CarAppService` runs in the same process as the rest of the app by default (no `android:process` override needed), so a plain singleton (not a bound service or cross-process mechanism) is sufficient.

**Permission nuance to get right:** `ACCESS_FINE_LOCATION` is granted at the app/UID level by Android, not per-component — once `MainActivity` has been granted it, `carContext.checkSelfPermission(ACCESS_FINE_LOCATION)` will already report `PERMISSION_GRANTED` with no further prompt. `CarContext.requestPermissions()` is still needed as a **fallback path**, not the primary one: a user could plug into Android Auto before ever launching the phone screen (cold start via car), in which case the car `Screen` must check first and only call `requestPermissions()` if not already granted — mirroring the existing `permissionGranted: MutableStateFlow<Boolean>` pattern in `MainActivity`, just entered from a different door.

**Rendering the "no signal" state:** reuse the existing `SpeedState` sealed model (`Searching` / `Reading(kmh)` / `NoSignal`) unchanged. Inside `SurfaceCallback.onSurfaceAvailable`/redraw, `when (state)` and draw either the big number or the equivalent of "Ricerca segnale GPS..." as centered `Canvas.drawText` with a `Paint` sized relative to `SurfaceContainer` dimensions — the same "giant text, no icons, no menu" visual language as the phone screen, just re-implemented with `Canvas` instead of an autosize `TextView` (templates don't give you an autosize text primitive; you own the layout math here).

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| `MessageTemplate` / `PaneTemplate` / `ListTemplate` as the **primary**, continuously-refreshing speed display | Content updates on these templates count against a per-task-flow step quota (~5 steps) designed to limit driver distraction; a 1×/second changing number will exhaust it almost immediately and the host will stop honoring further updates | `NavigationTemplate` + `SurfaceCallback`, whose content updates are explicitly treated as non-step-incrementing "refreshes," and whose `Surface`/`Canvas` drawing isn't templated at all |
| `androidx.car.app:app-automotive` | Supplies `CarAppActivity` for **Android Automotive OS** (built into the car, no phone) — a different product/distribution surface entirely, not requested by this milestone | `androidx.car.app:app` alone, which is sufficient for Android Auto (projected) |
| A second `GpsSpeedProvider`/`FusedLocationProviderClient` instance constructed inside the car `Screen`/`Session` | Duplicates GPS polling and battery drain, and risks the phone and car screens showing subtly different values for the same instant | One application-scoped `GpsSpeedProvider` singleton shared by `MainActivity` and the car `Screen` (see integration section) |
| `MapWithContentTemplate` (POI/WEATHER category) as a way to "avoid" the NAVIGATION category question | Doesn't remove the fundamental category-mismatch problem (Tachimetro isn't POI or weather content either), and requires a materially higher `minCarApiLevel` (~5-7 vs. 1), narrowing the pool of compatible head units, for no real benefit here | `NavigationTemplate` (CarAppApiLevel 1, mature since library 1.0) — accept the category question head-on rather than reframe it |
| Sideloading the `.apk` to test on a real Android Auto head unit | Android Auto's "Unknown sources" developer setting explicitly does **not** apply to Car App Library apps — this path simply doesn't work for this library, unlike media/messaging/parked apps | Play Console Internal Test Track or Internal App Sharing (still no production review required) |

## Play Console implications (read before writing REQUIREMENTS.md)

This app already ships to Play Store (per project context: `playstore/` release notes, deploy-ready sync). That makes this a live decision, not a hypothetical:

- Google's [car app quality guidelines](https://developer.android.com/docs/quality-guidelines/car-app-quality) impose functional requirements per category. For **NAVIGATION** (Tier 2 "Car Optimized"): turn-by-turn directions (NF-1), handling of navigation intents from other apps (NF-6), and support for a simulated "test drive" via `NavigationManagerCallback.onAutoDriveEnabled` (NF-7) for review purposes. Tachimetro has none of these and structurally can't — it's a passive speed readout, not a router.
- Confirmed nuance that makes this workable in the short term: **the additional car-app manual review only blocks the *production* track.** For closed/internal testing tracks, a non-compliant build is flagged/notified but the submission is still approved (per Google's own review-process documentation). This means the Android Auto screen can be fully built, tested on a real head unit via Internal Test Track / Internal App Sharing, and used personally, without ever tripping the production blocker.
- **Recommendation for the roadmap:** build and validate the whole feature against Internal Test Track first. Treat "declare NAVIGATION category and submit to production" as a separate, explicit, later decision — one that should probably be preceded by either (a) accepting the compliance risk and the possibility of rejection/removal from production, or (b) reconsidering whether a less strict category or a template-only (non-Surface) fallback for production is preferable, accepting the quota limitation described above for a lower-fidelity production experience. Do not let this get decided implicitly by whoever writes the phase plan — flag it as an open question for REQUIREMENTS.md.

## Alternatives Considered

| Recommended | Alternative | When to Use Alternative |
|-------------|-------------|--------------------------|
| `NavigationTemplate` + `SurfaceCallback`, category `NAVIGATION` | `MapWithContentTemplate`, category `POI` or `WEATHER` | Only if the app grows real POI or weather content later — for a bare speedometer it adds a higher `minCarApiLevel` floor without solving the category-mismatch problem |
| Custom `Canvas` rendering on the `Surface` | Standard templates (`PaneTemplate`/`MessageTemplate`) for the whole car experience, sacrificing 1Hz live updates | If the roadmap ultimately decides the policy risk of NAVIGATION category isn't acceptable for production, and a periodically-refreshing (not continuous) speed value is judged good enough — accept a template-only fallback that avoids Surface/NAVIGATION entirely and stays inside any category's default quota by refreshing at a much lower cadence (e.g., only on notable speed changes) |
| One application-scoped `GpsSpeedProvider` singleton | Two independent providers (phone + car) | Never for this project — no scenario in a single-GPS-source speedometer benefits from two independent location subscriptions |

## Version Compatibility

| Package A | Compatible With | Notes |
|-----------|-----------------|-------|
| `androidx.car.app:app:1.7.0` | Tachimetro `minSdk 30` | No conflict — the Car App Library's own minimum is API 21 (moving to API 23 starting 1.8.0-alpha03), well below this project's floor. |
| `androidx.car.app:app:1.7.0` | `compileSdk 36` (already set) | No change needed to compile SDK/target SDK for this milestone. |
| `androidx.car.app:app:1.7.0` | Kotlin Coroutines 1.10.2 / Lifecycle Runtime 2.11.0 (already present) | Both `Session` and `Screen` implement `LifecycleOwner`, so `lifecycleScope` + `repeatOnLifecycle` work exactly as they already do in `MainActivity` — no new coroutine plumbing needed. |
| `androidx.car.app:app-automotive` | N/A | Not adding this artifact — confirmed unnecessary for Android Auto (projected); only relevant if a future milestone targets Android Automotive OS. |

## Sources

- https://developer.android.com/jetpack/androidx/releases/car-app — version table (1.7.0 stable 2025-07-16 confirmed as latest non-prerelease; 1.8.0 in RC as of late Aug 2026), artifact list (`app`, `app-projected`, `app-automotive`, `app-testing`), minSdk history — HIGH confidence
- https://developer.android.com/training/cars/apps/library/set-up-project — manifest metadata (`minCarApiLevel`, category list, `CarAppService` declaration) — HIGH confidence
- https://developer.android.com/training/cars/apps/library/draw-maps — `SurfaceCallback`, `AppManager.setSurfaceCallback`, `ACCESS_SURFACE` permission, category gating (NAVIGATION/POI/WEATHER only) — HIGH confidence, cross-verified across three separate fetches/searches
- https://developer.android.com/training/cars/apps/navigation — `NAVIGATION_TEMPLATES` permission requirement, category declaration in intent-filter — HIGH confidence
- https://developer.android.com/training/cars/apps/auto — `automotive_app_desc.xml` name and contents, `com.google.android.gms.car.application` meta-data — HIGH confidence (corrects the `car-app-desc.xml` filename assumption in the original question)
- https://developer.android.com/docs/quality-guidelines/car-app-quality — Tier 1/2 functional requirements per category (NF-1, NF-6, NF-7 for NAVIGATION), production-vs-closed-track review consequences — MEDIUM-HIGH confidence (policy text verified, but real-world enforcement outcomes can vary and weren't independently confirmed via a rejected-app case study)
- https://developers.google.com/cars/design/create-apps/apps-for-drivers/plan-task-flows — task-flow step quota, refresh-vs-step distinction, NavigationTemplate's refresh exemption — MEDIUM confidence (design-guideline language, cross-checked against a second independent search but not against SDK source code)
- https://developer.android.com/training/cars/apps/library/request-permissions — `CarContext.requestPermissions()`, `checkSelfPermission()` check-first pattern — HIGH confidence, corroborated across two independent searches
- WebSearch (multiple queries) on Android Auto sideloading/developer-mode limitations for Car App Library apps — MEDIUM confidence (community sources — AndroidAuthority, XDA — corroborating each other and consistent with the "trusted source" language in official testing docs, but not itself an official Google statement)
- WebSearch on `Screen`/`Session` implementing `LifecycleOwner` — MEDIUM-HIGH confidence (consistent across independent queries, aligns with known AndroidX conventions)
- Direct code reading: `app/src/main/java/com/sed/tachimetro/MainActivity.kt`, `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt`, `app/src/main/AndroidManifest.xml` — HIGH confidence (primary source, not research)

---
*Stack research for: Android Auto integration (Car App Library) — Tachimetro v2.0 milestone*
*Researched: 2026-08-31*
