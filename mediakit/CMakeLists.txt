
cmake_minimum_required(VERSION 3.4.1)
project(mediakit)

# 设置变量名
set(PATH_NATIVE src/main/cpp)
set(PATH_THIRDTY ${PATH_NATIVE}/thirdparty)
# android_abi gradle配置的经过过滤的二进制ABI接口
set(PATH_PRE_BUILT ${PATH_THIRDTY}/prebuilt/${ANDROID_ABI})

# 将指定文件夹添加到build列表
add_subdirectory(${PATH_NATIVE}/gles)
add_subdirectory(${PATH_NATIVE})

#add_library(GLJni SHARED src/main/cpp/com_jadyn_mediakit_gl_GLJni.cpp)
#add_library(Player SHARED src/main/cpp/ALVideoPlayer.cpp)
include_directories(${PATH_NATIVE}/gles)
include_directories(${PATH_NATIVE})

