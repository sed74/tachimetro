---
status: partial
phase: 01-fondamenta-permessi-e-avvio
source: [01-VERIFICATION.md]
started: 2026-07-07T00:00:00Z
updated: 2026-07-07T00:00:00Z
---

## Current Test

[awaiting human testing]

## Tests

### 1. Return-from-Settings permission re-check
expected: Dopo rifiuto permanente → "Apri impostazioni" → concedere il permesso nelle Impostazioni di sistema → premere Indietro per tornare all'app (senza forzare la chiusura). L'app deve mostrare immediatamente lo schermo nero con "Pronto", senza bisogno di riavviare l'app. Questo verifica la correzione CR-01 (onResume, commit 927e3c0) non ancora testata su device reale.
result: [pending]

## Summary

total: 1
passed: 0
issues: 0
pending: 1
skipped: 0
blocked: 0

## Gaps
