# Quick Task 260710-tuh — Summary

**Description:** Impedire che il numero della velocità (`messageText`) vada mai a capo in portrait e landscape: deve rimpicciolirsi invece di andare su due righe.
**Date:** 2026-07-10
**Status:** ✅ Completato e verificato su emulatore (checkpoint umano approvato)
**Commit (codice):** `3600992`

## Problema

`messageText` (il numero dominante della velocità) usa `autoSizeTextType="uniform"` (12–300sp) ma non aveva alcun vincolo `maxLines`. Senza limite di righe, l'algoritmo autosize uniform mandava le cifre a capo su una 2ª riga invece di rimpicciolirle — visibile con numeri a 2 cifre in portrait (larghezza minore), rendendo il numero poco leggibile (contro il core value dell'app).

## Fix

`messageText` è la **stessa** TextView riusata per due tipi di contenuto, veicolati da due helper chokepoint già esistenti in `MainActivity.kt`. Il fix imposta `maxLines` dinamicamente in entrambi:

- `applySpeedAutosize()` (stato `Reading`, cifre, cap 300sp) → `messageText.maxLines = 1`: le cifre non vanno **mai** a capo, l'autosize le rimpicciolisce per stare su una riga.
- `applyMessageAutosize()` (Searching/NoSignal/showReady/showDenied, messaggi, cap 56sp) → `messageText.maxLines = Integer.MAX_VALUE`: ripristina esplicitamente il wrapping libero, così un precedente `applySpeedAutosize()` non lascia `maxLines = 1` bloccando il wrapping dei messaggi multi-parola.

Impostare `maxLines` esplicitamente in **entrambi** i chokepoint rende ogni percorso di transizione di stato self-consistent indipendentemente dall'ordine. `activity_main.xml` e le costanti autosize sono lasciati intatti di proposito: un `maxLines="1"` statico in XML avrebbe rotto il wrapping dei messaggi di stato.

## File modificati

- `app/src/main/java/com/sed/tachimetro/MainActivity.kt` (+9 righe, commenti inclusi)

## Verifica

- `./gradlew.bat :app:compileDebugKotlin` → passa
- Checkpoint umano su emulatore — **approvato** (3 casi):
  1. Portrait, velocità a 2/3 cifre → numero su una sola riga, rimpicciolito, mai a capo
  2. Messaggio di stato ("Ricerca segnale GPS…") → wrappa ancora normalmente (nessuna regressione)
  3. Landscape → numero su una riga
