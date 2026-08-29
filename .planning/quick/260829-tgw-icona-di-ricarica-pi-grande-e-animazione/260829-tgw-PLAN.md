---
phase: quick-260829-tgw
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/res/layout/activity_main.xml
  - app/src/main/java/com/sed/tachimetro/MainActivity.kt
  - .planning/phases/06-indicatore-di-ricarica/06-UI-SPEC.md
autonomous: false
requirements: [CHRG-01, CHRG-02]

must_haves:
  truths:
    - "L'icona fulmine in carica è visibilmente più grande di prima, pur restando nella stessa riga di keepScreenOnSwitch senza alterarne l'altezza né la posizione"
    - "Durante la ricarica il lime sale gradualmente dal basso verso l'alto come prima (movimento morbido, non lineare/robotico)"
    - "Una volta raggiunto il pieno, l'icona torna bianca ISTANTANEAMENTE (nessuno svuotamento graduale visibile) e ricomincia subito a riempirsi"
    - "Il ciclo completo (riempimento + reset istantaneo) dura circa 2,5 secondi, dentro la finestra ~2-3s di CHRG-02"
    - "Lo stato 'batteria piena' resta fermo e completamente lime, senza movimento"
    - "Scollegando il cavo l'icona sparisce immediatamente e un ricollegamento riparte da vuoto/bianco"
    - "Nessun lime compare altrove nella UI e nessuna altra animazione viene introdotta"
  artifacts:
    - path: "app/src/main/res/layout/activity_main.xml"
      provides: "chargingIcon ImageView con dimensioni ingrandite"
      contains: "36dp"
    - path: "app/src/main/java/com/sed/tachimetro/MainActivity.kt"
      provides: "startChargingFillAnimation() con repeat mode RESTART e durata ciclo pieno"
      contains: "CHARGING_FILL_CYCLE_MS"
  key_links:
    - from: "app/src/main/java/com/sed/tachimetro/MainActivity.kt"
      to: "chargingFillLayer (ClipDrawable level 0..10000)"
      via: "ValueAnimator addUpdateListener in startChargingFillAnimation()"
      pattern: "repeatMode = ValueAnimator\\.RESTART"
    - from: "app/src/main/res/layout/activity_main.xml"
      to: "keepScreenOnSwitch"
      via: "chargingIcon vincolata top/bottom allo switch, switch start_toEndOf icona"
      pattern: "layout_constraintStart_toEndOf=\"@id/chargingIcon\""
---

<objective>
Applicare le 2 rifiniture estetiche richieste dall'utente durante il checkpoint reale della Fase 6 (registrate testualmente in `06-04-SUMMARY.md`):

1. **Icona fulmine più grande** — da 24dp a 36dp.
2. **Animazione con svuotamento istantaneo** — il lime sale gradualmente dal basso (come ora), ma una volta pieno si azzera **di colpo** e ricomincia a salire: `ValueAnimator.RESTART` al posto di `REVERSE`, con la durata dell'intero ciclo assegnata alla sola fase di riempimento.

Purpose: chiudere i 2 follow-up approvati dall'utente sul dispositivo reale, senza riaprire nulla del comportamento già verificato PASS in Fase 6.
Output: layout con icona 36dp, animatore in modalità RESTART a 2500ms, nota di revisione in `06-UI-SPEC.md`, verifica umana su dispositivo.

**Fuori scope (NON toccare):** `ChargingStateProvider`, `ChargingState`, `charging_flash_fill.xml`, `ic_charging_flash*.xml`, `colors.xml`, la logica di `updateChargingIcon()`, `freezeChargingFillAtFull()`, `stopChargingFillAnimation()`, `resolveChargingFillLayer()`, la gestione window insets in `applyBottomLeftWindowInsets()`, la posizione dell'icona (a sinistra dello switch, stessa riga), il colore lime, il vincolo "nessun altro colore/animazione altrove".
</objective>

<execution_context>
@$HOME/.claude/get-shit-done/workflows/execute-plan.md
@$HOME/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@./CLAUDE.md
@app/src/main/res/layout/activity_main.xml
@app/src/main/java/com/sed/tachimetro/MainActivity.kt
@app/src/main/res/drawable/charging_flash_fill.xml
@.planning/phases/06-indicatore-di-ricarica/06-04-SUMMARY.md
@.planning/phases/06-indicatore-di-ricarica/06-UI-SPEC.md

<interfaces>
<!-- Contratti già esistenti che l'esecutore deve usare così come sono: nessuna esplorazione del codebase necessaria. -->

