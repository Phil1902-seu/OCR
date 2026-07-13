package com.rapidocr.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rapidocr.app.di.ViewModelFactory
import com.rapidocr.app.ui.navigation.MainNavigation
import com.rapidocr.app.ui.setup.FirstLaunchScreen
import com.rapidocr.app.ui.theme.RapidOCRTheme
import com.rapidocr.app.viewmodel.HistoryViewModel
import com.rapidocr.app.viewmodel.OcrViewModel
import com.rapidocr.app.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RapidOCRTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RapidOCRApp(viewModelFactory)
                }
            }
        }
    }
}

@Composable
fun RapidOCRApp(factory: ViewModelFactory) {
    var isInitialized by remember { mutableStateOf(false) }

    val ocrViewModel: OcrViewModel = viewModel(factory = factory)
    val historyViewModel: HistoryViewModel = viewModel(factory = factory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)

    if (isInitialized) {
        MainNavigation(
            ocrViewModel = ocrViewModel,
            historyViewModel = historyViewModel,
            settingsViewModel = settingsViewModel
        )
    } else {
        FirstLaunchScreen(
            onInitialized = { isInitialized = true }
        )
    }
}
