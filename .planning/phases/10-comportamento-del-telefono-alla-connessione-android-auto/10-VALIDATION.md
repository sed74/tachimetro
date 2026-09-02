---
phase: 10
slug: comportamento-del-telefono-alla-connessione-android-auto
status: draft
nyquist_compliant: true
wave_0_complete: true
created: 2026-09-02
---

# Phase 10 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.
>
> Reconstructed retroactively via `/gsd-validate-phase` (State B: no VALIDATION.md existed, 3 SUMMARY.md files present). No gaps found — every task already carried an automated `<verify>` command, so no Wave 0 backfill or gsd-nyquist-auditor spawn was needed.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4.13.2 (JVM unit tests, `app/src/test/`) |
| **Config file** | none — standard Gradle Android test source sets, no separate test config |
| **Quick run command** | `./gradlew.bat :app:testDebugUnitTest --tests 'com.sed.tachimetro.car.CarLinkStateTest' --tests 'com.sed.tachimetro.car.CarLinkSequenceTest' --console=plain` |
| **Full suite command** | `./gradlew.bat :app:testDebugUnitTest --console=plain` |
| **Estimated runtime** | ~15–30s (pure JVM tests, no emulator/device required) |

---

## Sampling Rate

- **After every task commit:** Run the quick run command above (targets this phase's two test files)
- **After every plan wave:** Run `./gradlew.bat :app:testDebugUnitTest --console=plain` (full regression, all 10 test files project-wide)
- **Before `/gsd:verify-work`:** Full suite must be green, plus `./gradlew.bat :app:assembleDebug --console=plain -q`
- **Max feedback latency:** ~30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 10-01-T1 | 01 | 1 | CONN-01, CONN-02 | T-10-01 / T-10-02 / T-10-03 | `resolveCarLinkState` fail-safe allow-list (only `CONNECTION_TYPE_PROJECTION` → `Connected`); `resolveEffectiveKeepScreenOn` stateless truth table, no access to persisted preference | unit | `./gradlew.bat :app:testDebugUnitTest --tests 'com.sed.tachimetro.car.CarLinkStateTest'` | ✅ | ✅ green |
| 10-01-T2 | 01 | 1 | CONN-01 | T-10-05 | Static string resource `android_auto_connected`, no format placeholders, purely additive diff | build+grep | `./gradlew.bat :app:assembleDebug -q` + grep on exact string text | ✅ | ✅ green |
| 10-02-T1 | 02 | 2 | CONN-01 | — | `renderSpeedArea()` neutral-state branch on `carLink is CarLinkState.Connected`; accumulation guard unaffected | build+grep+regression | `./gradlew.bat :app:assembleDebug -q` + structural grep + `testDebugUnitTest` | ✅ | ✅ green |
| 10-02-T2 | 02 | 2 | CONN-01, CONN-02 | T-10-08 | `CarConnection` observer wired via `applicationContext`; `onCarLinkChanged()` never calls `screenOnStore.write(` (count locked at 2 project-wide) | build+grep+regression | `./gradlew.bat :app:assembleDebug -q` + structural grep (`screenOnStore.write(` count, `applyKeepScreenOn(` count) + `testDebugUnitTest` | ✅ | ✅ green |
| 10-03-T1 | 03 | 3 | CONN-01, CONN-02 | — | No-drift property: any sequence/length of connect↔disconnect transitions restores exactly the saved preference | unit | `./gradlew.bat :app:testDebugUnitTest --tests 'com.sed.tachimetro.car.CarLinkSequenceTest'` | ✅ | ✅ green |
| 10-03-T2 | 03 | 3 | CONN-01, CONN-02 (Roadmap SC1–SC3) | — | Live end-to-end confirmation on physical device + real Android Auto host across scenarios A–G | checkpoint:human-verify | N/A — requires physical Android Auto connection, not automatable | ✅ (documented in 10-03-SUMMARY.md) | ✅ green (human, documented "approvato") |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

All 6 tasks re-run clean at audit time (`./gradlew.bat :app:testDebugUnitTest` → `BUILD SUCCESSFUL`, per `10-VERIFICATION.md` Behavioral Spot-Checks).

---

## Wave 0 Requirements

*None — existing infrastructure covers all phase requirements.* `CarLinkStateTest.kt` (11 assertions) and `CarLinkSequenceTest.kt` (5 tests) were written during phase execution itself (Plans 01 and 03), not backfilled by this audit.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Live Android Auto connect/disconnect on a physical device against a real head unit / DHU, confirming screen-state transition, neutral message, and no regression on the always-on toggle / max speed / distance accumulation | CONN-01, CONN-02 (Roadmap SC1–SC3) | Requires a genuine `androidx.car.app.connection.CarConnection` transition driven by the Android Auto host app — the project has no Robolectric/Espresso Activity harness and `androidx.car.app-testing` (already a dependency) covers `Session`/`Screen` unit tests, not real host connection-state changes | See `10-03-PLAN.md` Task 2 checklist (scenarios A1–G1): `adb install` the debug build, `adb logcat -s TachimetroPhone TachimetroCar`, connect/disconnect via DHU or a real head unit, inspect `tachimetro_prefs.xml` via `run-as` to confirm the persisted preference is untouched |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references (none found — N/A)
- [x] No watch-mode flags
- [x] Feedback latency < 30s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** approved 2026-09-02 (retroactive audit, no gaps found)

---

## Known Non-Blocking Findings (carried from 10-REVIEW.md / 10-VERIFICATION.md)

Not gaps against CONN-01/CONN-02 as written, but disclosed for visibility since they touch the same code:

- **WR-01** — transient stale `carLink` window on cold-launch-already-connected or resume-after-background-change (async `LiveData` delivery vs. synchronous field read in `showReady()`/`applyKeepScreenOn()`). Self-correcting, zero automated coverage (would require Activity-lifecycle + async timing tests outside this project's pure-function-only test strategy), not exercised by the DHU checklist either (all DHU scenarios start from an already-foregrounded, settled app). Does not fail any Phase 10 must-have or Roadmap Success Criterion.
- **IN-01** — `carLink is CarLinkState.Connected` neutral-message branch duplicated in `showReady()` and `renderSpeedArea()`. Maintenance risk only.
