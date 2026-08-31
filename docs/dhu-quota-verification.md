# Verifica quota refresh Android Auto (DHU)

Runbook riproducibile per il gate SC4 della Fase 8 (`08-fondamenta-condivise-e-velocit-sullo-schermo-auto`):
dimostrare empiricamente che un refresh continuo a 1 Hz del `PaneTemplate` di `SpeedScreen` non
fa chiudere l'app dall'host Android Auto, prima di considerare il refresh a piena cadenza 1 Hz
definitivo per l'intera milestone v2.0 (D-06).

## Perche' questa verifica esiste

La ricerca di progetto (`.planning/research/STACK.md`, `.planning/research/PITFALLS.md` Pitfall 2)
segnala che i template non-Surface della Car App Library (`PaneTemplate`/`ListTemplate`/`GridTemplate`)
sono soggetti a una quota host di **5 template per task**: se l'app supera la quota, l'host mostra
un errore e chiude l'app. `SpeedScreen` ricostruisce il `PaneTemplate` una volta al secondo,
indefinitamente per l'intera durata di una guida (D-05, nessun throttle preventivo). La
documentazione ufficiale Google **non chiarisce** se un refresh dello stesso template, con solo
il contenuto numerico cambiato, sia esente dalla quota o venga conteggiato come un nuovo push.
D-06 rende questa domanda un gate esplicito, bloccante, invece di un dettaglio da scoprire durante
una guida reale in una fase successiva.

## Prerequisiti

- **AVD di Android Studio con Google Play, API 30+** (non un emulatore senza Google Play Services,
  necessario per Android Auto). Un telefono fisico **NON serve** per questa specifica verifica
  (D-09): la documentazione ufficiale del Desktop Head Unit supporta anche un emulatore. Questo e'
  un cambio consapevole rispetto all'assunzione iniziale di `.planning/research/PITFALLS.md`
  (Pitfall 7, "richiede un telefono reale via USB") -- quell'assunzione riguardava il collegamento
  USB del DHU a un telefono fisico, ma la documentazione Android for Cars conferma il supporto
  emulatore. Il telefono fisico resta necessario solo per la **Fase 11** (comportamento
  background-location a telefono bloccato durante una guida reale), fuori scope qui.
- **Desktop Head Unit** installato dall'SDK Manager di Android Studio (pacchetto
  `extras/google/auto`, contiene `desktop-head-unit.exe` sotto `<sdk>\extras\google\auto\`).
- **Android Auto installato sull'AVD**, con **Developer Mode** attivo e l'**overlay di debug**
  abilitato (tocco ripetuto sul numero di versione in Impostazioni > Info di Android Auto, poi
  toggle esplicito "Attiva modalita' sviluppatore" e "Avvia server head unit").
- **Permesso `ACCESS_FINE_LOCATION` gia' concesso** all'app sull'AVD (dalla UI del telefono, prima
  della sessione -- la richiesta del permesso dallo schermo auto e' fuori scope, Fase 9/AA-04).
- **Posizione simulata in movimento** sull'AVD (Extended Controls -> Location -> route/playback),
  cosi' che `SpeedState` sia `Reading` (non `Searching`) per la maggior parte della sessione e il
  refresh mostri cifre di velocita' che cambiano davvero, non solo "Ricerca segnale...".

## Procedura passo-passo

1. Avviare l'AVD da Android Studio (o da riga di comando) e attendere il boot completo.
2. Installare la build debug: `./gradlew.bat :app:installDebug`
3. Aprire l'app sull'AVD e concedere il permesso di localizzazione se richiesto.
4. Attivare una posizione simulata in movimento: Extended Controls (icona "..." della finestra
   emulatore) -> Location -> caricare/riprodurre un percorso (route playback).
5. Avviare lo script di misura con la durata scelta (600 secondi = 10 minuti, estremo alto della
   finestra 5-10 minuti di D-08):
   ```
   powershell -File scripts/dhu-quota-check.ps1 -DurationSeconds 600
   ```
6. Quando lo script lo chiede (dopo aver aperto il forward `adb forward tcp:5277 tcp:5277` e
   avviato la cattura logcat), avviare `desktop-head-unit.exe` dall'SDK
   (`<sdk>\extras\google\auto\desktop-head-unit.exe`) e selezionare **Tachimetro** nella lista app
   dell'head unit.
7. Premere INVIO nello script solo quando l'app Tachimetro e' visibile sullo schermo del DHU --
   questo avvia il cronometro e il campionamento periodico del PID.
8. Lasciare girare la sessione per l'intera durata scelta, **osservando continuamente lo schermo
   del DHU** (i punti da confermare a occhio sono elencati sotto).
9. Al termine, lo script stampa e salva un riepilogo in `build/dhu-quota/` con l'esito euristico.

## Cosa osservare a occhio durante la sessione

Questi punti sono l'input diretto del checkpoint umano del Task 3 -- nessuno di questi e'
verificabile da codice:

- Il numero sullo schermo auto **cambia visibilmente** e corrisponde al numero mostrato
  sull'AVD/telefono nello stesso istante.
- Nei momenti di perdita del segnale simulato (fermare la riproduzione del percorso o disattivare
  la posizione per oltre 5 secondi), lo schermo auto passa a **"Ricerca segnale..."** -- mai
  bloccato su un valore ormai scaduto.
- **Nessun ritorno improvviso** alla lista app dell'head unit durante la sessione.
- **Nessun messaggio di errore** mostrato dall'host (es. schermate di quota superata).

## Criteri di esito

Le stesse soglie usate dallo script (`scripts/dhu-quota-check.ps1`):

| Esito | Condizione |
|-------|------------|
| `FAIL` | Il PID di `com.sed.tachimetro` e' sparito o e' cambiato durante la sessione, **oppure** il conteggio dei refresh `onGetTemplate #` si e' fermato per piu' di 30 secondi consecutivi prima della fine |
| `PASS` | Il processo e' rimasto vivo per tutta la durata **e** la cadenza media osservata e' compresa tra 0,8 e 1,2 refresh/secondo |
| `INCONCLUSIVO` | Ogni altro caso |

