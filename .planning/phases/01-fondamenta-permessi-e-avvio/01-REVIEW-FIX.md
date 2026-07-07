---
phase: 01-fondamenta-permessi-e-avvio
fixed_at: 2026-07-07T14:44:46Z
review_path: .planning/phases/01-fondamenta-permessi-e-avvio/01-REVIEW.md
iteration: 1
findings_in_scope: 4
fixed: 4
skipped: 0
status: all_fixed
---

# Phase 01: Code Review Fix Report

**Fixed at:** 2026-07-07T14:44:46Z
**Source review:** .planning/phases/01-fondamenta-permessi-e-avvio/01-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 4 (Critical + Warning; Info findings IN-01, IN-02 out of scope per run flags)
- Fixed: 4
- Skipped: 0

## Fixed Issues

### CR-01: Permission state is never re-checked after returning from Settings, leaving the app stuck on the denial screen

**Files modified:** `app/src/main/java/com/sed/tachimetro/MainActivity.kt`
**Commit:** 927e3c0
**Applied fix:** Added an `onResume()` override that re-checks `ACCESS_FINE_LOCATION` permission state on every foreground resume. If granted, calls `showReady()`; if still denied and the retry button is already visible (i.e. not the very first launch), refreshes the denial UI via `showDenied()`. This fixes the path where the user grants the permission from system Settings (via `openAppSettings()`) and returns to the app without a force-kill.

### WR-01: Redundant branches in `checkAndRequestPermission()` obscure the actual logic

**Files modified:** `app/src/main/java/com/sed/tachimetro/MainActivity.kt`
**Commit:** 238b280
**Applied fix:** Collapsed the `when` expression's `shouldShowRequestPermissionRationale` branch and `else` branch (which performed the identical action) into a single `if (granted) showReady() else requestPermissionLauncher.launch(...)`, with a comment explaining why the rationale check is intentionally omitted here.

### WR-02: `permission_denied` and `permission_denied_permanent` strings are identical, defeating the purpose of having two states

**Files modified:** `app/src/main/res/values/strings.xml`
**Commit:** 814e1f8
**Applied fix:** Gave `permission_denied_permanent` its own Italian message ("Permesso GPS negato. Aprire le impostazioni per abilitarlo") distinct from the retryable-denial message, consistent with CONTEXT.md's decision that permanent denial should direct the user to Settings.

### WR-03: Code directly uses `androidx.activity` APIs without declaring a direct dependency on them

**Files modified:** `gradle/libs.versions.toml`, `app/build.gradle.kts`
**Commit:** 132bbf7
**Applied fix:** Added `activity = "1.9.3"` to `[versions]` and `activity = { group = "androidx.activity", name = "activity-ktx", version.ref = "activity" }` to `[libraries]` in the version catalog, then added `implementation(libs.activity)` to `app/build.gradle.kts`, following the project's version-catalog-first dependency convention.

## Skipped Issues

None — all in-scope findings were fixed.

## Verification

- Tier 1 (re-read modified sections): passed for all 4 fixes.
- Tier 2 (build-based syntax/resolution checks): `./gradlew.bat compileDebugKotlin` passed after CR-01 and WR-01; `./gradlew.bat processDebugResources` passed after WR-02; full `./gradlew.bat assembleDebug` passed after WR-03 (BUILD SUCCESSFUL, 33 actionable tasks, all executed).
- No logic-only findings required "requires human verification" flagging — CR-01's fix mirrors the reviewer-suggested pattern and is a behavioral addition (new lifecycle override), not a modification of existing conditional logic with ambiguous correctness. All fixes were verified via successful compilation/build.

---

_Fixed: 2026-07-07T14:44:46Z_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
