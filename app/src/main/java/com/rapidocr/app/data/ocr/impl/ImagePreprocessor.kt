package com.rapidocr.app.data.ocr.impl

import android.graphics.Bitmap

object ImagePreprocessor {
    private const val MEAN_R = 0.485f
    private const val MEAN_G = 0.456f
    private const val MEAN_B = 0.406f
    private const val STD_R = 0.229f
    private const val STD_G = 0.224f
    private const val STD_B = 0.225f

    fun bitmapToNormalized(
        targetWidth: Int,
        targetHeight: Int
    ): Pair<FloatArray, Bitmap> {
        val dummyBitmap = createDummyBitmap(targetWidth, targetHeight)
        return bitmapToNormalized(dummyBitmap, targetWidth, targetHeight)
    }

    fun bitmapToNormalized(
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
                val r = ((pixel shr 16) and 0xFF).toFloat() / 255f
                val g = ((pixel shr 8) and 0xFF).toFloat() / 255f
                val b = (pixel and 0xFF).toFloat() / 255f

                val chwIdxR = 0 * height * width + y * width + x
                val chwIdxG = 1 * height * width + y * width + x
                val chwIdxB = 2 * height * width + y * width + x

                floatArray[chwIdxR] = (r - MEAN_R) / STD_R
                floatArray[chwIdxG] = (g - MEAN_G) / STD_G
                floatArray[chwIdxB] = (b - MEAN_B) / STD_B
            }
        }

        return Pair(floatArray, resized)
    }

    fun bitmapToBgr(
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
                val r = ((pixel shr 16) and 0xFF).toFloat() / 255f
                val g = ((pixel shr 8) and 0xFF).toFloat() / 255f
                val b = (pixel and 0xFF).toFloat() / 255f

                val chwIdxR = 0 * height * width + y * width + x
                val chwIdxG = 1 * height * width + y * width + x
                val chwIdxB = 2 * height * width + y * width + x

                floatArray[chwIdxR] = (r - MEAN_R) / STD_R
                floatArray[chwIdxG] = (g - MEAN_G) / STD_G
                floatArray[chwIdxB] = (b - MEAN_B) / STD_B
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

    private fun createDummyBitmap(width: Int, height: Int): Bitmap {
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.WHITE)
        }
    }
}
