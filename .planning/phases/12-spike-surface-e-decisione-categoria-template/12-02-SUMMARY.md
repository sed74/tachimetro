---
phase: 12-spike-surface-e-decisione-categoria-template
plan: 02
subsystem: android-auto-surface
tags: [android-auto, surface, spike, template, renderer]
requires:
  - "SurfaceTextFit (AreaPx, effectiveArea, fitTextSizePx, digitHeightPx) dal Piano 01"
  - "BuildConfig.SPIKE_POI_CARD / FLAVOR dal Piano 01"
provides:
  - "SpeedSurfaceRenderer: disegno di prova D-03, log D-10 TachimetroSurface, testo api D-11"
  - "buildSurfaceTemplate(carContext) per flavor (spikePoi: MapWithContentTemplate, spikeNav: NavigationTemplate)"
  - "SpeedScreen: ramo Granted deviato sul template con Surface"
affects:
  - "Piano 03 / sessioni DHU (lo script consuma i log TachimetroSurface)"
  - "Fase 13 (renderer definitivo collegato alla velocita')"
tech-stack:
  added: []
  patterns:
    - "SurfaceCallback passivo posseduto dalla Session come DefaultLifecycleObserver"
    - "stessa funzione top-level in due source set di flavor per variare il template"
key-files:
  created:
    - app/src/main/java/com/sed/tachimetro/car/SpeedSurfaceRenderer.kt
    - app/src/spikePoi/java/com/sed/tachimetro/car/SurfaceTemplateFactory.kt
    - app/src/spikeNav/java/com/sed/tachimetro/car/SurfaceTemplateFactory.kt
  modified:
    - app/src/main/java/com/sed/tachimetro/car/TachimetroCarSession.kt
    - app/src/main/java/com/sed/tachimetro/car/SpeedScreen.kt
decisions:
  - "Card POI senza action strip e azione NAV con CarIcon.APP_ICON: entrambe accettate da build(); il comportamento dell'host va confermato su DHU"
  - "Rimosso invalidate() a 1 Hz nel ramo Granted: il template con Surface e' statico (Anti-Pattern 1)"
  - "Contorni disegnati mezzo spessore all'interno dell'area, cosi' restano visibili anche sui bordi della Surface"
metrics:
  duration: "~12 min"
  completed: 2026-09-23
  tasks: 2
  files: 5
---

# Phase 12 Plan 02: Renderer Surface e template per flavor Summary

`SpeedSurfaceRenderer` passivo (sfondo nero, "888" dimensionato con `fitTextSizePx` nella stable area, contorni verde/magenta, testo giallo `api=... <flavor>/<card> WxH`, log `TachimetroSurface` nel formato del contratto D-10) posseduto dalla Session, piu' `buildSurfaceTemplate()` per flavor (POI: `MapWithContentTemplate` con 4 card; NAV: `NavigationTemplate` con action strip minima) usato solo nel ramo Granted di `SpeedScreen`.

## Task

| Task | Nome | Commit | File |
|------|------|--------|------|
| 1 | SpeedSurfaceRenderer + aggancio alla Session | af151bb, 2f840a6 (fix) | SpeedSurfaceRenderer.kt, TachimetroCarSession.kt |
| 2 | SurfaceTemplateFactory per flavor + deviazione ramo Granted | a210f68 | spikePoi/.../SurfaceTemplateFactory.kt, spikeNav/.../SurfaceTemplateFactory.kt, SpeedScreen.kt |

## Verifiche eseguite

- `:app:compileSpikePoiDebugKotlin :app:compileSpikeNavDebugKotlin`: OK
- `:app:assembleSpikePoiDebug -PpoiCard=message|list|pane|grid`: tutte e 4 OK
- `:app:assembleSpikeNavDebug`, `:app:assembleSpikePoiDebugAndroidTest`: OK
- `:app:testSpikePoiDebugUnitTest` / `:app:testSpikeNavDebugUnitTest`: 86 test, 0 fallimenti ciascuno
- Grep del piano: tutte le stringhe di log del contratto presenti letteralmente, nessun riferimento a `GpsSpeedProvider` nel renderer, `addObserver` nella Session, una sola `addAction(` nel NAV, nessun `NavigationTemplate` nel POI
- `import android.graphics.Canvas` solo in `SpeedSurfaceRenderer.kt`
- `git diff` di `SpeedScreen.kt`: `buildTemplate()` e rami non-Granted non toccati; l'`invalidate()` in testa a `collectLatest` e' ancora presente

## Deviazioni dal piano

### Correzioni automatiche

**1. [Rule 1 - Bug] Log D-10 e KDoc allineati al contratto**
- **Trovato durante:** Task 1 (verify automatico)
- **Problema:** le righe `onStableAreaChanged`/`onVisibleAreaChanged` componevano `l= t= r= b=` in un helper, quindi le stringhe letterali richieste dal criterio non c'erano; il KDoc citava `GpsSpeedProvider` e faceva fallire il grep negativo di D-03
- **Correzione:** campi scritti direttamente nel messaggio di log (output identico), KDoc riformulato ("provider GPS")
- **Commit:** 2f840a6

**2. [Precisazione] KDoc "unico file che importa android.graphics.*"**
- `MainActivity.kt` importa gia' `android.graphics.drawable.*` (codice esistente). Il KDoc dice ora "unico file che disegna con Canvas/Paint"; il criterio del piano (solo `import android.graphics.Canvas`) e' rispettato. La verifica di piano `grep -rn 'import android.graphics' app/src/main/java` elenca quindi anche `MainActivity.kt` (preesistente, fuori scope).

A parte questo il piano e' stato eseguito come scritto: nessun fallback necessario (la card POI senza action strip e `CarIcon.APP_ICON` sull'azione NAV passano entrambi da `build()`; il comportamento dell'host resta da confermare nelle sessioni DHU).

## Known Stubs

- `SurfaceTemplateFactory.kt` (entrambi i flavor): testo segnaposto "Tachimetro" e azione NAV senza effetti sono voluti (D-07/D-08), la build di spike non viene distribuita (D-05); flavor e factory vengono rimossi dal Piano 06 (D-02).
- `SpeedSurfaceRenderer`: "888" fisso, non collegato alla velocita' (D-03); il collegamento e' compito della Fase 13.

## Self-Check: PASSED

- FOUND: app/src/main/java/com/sed/tachimetro/car/SpeedSurfaceRenderer.kt, app/src/spikePoi/java/com/sed/tachimetro/car/SurfaceTemplateFactory.kt, app/src/spikeNav/java/com/sed/tachimetro/car/SurfaceTemplateFactory.kt
- FOUND commit: af151bb, 2f840a6, a210f68

## Nota sui requisiti

REL-01 non e' stato marcato completo: richiede le misure su DHU e la decisione esplicita dell'utente (piani successivi della fase).
