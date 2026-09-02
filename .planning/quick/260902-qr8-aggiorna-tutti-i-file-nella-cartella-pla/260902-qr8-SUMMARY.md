---
phase: quick-260902-qr8
plan: 01
subsystem: playstore-publication
tags: [docs, playstore, data-safety, privacy-policy]

requires: []
provides:
  - "data_safety.md e privacy_policy.html (IT+EN) corretti: dichiarano le tre voci realmente persistite dalla v1.1 (velocità massima, distanza percorsa, preferenza schermo sempre acceso) invece delle due dichiarate erroneamente prima"
  - "content_rating.md e i due full_description.txt verificati contro strings.xml/AndroidManifest.xml alla v1.1: già accurati, non modificati"
affects: ["260902-qr8 plan 02 (v2.0/Android Auto): riparte da questa correzione e aggiunge un quarto valore persistito scoperto successivamente — car_location_denial_count — non ancora noto quando questo piano è stato eseguito"]

tech-stack:
  added: []
  patterns: []

key-files:
  created: []
  modified:
    - playstore/data_safety.md
    - playstore/privacy_policy.html
    - playstore/README.md

key-decisions:
  - "Nessuna modifica a playstore/apk/, playstore/screenshots/ o app/build.gradle.kts — il piano originale (checkpoint di decisione + build APK/AAB) è stato rimosso dall'orchestratore dopo che l'utente ha risposto 'il file aab lo creo io, tu pensa alla documentazione': la build release resta a carico dell'utente"

patterns-established: []

requirements-completed: [QUICK-260902-qr8]

# Metrics
duration: ~12min
completed: 2026-09-02
---

# Quick Task 260902-qr8 Plan 01: Correzione dati persistiti v1.1 Summary

**`data_safety.md` e `privacy_policy.html` dichiaravano solo due dei tre valori realmente persistiti dalla v1.1 (mancava la distanza percorsa) — corretti in entrambe le lingue; `content_rating.md` e i testi di listing verificati e confermati già accurati.**

## Performance

- **Duration:** ~12 min
- **Tasks:** 1 completato (Task 1 dell'unico piano rimasto dopo la rimozione dei task di build da parte dell'orchestratore)

## Accomplishments

