package com.rapidocr.app

import com.rapidocr.app.data.ocr.OcrConfig
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OcrConfigTest {
    @Test
    fun `OcrConfig has correct default values`() {
        val config = OcrConfig()
        assertEquals("PP-OCRv6_det_small.onnx", config.detectionModel)
        assertEquals("ch_ppocr_mobile_v2.0_cls_mobile.onnx", config.classificationModel)
        assertEquals("PP-OCRv6_rec_small.onnx", config.recognitionModel)
        assertEquals(4, config.threadNum)
        assertTrue(config.useCpu)
    }

    @Test
    fun `OcrConfig can be customized`() {
        val config = OcrConfig(
            detectionModel = "custom_det.onnx",
            threadNum = 2,
            useCpu = false
        )
        assertEquals("custom_det.onnx", config.detectionModel)
        assertEquals(2, config.threadNum)
        assertEquals(false, config.useCpu)
    }

    @Test
    fun `OcrConfig threadNum must be positive`() {
        val config = OcrConfig(threadNum = 1)
        assert(config.threadNum > 0)
    }
}
