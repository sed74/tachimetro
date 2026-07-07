# External Integrations

**Analysis Date:** 2026-07-07

## APIs & External Services

**None detected.** No SDK/client dependencies for any third-party service are declared in `gradle/libs.versions.toml` or `app/build.gradle.kts`, and no application source code exists yet (`app/src/main/` contains only `AndroidManifest.xml` and Android resources — no Kotlin/Java classes). This appears to be a freshly scaffolded Android Studio project ("Tachimetro", package `com.sed.tachimetro`) with no implemented features.

## Data Storage

**Databases:**
- None. No Room, SQLite, or other persistence library present.

**File Storage:**
- Not implemented. `app/src/main/res/xml/backup_rules.xml` and `app/src/main/res/xml/data_extraction_rules.xml` exist but contain only default Android Studio template content (auto-backup/data-extraction policy scaffolding, not custom rules).

**Caching:**
- None.

## Authentication & Identity

**Auth Provider:**
- None. No auth-related dependencies, no login/session code.

## Monitoring & Observability

**Error Tracking:**
- None (no Crashlytics, Sentry, or similar SDK).

**Logs:**
- None implemented (no `Log.*` usage found; no source files to contain it).

## CI/CD & Deployment

**Hosting:**
- Not applicable (native Android app, no server-side component).

**CI Pipeline:**
- None detected. No `.github/workflows/`, no `.gitlab-ci.yml`, no other CI config files found in the repository.

## Environment Configuration

**Required env vars:**
- None detected. No `.env` files present.

**Secrets location:**
- None found. `local.properties` exists (standard Android Studio local SDK path file) but its contents were not read per security policy; no other credential/secret files (`*.pem`, `*.key`, `serviceAccountKey.json`, etc.) are present in the repository tree.

## Webhooks & Callbacks

**Incoming:**
- None (native mobile app; no server component).

**Outgoing:**
- None. `AndroidManifest.xml` declares no `<uses-permission>` entries (e.g., no `INTERNET` permission), confirming no network calls are currently possible from this app.

---

*Integration audit: 2026-07-07*
