# Phase 3: Interfaccia Tachimetro - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-07
**Phase:** 3-Interfaccia Tachimetro
**Areas discussed:** Dimensionamento automatico del numero, Layout portrait vs landscape, Integrazione numero + messaggi di stato, Spazio riservato per velocità massima

---

## Dimensionamento automatico del numero

| Domanda | Opzione scelta |
|---------|-----------------|
| Come garantire il numero sempre più grande possibile? | autoSizeTextType di TextView |

## Layout portrait vs landscape

| Domanda | Opzione scelta |
|---------|-----------------|
| Layout separato per landscape? | Un unico layout adattivo |

## Integrazione numero + messaggi di stato

| Domanda | Opzione scelta |
|---------|-----------------|
| Come convivono numero e messaggi di stato? | Stesso elemento di testo, dimensione ridotta automaticamente per i messaggi |
| Posizionamento pulsante Riprova/Apri impostazioni | Stesso posizionamento attuale |

## Spazio riservato per velocità massima (Fase 4)

| Domanda | Opzione scelta |
|---------|-----------------|
| Riservare spazio per la Fase 4? | No, solo il numero principale in questa fase |

---

## Claude's Discretion

- Valori esatti di autoSizeMinTextSize/MaxTextSize/StepGranularity, margini/padding, dettagli di stile del font

## Deferred Ideas

Nessuna — la discussione non ha prodotto scope creep.
