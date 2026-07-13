package com.rapidocr.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rapidocr.app.data.remote.CredentialManager
import com.rapidocr.app.ui.setup.InitState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InitializationViewModel @Inject constructor(
    private val credentialManager: CredentialManager
) : ViewModel() {

    private val _initState = MutableStateFlow(InitState.CHECKING)
    val initState: StateFlow<InitState> = _initState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun startInitialization() {
        if (_initState.value == InitState.READY) return

        viewModelScope.launch {
            _initState.value = InitState.CHECKING
            if (credentialManager.hasCredentials()) {
                _initState.value = InitState.READY
            } else {
                _initState.value = InitState.NO_CREDENTIAL
                _errorMessage.value = "请先配置百度智能云 API 凭证"
            }
        }
    }
}
