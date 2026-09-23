---
phase: 11-hardening-di-produzione-e-verifica-su-dispositivo-reale
plan: 02
subsystem: testing
tags: [powershell, adb, logcat, android-auto, runbook, dhu-verification]

# Dependency graph
requires:
  - phase: 08-fondamenta-condivise-e-velocit-sullo-schermo-auto
    provides: "docs/dhu-quota-verification.md e scripts/dhu-quota-check.ps1 -- pattern 'script euristico + runbook per occhi umani' ricalcato qui"
  - phase: 11-hardening-di-produzione-e-verifica-su-dispositivo-reale
    provides: "11-PATTERNS.md: formato verificato delle entry di allow-list (<sha256Digest>,<packageName>) e segnali di log CarApp.Val"
provides:
  - "docs/android-auto-hardening-verification.md: runbook dei tre gate empirici SC1/SC2/SC3 con prerequisiti, procedura, criteri di esito misurabili scritti PRIMA della sessione e contingenze gia' decise"
  - "scripts/aa-connect-cycle-check.ps1: misura riproducibile di crash/ANR/rifiuti host durante cicli rapidi di connessione-disconnessione (SC3)"
  - "Tabella 'Esiti registrati' pronta per essere compilata dai Piani 03 (SC1, SC3) e 04 (SC2)"
affects:
  - "11-03 (sessione da scrivania: esegue SC1 e SC3 seguendo questo runbook e questo script)"
  - "11-04 (test su strada: esegue SC2 seguendo questo runbook; la contingenza D-03/D-04 e' gia' scritta, non da decidere)"

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Criteri di esito FAIL/PASS/INCONCLUSIVO scritti prima della sessione, con FAIL come prima riga della tabella e INCONCLUSIVO come esito legittimo -- impedisce di razionalizzare un'osservazione ambigua come PASS a posteriori (T-11-08)"
    - "Segnale di log come discriminante oggettivo del binario in esecuzione: 'Accepted - Validator disabled' e' trattato come FAIL di SC1, non come PASS, perche' prova che si sta testando un build di debug (T-11-09)"
    - "Contingenza scritta come decisione gia' presa con l'opzione chiusa riaffermata come chiusa, ricalcata da docs/dhu-quota-verification.md:104-107 (T-11-10)"

key-files:
  created:
    - scripts/aa-connect-cycle-check.ps1
    - docs/android-auto-hardening-verification.md
  modified: []

key-decisions:
  - "Il forward tcp:5277 non fallisce piu' la sessione: serve solo al Desktop Head Unit, quindi con un head unit reale viene segnalato a video e lo script prosegue. Divergenza consapevole da scripts/dhu-quota-check.ps1, dove il forward e' obbligatorio perche' quella verifica gira solo su DHU."
  - "Selezione del dispositivo senza preferenza per i serial 'emulator-' (opposto dell'analogo di Fase 8): SC3 gira su telefono fisico, quindi con piu' di un dispositivo connesso lo script chiede -Serial esplicito invece di indovinare."
  - "Aggiunto un uscita 'stop' al prompt di ogni ciclo, non prevista dal piano: senza di essa il ramo INCONCLUSIVO del verdetto (cicli completati < cicli richiesti) sarebbe stato irraggiungibile, rendendo la tripletta FAIL/PASS/INCONCLUSIVO nominale invece che reale."
  - "Em dash normalizzati ad ASCII '--' su tutto il runbook, per allineamento a docs/dhu-quota-verification.md che e' interamente ASCII (verificato: 0 caratteri non-ASCII in entrambi)."

patterns-established:
  - "Runbook di verifica empirica con tabella 'Quale build per quale criterio': quando i criteri di un gate richiedono binari diversi, il vincolo va scritto in cima al documento con la motivazione per riga, non lasciato dedurre a chi esegue la sessione"

requirements-completed: []

# Metrics
duration: ~35min
completed: 2026-09-03
---

# Phase 11 Plan 02: Strumenti di Verifica per SC1, SC2, SC3 Summary

Runbook dei tre gate empirici della Fase 11 con criteri di esito misurabili scritti prima delle
sessioni, piu' uno script PowerShell che misura crash, ANR e rifiuti host durante cicli rapidi di
connessione/disconnessione di Android Auto.

## Cosa e' stato costruito

**`scripts/aa-connect-cycle-check.ps1`** (337 righe) -- misura di SC3, ricalcata su
`scripts/dhu-quota-check.ps1`. Abilita `log.tag.CarApp.Val` a DEBUG (senza cui le righe
`Accepted - ...` della Car App Library non vengono mai emesse, perche' sono dietro
`Log.isLoggable`), apre il forward `tcp:5277`, cattura `logcat -v time CarApp.Val:D *:W` in
background, e guida l'operatore attraverso N cicli di collega/attendi/scollega campionando il PID a
ogni ciclo. Al termine conta cinque segnali (`FATAL EXCEPTION`, `ANR in com.sed.tachimetro`,
`Rejected -`, `Accepted - Host in allow-list`, `Accepted - Validator disabled`), dichiara quale
build era in esecuzione, ed emette un verdetto `FAIL`/`PASS`/`INCONCLUSIVO` a video e su file di
riepilogo sotto `build/aa-hardening/`.

