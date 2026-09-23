# Spike Surface Android Auto: misure POI vs NAVIGATION (Fase 12)

Runbook riproducibile per i gate SC1/SC2 della Fase 12 (`12-spike-surface-e-decisione-categoria-template`):
misurare su Desktop Head Unit (DHU), con il telefono fisico, quanto spazio resta davvero per le cifre
della velocita' nei due percorsi che danno accesso alla Surface di Android Auto, e raccogliere i numeri
su cui l'utente decide categoria e template (REL-01).

REL-01 **riapre di proposito** la strada Surface/`NAVIGATION` che in v2.0 era stata dichiarata
"esplicitamente scartata" (vedi la sezione "Contingenza se FAIL (D-07)" di
`docs/dhu-quota-verification.md`). Quella chiusura valeva per la milestone v2.0 con `PaneTemplate`;
v2.1 esiste proprio per rimettere la scelta sul tavolo, con misure concrete.

## Perche' questa verifica esiste

Per disegnare un numero grande a schermo pieno serve la Surface, e la Car App Library la concede solo
a due combinazioni di categoria e template:

- **POI + `MapWithContentTemplate`** (`@RequiresCarApi(7)`): categoria gia' usata dall'app, revisione
  Play Store senza sorprese, ma l'host impone una **card di contenuto obbligatoria** (message, list,
  pane o grid) che copre una parte della Surface. Quanto spazio porta via lo decidono le misure.
- **NAVIGATION + `NavigationTemplate`**: sulla Surface c'e' solo l'action strip (che l'host nasconde
  dopo ~10 s), quindi piu' spazio per le cifre, ma la categoria NAVIGATION espone al rischio di
  rifiuto in revisione per le linee guida di qualita' **NF-1** (navigazione turn-by-turn attesa) e
  **NF-6**.

Lo spike misura entrambe le strade con lo stesso disegno di prova (D-03) e alle stesse risoluzioni
(D-09). **La decisione resta dell'utente** (D-12, gate umano esplicito) e viene registrata come da
D-14 in `12-SPIKE-RESULTS.md` e in PROJECT.md Key Decisions.

Le build dello spike (flavor `spikePoi` / `spikeNav`) **non vanno mai sul Play Store**, in nessun
canale, nemmeno test interno (D-05): solo APK debug installati in locale.

## Prerequisiti

- **Telefono fisico OnePlus 8T** collegato via USB al PC, con debug USB autorizzato. Se sono collegati
  altri dispositivi o emulatori, passare `-Serial <serial>` allo script.
- **Android Auto** sul telefono in **Developer Mode** (tocco ripetuto sul numero di versione in
  Impostazioni > Info), con **"Avvia server head unit"** attivo dal menu sviluppatore.
- **Desktop Head Unit** installato dall'SDK Manager (pacchetto `extras/google/auto`):
  `<sdk>\extras\google\auto\desktop-head-unit.exe`.
- **Permesso di localizzazione gia' concesso** all'app dal telefono prima della sessione. Altrimenti
  lo schermo auto resta sui `PaneTemplate` del flusso permesso (D-04, invariato nello spike) e la
  Surface non viene mai mostrata. Nota: ogni installazione di un flavor diverso puo' richiedere di
  riconcederlo (stesso `applicationId`, ma una disinstallazione azzera i permessi).
- **Le tre configurazioni DHU** in `scripts/dhu/`: `dhu-800x480.ini`, `dhu-1280x720.ini`,
  `dhu-1920x1080.ini` (sezione `[general]`, chiave `resolution`), passate al DHU con
  `desktop-head-unit.exe -c <file>.ini`.
- `adb` (platform-tools) sul PATH.

## Procedura passo-passo

Ogni sessione e' un solo comando (D-01). Lo script `scripts/surface-spike-check.ps1`:

1. seleziona il telefono e installa la variante con Gradle
   (`nav` -> `:app:installSpikeNavDebug`; `poi-<card>` -> `:app:installSpikePoiDebug -PpoiCard=<card>`);
2. apre `adb forward tcp:5277 tcp:5277` e avvia la cattura logcat del tag `TachimetroSurface`;
3. si ferma e stampa il comando DHU esatto per la risoluzione scelta e il percorso dello screenshot;
4. dopo l'INVIO cattura per 20 s (`-DurationSeconds`), poi stampa e salva in `build/surface-spike/`
   la riga di tabella pronta da incollare.

### Le 15 sessioni (5 varianti x 3 risoluzioni, D-06 x D-09)

