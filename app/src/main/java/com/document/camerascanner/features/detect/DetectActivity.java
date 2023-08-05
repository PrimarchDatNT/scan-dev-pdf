package com.document.camerascanner.features.detect;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.document.camerascanner.R;
import com.document.camerascanner.databases.model.PageItem;
import com.document.camerascanner.databinding.ActivityCameraEditBinding;
import com.document.camerascanner.databinding.ViewOverlaySpotlightBottomBinding;
import com.document.camerascanner.databinding.ViewOverlaySuperBottomBinding;
import com.document.camerascanner.features.detailshow.detailpage.DetailPageActivity;
import com.document.camerascanner.features.enhance.EnhanceActivity;
import com.document.camerascanner.features.save.SaveActivity;
import com.document.camerascanner.features.view.CustomisedDialog;
import com.document.camerascanner.prefs.AppPref;
import com.document.camerascanner.prefs.ConstantsPrefs;
import com.document.camerascanner.utils.AppUtils;
import com.document.camerascanner.utils.Constants;
import com.document.camerascanner.utils.OnSingleClickListener;
import com.google.firebase.analytics.FirebaseAnalytics;
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

public class DetectActivity extends AppCompatActivity implements ProcessCropImageFragment.ProccesCropCallback {

    private boolean isFromSave;
    private boolean isFromDetailPage;

