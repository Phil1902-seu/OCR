package com.rapidocr.app

import android.graphics.PointF
import com.rapidocr.app.domain.model.ExportFormat
import com.rapidocr.app.domain.model.HistoryRecord
import com.rapidocr.app.domain.model.ImageMeta
import com.rapidocr.app.domain.model.ModelType
import com.rapidocr.app.domain.model.OcrResult
import com.rapidocr.app.domain.model.TextBox
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DomainModelTest {
    @Test
    fun `OcrResult holds boxes, fullText and elapsedMs`() {
        val boxes = listOf(
            TextBox(
                points = listOf(PointF(0f, 0f), PointF(100f, 0f), PointF(100f, 50f), PointF(0f, 50f)),
                text = "Hello",
                confidence = 0.99f
            )
        )
        val result = OcrResult(boxes = boxes, fullText = "Hello", elapsedMs = 150L)
        assertEquals(1, result.boxes.size)
        assertEquals("Hello", result.fullText)
        assertEquals(150L, result.elapsedMs)
    }

    @Test
    fun `TextBox has four corner points`() {
        val points = listOf(PointF(0f, 0f), PointF(100f, 0f), PointF(100f, 50f), PointF(0f, 50f))
        val box = TextBox(points = points, text = "Test", confidence = 0.95f)
        assertEquals(4, box.points.size)
        assertEquals("Test", box.text)
        assertTrue(box.confidence in 0f..1f)
    }

    @Test
    fun `HistoryRecord summary truncated to 100 chars`() {
        val longText = "a".repeat(200)
        val record = HistoryRecord(
            id = 1L,
            thumbnailPath = "/path/to/thumb.jpg",
            fullText = longText,
            summary = longText.take(100),
            createdAt = System.currentTimeMillis()
        )
        assertEquals(100, record.summary.length)
    }

    @Test
    fun `ExportFormat has four values`() {
        val formats = ExportFormat.values()
        assertEquals(4, formats.size)
        assertTrue(formats.contains(ExportFormat.TXT))
        assertTrue(formats.contains(ExportFormat.PDF))
        assertTrue(formats.contains(ExportFormat.MARKDOWN))
        assertTrue(formats.contains(ExportFormat.JSON))
    }

    @Test
    fun `ModelType has SMALL and STANDARD`() {
        assertEquals(2, ModelType.values().size)
        assertNotNull(ModelType.SMALL)
        assertNotNull(ModelType.STANDARD)
    }

    @Test
    fun `ImageMeta holds width height format`() {
        val meta = ImageMeta(width = 1920, height = 1080, format = "JPEG")
        assertEquals(1920, meta.width)
        assertEquals(1080, meta.height)
        assertEquals("JPEG", meta.format)
    }
}
