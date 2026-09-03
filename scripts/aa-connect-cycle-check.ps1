<#
.SYNOPSIS
    D-05/D-07: misura riproducibile di crash, ANR e rifiuti dell'host durante cicli rapidi di
    connessione/disconnessione di Android Auto -- il Success Criterion 3 della Fase 11 nella
    ROADMAP ("Connettendo e disconnettendo Android Auto ripetutamente in rapida successione,
    l'app non va in crash e lo schermo auto non resta bloccato in uno stato incoerente").

.DESCRIPTION
    Vedi docs/android-auto-hardening-verification.md per il runbook completo (prerequisiti,
    procedura passo-passo, criteri di esito, contingenze). Questo script automatizza tutto cio'
    che non richiede occhi umani sullo schermo dell'head unit: abilita i Log.d di validazione
    host del tag CarApp.Val, apre il forward di porta richiesto dal Desktop Head Unit, cattura
    logcat per l'intera sessione, campiona il PID del processo com.sed.tachimetro a ogni ciclo,
    e stampa un esito euristico PASS/FAIL/INCONCLUSIVO.

    NON installa e non scarica nulla: usa esclusivamente adb dall'Android SDK locale gia'
    presente sul PATH. Il collegamento e lo scollegamento fisico del cavo (o l'avvio/arresto del
    Desktop Head Unit) e l'osservazione dello schermo auto NON sono automatizzabili da script
    (nessuna API adb per farlo) -- lo script si ferma e attende un INVIO a ogni ciclo.

    ATTENZIONE (D-05/D-07): l'esito euristico stampato da questo script NON sostituisce la
    conferma umana richiesta dal checkpoint del Piano 03. Il rendering visivo sullo schermo
    dell'head unit e il comportamento dell'host (schermata vuota, valore congelato, messaggi di
    errore) non sono verificabili da codice/log -- solo una persona che osserva lo schermo puo'
    confermarli. I due esiti vanno registrati entrambi, nessuno dei due sostituisce l'altro.

    Il binario da usare per SC3 e' il build di RELEASE firmato: e' quello che si spedisce, ed
    esercita l'HostValidator reale a ogni ri-bind. Un build di debug produrrebbe un falso PASS
    sulla parte di validazione host (vedi la riga "Accepted - Validator disabled" nel riepilogo).

.PARAMETER Cycles
    Numero di cicli di connessione/disconnessione rapida da eseguire. Default 10, il valore
    indicato dal runbook per una sessione da scrivania di durata ragionevole.

.PARAMETER OutputDir
    Directory dove salvare la cattura logcat e il file di riepilogo. Default
    build/aa-hardening, gia' ignorata da git tramite la regola /build in .gitignore
    (verificato con `git check-ignore`) -- i log catturati, che possono contenere valori di
    velocita' e identificatori di dispositivo, non devono entrare nel repository (T-11-06).

.PARAMETER Serial
    Serial adb del dispositivo su cui eseguire la verifica. Facoltativo: se omesso, lo script
    seleziona automaticamente l'unico dispositivo connesso in stato 'device'. A differenza di
    scripts/dhu-quota-check.ps1 NON viene data alcuna preferenza ai serial 'emulator-': SC3 gira
    su un telefono fisico collegato a un Desktop Head Unit o a un head unit reale.

.EXAMPLE
    powershell -File scripts/aa-connect-cycle-check.ps1 -Cycles 10
#>
[CmdletBinding()]
param(
    [int]$Cycles = 10,
    [string]$OutputDir = "build/aa-hardening",
    [string]$Serial = ""
)

$ErrorActionPreference = 'Stop'

# applicationId di app/build.gradle.kts -- valore letterale, non caricato dinamicamente:
# lo script non deve dipendere da un parsing del build script per restare semplice e
# ispezionabile a occhio (stessa motivazione di scripts/dhu-quota-check.ps1).
$AppId = "com.sed.tachimetro"

# Tag di androidx/car/car-app/utils/LogTags.java (TAG_HOST_VALIDATION): e' il tag su cui la
# Car App Library emette l'esito della validazione dell'host a ogni bind.
$HostValidationTag = "CarApp.Val"

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

# Selezione del dispositivo target: un comando 'adb' senza '-s' fallisce con
# "more than one device/emulator" appena piu' di un device e' connesso -- situazione realistica
# su questa macchina di sviluppo. Ogni comando adb successivo va sempre targettizzato con -s.
if ($Serial) {
    if (-not ($connectedSerials -contains $Serial)) {
        Write-Error "Il serial '$Serial' passato con -Serial non e' tra i dispositivi connessi in stato 'device': $($connectedSerials -join ', ')"
        exit 1
    }
    $targetSerial = $Serial
}
else {
    # @(...) forza un array anche con un solo elemento: senza, un solo risultato verrebbe
    # "srotolato" a stringa scalare e [0] indicizzerebbe il primo CARATTERE del serial.
    $allSerials = @($connectedSerials)
    if ($allSerials.Count -gt 1) {
        Write-Error "Piu' di un dispositivo connesso ($($allSerials -join ', ')) -- specificare quale usare con -Serial. SC3 va eseguito sul telefono fisico collegato all'head unit."
        exit 1
    }
    $targetSerial = $allSerials[0]
}
Write-Host "Dispositivo target selezionato: $targetSerial"

# --- 2. Preparazione directory di output ------------------------------------------------------
if (-not (Test-Path -Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$logFile = Join-Path $OutputDir "aa-connect-cycle-$timestamp.log"
$summaryFile = Join-Path $OutputDir "aa-connect-cycle-$timestamp-summary.txt"

# --- 3. Abilitazione dei Log.d di validazione host --------------------------------------------
Write-Section "Abilitazione dei Log.d del tag $HostValidationTag"
# Le due righe "Accepted - ..." della Car App Library sono dietro Log.isLoggable(tag, DEBUG):
# senza questa proprieta' NON verrebbero mai emesse e la sessione risulterebbe INCONCLUSIVA
# sulla parte di validazione host. La riga di rifiuto e' invece un Log.w, sempre emesso.
& adb -s $targetSerial shell setprop log.tag.CarApp.Val DEBUG
Write-Host "Eseguito: adb -s $targetSerial shell setprop log.tag.CarApp.Val DEBUG"
Write-Host "(senza questa proprieta' le righe 'Accepted - ...' non verrebbero emesse dall'host)"

# --- 4. adb forward per il Desktop Head Unit --------------------------------------------------
Write-Section "adb forward tcp:5277 tcp:5277 (porta standard richiesta dal Desktop Head Unit)"
& adb -s $targetSerial forward tcp:5277 tcp:5277
if ($LASTEXITCODE -ne 0) {
    # adb puo' auto-riavviare il proprio server in background appena prima di eseguire un
    # comando, facendo fallire quello stesso comando con la vecchia connessione. Un singolo
    # retry dopo una breve pausa e' sufficiente perche' il nuovo server si stabilizzi.
    Write-Host "Primo tentativo di 'adb forward' fallito (codice $LASTEXITCODE) -- possibile riavvio del server adb in corso. Nuovo tentativo tra 3 secondi..." -ForegroundColor Yellow
    Start-Sleep -Seconds 3
    & adb -s $targetSerial forward tcp:5277 tcp:5277
    if ($LASTEXITCODE -ne 0) {
        # Il forward serve SOLO al Desktop Head Unit. Se la sessione usa un head unit reale
        # (cavo verso l'auto) il forward e' semplicemente inutile, non un errore: si segnala e
        # si prosegue, invece di far fallire una verifica perfettamente valida.
        Write-Host "adb forward non riuscito (codice $LASTEXITCODE). Innocuo se la sessione usa un head unit REALE invece del Desktop Head Unit: si prosegue." -ForegroundColor Yellow
    }
    else {
        Write-Host "Forward attivo su tcp:5277."
    }
}
else {
    Write-Host "Forward attivo su tcp:5277."
}

# --- 5. Svuota il buffer logcat e avvia la cattura in background -------------------------------
Write-Section "Avvio cattura logcat (tag $HostValidationTag a livello DEBUG, tutto il resto da WARN in su)"
& adb -s $targetSerial logcat -c

# CarApp.Val:D cattura l'esito della validazione host; *:W cattura le righe di crash
# (FATAL EXCEPTION) e di ANR, che sono emesse a livello ERROR e non filtrate da un tag singolo.
$logcatProcess = Start-Process -FilePath "adb" `
    -ArgumentList @("-s", $targetSerial, "logcat", "-v", "time", "$HostValidationTag`:D", "*:W") `
    -NoNewWindow -RedirectStandardOutput $logFile -PassThru
Write-Host "Cattura avviata (processo adb PID $($logcatProcess.Id)) -> $logFile"

# --- 6. Campionamento del PID iniziale ---------------------------------------------------------
function Get-AppPid {
    param([string]$DeviceSerial)
    # $ErrorActionPreference='Stop' promuoverebbe qualunque riga stderr transitoria di adb
    # (es. "device offline" durante lo scollegamento del cavo, che in questo test succede a ogni
    # ciclo per costruzione) a eccezione fatale. Abbassato localmente: un campione fallito e'
    # "nessun PID rilevato", non un errore che interrompe la sessione.
    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = 'SilentlyContinue'
    $rawOutput = & adb -s $DeviceSerial shell pidof com.sed.tachimetro 2>$null
    $ErrorActionPreference = $previousErrorActionPreference

    # Un PID valido e' sempre puramente numerico: accettare solo token ^\d+$ scarta testo spurio
    # che a volte finisce nello stesso output (es. il banner di riavvio del server adb).
    if ($rawOutput) {
        $numericToken = ($rawOutput -split '\s+') | Where-Object { $_ -match '^\d+$' } | Select-Object -First 1
        if ($numericToken) { return $numericToken.Trim() }
    }
    return $null
}

Write-Section "Campionamento del PID iniziale di $AppId"
$initialPid = Get-AppPid -DeviceSerial $targetSerial
$initialPidLabel = "MAI RILEVATO"
if ($initialPid) { $initialPidLabel = $initialPid }
Write-Host "PID iniziale: $initialPidLabel"

# --- 7. Cicli di connessione/disconnessione rapida ---------------------------------------------
Write-Section "Sessione di misura: $Cycles cicli di connessione/disconnessione rapida"
Write-Host "A ogni ciclo: collegare Android Auto, attendere che Tachimetro sia visibile sullo"
Write-Host "schermo auto, poi scollegare SUBITO. Osservare lo schermo auto a ogni riconnessione."
Write-Host "Premere INVIO per confermare il ciclo, oppure scrivere 'stop' per interrompere."

$cyclePidSamples = New-Object System.Collections.Generic.List[string]
$completedCycles = 0
$aborted = $false

for ($cycleIndex = 1; $cycleIndex -le $Cycles; $cycleIndex++) {
    Write-Host ""
    Write-Host "--- Ciclo $cycleIndex di $Cycles ---" -ForegroundColor Green
    Write-Host "Collegare Android Auto, attendere che Tachimetro sia visibile sullo schermo auto, poi scollegare subito."
    $answer = Read-Host "Premere INVIO a ciclo completato (oppure 'stop' per interrompere)"
    if ($answer -and $answer.Trim().ToLower() -eq 'stop') {
        Write-Host "Sessione interrotta dall'operatore al ciclo $cycleIndex." -ForegroundColor Yellow
        $aborted = $true
        break
    }

    # NOTA sul PID: un cambio di PID fra un ciclo e l'altro NON e' di per se' un FAIL. Alla
    # disconnessione il sistema puo' legittimamente terminare il processo se l'app non e' in
    # primo piano sul telefono -- e' comportamento normale di Android, non un crash. Il segnale
    # di crash e' 'FATAL EXCEPTION'/ANR nel log, non il PID. Il PID viene comunque registrato
    # perche' e' il contesto che serve a un umano per interpretare un log ambiguo (es. per
    # distinguere "processo riavviato pulito" da "processo morto subito dopo un'eccezione").
    $cyclePid = Get-AppPid -DeviceSerial $targetSerial
    $cyclePidLabel = "MAI RILEVATO"
    if ($cyclePid) { $cyclePidLabel = $cyclePid }
    $cyclePidSamples.Add("Ciclo $cycleIndex : PID $cyclePidLabel")
    Write-Host "Ciclo $cycleIndex completato -- PID campionato: $cyclePidLabel"
    $completedCycles = $cycleIndex
}

# --- 8. Arresto della cattura logcat -----------------------------------------------------------
Write-Section "Fine sessione, arresto della cattura logcat"
if (-not $logcatProcess.HasExited) {
    Stop-Process -Id $logcatProcess.Id -Force -ErrorAction SilentlyContinue
}
Start-Sleep -Seconds 1

# --- 9. Analisi del log catturato --------------------------------------------------------------
$logLines = @()
if (Test-Path $logFile) { $logLines = Get-Content $logFile }

$fatalCount = 0            # crash: 'FATAL EXCEPTION'
$anrCount = 0              # ANR: 'ANR in com.sed.tachimetro'
$rejectedCount = 0         # host rifiutato dal validator: 'Rejected -' sul tag CarApp.Val
$acceptedAllowListCount = 0 # 'Accepted - Host in allow-list'  -> build di RELEASE in esecuzione
$acceptedDisabledCount = 0  # 'Accepted - Validator disabled'  -> build di DEBUG in esecuzione

foreach ($rawLine in $logLines) {
    if ($rawLine -match 'FATAL EXCEPTION') { $fatalCount++ }
    if ($rawLine -match 'ANR in com\.sed\.tachimetro') { $anrCount++ }
    if ($rawLine -match [regex]::Escape($HostValidationTag)) {
        if ($rawLine -match 'Rejected -') { $rejectedCount++ }
        if ($rawLine -match 'Accepted - Host in allow-list') { $acceptedAllowListCount++ }
        if ($rawLine -match 'Accepted - Validator disabled') { $acceptedDisabledCount++ }
    }
}

# Quale binario stava girando: e' il segnale che distingue oggettivamente release da debug e
# impedisce di registrare come valida una sessione eseguita sul binario sbagliato (T-11-09).
$buildInEvidence = "NON DETERMINABILE (nessuna riga 'Accepted - ...' catturata: setprop non applicato, oppure nessuna connessione riuscita)"
if ($acceptedDisabledCount -gt 0 -and $acceptedAllowListCount -eq 0) {
    $buildInEvidence = "DEBUG (validator permissivo: 'Accepted - Validator disabled')"
}
elseif ($acceptedAllowListCount -gt 0 -and $acceptedDisabledCount -eq 0) {
    $buildInEvidence = "RELEASE (allow-list reale: 'Accepted - Host in allow-list')"
}
elseif ($acceptedAllowListCount -gt 0 -and $acceptedDisabledCount -gt 0) {
    $buildInEvidence = "AMBIGUO (catturate righe di entrambi i rami: probabile cambio di build a meta' sessione)"
}

# --- 10. Esito euristico PASS/FAIL/INCONCLUSIVO ------------------------------------------------
# FAIL: almeno un crash, un ANR o un rifiuto dell'host durante la sessione.
# PASS: zero crash, zero ANR, zero rifiuti E tutti i cicli richiesti completati.
# INCONCLUSIVO: ogni altro caso (tipicamente sessione interrotta prima della fine).
if (($fatalCount -gt 0) -or ($anrCount -gt 0) -or ($rejectedCount -gt 0)) {
    $verdict = "FAIL"
}
elseif (($fatalCount -eq 0) -and ($anrCount -eq 0) -and ($rejectedCount -eq 0) -and ($completedCycles -eq $Cycles)) {
    $verdict = "PASS"
}
else {
    $verdict = "INCONCLUSIVO"
}

# --- 11. Riepilogo: stampato a video e salvato su file -----------------------------------------
$summaryLines = New-Object System.Collections.Generic.List[string]
$summaryLines.Add("Riepilogo verifica cicli rapidi di connessione/disconnessione Android Auto (SC3, D-05/D-07)")
$summaryLines.Add("==========================================================================================")
$summaryLines.Add("Dispositivo (serial adb): $targetSerial")
$summaryLines.Add("Applicazione sotto test: $AppId")
$summaryLines.Add("Cicli richiesti: $Cycles")
$summaryLines.Add("Cicli completati: $completedCycles")
if ($aborted) { $summaryLines.Add("Sessione INTERROTTA dall'operatore prima del completamento.") }
$summaryLines.Add("PID iniziale: $initialPidLabel")
$summaryLines.Add("PID campionato a fine di ogni ciclo:")
if ($cyclePidSamples.Count -eq 0) {
    $summaryLines.Add("  (nessun ciclo completato)")
}
else {
    foreach ($sample in $cyclePidSamples) { $summaryLines.Add("  $sample") }
}
$summaryLines.Add("Nota: un cambio di PID tra un ciclo e l'altro NON e' di per se' un FAIL -- alla")
$summaryLines.Add("disconnessione il sistema puo' legittimamente terminare il processo. Il segnale di")
$summaryLines.Add("crash e' 'FATAL EXCEPTION'/ANR, non il PID.")
$summaryLines.Add("")
$summaryLines.Add("Conteggi sul log catturato:")
$summaryLines.Add("  'FATAL EXCEPTION' (crash): $fatalCount")
$summaryLines.Add("  'ANR in com.sed.tachimetro': $anrCount")
$summaryLines.Add("  'Rejected -' su tag $HostValidationTag (host rifiutato): $rejectedCount")
$summaryLines.Add("  'Accepted - Host in allow-list' (build di release): $acceptedAllowListCount")
$summaryLines.Add("  'Accepted - Validator disabled' (build di debug): $acceptedDisabledCount")
$summaryLines.Add("Build in esecuzione secondo il log: $buildInEvidence")
$summaryLines.Add("")
$summaryLines.Add("ESITO EURISTICO: $verdict")
$summaryLines.Add("Soglie: FAIL se 'FATAL EXCEPTION' > 0 oppure 'ANR in com.sed.tachimetro' > 0 oppure")
$summaryLines.Add("'Rejected -' > 0; PASS se tutti e tre i conteggi sono 0 e sono stati completati tutti i")
$summaryLines.Add("$Cycles cicli richiesti; INCONCLUSIVO in ogni altro caso.")
$summaryLines.Add("")
$summaryLines.Add("ATTENZIONE (D-05/D-07): questo esito euristico NON sostituisce la conferma umana")
$summaryLines.Add("richiesta dal checkpoint del Piano 03 -- schermata vuota, valore congelato o messaggi di")
$summaryLines.Add("errore dell'host non sono verificabili da codice/log. Allegare questo riepilogo alla")
$summaryLines.Add("risposta del checkpoint umano e registrare entrambi gli esiti in")
$summaryLines.Add("docs/android-auto-hardening-verification.md (sezione 'Esiti registrati').")
$summaryLines.Add("")
$summaryLines.Add("Cattura logcat completa: $logFile")

$summaryText = ($summaryLines -join "`n")

Write-Section "RIEPILOGO"
Write-Host $summaryText
$summaryText | Out-File -FilePath $summaryFile -Encoding utf8

Write-Host ""
Write-Host "Riepilogo salvato in: $summaryFile"
Write-Host "Cattura logcat completa salvata in: $logFile"
