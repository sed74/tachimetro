---
phase: quick-260929-cyv
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/java/com/sed/tachimetro/VersionLabel.kt
  - app/src/test/java/com/sed/tachimetro/VersionLabelTest.kt
  - app/src/main/res/layout/activity_main.xml
  - app/src/main/res/values/colors.xml
  - app/src/main/java/com/sed/tachimetro/MainActivity.kt
  - app/src/main/java/com/sed/tachimetro/car/SpeedSurfaceRenderer.kt
  - app/build.gradle.kts
  - playstore/release_notes/release_notes_v2.1-beta.txt
  - playstore/README.md
autonomous: true
requirements: [QUICK-260929-cyv]

must_haves:
  truths:
    - "Sul telefono, in alto al centro, compare in piccolo e grigio 'v2.1-beta (6)', sempre visibile, fuori da status bar/cutout, senza coprire velocita', MAX, km/h, distanza, icona ricarica, toggle o pulsanti"
    - "Sulla Surface di Android Auto compare in piccolo e grigio 'v2.1-beta (6)' nell'angolo in basso a destra della stable area, anche nelle build release"
    - "L'etichetta e' prodotta da una sola funzione pura formatVersionLabel(name, code) coperta da unit test"
    - "versionCode e' 6 (versionName resta 2.1-beta) e playstore/ riporta versionCode 6 e cita la versione visibile nelle note"
  artifacts:
    - path: "app/src/main/java/com/sed/tachimetro/VersionLabel.kt"
      provides: "funzione pura formatVersionLabel"
      contains: "fun formatVersionLabel"
    - path: "app/src/test/java/com/sed/tachimetro/VersionLabelTest.kt"
      provides: "unit test JVM di formatVersionLabel"
    - path: "app/src/main/res/layout/activity_main.xml"
      provides: "TextView versionText in alto al centro"
      contains: "@+id/versionText"
  key_links:
    - from: "MainActivity.kt"
      to: "formatVersionLabel(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)"
      via: "versionText.text in onCreate/setup"
      pattern: "formatVersionLabel\\(BuildConfig\\.VERSION_NAME"
    - from: "SpeedSurfaceRenderer.kt"
      to: "formatVersionLabel"
      via: "drawText fuori dal blocco if (BuildConfig.DEBUG)"
      pattern: "formatVersionLabel\\(BuildConfig\\.VERSION_NAME"
---

<objective>
Rendere visibile, in modo discreto, la versione dell'app ("v<versionName> (<versionCode>)") sia
sul telefono sia sulla Surface di Android Auto, cosi' l'utente in auto capisce a colpo d'occhio
quale build sta girando (ieri girava la vecchia 2.0 senza che se ne accorgesse). Bump del
versionCode 5 -> 6 e riallineamento di playstore/.

Output: VersionLabel.kt + test, etichetta sul telefono (layout + MainActivity con insets),
etichetta sulla Surface (SpeedSurfaceRenderer, anche in release), versionCode 6, playstore/
aggiornato.
</objective>

<execution_context>
@$HOME/.claude/get-shit-done/workflows/execute-plan.md
@$HOME/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@./CLAUDE.md
@.planning/STATE.md
@.planning/quick/260924-n9g-build-test-chiuso-play-store-spikepoi-co/260924-n9g-SUMMARY.md
@app/src/main/res/layout/activity_main.xml
@app/src/main/java/com/sed/tachimetro/car/SpeedSurfaceRenderer.kt

