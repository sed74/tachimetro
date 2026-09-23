# Pitfalls Research

**Domain:** Aggiungere il rendering diretto sulla Surface (`SurfaceCallback` + template a mappa) a un'app Android Auto già pubblicata con template (`PaneTemplate`, categoria POI, `androidx.car.app` 1.7.0), con possibile cambio di categoria
**Researched:** 2026-09-23
**Confidence:** MEDIUM-HIGH. Le meccaniche di API (permessi, livelli Car API, threading, ciclo di vita della Surface) sono HIGH perché verificate sui **sorgenti di `androidx.car.app:app:1.7.0` presenti nella cache Gradle del progetto**. Le regole di revisione Play Store sono HIGH per il testo delle linee guida e LOW-MEDIUM su come verrà giudicato un "tachimetro su Surface". Qui l'interpretazione del revisore non è documentata.

> Numerazione fasi: v2.0 si è chiusa con la Fase 11, quindi qui "Fase 12+" indica le fasi suggerite per v2.1. Il roadmapper può rinominarle. Riferimenti: **F12 = spike categoria/distribuzione**, **F13 = renderer Surface**, **F14 = stati/permessi/dark mode sul nuovo rendering**, **F15 = verifica su hardware + rilascio**, **Pulizie** = `isDeviceCharging()` / finestra transitoria `carLink`.

## Fatti verificati che guidano tutto il resto

| Fatto | Fonte | Confidenza |
|-------|-------|------------|
| `AppManager.setSurfaceCallback()` richiede `androidx.car.app.ACCESS_SURFACE` e lancia `SecurityException` se manca | Javadoc `AppManager.java` 1.7.0 | HIGH |
| I callback `SurfaceCallback` arrivano sul **main thread** (`Looper.getMainLooper()`) | Javadoc `AppManager.java` 1.7.0 | HIGH |
| "every instance of Surface received through [onSurfaceAvailable] must be released by calling `Surface#release()`". `onSurfaceAvailable` può essere chiamato più volte (cambio dimensione/DPI) senza destroy intermedio | Javadoc `SurfaceCallback.java` 1.7.0 | HIGH |
| `SurfaceContainer.getSurface()` può essere `null` ("surface not ready"), e width/height/dpi possono valere 0 | `SurfaceContainer.java` 1.7.0 | HIGH |
| `visibleArea`/`stableArea` possono essere `Rect` vuoti, cioè area "sconosciuta" | Javadoc `SurfaceCallback.java` | HIGH |
| `MapWithContentTemplate` è `@RequiresCarApi(7)`. Richiede `NAVIGATION_TEMPLATES` **oppure** `MAP_TEMPLATES`. `setContentTemplate()` è **obbligatorio** (`build()` valida: solo List/Pane/Grid/MessageTemplate) | `MapWithContentTemplate.java` 1.7.0 | HIGH |
| `MapWithContentTemplate` è utilizzabile dalle app POI (con `MAP_TEMPLATES`) oltre che da Navigation e Weather | developer.android.com/training/cars/apps/poi, /library/draw-maps | HIGH |
| `NavigationTemplate` richiede `NAVIGATION_TEMPLATES` e un action strip obbligatorio (`"Action strip for this template must be set"`). Ogni cambio di contenuto conta come refresh e il template **azzera la quota** | `NavigationTemplate.java` 1.7.0 | HIGH |
| `NAVIGATION_TEMPLATES` e `MAP_TEMPLATES`: "An app not in one of those categories requesting this permission may be rejected upon submission to the Play Store" | Javadoc `CarAppPermission.java` 1.7.0 | HIGH |
| NF-2: una nav app "draws only map content on the surface"; sulla safe area della mappa si possono disegnare "speed limit, road obstructions". PF-1 per POI: "meaningful functionality relevant to driving". PC-1: nessuna funzione fuori dai tipi di app previsti. MR-1: le app che disegnano mappe devono seguire light/dark quando richiesto | Car app quality guidelines | HIGH (testo) |
| Revisione form factor: Internal = nessuna, **Closed = non bloccante**, **Open testing e Production = bloccante** | developer.android.com/training/cars/distribute | HIGH |
| `TestAppManager` (app-testing) si limita a registrare il `SurfaceCallback` impostato. Non crea né renderizza una Surface | `TestAppManager.java` app-testing 1.7.0 | HIGH |

---

## Critical Pitfalls

### Pitfall 1: Un aggiornamento con permessi Surface/mappa sul canale test aperto viene ri-revisionato e può essere respinto

**What goes wrong:**
Oggi la 2.0 (vC4) è sul canale **test aperto**, che per il form factor Android Auto ha revisione **bloccante**. La v2.1 aggiunge `ACCESS_SURFACE` più `MAP_TEMPLATES` (restando POI) oppure `NAVIGATION_TEMPLATES` (passando a NAVIGATION). Il nuovo artefatto torna in revisione contro le quality guidelines e il revisore vede un template a mappa **senza mappa**, con un numero al posto delle tile. Sono tre gli appigli testuali per un rifiuto:
- **POI + `MapWithContentTemplate`**: il template "render[s] map tiles". Senza mappa e senza POI, PF-1 ("meaningful functionality relevant to driving") e PC-1 diventano discutibili. Il Javadoc di `MAP_TEMPLATES` avverte esplicitamente del possibile rifiuto.
- **NAVIGATION + `NavigationTemplate`**: NF-1 (turn-by-turn), NF-6 (intent di navigazione) e NF-7 (test drive) non sono soddisfatti, e NF-2 dice che sulla Surface va "only map content". La velocità è citata solo come *informazione aggiuntiva sulla safe area della mappa*, non come contenuto unico. Il rifiuto è quasi certo (confidenza MEDIUM-HIGH).
- In entrambi i casi il revisore potrebbe anche non obiettare (i thread "Category not permitted" mostrano che i rifiuti esistono, ma i criteri concreti non sono pubblici).

