---
phase: 12-spike-surface-e-decisione-categoria-template
plan: 03
subsystem: tooling-dhu
tags: [android-auto, dhu, surface, spike, powershell, runbook]
requires:
  - "Contratto di log TachimetroSurface (Piano 02)"
  - "Flavor spikePoi/spikeNav e proprieta' -PpoiCard (Piano 01)"
provides:
  - "scripts/surface-spike-check.ps1: una sessione di misura = un comando"
  - "scripts/dhu/dhu-{800x480,1280x720,1920x1080}.ini"
  - "docs/surface-spike-verification.md: runbook delle 15 sessioni"
affects:
  - "12-SPIKE-RESULTS.md (riceve le righe dei summary)"
tech-stack:
  added: []
  patterns: ["script PowerShell + runbook markdown per sessioni DHU (modello dhu-quota-check.ps1)"]
key-files:
  created:
    - scripts/surface-spike-check.ps1
    - scripts/dhu/dhu-800x480.ini
    - scripts/dhu/dhu-1280x720.ini
    - scripts/dhu/dhu-1920x1080.ini
    - docs/surface-spike-verification.md
  modified: []
decisions:
  - "OutputDir e percorso screenshot risolti rispetto alla radice del repo ($PSScriptRoot/..), non alla cwd"
  - "Lo script crea la cartella spike-screenshots/ se manca, cosi' l'utente puo' salvarci subito"
  - "Controllo di coerenza aggiuntivo: variant/card nei log confrontati con la variante richiesta (utile con -SkipInstall)"
metrics:
  duration: "~15 min"
  completed: 2026-09-23
  tasks: 2
  files: 5
---

# Phase 12 Plan 03: Tooling misure spike Surface Summary

Script PowerShell `surface-spike-check.ps1` che in un comando installa la variante (`nav` o `poi-message|list|pane|grid`), cattura il tag `TachimetroSurface`, estrae l'ultima riga di ogni callback e dell'ultimo frame e produce la riga di tabella per `12-SPIKE-RESULTS.md`; tre ini DHU per le risoluzioni D-09; runbook con le 15 sessioni, il criterio D-12 e la verifica opzionale in auto D-11.

## Task completati

| Task | Nome | Commit | File |
|------|------|--------|------|
| 1 | Script di cattura + config DHU | 04e2414 | scripts/surface-spike-check.ps1, scripts/dhu/*.ini |
| 2 | Runbook | 187c23c | docs/surface-spike-verification.md |

## Dettagli

- Mappatura variante: `nav` -> `:app:installSpikeNavDebug`; `poi-<card>` -> `:app:installSpikePoiDebug -PpoiCard=<card>`, con `ANDROID_SERIAL` impostato solo per il processo Gradle e ripristinato subito dopo.
- Parsing: regex sul contratto del Piano 02 (`onSurfaceAvailable w= h= dpi= api= variant= card=`, `onStableAreaChanged/onVisibleAreaChanged l= t= r= b=`, `frame area= w= h= textSize= digitH=`), vale l'ultima occorrenza; i campi mancanti diventano `n/d` con avviso giallo.
- Avvisi di coerenza: Surface diversa da `-Resolution` (il DHU non ha caricato l'ini), variante/card inattese, ultimo frame non su `area=stable`, `onSurfaceDestroyed` durante la cattura, screenshot mancante.
- Formato degli ini DHU: `[general]` + `resolution = WxH`, come da documentazione DHU; nessuna chiave diversa necessaria.

## Deviations from Plan

None - plan executed exactly as written.

Nota di verifica: il comando automatico del Task 1 prevede `powershell ... Parser::ParseFile`, ma in questo worktree la guardia di isolamento dell'agente blocca qualunque invocazione di `powershell`. Il parse check non e' stato quindi eseguito; al suo posto: controllo con grep di tutti i token richiesti e revisione manuale della sintassi (corretto un `$Resolution:` dentro una stringa, che PowerShell avrebbe letto come riferimento con scope, in `${Resolution}:`). Da eseguire alla prima esecuzione reale: `powershell -NoProfile -Command "[System.Management.Automation.Language.Parser]::ParseFile(...)"` oppure lanciare lo script con `-?`.

## Known Stubs

Nessuno.

## Self-Check: PASSED

- FOUND: scripts/surface-spike-check.ps1 (389 righe)
- FOUND: scripts/dhu/dhu-800x480.ini, dhu-1280x720.ini, dhu-1920x1080.ini
- FOUND: docs/surface-spike-verification.md
- FOUND: commit 04e2414, 187c23c
