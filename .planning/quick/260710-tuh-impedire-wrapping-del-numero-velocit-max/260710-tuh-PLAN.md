---
phase: quick
plan: 260710-tuh
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/java/com/sed/tachimetro/MainActivity.kt
autonomous: false
requirements: [QUICK-WRAP-01]

must_haves:
  truths:
    - "In portrait, un numero di velocità a 2 cifre resta su UNA sola riga, grande, rimpicciolito per stare a schermo — mai mandato a capo su due righe"
    - "In landscape il numero della velocità resta su una sola riga"
    - "I messaggi di stato/errore (es. 'Ricerca segnale GPS...') continuano ad andare a capo su più righe come oggi"
  artifacts:
    - path: "app/src/main/java/com/sed/tachimetro/MainActivity.kt"
      provides: "maxLines dinamico impostato nei due helper chokepoint dell'autosize"
      contains: "messageText.maxLines"
  key_links:
    - from: "applySpeedAutosize()"
      to: "messageText.maxLines = 1"
      via: "impostazione maxLines prima/insieme alla config autosize"
      pattern: "messageText\\.maxLines = 1"
    - from: "applyMessageAutosize()"
      to: "messageText.maxLines = Integer.MAX_VALUE"
      via: "ripristino wrapping libero per i messaggi"
      pattern: "messageText\\.maxLines = Integer\\.MAX_VALUE"
---

<objective>
Impedire che il numero della velocità (`messageText`) vada mai a capo su due righe in portrait e landscape: deve rimpicciolirsi per stare su una sola riga invece di wrappare.

Purpose: Core Value del progetto — la velocità deve essere sempre leggibile a colpo d'occhio. Un numero spezzato su due righe è illeggibile e rompe il valore fondamentale dell'app.

Output: `MainActivity.kt` con `maxLines` impostato dinamicamente nei due helper chokepoint già esistenti, così le cifre della velocità restano su una riga e i messaggi di stato/errore continuano a wrappare come oggi.
</objective>

<execution_context>
@$HOME/.claude/get-shit-done/workflows/execute-plan.md
@$HOME/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@app/src/main/java/com/sed/tachimetro/MainActivity.kt
@app/src/main/res/layout/activity_main.xml

<interfaces>
<!-- I due helper chokepoint esistenti in MainActivity.kt che centralizzano la config autosize di messageText. -->
<!-- La stessa TextView (messageText) mostra due tipi di contenuto attraverso questi due percorsi: -->

applySpeedAutosize()  → chiamato SOLO per SpeedState.Reading (mostra le CIFRE, cap 300sp) — le cifre NON devono MAI wrappare, solo rimpicciolirsi.
applyMessageAutosize() → chiamato per Searching/NoSignal/showReady/showDenied (mostra stringhe di STATO/ERRORE, cap 56sp) — devono restare libere di wrappare come oggi.

Codice attuale (MainActivity.kt:306-324):
```kotlin
private fun applySpeedAutosize() {
    TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
        messageText,
        AUTOSIZE_MIN_SP,
        AUTOSIZE_MAX_SPEED_SP,
        AUTOSIZE_STEP_SP,
        TypedValue.COMPLEX_UNIT_SP
    )
}

private fun applyMessageAutosize() {
    TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
        messageText,
        AUTOSIZE_MIN_SP,
        AUTOSIZE_MAX_MESSAGE_SP,
        AUTOSIZE_STEP_SP,
        TypedValue.COMPLEX_UNIT_SP
    )
}
```
</interfaces>

<diagnosis>
`messageText` in `activity_main.xml` ha `app:autoSizeTextType="uniform"` (12–300sp) ma NESSUN `android:maxLines`/`singleLine`. Senza un tetto di righe, l'algoritmo uniform autosize manda le cifre a capo su una 2ª riga (visibile con numeri a 2 cifre in portrait, dove la larghezza è più stretta) invece di rimpicciolirle.

Fix: impostare `maxLines` DINAMICAMENTE dentro i due helper chokepoint già esistenti — NON aggiungere un `maxLines="1"` statico all'XML (romperebbe il wrapping dei messaggi di stato). L'autosize uniforme di Android rispetta `maxLines`: con `maxLines = 1` le cifre si rimpiccioliscono per stare su una riga; con `maxLines = Integer.MAX_VALUE` i messaggi restano liberi di wrappare.
</diagnosis>
</context>

