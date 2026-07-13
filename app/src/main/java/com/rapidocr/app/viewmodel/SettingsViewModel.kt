package com.rapidocr.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.SharedPreferences
import com.rapidocr.app.data.remote.CredentialManager
import com.rapidocr.app.data.remote.TokenManager
import com.rapidocr.app.domain.model.OcrMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class SettingsViewModel @Inject constructor(
    private val credentialManager: CredentialManager,
    private val tokenManager: TokenManager,
    private val prefs: SharedPreferences
) : ViewModel() {

    private val _apiKey = MutableStateFlow(credentialManager.getApiKey() ?: "")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _secretKey = MutableStateFlow(credentialManager.getSecretKey() ?: "")
    val secretKey: StateFlow<String> = _secretKey.asStateFlow()

    private val _saveResult = MutableStateFlow<Boolean?>(null)
    val saveResult: StateFlow<Boolean?> = _saveResult.asStateFlow()

    private val _testResult = MutableStateFlow<TestState>(TestState.Idle)
    val testResult: StateFlow<TestState> = _testResult.asStateFlow()

    private val _currentMode = MutableStateFlow(loadMode())
    val currentMode: StateFlow<OcrMode> = _currentMode.asStateFlow()

    fun updateApiKey(value: String) {
        _apiKey.value = value
    }

    fun updateSecretKey(value: String) {
        _secretKey.value = value
    }

    fun saveCredentials() {
        viewModelScope.launch {
            val ok = credentialManager.saveCredentials(_apiKey.value.trim(), _secretKey.value.trim())
            if (ok) tokenManager.clearToken()
            _saveResult.value = ok
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _testResult.value = TestState.Testing
            val result = tokenManager.refreshToken()
            _testResult.value = if (result.isSuccess) {
                TestState.Success
            } else {
                TestState.Failure(result.exceptionOrNull()?.message ?: "连接失败")
            }
        }
    }

    fun setMode(mode: OcrMode) {
        _currentMode.value = mode
        prefs.edit().putString(KEY_OCR_MODE, mode.name).apply()
    }

    fun clearSaveResult() {
        _saveResult.value = null
    }

    private fun loadMode(): OcrMode {
        val name = prefs.getString(KEY_OCR_MODE, OcrMode.STANDARD.name) ?: OcrMode.STANDARD.name
        return try {
            OcrMode.valueOf(name)
        } catch (e: IllegalArgumentException) {
            OcrMode.STANDARD
        }
    }

    sealed class TestState {
        object Idle : TestState()
        object Testing : TestState()
        object Success : TestState()
        data class Failure(val message: String) : TestState()
    }

    companion object {
        private const val KEY_OCR_MODE = "ocr_mode"
    }
}
