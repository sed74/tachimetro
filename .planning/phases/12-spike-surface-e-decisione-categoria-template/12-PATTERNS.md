# Phase 12: Spike Surface e Decisione Categoria/Template - Pattern Map

**Mapped:** 2026-09-23
**Files analyzed:** 15 (nuovi, modificati, temporanei)
**Analogs found:** 13 / 15

> Nota di fase: una parte dei file è **temporanea** (flavor `spikePoi`/`spikeNav`, D-01) e viene rimossa prima della chiusura (D-02). Il planner deve prevedere due momenti: (A) introduzione dei flavor + misura, (B) gate umano D-12, poi consolidamento su `main` e cancellazione dei source set di flavor.

## File Classification

| File nuovo/modificato | Ruolo | Data Flow | Analog più vicino | Match |
|---|---|---|---|---|
| `app/build.gradle.kts` (MOD: flavor aggiunti e poi rimossi) | config | build | se stesso (blocco `buildTypes` / `buildFeatures`, righe 47-68) | exact |
| `app/src/spikePoi/AndroidManifest.xml` (NUOVO, temporaneo) | config | manifest-merge | `app/src/main/AndroidManifest.xml` righe 23-33, 49-58 | role-match |
| `app/src/spikeNav/AndroidManifest.xml` (NUOVO, temporaneo) | config | manifest-merge | `app/src/main/AndroidManifest.xml` righe 49-58 | role-match |
| `app/src/main/AndroidManifest.xml` (MOD: `minCarApiLevel` 7, permessi del percorso scelto) | config | — | se stesso | exact |
| `app/src/main/res/xml/automotive_app_desc.xml` (solo verifica) | config | — | se stesso | exact |
| `app/src/main/java/com/sed/tachimetro/car/SpeedSurfaceRenderer.kt` (NUOVO) | component (renderer) | event-driven (callback host) | `car/SpeedScreen.kt` (lifecycle, log DEBUG) + ARCHITECTURE.md Pattern 1 | partial |
| `app/src/main/java/com/sed/tachimetro/car/TachimetroCarSession.kt` (MOD) | component (session) | event-driven (lifecycle) | se stesso + ARCHITECTURE.md "Internal Boundaries" | exact |
| `app/src/main/java/com/sed/tachimetro/car/SpeedScreen.kt` (MOD: ramo Granted → template con Surface) | component (screen) | request-response (host → `onGetTemplate`) | se stesso, `buildTemplate()` righe 209-284 | exact |
| `app/src/spikePoi/java/com/sed/tachimetro/car/SurfaceTemplateFactory.kt` (NUOVO, temporaneo) | utility (builder template) | transform | `SpeedScreen.buildTemplate()` righe 216-284 | role-match |
| `app/src/spikeNav/java/com/sed/tachimetro/car/SurfaceTemplateFactory.kt` (NUOVO, temporaneo) | utility (builder template) | transform | `SpeedScreen.buildTemplate()` righe 216-284 | role-match |
| `app/src/main/java/com/sed/tachimetro/car/SpikeDigitHeight.kt` (NUOVO, opzionale: criterio D-12 puro) | utility (funzione pura) | transform | `car/CarSpeedContent.kt` | role-match |
| `app/src/test/java/com/sed/tachimetro/car/SpikeDigitHeightTest.kt` (NUOVO, opzionale) | test (JVM) | — | `car/CarSpeedContentTest.kt` | exact |
| `app/src/androidTest/java/com/sed/tachimetro/car/SpeedScreenTemplateTest.kt` (MOD) | test (strumentato) | — | se stesso | exact |
| `scripts/surface-spike-check.ps1` (NUOVO) | script (tooling DHU) | batch / log-capture | `scripts/dhu-quota-check.ps1` + selezione device di `scripts/aa-connect-cycle-check.ps1` | exact |
| `docs/surface-spike-verification.md` (NUOVO, runbook) | doc | — | `docs/dhu-quota-verification.md` | exact |
| `.planning/phases/12-.../12-SPIKE-RESULTS.md` (NUOVO) | doc (risultati) | — | nessun analog di "tabella misure" | none |
| `.planning/PROJECT.md` (MOD, Key Decisions righe 123-124) / `.planning/REQUIREMENTS.md` (MOD condizionale AA-05/AA-08) | doc | — | se stessi | exact |

