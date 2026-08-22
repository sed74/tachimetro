# External Integrations

**Analysis Date:** 2026-08-22

## APIs & External Services

**Location Services:**
- Google Play Services Location - Provides real-time GPS speed data
  - SDK/Client: `com.google.android.gms:play-services-location:21.4.0`
  - Implementation: FusedLocationProviderClient (high-level location provider combining GPS, WiFi, cellular)
  - Location class: `app/src/main/java/com/sed/tachimetro/gps/GpsSpeedProvider.kt`

## Data Storage

**Databases:**
- None declared — No persistent relational database (no Room, SQLite, or third-party ORM)
- State management: In-memory Kotlin Flow/State objects in `app/src/main/java/com/sed/tachimetro/`

**File Storage:**
- Local filesystem only (SharedPreferences for app preferences if any)
- No cloud storage integration

**Caching:**
- In-memory caching via Android process memory
- No Redis, Memcached, or external caching service

## Authentication & Identity

**Auth Provider:**
- Android runtime permissions system
  - Permission: `android.permission.ACCESS_FINE_LOCATION` (user-grant at runtime, Android 6+)
  - Manifested in: `app/src/main/AndroidManifest.xml:10`
  - No OAuth, API keys, or third-party identity provider

**User Sessions:**
- Not applicable — No user accounts or authentication framework

## Monitoring & Observability

**Error Tracking:**
- None detected (no Sentry, Firebase Crashlytics, or similar)

**Logs:**
- Android platform logs (logcat) only
- No centralized log aggregation or monitoring service

**Analytics:**
- None detected (no Firebase Analytics, Mixpanel, or similar event tracking)

## CI/CD & Deployment

**Hosting:**
- Android devices/emulators only
- No server backend or cloud hosting (APK is self-contained)
- No Play Store integration configured

**CI Pipeline:**
- None detected (no GitHub Actions, Jenkins, or similar CI configuration in repo)
- Build performed locally via `gradlew assembleDebug` / `gradlew assembleRelease`

**Testing Infrastructure:**
- Local JUnit execution: `./gradlew test`
- On-device instrumented testing: `./gradlew connectedAndroidTest` (requires connected device/emulator)
- No remote testing service configured

## Environment Configuration

**Required env vars:**
- None — No external API credentials or service tokens required in current implementation

**Secrets location:**
- `local.properties` - Contains Android SDK path only (no secrets); not version-controlled
- No `.env` file
- No credential vault or secret management integration

**Build Configuration:**
- All build parameters centralized in `gradle/libs.versions.toml` (version catalog)
- No environment-specific build profiles beyond default debug/release

## Webhooks & Callbacks

**Incoming:**
- None (no server endpoints or webhook receivers)

**Outgoing:**
- None (no HTTP requests to external services initiated by the app)

## Runtime Permissions

**Declared in Manifest:**
- `android.permission.ACCESS_FINE_LOCATION` - Required for GPS speed data
  - Granted via user dialog at app launch (Android 6+)
  - Linter annotation override: `tools:ignore="CoarseFineLocation"` (project constraint: fine-grained location only)

**Implicit Permissions (via Play Services):**
- Network access (implied by Google Play Services for location provider backend)
- No explicit `INTERNET` permission declared (Play Services handles this)

## Third-Party Library Dependencies

**Google Play Services:**
- `com.google.android.gms:play-services-location:21.4.0` - Only GMS dependency
- Requires Google Play Services APK on device (not available on pure AOSP devices without GMS)

**AndroidX Ecosystem:**
- AppCompat, Material, Activity, Lifecycle, ConstraintLayout (UI/lifecycle support)
- No database, network, or reactive libraries (Retrofit, Room, RxJava absent)

**Kotlin Ecosystem:**
- Coroutines Core 1.10.2 - Async location updates

---

*Integration audit: 2026-08-22*
