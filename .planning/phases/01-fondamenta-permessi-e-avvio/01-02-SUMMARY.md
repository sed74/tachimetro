---
phase: 01-fondamenta-permessi-e-avvio
plan: 02
subsystem: android-app
tags: [android, kotlin, permissions, activity, location, constraintlayout]

# Dependency graph
requires:
  - phase: 01-fondamenta-permessi-e-avvio (plan 01)
    provides: Kotlin compilation via AGP built-in support, ConstraintLayout dependency
provides:
  - MainActivity registered as LAUNCHER activity (app opens directly, no menu)
  - ACCESS_FINE_LOCATION permission declaration + full request/grant/deny/deny-permanently flow
  - Black placeholder screen (messageText + retryButton) shown once permission is resolved
  - Italian-only user-facing strings for the permission flow
affects: [gps-phase, ui-phase]

# Tech tracking
tech-stack:
  added: []
  patterns: ["registerForActivityResult(ActivityResultContracts.RequestPermission()) for permission requests (non-deprecated API)", "shouldShowRequestPermissionRationale() to distinguish denied vs permanently-denied", "Settings.ACTION_APPLICATION_DETAILS_SETTINGS with Uri.fromParts(\"package\", packageName, null) for the permanently-denied recovery path"]

key-files:
  created:
    - app/src/main/res/layout/activity_main.xml
    - app/src/main/java/com/sed/tachimetro/MainActivity.kt
  modified:
    - app/src/main/AndroidManifest.xml
    - app/src/main/res/values/strings.xml

key-decisions:
  - "Followed the plan's locked decisions exactly: permission requested immediately in onCreate (no intermediate screens), denial shows Italian message + Riprova button, permanent denial swaps the button to Apri impostazioni and opens app-specific Settings."

patterns-established:
  - "First Activity/layout precedent for the app: single LAUNCHER activity, ConstraintLayout-based View XML (no Compose), Italian-only strings via getString(R.string.*), android.* then androidx.* import grouping."

requirements-completed: []  # NOT marked complete - Task 3 (human-verify checkpoint) has not been approved yet. See "Status" below.

# Metrics
duration: 18min
completed: 2026-07-07
---

# Phase 1 Plan 2: MainActivity + ACCESS_FINE_LOCATION Permission Flow Summary

**MainActivity registered as the app's LAUNCHER activity with a complete ACCESS_FINE_LOCATION permission flow (grant/deny/deny-permanently) and a black "Pronto" placeholder screen, verified to compile via `./gradlew.bat assembleDebug` — BUILD SUCCESSFUL. Awaiting human device verification (Task 3 checkpoint).**

## Status: PAUSED AT CHECKPOINT

Tasks 1 and 2 (both `type="auto"`) are complete, committed, and build-verified. Task 3 is a `type="checkpoint:human-verify"` (`gate="blocking"`) that requires installing the app on a real Android device/emulator to exercise the runtime permission popup — this cannot be verified from a build alone and was not fabricated. See "Checkpoint Details" below for exact verification steps. This plan is **not** complete; requirements APP-01/PERM-01/PERM-02 are implemented but not yet human-confirmed, so `requirements-completed` is intentionally left empty in this summary.

## Performance

- **Duration:** 18 min (Task 1 + Task 2, up to the checkpoint)
- **Started:** 2026-07-07T13:52:37Z
- **Completed (up to checkpoint):** 2026-07-07T14:10:00Z (approx)
- **Tasks:** 2 of 3 completed (Task 3 is the pending checkpoint)
- **Files modified:** 4 (2 modified, 2 created)

