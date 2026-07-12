package com.rapidocr.app.data.ocr

data class OcrConfig(
    val detectionModel: String = "PP-OCRv6_det_small.onnx",
    val classificationModel: String = "ch_ppocr_mobile_v2.0_cls_mobile.onnx",
    val recognitionModel: String = "PP-OCRv6_rec_small.onnx",
    val threadNum: Int = 4,
    val useCpu: Boolean = true
)
