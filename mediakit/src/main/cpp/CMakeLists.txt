cmake_minimum_required(VERSION 3.4.1)

set(PATH_TO_AV_ENGINE ${PATH_TO_MEDIACORE}/src/main/cpp)
set(PATH_TO_THIRDPARTY ${PATH_TO_AV_ENGINE}/thirdparty)
set(PATH_TO_PRE_BUILT ${PATH_TO_THIRDPARTY}/prebuild/${ANDROID_ABI})

message(${PATH_TO_AV_ENGINE})

add_definitions("
-DGL-GLEXT-PROTOTYPES
-DEGL_EGLEXT_PROTOTYPETS
")

include_directories(${PATH_TO_AV_ENGINE}/)
include_directories(${PATH_TO_AV_ENGINE}/common/)
include_directories(${PATH_TO_AV_ENGINE}/video_player/)

file(GLOB FILES_LIB_COMMON "${PATH_TO_AV_ENGINE}/common/*.cpp")
file(GLOB FILES_LIB_VIDEO_PLAYER "${PATH_TO_AV_ENGINE}/video_player/*.cpp")

add_library(av-engine STATIC
        ${FILES_LIB_COMMON}
        ${FILES_LIB_VIDEO_PLAYER})

target_link_libraries(av-engine 
        log
        android
        GLESv2
        EGL
        z
        OpenSLES
        # 引入ffmpeg相关静态库
        ${PATH_TO_PRE_BUILT}/libavfilter.a
        ${PATH_TO_PRE_BUILT}/libavformat.a
        ${PATH_TO_PRE_BUILT}/libavcodec.a
        ${PATH_TO_PRE_BUILT}/libpostproc.a
        ${PATH_TO_PRE_BUILT}/libswresample.a
        ${PATH_TO_PRE_BUILT}/libswscale.a
        ${PATH_TO_PRE_BUILT}/libavutil.a
        ${PATH_TO_PRE_BUILT}/libpostproc.a
        ${PATH_TO_PRE_BUILT}/libfdk-aac.a
        ${PATH_TO_PRE_BUILT}/libvo-aacenc.a
        ${PATH_TO_PRE_BUILT}/libx264.a
        )