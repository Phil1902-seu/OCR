package com.rapidocr.app

import com.rapidocr.app.domain.model.OcrMode
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OcrModeTest {
    @Test
    fun `OcrMode has three recognition modes`() {
        assertEquals(3, OcrMode.values().size)
        assertTrue(OcrMode.values().contains(OcrMode.STANDARD))
        assertTrue(OcrMode.values().contains(OcrMode.HIGH_ACCURACY))
        assertTrue(OcrMode.values().contains(OcrMode.HIGH_ACCURACY_WITH_LOCATION))
    }

    @Test
    fun `OcrMode names match api contract`() {
        assertEquals("STANDARD", OcrMode.STANDARD.name)
        assertEquals("HIGH_ACCURACY", OcrMode.HIGH_ACCURACY.name)
        assertEquals("HIGH_ACCURACY_WITH_LOCATION", OcrMode.HIGH_ACCURACY_WITH_LOCATION.name)
    }

    @Test
    fun `OcrMode can be parsed from string`() {
        assertEquals(OcrMode.STANDARD, OcrMode.valueOf("STANDARD"))
        assertEquals(OcrMode.HIGH_ACCURACY, OcrMode.valueOf("HIGH_ACCURACY"))
        assertEquals(OcrMode.HIGH_ACCURACY_WITH_LOCATION, OcrMode.valueOf("HIGH_ACCURACY_WITH_LOCATION"))
    }
}
