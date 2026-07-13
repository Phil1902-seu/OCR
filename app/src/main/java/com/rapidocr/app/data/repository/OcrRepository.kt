package com.rapidocr.app.data.repository

import android.graphics.Bitmap
import com.rapidocr.app.data.remote.BaiduOcrService
import com.rapidocr.app.data.remote.CredentialManager
import com.rapidocr.app.domain.model.OcrMode
import com.rapidocr.app.domain.model.OcrResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OcrRepository @Inject constructor(
    private val ocrService: BaiduOcrService,
    private val credentialManager: CredentialManager
) {
    suspend fun recognize(bitmap: Bitmap, mode: OcrMode): Result<OcrResult> {
        if (!credentialManager.hasCredentials()) {
            return Result.failure(NoCredentialsException("请先配置百度智能云 API 凭证"))
        }
        return withContext(Dispatchers.IO) {
            ocrService.recognize(bitmap, mode)
        }
    }

    fun isReady(): Boolean = credentialManager.hasCredentials()
}

class NoCredentialsException(message: String) : Exception(message)
