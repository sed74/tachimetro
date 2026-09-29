---
phase: quick-260929-cyv
plan: 01
subsystem: ui (telefono + Surface Android Auto), build, playstore
tags: [version-label, android-auto, surface, playstore, versionCode]
requires: []
provides:
  - formatVersionLabel(versionName, versionCode) pura e testata
  - etichetta versione sul telefono (alto centro) e sulla Surface (basso destra, anche in release)
  - versionCode 6 / 2.1-beta
affects:
  - app/src/main/java/com/sed/tachimetro/MainActivity.kt
  - app/src/main/java/com/sed/tachimetro/car/SpeedSurfaceRenderer.kt
tech-stack:
  added: []
  patterns: [funzione pura top-level + unit test JVM, window insets top su TextView ancorata al parent]
key-files:
  created:
    - app/src/main/java/com/sed/tachimetro/VersionLabel.kt
    - app/src/test/java/com/sed/tachimetro/VersionLabelTest.kt
  modified:
    - app/src/main/res/values/colors.xml
    - app/src/main/res/layout/activity_main.xml
    - app/src/main/java/com/sed/tachimetro/MainActivity.kt
    - app/src/main/java/com/sed/tachimetro/car/SpeedSurfaceRenderer.kt
    - app/build.gradle.kts
    - playstore/README.md
    - playstore/release_notes/release_notes_v2.1-beta.txt
decisions:
  - "Etichetta versione 'v<name> (<code>)' prodotta da un'unica funzione pura formatVersionLabel; nome vuoto -> '?'"
  - "Telefono: etichetta grigia 11sp in alto al centro con inset top; Surface: basso a destra della stable area, disegnata sempre (anche in release), fuori dal blocco BuildConfig.DEBUG"
  - "versionCode 6 (versionName resta 2.1-beta; il 5 e' gia' sul test chiuso)"
metrics:
  duration: ~10 min
  completed: 2026-09-29
  tasks: 3
  files: 9
---

# Quick 260929-cyv: numero di versione visibile su telefono e Android Auto Summary

Etichetta grigia "v2.1-beta (6)" da `formatVersionLabel(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)`: sul telefono in alto al centro (11sp, sotto status bar/cutout grazie agli inset), sulla Surface di Android Auto in basso a destra della stable area anche in release. versionCode portato a 6 e playstore/ riallineato.

## Tasks

| Task | Nome | Commit |
|------|------|--------|
| 1 (RED) | Test fallito per formatVersionLabel | fb98f51 |
| 1 (GREEN) | Implementazione formatVersionLabel | f009624 |
| 2 | Etichetta versione su telefono e Surface | c9579b9 |
| 3 | versionCode 6 e playstore/ allineato | 22164c0 |

## Dettagli

- `VersionLabel.kt`: `formatVersionLabel` pura, trim del nome, "?" se vuoto. 5 casi di test verdi.
- `activity_main.xml`: `@+id/versionText` dichiarata dopo messageText, vincoli top/start/end al parent, nessun vincolo esistente modificato.
- `MainActivity.kt`: `versionText`, `setupVersionLabel()` chiamata subito dopo `setupPermissionViews()`, `applyVersionTextWindowInsets()` (specchio top-only di `applyUnitTextWindowInsets()`, posizionata subito dopo di essa).
- `SpeedSurfaceRenderer.kt`: `versionPaint` (Align.RIGHT, #808080), `versionLabel`, nuovo passo 3b in `drawContent` prima e fuori da `if (BuildConfig.DEBUG)`; dimensione `max(14px, 3% altezza Surface)`, padding `OUTLINE_STROKE_PX * 2`. KDoc D-11 e commento passo 4-5 aggiornati. Nessun nuovo Log (T-q-02).
- Note di rilascio 2.1-beta: nuovo bullet in entrambe le lingue. Lunghezza blocchi: it-IT 366 caratteri, en-US 332 caratteri (limite 500).
- README playstore/: riferimenti correnti portati a 6 (intestazione, Versione, Aspetto in release, tabella, sezione Versionamento, prossimo caricamento -> 7); unico riferimento residuo al 5 e' la nota "gia' caricato". Riferimenti storici al 4 invariati.

## Verifica

- `:app:testSpikePoiDebugUnitTest` BUILD SUCCESSFUL (97 test, 0 falliti, incluso VersionLabelTest).
- `:app:assembleSpikePoiDebug :app:assembleSpikeNavDebug` BUILD SUCCESSFUL.
- Nessuna build release firmata eseguita; keystore.properties non toccato.
- Controllo visivo consigliato (non bloccante): telefono portrait/landscape (etichetta non tocca il numero) e DHU/auto (angolo basso-destro libero dalle cifre).

## Deviations from Plan

None - plan executed exactly as written.

## TDD Gate Compliance

RED `test(quick-260929-cyv)` fb98f51 precede GREEN `feat(quick-260929-cyv)` f009624. Nessun refactor necessario.

## Self-Check: PASSED

- File creati presenti: VersionLabel.kt, VersionLabelTest.kt.
- Commit presenti: fb98f51, f009624, c9579b9, 22164c0.
