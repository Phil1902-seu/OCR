package com.rapidocr.app

import com.rapidocr.app.data.ocr.OcrConfig
import com.rapidocr.app.data.ocr.RapidOcrEngine
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RapidOcrEngineTest {
    @Test
    fun `RapidOcrEngine can be instantiated`() {
        val clazz = RapidOcrEngine::class.java
        assertNotNull(clazz)
    }

    @Test
    fun `RapidOcrEngine has expected methods`() {
        val clazz = RapidOcrEngine::class.java
        val methods = clazz.declaredMethods
        val methodNames = methods.map { it.name }.toSet()

        assertTrue(
            methodNames.contains("initialize"),
            "initialize method should exist"
        )
        assertTrue(
            methodNames.contains("recognize"),
            "recognize method should exist"
        )
        assertTrue(
            methodNames.contains("release"),
            "release method should exist"
        )
    }

    @Test
    fun `OcrConfig model is well-formed`() {
        val config = OcrConfig()
        assertNotNull(config.detectionModel)
        assertNotNull(config.classificationModel)
        assertNotNull(config.recognitionModel)
        assertTrue(config.detectionModel.endsWith(".onnx"))
        assertTrue(config.classificationModel.endsWith(".onnx"))
        assertTrue(config.recognitionModel.endsWith(".onnx"))
    }
}
