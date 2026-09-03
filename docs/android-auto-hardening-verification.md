# Verifica di hardening Android Auto (SC1, SC2, SC3)

Runbook riproducibile per i tre gate empirici della Fase 11
(`11-hardening-di-produzione-e-verifica-su-dispositivo-reale`): dimostrare che l'`HostValidator`
reale accetta un host Android Auto legittimo (SC1), che la velocita' sullo schermo auto continua
ad aggiornarsi con il telefono bloccato durante un tragitto reale (SC2), e che cicli rapidi di
connessione/disconnessione non mandano l'app in crash ne' lasciano lo schermo auto in uno stato
incoerente (SC3). Per questa fase questo documento sostituisce il ruolo che
`docs/dhu-quota-verification.md` aveva per il gate SC4 della Fase 8: stesso pattern (script
euristico + occhi umani), criteri diversi.

I criteri di esito qui sotto sono scritti **prima** delle sessioni di verifica, deliberatamente:
servono a impedire che un'osservazione ambigua venga razionalizzata come PASS a posteriori.
`INCONCLUSIVO` e' un esito legittimo e previsto -- se una sessione non soddisfa i prerequisiti, si
ripete, non si arrotonda.

## Perche' questa verifica esiste

Fino alla Fase 10 l'integrazione Android Auto e' stata validata solo in condizioni di sviluppo:
Desktop Head Unit, telefono sbloccato con lo schermo acceso, `HostValidator` permissivo
(`ALLOW_ALL_HOSTS_VALIDATOR`, debito marcato esplicitamente verso questa fase in
`TachimetroCarAppService.kt`). Tre proprieta' non sono mai state osservate nelle condizioni in cui
l'app viene realmente usata:

- **SC1** -- un validatore reale non e' verificabile leggendo il codice: o l'host viene accettato
  dall'allow-list a runtime, o l'app non si apre affatto sullo schermo auto. La modalita' di
  fallimento tipica e' silenziosa (allow-list vuota o con il formato di entry invertito) e produce
  un binario che compila, si installa, si spedisce e rifiuta ogni host.
- **SC2** -- il comportamento del GPS a telefono bloccato durante una connessione Android Auto e' un
  gap di documentazione della piattaforma, non un dettaglio implementativo: vedi
  `.planning/research/PITFALLS.md` Pitfall 6 per l'enunciato completo del problema e delle opzioni
  note. E' il rischio piu' incerto della fase, ed e' l'unico che nessuna sessione DHU precedente
  poteva rilevare (le sessioni DHU girano per costruzione a schermo acceso).
- **SC3** -- connettere e scollegare ripetutamente e' l'uso reale (fermata al semaforo, cavo che
  balla, spegnimento del quadro). D-05 impone di misurare prima e patchare solo se si rompe
  davvero, quindi serve una misura riproducibile che dica se si e' rotto qualcosa.

## Quale build per quale criterio

I tre criteri **non** possono essere verificati dallo stesso binario. Questo e' un vincolo
strutturale, non una preferenza organizzativa:

| SC | Build richiesto | Perche' |
|----|-----------------|---------|
| SC1 (HostValidator reale) | **release** (firmato) | D-01: il build di debug usa di proposito `ALLOW_ALL_HOSTS_VALIDATOR`. Verificare SC1 su un debug sarebbe un falso PASS: l'app si aprirebbe comunque, senza che l'allow-list reale sia mai stata esercitata. |
| SC2 (velocita' a telefono bloccato) | **debug** | Serve il log `onGetTemplate #` e non serve il validator reale: SC2 riguarda il flusso GPS in background, non l'host. |
| SC3 (connect/disconnect rapido) | **release** (firmato) | Piu' informativo: esercita il binario che si spedisce e il validator reale a ogni ri-bind, e non dipende da log emessi solo in debug. |

Due conseguenze operative verificate, da tenere presenti prima di pianificare una sessione:

- **Il contatore `onGetTemplate #<n>` esiste solo nei build di debug.** In
  `app/src/main/java/com/sed/tachimetro/car/SpeedScreen.kt` la riga di log del tag `TachimetroCar`
  e' dentro un `if (BuildConfig.DEBUG)`. In una sessione SC1 o SC3 (che girano su release) quel
  contatore **non viene emesso**: non e' utilizzabile come misura, e la sua assenza dal log non
  significa che l'app non stia aggiornando lo schermo. Il conteggio dei refresh resta disponibile
  solo nelle sessioni su build di debug (SC2, e le sessioni di Fase 8).
- **I build di release richiedono `keystore.properties`.** Il file e' assente dal repository per
  scelta di progetto (credenziali fuori dal controllo di versione -- vedi
  `keystore.properties.example` per i campi attesi). Senza di esso `assembleRelease` produce un APK
  **non firmato e non installabile**: SC1 e SC3 non sono eseguibili finche' il file non e' stato
  creato localmente.

## SC1 -- L'app usa un HostValidator reale che accetta un host legittimo

### Prerequisiti

- **Build di release firmato installato**: `gradlew.bat :app:installRelease` (richiede
  `keystore.properties`, vedi sopra).
- **Desktop Head Unit** dall'SDK (`<sdk>\extras\google\auto\desktop-head-unit.exe`) **oppure** un
  head unit reale in auto. Entrambi vanno bene: l'obiettivo e' che un host **legittimo** venga
  accettato dall'allow-list.
- **Android Auto sul telefono** con Developer Mode attivo e server head unit avviato (necessario
  solo per il percorso DHU).
- **Permesso `ACCESS_FINE_LOCATION` gia' concesso** all'app dalla UI del telefono, prima della
  sessione: la richiesta di permesso dallo schermo auto e' scope della Fase 9, non di questa
  verifica.

### Procedura passo-passo

1. Installare il build di release firmato: `gradlew.bat :app:installRelease`.
2. Abilitare i `Log.d` di validazione host (senza questo passo le righe `Accepted - ...` non
   vengono mai emesse, perche' sono dietro `Log.isLoggable`):
   ```
   adb shell setprop log.tag.CarApp.Val DEBUG
   ```
3. Aprire la cattura logcat sul tag di validazione, in una finestra separata:
   ```
   adb logcat -s CarApp.Val
   ```
4. Aprire il forward di porta richiesto dal Desktop Head Unit (saltabile con un head unit reale):
   ```
   adb forward tcp:5277 tcp:5277
   ```
5. Avviare `desktop-head-unit.exe` (oppure collegare il telefono all'auto).
6. Selezionare **Tachimetro** nella lista app dell'head unit e osservare sia lo schermo auto sia la
   finestra di logcat.

### Cosa osservare a occhio

Nessuno di questi punti e' verificabile da codice -- sono l'input diretto del checkpoint umano:

- Tachimetro **compare** nella lista app dell'head unit.
- L'app **si apre** quando viene selezionata.
- Lo schermo auto **mostra le cifre della velocita'** (o "Ricerca segnale...", se il fix GPS non
  e' ancora arrivato), non una schermata vuota o di errore.
- L'app **non torna** improvvisamente alla lista app dell'head unit subito dopo l'apertura: e' il
  sintomo tipico di un host rifiutato dal validator.

### Criteri di esito

| Esito | Condizione |
|-------|------------|
| `FAIL` | In logcat compare una riga `Rejected -` sul tag `CarApp.Val`, **oppure** l'app non si apre sull'head unit, **oppure** compare `Accepted - Validator disabled, all hosts allowed` (in quest'ultimo caso si sta testando un build di **debug**: la verifica non e' valida e non va registrata come PASS) |
| `PASS` | Compare `Accepted - Host in allow-list` sul tag `CarApp.Val` **e** l'app si apre e mostra la velocita' sullo schermo auto |
| `INCONCLUSIVO` | Ogni altro caso -- in particolare se **nessuna** riga `CarApp.Val` viene emessa, che significa quasi sempre `setprop log.tag.CarApp.Val DEBUG` dimenticato o eseguito dopo l'avvio della sessione |

La riga `Accepted - Validator disabled, all hosts allowed` e' il segnale che distingue
oggettivamente i due binari: e' la ragione per cui viene trattata come `FAIL` e non come `PASS`.

### Contingenza se FAIL

**Non reintrodurre `ALLOW_ALL_HOSTS_VALIDATOR` per far passare il test.** Sarebbe esattamente il
debito che questa fase esiste per chiudere.

Una riga `Rejected -` indica una di due cose:

- **Allow-list sbagliata.** La causa piu' probabile e' l'inversione del formato delle entry: il
  formato corretto e' `<sha256Digest>,<packageName>` (digest **prima**, package **dopo**), come
  documentato in `11-PATTERNS.md`. Invertirlo produce un validator che compila, si installa e
  rifiuta ogni host, senza alcuna eccezione a runtime.
- **Host non ufficiale.** L'head unit in uso non e' uno degli host legittimi dell'allow-list della
  Car App Library.

In entrambi i casi si corregge `createCarHostValidator`, si riesegue il test strumentato
`CarHostValidationTest` (che verifica l'allow-list senza bisogno di un host reale) e si **ripete la
sessione** su un nuovo build di release firmato. Il gate resta aperto finche' la sessione non da'
`PASS`.

## SC2 -- La velocita' continua ad aggiornarsi a telefono bloccato

Questo criterio verifica empiricamente il gap descritto in `.planning/research/PITFALLS.md`
Pitfall 6 ("Assuming location updates keep flowing to the car screen when the phone is
locked/backgrounded"). L'enunciato del problema, le sue cause e le opzioni note stanno **li'** e non
vengono riscritti qui: questo runbook definisce solo come si misura e cosa si fa dell'esito.

### Prerequisiti

- **Telefono fisico reale.** Non un AVD e non una sessione DHU: entrambi girano a schermo acceso e
  mascherano per costruzione esattamente il comportamento sotto esame.
- **Build di debug installato**: `gradlew.bat :app:installDebug` (serve il log `onGetTemplate #`,
  che non esiste in release).
- **Android Auto funzionante su un'auto vera**, con l'head unit del veicolo.
- **Permesso `ACCESS_FINE_LOCATION` gia' concesso**.
- **Un tragitto reale disponibile**, con il veicolo effettivamente in movimento per la maggior
  parte della sessione.

### Procedura passo-passo

1. Installare il build di debug e verificare che l'app si apra normalmente sul telefono.
2. Collegare il telefono all'auto e attendere che Tachimetro sia visibile sullo schermo auto con una
   velocita' o "Ricerca segnale..." mostrata.
3. **Bloccare il telefono** (schermo spento) e riporlo. Da questo momento non toccarlo piu'.
4. Guidare per **5-10 minuti** (D-06: stessa soglia gia' validata empiricamente in Fase 8, riusata
   qui invece di introdurne una nuova senza precedenti nel progetto), con il telefono bloccato per
   l'**intera** durata e senza alcuna interazione con il telefono.
5. Osservare lo schermo auto per tutta la sessione (da passeggero, o comunque senza compromettere la
   guida).
6. Al termine, registrare l'esito nella tabella `## Esiti registrati` di questo documento.

**Non esiste uno script di misura per SC2, ed e' una scelta, non una dimenticanza:** durante una
sessione Android Auto cablata la porta USB del telefono e' occupata dall'head unit, quindi `adb` non
e' disponibile in parallelo. Nessuna cattura logcat, nessun campionamento di PID, nessuna misura
automatica. SC2 e' una verifica **puramente umana** (D-07).

### Cosa osservare a occhio

- Il numero sullo schermo auto **cambia visibilmente** e in modo coerente con la velocita' reale del
  veicolo (accelera quando si accelera, scende quando si rallenta).
- Il numero **non resta congelato** su un valore ormai scaduto mentre il veicolo e' chiaramente in
  movimento.
- Lo schermo auto **non resta permanentemente** su "Ricerca segnale...".
- Il telefono **resta bloccato** (schermo spento) per tutta la durata: se si e' acceso o e' stato
  toccato, la sessione e' `INCONCLUSIVO` e va ripetuta.

### Criteri di esito

| Esito | Condizione |
|-------|------------|
| `FAIL` | In una sessione di almeno 5 minuti a telefono bloccato, il numero smette di aggiornarsi -- resta fermo sullo stesso valore, oppure resta su "Ricerca segnale..." -- per **piu' di 30 secondi consecutivi** mentre il veicolo e' chiaramente in movimento |
| `PASS` | Il numero continua ad aggiornarsi per l'intera sessione di 5-10 minuti a telefono bloccato, senza alcun blocco superiore a 30 secondi |
| `INCONCLUSIVO` | Ogni altro caso: sessione piu' corta di 5 minuti, telefono sbloccato o toccato durante il test, veicolo fermo per gran parte del tragitto |

### Contingenza se FAIL

**Questa decisione e' gia' presa (D-03/D-04). Non va presa sul momento, e non va riaperta durante la
sessione di verifica.**

Se il test rivela che gli aggiornamenti GPS si fermano a telefono bloccato, la fase si chiude
**documentando il limite di piattaforma come noto e accettato**:

1. Registrare l'esito `FAIL` nella tabella `## Esiti registrati` di questo documento, con la data e
   una nota che descriva il comportamento osservato (dopo quanti secondi si e' fermato, se si e'
   ripreso allo sblocco del telefono).
2. Registrare il limite in `.planning/STATE.md` fra i Blockers/Concerns, con cross-reference
   esplicita a `.planning/research/PITFALLS.md` Pitfall 6.

L'aggiunta di `ACCESS_BACKGROUND_LOCATION` al manifest resta **esplicitamente scartata**
e **non va riaperta**. Motivazione dell'utente, gia' messa a verbale: coerenza con la minimizzazione dei
permessi che il progetto segue dalla Fase 2 (threat model T-02-EP), ed evitare l'onere aggiuntivo di
disclosure data-safety su Play Store. Se in una milestone futura il gap risultasse inaccettabile per
l'uso reale, va riaperto come **decisione a se'** in un nuovo giro di conversazione esplicito
(D-04) -- non anticipato qui e non deciso da chi esegue la sessione.

Un `FAIL` su SC2 **non e' un fallimento della fase**: il Success Criterion 2 della ROADMAP ammette
esplicitamente questo esito ("o il limite di piattaforma viene documentato esplicitamente se non
risolvibile"). Documentarlo *e'* soddisfare il criterio.

## SC3 -- Cicli rapidi di connessione/disconnessione non rompono l'app

### Prerequisiti

- **Build di release firmato installato**: `gradlew.bat :app:installRelease`.
- **Desktop Head Unit** oppure un head unit reale.
- **`adb` disponibile** sul PATH e non occupato: questa e' una sessione **da scrivania**, non in
  guida. Con il DHU il telefono resta raggiungibile da `adb` durante tutta la sessione.
- **Permesso `ACCESS_FINE_LOCATION` gia' concesso**.

### Procedura passo-passo

1. Installare il build di release firmato.
2. Avviare lo script di misura:
   ```
   powershell -File scripts/aa-connect-cycle-check.ps1 -Cycles 10
   ```
   Parametri disponibili: `-Cycles` (numero di cicli, default 10), `-OutputDir` (default
   `build/aa-hardening`, gia' ignorata da git -- le catture logcat non devono entrare nel
   repository), `-Serial` (serial adb esplicito, necessario se sono connessi piu' dispositivi).
3. Lo script abilita `log.tag.CarApp.Val` a DEBUG, apre il forward `tcp:5277`, svuota il buffer
   logcat e avvia la cattura in background. Se il forward fallisce perche' si sta usando un head
   unit reale, lo script lo segnala e prosegue: non e' un errore.
4. A ogni ciclo lo script si ferma e chiede di: **collegare Android Auto, attendere che Tachimetro
   sia visibile sullo schermo auto, poi scollegare subito**. Premere INVIO a ciclo completato.
   Scrivere `stop` invece di premere INVIO interrompe la sessione (l'esito diventa `INCONCLUSIVO`).
5. Osservare lo schermo auto a **ogni** riconnessione: i punti da confermare sono elencati sotto.
6. Al termine lo script stampa e salva sotto `build/aa-hardening/` la cattura logcat completa e un
   riepilogo con i conteggi grezzi e il verdetto euristico.
7. Registrare **entrambi** gli esiti (euristico dello script e conferma visiva) nella tabella
   `## Esiti registrati`.

### Cosa osservare a occhio

- A ogni riconnessione lo schermo auto **torna a mostrare la velocita'** o "Ricerca segnale...".
- **Mai una schermata vuota** sullo schermo auto dopo una riconnessione.
- **Mai un valore evidentemente vecchio congelato** (es. la velocita' dell'ultima connessione,
  rimasta ferma).
- **Nessun messaggio di errore dell'host** e nessun ritorno improvviso alla lista app.
- Sul telefono, **nessun crash** e nessun dialog "L'app si e' interrotta".

### Criteri di esito

Le stesse soglie usate dallo script (`scripts/aa-connect-cycle-check.ps1`):

| Esito | Condizione |
|-------|------------|
| `FAIL` | Nel log compare `FATAL EXCEPTION`, **oppure** `ANR in com.sed.tachimetro`, **oppure** una riga `Rejected -` sul tag `CarApp.Val` (conteggio > 0 per almeno uno dei tre) |
| `PASS` | Zero occorrenze di tutti e tre i segnali **e** tutti i cicli richiesti completati **e** conferma visiva umana di tutti i punti elencati sopra |
| `INCONCLUSIVO` | Ogni altro caso -- tipicamente sessione interrotta prima di completare i cicli richiesti |

Il gate SC3 richiede **sia** l'esito euristico `PASS` dello script **sia** la conferma visiva umana:
l'uno non sostituisce l'altro (stesso pattern del gate SC4 di Fase 8, D-10; qui D-07). Lo script non
puo' vedere una schermata vuota, e un umano non puo' vedere un'eccezione catturata nel log.

Nota sul PID: lo script campiona il PID di `com.sed.tachimetro` a ogni ciclo, ma un **cambio di PID
non e' di per se' un FAIL**. Alla disconnessione il sistema puo' legittimamente terminare il
processo se l'app non e' in primo piano sul telefono. Il PID e' registrato come contesto per
interpretare un log ambiguo, non come criterio.

### Contingenza se FAIL

D-05, "testa prima, patcha solo se si rompe davvero": guardie difensive (debounce, guard su race
condition) si aggiungono **solo** in caso di crash o stato incoerente **realmente osservato** in
questa sessione. Il punto di intervento indicato dal pattern del progetto e'
`app/src/main/java/com/sed/tachimetro/car/SpeedScreen.kt` -- dove vive gia' la guardia in memoria
`requestInFlight` introdotta in Fase 9, analogo diretto del tipo di fix che servirebbe.

Due vincoli espliciti su questa contingenza:

- **Nessun hardening preventivo** va scritto per uno scenario non ancora osservato. Un `PASS` su SC3
  chiude il criterio senza aggiungere una riga di codice: e' l'esito atteso, non un'occasione mancata
  di irrobustire.
- **Un fix va pianificato come lavoro a se'**, discusso con l'utente, non improvvisato durante la
  sessione di verifica. Chi rileva un `FAIL` riporta il risultato con il riepilogo dello script
  allegato e si ferma.

## Cosa e' fuori scope di questa verifica

- **Il passaggio a `NavigationTemplate` + `SurfaceCallback`** (numero grande e centrato sullo schermo
  auto): rimandato esplicitamente a una milestone v2.1 dedicata, D-14 in `08-CONTEXT.md`. Il layout
  `PaneTemplate` host-controlled e' accettato com'e' per la v2.0 e non viene rimesso in discussione
  da nessuno dei tre gate qui.
- **Velocita' massima e distanza percorsa sullo schermo auto**: fuori scope di milestone (restano
  solo sul telefono).
- **Qualunque nuova capability** oltre ai tre Success Criteria della Fase 11. Questa fase non
  introduce requisiti funzionali nuovi: valida e mette in sicurezza AA-01..AA-04 e CONN-01/CONN-02
  gia' esistenti.
- **Scenario G / Pitfall 1 di Fase 9** (`CarContext.requestPermissions()` puo' essere ignorato
  dall'host a veicolo in movimento): limite gia' osservato, documentato e accettato in Fase 9. Non
  viene riaperto qui, e osservarlo di nuovo durante una di queste sessioni non e' un `FAIL`.
- **La quota di refresh dei template** (Pitfall 2): gia' chiusa con `PASS` dal gate SC4 di Fase 8,
  vedi `docs/dhu-quota-verification.md`.

## Esiti registrati

Compilata dai Piani 03 (SC1, SC3) e 04 (SC2) al termine di ogni sessione di verifica.

| Criterio | Data | Esito | Note |
|----------|------|-------|------|
| SC1 -- HostValidator reale accetta un host legittimo | -- | da eseguire | -- |
| SC2 -- Velocita' aggiornata a telefono bloccato | -- | da eseguire | -- |
| SC3 -- Cicli rapidi di connessione/disconnessione | -- | da eseguire | -- |
