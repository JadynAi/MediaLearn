//
// Created by JadynAi on 2019-09-29.
//
#include "./com_jadyn_mediakit_player_ALVideoController.h"

#include <android/native_window.h>
#include <android/native_window_jni.h>

#define LOG_TAG "Player_JNI_Layer"

extern "C" JNIEXPORT void JNICALLJava_com_jadyn_mediakit_player_ALVideoController_prepare(JNIEnv
                                                                                          * env,
                                                                                          jobject obj,
jstring
        name
){
LOGI("al video player prepare")

}

