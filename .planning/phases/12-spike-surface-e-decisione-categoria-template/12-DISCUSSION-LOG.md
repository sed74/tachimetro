# Phase 12: Spike Surface e Decisione Categoria/Template - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-09-23
**Phase:** 12-spike-surface-e-decisione-categoria-template
**Areas discussed:** Struttura codice spike, Card POI e action strip, Protocollo di misura, Criteri di decisione

---

## Struttura codice spike

| Option | Description | Selected |
|--------|-------------|----------|
| Due flavor temporanei | Flavor spikePoi/spikeNav con manifest e builder propri, rimossi dopo la decisione | ✓ |
| Due branch git usa-e-getta | Un branch per percorso, codice comune duplicato/cherry-pick | |
| Modifica manuale sequenziale | Prima POI poi NAV sullo stesso codice | |

| Option | Description | Selected |
|--------|-------------|----------|
| Resta solo il percorso scelto | Scheletro per Fase 13, l'altro eliminato | ✓ |
| Tutto usa-e-getta | Solo misure e decisione, Fase 13 da zero | |

| Option | Description | Selected |
|--------|-------------|----------|
| Testo fisso + rettangoli aree | "888" centrato + contorni stable/visible area | ✓ |
| Solo testo fisso centrato | Aree solo nei log | |
| Velocità GPS vera | Anticipa Fase 13 | |

| Option | Description | Selected |
|--------|-------------|----------|
| Flusso permesso invariato, Surface solo con Granted | Macchina a stati intatta | ✓ |
| Surface sempre, permesso ignorato | Da ricollegare a fine fase | |

| Option | Description | Selected |
|--------|-------------|----------|
| Mai sul Play Store, solo DHU in debug | Nessun rischio revisione | ✓ |
| Test interno ammesso per head unit reale | Installazione da Play | |

**User's choice:** tutte le opzioni raccomandate.

---

## Card POI e action strip

| Option | Description | Selected |
|--------|-------------|----------|
| Più varianti, si misura la card più piccola | Almeno MessageTemplate e ListTemplate | ✓ |
| Solo MessageTemplate minimo | Un tentativo | |
| Solo PaneTemplate (come v2.0) | Con icona app già osservata | |

| Option | Description | Selected |
|--------|-------------|----------|
| Minimo indispensabile, da decidere dopo | Segnaposto nello spike | ✓ |
| Anche la velocità nella card | Due numeri sullo schermo | |

| Option | Description | Selected |
|--------|-------------|----------|
| Decide Claude, la più discreta valida | Azione minima accettata da build() | ✓ |
| Azione utile (azzera/info) | Nessuna azione ovvia | |

**User's choice:** tutte le opzioni raccomandate.

---

## Protocollo di misura

| Option | Description | Selected |
|--------|-------------|----------|
| Tre: 800×480, 1280×720, 1920×1080 | Stesse di SC1 Fase 13 | ✓ |
| Solo default DHU | SC2 alla lettera | |
| Tre + profilo wide | es. 1920×720 | |

| Option | Description | Selected |
|--------|-------------|----------|
| Log + screenshot, script sul modello esistente | Tag DEBUG + dhu-quota-check.ps1 come modello | ✓ |
| Valori disegnati sulla Surface + screenshot | Senza logcat | |
| Entrambi | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Opzionale: sideload in auto se l'utente vuole | SC2 soddisfatto da DHU altrimenti | ✓ |
| Salta, basta DHU | | |
| Obbligatorio prima di decidere | | |

**User's choice:** tutte le opzioni raccomandate.
**Notes:** per rendere il sideload utile senza logcat, il Car API level viene anche scritto piccolo sulla Surface di spike (derivato, D-11).

---

## Criteri di decisione

| Option | Description | Selected |
|--------|-------------|----------|
| Soglia: POI se lo spazio basta | Criterio fissato ora | ✓ |
| Fedeltà alla spec D-14 | Preferenza NAV | |
| Sicurezza distribuzione | POI salvo illeggibilità | |
| Nessun criterio fissato | Giudizio sugli screenshot | |

| Option | Description | Selected |
|--------|-------------|----------|
| Relativa a NAVIGATION (≥70% altezza cifre) | Confronto diretto alla stessa risoluzione | ✓ |
| Assoluta (≥40% altezza schermo) | | |
| Giudizio visivo guidato | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Tentare NAV, prima solo canale chiuso | Rientro su POI se rifiutata | ✓ |
| NAV solo uso personale | Due app da mantenere | |
| Non si tenta, si rivaluta | | |

| Option | Description | Selected |
|--------|-------------|----------|
| 12-SPIKE-RESULTS.md + PROJECT.md | Con aggiornamento AA-05/AA-08/D-14 se POI | ✓ |
| Solo PROJECT.md | | |

**User's choice:** tutte le opzioni raccomandate; soglia 70% da confermare vedendo i numeri.

---

## Claude's Discretion

- Azione dell'action strip nello spike
- Nomi flavor/tag/classi, meccanismo di cambio variante card POI
- lockCanvas vs lockHardwareCanvas, gestione risoluzioni DHU e script

## Deferred Ideas

Nessuna.
