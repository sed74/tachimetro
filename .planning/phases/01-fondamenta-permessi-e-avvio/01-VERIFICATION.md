---
phase: 01-fondamenta-permessi-e-avvio
verified: 2026-07-07T15:30:00Z
status: human_needed
score: 9/9 must-haves verified (code + build evidence)
overrides_applied: 0
human_verification:
  - test: "Rifiuto permanente -> apri Impostazioni -> concedi il permesso manualmente -> torna all'app (tasto Indietro, senza chiudere/riaprire il processo)"
    expected: "L'app mostra immediatamente sfondo nero con testo 'Pronto' (nessun pulsante), senza bisogno di forzare la chiusura e riavviare l'app"
    why_human: "Comportamento di lifecycle Android (onResume dopo il ritorno da Settings) verificabile solo su un dispositivo/emulatore reale. Questo e esattamente il bug critico (CR-01) trovato dal code reviewer DOPO che il checkpoint umano del piano 01-02 era gia stato approvato (\"approvato\") -- il fix (commit 927e3c0, onResume() override) non e mai stato ri-testato dall'utente su device reale. Il checkpoint originale testava solo l'apertura delle Impostazioni (item 5) e il riavvio completo dell'app con permesso gia concesso (item 6), non il ritorno in foreground senza restart del processo."
---

# Phase 1: Fondamenta, Permessi e Avvio Verification Report

