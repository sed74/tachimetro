---
phase: 08-fondamenta-condivise-e-velocit-sullo-schermo-auto
plan: 02
subsystem: infra
tags: [android-auto, car-app-library, kotlin, pane-template, poi]

# Dependency graph
requires:
  - phase: 08-fondamenta-condivise-e-velocit-sullo-schermo-auto (Piano 01)
    provides: "GpsSpeedProvider Application-scoped (TachimetroApplication.gpsSpeedProvider), CarSpeedContent (contratto puro SpeedState -> Speed/Searching), androidx.car.app 1.7.0 pinnato, BuildConfig abilitato, R.string.car_searching_gps_signal"
provides:
  - "Manifest Android Auto completo: automotive_app_desc.xml, meta-data com.google.android.gms.car.application/minCarApiLevel, <service> TachimetroCarAppService categoria POI"
  - "TachimetroCarAppService: entry point bindato dall'host (HostValidator permissivo marcato Fase 11)"
  - "TachimetroCarSession: crea SpeedScreen come schermo iniziale, nessun routing su intent"
  - "SpeedScreen: collega lo StateFlow condiviso, costruisce PaneTemplate a 1 Hz, gate difensivo sul permesso"
affects: [08-03-verifica-quota-refresh, 09-permesso-localizzazione-schermo-auto, 11-hardening-produzione]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "PaneTemplate/Row/Pane come unico contenuto dello schermo auto POI, nessun Surface/Canvas"
    - "Screen come LifecycleOwner: stesso repeatOnLifecycle(STARTED)/lifecycleScope della fase telefono, terminale invalidate() invece di TextView.text"
    - "Gate difensivo ContextCompat.checkSelfPermission prima di collezionare uno StateFlow che internamente chiama un'API @Suppress(MissingPermission)"

key-files:
  created:
    - app/src/main/res/xml/automotive_app_desc.xml
    - app/src/main/java/com/sed/tachimetro/car/TachimetroCarAppService.kt
    - app/src/main/java/com/sed/tachimetro/car/TachimetroCarSession.kt
    - app/src/main/java/com/sed/tachimetro/car/SpeedScreen.kt
  modified:
    - app/src/main/AndroidManifest.xml

key-decisions:
  - "Verifica assembleDebug eseguita una sola volta dopo il Task 3 (non dopo ogni task): TachimetroCarSession.onCreateScreen referenzia SpeedScreen, quindi il progetto non compila finche' il Task 3 non esiste. I commit restano comunque atomici per file/task; solo il controllo di build e' stato posticipato."
  - "Commenti XML nel manifest riscritti per evitare '--' consecutivi (SAXParseException: la stringa '--' non e' permessa nei commenti XML) -- stesso contenuto informativo, solo punteggiatura del commento."

patterns-established:
  - "Pattern: Screen (Car App Library) come consumatore reattivo dello StateFlow Application-scoped, con lo stesso repeatOnLifecycle(STARTED) gia' usato da MainActivity, ma con un gate difensivo sul permesso prima della collect (assente lato telefono perche' li' il permesso e' gia' garantito da MainActivity prima di avviare la collection)"

requirements-completed: [AA-01, AA-02, AA-03]

# Metrics
duration: ~15min
completed: 2026-08-31
---

# Phase 08 Plan 02: Scaffolding Android Auto e Schermo Velocita' Summary

**App scopribile dall'host Android Auto come servizio POI a template (TachimetroCarAppService/Session), con SpeedScreen che mostra la velocita' corrente in un PaneTemplate aggiornato a 1 Hz dallo stesso GpsSpeedProvider condiviso col telefono.**

## Performance

- **Duration:** ~15 min
- **Started:** 2026-08-31 (dopo base commit ab1234c, aggiornamento tracking Piano 01)
- **Completed:** 2026-08-31T08:15Z
- **Tasks:** 3 completati
- **Files modified:** 5 (4 creati, 1 modificato)

## Accomplishments
- L'APK debug dichiara ora `TachimetroCarAppService` come servizio esportato di categoria `androidx.car.app.category.POI` con `automotive_app_desc.xml` (solo `<uses name="template" />`) e `minCarApiLevel` 1 -- nessun nuovo permesso, nessuna esposizione come app Automotive OS nativa
- `SpeedScreen` collega lo `StateFlow<SpeedState>` condiviso da `TachimetroApplication` (nessuna seconda sottoscrizione GPS), ricostruisce il `PaneTemplate` a ogni emissione tramite `invalidate()`, ritmato esclusivamente dal ticker interno a 1 Hz di `GpsSpeedProvider` -- nessun timer lato auto (D-05)
- Contenuto della Row conforme al Content Contract: cifre + "km/h" separato quando c'e' un fix (D-01), "Ricerca segnale..." senza unita' quando manca (D-02), nessuna icona nella Row, nessun titolo/branding a livello di template (`setHeaderAction(Action.APP_ICON)`, D-03)
- Gate difensivo `ContextCompat.checkSelfPermission` prima di collezionare lo StateFlow, cosi' `SpeedScreen` non va in crash se raggiunto prima che ACCESS_FINE_LOCATION sia concesso (T-08-08) -- rivalutato a ogni rientro in `STARTED`

