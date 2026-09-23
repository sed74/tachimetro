---
phase: 08
slug: fondamenta-condivise-e-velocit-sullo-schermo-auto
status: complete
nyquist_compliant: true
wave_0_complete: true
created: 2026-09-03
reconstructed: true
---

# Phase 08 — Validation Strategy

> Ricostruito retroattivamente (State B: nessun `08-VALIDATION.md` esisteva, ricostruito da PLAN/SUMMARY dei 3 piani) tramite `/gsd:validate-phase 8`.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4.13.2 (JVM, `app/src/test/`) + AndroidX Test Ext JUnit 1.1.5 / Espresso 3.5.1 (strumentato, `app/src/androidTest/`) |
| **Config file** | `app/build.gradle.kts` (testRunner `androidx.test.runner.AndroidJUnitRunner`) |
| **Quick run command** | `./gradlew.bat :app:testDebugUnitTest` |
| **Full suite command** | `./gradlew.bat :app:testDebugUnitTest && ./gradlew.bat :app:connectedDebugAndroidTest` (il secondo richiede device/emulatore connesso) |
| **Estimated runtime** | ~30s (JVM) + ~1-2min (strumentato, con device) |

---

## Sampling Rate

- **After every task commit:** `./gradlew.bat :app:testDebugUnitTest`
- **After every plan wave:** full suite (JVM + strumentato se device disponibile) + `./gradlew.bat :app:assembleDebug`
- **Before `/gsd:verify-work`:** full suite verde
- **Max feedback latency:** ~2min

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 08-01-T1 | 01 | 1 | AA-01/AA-02 (infra) | build+grep | `./gradlew.bat :app:assembleDebug` + grep versioni/stringhe | ✅ | ✅ green |
| 08-01-T2 | 01 | 1 | AA-01/AA-02/AA-03 (infra) | build+grep | `./gradlew.bat :app:assembleDebug` + grep scope Application | ✅ | ✅ green |
| 08-01-T3 | 01 | 1 | AA-01, AA-02 | unit (TDD) | `./gradlew.bat :app:testDebugUnitTest --tests 'com.sed.tachimetro.car.CarSpeedContentTest'` | ✅ `CarSpeedContentTest.kt` | ✅ green (5/5 test) |
| 08-02-T1 | 02 | 2 | AA-01 (discoverability) | build+grep | `./gradlew.bat :app:assembleDebug` + grep manifest/POI | ✅ | ✅ green |
| 08-02-T2 | 02 | 2 | AA-01 | build+grep | `./gradlew.bat :app:assembleDebug` + grep CarAppService/Session | ✅ | ✅ green |
| 08-02-T3 | 02 | 2 | AA-01, AA-02, AA-03 | build+grep | `./gradlew.bat :app:assembleDebug` + grep SpeedScreen (carSpeedContent, repeatOnLifecycle, invalidate, no delay/throttle) | ✅ | ✅ green |
| 08-03-T1 | 03 | 3 | AA-01, AA-02, AA-03 | instrumented | `./gradlew.bat :app:connectedDebugAndroidTest` (compile riverificato in questo audit: `compileDebugAndroidTestKotlin` OK) | ✅ `SpeedScreenTemplateTest.kt` | ✅ green (eseguito con successo durante sessione DHU, 08-03-SUMMARY.md) |
| 08-03-T2 | 03 | 3 | SC4 (tooling) | script parse | PowerShell AST parse di `scripts/dhu-quota-check.ps1` + grep struttura | ✅ `dhu-quota-check.ps1` + `docs/dhu-quota-verification.md` | ✅ green |
| 08-03-T3 | 03 | 3 | SC1-SC5 (gate umano) | manual | checkpoint bloccante, vedi Manual-Only sotto | n/a | ✅ approvato 2026-09-02 |

*Verifica di questo audit (2026-09-03): `./gradlew.bat :app:testDebugUnitTest` → 75 test totali nel modulo, 0 failures, 0 errors. `./gradlew.bat :app:compileDebugAndroidTestKotlin` → BUILD SUCCESSFUL, nessuna regressione di compilazione su `SpeedScreenTemplateTest.kt`.*

