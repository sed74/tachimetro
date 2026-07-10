---
phase: 03-interfaccia-tachimetro
verified: 2026-07-10T00:00:00Z
status: passed
resolved: 2026-07-10T00:00:00Z
resolution: "Round-4 immersive fullscreen human_verification items confirmed by user (\"approvato\") in-session; persisted in 03-HUMAN-UAT.md."
score: 8/8 must-haves verified
human_verification:
  - test: "Round 4 immersive fullscreen: avviare l'app e osservare se status bar e nav bar sono nascoste, nessuna barra del titolo/ActionBar visibile in nessun momento"
    expected: "Nessuna barra di sistema visibile all'avvio; nessuna ActionBar/barra del titolo in nessun momento"
    why_human: "Comportamento runtime di system-UI (WindowInsetsControllerCompat.hide) non verificabile staticamente da codice/grep; richiede device/emulatore reale"
  - test: "Swipe dal bordo superiore/inferiore dello schermo durante l'uso dell'app"
    expected: "Le barre di sistema appaiono temporaneamente (swipe-to-reveal, BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE) senza bloccare l'interazione con l'app"
    why_human: "Comportamento gestuale a runtime, non verificabile senza device reale"
  - test: "Con una lettura di velocità attiva, verificare che l'etichetta 'km/h' (unitText, in alto a destra) resti leggibile e correttamente posizionata sia in portrait sia in landscape, con le barre di sistema nascoste"
    expected: "unitText visibile, non tagliata, senza margine eccessivo residuo, in entrambi gli orientamenti"
    why_human: "Interazione tra applyUnitTextWindowInsets() e barre nascoste è verificabile a livello di codice (margine ricalcolato da zero ad ogni callback) ma il rendering visivo finale richiede conferma visiva"
  - test: "Negare permanentemente il permesso GPS, aprire 'Impostazioni', tornare all'app (flusso openAppSettings())"
    expected: "Le barre di sistema tornano nascoste automaticamente al rientro nell'app (onWindowFocusChanged → enableImmersiveFullscreen())"
    why_human: "Comportamento di focus/lifecycle a runtime tra due app, non verificabile staticamente"
---

# Phase 3: Interfaccia Tachimetro Verification Report

**Phase Goal:** L'utente vede la velocità corrente a schermo intero, come elemento dominante, leggibile a colpo d'occhio in ogni orientamento e condizione di luce, interamente in italiano.
**Verified:** 2026-07-10
**Status:** passed (resolved 2026-07-10 — see note below)
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Il numero della velocità si ridimensiona automaticamente per riempire lo schermo (1/2/3 cifre) | ✓ VERIFIED | `activity_main.xml` `messageText`: `app:autoSizeTextType="uniform"`, `autoSizeMinTextSize="12sp"`, `autoSizeMaxTextSize="300sp"`, `autoSizeStepGranularity="4sp"`, box `0dp`/`0dp`. `MainActivity.kt` `applySpeedAutosize()` applies the same 12-300sp range at runtime before every `SpeedState.Reading` text assignment. Human-confirmed on device across 1/2/3-digit values per Task 2 checkpoint ("approvato"). |
| 2 | Il numero è centrato orizzontalmente e verticalmente | ✓ VERIFIED | `android:gravity="center"`, `android:textAlignment="center"`, four constraints to parent (Top/Bottom/Start/End) + `layout_constraintVertical_chainStyle="packed"`, unchanged from plan. |
| 3 | Ruotando in landscape il numero resta centrato/massimizzato senza tagli, layout adattivo unico (no `res/layout-land`) | ✓ VERIFIED | No `res/layout-land*` directory exists (confirmed via directory listing). Single `ConstraintLayout` with `0dp` match-constraint box adapts to both orientations. Human-confirmed portrait+landscape × 1/2/3 digits at Task 2 checkpoint. |
| 4 | Testo bianco Black/900 grassetto, alto contrasto su sfondo nero | ✓ VERIFIED | `android:textColor="@android:color/white"`, `android:textStyle="bold"`, `android:textFontWeight="900"`, `android:fontFamily="sans-serif"`; root `android:background="@android:color/black"`. |
| 5 | I messaggi di stato riusano lo stesso `messageText` e si ridimensionano automaticamente più piccoli del numero | ✓ VERIFIED | Same `TextView messageText` used for `status_ready`/`searching_gps_signal`/`permission_denied*` and for the speed digits. `applyMessageAutosize()` (12-56sp cap) vs `applySpeedAutosize()` (12-300sp cap) switched at runtime via `TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration()` depending on content type — code-level confirmation that status text cannot grow into the huge digit range. |
| 6 | Il pulsante Riprova/Apri impostazioni mantiene lo stesso posizionamento, ricentrato | ✓ VERIFIED | `retryButton` constraints unchanged (`wrap_content`/`wrap_content`, chain to `messageText`, `layout_marginTop="16dp"`, centered start/end to parent). |
| 7 | Nessun menu/animazione/elemento grafico oltre `messageText`/`retryButton`; nessuno spazio riservato per Fase 4 | ✓ VERIFIED (with documented, user-approved deviation) | No `TransitionManager`/property animators/menu resources found. A third view, `unitText`, was added in round 2 as an explicit, user-requested architectural change (Rule 4, approved) to relocate the "km/h" unit label out of `messageText` — it is a necessary text label (the unit was already part of the pre-existing "N km/h" format string), not a decorative/unnecessary graphical element, so UI-04's intent is preserved. No area/placeholder reserved for Phase 4's max-speed value (D-05 honored). |
| 8 | Tutti i testi restano in italiano, nessuna stringa hardcoded | ✓ VERIFIED | `strings.xml`: all copy Italian, no new hardcoded strings; `unit_kmh` ("km/h") added via resource, not hardcoded. All `messageText`/`unitText`/`retryButton` assignments go through `getString()`/`@string/`. |

