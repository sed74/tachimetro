---
phase: 05-gestione-schermo
plan: 01
subsystem: ui
tags: [android, kotlin, switchcompat, sharedpreferences, batterymanager, window-insets]

# Dependency graph
requires:
  - phase: 04-velocit-massima-e-persistenza
    provides: SharedPreferences persistence pattern (MaxSpeedStore) and window insets pattern (applyMaxAreaWindowInsets) replicated here
provides:
  - keepScreenOnSwitch (SwitchCompat) sempre visibile in basso a sinistra, speculare all'area MAX
  - ScreenOnPreferenceStore: wrapper SharedPreferences per un Boolean nullable (distingue "nessuna preferenza" da "false salvato")
  - Applicazione/rimozione immediata di FLAG_KEEP_SCREEN_ON al tap dello switch
  - Default derivato dallo stato di ricarica (BatteryManager) al primissimo avvio, persistito una sola volta
  - applyScreenSwitchWindowInsets: gestione insets bottom-left speculare a applyMaxAreaWindowInsets
affects: [05-02 (verifica on-device di questo piano)]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "SharedPreferences Boolean nullable via prefs.contains() per distinguere 'primo avvio' da 'false esplicito'"
    - "Default comportamentale derivato da un broadcast sticky di sistema (ACTION_BATTERY_CHANGED) letto una tantum al primo avvio"
    - "ColorStateList grayscale (alpha-only su bianco) per override di tint AppCompat senza introdurre nuove tinte"

key-files:
  created:
    - app/src/main/java/com/sed/tachimetro/screen/ScreenOnPreferenceStore.kt
    - app/src/main/res/color/switch_track_tint.xml
    - app/src/main/res/color/switch_thumb_tint.xml
  modified:
    - app/src/main/res/values/strings.xml
    - app/src/main/res/layout/activity_main.xml
    - app/src/main/java/com/sed/tachimetro/MainActivity.kt

key-decisions:
  - "Riuso del file SharedPreferences esistente 'tachimetro_prefs' (stesso di MaxSpeedStore) con chiave nuova 'keep_screen_on', invece di un file dedicato"
  - "read() restituisce Boolean? (null = primo avvio) invece di un default hardcoded, per permettere al chiamante di derivare il default dallo stato di ricarica solo quando serve (D-05)"

patterns-established:
  - "Pattern 2: Window insets per-angolo con listener dedicato non condiviso (bottom-left per keepScreenOnSwitch, distinto da top-left/top-right esistenti)"

requirements-completed: [SCRN-01, SCRN-02, SCRN-03]

# Metrics
duration: 15min
completed: 2026-07-10
---

# Phase 05 Plan 01: Toggle Schermo Sempre Acceso Summary

**SwitchCompat "Sempre acceso" in basso a sinistra con FLAG_KEEP_SCREEN_ON applicato/rimosso immediatamente al tap e persistenza SharedPreferences, con default derivato dallo stato di ricarica al primissimo avvio.**

## Performance

- **Duration:** ~15 min
- **Started:** 2026-07-10T18:07:00Z
- **Completed:** 2026-07-10T18:22:46Z
- **Tasks:** 3/3 completati
- **Files modified:** 6 (3 creati, 3 modificati)

## Accomplishments
- Nuovo `ScreenOnPreferenceStore` con `read(): Boolean?` che distingue "nessuna preferenza salvata" (primo avvio) da "false salvato esplicitamente", riusando il file `tachimetro_prefs` esistente con chiave `keep_screen_on`
- `keepScreenOnSwitch` (SwitchCompat) aggiunto al layout, bottom-left, speculare verticalmente a `maxSpeedText`/`resetMaxButton`, sempre `VISIBLE`, tint grayscale via due nuovi `ColorStateList`
- `MainActivity` cablata: legge la preferenza, deriva il default da `isDeviceCharging()` (broadcast sticky `ACTION_BATTERY_CHANGED`) solo al primo avvio, imposta `isChecked` prima di registrare il listener (nessun flash), applica/rimuove `FLAG_KEEP_SCREEN_ON` immediatamente al tap e persiste ogni cambio
- `applyScreenSwitchWindowInsets()` aggiunto, speculare a `applyMaxAreaWindowInsets()`, per l'angolo bottom-left

