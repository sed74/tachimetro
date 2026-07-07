# Phase 2: Motore GPS - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-07
**Phase:** 2-Motore GPS
**Areas discussed:** Soglia "nessun segnale", Architettura del dato velocità, Ciclo di vita aggiornamenti GPS, Filtro accuratezza GPS, Formato numero nel placeholder di test, Test su emulatore senza movimento reale

---

## Soglia "nessun segnale"

| Domanda | Opzione scelta |
|---------|-----------------|
| Quando mostrare "Ricerca segnale GPS..."? | Anche durante l'uso se manca un aggiornamento |
| Timeout per considerare il segnale perso durante l'uso | 5 secondi |
| Arrotondare a 0 sotto soglia minima da fermo? | Sì |
| Location senza hasSpeed() valido | Trattalo come 0 km/h |

---

## Architettura del dato velocità

| Domanda | Opzione scelta |
|---------|-----------------|
| Come esporre il valore di velocità? | Kotlin Flow/StateFlow (richiede kotlinx-coroutines-play-services) |
| Come rendere verificabile questa fase senza la UI finale? | Sostituisci temporaneamente "Pronto" col numero |

---

## Ciclo di vita aggiornamenti GPS

| Domanda | Opzione scelta |
|---------|-----------------|
| Quando avviare/fermare gli aggiornamenti? | onStart/onStop |
| Velocità massima in background (nota per Fase 4) | Continua a registrare solo mentre l'app è visibile |

**Nota:** l'utente ha menzionato il toggle "schermo sempre acceso" durante questa discussione — reindirizzato, appartiene già alla Fase 5 (Gestione Schermo).

---

## Filtro accuratezza GPS / Formato test / Verifica emulatore

| Domanda | Opzione scelta |
|---------|-----------------|
| Scartare letture GPS poco accurate? | Sì |
| Decimali nel placeholder di test | Numero intero (es. "42 km/h") |
| Come verificare su emulatore senza movimento reale | Route playback (Extended Controls → Location → Routes) |

---

## Claude's Discretion

- Conversione m/s → km/h, struttura interna delle classi del motore GPS, gestione permessi già negati (coperta dalla Fase 1)

## Deferred Ideas

- Toggle "Schermo sempre acceso" / "Schermo automatico" — già pianificato in Fase 5 (Gestione Schermo), fuori scope per questa fase
