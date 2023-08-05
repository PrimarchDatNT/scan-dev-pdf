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
import com.takusemba.spotlight.shape.Circle;
import com.takusemba.spotlight.shape.RoundedRectangle;
import com.takusemba.spotlight.shape.Shape;

import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.List;

public class EnhanceIntro {

    private final AppPref appPref;
    private final Activity activity;
    private final ViewGroup rootView;

    private View __filterAnchor;
    private View __rotateAnchor;
    private View __cropAnchor;
    private View __tickAnchor;

    private Spotlight spotlight;
    private List<Target> listTarget;
    private IntroCallback callback;

    public EnhanceIntro(Activity activity, ViewGroup rootView) {
        this.activity = activity;
        this.rootView = rootView;
        this.appPref = AppPref.getInstance(activity);
    }

    public EnhanceIntro setFilterAnchor(View __filterAnchor) {
        this.__filterAnchor = __filterAnchor;
        return this;
    }

    public EnhanceIntro setRotateAnchor(View __rotateAnchor) {
        this.__rotateAnchor = __rotateAnchor;
        return this;
    }

    public EnhanceIntro setCropAnchor(View __cropAnchor) {
        this.__cropAnchor = __cropAnchor;
        return this;
    }

    public EnhanceIntro setTickAnchor(View __tickAnchor) {
        this.__tickAnchor = __tickAnchor;
        return this;
    }

    public EnhanceIntro setCallback(IntroCallback callback) {
        this.callback = callback;
        return this;
    }

    public void showGuilde() {
        this.listTarget = new ArrayList<>();
        float diff = this.activity.getResources().getDimension(R.dimen.dp30);
        ViewOverlaySpotlightBottomBinding bottomBinding = ViewOverlaySpotlightBottomBinding.inflate(LayoutInflater.from(this.activity));

        ViewOverlaySpotlightTopBinding topBinding = ViewOverlaySpotlightTopBinding.inflate(LayoutInflater.from(this.activity));


        float[] filterBar = this.getTargetSize(this.__filterAnchor);
        float rh = this.activity.getResources().getDimension(R.dimen.dp54);
        float rw = this.rootView.getWidth() - 2 * diff;
        RoundedRectangle filterBarShape = new RoundedRectangle(rh, rw, 30, 500L, new DecelerateInterpolator(2f));

        RippleEffect rippleEffect = new RippleEffect(80f, 100f, (30), 124, new DecelerateInterpolator(2f), 90);
        this.createTarget(bottomBinding.getRoot(), bottomBinding.tvOverlayMessageBottom, filterBar, filterBarShape, rippleEffect, R.string.enhance_target_filter_bar,
                topBinding.closeTarget);

        float[] ivRotateSize = this.getTargetSize(this.__rotateAnchor);
        Circle circleShape = new Circle(62f, 500L, new DecelerateInterpolator(2f));
        this.createTarget(bottomBinding.getRoot(), bottomBinding.tvOverlayMessageBottom,
                ivRotateSize, circleShape, rippleEffect, R.string.enhance_target_rotate, bottomBinding.closeTarget);

        float[] cropTargetSize = this.getTargetSize(this.__cropAnchor);
        this.createTarget(topBinding.getRoot(), topBinding.tvOverlayMessageTop,
                cropTargetSize, circleShape, rippleEffect, R.string.enhance_target_crop, topBinding.closeTarget);

        float[] tickTargetSize = this.getTargetSize(this.__tickAnchor);
        this.createTarget(bottomBinding.getRoot(), bottomBinding.tvOverlayMessageBottom
                , tickTargetSize, circleShape, rippleEffect, R.string.enhance_target_tick, bottomBinding.closeTarget);

        this.spotlight = new Spotlight.Builder(this.activity)
                .setTargets(this.listTarget)
                .setBackgroundColor(R.color.spotlightBackground)
                .setDuration(1000L)
                .setAnimation(new DecelerateInterpolator(2f))
                .setContainer(this.rootView)
                .setOnSpotlightListener(new OnSpotlightListener() {
                    @Override
                    public void onStarted() {
                        topBinding.getRoot().setClickable(true);
                        bottomBinding.getRoot().setClickable(true);
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

        topBinding.closeTarget.setOnClickListener(clickCloseTarget);
        topBinding.closeSpotlight.setOnClickListener(clickCloseSpot);

        bottomBinding.closeTarget.setOnClickListener(clickCloseTarget);
        bottomBinding.closeSpotlight.setOnClickListener(clickCloseSpot);
    }

    private void createTarget(View rooot, TextView textView, @NonNull float[] size, Shape shape, RippleEffect rippleEffect, int resId, TextView btnTextView) {
        Target viewTarget = new Target.Builder()
                .setAnchor(size[0], size[1])
                .setShape(shape)
                .setOverlay(rooot)
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
        this.appPref.setShowSpotlight(ConstantsPrefs.KEY_SHOW_SPOTLIGHT_ENHANCE_ACTIVITY, false);
        if (this.spotlight != null) {
            this.spotlight.finish();
        }
    }

}
