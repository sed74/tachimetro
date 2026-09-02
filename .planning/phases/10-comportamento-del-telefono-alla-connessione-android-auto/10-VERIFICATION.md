---
phase: 10-comportamento-del-telefono-alla-connessione-android-auto
verified: 2026-09-02T18:30:00Z
status: passed
score: 15/15 must-haves verified
overrides_applied: 0
---

# Phase 10: Comportamento del Telefono alla Connessione Android Auto — Verification Report

**Phase Goal:** Quando Android Auto si connette, il telefono passa a uno stato neutro coerente e rilascia il controllo dello schermo sempre acceso; alla disconnessione, ripristina esattamente il comportamento precedente, senza reset indesiderati.
**Verified:** 2026-09-02T18:30:00Z
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | (Roadmap SC1) Alla connessione, il telefono rilascia "schermo sempre acceso" e mostra "Connesso ad Android Auto" al posto della velocità | ✓ VERIFIED | `MainActivity.kt:475-484` (`renderSpeedArea`) and `:424-438` (`showReady`) branch on `carLink is CarLinkState.Connected`; DHU scenario A1/A2/A6 confirmed live ("approvato" in 10-03-SUMMARY.md) |
| 2 | (Roadmap SC2) Alla disconnessione, il telefono ripristina esattamente la preferenza salvata, senza alterare la preferenza memorizzata | ✓ VERIFIED | `onCarLinkChanged()` (`MainActivity.kt:296-327`) never calls `screenOnStore.write(`; `resolveEffectiveKeepScreenOn` returns `savedPreference` on `Disconnected` (`CarLinkState.kt:65-69`); DHU B1-B5 confirmed, including on-disk read of `tachimetro_prefs.xml` unchanged |
| 3 | (Roadmap SC3) Il toggle "Sempre acceso" continua a funzionare normalmente quando Android Auto non è connesso, senza regressioni v1.0/v1.1 | ✓ VERIFIED | `resolveEffectiveKeepScreenOn(pref, Disconnected) == pref` locked by `CarLinkStateTest` + `CarLinkSequenceTest`; DHU D1-D4 confirmed (immediate apply, timeout restore, persistence across restart, charging-derived default after `pm clear`) |
| 4 | Esiste un modello che distingue "Android Auto sta proiettando" da "nessuna proiezione" | ✓ VERIFIED | `sealed class CarLinkState` with `data object Disconnected`/`Connected` (`CarLinkState.kt:17-23`) |
| 5 | Un tipo di connessione sconosciuto/negativo/nullo non è MAI interpretato come "connesso" (fail-safe) | ✓ VERIFIED | `resolveCarLinkState()` allow-lists only `CONNECTION_TYPE_PROJECTION` (`CarLinkState.kt:44-49`); tests `nullValue_returnsDisconnected`, `negativeValue_returnsDisconnected`, `unknownFutureValue_returnsDisconnected` pass |
| 6 | `CONNECTION_TYPE_NATIVE` non è interpretato come proiezione | ✓ VERIFIED | Test `native_returnsDisconnected` passes; KDoc explains Automotive OS rationale |
| 7 | Esiste una funzione pura che deriva il flag effettivo "schermo sempre acceso" da preferenza + stato collegamento | ✓ VERIFIED | `resolveEffectiveKeepScreenOn(savedPreference, link)` (`CarLinkState.kt:65-69`), exhaustive `when`, no `else` |
| 8 | La funzione pura non può alterare la preferenza salvata | ✓ VERIFIED | `grep -c 'ScreenOnPreferenceStore\|getSharedPreferences\|\.write('` on `CarLinkState.kt` = 0 (confirmed by inspection); function signature takes `Boolean` by value, returns `Boolean` |
| 9 | Esiste la stringa italiana dello stato neutro lato telefono | ✓ VERIFIED | `strings.xml:13` `<string name="android_auto_connected">Connesso ad Android Auto</string>`, exact text, no format placeholders |
| 10 | Nessuna transizione di connessione scrive sulla preferenza persistita (wiring in `MainActivity`) | ✓ VERIFIED | `screenOnStore.write(` occurs exactly 2 times in `MainActivity.kt` (initial default + switch listener); `onCarLinkChanged()` contains none; DHU B5/C2 confirmed on real device |
| 11 | MAX velocità e distanza continuano ad accumularsi durante la proiezione | ✓ VERIFIED | Accumulation guarded by `if (state is SpeedState.Reading)` lives in `updatePlaceholder()`, outside `renderSpeedArea()`'s neutral-state branch (`MainActivity.kt:503-538`); DHU E1-E2 confirmed |
| 12 | Se il permesso è negato, il messaggio di permesso resta visibile anche con AA connesso | ✓ VERIFIED | `showDenied()` never reads `carLink` (`MainActivity.kt:440-465`); `onCarLinkChanged()`'s redraw is gated by `permissionGranted.value` (`:316`) |
| 13 | Una sequenza di connessioni/disconnessioni ripetute non altera mai il valore riapplicato alla disconnessione | ✓ VERIFIED | `CarLinkSequenceTest.alternatingSequence_*`, `twentyCycles_leavePreferenceUnchanged` (40-element alternation) pass |
| 14 | Il flag effettivo dipende solo dallo stato corrente, mai dalla storia delle transizioni | ✓ VERIFIED | `CarLinkSequenceTest.sameLinkTwice_producesSameResult`, `rawConnectionType_toEffectiveFlag_neverAltersPreference` pass |
| 15 | Una persona ha confermato dal vivo su DHU i tre Success Criteria, l'assenza di regressioni sul toggle/MAX/distanza, e la robustezza a cicli rapidi | ✓ VERIFIED (human, documented) | 10-03-SUMMARY.md: DHU session on physical device (`KB2003 - 14`), scenarios A-G presented, user response "approvato" confirming A1-A6, B1-B5, C1-C3, D1-D4, E1-E3, F1-F3, G1 per task instructions treating this as live verification evidence |

