---
phase: 09
slug: permesso-di-localizzazione-dallo-schermo-auto
status: complete
nyquist_compliant: true
wave_0_complete: true
created: 2026-09-03
reconstructed: true
---

# Phase 09 — Validation Strategy

> Ricostruito retroattivamente (State B: nessun `09-VALIDATION.md` esisteva, ricostruito da PLAN/SUMMARY dei 3 piani) tramite `/gsd:validate-phase 9`.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4.13.2 (JVM, `app/src/test/`) + AndroidX Test Ext JUnit 1.1.5 / Espresso 3.5.1 (strumentato, `app/src/androidTest/`) |
| **Config file** | `app/build.gradle.kts` (testRunner `androidx.test.runner.AndroidJUnitRunner`) |
| **Quick run command** | `./gradlew.bat :app:testDebugUnitTest --tests 'com.sed.tachimetro.car.CarPermissionStateTest'` |
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
| 09-01-T1 | 01 | 1 | AA-04 | unit (TDD) | `./gradlew.bat :app:testDebugUnitTest --tests 'com.sed.tachimetro.car.CarPermissionStateTest'` | ✅ `CarPermissionStateTest.kt` | ✅ green (10/10 test) |
| 09-01-T2 | 01 | 1 | AA-04 (persistenza) | build+grep | `./gradlew.bat :app:assembleDebug` + grep struttura store | ✅ `CarPermissionDenialStore.kt` | ✅ green |
| 09-01-T3 | 01 | 1 | AA-04 (copy) | build+grep | `./gradlew.bat :app:assembleDebug` + grep stringhe | ✅ | ✅ green |
| 09-02-T1 | 02 | 2 | AA-04 | build+regression+grep | `./gradlew.bat :app:assembleDebug && :app:testDebugUnitTest` + grep macchina a stati | ✅ | ✅ green (nessuna regressione JVM) |
| 09-02-T2 | 02 | 2 | AA-04 | build+regression+grep | `./gradlew.bat :app:assembleDebug && :app:testDebugUnitTest` + grep rendering/Action | ✅ | ✅ green |
| 09-03-T1 | 03 | 3 | AA-04 | instrumented | `./gradlew.bat :app:connectedDebugAndroidTest` (compile riverificato in questo audit: `compileDebugAndroidTestKotlin` OK) | ✅ `SpeedScreenTemplateTest.kt` (esteso a tutti gli stati permesso) | ✅ green (eseguito con successo durante sessione DHU, 09-03-SUMMARY.md) |
| 09-03-T2 | 03 | 3 | AA-04 (gate umano) | manual | checkpoint bloccante, vedi Manual-Only sotto | n/a | ✅ approvato 2026-09-02 (D-08/D-09) |

*Verifica di questo audit (2026-09-03): `./gradlew.bat :app:testDebugUnitTest` → 75 test totali nel modulo (inclusi i 10 di `CarPermissionStateTest`), 0 failures, 0 errors. `./gradlew.bat :app:compileDebugAndroidTestKotlin` → BUILD SUCCESSFUL, nessuna regressione di compilazione su `SpeedScreenTemplateTest.kt` esteso.*

---

## Wave 0 Requirements

Nessuna — infrastruttura di test già presente dalla Fase 1/2/8 e riusata senza modifiche per la Fase 9.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|--------------------|
| Richiesta automatica del permesso al primo collegamento, dialogo di sistema sul telefono, distinzione rifiuto singolo/permanente, apertura impostazioni | AA-04 (SC1/SC2/SC3 di roadmap Fase 9) | Richiede un dialogo di sistema reale e un host Android Auto reale (DHU/head unit) — non riproducibile in sandbox JVM/strumentata | Setup + 6 scenari (A-F) in `09-03-PLAN.md` `<how-to-verify>`, seguendo `docs/dhu-quota-verification.md` per il setup DHU. **Già eseguito e approvato** (2026-09-02): tutti i punti A1-F2 confermati, D-08 registrato in `09-CONTEXT.md`. |
| Transizione di forma del template (Pane di sola Row ↔ Pane con Row+Action) senza chiusura dell'app da parte dell'host (Pitfall 4) | AA-04 (quota host) | Dipende dalla quota interna di refresh/rebuild dei template dell'host, mai esercitata prima su un cambio di *forma* strutturale (solo di contenuto in Fase 8) | Stesso checkpoint DHU (Scenario E, 09-03-PLAN.md). **Già eseguito e superato**: PID stabile durante tutte le transizioni, nessuna chiusura, nessun errore host. Chiuso empiricamente in D-08. |
| Richiesta di permesso silenziosamente ignorata dall'host se il veicolo è già in movimento al collegamento (Pitfall 1) | AA-04 (limite noto) | Comportamento dell'host non documentato ufficialmente e non riproducibile in modo deterministico in un test automatico (dipende dallo stato "in movimento" rilevato dall'head unit) | Nessuna azione di verifica ulteriore prevista: **limite di piattaforma accettato esplicitamente dall'utente per v2.0** (Scenario G, D-09 in `09-CONTEXT.md`) — nessuna azione di sblocco manuale aggiunta allo stato `Waiting`, nessuna modifica a D-05/D-06. Registrato come concern noto in `STATE.md`. |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references (nessuna referenza MISSING trovata)
- [x] No watch-mode flags
- [x] Feedback latency < 2min
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** approved 2026-09-03 (audit retroattivo, nessun gap trovato — tutti i 7 task dei 3 piani avevano già verifica automatizzata eseguita con successo; il comportamento manual-only era già stato chiuso con checkpoint umano DHU durante l'esecuzione originale della fase, 2026-09-02)

---

## Audit Trail

### Validation Audit 2026-09-03

| Metric | Count |
|--------|-------|
| Gaps found | 0 |
| Resolved | 0 |
| Escalated | 0 |
| Manual-only items confirmed already discharged | 3 (SC1-SC3 roadmap approvati D-08, Pitfall 4 chiuso D-08, Pitfall 1 accettato D-09) |

Nessun task del codebase auditor (`gsd-nyquist-auditor`) spawnato: la ricostruzione da PLAN/SUMMARY più la riesecuzione di `testDebugUnitTest` (75/75 verdi, inclusi i 10 test di `CarPermissionStateTest`) e `compileDebugAndroidTestKotlin` (BUILD SUCCESSFUL) in questo audit ha confermato che ogni task aveva già un comando `<automated>` funzionante e nessun requisito risultava senza copertura raggiungibile via automazione.
