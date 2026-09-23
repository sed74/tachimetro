<#
.SYNOPSIS
    D-01/D-09/D-10 (Fase 12): una sessione di misura dello spike Surface Android Auto con un
    solo comando. Installa la variante richiesta (POI con card message/list/pane/grid oppure
    NAVIGATION) sul telefono fisico, cattura i log del tag TachimetroSurface durante una sessione
    DHU alla risoluzione scelta (800x480, 1280x720, 1920x1080) e produce una riga di tabella
    pronta da incollare in 12-SPIKE-RESULTS.md.

.DESCRIPTION
    Vedi docs/surface-spike-verification.md per il runbook completo (prerequisiti, elenco delle
    15 sessioni variante x risoluzione, cosa osservare a occhio, criterio D-12, verifica
    opzionale in auto D-11).

    Flusso:
      1. verifica adb e seleziona il telefono fisico (errore se piu' device senza -Serial);
      2. installa la variante con gradlew (:app:installSpikeNavDebug oppure
         :app:installSpikePoiDebug -PpoiCard=<card>), salvo -SkipInstall;
      3. apre il forward tcp:5277 richiesto dal Desktop Head Unit;
      4. svuota logcat e cattura in background solo il tag TachimetroSurface;
      5. attende che l'utente avvii il DHU con l'ini della risoluzione, apra Tachimetro e salvi lo
         screenshot della finestra DHU (manuale: adb screencap cattura il telefono, non il DHU);
      6. lascia girare la cattura per -DurationSeconds (per vedere la scomparsa dell'action
         strip, D-08), poi estrae l'ULTIMA riga di onSurfaceAvailable / onStableAreaChanged /
         onVisibleAreaChanged / frame e scrive il riepilogo.

    NON scarica nulla: usa adb dal PATH e il Gradle wrapper del repository.

    ATTENZIONE (D-10): il riepilogo di questo script NON sostituisce la conferma umana ne' lo
    screenshot della finestra DHU. Cosa l'host sovrappone alla Surface (card POI, action strip,
    titoli) si giudica guardando lo schermo, non i log.

.PARAMETER Variant
    Variante da misurare (D-06): 'nav' (flavor spikeNav, NavigationTemplate) oppure
    'poi-message' / 'poi-list' / 'poi-pane' / 'poi-grid' (flavor spikePoi,
    MapWithContentTemplate con la card corrispondente, selezionata con -PpoiCard).

.PARAMETER Resolution
    Risoluzione DHU (D-09): '800x480', '1280x720' o '1920x1080'. Determina il file
    scripts/dhu/dhu-<Resolution>.ini da passare a desktop-head-unit.exe con -c, e viene
    confrontata con la dimensione Surface riportata da onSurfaceAvailable.

.PARAMETER DurationSeconds
    Durata della cattura dopo l'INVIO, in secondi. Default 20: abbastanza per osservare se e
    quando l'action strip si nasconde (atteso ~10 s su NavigationTemplate, D-08).

.PARAMETER OutputDir
    Directory per log e riepilogo. Default build/surface-spike (relativa alla radice del
    repository), gia' ignorata da git tramite la regola /build di .gitignore (T-12-11).

.PARAMETER Serial
    Serial adb del telefono. Facoltativo se e' connesso un solo dispositivo; obbligatorio se ne
    sono connessi piu' di uno (T-12-12).

.PARAMETER SkipInstall
    Salta l'installazione con Gradle (utile per ripetere la misura della stessa variante a
    un'altra risoluzione senza reinstallare).

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts\surface-spike-check.ps1 -Variant poi-list -Resolution 1280x720

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts\surface-spike-check.ps1 -Variant nav -Resolution 800x480 -SkipInstall
#>
[CmdletBinding()]
param(
    # T-12-10: ValidateSet -- nessun valore libero finisce nella riga di comando di Gradle/adb.
    [Parameter(Mandatory)]
    [ValidateSet('nav','poi-message','poi-list','poi-pane','poi-grid')]
    [string]$Variant,

    [Parameter(Mandatory)]
    [ValidateSet('800x480','1280x720','1920x1080')]
    [string]$Resolution,

    [int]$DurationSeconds = 20,
    [string]$OutputDir = "build/surface-spike",
    [string]$Serial = "",
    [switch]$SkipInstall
)

