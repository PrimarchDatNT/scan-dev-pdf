
#ifndef CAMERASCANNER_NATIVE_LIB_H
#define CAMERASCANNER_NATIVE_LIB_H

#include<jni.h>

using namespace std;

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jobject JNICALL
Java_com_document_camerascanner_features_enhance_NativeFilter_applyGrayFilter(JNIEnv *env, jobject thiz, jobject bitmap);

JNIEXPORT jobject JNICALL
Java_com_document_camerascanner_features_enhance_NativeFilter_applyNoShadowFilter(JNIEnv *env, jobject thiz, jobject bitmap);

JNIEXPORT jobject JNICALL
Java_com_document_camerascanner_features_enhance_NativeFilter_applyBnW1Filter(JNIEnv *env, jobject thiz, jobject bitmap);

JNIEXPORT jobject JNICALL
Java_com_document_camerascanner_features_enhance_NativeFilter_applyBnW2Filter(JNIEnv *env, jobject thiz, jobject bitmap);

JNIEXPORT jobject JNICALL
Java_com_document_camerascanner_features_enhance_NativeFilter_applyMagicColor(JNIEnv *env, jobject thiz, jobject bitmap);

#ifdef __cplusplus
}

#endif
#endif //CAMERASCANNER_NATIVE_LIB_H
