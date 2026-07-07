---
phase: 01-fondamenta-permessi-e-avvio
reviewed: 2026-07-07T00:00:00Z
depth: standard
files_reviewed: 6
files_reviewed_list:
  - app/build.gradle.kts
  - app/src/main/AndroidManifest.xml
  - app/src/main/java/com/sed/tachimetro/MainActivity.kt
  - app/src/main/res/layout/activity_main.xml
  - app/src/main/res/values/strings.xml
  - gradle/libs.versions.toml
findings:
  critical: 1
  warning: 3
  info: 2
  total: 6
status: issues_found
---

# Phase 01: Code Review Report

**Reviewed:** 2026-07-07T00:00:00Z
**Depth:** standard
**Files Reviewed:** 6
**Status:** issues_found

## Summary

Reviewed the foundational permission-request flow (`MainActivity.kt`), its supporting manifest/layout/string resources, and the build configuration (`app/build.gradle.kts`, `gradle/libs.versions.toml`). The permission-request scaffolding follows the modern `ActivityResultContracts.RequestPermission()` pattern and correctly distinguishes "can ask again" vs. "permanently denied" states when the denial callback fires. However, the app never re-checks permission state after the user returns from the system Settings screen it explicitly sends them to — the one path this phase exists to support (`Open Settings` → user grants location → back to app) leaves the UI stuck on the "denied" screen until the process is killed and restarted. This is a functional blocker for the phase's stated purpose ("fondamenta permessi e avvio"). Additional lower-severity issues: dead/redundant branching in the permission-check logic, two string resources that are supposed to represent different denial states but are byte-for-byte identical, and an undeclared direct dependency on the `androidx.activity` APIs the code calls directly.

## Critical Issues

### CR-01: Permission state is never re-checked after returning from Settings, leaving the app stuck on the denial screen

**File:** `app/src/main/java/com/sed/tachimetro/MainActivity.kt:31-69`
**Issue:** `checkAndRequestPermission()` is only invoked once, from `onCreate()` (line 39). When the permission is permanently denied, `onRetryClicked()` (lines 56-62) sends the user to the system app-settings screen via `openAppSettings()` (lines 64-69), which is the documented recovery path for a permanently-denied permission. When the user grants the permission there and presses back, Android resumes the existing `MainActivity` instance through `onStart()`/`onResume()` — `onCreate()` is **not** called again. Because `MainActivity` overrides neither `onResume()` nor `onStart()`, `checkAndRequestPermission()` never re-runs, `showReady()` is never called, and the UI keeps displaying the "permission denied" message with the "Open settings" button even though the permission is now granted. The user has no way to reach the ready state short of force-killing and relaunching the app.
**Fix:**
```kotlin
override fun onResume() {
    super.onResume()
    // Re-check permission state whenever the activity comes back to the
    // foreground (e.g. returning from the system Settings screen opened
    // by openAppSettings()).
    if (ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        showReady()
    } else if (retryButton.visibility == View.VISIBLE) {
        // Refresh the denial message/button label in case the
        // "can ask again" state changed while we were away.
        showDenied()
    }
}
```

## Warnings

### WR-01: Redundant branches in `checkAndRequestPermission()` obscure the actual logic

**File:** `app/src/main/java/com/sed/tachimetro/MainActivity.kt:42-54`
**Issue:** The `shouldShowRequestPermissionRationale(...)` branch (lines 48-50) and the `else` branch (line 52) both do exactly the same thing — call `requestPermissionLauncher.launch(...)`. The rationale check has no effect on control flow here (it would only matter if the app wanted to show an explanatory UI before requesting), so the `when` reads as if two different behaviors are being handled when there is really only one. This is misleading for future maintainers who might assume the rationale branch already contains special handling.
**Fix:**
```kotlin
private fun checkAndRequestPermission() {
    val granted = ContextCompat.checkSelfPermission(
        this, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    if (granted) {
        showReady()
    } else {
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}
```

### WR-02: `permission_denied` and `permission_denied_permanent` strings are identical, defeating the purpose of having two states

**File:** `app/src/main/res/values/strings.xml:4-5`
**Issue:** `showDenied()` in `MainActivity.kt` (lines 76-90) explicitly branches on `permanentlyDenied` to select between `R.string.permission_denied` and `R.string.permission_denied_permanent`, and also swaps the button label between "Riprova" and "Apri impostazioni". But both message strings resolve to the exact same text: `"Permesso GPS necessario per funzionare"`. The permanently-denied case gives the user a button that says "Apri impostazioni" with no message explaining *why* they need to go there, which undermines the app's stated goal of instant, unambiguous readability.
**Fix:**
```xml
<string name="permission_denied">Permesso GPS necessario per funzionare</string>
<string name="permission_denied_permanent">Permesso GPS negato. Aprire le impostazioni per abilitarlo</string>
```

### WR-03: Code directly uses `androidx.activity` APIs without declaring a direct dependency on them

**File:** `app/build.gradle.kts:44-51`, `gradle/libs.versions.toml:1-24`
**Issue:** `MainActivity.kt` imports and calls `androidx.activity.result.contract.ActivityResultContracts` and `registerForActivityResult(...)` (from `androidx.activity.ComponentActivity`), but neither `app/build.gradle.kts` nor `gradle/libs.versions.toml` declares a dependency on `androidx.activity:activity` / `activity-ktx`. This currently works only because `androidx.appcompat:appcompat` transitively pulls in `androidx.activity` via `androidx.fragment`. Relying on a transitive dependency for an API surface the code calls directly is fragile — a future AppCompat/Fragment version bump could resolve a different (or absent) transitive `activity` version and silently break compilation or introduce API mismatches, with no direct signal in the version catalog of what version is actually required.
**Fix:**
```toml
# gradle/libs.versions.toml
[versions]
activity = "1.9.3"
...
[libraries]
activity = { group = "androidx.activity", name = "activity-ktx", version.ref = "activity" }
```
```kotlin
// app/build.gradle.kts
dependencies {
    implementation(libs.appcompat)
    implementation(libs.activity)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    ...
}
```

## Info

### IN-01: `Manifest.permission.ACCESS_FINE_LOCATION` string literal repeated 7 times

**File:** `app/src/main/java/com/sed/tachimetro/MainActivity.kt:45,48,49,52,57,58,79`
**Issue:** The same permission constant is referenced seven separate times across the class. It's a compile-time constant so there's no runtime risk, but the duplication makes it easy for a future edit (e.g. adding `ACCESS_COARSE_LOCATION` as a fallback) to miss a call site.
**Fix:**
```kotlin
companion object {
    private const val LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION
}
```
Then reference `LOCATION_PERMISSION` at each call site instead of the fully-qualified constant.

### IN-02: No `<uses-feature>` declaration for GPS hardware

**File:** `app/src/main/AndroidManifest.xml:5`
**Issue:** The manifest declares `ACCESS_FINE_LOCATION` but does not declare `<uses-feature android:name="android.hardware.location.gps" android:required="true" />`. Since this app's entire purpose is reading precise GPS speed, declaring the hardware feature requirement documents intent and (for store distribution) prevents installation on devices without GPS hardware. This phase doesn't yet read location data, so it's not blocking, but it should be added before/alongside the phase that implements `FusedLocationProviderClient` usage.
**Fix:**
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-feature android:name="android.hardware.location.gps" android:required="true" />
```

---

_Reviewed: 2026-07-07T00:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
