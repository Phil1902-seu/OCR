package com.rapidocr.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.rapidocr.app.domain.model.OcrMode

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val thumbnailPath: String,
    val fullText: String,
    val summary: String,
    val createdAt: Long,
    val ocrMode: String = OcrMode.STANDARD.name
)