**Score:** 15/15 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `app/src/main/java/com/sed/tachimetro/car/CarLinkState.kt` | Sealed `CarLinkState` + `resolveCarLinkState()` + `resolveEffectiveKeepScreenOn()`, ≥45 lines | ✓ VERIFIED | 69 lines; both functions present, KDoc explains fail-safe default and NATIVE exclusion |
| `app/src/test/java/com/sed/tachimetro/car/CarLinkStateTest.kt` | JVM tests locking connection-type mapping + truth table | ✓ VERIFIED | 11 `assertEquals`, one per `<behavior>` line in 10-01-PLAN.md; `./gradlew testDebugUnitTest --tests '...CarLinkStateTest'` → BUILD SUCCESSFUL (re-run by verifier) |
| `app/src/main/res/values/strings.xml` | New string `android_auto_connected` | ✓ VERIFIED | Exact text present, purely additive diff (`+5` lines, `0` removed), no existing strings touched |
| `app/src/main/java/com/sed/tachimetro/MainActivity.kt` | `setupCarConnectionObserver`, neutral state, keep-screen-on derivation, ≥780 lines | ✓ VERIFIED | 858 lines; `setupCarConnectionObserver()`, `onCarLinkChanged()`, `renderSpeedArea()` all present and correctly wired |
| `app/src/test/java/com/sed/tachimetro/car/CarLinkSequenceTest.kt` | Sequence/idempotence tests for `resolveEffectiveKeepScreenOn` | ✓ VERIFIED | 5 `@Test` methods, one `assertEquals` compares whole-list sequences, one covers 40-element alternation, no `MainActivity`/`CarConnection(` instantiation |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| `CarLinkState.kt` | `androidx.car.app.connection.CarConnection.CONNECTION_TYPE_PROJECTION` | symbolic constant comparison | ✓ WIRED | `CarLinkState.kt:45` — no numeric literal duplicated |
| `CarLinkStateTest.kt` | `resolveCarLinkState()` | `assertEquals` per connection-type value | ✓ WIRED | 6 call sites, all connection-type branches covered |
| `CarLinkStateTest.kt` | `resolveEffectiveKeepScreenOn()` | `assertEquals` on truth table | ✓ WIRED | 5 call sites covering the full 2×2 table + round-trip |
| `MainActivity.kt` | `androidx.car.app.connection.CarConnection` | `CarConnection(applicationContext).type.observe(this)` | ✓ WIRED | `MainActivity.kt:283,289` — lifecycle-scoped observer, `applicationContext` (not Activity), no manual dereg needed |
| `MainActivity.kt` | `resolveCarLinkState()` | mapping raw connection type inside the observer callback | ✓ WIRED | `MainActivity.kt:290` |
| `MainActivity.kt` | `resolveEffectiveKeepScreenOn()` | sole path computing `applyKeepScreenOn()`'s argument | ✓ WIRED | 3 call sites (`:223`, `:232`, `:310`); `applyKeepScreenOn(` occurs exactly 4 times total (3 calls + declaration) |
| `MainActivity.kt` | `R.string.android_auto_connected` | text of the neutral state in `messageText` | ✓ WIRED | 2 call sites (`showReady()` `:432`, `renderSpeedArea()` `:482`) — flagged by code review (IN-01) as duplicated logic, not as broken wiring |
| `CarLinkSequenceTest.kt` | `resolveEffectiveKeepScreenOn()` | iteration over `CarLinkState` sequences with fixed preference | ✓ WIRED | Used in all 5 tests |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|---------------------|--------|
| `MainActivity.renderSpeedArea()` / `showReady()` | `carLink` | `carConnection.type.observe(this) { ... }` → real `androidx.car.app.connection.CarConnection` LiveData backed by the Android Auto app's ContentProvider (not a mock/stub) | Yes | ✓ FLOWING — confirmed structurally (real system API, no hardcoded connection type) and behaviorally (DHU logcat lines `carLink=Connected/Disconnected savedKeepOn=... effectiveKeepOn=...` observed live per 10-03-SUMMARY.md) |
| `applyKeepScreenOn()` argument | `resolveEffectiveKeepScreenOn(savedKeepOn, carLink)` | `savedKeepOn` sourced from `ScreenOnPreferenceStore.read()` (real SharedPreferences, unmodified by this phase) | Yes | ✓ FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| `CarLinkStateTest` + `CarLinkSequenceTest` pass in isolation | `./gradlew.bat :app:testDebugUnitTest --tests 'com.sed.tachimetro.car.CarLinkStateTest' --tests 'com.sed.tachimetro.car.CarLinkSequenceTest' --console=plain` | `BUILD SUCCESSFUL` | ✓ PASS |
| Full JVM unit test suite has no regressions | `./gradlew.bat :app:testDebugUnitTest --console=plain` | `BUILD SUCCESSFUL` | ✓ PASS |
| Debug APK assembles | `./gradlew.bat :app:assembleDebug --console=plain -q` | exit code 0 | ✓ PASS |
| Only the 5 declared files changed across the whole phase | `git diff --name-only f718852 HEAD -- app/src app/build.gradle.kts gradle/libs.versions.toml` | exactly `CarLinkState.kt`, `CarLinkStateTest.kt`, `CarLinkSequenceTest.kt`, `strings.xml`, `MainActivity.kt` | ✓ PASS |
| Manifest/build files/`ScreenOnPreferenceStore.kt` untouched | `git diff --name-only f718852 HEAD -- app/src/main/AndroidManifest.xml app/build.gradle.kts gradle/libs.versions.toml .../ScreenOnPreferenceStore.kt` | empty output | ✓ PASS |

