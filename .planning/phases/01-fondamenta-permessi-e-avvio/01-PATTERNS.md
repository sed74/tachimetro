# Phase 1: Fondamenta, Permessi e Avvio - Pattern Map

**Mapped:** 2026-07-07
**Files analyzed:** 5
**Analogs found:** 3 / 5 (self-modification baseline only; 2 files have no in-repo analog and use prescriptive defaults)

## Context

The codebase is a near-empty Android Studio scaffold (see `.planning/codebase/ARCHITECTURE.md`, `STRUCTURE.md`, `CONVENTIONS.md`, `STACK.md`). There is **no existing Activity, no layout directory, no Kotlin source, and no permission-handling code anywhere in the repository**. Three of the five files in scope are *modifications* to existing config files (their "analog" is their own current content — shown below verbatim so the planner can write precise diffs). The other two files (`MainActivity.kt`, layout XML) have **no analog** and must follow prescriptive Android/Kotlin defaults, constrained by the project's locked decisions (Kotlin, View-based XML layouts, no Compose — see `.planning/PROJECT.md`).

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|--------------------|------|-----------|-----------------|----------------|
| `app/build.gradle.kts` | config | build-config | itself (current content, shown below) | self-modify |
| `gradle/libs.versions.toml` | config | build-config | itself (current content, shown below) | self-modify |
| `app/src/main/AndroidManifest.xml` | config | build-config | itself (current content, shown below) | self-modify |
| `app/src/main/java/com/sed/tachimetro/MainActivity.kt` | controller (Activity) | event-driven (lifecycle + permission-result callback) | none in repo | no analog — prescriptive default |
| `app/src/main/res/layout/activity_main.xml` | component (layout resource) | transform (declarative view state) | none in repo | no analog — prescriptive default |

## Pattern Assignments

### `app/build.gradle.kts` (config, build-config)

**Analog:** itself — current full content (`app/build.gradle.kts:1-44`):
```kotlin
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.sed.tachimetro"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.sed.tachimetro"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
```

**Required edit — plugins block (line 1-3):** add the Kotlin Android plugin alias next to the existing AGP alias, following the same `alias(libs.plugins.<name>)` convention already used for `android.application`:
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}
```

**Note on `compileOptions`:** Kotlin projects conventionally add a `kotlinOptions { jvmTarget = "11" }` block (or `kotlin { compilerOptions { jvmTarget.set(...) } }` in newer AGP/Kotlin DSL) alongside `compileOptions` to keep Kotlin bytecode target aligned with the existing Java 11 `sourceCompatibility`/`targetCompatibility` (lines 32-35). No existing block to copy from — this is a prescriptive addition consistent with the current Java 11 pin.

**No new dependency required for permission handling:** `AppCompatActivity` (already available via `libs.appcompat` on line 39) extends `androidx.activity.ComponentActivity`, which provides `registerForActivityResult(ActivityResultContracts.RequestPermission())` out of the box. No `activity-ktx` or `core-ktx` dependency needs to be added for this phase's scope.

---

### `gradle/libs.versions.toml` (config, build-config)

**Analog:** itself — current full content (`gradle/libs.versions.toml:1-19`):
```toml
[versions]
agp = "9.1.1"
junit = "4.13.2"
junitVersion = "1.1.5"
espressoCore = "3.5.1"
appcompat = "1.6.1"
material = "1.10.0"

