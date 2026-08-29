# Phase 7: Distanza Percorsa e Reset Unificato - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-08-29
**Phase:** 7-Distanza Percorsa e Reset Unificato
**Areas discussed:** Formato e stile della distanza, Comportamento a fermo / GPS instabile, Testo del pulsante di reset unificato, Metodo di calcolo della distanza

---

## Formato e stile della distanza

| Option | Description | Selected |
|--------|-------------|----------|
| Km con una decimale, sempre | Es. "12,3 km", formato costante | |
| Adattivo: metri sotto 1km, poi km | Es. "850 m" poi "1,2 km" | ✓ |
| Km arrotondato a intero | Es. "12 km" | |

**User's choice:** Adattivo: metri sotto 1km, poi km

| Option | Description | Selected |
|--------|-------------|----------|
| Stringa combinata in un solo TextView | Come "MAX %d" | |
| Vista unità separata, come unitText | Numero grande + "km" piccolo accanto | ✓ |

**User's choice:** Vista unità separata, come unitText

| Option | Description | Selected |
|--------|-------------|----------|
| 28sp | Leggermente più grande di MAX | |
| 32sp | Chiaramente più grande | ✓ |
| Altro valore (specifica) | — | |

**User's choice:** 32sp

---

## Comportamento a fermo / GPS instabile

| Option | Description | Selected |
|--------|-------------|----------|
| Sì, stessa soglia di rumore della velocità (2 km/h) | Riusa noiseFloorKmh esistente | ✓ |
| No, accumula ogni delta di posizione grezzo | Rischio deriva da jitter GPS | |

**User's choice:** Sì, stessa soglia di rumore della velocità (2 km/h)

| Option | Description | Selected |
|--------|-------------|----------|
| Sì, stesso filtro di accuratezza della velocità (50m) | Coerenza col filtro esistente | ✓ |
| Filtro più stringente solo per la distanza | Distanza è cumulativa, più sensibile | |

**User's choice:** Sì, stesso filtro di accuratezza della velocità (50m)

---

## Testo del pulsante di reset unificato

| Option | Description | Selected |
|--------|-------------|----------|
| Mantieni "Azzera massimo" | Nessuna modifica | |
| Cambia in "Azzera" (generico) | Più corto, non legato a una sola metrica | ✓ |
| Cambia in "Azzera tutto" | Esplicita che azzera più valori | |

**User's choice:** Cambia in "Azzera" (generico)

---

## Metodo di calcolo della distanza

| Option | Description | Selected |
|--------|-------------|----------|
| Integra la velocità letta ogni secondo (km/h × tempo) | Riusa i filtri già decisi senza duplicare logica | |
| Somma la distanza tra fix GPS consecutivi (Location.distanceTo) | Più fedele al percorso reale (curve), richiede riapplicare i filtri separatamente | ✓ |

**User's choice:** Somma la distanza tra fix GPS consecutivi (Location.distanceTo)
**Notes:** Scelta esplicita nonostante la maggiore complessità implementativa segnalata (D-07 in CONTEXT.md nota l'impatto architetturale su GpsSpeedProvider).

---

## Claude's Discretion

- Tipo/precisione dei dati persistiti per la distanza
- Come GpsSpeedProvider espone i delta di posizione filtrati (D-07)
- Window insets per il nuovo angolo bottom-right
- Se l'area distanza resta nascosta a "0" come l'area MAX, o sempre visibile
- Nomi delle string resources per i nuovi formati

## Deferred Ideas

None — discussion stayed within phase scope.
