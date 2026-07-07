---
phase: 01-fondamenta-permessi-e-avvio
plan: 01
subsystem: infra
tags: [android, gradle, kotlin, build-config, constraintlayout, agp]

# Dependency graph
requires: []
provides:
  - Kotlin compilation enabled for the app module (via AGP built-in Kotlin support, not a separate plugin)
  - androidx.constraintlayout:constraintlayout dependency available for layout XML
  - Kotlin bytecode jvmTarget aligned to Java 11
  - Android project scaffold (app/, gradle/, build files) committed to git for the first time
affects: [01-02, gps-phase, ui-phase]

# Tech tracking
tech-stack:
  added: ["androidx.constraintlayout:constraintlayout 2.2.1"]
  patterns: ["version-catalog-first dependency management", "AGP built-in Kotlin support (no separate org.jetbrains.kotlin.android plugin in AGP 9.1.1+)"]

key-files:
  created: []
  modified:
    - gradle/libs.versions.toml
    - app/build.gradle.kts
  # Also committed for the first time (pre-existing untracked scaffold, not authored by this plan):
  #   app/, gradle/, build.gradle.kts, settings.gradle.kts, gradle.properties, gradlew, gradlew.bat, .gitignore

key-decisions:
  - "AGP 9.1.1 ships mandatory built-in Kotlin support (android.builtInKotlin, default true, deprecated flag being removed in AGP 10.0). The classic org.jetbrains.kotlin.android plugin cannot be applied alongside it (extension/classpath conflict, verified by direct testing) -- Kotlin is enabled via AGP's built-in mechanism instead of a separate version-catalog plugin entry."
  - "Kotlin jvmTarget=11 set via the `kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_11 } }` extension, which AGP's built-in Kotlin support auto-registers on the android.application plugin -- no separate plugin application needed for this DSL to work."
  - "Pre-existing Android scaffold (app/, gradle/, build.gradle.kts, settings.gradle.kts, gradlew, gradle.properties, .gitignore) was never committed to git; committed as a preparatory chore commit so the worktree (and all future phase plans) can actually track and diff these files."

patterns-established:
  - "Do NOT add org.jetbrains.kotlin.android to gradle/libs.versions.toml or app/build.gradle.kts in this project -- Kotlin compilation is already active via AGP's built-in support. Future plans creating .kt files need no additional plugin setup."

requirements-completed: [APP-01]

# Metrics
duration: 35min
completed: 2026-07-07
---

# Phase 1 Plan 1: Kotlin + ConstraintLayout Build Enablement Summary

**Kotlin compilation enabled via AGP 9.1.1's built-in Kotlin support (not the classic separate plugin, which is incompatible with this AGP version) plus ConstraintLayout 2.2.1 dependency, verified with a real `.kt` file compiling through `:app:compileDebugKotlin`.**

## Performance

- **Duration:** 35 min
- **Started:** 2026-07-07T13:21:00Z (approx, per STATE.md session start)
- **Completed:** 2026-07-07T13:38:49Z
- **Tasks:** 2 planned + 1 preparatory (scaffold-to-git) = 3 commits
- **Files modified:** 2 (gradle/libs.versions.toml, app/build.gradle.kts) + 36 newly tracked (pre-existing scaffold)

## Accomplishments
- Android project scaffold brought under git version control for the first time (was entirely untracked in the main working directory, absent from this isolated worktree)
- Kotlin compilation confirmed working end-to-end (throwaway `.kt` probe file compiled via `:app:compileDebugKotlin`, then removed) using AGP 9.1.1's built-in Kotlin support
- `androidx.constraintlayout:constraintlayout:2.2.1` declared in the version catalog and added as an `implementation` dependency, ready for the Plan 02 layout
- Kotlin bytecode jvmTarget aligned to Java 11 via `kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_11 } }`
- `./gradlew.bat clean assembleDebug` → BUILD SUCCESSFUL

## Task Commits

