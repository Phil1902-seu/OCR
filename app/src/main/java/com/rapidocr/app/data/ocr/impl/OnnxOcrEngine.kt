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
import kotlin.math.max
import kotlin.math.min

class OnnxOcrEngine(private val context: Context) {

    private var detSession: OrtSession? = null
    private var recSession: OrtSession? = null
    private var clsSession: OrtSession? = null
    private val ortEnv = OrtEnvironment.getEnvironment()
    private var dictionary: List<String> = emptyList()

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
            dictionary = loadDictionary()

            val detFile = copyAssetToCache("models/PP-OCRv6_det_small.onnx", modelDir)
            val recFile = copyAssetToCache("models/PP-OCRv6_rec_small.onnx", modelDir)
            val clsFile = copyAssetToCache("models/ch_ppocr_mobile_v2.0_cls_mobile.onnx", modelDir)

            val sessionOptions = OrtSession.SessionOptions().apply {
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                setIntraOpNumThreads(4)
            }

            detSession = if (detFile.exists()) ortEnv.createSession(detFile.absolutePath, sessionOptions) else null
            recSession = if (recFile.exists()) ortEnv.createSession(recFile.absolutePath, sessionOptions) else null
            clsSession = if (clsFile.exists()) ortEnv.createSession(clsFile.absolutePath, sessionOptions) else null

