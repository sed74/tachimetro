# Codebase Structure

**Analysis Date:** 2026-07-07

## Directory Layout

```
Tachimetro/                                    # Repo root — no VCS metadata detected other than .git
├── .claude/                                   # Claude Code local settings
│   └── settings.local.json
├── .gradle/                                   # Gradle cache (generated, not committed)
├── .idea/                                     # Android Studio project settings (generated)
├── .planning/                                 # GSD planning artifacts (this mapping output lives here)
│   └── codebase/
├── app/                                       # The single Gradle module ":app"
│   ├── build.gradle.kts                       # Module-level build config (SDK versions, deps)
│   ├── proguard-rules.pro                     # (referenced by build.gradle.kts release block)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml            # App-level manifest — no components declared
│       │   ├── java/com/sed/tachimetro/       # EMPTY — production code goes here
│       │   └── res/
│       │       ├── drawable/                  # ic_launcher_background.xml, ic_launcher_foreground.xml
│       │       ├── mipmap-anydpi/              # Adaptive launcher icon XML
│       │       ├── mipmap-{h,m,x,xx,xxx}hdpi/  # Launcher icon PNG/WebP per density
│       │       ├── values/                    # colors.xml, strings.xml, themes.xml
│       │       ├── values-night/               # themes.xml (dark theme override)
│       │       └── xml/                        # backup_rules.xml, data_extraction_rules.xml
│       ├── test/
│       │   └── java/com/sed/tachimetro/
│       │       └── ExampleUnitTest.java        # IDE-generated placeholder JVM test
│       └── androidTest/
│           └── java/com/sed/tachimetro/
│               └── ExampleInstrumentedTest.java # IDE-generated placeholder instrumented test
├── gradle/
│   ├── libs.versions.toml                     # Gradle version catalog (deps + plugin versions)
│   └── wrapper/                                # Gradle wrapper jar/properties
├── build.gradle.kts                            # Root build script (applies AGP plugin alias only)
├── settings.gradle.kts                         # Declares rootProject name "Tachimetro", includes ":app"
├── gradle.properties                           # Gradle/AGP global properties (JVM args, AndroidX flags)
├── gradlew / gradlew.bat                       # Gradle wrapper scripts
├── local.properties                            # Local SDK path (machine-specific, gitignored)
└── .gitignore
```

**No `res/layout/` directory exists** — confirms no Activity/Fragment layouts have been created yet.
**No `AndroidManifest.xml` component entries** — confirms no Activity classes exist yet.

## Directory Purposes

**`app/src/main/java/com/sed/tachimetro/`:**
- Purpose: Root Java package for all production application code
- Contains: Nothing currently (zero files) — this is the single source root for the entire app
- Key files: None yet

**`app/src/main/res/`:**
- Purpose: Android resources — layouts, drawables, values, XML configs
- Contains: Only scaffold resources (launcher icon in all densities, base + night theme, color palette with 2 colors, one string `app_name`)
- Key files: `values/themes.xml` (defines `Theme.Tachimetro`, parent `Theme.MaterialComponents.DayNight.DarkActionBar`), `values/colors.xml`, `values/strings.xml`

**`app/src/test/java/com/sed/tachimetro/`:**
- Purpose: JVM-only unit tests (run via `./gradlew test`, no Android framework/emulator needed)
- Contains: `ExampleUnitTest.java` (trivial `2+2` assertion, placeholder only)
- Key files: `ExampleUnitTest.java`

**`app/src/androidTest/java/com/sed/tachimetro/`:**
- Purpose: Instrumented tests requiring a device/emulator (run via `./gradlew connectedAndroidTest`)
- Contains: `ExampleInstrumentedTest.java` (verifies app package name via `InstrumentationRegistry`)
- Key files: `ExampleInstrumentedTest.java`

**`gradle/`:**
- Purpose: Gradle wrapper binaries and the dependency version catalog
- Contains: `libs.versions.toml` (single source of truth for dependency/plugin versions), `wrapper/gradle-wrapper.properties` + jar