**Why it happens:**
L'upgrade viene trattato come un refactor di UI, mentre per Google Play cambia il profilo dell'app: nuovi permessi Car App, template a mappa, magari una categoria diversa. Ogni submission con artefatto Android Auto viene rivalutata.

**How to avoid:**
1. **Decisione scritta prima di qualsiasi codice (F12):** categoria, permessi e canale di destinazione della v2.1.
2. **Strategia di canale raccomandata:** caricare la v2.1 prima sul **test chiuso** (non bloccante: si riceve la notifica di non conformità ma la build viene approvata). Si legge il verdetto del revisore senza rischiare il canale aperto. Si promuove al test aperto solo dopo un esito pulito. Se nel test chiuso arriva una segnalazione, si decide lì (restare in chiuso, rinunciare, adattare).
3. **Preferire POI + `MAP_TEMPLATES` a NAVIGATION:** stessa categoria di oggi e requisiti di qualità minimi (solo PF-1), contro i requisiti NF-1..NF-9 che l'app non può soddisfare. Passare a NAVIGATION solo se lo spike dimostra che POI non può ottenere la Surface. Questa è la condizione già scritta in PROJECT.md.
4. Tenere pronto un **fallback** chiaro: il `PaneTemplate` di v2.0 resta il comportamento per host/canali in cui la Surface non è ammessa (vedi Pitfall 4).

**Warning signs:**
- Email di Play Console "Issue found" o "Category not permitted" dopo l'upload.
- Build v2.1 bloccata "In revisione" sul canale aperto più a lungo del solito.
- Si pensa di dichiarare `NAVIGATION_TEMPLATES` "per sicurezza" insieme a `MAP_TEMPLATES`.

**Phase to address:** F12 (decisione) e F15 (rilascio sul canale chiuso, poi promozione).

---

### Pitfall 2: Effetto di un rifiuto sulla release già pubblicata e cambio di categoria su un'app esistente: comportamento non documentato

**What goes wrong:**
Non è stata trovata documentazione ufficiale su (a) cosa succede alla vC4 già live sul canale aperto se l'update v2.1 viene respinto, e (b) come Play Console tratta un cambio di categoria (POI → NAVIGATION) su un'app già approvata. L'ipotesi comune è che un update respinto non tocchi la release precedente. Resta però un'ipotesi (LOW): con il form factor Android Auto Google può anche chiedere di rimuovere il supporto auto dall'artefatto.

**Why it happens:**
La pagina "Distribute to cars" descrive solo il gating per canale, non gli effetti sulle release esistenti né i cambi di categoria.

**How to avoid:**
- Non caricare mai la v2.1 direttamente sul canale aperto (vedi Pitfall 1, punto 2).
- Tenere versionCode e artefatti della vC4 in `playstore/` per poter rifare una release di rollback (versionCode superiore, codice v2.0) se servisse.
- Se si cambia categoria, trattarlo come **decisione irreversibile a livello di listing**. Documentarla in Key Decisions con il motivo.

**Warning signs:** avvisi di policy nella sezione "Form factor → Android Auto" della Play Console, o la vC4 che sparisce dal launcher Android Auto dei tester.

**Phase to address:** F12 (strategia), F15 (esecuzione del rilascio).

---

### Pitfall 3: `MapWithContentTemplate` impone un contenuto sovrapposto che ruba spazio al numero "a tutto schermo"

**What goes wrong:**
`MapWithContentTemplate.Builder.build()` lancia un'eccezione senza `setContentTemplate()`: serve per forza una List, Pane, Grid o Message sovrapposta alla "mappa". L'host disegna questo contenuto come pannello/card sopra la Surface e riduce di conseguenza `visibleArea` e `stableArea`. Il risultato: il numero "grande e centrato" della spec D-14 si trova in un'area più piccola e decentrata rispetto allo schermo fisico. L'host aggiunge anche un action strip e altro chrome. Anche `NavigationTemplate` ha un action strip obbligatorio.

**Why it happens:**
Si immagina la Surface come un canvas a tutto schermo come il telefono in modalità immersiva. In realtà è sempre sotto il chrome dell'host.

**How to avoid:**
- Nello spike F12, **fare un prototipo su DHU** di entrambe le opzioni (POI+MapWithContent con il MessageTemplate/PaneTemplate più piccolo possibile e NavigationTemplate con action strip minimo). Misurare i `Rect` visible/stable restituiti e confrontarli con la spec D-14 *prima* di scegliere.
- Usare il contenuto obbligatorio in modo utile: per esempio il MessageTemplate che porta gli stati "Ricerca segnale..." / permesso (vedi Pitfall 9), invece di un pannello vuoto.
- Aggiornare la spec D-14 in modo esplicito: "centrato nella stable area", non "centrato nello schermo".

**Warning signs:** numero centrato sullo schermo fisico ma parzialmente coperto dal pannello, o spostato a destra su DHU.

**Phase to address:** F12 (prototipo e misura), F13 (implementazione).

---

### Pitfall 4: Livello Car API dell'host: `MapWithContentTemplate` richiede API 7 ma il manifest dichiara `minCarApiLevel=1`

**What goes wrong:**
Il manifest attuale dichiara `androidx.car.app.minCarApiLevel = 1`. Se `SpeedScreen` restituisce un `MapWithContentTemplate` (`@RequiresCarApi(7)`) a un host con livello inferiore, l'host non lo supporta: errore dell'host o app chiusa. Alzare `minCarApiLevel` a 7 invece fa **sparire l'app** dal launcher Android Auto sui telefoni con Android Auto più vecchio, e anche questo è silenzioso.

**Why it happens:**
Il livello Car API è quello dell'host, cioè l'app Android Auto **sul telefono** (non la head unit, trattandosi di proiezione). DHU e head unit reale collegati allo stesso telefono espongono quindi lo stesso livello. Sviluppatore e tester però hanno spesso versioni diverse di Android Auto.

**How to avoid:**
- Scegliere una delle due strade in modo esplicito (F12):
  - (a) `minCarApiLevel=7` se si adotta `MapWithContentTemplate`, accettando di perdere gli host vecchi (oggi rari, ma verificare);
  - (b) mantenere `minCarApiLevel` basso e fare branch a runtime su `carContext.carAppApiLevel >= CarAppApiLevels.LEVEL_7`, con fallback al `PaneTemplate` di v2.0.
