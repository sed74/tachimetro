---
phase: 11-hardening-di-produzione-e-verifica-su-dispositivo-reale
plan: 04
subsystem: verification
tags: [android-auto, background-location, checkpoint-umano]
provides:
  - "SC2 registrato PASS (visivo) nel runbook"
key-files:
  modified:
    - docs/android-auto-hardening-verification.md
key-decisions:
  - "Nessun ACCESS_BACKGROUND_LOCATION necessario: il GPS continua a telefono bloccato (D-03/D-04 non attivate)"
completed: 2026-09-23
---

# Piano 11-04: SC2 verificato su strada

## Esito

- **SC2 — PASS (visivo).** Durante la stessa sessione in auto di SC1 (head unit reale, build release
  vC4 dal Play Store) l'utente ha osservato che con il telefono bloccato la velocita' sullo schermo
  auto continuava ad aggiornarsi. Durata non cronometrata; SC2 e' per design una verifica solo visiva
  (D-07, nessuno script: USB occupata dall'head unit).
- La contingenza D-03/D-04 (documentare il limite di piattaforma, niente `ACCESS_BACKGROUND_LOCATION`)
  non e' stata attivata: nessun limite osservato. Il concern "GPS in background a telefono bloccato"
  in `.planning/STATE.md` e' risolto.

## Deviazioni dal piano

- Nessuna sessione dedicata di 5-10 minuti cronometrata: l'esito e' stato riportato dall'utente a
  posteriori sulla base dell'uso reale in auto.
