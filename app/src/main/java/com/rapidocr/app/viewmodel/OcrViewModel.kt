package com.rapidocr.app.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rapidocr.app.domain.model.OcrResult
import com.rapidocr.app.domain.usecase.RecognizeTextUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class OcrUiState {
    object Idle : OcrUiState()
    object Loading : OcrUiState()
    data class Success(val result: OcrResult) : OcrUiState()
    data class Error(val message: String) : OcrUiState()
}

class OcrViewModel(
    private val recognizeTextUseCase: RecognizeTextUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<OcrUiState>(OcrUiState.Idle)
    val uiState: StateFlow<OcrUiState> = _uiState.asStateFlow()

    private var currentResult: OcrResult? = null
    private var currentBitmap: Bitmap? = null

    fun recognize(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.value = OcrUiState.Loading
            currentBitmap = bitmap
            val result = recognizeTextUseCase(bitmap)
            if (result.isSuccess) {
                currentResult = result.getOrThrow()
                _uiState.value = OcrUiState.Success(currentResult!!)
            } else {
                _uiState.value = OcrUiState.Error(
                    result.exceptionOrNull()?.message ?: "Recognition failed"
                )
            }
        }
    }

    fun getCurrentResult(): OcrResult? = currentResult
    fun getCurrentBitmap(): Bitmap? = currentBitmap

    fun reset() {
        _uiState.value = OcrUiState.Idle
    }
}
