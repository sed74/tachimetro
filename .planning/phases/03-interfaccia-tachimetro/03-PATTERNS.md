# Phase 3: Interfaccia Tachimetro - Pattern Map

**Mapped:** 2026-07-07
**Files analyzed:** 1
**Analogs found:** 1 / 1 (self-modification — no sibling layout files exist in this project)

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|--------------------|------|-----------|-----------------|----------------|
| `app/src/main/res/layout/activity_main.xml` | layout (view/component definition) | event-driven (re-rendered by `MainActivity.kt` on every `SpeedState`/permission change) | `app/src/main/res/layout/activity_main.xml` (current version, Phase 1-2 baseline) | exact (self, evolve in place) |

No other layout XML files exist in `app/src/main/res/layout/` — this is the project's only screen, so there is no sibling analog to borrow from. The analog is the file's own current content, which must be evolved (not replaced) per CONTEXT.md D-01–D-05 and UI-SPEC.md.

`app/src/main/java/com/sed/tachimetro/MainActivity.kt` is **not modified** this phase (confirmed by CONTEXT.md line 67 and UI-SPEC's "visual-only" framing) — it is included below only as the **consumer contract** that constrains what the layout must keep intact (view IDs, no new views required).

`app/src/main/res/values/themes.xml` is **not modified** — UI-SPEC.md specifies `android:textFontWeight="900"` and `android:textStyle="bold"` as direct view attributes on `messageText`, not a style/theme resource. No new style is needed.

## Pattern Assignments

### `app/src/main/res/layout/activity_main.xml` (layout, event-driven)

**Analog:** itself, current committed version (Phase 1/2 baseline, reproduced in full below — 37 lines, already read in full, no re-read needed)

**Current full content (baseline to evolve):**
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@android:color/black">

    <TextView
        android:id="@+id/messageText"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textColor="@android:color/white"
        android:textSize="20sp"
        android:textStyle="bold"
        app:layout_constraintBottom_toTopOf="@id/retryButton"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintVertical_chainStyle="packed"
        tools:text="Pronto" />

    <Button
        android:id="@+id/retryButton"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:visibility="gone"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@id/messageText"
        tools:text="Riprova"
        tools:visibility="visible" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

**What must be preserved as-is (do not touch):**
- Root `androidx.constraintlayout.widget.ConstraintLayout` with `match_parent`/`match_parent` and `android:background="@android:color/black"` (UI-SPEC Color table: dominant `#000000` already satisfied)
- `android:id="@+id/messageText"` and `android:id="@+id/retryButton"` — exact IDs, referenced by `findViewById` in `MainActivity.kt` lines 53-54; renaming breaks the activity without a corresponding (out-of-scope) Kotlin change
- `retryButton`'s existing constraint chain (`layout_constraintTop_toBottomOf="@id/messageText"`, `layout_constraintBottom_toBottomOf="parent"`, `layout_constraintStart/EndOf="parent"`, `layout_constraintVertical_chainStyle="packed"` set on `messageText`, `layout_marginTop="16dp"`) — CONTEXT.md D-04: "mantiene lo stesso posizionamento attuale ... semplicemente ricentrato"
- `retryButton`'s `wrap_content`/`wrap_content` sizing — UI-SPEC only mandates the min-48dp touch target and does not put this view in the autosize scope; do not add `autoSizeTextType` here
- `tools:text` / `tools:visibility` preview attributes — designtime-only, keep for layout-editor usability

**What must change on `messageText` (per UI-SPEC.md Layout Contract + Typography table):**
1. `android:layout_width="wrap_content"` → `android:layout_width="0dp"` (match-constraint), keep `layout_constraintStart_toStartOf="parent"` + `layout_constraintEnd_toEndOf="parent"` (already present) and add horizontal margins: `android:layout_marginStart="16dp"` / `android:layout_marginEnd="16dp"` (UI-SPEC Spacing Scale, `md` token = 16dp, "Horizontal margin between messageText and screen edges")
2. `android:layout_height="wrap_content"` → `android:layout_height="0dp"` (match-constraint) — UI-SPEC explicitly flags this: "Current XML uses `wrap_content`, which must change — autosize cannot grow/shrink against a self-sizing box." Vertical constraints (`layout_constraintTop_toTopOf="parent"`, `layout_constraintBottom_toTopOf="@id/retryButton"`) are already present and sufficient to bound the 0dp height.
3. Remove fixed `android:textSize="20sp"` — replaced by autosize attributes below (autosize and a static `textSize` are mutually exclusive in effect; UI-SPEC's Typography table only lists the autosize triad, no fixed size)
4. Add `app:autoSizeTextType="uniform"` (namespaced `app:`, not `android:`, per AndroidX `TextViewCompat` convention — this project already declares `xmlns:app="http://schemas.android.com/apk/res-auto"` on the root, no new namespace needed)
5. Add `app:autoSizeMinTextSize="12sp"`, `app:autoSizeMaxTextSize="300sp"`, `app:autoSizeStepGranularity="4sp"` (exact values mandated by UI-SPEC Typography table, not left to discretion — CONTEXT.md D-01/discretion note is superseded by the more specific UI-SPEC values)
6. Add `android:gravity="center"` and `android:textAlignment="center"` (UI-SPEC: "number/message always centered") — neither is present in the current file; the current centering comes only from the ConstraintLayout chain, which centers the view's box but not necessarily multi-line text within it once `wrap_content` becomes `0dp`
7. Add `android:textFontWeight="900"` (minSdk 30 ≥ API 28 requirement, confirmed via `app/build.gradle.kts` line 15 `minSdk = 30` — safe to use unconditionally, no `sdk>=` `tools:targetApi` guard needed) alongside keeping `android:textStyle="bold"` as the UI-SPEC-mandated "belt-and-suspenders fallback"
8. Add `android:fontFamily="sans-serif"` (UI-SPEC: explicit system-default declaration)
9. Do NOT add `android:singleLine="true"` or any `maxLines` constraint — UI-SPEC: "Allow line wrap ... so the longest permission-denied string can wrap to 2 lines rather than shrink to unreadable size"
10. Do NOT use `app:autoSizePresetSizes` array form — UI-SPEC explicitly forbids it ("do not use the autoSizePresetSizes array form")

**Consumer contract — `MainActivity.kt` (read-only reference, not modified):**
```kotlin
// lines 53-54 — findViewById by the exact IDs that must survive layout changes
messageText = findViewById(R.id.messageText)
retryButton = findViewById(R.id.retryButton)
```
```kotlin
// lines 148-167 — all text is set via getString(), never hardcoded; layout changes
// must not require changes here since only presentation (size/position), not content, changes
private fun showReady() {
    retryButton.visibility = View.GONE
    messageText.text = getString(R.string.status_ready)
}

private fun showDenied() {
    retryButton.visibility = View.VISIBLE
    val permanentlyDenied =
        !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
    messageText.text = if (permanentlyDenied) {
        getString(R.string.permission_denied_permanent)
    } else {
        getString(R.string.permission_denied)
    }
    retryButton.text = if (permanentlyDenied) {
        getString(R.string.open_settings)
    } else {
        getString(R.string.retry)
    }
}
```
This confirms: no Kotlin changes are required by the layout edits above — `messageText.text` / `retryButton.text` / `.visibility` assignments are agnostic to autosize/width/height/gravity attributes.

**Error handling pattern:** Not applicable — this is a static XML layout resource, no runtime error handling occurs in the file itself. Error/denial *states* are represented as text content set by `MainActivity.kt` (`showDenied()`), already unaffected by this phase's changes (see D-03/D-04).

**Validation pattern:** Not applicable — no form inputs in this screen.

**Testing pattern:** No existing test file targets `activity_main.xml` (`Glob` found no instrumented UI test under `app/src/androidTest/` referencing `activity_main`). No test scaffolding to copy from; this phase is not expected to add tests (visual-only XML change).

---

## Shared Patterns

### Resource-only text (no hardcoded strings)
**Source:** `app/src/main/res/values/strings.xml` (all copy already present, confirmed by UI-SPEC.md Copywriting Contract — `status_ready`, `searching_gps_signal`, `speed_kmh_format`, `permission_denied`, `permission_denied_permanent`, `retry`, `open_settings`)
**Apply to:** No new strings needed this phase; the layout XML itself never contains literal user-facing text (only `tools:text` designtime previews, which are stripped at build time).

### ConstraintLayout percentage/chain centering
**Source:** `app/src/main/res/layout/activity_main.xml` lines 17-21 (existing `layout_constraintVertical_chainStyle="packed"` + top/bottom/start/end constraints)
**Apply to:** `messageText` — keep the existing chain; only change `wrap_content` → `0dp` on width/height per UI-SPEC's flagged correction, do not restructure the chain itself.

### Two-color flat palette
**Source:** `app/src/main/res/layout/activity_main.xml` line 8 (`android:background="@android:color/black"`) and line 14 (`android:textColor="@android:color/white"`)
**Apply to:** `messageText` retains `@android:color/white`; no new colors introduced (UI-SPEC Color table: "never introduce a third color in this phase"). `colors.xml` (`purple_500`, `teal_200`, etc., leftover from the AS template) is unused by this screen and out of scope.

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| — | — | — | None. The single in-scope file (`activity_main.xml`) has itself as the analog; `MainActivity.kt` and `themes.xml` are not modified this phase so they need no analog. |

## Metadata

**Analog search scope:** `app/src/main/res/layout/` (Glob, 1 file found total), `app/src/main/res/values/` (themes.xml, colors.xml, strings.xml read/confirmed), `app/src/main/java/com/sed/tachimetro/MainActivity.kt` (read in full as consumer contract), `app/build.gradle.kts` (read to confirm `minSdk = 30` for `textFontWeight` API-28 safety)
**Files scanned:** 5 (`activity_main.xml`, `themes.xml`, `MainActivity.kt`, `build.gradle.kts`, plus `Glob` confirming no sibling layout files or UI test files exist)
**Pattern extraction date:** 2026-07-07