            detSession != null && recSession != null
        } catch (e: Exception) {
            false
        }
    }

    fun recognize(bitmap: Bitmap): OcrResult {
        val startTime = System.currentTimeMillis()

        val detBoxes = runDetection(bitmap)

        val sortedBoxes = detBoxes.sortedBy { box ->
            val yCenter = (box[1] + box[5]) / 2
            val xCenter = (box[0] + box[2]) / 2
            val rowThreshold = 30f
            val row = (yCenter / rowThreshold).toInt()
            row * 10000 + xCenter.toInt()
        }

        val results = sortedBoxes.mapNotNull { box ->
            val cropped = cropBitmap(bitmap, box)
            if (cropped != null) {
                val rotated = runClassification(cropped)
                val (text, conf) = runRecognition(rotated)
                if (text.isNotBlank()) {
                    TextBox(box, text, conf)
                } else {
                    null
                }
            } else {
                null
            }
        }

        val fullText = results.joinToString("\n") { it.text }
        val elapsed = System.currentTimeMillis() - startTime

        return OcrResult(results, fullText, elapsed)
    }

    private fun runDetection(bitmap: Bitmap): List<FloatArray> {
        val session = detSession ?: return getDefaultBox(bitmap)

        return try {
            val maxSide = 960
            val scale = if (bitmap.width > bitmap.height) {
                maxSide.toFloat() / bitmap.width
            } else {
                maxSide.toFloat() / bitmap.height
            }
            val targetW = (bitmap.width * scale).toInt().let { (it / 32) * 32 }
            val targetH = (bitmap.height * scale).toInt().let { (it / 32) * 32 }

            val (inputData, _) = ImagePreprocessor.bitmapToNormalized(bitmap, targetW, targetH)
            val chwData = inputData

            val shape = longArrayOf(1, 3, targetH.toLong(), targetW.toLong())
            val buffer = FloatBuffer.wrap(chwData)

            val inputTensor = OnnxTensor.createTensor(ortEnv, buffer, shape)

            val inputName = session.inputNames.first()
            val outputs = session.run(mapOf(inputName to inputTensor))
            val outputTensor = outputs[0] as? OnnxTensor ?: return getDefaultBox(bitmap)
            val outputBuffer = outputTensor.floatBuffer

            val outputArray = FloatArray(outputBuffer.remaining())
            outputBuffer.get(outputArray)

            inputTensor.close()
            outputTensor.close()
            outputs.close()

            postProcessDetection(outputArray, targetW, targetH, bitmap.width, bitmap.height)
        } catch (e: OrtException) {
            getDefaultBox(bitmap)
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
        val minSize = 3

        val segmentation = Array(modelH) { FloatArray(modelW) }
        for (y in 0 until modelH) {
            for (x in 0 until modelW) {
                segmentation[y][x] = data[y * modelW + x]
            }
        }

        val boxes = mutableListOf<FloatArray>()
        val scaleX = origW.toFloat() / modelW
        val scaleY = origH.toFloat() / modelH

        val visited = Array(modelH) { BooleanArray(modelW) }
        for (y in 0 until modelH) {
            for (x in 0 until modelW) {
                if (!visited[y][x] && segmentation[y][x] > threshold) {
                    val region = floodFill(segmentation, visited, x, y, modelW, modelH, threshold)
                    if (region.size >= minSize) {
                        val bbox = computeBoundingBox(region, scaleX, scaleY, origW, origH)
                        if (bbox != null) {
                            boxes.add(bbox)
                        }
                    }
                }
            }
        }

        if (boxes.isEmpty()) {
            return getDefaultBox(Bitmap.createBitmap(origW, origH, Bitmap.Config.ARGB_8888))
        }

        return boxes
    }

    private fun floodFill(
        segmentation: Array<FloatArray>,
        visited: Array<BooleanArray>,
        startX: Int,
        startY: Int,
        width: Int,
        height: Int,
        threshold: Float
    ): List<Pair<Int, Int>> {
        val region = mutableListOf<Pair<Int, Int>>()
        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.add(Pair(startX, startY))
        visited[startY][startX] = true

        while (queue.isNotEmpty()) {
            val (x, y) = queue.removeFirst()
            region.add(Pair(x, y))

            for (dy in -1..1) {
                for (dx in -1..1) {
                    if (dx == 0 && dy == 0) continue
                    val nx = x + dx
                    val ny = y + dy
                    if (nx in 0 until width && ny in 0 until height &&
                        !visited[ny][nx] && segmentation[ny][nx] > threshold
                    ) {
                        visited[ny][nx] = true
                        queue.add(Pair(nx, ny))
                    }
                }
            }
        }
        return region
    }

    private fun computeBoundingBox(
        region: List<Pair<Int, Int>>,
        scaleX: Float,
        scaleY: Float,
        origW: Int,
        origH: Int
    ): FloatArray? {
        if (region.isEmpty()) return null

        val minX = region.minOf { it.first }
        val maxX = region.maxOf { it.first }
        val minY = region.minOf { it.second }
        val maxY = region.maxOf { it.second }

        val x1 = (minX * scaleX).coerceIn(0f, origW.toFloat() - 1)
        val y1 = (minY * scaleY).coerceIn(0f, origH.toFloat() - 1)
        val x2 = ((maxX + 1) * scaleX).coerceIn(0f, origW.toFloat())
        val y2 = ((maxY + 1) * scaleY).coerceIn(0f, origH.toFloat())

        return floatArrayOf(x1, y1, x2, y1, x2, y2, x1, y2)
    }

    private fun getDefaultBox(bitmap: Bitmap): List<FloatArray> {
        val margin = 10f
        return listOf(floatArrayOf(
            margin, margin,
            (bitmap.width - margin), margin,
            (bitmap.width - margin), (bitmap.height - margin),
            margin, (bitmap.height - margin)
        ))
    }

    private fun runRecognition(cropped: Bitmap): Pair<String, Float> {
        val session = recSession ?: return Pair("", 0f)

        return try {
            val targetH = 48
            val aspectRatio = cropped.width.toFloat() / cropped.height
            val targetW = max(48, (targetH * aspectRatio).toInt()).let { (it / 4) * 4 }

            val (inputData, _) = ImagePreprocessor.bitmapToNormalized(cropped, targetW, targetH)
            val chwData = inputData

            val shape = longArrayOf(1, 3, targetH.toLong(), targetW.toLong())
            val buffer = FloatBuffer.wrap(chwData)
            val inputTensor = OnnxTensor.createTensor(ortEnv, buffer, shape)

            val inputName = session.inputNames.first()
            val outputs = session.run(mapOf(inputName to inputTensor))
            val outputTensor = outputs[0] as? OnnxTensor ?: return Pair("", 0f)

            val outputData = outputTensor.floatBuffer
            val outputArray = FloatArray(outputData.remaining())
            outputData.get(outputArray)

            inputTensor.close()
            outputTensor.close()
            outputs.close()

            decodeCTC(outputArray)
        } catch (e: OrtException) {
            Pair("", 0f)
        }
    }

    private fun runClassification(cropped: Bitmap): Bitmap {
        val session = clsSession ?: return cropped

        return try {
            val targetH = 48
            val targetW = 192
            val (inputData, _) = ImagePreprocessor.bitmapToBgr(cropped, targetW, targetH)
            val chwData = inputData

            val shape = longArrayOf(1, 3, targetH.toLong(), targetW.toLong())
            val buffer = FloatBuffer.wrap(chwData)
            val inputTensor = OnnxTensor.createTensor(ortEnv, buffer, shape)

            val inputName = session.inputNames.first()
            val outputs = session.run(mapOf(inputName to inputTensor))
            val outputTensor = outputs[0] as? OnnxTensor ?: return cropped

            val outputData = outputTensor.floatBuffer
            val outputArray = FloatArray(outputData.remaining())
            outputData.get(outputArray)

            inputTensor.close()
            outputTensor.close()
            outputs.close()

            val labelIdx = if (outputArray.size >= 2) {
                if (outputArray[0] > outputArray[1]) 0 else 1
            } else 0

            if (labelIdx == 1) {
                rotateBitmap(cropped)
            } else {
                cropped
            }
        } catch (e: OrtException) {
            cropped
        }
    }

    private fun rotateBitmap(bitmap: Bitmap): Bitmap {
        val matrix = android.graphics.Matrix().apply { postRotate(180f) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun decodeCTC(data: FloatArray): Pair<String, Float> {
        val dict = dictionary
        if (dict.isEmpty()) return Pair("", 0f)

        val numClasses = dict.size
        val seqLen = data.size / numClasses
        val text = StringBuilder()
        var totalConf = 0f
        var charCount = 0
        var prevIdx = 0

        for (t in 0 until seqLen) {
            var maxIdx = 0
            var maxVal = -Float.MAX_VALUE
            for (c in 0 until numClasses) {
                val value = data[t * numClasses + c]
                if (value > maxVal) {
                    maxVal = value
                    maxIdx = c
                }
            }

            if (maxIdx > 0 && maxIdx != prevIdx && maxIdx < numClasses) {
                text.append(dict[maxIdx])
                totalConf += maxVal
                charCount++
            }
            prevIdx = maxIdx
        }

        val avgConf = if (charCount > 0) totalConf / charCount else 0f
        return Pair(text.toString(), avgConf)
    }

    private fun loadDictionary(): List<String> {
        return try {
            context.assets.open("dict/ppocr_keys_v1.txt").bufferedReader().readLines()
        } catch (e: Exception) {
            listOf("blank") + ('a'..'z').map { it.toString() } +
                ('A'..'Z').map { it.toString() } + ('0'..'9').map { it.toString() }
        }
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
        if (destFile.exists() && destFile.length() > 100) return destFile

        destFile.parentFile?.mkdirs()
        try {
            context.assets.open(assetPath).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
        }
        return destFile
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