Vincoli di esecuzione:
- Esecuzione SEQUENZIALE sul main tree, niente worktree (su Windows falliscono per "Filename too long").
- Commenti KDoc/inline e testi in italiano, come il resto del codice.
- Gradle da Git Bash: `cd /c/Users/fedes/AndroidStudioProjects/Tachimetro && ./gradlew.bat ...`
- NON toccare keystore.properties e NON tentare build release firmate (l'utente firma col wizard di Android Studio).

<interfaces>
Stato attuale rilevante (estratto dal codice):

- `app/build.gradle.kts`: `versionCode = 5`, `versionName = "2.1-beta"`, `buildConfig = true` gia'
  attivo; commento sui flavor (riga ~65) dice "(versionCode 5, 2.1-beta)". Flavor: spikePoi, spikeNav.
- `MainActivity.kt`: importa gia' `BuildConfig` (usato a riga ~299) e `ViewCompat`,
  `WindowInsetsCompat`, `ConstraintLayout`. Pattern insets da copiare, `applyUnitTextWindowInsets()`
  (righe ~738-753): legge `baseTopMargin` dai LayoutParams XML, poi in
  `ViewCompat.setOnApplyWindowInsetsListener(view)` somma `maxOf(systemBars.top, cutout.top)` al
  margine base e restituisce `insets`. Le view sono assegnate con `findViewById` in funzioni di
  setup chiamate da onCreate (es. righe ~160-166 per messageText/unitText/retryButton).
- `activity_main.xml` (ConstraintLayout, sfondo nero). Occupazione angoli: alto-sx maxSpeedText +
  resetMaxButton; alto-dx unitText ("km/h"); basso-sx chargingIcon + keepScreenOnSwitch; basso-dx
  distanceText + distanceUnitText. messageText (velocita'/messaggi) e' 0dp x 0dp, dal top del parent
  al top di retryButton, con autosize uniform: le cifre sono centrate verticalmente, quindi la
  fascia alta centrale e' libera. L'alto-centro e' l'unico punto libero da elementi esistenti.
- `colors.xml`: contiene black, white, lime_charging_accent (+ purple/teal di template).
- `SpeedSurfaceRenderer.kt`: `drawContent(canvas)` disegna 1) sfondo nero, 2) `area =
  effectiveArea(stableArea, surfaceWidth, surfaceHeight)`, 3) cifre centrate con
  `fitTextSizePx(..., fillFraction = 0.9f)` (quindi resta libero un bordo del 5% per lato, e sugli
  schermi auto larghi le cifre sono limitate in altezza e i lati restano liberi), 4-5) overlay di
  debug (contorni + righe `api=...` in ALTO A SINISTRA della visible area) SOLO dentro
  `if (BuildConfig.DEBUG)`, 6) log frame in debug. Costanti companion: `INFO_TEXT_MIN_PX = 16f`,
  `INFO_TEXT_FRACTION = 0.04f`, `OUTLINE_STROKE_PX = 4f`. Classe `AreaPx(left, top, right, bottom)`
  con `width`, `height`, `isEmpty` in `car/SurfaceTextFit.kt`.
- Test JVM di riferimento: `app/src/test/java/com/sed/tachimetro/car/SurfaceSpeedTextTest.kt`
  (JUnit4, `org.junit.Assert.assertEquals`, nomi test snake_case, KDoc italiano sulla classe).
- `playstore/release_notes/release_notes_v2.1-beta.txt`: file unico bilingue con blocchi
  `<it-IT>...</it-IT>` e `<en-US>...</en-US>`, intestazione "Novita' della versione 2.1-beta (test
  chiuso):" + 3 bullet "•". Limite Play Console 500 caratteri per lingua.
