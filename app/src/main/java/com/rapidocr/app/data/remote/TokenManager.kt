package com.rapidocr.app.data.remote

import android.content.SharedPreferences
import com.rapidocr.app.data.remote.dto.TokenResponseDto
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    private val authApi: BaiduAuthApi,
    private val credentialManager: CredentialManager,
    private val prefs: SharedPreferences
) {
    private val tokenLock = Mutex()

    suspend fun getValidToken(): Result<String> {
        return tokenLock.withLock {
            if (isTokenValid()) {
                val token = prefs.getString(KEY_ACCESS_TOKEN, null)
                if (!token.isNullOrBlank()) {
                    return@withLock Result.success(token)
                }
            }
            refreshToken()
        }
    }

    suspend fun refreshToken(): Result<String> {
        val apiKey = credentialManager.getApiKey()
        val secretKey = credentialManager.getSecretKey()
        if (apiKey.isNullOrBlank() || secretKey.isNullOrBlank()) {
            return Result.failure(SecurityException("API credentials not configured"))
        }
        return try {
            val response: TokenResponseDto = authApi.getAccessToken(
                grantType = "client_credentials",
                apiKey = apiKey,
                secretKey = secretKey
            )
            if (!response.accessToken.isNullOrBlank()) {
                val expiresIn = response.expiresIn ?: DEFAULT_EXPIRE_SECONDS
                val expireAt = System.currentTimeMillis() + expiresIn * 1000
                prefs.edit()
                    .putString(KEY_ACCESS_TOKEN, response.accessToken)
                    .putLong(KEY_TOKEN_EXPIRE_AT, expireAt)
                    .apply()
                Result.success(response.accessToken)
            } else {
                Result.failure(
                    Exception("Get token failed: ${response.error ?: response.errorDescription}")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isTokenValid(): Boolean {
        val expireAt = prefs.getLong(KEY_TOKEN_EXPIRE_AT, 0L)
        if (expireAt == 0L) return false
        return System.currentTimeMillis() < (expireAt - TOKEN_REFRESH_BUFFER_MS)
    }

    fun clearToken() {
        prefs.edit().remove(KEY_ACCESS_TOKEN).remove(KEY_TOKEN_EXPIRE_AT).apply()
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "baidu_access_token"
        private const val KEY_TOKEN_EXPIRE_AT = "baidu_token_expire_at"
        private const val TOKEN_REFRESH_BUFFER_MS = 5 * 60 * 1000L
        private const val DEFAULT_EXPIRE_SECONDS = 2592000L
    }
}