- `NavigationTemplate` e `SurfaceCallback.onSurfaceAvailable` sono API 1. `onScroll`/`onFling` sono API 2 ma non servono.
- Registrare il livello (`carAppApiLevel`) nel log di debug esistente (`SpeedScreen` ha già un log `onGetTemplate #n`) e leggerlo sia su DHU sia sulla head unit reale.

**Warning signs:** errore host "template not supported" o app chiusa solo sul telefono di un tester. App assente dal launcher dopo l'aumento di `minCarApiLevel`.

**Phase to address:** F12 (decisione), F13 (branch di runtime e test della funzione pura di selezione).

---

### Pitfall 5: Permessi e categoria non allineati: `SecurityException` a runtime o template rifiutato dall'host

**What goes wrong:**
Combinazioni sbagliate si rompono in punti diversi:
- `ACCESS_SURFACE` assente → `setSurfaceCallback()` lancia `SecurityException` (sorgente verificato).
- `MapWithContentTemplate` senza `MAP_TEMPLATES`/`NAVIGATION_TEMPLATES` → template rifiutato dall'host.
- `NavigationTemplate` in un'app dichiarata POI → l'host lo rifiuta: servono il permesso `NAVIGATION_TEMPLATES` e la categoria NAVIGATION (MEDIUM: comportamento host non documentato nel dettaglio, ma il Javadoc lega il permesso alle categorie).
- Dichiarare permessi "extra" (per esempio sia `NAVIGATION_TEMPLATES` sia `MAP_TEMPLATES`) aumenta il rischio di rifiuto in revisione (Pitfall 1).

**How to avoid:**
- Tabella di coerenza scritta in F12, da applicare 1:1 in F13:
  - **POI:** `ACCESS_SURFACE` + `MAP_TEMPLATES`, `MapWithContentTemplate`, niente `NAVIGATION_TEMPLATES`.
  - **NAVIGATION:** `ACCESS_SURFACE` + `NAVIGATION_TEMPLATES`, `NavigationTemplate`.
- Aggiornare in blocco il commento `D-00a` del manifest, che oggi giustifica POI "senza Surface".
- Chiamare `setSurfaceCallback` dentro `try/catch(SecurityException)` con fallback al `PaneTemplate`, per non crashare in caso di configurazione sbagliata.

**Warning signs:** crash all'apertura dello schermo auto con `SecurityException` in logcat. Schermata di errore generica dell'host.

**Phase to address:** F13.

---

### Pitfall 6: Ciclo di vita della Surface: leak, disegno su Surface distrutta, schermo nero dopo ricreazione

**What goes wrong:**
Tre errori tipici:
1. **Leak:** il Javadoc impone `Surface.release()` per ogni Surface ricevuta. `onSurfaceAvailable` può arrivare più volte senza destroy (cambio dimensione/DPI). Se si sovrascrive il riferimento senza rilasciare quello vecchio, si perdono buffer.
2. **Crash:** si disegna dopo `onSurfaceDestroyed` o su una Surface con `isValid == false` (per esempio perché arriva un tick GPS subito dopo il destroy). `lockCanvas`/`lockHardwareCanvas` lanciano `IllegalStateException`/`IllegalArgumentException`. Oppure `SurfaceContainer.surface == null` e width/height = 0.
3. **Schermo nero dopo ricreazione:** la Surface viene ricreata (cambio template, ritorno da un'altra app, cambio giorno/notte, resize) e l'app ridisegna solo al prossimo cambio di `SpeedState`. `StateFlow` **non riemette valori uguali**: a velocità costante, a veicolo fermo (0) o in `NoSignal` lo schermo resta nero finché il valore non cambia. In più il ticker a 1 s interno a `GpsSpeedProvider` produce `SpeedState` uguali che non attraversano lo `StateFlow`.

**Why it happens:**
Il modello mentale è quello "event-driven" della `TextView` del telefono (il framework ridisegna da solo). Con la Surface è l'app a dover ridisegnare in ogni evento.

**How to avoid:**
- Un unico oggetto `SpeedSurfaceRenderer` (proprietà dello `Screen` o della `Session`) con uno stato cache: `surface`, `size`, `dpi`, `visibleArea`, `stableArea`, `isDark`, `lastContent`. Un solo metodo `render()` chiamato da **tutti** gli eventi: available, visibleAreaChanged, stableAreaChanged, cambio config dark, nuova emissione di `SpeedState`.
- In `onSurfaceAvailable`: se c'era già una Surface diversa, `release()` di quella vecchia. Poi `render()` immediato.
- In `onSurfaceDestroyed`: azzerare il riferimento, poi `release()`. Guardie `surface?.isValid == true && width > 0 && height > 0` prima di ogni lock.
- `unlockCanvasAndPost` in `finally`.
- `setSurfaceCallback(null)` e rilascio quando lo `Screen` va in `DESTROYED` (lifecycle observer), per non tenere lo `Screen` referenziato dall'`AppManager`.

**Warning signs:** `IllegalStateException: Surface has already been released` / `lockCanvas` failures in logcat. Schermo auto nero a veicolo fermo dopo essere tornati da Google Maps. Memoria che cresce dopo molti cicli connetti/disconnetti.

**Phase to address:** F13. Verifica cicli in F15, come estensione della SC3 rimandata di Fase 11.

---

### Pitfall 7: Disegnare fuori dall'area stabile/visibile, o fidarsi di un'area "sconosciuta"

**What goes wrong:**
Si centra il numero su `width/2, height/2` della Surface. L'host però copre parte della Surface con action strip, pannello del contenuto, barra di stato e rail di sistema (layout Coolwalk su schermi larghi). Il numero finisce parzialmente coperto, oppure l'unità "km/h" in basso a destra finisce sotto un controllo dell'host. `onVisibleAreaChanged` inoltre cambia di frequente (strip che compare/scompare): se il numero segue la visible area "salta" a ogni interazione, e un `Rect` vuoto significa area **sconosciuta**, non "tutto lo schermo".

**How to avoid:**
- Layout del numero e dell'unità basato sulla **stable area** (dato persistente, niente salti). La visible area serve solo come limite di clipping di sicurezza.
- Fallback esplicito se entrambe sono vuote: usare l'intera Surface con un margine prudente (per esempio 8%) finché non arriva un rect valido, e ridisegnare quando arriva.
- Sfondo pieno su **tutta** la Surface, così nessuna zona resta con buffer spazzatura sotto il chrome dell'host.
- Mettere la geometria in una funzione pura `computeSpeedLayout(surfaceW, surfaceH, dpi, stable: RectLike, visible: RectLike, text): SpeedLayout`, testabile su JVM (vedi Pitfall 13).

**Warning signs:** "km/h" invisibile su DHU con action strip visibile. Numero che si sposta toccando lo schermo.

**Phase to address:** F13.

---

### Pitfall 8: Tipografia tarata sul telefono (sp, fontScale, densità) invece che sui pixel e sui dpi della Surface

**What goes wrong:**
Sul telefono il numero usa `autoSizeTextType=uniform` 12-300 sp. Sulla Surface non ci sono `TextView` né autosize, e i display auto vanno da 800x480 a 160 dpi fino a ultrawide 1920x720 o più. Errori tipici:
- dimensioni in sp o dp calcolate con le `DisplayMetrics` del **telefono** (densità e `fontScale` sbagliati) → numero minuscolo o tagliato;
- dimensionare sulla larghezza di "88" e poi passare a "188" → overflow orizzontale;
- centrare con `ascent/descent` invece dei bound reali delle cifre → numero visivamente fuori centro;
- cifre proporzionali → il numero "balla" lateralmente ogni secondo (1 vs 8 hanno larghezze diverse).

**How to avoid:**
- Calcolare la dimensione del testo in **pixel** con una ricerca (binaria o per rapporto) che faccia entrare la stringa **più larga prevedibile** (per esempio "888", o almeno lo stesso numero di cifre) nel box disponibile. Limitare con l'altezza della stable area (sugli ultrawide vincola l'altezza, non la larghezza).
- `Paint` riusato con `isAntiAlias = true`, `Typeface.create(Typeface.DEFAULT, 900, false)` (API 28+, disponibile con minSdk 30) per il Black coerente con il telefono, e `fontFeatureSettings = "tnum"` per cifre a larghezza fissa. Verificare su dispositivo che il font di sistema supporti `tnum`, altrimenti misurare "888".
- Centrare con `getTextBounds` sul testo reale (o sulle cifre) e non con le metriche di linea.
- Usare `SurfaceContainer.dpi` solo per le dimensioni minime/minori (unità "km/h", messaggi), mai `resources.displayMetrics` del telefono. Ignorare `fontScale` dell'utente: sul display auto la dimensione è guidata dall'area.

