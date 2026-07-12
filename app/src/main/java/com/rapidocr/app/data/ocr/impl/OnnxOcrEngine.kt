package com.rapidocr.app.data.ocr.impl

import android.content.Context
import android.graphics.Bitmap
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtException
import ai.onnxruntime.OrtSession
import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer

class OnnxOcrEngine(private val context: Context) {

    private var detSession: OrtSession? = null
    private var recSession: OrtSession? = null
    private var clsSession: OrtSession? = null
    private val ortEnv = OrtEnvironment.getEnvironment()

    data class TextBox(
        val points: FloatArray,
        val text: String,
        val confidence: Float
    )

    data class OcrResult(
        val boxes: List<TextBox>,
        val fullText: String,
        val elapsedMs: Long
    )

    fun initialize(modelDir: String): Boolean {
        return try {
            val detFile = copyAssetToCache("models/PP-OCRv6_det_small.onnx", modelDir)
            val recFile = copyAssetToCache("models/PP-OCRv6_rec_small.onnx", modelDir)
            val clsFile = copyAssetToCache("models/ch_ppocr_mobile_v2.0_cls_mobile.onnx", modelDir)

            val sessionOptions = OrtSession.SessionOptions().apply {
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                setIntraOpNumThreads(4)
            }

            detSession = ortEnv.createSession(detFile.absolutePath, sessionOptions)
            recSession = ortEnv.createSession(recFile.absolutePath, sessionOptions)
            clsSession = ortEnv.createSession(clsFile.absolutePath, sessionOptions)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun recognize(bitmap: Bitmap): OcrResult {
        val startTime = System.currentTimeMillis()

        val (inputData, resized) = ImagePreprocessor.bitmapToBgrFloatArray(bitmap, 640, 640)
        val chwData = ImagePreprocessor.hwcToChw(inputData, 640, 640)

        val detBoxes = runDetection(chwData, 640, 640, bitmap.width, bitmap.height)

        val results = detBoxes.mapNotNull { box ->
            val cropped = cropBitmap(bitmap, box)
            if (cropped != null) {
                val text = runRecognition(cropped)
                TextBox(box, text.first, text.second)
            } else {
                null
            }
        }

        val fullText = results.joinToString("\n") { it.text }
        val elapsed = System.currentTimeMillis() - startTime

        return OcrResult(results, fullText, elapsed)
    }

    private fun runDetection(inputData: FloatArray, inputH: Int, inputW: Int, origW: Int, origH: Int): List<FloatArray> {
        val session = detSession ?: return emptyList()

        return try {
            val shape = longArrayOf(1, 3, inputH.toLong(), inputW.toLong())
            val buffer = FloatBuffer.wrap(inputData)
            val inputTensor = OnnxTensor.createTensor(ortEnv, buffer, shape)

            val outputs = session.run(mapOf("input" to inputTensor))
            val outputTensor = outputs[0] as? OnnxTensor ?: return emptyList()
            val outputData = outputTensor.floatBuffer

            val outputArray = FloatArray(outputData.remaining())
            outputData.get(outputArray)

            inputTensor.close()
            outputTensor.close()
            outputs.close()

            postProcessDetection(outputArray, inputW, inputH, origW, origH)
        } catch (e: OrtException) {
            emptyList()
        }
    }

    private fun postProcessDetection(
        data: FloatArray,
        modelW: Int,
        modelH: Int,
        origW: Int,
        origH: Int
    ): List<FloatArray> {
        val threshold = 0.3f
        val boxes = mutableListOf<FloatArray>()

        val scaleX = origW.toFloat() / modelW
        val scaleY = origH.toFloat() / modelH

        for (y in 0 until modelH) {
            for (x in 0 until modelW) {
                if (data[y * modelW + x] > threshold) {
                    val x1 = (x * scaleX).coerceIn(0f, origW.toFloat())
                    val y1 = (y * scaleY).coerceIn(0f, origH.toFloat())
                    val x2 = ((x + 20) * scaleX).coerceIn(0f, origW.toFloat())
                    val y2 = ((y + 10) * scaleY).coerceIn(0f, origH.toFloat())
                    boxes.add(floatArrayOf(x1, y1, x2, y1, x2, y2, x1, y2))
                }
            }
        }

        if (boxes.isEmpty()) {
            boxes.add(floatArrayOf(
                10f, 10f,
                (origW - 10).toFloat(), 10f,
                (origW - 10).toFloat(), (origH - 10).toFloat(),
                10f, (origH - 10).toFloat()
            ))
        }

        return boxes
    }

    private fun runRecognition(cropped: Bitmap): Pair<String, Float> {
        val session = recSession ?: return Pair("", 0f)

        return try {
            val targetH = 48
            val targetW = cropped.width * targetH / cropped.height
            val (inputData, _) = ImagePreprocessor.bitmapToBgrFloatArray(cropped, targetW, targetH)
            val chwData = ImagePreprocessor.hwcToChw(inputData, targetH, targetW)

            val shape = longArrayOf(1, 3, targetH.toLong(), targetW.toLong())
            val buffer = FloatBuffer.wrap(chwData)
            val inputTensor = OnnxTensor.createTensor(ortEnv, buffer, shape)

            val outputs = session.run(mapOf("input" to inputTensor))
            val outputTensor = outputs[0] as? OnnxTensor ?: return Pair("", 0f)
            val outputData = outputTensor.floatBuffer

            val outputArray = FloatArray(outputData.remaining())
            outputData.get(outputArray)

            inputTensor.close()
            outputTensor.close()
            outputs.close()

            decodeRecognitionOutput(outputArray)
        } catch (e: OrtException) {
            Pair("", 0f)
        }
    }

    private fun decodeRecognitionOutput(data: FloatArray): Pair<String, Float> {
        val dict = getDictionary()
        val text = StringBuilder()
        var maxConf = 0f
        var count = 0

        val seqLen = data.size / dict.size
        for (t in 0 until seqLen) {
            var maxIdx = 0
            var maxVal = -Float.MAX_VALUE
            for (c in dict.indices) {
                val val_ = data[t * dict.size + c]
                if (val_ > maxVal) {
                    maxVal = val_
                    maxIdx = c
                }
            }
            if (maxIdx > 0 && maxIdx < dict.size) {
                text.append(dict[maxIdx])
                maxConf += maxVal
                count++
            }
        }

        val conf = if (count > 0) maxConf / count else 0f
        return Pair(text.toString(), conf)
    }

    private fun cropBitmap(bitmap: Bitmap, box: FloatArray): Bitmap? {
        return try {
            val x1 = box[0].toInt().coerceIn(0, bitmap.width - 1)
            val y1 = box[1].toInt().coerceIn(0, bitmap.height - 1)
            val x2 = box[4].toInt().coerceIn(0, bitmap.width)
            val y2 = box[5].toInt().coerceIn(0, bitmap.height)

            val width = (x2 - x1).coerceAtLeast(1)
            val height = (y2 - y1).coerceAtLeast(1)

            Bitmap.createBitmap(bitmap, x1, y1, width, height)
        } catch (e: Exception) {
            null
        }
    }

    private fun copyAssetToCache(assetPath: String, destDir: String): File {
        val destFile = File(destDir, assetPath.substringAfterLast("/"))
        if (destFile.exists()) return destFile

        destFile.parentFile?.mkdirs()
        context.assets.open(assetPath).use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        return destFile
    }

    private fun getDictionary(): List<String> {
        val dict = mutableListOf<String>()
        dict.add("blank")
        for (c in 'a'..'z') dict.add(c.toString())
        for (c in 'A'..'Z') dict.add(c.toString())
        for (c in '0'..'9') dict.add(c.toString())
        dict.addAll(listOf(
            "一", "二", "三", "四", "五", "六", "七", "八", "九", "十",
            "的", "是", "在", "了", "和", "有", "这", "我", "他", "她",
            "你", "们", "来", "去", "好", "不", "大", "小", "中", "上",
            "下", "出", "入", "人", "口", "手", "日", "月", "水", "火",
            "山", "石", "田", "土", "天", "地", "时", "分", "秒", "年",
            "说", "看", "想", "做", "吃", "喝", "买", "卖", "开", "关"
        ))
        return dict
    }

    fun release() {
        detSession?.close()
        recSession?.close()
        clsSession?.close()
        detSession = null
        recSession = null
        clsSession = null
    }
}
