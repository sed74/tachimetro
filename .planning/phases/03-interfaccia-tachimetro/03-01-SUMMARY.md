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
    - "Vista secondaria dedicata (unitText), dimensione fissa, ancorata top-end del ConstraintLayout, per l'etichetta 'km/h' — sostituisce il precedente approccio SpannableString/RelativeSizeSpan"

key-files:
  created: []
  modified:
    - app/src/main/res/layout/activity_main.xml
    - app/src/main/java/com/sed/tachimetro/MainActivity.kt
    - app/src/main/res/values/strings.xml

# Round 3 (this update) modified only app/src/main/java/com/sed/tachimetro/MainActivity.kt
# (added applyUnitTextWindowInsets()) — no layout/strings changes this round.

key-decisions:
  - "Autosize a due range: 12-300sp/4sp per il numero di velocità (dominante), 12-56sp/4sp per i messaggi di stato (compatti/leggibili) — stesso TextView, configurazione autosize applicata a runtime prima di ogni text assignment"
  - "Round 2 (feedback utente): l'unità 'km/h' non è più uno span dentro messageText, ma una TextView separata (unitText), dimensione fissa 22sp, ancorata top-end del ConstraintLayout, visibile solo durante SpeedState.Reading"
  - "Round 3 (feedback utente): unitText ancorata top-end era invisibile perché disegnava dietro la status bar (edge-to-edge forzato da targetSdk 36) — aggiunto un OnApplyWindowInsetsListener dedicato che somma l'inset live di systemBars/displayCutout al margine XML base, senza introdurre alcuna nuova dipendenza"

patterns-established:
  - "Pattern 1: quando un unico TextView autosize deve rappresentare contenuti di natura diversa (numero dominante vs testo di stato), commutare min/max/step via TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration() prima di ogni assegnazione di testo, invece di un unico range condiviso"
  - "Pattern 2 (superato in round 2, vedi Deviazione 3 sotto): un'etichetta secondaria fissa (es. unità di misura) va in una vista dedicata separata, ancorata via vincoli ConstraintLayout a un angolo dello schermo, non in uno span dentro il TextView dominante — più semplice da posizionare in modo indipendente in portrait/landscape senza res/layout-land"
  - "Pattern 3 (round 3): con targetSdk >= 35 (edge-to-edge forzato, nessun opt-out), qualunque vista ancorata a un angolo/bordo dello schermo con solo margini XML fissi va protetta con ViewCompat.setOnApplyWindowInsetsListener che somma l'inset live (systemBars/displayCutout) al margine base — altrimenti rischia di disegnare dietro la status bar/nav bar/cutout ed essere invisibile"

requirements-completed: [UI-01, UI-02, UI-03, UI-04, UI-05]

# Metrics
duration: n/a (Task 1 storico + tre round di fix di deviazione da checkpoint, chiuso con approvazione utente)
completed: 2026-07-10
---

# Phase 3 Plan 1: Interfaccia dominante con autosize a due range Summary

**TextView messageText a schermo intero con autosize uniform per il numero di velocità (solo cifre, 12-300sp), unitText separato (22sp fisso, ancorato top-end) per l'etichetta "km/h", e cap autosize separato (12-56sp) per i messaggi di stato così restano compatti e leggibili invece di ingrandirsi come il numero.**

## Performance