## Task Commits

Each task was committed atomically:

1. **Task 1: Scaffolding manifest Android Auto (categoria POI)** - `e788bac` (feat)
2. **Task 2: CarAppService e Session** - `5c8f2d3` (feat)
3. **Task 3: SpeedScreen -- template della velocita' aggiornato a 1 Hz** - `7c1baa8` (feat)

_Nessun task TDD in questo piano (tutte le classi sono componenti di integrazione Car App Library, non logica pura testabile in isolamento)._

## Files Created/Modified
- `app/src/main/res/xml/automotive_app_desc.xml` - descrittore `<automotiveApp><uses name="template" /></automotiveApp>`
- `app/src/main/AndroidManifest.xml` - meta-data `com.google.android.gms.car.application`/`androidx.car.app.minCarApiLevel`, `<service>` `TachimetroCarAppService` esportato categoria POI
- `app/src/main/java/com/sed/tachimetro/car/TachimetroCarAppService.kt` - `CarAppService`, `HostValidator.ALLOW_ALL_HOSTS_VALIDATOR` marcato `Fase 11`, `onCreateSession() -> TachimetroCarSession()`
- `app/src/main/java/com/sed/tachimetro/car/TachimetroCarSession.kt` - `Session`, `onCreateScreen(intent) -> SpeedScreen(carContext)`, nessun routing
- `app/src/main/java/com/sed/tachimetro/car/SpeedScreen.kt` - `Screen`, collector reattivo + `PaneTemplate`/`Row` costruiti da `carSpeedContent()`, log diagnostico refresh sotto `BuildConfig.DEBUG`

## Decisions Made
- Verifica `assembleDebug` posticipata dal Task 2 al Task 3: `TachimetroCarSession` referenzia `SpeedScreen`, quindi il modulo non compila finche' il Task 3 non esiste nello stesso piano. I commit restano atomici (ogni commit contiene solo i file del proprio task); solo l'esecuzione della build completa e' stata fatta una volta dopo il Task 3, confermando retroattivamente anche Task 1 e Task 2.
- Commenti XML nel manifest riformulati per evitare `--` consecutivi (regola XML: la sequenza `--` non e' permessa dentro un commento) -- stesso contenuto, solo wording; nessun cambio di comportamento o di contenuto informativo.
- `local.properties` (gitignored) creato localmente nel worktree con lo stesso `sdk.dir` del repo principale, per poter eseguire `./gradlew.bat` -- non tracciato da git, nessun impatto sul commit (stesso pattern gia' osservato nel Piano 01).

## Deviations from Plan

None - plan eseguito esattamente come scritto. I due aggiustamenti sopra (ordine della verifica build, sintassi commenti XML) sono correzioni meccaniche/di sequenza (Rule 3 - blocking issue: il progetto non avrebbe compilato altrimenti), non cambi di scope, contenuto o comportamento.

## Issues Encountered
- `./gradlew.bat :app:assembleDebug` ha fallito al primo tentativo con `SAXParseException: the string "--" is not permitted within comments` sui commenti XML aggiunti in `AndroidManifest.xml` (Task 1). Risolto riscrivendo i commenti senza `--` consecutivi; nessun impatto sul contenuto o sulle acceptance criteria (tutti i grep richiesti continuano a passare).
- Il worktree non aveva `local.properties`: ricreato localmente come nel Piano 01, file gitignored, nessun impatto sul commit.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Il Piano 03 (verifica empirica quota refresh via DHU/Developer Mode) puo' ora partire: `SpeedScreen` esiste, logga `onGetTemplate #<n> content=<Speed|Searching>` sotto `BuildConfig.DEBUG` per contare i refresh effettivi, e la cadenza e' gia' 1 Hz senza throttle
- Verifica formale del comportamento telefono invariato (`installDebug` + test manuale velocita'/MAX/distanza) resta esplicitamente demandata al Piano 03 per questo piano, come da `<verification>` punto 6 del PLAN.md
- Nessun blocco noto per il Piano 03; `HostValidator.ALLOW_ALL_HOSTS_VALIDATOR` resta marcato `Fase 11` per la sostituzione con allow-list reale

---
*Phase: 08-fondamenta-condivise-e-velocit-sullo-schermo-auto*
*Completed: 2026-08-31*

## Self-Check: PASSED

All created files verified present on disk: `automotive_app_desc.xml`, `TachimetroCarAppService.kt`, `TachimetroCarSession.kt`, `SpeedScreen.kt`, this SUMMARY.md. All task commits verified present in `git log --oneline --all`: `e788bac`, `5c8f2d3`, `7c1baa8`, `723ee58`.