$ErrorActionPreference = 'Stop'

# applicationId di app/build.gradle.kts -- valore letterale, come negli altri script DHU.
$AppId = "com.sed.tachimetro"
# Tag del contratto di log del Piano 02 (renderer di spike, solo BuildConfig.DEBUG).
$LogTag = "TachimetroSurface"
$PhaseDirRelative = ".planning/phases/12-spike-surface-e-decisione-categoria-template"

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

function Write-Section {
    param([string]$Text)
    Write-Host ""
    Write-Host "=== $Text ===" -ForegroundColor Cyan
}

function Write-Warn {
    param([string]$Text)
    Write-Host "AVVISO: $Text" -ForegroundColor Yellow
}

# Mappatura -Variant -> task Gradle + proprieta' di progetto (flavor del Piano 01).
if ($Variant -eq 'nav') {
    $gradleArgs = @(":app:installSpikeNavDebug")
    $expectedFlavor = "spikeNav"
    $expectedCard = "none"
}
else {
    $expectedCard = $Variant.Substring(4)   # 'poi-list' -> 'list'
    $gradleArgs = @(":app:installSpikePoiDebug", "-PpoiCard=$expectedCard")
    $expectedFlavor = "spikePoi"
}

$iniPath = Join-Path $RepoRoot "scripts\dhu\dhu-$Resolution.ini"
if (-not (Test-Path $iniPath)) {
    Write-Error "Config DHU non trovata: $iniPath"
    exit 1
}

$screenshotDir = Join-Path $RepoRoot ($PhaseDirRelative + "/spike-screenshots")
$screenshotPath = Join-Path $screenshotDir "$Variant-$Resolution.png"

# --- 1. Verifica adb e selezione del telefono fisico ------------------------------------------
Write-Section "Verifica adb e selezione del dispositivo"

$adbCmd = Get-Command adb -ErrorAction SilentlyContinue
if (-not $adbCmd) {
    Write-Error "adb non trovato sul PATH. Verificare che platform-tools dell'Android SDK sia nel PATH."
    exit 1
}

$devicesOutput = & adb devices
$deviceLines = @($devicesOutput | Select-String -Pattern "\tdevice$")
if ($deviceLines.Count -eq 0) {
    Write-Error "Nessun dispositivo in stato 'device'. Output di 'adb devices':`n$devicesOutput"
    exit 1
}
Write-Host "Dispositivo/i in stato 'device' trovato/i:"
$connectedSerials = New-Object System.Collections.Generic.List[string]
foreach ($deviceLine in $deviceLines) {
    Write-Host "  $deviceLine"
    $serialToken = ($deviceLine.Line -split '\s+')[0]
    $connectedSerials.Add($serialToken)
}

# T-12-12: ogni comando adb successivo e' targettizzato con -s; con piu' device serve -Serial.
if ($Serial) {
    if (-not ($connectedSerials -contains $Serial)) {
        Write-Error "Il serial '$Serial' passato con -Serial non e' tra i dispositivi connessi in stato 'device': $($connectedSerials -join ', ')"
        exit 1
    }
    $targetSerial = $Serial
}
else {
    # @(...) forza un array anche con un solo elemento: senza, [0] indicizzerebbe il primo
    # CARATTERE del serial.
    $allSerials = @($connectedSerials)
    if ($allSerials.Count -gt 1) {
        Write-Error "Piu' di un dispositivo connesso ($($allSerials -join ', ')) -- specificare quale usare con -Serial. Lo spike va eseguito sul telefono fisico collegato al DHU."
        exit 1
    }
    $targetSerial = $allSerials[0]
}
Write-Host "Dispositivo target selezionato: $targetSerial"

