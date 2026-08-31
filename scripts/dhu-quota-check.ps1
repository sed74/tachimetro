<#
.SYNOPSIS
    D-06/D-08/D-10: misura riproducibile della cadenza di refresh del PaneTemplate di
    SpeedScreen durante una sessione DHU continua, e rilevamento della chiusura del
    processo da parte dell'host Android Auto per superamento della quota di refresh dei
    template non-Surface (Pitfall 2, .planning/research/PITFALLS.md).

.DESCRIPTION
    Vedi docs/dhu-quota-verification.md per il runbook completo (prerequisiti, procedura
    passo-passo, criteri di esito, contingenza D-07). Questo script automatizza tutto cio'
    che non richiede occhi umani sullo schermo del Desktop Head Unit (DHU): apre il forward
    di porta richiesto dal DHU, cattura logcat filtrata sul tag TachimetroCar emesso da
    SpeedScreen, campiona periodicamente il PID del processo com.sed.tachimetro, e stampa un
    esito euristico PASS/FAIL/INCONCLUSIVO.

    NON installa e non scarica nulla: usa esclusivamente adb dall'Android SDK locale gia'
    presente sul PATH. L'avvio del Desktop Head Unit e la selezione dell'app Tachimetro
    nella lista app dell'head unit NON sono automatizzabili da script (nessuna API adb per
    farlo) -- lo script si ferma e attende un INVIO dopo aver stampato l'istruzione.

    ATTENZIONE (D-10): l'esito euristico stampato da questo script NON sostituisce la
    conferma umana richiesta dal Task 3 del piano di esecuzione. Il rendering visivo sullo
    schermo del DHU e il comportamento dell'host (ritorno alla lista app, messaggi di
    errore) non sono verificabili da codice/log -- solo una persona che osserva lo schermo
    puo' confermarli.

.PARAMETER DurationSeconds
    Durata della sessione di misura in secondi. Default 600 (10 minuti), l'estremo alto
    della finestra 5-10 minuti richiesta da D-08.

.PARAMETER OutputDir
    Directory dove salvare la cattura logcat e il file di riepilogo. Default
    build/dhu-quota, gia' ignorata da git tramite la regola /build in .gitignore
    (verificato con `git check-ignore`) -- i log catturati non devono entrare nel
    repository (T-08-12).

.PARAMETER Serial
    Serial adb del dispositivo/emulatore su cui eseguire la verifica (es. "emulator-5554").
    Facoltativo: se omesso, lo script seleziona automaticamente l'unico dispositivo il cui
    serial inizia per "emulator-" tra quelli connessi (D-09, la verifica gira su AVD, non su
    un telefono fisico). Necessario esplicitamente se sono connessi piu' emulatori
    contemporaneamente, o per puntare a un device diverso dal default automatico.

.EXAMPLE
    powershell -File scripts/dhu-quota-check.ps1 -DurationSeconds 600
#>
[CmdletBinding()]
param(
    [int]$DurationSeconds = 600,
    [string]$OutputDir = "build/dhu-quota",
    [string]$Serial = ""
)

$ErrorActionPreference = 'Stop'

# applicationId di app/build.gradle.kts -- valore letterale, non caricato dinamicamente:
# lo script non deve dipendere da un parsing del build script per restare semplice e
# ispezionabile a occhio.
$AppId = "com.sed.tachimetro"
$LogTag = "TachimetroCar"
$SampleIntervalSeconds = 10
$StallFailThresholdSeconds = 30
$PassCadenceMin = 0.8
$PassCadenceMax = 1.2

function Write-Section {
    param([string]$Text)
    Write-Host ""
    Write-Host "=== $Text ===" -ForegroundColor Cyan
}

# --- 1. Verifica adb e selezione del dispositivo target --------------------------------------
Write-Section "Verifica adb e selezione del dispositivo"

$adbCmd = Get-Command adb -ErrorAction SilentlyContinue
if (-not $adbCmd) {
    Write-Error "adb non trovato sul PATH. Verificare che platform-tools dell'Android SDK sia nel PATH."
    exit 1
}

$devicesOutput = & adb devices
$deviceLines = $devicesOutput | Select-String -Pattern "\tdevice$"
if (-not $deviceLines -or $deviceLines.Count -eq 0) {
    Write-Error "Nessun dispositivo/emulatore in stato 'device'. Output di 'adb devices':`n$devicesOutput"
    exit 1
}
Write-Host "Dispositivo/i in stato 'device' trovato/i:"
$connectedSerials = New-Object System.Collections.Generic.List[string]
foreach ($deviceLine in $deviceLines) {
    Write-Host "  $deviceLine"
    $serialToken = ($deviceLine.Line -split '\s+')[0]
    $connectedSerials.Add($serialToken)
}