    private AppPref appPref;
    private Spotlight spotlight;
    private List<Target> listTarget;
    private FirebaseAnalytics event;
    private PageItem pageItem;
    private ActivityCameraEditBinding binding;
    private ProcessCropImageFragment cropImageFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.init();
    }

    private void init() {
        this.initData();
        this.initView();
    }

    private void initData() {
        this.event = FirebaseAnalytics.getInstance(this);
        this.event.logEvent("DETECT_OPEN", null);

        this.appPref = AppPref.getInstance(this);
        Intent intent = this.getIntent();
        if (intent == null) {
            return;
        }

        this.isFromSave = intent.getBooleanExtra(Constants.EXTRA_IS_FROM_SAVE, false);
        this.isFromDetailPage = intent.getBooleanExtra(Constants.EXTRA_IS_FROM_DETAIL_PAGE, false);
        this.pageItem = (PageItem) intent.getSerializableExtra(Constants.EXTRA_PAGE_ITEM);
    }

    @SuppressLint("SetTextI18n")
    private void initView() {
        this.binding = ActivityCameraEditBinding.inflate(this.getLayoutInflater());
        this.setContentView(this.binding.getRoot());

        if (this.pageItem == null) {
            return;
        }

        this.cropImageFragment = ProcessCropImageFragment.newInstance(this.pageItem);
        this.cropImageFragment.setCallback(this);
        this.getSupportFragmentManager()
                .beginTransaction()
                .replace(this.binding.flDetectResult.getId(), this.cropImageFragment)
                .addToBackStack(null)
                .commit();
    }

    private void initSpotlight(int bitmapWidth, int bitmapHeight) {
        if (!this.appPref.isShowSpotlight(ConstantsPrefs.KEY_SHOW_SPOTLIGHT_DETECT_ACTIVITY)) {
            return;
        }

        this.listTarget = new ArrayList<>();

        ViewOverlaySuperBottomBinding superBottomBinding = ViewOverlaySuperBottomBinding.inflate(LayoutInflater.from(this));

        ViewOverlaySpotlightBottomBinding bottomBinding = ViewOverlaySpotlightBottomBinding.inflate(LayoutInflater.from(this));

        float[] cropImageSize = this.getTargetSize(this.binding.flDetectResult);
        RoundedRectangle cropImgShape = new RoundedRectangle(bitmapHeight, bitmapWidth, 30, 500L,
                new DecelerateInterpolator(2f));
        Target cropTarget = this.creatSpotTarget(cropImageSize, superBottomBinding.getRoot(), cropImgShape,
                this.getTargetListener(R.string.detect_target_polygon_view, superBottomBinding.tvOverlayMessageSuperBottom));
        this.listTarget.add(cropTarget);

        float[] selectAllTarget = this.getTargetSize(this.binding.ivSelectAll);
        Circle circle = new Circle(62f, 500L, new DecelerateInterpolator(2f));
        Target selectAll = this.creatSpotTarget(selectAllTarget, bottomBinding.getRoot(), circle
                , this.getTargetListener(R.string.detect_target_select_all, bottomBinding.tvOverlayMessageBottom));
        this.listTarget.add(selectAll);

        float[] confirmTarget = this.getTargetSize(this.binding.ivCofirmSelect);
        Target btnConfirmTarget = this.creatSpotTarget(confirmTarget, bottomBinding.getRoot(), circle,
                this.getTargetListener(R.string.detect_target_tick, bottomBinding.tvOverlayMessageBottom));
        this.listTarget.add(btnConfirmTarget);

        this.spotlight = new Spotlight.Builder(this)
                .setTargets(this.listTarget)
                .setBackgroundColor(R.color.spotlightBackground)
                .setDuration(1000L)
                .setAnimation(new DecelerateInterpolator(2f))
                .setOnSpotlightListener(new OnSpotlightListener() {
                    @Override
                    public void onStarted() {
                        superBottomBinding.getRoot().setClickable(true);
                        bottomBinding.getRoot().setClickable(true);
                    }

                    @Override
                    public void onEnded() {
                    }
                })
                .setContainer(binding.getRoot())
                .build();

        this.spotlight.start();

        final OnSingleClickListener onclickCloseTarget = new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                if (listTarget.size() == 1) {
                    finishSpot();
                    return;
                }
                spotlight.next();
            }
        };

        final OnSingleClickListener onClickCloseSpot = new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                finishSpot();
            }
        };

        superBottomBinding.closeTarget.setOnClickListener(onclickCloseTarget);
        superBottomBinding.closeSpotlight.setOnClickListener(onClickCloseSpot);

        bottomBinding.closeTarget.setOnClickListener(onclickCloseTarget);
        bottomBinding.closeSpotlight.setOnClickListener(onClickCloseSpot);
    }

    private void finishSpot() {
        this.appPref.setShowSpotlight(ConstantsPrefs.KEY_SHOW_SPOTLIGHT_DETECT_ACTIVITY, false);
        if (this.spotlight != null) {
            this.spotlight.finish();
        }
    }

    @NonNull
    private Target creatSpotTarget(@NonNull float[] size, View root, Shape shape, OnTargetListener listener) {
        return new Target.Builder()
                .setAnchor(size[0], size[1])
                .setShape(shape)
                .setEffect((new RippleEffect(80f, 100f, (30), 124, new DecelerateInterpolator(2f), 90)))
                .setOverlay(root)
                .setOnTargetListener(listener)
                .build();
    }

    @NonNull
    @Contract(value = "_, _ -> new", pure = true)
    private OnTargetListener getTargetListener(int resId, TextView textView) {
        return new OnTargetListener() {
            @Override
            public void onStarted() {
                if (textView == null) {
                    return;
                }
                textView.setVisibility(View.VISIBLE);
                textView.setText(resId);
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

    @NonNull
    @Contract("_ -> new")
    private float[] getTargetSize(@NonNull View targetView) {
        Rect offsetsSettings = new Rect();
        targetView.getDrawingRect(offsetsSettings);
        this.binding.getRoot().offsetDescendantRectToMyCoords(targetView, offsetsSettings);
        float settingsWith = offsetsSettings.centerX();
        float settingsHeight = offsetsSettings.centerY();
        return new float[]{settingsWith, settingsHeight};
    }

    public void onClickBack(View view) {
        this.onBackPressed();
    }

    public void onClickSelectAll(View v) {
        this.event.logEvent("DETECT_CLICK_SELECT_ALL", null);
        if (this.cropImageFragment != null) {
            this.binding.ivSelectAll.setImageResource(this.cropImageFragment.isClickMax ? R.drawable.ic_select_all : R.drawable.ic_use_selected_point);
            this.cropImageFragment.setCropAll();
        }
    }

    public void onClickCofirmSelect(View v) {
        this.event.logEvent("DETECT_CLICK_TICK", null);
        this.showLoadingView(true);
        if (this.cropImageFragment != null) {
            this.cropImageFragment.warpImage();
        }
    }

    private void showLoadingView(boolean isLoading) {
        this.binding.ivCofirmSelect.setEnabled(!isLoading);
        this.binding.pbCropProgress.setVisibility(isLoading ? View.VISIBLE : View.INVISIBLE);
    }

    private void startEnhance(PageItem pageItem) {
        Intent iEnhance = new Intent(this, EnhanceActivity.class);
        iEnhance.putExtra(Constants.EXTRA_IS_FROM_SAVE, this.isFromSave);
        iEnhance.putExtra(Constants.EXTRA_PAGE_ITEM, pageItem);
        iEnhance.putExtra(Constants.EXTRA_IS_FROM_DETAIL_PAGE, this.isFromDetailPage);
        iEnhance.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(iEnhance);
        this.finish();
    }

    @Override
    protected void onDestroy() {
        System.gc();
        super.onDestroy();
    }

    private void startSaveActivity() {
        this.startActivity(new Intent(this, SaveActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        this.finish();
    }

    @Override
    public void onBackPressed() {
        if (this.appPref.isShowSpotlight(ConstantsPrefs.KEY_SHOW_SPOTLIGHT_DETECT_ACTIVITY)) {
            this.finishSpot();
            return;
        }

        if (this.isFromSave) {
            this.startSaveActivity();
            return;
        }

        if (this.isFromDetailPage) {
            this.showDiscardPageDialog();
            return;
        }

        this.showDiscardDialog();
    }

    private void showDiscardPageDialog() {
        CustomisedDialog dialog = new CustomisedDialog(this)
                .setCancelable(false)
                .setTitle(R.string.save_discard_page_dialog_title)
                .setMessage(R.string.discard_change_detail_page_confirm)
                .setButtonCancelText(R.string.save_discard_save_dialog_negative_cta)
                .setButtonAllowText(R.string.save_discard_save_dialog_positive_cta);
        dialog.setListener(new CustomisedDialog.DialogOnClickListener() {
            @Override
            public void onCancel() {
                dialog.dimiss();
            }

            @Override
            public void onAccept() {
                dialog.dimiss();
                startDetailPage();
            }
        });
        dialog.show();
    }

    private void startDetailPage() {
        Intent iDetailPage = new Intent(DetectActivity.this, DetailPageActivity.class);
        iDetailPage.putExtra(Constants.EXTRA_DOCUMENT_ID, this.pageItem.getParentId());
        iDetailPage.putExtra(Constants.EXTRA_SELECTED_PAGE_POSTION, this.pageItem.getPosition() - 1);
        this.startActivity(iDetailPage);
        this.finish();
    }

    private void showDiscardDialog() {
        boolean isFromDocument = this.appPref.isDocument();

        CustomisedDialog dialog = new CustomisedDialog(this)
                .setCancelable(false)
                .setTitle(isFromDocument ? R.string.save_discard_page_dialog_title : R.string.save_discard_save_dialog_title)
                .setMessage(isFromDocument ? R.string.save_show_dialog_discard_confirm : R.string.save_discard_save_dialog_message)
                .setButtonCancelText(R.string.save_discard_save_dialog_negative_cta)
                .setButtonAllowText(R.string.save_discard_save_dialog_positive_cta);
        dialog.setListener(new CustomisedDialog.DialogOnClickListener() {
            @Override
            public void onCancel() {
                dialog.dimiss();
            }

            @Override
            public void onAccept() {
                dialog.dimiss();
                AppUtils.startActivity(DetectActivity.this);
            }
        });
        dialog.show();
    }

    @Override
    public void onDataPass(PageItem pageItem) {
        if (this.pageItem == null) {
            this.showLoadingView(false);
            Toast.makeText(this, R.string.detect_select_crop_invalid, Toast.LENGTH_SHORT).show();
            return;
        }

        this.showLoadingView(false);
        this.startEnhance(this.pageItem);
    }

    @Override
    public void onDataError() {
        Toast.makeText(this, R.string.detect_alert_error_warp_image, Toast.LENGTH_SHORT).show();
        this.showLoadingView(false);
    }

    @Override
    public void onInitBitmapSucces(int width, int height) {
        this.initSpotlight(width, height);
    }

    @Override
    public void onMoveSelectCrop() {
        this.binding.ivSelectAll.setImageResource(R.drawable.ic_select_all);
    }

}