- **Tasks:** 2 di 2 completati (Task 1 auto; Task 2 checkpoint umano approvato dall'utente con "approvato" dopo tre round di fix da feedback)
- **Files modified:** 3 (`activity_main.xml`, `MainActivity.kt`, `strings.xml`)

## Accomplishments
- `messageText` occupa l'intero spazio disponibile (box `0dp`/`0dp`), centrato, bianco Black/900 su sfondo nero, con autosize uniform e nessun `res/layout-land/` separato (D-01/D-02 rispettati)
- Il numero di velocità (es. "180") è ora l'unico contenuto di `messageText` durante `SpeedState.Reading` — nessuna unità di misura al suo interno, così resta l'elemento centrato e dominante richiesto dal feedback utente round 2
- Nuova `TextView unitText` dedicata, dimensione fissa (22sp, non-autosize), ancorata all'angolo top-end del `ConstraintLayout` (16dp di margine), visibile solo mentre è mostrata una lettura di velocità numerica, nascosta (`GONE`) durante "Pronto"/ricerca segnale/permesso negato — funziona identicamente in portrait e landscape tramite lo stesso layout adattivo unico
- I messaggi di stato ("Pronto", "Ricerca segnale GPS...", i due messaggi di permesso negato) continuano a usare il cap autosize più basso (max 56sp invece di 300sp) introdotto nel fix precedente, comportamento non modificato in questo round

## Task Commits

Storico esecuzione di questo plan (branch merged + fix correnti):

1. **Task 1: Applicare auto-size e stile dominante a messageText in activity_main.xml** - `8ecfb4e` (feat) — merged in `e7bbe89`
2. **Fix deviazione round 1 (Task 2 pending): unità km/h più piccola + cap autosize distinto per i messaggi di stato** - `fa7e3ef` (fix)
3. **Fix deviazione round 2 (Task 2 pending): unitText separato top-right per l'unità km/h, messageText solo cifre** - `1265bfc` (fix)
4. **Fix deviazione round 3 (Task 2 pending): unitText nascosta dietro la status bar (edge-to-edge targetSdk 36)** - `914ce66` (fix)

_Task 2 (checkpoint:human-verify) non è ancora marcato "done": richiede una nuova verifica visiva su device/emulatore dopo questo terzo fix, per confermare che "km/h" sia ora visibile, piccola, in alto a destra e completamente libera dalla status bar, in entrambi gli orientamenti._

## Files Created/Modified
- `app/src/main/res/layout/activity_main.xml` - messageText a schermo intero, autosize uniform 12/300/4sp, box 0dp/0dp, centrato, Black/900, margini 16dp; nuova `unitText` (22sp fisso, top-end, `visibility="gone"` di default); retryButton invariato
- `app/src/main/java/com/sed/tachimetro/MainActivity.kt` - rimossi `buildSpeedText()`/`SpannableString`/`RelativeSizeSpan`; `messageText.text` ora `state.kmh.toString()` (solo cifre) durante `SpeedState.Reading`; `unitText.visibility` commutata a `VISIBLE` solo in `Reading`, `GONE` altrove (`showReady()`, `showDenied()`, `Searching`/`NoSignal`); `applySpeedAutosize()`/`applyMessageAutosize()` invariate; **round 3:** aggiunto `applyUnitTextWindowInsets()`, chiamato una volta in `onCreate()` subito dopo il `findViewById` di `unitText`, che installa un `ViewCompat.setOnApplyWindowInsetsListener` per sommare l'inset live di `systemBars`/`displayCutout` al margine XML base (top/end) di `unitText`
- `app/src/main/res/values/strings.xml` - aggiunta `unit_kmh` ("km/h") per la nuova `unitText`; `speed_kmh_format` lasciata invariata nel file (non più referenziata dal codice, ma non rimossa per non alterare risorse fuori scope di questo fix)

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

**3. [Rule 4 - Architectural, richiesta esplicita utente] Unità "km/h" spostata da span dentro messageText a TextView dedicata top-right**
- **Found during:** Secondo giro di feedback utente al checkpoint Task 2, dopo aver ri-testato il fix della Deviazione 1 (RelativeSizeSpan) su device reale
- **Issue:** L'utente ha rigettato esplicitamente l'approccio "unità più piccola come span dentro lo stesso TextView del numero" e ha richiesto, con parole proprie: "voglio il numero grande e centrato, mentre l'unità di misura deve stare in un riquadro di testo a parte, piccolo, in alto a destra, sia in verticale che in orizzontale". Questa è una richiesta di ristrutturazione visiva (nuova vista nel layout), non un bug — trattata secondo Regola 4 (cambio architetturale), ma la direzione era già esplicita e univoca da parte dell'utente, quindi applicata direttamente in questo giro di lavoro anziché fermarsi a un ulteriore checkpoint di decisione.
- **Fix:** Aggiunta una seconda `TextView` (`unitText`) in `activity_main.xml`: dimensione fissa 22sp (non autosize), ancorata a `layout_constraintEnd_toEndOf="parent"` + `layout_constraintTop_toTopOf="parent"` con margine 16dp, `visibility="gone"` di default — nessun `res/layout-land/` aggiunto, i vincoli ConstraintLayout la mantengono in alto a destra sia in portrait sia in landscape (D-02 preservato). In `MainActivity.kt`: rimossi `buildSpeedText()`/`SpannableString`/`RelativeSizeSpan`/`UNIT_RELATIVE_SIZE`; `messageText.text = state.kmh.toString()` (solo cifre) durante `SpeedState.Reading`, con `unitText.visibility = View.VISIBLE` impostata nello stesso branch; tutti gli altri stati (`showReady()`, `showDenied()`, `Searching`/`NoSignal`) impostano `unitText.visibility = View.GONE`. Nuova stringa `unit_kmh` ("km/h") aggiunta a `strings.xml` per popolare `unitText` senza hardcodare testo.
- **Files modified:** `app/src/main/res/layout/activity_main.xml`, `app/src/main/java/com/sed/tachimetro/MainActivity.kt`, `app/src/main/res/values/strings.xml`
- **Verification:** `./gradlew.bat :app:assembleDebug` → BUILD SUCCESSFUL; revisione del codice conferma che `unitText.visibility` è impostata in ogni ramo di stato (Reading → VISIBLE, tutti gli altri → GONE) e che `messageText` non contiene più span o suffissi di unità
- **Committed in:** `1265bfc` (fix commit)

**4. [Rule 1 - Bug] unitText invisibile: disegnava dietro la status bar (edge-to-edge forzato da targetSdk 36)**
- **Found during:** Terzo giro di feedback utente al checkpoint Task 2, dopo aver ri-testato il fix della Deviazione 3 (unitText separata top-right) su emulatore reale
- **Issue:** Con una lettura di velocità numerica attiva (es. "0"), l'etichetta "km/h" non compariva affatto, mentre tutto il resto (numero dominante centrato, comportamento hidden/visible sui messaggi di stato) funzionava correttamente. Root cause verificata: `app/build.gradle.kts` dichiara `targetSdk = 36` (Android 16); da Android 15 (API 35) in poi il rendering edge-to-edge è forzato di default per le app che puntano a SDK 35+, senza possibilità di opt-out per SDK 36. Nessun file del progetto (`AndroidManifest.xml`, `values/themes.xml`, `values-night/themes.xml`, `MainActivity.kt`) chiamava `WindowCompat`, `enableEdgeToEdge`, `fitsSystemWindows` o `OnApplyWindowInsetsListener` — confermato per lettura diretta di tutti e quattro i file. `unitText` era ancorata all'angolo top-end del `ConstraintLayout` con solo un margine fisso `layout_marginTop="16dp"`/`layout_marginEnd="16dp"`, quindi finiva a disegnare direttamente sotto/dietro la status bar di sistema (orologio/batteria/segnale), risultando invisibile. Non era un bug di logica di visibilità: `unitText.visibility = View.VISIBLE` in `SpeedState.Reading` e il binding testo a `@string/unit_kmh` erano già corretti nel codice.
- **Fix:** Aggiunto `applyUnitTextWindowInsets()` in `MainActivity.kt`, chiamato una sola volta in `onCreate()` subito dopo il `findViewById` di `unitText`. Installa un `ViewCompat.setOnApplyWindowInsetsListener` su `unitText` che legge `WindowInsetsCompat.Type.systemBars()` e `.displayCutout()`, calcola l'inset extra massimo (top/end) tra i due, e lo somma al margine XML base (catturato una volta come `baseTopMargin`/`baseEndMargin` dal `ConstraintLayout.LayoutParams` originale) per ottenere il margine finale — mutando `topMargin`/`marginEnd` sull'oggetto `LayoutParams` direttamente (nessuna estensione core-ktx come `updateLayoutParams`/`updatePadding`, dato che `androidx.core:core-ktx` non è una dipendenza dichiarata nel progetto: confermato assente sia in `gradle/libs.versions.toml` sia in `app/build.gradle.kts`). `androidx.core.view.ViewCompat`/`WindowInsetsCompat` e `androidx.constraintlayout.widget.ConstraintLayout` sono già disponibili transitivamente via `androidx.appcompat:appcompat`/`androidx.constraintlayout:constraintlayout` (già dipendenze esistenti) — **nessuna nuova dipendenza Gradle aggiunta**. `messageText` e `retryButton` non sono stati toccati in alcun modo.
- **Files modified:** `app/src/main/java/com/sed/tachimetro/MainActivity.kt`
- **Verification:** `./gradlew.bat :app:assembleDebug` → BUILD SUCCESSFUL; revisione del codice conferma che il listener è registrato una sola volta in `onCreate()`, non introduce dipendenze core-ktx, e non modifica constraint/visibility di `messageText`/`retryButton`
- **Committed in:** `914ce66` (fix commit)

---

**Total deviations:** 4 auto-fixed/applicate (2x Rule 1 - bug di resa visiva round 1; 1x Rule 4 - cambio architetturale con direzione esplicita e univoca dell'utente, round 2; 1x Rule 1 - bug di rendering edge-to-edge, round 3)
**Impact on plan:** D-01/D-02/D-03 restano intatti nella sostanza (un solo layout adattivo, nessun `res/layout-land/`, `messageText` resta il TextView condiviso per numero e messaggi di stato). L'unica estensione strutturale è l'aggiunta di una seconda vista di testo dedicata (`unitText`), piccola e statica, per l'unità di misura — esplicitamente richiesta dall'utente al posto dell'approccio a span precedente. Il fix round 3 è puramente correttivo (gestione window insets su una vista già esistente), nessun impatto strutturale aggiuntivo.

## Issues Encountered
- Il file `local.properties` (SDK path locale, gitignored) non era presente in questo worktree; ricreato localmente con lo stesso `sdk.dir` del repo principale solo per poter eseguire `./gradlew.bat :app:assembleDebug` — non committato (resta escluso da `.gitignore`).

## User Setup Required
None - nessuna configurazione di servizi esterni richiesta.

## Next Phase Readiness
- Task 2 approvato dall'utente ("approvato") dopo il terzo round di fix: la Fase 4 (velocità massima) può iniziare quando pianificata, il layout di questa fase è definitivamente chiuso.

---

## Chiusura Task 2 (checkpoint approvato)

Dopo tre round di fix da feedback utente su device/emulatore reale (round 1: unità km/h ridimensionata via span e cap autosize distinto per i messaggi di stato; round 2: unità km/h spostata da span dentro `messageText` a `unitText` dedicata, ancorata in alto a destra; round 3: fix window-insets per far sì che `unitText` non disegnasse più dietro la status bar con edge-to-edge forzato da targetSdk 36), l'utente ha ri-verificato il risultato finale su portrait/landscape con velocità a 1/2/3 cifre e stati di messaggio, e ha confermato esplicitamente con la parola **"approvato"**.

Task 2 (checkpoint:human-verify) è quindi marcato **done**. Il plan 03-01 è **completo al 100%** (Task 1 + Task 2, entrambi done). Nessuna ulteriore modifica di codice è stata necessaria in questa chiusura: il self-check finale (`./gradlew.bat :app:assembleDebug` e `./gradlew.bat test`) conferma BUILD SUCCESSFUL su entrambi, a conferma che l'albero è pulito e buildabile dopo i tre round di deviazione.

---
*Phase: 03-interfaccia-tachimetro*
*Completed: 2026-07-10 (Task 2 approvato dall'utente dopo tre round di fix da checkpoint; plan 03-01 completo)*