**`.planning/`:**
- Purpose: GSD (this tool's) planning and codebase-mapping output; not part of the Android build
- Contains: `codebase/` subdirectory holding generated architecture/structure docs

## Key File Locations

**Entry Points:**
- None exist. When created, the launcher Activity should live at `app/src/main/java/com/sed/tachimetro/MainActivity.java` (or `.kt` if Kotlin is added) and must be registered in `app/src/main/AndroidManifest.xml` with a `LAUNCHER`/`MAIN` intent filter.

**Configuration:**
- `app/build.gradle.kts`: module SDK versions, Java compatibility, dependency declarations
- `build.gradle.kts` (root): applies the Android application plugin alias from the version catalog
- `settings.gradle.kts`: module inclusion (`:app`), repository resolution management
- `gradle/libs.versions.toml`: all dependency/plugin version numbers — add new library versions here first, then reference via `libs.xxx` alias in `app/build.gradle.kts`
- `gradle.properties`: JVM args for Gradle daemon, AndroidX enablement flags
- `local.properties`: machine-local Android SDK path (never commit real values; already gitignored)

**Core Logic:**
- None yet — `app/src/main/java/com/sed/tachimetro/` is empty.

**Testing:**
- `app/src/test/java/com/sed/tachimetro/`: unit tests (JVM, fast, no Android APIs unless mocked)
- `app/src/androidTest/java/com/sed/tachimetro/`: instrumented tests (real/emulated device, can call Android framework APIs)

## Naming Conventions

**Files:**
- Java class files: `PascalCase.java` matching the public class name (e.g., `ExampleUnitTest.java`, `ExampleInstrumentedTest.java`)
- Resource XML files: `snake_case.xml` (e.g., `ic_launcher_background.xml`, `backup_rules.xml`, `data_extraction_rules.xml`)
- Drawable/mipmap assets: `ic_<purpose>.xml` / `ic_launcher.webp` prefix convention already established for icons

**Directories:**
- Java package directories mirror the reverse-domain package name: `com/sed/tachimetro/` under each source set (`main`, `test`, `androidTest`)
- Resource qualifiers follow Android convention: `values-night/` for dark theme, `mipmap-{density}dpi/` for icon densities

**Package:**
- Application ID / base package: `com.sed.tachimetro` (set in `app/build.gradle.kts` via `namespace` and `applicationId`, and mirrored by the `package` directory structure). Any new class must be created under this package (or a sub-package of it) to match the manifest namespace.

## Where to Add New Code

**New Activity/Fragment/Feature:**
- Implementation: `app/src/main/java/com/sed/tachimetro/` (create sub-packages here as the app grows, e.g. `com.sed.tachimetro.ui`, `com.sed.tachimetro.data` — none exist yet, so the first feature establishes the convention)
- Layout XML: create `app/src/main/res/layout/` (does not exist yet — must be created) with files named `activity_<name>.xml` or `fragment_<name>.xml` per Android convention
- Manifest registration: add `<activity>` (or other component) entries inside `app/src/main/AndroidManifest.xml`
- Tests: unit test in `app/src/test/java/com/sed/tachimetro/`, instrumented/UI test in `app/src/androidTest/java/com/sed/tachimetro/`

**New dependency:**
- Add version to `gradle/libs.versions.toml` under `[versions]` and `[libraries]` (or `[plugins]`)
- Reference it in `app/build.gradle.kts` via `implementation(libs.<alias>)`

**New string/color/dimension resource:**
- Strings: `app/src/main/res/values/strings.xml`
- Colors: `app/src/main/res/values/colors.xml`
- Theme overrides: `app/src/main/res/values/themes.xml` (light) and `app/src/main/res/values-night/themes.xml` (dark)

**Utilities:**
- No shared-utility package exists yet; when needed, create `com.sed.tachimetro.util` (or similar) under the main source root.

## Special Directories

**`.gradle/`:**
- Purpose: Gradle build cache and daemon state
- Generated: Yes
- Committed: No (gitignored)

**`.idea/`:**
- Purpose: Android Studio IDE project/workspace settings
- Generated: Yes
- Committed: Not recommended (check `.gitignore`)

**`app/build/` (not present until first build):**
- Purpose: Compiled classes, APK/AAB outputs, generated R/BuildConfig classes
- Generated: Yes
- Committed: No

**`.planning/`:**
- Purpose: GSD tool-generated planning documents (this file included)
- Generated: Yes (by mapping/planning commands)
- Committed: Project-dependent — not part of the Android build regardless

---

*Structure analysis: 2026-07-07*
