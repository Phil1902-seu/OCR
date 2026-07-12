package com.rapidocr.app

import android.app.Application
import android.util.Log
import com.rapidocr.app.data.repository.OcrRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class RapidOcrApplication : Application() {

    @Inject
    lateinit var ocrRepository: OcrRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        initOcrEngine()
    }

    private fun initOcrEngine() {
        applicationScope.launch {
            try {
                val success = ocrRepository.initialize()
                Log.d("RapidOCR", "OCR Engine initialized: $success")
            } catch (e: Exception) {
                Log.e("RapidOCR", "Failed to initialize OCR engine", e)
            }
        }
    }
}
