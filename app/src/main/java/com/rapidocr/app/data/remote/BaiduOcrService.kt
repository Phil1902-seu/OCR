package com.rapidocr.app.data.remote

import android.graphics.Bitmap
import android.graphics.PointF
import com.rapidocr.app.data.remote.dto.OcrResponseDto
import com.rapidocr.app.data.remote.dto.OcrResponseWithLocationDto
import com.rapidocr.app.data.util.ImageEncoder
import com.rapidocr.app.domain.model.OcrMode
import com.rapidocr.app.domain.model.OcrResult
import com.rapidocr.app.domain.model.TextBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

class OcrApiException(val code: Int, message: String) : Exception(message)

@Singleton
class BaiduOcrService @Inject constructor(
    private val ocrApi: BaiduOcrApi,
    private val tokenManager: TokenManager
) {
    suspend fun recognize(bitmap: Bitmap, mode: OcrMode): Result<OcrResult> {
        return withContext(Dispatchers.IO) {
            try {
                val imageBase64 = ImageEncoder.encodeAndCompress(bitmap)
                val tokenResult = tokenManager.getValidToken()
                if (tokenResult.isFailure) {
                    return@withContext Result.failure(
                        tokenResult.exceptionOrNull() ?: Exception("Token unavailable")
                    )
                }
                val token = tokenResult.getOrThrow()

                val ocrResult: OcrResult = when (mode) {
                    OcrMode.STANDARD -> {
                        val resp = callWithRetry { ocrApi.recognizeGeneralBasic(token, imageBase64) }
                        if (resp.errorCode != null && resp.errorCode != 0) {
                            return@withContext Result.failure(
                                OcrApiException(resp.errorCode, resp.errorMessage ?: "OCR API error")
                            )
                        }
                        mapBasic(resp, 0L)
                    }
                    OcrMode.HIGH_ACCURACY -> {
                        val resp = callWithRetry { ocrApi.recognizeAccurateBasic(token, imageBase64) }
                        if (resp.errorCode != null && resp.errorCode != 0) {
                            return@withContext Result.failure(
                                OcrApiException(resp.errorCode, resp.errorMessage ?: "OCR API error")
                            )
                        }
                        mapBasic(resp, 0L)
                    }
                    OcrMode.HIGH_ACCURACY_WITH_LOCATION -> {
                        val resp = callWithRetry { ocrApi.recognizeAccurate(token, imageBase64) }
                        if (resp.errorCode != null && resp.errorCode != 0) {
                            return@withContext Result.failure(
                                OcrApiException(resp.errorCode, resp.errorMessage ?: "OCR API error")
                            )
                        }
                        mapWithLocation(resp, 0L)
                    }
                }
                Result.success(ocrResult)
            } catch (e: OcrApiException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun <T> callWithRetry(call: suspend () -> T): T {
        return try {
            call()
        } catch (e: OcrApiException) {
            when (e.code) {
                110, 111 -> {
                    tokenManager.clearToken()
                    val refreshed = tokenManager.refreshToken()
                    if (refreshed.isSuccess) call() else throw e
                }
                18 -> {
                    delay(500)
                    try {
                        call()
                    } catch (inner: OcrApiException) {
                        throw inner
                    }
                }
                else -> throw e
            }
        }
    }

    private fun mapBasic(response: OcrResponseDto, elapsedMs: Long): OcrResult {
        val words = response.wordsResult?.map { it.words } ?: emptyList()
        return OcrResult(
            boxes = emptyList(),
            fullText = words.joinToString("\n"),
            elapsedMs = elapsedMs
        )
    }

    private fun mapWithLocation(response: OcrResponseWithLocationDto, elapsedMs: Long): OcrResult {
        val boxes = response.wordsResult?.map { item ->
            val loc = item.location
            TextBox(
                points = if (loc != null) listOf(
                    PointF(loc.left.toFloat(), loc.top.toFloat()),
                    PointF((loc.left + loc.width).toFloat(), loc.top.toFloat()),
                    PointF((loc.left + loc.width).toFloat(), (loc.top + loc.height).toFloat()),
                    PointF(loc.left.toFloat(), (loc.top + loc.height).toFloat())
                ) else emptyList(),
                text = item.words,
                confidence = 1.0f
            )
        } ?: emptyList()
        return OcrResult(
            boxes = boxes,
            fullText = boxes.joinToString("\n") { it.text },
            elapsedMs = elapsedMs
        )
    }
}
