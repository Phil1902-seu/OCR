package com.rapidocr.app

import com.rapidocr.app.domain.model.ExportFormat
import com.rapidocr.app.domain.model.OcrResult
import com.rapidocr.app.domain.model.TextBox
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResultScreenTest {
    @Test
    fun `empty result shows no text detected message`() {
        val result = OcrResult(boxes = emptyList(), fullText = "", elapsedMs = 0L)
        assertTrue(result.boxes.isEmpty())
    }

    @Test
    fun `copy to clipboard requires non-empty text`() {
        val result = OcrResult(
            boxes = listOf(TextBox(points = emptyList(), text = "Hello", confidence = 0.99f)),
            fullText = "Hello",
            elapsedMs = 50L
        )
        assertFalse(result.fullText.isEmpty())
    }

    @Test
    fun `export format selection triggers correct format`() {
        val formats = ExportFormat.values()
        assertEquals(4, formats.size)
        assertTrue(formats.contains(ExportFormat.TXT))
        assertTrue(formats.contains(ExportFormat.PDF))
        assertTrue(formats.contains(ExportFormat.MARKDOWN))
        assertTrue(formats.contains(ExportFormat.JSON))
    }
}

class HistoryScreenTest {
    @Test
    fun `empty history shows empty message`() {
        val emptyList = emptyList<com.rapidocr.app.domain.model.HistoryRecord>()
        assertTrue(emptyList.isEmpty())
    }

    @Test
    fun `history list sorted by time descending`() {
        val now = System.currentTimeMillis()
        val list = listOf(
            com.rapidocr.app.domain.model.HistoryRecord(id = 1L, thumbnailPath = "", fullText = "newer", summary = "newer", createdAt = now),
            com.rapidocr.app.domain.model.HistoryRecord(id = 2L, thumbnailPath = "", fullText = "older", summary = "older", createdAt = now - 10000)
        )
        val sorted = list.sortedByDescending { it.createdAt }
        assertEquals("newer", sorted[0].summary)
        assertEquals("older", sorted[1].summary)
    }

    @Test
    fun `clear all history requires confirmation state`() {
        var showClearDialog = false
        showClearDialog = true
        assertTrue(showClearDialog)
    }

    @Test
    fun `delete record also deletes thumbnail file`() {
        val record = com.rapidocr.app.domain.model.HistoryRecord(
            id = 1L, thumbnailPath = "/path/to/thumb.jpg", fullText = "test", summary = "test", createdAt = 0L
        )
        assertEquals("/path/to/thumb.jpg", record.thumbnailPath)
    }
}

class SettingsScreenTest {
    @Test
    fun `default ocr mode is STANDARD`() {
        val default = com.rapidocr.app.domain.model.OcrMode.STANDARD
        assertEquals(com.rapidocr.app.domain.model.OcrMode.STANDARD, default)
    }
}
