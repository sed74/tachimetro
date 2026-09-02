# Phase 09 — Security Audit

**Phase:** 09 — Permesso di localizzazione dallo schermo auto
**Plans audited:** 09-01, 09-02, 09-03
**ASVS Level:** 1
**Block on:** critical
**Audit date:** 2026-09-02
**Register authored at plan time:** yes (verification only, no blind scan performed)

## Threat Verification

| Threat ID | Category | Disposition | Status | Evidence |
|-----------|----------|-------------|--------|----------|
| T-09-01 | Tampering | mitigate | CLOSED | `CarPermissionDenialStore.denialCount()` returns `sanitizeDenialCount(prefs.getInt(KEY_DENIAL_COUNT, 0))` — `app/src/main/java/com/sed/tachimetro/car/CarPermissionDenialStore.kt:24`. `sanitizeDenialCount(raw) = if (raw < 0) 0 else raw` — `app/src/main/java/com/sed/tachimetro/car/CarPermissionState.kt:57`. Locked by tests `sanitizeDenialCount_negativeValue_isClampedToZero` and `notGranted_tamperedNegativeDenialCount_sanitizedToNotRequested` — `app/src/test/java/com/sed/tachimetro/car/CarPermissionStateTest.kt:59-85`. |
| T-09-02 | Elevation of Privilege | mitigate | CLOSED | `resolveCarPermissionState(granted, denialCount)` takes `granted` as an independent parameter; the `when` only returns `Granted` on `granted == true`, never as a function of `denialCount` alone — `CarPermissionState.kt:50-54`. Locked by tests `notGranted_zeroDenials_returnsNotRequested`, `notGranted_oneDenial_returnsDeniedNotPermanent`, `notGranted_twoDenials_returnsDeniedPermanent`, `notGranted_fiveDenials_returnsDeniedPermanent` — `CarPermissionStateTest.kt:26-57`. Call sites confirm `granted` is always sourced from `ContextCompat.checkSelfPermission(...)`, never from persisted data — `app/src/main/java/com/sed/tachimetro/car/SpeedScreen.kt:136-142` (`refreshPermissionState()`) and `:163-166` (request callback). |
| T-09-05 | Elevation of Privilege | mitigate | CLOSED | Grepped `SpeedScreen.kt` for `.collect(` / `.collect {`: the only call to `provider?.gpsSpeedProvider?.state?.collect { ... }` is at line 112, inside the `CarPermissionState.Granted ->` branch of the exhaustive `when` (lines 99-124). No other collection of `gpsSpeedProvider.state` exists in the file. `Granted` is only reachable via `resolveCarPermissionState` (T-09-02) or the explicit assignment in the `requestPermissions` callback after `ContextCompat.checkSelfPermission(...) == PERMISSION_GRANTED` (`SpeedScreen.kt:163-168`) — never from `denialStore`. |
| T-09-06 | Denial of Service | mitigate | CLOSED | Grepped `SpeedScreen.kt` for `requestPermissions(`: the only call site is inside `requestLocationPermission()` (line 155). `requestLocationPermission()` is called only from two places: the `NotRequested` branch guarded by `if (!requestInFlight)` (lines 103-107) and `onRetryOrSettingsClicked()`, which itself returns immediately `if (requestInFlight)` (line 188). `requestInFlight = true` is set before the call and reset only inside the callback (lines 154, 159). The persisted counter moves state to `Denied` after refusal (`recordDenial()`, single call site at line 173) and the `is CarPermissionState.Denied -> Unit` branch (line 123) never re-triggers a request automatically. Both independent guards verified present and wired as declared. |
| T-09-07 | Tampering | mitigate | CLOSED | `openAppSettingsFromCar()` builds `Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)` with `data = Uri.fromParts("package", carContext.packageName, null)` and `addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)` — `SpeedScreen.kt:201-207`. No host-supplied or external data is interpolated; `packageName` originates from the app's own `CarContext`. |
| T-09-08 | Elevation of Privilege | mitigate | CLOSED | The single `Action` built in the `is CarPermissionState.Denied ->` branch wraps its click handler in `ParkedOnlyOnClickListener.create { onRetryOrSettingsClicked() }` — `SpeedScreen.kt:267-274`. No other `Action` is constructed anywhere in the file (confirmed: exactly one `addAction(` call site, in the `Denied` branch). No home-grown driving-state logic present. |
| T-09-09 | Information Disclosure | mitigate | CLOSED | `templateLogLabel()` maps every state to a label string containing no location/speed values beyond what Phase 8 already logged (`Speed`, `Searching`, `PermissionNotRequested`, `PermissionWaiting`, `PermissionDenied`, `PermissionDeniedPermanent`) — `SpeedScreen.kt:288-301`. The only `Log.d(...)` call site is wrapped in `if (BuildConfig.DEBUG)` — `SpeedScreen.kt:307-312`. |
| T-09-13 | Information Disclosure | mitigate | CLOSED | Verification commands documented in `09-03-PLAN.md` `<how-to-verify>`/checkpoint task: `adb shell pm clear`, `adb shell pm revoke ... ACCESS_FINE_LOCATION`, `adb logcat -s TachimetroCar`, `adb devices`, `adb shell pidof com.sed.tachimetro`, `adb logcat -b crash -d`. None dump `tachimetro_prefs` contents, coordinates, or any personal identifier; `logcat -s TachimetroCar` is scoped to the app's own diagnostic tag whose only payload is the counter/state label verified under T-09-09. 09-03-SUMMARY.md `## Threat Flags` confirms no command executed during the live DHU session printed personal or location data. |