**Score:** 8/8 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `app/src/main/res/layout/activity_main.xml` | Full-screen layout with autosize `messageText` + recentered `retryButton` | ✓ VERIFIED | Contains `autoSizeTextType="uniform"`, `0dp`/`0dp` box, all mandated attributes present; `messageText`/`retryButton` IDs unchanged; plus `unitText` (documented deviation). |
| `app/src/main/java/com/sed/tachimetro/MainActivity.kt` | Not modified per original plan intent (visual-only) | ⚠️ MODIFIED (approved deviations) | 4 rounds of code changes beyond the original "no Kotlin changes" assumption: `applySpeedAutosize()`/`applyMessageAutosize()`, `unitText` visibility wiring, `applyUnitTextWindowInsets()`, `enableImmersiveFullscreen()`/`onWindowFocusChanged()`. All documented in SUMMARY.md as Rule 1/Rule 4 deviations with explicit user approval. |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| `activity_main.xml` | `MainActivity.kt findViewById(R.id.messageText)` / `R.id.retryButton` | Unchanged view IDs | ✓ WIRED | `findViewById(R.id.messageText)`/`findViewById(R.id.retryButton)` present at `MainActivity.kt:76,78`, IDs match XML. |
| `activity_main.xml messageText` | Autosize uniform bounding box `0dp`/`0dp` | TextView auto-size | ✓ WIRED | `app:autoSizeTextType="uniform"` present with box `0dp`/`0dp` and full min/max/step triad. |
| `activity_main.xml unitText` | `MainActivity.kt findViewById(R.id.unitText)` + visibility state machine | View ID + `View.VISIBLE`/`GONE` per `SpeedState` branch | ✓ WIRED | `findViewById(R.id.unitText)` at line 77; `unitText.visibility` set in every state branch (`showReady`, `showDenied`, `Searching`/`NoSignal`, `Reading`). |
| `unitText` | Live window insets (status bar / display cutout) | `ViewCompat.setOnApplyWindowInsetsListener` | ✓ WIRED | `applyUnitTextWindowInsets()` installs listener once in `onCreate()`, recalculates margin from base + live inset on every callback (no residual/incremental drift). |
| `MainActivity.kt onCreate()`/`onWindowFocusChanged()` | System bars hidden (immersive) | `WindowCompat.setDecorFitsSystemWindows` + `WindowInsetsControllerCompat.hide()` | ✓ WIRED (code) / ? UNVERIFIED (runtime) | Called once in `onCreate()` and re-applied on every `onWindowFocusChanged(true)`. Code path is wired correctly; actual on-device visual behavior not yet human-confirmed for this round (see Human Verification section). |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|---------------------|--------|
| `messageText` (Reading state) | `state.kmh` | `gpsSpeedProvider.state` (`StateFlow<SpeedState>`, established Phase 2, `FusedLocationProviderClient`) | Yes — live GPS-derived speed, collected via `repeatOnLifecycle(STARTED)` → `permissionGranted.collectLatest` → `gpsSpeedProvider.state.collect` | ✓ FLOWING |
| `messageText` (status states) | `getString(R.string.*)` | Static resource strings, branch-selected by `SpeedState`/permission logic | Yes — deterministic, not empty/placeholder | ✓ FLOWING |
| `unitText` | `@string/unit_kmh` + visibility toggle | Static string + `SpeedState` branch (`Reading` → VISIBLE, else GONE) | Yes — correctly gated by the same state used to populate `messageText` | ✓ FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Debug build compiles with all Phase 3 layout/Kotlin changes | `./gradlew.bat :app:assembleDebug` | `BUILD SUCCESSFUL in 4s` | ✓ PASS |
| No `res/layout-land` directory exists (D-02) | directory listing of `app/src/main/res` | no match | ✓ PASS |
| Full runtime UI behavior (autosize rendering, immersive fullscreen, swipe-reveal) | N/A — requires running app on device/emulator | N/A | ? SKIP — routed to human verification |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|--------------|--------|----------|
| UI-01 | 03-01-PLAN.md | Numero elemento dominante, sempre centrato, il più grande possibile | ✓ SATISFIED | Autosize uniform 12-300sp, box `0dp`/`0dp`, center gravity/alignment; human-confirmed at Task 2 checkpoint. |
| UI-02 | 03-01-PLAN.md | Sfondo nero, testo alto contrasto Bold/Black | ✓ SATISFIED | `background="@android:color/black"`, `textColor="white"`, `textStyle="bold"`, `textFontWeight="900"`. |
| UI-03 | 03-01-PLAN.md | Supporto portrait e landscape, layout adattivo, numero centrato | ✓ SATISFIED | Single adaptive `ConstraintLayout`, no `res/layout-land`; human-confirmed both orientations at Task 2 checkpoint. |
| UI-04 | 03-01-PLAN.md | Nessun menu/animazioni/elementi grafici non necessari | ✓ SATISFIED | No menus/animations/transitions found; `unitText` addition is a necessary text label (approved deviation), not decorative; round-4 fullscreen removes UI chrome rather than adding it. |
| UI-05 | 03-01-PLAN.md | Tutti i testi in italiano | ✓ SATISFIED | All copy sourced from `strings.xml`, all Italian, no hardcoded strings; new `unit_kmh` resource follows the same pattern. |

