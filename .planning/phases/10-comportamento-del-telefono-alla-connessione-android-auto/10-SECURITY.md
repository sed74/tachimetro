# Phase 10 — Security Audit

**Phase:** 10 — Comportamento del telefono alla connessione Android Auto
**Plans audited:** 10-01, 10-02, 10-03
**ASVS Level:** 1
**Block on:** critical
**Audit date:** 2026-09-02
**Register authored at plan time:** yes (verification only, no blind scan performed)

## Threat Verification

| Threat ID | Category | Disposition | Status | Evidence |
|-----------|----------|-------------|--------|----------|
| T-10-01 | Tampering | mitigate | CLOSED | `resolveCarLinkState()` allow-lists a single value: `if (connectionType == CarConnection.CONNECTION_TYPE_PROJECTION) Connected else Disconnected` — `app/src/main/java/com/sed/tachimetro/car/CarLinkState.kt:44-49`. Locked by `CarLinkStateTest.kt`: `notConnected_returnsDisconnected` (:24-31), `native_returnsDisconnected` (:33-41), `nullValue_returnsDisconnected` (:43-47), `negativeValue_returnsDisconnected` (:49-53), `unknownFutureValue_returnsDisconnected` (:55-59) — every non-`PROJECTION` input (`NOT_CONNECTED`, `NATIVE`, `null`, `-1`, `99`) asserted `Disconnected`. |
| T-10-02 | Denial of Service | mitigate | CLOSED | Structural consequence of T-10-01's allow-list: the only branch producing `Connected` requires an exact match on `CarConnection.CONNECTION_TYPE_PROJECTION` (`CarLinkState.kt:44-49`); no other code path in `CarLinkState.kt` or `MainActivity.kt` can reach `Connected`. A spurious/tampered value can only fall to `Disconnected` (v1.1 behavior), never neutralize the speedometer. |
| T-10-03 | Tampering | mitigate | CLOSED | `resolveEffectiveKeepScreenOn(savedPreference: Boolean, link: CarLinkState): Boolean` (`CarLinkState.kt:65-69`) takes the preference by value and returns a derived `Boolean`. Grep confirms `CarLinkState.kt` contains zero occurrences of `ScreenOnPreferenceStore`, `getSharedPreferences`, or `.write(` as executable code (`ScreenOnPreferenceStore` appears once, only inside KDoc prose at line 56) and imports only `androidx.car.app.connection.CarConnection` — no `android.content.Context` import. |
| T-10-04 | Elevation of Privilege | mitigate | CLOSED | `git log -- app/src/main/AndroidManifest.xml` shows the file was last modified by `e788bac` (Phase 8, "scaffolding manifest Android Auto categoria POI"); no Phase 10 commit (`4766a3f`, `c46bd93`, `36db92a`, `d65f91e`, `8fb3d19`, `363dbe7`) touches it. Manifest content confirmed unchanged: single `<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION">`, no new `<queries>` block. |
| T-10-07 | Tampering | mitigate | CLOSED | `grep -c 'CONNECTION_TYPE_' app/src/main/java/com/sed/tachimetro/MainActivity.kt` → 0 matches. The only place the raw connection type is consumed is `carConnection.type.observe(this) { connectionType -> onCarLinkChanged(resolveCarLinkState(connectionType)) }` — `MainActivity.kt:289-291`. |
| T-10-08 | Tampering | mitigate | CLOSED | `screenOnStore.write(` occurs exactly twice in `MainActivity.kt`: the one-time default write at `:218` (`setupScreenOnSwitch()`, first-launch default) and the switch listener at `:231`. `onCarLinkChanged()` (`:296-327`) contains no `.write(` call — the only reference to `screenOnStore.write(` near it is a KDoc-style comment at `:324` stating it deliberately does *not* call it. |
| T-10-09 | Elevation of Privilege | mitigate | CLOSED | `onCarLinkChanged()` (`MainActivity.kt:296-327`) reads `permissionGranted.value` once as a read-only guard (`:316`) and never assigns to `permissionGranted`, never calls `requestPermissionLauncher.launch(...)`, and never starts/stops `gpsSpeedProvider.state` collection (that collection lives exclusively in `setupGpsCollection()`, gated by `repeatOnLifecycle`/`permissionGranted.collectLatest`, `:239-260`, untouched by this phase). |
| T-10-10 | Information Disclosure | mitigate | CLOSED | Exactly one `Log.d(` call in `MainActivity.kt` (`:302`), wrapped in `if (BuildConfig.DEBUG)` (`:299`), payload `"carLink=$carLink savedKeepOn=$savedKeepOn effectiveKeepOn=..."` (`:303-306`) — no speed, distance, or coordinate values. |
| T-10-11 | Denial of Service | mitigate | CLOSED | `carConnection = CarConnection(applicationContext)` (`:283`, WR-04, never the Activity) observed via `carConnection.type.observe(this) { ... }` (`:289`). `onDestroy()` (`:371-383`) contains no manual `removeObserver`/receiver-unregister call for `carConnection` — relies on the documented `LiveData` `onActive()`/`onInactive()` lifecycle contract (cited from the library source in 10-01-PLAN.md interfaces block), consistent with the pattern already used for every other `repeatOnLifecycle(STARTED)` collector in the file. |
| T-10-12 | Denial of Service | mitigate | CLOSED | `renderSpeedArea(state)` re-evaluates `carLink is CarLinkState.Connected` at the top of its body on every call (`:470-484`) and is invoked from `updatePlaceholder()` (`:504`), which itself runs on every `gpsSpeedProvider.state` emission (1 Hz, `:255`). No neutral-state flag is written to `SharedPreferences` or any other disk-backed store anywhere in the phase's diff. |
| T-10-15 | Information Disclosure | mitigate | CLOSED | Same single `Log.d(` call verified under T-10-10: tag `"TachimetroPhone"` (`LOG_TAG`, `:87`), gated on `BuildConfig.DEBUG`, payload limited to `carLink`/`savedKeepOn`/derived boolean — no location data reaches logcat. |
| T-10-16 | Tampering | mitigate | CLOSED | Current `git status --short` shows no modifications under `app/src` (only unrelated untracked files: `.idea/markdown.xml`, a Phase 9 pattern doc). `10-03-SUMMARY.md` records Task 2 (the checkpoint) with "nessun commit di codice" and `git diff --name-only` empty after approval, matching the plan's acceptance criterion. |

