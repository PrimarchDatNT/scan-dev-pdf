#include "native-lib.h"
#include <jni.h>
#include <string>
#include <vector>
#include <android/bitmap.h>
#include <opencv2/opencv.hpp>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc.hpp>

using namespace std;
using namespace cv;

jobject mat_to_bitmap(JNIEnv *env, Mat &src, bool needPremultiplyAlpha, jobject bitmap_config) {
    auto java_bitmap_class = (jclass) env->FindClass("android/graphics/Bitmap");
    jmethodID mid = env->GetStaticMethodID(java_bitmap_class, "createBitmap",
                                           "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");

    jobject bitmap = env->CallStaticObjectMethod(java_bitmap_class,
                                                 mid, src.size().width, src.size().height, bitmap_config);
    AndroidBitmapInfo info;
    void *pixels = nullptr;

    try {
        CV_Assert(AndroidBitmap_getInfo(env, bitmap, &info) >= 0);
        CV_Assert(src.type() == CV_8UC1 || src.type() == CV_8UC3 || src.type() == CV_MAKETYPE(CV_8U, 4));
        CV_Assert(AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0);
        CV_Assert(pixels);
        if (info.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
            Mat tmp(info.height, info.width, CV_MAKETYPE(CV_8U, 4), pixels);
            if (src.type() == CV_8UC1) {
                cvtColor(src, tmp, CV_GRAY2RGBA);
            } else if (src.type() == CV_8UC3) {
                cvtColor(src, tmp, CV_RGB2RGBA);
            } else if (src.type() == CV_MAKETYPE(CV_8U, 4)) {
                if (needPremultiplyAlpha) {
                    cvtColor(src, tmp, COLOR_RGBA2mRGBA);
                } else {
                    src.copyTo(tmp);
                }
            }
        } else {
            // info.format == ANDROID_BITMAP_FORMAT_RGB_565
            Mat tmp(info.height, info.width, CV_8UC2, pixels);
            if (src.type() == CV_8UC1) {
                cvtColor(src, tmp, CV_GRAY2BGR565);
            } else if (src.type() == CV_8UC3) {
                cvtColor(src, tmp, CV_RGB2BGR565);
            } else if (src.type() == CV_MAKETYPE(CV_8U, 4)) {
                cvtColor(src, tmp, CV_RGBA2BGR565);
            }
        }
        AndroidBitmap_unlockPixels(env, bitmap);
        return bitmap;
    } catch (cv::Exception e) {
        AndroidBitmap_unlockPixels(env, bitmap);
        jclass je = env->FindClass("org/opencv/core/CvException");
        if (!je) je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, e.what());
        return bitmap;
    } catch (...) {
        AndroidBitmap_unlockPixels(env, bitmap);
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "Unknown exception in JNI code {nMatToBitmap}");
        return bitmap;
    }
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_document_camerascanner_features_enhance_NativeFilter_applyGrayFilter(JNIEnv *env, jobject thiz,
                                                                              jobject bitmap) {
    AndroidBitmapInfo info;
    void *pixels = nullptr;

    if (0 > AndroidBitmap_getInfo(env, bitmap, &info)) {
        return nullptr;
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        return nullptr;
    }
    AndroidBitmap_lockPixels(env, bitmap, &pixels);

    Mat src(info.height, info.width, CV_MAKETYPE(CV_8U, 4), pixels);
    // init our output image

    cvtColor(src, src, CV_BGR2GRAY);

    //get source bitmap's config
    auto java_bitmap_class = (jclass) env->FindClass("android/graphics/Bitmap");
    jmethodID mid = env->GetMethodID(java_bitmap_class, "getConfig", "()Landroid/graphics/Bitmap$Config;");
    jobject bitmap_config = env->CallObjectMethod(bitmap, mid);
    jobject _bitmap = mat_to_bitmap(env, src, false, bitmap_config);

    AndroidBitmap_unlockPixels(env, bitmap);
    return _bitmap;
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_document_camerascanner_features_enhance_NativeFilter_applyMagicColor(JNIEnv *env, jobject thiz,
                                                                              jobject bitmap) {
    AndroidBitmapInfo info;
    void *pixels = nullptr;

    if (0 > AndroidBitmap_getInfo(env, bitmap, &info)) {
        return nullptr;
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        return nullptr;
    }
    AndroidBitmap_lockPixels(env, bitmap, &pixels);

    Mat src(info.height, info.width, CV_MAKETYPE(CV_8U, 4), pixels);
    // init our output image

    src.convertTo(src, src.depth(), 1, 15);

    int constrast = 50;
    double f = (double) 131 * (constrast + 127) / (127 * (131 - constrast));
    double gamma_c = 127 * (1 - f);
    addWeighted(src, f, src, 0, gamma_c, src);

    //get source bitmap's config
    auto java_bitmap_class = (jclass) env->FindClass("android/graphics/Bitmap");
    jmethodID mid = env->GetMethodID(java_bitmap_class, "getConfig", "()Landroid/graphics/Bitmap$Config;");
    jobject bitmap_config = env->CallObjectMethod(bitmap, mid);
    jobject _bitmap = mat_to_bitmap(env, src, false, bitmap_config);

    AndroidBitmap_unlockPixels(env, bitmap);
    return _bitmap;
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_document_camerascanner_features_enhance_NativeFilter_applyNoShadowFilter(JNIEnv *env, jobject thiz,
                                                                                  jobject bitmap) {
    AndroidBitmapInfo info;
    void *pixels = nullptr;

    if (0 > AndroidBitmap_getInfo(env, bitmap, &info)) {
        return nullptr;
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        return nullptr;
    }
    AndroidBitmap_lockPixels(env, bitmap, &pixels);

    Mat src(info.height, info.width, CV_MAKETYPE(CV_8U, 4), pixels);
    // init our output image
    Mat dst;

    int kernelSize = 5;
    Mat kernel = getStructuringElement(CV_SHAPE_RECT, Size(2 * kernelSize + 1, 2 * kernelSize + 1),
                                       Point(kernelSize, kernelSize));
    Mat bgImg;

    medianBlur(src, bgImg, 21);
    dilate(bgImg, bgImg, kernel);
    absdiff(src, bgImg, dst);
    kernel.release();
    bgImg.release();

    bitwise_not(dst, dst);

    dst.convertTo(dst, dst.depth(), 1, 25);

    Mat imgR(dst.size(), CV_8UC1);
    Mat imgG(dst.size(), CV_8UC1);
    Mat imgB(dst.size(), CV_8UC1);
    Vec4b pixel;

    for (int r = 0; r < dst.rows; r++) {
        for (int c = 0; c < dst.cols; c++) {
            pixel = dst.at<Vec4b>(r, c);
            imgB.at<uchar>(r, c) = pixel[0];
            imgG.at<uchar>(r, c) = pixel[1];
            imgR.at<uchar>(r, c) = pixel[2];
        }
    }

    equalizeHist(imgB, imgB);
    equalizeHist(imgG, imgG);
    equalizeHist(imgR, imgR);

    for (int r = 0; r < dst.rows; r++) {
        for (int c = 0; c < dst.cols; c++) {
            pixel = Vec4b(imgB.at<uchar>(r, c), imgG.at<uchar>(r, c), imgR.at<uchar>(r, c), dst.at<Vec4b>(r, c)[3]);
            dst.at<Vec4b>(r, c) = pixel;
        }
    }
    imgR.release();
    imgG.release();
    imgB.release();

    auto java_bitmap_class = (jclass) env->FindClass("android/graphics/Bitmap");
    jmethodID mid = env->GetMethodID(java_bitmap_class, "getConfig", "()Landroid/graphics/Bitmap$Config;");
    jobject bitmap_config = env->CallObjectMethod(bitmap, mid);
    jobject _bitmap = mat_to_bitmap(env, dst, false, bitmap_config);

    AndroidBitmap_unlockPixels(env, bitmap);
    return _bitmap;
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_document_camerascanner_features_enhance_NativeFilter_applyBnW1Filter(JNIEnv *env, jobject thiz,
                                                                              jobject bitmap) {
    AndroidBitmapInfo info;
    void *pixels = nullptr;

    if (0 > AndroidBitmap_getInfo(env, bitmap, &info)) {
        return nullptr;
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        return nullptr;
    }

    AndroidBitmap_lockPixels(env, bitmap, &pixels);

    Mat src(info.height, info.width, CV_MAKETYPE(CV_8U, 4), pixels);

    cvtColor(src, src, CV_BGR2GRAY);
    threshold(src, src, 0, 255, THRESH_BINARY | THRESH_OTSU);

    auto java_bitmap_class = (jclass) env->FindClass("android/graphics/Bitmap");
    jmethodID mid = env->GetMethodID(java_bitmap_class, "getConfig", "()Landroid/graphics/Bitmap$Config;");
    jobject bitmap_config = env->CallObjectMethod(bitmap, mid);
    jobject _bitmap = mat_to_bitmap(env, src, false, bitmap_config);

    AndroidBitmap_unlockPixels(env, bitmap);
    return _bitmap;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_document_camerascanner_features_enhance_NativeFilter_applyBnW2Filter(JNIEnv *env, jobject thiz,
                                                                              jobject bitmap) {
    AndroidBitmapInfo info;
    void *pixels = nullptr;

    if (0 > AndroidBitmap_getInfo(env, bitmap, &info)) {
        return nullptr;
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        return nullptr;
    }

    AndroidBitmap_lockPixels(env, bitmap, &pixels);

    Mat src(info.height, info.width, CV_MAKETYPE(CV_8U, 4), pixels);

    cvtColor(src, src, CV_BGR2GRAY);
    adaptiveThreshold(src, src, 255, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 15, 8);

    auto java_bitmap_class = (jclass) env->FindClass("android/graphics/Bitmap");
    jmethodID mid = env->GetMethodID(java_bitmap_class, "getConfig", "()Landroid/graphics/Bitmap$Config;");
    jobject bitmap_config = env->CallObjectMethod(bitmap, mid);
    jobject _bitmap = mat_to_bitmap(env, src, false, bitmap_config);

    AndroidBitmap_unlockPixels(env, bitmap);
    return _bitmap;
}
