package com.rapidocr.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rapidocr.app.domain.model.HistoryRecord
import com.rapidocr.app.domain.usecase.HistoryUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val historyUseCase: HistoryUseCase
) : ViewModel() {

    val historyList: StateFlow<List<HistoryRecord>> = historyUseCase.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteRecord(id: Long) {
        viewModelScope.launch {
            historyUseCase.delete(id)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            historyUseCase.clearAll()
        }
    }
}