# Selezione del dispositivo target: un comando 'adb' senza '-s' fallisce con
# "more than one device/emulator" appena piu' di un device/emulatore e' connesso -- una
# situazione realistica su questa macchina di sviluppo, dove un telefono fisico resta spesso
# collegato via USB per la verifica delle fasi precedenti (v1.0/v1.1). Ogni comando adb
# successivo in questo script va quindi sempre targettizzato esplicitamente con -s.
if ($Serial) {
    if (-not ($connectedSerials -contains $Serial)) {
        Write-Error "Il serial '$Serial' passato con -Serial non e' tra i dispositivi connessi in stato 'device': $($connectedSerials -join ', ')"
        exit 1
    }
    $targetSerial = $Serial
}
else {
    # @(...) forza un array anche con un solo match: senza, Where-Object un solo risultato lo
    # "srotola" a stringa scalare e [0] indicizzerebbe il primo CARATTERE del serial invece
    # del serial intero (bug osservato in fase di test manuale di questo script).
    $emulatorSerials = @($connectedSerials | Where-Object { $_ -like 'emulator-*' })
    if ($emulatorSerials.Count -eq 0) {
        Write-Error "Nessun emulatore (serial 'emulator-*') tra i dispositivi connessi -- D-09 richiede un AVD, non un telefono fisico. Avviare l'AVD prima di rilanciare lo script, oppure passare -Serial esplicitamente. Connessi: $($connectedSerials -join ', ')"
        exit 1
    }
    if ($emulatorSerials.Count -gt 1) {
        Write-Error "Piu' di un emulatore connesso ($($emulatorSerials -join ', ')) -- specificare quale usare con -Serial."
        exit 1
    }
    $targetSerial = $emulatorSerials[0]
}
Write-Host "Dispositivo target selezionato: $targetSerial"

