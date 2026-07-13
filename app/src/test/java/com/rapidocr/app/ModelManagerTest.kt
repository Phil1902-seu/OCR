package com.rapidocr.app

import com.rapidocr.app.domain.model.ExportFormat
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExportFormatTest {
    @Test
    fun `ExportFormat has four values`() {
        val formats = ExportFormat.values()
        assertEquals(4, formats.size)
        assertTrue(formats.contains(ExportFormat.TXT))
        assertTrue(formats.contains(ExportFormat.PDF))
        assertTrue(formats.contains(ExportFormat.MARKDOWN))
        assertTrue(formats.contains(ExportFormat.JSON))
    }
}
