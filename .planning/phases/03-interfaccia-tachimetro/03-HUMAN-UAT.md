---
status: complete
phase: 03-interfaccia-tachimetro
source: [03-VERIFICATION.md]
started: 2026-07-10T00:00:00Z
updated: 2026-07-10T00:00:00Z
---

## Current Test

Nessuno — tutti i test completati.

## Tests

### 1. Numero dominante centrato — portrait/landscape, 1/2/3 cifre
expected: Il numero della velocità è il più grande possibile, centrato orizzontalmente e verticalmente, senza tagli, in entrambi gli orientamenti.
result: passed — approvato dall'utente ("approvato") dopo il fix round 3 (unitText separata + window insets).

### 2. Messaggi di stato leggibili (ricerca segnale, permesso negato)
expected: I messaggi si ridimensionano automaticamente più piccoli del numero, restando leggibili senza wrap eccessivo.
result: passed — approvato dall'utente dopo il fix round 1 (cap autosize messaggi a 56sp).

### 3. Unità "km/h" in vista separata, top-right, libera dalla status bar
expected: L'unità di misura appare piccola, in alto a destra, senza sovrapporsi alla status bar/notch, sia in portrait sia in landscape.
result: passed — approvato dall'utente dopo il fix round 3 (window insets su targetSdk 36 edge-to-edge).

### 4. Modalità fullscreen immersiva (post-completamento, round 4)
expected: Nessuna barra del titolo, status bar e nav bar nascoste all'avvio con swipe-to-reveal, `unitText` resta corretta, le barre tornano nascoste rientrando da "Apri impostazioni".
result: passed — approvato dall'utente ("approvato") in sessione dopo il fix round 4 (tema NoActionBar + `enableImmersiveFullscreen()`, commit `42bb31a`/`7aa01ec`).

## Summary

total: 4
passed: 4
issues: 0
pending: 0
skipped: 0
blocked: 0

## Gaps

Nessuno.
