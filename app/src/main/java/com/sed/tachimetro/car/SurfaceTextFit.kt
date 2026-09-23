package com.sed.tachimetro.car

import kotlin.math.min

/*
 * D-12 (Fase 12): calcolo puro dell'altezza delle cifre "888" ottenibile dentro un'area della
 * Surface di Android Auto. Il criterio di decisione POI vs NAVIGATION confronta proprio questa
 * altezza, quindi la STESSA formula viene applicata a entrambi i percorsi: il confronto e' equo
 * perche' cambia solo l'area (stable area dell'host), mai il modo di riempirla.
 *
 * Nessun import Android qui (ARCHITECTURE.md Anti-Pattern 6: niente `android.graphics.Rect`
 * nelle funzioni pure): le misure del testo arrivano dal chiamante, che le ottiene con
 * `Paint.getTextBounds` nel renderer (Piano 02). Nomi generici di proposito, cosi' la Fase 13
 * riusa queste funzioni nel layout definitivo.
 */

/**
 * D-12: rettangolo in pixel della Surface, equivalente framework-free di `android.graphics.Rect`
 * (stessa semantica: `right`/`bottom` esclusivi).
 */
data class AreaPx(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top

    /** Area vuota o degenere: l'host la usa per dire "area non ancora nota". */
    val isEmpty: Boolean get() = width <= 0 || height <= 0
}

/**
 * D-12: area in cui centrare e dimensionare "888".
 *
 * L'host segnala "area non ancora nota" con un Rect vuoto (ARCHITECTURE.md, fatti verificati),
 * oppure non ha ancora chiamato `onStableAreaChanged` (null): in entrambi i casi si ripiega
 * sull'intera Surface, altrimenti si usa la stable area ricevuta.
 *
 * @param stable ultima stable area ricevuta dall'host, o null se non ancora ricevuta
 * @param surfaceWidth larghezza della Surface in pixel
 * @param surfaceHeight altezza della Surface in pixel
 * @return l'area effettiva da usare per il layout
 */
fun effectiveArea(stable: AreaPx?, surfaceWidth: Int, surfaceHeight: Int): AreaPx =
    if (stable == null || stable.isEmpty) AreaPx(0, 0, surfaceWidth, surfaceHeight) else stable

/**
 * D-12: dimensione del testo (px) che fa entrare "888" nell'area, occupandone al massimo la
 * frazione [fillFraction] sia in larghezza sia in altezza.
 *
 * Le misure sono i bounds reali del testo "888" disegnato a [refSizePx], ottenuti dal chiamante
 * con `Paint.getTextBounds` (il renderer, Piano 02); la scala e' lineare nella dimensione del
 * testo, quindi basta una misura di riferimento.
 *
 * @return la dimensione del testo, oppure `0f` se area, misure o [refSizePx] non sono positive
 *   (area sconosciuta o misura fallita: niente testo invece di un valore assurdo)
 */
fun fitTextSizePx(
    areaWidth: Int,
    areaHeight: Int,
    measuredWidthAtRef: Float,
    measuredHeightAtRef: Float,
    refSizePx: Float,
    fillFraction: Float = 0.9f,
): Float {
    if (areaWidth <= 0 || areaHeight <= 0) return 0f
    if (measuredWidthAtRef <= 0f || measuredHeightAtRef <= 0f || refSizePx <= 0f) return 0f
    val heightBound = fillFraction * areaHeight / measuredHeightAtRef
    val widthBound = fillFraction * areaWidth / measuredWidthAtRef
    return min(heightBound, widthBound) * refSizePx
}

/**
 * D-12: altezza in pixel delle cifre "888" disegnate a [textSizePx], a partire dall'altezza
 * [measuredHeightAtRef] misurata a [refSizePx]. E' il numero che il criterio D-12 confronta tra
 * POI e NAVIGATION (POI vince se >= 70% di NAVIGATION alla stessa risoluzione).
 *
 * @return l'altezza delle cifre, oppure `0f` se [refSizePx] non e' positivo
 */
fun digitHeightPx(textSizePx: Float, measuredHeightAtRef: Float, refSizePx: Float): Float =
    if (refSizePx <= 0f) 0f else measuredHeightAtRef * textSizePx / refSizePx
