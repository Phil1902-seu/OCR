package com.rapidocr.app.data.ocr.impl

import android.graphics.Bitmap
import java.nio.ByteBuffer
import java.nio.ByteOrder

object ImagePreprocessor {
    private const val MEAN_R = 0.485f
    private const val MEAN_G = 0.456f
    private const val MEAN_B = 0.406f
    private const val STD_R = 0.229f
    private const val STD_G = 0.224f
    private const val STD_B = 0.225f

    fun bitmapToFloatArray(
        bitmap: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        normalize: Boolean = true
    ): Pair<FloatArray, Bitmap> {
        val resized = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        val width = resized.width
        val height = resized.height
        val pixels = IntArray(width * height)
        resized.getPixels(pixels, 0, width, 0, 0, width, height)

        val floatArray = FloatArray(3 * width * height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = ((pixel shr 16) and 0xFF).toFloat() / 255f
            val g = ((pixel shr 8) and 0xFF).toFloat() / 255f
            val b = (pixel and 0xFF).toFloat() / 255f

            val idx = i * 3
            if (normalize) {
                floatArray[idx] = (r - MEAN_R) / STD_R
                floatArray[idx + 1] = (g - MEAN_G) / STD_G
                floatArray[idx + 2] = (b - MEAN_B) / STD_B
            } else {
                floatArray[idx] = r
                floatArray[idx + 1] = g
                floatArray[idx + 2] = b
            }
        }

        return Pair(floatArray, resized)
    }

    fun bitmapToBgrFloatArray(
        bitmap: Bitmap,
        targetWidth: Int,
        targetHeight: Int
    ): Pair<FloatArray, Bitmap> {
        val resized = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        val width = resized.width
        val height = resized.height
        val pixels = IntArray(width * height)
        resized.getPixels(pixels, 0, width, 0, 0, width, height)

        val floatArray = FloatArray(3 * height * width)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = pixels[y * width + x]
                val r = ((pixel shr 16) and 0xFF).toFloat()
                val g = ((pixel shr 8) and 0xFF).toFloat()
                val b = (pixel and 0xFF).toFloat()

                val idx = (y * width + x) * 3
                floatArray[idx] = (b - 127.5f) / 127.5f
                floatArray[idx + 1] = (g - 127.5f) / 127.5f
                floatArray[idx + 2] = (r - 127.5f) / 127.5f
            }
        }

        return Pair(floatArray, resized)
    }

    fun hwcToChw(hwc: FloatArray, height: Int, width: Int): FloatArray {
        val chw = FloatArray(hwc.size)
        val channelSize = height * width
        for (c in 0 until 3) {
            for (h in 0 until height) {
                for (w in 0 until width) {
                    chw[c * channelSize + h * width + w] = hwc[(h * width + w) * 3 + c]
                }
            }
        }
        return chw
    }
}
