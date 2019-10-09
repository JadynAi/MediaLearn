/*
 * Copyright (c) 2018-present, lmyooyo@gmail.com.
 *
 * This source code is licensed under the GPL license found in the
 * LICENSE file in the root directory of this source tree.
 */
#include "com_jadyn_mediakit_gl_GLJni.h"

//Log
#ifdef ANDROID

#include <jni.h>
#include <GLES2/gl2.h>
#include <cpu-features.h>
#include <string.h>

#ifdef  __ARM__
#include <arm_neon.h>
#endif

#define LOGE(format, ...)  __android_log_print(ANDROID_LOG_ERROR, "JNI", format, ##__VA_ARGS__)
#define LOGI(format, ...)  __android_log_print(ANDROID_LOG_INFO,  "JNI", format, ##__VA_ARGS__)
#else
#define LOGE(format, ...)  printf("JNI" format "\n", ##__VA_ARGS__)
#define LOGI(format, ...)  printf("JNI" format "\n", ##__VA_ARGS__)
#endif

#ifdef  __ARM__
static void neon_memcpy(volatile unsigned char *dst, volatile unsigned char *src, int sz){
    if (sz & 63)
        sz = (sz & -64) + 64;
    asm volatile (
    "NEONCopyPLD: \n"
            " VLDM %[src]!,{d0-d7} \n"
            " VSTM %[dst]!,{d0-d7} \n"
            " SUBS %[sz],%[sz],#0x40 \n"
            " BGT NEONCopyPLD \n"
    : [dst]"+r"(dst), [src]"+r"(src), [sz]"+r"(sz) : : "d0", "d1", "d2", "d3", "d4", "d5", "d6", "d7", "cc", "memory");
}
#endif

JNIEXPORT void JNICALL Java_com_jadyn_mediakit_gl_GLJni_glReadPixels(JNIEnv *env, jobject thiz, jint x, jint y, jint width, jint height, jint format,
                                                                     jint type) {
    glReadPixels(x, y, width, height, format, type, 0);
}

