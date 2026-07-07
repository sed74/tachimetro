# Tachimetro

## What This Is

App Android nativa che mostra la velocità GPS in tempo reale a schermo intero, con un'interfaccia minimale ad altissimo contrasto pensata per essere letta a colpo d'occhio mentre l'app è montata su un supporto in auto o in moto. Nessun menu, nessuna animazione, nessun grafico: solo il numero della velocità.

## Core Value

La velocità attuale deve essere sempre visibile, corretta e leggibile istantaneamente in ogni condizione di luce — se questo non funziona alla perfezione, il resto non conta.

## Requirements

### Validated

- ✓ App che apre direttamente la schermata principale, nessuna schermata iniziale/menu — Fase 1
- ✓ Richiesta del permesso ACCESS_FINE_LOCATION con gestione corretta di concessione/rifiuto/rifiuto permanente — Fase 1

### Active

- [ ] Mostrare la velocità attuale in km/h con un numero enorme, sempre centrato, elemento dominante dello schermo
- [ ] Sfondo nero, testo ad alto contrasto, font di sistema in versione Bold/Black
- [ ] Supporto completo portrait e landscape, con il numero che si adatta e resta il più grande possibile
- [ ] Lettura della velocità via GPS usando FusedLocationProviderClient (Google Play Services)
- [ ] Aggiornamento della velocità a schermo 1 volta al secondo
- [ ] Messaggio semplice quando manca il segnale GPS (es. "Ricerca segnale GPS...")
- [ ] Area secondaria con la velocità massima raggiunta dall'ultimo azzeramento
- [ ] Pulsante "Azzera massimo" che azzera il valore e avvia una nuova misurazione
- [ ] Velocità massima persistente su disco (sopravvive a chiusura app e riavvio del telefono)
- [ ] Toggle "Schermo sempre acceso" / "Schermo automatico", con preferenza salvata tra sessioni
- [ ] Quando "sempre acceso" è attivo, impedire lo spegnimento schermo durante l'uso (FLAG_KEEP_SCREEN_ON)
- [ ] Testi e messaggi dell'interfaccia interamente in italiano

### Out of Scope

- Cambio unità km/h ↔ mph — non richiesto per v1, l'app mostra solo km/h
- Tracciamento percorso, mappa o cronologia velocità — l'app è un tachimetro istantaneo, non un GPS tracker
- Menu, impostazioni avanzate, grafici o animazioni — l'interfaccia deve restare estremamente semplice per definizione di prodotto
- Supporto Android precedente alla versione 11 (minSdk < 30) — il progetto fissa già minSdk 30, nessun device più vecchio da supportare

## Context

- Fase 1 completa (2026-07-07): scaffold portato sotto controllo versione, Kotlin abilitato via supporto built-in AGP 9.1.1 (non il plugin classico, incompatibile con questa versione AGP), MainActivity LAUNCHER con flusso permesso GPS completo, verificato su emulatore reale
- Progetto Android Studio inizializzato (`com.sed.tachimetro`); vedi `.planning/codebase/` per la mappatura pre-esistente
- minSdk 30, targetSdk 36, AGP 9.1.1, Gradle 9.3.1, Kotlin DSL per i build script
- Uso previsto: app montata su supporto auto/moto, quindi priorità assoluta a leggibilità a distanza/in movimento e a basso consumo batteria durante sessioni prolungate

## Constraints

- **Tech stack**: Kotlin per il codice applicativo, layout XML tradizionali (no Jetpack Compose) — coerente con AppCompat già presente e adeguato per una singola schermata statica
- **GPS**: FusedLocationProviderClient (Google Play Services) — richiede un device con Google Play Services installato
- **Compatibility**: minSdk 30 (Android 11+), targetSdk 36
- **Performance**: aggiornamento velocità 1 volta/sec — bilanciamento scelto tra fluidità percepita e consumo batteria
- **UX**: nessun elemento grafico non necessario, nessun menu, nessuna animazione — massima leggibilità in ogni condizione di luce

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Kotlin invece di Java | Standard moderno per Android, build script già in Kotlin DSL | ✓ Good |
| Kotlin abilitato via supporto built-in AGP 9.1.1 (non plugin separato) | Il plugin `org.jetbrains.kotlin.android` classico è incompatibile con AGP 9.1.1 (conflitto verificato) | ✓ Good |
| Layout XML invece di Jetpack Compose | Schermata singola e statica, meno overhead di setup, coerente con AppCompat esistente | ✓ Good |
| FusedLocationProviderClient invece di LocationManager nativo | Più efficiente e preciso, accettato il vincolo di richiedere Google Play Services | — Pending |
| Velocità massima persistente (SharedPreferences) | L'utente vuole confrontare sessioni di guida diverse senza perdere il record | — Pending |
| Aggiornamento GPS 1/sec | Bilancia fluidità e battery drain per uso prolungato in auto/moto | — Pending |
| Solo km/h, nessun toggle unità | Riduce complessità UI, non richiesto per v1 | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd:complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-07-07 after Phase 1 completion*
