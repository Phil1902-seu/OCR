package com.rapidocr.app.data.repository

import android.graphics.Bitmap
import com.rapidocr.app.data.ocr.OcrConfig
import com.rapidocr.app.data.ocr.RapidOcrEngine
import com.rapidocr.app.data.local.model.ModelManager
import com.rapidocr.app.domain.model.ModelType
import com.rapidocr.app.domain.model.OcrResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OcrRepository @Inject constructor(
    private val engine: RapidOcrEngine,
    private val modelManager: ModelManager
) {
    private var initialized = false

    suspend fun initialize(): Boolean {
        if (initialized) return true

        try {
            val modelType = modelManager.getCurrentModelType()
            val modelDir = modelManager.copyModelFromAssets(modelType)
            if (modelDir.isEmpty()) {
                lastError = "Model files could not be prepared. Please check storage."
                return false
            }

            val config = when (modelType) {
                ModelType.SMALL -> OcrConfig()
                ModelType.STANDARD -> OcrConfig(
                    detectionModel = "PP-OCRv6_det.onnx",
                    classificationModel = "ch_ppocr_mobile_v2.0_cls.onnx",
                    recognitionModel = "PP-OCRv6_rec.onnx"
                )
            }

            val initResult = withContext(Dispatchers.IO) {
                engine.initialize(modelDir, config)
            }
            if (initResult) {
                initialized = true
            } else {
                lastError = "ONNX model loading failed. Check: (1) model files integrity (2) available memory (3) ONNX opset compatibility"
            }
            return initResult
        } catch (e: Exception) {
            lastError = "Init error: ${e.javaClass.simpleName}: ${e.message}"
            return false
        }
    }

    fun getLastError(): String? = lastError

    private var lastError: String? = null

    suspend fun recognize(bitmap: Bitmap): Result<OcrResult> {
        if (!initialized) {
            initialize()
        }
        return withContext(Dispatchers.Default) {
            try {
                val result = engine.recognize(bitmap)
                if (result != null) {
                    Result.success(result)
                } else {
                    Result.failure(Exception("OCR recognition returned null"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun isEngineReady(): Boolean = initialized
}
