package com.rapidocr.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OcrResponseWithLocationDto(
    @SerializedName("words_result_num") val wordsResultNum: Int = 0,
    @SerializedName("words_result") val wordsResult: List<WordsLocationItemDto>? = null,
    @SerializedName("error_code") val errorCode: Int? = null,
    @SerializedName("error_msg") val errorMessage: String? = null,
    @SerializedName("log_id") val logId: Long? = null
)

data class WordsLocationItemDto(
    @SerializedName("words") val words: String,
    @SerializedName("location") val location: LocationDto?
)

data class LocationDto(
    @SerializedName("top") val top: Int,
    @SerializedName("left") val left: Int,
    @SerializedName("width") val width: Int,
    @SerializedName("height") val height: Int
)
