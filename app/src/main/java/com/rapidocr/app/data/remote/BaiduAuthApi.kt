package com.rapidocr.app.data.remote

import com.rapidocr.app.data.remote.dto.TokenResponseDto
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface BaiduAuthApi {
    @POST("oauth/2.0/token")
    @FormUrlEncoded
    suspend fun getAccessToken(
        @Field("grant_type") grantType: String = "client_credentials",
        @Field("client_id") apiKey: String,
        @Field("client_secret") secretKey: String
    ): TokenResponseDto
}