---

## Pattern Assignments

### `app/build.gradle.kts` (config)

**Analog:** stesso file. Oggi nessun `flavorDimensions`/`productFlavors`. `buildConfig = true` è già attivo (righe 62-67) quindi `BuildConfig.DEBUG` e `BuildConfig.FLAVOR` sono disponibili senza altre modifiche.

**Commento-motivazione in stile progetto** (righe 62-67, da imitare per il blocco flavor):
```kotlin
    // AGP 8+ no longer generates BuildConfig by default. Enabled here so Piano 02 can gate
    // the car-screen refresh-count diagnostic log behind BuildConfig.DEBUG, keeping speed
    // values out of logcat in release builds (T-08-03).
    buildFeatures {
        buildConfig = true
    }
```

**Da aggiungere dentro `android { }` (dopo `buildTypes`, righe 47-58)** — nomi a discrezione (Claude's Discretion):
```kotlin
    // D-01 (Fase 12): flavor TEMPORANEI dello spike Surface. Rimossi a fine fase (D-02).
    // Mai distribuiti (D-05): solo installDebug locale su DHU.
    flavorDimensions += "surfaceSpike"
    productFlavors {
        create("spikePoi") { dimension = "surfaceSpike" }
        create("spikeNav") { dimension = "surfaceSpike" }
    }
```
Comandi ripetibili (D-01): `./gradlew.bat :app:installSpikePoiDebug` / `:app:installSpikeNavDebug`. Attenzione: con flavor attivi `installDebug`/`testDebugUnitTest` cambiano nome (`testSpikePoiDebugUnitTest`...) — il runbook e i comandi di verifica dei piani devono usarli finché i flavor esistono, poi tornare ai nomi originali (SC4).

Dipendenze: **nessuna nuova** (STACK.md: tutto in `androidx.car.app:app:1.7.0`, `libs.versions.toml` riga 13).

---

### `app/src/spikePoi/AndroidManifest.xml` e `app/src/spikeNav/AndroidManifest.xml` (config, manifest-merge)

**Analog:** `app/src/main/AndroidManifest.xml`.

**Stile commento con tag decisionale + permesso** (righe 5-11):
```xml
    <!-- Deliberately fine-only: this app needs precise instantaneous speed for a
         speedometer display, ... (Phase 2 threat model T-02-EP: no additional permission scope). -->
    <uses-permission
        android:name="android.permission.ACCESS_FINE_LOCATION"
        tools:ignore="CoarseFineLocation" />
```

**Service e categoria da sovrascrivere** (righe 49-58):
```xml
        <service
            android:name=".car.TachimetroCarAppService"
            android:exported="true"
            android:label="@string/app_name"
            android:icon="@mipmap/ic_launcher">
            <intent-filter>
                <action android:name="androidx.car.app.CarAppService" />
                <category android:name="androidx.car.app.category.POI" />
            </intent-filter>
        </service>
```

Regole per i manifest di flavor:
- `spikePoi`: `<uses-permission android:name="androidx.car.app.ACCESS_SURFACE" />` + `androidx.car.app.MAP_TEMPLATES`. La categoria POI del main resta.
- `spikeNav`: `ACCESS_SURFACE` + `androidx.car.app.NAVIGATION_TEMPLATES`; nel `<service>` aggiungere `<category android:name="androidx.car.app.category.NAVIGATION" />` e **rimuovere** la categoria POI ereditata con `<category android:name="androidx.car.app.category.POI" tools:node="remove" />` (il merger unisce gli `intent-filter` elemento per elemento; senza `tools:node="remove"` l'APK NAV dichiarerebbe entrambe le categorie).
- **Mai** `MAP_TEMPLATES` e `NAVIGATION_TEMPLATES` nello stesso APK (D-01): il main manifest non deve dichiarare nessuno dei due finché i flavor esistono. Verifica consigliata nel piano: `aapt2 dump badging` / ispezione di `app/build/intermediates/merged_manifests/...` per entrambe le varianti.
- `minCarApiLevel` 7 può andare direttamente nel main (è comune ad entrambi i percorsi, REL-02).

---

### `app/src/main/AndroidManifest.xml` (config, MOD finale)

**Righe da modificare** (23-33):
```xml
        <!-- D-00a: dichiara l'esperienza a template Android Auto (categoria POI, non
             NAVIGATION, vedi service sotto). minCarApiLevel 1 massimizza la compatibilita'
             con head unit vecchi; tutte le API usate in questa fase sono disponibili da
             CarAppApiLevel 1. -->
        <meta-data
            android:name="com.google.android.gms.car.application"
            android:resource="@xml/automotive_app_desc" />
        <meta-data
            android:name="androidx.car.app.minCarApiLevel"
            android:value="1" />
```
→ `android:value="7"` e commento riscritto con tag `REL-02` (nessun fallback `PaneTemplate`, nessun check runtime). Alla chiusura (D-02) i permessi del solo percorso scelto passano dal manifest di flavor al main; categoria del `<service>` coerente con la decisione; commento `D-00a` del service (righe 40-48) aggiornato citando REL-01 e `12-SPIKE-RESULTS.md`.

### `app/src/main/res/xml/automotive_app_desc.xml` (solo verifica)
Contenuto attuale: `<uses name="template" />` con commento `D-00a`. ARCHITECTURE.md ("Integration Points"): nessun cambio previsto per nessuno dei due percorsi; se vince NAV aggiornare solo il commento ("categoria POI").

---

### `car/SpeedSurfaceRenderer.kt` (component, event-driven) — NUOVO

**Analog codice:** nessun `SurfaceCallback` nel progetto. Struttura da ARCHITECTURE.md Pattern 1 (righe 135-197, sample ufficiale `navigation/SurfaceRenderer` ridotto); convenzioni da `SpeedScreen.kt`.

**Import — ordine a gruppi separati da riga vuota** (SpeedScreen.kt righe 3-30: `android.*` → `androidx.*` → `kotlinx.*` → `com.sed.tachimetro.*`):
```kotlin
import android.Manifest
import android.content.Intent
...
import android.util.Log

import androidx.car.app.CarContext
import androidx.car.app.Screen
...
import androidx.lifecycle.lifecycleScope

import kotlinx.coroutines.flow.MutableStateFlow
...

import com.sed.tachimetro.BuildConfig
import com.sed.tachimetro.R
```
Per il renderer: `android.graphics.{Canvas,Color,Paint,Rect}`, `android.os.{Handler,Looper}`, `android.util.Log`, `android.view.Surface` / `androidx.car.app.{AppManager,CarContext,SurfaceCallback,SurfaceContainer}`, `androidx.lifecycle.{DefaultLifecycleObserver,LifecycleOwner}` / `com.sed.tachimetro.BuildConfig`. È l'**unico** file che importa `android.graphics.*` (ARCHITECTURE.md "Structure Rationale").

**Log diagnostico DEBUG — pattern da copiare** (SpeedScreen.kt righe 41-43 e 307-312):
```kotlin
    companion object {
        private const val LOG_TAG = "TachimetroCar"
    }
...
        if (BuildConfig.DEBUG) {
            Log.d(
                LOG_TAG,
                "onGetTemplate #$templateBuildCount content=${templateLogLabel(permission, latestState)}",
            )
        }
```
D-10: tag **dedicato** (es. `TachimetroSurface`, distinto da `TachimetroCar` così lo script filtra solo le misure), una riga per callback con formato chiave=valore stabile e parsabile da regex, es.:
`onSurfaceAvailable w=1280 h=720 dpi=160 api=7` / `onStableAreaChanged l=.. t=.. r=.. b=..` / `onVisibleAreaChanged l=.. t=.. r=.. b=..`. Il `carContext.carAppApiLevel` va loggato (e disegnato piccolo sulla Surface, D-11).

**Core pattern (da ARCHITECTURE.md Pattern 1, ridotto per lo spike — nessun `submit()`/`SurfaceFrame`, D-03):**
```kotlin
class SpeedSurfaceRenderer(private val carContext: CarContext) :
    SurfaceCallback, DefaultLifecycleObserver {
    private var surface: Surface? = null
    private var surfaceW = 0; private var surfaceH = 0; private var dpi = 160
    private var stable: Rect? = null; private var visible: Rect? = null
    private val handler = Handler(Looper.getMainLooper())
    private var renderPending = false

    fun requestRender() { if (renderPending) return; renderPending = true
        handler.post { renderPending = false; drawFrame() } }

    override fun onCreate(owner: LifecycleOwner) {
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(this)
    }
    override fun onDestroy(owner: LifecycleOwner) { handler.removeCallbacksAndMessages(null); releaseSurface() }

    override fun onSurfaceAvailable(c: SurfaceContainer) {
        val incoming = c.surface
        if (surface !== incoming) surface?.release()   // Anti-Pattern 4
        surface = incoming; surfaceW = c.width; surfaceH = c.height; dpi = c.dpi
        requestRender()
    }
    override fun onStableAreaChanged(r: Rect) { stable = Rect(r); requestRender() }
    override fun onVisibleAreaChanged(r: Rect) { visible = Rect(r); requestRender() }
    override fun onSurfaceDestroyed(c: SurfaceContainer) { releaseSurface() }

    private fun drawFrame() {
        val s = surface ?: return
        if (!s.isValid) return
        val canvas = try { s.lockCanvas(null) } catch (e: IllegalArgumentException) { return }
                     catch (e: IllegalStateException) { return }
        try { /* D-03: drawColor pieno, "888" centrato nella stable area,
                 strokeRect stable (colore A) e visible (colore B), testo piccolo "api=N" (D-11) */ }
        finally { s.unlockCanvasAndPost(canvas) }
    }
}
```
Punti vincolanti:
- `Rect` vuoto = "sconosciuto" (ARCHITECTURE.md fatti verificati): fallback all'intera surface per centrare "888"; loggare comunque il valore grezzo.
- `lockCanvas` vs `lockHardwareCanvas`: discrezione; ARCHITECTURE.md raccomanda `lockCanvas` (sample ufficiale).
- Nessun collegamento a `GpsSpeedProvider` (D-03, Anti-Pattern 3).
- `setSurfaceCallback` richiede `ACCESS_SURFACE` → `SecurityException` se il manifest del flavor manca: il manifest va fatto **prima** (ordine dei task).
- KDoc di classe in italiano con tag (`D-03`, `D-10`, `D-11`), stesso stile di SpeedScreen.kt righe 32-38.

---

### `car/TachimetroCarSession.kt` (component, lifecycle) — MOD

**Analog:** stesso file (15 righe, intero):
```kotlin
class TachimetroCarSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen = SpeedScreen(carContext)
}
```
**Modifica** (ARCHITECTURE.md "Internal Boundaries"): il renderer vive quanto la Session.
```kotlin
class TachimetroCarSession : Session() {
    private var renderer: SpeedSurfaceRenderer? = null
    override fun onCreateScreen(intent: Intent): Screen {
        val r = SpeedSurfaceRenderer(carContext).also { lifecycle.addObserver(it) }
        renderer = r
        return SpeedScreen(carContext)       // D-04: firma di SpeedScreen invariata nello spike
    }
    override fun onCarConfigurationChanged(newConfiguration: Configuration) { renderer?.requestRender() }
}
```
Nota: `onCreateScreen` avviene dopo `ON_CREATE` della Session; `addObserver` su un lifecycle già CREATED riceve comunque `onCreate` (replay dei LifecycleObserver), quindi `setSurfaceCallback` parte. In alternativa creare il renderer in un blocco `init { lifecycle.addObserver(...) }` — ma `carContext` non è valido nel costruttore della Session: preferire `onCreateScreen`. Nessun `setSurfaceCallback(null)` in `onDestroy` (ARCHITECTURE.md riga 338).

---

### `car/SpeedScreen.kt` (component, request-response) — MOD

**Analog:** stesso file. D-04: la macchina a stati (righe 58-207: `permissionState`, `requestInFlight`, `refreshPermissionState()`, `requestLocationPermission()`, `onRetryOrSettingsClicked()`, `openAppSettingsFromCar()`) **non si tocca**.

**Punto di intervento** (righe 303-315):
```kotlin
    override fun onGetTemplate(): Template {
        templateBuildCount++
        val permission = permissionState.value
        if (BuildConfig.DEBUG) { Log.d(LOG_TAG, "onGetTemplate #$templateBuildCount content=...") }
        return buildTemplate(permission, latestState)
    }
```
→ se `permission == CarPermissionState.Granted` restituire `buildSurfaceTemplate()` (unico punto dipendente dalla categoria, ARCHITECTURE.md Pattern 4 righe 271-276), altrimenti `buildTemplate(permission, latestState)` invariato. Tipo di ritorno `Template` già generico.

**Ramo collect GPS** (righe 111-116): nello spike la collect nel ramo Granted può restare (aggiorna `latestState` + `invalidate()`); valutare se sopprimere l'`invalidate()` per tick quando il template è con Surface (Anti-Pattern 1) — `NavigationTemplate` accetta refresh illimitati, `MapWithContentTemplate` non documentato: nello spike è accettabile lasciarlo ma annotarlo in `12-SPIKE-RESULTS.md`; va rimosso in Fase 13.

**Action/Header pattern da riusare nei builder** (righe 267-283):
```kotlin
                pane.addAction(
                    Action.Builder()
                        .setTitle(carContext.getString(actionTitleRes))
                        .setOnClickListener(
                            ParkedOnlyOnClickListener.create { onRetryOrSettingsClicked() }
                        )
                        .build()
                )
...
        return PaneTemplate.Builder(pane.build())
            .setHeaderAction(Action.APP_ICON)
            .build()
```

---

### `src/spikePoi/java/.../car/SurfaceTemplateFactory.kt` e `src/spikeNav/java/.../car/SurfaceTemplateFactory.kt` (utility, transform) — NUOVI, temporanei

**Analog:** `SpeedScreen.buildTemplate()` (righe 216-284): funzione senza effetti collaterali che riceve tutto per parametro e ritorna un template; KDoc "seam di test" righe 209-215.

Meccanismo: stessa firma top-level in entrambi i source set di flavor (es. `fun buildSurfaceTemplate(carContext: CarContext): Template`), così `SpeedScreen` in `main` compila per entrambe le varianti senza `if (BuildConfig.FLAVOR == ...)`. Alternativa accettabile (discrezione): un unico file in `main` con `when (BuildConfig.FLAVOR)` — ma allora gli import di `NavigationTemplate` e `MapWithContentTemplate` coesistono (ok a livello di codice; il vincolo D-01 riguarda solo i permessi nel manifest).

- **spikeNav:** `NavigationTemplate.Builder().setActionStrip(ActionStrip.Builder().addAction(<azione minima>).build()).build()` — `build()` lancia `IllegalStateException` senza action strip. Azione minima: icona senza titolo (discrezione, D-08), es. `Action.Builder().setIcon(CarIcon.APP_ICON)...` o `Action.PAN` se accettata.
- **spikePoi:** `MapWithContentTemplate.Builder().setContentTemplate(<variante>).setActionStrip(...)?.build()` con varianti D-06: `MessageTemplate.Builder("Tachimetro")` (D-07 segnaposto), `ListTemplate` con una `Row` minima, eventualmente `PaneTemplate` (header `Action.APP_ICON` come riga 282) / `GridTemplate`. Selettore variante: costante `const val` o seconda flavor dimension (discrezione); deve essere loggato nella riga `onSurfaceAvailable` (es. `variant=message`) così lo script associa le misure alla variante.
- `@RequiresCarApi(7)` su `MapWithContentTemplate`: con `minCarApiLevel` 7 nessun check runtime (REL-02); eventuale warning lint si sopprime con commento `REL-02`.

---

### `car/SpikeDigitHeight.kt` + `SpikeDigitHeightTest.kt` (opzionali, funzione pura D-12)

**Analog:** `car/CarSpeedContent.kt` (framework-free, KDoc con tag, `when` esaustivo) e `CarSpeedContentTest.kt`.

**Header/KDoc pura** (CarSpeedContent.kt righe 1-9 e 67-87):
```kotlin
package com.sed.tachimetro.car

import com.sed.tachimetro.gps.SpeedState

/**
 * D-01: modello sealed del contenuto della Row dello schermo auto. ...
 * Nessun import Android qui: framework-free, come
 * `DistanceDisplay`/`formatDistanceDisplay`.
 */
```
**Test JVM** (CarSpeedContentTest.kt righe 1-19):
```kotlin
package com.sed.tachimetro.car

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Plain JVM unit tests for [carSpeedContent] -- no Android runtime.
 * Locks D-01 ..., D-02 ... and AA-02 ...
 */
class CarSpeedContentTest {
    @Test
    fun reading_returnsSpeedWithKmh() {
```
Vincolo: niente `android.graphics.Rect` nella funzione pura (ARCHITECTURE.md Anti-Pattern 6) — usare `Int` o una data class propria. Se la funzione nasce qui con un nome generico (es. `AreaPx`), va messa in `car/` in modo che Fase 13 la riusi in `SurfaceLayout.kt`. Se il calcolo del 70% si fa a mano nel documento risultati, file e test si omettono.

---

### `SpeedScreenTemplateTest.kt` (test strumentato) — MOD

**Analog:** stesso file. Impatto:
- `createTemplate()` (righe 81-90) fa `screen.onGetTemplate() as PaneTemplate` a CREATED → stato `NotRequested` → resta `PaneTemplate`: **nessun cambio**.
- `buildTemplate(permission, speed)` (righe 100-111) chiama il seam `SpeedScreen.buildTemplate()`: se il seam resta invariato (il ramo Granted del `PaneTemplate` rimane nel seam e solo `onGetTemplate()` devia su `buildSurfaceTemplate()`), i test `granted_*` (righe 129-159) e `everyPermissionState_*` (righe 199-221) restano verdi senza modifiche. Scelta minima consigliata per lo spike (D-04); la riscrittura completa è Fase 13.
- Con i flavor attivi il task diventa `:app:connectedSpikePoiDebugAndroidTest`.

---

### `scripts/surface-spike-check.ps1` (script, log-capture) — NUOVO

**Analog principale:** `scripts/dhu-quota-check.ps1`. **Selezione device:** `scripts/aa-connect-cycle-check.ps1` righe 97-115 (telefono fisico OnePlus 8T, **non** la preferenza `emulator-*` di dhu-quota-check righe 107-121).

**Header comment-based help in italiano** (dhu-quota-check.ps1 righe 1-46): `.SYNOPSIS` con tag decisionali, `.DESCRIPTION` che rimanda al runbook, avviso "l'esito NON sostituisce la conferma umana", `.PARAMETER`, `.EXAMPLE`.

**Parametri e costanti** (righe 47-64):
```powershell
[CmdletBinding()]
param(
    [int]$DurationSeconds = 600,
    [string]$OutputDir = "build/dhu-quota",
    [string]$Serial = ""
)
$ErrorActionPreference = 'Stop'
$AppId = "com.sed.tachimetro"
$LogTag = "TachimetroCar"
```
→ per lo spike: `-Variant` (es. `nav`, `poi-message`, `poi-list`), `-Resolution` (`800x480|1280x720|1920x1080`), `-OutputDir "build/surface-spike"` (sotto `/build`, già gitignored, T-08-12), `$LogTag` = tag dedicato del renderer.

**Blocchi da copiare quasi letterali:**
- `Write-Section` (righe 66-70).
- Verifica adb + elenco device (righe 72-93).
- Selezione device fisico (aa-connect-cycle-check.ps1 righe 100-115):
```powershell
    $allSerials = @($connectedSerials)
    if ($allSerials.Count -gt 1) {
        Write-Error "Piu' di un dispositivo connesso ($($allSerials -join ', ')) -- specificare quale usare con -Serial. ..."
        exit 1
    }
    $targetSerial = $allSerials[0]
```
- Output con timestamp (righe 124-130), `adb forward tcp:5277 tcp:5277` con retry (righe 132-148).
- `logcat -c` + cattura filtrata in background (righe 150-157):
```powershell
$logcatProcess = Start-Process -FilePath "adb" `
    -ArgumentList @("-s", $targetSerial, "logcat", "-s", "$LogTag`:D") `
    -NoNewWindow -RedirectStandardOutput $logFile -PassThru
```
- Pausa manuale `Read-Host` per avvio DHU (righe 159-165) — qui: "avviare DHU con `-c <ini risoluzione>`, aprire Tachimetro, fare lo screenshot, premere INVIO".
- Stop cattura (righe 224-229), parsing con `-match` + `$Matches` (righe 239-261) → estrarre l'**ultima** riga per ciascun callback (`onSurfaceAvailable`, `onStableAreaChanged`, `onVisibleAreaChanged`).
- Riepilogo su video + file UTF-8 (righe 303-334), formattato come riga pronta da incollare nella tabella di `12-SPIKE-RESULTS.md` (variante, risoluzione, surface WxH, dpi, stable, visible, api).
- Opzionale: screenshot automatico `adb exec-out screencap` NON cattura il DHU (finestra desktop) — lo screenshot resta manuale; il runbook deve dirlo.

Niente cronometro/campionamento PID (righe 167-222) — non serve per la misura; eventualmente una durata breve (es. 15-20 s) per osservare la scomparsa dell'action strip (~10 s, D-08).

---

### `docs/surface-spike-verification.md` (runbook) — NUOVO

**Analog:** `docs/dhu-quota-verification.md` — struttura sezioni da copiare:
1. `# Titolo` + paragrafo "Runbook riproducibile per il gate SCx della Fase NN" (righe 1-6)
2. `## Perche' questa verifica esiste` (righe 8-18)
3. `## Prerequisiti` (righe 20-39) — qui: telefono fisico, DHU da `<sdk>\extras\google\auto\`, Developer Mode + "Avvia server head unit", file `.ini` per le tre risoluzioni D-09 (`[general] resolution = 800x480` ecc., passati con `desktop-head-unit.exe -c <file>.ini`), permesso già concesso.
4. `## Procedura passo-passo` con blocchi di comando (righe 41-61) — un ciclo per (flavor × variante POI × risoluzione).
5. `## Cosa osservare a occhio durante la sessione` (righe 63-74) — contorni stable/visible, card POI, action strip e sua scomparsa, `api=` sulla Surface.
6. `## Criteri di esito` tabellare (righe 76-87) — qui il criterio D-12 (≥ 70% per tutte e tre le risoluzioni) + gate umano.
7. `## Cosa e' fuori scope` (righe 109-114).
Il vecchio runbook (righe 104-107) dichiara la strada Surface/`NAVIGATION` "esplicitamente scartata": il nuovo runbook deve citare che REL-01 la riapre di proposito.

Sezione opzionale D-11 (sideload in auto): riusare lo stile di `docs/android-auto-hardening-verification.md` (PASS visivi senza logcat).

---

### `.planning/PROJECT.md` / `.planning/REQUIREMENTS.md` (doc) — MOD

**Righe da aggiornare in PROJECT.md** (Key Decisions, righe 123-124), formato tabella `| Decisione | Motivazione | Esito |`:
```
| Passaggio a `NavigationTemplate`+`SurfaceCallback` rimandato a milestone v2.1 dedicata | ... | Pending (v2.1, non ancora avviata) |
```
→ esito aggiornato + nuova riga "Categoria/template v2.1: <scelta> (REL-01)" che rimanda a `.planning/phases/12-.../12-SPIKE-RESULTS.md`, con ✓ e data, stesso stile della riga 123 (`✓ Accettato consapevolmente (verificato su DHU dal vivo, Fase 8)`).
REQUIREMENTS.md righe 13 e 16 (AA-05, AA-08): riscrivere solo se vince POI (D-14); riga 24-25 (REL-01/02) e tabella tracciabilità righe 54-55 → stato.

---

## Shared Patterns

### Log diagnostico solo in debug
**Source:** `car/SpeedScreen.kt` righe 41-43, 307-312
**Apply to:** `SpeedSurfaceRenderer.kt`, eventuale log della variante nei `SurfaceTemplateFactory`
Sempre dentro `if (BuildConfig.DEBUG)`, tag in `companion object` `private const val`, nessun dato di posizione/velocità (T-08-07/T-09-09).

### Contesto e lifetime (WR-04)
**Source:** `SpeedScreen.kt` righe 45-53, `TachimetroCarAppService.kt` righe 39-44
**Apply to:** renderer e Session. Il renderer tiene il `CarContext` della Session (stessa vita della Session, ammesso); mai un'Activity; niente riferimenti che sopravvivano alla Session. Handler svuotato in `onDestroy`.

### Commenti e KDoc
**Source:** tutti i file `car/` (es. `SpeedScreen.kt` righe 32-38, 209-215; `CarSpeedContent.kt` righe 52-58)
**Apply to:** tutti i file Kotlin/XML/PS1 nuovi. Italiano, apostrofo al posto delle lettere accentate nei commenti (`e'`, `perche'`), tag decisionali `D-xx` / `REL-0x` / `AA-xx` in testa al commento.

### Gate debug/release per l'host
**Source:** `TachimetroCarAppService.kt` righe 32-44 (`createCarHostValidator(applicationContext, BuildConfig.DEBUG)`)
**Apply to:** nessuna modifica; conferma che le build debug di spike su DHU sono accettate (ALLOW_ALL in debug) — D-05: solo debug.

### Script DHU
**Source:** `scripts/dhu-quota-check.ps1` + `scripts/aa-connect-cycle-check.ps1`
**Apply to:** `scripts/surface-spike-check.ps1`. `$ErrorActionPreference='Stop'`, ogni `adb` con `-s $targetSerial`, `@(...)` per forzare array, output sotto `build/`, esito euristico + avviso conferma umana.

## No Analog Found

| File | Ruolo | Data Flow | Motivo |
|---|---|---|---|
| `car/SpeedSurfaceRenderer.kt` (parte `SurfaceCallback`/Canvas) | component | event-driven | Nessun `SurfaceCallback` né disegno su Canvas nel progetto: usare ARCHITECTURE.md Pattern 1 (righe 135-197) e il sample AOSP `navigation/SurfaceRenderer`; solo convenzioni (import, log, KDoc) dal codice esistente |
| `12-SPIKE-RESULTS.md` | doc risultati | — | Nessun documento di misure esistente; struttura suggerita: tabella per (percorso/variante × risoluzione) con surface, dpi, stable, visible, api, altezza "888" ottenibile, rapporto POI/NAV, screenshot; sezione criterio D-12; motivazione con NF-1/NF-6 (D-13); decisione utente |

## Metadata

**Analog search scope:** `app/src/main/java/com/sed/tachimetro/car/`, `app/src/test/.../car/`, `app/src/androidTest/.../car/`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/xml/`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `scripts/`, `docs/`, `.planning/PROJECT.md`, `.planning/REQUIREMENTS.md`
**Files scanned:** 16
**Pattern extraction date:** 2026-09-23