- `playstore/data_safety.md`: la nota finale dichiarava «le uniche due voci persistite localmente (max velocità sessione, preferenza "schermo sempre acceso")» — corretta a tre voci, aggiunta la distanza percorsa (`DistanceStore`). Disambiguata la riga sulla posizione: nessuna coordinata GPS viene mai salvata su disco, ma la distanza percorsa (un totale aggregato in metri, non un tracciato) sì.
- `playstore/privacy_policy.html`: aggiunta la distanza percorsa all'elenco «Dati salvati localmente» in entrambe le sezioni IT ed EN, con la stessa precisazione (numero aggregato, non tracciato di posizioni). Data di aggiornamento portata a 2026-09-02 in entrambe le lingue. I due placeholder email (righe IT/EN) lasciati intatti come da vincolo del piano.
- Verificati come già accurati e NON modificati: `content_rating.md` (nessun permesso INTERNET, nessun SDK pubblicitario, nessun UGC — tutto confermato contro AndroidManifest.xml), `listing/it/full_description.txt` e `listing/en/full_description.txt` (label "Azzera"/"Sempre acceso", distanza in basso a destra, icona di ricarica, "Android 11 e versioni successive" ↔ `minSdk = 30`), le release notes esistenti.
- `playstore/README.md` riscritto per riflettere lo stato reale: tabella "Contenuto" con colonna Stato, sezione dedicata ai tre dati persistiti dichiarati, passi manuali rinumerati (rigenerazione APK/AAB a carico dell'utente, firma, screenshot ancora v1.0, hosting privacy policy + email di contatto, form Play Console, versionamento).

## Files Created/Modified
- `playstore/data_safety.md` — nota finale e riga "Posizione" corrette a tre voci persistite
- `playstore/privacy_policy.html` — distanza percorsa aggiunta IT+EN, date aggiornate
- `playstore/README.md` — riscritto sullo stato reale post-verifica

## Decisions Made

Il piano originale (generato dal planner) apriva con un checkpoint di decisione sulla sorgente di build (tag `v1.1` vs `HEAD`, per via del codice Android Auto non rilasciato già presente su `HEAD` con `HostValidator.ALLOW_ALL_HOSTS_VALIDATOR` non indurito) seguito da un task di build automatico. Prima di eseguire, l'orchestratore ha posto la domanda del checkpoint all'utente, che ha risposto: **"il file aab lo creo io, tu pensa alla documentazione"**. L'orchestratore ha quindi editato il piano rimuovendo i Task 1 (checkpoint) e 2 (build), lasciando solo il Task di verifica testi + README (rinumerato Task 1), e impostando `user_setup` per registrare che la build resta a carico dell'utente. Nessun file sotto `playstore/apk/` è stato toccato da questo piano.

## Deviations from Plan

**Deviazione di processo (non di codice):** il piano assegnato all'esecutore era già stato ridotto dall'orchestratore rispetto a quanto generato dal planner (vedi sopra) — non è una deviazione presa dall'esecutore, ma una modifica pre-esecuzione del piano stesso.

**Nota retroattiva (scoperta DOPO il completamento di questo piano, non durante):** mentre l'esecutore lavorava in un worktree isolato, l'utente ha bumpato `app/build.gradle.kts` a `versionCode = 3` / `versionName = "2.0"` (commit `69de1e2`) e, alla domanda successiva dell'orchestratore, ha confermato di voler descrivere la v2.0/Android Auto nel pacchetto playstore. Questo rende **superata** la sezione "Versionamento" e il banner di avvertenza in cima al `README.md` scritto da questo piano (che assumevano "resta sulla 1.1, non menzionare Android Auto finché la Fase 11 non è chiusa"). La correzione è demandata al piano 02 dello stesso quick task (`260902-qr8-02-PLAN.md`), che riscrive `README.md` sullo stato v2.0 e aggiunge un quarto valore persistito (`car_location_denial_count`, contatore di rifiuti del permesso su schermo auto) scoperto solo durante la pianificazione del piano 02. Il lavoro di QUESTO piano (correzione dei tre valori persistiti v1.1, verifica delle altre affermazioni) resta corretto e valido — la distanza percorsa era davvero mancante indipendentemente da quale versione si stia descrivendo.

## Issues Encountered

**Perdita del worktree pre-rescue:** l'orchestratore ha eseguito il merge del branch worktree e la rimozione del worktree (`git worktree remove --force`) senza prima eseguire il passo di "rescue" previsto dal workflow `/gsd:quick` per un `SUMMARY.md` non ancora committato (l'esecutore aveva scritto `260902-qr8-SUMMARY.md` nel working tree del worktree ma non lo aveva committato, come da istruzione esplicita "l'orchestratore gestisce il commit dei docs"). Il file è stato perso con la rimozione del worktree. Questo file `SUMMARY.md` è stato **ricostruito a posteriori dall'orchestratore** leggendo il diff reale del commit di merge (`8cd7793`) e il report testuale restituito dall'esecutore al termine del task — non da una copia del file originale, che non esiste più.

## User Setup Required

Nessuna azione di configurazione: la rigenerazione di `playstore/apk/tachimetro-1.1-unsigned.apk`/`.aab` resta esplicitamente a carico dell'utente (vedi Decisions Made).

## Next Phase Readiness

- Piano 02 (stesso quick task): riscrive `README.md`/listing/release-notes/data-safety/privacy-policy per descrivere la v2.0 (Android Auto), ora che l'utente ha confermato la direzione.
- `playstore/apk/` resta invariato (ancora i due file 1.0 obsoleti) — in attesa che l'utente depositi gli artefatti 1.1/2.0.
- `playstore/screenshots/` resta invariato — ancora i 5 PNG v1.0, da ricatturare a mano.

## Self-Check: PASSED (ricostruito)

- FOUND: playstore/data_safety.md (modificato, diff confermato in `8cd7793`)
- FOUND: playstore/privacy_policy.html (modificato, diff confermato in `8cd7793`)
- FOUND: playstore/README.md (modificato, diff confermato in `8cd7793`)
- FOUND commit: `2848ecd` (docs, task originale nel worktree) → mergiato in `8cd7793`

---
*Phase: quick-260902-qr8*
*Completed: 2026-09-02*
