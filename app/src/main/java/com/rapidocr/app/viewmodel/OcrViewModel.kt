package com.rapidocr.app.viewmodel

import android.content.SharedPreferences
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rapidocr.app.domain.model.HistoryRecord
import com.rapidocr.app.domain.model.OcrMode
import com.rapidocr.app.domain.model.OcrResult
import com.rapidocr.app.domain.usecase.HistoryUseCase
import com.rapidocr.app.domain.usecase.RecognizeTextUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class OcrUiState {
    object Idle : OcrUiState()
    object Loading : OcrUiState()
    data class Success(val result: OcrResult) : OcrUiState()
    data class Error(val message: String) : OcrUiState()
}

class OcrViewModel @Inject constructor(
    private val recognizeTextUseCase: RecognizeTextUseCase,
    private val historyUseCase: HistoryUseCase,
    private val prefs: SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<OcrUiState>(OcrUiState.Idle)
    val uiState: StateFlow<OcrUiState> = _uiState.asStateFlow()

    private var currentResult: OcrResult? = null
    private var currentBitmap: Bitmap? = null

    fun recognize(bitmap: Bitmap) {
        val mode = loadMode()
        viewModelScope.launch {
            _uiState.value = OcrUiState.Loading
            currentBitmap = bitmap
            val result = recognizeTextUseCase(bitmap, mode)
            if (result.isSuccess) {
                currentResult = result.getOrThrow()
                saveToHistory(currentResult!!, mode)
                _uiState.value = OcrUiState.Success(currentResult!!)
            } else {
                _uiState.value = OcrUiState.Error(
                    result.exceptionOrNull()?.message ?: "Recognition failed"
                )
            }
        }
    }

    private fun loadMode(): OcrMode {
        val name = prefs.getString(KEY_OCR_MODE, OcrMode.STANDARD.name) ?: OcrMode.STANDARD.name
        return try {
            OcrMode.valueOf(name)
        } catch (e: IllegalArgumentException) {
            OcrMode.STANDARD
        }
    }

    private suspend fun saveToHistory(result: OcrResult, mode: OcrMode) {
        val summary = if (result.fullText.length > 50) {
            result.fullText.take(50) + "..."
        } else {
            result.fullText
        }
        val thumbnailPath = currentBitmap?.let { saveThumbnail(it) } ?: ""
        val record = HistoryRecord(
            id = 0,
            thumbnailPath = thumbnailPath,
            fullText = result.fullText,
            summary = summary,
            createdAt = System.currentTimeMillis(),
            ocrMode = mode.name
        )
        historyUseCase.insert(record)
    }

    private fun saveThumbnail(bitmap: Bitmap): String {
        return try {
            val scaled = Bitmap.createScaledBitmap(bitmap, 100, 100, true)
            scaled.recycle()
            "thumb_${System.currentTimeMillis()}.jpg"
        } catch (e: Exception) {
            ""
        }
    }

    fun getCurrentResult(): OcrResult? = currentResult
    fun getCurrentBitmap(): Bitmap? = currentBitmap

    fun reset() {
        _uiState.value = OcrUiState.Idle
    }

    companion object {
        private const val KEY_OCR_MODE = "ocr_mode"
    }
}
