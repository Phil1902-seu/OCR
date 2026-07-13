package com.rapidocr.app.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import android.content.SharedPreferences
import com.rapidocr.app.data.remote.CredentialManager
import com.rapidocr.app.data.remote.TokenManager
import com.rapidocr.app.domain.usecase.HistoryUseCase
import com.rapidocr.app.domain.usecase.RecognizeTextUseCase
import com.rapidocr.app.viewmodel.HistoryViewModel
import com.rapidocr.app.viewmodel.OcrViewModel
import com.rapidocr.app.viewmodel.SettingsViewModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ViewModelFactory @Inject constructor(
    private val recognizeTextUseCase: RecognizeTextUseCase,
    private val historyUseCase: HistoryUseCase,
    private val credentialManager: CredentialManager,
    private val tokenManager: TokenManager,
    private val prefs: SharedPreferences
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(OcrViewModel::class.java) ->
                OcrViewModel(recognizeTextUseCase, historyUseCase, prefs) as T
            modelClass.isAssignableFrom(HistoryViewModel::class.java) ->
                HistoryViewModel(historyUseCase) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(credentialManager, tokenManager, prefs) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