1. **Prep: Commit existing Android scaffold to version control** - `c49b9f6` (chore) — required because this worktree had none of `app/`, `gradle/`, or the root build files; they existed only as untracked files in the main repo directory.
2. **Task 1: Dichiarare Kotlin e ConstraintLayout nella version catalog** - `49e2a16` (feat) — initial version, later corrected in the Task 2 commit once the plugin conflict was discovered.
3. **Task 2: Applicare il plugin Kotlin e allineare jvmTarget in app/build.gradle.kts** - `05b7bc9` (fix) — includes both the app/build.gradle.kts changes and the corrective removal of the unusable `kotlin`/`kotlin-android` catalog entries added in commit `49e2a16`.

**Plan metadata:** (final docs commit made by orchestrator after wave completion, per parallel-executor instructions — not created by this agent)

## Files Created/Modified

- `gradle/libs.versions.toml` - Added `constraintlayout` version/library entries; a `kotlin` version and `kotlin-android` plugin entry were added in commit `49e2a16` then removed in commit `05b7bc9` once proven unusable in this AGP version (see Deviations)
- `app/build.gradle.kts` - Added `kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_11 } }` block and `implementation(libs.constraintlayout)`; the `alias(libs.plugins.kotlin.android)` plugins-block entry was added then removed for the same reason
- `app/`, `gradle/`, `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `gradlew`, `gradlew.bat`, `.gitignore` - Committed to git for the first time (pre-existing scaffold, content unchanged from what already existed in the main working directory)

## Decisions Made

- Kept the plan's ConstraintLayout version target (`2.2.x` series) and picked `2.2.1` — no network/Context7 access was available in this environment to verify the exact latest patch; this is the most recent stable 2.2.x release known at model training time and matches the plan's guidance ("es. 2.2.x").
- Superseded the plan's locked decision "Kotlin: plugin org.jetbrains.kotlin.android, gestito via version catalog" with "Kotlin: AGP 9.1.1 built-in support, no separate plugin" — this was not a preference choice; the classic plugin is technically incompatible with this AGP version (see Deviations for full evidence).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Pre-existing Android scaffold was never committed to git**
- **Found during:** Pre-task setup, before Task 1
- **Issue:** `app/`, `gradle/`, `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `gradlew`, `gradlew.bat`, `.gitignore` all existed only as untracked files in the main repository working directory (`git status` showed them as `??`). Since this plan runs in an isolated git worktree, none of these files were present at all — `gradle/libs.versions.toml` and `app/build.gradle.kts` (the plan's declared `files_modified`) did not exist in the worktree, making Task 1 and Task 2 impossible to execute.
- **Fix:** Copied the untracked files from the main working directory into the worktree filesystem, then `git add` + committed them as a preparatory chore commit, before starting Task 1.
- **Files modified:** 36 files (full scaffold) — see commit `c49b9f6` for the complete list.
- **Verification:** `git status --short` clean after commit; `find app gradle -type f | wc -l` matched the main repo's file count (30).
- **Committed in:** `c49b9f6`

**2. [Rule 1/3 - Bug / Blocking, environment-forced] Classic `org.jetbrains.kotlin.android` plugin is incompatible with AGP 9.1.1's built-in Kotlin support**
- **Found during:** Task 2 verification (`./gradlew.bat assembleDebug`)
- **Issue:** Applying `alias(libs.plugins.kotlin.android)` (the plan's exact specified mechanism, matching its `<decisions_locked>` section) fails with `Error resolving plugin [id: 'org.jetbrains.kotlin.android', version: '2.1.0'] > The request for this plugin could not be satisfied because the plugin is already on the classpath with an unknown version, so compatibility cannot be checked`. Investigation of the AGP 9.1.1 jar confirmed it ships classes for `builtInKotlinSupportMode` and a `BUILT_IN_KOTLIN` boolean option (`android.builtInKotlin`). Testing `-Pandroid.builtInKotlin=false` produced Gradle's own deprecation warning: *"The option setting 'android.builtInKotlin=false' is deprecated. The current default is 'true'. It will be removed in version 10.0 of the Android Gradle plugin."* — confirming built-in Kotlin is mandatory and default-on in this AGP line, and the override path is being removed entirely. Applying the classic plugin via a bare `id("org.jetbrains.kotlin.android")` (no version) produced a second, different failure: `Cannot add extension with name 'kotlin', as there is an extension already registered with that name` — proving AGP's `android.application` plugin already registers the `kotlin { }` extension itself.
- **Fix:** Did not apply any separate Kotlin plugin. Kept only `alias(libs.plugins.android.application)` in the `plugins { }` block. The `kotlin { compilerOptions { jvmTarget = ... } }` block works unmodified because AGP's built-in support already registers that extension. Removed the now-unusable `kotlin` version and `kotlin-android` plugin entries from `gradle/libs.versions.toml` (left a comment explaining why, pointing back to this summary). Verified Kotlin compilation genuinely works by adding a throwaway `.kt` probe file (`app/src/main/java/com/sed/tachimetro/_KotlinProbe.kt`), confirming `:app:compileDebugKotlin` actually ran (not `NO-SOURCE`) and the build succeeded, then deleted the probe file before committing (never part of any commit).
- **Files modified:** `gradle/libs.versions.toml`, `app/build.gradle.kts`
- **Verification:** `./gradlew.bat clean assembleDebug` → BUILD SUCCESSFUL (32 tasks executed, including `:app:compileDebugKotlin`)
- **Committed in:** `05b7bc9`

---

**Total deviations:** 2 auto-fixed (1 Rule 3 environment-setup blocker, 1 Rule 1/3 environment-forced build-mechanism substitution)
**Impact on plan:** The plan's literal `must_haves` artifact expectations (`gradle/libs.versions.toml` containing `kotlin-android`, `app/build.gradle.kts` containing `libs.plugins.kotlin.android`) are **not** met as literally worded — they are technically impossible to satisfy in this AGP 9.1.1 / Gradle 9.3.1 environment without breaking the build. The plan's actual objective (Kotlin enabled so future `.kt` files compile, ConstraintLayout available, jvmTarget 11) **is** fully met and verified via a real compiled Kotlin file and a clean successful build. No scope creep beyond what was strictly necessary to reach a working build.

## Issues Encountered

- No Context7/web access was available to verify the exact latest-stable Kotlin/ConstraintLayout version numbers per the plan's instructions ("verificare via Context7... prima di fissarla"). Ultimately moot for Kotlin (no version is declared at all, per the deviation above); for ConstraintLayout, `2.2.1` was used based on training-time knowledge of the 2.2.x stable series, matching the plan's own suggested example version. If a newer 2.2.x patch exists, bumping the `constraintlayout` version in `gradle/libs.versions.toml` is a trivial follow-up.

## User Setup Required

None - no external service configuration required. Note: `local.properties` (containing `sdk.dir`) is gitignored and machine-local; it was present in the main working directory and was copied into this worktree only to run build verification — it remains untracked/ignored and was never committed.

## Next Phase Readiness

- Plan 01-02 (permissions/activity work, per ROADMAP) can now create `.kt` files under `app/src/main/java/com/sed/tachimetro/` — they will compile automatically via AGP's built-in Kotlin support, no further plugin setup required.
- ConstraintLayout is available for the Plan 02 placeholder layout.
- **Important note for future phases/plans:** do not attempt to add `org.jetbrains.kotlin.android` to the version catalog or `app/build.gradle.kts` — it will break the build in this AGP version. If `CONVENTIONS.md`/`ARCHITECTURE.md`/`PATTERNS.md` in later phases reference "apply the Kotlin plugin first," they should be corrected to reflect that Kotlin is already active via AGP's built-in support.
- The Android project is now under git version control; future plans will diff cleanly against this baseline.

---
*Phase: 01-fondamenta-permessi-e-avvio*
*Completed: 2026-07-07*
