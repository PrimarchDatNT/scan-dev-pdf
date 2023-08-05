package com.document.camerascanner.features.enhance;

import android.graphics.Bitmap;

public class NativeFilter {

    static {
        System.loadLibrary("native-lib");
    }

    public native Bitmap applyGrayFilter(Bitmap bitmap);

    public native Bitmap applyNoShadowFilter(Bitmap bitmap);

    public native Bitmap applyBnW1Filter(Bitmap bitmap);

    public native Bitmap applyBnW2Filter(Bitmap bitmap);

    public native Bitmap applyMagicColor(Bitmap bitmap);
}