| # | Variante | Risoluzione | Comando |
|---|----------|-------------|---------|
| 1 | `nav` | `800x480` | `powershell -ExecutionPolicy Bypass -File scripts\surface-spike-check.ps1 -Variant nav -Resolution 800x480` |
| 2 | `nav` | `1280x720` | `powershell -ExecutionPolicy Bypass -File scripts\surface-spike-check.ps1 -Variant nav -Resolution 1280x720` |
| 3 | `nav` | `1920x1080` | `powershell -ExecutionPolicy Bypass -File scripts\surface-spike-check.ps1 -Variant nav -Resolution 1920x1080` |
| 4 | `poi-message` | `800x480` | `powershell -ExecutionPolicy Bypass -File scripts\surface-spike-check.ps1 -Variant poi-message -Resolution 800x480` |
| 5 | `poi-message` | `1280x720` | `powershell -ExecutionPolicy Bypass -File scripts\surface-spike-check.ps1 -Variant poi-message -Resolution 1280x720` |
| 6 | `poi-message` | `1920x1080` | `powershell -ExecutionPolicy Bypass -File scripts\surface-spike-check.ps1 -Variant poi-message -Resolution 1920x1080` |
| 7 | `poi-list` | `800x480` | `powershell -ExecutionPolicy Bypass -File scripts\surface-spike-check.ps1 -Variant poi-list -Resolution 800x480` |
| 8 | `poi-list` | `1280x720` | `powershell -ExecutionPolicy Bypass -File scripts\surface-spike-check.ps1 -Variant poi-list -Resolution 1280x720` |
| 9 | `poi-list` | `1920x1080` | `powershell -ExecutionPolicy Bypass -File scripts\surface-spike-check.ps1 -Variant poi-list -Resolution 1920x1080` |
| 10 | `poi-pane` | `800x480` | `powershell -ExecutionPolicy Bypass -File scripts\surface-spike-check.ps1 -Variant poi-pane -Resolution 800x480` |
| 11 | `poi-pane` | `1280x720` | `powershell -ExecutionPolicy Bypass -File scripts\surface-spike-check.ps1 -Variant poi-pane -Resolution 1280x720` |
| 12 | `poi-pane` | `1920x1080` | `powershell -ExecutionPolicy Bypass -File scripts\surface-spike-check.ps1 -Variant poi-pane -Resolution 1920x1080` |
| 13 | `poi-grid` | `800x480` | `powershell -ExecutionPolicy Bypass -File scripts\surface-spike-check.ps1 -Variant poi-grid -Resolution 800x480` |
| 14 | `poi-grid` | `1280x720` | `powershell -ExecutionPolicy Bypass -File scripts\surface-spike-check.ps1 -Variant poi-grid -Resolution 1280x720` |
| 15 | `poi-grid` | `1920x1080` | `powershell -ExecutionPolicy Bypass -File scripts\surface-spike-check.ps1 -Variant poi-grid -Resolution 1920x1080` |

Per risparmiare installazioni, le sessioni 2-3, 5-6, 8-9, 11-12, 14-15 (stessa variante della riga
precedente) possono aggiungere `-SkipInstall`: lo script controlla comunque nei log che variante e
card siano quelle attese.

### Per ogni sessione

1. Lanciare il comando della tabella dalla radice del repository.
2. **Chiudere e riaprire il DHU a ogni cambio di risoluzione**: la risoluzione si legge solo
   all'avvio. Usare il comando stampato dallo script, es.
   `desktop-head-unit.exe -c C:\...\Tachimetro\scripts\dhu\dhu-1280x720.ini`.
3. Aprire **Tachimetro** dalla lista app dell'head unit e attendere il disegno di prova.
4. **Screenshot manuale della finestra DHU** (Win+Shift+S oppure Strumento di cattura), salvato come
   `.planning/phases/12-spike-surface-e-decisione-categoria-template/spike-screenshots/<variante>-<risoluzione>.png`
   (es. `spike-screenshots/poi-list-1280x720.png`). Lo screenshot e' manuale perche'
   `adb screencap` cattura lo schermo del telefono, non la finestra del DHU sul PC.
5. Premere INVIO nello script e continuare a guardare il DHU per i 20 s della cattura (vedi sotto).
6. Annotare accanto alla riga di riepilogo le osservazioni a occhio.

## Cosa osservare a occhio durante la sessione

Il disegno di prova (D-03) e' fatto per rendere visibile cio' che i log non dicono:

- **Contorno verde = stable area**, **contorno magenta = visible area**. Tutto cio' che sta fuori dal
  verde e' spazio che l'host puo' coprire.
- **"888" centrato nel verde**: e' la misura che conta (`digitH` nei log). Deve stare dentro il
  contorno verde senza essere tagliato.
- **Testo giallo piccolo** `api=<n> <variante> <W>x<H>`: conferma Car API level, variante installata
  e dimensione della Surface.
- **Cosa sovrappone l'host**: nel percorso POI la card obbligatoria (dove sta, quanto e' larga/alta,
  se copre il contorno magenta o solo quello verde); nel percorso NAV l'action strip; in entrambi
  eventuali titoli, icone o pulsanti aggiunti dall'host.
