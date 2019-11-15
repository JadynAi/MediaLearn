
cmake_minimum_required(VERSION 3.4.1)

# 设置了cache选项，则必须带类型
set(PATH_TO_MEDIACORE CACHE STRING "")
# 设置变量名 
set(PATH_TO_NATIVE ${PATH_TO_MEDIACORE}/src/main/cpp)
set(PATH_THIRDTY ${PATH_TO_NATIVE}/thirdparty)
# android_abi gradle配置的经过过滤的二进制ABI接口
set(PATH_PRE_BUILT ${PATH_THIRDTY}/prebuilt/${ANDROID_ABI})

message(${PATH_TO_NATIVE})

# 构建一个子路径，这个子路径必须有CMakeLists.txt文件
add_subdirectory(src/main/cpp)


# 搜索.h文件？
include_directories(${PATH_TO_NATIVE})
include_directories(${PATH_TO_NATIVE}/gles)
include_directories(${PATH_TO_NATIVE}/video_player)
include_directories(${PATH_TO_NATIVE}/thirdparty)

