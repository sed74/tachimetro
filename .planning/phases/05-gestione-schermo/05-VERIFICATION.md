---
phase: 05-gestione-schermo
verified: 2026-07-10T00:00:00Z
status: passed
score: 8/8 must-haves verified
---

# Phase 5: Gestione Schermo Verification Report

**Phase Goal:** L'utente può scegliere se mantenere lo schermo sempre acceso durante l'uso dell'app, con la preferenza salvata tra sessioni.
**Verified:** 2026-07-10
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | L'utente vede un toggle per scegliere tra "Schermo sempre acceso" e "Schermo automatico" (SCRN-01) | VERIFIED | `activity_main.xml:89-105` — `SwitchCompat` `@+id/keepScreenOnSwitch`, bottom-left, `android:text="@string/keep_screen_on_label"` = "Sempre acceso" (strings.xml:13), no `android:visibility` attribute → always VISIBLE (D-03) |
| 2 | Con "Schermo sempre acceso" attivo, lo schermo non si spegne mentre l'app è in uso (SCRN-02) | VERIFIED | `MainActivity.kt:404-410` `applyKeepScreenOn()` adds/clears `WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON` on the window; wired to `setOnCheckedChangeListener` (line 119-122), applied immediately on toggle. Human-verified on-device (case 2, 05-02-SUMMARY.md — "approvato") |
| 3 | Chiudendo e riaprendo l'app, la preferenza scelta in precedenza è ancora impostata (SCRN-03) | VERIFIED | `ScreenOnPreferenceStore.write()` persists immediately via `SharedPreferences.edit().apply()`; `onCreate()` reads it back via `screenOnStore.read()` before setting `isChecked`. Human-verified on-device for both app-close and phone-reboot persistence (cases 3 and 4, 05-02-SUMMARY.md — "approvato") |
| 4 | Default al primo avvio derivato dallo stato di ricarica, persistito una sola volta (D-04/D-05) | VERIFIED | `MainActivity.kt:110-114`: `savedKeepOn ?: isDeviceCharging()`, write only `if (savedKeepOn == null)`; `isDeviceCharging()` (line 415-420) reads sticky `ACTION_BATTERY_CHANGED`. Human-verified on-device (case 5, 05-02-SUMMARY.md — "approvato") |
| 5 | Nessun flash di stato: `isChecked` impostato prima della registrazione del listener | VERIFIED | `MainActivity.kt:116` (`keepScreenOnSwitch.isChecked = keepOn`) precedes line 119 (`setOnCheckedChangeListener`) |
| 6 | Switch rispetta gli insets bottom+left in portrait/landscape | VERIFIED | `applyScreenSwitchWindowInsets()` (`MainActivity.kt:426-441`) mirrors `applyMaxAreaWindowInsets()`, applied at line 123. Human-verified on-device (case 6, 05-02-SUMMARY.md — "approvato") |
| 7 | Track/thumb in scala di grigi, nessuna nuova dipendenza Gradle | VERIFIED | `switch_track_tint.xml` (`#80FFFFFF`/`#33FFFFFF`), `switch_thumb_tint.xml` (`#FFFFFF`), both grayscale/alpha-only; `SwitchCompat` from existing `androidx.appcompat` dependency, no new Gradle deps added (confirmed in 05-01-SUMMARY.md `tech-stack.added: []`) |
| 8 | Nessuna sovrapposizione con retryButton in stato di permesso negato | VERIFIED | `showDenied()`/`showReady()`/`updatePlaceholder()` (MainActivity.kt) never reference `keepScreenOnSwitch` — no conditional visibility logic touches it, consistent with D-03. Human-verified visually on-device (case 7, 05-02-SUMMARY.md — "approvato") |

