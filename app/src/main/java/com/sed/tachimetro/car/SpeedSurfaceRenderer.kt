package com.sed.tachimetro.car

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface

import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

import kotlin.math.max

import com.sed.tachimetro.BuildConfig

/**
 * Fase 12 spike (REL-01): renderer passivo della Surface di Android Auto.
 *
 * - D-03: disegna solo una prova statica -- sfondo pieno, "888" centrato e dimensionato nella
 *   stable area con [fitTextSizePx], contorni della stable area (verde) e della visible area
 *   (magenta). NON e' collegato al provider GPS: nessun dato di velocita' o posizione.
 * - D-10: ogni callback della Surface e ogni frame disegnato producono una riga logcat con tag
 *   `TachimetroSurface` (solo in `BuildConfig.DEBUG`), nel formato chiave=valore consumato dallo
 *   script `scripts/surface-spike-check.ps1` (Piano 03).
 * - D-11: scrive in piccolo `api=<carAppApiLevel>` e la variante di build, leggibili da uno
 *   screenshot o in auto senza USB.
 *
 * Unico file del progetto che disegna con `Canvas`/`Paint`: la geometria viene convertita in
 * [AreaPx] qui e le funzioni pure di `SurfaceTextFit.kt` restano framework-free.
 *
 * WR-04: tiene il [CarContext] della Session che lo possiede (vive quanto lei), mai
 * un'Activity. Registrato come observer del lifecycle della Session: `onCreate` registra il
 * callback, `onDestroy` ferma i frame pendenti e rilascia la Surface.
 */
