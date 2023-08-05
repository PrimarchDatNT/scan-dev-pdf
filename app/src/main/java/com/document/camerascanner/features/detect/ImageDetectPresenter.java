package com.document.camerascanner.features.detect;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.net.Uri;

import java.util.Map;

import io.reactivex.disposables.Disposable;

public class ImageDetectPresenter implements ImageDetect.Presenter, ImageDetectProcess.ImageDetectCallback {

    private final ImageDetect.View callback;
    private final ImageDetectProcess detectProcess;

    public ImageDetectPresenter(Context context, ImageDetect.View callback) {
        this.callback = callback;
        this.detectProcess = new ImageDetectProcess(context);
        this.detectProcess.setCallback(this);
    }

    @Override
    public void processUri(Context context, Uri uri) {
        if (this.detectProcess != null) {
            this.detectProcess.convertUriToBitmap(context, uri);
        }
    }

    @Override
    public void detectRect(Bitmap bitmap) {
        if (this.detectProcess != null) {
            this.detectProcess.processDetectRect(bitmap);
        }
    }

    @Override
    public void processWarpTransform(Bitmap bitmap, Uri uri) {
        if (this.detectProcess != null) {
            this.detectProcess.wrapTransformImage(bitmap, uri);
        }
    }

    @Override
    public void onSubsribeDispose(Disposable dispose) {
        if (this.callback != null) {
            this.callback.onDispose(dispose);
        }
    }

    @Override
    public void onProcessUriSuccess(Bitmap bitmap) {
        if (this.callback != null) {
            this.callback.onShowResult(bitmap);
        }
    }

    @Override
    public void onDetectError() {
        if (this.callback != null) {
            this.callback.onDetectError();
        }
    }

    @Override
    public void onWarpTransformSucces() {
        if (this.callback != null) {
            this.callback.onWarpTransformResult();
        }
    }

    @Override
    public void onDetectSuscces(Map<Integer, PointF> pointFMap) {
        if (this.callback != null) {
            this.callback.onShowDetectedPointF(pointFMap);
        }
    }

    @Override
    public void processWarpTransform(Bitmap bitmap, PointF[] points, Uri uri) {
        if (this.detectProcess != null) {
            this.detectProcess.wrapTransformImage(bitmap, points, uri);
        }
    }

    @Override
    public void onWarpError() {
        if (this.callback != null) {
            this.callback.onWarpError();
        }
    }

}