**Warning signs:** numero diverso di dimensione tra DHU 720p e 1080p a parità di area. Tremolio orizzontale ogni secondo. Testo tagliato a 3 cifre.

**Phase to address:** F13 (funzione pura + test), F15 (verifica su più configurazioni DHU e sulla head unit reale).

---

### Pitfall 9: Regressione del flusso permesso di Fase 9 e degli stati "Ricerca segnale..."

**What goes wrong:**
Oggi `SpeedScreen.buildTemplate()` mostra permesso negato/permanente con **Action** (riprova / apri impostazioni) in un `PaneTemplate`, e `requestPermissions()` parte da `SpeedScreen`. Con la Surface ci sono due errori opposti:
- si disegnano i messaggi di stato sulla Surface **senza pulsanti**, e il flusso di rifiuto perde le Action di Fase 9 (AA-04 regredisce);
- si alterna ogni volta tra template a mappa e `PaneTemplate`. Ogni cambio di tipo conta nella quota (5 per task) e ogni uscita da un template a mappa distrugge e ricrea la Surface (vedi Pitfall 6). Se il flusso si ripete, l'host può chiudere l'app per quota esaurita. `NavigationTemplate` azzera la quota quando lo si raggiunge, `MapWithContentTemplate` **non** è documentato come reset (MEDIUM).

**How to avoid:**
- Tenere **un solo tipo di template contenitore** per tutti gli stati. Per POI: `MapWithContentTemplate` con contenuto `MessageTemplate`, che supporta Action. Gli stati permesso/negato e "Ricerca segnale..." vanno nel `MessageTemplate`, mentre la Surface mostra velocità o sfondo neutro. Un `MessageTemplate` è un refresh solo se "title and messages have not changed" (sorgente 1.7.0): il cambio messaggio conta quindi solo alle transizioni di stato, non ogni secondo.
- La **velocità** non passa più dal template: si ridisegna solo la Surface. **Eliminare l'`invalidate()` a 1 Hz** attuale (vedi Pitfall 10).
- Ripetere la matrice di verifica live di Fase 9 (richiesta automatica, concessione, primo rifiuto, rifiuto permanente, apertura impostazioni) sul nuovo rendering. Ricordare il limite noto: `requestPermissions()` può essere ignorato con veicolo in movimento.
- Riusare le funzioni pure esistenti (`CarPermissionState`, `CarSpeedContent`) come unica fonte del testo, e adattare solo il "sink" (template o Surface).

**Warning signs:** dopo un rifiuto lo schermo auto non offre più "Apri impostazioni". Contatore quota nell'overlay Developer Mode di Android Auto che cresce a ogni transizione. App chiusa dall'host dopo più cicli di rifiuto e concessione.

**Phase to address:** F14. Test delle funzioni pure di selezione template/testo in F13/F14.

---

### Pitfall 10: Mantenere l'`invalidate()` a 1 Hz o ridisegnare in loop continuo