# --- 2. Preparazione directory di output --------------------------------------------------
if (-not (Test-Path -Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$logFile = Join-Path $OutputDir "dhu-quota-$timestamp.log"
$summaryFile = Join-Path $OutputDir "dhu-quota-$timestamp-summary.txt"

# --- 3. adb forward per il Desktop Head Unit ------------------------------------------------
Write-Section "adb forward tcp:5277 tcp:5277 (porta standard richiesta dal Desktop Head Unit)"
& adb -s $targetSerial forward tcp:5277 tcp:5277
if ($LASTEXITCODE -ne 0) {
    # adb puo' auto-riavviare il proprio server in background ("adb server is out of date,
    # killing...") appena prima di eseguire un comando, facendo fallire quello stesso comando
    # con la vecchia connessione (osservato in fase di test manuale di questo script). Un
    # singolo retry dopo una breve pausa e' sufficiente perche' il nuovo server si stabilizzi.
    Write-Host "Primo tentativo di 'adb forward' fallito (codice $LASTEXITCODE) -- possibile riavvio del server adb in corso. Nuovo tentativo tra 3 secondi..." -ForegroundColor Yellow
    Start-Sleep -Seconds 3
    & adb -s $targetSerial forward tcp:5277 tcp:5277
    if ($LASTEXITCODE -ne 0) {
        Write-Error "adb forward tcp:5277 tcp:5277 fallito (codice $LASTEXITCODE) anche dopo un secondo tentativo."
        exit 1
    }
}
Write-Host "Forward attivo su tcp:5277."

# --- 4. Svuota il buffer logcat e avvia la cattura filtrata in background -------------------
Write-Section "Avvio cattura logcat filtrata sul tag $LogTag"
& adb -s $targetSerial logcat -c

$logcatProcess = Start-Process -FilePath "adb" `
    -ArgumentList @("-s", $targetSerial, "logcat", "-s", "$LogTag`:D") `
    -NoNewWindow -RedirectStandardOutput $logFile -PassThru
Write-Host "Cattura avviata (processo adb PID $($logcatProcess.Id)) -> $logFile"

# --- 5. Avvio manuale del DHU (non automatizzabile) -----------------------------------------
Write-Section "Avvio manuale del Desktop Head Unit"
Write-Host "Avviare ora il Desktop Head Unit (desktop-head-unit.exe, dall'SDK, extras\google\auto\)"
Write-Host "e selezionare 'Tachimetro' nella lista app dell'head unit."
Write-Host ""
Write-Host "Premere INVIO quando l'app Tachimetro e' visibile sullo schermo del DHU per avviare il cronometro..."
[void](Read-Host)

# --- 6. Campionamento periodico del PID durante la sessione ---------------------------------
Write-Section "Sessione di misura: $DurationSeconds secondi (campionamento ogni $SampleIntervalSeconds s)"

$startTime = Get-Date
$events = New-Object System.Collections.Generic.List[string]
$initialPid = $null
$lastPid = $null
$elapsedSeconds = 0

while ($elapsedSeconds -lt $DurationSeconds) {
    # campiona il PID del processo dell'app: adb shell pidof com.sed.tachimetro
    # $ErrorActionPreference='Stop' (impostato all'inizio dello script) promuoverebbe
    # qualunque riga stderr transitoria di adb (es. "device offline" durante un momentaneo
    # blip di connessione, osservato in fase di test manuale di questo script) a eccezione
    # fatale, interrompendo l'intera sessione di 5-10 minuti per un singolo campione fallito.
    # Abbassato localmente solo per questa chiamata: un campione fallito va trattato come
    # "nessun PID rilevato in questo giro", non come un errore che interrompe lo script.
    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = 'SilentlyContinue'
    $currentPidRaw = & adb -s $targetSerial shell pidof com.sed.tachimetro 2>$null
    $ErrorActionPreference = $previousErrorActionPreference

    # Un PID valido e' sempre puramente numerico. Accettare solo token che rispettano
    # ^\d+$ scarta testo spurio che a volte finisce nell'output dello stesso comando (es. il
    # banner "adb server is out of date. killing..." quando adb riavvia il proprio server in
    # background esattamente durante questa chiamata, osservato in fase di test manuale di
    # questo script) -- senza questa validazione un banner del genere verrebbe scambiato per
    # un PID reale.
    $currentPid = $null
    if ($currentPidRaw) {
        $numericToken = ($currentPidRaw -split '\s+') | Where-Object { $_ -match '^\d+$' } | Select-Object -First 1
        if ($numericToken) { $currentPid = $numericToken.Trim() }
    }
    $sampleTimeLabel = Get-Date -Format "HH:mm:ss"

    if (($null -eq $initialPid) -and $currentPid) {
        $initialPid = $currentPid
        $lastPid = $currentPid
        Write-Host "[$sampleTimeLabel] PID iniziale rilevato: $currentPid"
    }
    elseif ($currentPid -ne $lastPid) {
        $eventText = "[$sampleTimeLabel] Evento: PID cambiato/sparito (era '$lastPid', ora '$currentPid')"
        Write-Host $eventText -ForegroundColor Yellow
        $events.Add($eventText)
        $lastPid = $currentPid
    }
    else {
        Write-Host "[$sampleTimeLabel] PID $currentPid ancora attivo (t=+${elapsedSeconds}s)"
    }

    Start-Sleep -Seconds $SampleIntervalSeconds
    $elapsedSeconds = [int]((Get-Date) - $startTime).TotalSeconds
}

$finalPid = $lastPid
$actualDurationSeconds = [int]((Get-Date) - $startTime).TotalSeconds

# --- 7. Ferma la cattura logcat ---------------------------------------------------------------
Write-Section "Fine sessione, arresto della cattura logcat"
if (-not $logcatProcess.HasExited) {
    Stop-Process -Id $logcatProcess.Id -Force -ErrorAction SilentlyContinue
}
Start-Sleep -Seconds 1

# --- 8. Analisi del log catturato: righe "onGetTemplate #<n>" e stalli -------------------------
$logLines = @()
if (Test-Path $logFile) { $logLines = Get-Content $logFile }

$refreshCount = 0
$maxCounter = 0
$refreshTimestamps = New-Object System.Collections.Generic.List[datetime]

foreach ($rawLine in $logLines) {
    if ($rawLine -match 'onGetTemplate #(\d+)') {
        $refreshCount++
        $counterValue = [int]$Matches[1]
        if ($counterValue -gt $maxCounter) { $maxCounter = $counterValue }

        # logcat -v default include un timestamp "MM-dd HH:mm:ss.SSS" a inizio riga -- usato
        # solo per rilevare stalli prolungati tra refresh consecutivi, non come fonte del
        # contatore (che viene letto dal valore #<n> emesso da SpeedScreen stesso).
        if ($rawLine -match '^(\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\.\d{3})') {
            $tsText = $Matches[1]
            $parsedTs = [datetime]::MinValue
            $parsedOk = [datetime]::TryParseExact(
                "$(Get-Date -Format yyyy)-$tsText",
                "yyyy-MM-dd HH:mm:ss.fff",
                [System.Globalization.CultureInfo]::InvariantCulture,
                [System.Globalization.DateTimeStyles]::None,
                [ref]$parsedTs
            )
            if ($parsedOk) { $refreshTimestamps.Add($parsedTs) }
        }
    }
}

$avgCadence = 0
if ($actualDurationSeconds -gt 0) {
    $avgCadence = [math]::Round($refreshCount / $actualDurationSeconds, 3)
}

# Gap massimo tra refresh consecutivi catturati -- usato per la soglia di stallo (>30s).
$maxGapSeconds = 0
if ($refreshTimestamps.Count -ge 2) {
    for ($i = 1; $i -lt $refreshTimestamps.Count; $i++) {
        $gapSeconds = ($refreshTimestamps[$i] - $refreshTimestamps[$i - 1]).TotalSeconds
        if ($gapSeconds -gt $maxGapSeconds) { $maxGapSeconds = $gapSeconds }
    }
}

# --- 9. Esito euristico PASS/FAIL/INCONCLUSIVO -------------------------------------------------
# FAIL: il PID e' sparito/cambiato durante la sessione, oppure i refresh si sono fermati per
#       piu' di StallFailThresholdSeconds secondi consecutivi prima della fine.
# PASS: il processo e' rimasto vivo (PID iniziale rilevato, nessun evento di terminazione) per
#       tutta la durata E la cadenza media e' compresa tra PassCadenceMin e PassCadenceMax.
# INCONCLUSIVO: ogni altro caso (es. PID mai rilevato ma nessun evento di cambio, cadenza fuori
#       soglia senza terminazione del processo).
$pidTerminated = ($events.Count -gt 0)
$stalled = ($maxGapSeconds -gt $StallFailThresholdSeconds)

if ($pidTerminated -or $stalled) {
    $verdict = "FAIL"
}
elseif (($null -ne $initialPid) -and (-not $pidTerminated) -and ($avgCadence -ge $PassCadenceMin) -and ($avgCadence -le $PassCadenceMax)) {
    $verdict = "PASS"
}
else {
    $verdict = "INCONCLUSIVO"
}

# --- 10. Riepilogo: stampato a video e salvato su file ------------------------------------------
$initialPidLabel = "MAI RILEVATO"
if ($initialPid) { $initialPidLabel = $initialPid }
$finalPidLabel = "MAI RILEVATO"
if ($finalPid) { $finalPidLabel = $finalPid }

$summaryLines = New-Object System.Collections.Generic.List[string]
$summaryLines.Add("Riepilogo verifica quota refresh Android Auto (D-06/D-08/D-10)")
$summaryLines.Add("================================================================")
$summaryLines.Add("Durata effettiva: $actualDurationSeconds secondi (richiesti >= 300s dalla finestra 5-10 min di D-08)")
$summaryLines.Add("Righe 'onGetTemplate #' catturate: $refreshCount")
$summaryLines.Add("Valore piu' alto del contatore #<n> osservato: $maxCounter")
$summaryLines.Add("Cadenza media: $avgCadence refresh/secondo")
$summaryLines.Add("Gap massimo osservato tra refresh consecutivi: $([math]::Round($maxGapSeconds, 1)) secondi")
$summaryLines.Add("PID iniziale: $initialPidLabel")
$summaryLines.Add("PID finale: $finalPidLabel")
$summaryLines.Add("Eventi di terminazione/cambio PID: $($events.Count)")
foreach ($eventText in $events) { $summaryLines.Add("  $eventText") }
$summaryLines.Add("")
$summaryLines.Add("ESITO EURISTICO: $verdict")
$summaryLines.Add("Soglie: FAIL se il PID e' sparito/cambiato oppure se i refresh si sono fermati per piu' di")
$summaryLines.Add("$StallFailThresholdSeconds secondi consecutivi prima della fine; PASS se il processo e' rimasto vivo per")
$summaryLines.Add("tutta la durata e la cadenza media e' tra $PassCadenceMin e $PassCadenceMax refresh/s; INCONCLUSIVO altrimenti.")
$summaryLines.Add("")
$summaryLines.Add("ATTENZIONE (D-10): questo esito euristico NON sostituisce la conferma umana richiesta dal")
$summaryLines.Add("Task 3 -- il rendering visivo sullo schermo DHU e il comportamento dell'host (chiusura")
$summaryLines.Add("improvvisa, messaggi di errore) non sono verificabili da codice/log. Allegare questo")
$summaryLines.Add("riepilogo alla risposta del checkpoint umano.")

$summaryText = ($summaryLines -join "`n")

Write-Section "RIEPILOGO"
Write-Host $summaryText
$summaryText | Out-File -FilePath $summaryFile -Encoding utf8

Write-Host ""
Write-Host "Riepilogo salvato in: $summaryFile"
Write-Host "Cattura logcat completa salvata in: $logFile"
