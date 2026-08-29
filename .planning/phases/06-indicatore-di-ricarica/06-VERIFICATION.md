---
phase: 06-indicatore-di-ricarica
verified: 2026-08-29T19:09:40Z
status: passed
score: 9/9 must-haves verified
overrides_applied: 0
---

# Phase 6: Indicatore di Ricarica Verification Report

**Phase Goal:** L'utente riconosce immediatamente quando il telefono è in carica, tramite un'icona a fulmine animata posizionata accanto al toggle "sempre acceso".
**Verified:** 2026-08-29T19:09:40Z
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Quando il telefono viene collegato alla ricarica, l'icona a fulmine appare accanto al toggle "sempre acceso" (Roadmap SC1 / CHRG-01) | ✓ VERIFIED | `MainActivity.updateChargingIcon()` sets `chargingIcon.visibility = View.VISIBLE` on `ChargingState.Pulsing`/`Full` (MainActivity.kt:370-378); layout wires `chargingIcon` at `layout_constraintStart_toStartOf="parent"` adjacent to `keepScreenOnSwitch` (activity_main.xml:89-102). Confirmed on real hardware: 06-04-SUMMARY.md checklist point 2 = PASS ("Approvo") |
| 2 | Quando il telefono viene scollegato dalla ricarica, l'icona a fulmine scompare immediatamente (Roadmap SC2 / CHRG-01) | ✓ VERIFIED | `updateChargingIcon()` `Hidden` branch sets `View.GONE` + calls `stopChargingFillAnimation()` which `cancel()`s the animator immediately (not waiting for cycle end) (MainActivity.kt:362-369, 423-427). Confirmed on real hardware: 06-04-SUMMARY.md checklist point 4 = PASS |
| 3 | Durante la ricarica, l'icona anima un riempimento continuo dal basso verso l'alto, bianco → lime → bianco, loop ~2-3s (Roadmap SC3 / CHRG-02) | ✓ VERIFIED | `charging_flash_fill.xml` uses `ClipDrawable` with `clipOrientation="vertical"` + `gravity="bottom"` (fills bottom-up); `startChargingFillAnimation()` uses `ValueAnimator.ofInt(0, 10_000)` with `duration=1250L` + `REVERSE` + `INFINITE` = 2500ms full cycle, `AccelerateDecelerateInterpolator` (MainActivity.kt:397-409). Confirmed on real hardware: 06-04-SUMMARY.md checklist point 3 = PASS ("ciclo ~2,5s, movimento morbido") |
| 4 | Nessun'altra icona, colore o animazione compare altrove nell'interfaccia (Roadmap SC4 / D-04) | ✓ VERIFIED | `grep -rn 'lime_charging_accent\|AEEA00' app/src/main/res/` returns only `colors.xml` (declaration) and `ic_charging_flash_lime.xml` (sole consumer) — no other resource references lime. `MainActivity.kt` uses "lime" only in code comments, never assigns it to another view. Confirmed on real hardware: 06-04-SUMMARY.md checklist point 8 = PASS |
| 5 | A batteria piena con cavo collegato, l'animazione si ferma e l'icona resta lime piena (D-03, plan 06-03 must-have) | ✓ VERIFIED | `updateChargingIcon()` `Full` branch calls `freezeChargingFillAtFull()`, which `cancel()`s the animator and sets `level = CHARGING_FILL_LEVEL_MAX` (10,000 = fully lime, no motion) (MainActivity.kt:374-378, 414-418). Confirmed on real hardware: 06-04-SUMMARY.md checklist point 5 = PASS |
| 6 | L'icona non finisce mai dietro la navigation bar o un cutout, in entrambi gli orientamenti (plan 06-03 must-have) | ✓ VERIFIED | `applyBottomLeftWindowInsets()` applies `extraBottom` to `keepScreenOnSwitch.bottomMargin` and `extraStart` to `chargingIcon.marginStart`, computed from `systemBars` + `displayCutout` insets (MainActivity.kt:565-583). Confirmed on real hardware: 06-04-SUMMARY.md checklist point 7 = PASS ("mai coperti") |
| 7 | L'animazione non continua a girare quando l'app è in background (plan 06-03 must-have) | ✓ VERIFIED | `override fun onStop()` calls `stopChargingFillAnimation()` before `super.onStop()` (MainActivity.kt:210-213), addressing that `repeatOnLifecycle(STARTED)` alone does not cancel an already-running `ValueAnimator`. Confirmed on real hardware: 06-04-SUMMARY.md checklist point 6 = PASS (no slowdown, resumes pulsing on foreground) |
| 8 | Lo stato di ricarica è osservabile in modo continuo (non one-shot), cambia senza riavviare l'app (plan 06-02 must-have) | ✓ VERIFIED | `ChargingStateProvider.state` is a `StateFlow` fed by a `callbackFlow`-wrapped `BroadcastReceiver` on the sticky `ACTION_BATTERY_CHANGED` intent, collected inside `repeatOnLifecycle(STARTED)` in `MainActivity.onCreate()` (ChargingStateProvider.kt:45-69, MainActivity.kt:176-180); distinct from the pre-existing one-shot `isDeviceCharging()` which is untouched and only used once at first-launch default |
| 9 | Il BroadcastReceiver viene deregistrato quando nessuno osserva più lo stato (plan 06-02 must-have) | ✓ VERIFIED | `callbackFlow` block ends with `awaitClose { appContext.unregisterReceiver(receiver) }`, combined with `stateIn(started = SharingStarted.WhileSubscribed())` (ChargingStateProvider.kt:57, 63-69); `close()` additionally cancels the owning scope, called from `MainActivity.onDestroy()` (MainActivity.kt:236) |

