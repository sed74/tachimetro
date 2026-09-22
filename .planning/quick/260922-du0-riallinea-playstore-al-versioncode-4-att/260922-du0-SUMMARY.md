---
phase: quick-260922-du0
plan: 01
subsystem: playstore-release-docs
tags: [docs, playstore, versioning, release]
requires: []
provides:
  - "playstore/README.md allineato a versionCode = 4"
  - "canale test aperto documentato come destinazione del build 2.0"
affects:
  - playstore/README.md
tech-stack:
  added: []
  patterns: []
key-files:
  created: []
  modified:
    - playstore/README.md
decisions:
  - "Il versionName resta \"2.0\": il bump 3 -> 4 e' solo tecnico (Play Console rifiuta un versionCode gia' usato), il contenuto funzionale del rilascio non cambia"
  - "La tabella artefatti e il passo 1 restano invariati: playstore/apk/ contiene ancora solo i due file 1.0, quindi il testo esistente era gia' onesto"
  - "Il .aab e' documentato come formato richiesto sia per il canale di test aperto sia per la produzione"
metrics:
  duration: ~8 min
  completed: 2026-09-22
requirements: [QUICK-260922-du0]
---

# Quick 260922-du0: Riallineamento playstore a versionCode 4 — Summary

Riallineato `playstore/README.md` al `versionCode = 4` reale di `app/build.gradle.kts`, riscritta la nota di versionamento ormai eseguita (prossimo caricamento a 5) e documentato il canale **test aperto** come destinazione del build 2.0 firmato.

## Cosa e' stato fatto

### Task 1 — Riferimenti di versione e sezione Versionamento (commit `745e4eb`)

- Tutte e quattro le occorrenze di `versionCode = 3` sostituite con `versionCode = 4`: intestazione, paragrafo "Questo pacchetto descrive la v2.0", passo 1 della rigenerazione APK/AAB, sezione 6 "Versionamento".
- Intestazione: separate le due date: i testi restano verificati contro il codice il **2026-09-02**, il riallineamento dei riferimenti di versione e' del **2026-09-22**. Non lascia piu' intendere che tutto sia stato riverificato oggi.
- Paragrafo v2.0: precisato che il `versionName` resta **"2.0"** e che il solo `versionCode` e' stato portato a 4 (commit `b62b879`) per poter caricare il pacchetto su Play Console.
- Sezione 6 riscritta: la versione corrente e' `versionName = "2.0"` / `versionCode = 4`; il bump "da 3 a 4" e' registrato come **gia' eseguito e committato**; il **prossimo** caricamento dovra' usare `versionCode = 5` (con aggiornamento del `versionName` solo se cambia il contenuto funzionale). Rimossa del tutto la vecchia istruzione "Il prossimo rilascio dovra' incrementare `versionCode` (a 4)", che descriveva un'azione gia' compiuta.
- La frase storica usa la formulazione "incrementato da 3 a 4", evitando di reintrodurre la stringa letterale `versionCode = 3` (che avrebbe fatto fallire il gate).

### Task 2 — Tabella artefatti e canale test aperto (commit `85e843b`)

- Verificato con `ls playstore/apk/`: la cartella contiene esattamente `tachimetro-1.0-unsigned.apk` e `tachimetro-1.0.aab`, come accertato in pianificazione. **Nessun artefatto 2.0 inventato**: nomi file, stato **OBSOLETO** e la frase "I due file in `apk/` sono ancora build **1.0**" sono stati confermati e lasciati invariati.
- Riga della tabella per `apk/tachimetro-1.0.aab`: la descrizione ora dice che il `.aab` e' il formato richiesto da Play Console **sia per il canale di test aperto sia per la produzione**.
- Stessa precisazione applicata alla frase conclusiva del passo 1.
- Aggiunto un punto elenco fra le "Conseguenze pratiche": il build 2.0 / `versionCode` 4 e' destinato al canale **test aperto** di Play Store; l'utente genera il build di release firmato e lo pubblica li' (decisione registrata in `.planning/HANDOFF.json` e nel `.continue-here.md` della Fase 11); lo stesso binario serve come artefatto per i checkpoint SC1/SC3 della Fase 11 e resta un binario release (`DEBUGGABLE=false`) anche se ri-firmato da Play App Signing; la pubblicazione in produzione resta un passo successivo non ancora eseguito.
- Mantenuti intatti il punto elenco sulla costruzione da `HEAD`, quello sui testi che menzionano Android Auto e i passi 2, 3, 4, 5.

