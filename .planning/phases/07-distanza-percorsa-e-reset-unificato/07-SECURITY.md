---
phase: 07
slug: distanza-percorsa-e-reset-unificato
status: verified
threats_open: 0
asvs_level: none specified
created: 2026-08-30
---

# Phase 07 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

**Audit type:** Retroactive, run from `PLAN.md` `<threat_model>` blocks (all 4 plans authored formal threat models at plan time; no prior `07-SECURITY.md` existed). Scope: verify each declared mitigation/accepted-risk actually exists in the implemented, merged code. Implementation files were not modified by this audit.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| disco (SharedPreferences) → app | `distance_meters` letto da `tachimetro_prefs`, alterabile su dispositivo rooted o da un ripristino di backup malformato | Float scalare (metri) |
| app → disco | `DistanceStore.write()` scrive nella sandbox privata dell'app (`MODE_PRIVATE`) | Float scalare (metri) |
| Google Play Services (processo di sistema) → app | Oggetti `Location` attraversano il confine di processo tramite `LocationCallback` | Coordinate GPS, velocità, accuratezza |
| `GpsSpeedProvider` → resto dell'app | Confine architetturale: solo primitivi derivati (`kmh: Int`, `deltaMeters: Float`) escono dalla classe, mai `Location` | Int + Float derivati |
| disco (SharedPreferences) → UI | `distanceStore.read()` in `onCreate()` mostrato direttamente all'utente | Float scalare (metri) |
| utente → app | Tocco su `resetMaxButton`/"Azzera": unico input che modifica in modo distruttivo lo stato persistito | Nessun dato, solo trigger |
| sistema Android → app | Insets di system bars/display cutout pilotano i margini delle nuove view | Layout insets |
| PC di sviluppo → dispositivo | `adb install` trasferisce un APK debug sul dispositivo dell'utente (solo Piano 07-04, verifica manuale) | APK debug |

---

## Threat Register

| Threat ID | Plan | Category | Disposition | Mitigation / Evidence | Status |
|-----------|------|----------|-------------|------------------------|--------|
| T-07-01-T | 07-01 | Tampering | mitigate | `DistanceStore.kt:15` `read()` → `sanitizePersistedDistance()`; `DistanceReducer.kt:26` clamps negative raw values to `0f` | closed |
| T-07-01-T2 | 07-01 | Tampering | mitigate | `DistanceReducer.kt:17,21` — `currentTotalMeters` and `deltaMeters` each defensively clamped to `0f` before summing | closed |
| T-07-01-I | 07-01 | Information disclosure | accept | `DistanceStore.kt:12` `MODE_PRIVATE`; only a `Float` in metri stored, no coordinates/timestamps; repo-wide grep for `Log.d/i/w/e/v(` returns zero matches | closed |
| T-07-01-D | 07-01 | Denial of service | accept | `DistanceStore.kt:19` uses `.apply()` (async, never `.commit()`); `LocationRequest` interval `1000L` caps write cadence at 1/sec | closed |
| T-07-01-E | 07-01 | Elevation of privilege | mitigate | `AndroidManifest.xml` declares only pre-existing `ACCESS_FINE_LOCATION`; no permission added | closed |
| T-07-01-SC | 07-01 | Tampering (supply chain) | accept | `git log -- gradle/libs.versions.toml app/build.gradle.kts` — none of the phase-07 commits touch either file | closed |
| T-07-02-T | 07-02 | Tampering | accept | Mock-location injection: inherent limitation of a personal, non-certified tool; no code mitigation intended | closed |
| T-07-02-I | 07-02 | Information disclosure | mitigate | `GpsSpeedProvider.kt:76-77` `lastAcceptedLocation` is a private field; `android.location.Location` appears in exactly one file repo-wide (`gps/GpsSpeedProvider.kt`); `SpeedState.Reading` carries only `kmh`/`deltaMeters` | closed |
| T-07-02-D | 07-02 | Denial of service | mitigate | Exactly one `scope`, one `ticker`, one `combine`/`stateIn` chain in `GpsSpeedProvider.kt` — no new flow/scope added for distance | closed |
| T-07-02-E | 07-02 | Elevation of privilege | mitigate | `AndroidManifest.xml` unchanged; `Location.distanceTo()` is a platform-1 API, no additional permission required | closed |
| T-07-02-SC | 07-02 | Tampering (supply chain) | accept | Same gradle-file git-log check as T-07-01-SC — phase-07 commits absent from that history | closed |
| T-07-03-T | 07-03 | Tampering | mitigate | `grep getSharedPreferences MainActivity.kt` → no matches; distance reads route exclusively through `distanceStore.read()` | closed |
| T-07-03-R | 07-03 | Repudiation | accept | `onResetClicked()` has no confirmation dialog, matches the pre-existing Phase 4 max-speed reset UX; no `AlertDialog` in the file | closed |
| T-07-03-I | 07-03 | Information disclosure | accept | `updateDistanceArea()` renders only a formatted scalar; no coordinates/route/history ever displayed or stored | closed |
| T-07-03-D | 07-03 | Denial of service | mitigate | `distanceStore.write()` gated by `newDistance != currentDistanceMeters`; D-04 noise floor means stationary vehicle never triggers a write; `.apply()` async | closed |
| T-07-03-D2 | 07-03 | Denial of service | mitigate | `setupGpsCollection()` wraps the sole collector in `repeatOnLifecycle(STARTED)`; no `WorkManager`/foreground service/wake lock added | closed |
| T-07-03-E | 07-03 | Elevation of privilege | mitigate | `AndroidManifest.xml` unchanged; reuses existing `ACCESS_FINE_LOCATION` grant | closed |
| T-07-03-SC | 07-03 | Tampering (supply chain) | accept | Same gradle-file git-log check — phase-07 commits absent from that file's history | closed |
| T-07-04-T | 07-04 | Tampering | accept | APK compiled locally from this repo's source with local debug key; no third-party artifact referenced anywhere in the repo | closed |
| T-07-04-E | 07-04 | Elevation of privilege | accept | `isMinifyEnabled = false` is on the `release` block; debug build used only for manual verification, never the shipped artifact | closed |
| T-07-04-I | 07-04 | Information disclosure | mitigate | 07-04 `<how-to-verify>` checklist asks only for on-screen distance values, never coordinates/positions; no location logging exists in the app | closed |
| T-07-04-SAFETY | 07-04 | Safety | mitigate | 07-04 Task 2 explicitly instructs: phone stays mounted, values read while stationary or by a passenger | closed |
| T-07-04-SC | 07-04 | Tampering (supply chain) | accept | `files_modified: []` confirmed by SUMMARY.md ("Files Created/Modified: Nessuno"); no gradle/build file changes in this plan | closed |