### T-10-13 — ACCEPTED (evidentiary gap, not a code gap; risk accepted by user on 2026-09-02)

**Resolution:** Presented to the user after this audit found the gap. The user chose to accept the risk as documented rather than obtain an itemized A1-G1 re-confirmation. Disposition downgraded from `mitigate` to `accept`; also recorded in the Accepted Risks Log below.

| Threat ID | Category | Disposition | Status | Mitigation Expected | Evidence Found |
|-----------|----------|-------------|--------|----------------------|-----------------|
| T-10-13 | Repudiation | ~~mitigate~~ → accept | **CLOSED** (accepted) | `10-03-PLAN.md` `<acceptance_criteria>` requires: *"La persona ha risposto ai singoli punti A1-A6, B1-B5, C1-C3, D1-D4, E1-E3, F1-F3, G1 — non con un'approvazione generica"* (an itemized answer per point, explicitly **not** a generic approval). The threat register's own mitigation text for T-10-13 states verbatim: *"un 'approvato' generico non soddisfa il gate"* (a generic "approved" does not satisfy the gate). | `10-03-SUMMARY.md:52` records the outcome as: "...confermati con risposta 'approvato'" — a single generic word, not an itemized per-point transcript. `10-VERIFICATION.md:107` independently confirms: *"The user responded 'approvato,' confirming every point A1-G1 individually per the plan's `<resume-signal>` contract"* — i.e. the verifier acknowledges the literal response was the single word "approvato" and infers itemized confirmation from it, rather than citing a recorded itemized transcript. No file in the repository (`10-03-SUMMARY.md`, `10-VERIFICATION.md`, `10-REVIEW.md`, or any other phase-10 artifact) contains a point-by-point (A1, A2, A3, ... G1) transcript of individual answers. Per this audit's evidentiary standard ("do not accept documentation or intent as evidence"), a narrative claim that a generic reply "confirmed every point individually" does not constitute the itemized-response artifact the plan's own acceptance criteria and threat model require. |

This is not a code vulnerability — `CarLinkState`/`MainActivity` are unaffected — but the phase's own threat model treats the checkpoint's repudiation risk as a `mitigate`-disposition threat with a specific, falsifiable control (itemized responses), and the recorded evidence does not meet that control's own bar. Per `<adversarial_stance>`, a narrative rationalization by a downstream reviewer does not substitute for the itemized-response artifact the mitigation plan calls for.

## Accepted Risks Log

The following threats were dispositioned `accept` at plan time, with justification already documented in the phase plans. Recorded here to close the loop per this audit's `accept` verification method (entry present in this log = CLOSED).

