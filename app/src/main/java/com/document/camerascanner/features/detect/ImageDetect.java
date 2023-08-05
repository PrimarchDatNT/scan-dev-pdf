package com.document.camerascanner.features.detect;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.net.Uri;

import java.util.Map;

import io.reactivex.disposables.Disposable;

public interface ImageDetect {

    interface Presenter {

        void processUri(Context context, Uri uri);

        void detectRect(Bitmap bitmap);

        void processWarpTransform(Bitmap bitmap, PointF[] points, Uri uri);

        void processWarpTransform(Bitmap bitmap, Uri uri);
    }

    interface View {

        void onShowResult(Bitmap bitmap);

        void onDispose(Disposable disposable);

        void onDetectError();

        void onShowDetectedPointF(Map<Integer, PointF> pointFMap);

        void onWarpError();

        void onWarpTransformResult();

    }

}