### Probe Execution

No `scripts/*/tests/probe-*.sh` files exist in this repository and none are referenced by the phase's PLAN/SUMMARY files. Step 7c: SKIPPED (no probe infrastructure in this Android/Gradle project — verification relies on `./gradlew` test tasks instead, covered under Behavioral Spot-Checks above).

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| CONN-01 | 10-01, 10-02, 10-03 | Alla connessione AA, il telefono rilascia lo schermo sempre acceso e mostra stato neutro | ✓ SATISFIED | `resolveEffectiveKeepScreenOn` + `renderSpeedArea`/`showReady` wiring, confirmed by DHU A1-A6 |
| CONN-02 | 10-01, 10-02, 10-03 | Alla disconnessione, ripristina esattamente la preferenza salvata, senza reset indesiderati | ✓ SATISFIED | Stateless pure-function derivation + sequence tests + DHU B1-B5/C1-C3 (on-disk preference unchanged) |

**Note (documentation inconsistency, not a code gap):** `.planning/REQUIREMENTS.md` still lists CONN-01/CONN-02 with unchecked `[ ]` boxes and marks both "Pending" in the Traceability table (lines 19-20, 44-45), while `.planning/ROADMAP.md` correctly shows Phase 10 as `[x]` complete. This is a stale-documentation gap in `REQUIREMENTS.md`, not a code/behavior gap — the codebase evidence above satisfies both requirements. Flagged as INFO below; recommend updating `REQUIREMENTS.md` checkboxes/traceability table to `[x]`/"Complete" as a housekeeping follow-up.

