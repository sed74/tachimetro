# Phase 8: Fondamenta Condivise e Velocità sullo Schermo Auto - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-08-31
**Phase:** 8-Fondamenta Condivise e Velocità sullo Schermo Auto
**Areas discussed:** Contenuto schermo auto, Mitigazione quota refresh, Icona app Android Auto, Checkpoint verifica DHU

---

## Contenuto schermo auto

| Option | Description | Selected |
|--------|-------------|----------|
| Numero grande + km/h separato | Numero come testo principale della Row, "km/h" come titolo/sottotitolo distinto (come unitText sul telefono) | ✓ |
| Numero e unità insieme | Una stringa unica tipo "72 km/h" nella Row | |
| Solo il numero nudo | Nessuna unità mostrata | |

**User's choice:** Numero grande + km/h separato
**Notes:** Rispecchia la separazione già usata sul telefono tra `messageText` (digit dominante) e `unitText` (unità piccola separata).

| Option | Description | Selected |
|--------|-------------|----------|
| Stesso testo del telefono | Riusa `searching_gps_signal` ("Ricerca segnale GPS...") | |
| Testo più breve per l'auto | Testo dedicato, più conciso | ✓ |

**User's choice:** Testo più breve per l'auto
**Notes:** Seguito da una domanda di dettaglio per fissare il testo esatto.

| Option | Description | Selected |
|--------|-------------|----------|
| Nessun titolo, solo il valore | Coerente con "nessun elemento non necessario" | ✓ |
| Titolo "Tachimetro" in cima | Valorizza la zona titolo dell'host | |

**User's choice:** Nessun titolo, solo il valore

| Option | Description | Selected |
|--------|-------------|----------|
| "Ricerca segnale..." | Tronca solo "GPS" dalla versione phone | ✓ |
| "Nessun segnale" | Più corto, comunica lo stato invece dell'azione | |

**User's choice:** "Ricerca segnale..."

---

## Mitigazione quota refresh

| Option | Description | Selected |
|--------|-------------|----------|
| 1Hz pieno, poi mitigare solo se serve | Implementa il refresh alla stessa cadenza del telefono (SC3), verifica empiricamente (SC4); mitiga solo se necessario | ✓ |
| Cadenza più prudente fin da subito | Es. refresh ogni 2s sull'auto — diverge da SC3 | |

**User's choice:** 1Hz pieno, poi mitigare solo se serve

| Option | Description | Selected |
|--------|-------------|----------|
| Throttle il refresh solo sull'auto | Riduce la cadenza solo lato auto (es. 2-3s), telefono resta a 1Hz | ✓ |
| Fermarsi e rivalutare l'approccio | Torna alla ricerca (Surface/NAVIGATION) prima di proseguire | |

**User's choice:** Throttle il refresh solo sull'auto
**Notes:** Accettato di rinegoziare esplicitamente SC3 solo in questo scenario, non come compromesso silenzioso.

---

## Icona app Android Auto

| Option | Description | Selected |
|--------|-------------|----------|
| Riusa ic_launcher esistente | Stessa icona del launcher telefono, zero lavoro grafico aggiuntivo | ✓ |
| Icona dedicata per l'auto | Asset nuovo pensato per la lista app Android Auto | |

**User's choice:** Riusa ic_launcher esistente

---

## Checkpoint verifica DHU

| Option | Description | Selected |
|--------|-------------|----------|
| Alcuni minuti (5-10 min) | Sessione simulata breve ma continua a 1Hz | ✓ |
| Sessione più lunga (15-30 min) | Simula meglio un tragitto reale | |

**User's choice:** Alcuni minuti (5-10 min)

| Option | Description | Selected |
|--------|-------------|----------|
| Checkpoint umano (tu su Windows) — telefono fisico | Come nelle fasi precedenti, richiede setup Windows/DHU su telefono reale | |
| Automatizzato dove possibile + conferma finale tua | Claude prepara script/istruzioni riproducibili, conferma finale resta umana | (variante) |
| **Other:** "L'ideale sarebbe su emulatore in Android Studio, come fallback automatizzato" | Risposta libera dell'utente | ✓ |

**User's choice:** Verifica su AVD emulator in Android Studio (non telefono fisico), automatizzata dove possibile, con conferma finale umana.
**Notes:** Claude ha segnalato che questo diverge dall'assunzione "telefono reale via USB" di `.planning/research/PITFALLS.md` Pitfall 7, ma è coerente con la documentazione ufficiale DHU (supporta anche emulatore API 23+). L'utente ha confermato esplicitamente questa lettura in un turno di conferma successivo. Il telefono fisico resta necessario solo per la Fase 11 (background location su strada), fuori scope qui.

---

## Claude's Discretion

- Nome esatto della risorsa stringa per "Ricerca segnale..." (es. `car_searching_gps_signal`).
- Scelta del template specifico della Car App Library (`PaneTemplate` vs altro) per comporre numero + unità separati.
- Meccanismo esatto di automazione del test DHU (script/tooling ADB specifico).

## Deferred Ideas

None — discussion stayed within phase scope.