## Task Commits

Each task was committed atomically:

1. **Task 1: Stringa italiana, due ColorStateList grayscale e ScreenOnPreferenceStore** - `1ff0776` (feat)
2. **Task 2: Aggiungere keepScreenOnSwitch (SwitchCompat) in basso a sinistra nel layout** - `2e82eab` (feat)
3. **Task 3: Cablare MainActivity — default da ricarica, FLAG_KEEP_SCREEN_ON, persistenza, insets** - `0b99a41` (feat)

_Nessuna commit di refactor necessaria — nessun task TDD in questo piano (nessuna logica di business pura da testare, vedi note del piano)._

## Files Created/Modified
- `app/src/main/res/values/strings.xml` - aggiunta stringa `keep_screen_on_label` ("Sempre acceso")
- `app/src/main/res/color/switch_track_tint.xml` - ColorStateList grayscale per il track (checked #80FFFFFF / unchecked #33FFFFFF)
- `app/src/main/res/color/switch_thumb_tint.xml` - ColorStateList bianco opaco per il thumb
- `app/src/main/java/com/sed/tachimetro/screen/ScreenOnPreferenceStore.kt` - wrapper SharedPreferences per un Boolean nullable
- `app/src/main/res/layout/activity_main.xml` - aggiunto `keepScreenOnSwitch` (SwitchCompat) bottom-left
- `app/src/main/java/com/sed/tachimetro/MainActivity.kt` - wiring completo: default da ricarica, FLAG_KEEP_SCREEN_ON, persistenza, insets

## Decisions Made
None - piano eseguito esattamente come scritto, incluse le interfacce e gli snippet di codice forniti nel PLAN.md.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Creato `local.properties` locale nel worktree per abilitare la build Gradle**
- **Found during:** Task 1 (prima esecuzione di `./gradlew.bat :app:assembleDebug`)
- **Issue:** Il worktree Git isolato non ha un proprio `local.properties` (file gitignored, specifico della macchina locale) — la build falliva con "SDK location not found"
- **Fix:** Creato `local.properties` nel worktree con lo stesso `sdk.dir` del checkout principale, per permettere la verifica automatizzata `assembleDebug` richiesta da ogni task
- **Files modified:** `local.properties` (worktree-only, gitignored, non committato)
- **Verification:** `./gradlew.bat :app:assembleDebug` verde su tutti e tre i task
- **Committed in:** N/A (file gitignored, intenzionalmente non tracciato)

---

**Total deviations:** 1 auto-fixed (1 blocking, ambientale/build-only, nessun impatto sul codice applicativo)
**Impact on plan:** Nessun impatto sul codice di prodotto o sul comportamento dell'app — solo un prerequisito locale per eseguire la verifica automatizzata richiesta dal piano in questo worktree isolato.

## Issues Encountered
None oltre alla deviazione documentata sopra.

## User Setup Required

None - nessuna configurazione di servizi esterni richiesta.

## Next Phase Readiness
- Codice compilato e verificato via `assembleDebug` dopo ogni task; pronto per la verifica comportamentale on-device nel piano 05-02 (applicazione immediata del flag, persistenza post-riavvio con `adb reboot`, default da stato di ricarica al primo avvio, insets nei due orientamenti, overlap check con `retryButton`)
- Nessun blocco noto

## Known Stubs

Nessuno stub — tutti gli elementi sono cablati a dati/comportamento reali (nessun placeholder, nessun valore hardcoded vuoto).

---
*Phase: 05-gestione-schermo*
*Completed: 2026-07-10*

## Self-Check: PASSED

All created/modified files verified present on disk; all three task commit hashes (1ff0776, 2e82eab, 0b99a41) verified present in git log.