**Score:** 8/8 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `app/src/main/res/values/strings.xml` | `keep_screen_on_label` = "Sempre acceso" | VERIFIED | Line 13, existing strings unmodified |
| `app/src/main/res/color/switch_track_tint.xml` | Grayscale ColorStateList, `state_checked` first | VERIFIED | Exists, checked=`#80FFFFFF`, unchecked=`#33FFFFFF` |
| `app/src/main/res/color/switch_thumb_tint.xml` | White opaque ColorStateList | VERIFIED | Exists, `#FFFFFF` |
| `app/src/main/java/com/sed/tachimetro/screen/ScreenOnPreferenceStore.kt` | `read(): Boolean?` distinguishing "unset" from "false" | VERIFIED | `prefs.contains(KEY_KEEP_SCREEN_ON)` gate; `PREFS_NAME="tachimetro_prefs"`, `KEY_KEEP_SCREEN_ON="keep_screen_on"` (reuses MaxSpeedStore's file, distinct key) |
| `app/src/main/res/layout/activity_main.xml` | `keepScreenOnSwitch` bottom-left, always visible, 48dp min | VERIFIED | Present, no visibility attribute, correct constraints/margins/tint |
| `app/src/main/java/com/sed/tachimetro/MainActivity.kt` | Full wiring: default-from-charging, FLAG_KEEP_SCREEN_ON, persistence, insets | VERIFIED | All elements present and correctly ordered (see truths 2-6 above) |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|----|--------|---------|
| `MainActivity.onCreate` | `ScreenOnPreferenceStore.read()` / `isDeviceCharging()` | first-launch default fallback | WIRED | `val keepOn = savedKeepOn ?: isDeviceCharging()` (line 111) |
| `keepScreenOnSwitch` | `applyKeepScreenOn` + `ScreenOnPreferenceStore.write` | `setOnCheckedChangeListener` | WIRED | Lines 119-122, both calls present in listener body |
| `applyKeepScreenOn` | `window.addFlags/clearFlags(FLAG_KEEP_SCREEN_ON)` | direct window flag control | WIRED | Lines 404-410 |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| SCRN-01 | 05-01, 05-02 | Toggle "Schermo sempre acceso"/"Schermo automatico" | SATISFIED | keepScreenOnSwitch always visible, italian label; human-verified case 1 |
| SCRN-02 | 05-01, 05-02 | Schermo non si spegne quando attivo | SATISFIED | FLAG_KEEP_SCREEN_ON applied immediately on toggle; human-verified case 2 |
| SCRN-03 | 05-01, 05-02 | Preferenza salvata/ripristinata tra sessioni | SATISFIED | SharedPreferences write/read cycle; human-verified persistence across app-close (case 3) and phone reboot (case 4) |

No orphaned requirements — REQUIREMENTS.md maps only SCRN-01/02/03 to Phase 5, and all three are declared in both plans' `requirements` frontmatter.

### Anti-Patterns Found

No TODO/FIXME/placeholder comments, no empty stub implementations, no hardcoded-empty state flowing to render, no console-log-only handlers found in `MainActivity.kt`, `ScreenOnPreferenceStore.kt`, `activity_main.xml`, or `strings.xml`. `05-01-SUMMARY.md` explicitly declares "Known Stubs: Nessuno" and this matches the code read.

### Behavioral Spot-Checks

Not run — this phase's critical behaviors (screen-off blocking, reboot persistence, charging-state default) are hardware/OS-level effects that cannot be exercised via a single non-interactive command; they were already exhaustively covered by the documented on-device human verification in 05-02 (8/8 cases, "approvato").

### Human Verification Required

None outstanding. Plan 05-02 was itself a `checkpoint:human-verify` task covering all behaviors that require a real device/emulator (screen-off blocking, close/reopen persistence, reboot persistence, charging-state default, insets in both orientations, no overlap with retryButton, grayscale styling). The user tested all 8 checklist items on a Pixel 10 Pro emulator and explicitly approved with "approvato", documented in `05-02-SUMMARY.md`. This satisfies Step 8 — no further human verification items remain.

### Gaps Summary

None. All roadmap Success Criteria and all PLAN must-haves are verified both statically (code inspection) and behaviorally (documented on-device human approval). Code is committed (`0b99a41`, `2e82eab`, `1ff0776`, plus tracking commits `0cfe6eb`, `b0076c3`, `6cbf81f`) and working tree is clean for the relevant files.

---

*Verified: 2026-07-10*
*Verifier: Claude (gsd-verifier)*