**What goes wrong:**
Due eccessi opposti:
- si lascia l'`invalidate()` per secondo di v2.0 anche dopo il passaggio alla Surface: round-trip IPC inutile con l'host, rischio che il contenuto del `MapWithContentTemplate` venga considerato cambiato (quota), log `onGetTemplate #n` rumorosi;
- si implementa un render loop (Choreographer, `postDelayed` a 16 ms o 60 fps) per un numero che cambia al massimo 1 volta al secondo: CPU e batteria del telefono (che già codifica il video per la proiezione) sprecate, e telefono caldo sul supporto.

**How to avoid:**
- `render()` **solo a evento** (Pitfall 6): nuova emissione di stato, eventi della Surface, cambio di dark mode. Al massimo ~1 frame al secondo in guida.
- Il template si ricostruisce solo alle transizioni di stato (permesso, ricerca, lettura), non per ogni velocità.
- Nessuna animazione: coerente con il vincolo UX del progetto.
- Allocazioni zero nel percorso di disegno (`Paint`, `Rect` e `String` riusati dove possibile).

**Warning signs:** `onGetTemplate` loggato ogni secondo anche dopo la migrazione. Profiler CPU con il main thread occupato di continuo.

**Phase to address:** F13.

---

### Pitfall 11: Threading: disegno da un thread sbagliato o disegno pesante sul main thread

**What goes wrong:**
I callback della Surface arrivano sul main thread (verificato). `GpsSpeedProvider` emette su `Dispatchers.Main.immediate`. Se qualcuno sposta il rendering su un `HandlerThread` o `Dispatchers.Default` "per performance", nasce una race tra `onSurfaceDestroyed` (main) e `lockCanvas` (worker), cioè il crash del Pitfall 6 in forma intermittente e difficile da riprodurre. Il caso opposto è meno probabile: disegno pesante sul main thread (per esempio `VirtualDisplay` + `Presentation` con inflate XML) che rallenta il binder con l'host.

**How to avoid:**
- Per un solo numero a 1 Hz, **tutto sul main thread**: callback, stato e `lockHardwareCanvas` (API 26+, ok con minSdk 30) o `lockCanvas`. Un disegno di testo costa pochi ms.
- Niente `VirtualDisplay`/`Presentation`, anche se la tentazione di riusare le `TextView` XML è forte: aggiunge ciclo di vita (display virtuale da rilasciare, `Presentation.dismiss()`) e thread UI separati per un guadagno nullo. Canvas diretto.
- Se in futuro servisse un worker thread, tutti gli accessi alla Surface vanno serializzati sullo stesso thread e il destroy va gestito in modo sincrono.

**Phase to address:** F13.

---

### Pitfall 12: Giorno/notte e contrasto: forzare sempre il nero, o seguire l'host rovinando la leggibilità

**What goes wrong:**
L'identità dell'app è "sfondo nero, testo bianco". MR-1 però dice che le app che disegnano mappe "must draw a light-themed or dark-themed map when instructed to do so", con l'eccezione "Apps can let users choose to always display the app in either light or dark theme". Rischi:
- ignorare `carContext.isDarkMode` e restare sempre nero: potenziale rilievo in revisione (MR-1) senza un'opzione utente, anche se con Surface senza mappa l'applicabilità è discutibile (LOW);
- seguire l'host in modalità giorno con sfondo bianco: coerente con MR-1 ma cambia l'identità visiva, e di notte un'eventuale gestione errata acceca il guidatore (bianco pieno sul display).
- Non ridisegnare quando cambia il tema: con la Surface il cambio non è automatico. Va gestito `Session.onCarConfigurationChanged` e poi `render()`.

**How to avoid:**
- Decisione esplicita in F12/F14: (a) seguire `isDarkMode` con due palette ad altissimo contrasto (giorno: nero su bianco, notte: bianco su nero) oppure (b) restare sempre scuro, documentando il rischio MR-1 e il fatto che una Surface senza mappa è comunque fuori dal caso tipico.
- In ogni caso **ridisegnare su `onCarConfigurationChanged`** e al primo `onSurfaceAvailable` leggere `isDarkMode` corrente.
- Verificare su DHU forzando giorno/notte (il comando da console DHU `day`/`night` va verificato nella versione installata, MEDIUM) e sulla head unit reale di giorno, di sera e in galleria.

**Warning signs:** colori che non cambiano quando cambia il tema di Android Auto. Screenshot DHU notte con sfondo bianco.

**Phase to address:** F14 (decisione e implementazione), F15 (verifica reale).

---

### Pitfall 13: Strategia di test illusoria: app-testing non renderizza e Canvas su JVM è uno stub

**What goes wrong:**
`TestCarContext`/`TestAppManager` registrano solo il `SurfaceCallback` impostato. Nessuna Surface reale, nessun pixel. Sui test JVM locali del progetto, `Canvas`/`Paint`/`Typeface` sono stub di `android.jar` ("Stub!" o valori di default). Si scrivono allora test che "passano" senza verificare nulla del rendering, o si rinuncia del tutto ai test e il layout regredisce senza che nessuno lo noti.

**How to avoid:**
- Seguire la convenzione del progetto (funzioni pure framework-free):
  - `computeSpeedLayout(...)`: dimensione testo in px data una funzione di misura iniettata (`(String, Float) -> Float` per la larghezza), posizione, area usata e fallback per rect vuoti. Test JVM con una misura finta lineare.
  - `selectCarTemplateKind(apiLevel, permissionState, speedState)` e `surfaceTextFor(state)`: test JVM come `CarSpeedContentTest`/`CarPermissionStateTest`.
  - Una macchina a stati del renderer (available → destroyed → available, surface null, rect vuoti) con un'interfaccia `SurfaceSink` finta, per verificare che `render()` venga invocato a ogni evento e mai dopo il destroy. Qui `TestAppManager.getSurfaceCallback()` serve a pilotare i callback con un `SurfaceContainer(null, w, h, dpi)`.
- Il rendering reale si verifica **solo** con checkpoint umani su DHU (più configurazioni `.ini` di risoluzione/dpi) e sulla head unit reale, come in Fase 8/11.
- Aggiornare o sostituire `CarSpeedContentTest`, che oggi testa la costruzione del `PaneTemplate`, invece di lasciare test verdi su codice non più usato nel percorso principale.

