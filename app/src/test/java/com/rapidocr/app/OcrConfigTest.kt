package com.rapidocr.app

import com.rapidocr.app.domain.model.HistoryRecord
import com.rapidocr.app.domain.model.OcrMode
import org.junit.Test
import kotlin.test.assertEquals

class HistoryOcrModeTest {
    @Test
    fun `history record keeps HIGH_ACCURACY_WITH_LOCATION mode`() {
        val record = HistoryRecord(
            thumbnailPath = "", fullText = "loc", summary = "loc", createdAt = 1L,
            ocrMode = OcrMode.HIGH_ACCURACY_WITH_LOCATION.name
        )
        assertEquals(OcrMode.HIGH_ACCURACY_WITH_LOCATION.name, record.ocrMode)
    }

    @Test
    fun `default ocrMode is STANDARD when omitted`() {
        val record = HistoryRecord(thumbnailPath = "", fullText = "", summary = "", createdAt = 0L)
        assertEquals(OcrMode.STANDARD.name, record.ocrMode)
    }
}