**Note (documentation staleness, not a code gap):** `.planning/REQUIREMENTS.md` still shows UI-01..UI-05 as unchecked (`[ ]`) and "Pending" in the traceability table, even though `ROADMAP.md` marks Phase 3 as complete (2026-07-10) and the code fully satisfies all five requirements. Flagged as an info-level anti-pattern below — recommend updating REQUIREMENTS.md checkboxes/traceability status during phase closure.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `.planning/REQUIREMENTS.md` | 18-22, 67-71 | UI-01..UI-05 still marked `[ ]`/"Pending" despite Phase 3 being complete and all requirements satisfied in code | ℹ️ Info | Documentation staleness only; does not affect runtime behavior or goal achievement. Recommend syncing during phase closure. |
| `app/src/main/res/xml/data_extraction_rules.xml` | 8 | `TODO` comment | ℹ️ Info | Default Android Studio template comment, unrelated to Phase 3 scope, pre-existing since project scaffold. |

No blocker or warning-level anti-patterns found in files modified by this phase (`activity_main.xml`, `MainActivity.kt`, `strings.xml`, `themes.xml`, `values-night/themes.xml`).

### Human Verification Required

Three rounds of the Task 2 checkpoint (autosize dominant number in portrait/landscape × 1/2/3 digits, status messages, permission-denied wrap, `unitText` visibility clear of the status bar) were explicitly re-tested on device/emulator and approved by the user with the word **"approvato"**, as documented in `03-01-SUMMARY.md`. These are considered closed and are not re-listed here.

A fourth round of changes — post-completion, user-requested immersive fullscreen (`enableImmersiveFullscreen()`, `NoActionBar` theme, hidden system bars with swipe-to-reveal) — was committed (`42bb31a`, `7aa01ec`) with its own "Verifica visiva richiesta (checkpoint umano)" checklist in `03-01-SUMMARY.md` (5 items). At the time this VERIFICATION.md was first generated, that round's approval had not yet been persisted to any phase artifact. **Resolution:** the user subsequently re-tested this round on the same emulator (status/nav bars hidden at launch, no ActionBar, swipe-to-reveal, `unitText` correctly positioned in both orientations, bars re-hide after returning from Settings) and confirmed with **"approvato"** in-session. This has been persisted to `03-HUMAN-UAT.md` (test #4, `passed`). All human-verification items across all four rounds are now closed.

### Gaps Summary

No code-level gaps found. All 8 must-have truths, both required key links to consumer code (`MainActivity.kt`), and all 5 requirement IDs (UI-01..UI-05) are satisfied in the current codebase, and the debug build compiles successfully. The phase goal — a dominant, centered, auto-sized, high-contrast, fully-Italian speed display adapting to both orientations — is achieved at the code level and was human-confirmed for the full scope (four rounds, all "approvato"), including the round-4 immersive fullscreen enhancement. See `03-HUMAN-UAT.md` for the complete, closed verification record.

---

*Verified: 2026-07-10*
*Verifier: Claude (gsd-verifier)*
