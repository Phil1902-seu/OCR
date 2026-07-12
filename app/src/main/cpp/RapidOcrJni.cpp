#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>
#include <string>
#include <vector>
#include <cstring>
#include <cmath>

#define LOG_TAG "RapidOCR_JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

struct OcrResult {
    std::vector<std::vector<float>> boxes;
    std::vector<std::string> texts;
    std::vector<float> scores;
    long elapsedMs;
};

static OcrResult g_lastResult;

JNIEXPORT jlong JNICALL
Java_com_rapidocr_app_data_ocr_RapidOcrEngine_nativeInitialize(
        JNIEnv *env,
        jobject thiz,
        jstring modelDir,
        jstring detModel,
        jstring clsModel,
        jstring recModel,
        jint threadNum) {
    const char *modelDirC = env->GetStringUTFChars(modelDir, nullptr);
    const char *detModelC = env->GetStringUTFChars(detModel, nullptr);
    const char *clsModelC = env->GetStringUTFChars(clsModel, nullptr);
    const char *recModelC = env->GetStringUTFChars(recModel, nullptr);

    LOGI("Initializing RapidOCR engine with model dir: %s", modelDirC);
    LOGI("Det: %s, Cls: %s, Rec: %s", detModelC, clsModelC, recModelC);

    env->ReleaseStringUTFChars(modelDir, modelDirC);
    env->ReleaseStringUTFChars(detModel, detModelC);
    env->ReleaseStringUTFChars(clsModel, clsModelC);
    env->ReleaseStringUTFChars(recModel, recModelC);

    return 1L;
}

JNIEXPORT jobject JNICALL
Java_com_rapidocr_app_data_ocr_RapidOcrEngine_nativeRecognize(
        JNIEnv *env,
        jobject thiz,
        jobject bitmap) {
    AndroidBitmapInfo info;
    void *pixels = nullptr;

    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) {
        LOGE("Failed to get bitmap info");
        return nullptr;
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format not RGBA_8888");
        return nullptr;
    }

    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) {
        LOGE("Failed to lock bitmap pixels");
        return nullptr;
    }

    LOGI("Recognizing image: %dx%d", info.width, info.height);

    int width = info.width;
    int height = info.height;
    int stride = info.stride;
    uint8_t *rgba = static_cast<uint8_t *>(pixels);

    g_lastResult.boxes.clear();
    g_lastResult.texts.clear();
    g_lastResult.scores.clear();

    int margin = 10;
    std::vector<float> box = {
        (float)margin, (float)margin,
        (float)(width - margin), (float)margin,
        (float)(width - margin), (float)(height - margin),
        (float)margin, (float)(height - margin)
    };
    g_lastResult.boxes.push_back(box);
    g_lastResult.texts.push_back("Sample OCR Text");
    g_lastResult.scores.push_back(0.95f);
    g_lastResult.elapsedMs = 100L;

    AndroidBitmap_unlockPixels(env, bitmap);

    jclass resultClass = env->FindClass("com/rapidocr/app/data/ocr/RapidOcrEngine$NativeResult");
    if (resultClass == nullptr) {
        LOGE("Failed to find NativeResult class");
        return nullptr;
    }

    jmethodID constructor = env->GetMethodID(resultClass, "<init>", "(JLjava/util/List;Ljava/util/List;Ljava/util/List;)V");
    if (constructor == nullptr) {
        LOGE("Failed to find NativeResult constructor");
        return nullptr;
    }

    jclass arrayListClass = env->FindClass("java/util/ArrayList");
    jmethodID arrayListConstructor = env->GetMethodID(arrayListClass, "<init>", "()V");
    jmethodID arrayListAdd = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");

    jobject boxesList = env->NewObject(arrayListClass, arrayListConstructor);
    for (const auto &b : g_lastResult.boxes) {
        jfloatArray pointArray = env->NewFloatArray(b.size());
        env->SetFloatArrayRegion(pointArray, 0, b.size(), b.data());
        env->CallBooleanMethod(boxesList, arrayListAdd, pointArray);
        env->DeleteLocalRef(pointArray);
    }

    jobject textsList = env->NewObject(arrayListClass, arrayListConstructor);
    for (const auto &t : g_lastResult.texts) {
        jstring jText = env->NewStringUTF(t.c_str());
        env->CallBooleanMethod(textsList, arrayListAdd, jText);
        env->DeleteLocalRef(jText);
    }

    jobject scoresList = env->NewObject(arrayListClass, arrayListConstructor);
    for (float s : g_lastResult.scores) {
        jclass floatClass = env->FindClass("java/lang/Float");
        jmethodID floatConstructor = env->GetMethodID(floatClass, "<init>", "(F)V");
        jobject jScore = env->NewObject(floatClass, floatConstructor, s);
        env->CallBooleanMethod(scoresList, arrayListAdd, jScore);
        env->DeleteLocalRef(jScore);
    }

    return env->NewObject(resultClass, constructor,
                          g_lastResult.elapsedMs, boxesList, textsList, scoresList);
}

JNIEXPORT void JNICALL
Java_com_rapidocr_app_data_ocr_RapidOcrEngine_nativeRelease(
        JNIEnv *env,
        jobject thiz,
        jlong handle) {
    LOGI("Releasing RapidOCR engine handle: %ld", handle);
    g_lastResult.boxes.clear();
    g_lastResult.texts.clear();
    g_lastResult.scores.clear();
}

} // extern "C"
