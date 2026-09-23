# Phase 9: Permesso di Localizzazione dallo Schermo Auto - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-09-02
**Phase:** 9-Permesso di Localizzazione dallo Schermo Auto
**Areas discussed:** Attesa "controlla il telefono", Rifiuto: messaggio e azione, Quando scatta la richiesta, Branding del dialogo

---

## Attesa "controlla il telefono"

| Option | Description | Selected |
|--------|-------------|----------|
| "Controlla il telefono" | Breve e diretto, stesso tono/lunghezza di car_searching_gps_signal | ✓ |
| "Permesso richiesto sul telefono" | Più esplicito sul motivo, leggermente più lungo | |
| Riusa "Ricerca segnale..." | Nessuna stringa nuova, ma ambiguo | |

**User's choice:** "Controlla il telefono"
**Notes:** Il dialogo di sistema per il permesso appare sul telefono, non sull'auto (limite strutturale di CarContext.requestPermissions(), Pitfall 4).

---

## Rifiuto: messaggio e azione

| Option | Description | Selected |
|--------|-------------|----------|
| Stessa idea del telefono, accorciata | Mirror di permission_denied ma più corto, coerente con D-02 Fase 8 | ✓ |
| Testo diverso, orientato all'azione | Es. "Concedi il permesso dal telefono per vedere la velocità" | |

**User's choice:** Stessa idea del telefono, accorciata

| Option | Description | Selected |
|--------|-------------|----------|
| Sì, azione di retry sullo schermo auto (consigliato) | Mirror del retryButton, un tocco rilancia la richiesta | ✓ |
| No, solo messaggio — l'utente deve andare sul telefono | Più semplice, ma meno comodo | |

**User's choice:** Sì, azione di retry sullo schermo auto

| Option | Description | Selected |
|--------|-------------|----------|
| Sì, stessa distinzione del telefono | Mirror esatto: messaggio/azione cambia se il rifiuto è permanente | ✓ |
| No, messaggio e retry sempre uguali | Più semplice, nessuna UX dedicata al caso permanente | |

**User's choice:** Sì, stessa distinzione del telefono
**Notes:** CarContext non espone un equivalente diretto di shouldShowRequestPermissionRationale() — meccanismo di rilevamento lasciato alla ricerca (vedi CONTEXT.md nota sotto D-04).

---

## Quando scatta la richiesta

| Option | Description | Selected |
|--------|-------------|----------|
| Automatico (consigliato) | Coerente con "nessun menu", mirror di MainActivity.checkAndRequestPermission() | ✓ |
| Richiede un tocco sull'auto prima | Un passo in più, più esplicito/intenzionale | |

**User's choice:** Automatico
**Notes:** Il retry dopo un rifiuto (area precedente) resta invece un'azione esplicita a tocco, per non rilanciare in loop un dialogo già rifiutato.

---

## Branding del dialogo

| Option | Description | Selected |
|--------|-------------|----------|
| Dialogo di default del sistema (consigliato) | Nessuna personalizzazione, coerente con D-03 Fase 8 | ✓ |
| Dialogo personalizzato (tema/branding) | Investire in androidx.car.app.theme | |

**User's choice:** Dialogo di default del sistema

---

## Claude's Discretion

- Testo esatto italiano per le nuove risorse stringa (nomi e wording preciso entro il tono/contenuto bloccato).
- Meccanismo tecnico per rilevare lo stato "permanentemente negato" dallo Screen auto — il researcher deve investigare le opzioni disponibili in CarContext/callback di requestPermissions().
- Meccanismo reattivo esatto per la transizione automatica post-concessione (SC2 di roadmap) — pattern coerente con permissionGranted di MainActivity.
- Forma esatta dell'azione di retry sul template auto (componente Car App Library specifico).

## Deferred Ideas

None — discussion stayed within phase scope.
