cmake_minimum_required(VERSION 3.22.1)
project("rapidocr_jni")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# JNI Bridge Library
add_library(
    rapidocr_jni
    SHARED
    RapidOcrJni.cpp
)

find_library(log-lib log)
find_library(android-lib android)
find_library(jnigraphics-lib jnigraphics)

target_link_libraries(
    rapidocr_jni
    ${log-lib}
    ${android-lib}
    ${jnigraphics-lib}
)
