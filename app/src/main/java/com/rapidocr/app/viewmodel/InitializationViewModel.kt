package com.rapidocr.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rapidocr.app.data.repository.OcrRepository
import com.rapidocr.app.ui.setup.InitState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InitializationViewModel @Inject constructor(
    private val ocrRepository: OcrRepository
) : ViewModel() {

    private val _initState = MutableStateFlow(InitState.CHECKING)
    val initState: StateFlow<InitState> = _initState.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun startInitialization() {
        if (_initState.value == InitState.READY) return

        viewModelScope.launch {
            try {
                _initState.value = InitState.CHECKING
                _progress.value = 0.1f

                _initState.value = InitState.COPYING
                _progress.value = 0.3f

                _initState.value = InitState.LOADING_MODEL
                _progress.value = 0.6f

                val success = ocrRepository.initialize()
                if (success) {
                    _progress.value = 1f
                    _initState.value = InitState.READY
                } else {
                    _initState.value = InitState.ERROR
                    _errorMessage.value = ocrRepository.getLastError() ?: "Unknown error"
                }
            } catch (e: Exception) {
                _initState.value = InitState.ERROR
                _errorMessage.value = "${e.javaClass.simpleName}: ${e.message}"
            }
        }
    }
}
