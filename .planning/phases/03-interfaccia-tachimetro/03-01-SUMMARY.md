---
phase: 03-interfaccia-tachimetro
plan: 01
subsystem: ui
tags: [android, constraintlayout, autosize, textview, spannable, kotlin]

# Dependency graph
requires:
  - phase: 02-motore-gps
    provides: "SpeedState/GpsSpeedProvider, MainActivity già scrive su messageText via getString()"
provides:
  - "activity_main.xml: messageText a schermo intero con autosize uniform (box 0dp/0dp, centrato, Black/900)"
  - "MainActivity.kt: buildSpeedText() con RelativeSizeSpan per rimpicciolire l'unità km/h rispetto alle cifre"
  - "MainActivity.kt: applySpeedAutosize()/applyMessageAutosize() per due range autosize distinti (numero vs messaggi di stato) sullo stesso TextView"
affects: [04-velocita-massima, 05-schermo-sempre-acceso]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Un solo TextView condiviso (messageText) per numero e messaggi di stato (D-03), con range autosize commutato a runtime via TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration a seconda del tipo di contenuto"
    - "SpannableString + RelativeSizeSpan per variare la dimensione relativa di una sottostringa (unità km/h) senza introdurre una seconda vista di testo"

key-files:
  created: []
  modified:
    - app/src/main/res/layout/activity_main.xml
    - app/src/main/java/com/sed/tachimetro/MainActivity.kt

key-decisions:
  - "Autosize a due range: 12-300sp/4sp per il numero di velocità (dominante), 12-56sp/4sp per i messaggi di stato (compatti/leggibili) — stesso TextView, configurazione autosize applicata a runtime prima di ogni text assignment"
  - "Unità 'km/h' resa a 0.35x la dimensione delle cifre tramite RelativeSizeSpan su una SpannableString costruita da speed_kmh_format, invece di splittare in due TextView"

patterns-established:
  - "Pattern 1: quando un unico TextView autosize deve rappresentare contenuti di natura diversa (numero dominante vs testo di stato), commutare min/max/step via TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration() prima di ogni assegnazione di testo, invece di un unico range condiviso"
  - "Pattern 2: per dare enfasi visiva relativa a una porzione di stringa dentro lo stesso TextView (es. unità di misura più piccola dei valori numerici), usare SpannableString + RelativeSizeSpan invece di introdurre nuove viste"

requirements-completed: [UI-01, UI-02, UI-03, UI-05]

# Metrics
duration: n/a (Task 1 storico + fix di deviazione da checkpoint)
completed: 2026-07-10
---

# Phase 3 Plan 1: Interfaccia dominante con autosize a due range Summary

**TextView messageText a schermo intero con autosize uniform per il numero di velocità (12-300sp), unità "km/h" ridotta al 35% tramite RelativeSizeSpan, e cap autosize separato (12-56sp) per i messaggi di stato così restano compatti e leggibili invece di ingrandirsi come il numero.**

## Performance

- **Tasks:** 1 di 2 completato (Task 1 auto), Task 2 è un checkpoint umano tuttora in attesa di ri-verifica dopo il fix di questa deviazione
- **Files modified:** 2 (`activity_main.xml`, `MainActivity.kt`)

## Accomplishments
- `messageText` occupa l'intero spazio disponibile (box `0dp`/`0dp`), centrato, bianco Black/900 su sfondo nero, con autosize uniform e nessun `res/layout-land/` separato (D-01/D-02 rispettati)
- Il numero di velocità (es. "180 km/h") mostra le cifre come elemento dominante e l'unità "km/h" visibilmente più piccola (0.35x), tramite `SpannableString` + `RelativeSizeSpan` — nessuna nuova stringa, nessuna nuova vista
- I messaggi di stato ("Pronto", "Ricerca segnale GPS...", i due messaggi di permesso negato) ora usano un cap autosize molto più basso (max 56sp invece di 300sp) così restano leggibili e non esplodono in una singola cifra enorme andando a capo in modo illeggibile

## Task Commits

Storico esecuzione di questo plan (branch merged + fix corrente):

1. **Task 1: Applicare auto-size e stile dominante a messageText in activity_main.xml** - `8ecfb4e` (feat) — merged in `e7bbe89`
2. **Fix deviazione da checkpoint (Task 2 pending): unità km/h più piccola + cap autosize distinto per i messaggi di stato** - `fa7e3ef` (fix)

_Task 2 (checkpoint:human-verify) non è ancora marcato "done": richiede una nuova verifica visiva su device/emulatore dopo questo fix, per confermare che i due difetti segnalati siano risolti._