## Accepted Risks Log

The following threats were dispositioned `accept` or `n/a` at plan time, with justification already documented in the phase plans. Recorded here to close the loop per the audit's `accept` verification method (entry present in this log = CLOSED).

| Threat ID | Category | Component | Justification | Source |
|-----------|----------|-----------|----------------|--------|
| T-09-03 | Information Disclosure | SharedPreferences (`tachimetro_prefs`) | Only new persisted data is a non-negative denial-count integer (`car_location_denial_count`) — no coordinates, identifiers, or personal data. File confirmed `MODE_PRIVATE` (`CarPermissionDenialStore.kt:21`). | 09-01-PLAN.md threat_model |
| T-09-04 | Tampering | string resources | Static resources compiled into the signed APK; no format placeholders; no external input interpolated into `car_check_your_phone` / `car_permission_denied` / `car_permission_denied_permanent`. | 09-01-PLAN.md threat_model |
| T-09-10 | Spoofing | exported `TachimetroCarAppService` with `HostValidator.ALLOW_ALL_HOSTS_VALIDATOR` | Inherited unchanged from Phase 8 (T-08-05); real `HostValidator` allow-list is explicit Phase 11 scope. A non-legitimate host can trigger the permission dialog but cannot grant it — grant still requires user interaction with the phone's system dialog. Manifest confirmed unchanged by this phase (`AndroidManifest.xml` service block, `exported="true"`, no `HostValidator` override present in reviewed files). | 09-02-PLAN.md threat_model |
| T-09-11 | Tampering | permission-dialog customization | D-07: no `androidx.car.app.theme` / `carPermissionActivityLayout` meta-data declared. Confirmed absent from `AndroidManifest.xml` (grepped, zero matches). Dialog remains default system appearance. | 09-01/09-02-PLAN.md threat_model |
| T-09-12 | Elevation of Privilege | public test seam `SpeedScreen.buildTemplate()` | Pure function: takes `permission`/`speed` as parameters, returns a `PaneTemplate`, no mutable-field reads, no permission/GPS/SharedPreferences access (confirmed by reading the function body, `SpeedScreen.kt:216-284`). Does not widen attack surface beyond the already-public `Screen.onGetTemplate()`. | 09-02/09-03-PLAN.md threat_model |
| T-09-14 | Tampering | `adb shell pm clear` during verification | Clears local data on the test device only; explicitly documented side effect in 09-03-PLAN.md checkpoint task; no production data involved. | 09-03-PLAN.md threat_model |
| T-09-15 | Spoofing | unvalidated DHU host (`ALLOW_ALL_HOSTS_VALIDATOR`) | Inherited unchanged from Phase 8 (T-08-05); DHU session is exactly the accepted scenario for the permissive validator. | 09-03-PLAN.md threat_model |
| T-09-SC (×3) | Tampering | package-manager installs | No new dependency introduced by this phase; `androidx.car.app:app`/`app-testing` 1.7.0 already present and evaluated in Phase 8 (T-08-SC). Confirmed: no `build.gradle.kts` changes in scope of files read for this audit. | 09-01/09-02/09-03-PLAN.md threat_model |

## Unregistered Flags

None. `09-03-SUMMARY.md` `## Threat Flags` section explicitly states no new attack surface was found beyond the plan's threat model. `09-01-SUMMARY.md` and `09-02-SUMMARY.md` contain no `## Threat Flags` section (no new surface flagged by the executor during those plans).

## Notes on Verification Method

- All `mitigate` threats were verified against the actual implementation files (`CarPermissionState.kt`, `CarPermissionDenialStore.kt`, `SpeedScreen.kt`, `CarPermissionStateTest.kt`), not against plan intent or SUMMARY narrative.
- Grep evidence for T-09-05 and T-09-06 specifically checked that the sole `collect` call on `gpsSpeedProvider.state` and the sole `requestPermissions(` call site match the declared single-entry-point claims — not just "a match exists somewhere in the file."
- `AndroidManifest.xml` was read directly to confirm no new permission was added (single `<uses-permission>` entry, unchanged `ACCESS_FINE_LOCATION`) and no `androidx.car.app.theme`/`carPermissionActivityLayout` declaration exists (T-09-11).
- Implementation files were not modified during this audit.

## Result

**Threats Closed:** 16/16 (7 mitigate + 8 accept + 1 n/a-group counted as 3 entries; see table above)
**Threats Open:** 0
