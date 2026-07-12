package com.rapidocr.app.domain.usecase

import com.rapidocr.app.data.repository.HistoryRepository
import com.rapidocr.app.domain.model.HistoryRecord
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class HistoryUseCase @Inject constructor(
    private val historyRepository: HistoryRepository
) {
    fun getAllHistory(): Flow<List<HistoryRecord>> {
        return historyRepository.getAllHistory()
    }

    suspend fun insert(record: HistoryRecord) {
        historyRepository.insert(record)
    }

    suspend fun delete(id: Long) {
        historyRepository.delete(id)
    }

    suspend fun clearAll() {
        historyRepository.clearAll()
    }
}