## Files Created/Modified
- `app/src/main/res/layout/activity_main.xml` - messageText a schermo intero, autosize uniform 12/300/4sp, box 0dp/0dp, centrato, Black/900, margini 16dp; retryButton invariato
- `app/src/main/java/com/sed/tachimetro/MainActivity.kt` - `buildSpeedText()` (SpannableString + RelativeSizeSpan sull'unità km/h), `applySpeedAutosize()`/`applyMessageAutosize()` (range autosize distinti commutati a runtime), chiamati da `showReady()`, `showDenied()`, `updatePlaceholder()`

## Decisions Made
- Range autosize distinti per numero (12-300sp) e messaggi di stato (12-56sp) sullo stesso `messageText`, commutati via `TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration()` a ogni cambio di contenuto — nessuna vista aggiuntiva, D-01/D-03 intatti
- Unità "km/h" a dimensione relativa 0.35x rispetto alle cifre, applicata con `RelativeSizeSpan` su una `SpannableString` costruita dalla stringa esistente `speed_kmh_format` — nessuna stringa nuova, nessuna modifica al formato localizzato
- Il default statico `app:autoSizeMaxTextSize="300sp"` in XML resta invariato: `checkAndRequestPermission()` (che chiama `showReady()`/`showDenied()`, entrambi ora con `applyMessageAutosize()`) viene eseguito sincronamente dentro `onCreate()` prima del primo frame disegnato, quindi non c'è flicker visibile con il default "grande" prima dello switch a runtime

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Unità "km/h" autosizata alla stessa dimensione enorme delle cifre**
- **Found during:** Feedback utente al checkpoint Task 2, testato su emulatore reale
- **Issue:** L'intera stringa "180 km/h" (cifre + unità) veniva autosizata come un blocco unico, rendendo l'unità di misura grande quanto le cifre invece di un'unità secondaria proporzionalmente più piccola
- **Fix:** `buildSpeedText()` costruisce una `SpannableString` da `speed_kmh_format` e applica `RelativeSizeSpan(0.35f)` sulla sola sottostringa " km/h" (calcolata da `kmh.toString().length` in poi), lasciando le cifre alla dimensione piena determinata dall'autosize
- **Files modified:** `app/src/main/java/com/sed/tachimetro/MainActivity.kt`
- **Verification:** `./gradlew.bat :app:assembleDebug` → BUILD SUCCESSFUL; revisione del codice conferma che lo span copre solo l'indice dopo le cifre fino alla fine della stringa
- **Committed in:** `fa7e3ef` (fix commit)

**2. [Rule 1 - Bug] Messaggi di stato lunghi autosizati verso il range enorme del numero, diventando quasi illeggibili**
- **Found during:** Feedback utente al checkpoint Task 2, testato su emulatore reale
- **Issue:** Con un unico range autosize condiviso (12-300sp) per numero e messaggi di stato, stringhe lunghe come "Permesso GPS negato. Aprire le impostazioni per abilitarlo" venivano ingrandite verso l'estremo alto del range e andavano a capo a dimensione enorme, risultando quasi illeggibili invece che compatte
- **Fix:** Aggiunte `applySpeedAutosize()` (12-300sp, invariato, per il numero) e `applyMessageAutosize()` (12-56sp, nuovo cap più basso, per tutti i messaggi di stato) via `TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration()`, chiamate rispettivamente da `updatePlaceholder()` (branch `Reading` vs `Searching`/`NoSignal`), `showReady()` e `showDenied()`
- **Files modified:** `app/src/main/java/com/sed/tachimetro/MainActivity.kt`
- **Verification:** `./gradlew.bat :app:assembleDebug` → BUILD SUCCESSFUL; revisione del codice conferma che ogni chiamata a `messageText.text = ...` è preceduta dalla configurazione autosize corretta per quel tipo di contenuto
- **Committed in:** `fa7e3ef` (fix commit)

---

**Total deviations:** 2 auto-fixed (entrambi Rule 1 - bug di resa visiva riportati dal checkpoint umano su device reale)
**Impact on plan:** Nessun cambiamento architetturale: D-01/D-02/D-03 restano intatti (un solo layout adattivo, nessun `res/layout-land/`, un solo `messageText` condiviso). Il fix è interamente contenuto nella configurazione/rendering runtime del TextView esistente in `MainActivity.kt`.

## Issues Encountered
- Il file `local.properties` (SDK path locale, gitignored) non era presente in questo worktree; ricreato localmente con lo stesso `sdk.dir` del repo principale solo per poter eseguire `./gradlew.bat :app:assembleDebug` — non committato (resta escluso da `.gitignore`).

## User Setup Required
None - nessuna configurazione di servizi esterni richiesta.

## Next Phase Readiness
- Task 2 (checkpoint:human-verify) di questo plan richiede una nuova verifica visiva su device/emulatore reale: confermare che (a) le cifre della velocità restano l'elemento dominante mentre "km/h" è visibilmente più piccolo, e (b) i messaggi di stato ("Ricerca segnale GPS...", i due messaggi di permesso negato) restano compatti/leggibili invece di ingrandirsi in modo illeggibile
- In attesa di questa ri-verifica, la Fase 4 (velocità massima) non deve iniziare: il layout di questa fase non è ancora approvato

---
*Phase: 03-interfaccia-tachimetro*
*Completed: 2026-07-10 (fix di deviazione da checkpoint; Task 2 checkpoint ancora in attesa di ri-verifica)*
