# Phase 1: Fondamenta, Permessi e Avvio - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-07
**Phase:** 1-Fondamenta, Permessi e Avvio
**Areas discussed:** Nessuna (utente ha delegato a Claude)

---

## Aree proposte (non discusse)

| Area | Descrizione |
|------|-------------|
| Tempo richiesta permesso | Popup GPS subito all'avvio, o prima un messaggio esplicativo? |
| Comportamento su rifiuto | Solo messaggio con "Riprova", o anche link alle Impostazioni? |
| Schermata placeholder | Cosa mostra questa fase quando il permesso è concesso, prima del motore GPS (Fase 2) e della UI finale (Fase 3)? |
| Rifiuto permanente | Comportamento se l'utente seleziona "Non chiedere più" e riavvia l'app |

**Risposta utente:** "niente, passa alla fase successiva" — ha scelto di non approfondire nessuna area e di lasciare le decisioni implementative a Claude.

---

## Claude's Discretion

- Popup permesso richiesto subito all'avvio, senza schermate intermedie
- Rifiuto → messaggio + pulsante "Riprova"; rifiuto permanente → pulsante verso Impostazioni app
- Placeholder: schermo nero con testo neutro "Pronto", sostituito dalla UI reale in Fase 3
- Aggiunta plugin Kotlin al progetto (attualmente solo Java)

## Deferred Ideas

Nessuna — la discussione non ha prodotto scope creep.
