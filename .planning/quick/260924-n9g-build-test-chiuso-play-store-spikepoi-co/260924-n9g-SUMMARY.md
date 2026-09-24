---
phase: quick-260924-n9g
plan: 01
subsystem: android-auto-surface, release
tags: [android-auto, surface, play-store, closed-test, spikePoi]
requires: [Fase 12 piani 12-01..12-03 (renderer Surface, flavor spike)]
provides: [velocita' GPS reale sulla Surface, build release spikePoi pronta per test chiuso]
affects: [SpeedSurfaceRenderer, SpeedScreen, TachimetroCarSession, app/build.gradle.kts, playstore/]
tech-stack:
  added: []
  patterns: [callback Session -> Screen -> renderer (method reference), overlay diagnostico sotto BuildConfig.DEBUG]
key-files:
  created:
    - app/src/main/java/com/sed/tachimetro/car/SurfaceSpeedText.kt
    - app/src/test/java/com/sed/tachimetro/car/SurfaceSpeedTextTest.kt
    - playstore/release_notes/release_notes_v2.1-beta.txt
  modified:
    - app/src/main/java/com/sed/tachimetro/car/SpeedSurfaceRenderer.kt
    - app/src/main/java/com/sed/tachimetro/car/SpeedScreen.kt
    - app/src/main/java/com/sed/tachimetro/car/TachimetroCarSession.kt
    - app/build.gradle.kts
    - app/src/spikePoi/AndroidManifest.xml
    - app/src/spikeNav/AndroidManifest.xml
    - app/src/spikeNav/java/com/sed/tachimetro/car/SurfaceTemplateFactory.kt
    - playstore/README.md
decisions:
  - "Deroga D-05 (2026-09-24, richiesta utente): solo spikePoi card message va in test chiuso Play Store (versionCode 5, 2.1-beta); spikeNav mai distribuito"
  - "La velocita' arriva alla Surface via callback onSpeedState passata dalla Session allo Screen (renderer::updateSpeed), nessun invalidate a 1 Hz"
  - "Overlay di debug (contorni, righe D-11) solo sotto BuildConfig.DEBUG; in release solo cifre bianche su nero"
metrics:
  duration: ~20 min
  completed: 2026-09-24
  tasks: 3
  files: 11
---

# Quick 260924-n9g: Build di test chiuso Play Store spikePoi Summary

La Surface di Android Auto del flavor spikePoi ora mostra la velocita' GPS reale ("--" senza fix), con la dimensione fissata sul campione "888" (D-12) e senza overlay di debug in release. La versione passa a 5 / "2.1-beta" e `playstore/` e' pronta per il test chiuso.

## Task completati

| Task | Nome | Commit |
|------|------|--------|
| 1 (RED) | Test SurfaceSpeedText | a85e854 |
| 1 (GREEN) | Mappatura pura `surfaceSpeedText` + `SURFACE_SPEED_PLACEHOLDER` | 185a147 |
| 2 | Renderer con velocita' reale, overlay solo in DEBUG, collegamento Session -> Screen -> renderer | 22c3329 |
| 3 | Versione 2.1-beta, commenti sulla deroga D-05, playstore/ | c485df6 |

## Verifiche (esiti reali)

- `:app:testSpikePoiDebugUnitTest --tests SurfaceSpeedTextTest`: RED fallito in compilazione (simbolo mancante), come previsto; GREEN 6/6 test passati.
- `:app:testSpikePoiDebugUnitTest :app:assembleSpikeNavDebug :app:compileSpikePoiDebugAndroidTestKotlin`: BUILD SUCCESSFUL (solo warning di deprecazione gia' presenti su PaneTemplate e SpeedScreenTemplateTest). Il test strumentato compila con `SpeedScreen(testCarContext)` grazie al default del nuovo parametro.
- Controlli grep del Task 2: `onSpeedState(gpsState)` = 1, `drawText(speedText` = 1, nessun `drawText(SAMPLE_TEXT`, nessun `Log.*speedText`.
- Controlli grep del Task 3: tutti OK; `git diff -- .planning/ROADMAP.md` vuoto. Note di rilascio: 244 caratteri (it-IT) e 217 (en-US), sotto il limite di 500.
- **`:app:assembleSpikePoiRelease -PpoiCard=message`: FALLITO in `packageSpikePoiRelease`**:
  `KeytoolException: Failed to read key CHANGE_ME from store "C:\Users\fedes\AndroidStudioProjects\keystore\keystore": keystore password was incorrect`.
  Causa: `keystore.properties` (gitignored) esiste ma contiene ancora i valori segnaposto `CHANGE_ME` (3 occorrenze). Il problema riguarda l'ambiente, non il codice. Rieseguito con `-x packageSpikePoiRelease`: BUILD SUCCESSFUL, quindi compilazione release, lintVital e tutto il resto fino al packaging passano. Non ho toccato `keystore.properties` perche' contiene segreti dell'utente.

## Deviazioni dal piano

- **[Rule 3 - Blocking, ambiente]** La verifica `assembleSpikePoiRelease` completa non passa per le credenziali segnaposto del keystore. Verificato fino al packaging escluso; la firma resta un passo manuale dell'utente (passo 2 del README di playstore/).
- Commento di `onCarConfigurationChanged` in TachimetroCarSession aggiornato ("ridisegno della Surface" al posto di "ridisegno della prova"): e' solo testo, coerente con il nuovo ruolo del renderer.

## Azione per l'utente

Prima di `./gradlew.bat :app:bundleSpikePoiRelease -PpoiCard=message` compilare `storePassword`, `keyAlias` e `keyPassword` reali in `keystore.properties`. Oggi contiene `CHANGE_ME` e la build firmata fallisce.

## Known Stubs

Nessuno. "888" resta volutamente come campione di misura, non viene disegnato.

## Threat Flags

Nessuna nuova superficie. T-q-01/T-q-02 mitigati (log e overlay solo in DEBUG, la velocita' non finisce mai nei log), T-q-03 invariato (`onSpeedState` e' chiamato solo nel ramo Granted), T-q-05 (comando di release documentato solo per spikePoi).

## TDD Gate Compliance

Il commit RED `test(...)` a85e854 precede il commit GREEN `feat(...)` 185a147. Nessun refactor necessario.

## Self-Check: PASSED

- File creati presenti: SurfaceSpeedText.kt, SurfaceSpeedTextTest.kt, release_notes_v2.1-beta.txt
- Commit presenti: a85e854, 185a147, 22c3329, c485df6
