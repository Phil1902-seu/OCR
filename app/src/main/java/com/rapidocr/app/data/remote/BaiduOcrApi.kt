package com.rapidocr.app.data.remote

import com.rapidocr.app.data.remote.dto.OcrResponseDto
import com.rapidocr.app.data.remote.dto.OcrResponseWithLocationDto
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Query

interface BaiduOcrApi {
    @FormUrlEncoded
    @POST("rest/2.0/ocr/v1/general_basic")
    suspend fun recognizeGeneralBasic(
        @Query("access_token") token: String,
        @Field("image") imageBase64: String,
        @Field("language_type") language: String = "CHN_ENG"
    ): OcrResponseDto

    @FormUrlEncoded
    @POST("rest/2.0/ocr/v1/accurate_basic")
    suspend fun recognizeAccurateBasic(
        @Query("access_token") token: String,
        @Field("image") imageBase64: String,
        @Field("language_type") language: String = "CHN_ENG"
    ): OcrResponseDto

    @FormUrlEncoded
    @POST("rest/2.0/ocr/v1/accurate")
    suspend fun recognizeAccurate(
        @Query("access_token") token: String,
        @Field("image") imageBase64: String,
        @Field("language_type") language: String = "CHN_ENG"
    ): OcrResponseWithLocationDto
}
