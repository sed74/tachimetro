package com.sed.tachimetro.car

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Plain JVM unit tests for [AreaPx], [effectiveArea], [fitTextSizePx] and [digitHeightPx] --
 * no Android runtime. Locks D-12 (the "888" digit height reachable inside an area is computed
 * with one shared formula for both the POI and the NAVIGATION path, so the comparison is fair)
 * and the "empty Rect = area not known yet" host contract (ARCHITECTURE.md).
 */
class SurfaceTextFitTest {

    private val delta = 0.01f

    @Test
    fun areaPx_computesWidthHeightAndNotEmpty() {
        val area = AreaPx(0, 0, 800, 480)
        assertEquals(800, area.width)
        assertEquals(480, area.height)
        assertFalse(area.isEmpty)
    }

    @Test
    fun zeroWidthArea_isEmpty() {
        // AreaPx(10, 10, 10, 50): width 0 -> empty.
        assertTrue(AreaPx(10, 10, 10, 50).isEmpty)
    }

    @Test
    fun nullStable_fallsBackToWholeSurface() {
        assertEquals(AreaPx(0, 0, 1280, 720), effectiveArea(null, 1280, 720))
    }

    @Test
    fun emptyStable_fallsBackToWholeSurface() {
        // The host signals "stable area not known yet" with an empty Rect.
        assertEquals(AreaPx(0, 0, 1280, 720), effectiveArea(AreaPx(0, 0, 0, 0), 1280, 720))
    }

    @Test
    fun validStable_isReturnedAsIs() {
        val stable = AreaPx(100, 50, 900, 600)
        assertEquals(stable, effectiveArea(stable, 1280, 720))
    }

    @Test
    fun heightBound_limitsTextSize() {
        // Area 1000x100, "888" at 100px measures 150x70: height bound 0.9*100/70*100 = 128.57,
        // width bound 0.9*1000/150*100 = 600 -> the height wins.
        val size = fitTextSizePx(
            areaWidth = 1000,
            areaHeight = 100,
            measuredWidthAtRef = 150f,
            measuredHeightAtRef = 70f,
            refSizePx = 100f,
        )
        assertEquals(0.9f * 100f / 70f * 100f, size, delta)
        assertEquals(128.57f, size, delta)
    }

    @Test
    fun widthBound_limitsTextSize() {
        // Area 300x1000, same measures: width bound 0.9*300/150*100 = 180.
        val size = fitTextSizePx(
            areaWidth = 300,
            areaHeight = 1000,
            measuredWidthAtRef = 150f,
            measuredHeightAtRef = 70f,
            refSizePx = 100f,
        )
        assertEquals(180f, size, delta)
    }

    @Test
    fun customFillFraction_scalesResult() {
        val size = fitTextSizePx(
            areaWidth = 300,
            areaHeight = 1000,
            measuredWidthAtRef = 150f,
            measuredHeightAtRef = 70f,
            refSizePx = 100f,
            fillFraction = 1f,
        )
        assertEquals(200f, size, delta)
    }

    @Test
    fun nonPositiveInputs_returnZero() {
        assertEquals(0f, fitTextSizePx(0, 100, 150f, 70f, 100f), 0f)
        assertEquals(0f, fitTextSizePx(1000, -1, 150f, 70f, 100f), 0f)
        assertEquals(0f, fitTextSizePx(1000, 100, 0f, 70f, 100f), 0f)
        assertEquals(0f, fitTextSizePx(1000, 100, 150f, 0f, 100f), 0f)
        assertEquals(0f, fitTextSizePx(1000, 100, 150f, 70f, 0f), 0f)
    }

    @Test
    fun digitHeight_scalesLinearly() {
        // "888" is 70px tall at 100px text size -> 140px tall at 200px.
        assertEquals(140f, digitHeightPx(200f, 70f, 100f), delta)
    }

    @Test
    fun digitHeight_zeroRefSize_returnsZero() {
        assertEquals(0f, digitHeightPx(200f, 70f, 0f), 0f)
    }
}