- `playstore/README.md`: riferimenti a versionCode 5 alle righe ~7, ~23, ~102 (tabella, "Pronto
  (`versionCode` 5)"), ~218-220 (sezione "6. Versionamento"), e riga ~226 "Il **prossimo**
  caricamento ... `versionCode = 6`". Riga ~32-33 "Aspetto in release: solo cifre bianche su nero".
  I riferimenti a versionCode 4 sono storico della 2.0 e restano invariati.
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Funzione pura formatVersionLabel con unit test</name>
  <files>app/src/main/java/com/sed/tachimetro/VersionLabel.kt, app/src/test/java/com/sed/tachimetro/VersionLabelTest.kt</files>
  <behavior>
    - formatVersionLabel("2.1-beta", 6) == "v2.1-beta (6)"
    - formatVersionLabel("2.0", 4) == "v2.0 (4)"
    - formatVersionLabel("1.0", 1) == "v1.0 (1)"
    - versionName vuoto o solo spazi: formatVersionLabel("", 6) == "v? (6)" (mai "v (6)", l'etichetta resta riconoscibile)
    - versionName con spazi ai bordi viene ripulito: formatVersionLabel(" 2.1-beta ", 6) == "v2.1-beta (6)"
  </behavior>
  <action>
RED: creare `VersionLabelTest.kt` nel package `com.sed.tachimetro` (JUnit4, stesso stile di
SurfaceSpeedTextTest: KDoc italiano sulla classe, metodi snake_case, es.
`betaName_formatsNameAndCode`, `blankName_usesQuestionMark`, `paddedName_isTrimmed`) con i casi di
<behavior>. Eseguire il test: deve fallire in compilazione (simbolo mancante). Commit
`test(quick-260929-cyv): add failing test for formatVersionLabel`.

GREEN: creare `VersionLabel.kt` nel package `com.sed.tachimetro` con una funzione top-level pura
`fun formatVersionLabel(versionName: String, versionCode: Int): String` (niente import Android),
che produce "v" + nome ripulito con trim (oppure "?" se vuoto dopo il trim) + " (" + codice + ")".
KDoc in italiano: scopo (far capire all'utente, in auto e sul telefono, quale build sta girando),
chiamanti (MainActivity e SpeedSurfaceRenderer con `BuildConfig.VERSION_NAME` /
`BuildConfig.VERSION_CODE`), formato di esempio "v2.1-beta (6)". Test verdi. Commit
`feat(quick-260929-cyv): implement formatVersionLabel pure mapping`.
  </action>
  <verify>
    <automated>cd /c/Users/fedes/AndroidStudioProjects/Tachimetro && ./gradlew.bat :app:testSpikePoiDebugUnitTest --tests com.sed.tachimetro.VersionLabelTest</automated>
  </verify>
  <done>VersionLabelTest verde (5 casi), commit RED precede commit GREEN.</done>
</task>

<task type="auto">
  <name>Task 2: Etichetta versione sul telefono e sulla Surface di Android Auto</name>
  <files>app/src/main/res/values/colors.xml, app/src/main/res/layout/activity_main.xml, app/src/main/java/com/sed/tachimetro/MainActivity.kt, app/src/main/java/com/sed/tachimetro/car/SpeedSurfaceRenderer.kt</files>
  <action>
Colore: in `colors.xml` aggiungere `<color name="version_label">#FF808080</color>` (grigio medio:
basso contrasto su nero, discreto ma leggibile da vicino).

Telefono, layout: in `activity_main.xml` aggiungere una TextView `@+id/versionText` in ALTO AL
CENTRO (unico punto libero: gli altri tre/quattro angoli sono gia' occupati da MAX/reset, km/h,
ricarica/toggle, distanza). Attributi: wrap_content x wrap_content, constraintTop_toTopOf parent,
constraintStart_toStartOf parent, constraintEnd_toEndOf parent, layout_marginTop="4dp",
textSize="11sp", textColor="@color/version_label", maxLines="1", singleLine="true",
clickable/focusable="false", importantForAccessibility="no", visibility sempre visible,
`tools:text="v2.1-beta (6)"`. Dichiararla DOPO messageText nell'XML (disegnata sopra). NON
modificare i vincoli di messageText/retryButton ne' degli altri elementi: le cifre della velocita'
sono centrate verticalmente e la fascia alta centrale resta libera, quindi la dimensione del
numero non cambia.

Telefono, MainActivity: aggiungere la proprieta' `private lateinit var versionText: TextView`,
assegnarla con `findViewById(R.id.versionText)` in una piccola funzione `setupVersionLabel()`
chiamata da onCreate accanto agli altri setup, che imposta
`versionText.text = formatVersionLabel(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)` e
chiama una nuova `applyVersionTextWindowInsets()`. Quest'ultima e' lo specchio di
`applyUnitTextWindowInsets()` ma solo sul lato top: margine base letto dai LayoutParams XML +
`maxOf(systemBars.top, cutout.top)`, restituisce `insets`. Commento in italiano che spiega perche'
(immersive + edge-to-edge targetSdk 36: senza inset l'etichetta finirebbe sotto status bar /
cutout centrale tipo punch-hole; quick 260929-cyv).

Surface Android Auto, SpeedSurfaceRenderer: aggiungere costanti companion `COLOR_VERSION =
"#808080"`, `VERSION_TEXT_MIN_PX = 14f`, `VERSION_TEXT_FRACTION = 0.03f`, un `versionPaint`
(antialias, Typeface.DEFAULT, colore COLOR_VERSION, `textAlign = Paint.Align.RIGHT`) e una
proprieta' `private val versionLabel = formatVersionLabel(BuildConfig.VERSION_NAME,
BuildConfig.VERSION_CODE)` (import `com.sed.tachimetro.formatVersionLabel`). In `drawContent`,
come nuovo passo dopo il disegno delle cifre (passo 3) e PRIMA e FUORI dal blocco
`if (BuildConfig.DEBUG)` dell'overlay, disegnare SEMPRE `versionLabel` nell'angolo in BASSO A
DESTRA dell'`area` gia' calcolata (stable area se nota, altrimenti intera Surface):
dimensione `max(VERSION_TEXT_MIN_PX, VERSION_TEXT_FRACTION * surfaceHeight)`, x = `area.right -
padding`, y (baseline) = `area.bottom - padding - versionPaint.descent()`, con padding =
`OUTLINE_STROKE_PX * 2`. Motivazione nel commento: le cifre occupano al massimo il 90% dell'area
centrate (bordo libero del 5% per lato) e sugli schermi auto larghi sono limitate in altezza, quindi
l'angolo basso-destro e' libero; l'overlay di debug sta in alto a sinistra, nessuna
sovrapposizione. Aggiornare il KDoc della classe (voce D-11: "In release solo cifre su nero" ->
cifre su nero + etichetta versione in basso a destra, sempre disegnata, quick 260929-cyv) e il
commento del passo 4-5. L'etichetta NON contiene la velocita' e NON va loggata (nessun nuovo Log).
  </action>
  <verify>
    <automated>cd /c/Users/fedes/AndroidStudioProjects/Tachimetro && grep -c "formatVersionLabel(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)" app/src/main/java/com/sed/tachimetro/MainActivity.kt app/src/main/java/com/sed/tachimetro/car/SpeedSurfaceRenderer.kt && grep -c "@+id/versionText" app/src/main/res/layout/activity_main.xml && grep -c "applyVersionTextWindowInsets" app/src/main/java/com/sed/tachimetro/MainActivity.kt && ./gradlew.bat :app:assembleSpikePoiDebug :app:assembleSpikeNavDebug</automated>
  </verify>
  <done>Ogni grep restituisce >= 1 (applyVersionTextWindowInsets >= 2: definizione + chiamata); il drawText di versionLabel nel renderer sta fuori da ogni `if (BuildConfig.DEBUG)` (controllo a lettura del diff); assemble spikePoiDebug e spikeNavDebug BUILD SUCCESSFUL. Commit `feat(quick-260929-cyv): show version label on phone and Android Auto surface`.</done>
</task>

<task type="auto">
  <name>Task 3: versionCode 6 e riallineamento playstore/, verifica finale</name>
  <files>app/build.gradle.kts, playstore/release_notes/release_notes_v2.1-beta.txt, playstore/README.md</files>
  <action>
`app/build.gradle.kts`: `versionCode = 5` -> `versionCode = 6` (versionName resta "2.1-beta",
il 5 e' gia' caricato sul test chiuso Play Store). Aggiornare il commento sui flavor
"(versionCode 5, 2.1-beta)" -> "(versionCode 6, 2.1-beta; il 5 e' gia' stato caricato)".

`playstore/release_notes/release_notes_v2.1-beta.txt`: stesso file unico bilingue (formato con
tag `<it-IT>`/`<en-US>`, NON file separati per lingua). Aggiungere in entrambe le lingue un bullet
"•" sulla versione visibile, es. it: "Numero di versione visibile in piccolo sul telefono e sullo
schermo dell'auto, per sapere sempre quale build sta girando"; en: "Version number shown in small
print on the phone and on the car screen, so you always know which build is running". Restare
sotto 500 caratteri per blocco (contarli e riportarli nel SUMMARY).

`playstore/README.md`: riallineare a versionCode 6 ogni riferimento corrente al 5 (riga ~7
"Aggiornato il ... versionCode = 5" -> aggiornato il 2026-09-29 per `versionCode = 6`, con nota che
il 5 e' gia' sul test chiuso; riga ~23 "Versione"; riga ~102 tabella "Pronto (`versionCode` 6)";
sezione "6. Versionamento" righe ~218-220 -> `versionCode = 6` (2026-09-29); riga ~226 "prossimo
caricamento" -> `versionCode = 7`). Aggiornare "Aspetto in release" (riga ~32) aggiungendo che in
release compare anche l'etichetta versione grigia in basso a destra della Surface e in alto al
centro sul telefono. Lasciare invariati i riferimenti storici a versionCode 4 (2.0). NON toccare
ROADMAP.md.

Verifica finale: eseguire l'intera suite unit test del flavor spikePoi e i due assemble debug.
Commit `chore(quick-260929-cyv): bump versionCode to 6 and align playstore/`.
  </action>
  <verify>
    <automated>cd /c/Users/fedes/AndroidStudioProjects/Tachimetro && grep -c "versionCode = 6" app/build.gradle.kts && ! grep -n "versionCode = 5\b\|\`versionCode\` 5)" playstore/README.md | grep -v "gia'" ; grep -c "<it-IT>\|<en-US>" playstore/release_notes/release_notes_v2.1-beta.txt && ./gradlew.bat :app:testSpikePoiDebugUnitTest :app:assembleSpikePoiDebug :app:assembleSpikeNavDebug</automated>
  </verify>
  <done>build.gradle.kts dichiara versionCode 6 / versionName "2.1-beta"; README e note di rilascio riferite al 6 e citano la versione visibile (solo menzioni storiche del 5 come "gia' caricato"); note < 500 caratteri per lingua; testSpikePoiDebugUnitTest, assembleSpikePoiDebug e assembleSpikeNavDebug BUILD SUCCESSFUL.</done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| app -> host Android Auto (Surface) | l'app disegna pixel sulla Surface fornita dall'host; nessun input nuovo |

## STRIDE Threat Register

| Threat ID | Category | Component | Disposition | Mitigation Plan |
|-----------|----------|-----------|-------------|-----------------|
| T-q-01 | Information Disclosure | etichetta versione (telefono + Surface) | accept | versionName/versionCode sono pubblici su Play Store; nessun dato utente, nessuna velocita' o posizione nell'etichetta |
| T-q-02 | Information Disclosure | logcat renderer | mitigate | nessun nuovo Log; i log esistenti restano sotto BuildConfig.DEBUG e senza velocita' (T-08-07) |
| T-q-03 | Tampering | firma release | accept | nessuna modifica a keystore.properties; firma manuale dell'utente via wizard Android Studio |
</threat_model>

<verification>
- `./gradlew.bat :app:testSpikePoiDebugUnitTest` verde (incluso VersionLabelTest).
- `./gradlew.bat :app:assembleSpikePoiDebug :app:assembleSpikeNavDebug` BUILD SUCCESSFUL.
- Nessuna build release firmata richiesta.
- Controllo visivo consigliato all'utente (non bloccante): su telefono l'etichetta in alto al
  centro non tocca il numero in portrait/landscape; su DHU o in auto l'etichetta e' in basso a
  destra e non tocca le cifre.
</verification>

<success_criteria>
- "v2.1-beta (6)" visibile sul telefono (alto centro, grigio, 11sp, con insets) e sulla Surface
  (basso destra della stable area, grigio, anche in release).
- Una sola sorgente del formato: formatVersionLabel, testata.
- versionCode 6 in build.gradle.kts; playstore/ coerente con il 6.
</success_criteria>

<output>
Create `.planning/quick/260929-cyv-numero-di-versione-visibile-su-telefono-/260929-cyv-SUMMARY.md` when done
</output>
