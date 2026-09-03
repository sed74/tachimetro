# Phase 11: Hardening di Produzione e Verifica su Dispositivo Reale - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-09-03
**Phase:** 11-Hardening di Produzione e Verifica su Dispositivo Reale
**Areas discussed:** HostValidator (SC1), Background GPS fallback (SC2), Hardening SC3, Test su strada (SC2)

---

## HostValidator (SC1)

| Option | Description | Selected |
|--------|-------------|----------|
| Reale ovunque, anche in debug | Debug e release usano lo stesso HostValidator reale. Dev/prod parity, ma richiede configurare la firma/allow-list che accetta anche DHU. | |
| Permissivo in debug, reale in release | I build di debug restano su ALLOW_ALL_HOSTS_VALIDATOR (comodo per sviluppo/DHU), solo i build di release usano l'allow-list reale. | ✓ |

**User's choice:** Permissivo in debug, reale in release (opzione raccomandata)
**Notes:** Comodità di sviluppo prevale sulla dev/prod parity totale; la differenza va documentata esplicitamente (D-01/D-02 in CONTEXT.md).

---

## Background GPS (SC2)

| Option | Description | Selected |
|--------|-------------|----------|
| Documentare il limite, nessun nuovo permesso | Se il gap è reale, si registra come limite di piattaforma noto (SC2 lo ammette esplicitamente) e non si aggiunge ACCESS_BACKGROUND_LOCATION. | ✓ |
| Valutare ACCESS_BACKGROUND_LOCATION se necessario | Si è disposti ad aggiungere il permesso di localizzazione in background pur di garantire l'aggiornamento continuo. | |

**User's choice:** Documentare il limite, nessun nuovo permesso (opzione raccomandata)
**Notes:** Coerenza con la filosofia di minimizzazione dei permessi del progetto (Phase 2 threat model T-02-EP) ed evitare l'onere di disclosure data-safety su Play Store. Precisato in D-04: questa decisione preclude esplicitamente ACCESS_BACKGROUND_LOCATION come piano di contingenza per questa fase.

---

## Hardening SC3

| Option | Description | Selected |
|--------|-------------|----------|
| Testa prima, patcha solo se si rompe | Si esegue il test di connessione/disconnessione rapida e si aggiungono guardie difensive SOLO se il test rivela un crash o uno stato incoerente. | ✓ |
| Aggiungere guardie difensive preventive | Si aggiungono meccanismi di debounce/guardia preventivamente, anche senza un fallimento osservato. | |

**User's choice:** Testa prima, patcha solo se si rompe (opzione raccomandata)
**Notes:** Coerente con CLAUDE.md — niente codice per scenari ipotetici, niente design per requisiti futuri non confermati.

---

## Test su strada (SC2)

| Option | Description | Selected |
|--------|-------------|----------|
| Stessa soglia di Fase 8 (5-10 minuti) | Riusa la stessa soglia già validata in Fase 8 (D-08) per la quota di refresh, ma con telefono bloccato/schermo spento durante un tragitto reale. | ✓ |
| Test più lungo (es. 20-30 minuti) | Sessione più lunga per essere più sicuri contro degradi intermittenti. | |

**User's choice:** Stessa soglia di Fase 8 (5-10 minuti) (opzione raccomandata)
**Notes:** Riusare una soglia già validata empiricamente nel progetto piuttosto che introdurne una nuova senza precedenti.

---

## Claude's Discretion

- Meccanismo tecnico esatto per il debug/release split del `HostValidator` (quale flag/build-type, quale risorsa di allow-list)
- Contenuto esatto dell'allow-list reale (quali host/signature Android Auto legittimi includere)
- Meccanismo tecnico per rilevare/prevenire race condition di connessione/disconnessione rapida, se il test SC3 rivela un problema reale
- Formato/luogo esatto della documentazione del limite di piattaforma se il test SC2 fallisce (coerente con il pattern già usato per Fase 8/9)

## Deferred Ideas

None — discussion stayed within phase scope.
