package com.document.camerascanner.features.intro;

import android.app.Activity;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

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
import com.takusemba.spotlight.shape.RoundedRectangle;
import com.takusemba.spotlight.shape.Shape;

import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.List;

public class PreviewIntro {

    private final AppPref appPref;
    private final Activity activity;
    private final ViewGroup rootView;

    private View __reCaptureAnchor;
    private View __confirmAnchor;
    private View __deleteAnchor;
    private View __typeShowAnchor;

    private Spotlight spotlight;
    private List<Target> listTarget;
    private IntroCallback callback;

    private ViewOverlaySpotlightBottomBinding bottomBinding;
    private ViewOverlaySpotlightTopBinding topBinding;

    public PreviewIntro(Activity activity, ViewGroup rootView) {
        this.activity = activity;
        this.rootView = rootView;
        this.appPref = AppPref.getInstance(activity);
        this.init();
    }

    private void init() {
        this.bottomBinding = ViewOverlaySpotlightBottomBinding.inflate(LayoutInflater.from(this.activity));
        this.topBinding = ViewOverlaySpotlightTopBinding.inflate(LayoutInflater.from(activity));

        final OnSingleClickListener clickCloseTarget = new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                if (listTarget.size() == 1) {
                    finishGuilde();
                    return;
                }

                if (spotlight != null) {
                    listTarget.remove(0);
                    spotlight.next();
                }
            }
        };

        final OnSingleClickListener clickCloseSpot = new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                finishGuilde();
            }
        };

        this.bottomBinding.closeTarget.setOnClickListener(clickCloseTarget);
        this.bottomBinding.closeSpotlight.setOnClickListener(clickCloseSpot);
        this.topBinding.closeSpotlight.setOnClickListener(clickCloseSpot);
        this.topBinding.closeTarget.setOnClickListener(clickCloseTarget);

    }

    public PreviewIntro setReCaptureAnchor(View __reCaptureAnchor) {
        this.__reCaptureAnchor = __reCaptureAnchor;
        return this;
    }

    public PreviewIntro setConfirmAnchor(View __confirmAnchor) {
        this.__confirmAnchor = __confirmAnchor;
        return this;
    }

    public PreviewIntro setCallback(IntroCallback callback) {
        this.callback = callback;
        return this;
    }

    public PreviewIntro setDeleteAnchor(View __deleteAnchor) {
        this.__deleteAnchor = __deleteAnchor;
        return this;
    }

    public PreviewIntro setTypeShowAnchor(View __typeShowAnchor) {
        this.__typeShowAnchor = __typeShowAnchor;
        return this;
    }

    public void showGuilde() {
        this.listTarget = new ArrayList<>();
        float height = this.activity.getResources().getDimension(R.dimen.dp40);
        float width = this.rootView.getWidth() / 4f;

        float[] size1 = this.getTargetSize(this.__reCaptureAnchor);
        float[] size2 = this.getTargetSize(this.__confirmAnchor);
        float[] size3 = this.getTargetSize(this.__deleteAnchor);
        float[] size4 = this.getTargetSize(this.__typeShowAnchor);

        RippleEffect rippleEffect = new RippleEffect(80f, 100f, (30), 124, new DecelerateInterpolator(2f), 90);
        RoundedRectangle rectangleViewMode = new RoundedRectangle(this.__typeShowAnchor.getHeight() / 1.2f, this.__typeShowAnchor.getWidth() / 1.2f, this.__typeShowAnchor.getHeight(), 500L, new DecelerateInterpolator(2f));

        this.createTarget(this.bottomBinding.getRoot(), this.bottomBinding.tvOverlayMessageBottom, size1, rectangleViewMode, rippleEffect,
                R.string.camera_preview_recapture, this.bottomBinding.closeTarget);

        this.createTarget(this.bottomBinding.getRoot(), this.bottomBinding.tvOverlayMessageBottom, size2, rectangleViewMode, rippleEffect,
                R.string.camera_preview_confirm, this.bottomBinding.closeTarget);

        this.createTarget(this.bottomBinding.getRoot(), this.bottomBinding.tvOverlayMessageBottom, size3, rectangleViewMode, rippleEffect,
                R.string.camera_preview_delete, this.bottomBinding.closeTarget);

        this.createTarget(this.topBinding.getRoot(), this.topBinding.tvOverlayMessageTop, size4, rectangleViewMode, rippleEffect,
                R.string.camera_preview_type_show, this.topBinding.closeTarget);

        this.spotlight = new Spotlight.Builder(this.activity)
                .setTargets(this.listTarget)
                .setBackgroundColor(R.color.spotlightBackground)
                .setDuration(1000L)
                .setAnimation(new DecelerateInterpolator(2f))
                .setContainer(this.rootView)
                .setOnSpotlightListener(new OnSpotlightListener() {
                    @Override
                    public void onStarted() {
                        bottomBinding.getRoot().setClickable(true);
                        topBinding.getRoot().setClickable(true);
                    }

                    @Override
                    public void onEnded() {
                        if (callback != null) {
                            callback.onGuildeFinish();
                        }
                    }
                })
                .build();

        this.spotlight.start();
    }

    private void createTarget(View root, TextView textView, @NonNull float[] size, Shape shape, RippleEffect rippleEffect, int resId, TextView btnTextView) {
        Target viewTarget = new Target.Builder()
                .setAnchor(size[0], size[1])
                .setShape(shape)
                .setOverlay(root)
                .setEffect(rippleEffect)
                .setOnTargetListener(new OnTargetListener() {
                    @Override
                    public void onStarted() {
                        if (textView != null) {
                            textView.setVisibility(View.VISIBLE);
                            textView.setText(resId);
                            btnTextView.setText(R.string.spotlight_got_it);
                        }
                    }

                    @Override
                    public void onEnded() {
                        if (textView != null) {
                            textView.setVisibility(View.GONE);
                        }
                    }
                })
                .build();

        this.listTarget.add(viewTarget);
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

    public void finishGuilde() {
        this.appPref.setShowSpotlight(ConstantsPrefs.KEY_SHOW_SPOTLIGHT_CAMERA_PREVIEW, false);
        if (this.spotlight != null) {
            this.spotlight.finish();
        }
    }
}
