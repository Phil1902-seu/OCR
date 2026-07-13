package com.rapidocr.app.data.remote

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "baidu_credential_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveCredentials(apiKey: String, secretKey: String): Boolean {
        return try {
            encryptedPrefs.edit()
                .putString(KEY_API_KEY, apiKey)
                .putString(KEY_SECRET_KEY, secretKey)
                .apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getApiKey(): String? = encryptedPrefs.getString(KEY_API_KEY, null)
    fun getSecretKey(): String? = encryptedPrefs.getString(KEY_SECRET_KEY, null)

    fun hasCredentials(): Boolean =
        !getApiKey().isNullOrBlank() && !getSecretKey().isNullOrBlank()

    fun clearCredentials() {
        encryptedPrefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_API_KEY = "baidu_api_key"
        private const val KEY_SECRET_KEY = "baidu_secret_key"
    }
}
