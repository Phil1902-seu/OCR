package com.rapidocr.app

import com.rapidocr.app.domain.model.HistoryRecord
import com.rapidocr.app.domain.model.OcrMode
import com.rapidocr.app.domain.model.OcrResult
import com.rapidocr.app.domain.model.TextBox
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OcrResultModelTest {
    @Test
    fun `OcrResult holds boxes, fullText and elapsedMs`() {
        val result = OcrResult(boxes = emptyList(), fullText = "Hello", elapsedMs = 150L)
        assertEquals("Hello", result.fullText)
        assertEquals(150L, result.elapsedMs)
        assertTrue(result.boxes.isEmpty())
    }

    @Test
    fun `TextBox confidence stays within range`() {
        val box = TextBox(points = emptyList(), text = "Test", confidence = 0.95f)
        assertEquals("Test", box.text)
        assertTrue(box.confidence in 0f..1f)
    }

    @Test
    fun `HistoryRecord defaults ocrMode to STANDARD`() {
        val record = HistoryRecord(
            thumbnailPath = "", fullText = "x", summary = "x", createdAt = 0L
        )
        assertEquals(OcrMode.STANDARD.name, record.ocrMode)
    }

    @Test
    fun `HistoryRecord preserves explicit ocrMode`() {
        val record = HistoryRecord(
            thumbnailPath = "", fullText = "x", summary = "x", createdAt = 0L,
            ocrMode = OcrMode.HIGH_ACCURACY.name
        )
        assertEquals(OcrMode.HIGH_ACCURACY.name, record.ocrMode)
    }
}