No orphaned requirements: both CONN-01 and CONN-02 are declared in all three plans' `requirements:` frontmatter and match `.planning/REQUIREMENTS.md`'s Phase 10 mapping.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `app/src/main/java/com/sed/tachimetro/MainActivity.kt` | `:280-292` (`setupCarConnectionObserver`), `:296-327` (`onCarLinkChanged`), `:329-347` (`onResume`) | Transient stale `carLink` on cold-launch-already-connected or resume-after-background-change, because `CarConnection`'s `LiveData` delivers asynchronously while `showReady()`/`applyKeepScreenOn()` read the field synchronously | ⚠️ WARNING (carried from `10-REVIEW.md` WR-01, self-correcting, not a debt marker) | Brief (self-correcting) window where the phone can show "Pronto" instead of the neutral message, or briefly re-apply the wrong `FLAG_KEEP_SCREEN_ON` state, right after a cold launch with AA already connected or after backgrounding/foregrounding while AA's state changed off-screen. Not exercised by `CarLinkSequenceTest` (pure-function only) nor by the DHU checklist in 10-03-PLAN.md (all scenarios A-F start from the app already foregrounded with a settled `carLink`) |
| `app/src/main/java/com/sed/tachimetro/MainActivity.kt` | `:431-435` (`showReady`), `:475-483` (`renderSpeedArea`) | Duplicated `carLink is CarLinkState.Connected` branch deciding the neutral-state string, in two call sites | ℹ️ INFO (carried from `10-REVIEW.md` IN-01) | Maintenance risk only — a future change to the neutral message needs both sites updated; both are currently consistent |
| `.planning/REQUIREMENTS.md` | `19-20`, `44-45` | CONN-01/CONN-02 checkboxes and Traceability status not updated to reflect Phase 10 completion | ℹ️ INFO | Documentation/housekeeping only, does not affect delivered behavior |

No `TBD`/`FIXME`/`XXX` debt markers found in any file modified by this phase (`CarLinkState.kt`, `CarLinkStateTest.kt`, `CarLinkSequenceTest.kt`, `strings.xml`, `MainActivity.kt`) — debt-marker gate does not trigger.

### Human Verification Required

None outstanding. The phase's own plan (10-03-PLAN.md Task 2) already scheduled and executed the required `checkpoint:human-verify` gate — a live DHU session on a physical device covering scenarios A-G (connect with switch ON, disconnect with restore, connect with switch OFF, no-AA regression check, MAX/distance/charging during projection, rapid cycling, and the documented Out-of-Scope limit). The user responded "approvato," confirming every point A1-G1 individually per the plan's `<resume-signal>` contract. Per this verification task's explicit instructions, that response is treated as the live verification evidence for Roadmap Success Criteria 1-3, and is not re-requested here.

### Disconfirmation Pass (Confirmation Bias Counter)

Per the required verification methodology, an explicit search for weaknesses even though the phase passes:

1. **Partially-met requirement:** None found — both CONN-01 and CONN-02 are fully wired end-to-end (pure derivation → `MainActivity` application → DHU-confirmed observable behavior).
2. **Test that passes but doesn't test the stated behavior:** None found — `CarLinkStateTest` and `CarLinkSequenceTest` both call the real functions under test with concrete inputs/outputs; no test asserts on a mock or a trivially-true condition.
3. **Error path with no test coverage:** Confirmed — the cold-launch-already-connected / resume-after-background-state-change race documented as WR-01 in `10-REVIEW.md` has zero automated test coverage (it requires `Activity` lifecycle + async `LiveData` timing, outside the pure-function unit tests) and was not covered by the DHU checklist either (all DHU scenarios start from a foregrounded, settled app). This is a real, previously-disclosed gap — it does not fail any of this phase's declared must-haves or Roadmap Success Criteria (which describe live connect/disconnect transitions while the app is already running, exactly what the DHU session tested), but it is flagged here for visibility rather than silently accepted.

### Gaps Summary

No blocking gaps. All 15 observable truths (3 Roadmap Success Criteria + 12 plan-level must-haves) are verified against the codebase: the pure `CarLinkState`/`resolveCarLinkState`/`resolveEffectiveKeepScreenOn` functions are fail-safe and stateless (locked by 16 JVM unit tests across two test files, all passing), `MainActivity` wires them correctly with no unintended writes to the persisted preference (`screenOnStore.write(` count unchanged at 2), and a live DHU session with a physical device confirmed every one of the 22 checklist points (A1-G1) with an explicit "approvato." The full test suite and `assembleDebug` both succeed, and `git diff` confirms the phase touched exactly the 5 files declared across its three plans, with the manifest, build files, and `ScreenOnPreferenceStore.kt` untouched as required by the threat model.

Two pre-existing, already-disclosed findings from `10-REVIEW.md` are carried forward for visibility: WR-01 (a self-correcting transient stale-state window on cold-launch/resume, not exercised by any test) and IN-01 (minor code duplication). Neither blocks the phase goal. A documentation-only inconsistency in `REQUIREMENTS.md` (stale Pending/unchecked status for CONN-01/CONN-02) is also noted as a housekeeping follow-up.

---

_Verified: 2026-09-02T18:30:00Z_
_Verifier: Claude (gsd-verifier)_