**`docs/android-auto-hardening-verification.md`** (320 righe) -- runbook dei tre criteri. Apre con
la tabella "Quale build per quale criterio" (SC1 e SC3 su release firmato, SC2 su debug) e le due
conseguenze operative verificate: il contatore `onGetTemplate #<n>` esiste solo in debug
(`SpeedScreen.kt:307`, dentro `if (BuildConfig.DEBUG)`), e i build di release richiedono
`keystore.properties`, assente dal repository per scelta di progetto. Poi un blocco per criterio con
prerequisiti, procedura, osservazioni umane, tabella dei criteri di esito e contingenza.

## Decisioni chiave

**Il PID non e' un criterio.** Sia lo script sia il runbook dichiarano esplicitamente che un cambio
di PID tra un ciclo e l'altro non e' di per se' un FAIL: alla disconnessione il sistema puo'
legittimamente terminare il processo se l'app non e' in primo piano. Il segnale di crash e'
`FATAL EXCEPTION`/ANR. Il PID resta registrato come contesto per interpretare un log ambiguo. Senza
questa nota scritta, la modalita' di fallimento probabile era un falso FAIL su comportamento normale
di Android.

**`Accepted - Validator disabled, all hosts allowed` e' un FAIL di SC1, non un PASS.** E' l'unico
segnale che distingue oggettivamente i due binari a runtime, e senza questa regola una sessione SC1
eseguita per sbaglio su un build di debug produrrebbe un PASS perfettamente convincente e
completamente privo di valore (T-11-09).

**La contingenza di SC2 e' scritta come decisione gia' presa.** `ACCESS_BACKGROUND_LOCATION` e'
riaffermato come esplicitamente scartato e non riapribile, con la motivazione a verbale
(minimizzazione permessi T-02-EP, onere data-safety su Play Store) e il rinvio a una decisione a se'
per una milestone futura (D-04). La formula ricalca `docs/dhu-quota-verification.md:104-107`. Un
FAIL su SC2 e' documentato come esito valido del criterio di roadmap, non come fallimento di fase.

## Deviazioni dal piano

### Auto-fixed

**1. [Rule 3 - Blocco] Verifica del parser PowerShell non eseguibile in questo ambiente**
- **Trovato durante:** Task 1, verifica automatica
- **Problema:** Il sandbox dell'agente isolato in worktree rifiuta ogni invocazione di `powershell`
  e `pwsh` ("this command runs powershell in a plain command"), sia inline (`-Command`) sia da file
  (`-File`). I due passi di verifica del piano che dipendono da PowerShell (parser
  `[Parser]::ParseFile` e `Get-Help -Full`) non sono quindi eseguibili qui.
