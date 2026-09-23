# Fase 12: risultati dello spike Surface (POI vs NAVIGATION)

## Contesto

- **Fase:** 12, Spike Surface e decisione categoria/template (milestone v2.1)
- **Requisiti:** REL-01 (scelta categoria/template su misure reali), REL-02 (manifest finale, `minCarApiLevel` 7)
- **Data sessione DHU:** TBD
- **Telefono:** OnePlus 8T (fisico, collegato via USB)
- **Versione DHU (Desktop Head Unit):** TBD
- **Versione Android Auto sul telefono:** TBD
- **Runbook:** [`docs/surface-spike-verification.md`](../../../docs/surface-spike-verification.md)
- **Script di misura:** `scripts/surface-spike-check.ps1` (summary in `build/surface-spike/<timestamp>-<variante>-<risoluzione>-summary.txt`)
- **Build:** flavor temporanei `spikeNav` (`NavigationTemplate`) e `spikePoi` (`MapWithContentTemplate`, card scelta con `-PpoiCard=message|list|pane|grid`), solo APK debug locali
- **D-05:** nessuna build di spike e' stata caricata sul Play Store (nessun canale, nemmeno test interno).

Disegno di prova (D-03): sfondo nero, "888" bianco centrato nella stable area, contorno verde = stable area, contorno magenta = visible area, testo giallo `api=<carAppApiLevel> <flavor>/<card> WxH`. Nessun dato di posizione o velocita' reale.

## Misure DHU

D-09/D-10: una riga per variante e risoluzione. I valori vengono dal summary piu' recente dello script per ogni combinazione; ogni riga rimanda al proprio screenshot.

| Variante | Risoluzione | Surface | dpi | Car API | Stable (l,t,r,b) | Visible (l,t,r,b) | Area fit | digitH px | Screenshot |
|----------|-------------|---------|-----|---------|------------------|-------------------|----------|-----------|------------|
| nav | 800x480 | TBD | TBD | TBD | TBD | TBD | TBD | TBD | [nav-800x480](spike-screenshots/nav-800x480.png) |
| poi-message | 800x480 | TBD | TBD | TBD | TBD | TBD | TBD | TBD | [poi-message-800x480](spike-screenshots/poi-message-800x480.png) |
| poi-list | 800x480 | TBD | TBD | TBD | TBD | TBD | TBD | TBD | [poi-list-800x480](spike-screenshots/poi-list-800x480.png) |
| poi-pane | 800x480 | TBD | TBD | TBD | TBD | TBD | TBD | TBD | [poi-pane-800x480](spike-screenshots/poi-pane-800x480.png) |
| poi-grid | 800x480 | TBD | TBD | TBD | TBD | TBD | TBD | TBD | [poi-grid-800x480](spike-screenshots/poi-grid-800x480.png) |
| nav | 1280x720 | TBD | TBD | TBD | TBD | TBD | TBD | TBD | [nav-1280x720](spike-screenshots/nav-1280x720.png) |
| poi-message | 1280x720 | TBD | TBD | TBD | TBD | TBD | TBD | TBD | [poi-message-1280x720](spike-screenshots/poi-message-1280x720.png) |
| poi-list | 1280x720 | TBD | TBD | TBD | TBD | TBD | TBD | TBD | [poi-list-1280x720](spike-screenshots/poi-list-1280x720.png) |
| poi-pane | 1280x720 | TBD | TBD | TBD | TBD | TBD | TBD | TBD | [poi-pane-1280x720](spike-screenshots/poi-pane-1280x720.png) |
| poi-grid | 1280x720 | TBD | TBD | TBD | TBD | TBD | TBD | TBD | [poi-grid-1280x720](spike-screenshots/poi-grid-1280x720.png) |
| nav | 1920x1080 | TBD | TBD | TBD | TBD | TBD | TBD | TBD | [nav-1920x1080](spike-screenshots/nav-1920x1080.png) |
| poi-message | 1920x1080 | TBD | TBD | TBD | TBD | TBD | TBD | TBD | [poi-message-1920x1080](spike-screenshots/poi-message-1920x1080.png) |
| poi-list | 1920x1080 | TBD | TBD | TBD | TBD | TBD | TBD | TBD | [poi-list-1920x1080](spike-screenshots/poi-list-1920x1080.png) |
| poi-pane | 1920x1080 | TBD | TBD | TBD | TBD | TBD | TBD | TBD | [poi-pane-1920x1080](spike-screenshots/poi-pane-1920x1080.png) |
| poi-grid | 1920x1080 | TBD | TBD | TBD | TBD | TBD | TBD | TBD | [poi-grid-1920x1080](spike-screenshots/poi-grid-1920x1080.png) |

Legenda: "Area fit" = area (stable) in cui e' stato adattato l'"888"; "digitH px" = altezza delle cifre disegnate nell'ultimo frame.