## Verifica

| Criterio | Esito |
|---|---|
| Zero occorrenze di `versionCode = 3` in `playstore/` | PASS (`grep -rn versionCode playstore/ --include=*.md --include=*.txt --include=*.html`) |
| Almeno 4 occorrenze di `versionCode = 4` | PASS (4) |
| `versionName = "2.0"` invariato | PASS (2 occorrenze letterali, come prima) |
| Sezione 6 indica `versionCode = 5` come prossimo | PASS |
| Vecchia istruzione "dovra' incrementare a 4" rimossa | PASS |
| Nota `ALLOW_ALL_HOSTS_VALIDATOR` ancora presente e byte-identica | PASS (diff vs base: 0 righe toccate nella sezione) |
| Ogni file realmente presente in `playstore/apk/` e' nominato nel README | PASS (2/2) |
| "test aperto" citato almeno 2 volte | PASS (3) |
| Nessuna modifica sotto `app/`, `docs/`, `.planning/phases/` | PASS (`git diff --name-only` vuoto) |
| Nessun file di `playstore/` diverso da `README.md` modificato | PASS |
| Nessuna cancellazione di file nei commit | PASS (`git diff --diff-filter=D` vuoto) |

Diff complessivo rispetto alla base `771754b`: `playstore/README.md | 26 insertions(+), 9 deletions(-)` — unico file toccato.

## Zona vietata

La sezione "## Rischio noto accettato per questo rilascio (nota interna)" (ALLOW_ALL_HOSTS_VALIDATOR / Fase 11) e' rimasta **byte-identica**: appartiene al Task 3 del piano 11-03, bloccato su checkpoint umano. Verificato con un diff filtrato sulle stringhe della sezione (0 righe aggiunte o rimosse).

## Deviazioni dal piano

**1. [Rule 3 - Blocking] Path dei comandi di verifica adattato al worktree**
- **Trovato durante:** Task 1
- **Problema:** i blocchi `<automated>` del piano fanno `cd "C:/Users/fedes/AndroidStudioProjects/Tachimetro"`, cioe' il checkout principale, mentre l'esecuzione avviene in un worktree isolato. Eseguiti li', avrebbero verificato file non modificati.
- **Fix:** stessi identici comandi eseguiti con `cd` sulla radice del worktree. Nessuna modifica alla logica dei gate.
- **File modificati:** nessuno.

**2. [Rule 3 - Blocking] Gate composti spezzati in comandi separati**
- **Trovato durante:** Task 1 e Task 2
- **Problema:** la sandbox del worktree rifiuta le catene `&&` lunghe che includono operazioni git ("command too complex to verify").
- **Fix:** gli stessi controlli sono stati eseguiti come comandi separati, con lo stesso esito atteso. Tutti i criteri dei gate sono stati verificati (tabella sopra).
- **File modificati:** nessuno.

Nessun'altra deviazione: nessun bug trovato, nessuna funzionalita' mancante, nessuna decisione architetturale.

## Note per il prossimo passo

- Il README e' ora deploy-ready per il caricamento del build 2.0 / `versionCode` 4 sul canale di test aperto.
- Resta aperto il Task 3 del piano 11-03 (aggiornamento della nota di rischio ALLOW_ALL_HOSTS_VALIDATOR), bloccato su checkpoint umano.
- Restano aperti i passi manuali 1-5 del README (rigenerazione artefatti 2.0, firma, screenshot, hosting privacy policy, form Play Console), tutti a carico dell'utente e invariati rispetto a prima.

## Self-Check: PASSED

- `playstore/README.md` — presente e modificato
- `.planning/quick/260922-du0-riallinea-playstore-al-versioncode-4-att/260922-du0-SUMMARY.md` — presente
- commit `745e4eb` — presente in `git log`
- commit `85e843b` — presente in `git log`