# --- 2. Installazione della variante ----------------------------------------------------------
if ($SkipInstall) {
    Write-Section "Installazione saltata (-SkipInstall)"
    Write-Warn "Si assume che sul telefono sia gia' installata la variante '$Variant' ($expectedFlavor, card=$expectedCard). Il controllo di coerenza sui log lo verifichera'."
}
else {
    Write-Section "Installazione variante '$Variant': gradlew $($gradleArgs -join ' ')"
    # ANDROID_SERIAL solo per il processo Gradle: installDebug rispetta questa variabile e
    # installa sul solo telefono selezionato. Ripristinata subito dopo.
    $previousAndroidSerial = $env:ANDROID_SERIAL
    $env:ANDROID_SERIAL = $targetSerial
    Push-Location $RepoRoot
    try {
        & .\gradlew.bat @gradleArgs
        $gradleExit = $LASTEXITCODE
    }
    finally {
        Pop-Location
        $env:ANDROID_SERIAL = $previousAndroidSerial
    }
    if ($gradleExit -ne 0) {
        Write-Error "Installazione fallita: gradlew $($gradleArgs -join ' ') ha restituito il codice $gradleExit."
        exit 1
    }
    Write-Host "Variante installata su $targetSerial."
}

# --- 3. Preparazione directory di output ------------------------------------------------------
if (-not [System.IO.Path]::IsPathRooted($OutputDir)) {
    $OutputDir = Join-Path $RepoRoot $OutputDir
}
if (-not (Test-Path -Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}
if (-not (Test-Path -Path $screenshotDir)) {
    New-Item -ItemType Directory -Path $screenshotDir -Force | Out-Null
}
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$logFile = Join-Path $OutputDir "$timestamp-$Variant-$Resolution.log"
$summaryFile = Join-Path $OutputDir "$timestamp-$Variant-$Resolution-summary.txt"

# --- 4. adb forward per il Desktop Head Unit --------------------------------------------------
Write-Section "adb forward tcp:5277 tcp:5277 (porta standard del Desktop Head Unit)"
& adb -s $targetSerial forward tcp:5277 tcp:5277
if ($LASTEXITCODE -ne 0) {
    # adb puo' riavviare il proprio server appena prima del comando, facendolo fallire: un
    # singolo retry dopo una breve pausa basta (osservato con dhu-quota-check.ps1).
    Write-Warn "Primo tentativo di 'adb forward' fallito (codice $LASTEXITCODE). Nuovo tentativo tra 3 secondi..."
    Start-Sleep -Seconds 3
    & adb -s $targetSerial forward tcp:5277 tcp:5277
    if ($LASTEXITCODE -ne 0) {
        Write-Error "adb forward tcp:5277 tcp:5277 fallito (codice $LASTEXITCODE) anche dopo un secondo tentativo."
        exit 1
    }
}
Write-Host "Forward attivo su tcp:5277."

# --- 5. Svuota logcat e avvia la cattura filtrata in background ------------------------------
Write-Section "Avvio cattura logcat filtrata sul tag $LogTag"
& adb -s $targetSerial logcat -c

# T-12-11: filtro stretto sul solo tag di geometria, nessun dato di posizione nei log.
$logcatProcess = Start-Process -FilePath "adb" `
    -ArgumentList @("-s", $targetSerial, "logcat", "-s", "$LogTag`:D") `
    -NoNewWindow -RedirectStandardOutput $logFile -PassThru
Write-Host "Cattura avviata (processo adb PID $($logcatProcess.Id)) -> $logFile"

# --- 6. Avvio manuale del DHU e screenshot (non automatizzabili) ------------------------------
Write-Section "Avvio manuale del Desktop Head Unit a $Resolution"
Write-Host "1. Chiudere eventuali finestre DHU gia' aperte (la risoluzione si legge solo all'avvio)."
Write-Host "2. Avviare il DHU con la config della risoluzione:"
Write-Host "     desktop-head-unit.exe -c `"$iniPath`"" -ForegroundColor Green
Write-Host "   (desktop-head-unit.exe si trova in <sdk>\extras\google\auto\)"
Write-Host "3. Aprire Tachimetro dalla lista app dell'head unit e attendere che compaiano i"
Write-Host "   contorni verde (stable area) e magenta (visible area) e il testo 'api=...'."
Write-Host "4. Fare lo screenshot della SOLA finestra DHU (Win+Shift+S o Strumento di cattura) e"
Write-Host "   salvarlo come:"
Write-Host "     $screenshotPath" -ForegroundColor Green
Write-Host "   (adb screencap cattura il telefono, non la finestra DHU: lo screenshot e' manuale.)"
Write-Host ""
Write-Host "Premere INVIO quando Tachimetro e' visibile sul DHU per avviare la cattura di $DurationSeconds s..."
[void](Read-Host)

# --- 7. Attesa con conteggio a video ----------------------------------------------------------
Write-Section "Cattura in corso: $DurationSeconds secondi (osservare l'action strip, D-08)"
for ($remaining = $DurationSeconds; $remaining -gt 0; $remaining--) {
    Write-Host -NoNewline "`r  Secondi rimanenti: $remaining   "
    Start-Sleep -Seconds 1
}
Write-Host ""