- **Sostituzione applicata:** audit strutturale meccanico al posto del parser -- bilanciamento di
  graffe (40/40) e parentesi (136/136), presenza dei due marcatori `<#`/`#>` del blocco di help,
  e ispezione mirata di tutti i backtick del file (5 occorrenze, tutte in posizioni attese:
  continuazioni di riga di `Start-Process`, escape `` `n ``, escape `` `: `` in
  `"$HostValidationTag`:D"`). Lo script e' inoltre ricalcato riga per riga su
  `scripts/dhu-quota-check.ps1`, gia' eseguito con successo in Fase 8.
- **Residuo:** la conferma definitiva della sintassi e dell'help arriva alla prima esecuzione reale
  dello script, che avviene nel Piano 03 (sessione SC3) -- vedi "Verifiche differite" sotto.
- **File:** `scripts/aa-connect-cycle-check.ps1`
- **Commit:** `4df6795`

**2. [Rule 2 - Funzionalita' mancante] Ramo INCONCLUSIVO irraggiungibile**
- **Trovato durante:** Task 1, implementazione del ciclo
- **Problema:** Il piano definisce `INCONCLUSIVO` come "cicli completati < cicli richiesti", ma il
  flusso descritto (un `Read-Host` per ciclo, `for` da 1 a `$Cycles`) completa sempre tutti i cicli:
  il terzo verdetto sarebbe stato codice morto, e il criterio T-11-08 ("INCONCLUSIVO come esito
  esplicito e legittimo") sarebbe stato solo nominale.
- **Fix:** il prompt di ogni ciclo accetta `stop` oltre a INVIO; l'interruzione registra
  `completedCycles` reale e marca il riepilogo come sessione interrotta.
- **File:** `scripts/aa-connect-cycle-check.ps1`
- **Commit:** `4df6795`

**3. [Rule 1 - Correzione] Formato delle entry di allow-list citato nel verso giusto**
- **Trovato durante:** Task 2, stesura della contingenza di SC1
- **Problema:** Il piano descrive l'errore come "l'inversione `<package>,<digest>`". Il formato
  corretto verificato in `11-PATTERNS.md` (fatto #2, dal bytecode della libreria) e'
  `<sha256Digest>,<packageName>`: scrivere nel runbook che l'inversione e' `<package>,<digest>` era
  ambiguo su quale dei due versi sia quello giusto.
- **Fix:** il runbook indica esplicitamente il formato corretto (digest prima, package dopo) e
  spiega che invertirlo produce un validator che compila, si installa e rifiuta ogni host senza
  eccezioni a runtime.
- **File:** `docs/android-auto-hardening-verification.md`
- **Commit:** `c620775`

**4. [Rule 2 - Coerenza di progetto] Normalizzazione ASCII**
- **Trovato durante:** Task 2, verifica del criterio "nessun carattere accentato nelle formule
  tecniche"
- **Problema:** 25 em dash Unicode nel documento, contro 0 caratteri non-ASCII in
  `docs/dhu-quota-verification.md`.
- **Fix:** sostituiti con `--`, la convenzione dell'analogo. Verificato: 0 caratteri non-ASCII.
- **File:** `docs/android-auto-hardening-verification.md`
- **Commit:** `c620775`

## Verifiche differite

| Verifica | Motivo | Quando si chiude |
|----------|--------|------------------|
| `[Parser]::ParseFile` senza errori su `aa-connect-cycle-check.ps1` | `powershell`/`pwsh` non invocabili dall'agente isolato in worktree | Prima esecuzione dello script nel Piano 03 (sessione SC3) |
| `Get-Help ./scripts/aa-connect-cycle-check.ps1 -Full` restituisce un help popolato | Stesso motivo | Stesso momento; il blocco di comment-based help e' presente e ben formato (`.SYNOPSIS`, `.DESCRIPTION`, 3 `.PARAMETER`, `.EXAMPLE`) |

Nessuna delle due e' una verifica di comportamento dell'app: entrambe riguardano solo lo strumento
di misura, e falliscono in modo rumoroso e immediato (errore di parsing all'avvio) se qualcosa non
va.

## Verifiche eseguite

| Verifica | Esito |
|----------|-------|
| Stringhe letterali richieste nello script (`FATAL EXCEPTION`, `ANR in com.sed.tachimetro`, `Rejected -`, `Accepted - Host in allow-list`, `setprop log.tag.CarApp.Val DEBUG`) | PASS |
| `param()` dichiara `$Cycles`=10, `$OutputDir`="build/aa-hardening", `$Serial`="" | PASS |
| Tre verdetti `FAIL`/`PASS`/`INCONCLUSIVO` presenti e raggiungibili | PASS |
| `grep -nE "Invoke-WebRequest\|curl\|Install-\|choco\|winget\|sdkmanager"` non trova nulla (T-11-07) | PASS (nessun match) |
| `git check-ignore -q build` esce 0 (T-11-06) | PASS |
| Bilanciamento graffe/parentesi dello script | PASS (40/40, 136/136) |
| Quattro sezioni di primo livello del runbook | PASS |
| Esattamente 3 `### Criteri di esito` e 3 `### Contingenza se FAIL` | PASS (3 e 3) |
| Cross-reference a `aa-connect-cycle-check.ps1`, `PITFALLS.md`, `ACCESS_BACKGROUND_LOCATION` | PASS |
| Tabella `## Esiti registrati` con intestazione `Criterio \| Data \| Esito \| Note` e 3 righe `da eseguire` | PASS |
| 0 caratteri non-ASCII nel runbook | PASS |
| `git status --short app/` vuoto (nessun file applicativo toccato) | PASS |
| `git status --short` non mostra file sotto `build/` | PASS |
| Nessuna cancellazione di file nei due commit | PASS |

## Note per le fasi successive

- Il Piano 03 esegue SC1 e SC3 e deve compilare le prime due righe utili della tabella
  `## Esiti registrati`. La prima esecuzione reale di `aa-connect-cycle-check.ps1` chiude anche le
  due verifiche differite sopra.
- Il Piano 04 esegue SC2. La contingenza e' gia' scritta: se il GPS si ferma a telefono bloccato,
  si registra l'esito nel runbook e in `STATE.md`, e **non** si aggiunge
  `ACCESS_BACKGROUND_LOCATION`.
- `playstore/README.md` contiene ancora la nota di rischio sull'`ALLOW_ALL_HOSTS_VALIDATOR`: il suo
  ritiro e' scope del Piano 03, non di questo piano.

## Self-Check: PASSED

- `scripts/aa-connect-cycle-check.ps1` -- FOUND
- `docs/android-auto-hardening-verification.md` -- FOUND
- Commit `4df6795` (Task 1) -- FOUND
- Commit `c620775` (Task 2) -- FOUND
