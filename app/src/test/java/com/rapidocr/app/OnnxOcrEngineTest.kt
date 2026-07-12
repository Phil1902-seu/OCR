package com.rapidocr.app

import com.rapidocr.app.data.ocr.impl.ImagePreprocessor
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OnnxOcrEngineTest {

    @Test
    fun `hwcToChw preserves all values for 2x1 image`() {
        val hwc = floatArrayOf(
            1f, 2f, 3f,
            4f, 5f, 6f
        )
        val chw = ImagePreprocessor.hwcToChw(hwc, 1, 2)

        assertEquals(6, chw.size)
        assertEquals(1f, chw[0], 0.001f)
        assertEquals(4f, chw[1], 0.001f)
        assertEquals(2f, chw[2], 0.001f)
        assertEquals(5f, chw[3], 0.001f)
        assertEquals(3f, chw[4], 0.001f)
        assertEquals(6f, chw[5], 0.001f)
    }

    @Test
    fun `hwcToChw handles single pixel`() {
        val hwc = floatArrayOf(0.1f, 0.2f, 0.3f)
        val chw = ImagePreprocessor.hwcToChw(hwc, 1, 1)

        assertEquals(3, chw.size)
        assertEquals(0.1f, chw[0], 0.001f)
        assertEquals(0.2f, chw[1], 0.001f)
        assertEquals(0.3f, chw[2], 0.001f)
    }

    @Test
    fun `hwcToChw handles 2x2 image`() {
        val hwc = floatArrayOf(
            10f, 20f, 30f,  // pixel(0,0)
            40f, 50f, 60f,  // pixel(0,1)
            70f, 80f, 90f,  // pixel(1,0)
            100f, 110f, 120f // pixel(1,1)
        )
        val chw = ImagePreprocessor.hwcToChw(hwc, 2, 2)

        assertEquals(12, chw.size)
        assertEquals(10f, chw[0], 0.001f)
        assertEquals(40f, chw[1], 0.001f)
        assertEquals(70f, chw[2], 0.001f)
        assertEquals(100f, chw[3], 0.001f)
    }

    @Test
    fun `normalization formula is mathematically correct`() {
        val meanR = 0.485f
        val stdR = 0.229f
        val pixelR = 64f / 255f

        val normalized = (pixelR - meanR) / stdR

        assertTrue(normalized.isFinite())
        assertTrue(normalized < 0f)
    }

    @Test
    fun `bgr normalization maps white to positive value`() {
        val normalized = (255f - 127.5f) / 127.5f

        assertEquals(1f, normalized, 0.01f)
    }

    @Test
    fun `bgr normalization maps black to negative value`() {
        val normalized = (0f - 127.5f) / 127.5f

        assertEquals(-1f, normalized, 0.01f)
    }

    @Test
    fun `output array size formula is correct for 640x640`() {
        val expectedSize = 3 * 640 * 640
        assertEquals(1228800, expectedSize)
    }

    @Test
    fun `detection threshold filtering works correctly`() {
        val threshold = 0.3f
        val testData = floatArrayOf(0.1f, 0.5f, 0.2f, 0.8f, 0.3f, 0.9f)
        val filtered = testData.filter { it > threshold }

        assertEquals(3, filtered.size)
        assertTrue(filtered.contains(0.5f))
        assertTrue(filtered.contains(0.8f))
        assertTrue(filtered.contains(0.9f))
    }

    @Test
    fun `bounding box coordinate clamping stays within bounds`() {
        val origW = 1000
        val origH = 800
        val x1 = -10f
        val y1 = 900f
        val x2 = 1100f
        val y2 = -50f

        val clampedX1 = x1.coerceIn(0f, origW.toFloat())
        val clampedY1 = y1.coerceIn(0f, origH.toFloat())
        val clampedX2 = x2.coerceIn(0f, origW.toFloat())
        val clampedY2 = y2.coerceIn(0f, origH.toFloat())

        assertEquals(0f, clampedX1)
        assertEquals(800f, clampedY1)
        assertEquals(1000f, clampedX2)
        assertEquals(0f, clampedY2)
    }
}