# --- 8. Arresto della cattura -----------------------------------------------------------------
Write-Section "Fine sessione, arresto della cattura logcat"
if (-not $logcatProcess.HasExited) {
    Stop-Process -Id $logcatProcess.Id -Force -ErrorAction SilentlyContinue
}
Start-Sleep -Seconds 1

# --- 9. Parsing: ultima riga di ogni callback e dell'ultimo frame ----------------------------
# Contratto di log del Piano 02 (tag TachimetroSurface):
#   onSurfaceAvailable w=<int> h=<int> dpi=<int> api=<int> variant=<flavor> card=<card>
#   onStableAreaChanged l=<int> t=<int> r=<int> b=<int>
#   onVisibleAreaChanged l=<int> t=<int> r=<int> b=<int>
#   frame area=<stable|surface> w=<int> h=<int> textSize=<int> digitH=<int>
$logLines = @()
if (Test-Path $logFile) { $logLines = @(Get-Content $logFile) }

$na = "n/d"
$surface = $null
$stable = $null
$visible = $null
$frame = $null
$destroyedCount = 0

foreach ($rawLine in $logLines) {
    if ($rawLine -match 'onSurfaceAvailable w=(\d+) h=(\d+) dpi=(\d+) api=(\d+) variant=(\S+) card=(\S+)') {
        $surface = @{ w = $Matches[1]; h = $Matches[2]; dpi = $Matches[3]; api = $Matches[4]; variant = $Matches[5]; card = $Matches[6] }
    }
    elseif ($rawLine -match 'onStableAreaChanged l=(-?\d+) t=(-?\d+) r=(-?\d+) b=(-?\d+)') {
        $stable = @{ l = $Matches[1]; t = $Matches[2]; r = $Matches[3]; b = $Matches[4] }
    }
    elseif ($rawLine -match 'onVisibleAreaChanged l=(-?\d+) t=(-?\d+) r=(-?\d+) b=(-?\d+)') {
        $visible = @{ l = $Matches[1]; t = $Matches[2]; r = $Matches[3]; b = $Matches[4] }
    }
    elseif ($rawLine -match 'frame area=(\S+) w=(\d+) h=(\d+) textSize=(\d+) digitH=(\d+)') {
        $frame = @{ area = $Matches[1]; w = $Matches[2]; h = $Matches[3]; textSize = $Matches[4]; digitH = $Matches[5] }
    }
    elseif ($rawLine -match 'onSurfaceDestroyed') {
        $destroyedCount++
    }
}

$warnings = New-Object System.Collections.Generic.List[string]

if ($surface) {
    $surfaceLabel = "$($surface.w)x$($surface.h)"
    $dpiLabel = $surface.dpi
    $apiLabel = $surface.api
    # Coerenza: se la Surface non ha la dimensione richiesta, il DHU non ha caricato l'ini.
    if ($surfaceLabel -ne $Resolution) {
        $warnings.Add("Surface $surfaceLabel diversa dalla risoluzione richiesta ${Resolution}: il DHU potrebbe non aver caricato $iniPath (riavviare il DHU con -c).")
    }
    if ($surface.variant -ne $expectedFlavor -or $surface.card -ne $expectedCard) {
        $warnings.Add("Variante nei log (variant=$($surface.variant) card=$($surface.card)) diversa da quella attesa ($expectedFlavor, card=$expectedCard): e' installata la build giusta?")
    }
}
else {
    $surfaceLabel = $na; $dpiLabel = $na; $apiLabel = $na
    $warnings.Add("Nessuna riga onSurfaceAvailable catturata: surface, dpi e api = $na.")
}

