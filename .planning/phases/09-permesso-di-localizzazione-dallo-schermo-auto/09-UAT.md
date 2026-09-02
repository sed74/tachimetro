---
status: complete
phase: 09-permesso-di-localizzazione-dallo-schermo-auto
source: [09-01-SUMMARY.md, 09-02-SUMMARY.md, 09-03-SUMMARY.md]
started: 2026-09-02T13:18:26Z
updated: 2026-09-02T13:28:00Z
---

## Current Test

[testing complete]

## Tests

### 1. Primo collegamento senza permesso — richiesta automatica
expected: Collegando Android Auto per la prima volta senza permesso mai concesso, lo schermo auto mostra "Controlla il telefono" e sul telefono appare automaticamente il dialogo di sistema per il permesso.
result: pass

### 2. Concessione del permesso — transizione automatica
expected: Concedendo il permesso nel dialogo di sistema, lo schermo auto passa automaticamente a mostrare la velocità (o "Ricerca segnale GPS...") senza dover riavviare l'app o ricollegare Android Auto.
result: pass

### 3. Rifiuto singolo — messaggio breve con Riprova
expected: Negando il permesso la prima volta, lo schermo auto mostra "Permesso GPS necessario" con un'azione "Riprova" che, toccata a veicolo fermo, rilancia la richiesta di permesso.
result: pass

### 4. Rifiuto permanente — messaggio con Apri impostazioni
expected: Dopo un secondo rifiuto, lo schermo auto mostra "Permesso negato. Apri le impostazioni sul telefono" con un'azione "Apri impostazioni" che, toccata, apre le impostazioni dell'app sul telefono.
result: pass

### 4. Rifiuto permanente — messaggio con Apri impostazioni
expected: Dopo un secondo rifiuto, lo schermo auto mostra "Permesso negato. Apri le impostazioni sul telefono" con un'azione "Apri impostazioni" che, toccata, apre le impostazioni dell'app sul telefono.
result: [pending]

### 5. Nessun loop automatico del dialogo dopo un rifiuto
expected: Dopo un rifiuto, il dialogo di permesso non ricompare da solo — serve toccare esplicitamente "Riprova"; l'azione Riprova/Apri impostazioni funziona solo a veicolo parcheggiato.
result: pass

## Summary

total: 5
passed: 5
issues: 0
pending: 0
skipped: 0
blocked: 0

## Gaps

[none yet]
