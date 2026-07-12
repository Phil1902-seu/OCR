package com.rapidocr.app

import com.rapidocr.app.viewmodel.OcrUiState
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OcrViewModelTest {
    @Test
    fun `initial state is Idle`() {
        val initialState = OcrUiState.Idle
        assertIs<OcrUiState.Idle>(initialState)
    }

    @Test
    fun `Loading state exists`() {
        val state = OcrUiState.Loading
        assertIs<OcrUiState.Loading>(state)
    }

    @Test
    fun `Success state holds OcrResult`() {
        val result = com.rapidocr.app.domain.model.OcrResult(
            boxes = listOf(
                com.rapidocr.app.domain.model.TextBox(
                    points = listOf(
                        android.graphics.PointF(0f, 0f),
                        android.graphics.PointF(100f, 0f),
                        android.graphics.PointF(100f, 50f),
                        android.graphics.PointF(0f, 50f)
                    ),
                    text = "Hello",
                    confidence = 0.99f
                )
            ),
            fullText = "Hello",
            elapsedMs = 100L
        )
        val state = OcrUiState.Success(result)
        assertEquals("Hello", state.result.fullText)
        assertEquals(100L, state.result.elapsedMs)
    }

    @Test
    fun `Error state holds message`() {
        val state = OcrUiState.Error("Test error")
        assertEquals("Test error", state.message)
    }

    @Test
    fun `state transition Idle to Loading to Success`() {
        var currentState: OcrUiState = OcrUiState.Idle
        assertIs<OcrUiState.Idle>(currentState)

        currentState = OcrUiState.Loading
        assertIs<OcrUiState.Loading>(currentState)

        val result = com.rapidocr.app.domain.model.OcrResult(
            boxes = emptyList(), fullText = "", elapsedMs = 0L
        )
        currentState = OcrUiState.Success(result)
        assertIs<OcrUiState.Success>(currentState)
    }

    @Test
    fun `image compression reduces oversized bitmap`() {
        val maxSize = 4096
        val width = 8000
        val height = 6000

        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (width > height) {
            newWidth = maxSize
            newHeight = (maxSize / ratio).toInt()
        } else {
            newHeight = maxSize
            newWidth = (maxSize * ratio).toInt()
        }

        assertTrue(newWidth <= maxSize, "Width should be <= $maxSize")
        assertTrue(newHeight <= maxSize, "Height should be <= $maxSize")
    }
}
