---
phase: 06-indicatore-di-ricarica
plan: 01
subsystem: ui
tags: [android, xml-layout, constraintlayout, vector-drawable, layer-list, clip-drawable, resources]

# Dependency graph
requires:
  - phase: 05-gestione-schermo
    provides: "keepScreenOnSwitch (SwitchCompat) nel layout esistente, ancora di riferimento per la nuova icona"
provides:
  - "Colore lime_charging_accent (#FFAEEA00) in colors.xml, unico colore accento dell'app (D-04)"
  - "Stringa charging_indicator_description (\"In carica\") per l'accessibilita dell'icona"
  - "Drawable ic_charging_flash.xml (base bianca) e ic_charging_flash_lime.xml (livello lime), glifo Material flash_on (D-05)"
  - "Drawable charging_flash_fill.xml: layer-list con ClipDrawable verticale bottom-gravity, level 0..10000 pilotabile via codice (R.id.chargingIconFill)"
  - "ImageView chargingIcon nel layout, 24dp, gone di default, non interattiva, centrata verticalmente sulla riga di keepScreenOnSwitch, a sinistra di esso"
affects: ["06-02", "06-03", "06-04"]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Layer-list + ClipDrawable per riempimento progressivo (prima occorrenza nel progetto di drawable multi-layer/animabile)"
    - "Due vector drawable statici distinti (bianco/lime) invece di un tint runtime, per evitare condivisione di ConstantState tra i due livelli dello stesso layer-list"

key-files:
  created:
    - app/src/main/res/drawable/ic_charging_flash.xml
    - app/src/main/res/drawable/ic_charging_flash_lime.xml
    - app/src/main/res/drawable/charging_flash_fill.xml
  modified:
    - app/src/main/res/values/colors.xml
    - app/src/main/res/values/strings.xml
    - app/src/main/res/layout/activity_main.xml

key-decisions:
  - "lime_charging_accent = #FFAEEA00 (Material Lime A700), come da UI-SPEC, valore bloccato non soggetto a discrezione ulteriore"
  - "chargingIcon ancorato verticalmente a @id/keepScreenOnSwitch (top/bottom), non a parent, per restare centrato sulla riga nonostante l'altezza minima 48dp dello switch (D-06)"

patterns-established:
  - "Contratto id chargingIconBase/chargingIconFill nel layer-list: il Piano 03 li referenzia via LayerDrawable.findDrawableByLayerId(R.id.chargingIconFill) — non rinominare"

requirements-completed: [CHRG-01, CHRG-02]

# Metrics
duration: 7min
completed: 2026-08-29
---

# Phase 6 Plan 1: Risorse Icona di Ricarica Summary

**Colore lime, stringa di accessibilità e i tre drawable del fulmine riempibile (layer-list + ClipDrawable), con `chargingIcon` inserita nel layout a sinistra di `keepScreenOnSwitch`.**

## Performance

- **Duration:** ~7 min
- **Started:** 2026-08-29T19:16:04+02:00 (base commit)
- **Completed:** 2026-08-29T19:22:40+02:00
- **Tasks:** 2/2 completed
- **Files modified:** 6 (3 created, 3 modified)

## Accomplishments
- Aggiunto l'unico colore accento ammesso nell'intera app (`lime_charging_accent`), riservato esclusivamente all'icona di ricarica (D-04), verificato che non compaia in nessun'altra risorsa
- Creati i tre drawable del fulmine: base bianca (`ic_charging_flash.xml`), livello lime (`ic_charging_flash_lime.xml`), e il layer-list combinato (`charging_flash_fill.xml`) con `ClipDrawable` verticale a gravità `bottom`, pronto per essere pilotato via `level` (0..10000) dal Piano 03
- Inserita `chargingIcon` (`ImageView` 24dp, non interattiva, `gone` di default) nel layout, centrata verticalmente sulla riga di `keepScreenOnSwitch` e non su `parent`, per restare visivamente allineata nonostante il `minHeight="48dp"` dello switch

## Task Commits

Each task was committed atomically:

1. **Task 1: Aggiungere colore lime, stringa di accessibilità e i tre drawable del fulmine** - `b628705` (feat)
2. **Task 2: Inserire chargingIcon nel layout e ri-agganciare keepScreenOnSwitch** - `a9e8366` (feat)

**Plan metadata:** commit pending (docs: complete plan, added by executor after this summary)

## Files Created/Modified
- `app/src/main/res/values/colors.xml` - Aggiunta `lime_charging_accent` (#FFAEEA00)
- `app/src/main/res/values/strings.xml` - Aggiunta `charging_indicator_description` ("In carica")
- `app/src/main/res/drawable/ic_charging_flash.xml` - Vector 24x24dp, glifo Material flash_on, fillColor bianco
- `app/src/main/res/drawable/ic_charging_flash_lime.xml` - Vector identico, fillColor lime
- `app/src/main/res/drawable/charging_flash_fill.xml` - Layer-list: item base (`chargingIconBase`) + item clip verticale bottom-gravity (`chargingIconFill`)
- `app/src/main/res/layout/activity_main.xml` - Nuova `ImageView chargingIcon`; `keepScreenOnSwitch` ri-agganciato a `layout_constraintStart_toEndOf="@id/chargingIcon"` con margine 8dp

## Decisions Made
- Nessuna deviazione dalle decisioni UI-SPEC/CONTEXT: colore lime, glifo, posizionamento e contratto id (`chargingIconBase`/`chargingIconFill`) applicati esattamente come specificato.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Creato `local.properties` locale nel worktree**
- **Found during:** Task 1 (verifica build `./gradlew.bat :app:assembleDebug`)
- **Issue:** Il worktree git è stato creato senza `local.properties` (file gitignored, specifico della macchina locale), causando il fallimento del build con "SDK location not found"
- **Fix:** Copiato lo stesso contenuto di `local.properties` dal repository principale (`sdk.dir=D:\Android\SDK`) nel worktree; il file resta non tracciato (gitignored), nessuna modifica al repository
- **Files modified:** `local.properties` (non tracciato, non incluso in alcun commit)
- **Verification:** `./gradlew.bat :app:assembleDebug` → `BUILD SUCCESSFUL`
- **Committed in:** n/a (file gitignored per design, non commitato)

---

**Total deviations:** 1 auto-fixed (1 blocking, ambiente locale)
**Impact on plan:** Nessun impatto sul codice applicativo o sul contenuto dei commit; necessario solo per eseguire la verifica di build all'interno del worktree isolato.

## Issues Encountered
None oltre alla deviazione sopra documentata.

## User Setup Required
None - nessuna configurazione di servizio esterno richiesta.

## Next Phase Readiness
- `R.id.chargingIcon`, `R.drawable.charging_flash_fill`, `R.id.chargingIconFill` e `@string/charging_indicator_description` sono pronti da referenziare nel Piano 03 (MainActivity/ChargingStateProvider)
- Nessun blocco noto: build verde, nessuna dipendenza circolare nei vincoli, lime confinato alle sole risorse previste (D-04 rispettato)
- Il Piano 02 (presumibilmente `ChargingStateProvider`/`ChargingState`) può procedere in parallelo poiché non modifica gli stessi file di questo piano

---
*Phase: 06-indicatore-di-ricarica*
*Completed: 2026-08-29*
