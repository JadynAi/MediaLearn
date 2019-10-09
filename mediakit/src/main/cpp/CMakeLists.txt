cmake_minimum_required(VERSION 3.4.1)

set(PATH_TO_AV_ENGINE src/main/cpp)
set(PATH_TO_PRE_BUILT ${PATH_TO_AV_ENGINE}/thirdparty/prebuild/${ANDROID_ABI})

add_definitions("
-DGL-GLEXT-PROTOTYPES
-DEGL_EGLEXT_PROTOTYPETS
")

include_directories(src/main/cpp)
include_directories(src/main/cpp/gles)

#
file(GLOB FILES_LIB_COMMON "${PATH_TO_AV_ENGINE}/common/*.cpp")
file(GLOB FILES_LIB_COMMON_EGL_CORE "${PATH_TO_AV_ENGINE}/common/egl_core/*.cpp")
file(GLOB FILES_LIB_COMMON_EGL_CORE "${PATH_TO_AV_ENGINE}/common/egl_core/*.cpp")
file(GLOB FILES_LIB_COMMON_RESAMPLER "${PATH_TO_AV_ENGINE}/common/libresampler/*.cpp")
file(GLOB FILES_LIB_COMMON_MSG_Q "${PATH_TO_AV_ENGINE}/common/message_queue/*.cpp")
file(GLOB FILES_LIB_COMMON_OPENGL_MEDIA "${PATH_TO_AV_ENGINE}/common/opengl_media/*.cpp")
file(GLOB FILES_LIB_COMMON_OPENGL_MEDIA_RENDER "${PATH_TO_AV_ENGINE}/common/opengl_media/render/*.cpp")
file(GLOB FILES_LIB_COMMON_OPENGL_MEDIA_TEXTURE "${PATH_TO_AV_ENGINE}/common/opengl_media/texture/*.cpp")
file(GLOB FILES_LIB_COMMON_OPENGL_MEDIA_TEX_COPIER "${PATH_TO_AV_ENGINE}/common/opengl_media/texture_copier/*.cpp")
file(GLOB FILES_LIB_COMMON_OPENSL "${PATH_TO_AV_ENGINE}/common/opensl_media/*.cpp")

file(GLOB FILES_LIB_VIDEO_PLAYER "${PATH_TO_AV_ENGINE}/video_player/*.cpp")
file(GLOB FILES_LIB_VIDEO_PLAYER_COMMON "${PATH_TO_AV_ENGINE}/video_player/common/*.cpp")
file(GLOB FILES_LIB_VIDEO_PLAYER_DECODER "${PATH_TO_AV_ENGINE}/video_player/decoder/*.cpp")
file(GLOB FILES_LIB_VIDEO_PLAYER_SYNC "${PATH_TO_AV_ENGINE}/video_player/sync/*.cpp")
file(GLOB FILES_LIB_VIDEO_PLAYER_TEX_UPLOADER "${PATH_TO_AV_ENGINE}/video_player/texture_uploader/*.cpp")

add_library(av-engine STATIC
        ${FILES_LIB_COMMON}
        ${FILES_LIB_COMMON_EGL_CORE}
        ${FILES_LIB_COMMON_MSG_Q}
        ${FILES_LIB_COMMON_RESAMPLER}
        ${FILES_LIB_COMMON_OPENSL}
        ${FILES_LIB_COMMON_OPENGL_MEDIA}
        ${FILES_LIB_COMMON_OPENGL_MEDIA_RENDER}
        ${FILES_LIB_COMMON_OPENGL_MEDIA_TEXTURE}
        ${FILES_LIB_COMMON_OPENGL_MEDIA_TEX_COPIER}
        ${FILES_LIB_VIDEO_PLAYER}
        ${FILES_LIB_VIDEO_PLAYER_COMMON}
        ${FILES_LIB_VIDEO_PLAYER_DECODER}
        ${FILES_LIB_VIDEO_PLAYER_SYNC}
        ${FILES_LIB_VIDEO_PLAYER_TEX_UPLOADER})

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