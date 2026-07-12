package com.rapidocr.app.data.ocr

import android.content.Context
import android.graphics.Bitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RapidOcrEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var nativeHandle: Long = 0

    data class NativeResult(
        val elapsedMs: Long,
        val boxes: List<FloatArray>,
        val texts: List<String>,
        val scores: List<Float>
    )

    fun initialize(modelDir: String, config: OcrConfig): Boolean {
        nativeHandle = nativeInitialize(
            modelDir,
            config.detectionModel,
            config.classificationModel,
            config.recognitionModel,
            config.threadNum
        )
        return nativeHandle != 0L
    }

    fun recognize(bitmap: Bitmap): com.rapidocr.app.domain.model.OcrResult? {
        if (nativeHandle == 0L) return null

        val result = nativeRecognize(bitmap) ?: return null

        val boxes = result.boxes.mapIndexed { index, points ->
            val x1 = points.getOrNull(0) ?: 0f
            val y1 = points.getOrNull(1) ?: 0f
            val x2 = points.getOrNull(2) ?: 0f
            val y2 = points.getOrNull(3) ?: 0f
            val x3 = points.getOrNull(4) ?: 0f
            val y3 = points.getOrNull(5) ?: 0f
            val x4 = points.getOrNull(6) ?: 0f
            val y4 = points.getOrNull(7) ?: 0f
            com.rapidocr.app.domain.model.TextBox(
                points = listOf(
                    android.graphics.PointF(x1, y1),
                    android.graphics.PointF(x2, y2),
                    android.graphics.PointF(x3, y3),
                    android.graphics.PointF(x4, y4)
                ),
                text = result.texts.getOrElse(index) { "" },
                confidence = result.scores.getOrElse(index) { 0f }
            )
        }

        val fullText = result.texts.joinToString("\n")

        return com.rapidocr.app.domain.model.OcrResult(
            boxes = boxes,
            fullText = fullText,
            elapsedMs = result.elapsedMs
        )
    }

    fun release() {
        if (nativeHandle != 0L) {
            nativeRelease(nativeHandle)
            nativeHandle = 0L
        }
    }

    protected fun finalize() {
        release()
    }

    private external fun nativeInitialize(
        modelDir: String,
        detModel: String,
        clsModel: String,
        recModel: String,
        threadNum: Int
    ): Long

    private external fun nativeRecognize(bitmap: Bitmap): NativeResult?

    private external fun nativeRelease(handle: Long)

    companion object {
        init {
            System.loadLibrary("rapidocr_jni")
        }
    }
}