- **Action strip (D-08)**: annotare il suo ingombro e **se e quando scompare**. Atteso ~10 s su
  `NavigationTemplate`; se scompare, verificare se stable/visible area cambiano (lo script registra
  l'ultimo valore).
- **L'app non deve essere chiusa dall'host**: nessun ritorno alla lista app, nessun messaggio di
  errore. Lo script segnala `onSurfaceDestroyed` se l'host distrugge la Surface durante la cattura.

## Criteri di esito

| Esito | Condizione |
|-------|------------|
| Sessione valida | Surface nei log uguale alla risoluzione richiesta, riga `frame area=stable` presente, screenshot salvato, nessuna chiusura dell'app |
| Sessione da ripetere | Surface diversa dalla risoluzione (DHU senza `-c`), variante/card inattese, campi `n/d`, screenshot mancante |
| **POI vince (D-12)** | Per **ognuna** delle tre risoluzioni, `digitH` della migliore variante POI (caso migliore di D-06) **>= 70%** del `digitH` di `nav` alla stessa risoluzione |
| NAVIGATION vince (D-12) | Il rapporto scende sotto il 70% in almeno una risoluzione, e l'utente conferma |

Il rapporto si calcola per risoluzione come `digitH(miglior POI) / digitH(nav)` e va riportato in
`12-SPIKE-RESULTS.md` accanto alle misure. **La soglia del 70% e' un valore di partenza**: l'utente la
conferma o la corregge guardando i numeri e gli screenshot. La decisione finale e' un **gate umano
esplicito**; nessuna uscita dello script la sostituisce.

Se vince NAVIGATION vale **D-13**: si tenta la strada NAVIGATION, ma la build passa **prima solo dal
canale di test chiuso** e si legge l'esito della revisione Android Auto; con esito negativo (rischio
NF-1/NF-6) si rientra su POI senza mai toccare il canale aperto. Se vince POI, la spec D-14 (v2.0)
viene aggiornata per convivere con la card obbligatoria.

## Verifica opzionale in auto (D-11)

Serve solo a leggere il Car API level dell'head unit reale; e' **facoltativa** (SC2 e' soddisfatto dal
solo DHU). In auto la USB e' occupata dall'head unit e non c'e' logcat, quindi il livello si legge
direttamente sulla Surface (testo giallo `api=`), come per le verifiche "PASS visivo senza logcat"
della Fase 11 (`docs/android-auto-hardening-verification.md`).

1. Generare l'APK debug della variante migliore:
   `.\gradlew.bat :app:assembleSpikePoiDebug -PpoiCard=<card>` oppure `.\gradlew.bat :app:assembleSpikeNavDebug`.
   L'APK si trova in `app/build/outputs/apk/spikePoi/debug/` oppure `app/build/outputs/apk/spikeNav/debug/`.
2. Installarlo sul telefono (sideload: `adb install -r <apk>` da casa, prima di uscire).
3. Sul telefono: Android Auto -> impostazioni sviluppatore -> abilitare **"Origini sconosciute"**,
   altrimenti l'head unit non mostra app installate fuori dal Play Store.
4. In auto, aprire Tachimetro sull'head unit (con il permesso gia' concesso) e leggere `api=<n>`
   sulla Surface; annotare anche risoluzione mostrata e cosa copre l'host.
5. A fine prova disattivare "Origini sconosciute" e reinstallare la build normale.

Se la verifica non si fa, annotare in `12-SPIKE-RESULTS.md` il rischio: **head unit con Car API < 7 =
app non visibile** (conseguenza di `minCarApiLevel` 7, REL-02, accettata senza fallback).

## Cosa e' fuori scope

- **Velocita' reale** sulla Surface (collegamento a `GpsSpeedProvider`): Fase 13. Nello spike le cifre
  sono sempre "888".
- **Tipografia, tema giorno/notte, "Ricerca segnale..." disegnato, contenuto definitivo della card**:
  Fase 13.
- **Caricamenti sul Play Store** di qualunque genere: Fase 15 (e mai per le build di spike, D-05).
- Pulizie lato telefono: Fase 14.

## Dopo la sessione

1. Aprire i file `build/surface-spike/*-summary.txt` delle 15 sessioni.
2. Incollare la riga di tabella di ciascuno in
   `.planning/phases/12-spike-surface-e-decisione-categoria-template/12-SPIKE-RESULTS.md`
   (l'intestazione e' la stessa per tutte), con le osservazioni a occhio (card, action strip).
3. Verificare che in `spike-screenshots/` ci siano 15 file `<variante>-<risoluzione>.png`.
4. Calcolare per ogni risoluzione il rapporto D-12 e passare la decisione all'utente.