[libraries]
junit = { group = "junit", name = "junit", version.ref = "junit" }
ext-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
material = { group = "com.google.android.material", name = "material", version.ref = "material" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
```

**Required edit — add Kotlin version + plugin alias**, following the exact structure already used for `agp`/`android-application`:
```toml
[versions]
agp = "9.1.1"
kotlin = "2.0.21"
...

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```
Reference this alias in `app/build.gradle.kts` as `libs.plugins.kotlin.android` (matches the `libs.plugins.android.application` naming convention already in use). Pin the Kotlin version to the latest stable release compatible with AGP 9.1.1 at implementation time (verify against AGP/Kotlin compatibility table rather than hardcoding "2.0.21" blindly — confirm current stable version during planning).

---

### `app/src/main/AndroidManifest.xml` (config, build-config)

**Analog:** itself — current full content (`app/src/main/AndroidManifest.xml:1-15`):
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.Tachimetro" />

</manifest>
```

**No existing `<uses-permission>` or `<activity>` entries to copy** — this phase establishes the first component registration. Prescriptive standard Android pattern (insert permission before `<application>`, activity as first child inside `<application>`):
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.Tachimetro">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@style/Theme.Tachimetro">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>

</manifest>
```
`android:exported="true"` is mandatory for the launcher Activity on API 31+ (project's targetSdk is 36) — omitting it causes a manifest merger/install failure. `ACCESS_COARSE_LOCATION` is intentionally NOT added (PERM-01 restricts scope to `ACCESS_FINE_LOCATION` only).

---

### `app/src/main/java/com/sed/tachimetro/MainActivity.kt` (controller, event-driven)

**Analog:** none — `app/src/main/java/com/sed/tachimetro/` is currently empty (confirmed in `ARCHITECTURE.md`, `STRUCTURE.md`). This is the first class in the codebase and establishes the package's Activity conventions for all later phases.

**Prescriptive default** (standard modern Android permission-request pattern, using the non-deprecated `ActivityResultContracts` API available via `AppCompatActivity`/`ComponentActivity`, already provided transitively by `libs.appcompat`):

**Imports** (grouping convention per `CONVENTIONS.md`: `android.*` → `androidx.*` → blank line, no third-party needed here):
```kotlin
package com.sed.tachimetro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
```

**Core pattern — permission request + state handling** (prescriptive, no existing analog):
```kotlin
class MainActivity : AppCompatActivity() {

    private lateinit var messageText: TextView
    private lateinit name retryButton: Button

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                showReady()
            } else {
                showDenied()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        messageText = findViewById(R.id.messageText)
        retryButton = findViewById(R.id.retryButton)
        retryButton.setOnClickListener { onRetryClicked() }

        checkAndRequestPermission()
    }

    private fun checkAndRequestPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> showReady()

            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // Rationale message shown inline in the placeholder text before re-prompting.
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }

            else -> requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun onRetryClicked() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            openAppSettings()
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    private fun showReady() {
        retryButton.visibility = android.view.View.GONE
        messageText.text = getString(R.string.status_ready)
    }

    private fun showDenied() {
        retryButton.visibility = android.view.View.VISIBLE
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
}
```
(Note: fix the stray `lateinit name` typo shown above to `lateinit var` when implementing — kept here only to flag it explicitly, do not copy verbatim.)

**Error handling pattern:** No try/catch needed — `ActivityResultContracts.RequestPermission()` and `ContextCompat.checkSelfPermission` never throw for a declared, valid permission string; the manifest declaration from the previous section is the only precondition. This establishes the phase's error-handling posture: rely on Android's typed callback/contract APIs rather than exceptions for permission flows (documented for `CONVENTIONS.md` update after this phase lands).

**String resources needed** (add to `app/src/main/res/values/strings.xml`, which currently only has `app_name` at line 2):
```xml
<string name="status_ready">Pronto</string>
<string name="permission_denied">Permesso GPS necessario per funzionare</string>
<string name="permission_denied_permanent">Permesso GPS necessario per funzionare</string>
<string name="retry">Riprova</string>
<string name="open_settings">Apri impostazioni</string>
```
All Italian per UI-05. Exact rationale-message copy is left to planner/implementer discretion per CONTEXT.md ("messaggio semplice in italiano") — the above are reasonable defaults.

---

### `app/src/main/res/layout/activity_main.xml` (component, transform)

**Analog:** none — `app/src/main/res/layout/` does not exist yet (confirmed in `STRUCTURE.md`: "No `res/layout/` directory exists"). This phase creates the directory and its first file.

**Prescriptive default** — minimal black-background layout with a message `TextView` and a `Button` (hidden when permission is granted, per CONTEXT.md placeholder-screen decision: "Nessun elemento grafico oltre al testo" when ready). Uses plain `ConstraintLayout` (already implied by AppCompat/Material scaffold; no navigation/Compose per `PROJECT.md` decision):
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
**Dependency check:** `androidx.constraintlayout:constraintlayout` is NOT currently declared in `gradle/libs.versions.toml` or `app/build.gradle.kts` (only `appcompat` and `material` are present — see `STACK.md`). If ConstraintLayout is used, it must be added to the version catalog and `app/build.gradle.kts` dependencies block alongside `libs.appcompat`/`libs.material` (same `implementation(libs.<alias>)` pattern shown in `app/build.gradle.kts:39-40`). Alternatively, a plain `LinearLayout` (vertical, centered via `gravity="center"`) avoids the new dependency entirely and is sufficient for this phase's minimal placeholder screen — planner should pick one and note the choice; no project precedent favors either yet.

**Naming convention applied:** `activity_main.xml` follows the `activity_<name>.xml` convention explicitly prescribed in `STRUCTURE.md` ("Where to Add New Code" section) for the layout matching `MainActivity`.

---

## Shared Patterns

### Package/namespace convention
**Source:** `app/build.gradle.kts:6` (`namespace = "com.sed.tachimetro"`), confirmed by `CONVENTIONS.md`
**Apply to:** `MainActivity.kt` — must live directly under `app/src/main/java/com/sed/tachimetro/` (package `com.sed.tachimetro`), matching the manifest namespace. No sub-package (e.g., `.ui`) is needed yet since there is only one class.

### Version-catalog-first dependency/plugin management
**Source:** `gradle/libs.versions.toml` + `app/build.gradle.kts:1-2, 38-43` (every plugin/dependency is declared as a version + alias in the catalog, then referenced via `libs.*` in the module build file — never hardcoded inline)
**Apply to:** Both the Kotlin plugin addition and any layout-library dependency (e.g., ConstraintLayout) added in this phase — add to `[versions]`/`[plugins]`/`[libraries]` in `libs.versions.toml` first, then reference via `libs.plugins.kotlin.android` / `libs.constraintlayout` in `app/build.gradle.kts`.

### Italian-only user-facing strings
**Source:** CONTEXT.md decisions + REQUIREMENTS.md UI-05
**Apply to:** All strings added to `strings.xml` in this phase (`status_ready`, `permission_denied`, `permission_denied_permanent`, `retry`, `open_settings`) — no hardcoded strings in Kotlin code, always via `getString(R.string.*)`.

### Manifest component registration (first precedent in repo)
**Source:** prescriptive default shown above (no repo analog)
**Apply to:** `AndroidManifest.xml` — this is the pattern all future Activities/Services in later phases should follow (single `LAUNCHER` activity for this app; per APP-01/PROJECT.md this app has exactly one screen, so no further activities are expected).

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| `app/src/main/java/com/sed/tachimetro/MainActivity.kt` | controller | event-driven | `app/src/main/java/com/sed/tachimetro/` is empty — no Activity, Kotlin file, or permission-handling code exists anywhere in the repo (confirmed in `ARCHITECTURE.md` line 8, `STRUCTURE.md` line 53). Prescriptive default provided above using standard `ActivityResultContracts` API. |
| `app/src/main/res/layout/activity_main.xml` | component | transform | `app/src/main/res/layout/` directory does not exist yet (confirmed in `STRUCTURE.md` line 46). Prescriptive default provided above; planner must decide between ConstraintLayout (new dependency) or LinearLayout (no new dependency). |

## Metadata

**Analog search scope:** `app/src/main/java/com/sed/tachimetro/` (empty), `app/src/main/res/` (layout dir absent), full repo root config files (`build.gradle.kts`, `settings.gradle.kts`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `AndroidManifest.xml`), plus `.planning/codebase/*.md` for confirmed current-state facts.
**Files scanned:** 10 (all existing project files + all 4 codebase-mapping docs + REQUIREMENTS.md)
**Pattern extraction date:** 2026-07-07
