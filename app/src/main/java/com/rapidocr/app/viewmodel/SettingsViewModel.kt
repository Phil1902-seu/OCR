package com.rapidocr.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rapidocr.app.data.local.model.ModelManager
import com.rapidocr.app.domain.model.ModelType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val modelManager: ModelManager
) : ViewModel() {

    private val _currentModel = MutableStateFlow(modelManager.getCurrentModelType())
    val currentModel: StateFlow<ModelType> = _currentModel.asStateFlow()

    private val _switchResult = MutableStateFlow<Boolean?>(null)
    val switchResult: StateFlow<Boolean?> = _switchResult.asStateFlow()

    fun switchModel(type: ModelType) {
        viewModelScope.launch {
            val success = modelManager.switchModel(type)
            if (success) {
                _currentModel.value = type
            }
            _switchResult.value = success
        }
    }

    fun clearSwitchResult() {
        _switchResult.value = null
    }
}
