package com.document.camerascanner.features.intro;

import android.app.Activity;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;

import com.document.camerascanner.R;
import com.document.camerascanner.databinding.ViewOverlaySpotlightBottomBinding;
import com.document.camerascanner.databinding.ViewOverlaySpotlightTopBinding;
import com.document.camerascanner.prefs.AppPref;
import com.document.camerascanner.prefs.ConstantsPrefs;
import com.document.camerascanner.utils.OnSingleClickListener;
import com.takusemba.spotlight.OnSpotlightListener;
import com.takusemba.spotlight.OnTargetListener;
import com.takusemba.spotlight.Spotlight;
import com.takusemba.spotlight.Target;
import com.takusemba.spotlight.effet.RippleEffect;
import com.takusemba.spotlight.shape.Circle;
import com.takusemba.spotlight.shape.Shape;

import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.List;

public class CameraIntro {

    private final AppPref appPref;
    private final Activity activity;
    private final ViewGroup rootView;

    private View __cameraAnchor;
    private View __flashAnchor;
    private View __multiCaptureAnchor;
    private View __singleCaptureAnchor;
    private View __gridAnchor;

    private Spotlight spotlight;
    private List<Target> listTarget;
    private IntroCallback callback;

    public CameraIntro(Activity activity, ViewGroup rootView) {
        this.activity = activity;
        this.rootView = rootView;
        this.appPref = AppPref.getInstance(activity);
    }

    public CameraIntro setCameraAnchor(View __cameraAnchor) {
        this.__cameraAnchor = __cameraAnchor;
        return this;
    }

    public CameraIntro setFlashAnchor(View __flashAnchor) {
        this.__flashAnchor = __flashAnchor;
        return this;
    }

    public CameraIntro setMultiCaptureAnchor(View __multiCaptureAnchor) {
        this.__multiCaptureAnchor = __multiCaptureAnchor;
        return this;
    }

    public CameraIntro setSingleCaptureAnchor(View __singleCaptureAnchor) {
        this.__singleCaptureAnchor = __singleCaptureAnchor;
        return this;
    }

    public CameraIntro setGridAnchor(View __gridAnchor) {
        this.__gridAnchor = __gridAnchor;
        return this;
    }

    public CameraIntro setCallback(IntroCallback callback) {
        this.callback = callback;
        return this;
    }

    public void showGuide() {
        this.listTarget = new ArrayList<>();

        float[] cameraTargetSize = this.getTargetSize(this.__cameraAnchor);
        RippleEffect rippleEffect = new RippleEffect(80f, 100f, 30, 124,
                new DecelerateInterpolator(2f), 90);
        Circle circleCamera = new Circle(100, 500L, new DecelerateInterpolator(2f));
        this.createTarget(false, cameraTargetSize, circleCamera, rippleEffect, R.string.camera_target_start_camera, false);

        /*Target FlashMode*/
        float[] ivFlashSize = this.getTargetSize(this.__flashAnchor);
        Circle circle = new Circle(62, 500L, new DecelerateInterpolator(2f));
        this.createTarget(false, ivFlashSize, circle, rippleEffect, R.string.camera_target_flash_mode, false);

        /*Target Batchmode*/
        float[] ivMultipleSize = this.getTargetSize(this.__multiCaptureAnchor);
        this.createTarget(false, ivMultipleSize, circle, rippleEffect, R.string.camera_target_batch_mode, false);

        /*SingleMode Target*/
        float[] ivSingleSize = this.getTargetSize(this.__singleCaptureAnchor);
        this.createTarget(false, ivSingleSize, circle, rippleEffect, R.string.camera_target_single_mode, false);

        float[] gridAnchorSize = this.getTargetSize(this.__gridAnchor);
        this.createTarget(true, gridAnchorSize, circle, rippleEffect, R.string.camera_target_grid_mode, true);

        this.spotlight = new Spotlight.Builder(this.activity)
                .setTargets(this.listTarget)
                .setBackgroundColor(R.color.spotlightBackground)
                .setDuration(1000L)
                .setOnSpotlightListener(new OnSpotlightListener() {
                    @Override
                    public void onStarted() {
                    }

                    @Override
                    public void onEnded() {
                        if (callback != null) {
                            callback.onGuildeFinish();
                        }
                    }
                })
                .setAnimation(new DecelerateInterpolator(2f))
                .setContainer(this.rootView)
                .build();

        this.spotlight.start();
    }