class SpeedSurfaceRenderer(private val carContext: CarContext) :
    SurfaceCallback,
    DefaultLifecycleObserver {

    companion object {
        private const val LOG_TAG = "TachimetroSurface"
        private const val REF_TEXT_SIZE_PX = 100f
        private const val SAMPLE_TEXT = "888"
        private const val OUTLINE_STROKE_PX = 4f
        private const val INFO_TEXT_MIN_PX = 16f
        private const val INFO_TEXT_FRACTION = 0.04f
        private const val COLOR_STABLE = "#00FF00"
        private const val COLOR_VISIBLE = "#FF00FF"
        private const val COLOR_INFO = "#FFFF00"
    }

    private var surface: Surface? = null
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var dpi = 0
    private var stableArea: AreaPx? = null
    private var visibleArea: AreaPx? = null

    private val handler = Handler(Looper.getMainLooper())
    private var renderPending = false

    private val digitPaint = Paint().apply {
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        color = Color.WHITE
    }

    private val stablePaint = outlinePaint(COLOR_STABLE)
    private val visiblePaint = outlinePaint(COLOR_VISIBLE)

    private val infoPaint = Paint().apply {
        isAntiAlias = true
        typeface = Typeface.MONOSPACE
        color = Color.parseColor(COLOR_INFO)
    }

    private fun outlinePaint(hex: String) = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = OUTLINE_STROKE_PX
        color = Color.parseColor(hex)
    }

    /**
     * Chiede un nuovo frame sul main thread. Coalescente: piu' richieste ravvicinate (es. stable
     * e visible area che arrivano insieme) producono un solo disegno.
     */
    fun requestRender() {
        if (renderPending) return
        renderPending = true
        handler.post {
            renderPending = false
            drawFrame()
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(this)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        // T-12-08: nessun frame pendente dopo la fine della Session, Surface rilasciata.
        handler.removeCallbacksAndMessages(null)
        renderPending = false
        surface?.release()
        surface = null
    }

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        val incoming = surfaceContainer.surface
        // ARCHITECTURE.md Anti-Pattern 4: se l'host consegna una Surface nuova senza aver
        // distrutto la precedente, quella vecchia va rilasciata qui.
        val previous = surface
        if (previous != null && previous !== incoming) {
            previous.release()
        }
        surface = incoming
        surfaceWidth = surfaceContainer.width
        surfaceHeight = surfaceContainer.height
        dpi = surfaceContainer.dpi

        if (BuildConfig.DEBUG) {
            Log.d(
                LOG_TAG,
                "onSurfaceAvailable w=$surfaceWidth h=$surfaceHeight dpi=$dpi " +
                    "api=${carContext.carAppApiLevel} variant=${BuildConfig.FLAVOR} " +
                    "card=${BuildConfig.SPIKE_POI_CARD}",
            )
        }
        requestRender()
    }

    override fun onStableAreaChanged(stableArea: Rect) {
        this.stableArea = stableArea.toAreaPx()
        // D-10: valore grezzo, anche se vuoto (l'host usa il Rect vuoto per "non ancora nota").
        if (BuildConfig.DEBUG) {
            Log.d(
                LOG_TAG,
                "onStableAreaChanged l=${stableArea.left} t=${stableArea.top} " +
                    "r=${stableArea.right} b=${stableArea.bottom}",
            )
        }
        requestRender()
    }

    override fun onVisibleAreaChanged(visibleArea: Rect) {
        this.visibleArea = visibleArea.toAreaPx()
        if (BuildConfig.DEBUG) {
            Log.d(
                LOG_TAG,
                "onVisibleAreaChanged l=${visibleArea.left} t=${visibleArea.top} " +
                    "r=${visibleArea.right} b=${visibleArea.bottom}",
            )
        }
        requestRender()
    }

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "onSurfaceDestroyed")
        }
        val current = surface
        if (current != null && current === surfaceContainer.surface) {
            current.release()
        }
        surface = null
    }

    // Conversione da Rect SOLO qui (Anti-Pattern 6): le funzioni pure ricevono AreaPx.
    private fun Rect.toAreaPx() = AreaPx(left, top, right, bottom)

    private fun drawFrame() {
        val target = surface ?: return
        if (!target.isValid) return

        // T-12-05: input anomalo dall'host (Surface gia' rilasciata o bloccata) -> niente frame,
        // mai un crash.
        val canvas = try {
            target.lockCanvas(null)
        } catch (e: IllegalArgumentException) {
            return
        } catch (e: IllegalStateException) {
            return
        } ?: return

        try {
            drawContent(canvas)
        } finally {
            target.unlockCanvasAndPost(canvas)
        }
    }

    private fun drawContent(canvas: Canvas) {
        // 1. D-03: sfondo pieno.
        canvas.drawColor(Color.BLACK)

        // 2. Area di fit: stable area se nota, altrimenti l'intera Surface.
        val area = effectiveArea(stableArea, surfaceWidth, surfaceHeight)
        val areaLabel = if (stableArea?.isEmpty == false) "stable" else "surface"

        // 3. "888" misurato alla dimensione di riferimento, poi scalato con la formula D-12.
        val bounds = Rect()
        digitPaint.textSize = REF_TEXT_SIZE_PX
        digitPaint.getTextBounds(SAMPLE_TEXT, 0, SAMPLE_TEXT.length, bounds)
        val measuredW = bounds.width().toFloat()
        val measuredH = bounds.height().toFloat()
        val textSize = fitTextSizePx(area.width, area.height, measuredW, measuredH, REF_TEXT_SIZE_PX)

        if (textSize > 0f) {
            digitPaint.textSize = textSize
            digitPaint.getTextBounds(SAMPLE_TEXT, 0, SAMPLE_TEXT.length, bounds)
            // Centratura sui bounds reali alla dimensione finale (non sulla baseline nuda):
            // l'origine del testo va spostata di -bounds.left / -bounds.top.
            val x = area.left + (area.width - bounds.width()) / 2f - bounds.left
            val y = area.top + (area.height - bounds.height()) / 2f - bounds.top
            canvas.drawText(SAMPLE_TEXT, x, y, digitPaint)
        }

        // 4. Contorni: verde = stable area, magenta = visible area (omessi se sconosciute).
        stableArea?.takeUnless { it.isEmpty }?.let { canvas.drawArea(it, stablePaint) }
        visibleArea?.takeUnless { it.isEmpty }?.let { canvas.drawArea(it, visiblePaint) }

        // 5. D-11: livello API e variante in alto a sinistra, dentro la visible area se nota.
        val infoArea = visibleArea?.takeUnless { it.isEmpty }
            ?: AreaPx(0, 0, surfaceWidth, surfaceHeight)
        val infoSize = max(INFO_TEXT_MIN_PX, INFO_TEXT_FRACTION * surfaceHeight)
        infoPaint.textSize = infoSize
        val infoX = infoArea.left + OUTLINE_STROKE_PX * 2
        val line1Y = infoArea.top + OUTLINE_STROKE_PX * 2 + infoSize
        canvas.drawText(
            "api=${carContext.carAppApiLevel} ${BuildConfig.FLAVOR}/${BuildConfig.SPIKE_POI_CARD} " +
                "${surfaceWidth}x$surfaceHeight",
            infoX,
            line1Y,
            infoPaint,
        )
        canvas.drawText("verde=stable magenta=visible", infoX, line1Y + infoSize * 1.2f, infoPaint)

        // 6. D-10: misura del frame (solo geometria, mai velocita' o posizione -- T-08-07).
        if (BuildConfig.DEBUG) {
            val digitH = digitHeightPx(textSize, measuredH, REF_TEXT_SIZE_PX).toInt()
            Log.d(
                LOG_TAG,
                "frame area=$areaLabel w=${area.width} h=${area.height} " +
                    "textSize=${textSize.toInt()} digitH=$digitH",
            )
        }
    }

    // Il contorno e' disegnato mezzo spessore all'interno, cosi' resta visibile anche quando
    // l'area coincide con i bordi della Surface.
    private fun Canvas.drawArea(area: AreaPx, paint: Paint) {
        val inset = paint.strokeWidth / 2f
        drawRect(
            area.left + inset,
            area.top + inset,
            area.right - inset,
            area.bottom - inset,
            paint,
        )
    }
}
