---
phase: 11-hardening-di-produzione-e-verifica-su-dispositivo-reale
plan: 03
subsystem: verification
tags: [android-auto, host-validator, release, checkpoint-umano]
requires:
  - phase: 11-hardening-di-produzione-e-verifica-su-dispositivo-reale
    provides: "11-01 allow-list reale, 11-02 runbook + script"
provides:
  - "SC1 registrato PASS (visivo) nel runbook"
  - "Nota di rischio ALLOW_ALL_HOSTS_VALIDATOR ritirata da playstore/README.md"
key-files:
  modified:
    - docs/android-auto-hardening-verification.md
    - playstore/README.md
key-decisions:
  - "SC1 PASS visivo (senza log CarApp.Val) accettato esplicitamente dall'utente"
  - "SC3 rimandato su richiesta dell'utente per chiudere la milestone v2.0"
completed: 2026-09-23
---

# Piano 11-03: SC1 verificato su host reale, SC3 rimandato

## Esiti

- **SC1 — PASS (visivo).** L'utente ha usato l'app su una head unit Android Auto reale in auto con il
  build release versionCode 4 installato dal Play Store canale test aperto. Verificato via
  `adb shell dumpsys package com.sed.tachimetro`: `versionCode=4`, `installerPackageName=com.android.vending`,
  nessun flag `DEBUGGABLE`. Il bump a vC4 (`b62b879`) e' successivo al merge dell'allow-list
  (`a2582f7`) e nessun file di `app/src/main` e' cambiato dopo. La velocita' compariva sullo schermo
  auto: con l'allow-list attiva un host rifiutato non puo' bindare il car service, quindi l'host e'
  stato accettato. La riga `CarApp.Val` non e' stata catturata (USB occupata dall'head unit, buffer
  logcat gia' ruotato alla verifica successiva): l'utente ha scelto PASS visivo invece di INCONCLUSIVO.
- **SC3 — RIMANDATO.** Su richiesta dell'utente, per chiudere la milestone e passare alla velocita'
  a tutto schermo (v2.1). Da verificare a mano in auto (10 cicli, osservazione visiva, nessuno
  script perche' adb non e' disponibile con la USB occupata). Copertura indiretta: `CarLinkSequenceTest`.

## Deviazioni dal piano

- Task 1/2 non eseguiti come sessione da scrivania con `installRelease` + logcat: il build e' stato
  pubblicato dall'utente sul canale test aperto (decisione del 2026-09-03) e testato in auto.
- Gate automatico del Task 3 (`grep -c 'da eseguire' == 1`) non applicabile: SC2 e' stato registrato
  nella stessa sessione (vedi 11-04) e SC3 e' `RIMANDATO`, quindi nel file non resta nessun `da eseguire`.
  Le altre condizioni del gate (titolo `Rischio host validation`, assenza di `restituisce ancora`,
  nessuna modifica a `listing/`/`release_notes/`) sono soddisfatte.

## Commit

- `6888ce5` docs(11-03): registra SC1 PASS visivo e ritira la nota di rischio