Da `MainActivity.kt` (companion object, stato attuale da modificare):
- `private const val CHARGING_FILL_LEVEL_MAX = 10_000` — livello ClipDrawable pieno (NON cambiare)
- `private const val CHARGING_FILL_HALF_CYCLE_MS = 1250L` — durata mezza fase, da sostituire

Da `MainActivity.kt` (campi/helper esistenti, firme invariate):
- `private var chargingFillLayer: ClipDrawable?` — livello 0..10000
- `private var chargingFillAnimator: ValueAnimator?`
- `private fun startChargingFillAnimation()` — UNICA funzione da modificare
- `private fun freezeChargingFillAtFull()` — invariata (stato batteria piena)
- `private fun stopChargingFillAnimation()` — invariata (unplug/onStop)
- `private fun updateChargingIcon(state: ChargingState)` — invariata

Da `activity_main.xml`, blocco `chargingIcon` (stato attuale):
```
android:layout_width="24dp"
android:layout_height="24dp"
android:layout_marginStart="16dp"
app:layout_constraintStart_toStartOf="parent"
app:layout_constraintTop_toTopOf="@id/keepScreenOnSwitch"
app:layout_constraintBottom_toBottomOf="@id/keepScreenOnSwitch"
```
`keepScreenOnSwitch` ha `android:minHeight="48dp"` e `app:layout_constraintStart_toEndOf="@id/chargingIcon"` con `layout_marginStart="8dp"`: 36dp resta sotto i 48dp della riga, quindi l'altezza della riga e gli insets non cambiano.
</interfaces>
</context>

<tasks>

<task type="auto">
  <name>Task 1: Ingrandire l'icona di ricarica da 24dp a 36dp</name>
  <files>app/src/main/res/layout/activity_main.xml</files>
  <action>
Nel blocco `ImageView` con `android:id="@+id/chargingIcon"`, cambiare `android:layout_width` e `android:layout_height` da `24dp` a `36dp`.

Nient'altro in quel blocco va toccato: `layout_marginStart="16dp"`, `src`, `contentDescription`, `clickable="false"`, `focusable="false"`, `visibility="gone"` e i tre vincoli (`constraintStart_toStartOf="parent"`, `constraintTop_toTopOf`/`constraintBottom_toBottomOf` su `@id/keepScreenOnSwitch`) restano identici — sono ciò che tiene l'icona sulla stessa riga dello switch e fa funzionare `applyBottomLeftWindowInsets()`, che legge `marginStart` a runtime.

Non toccare il blocco `keepScreenOnSwitch`: il suo `minHeight="48dp"` continua a definire l'altezza della riga e 36dp ci sta dentro, quindi la posizione verticale del gruppo bottom-left non cambia.

