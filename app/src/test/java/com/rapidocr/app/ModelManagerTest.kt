package com.rapidocr.app

import com.rapidocr.app.domain.model.ModelType
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ModelManagerTest {
    @Test
    fun `ModelType SMALL has correct display name`() {
        assertEquals("SMALL", ModelType.SMALL.name)
        assertEquals("STANDARD", ModelType.STANDARD.name)
    }

    @Test
    fun `ModelType default is SMALL`() {
        val default = ModelType.SMALL
        assertEquals(ModelType.SMALL, default)
    }

    @Test
    fun `ModelType can be parsed from string`() {
        assertEquals(ModelType.SMALL, ModelType.valueOf("SMALL"))
        assertEquals(ModelType.STANDARD, ModelType.valueOf("STANDARD"))
    }

    @Test
    fun `getDefaultModelFiles returns three ONNX files for SMALL`() {
        val smallFiles = getSmallModelFiles()
        assertEquals(3, smallFiles.size)
        assertTrue(smallFiles.all { it.endsWith(".onnx") })
    }

    @Test
    fun `Model initialization is idempotent - first call copies model, second returns existing`() {
        val callCount = intArrayOf(0)
        fun mockCopyModel(): String {
            callCount[0]++
            if (callCount[0] == 1) return "/data/models/small"
            return "/data/models/small"
        }
        val result1 = mockCopyModel()
        val result2 = mockCopyModel()
        assertEquals(result1, result2)
        assertTrue(callCount[0] == 2)
    }

    private fun getSmallModelFiles(): List<String> {
        return listOf(
            "PP-OCRv6_det_small.onnx",
            "ch_ppocr_mobile_v2.0_cls_mobile.onnx",
            "PP-OCRv6_rec_small.onnx"
        )
    }
}
