# Phase 5: Gestione Schermo - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-10
**Phase:** 5-Gestione Schermo
**Areas discussed:** Posizione e forma del toggle, Visibilità del toggle, Valore iniziale di default

---

## Posizione e forma del toggle

| Domanda | Opzione scelta |
|---------|-----------------|
| Dove posizionare il toggle? | In basso, angolo libero |
| Quale angolo, sinistra o destra? | In basso a sinistra |
| Che forma deve avere il controllo? | Switch (interruttore) con etichetta breve |

## Visibilità del toggle

| Domanda | Opzione scelta |
|---------|-----------------|
| Sempre visibile o nascosto/richiamabile? | Sempre visibile, piccolo |

## Valore iniziale di default

| Domanda | Opzione scelta |
|---------|-----------------|
| Default al primo avvio? | Sempre acceso se il telefono è in ricarica, automatico se non lo è (risposta libera dell'utente) |
| Il rilevamento ricarica si applica solo al primo avvio o ad ogni avvio? | Solo al primo avvio — poi resta fisso finché l'utente non lo cambia manualmente |

---

## Claude's Discretion

- Nome/struttura chiave SharedPreferences (nuovo store o estensione di MaxSpeedStore)
- API esatta per rilevare stato di ricarica (BatteryManager/ACTION_BATTERY_CHANGED)
- Testo esatto dell'etichetta breve dello switch
- Stile/dimensioni esatte del widget Switch
- Gestione window insets per il nuovo elemento in basso a sinistra

## Deferred Ideas

Nessuna — la discussione non ha prodotto scope creep.