| Threat ID | Category | Component | Justification | Source |
|-----------|----------|-----------|----------------|--------|
| T-10-05 | Information Disclosure | string resource `android_auto_connected` | Static string, no format placeholders (`%1$s` etc.), no external data interpolated — confirmed by direct read of `strings.xml:13`: `<string name="android_auto_connected">Connesso ad Android Auto</string>`. Compiled into the signed APK like every other resource. | 10-01-PLAN.md threat_model |
| T-10-06 | Spoofing | broadcast `CAR_CONNECTION_UPDATED` received by the library's internal `LiveData` | The broadcast carries no state payload; the library's receiver reacts by re-querying the `androidx.car.app.connection` `ContentProvider`, which remains the sole source of the value. The provider authority is unique per device and not impersonable by a third-party app. Worst case for a malicious broadcast sender: one extra query, no state corruption, no data exposure. | 10-02-PLAN.md threat_model |
| T-10-14 | Information Disclosure | `run-as` + `cat` of `tachimetro_prefs.xml` during DHU verification | Works only on debug builds, only for the app's own data. File contains only max speed, distance, denial counter, and the keep-screen-on boolean — no personal data, no coordinates. Diagnostic value (on-disk proof of CONN-02) outweighs the negligible risk. | 10-03-PLAN.md threat_model |
| T-10-13 | Repudiation | esito del checkpoint DHU umano (10-03-PLAN.md Task 2) | Downgraded from `mitigate` to `accept` after this audit found the itemized-response artifact (A1-G1 individually) absent — only the single word "approvato" is recorded, though the plan's own `<resume-signal>` field explicitly permitted that shorthand ("Rispondere 'approvato' solo se ... sono tutti confermati"), while its `<acceptance_criteria>` and threat model text required itemized answers — an internal inconsistency in the plan authored at 10-03-PLAN.md creation time, not a gap introduced during execution. Presented to the user with two remediation options (itemized re-confirmation vs. accept); user chose to accept: the full A1-G1 checklist was displayed verbatim by the executor before the response was given, and "approvato" was solicited only under the plan's own explicit all-or-nothing contract. | This audit (2026-09-02), user decision via `/gsd:secure-phase 10` |

## Informational — n/a Disposition

| Threat ID | Category | Component | Note |
|-----------|----------|-----------|------|
| T-10-SC (×3: 10-01/10-02/10-03) | Tampering | package-manager installs | No new dependency/Maven coordinate introduced by any of the three plans. `git log --oneline -- app/build.gradle.kts gradle/libs.versions.toml` shows the most recent change (`ada5f8f`, "add Car App Library dependency and car status string") predates Phase 10 (Phase 8); no Phase 10 commit appears in that history. `androidx.car.app:app:1.7.0` and the transitive `androidx.lifecycle:lifecycle-livedata-core:2.11.0` were already present. |

## Unregistered Flags

None. `10-01-SUMMARY.md`, `10-02-SUMMARY.md`, and `10-03-SUMMARY.md` contain no `## Threat Flags` section (grepped directly, zero matches across the phase directory) — no new attack surface was flagged by any executor during implementation.

## Notes on Verification Method

- All `mitigate` threats were verified against the actual implementation files (`CarLinkState.kt`, `MainActivity.kt`, `CarLinkStateTest.kt`, `CarLinkSequenceTest.kt`) and live `git log`/`git status` output, not against plan intent or SUMMARY narrative — except T-10-13, where the declared mitigation *is* a documentation artifact (itemized checkpoint responses), and that artifact was found absent.
- `grep -c 'CONNECTION_TYPE_' MainActivity.kt` and `grep -c 'screenOnStore.write('` were re-run directly against the current file content (not sourced from the plan's own `<verify>` blocks, which 10-02-SUMMARY.md documents as containing two comment-counting bugs unrelated to actual behavior).
- `AndroidManifest.xml`, `app/build.gradle.kts`, and `gradle/libs.versions.toml` were checked via `git log`/`git diff` directly against the file, not assumed unchanged from `files_modified` frontmatter.
- Implementation files were not modified during this audit.

## Security Audit 2026-09-02 (post-decision update)

| Metric | Count |
|--------|-------|
| Threats found | 19 |
| Closed | 19 |
| Open | 0 |

T-10-13 moved from OPEN to CLOSED (accepted) after the user was presented with the gap and chose to accept the risk as documented, rather than obtain an itemized A1-G1 re-confirmation. See Accepted Risks Log above for the full justification.

## Result

**Threats Closed:** 19/19 (12 mitigate + 4 accept [T-10-05, T-10-06, T-10-14, T-10-13] + 3 n/a-group counted as 1 verified entry)
**Threats Open:** 0/19

### Count breakdown
- `mitigate`, CLOSED: T-10-01, T-10-02, T-10-03, T-10-04, T-10-07, T-10-08, T-10-09, T-10-10, T-10-11, T-10-12, T-10-15, T-10-16 (12)
- `accept`, CLOSED: T-10-05, T-10-06, T-10-14, T-10-13 (4, T-10-13 downgraded from mitigate post-audit)
- `n/a`, verified informational: T-10-SC ×3 (3)
- **Total register entries:** 19 — **Closed:** 19 — **Open:** 0
