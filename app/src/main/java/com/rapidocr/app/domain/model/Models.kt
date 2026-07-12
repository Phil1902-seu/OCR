package com.rapidocr.app.domain.model

import android.graphics.PointF

data class OcrResult(
    val boxes: List<TextBox>,
    val fullText: String,
    val elapsedMs: Long
)

data class TextBox(
    val points: List<PointF>,
    val text: String,
    val confidence: Float
)

data class HistoryRecord(
    val id: Long = 0,
    val thumbnailPath: String,
    val fullText: String,
    val summary: String,
    val createdAt: Long
)

data class ImageMeta(
    val width: Int,
    val height: Int,
    val format: String
)

enum class ModelType { SMALL, STANDARD }
enum class ExportFormat { TXT, PDF, MARKDOWN, JSON }