---

## Wave 0 Requirements

Nessuna — infrastruttura di test (JUnit JVM + AndroidX Test strumentato) già presente dalla Fase 1/2 e riusata senza modifiche per la Fase 8.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|--------------------|
| L'host Android Auto non chiude l'app durante un refresh continuo a 1Hz per 5-10 minuti | SC4 (Phase 8, non un requirement ID dedicato) | Dipende dal comportamento reale del binding IPC dell'host (DHU/head unit) e dalla sua quota interna di refresh dei template — non riproducibile in sandbox JVM/strumentata | `powershell -File scripts/dhu-quota-check.ps1 -DurationSeconds 600` seguendo `docs/dhu-quota-verification.md`. **Già eseguito e superato** (2026-09-02, telefono fisico OnePlus 8T via DHU): 586 refresh in 608s, cadenza media 0.964/s, PID mai cambiato, host mai chiuso l'app. Vedi `08-03-SUMMARY.md`. |
| Numero grande/leggibile e centrato sullo schermo auto (SC1 come letteralmente formulato) | AA-01 (nuance) | Richiede ispezione visiva del rendering host-controlled del `PaneTemplate` — non esiste API per asserire dimensione/posizione del font in un test | Nessuna azione di verifica ulteriore prevista: **osservato FALLIRE** come letteralmente formulato (limite strutturale dell'API `PaneTemplate`/`Row`, non un bug) e **accettato consapevolmente per v2.0** via decisione esplicita D-13 (`08-CONTEXT.md`). `AA-01` resta soddisfatto nell'accezione "stile/tipografia gestiti dall'host" già scritta in REQUIREMENTS.md. Alternativa (`NavigationTemplate`+`SurfaceCallback`) rimandata a milestone v2.1 dedicata (D-14). |
| Stato "Ricerca segnale..." mostrato sullo schermo auto durante una perdita di segnale reale in sessione live | AA-02 (nuance) | Richiede simulare una perdita di segnale GPS reale durante una sessione DHU attiva | **Accettato senza verifica live** su istruzione esplicita dell'utente ("segna SC2 come superata e vai avanti", `08-03-SUMMARY.md`). Copertura indiretta esistente: `CarSpeedContentTest.noSignal_returnsSearching` (mappatura pura NoSignal→Searching) + `GpsSpeedProviderStateTest` (transizione di staleness a 5s) + `SpeedScreenTemplateTest.initialState_showsSearchingRowWithoutUnit` (rendering dello stato Searching nel template). Rischio residuo documentato, non riaperto qui — se un problema emergesse in una fase futura (es. Fase 11 su strada), va investigato lì senza assumere che questa fase l'abbia già escluso empiricamente. |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references (nessuna referenza MISSING trovata)
- [x] No watch-mode flags
- [x] Feedback latency < 2min
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** approved 2026-09-03 (audit retroattivo, nessun gap trovato — tutti i 9 task dei 3 piani avevano già verifica automatizzata eseguita con successo; i 3 comportamenti manual-only erano già stati chiusi o esplicitamente accettati durante l'esecuzione originale della fase, 2026-08-31 → 2026-09-02)

---

## Audit Trail

### Validation Audit 2026-09-03

| Metric | Count |
|--------|-------|
| Gaps found | 0 |
| Resolved | 0 |
| Escalated | 0 |
| Manual-only items confirmed already discharged | 3 (SC4 PASS, SC1 accettato con nuance, SC2 accettato senza verifica live) |

Nessun task del codebase auditor (`gsd-nyquist-auditor`) spawnato: la ricostruzione da PLAN/SUMMARY più la riesecuzione di `testDebugUnitTest` (75/75 verdi) e `compileDebugAndroidTestKotlin` (BUILD SUCCESSFUL) in questo audit ha confermato che ogni task aveva già un comando `<automated>` funzionante e nessun requisito risultava senza copertura raggiungibile via automazione.
