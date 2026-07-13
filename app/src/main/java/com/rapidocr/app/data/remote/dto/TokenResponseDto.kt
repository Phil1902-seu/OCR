package com.rapidocr.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TokenResponseDto(
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("expires_in") val expiresIn: Long?,
    @SerializedName("error") val error: String?,
    @SerializedName("error_description") val errorDescription: String?
)