**Score:** 9/9 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `app/src/main/res/drawable/ic_charging_flash.xml` | White 24x24dp Material flash_on glyph | ✓ VERIFIED | Contains exact `pathData="M7,2v11h3v9l7,-12h-4l4,-8z"`, `fillColor="#FFFFFFFF"`, viewport 24x24 |
| `app/src/main/res/drawable/ic_charging_flash_lime.xml` | Lime copy of same glyph | ✓ VERIFIED | Identical pathData/viewport, `fillColor="@color/lime_charging_accent"` |
| `app/src/main/res/drawable/charging_flash_fill.xml` | layer-list: base + vertical bottom-gravity clip | ✓ VERIFIED | Contains `@+id/chargingIconBase`, `@+id/chargingIconFill`, `clipOrientation="vertical"`, `gravity="bottom"` |
| `app/src/main/res/values/colors.xml` | `lime_charging_accent` | ✓ VERIFIED | `#FFAEEA00` present, single declaration |
| `app/src/main/res/layout/activity_main.xml` | `chargingIcon` ImageView + re-chained switch | ✓ VERIFIED | ImageView present, 24dp, `gone` default, anchored to `@id/keepScreenOnSwitch` top/bottom (D-06); switch re-anchored `layout_constraintStart_toEndOf="@id/chargingIcon"` with 8dp margin |
| `app/src/main/java/com/sed/tachimetro/charging/ChargingState.kt` | sealed Hidden/Pulsing/Full | ✓ VERIFIED | `sealed class ChargingState` with 3 `data object`s, matches `SpeedState.kt` style |
| `app/src/main/java/com/sed/tachimetro/charging/ChargingStateProvider.kt` | Continuous StateFlow + pure `deriveChargingState` + `close()` | ✓ VERIFIED | 94 lines; `class ChargingStateProvider(context: Context)`, `val state: StateFlow<ChargingState>`, `fun close()`, top-level `deriveChargingState` |
| `app/src/test/java/com/sed/tachimetro/charging/ChargingStateProviderStateTest.kt` | 6 JVM tests, no Robolectric/coroutines-test | ✓ VERIFIED | 6 `@Test` methods present, imports only `BatteryManager`/JUnit; test run confirms 6/6 pass, 0 failures |
| `app/src/main/java/com/sed/tachimetro/MainActivity.kt` | Binding, animation helpers, collector, insets migration | ✓ VERIFIED | `chargingIcon`/`chargingFillLayer`/`chargingFillAnimator`/`chargingStateProvider` fields; `resolveChargingFillLayer`/`startChargingFillAnimation`/`freezeChargingFillAtFull`/`stopChargingFillAnimation`/`updateChargingIcon` functions all present and invoked |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| `activity_main.xml` | `charging_flash_fill.xml` | `android:src` | ✓ WIRED | `android:src="@drawable/charging_flash_fill"` on `chargingIcon` |
| `charging_flash_fill.xml` | `ic_charging_flash_lime.xml` | clip item drawable | ✓ WIRED | `<clip android:drawable="@drawable/ic_charging_flash_lime" ...>` |
| `ChargingStateProvider.kt` | `Intent.ACTION_BATTERY_CHANGED` | callbackFlow + registered receiver | ✓ WIRED | `IntentFilter(Intent.ACTION_BATTERY_CHANGED)` registered via `ContextCompat.registerReceiver(..., RECEIVER_NOT_EXPORTED)` |
| `ChargingStateProvider.kt` | `deriveChargingState` | `.map {}` on raw flow | ✓ WIRED | `rawBatteryStatus.map { status -> deriveChargingState(status) }` |
| `MainActivity.kt` | `ChargingStateProvider.state` | `collect` inside `repeatOnLifecycle(STARTED)` | ✓ WIRED | `chargingStateProvider.state.collect { state -> updateChargingIcon(state) }` (MainActivity.kt:178) |
| `MainActivity.kt` | `R.id.chargingIconFill` | `LayerDrawable.findDrawableByLayerId` | ✓ WIRED | `resolveChargingFillLayer()`: `layerDrawable?.findDrawableByLayerId(R.id.chargingIconFill) as? ClipDrawable` |
| `onCreate()` | `resolveChargingFillLayer()` | direct invocation after view binding | ✓ WIRED | Called immediately after `chargingIcon = findViewById(R.id.chargingIcon)` (MainActivity.kt:129-133), before insets setup |
| `MainActivity.kt` | `chargingStateProvider.close()` | `onDestroy()` | ✓ WIRED | Present in `onDestroy()` alongside `gpsSpeedProvider.close()` |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|---------------------|--------|
| `chargingIcon` visibility/level | `ChargingState` (Hidden/Pulsing/Full) | `ChargingStateProvider.state` ← `ACTION_BATTERY_CHANGED` sticky broadcast (real OS battery status, not mocked/static) | Yes | ✓ FLOWING |