Motivazione del valore: 36dp è +50% rispetto ai 24dp attuali (chiaramente più prominente a colpo d'occhio, in linea con il core value "leggibilità istantanea"), multiplo di 4 coerente con la spacing scale del progetto, e resta sotto i 48dp dello switch così la riga non si allarga. La dimensione esatta è comunque soggetta all'approvazione umana del Task 3.
  </action>
  <verify>
    <automated>cd "C:/Users/fedes/AndroidStudioProjects/Tachimetro" && grep -A2 'android:id="@+id/chargingIcon"' app/src/main/res/layout/activity_main.xml | grep -c '36dp'</automated>
    <automated>cd "C:/Users/fedes/AndroidStudioProjects/Tachimetro" && ./gradlew.bat :app:assembleDebug</automated>
  </verify>
  <done>Il primo grep stampa `2` (width + height a 36dp), la build debug termina con BUILD SUCCESSFUL, e `keepScreenOnSwitch` è invariato in `git diff`.</done>
</task>

<task type="auto">
  <name>Task 2: Riempimento graduale con svuotamento istantaneo (RESTART invece di REVERSE)</name>
  <files>app/src/main/java/com/sed/tachimetro/MainActivity.kt, .planning/phases/06-indicatore-di-ricarica/06-UI-SPEC.md</files>
  <action>
In `MainActivity.kt`:

1. Nel `companion object`, sostituire `CHARGING_FILL_HALF_CYCLE_MS = 1250L` con `private const val CHARGING_FILL_CYCLE_MS = 2500L`. Riscrivere il commento sopra la costante: adesso non esistono più due mezze fasi da 1250ms; con lo svuotamento istantaneo l'intero budget di ~2,5s (dentro la finestra "~2-3 secondi" di CHRG-02) è assegnato alla sola salita 0 -> `CHARGING_FILL_LEVEL_MAX`, e il ritorno a 0 avviene nel frame successivo senza durata.

2. In `startChargingFillAnimation()`: `duration = CHARGING_FILL_CYCLE_MS` e `repeatMode = ValueAnimator.RESTART`. Lasciare invariati `ValueAnimator.ofInt(0, CHARGING_FILL_LEVEL_MAX)`, `repeatCount = ValueAnimator.INFINITE`, `interpolator = AccelerateDecelerateInterpolator()` (il movimento morbido è il punto 3 già approvato PASS dall'utente, non va reso lineare), l'`addUpdateListener`, il `cancel()`/reset a level 0 iniziale e `start()`.

3. Aggiornare il commento KDoc/inline sopra `startChargingFillAnimation()` per descrivere il nuovo comportamento: salita graduale, poi azzeramento istantaneo a ogni iterazione (`RESTART` riparte da `ofInt`'s start value 0 nel frame successivo, quindi lo svuotamento non è animato — è esattamente il "si svuota di colpo" richiesto dall'utente in `06-04-SUMMARY.md`). Nel commento riferirsi alla vecchia modalità come "modalità REVERSE" in prosa, **senza** scrivere la forma di codice `ValueAnimator.REVERSE` (il gate automatico verifica che quella stringa non esista più nel file).

4. NON toccare `freezeChargingFillAtFull()` (batteria piena resta ferma e solid lime), `stopChargingFillAnimation()`, `updateChargingIcon()`, `resolveChargingFillLayer()`, `onStop()`, né alcun altro metodo.

In `06-UI-SPEC.md`, aggiungere in fondo (dopo "Checker Sign-Off") una sezione breve `## Revisions (post-checkpoint, quick task 260829-tgw)` che registra i due valori superati, così lo spec non resta in contraddizione con il codice:
- Spacing scale / Layout: dimensione icona `24dp` -> `36dp`
- Animation spec: ciclo `REVERSE` 1250+1250ms -> riempimento `RESTART` 2500ms con svuotamento istantaneo (non animato)
Indicare che le altre righe dello spec (colore lime, direzione basso->alto, posizione, stati Hidden/Pulsing/Full, transizioni istantanee, window insets) restano invariate e approvate.
  </action>
  <verify>
    <automated>cd "C:/Users/fedes/AndroidStudioProjects/Tachimetro" && grep -c 'repeatMode = ValueAnimator.RESTART' app/src/main/java/com/sed/tachimetro/MainActivity.kt</automated>
    <automated>cd "C:/Users/fedes/AndroidStudioProjects/Tachimetro" && grep -c 'ValueAnimator.REVERSE\|CHARGING_FILL_HALF_CYCLE_MS' app/src/main/java/com/sed/tachimetro/MainActivity.kt || true</automated>
    <automated>cd "C:/Users/fedes/AndroidStudioProjects/Tachimetro" && grep -c 'duration = CHARGING_FILL_CYCLE_MS' app/src/main/java/com/sed/tachimetro/MainActivity.kt</automated>
    <automated>cd "C:/Users/fedes/AndroidStudioProjects/Tachimetro" && grep -c 'freezeChargingFillAtFull\|stopChargingFillAnimation' app/src/main/java/com/sed/tachimetro/MainActivity.kt</automated>
    <automated>cd "C:/Users/fedes/AndroidStudioProjects/Tachimetro" && ./gradlew.bat :app:testDebugUnitTest :app:assembleDebug</automated>
  </verify>
  <done>Gate 1 = 1, Gate 2 = 0 (nessun residuo di REVERSE né della vecchia costante, nemmeno nei commenti), Gate 3 = 1, Gate 4 >= 4 (helper freeze/stop ancora dichiarati e richiamati), unit test verdi e BUILD SUCCESSFUL. `git diff` mostra modifiche confinate al companion object, a `startChargingFillAnimation()` e ai relativi commenti.</done>
</task>

<task type="checkpoint:human-verify" gate="blocking">
  <what-built>
Le 2 rifiniture richieste al checkpoint della Fase 6:
- `chargingIcon` ingrandita da 24dp a 36dp (stessa posizione: a sinistra di "Sempre acceso", stessa riga)
- animazione di riempimento passata da simmetrica (`REVERSE`, riempi 1250ms + svuota 1250ms) a riempimento graduale di 2500ms con **azzeramento istantaneo** al pieno (`RESTART`), interpolatore morbido invariato

Non è stato toccato nulla della logica di ricarica (`ChargingStateProvider`), del colore lime, dei drawable, degli window insets, né dello stato "batteria piena".
  </what-built>
  <how-to-verify>
Se nessun dispositivo risulta collegato via `adb devices`, installa manualmente `app/build/outputs/apk/debug/app-debug.apk` sul telefono (stesso flusso già usato in 06-04).

1. **Dimensione** — collega il caricabatterie: il fulmine a sinistra di "Sempre acceso" è chiaramente più grande di prima e ben leggibile a colpo d'occhio. Se preferisci un'altra misura (es. 32dp o 40dp), dillo e la cambio.
2. **Riempimento** — il lime sale dal basso verso l'alto in modo graduale e morbido, come prima.
3. **Svuotamento istantaneo** — arrivato in cima, il fulmine torna bianco **di colpo**, senza scendere gradualmente, e riparte subito a riempirsi. Questa è la modifica chiave richiesta.
4. **Durata** — un ciclo completo (bianco -> pieno lime -> scatto a bianco) dura circa 2,5 secondi.
5. **Layout intatto** — lo switch "Sempre acceso" è alla stessa altezza di prima, non spostato né tagliato; icona e switch restano interamente visibili sia in verticale che in orizzontale (ruota il telefono).
6. **Batteria piena** (se raggiungibile) — a carica completa l'icona resta ferma e tutta lime, senza movimento.
7. **Scollegamento** — staccando il cavo l'icona sparisce all'istante, anche a metà riempimento; ricollegando riparte da vuoto/bianco.
8. **Nessun lime altrove** — il lime resta solo dentro il fulmine; nessuna altra animazione compare nella UI.
  </how-to-verify>
  <resume-signal>Scrivi "approvato" oppure indica cosa correggere (es. una dimensione icona diversa, o una durata di riempimento diversa)</resume-signal>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| *(nessuno nuovo)* | Il task modifica solo due valori di presentazione (dimensione di una `ImageView` e parametri di un `ValueAnimator`). Nessun input esterno, nessuna nuova permission, nessuna I/O, nessuna dipendenza aggiunta. Il confine sistema->app già esistente (`ACTION_BATTERY_CHANGED` sticky broadcast) resta invariato in `ChargingStateProvider`, fuori scope. |

## STRIDE Threat Register

| Threat ID | Category | Component | Disposition | Mitigation Plan |
|-----------|----------|-----------|-------------|-----------------|
| T-tgw-01 | Denial of Service | `startChargingFillAnimation()` — `ValueAnimator` INFINITE che gira in background consumando batteria | mitigate | Nessuna modifica al ciclo di vita: `onStop()` continua a chiamare `stopChargingFillAnimation()` e `updateChargingIcon(Hidden)` a chiamare `cancel()`. Il gate automatico del Task 2 verifica che entrambi gli helper siano ancora presenti e richiamati. |
| T-tgw-02 | Tampering | Dipendenze di terze parti | accept | Nessuna installazione npm/pip/cargo/Gradle in questo task: `libs.versions.toml` e i file build non vengono toccati. Nessun gate di legittimità pacchetti richiesto. |
</threat_model>

<verification>
1. `./gradlew.bat :app:testDebugUnitTest` — verde (la logica pura di GPS/max/charging è invariata e non deve regredire)
2. `./gradlew.bat :app:assembleDebug` — BUILD SUCCESSFUL
3. `git diff --stat` — solo `activity_main.xml`, `MainActivity.kt` e `06-UI-SPEC.md` modificati; nessun file sotto `app/src/main/java/com/sed/tachimetro/charging/` o `app/src/main/res/drawable/` toccato
4. Checkpoint umano su dispositivo reale approvato (unica fonte di verità per il comportamento visivo, coerentemente con l'assenza di test strumentati nel progetto)
</verification>

<success_criteria>
- `chargingIcon` è 36dp × 36dp, nella stessa posizione e con gli stessi vincoli/insets di prima
- `startChargingFillAnimation()` usa `repeatMode = ValueAnimator.RESTART` con `duration = CHARGING_FILL_CYCLE_MS` (2500L)
- Nessuna occorrenza residua di `ValueAnimator.REVERSE` o `CHARGING_FILL_HALF_CYCLE_MS` in `MainActivity.kt`
- `freezeChargingFillAtFull()`, `stopChargingFillAnimation()`, `updateChargingIcon()`, `ChargingStateProvider`, drawable e colori invariati
- `06-UI-SPEC.md` riporta la nota di revisione dei due valori superati
- L'utente ha approvato il risultato sul proprio dispositivo reale
</success_criteria>

<output>
Create `.planning/quick/260829-tgw-icona-di-ricarica-pi-grande-e-animazione/260829-tgw-SUMMARY.md` when done.

Nel SUMMARY registrare: i valori finali approvati (dimensione icona e durata ciclo, se l'utente ne ha chiesti di diversi in checkpoint), il fatto che i due follow-up di `06-04-SUMMARY.md` sono chiusi, e l'esito punto per punto della verifica su dispositivo.
</output>