## Accomplishments
- `AndroidManifest.xml` now declares exactly one permission (`ACCESS_FINE_LOCATION`, no COARSE/background) and registers `MainActivity` as the LAUNCHER activity (`exported="true"`, MAIN/LAUNCHER intent-filter)
- `strings.xml` extended with 5 Italian strings for the permission flow (`status_ready`, `permission_denied`, `permission_denied_permanent`, `retry`, `open_settings`)
- `activity_main.xml` created: black `ConstraintLayout` placeholder with `messageText` (TextView) and `retryButton` (Button, hidden by default)
- `MainActivity.kt` created: full permission flow using `registerForActivityResult(ActivityResultContracts.RequestPermission())`, `ContextCompat.checkSelfPermission`, `shouldShowRequestPermissionRationale`, and `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` for the permanently-denied recovery path
- `./gradlew.bat assembleDebug` → BUILD SUCCESSFUL, with `:app:compileDebugKotlin` actually executing (proving `MainActivity.kt` compiles and all `R.*` references resolve against Task 1's manifest/strings/layout)

## Task Commits

Each task was committed atomically:

1. **Task 1: Registrare permesso + LAUNCHER activity e definire stringhe e layout placeholder** - `cc38cc8` (feat)
2. **Task 2: Implementare MainActivity con il flusso completo del permesso** - `48f16ae` (feat)

**Task 3 (checkpoint:human-verify) not yet reached/approved** - no commit; awaiting device verification.

**Plan metadata:** deferred (SUMMARY committed by this agent per worktree policy; orchestrator handles STATE.md/ROADMAP.md/REQUIREMENTS.md updates after wave completion and, for this plan, after checkpoint approval)

## Files Created/Modified
- `app/src/main/AndroidManifest.xml` - Added `<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />` and registered `.MainActivity` as the exported LAUNCHER activity with a MAIN/LAUNCHER intent-filter
- `app/src/main/res/values/strings.xml` - Added `status_ready`, `permission_denied`, `permission_denied_permanent`, `retry`, `open_settings` (all Italian, `app_name` unchanged)
- `app/src/main/res/layout/activity_main.xml` - New file: black `ConstraintLayout` with `messageText` (TextView) and `retryButton` (Button, `visibility="gone"` by default)
- `app/src/main/java/com/sed/tachimetro/MainActivity.kt` - New file: `AppCompatActivity` subclass implementing `checkAndRequestPermission()`, `onRetryClicked()`, `openAppSettings()`, `showReady()`, `showDenied()`

## Decisions Made
None beyond the plan's locked decisions — followed `01-CONTEXT.md`/`01-PATTERNS.md` exactly (immediate permission request in `onCreate`, Italian denial message + Riprova, permanent-denial opens app Settings, black placeholder with only "Pronto" text when granted).

## Deviations from Plan

None - plan executed exactly as written for Tasks 1 and 2. No auto-fixes, no bugs found, no missing critical functionality, no blocking issues, and no architectural changes were required. The Kotlin-enablement deviation documented in `01-01-SUMMARY.md` (AGP built-in Kotlin support instead of the classic plugin) was already in effect and required no action here — `MainActivity.kt` compiled without any additional plugin setup, confirming that summary's "Next Phase Readiness" note.

One environment-only action (not a plan deviation): `local.properties` (gitignored, machine-local SDK path) was copied from the main working directory into this fresh worktree so that `./gradlew.bat assembleDebug` could run for verification. It remains untracked/ignored and was never committed, consistent with how Plan 01-01 handled the same file.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Tasks 1 and 2 are complete, committed, and build-verified (`BUILD SUCCESSFUL`, `:app:compileDebugKotlin` executed).
- **This plan is not complete.** Task 3 is a blocking human-verify checkpoint requiring a real Android 11+ (API 30+) device or emulator to confirm the runtime permission popup behavior end-to-end: direct app launch (APP-01), immediate permission popup, grant → "Pronto" screen, deny → Italian message + "Riprova", permanent deny → "Apri impostazioni" opening the app's Settings page, and persistence of the granted state across app restarts (PERM-02). None of this is verifiable from `assembleDebug` alone.
- A fresh agent (per GSD checkpoint protocol) should resume from Task 3 using the `## CHECKPOINT REACHED` details below once a device/emulator is available and the user has completed the verification steps.
- All code needed for GPS-phase and UI-phase work builds on `MainActivity.kt`'s permission-granted path (`showReady()`); no further permission-handling changes are expected in those phases per PERM-01/PERM-02 scope.

---
*Phase: 01-fondamenta-permessi-e-avvio*
*Completed: paused at checkpoint, 2026-07-07*

## Self-Check: PASSED

- FOUND: app/src/main/AndroidManifest.xml
- FOUND: app/src/main/res/values/strings.xml
- FOUND: app/src/main/res/layout/activity_main.xml
- FOUND: app/src/main/java/com/sed/tachimetro/MainActivity.kt
- FOUND commit: cc38cc8 (feat: registrare permesso + LAUNCHER activity + stringhe + layout placeholder)
- FOUND commit: 48f16ae (feat: implementare MainActivity con flusso completo permesso)