No hardcoded/static fallback found — `deriveChargingState` is a pure fail-closed mapper over the real `EXTRA_STATUS` extra delivered by the platform broadcast; verified this maps correctly to UI state via `updateChargingIcon()` and, independently, via live hardware test in 06-04.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Project compiles with all charging code wired | `./gradlew.bat :app:assembleDebug` | `BUILD SUCCESSFUL` | ✓ PASS |
| `deriveChargingState` unit tests pass | `./gradlew.bat :app:testDebugUnitTest --tests "com.sed.tachimetro.charging.*"` | 6/6 tests pass, 0 failures (verified via `TEST-com.sed.tachimetro.charging.ChargingStateProviderStateTest.xml`) | ✓ PASS |
| No lime color leaks outside intended files | `grep -rn 'lime_charging_accent\|AEEA00' app/src/main/res/` | Only `colors.xml` + `ic_charging_flash_lime.xml` | ✓ PASS |
| Manifest unchanged (no static receiver/permission added) | `grep -n 'BATTERY\|charging\|RECEIVER' app/src/main/AndroidManifest.xml` | No matches | ✓ PASS |
| Live device behavior (icon appear/disappear/animate/freeze/insets/no-stray-color) | N/A — requires physical charger plug/unplug | See 06-04-SUMMARY.md: all 8 checklist points PASS, user replied "Approvo" | ✓ PASS (human-verified, see note below) |

Note: item 5 above cannot be exercised by an automated command (requires physically connecting/disconnecting a charger to real hardware). Per task instruction, the genuine human verification performed and documented in `06-04-SUMMARY.md` (real device, real charger, 8/8 checklist points confirmed by the actual user) is accepted as evidence in lieu of a fresh human-verification request from this report.

### Probe Execution