<tasks>

<task type="auto">
  <name>Task 1: Impostare maxLines dinamico nei due helper autosize di messageText</name>
  <files>app/src/main/java/com/sed/tachimetro/MainActivity.kt</files>
  <action>
In `applySpeedAutosize()` aggiungere `messageText.maxLines = 1` (prima della chiamata a `setAutoSizeTextTypeUniformWithConfiguration`), così le cifre della velocità non wrappano mai e l'autosize uniforme le rimpicciolisce per stare su una singola riga.

In `applyMessageAutosize()` aggiungere `messageText.maxLines = Integer.MAX_VALUE` (prima della chiamata a `setAutoSizeTextTypeUniformWithConfiguration`), così i messaggi di stato/errore multi-parola (es. "Ricerca segnale GPS...", `permission_denied_permanent`) restano liberi di andare a capo su più righe esattamente come oggi.

Motivazione del ripristino esplicito in `applyMessageAutosize()`: `messageText` è la STESSA TextView riusata per entrambi i contenuti; senza reimpostare `maxLines = Integer.MAX_VALUE`, un precedente `applySpeedAutosize()` lascerebbe `maxLines = 1` bloccando il wrapping dei messaggi. Impostarlo in entrambi i chokepoint rende ogni percorso self-consistent indipendentemente dall'ordine di transizione degli stati.

NON toccare `activity_main.xml`: nessun `maxLines`/`singleLine` statico su `messageText` (romperebbe il wrapping dei messaggi di stato). NON toccare le costanti di autosize, le altre view, i constraint, o la logica GPS/permessi.
  </action>
  <verify>
    <automated>./gradlew.bat :app:compileDebugKotlin</automated>
  </verify>
  <done>`applySpeedAutosize()` contiene `messageText.maxLines = 1` e `applyMessageAutosize()` contiene `messageText.maxLines = Integer.MAX_VALUE`; il modulo compila senza errori; `activity_main.xml` invariato.</done>
</task>

<task type="checkpoint:human-verify" gate="blocking">
  <what-built>maxLines dinamico impostato nei due helper autosize di `messageText`: `maxLines = 1` per le cifre della velocità (non wrappano mai, si rimpiccioliscono), `maxLines = Integer.MAX_VALUE` per i messaggi di stato/errore (wrappano liberamente come prima).</what-built>
  <how-to-verify>
Installare ed eseguire su emulatore/device: `./gradlew.bat :app:installDebug` (poi avviare l'app), oppure lanciare da Android Studio.

1. **Portrait — numero a 2 cifre (caso principale):** con GPS attivo/mock, portare la velocità a un valore a 2 cifre (es. 88) in orientamento verticale. Il numero deve stare su UNA sola riga, grande, rimpicciolito quanto serve per rientrare in larghezza — MAI spezzato su due righe. Verificare anche un numero a 3 cifre (es. 120): una sola riga.
2. **Messaggio di stato — deve wrappare:** mettere l'app nello stato "Ricerca segnale GPS..." (o negare il permesso per vedere il messaggio di permesso negato). Il messaggio multi-parola deve poter andare a capo su più righe esattamente come prima del fix.
3. **Landscape sanity check:** ruotare in orizzontale con un numero a 2/3 cifre: il numero resta su una sola riga.
  </how-to-verify>
  <resume-signal>Scrivi "approvato" se tutti e tre i casi sono corretti, oppure descrivi cosa non va (es. quale caso wrappa ancora o quale messaggio non wrappa più).</resume-signal>
</task>

</tasks>

<verification>
- `./gradlew.bat :app:compileDebugKotlin` compila senza errori.
- Checkpoint on-device: numero velocità (2 e 3 cifre) su una riga in portrait e landscape; messaggi di stato ancora liberi di wrappare.
</verification>

<success_criteria>
- Il numero della velocità non va mai a capo su due righe: si rimpicciolisce per stare su una singola riga in portrait e landscape.
- I messaggi di stato/errore continuano ad andare a capo su più righe come prima.
- Nessuna modifica a `activity_main.xml`, alle costanti di autosize, o ad altra logica.
</success_criteria>

<output>
After completion, create `.planning/quick/260710-tuh-impedire-wrapping-del-numero-velocit-max/260710-tuh-SUMMARY.md`
</output>
