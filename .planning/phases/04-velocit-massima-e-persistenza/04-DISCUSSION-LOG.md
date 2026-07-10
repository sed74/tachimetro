# Phase 4: Velocità Massima e Persistenza - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-10
**Phase:** 4-Velocità Massima e Persistenza
**Areas discussed:** Posizione e formato area velocità massima, Comportamento pulsante "Azzera massimo", Timing della persistenza su disco, Comportamento iniziale / valore assente

---

## Posizione e formato area velocità massima

| Domanda | Opzione scelta |
|---------|-----------------|
| Dove posizionare l'area della velocità massima? | In alto a sinistra, speculare a "km/h" |
| Che formato deve avere il testo? | Etichetta "MAX" + numero, es. "MAX 120" |

## Comportamento pulsante "Azzera massimo"

| Domanda | Opzione scelta |
|---------|-----------------|
| Quando deve essere visibile il pulsante? | Solo quando il massimo è > 0 |
| Serve una conferma prima di azzerare? | Immediato, nessuna conferma |
| Dove posizionare il pulsante? | Sotto l'etichetta MAX, in alto a sinistra |

## Timing della persistenza su disco

| Domanda | Opzione scelta |
|---------|-----------------|
| Quando salvare il nuovo record massimo? | Ad ogni nuovo massimo registrato |
| L'azzeramento va salvato subito? | Sì, subito |

## Comportamento iniziale / valore assente

| Domanda | Opzione scelta |
|---------|-----------------|
| Cosa mostrare prima di una lettura valida? | Area MAX nascosta finché il massimo non supera 0 |

---

## Claude's Discretion

- Nome/struttura chiave SharedPreferences
- Stile/dimensione esatta del testo "MAX 120" (fisso o autosize)
- Padding/margini esatti dell'area MAX e del pulsante Azzera
- Punto esatto del ciclo di vita per leggere il valore salvato all'avvio
- Formato interno del dato persistito

## Deferred Ideas

Nessuna — la discussione non ha prodotto scope creep.
