package com.rapidocr.app.data.local.model

import android.content.Context
import android.content.SharedPreferences
import com.rapidocr.app.domain.model.ModelType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: SharedPreferences
) {
    fun getCurrentModelType(): ModelType {
        val modelName = prefs.getString(KEY_MODEL_TYPE, ModelType.SMALL.name)
        return try {
            ModelType.valueOf(modelName ?: ModelType.SMALL.name)
        } catch (e: IllegalArgumentException) {
            ModelType.SMALL
        }
    }

    fun switchModel(type: ModelType): Boolean {
        return try {
            val modelDir = copyModelFromAssets(type)
            if (modelDir.isNotEmpty()) {
                prefs.edit().putString(KEY_MODEL_TYPE, type.name).apply()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun copyModelFromAssets(type: ModelType): String {
        val modelDir = File(context.filesDir, "models/${type.name.lowercase()}")
        val existingFiles = modelDir.listFiles()?.filter { it.length() > 100 } ?: emptyList()
        if (existingFiles.size >= 2) {
            return modelDir.absolutePath
        }

        modelDir.mkdirs()
        val modelFiles = getModelFilesForType(type)
        var successCount = 0

        for (modelFile in modelFiles) {
            val assetPath = "models/$modelFile"
            val outputFile = File(modelDir, modelFile)

            try {
                context.assets.open(assetPath).use { input ->
                    val bytes = input.readBytes()
                    if (bytes.size > 100) {
                        outputFile.outputStream().use { output ->
                            output.write(bytes)
                        }
                        successCount++
                    }
                }
            } catch (e: Exception) {
                if (outputFile.length() > 100) {
                    successCount++
                }
            }
        }

        return if (successCount >= 2) modelDir.absolutePath else ""
    }

    fun isModelReady(type: ModelType): Boolean {
        val modelDir = File(context.filesDir, "models/${type.name.lowercase()}")
        return modelDir.exists() && (modelDir.listFiles()?.count { it.length() > 100 } ?: 0) >= 2
    }

    private fun getModelFilesForType(type: ModelType): List<String> {
        return when (type) {
            ModelType.SMALL -> listOf(
                "PP-OCRv6_det_small.onnx",
                "ch_ppocr_mobile_v2.0_cls_mobile.onnx",
                "PP-OCRv6_rec_small.onnx"
            )
            ModelType.STANDARD -> listOf(
                "PP-OCRv6_det.onnx",
                "ch_ppocr_mobile_v2.0_cls.onnx",
                "PP-OCRv6_rec.onnx"
            )
        }
    }

    companion object {
        private const val KEY_MODEL_TYPE = "ocr_model_type"
    }
}
