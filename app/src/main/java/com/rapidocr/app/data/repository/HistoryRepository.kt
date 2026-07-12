package com.rapidocr.app.data.repository

import com.rapidocr.app.data.local.database.HistoryDao
import com.rapidocr.app.data.local.entity.HistoryEntity
import com.rapidocr.app.domain.model.HistoryRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val historyDao: HistoryDao
) {
    fun getAllHistory(): Flow<List<HistoryRecord>> {
        return historyDao.getAllHistory().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    suspend fun insert(record: HistoryRecord) {
        historyDao.insert(record.toEntity())
    }

    suspend fun delete(id: Long) {
        val entity = historyDao.getAllHistory().let { flow ->
            var result: HistoryEntity? = null
            flow.collect { list ->
                result = list.find { it.id == id }
            }
            result
        }
        if (entity != null) {
            File(entity.thumbnailPath).delete()
        }
        historyDao.delete(id)
    }

    suspend fun clearAll() {
        historyDao.clearAll()
    }

    private fun HistoryEntity.toDomainModel() = HistoryRecord(
        id = id,
        thumbnailPath = thumbnailPath,
        fullText = fullText,
        summary = summary,
        createdAt = createdAt
    )

    private fun HistoryRecord.toEntity() = HistoryEntity(
        id = id,
        thumbnailPath = thumbnailPath,
        fullText = fullText,
        summary = summary,
        createdAt = createdAt
    )
}
