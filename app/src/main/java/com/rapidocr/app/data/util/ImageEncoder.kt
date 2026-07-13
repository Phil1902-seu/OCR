package com.rapidocr.app.data.util

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImageEncoder {
    fun encodeAndCompress(bitmap: Bitmap, maxSizeBytes: Int = 4 * 1024 * 1024): String {
        var quality = 100
        var outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        var bytes = outputStream.toByteArray()

        while (bytes.size > maxSizeBytes && quality > 10) {
            quality -= 10
            outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            bytes = outputStream.toByteArray()
        }
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