if ($stable) { $stableLabel = "$($stable.l),$($stable.t),$($stable.r),$($stable.b)" }
else { $stableLabel = $na; $warnings.Add("Nessuna riga onStableAreaChanged catturata: stable = $na.") }

if ($visible) { $visibleLabel = "$($visible.l),$($visible.t),$($visible.r),$($visible.b)" }
else { $visibleLabel = $na; $warnings.Add("Nessuna riga onVisibleAreaChanged catturata: visible = $na.") }

if ($frame) {
    $frameAreaLabel = "$($frame.area) $($frame.w)x$($frame.h)"
    $digitHLabel = $frame.digitH
    $textSizeLabel = $frame.textSize
    if ($frame.area -ne 'stable') {
        $warnings.Add("L'ultimo frame e' stato disegnato su area=$($frame.area) (stable area non ancora nota?): digitH potrebbe non essere confrontabile.")
    }
}
else {
    $frameAreaLabel = $na; $digitHLabel = $na; $textSizeLabel = $na
    $warnings.Add("Nessuna riga frame catturata: digitH = $na.")
}

if ($destroyedCount -gt 0) {
    $warnings.Add("onSurfaceDestroyed osservato $destroyedCount volta/e durante la cattura: l'host ha chiuso o ricreato la Surface.")
}

$screenshotPresent = Test-Path $screenshotPath
if (-not $screenshotPresent) {
    $warnings.Add("Screenshot non trovato: $screenshotPath (salvarlo prima di passare alla sessione successiva).")
}

# --- 10. Riepilogo: a video e su file UTF-8 ----------------------------------------------------
$tableHeader = "| Variante | Risoluzione | Surface | dpi | api | Stable (l,t,r,b) | Visible (l,t,r,b) | Frame area | digitH |"
$tableSeparator = "|----------|-------------|---------|-----|-----|------------------|-------------------|------------|--------|"
$tableRow = "| $Variant | $Resolution | $surfaceLabel | $dpiLabel | $apiLabel | $stableLabel | $visibleLabel | $frameAreaLabel | $digitHLabel |"

$summaryLines = New-Object System.Collections.Generic.List[string]
$summaryLines.Add("Riepilogo misura spike Surface (Fase 12, D-01/D-09/D-10)")
$summaryLines.Add("=========================================================")
$summaryLines.Add("App: $AppId  Dispositivo: $targetSerial  Durata cattura: $DurationSeconds s")
$summaryLines.Add("Variante: $Variant ($expectedFlavor, card=$expectedCard)  Risoluzione DHU: $Resolution")
$summaryLines.Add("Righe $LogTag catturate: $($logLines.Count)")
$summaryLines.Add("textSize ultimo frame: $textSizeLabel")
$summaryLines.Add("")
$summaryLines.Add("Riga da incollare in $PhaseDirRelative/12-SPIKE-RESULTS.md:")
$summaryLines.Add("")
$summaryLines.Add($tableHeader)
$summaryLines.Add($tableSeparator)
$summaryLines.Add($tableRow)
$summaryLines.Add("")
$summaryLines.Add("Screenshot atteso: $screenshotPath ($(if ($screenshotPresent) { 'presente' } else { 'MANCANTE' }))")
$summaryLines.Add("")
if ($warnings.Count -gt 0) {
    $summaryLines.Add("Avvisi:")
    foreach ($w in $warnings) { $summaryLines.Add("  - $w") }
}
else {
    $summaryLines.Add("Avvisi: nessuno.")
}
$summaryLines.Add("")
$summaryLines.Add("ATTENZIONE (D-10): queste misure NON sostituiscono la conferma umana e lo screenshot.")
$summaryLines.Add("Annotare a parte cosa l'host sovrappone (card POI, action strip) e se/quando l'action")
$summaryLines.Add("strip scompare (D-08).")

$summaryText = ($summaryLines -join "`n")

Write-Section "RIEPILOGO"
foreach ($line in $summaryLines) {
    if ($line -like "  - *") { Write-Host $line -ForegroundColor Yellow }
    else { Write-Host $line }
}
$summaryText | Out-File -FilePath $summaryFile -Encoding utf8

Write-Host ""
Write-Host "Riepilogo salvato in: $summaryFile"
Write-Host "Cattura logcat completa salvata in: $logFile"
