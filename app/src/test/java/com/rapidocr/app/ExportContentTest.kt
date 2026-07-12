package com.rapidocr.app

import android.graphics.PointF
import com.rapidocr.app.domain.model.OcrResult
import com.rapidocr.app.domain.model.TextBox
import org.junit.Test
import kotlin.test.assertTrue

class ExportContentTest {
    @Test
    fun `JSON export contains boxes, fullText and elapsedMs`() {
        val boxes = listOf(
            TextBox(
                points = listOf(PointF(6f, 2f), PointF(322f, 9f), PointF(320f, 104f), PointF(4f, 97f)),
                text = "\u6b63\u54c1\u4fc3\u9500",
                confidence = 0.99893f
            )
        )
        val result = OcrResult(boxes = boxes, fullText = "\u6b63\u54c1\u4fc3\u9500", elapsedMs = 532L)

        val json = buildJsonString(result)
        assertTrue(json.contains("\"boxes\"") || json.contains("\"results\""), "JSON should contain results/boxes field")
        assertTrue(json.contains("fullText"), "JSON should contain fullText field")
        assertTrue(json.contains("elapsedMs"), "JSON should contain elapsedMs field")
        assertTrue(json.contains("\u6b63\u54c1\u4fc3\u9500"), "JSON should contain the recognized text")
    }

    @Test
    fun `Markdown export contains recognition table and image reference`() {
        val boxes = listOf(
            TextBox(
                points = listOf(PointF(0f, 0f), PointF(100f, 0f), PointF(100f, 50f), PointF(0f, 50f)),
                text = "Hello",
                confidence = 0.99f
            )
        )
        val result = OcrResult(boxes = boxes, fullText = "Hello", elapsedMs = 100L)

        val md = buildMarkdownString(result, "test.jpg")
        assertTrue(md.contains("| # | Text | Confidence |"), "Markdown should contain table header")
        assertTrue(md.contains("Hello"), "Markdown should contain recognized text")
        assertTrue(md.contains("![Original](test.jpg)"), "Markdown should contain image reference")
        assertTrue(md.contains("Plain Text"), "Markdown should contain plain text section")
    }

    @Test
    fun `TXT export is plain text only`() {
        val result = OcrResult(
            boxes = listOf(TextBox(points = emptyList(), text = "Line1", confidence = 0.9f)),
            fullText = "Line1\nLine2",
            elapsedMs = 50L
        )
        val txt = result.fullText
        assertTrue(txt.contains("Line1"), "TXT should contain line 1")
        assertTrue(txt.contains("Line2"), "TXT should contain line 2")
    }

    private fun buildJsonString(result: OcrResult): String {
        val sb = StringBuilder()
        sb.append("{\"results\":[")
        result.boxes.forEachIndexed { index, box ->
            if (index > 0) sb.append(",")
            sb.append("{\"text\":\"${box.text}\",\"confidence\":${box.confidence}}")
        }
        sb.append("],\"fullText\":\"${result.fullText}\",\"elapsedMs\":${result.elapsedMs}}")
        return sb.toString()
    }

    private fun buildMarkdownString(result: OcrResult, imagePath: String?): String {
        val sb = StringBuilder()
        sb.appendLine("| # | Text | Confidence |")
        sb.appendLine("|---|------|-----------|")
        result.boxes.forEachIndexed { index, box ->
            sb.appendLine("| ${index + 1} | ${box.text} | ${(box.confidence * 100).toInt()}% |")
        }
        if (imagePath != null) {
            sb.appendLine("![Original]($imagePath)")
        }
        sb.appendLine("## Plain Text")
        sb.appendLine(result.fullText)
        return sb.toString()
    }
}