Il gate SC4 richiede, oltre all'esito euristico `PASS` dello script, la conferma visiva umana dei
punti elencati sopra -- l'uno non sostituisce l'altro (D-10).

## Contingenza se FAIL (D-07)

Se la verifica empirica rivela che la quota si esaurisce davvero durante una sessione continua
(l'host chiude l'app o interrompe i refresh), la mitigazione concordata in fase di
discussione (`08-CONTEXT.md`) e':

- **Throttle del refresh SOLO lato auto**, applicato esclusivamente in `SpeedScreen`
  (`app/src/main/java/com/sed/tachimetro/car/SpeedScreen.kt`) -- ad esempio ricostruire il
  template ogni 2-3 secondi invece che ogni secondo. Il **telefono resta a 1 Hz invariato**: il
  throttle non va MAI applicato in `GpsSpeedProvider` (che e' condiviso tra telefono e auto,
  D-00b) ne' in nessun altro punto che influenzi il comportamento del telefono.
- Questo comporta la **rinegoziazione esplicita di SC3** ("stessa cadenza del telefono, senza
  salti ne' disallineamenti") con l'utente -- non e' un compromesso silenzioso da applicare
  autonomamente. L'esecutore che rileva un FAIL riporta il risultato e attende la decisione
  esplicita dell'utente prima di implementare qualunque throttle.
- L'opzione di ripiegare sul percorso Surface/`NAVIGATION` (accesso diretto al canvas, categoria
  `NAVIGATION` invece di `POI`) resta **esplicitamente scartata** e **non va riaperta** -- decisa
  e chiusa in fase di ricerca/roadmap (`.planning/research/PITFALLS.md` Pitfall 1, PROJECT.md Key
  Decisions), non rimessa in discussione da un fallimento di questo gate.

## Cosa e' fuori scope di questa verifica

- Il comportamento a telefono bloccato/in background durante una guida reale (background
  location) -- verificato in **Fase 11**, richiede un dispositivo fisico reale, non un AVD/DHU.
- Un `HostValidator` reale al posto di `ALLOW_ALL_HOSTS_VALIDATOR` -- hardening esplicito di
  **Fase 11**, non anticipato qui (`08-UI-SPEC.md`, tabella manifest/categoria).
