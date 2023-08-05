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
import com.document.camerascanner.databinding.ViewOverlaySpotlightSuperTopBinding;
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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MainIntro {

    private final AppPref appPref;
    private final Activity activity;
    private final ViewGroup rootView;
    private final ViewOverlaySpotlightTopBinding topBinding;
    private final ViewOverlaySpotlightBottomBinding bottomBinding;
    private final ViewOverlaySpotlightSuperTopBinding superTopBinding;

    private View __captrueAnchor;
    private View __importAnchor;
    private View __datasAnchor;
    private View __utilbarAnchor;
    private View __settingAnchor;

    private Spotlight spotlight;
    private List<Target> listTarget;
    private IntroCallback callback;

    public MainIntro(Activity activity, ViewGroup viewGroup) {
        this.activity = activity;
        this.appPref = AppPref.getInstance(activity);
        this.rootView = viewGroup;
        this.topBinding = ViewOverlaySpotlightTopBinding.inflate(LayoutInflater.from(activity));
        this.bottomBinding = ViewOverlaySpotlightBottomBinding.inflate(LayoutInflater.from(activity));
        this.superTopBinding = ViewOverlaySpotlightSuperTopBinding.inflate(LayoutInflater.from(activity));
        this.init();
    }

    public MainIntro setCallback(IntroCallback callback) {
        this.callback = callback;
        return this;
    }

    public MainIntro setCaptureAnchor(View view) {
        this.__captrueAnchor = view;
        return this;
    }

    public MainIntro setImportAnchor(View view) {
        this.__importAnchor = view;
        return this;
    }

    public MainIntro setDatasAnchor(View view) {
        this.__datasAnchor = view;
        return this;
    }

    public MainIntro setUtilBarAnchor(View view) {
        this.__utilbarAnchor = view;
        return this;
    }

    public MainIntro setSettingAnchor(View view) {
        this.__settingAnchor = view;
        return this;
    }

    private void init() {
        final OnSingleClickListener onClickCloseTarget = new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                if (listTarget.size() == 1) {
                    finishGuilde();
                    return;
                }
                spotlight.next();
            }
        };

        final OnSingleClickListener onClickCloseSpot = new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                finishGuilde();
            }
        };

        this.topBinding.closeTarget.setOnClickListener(onClickCloseTarget);
        this.topBinding.closeSpotlight.setOnClickListener(onClickCloseSpot);

        this.superTopBinding.closeTarget.setOnClickListener(onClickCloseTarget);
        this.superTopBinding.closeSpotlight.setOnClickListener(onClickCloseSpot);

        this.bottomBinding.closeTarget.setOnClickListener(onClickCloseTarget);
        this.bottomBinding.closeSpotlight.setOnClickListener(onClickCloseSpot);
    }

    public void showGuilde() {
        this.listTarget = new ArrayList<>();

        float[] welMessageSize = this.getTargetSize(this.rootView, false);
        RoundedRectangle welcomeShape = new RoundedRectangle(0L, 0L, 0, 500L,
                new DecelerateInterpolator(0f));

        Target welcomeTarget = this.createSpotTarget(welMessageSize, topBinding.getRoot(), welcomeShape,
                this.getTargetListener(String.format(this.activity.getString(R.string.main_target_welcome_message),
                        this.activity.getString(R.string.app_name)), this.topBinding.tvOverlayMessageTop,
                        this.topBinding.closeTarget, this.activity.getString(R.string.spotlight_let_go)));
        this.listTarget.add(welcomeTarget);

        //capture
        float[] cameraSize = this.getTargetSize(this.__captrueAnchor, true);
        Circle circleCamera = new Circle(88, 500L, new DecelerateInterpolator(2f));
        Target cameraTarget = this.createSpotTarget(cameraSize, bottomBinding.getRoot(), circleCamera,
                this.getTargetListener(this.activity.getString(R.string.main_target_camera),
                        this.bottomBinding.tvOverlayMessageBottom, this.bottomBinding.closeTarget, this.activity.getString(R.string.spotlight_got_it)));
        this.listTarget.add(cameraTarget);

        // import
        float[] importSize = this.getTargetSize(this.__importAnchor, false);
        RoundedRectangle importTargetShape = new RoundedRectangle((float) (importSize[0] / 6.5), importSize[1] / 5,
                30, 500L, new DecelerateInterpolator(2f));

        Target importTarget = this.createSpotTarget(importSize, this.bottomBinding.getRoot(), importTargetShape,
                this.getTargetListener(this.activity.getString(R.string.main_target_import),
                        this.bottomBinding.tvOverlayMessageBottom, this.bottomBinding.closeTarget,
                        this.activity.getString(R.string.spotlight_got_it)));
        this.listTarget.add(importTarget);

        // listdata
        float[] listFolderSize = this.getTargetSize(this.__datasAnchor, false);
        RoundedRectangle listFolerShape = new RoundedRectangle((float) (listFolderSize[0] * 2.6), (listFolderSize[1]),
                30, 500L, new DecelerateInterpolator(2f));

        Target rvTarget = this.createSpotTarget(listFolderSize, superTopBinding.getRoot(), listFolerShape,
                this.getTargetListener(this.activity.getString(R.string.main_target_folder_showcase),
                        this.superTopBinding.tvOverlayMessageSuperTop, this.superTopBinding.closeTarget,
                        this.activity.getString(R.string.spotlight_got_it)));
        this.listTarget.add(rvTarget);

        //utilbar
        float[] utilbarTarget = this.getTargetSize(this.__utilbarAnchor, false);
        RoundedRectangle utilBar = new RoundedRectangle(utilbarTarget[1] / 1.7f, utilbarTarget[0] * 2,
                30, 500L, new DecelerateInterpolator(2f));

        Target utilTarget = this.createSpotTarget(utilbarTarget, topBinding.getRoot(), utilBar,
                this.getTargetListener(this.activity.getString(R.string.main_target_utility_bar),
                        this.topBinding.tvOverlayMessageTop, this.topBinding.closeTarget, this.activity.getString(R.string.spotlight_got_it)));
        this.listTarget.add(utilTarget);

        //settings
        float[] settingSize = this.getTargetSize(this.__settingAnchor, false);
        RoundedRectangle shape = new RoundedRectangle((settingSize[0] / 1.70f), settingSize[1] / 5,
                30, 500L, new DecelerateInterpolator(2f));

        Target settingTarget = this.createSpotTarget(settingSize, bottomBinding.getRoot(), shape,
                this.getTargetListener(this.activity.getString(R.string.main_target_settings),
                        this.bottomBinding.tvOverlayMessageBottom, this.bottomBinding.closeTarget,
                        this.activity.getString(R.string.spotlight_got_it)));

        this.listTarget.add(settingTarget);

        this.spotlight = new Spotlight.Builder(this.activity)
                .setTargets(this.listTarget)
                .setBackgroundColor(R.color.spotlightBackground)
                .setDuration(1000L)
                .setAnimation(new DecelerateInterpolator(2f))
                .setContainer(this.rootView)
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
                .build();

        this.spotlight.start();
    }

    @NonNull
    private Target createSpotTarget(@NonNull float[] size, View root, Shape shape, OnTargetListener listener) {
        return new Target.Builder()
                .setAnchor(size[0], size[1])
                .setShape(shape)
                .setEffect((new RippleEffect(80f, 100f, (30), 124, new DecelerateInterpolator(2f), 90)))
                .setOverlay(root)
                .setOnTargetListener(listener)
                .build();
    }

    @Contract("_, _ -> new")
    private float @NotNull [] getTargetSize(@NonNull View targetView, boolean isCamera) {
        Rect offsetsSettings = new Rect();
        targetView.getDrawingRect(offsetsSettings);
        this.rootView.offsetDescendantRectToMyCoords(targetView, offsetsSettings);

        float diff = 0;

        if (isCamera) {
            diff = this.activity.getResources().getDimension(R.dimen.dp4);
        }

        float settingsWith = offsetsSettings.centerX();
        float settingsHeight = offsetsSettings.centerY() - diff;
        return new float[]{settingsWith, settingsHeight};
    }

    public void finishGuilde() {
        this.appPref.setShowSpotlight(ConstantsPrefs.KEY_SHOW_SPOTLIGHT_MAIN_ACTIVITY, false);
        if (this.spotlight != null) {
            this.spotlight.finish();
        }
    }

    @Contract(value = "_, _, _, _ -> new", pure = true)
    private @NotNull OnTargetListener getTargetListener(String resId, TextView textView, TextView tvBtn, String btnResId) {
        return new OnTargetListener() {
            @Override
            public void onStarted() {
                if (textView == null) {
                    return;
                }
                textView.setVisibility(View.VISIBLE);
                textView.setText(resId);
                tvBtn.setText(btnResId);
            }

            @Override
            public void onEnded() {
                if (textView != null) {
                    textView.setVisibility(View.GONE);
                }

                if (listTarget == null || listTarget.isEmpty()) {
                    return;
                }
                listTarget.remove(0);
            }
        };
    }


}
