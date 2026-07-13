package com.rapidocr.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rapidocr.app.domain.model.OcrMode
import com.rapidocr.app.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val apiKey by viewModel.apiKey.collectAsState()
    val secretKey by viewModel.secretKey.collectAsState()
    val saveResult by viewModel.saveResult.collectAsState()
    val testResult by viewModel.testResult.collectAsState()
    val currentMode by viewModel.currentMode.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Text(
            text = "Baidu Cloud Credentials",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = apiKey,
            onValueChange = viewModel::updateApiKey,
            label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = secretKey,
            onValueChange = viewModel::updateSecretKey,
            label = { Text("Secret Key") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = { viewModel.saveCredentials() }) {
                Text("Save")
            }
            OutlinedButton(onClick = { viewModel.testConnection() }) {
                Text("Test Connection")
            }
        }

        when (saveResult) {
            true -> Text(
                "Credentials saved",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
            false -> Text(
                "Failed to save credentials",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
            null -> {}
        }

        when (val t = testResult) {
            is SettingsViewModel.TestState.Success -> Text(
                "Connection OK",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
            is SettingsViewModel.TestState.Failure -> Text(
                "Test failed: ${t.message}",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
            is SettingsViewModel.TestState.Testing -> Text(
                "Testing...",
                modifier = Modifier.padding(top = 8.dp)
            )
            else -> {}
        }

        Text(
            text = "Recognition Mode",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.setMode(OcrMode.STANDARD) },
                enabled = currentMode != OcrMode.STANDARD
            ) {
                Text("Standard")
            }
            OutlinedButton(
                onClick = { viewModel.setMode(OcrMode.HIGH_ACCURACY) },
                enabled = currentMode != OcrMode.HIGH_ACCURACY
            ) {
                Text("High Accuracy")
            }
            OutlinedButton(
                onClick = { viewModel.setMode(OcrMode.HIGH_ACCURACY_WITH_LOCATION) },
                enabled = currentMode != OcrMode.HIGH_ACCURACY_WITH_LOCATION
            ) {
                Text("With Location")
            }
        }
    }
}