**Phase to address:** F13 (test puri), F15 (checkpoint umani).

---

### Pitfall 14: DHU ≠ head unit reale

**What goes wrong:**
Lo sviluppo si fa su DHU (Windows + telefono fisico). La head unit reale differisce per risoluzione/dpi/aspect ratio, input (touch vs rotary/touchpad), sorgente del segnale notte (sensore/fari dell'auto), layout Coolwalk e chrome dell'host. Inoltre durante l'uso in auto la USB è occupata e `adb logcat` non è disponibile (già emerso in Fase 11): i crash del renderer sulla head unit reale non lasciano log.

**How to avoid:**
- Su DHU provare almeno le configurazioni 480p/720p/1080p e una widescreen (file `.ini` in `extras/google/auto/config`, da verificare nella versione installata) prima di andare in auto.
- Per la head unit reale, predisporre un **log persistente minimo** (ring buffer su file interno o `SharedPreferences` con l'ultimo errore del renderer più il livello Car API più le dimensioni Surface/stable) leggibile dopo, a telefono ricollegato al PC. In alternativa, usare ADB via Wi-Fi durante la sessione in auto.
- Checklist di verifica in auto: numero centrato nella stable area, "km/h" visibile, giorno/notte, perdita segnale (galleria), rientro da Google Maps, disconnessione/riconnessione.

**Phase to address:** F15.

---

### Pitfall 15 (solo se si passa a NAVIGATION): effetti collaterali della categoria navigazione

**What goes wrong:**
Oltre alla revisione (Pitfall 1), NAVIGATION cambia il comportamento dell'host: l'app finisce tra le app di navigazione e compete con Google Maps per lo slot "mappa" nel layout a schede. Aprire Tachimetro può sostituire la mappa di Maps, e il guidatore perde la navigazione vera (MEDIUM, comportamento host non documentato in dettaglio). La categoria porta anche obblighi (NavigationManager, gestione `onStopNavigation`, intent di navigazione, auto-drive) che un tachimetro non può soddisfare in modo onesto.

**How to avoid:** restare POI se lo spike F12 conferma la fattibilità di `MapWithContentTemplate` + `MAP_TEMPLATES`. È quanto già indica PROJECT.md: NAVIGATION solo come ultima risorsa.

**Phase to address:** F12.

---

## Moderate Pitfalls

### Pitfall 16: Screen o Session referenziati dopo la distruzione

`SurfaceCallback` implementato come inner class dello `Screen`, registrato in `AppManager` e mai rimosso: lo `Screen` (e il `CarContext`) resta vivo dopo la fine della sessione. **Prevenzione:** `setSurfaceCallback(null)` in `onDestroy` dello `Screen` (lifecycle observer). Il renderer non tiene riferimenti al `MainActivity` né al `Context` del telefono, coerentemente con WR-04.

### Pitfall 17: `SpeedState` non ancora disponibile al primo frame

Al primo `onSurfaceAvailable` lo stato può essere ancora `Searching` o non raccolto. Disegnare lo sfondo e uno stato neutro (per esempio vuoto, con il messaggio nel `MessageTemplate`), mai un "0" che sembri una lettura reale. Per il Core Value, uno "0" falso è peggio di nessun numero.

### Pitfall 18: Collezione GPS ancorata alla Surface invece che allo `Screen`

Avviare e fermare la collezione di `GpsSpeedProvider.state` su `onSurfaceAvailable`/`onSurfaceDestroyed` fa ripartire la pipeline a ogni ricreazione della Surface. Questo peggiora il bug noto di `lastAcceptedLocation` non resettato (fuori scope v2.1 ma latente) e il `WhileSubscribed` fa ripartire le richieste di posizione. **Prevenzione:** la collezione resta legata al lifecycle dello `Screen` come in v2.0. La Surface consuma solo l'ultimo valore in cache.

### Pitfall 19: Pulizie (`isDeviceCharging()` → `deriveChargingState()`, finestra `carLink`) che introducono regressioni sul telefono

Il consolidamento tocca il default "sempre acceso" al primo avvio (derivato dallo stato di ricarica, Fase 5) e la logica CONN-01/02. **Prevenzione:** test JVM di equivalenza prima e dopo (stessi input, stesso output). Per `carLink`, lettura sincrona iniziale dello stato di connessione su `onCreate`/`onResume`, coperta da un caso in `CarLinkSequenceTest`. Tenerle in una fase separata dal renderer così che un bug non si confonda con l'altro.

## Minor Pitfalls

- **Stringhe hardcoded nel renderer:** "km/h" e i messaggi vanno presi da `strings.xml` via `carContext.getString`, in italiano come il resto.
- **Anti-aliasing o subpixel off:** bordi del numero seghettati a bassa dpi. Serve `isAntiAlias = true` (e `isSubpixelText` se utile).
- **Formato dei numeri:** usare lo stesso formatter del telefono (intero, niente separatori), per non avere "1.000" o simili.
- **Dimenticare `playstore/`:** le release notes it-IT/en-US e il listing vanno aggiornati con la v2.1 (memoria utente: tenere `playstore/` sempre deploy-ready). Se la categoria cambia, rivedere anche il testo del listing che descrive Android Auto.

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Dichiarare sia `MAP_TEMPLATES` sia `NAVIGATION_TEMPLATES` "per provare" | Si sperimentano entrambi i template senza cambiare manifest | Rischio di rifiuto in revisione, incoerenza con la categoria | Solo in build debug locali, mai in un artefatto caricato su Play |
| `VirtualDisplay` + `Presentation` per riusare il layout XML del telefono | Riuso di autosize e `TextView` | Ciclo di vita doppio (display e presentation), leak e crash su destroy, thread UI extra | Mai per un singolo numero |
| Lasciare l'`invalidate()` a 1 Hz "tanto funzionava" | Nessun refactor di `SpeedScreen` | IPC inutile, rischio quota su `MapWithContentTemplate`, log confusi | Mai dopo la migrazione |
| Sempre sfondo nero ignorando `isDarkMode` | Identità visiva invariata | Possibile rilievo MR-1 in revisione | Accettabile se documentato come decisione e verificato sul canale chiuso |
| Test solo manuali su DHU | Veloce | Regressioni di layout invisibili | Mai: la geometria va in funzioni pure testate |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| `AppManager.setSurfaceCallback` | Chiamarlo senza `ACCESS_SURFACE`, o non azzerarlo alla distruzione | Permesso nel manifest, `try/catch(SecurityException)` con fallback Pane, `setSurfaceCallback(null)` su destroy |
| `MapWithContentTemplate` | Costruirlo senza content template, o su host < API 7 | Content `MessageTemplate` sempre presente, check di `carAppApiLevel` o `minCarApiLevel=7` |
| `SurfaceContainer` | Assumere surface non null e dimensioni > 0 | Guardie null/0, `isValid` prima del lock |
| Visible e stable area | Centrare sulla Surface intera, o trattare un rect vuoto come "tutto lo schermo" | Layout su stable area, fallback con margine se vuota |
| Play Console | Caricare la v2.1 direttamente sul test aperto | Prima test chiuso (non bloccante), poi promozione |
| Tema auto | Leggere `isDarkMode` una sola volta | Ridisegnare su `Session.onCarConfigurationChanged` |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| Render loop continuo | Telefono caldo, batteria in calo più veloce che in v2.0 | Render solo a evento, al massimo 1 fps in guida | Sessioni di guida lunghe (oltre 30 min) |
| Ricerca della dimensione font a ogni frame | Spike CPU a ogni tick | Ricalcolare la dimensione solo quando cambiano area, dpi o numero di cifre, poi cache | Ogni secondo, sempre |
| Collezione GPS legata alla Surface | Richieste di posizione riavviate, salti di distanza | Collezione legata allo `Screen` | A ogni cambio template o rientro da un'altra app |

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| Rilassare `HostValidator` (per esempio `ALLOW_ALL` in release) per "far funzionare" la Surface su DHU | Binding da host arbitrari | Invariato rispetto a Fase 11: allow-list in release, `ALLOW_ALL` solo in debug |
| Aggiungere `ACCESS_BACKGROUND_LOCATION` per problemi di stale che in realtà vengono dalla Surface (Pitfall 6) | Espansione di permessi inutile e onere data-safety | Diagnosticare prima il renderer (redraw da cache). SC2 di Fase 11 ha già provato che la posizione arriva a telefono bloccato |
| Log persistente di debug con coordinate GPS | Dati di posizione su disco | Loggare solo errori, dimensioni e livello API, mai lat/lon |

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| Numero centrato sullo schermo fisico ma coperto dal pannello o dallo strip | Lettura a colpo d'occhio compromessa (Core Value) | Centrare nella stable area |
| "0" disegnato prima della prima lettura | Velocità falsa mostrata come reale | Stato neutro finché non c'è `Reading` |
| Numero che tremola ogni secondo | Distrazione | Cifre tabulari (`tnum`) o misura su "888" |
| Messaggi di stato disegnati sulla Surface senza Action | Utente bloccato dopo un rifiuto del permesso | Messaggi e Action nel `MessageTemplate`, Surface solo per la velocità |
| Sfondo bianco pieno di notte | Abbagliamento | Seguire `isDarkMode` o sempre scuro, mai "sempre chiaro" |

## "Looks Done But Isn't" Checklist

- [ ] **Surface ricreata:** dopo il rientro da Google Maps, a veicolo **fermo**, il numero ricompare subito (niente attesa di un cambio di velocità).
- [ ] **Rilascio Surface:** ogni `onSurfaceAvailable` con Surface nuova rilascia la precedente, e `onSurfaceDestroyed` rilascia.
- [ ] **Nessun `invalidate()` periodico:** il log `onGetTemplate #n` cresce solo alle transizioni di stato.
- [ ] **Flusso di Fase 9 intatto:** richiesta automatica, concessione, rifiuto, rifiuto permanente e apertura impostazioni rifatti dal vivo su DHU con il nuovo template.
- [ ] **Livello Car API:** loggato e compatibile (≥7 se `MapWithContentTemplate`) sul telefono usato con la head unit reale, oppure fallback Pane verificato.
- [ ] **Giorno/notte:** cambio di tema dell'host con ridisegno immediato.
- [ ] **Più risoluzioni DHU:** numero e "km/h" interi e centrati su 480p, 720p, 1080p e widescreen.
- [ ] **Manifest coerente:** categoria, `ACCESS_SURFACE` e un solo permesso di template allineati. Commento `D-00a` aggiornato.
- [ ] **Canale:** v2.1 caricata prima sul test chiuso ed esito di revisione form factor letto prima della promozione all'aperto.
- [ ] **`playstore/`:** release notes it-IT/en-US e listing aggiornati alla v2.1.
- [ ] **Telefono invariato:** nessuna regressione su toggle, MAX, distanza, ricarica, CONN-01/02 dopo le pulizie.

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| v2.1 respinta in revisione (Pitfall 1) | MEDIUM | Tenere la v2.1 sul canale chiuso. Sul canale aperto restare alla vC4, oppure rilasciare una build che usa la Surface solo su canali non bloccanti (non esistono build flavor per canale: servirebbe un artefatto separato) o che torna al `PaneTemplate`. |
| Host < API 7 sui telefoni dei tester (Pitfall 4) | LOW | Branch a runtime con fallback `PaneTemplate` v2.0, che è già codice collaudato |
| Crash o leak della Surface in auto (Pitfall 6) | LOW-MEDIUM | Guardie e rilascio centralizzati nel renderer. Log persistente per la diagnosi |
| Layout coperto o tagliato su una head unit specifica (Pitfall 7/8) | LOW | Correggere la funzione pura di layout e aggiungere un caso di test con le dimensioni reali lette dal log |
| Quota esaurita per alternanza di template (Pitfall 9) | MEDIUM | Unificare tutti gli stati in un solo tipo di template contenitore |

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| 1. Revisione del canale aperto | F12 decisione, F15 rilascio | Decisione scritta categoria/permessi/canale. Esito revisione sul test chiuso prima della promozione |
| 2. Effetti su release esistente e cambio categoria | F12 | Piano di rollback documentato, artefatti vC4 conservati |
| 3. Contenuto obbligatorio di `MapWithContentTemplate` | F12 prototipo, F13 | Rect stable/visible misurati su DHU e confrontati con la spec D-14 aggiornata |
| 4. Livello Car API | F12, F13 | Log `carAppApiLevel` su DHU e sul telefono della head unit. Test puro della selezione template |
| 5. Permessi e categoria incoerenti | F13 | Revisione del manifest e avvio senza `SecurityException` su DHU |
| 6. Ciclo di vita della Surface | F13 | Test della macchina a stati del renderer. Rientro da Maps a veicolo fermo. Cicli connetti/disconnetti |
| 7. Aree stable/visible | F13 | Test JVM di `computeSpeedLayout` con rect vuoti e ridotti. DHU con action strip visibile |
| 8. Tipografia e dpi | F13, F15 | Test JVM su dimensioni multiple. DHU su 4 configurazioni. Head unit reale |
| 9. Flusso permesso di Fase 9 | F14 | Matrice live di Fase 9 rifatta. Contatore quota in Developer Mode |
| 10. Redraw eccessivo o `invalidate()` residuo | F13 | Log `onGetTemplate` solo alle transizioni. Nessun loop |
| 11. Threading | F13 | Revisione del codice: tutto sul main thread, niente `VirtualDisplay` |
| 12. Giorno/notte | F14, F15 | Cambio tema DHU e verifica in auto di giorno e di notte |
| 13. Strategia di test | F13 | Suite JVM nuova verde. `CarSpeedContentTest` aggiornato |
| 14. DHU vs head unit | F15 | Checklist in auto e log persistente letto dopo la sessione |
| 15. Effetti di NAVIGATION | F12 | Scelta POI confermata o motivata per iscritto |
| 19. Regressioni dalle pulizie | Fase pulizie (separata) | Test di equivalenza. `CarLinkSequenceTest` esteso |

## Sources

- **Sorgenti `androidx.car.app:app:1.7.0` e `app-testing:1.7.0`** (`-sources.jar` dalla cache Gradle locale del progetto): `AppManager.java` (permesso `ACCESS_SURFACE`, main thread, `SecurityException`), `SurfaceCallback.java` (obbligo `Surface#release()`, `onSurfaceAvailable` multiplo, rect vuoti = sconosciuti), `SurfaceContainer.java` (surface nullable), `navigation/model/MapWithContentTemplate.java` (`@RequiresCarApi(7)`, content obbligatorio, permessi), `navigation/model/NavigationTemplate.java` (action strip obbligatorio, reset quota), `model/MessageTemplate.java` (regola di refresh), `CarAppPermission.java` (avviso di rifiuto per categorie non ammesse), `testing/TestAppManager.java`. HIGH.
- [Draw maps | Android for Cars](https://developer.android.com/training/cars/apps/library/draw-maps): template a mappa per categoria, permessi, stable/visible area, dark mode, approccio VirtualDisplay. HIGH.
- [Build a point of interest app](https://developer.android.com/training/cars/apps/poi): `MapWithContentTemplate` disponibile alle POI con `MAP_TEMPLATES`. HIGH.
- [Build a navigation app](https://developer.android.com/training/cars/apps/navigation): obblighi della categoria NAVIGATION (NavigationManager, intent, auto-drive). HIGH.
- [Car app quality guidelines](https://developer.android.com/docs/quality-guidelines/car-app-quality): PF-1, NF-1..NF-9, MR-1, PC-1. HIGH sul testo, LOW su come verrà applicato a un tachimetro.
- [Distribute to cars](https://developer.android.com/training/cars/distribute): gating per canale (chiuso non bloccante, aperto/produzione bloccanti). HIGH. Nessuna informazione su cambi di categoria o effetti sulle release esistenti (gap, Pitfall 2).
- [Template restrictions](https://developer.android.com/training/cars/apps/library/template-restrictions): quota di 5 template, refresh, reset tramite `NavigationTemplate`. HIGH. Il riassunto recuperato non elenca `MapWithContentTemplate` tra i template ammessi come "ultimo del task" né come reset: da verificare empiricamente con l'overlay Developer Mode (MEDIUM).
- [Can I use MapWithContentTemplate in POI Category app — Android Auto Community](https://support.google.com/androidauto/thread/284127264/can-i-use-mapwithcontenttemplate-in-poi-category-app-in-android-auto?hl=en): la domanda esiste, contenuto non leggibile. LOW.
- [App rejected. Issue found: Category not permitted](https://support.google.com/googleplay/android-developer/thread/232683714/app-rejected-issue-found-category-not-permitted?hl=en), [App rejected due to map not loading on Android Auto](https://support.google.com/googleplay/android-developer/thread/337067941/app-is-rejected-due-to-map-not-loading-on-android-auto-but-actually-works-in-real-physical-device?hl=en): i rifiuti per categoria o contenuto della mappa sono reali e ricorrenti. MEDIUM sull'esistenza, LOW sui dettagli.
- [Android Developers Blog, maggio 2026](https://android-developers.googleblog.com/2026/05/android-for-cars-unifying-platforms-premium-experiences.html?m=1): Car App Library 1.8.0-beta01 / 1.9.0-alpha01, nessuna nuova categoria o accesso generico alla Surface annunciato. MEDIUM.
- `.planning/milestones/v2.0-research/PITFALLS.md` e `.planning/PROJECT.md`: contesto di Fase 8-11 (limite di `PaneTemplate`, flusso permesso, HostValidator, USB occupata in auto).

---
*Pitfalls research for: rendering diretto sulla Surface Android Auto in un'app template esistente (Tachimetro v2.1)*
*Researched: 2026-09-23*
