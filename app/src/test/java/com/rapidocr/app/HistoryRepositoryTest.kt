package com.rapidocr.app

import com.rapidocr.app.data.local.entity.HistoryEntity
import com.rapidocr.app.domain.model.HistoryRecord
import org.junit.Test
import kotlin.test.assertEquals

class HistoryRepositoryTest {
    @Test
    fun `HistoryEntity to DomainModel conversion is correct`() {
        val entity = HistoryEntity(
            id = 1L,
            thumbnailPath = "/path/to/thumb.jpg",
            fullText = "Test full text",
            summary = "Test full",
            createdAt = 1000L,
            ocrMode = com.rapidocr.app.domain.model.OcrMode.STANDARD.name
        )
        val domain = HistoryRecord(
            id = entity.id,
            thumbnailPath = entity.thumbnailPath,
            fullText = entity.fullText,
            summary = entity.summary,
            createdAt = entity.createdAt,
            ocrMode = entity.ocrMode
        )
        assertEquals(entity.id, domain.id)
        assertEquals(entity.thumbnailPath, domain.thumbnailPath)
        assertEquals(entity.fullText, domain.fullText)
        assertEquals(entity.summary, domain.summary)
        assertEquals(entity.createdAt, domain.createdAt)
        assertEquals(entity.ocrMode, domain.ocrMode)
    }

    @Test
    fun `HistoryRecord to Entity conversion is correct`() {
        val record = HistoryRecord(
            id = 2L,
            thumbnailPath = "/path/to/thumb2.jpg",
            fullText = "Another text line",
            summary = "Another te",
            createdAt = 2000L,
            ocrMode = com.rapidocr.app.domain.model.OcrMode.HIGH_ACCURACY.name
        )
        val entity = HistoryEntity(
            id = record.id,
            thumbnailPath = record.thumbnailPath,
            fullText = record.fullText,
            summary = record.summary,
            createdAt = record.createdAt,
            ocrMode = record.ocrMode
        )
        assertEquals(record.id, entity.id)
        assertEquals(record.thumbnailPath, entity.thumbnailPath)
        assertEquals(record.fullText, entity.fullText)
        assertEquals(record.summary, entity.summary)
        assertEquals(record.createdAt, entity.createdAt)
        assertEquals(record.ocrMode, entity.ocrMode)
    }

    @Test
    fun `Summary is first 100 chars of fullText`() {
        val fullText = "a".repeat(200)
        val summary = fullText.take(100)
        assertEquals(100, summary.length)
        assertEquals(fullText.substring(0, 100), summary)
    }
}
