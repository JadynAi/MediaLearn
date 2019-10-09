cmake_minimum_required(VERSION 3.4.1)

add_library(GLJni STATIC com_jadyn_mediakit_gl_GLJni.cpp)

include(AndroidNdkModules)
android_ndk_import_module_cpufeatures()

target_link_libraries(
        GLJni
        android
        cpufeatures
        GLESv2
)