    private void createTarget(boolean isTopIntro, @NonNull float[] size, Shape shape, RippleEffect rippleEffect, int stringResource, boolean isLastView) {
        OnSingleClickListener onClickFinishIntro = new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                finishGuilde();
            }
        };

        if (isTopIntro) {
            ViewOverlaySpotlightTopBinding topBinding = ViewOverlaySpotlightTopBinding.inflate(LayoutInflater.from(activity));
            Target viewTarget = new Target.Builder()
                    .setAnchor(size[0], size[1])
                    .setShape(shape)
                    .setOverlay(topBinding.getRoot())
                    .setEffect(rippleEffect)
                    .setOnTargetListener(new OnTargetListener() {
                        @Override
                        public void onStarted() {
                            topBinding.closeTarget.setText(R.string.spotlight_got_it);
                            topBinding.tvOverlayMessageTop.setVisibility(View.VISIBLE);
                            topBinding.tvOverlayMessageTop.setText(stringResource);
                        }

                        @Override
                        public void onEnded() {
                        }
                    })
                    .build();

            this.listTarget.add(viewTarget);

            if (isLastView) {
                topBinding.closeTarget.setOnClickListener(onClickFinishIntro);
            } else {
                topBinding.closeTarget.setOnClickListener(new OnSingleClickListener() {
                    @Override
                    public void onSingleClick(View v) {
                        spotlight.next();
                    }
                });
            }
            topBinding.closeSpotlight.setOnClickListener(onClickFinishIntro);

        } else {
            ViewOverlaySpotlightBottomBinding bottomBinding = ViewOverlaySpotlightBottomBinding.inflate(LayoutInflater.from(this.activity));
            Target viewTarget = new Target.Builder()
                    .setAnchor(size[0], size[1])
                    .setShape(shape)
                    .setOverlay(bottomBinding.getRoot())
                    .setEffect(rippleEffect)
                    .setOnTargetListener(new OnTargetListener() {
                        @Override
                        public void onStarted() {
                            bottomBinding.tvOverlayMessageBottom.setVisibility(View.VISIBLE);
                            bottomBinding.tvOverlayMessageBottom.setText(stringResource);
                        }

                        @Override
                        public void onEnded() {
                        }
                    })
                    .build();

            this.listTarget.add(viewTarget);

            if (isLastView) {
                bottomBinding.closeTarget.setOnClickListener(onClickFinishIntro);
            } else {
                bottomBinding.closeTarget.setOnClickListener(new OnSingleClickListener() {
                    @Override
                    public void onSingleClick(View v) {
                        spotlight.next();
                    }
                });
            }
            bottomBinding.closeSpotlight.setOnClickListener(onClickFinishIntro);
        }
    }

    public void finishGuilde() {
        this.appPref.setShowSpotlight(ConstantsPrefs.KEY_SHOW_SPOTLIGHT_CAMERA_ACTIVITY, false);
        if (this.spotlight != null) {
            this.spotlight.finish();
        }
    }

    @NonNull
    @Contract("_ -> new")
    private float[] getTargetSize(@NonNull View targetView) {
        Rect offsetsSettings = new Rect();
        targetView.getDrawingRect(offsetsSettings);
        this.rootView.offsetDescendantRectToMyCoords(targetView, offsetsSettings);
        float settingsWith = offsetsSettings.centerX();
        float settingsHeight = offsetsSettings.centerY();
        return new float[]{settingsWith, settingsHeight};
    }
}
