package com.rapidocr.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OcrResponseDto(
    @SerializedName("words_result_num") val wordsResultNum: Int = 0,
    @SerializedName("words_result") val wordsResult: List<WordsItemDto>? = null,
    @SerializedName("error_code") val errorCode: Int? = null,
    @SerializedName("error_msg") val errorMessage: String? = null,
    @SerializedName("log_id") val logId: Long? = null
)

data class WordsItemDto(
    @SerializedName("words") val words: String
)