## Osservazioni sull'host

SC1, D-06, D-08: cosa sovrappone l'host alla Surface in ciascun percorso.

### Percorso NAVIGATION (`NavigationTemplate`)

| Aspetto | Osservazione |
|---------|--------------|
| Action strip: posizione e ingombro | TBD |
| Action strip: scompare? dopo quanti secondi (atteso ~10 s) | TBD |
| Altro chrome dell'host (icona/titolo app, pulsanti) | TBD |
| Chiusure dell'app o errori dell'host | TBD |

### Percorso POI (`MapWithContentTemplate`)

| Variante card | Posizione e ingombro della card | Icona/titolo | Action strip | Chiusure/errori |
|---------------|---------------------------------|--------------|--------------|-----------------|
| poi-message | TBD | TBD | TBD | TBD |
| poi-list | TBD | TBD | TBD | TBD |
| poi-pane | TBD | TBD | TBD | TBD |
| poi-grid | TBD | TBD | TBD | TBD |

## Car API level

SC2, D-11.

| Host | Car API level | Fonte |
|------|---------------|-------|
| DHU | TBD | testo giallo `api=` sulla Surface e log `TachimetroSurface` |
| Head unit reale (sideload, opzionale) | TBD | testo giallo `api=` sulla Surface |

Se la head unit reale non viene osservata: rischio annotato, **head unit con Car API < 7 = app non visibile** (`minCarApiLevel` 7, rischio accettato con REL-02).

## Criterio D-12

Per ogni risoluzione R: `bestPoi(R) = max(digitH delle 4 varianti poi-*)`, `ratio(R) = bestPoi(R) / digitH(nav, R)`. POI soddisfa il criterio se `ratio(R) >= 0,70` su tutte e tre le risoluzioni. La soglia del 70% e' un valore di partenza: l'utente la conferma o la corregge nel Piano 05.

| Risoluzione | digitH NAV | Miglior POI (variante) | digitH miglior POI | Rapporto | >= 70%? |
|-------------|------------|------------------------|--------------------|----------|---------|
| 800x480 | TBD | TBD | TBD | TBD | TBD |
| 1280x720 | TBD | TBD | TBD | TBD | TBD |
| 1920x1080 | TBD | TBD | TBD | TBD | TBD |

**Esito complessivo:** POI soddisfa D-12 su tutte e tre le risoluzioni: TBD

## Rischi di distribuzione

D-13 e `.planning/research/PITFALLS.md` (Pitfall 1). Il canale di test aperto ha revisione Android Auto **bloccante**.

- **NAVIGATION + `NavigationTemplate`:** i requisiti di qualita' NF-1 (navigazione turn-by-turn obbligatoria) e NF-6 (gestione degli intent di navigazione) non sono soddisfacibili da un tachimetro; NF-2 chiede che sulla Surface ci sia "only map content". Il rifiuto in revisione e' molto probabile (confidenza MEDIUM-HIGH). Se si sceglie comunque NAVIGATION, la build passa prima solo dal canale di test chiuso e, con esito negativo, si rientra su POI senza toccare il canale aperto (D-13).
- **POI + `MapWithContentTemplate`:** `MAP_TEMPLATES` senza una mappa reale e' discutibile in revisione (PF-1 "meaningful functionality relevant to driving", PC-1; il Javadoc di `MAP_TEMPLATES` avverte del possibile rifiuto per app fuori categoria). Resta pero' la stessa categoria di oggi. La card di contenuto e' **obbligatoria** (`setContentTemplate()` validato da `build()`) e l'host la disegna sopra la Surface, riducendo stable/visible area.
- **Mai** dichiarare insieme `MAP_TEMPLATES` e `NAVIGATION_TEMPLATES` in un artefatto (D-01).

## Note tecniche dello spike

- Nel ramo Granted di `SpeedScreen` l'`invalidate()` a 1 Hz e' soppresso: il template con Surface e' statico e il disegno passa dalla Surface, non dal template (Anti-Pattern 1).
- Card POI senza action strip e azione NAV con `CarIcon.APP_ICON`: entrambe accettate da `build()` (12-02-SUMMARY), nessun fallback d'icona necessario; il comportamento dell'host si conferma con le osservazioni sopra.
- I contorni sono disegnati mezzo spessore all'interno dell'area, cosi' restano visibili anche sui bordi della Surface.
- Il renderer logga con tag `TachimetroSurface` (solo `BuildConfig.DEBUG`) a ogni `onSurfaceAvailable`, `onStableAreaChanged`, `onVisibleAreaChanged` (contratto D-10).
- Testo della card POI: segnaposto "Tachimetro" (D-07), mai la velocita'.

## Decisione

Compilata nel Piano 05 dopo il gate umano D-12/D-14.