**Phase Goal:** L'app si avvia direttamente sulla schermata principale (nessun menu iniziale) e gestisce correttamente la richiesta del permesso ACCESS_FINE_LOCATION, incluso il caso di rifiuto.
**Verified:** 2026-07-07T15:30:00Z
**Status:** human_needed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths (Roadmap Success Criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | L'app si apre direttamente sulla schermata della velocità, senza schermate iniziali o menu | VERIFIED | `AndroidManifest.xml` declares exactly one `<activity>` (`.MainActivity`) with `android:exported="true"` and a single MAIN/LAUNCHER intent-filter; no other activities exist in the manifest or codebase. |
| 2 | Al primo avvio, l'app richiede all'utente il permesso ACCESS_FINE_LOCATION | VERIFIED | `MainActivity.onCreate()` calls `checkAndRequestPermission()` unconditionally (line 39), which calls `requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)` whenever the permission is not already granted (lines 61-76). |
| 3 | Se l'utente nega il permesso, vede un messaggio appropriato che spiega l'impossibilità di leggere il GPS | VERIFIED | `showDenied()` sets `messageText` to `R.string.permission_denied` ("Permesso GPS necessario per funzionare") or `R.string.permission_denied_permanent` ("Permesso GPS negato. Aprire le impostazioni per abilitarlo") and shows `retryButton` with the appropriate label (`Riprova` / `Apri impostazioni`). |
| 4 | Se l'utente concede il permesso, l'app procede senza ulteriori richieste non necessarie | PARTIALLY VERIFIED (code) / NEEDS HUMAN (device) | `showReady()` hides `retryButton` and displays "Pronto"; `checkAndRequestPermission()` short-circuits to `showReady()` if already granted (no re-prompt on restart — this exact path was human-tested and approved). However, the "grant via Settings and return without restart" sub-path relies on the `onResume()` override added in review-fix commit `927e3c0`, added AFTER the approved human checkpoint, and never re-tested on a real device. See Human Verification section. |

**Score:** 3/4 fully device-confirmed, 1/4 confirmed by code+build only pending device re-confirmation of a post-approval fix.

### Plan 01-02 must_haves (frontmatter)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | L'app si apre direttamente su MainActivity all'avvio, senza schermate iniziali o menu | VERIFIED | Same as SC1 above. |
| 2 | Al primo avvio l'app richiede il permesso ACCESS_FINE_LOCATION tramite il popup di sistema | VERIFIED | Same as SC2 above. |
| 3 | Se l'utente nega il permesso, vede un messaggio in italiano con un pulsante 'Riprova' che ri-richiede il permesso | VERIFIED | `showDenied()` + `onRetryClicked()` re-launches `requestPermissionLauncher` when `shouldShowRequestPermissionRationale` is true. Device-confirmed pre-fix (unaffected by later refactor WR-01, which only removed a redundant branch without changing behavior). |
| 4 | Se il rifiuto e permanente, il pulsante apre le Impostazioni dell'app invece di ri-richiedere il popup | VERIFIED | `onRetryClicked()` calls `openAppSettings()` (`Settings.ACTION_APPLICATION_DETAILS_SETTINGS` + `Uri.fromParts("package", packageName, null)`) when rationale is false. Device-confirmed pre-fix. |
| 5 | Se il permesso e concesso, la schermata mostra sfondo nero con il testo 'Pronto' e nessun altro elemento grafico | VERIFIED | `activity_main.xml` root is `ConstraintLayout` with `android:background="@android:color/black"`; only `messageText` (TextView) and `retryButton` (Button, `GONE` when ready) exist. Device-confirmed. |

### Plan 01-01 must_haves (frontmatter) — noted deviation

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Il progetto compila con il plugin Kotlin Android applicato | VERIFIED (with documented deviation) | Kotlin compiles via AGP 9.1.1's mandatory built-in Kotlin support instead of the classic `org.jetbrains.kotlin.android` plugin, which is technically incompatible with this AGP version (documented and evidenced in `01-01-SUMMARY.md` "Deviations from Plan"). Actual objective (Kotlin compiles, `.kt` files build) is met; `./gradlew.bat assembleDebug` → BUILD SUCCESSFUL with `:app:compileDebugKotlin` executing (confirmed independently below). |
| 2 | Il plugin Kotlin e la libreria ConstraintLayout sono dichiarati nella version catalog e referenziati via alias libs.* | PARTIALLY VERIFIED (deviation, ConstraintLayout only) | `gradle/libs.versions.toml` declares `constraintlayout = "2.2.1"` and the `constraintlayout` library alias, referenced in `app/build.gradle.kts` via `implementation(libs.constraintlayout)`. No `kotlin-android` plugin alias exists (removed as unusable — see deviation above); this is an environment-forced substitution, not scope creep, and does not block the phase goal. |
| 3 | Il target bytecode Kotlin e allineato a Java 11 | VERIFIED | `app/build.gradle.kts` sets `kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_11 } }`, matching `compileOptions` Java 11. |

This looks intentional and well-documented (verified by direct build-failure evidence in the summary, not a shortcut). No override entry needed since the underlying goal ("Kotlin compiles, ConstraintLayout available, jvmTarget 11") is independently verified true.

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `app/src/main/AndroidManifest.xml` | ACCESS_FINE_LOCATION permission + MainActivity as LAUNCHER | VERIFIED | Exactly one `<uses-permission>` (ACCESS_FINE_LOCATION, no COARSE/background), one `<activity android:name=".MainActivity" android:exported="true">` with MAIN/LAUNCHER intent-filter. |
| `app/src/main/res/values/strings.xml` | Italian strings for permission flow | VERIFIED | `status_ready`, `permission_denied`, `permission_denied_permanent` (now distinct text, WR-02 fix applied), `retry`, `open_settings`, `app_name` — all Italian. |
| `app/src/main/res/layout/activity_main.xml` | Black placeholder layout with messageText + retryButton | VERIFIED | Root `ConstraintLayout`, black background, `messageText` TextView, `retryButton` Button (`visibility="gone"`). No hardcoded user text (only `tools:text` for preview). |
| `app/src/main/java/com/sed/tachimetro/MainActivity.kt` | Full permission flow (granted/denied/permanently denied) | VERIFIED | Implements `registerForActivityResult(ActivityResultContracts.RequestPermission())`, `checkAndRequestPermission()`, `onRetryClicked()`, `openAppSettings()`, `showReady()`, `showDenied()`, plus `onResume()` override (post-review fix). |
| `gradle/libs.versions.toml` / `app/build.gradle.kts` | Kotlin enabled, ConstraintLayout + activity-ktx dependencies | VERIFIED | `constraintlayout` and `activity` (added in WR-03 fix) declared and used; Kotlin enabled via AGP built-in support (documented deviation). |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| `AndroidManifest.xml` | `com.sed.tachimetro.MainActivity` | `android:name=".MainActivity"` + MAIN/LAUNCHER intent-filter | WIRED | Confirmed via file read; grep pattern `\.MainActivity` and `LAUNCHER` both present. |
| `MainActivity.kt` | `activity_main.xml` | `setContentView(R.layout.activity_main)` + `findViewById(R.id.messageText/retryButton)` | WIRED | Line 33, 35-36; `./gradlew.bat assembleDebug` succeeds, proving `R.*` references resolve. |
| `MainActivity.kt` | `android.permission.ACCESS_FINE_LOCATION` | `requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)` | WIRED | Lines 74, 80; declared once in manifest, referenced consistently, no COARSE/background variant anywhere in the codebase (`grep -r ACCESS_COARSE_LOCATION` → no matches). |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Project builds with all Task 1/2 artifacts wired (R.* resolution) | `./gradlew.bat assembleDebug` | `BUILD SUCCESSFUL in 1s`, 33 actionable tasks, `:app:compileDebugKotlin` executed (not NO-SOURCE) | PASS |
| No forbidden coarse/background location permission present | `grep -rn "ACCESS_COARSE_LOCATION\|ACCESS_BACKGROUND_LOCATION" app/src/main/` | No matches | PASS |
| No debt markers / stub patterns in phase-modified files | `grep -n -E "TBD\|FIXME\|XXX\|TODO\|HACK\|PLACEHOLDER\|placeholder\|coming soon\|not yet implemented" ...` | No matches | PASS |

Note: actual runtime behavior (system permission popup appearing, grant/deny/permanent-deny UI transitions, Settings deep-link) cannot be exercised by shell commands — those rely on the human device verification already performed for Task 3, plus one item still pending re-confirmation (see below).

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|--------------|--------|----------|
| APP-01 | 01-01 (declared), 01-02 (implements) | L'app si apre direttamente sulla schermata della velocità, senza schermate iniziali o menu | SATISFIED | Single LAUNCHER activity, human-confirmed direct launch with no menu. |
| PERM-01 | 01-02 | L'app richiede solo il permesso ACCESS_FINE_LOCATION necessario per leggere il GPS | SATISFIED | Manifest declares exactly one permission; no COARSE/background anywhere. |
| PERM-02 | 01-02 | L'app gestisce correttamente sia la concessione sia il rifiuto del permesso GPS, mostrando un messaggio appropriato in caso di rifiuto | SATISFIED (core flow) / PENDING RE-CONFIRMATION (post-fix onResume path) | Grant/deny/permanent-deny UI flows human-confirmed pre-fix; the "grant via Settings, return without restart" recovery path was a critical bug found by code review after approval and fixed in commit 927e3c0, not yet re-tested on device. |

No orphaned requirements: REQUIREMENTS.md traceability table maps exactly APP-01, PERM-01, PERM-02 to Phase 1, and all three appear in plan frontmatter (`01-01-PLAN.md` declares APP-01; `01-02-PLAN.md` declares APP-01, PERM-01, PERM-02). REQUIREMENTS.md checkboxes and traceability status still show "Pending" — this is expected, as the orchestrator updates this file after phase verification passes; not a gap.

### Anti-Patterns Found

None. Scanned all phase-modified files (`MainActivity.kt`, `AndroidManifest.xml`, `strings.xml`, `activity_main.xml`, `app/build.gradle.kts`, `gradle/libs.versions.toml`) for TBD/FIXME/XXX/TODO/HACK/PLACEHOLDER/empty-return patterns — zero matches.

### Code Review Findings — Disposition

`01-REVIEW.md` found 1 critical + 3 warning + 2 info issues. `01-REVIEW-FIX.md` confirms all 4 in-scope (critical + warning) findings were fixed and build-verified (commits `927e3c0`, `238b280`, `814e1f8`, `132bbf7`), independently confirmed present in the current codebase by this verification. The 2 info-level findings (constant duplication, missing `<uses-feature>` GPS declaration) were explicitly deferred as non-blocking and don't affect this phase's goal.

The critical finding (CR-01) is the one substantive residual risk: its fix is code-verified and build-verified, but not yet device-verified, and it directly affects PERM-02 (the "correct handling" of the permission flow) for the one path (Settings → grant → return without restart) that was never part of the originally-scripted human checkpoint.

### Human Verification Required

### 1. Return-from-Settings permission re-check (post-review-fix regression check)

**Test:** On a real device/emulator: trigger permanent denial (deny the permission twice until Android stops showing the popup), press the resulting "Apri impostazioni" button, grant the location permission from the system Settings screen, then press the device Back button to return to the app (do NOT force-close/relaunch the app process).
**Expected:** The app immediately shows the black screen with "Pronto" and no button — no force-kill/relaunch needed.
**Why human:** This is Android Activity lifecycle behavior (`onResume()`) that can only be exercised on a real device or emulator. It is the exact defect (CR-01) the code reviewer found as a **critical** blocker after the phase's human checkpoint had already been approved ("approvato"). The fix (commit `927e3c0`) is present in the code and compiles, but has never been exercised on-device.

## Gaps Summary

No code-level gaps: every must-have artifact, key link, and observable truth from both plans and the roadmap's Success Criteria is present, substantive, and wired, and the full build succeeds. All 4 code-review findings in scope were fixed and are confirmed present in the codebase.

The phase is being held at `human_needed` rather than `passed` for a single reason: the critical fix for CR-01 (permission re-check on return from Settings) was written and merged *after* the human device checkpoint was approved, and per the task briefing, the user has not yet re-tested the app on a real device since that fix landed. Since this exact path was the phase's own most important edge case (recovering from permanent denial), it should be confirmed on-device before the phase is declared fully done — a quick, low-risk check (a few taps), not a rework.

---

*Verified: 2026-07-07T15:30:00Z*
*Verifier: Claude (gsd-verifier)*
