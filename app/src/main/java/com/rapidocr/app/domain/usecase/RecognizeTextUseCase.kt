package com.rapidocr.app.domain.usecase

import android.graphics.Bitmap
import com.rapidocr.app.data.repository.OcrRepository
import com.rapidocr.app.domain.model.OcrMode
import com.rapidocr.app.domain.model.OcrResult
import javax.inject.Inject

class RecognizeTextUseCase @Inject constructor(
    private val ocrRepository: OcrRepository
) {
    suspend operator fun invoke(
        bitmap: Bitmap,
        mode: OcrMode = OcrMode.STANDARD
    ): Result<OcrResult> {
        return try {
            val result = ocrRepository.recognize(bitmap, mode)
            if (result.isSuccess) {
                Result.success(result.getOrThrow())
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("OCR recognition failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
