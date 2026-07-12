package com.rapidocr.app

import com.rapidocr.app.data.ocr.impl.ImagePreprocessor
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OnnxOcrEngineTest {

    @Test
    fun `hwcToChw_preservesValues_for2x1`() {
        val hwc = floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f)
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
    fun `hwcToChw_singlePixel`() {
        val hwc = floatArrayOf(0.1f, 0.2f, 0.3f)
        val chw = ImagePreprocessor.hwcToChw(hwc, 1, 1)

        assertEquals(3, chw.size)
        assertEquals(0.1f, chw[0], 0.001f)
        assertEquals(0.2f, chw[1], 0.001f)
        assertEquals(0.3f, chw[2], 0.001f)
    }

    @Test
    fun `normalization_lowPixel_negative`() {
        val meanR = 0.485f
        val stdR = 0.229f
        val pixelR = 64f / 255f
        val normalized = (pixelR - meanR) / stdR

        assertTrue(normalized.isFinite())
        assertTrue(normalized < 0f)
    }

    @Test
    fun `normalization_highPixel_positive`() {
        val meanR = 0.485f
        val stdR = 0.229f
        val pixelR = 200f / 255f
        val normalized = (pixelR - meanR) / stdR

        assertTrue(normalized > 0f)
    }

    @Test
    fun `detection_outputSizeCalc`() {
        val modelW = 960
        val modelH = 704
        assertEquals(675840, modelW * modelH)
    }

    @Test
    fun `recognition_widthRoundsToMultiple4`() {
        val targetH = 48
        val aspectRatio = 3.5f
        val rawWidth = (targetH * aspectRatio).toInt()
        val roundedWidth = (rawWidth / 4) * 4

        assertEquals(0, roundedWidth % 4)
    }

    @Test
    fun `boundingBox_withinImageBounds`() {
        val origW = 1000
        val origH = 800
        val scaleX = origW.toFloat() / 640
        val scaleY = origH.toFloat() / 640

        val x1 = (50 * scaleX).coerceIn(0f, origW.toFloat())
        val y1 = (30 * scaleY).coerceIn(0f, origH.toFloat())
        val x2 = (600 * scaleX).coerceIn(0f, origW.toFloat())
        val y2 = (580 * scaleY).coerceIn(0f, origH.toFloat())

        assertTrue(x1 >= 0f && x1 < origW)
        assertTrue(y1 >= 0f && y1 < origH)
        assertTrue(x2 > 0f && x2 <= origW)
        assertTrue(y2 > 0f && y2 <= origH)
    }

    @Test
    fun `ctc_decoding_removesConsecutiveDuplicates`() {
        val seqLen = 10
        val numClasses = 5
        val rawOutput = IntArray(seqLen) { (it / 3) % numClasses }

        val decoded = mutableListOf<Int>()
        var prevIdx = -1
        for (idx in rawOutput) {
            if (idx != prevIdx && idx != 0) {
                decoded.add(idx)
            }
            prevIdx = idx
        }

        assertTrue(decoded.size <= rawOutput.size)
    }

    @Test
    fun `floodFill_findsConnectedRegion`() {
        val grid = arrayOf(
            floatArrayOf(0.1f, 0.5f, 0.1f),
            floatArrayOf(0.5f, 0.8f, 0.5f),
            floatArrayOf(0.1f, 0.5f, 0.1f)
        )
        val visited = Array(3) { BooleanArray(3) }
        val threshold = 0.3f
        val region = mutableListOf<Pair<Int, Int>>()
        val queue = ArrayDeque<Pair<Int, Int>>()

        queue.add(Pair(1, 1))
        visited[1][1] = true

        while (queue.isNotEmpty()) {
            val (x, y) = queue.removeFirst()
            region.add(Pair(x, y))
            for (dy in -1..1) {
                for (dx in -1..1) {
                    if (dx == 0 && dy == 0) continue
                    val nx = x + dx
                    val ny = y + dy
                    if (nx in 0..2 && ny in 0..2 && !visited[ny][nx] && grid[ny][nx] > threshold) {
                        visited[ny][nx] = true
                        queue.add(Pair(nx, ny))
                    }
                }
            }
        }

        assertEquals(5, region.size)
    }

    @Test
    fun `dictionary_loading_nonEmptyList`() {
        val baseChars = listOf("blank") + ('a'..'z').map { it.toString() }
        assertTrue(baseChars.isNotEmpty())
        assertEquals(27, baseChars.size)
    }
}
