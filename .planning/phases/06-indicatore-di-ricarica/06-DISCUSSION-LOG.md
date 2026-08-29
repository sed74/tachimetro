# Phase 6: Indicatore di Ricarica - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-08-29
**Phase:** 6-Indicatore di Ricarica
**Areas discussed:** Stile del fulmine, Posizione esatta vs toggle, Comportamento a batteria piena

---

## Stile del fulmine

| Option | Description | Selected |
|--------|-------------|----------|
| Flash Material classico | Zigzag pieno stile Android standard (es. ic_flash_on) — riconoscibile, icona vettoriale pronta da riusare | ✓ |
| Sottile/outline | Solo contorno lineare, più leggero visivamente — coerente con l'estetica minimale bianco/nero attuale | |
| Altro/descrivo io | Stile specifico indicato dall'utente | |

**User's choice:** Flash Material classico
**Notes:** Nessuna nota aggiuntiva.

---

## Posizione esatta vs toggle

| Option | Description | Selected |
|--------|-------------|----------|
| Sopra lo switch | Icona sopra, switch sotto — stessa colonna verticale | |
| A sinistra dello switch | Icona e switch sulla stessa riga orizzontale, fulmine come prima cosa a sinistra | ✓ |
| A destra dello switch | Icona e switch sulla stessa riga, fulmine più vicino al centro schermo | |

**User's choice:** A sinistra dello switch
**Notes:** Nessuna nota aggiuntiva.

---

## Comportamento a batteria piena

| Option | Description | Selected |
|--------|-------------|----------|
| Continua a pulsare | Stesso loop di riempimento bianco→lime→bianco anche a batteria piena | |
| Resta fissa piena (lime solido) | Segnala visivamente che la ricarica è completa e non sta più caricando attivamente | ✓ |

**User's choice:** Resta fissa piena (lime solido)
**Notes:** L'utente ha preferito l'opzione più informativa nonostante aggiunga un secondo stato visivo da gestire.

---

## Claude's Discretion

- Dimensione esatta dell'icona in dp/sp
- Meccanismo tecnico di rilevamento continuo dello stato di ricarica (BroadcastReceiver su ACTION_POWER_CONNECTED/DISCONNECTED vs osservazione sticky ACTION_BATTERY_CHANGED)
- Implementazione dell'animazione (AnimatedVectorDrawable, ValueAnimator, o altra tecnica)
- Valore hex esatto del colore lime

## Deferred Ideas

None — discussion stayed within phase scope.
