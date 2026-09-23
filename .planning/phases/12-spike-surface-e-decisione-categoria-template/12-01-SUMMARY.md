---
phase: 12-spike-surface-e-decisione-categoria-template
plan: 01
subsystem: android-auto-build
tags: [android-auto, gradle-flavors, manifest, surface, spike, tdd]
requires: []
provides:
  - "flavor temporanei spikePoi/spikeNav (dimensione surfaceSpike)"
  - "BuildConfig.SPIKE_POI_CARD (message|list|pane|grid, none per spikeNav)"
  - "manifest di flavor con permessi Car App disgiunti"
  - "minCarApiLevel 7 nel manifest main"
  - "AreaPx, effectiveArea(), fitTextSizePx(), digitHeightPx() framework-free"
affects:
  - "Piano 02 (renderer Surface: consuma SurfaceTextFit e SPIKE_POI_CARD)"
  - "Piano 06 (rimozione flavor, D-02)"
tech-stack:
  added: []
  patterns:
    - "product flavor temporanei con manifest per flavor e tools:node=replace sul service"
    - "proprieta' Gradle validata contro allow-list -> buildConfigField"
key-files:
  created:
    - app/src/spikePoi/AndroidManifest.xml
    - app/src/spikeNav/AndroidManifest.xml
    - app/src/main/java/com/sed/tachimetro/car/SurfaceTextFit.kt
    - app/src/test/java/com/sed/tachimetro/car/SurfaceTextFitTest.kt
  modified:
    - app/build.gradle.kts
    - app/src/main/AndroidManifest.xml
decisions:
  - "Selettore card POI = proprieta' Gradle -PpoiCard (default message), validata a configurazione: un valore non valido fa fallire qualsiasi task, anche delle varianti spikeNav"
  - "spikeNav ridichiara l'intero service con tools:node=replace invece di rimuovere la sola category POI"
  - "Commenti nei manifest scritti senza i nomi letterali dei permessi dell'altro percorso: il merger conserva i commenti nel manifest unito e i grep di verifica li troverebbero"
metrics:
  duration: "~15 min"
  completed: 2026-09-23
  tasks: 2
  files: 6
---

# Phase 12 Plan 01: Infrastruttura build spike Surface Summary

Due flavor Gradle temporanei (`spikePoi`/`spikeNav`) con manifest a permessi Car App disgiunti, selettore `-PpoiCard` validato che finisce in `BuildConfig.SPIKE_POI_CARD`, `minCarApiLevel` 7 nel manifest main e funzioni pure di fit di "888" (`SurfaceTextFit.kt`), testate in JVM, che fanno da base al criterio D-12.

## Tasks

| Task | Nome | Commit | File |
|------|------|--------|------|
| 1 | Flavor spikePoi/spikeNav, manifest di flavor, minCarApiLevel 7 | 5a2ef91 | app/build.gradle.kts, app/src/main/AndroidManifest.xml, app/src/spikePoi/AndroidManifest.xml, app/src/spikeNav/AndroidManifest.xml |
| 2 (RED) | Test JVM SurfaceTextFit | 434035f | app/src/test/java/com/sed/tachimetro/car/SurfaceTextFitTest.kt |
| 2 (GREEN) | Implementazione SurfaceTextFit | fab5f42 | app/src/main/java/com/sed/tachimetro/car/SurfaceTextFit.kt |

## Verifiche eseguite

- `:app:processSpikePoiDebugManifest :app:processSpikeNavDebugManifest` + verify automatico del Task 1: **OK**
  - spikePoiDebug: `ACCESS_SURFACE`, `MAP_TEMPLATES`, `category.POI`, niente `NAVIGATION_TEMPLATES`, `minCarApiLevel` "7"
  - spikeNavDebug: `ACCESS_SURFACE`, `NAVIGATION_TEMPLATES`, `category.NAVIGATION`, niente `MAP_TEMPLATES` ne' `category.POI`; il service mantiene `exported`, `label`, `icon`
- `-PpoiCard=bogus` fallisce con: `Valore -PpoiCard='bogus' non valido. Valori ammessi: message, list, pane, grid`
- Nel manifest main non ci sono righe con `TEMPLATES` (0 occorrenze, commenti inclusi)
- `:app:testSpikePoiDebugUnitTest :app:testSpikeNavDebugUnitTest`: BUILD SUCCESSFUL (SurfaceTextFitTest: 11 test, 0 fallimenti, piu' tutti i test JVM esistenti)
- `:app:assembleSpikePoiDebug :app:assembleSpikeNavDebug`: BUILD SUCCESSFUL
- `SurfaceTextFit.kt` non importa niente da `android.*`
- `automotive_app_desc.xml` non modificato

## TDD Gate Compliance

RED (`434035f`, compilazione fallita per `AreaPx`/`effectiveArea` non risolti) -> GREEN (`fab5f42`). Nessun refactor necessario.

## Deviazioni dal piano

### Correzioni automatiche

**1. [Rule 3 - Bloccante] `local.properties` assente nel worktree**
- **Trovato durante:** Task 1
- **Problema:** il worktree non ha `local.properties` (gitignored), quindi Gradle non trova l'SDK
- **Correzione:** copiato dalla checkout principale; il file resta gitignored e non committato
- **Commit:** nessuno (file non versionato)

**2. [Precauzione di verifica] Commenti dei manifest senza nomi letterali dei permessi**
- Il merger conserva i commenti XML nel manifest unito (verificato: il commento REL-02 compare nei manifest uniti). Per questo il commento del main dice "alcun permesso template Car App" invece di citare `MAP_TEMPLATES`/`NAVIGATION_TEMPLATES`, e il commento di spikeNav parla di "categoria POI" invece di `category.POI`. Senso invariato rispetto al testo del piano; altrimenti i grep di verifica (e il criterio "0 righe TEMPLATES nel main") darebbero falsi positivi.

A parte questo, il piano e' stato eseguito come scritto.

## Note

- La validazione di `poiCard` avviene in fase di configurazione Gradle: con un valore non valido falliscono anche i task delle varianti `spikeNav`. E' voluto (T-12-03) ed e' innocuo, perche' il default e' `message`.
- Finche' i flavor esistono i comandi cambiano nome (`installSpikePoiDebug`, `testSpikeNavDebugUnitTest`, ...); tornano quelli originali quando il Piano 06 rimuove i flavor (D-02).

## Self-Check: PASSED

- FOUND: app/src/spikePoi/AndroidManifest.xml, app/src/spikeNav/AndroidManifest.xml, app/src/main/java/com/sed/tachimetro/car/SurfaceTextFit.kt, app/src/test/java/com/sed/tachimetro/car/SurfaceTextFitTest.kt
- FOUND commit: 5a2ef91, 434035f, fab5f42
