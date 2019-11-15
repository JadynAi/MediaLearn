//
// Created by JadynAi on 2019-09-29.
//
#include "./com_jadyn_mediakit_player_ALVideoPlayer.h"
#include "./common/CommonTools.h"

#include <android/native_window.h>
#include <android/native_window_jni.h>

#define LOG_TAG "Player_JNI_Layer"

extern "C"

JNIEXPORT void JNICALL Java_com_jadyn_mediakit_player_ALVideoPlayer_prepare
        (JNIEnv *env, jobject obj, jstring url, jobject size, jobject surface) {
    LOGI("JNI VideoPlayer prepare");
    JavaVM *g_jvm = NULL;
    env->GetJavaVM(&g_jvm);
    
}