Step 7c: SKIPPED — no `scripts/*/tests/probe-*.sh` files exist in this repository and neither PLAN nor SUMMARY files for this phase reference any probe script. Not a migration/CLI tooling phase.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|--------------|--------|----------|
| CHRG-01 | 06-01, 06-02, 06-03, 06-04 | L'utente vede un'icona a fulmine accanto al toggle "sempre acceso" solo quando il telefono è in carica | ✓ SATISFIED | Truths 1, 2, 8, 9 above; REQUIREMENTS.md marked `[x]` |
| CHRG-02 | 06-01, 06-03, 06-04 | L'icona anima un riempimento progressivo dal basso verso l'alto, bianco → lime → bianco, in loop continuo (~2-3s per ciclo) per tutta la durata della ricarica | ✓ SATISFIED | Truths 3, 5 above; REQUIREMENTS.md marked `[x]` |

No orphaned requirements: REQUIREMENTS.md traceability table maps only CHRG-01 and CHRG-02 to Phase 6, and both appear in the `requirements:` frontmatter of every plan in this phase.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `app/src/main/java/com/sed/tachimetro/MainActivity.kt` | 547-552 | `isDeviceCharging()` duplicates `deriveChargingState()` logic instead of reusing it (flagged in 06-REVIEW.md as WR-01) | ℹ️ Info | Code-quality/maintainability concern only — does not affect CHRG-01/CHRG-02 behavior since `isDeviceCharging()` is explicitly scoped (by design, per plan constraints) to a one-shot first-launch default and is never called from the new continuous charging-indicator path. Not a blocker to phase goal. |
| `app/src/main/res/values/strings.xml` | 9 | Unused string `speed_kmh_format` (06-REVIEW.md IN-01) | ℹ️ Info | Pre-existing dead resource, unrelated to this phase's scope |
| `app/src/main/java/com/sed/tachimetro/MainActivity.kt` | 362-380 | Static content-description doesn't distinguish Pulsing vs Full for accessibility (06-REVIEW.md IN-02) | ℹ️ Info | Accessibility polish item, not a functional gap in the roadmap success criteria |
| `app/src/main/java/com/sed/tachimetro/MainActivity.kt` | 227-238 | `chargingStateProvider.close()` not guarded against partially-initialized Activity (06-REVIEW.md IN-03) | ℹ️ Info | Mirrors pre-existing pattern for `gpsSpeedProvider`; theoretical edge case, not a regression |

No TBD/FIXME/XXX/TODO/HACK/PLACEHOLDER markers found in any file modified by this phase (`grep -n -E "TBD|FIXME|XXX|TODO|HACK|PLACEHOLDER"` returned no matches across all 8 modified/created source and resource files).

### Human Verification Required

None outstanding. Plan 06-04 was a `checkpoint:human-verify` task that already executed the full 8-point on-device checklist (charger plug/unplug, animation direction/timing, full-battery freeze, background behavior, rotation/insets, no-stray-color) against real hardware. The user responded "Approvo" to all 8 points, documented verbatim in `06-04-SUMMARY.md`. Per this verification's scope instructions, that is accepted as genuine completed human verification evidence, not a pending gap.

Two cosmetic refinement requests were raised by the user *after* approving all 8 checklist points (larger icon; instant-drain instead of symmetric REVERSE animation). These are documented in `06-04-SUMMARY.md` as explicitly out-of-scope follow-ups for CHRG-01/CHRG-02, deferred to a separate quick task, and do not block this phase's success criteria — the user approved the phase as delivered before raising them.

### Gaps Summary

No gaps found. All 9 observable truths (4 roadmap Success Criteria + 5 plan-level supporting truths) are verified through a combination of: (1) direct code inspection matching the plan's binding interface contracts exactly, (2) a green `assembleDebug` + `testDebugUnitTest` build with 6/6 charging-domain unit tests passing, (3) grep-based confirmation that the lime accent color and animation are confined exclusively to the charging icon (D-04), and (4) genuine on-device human verification of all visual/hardware-dependent behavior, approved by the real user in 06-04-SUMMARY.md. Code review (06-REVIEW.md) found zero critical issues and one non-blocking duplication warning, which does not affect goal achievement.

---

_Verified: 2026-08-29T19:09:40Z_
_Verifier: Claude (gsd-verifier)_