*Status: open · closed*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|--------------|------|
| T-07-01-I | T-07-01-I | Distanza percorsa è uno scalare singolo non sensibile (nessuna coordinata/timestamp), resta in sandbox `MODE_PRIVATE`, mai loggato né trasmesso — coerente col precedente non cifrato di `MaxSpeedStore` | gsd-security-auditor (disposizione a tempo di piano, verificata retroattivamente) | 2026-08-30 |
| T-07-01-D | T-07-01-D | `apply()` asincrona non blocca il main thread; cadenza limitata a 1 fix/sec da `LocationRequest`, nessuna amplificazione possibile lato app | gsd-security-auditor (disposizione a tempo di piano, verificata retroattivamente) | 2026-08-30 |
| T-07-01-SC | T-07-01-SC | Nessun pacchetto installato da questo piano; solo API di piattaforma e JUnit già presente | gsd-security-auditor (disposizione a tempo di piano, verificata retroattivamente) | 2026-08-30 |
| T-07-02-T | T-07-02-T | Location da mock provider: l'app è uno strumento personale locale, non un sistema di misura certificato; nessuna conseguenza di sicurezza oltre a un numero errato sullo schermo dell'utente stesso | gsd-security-auditor (disposizione a tempo di piano, verificata retroattivamente) | 2026-08-30 |
| T-07-02-SC | T-07-02-SC | `Location.distanceTo()` è API di piattaforma dal livello 1, nessun pacchetto installato | gsd-security-auditor (disposizione a tempo di piano, verificata retroattivamente) | 2026-08-30 |
| T-07-03-R | T-07-03-R | Reset irreversibile senza dialog di conferma è la UX già spedita in Fase 4 per il massimo; dato non critico, nessun requisito di audit trail | gsd-security-auditor (disposizione a tempo di piano, verificata retroattivamente) | 2026-08-30 |
| T-07-03-I | T-07-03-I | Distanza percorsa è esattamente l'informazione richiesta dall'utente; nessuna coordinata/percorso/cronologia mostrata o memorizzata (tracciamento percorso esplicitamente Out of Scope) | gsd-security-auditor (disposizione a tempo di piano, verificata retroattivamente) | 2026-08-30 |
| T-07-03-SC | T-07-03-SC | Nessun pacchetto installato: solo risorse XML e API di piattaforma già in uso | gsd-security-auditor (disposizione a tempo di piano, verificata retroattivamente) | 2026-08-30 |
| T-07-04-T | T-07-04-T | APK compilato localmente dal sorgente di questo repository con la debug key locale; nessun artefatto scaricato da terzi | gsd-security-auditor (disposizione a tempo di piano, verificata retroattivamente) | 2026-08-30 |
| T-07-04-E | T-07-04-E | Build debug con `isMinifyEnabled=false` installata solo per verifica manuale sul dispositivo di sviluppo; non è l'artefatto di rilascio | gsd-security-auditor (disposizione a tempo di piano, verificata retroattivamente) | 2026-08-30 |
| T-07-04-SC | T-07-04-SC | Nessun pacchetto installato in questo piano; nessuna modifica ai file di build | gsd-security-auditor (disposizione a tempo di piano, verificata retroattivamente) | 2026-08-30 |

*Accepted risks do not resurface in future audit runs.*

### Notes for future maintainers

- L'accettazione del rischio per T-07-01-I / T-07-02-T / T-07-03-I si basa tutta su "nessun dato di posizione esce da `GpsSpeedProvider`, niente viene loggato". Riverificato all'audit (non solo assunto dal piano) via grep repo-wide su `Log.*(` e su import di `android.location.Location` — entrambi confermati stretti come dichiarato. Se una fase futura aggiunge logging o esporta `Location`/coordinate fuori da `GpsSpeedProvider`, queste tre accettazioni vanno rivalutate, non ereditate silenziosamente.
- `gradle/libs.versions.toml` e `app/build.gradle.kts` risultavano modificati in `git status` al momento dell'audit — confermato via `git log` che si tratta di attività successive e non correlate alla Fase 07 (bump AGP, prep rilascio Play Store), non una regressione dei rischi accettati T-07-0X-SC.

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|----------------|--------|------|--------|
| 2026-08-30 | 22 | 22 | 0 | gsd-security-auditor (retroattivo, da `<threat_model>` di piano) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-08-30
