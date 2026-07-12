package com.rapidocr.app.data.ocr

import android.content.Context
import android.graphics.Bitmap
import com.rapidocr.app.data.ocr.impl.OnnxOcrEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RapidOcrEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val onnxEngine = OnnxOcrEngine(context)

    fun initialize(modelDir: String, config: OcrConfig): Boolean {
        return onnxEngine.initialize(modelDir)
    }

    fun recognize(bitmap: Bitmap): com.rapidocr.app.domain.model.OcrResult? {
        return try {
            val result = onnxEngine.recognize(bitmap)
            val boxes = result.boxes.map { box ->
                com.rapidocr.app.domain.model.TextBox(
                    points = listOf(
                        android.graphics.PointF(box.points[0], box.points[1]),
                        android.graphics.PointF(box.points[2], box.points[3]),
                        android.graphics.PointF(box.points[4], box.points[5]),
                        android.graphics.PointF(box.points[6], box.points[7])
                    ),
                    text = box.text,
                    confidence = box.confidence
                )
            }
            com.rapidocr.app.domain.model.OcrResult(
                boxes = boxes,
                fullText = result.fullText,
                elapsedMs = result.elapsedMs
            )
        } catch (e: Exception) {
            null
        }
    }

    fun release() {
        onnxEngine.release()
    }
}
