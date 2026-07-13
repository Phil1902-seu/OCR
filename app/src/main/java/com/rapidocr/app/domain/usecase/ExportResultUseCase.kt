package com.rapidocr.app.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.io.image.ImageDataFactory
import com.rapidocr.app.domain.model.ExportFormat
import com.rapidocr.app.domain.model.ImageMeta
import com.rapidocr.app.domain.model.OcrResult
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import android.net.Uri

@Singleton
class ExportResultUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun exportToTxt(text: String, uri: Uri): Result<Unit> {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(text)
                }
            } ?: return Result.failure(Exception("Cannot open output stream"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportToPdf(text: String, image: Bitmap?, uri: Uri): Result<Unit> {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                val pdfWriter = PdfWriter(outputStream)
                val pdfDocument = PdfDocument(pdfWriter)
                val document = Document(pdfDocument)
                document.add(Paragraph("OCR Recognition Result"))
                document.add(Paragraph("Exported at: ${formatTimestamp(System.currentTimeMillis())}"))
                document.add(Paragraph(" "))
                if (image != null) {
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    image.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                    val imageData = ImageDataFactory.create(byteArrayOutputStream.toByteArray())
                    document.add(Image(imageData))
                    document.add(Paragraph(" "))
                }
                text.split("\n").forEach { line ->
                    document.add(Paragraph(line))
                }
                document.close()
            } ?: return Result.failure(Exception("Cannot open output stream"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportToMarkdown(result: OcrResult, imagePath: String?, uri: Uri): Result<Unit> {
        return try {
            val sb = StringBuilder()
            sb.appendLine("# OCR Recognition Result")
            sb.appendLine()
            sb.appendLine("> Exported at: ${formatTimestamp(System.currentTimeMillis())}")
            sb.appendLine("> Recognition engine: Baidu PaddleOCR")
            sb.appendLine("> Elapsed: ${result.elapsedMs}ms")
            sb.appendLine()
            sb.appendLine("## Original Image")
            sb.appendLine()
            if (imagePath != null) {
                sb.appendLine("![Original]($imagePath)")
            }
            sb.appendLine()
            sb.appendLine("## Recognition Results")
            sb.appendLine()
            sb.appendLine("| # | Text | Confidence |")
            sb.appendLine("|---|------|-----------|")
            result.boxes.forEachIndexed { index, box ->
                val confidencePct = String.format(Locale.US, "%.2f%%", box.confidence * 100)
                sb.appendLine("| ${index + 1} | ${box.text} | $confidencePct |")
            }
            sb.appendLine()
            sb.appendLine("## Plain Text")
            sb.appendLine()
            result.fullText.split("\n").forEach { line ->
                sb.appendLine(line)
            }
            sb.appendLine()
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(sb.toString())
                }
            } ?: return Result.failure(Exception("Cannot open output stream"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportToJson(result: OcrResult, imageMeta: ImageMeta?, uri: Uri): Result<Unit> {
        return try {
            val json = JSONObject()
            json.put("version", "1.0")
            json.put("exportedAt", formatTimestampIso(System.currentTimeMillis()))
            if (imageMeta != null) {
                val imageJson = JSONObject()
                imageJson.put("width", imageMeta.width)
                imageJson.put("height", imageMeta.height)
                imageJson.put("format", imageMeta.format)
                json.put("image", imageJson)
            }
            val ocrEngineJson = JSONObject()
            ocrEngineJson.put("name", "Baidu PaddleOCR")
            ocrEngineJson.put("model", "PaddleOCR online API")
            ocrEngineJson.put("language", "ch_en")
            ocrEngineJson.put("elapsedMs", result.elapsedMs)
            json.put("ocrEngine", ocrEngineJson)
            val resultsArray = JSONArray()
            result.boxes.forEachIndexed { index, box ->
                val boxJson = JSONObject()
                boxJson.put("id", index + 1)
                boxJson.put("text", box.text)
                boxJson.put("confidence", box.confidence.toDouble())
                val pointsArray = JSONArray()
                box.points.forEach { point ->
                    val pointArray = JSONArray()
                    pointArray.put(point.x.toDouble())
                    pointArray.put(point.y.toDouble())
                    pointsArray.put(pointArray)
                }
                val boxPointsJson = JSONObject()
                boxPointsJson.put("points", pointsArray)
                boxJson.put("box", boxPointsJson)
                resultsArray.put(boxJson)
            }
            json.put("results", resultsArray)
            json.put("fullText", result.fullText)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(json.toString(2))
                }
            } ?: return Result.failure(Exception("Cannot open output stream"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    }

    private fun formatTimestampIso(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date(timestamp))
    }
}
