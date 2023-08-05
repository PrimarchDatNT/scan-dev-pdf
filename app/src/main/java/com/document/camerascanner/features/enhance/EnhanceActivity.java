package com.document.camerascanner.features.enhance;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.document.camerascanner.R;
import com.document.camerascanner.databases.AppDatabases;
import com.document.camerascanner.databases.model.DocumentItem;
import com.document.camerascanner.databases.model.PageItem;
import com.document.camerascanner.databinding.ActivityEnhanceBinding;
import com.document.camerascanner.features.detailshow.detailpage.DetailPageActivity;
import com.document.camerascanner.features.detailshow.documents.DetailDocumentActivity;
import com.document.camerascanner.features.detect.DetectActivity;
import com.document.camerascanner.features.intro.EnhanceIntro;
import com.document.camerascanner.features.save.SaveActivity;
import com.document.camerascanner.features.save.StartType;
import com.document.camerascanner.features.save.dialog.SaveSingleDialog;
import com.document.camerascanner.features.view.CustomisedDialog;
import com.document.camerascanner.prefs.AppPref;
import com.document.camerascanner.prefs.ConstantsPrefs;
import com.document.camerascanner.utils.AppUtils;
import com.document.camerascanner.utils.Constants;
import com.document.camerascanner.utils.FileUtils;
import com.document.camerascanner.utils.ImageUtils;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class EnhanceActivity extends AppCompatActivity implements Enhance.View, SaveSingleDialog.DialogOnClickListener {

    private boolean isShowSpot;
    private boolean isFromSave;
    private boolean isAutoSave;
    private boolean isFromDetailPage;
    private boolean isShowFilterTools;
    private int callActivity;

    private Bitmap bmFilter;
    private Bitmap bmOriginal;
    private AppPref mAppPref;
    private PageItem pageItem;
    private FirebaseAnalytics event;
    private EnhanceIntro enhanceIntro;
    private EnhancePresenter presenter;
    private CompositeDisposable disposable;
    private ActivityEnhanceBinding binding;
    private SaveSingleDialog saveDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.init();
    }

    private void init() {
        this.initData();
        this.initView();
    }

    private void initData() {
        this.mAppPref = AppPref.getInstance(this);
        this.event = FirebaseAnalytics.getInstance(this);
        this.presenter = new EnhancePresenter(this);
        this.event.logEvent("ENHANCE_OPEN", null);

        Intent intent = this.getIntent();
        if (intent == null) {
            return;
        }

        this.isShowSpot = true;
        this.isShowFilterTools = true;
        this.pageItem = (PageItem) intent.getSerializableExtra(Constants.EXTRA_PAGE_ITEM);
        this.isFromDetailPage = intent.getBooleanExtra(Constants.EXTRA_IS_FROM_DETAIL_PAGE, false);
        this.isFromSave = intent.getBooleanExtra(Constants.EXTRA_IS_FROM_SAVE, false);
        this.isAutoSave = this.mAppPref.isAutoSaveDefaultName();
        this.callActivity = AppUtils.getCallActivity(this.mAppPref.isDocument(), this.mAppPref.getSaveId() < 1);
    }

    private void initSpot() {
        if (!this.mAppPref.isShowSpotlight(ConstantsPrefs.KEY_SHOW_SPOTLIGHT_ENHANCE_ACTIVITY)) {
            this.isShowSpot = false;
            return;
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            this.enhanceIntro = new EnhanceIntro(this, this.binding.getRoot())
                    .setFilterAnchor(this.binding.layoutFilter)
                    .setRotateAnchor(this.binding.ivRotate)
                    .setCropAnchor(this.binding.ivEnhanceCrop)
                    .setTickAnchor(this.binding.ivEnhanceTickIcon)
                    .setCallback(() -> this.isShowSpot = false);
            this.enhanceIntro.showGuilde();
        }, 500);
    }

    private void initView() {
        this.binding = ActivityEnhanceBinding.inflate(this.getLayoutInflater());
        this.setContentView(this.binding.getRoot());
        if (!this.isAutoSave) {
            this.initDialog();
        }
        this.updateSelectedFilter(AppUtils.getFilterOption(this));

        if (this.presenter != null) {
            Uri cropUri = this.getCropUri();
            if (cropUri != null) {
                this.presenter.loadEnhanceImage(this, cropUri);
            }
        }

        this.initSpot();
    }

    private @Nullable Uri getCropUri() {
        if (this.pageItem == null || TextUtils.isEmpty(this.pageItem.getOrgUri())) {
            return null;
        }

        try {
            String cropName = this.pageItem.getOrgUri().replaceAll(Constants.IMAGE_ORIGINAL, Constants.IMAGE_CROP);
            return Uri.parse(cropName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void hideOverlay() {
        this.binding.llOverlay.setVisibility(View.GONE);
    }

    private void showOverlay() {
        this.binding.llOverlay.setVisibility(View.VISIBLE);
    }

    public void onClickEnhanceCrop(View view) {
        if (this.isShowSpot) {
            return;
        }

        this.event.logEvent("ENHANCE_CLICK_CROP", null);

        if (this.pageItem == null) {
            return;
        }

        Intent intent = new Intent(this, DetectActivity.class);
        intent.putExtra(Constants.EXTRA_IS_FROM_SAVE, this.isFromSave);
        intent.putExtra(Constants.EXTRA_PAGE_ITEM, this.pageItem);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(intent);
        this.finish();
    }

    public void onClickOriginal(View view) {
        if (this.isShowSpot) {
            return;
        }
        this.event.logEvent("ENHANCE_CLICK_ORIGINAL_FILTER", null);
        this.mAppPref.setCurrentFilter(FilterType.ORIGNAL);
        this.bmFilter = this.bmOriginal.copy(this.bmOriginal.getConfig(), false);
        this.binding.ivEnhancePreview.setImageBitmap(this.bmFilter);
        this.updateSelectedFilter(FilterType.ORIGNAL);
        this.makeToast(R.string.enhance_filter_mode_original);
    }

    public void onCLickGreyScale(View view) {
        if (this.isShowSpot) {
            return;
        }
        this.event.logEvent("ENHANCE_CLICK_GRAY_SCALE_FILTER", null);
        this.applyFilterMode(FilterType.GREY_SCALE);
        this.makeToast(R.string.enhance_filter_mode_grey_scale);
    }

    public void onClickMagicColor(View view) {
        if (this.isShowSpot) {
            return;
        }
        this.event.logEvent("ENHANCE_CLICK_MAGIC_COLOR_FILTER", null);
        this.applyFilterMode(FilterType.MAGIC_COLOR);
        this.makeToast(R.string.enhance_filter_mode_magic_color);
    }

    public void onClickBlackAndWhite(View view) {
        if (this.isShowSpot) {
            return;
        }
        this.event.logEvent("ENHANCE_CLICK_BLACK_AND_WHITE", null);
        this.applyFilterMode(FilterType.BLACK_AND_WHITE);
        this.makeToast(R.string.enhance);
    }

    public void onClickBlackAndWhite2(View view) {
        if (this.isShowSpot) {
            return;
        }
        this.event.logEvent("ENHANCE_CLICK_BLACK_AND_WHITE_2", null);
        this.applyFilterMode(FilterType.BLACK_AND_WHITE_2);
        this.makeToast(R.string.enhance_filter_mode_bnw2);
    }

    public void onClickNoShadow(View view) {
        if (this.isShowSpot) {
            return;
        }
        this.showOverlay();
        this.event.logEvent("ENHANCE_CLICK_NO_SHADOW", null);
        this.applyFilterMode(FilterType.NO_SHADOW);
        this.makeToast(R.string.enhance_filter_mode_no_shadow);
    }

    public void onClickFilterTools(View view) {
        this.showFilterTool(!this.isShowFilterTools);
    }

    private void makeToast(int resId) {
        Toast toast = Toast.makeText(this, resId, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 200);
        toast.show();
    }

    private void applyFilterMode(@FilterType int filterType) {
        this.mAppPref.setCurrentFilter(filterType);
        this.updateSelectedFilter(filterType);

        if (this.presenter == null) {
            return;
        }

        this.presenter.applyFilter(filterType, this.bmOriginal);
    }

    public void onClickRotate(View view) {
        this.event.logEvent("ENHANCE_CLICK_ROTATE", null);

        Completable.create(emitter -> {
            this.bmOriginal = ImageUtils.rotate(this.bmOriginal, 90);
            this.bmFilter = ImageUtils.rotate(this.bmFilter, 90);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        onEnhanceDispose(d);
                    }

                    @Override
                    public void onComplete() {
                        binding.ivEnhancePreview.setImageBitmap(bmFilter);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

    public void onClickBackPress(View view) {
        this.onBackPressed();
    }

    private void updateSelectedFilter(int position) {
        this.binding.ivOriginalMode.setSelected(position == FilterType.ORIGNAL);
        this.binding.ivGreyScaleMode.setSelected(position == FilterType.GREY_SCALE);
        this.binding.ivMagicColorMode.setSelected(position == FilterType.MAGIC_COLOR);
        this.binding.ivBlackAndWhiteMode.setSelected(position == FilterType.BLACK_AND_WHITE);
        this.binding.ivBlackAndWhite2Mode.setSelected(position == FilterType.BLACK_AND_WHITE_2);
        this.binding.ivNoShadowMode.setSelected(position == FilterType.NO_SHADOW);
    }

    public void onClickSave(View view) {
        if (this.isShowSpot) {
            return;
        }

        this.binding.ivEnhanceTickIcon.setEnabled(false);

        if (this.presenter == null) {
            return;
        }

        if (this.isFromDetailPage || this.isFromSave) {
            this.presenter.saveEnhace(this, false, this.pageItem, this.bmFilter);
            return;
        }

        if (this.callActivity == StartType.START_FROM_DETAIL_DOC) {
            this.pageItem.setParentId(this.mAppPref.getSaveId());
            this.presenter.saveEnhace(this, true, this.pageItem, this.bmFilter);
            return;
        }


        if (this.saveDialog != null) {
            this.saveDialog.show();
            return;
        }

        this.onAccept();
    }

    private void initDialog() {
        this.saveDialog = new SaveSingleDialog(this)
                .setCancelable(false)
                .setDocumentName(FileUtils.createNameFolder(this));
        this.saveDialog.setListener(this);
    }

    private void startSaveActivity() {
        Intent intent = new Intent(this, SaveActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(intent);
        this.finish();
    }

    @Override
    public void onBackPressed() {
        if (this.mAppPref.isShowSpotlight(ConstantsPrefs.KEY_SHOW_SPOTLIGHT_ENHANCE_ACTIVITY)) {
            if (this.enhanceIntro != null) {
                this.enhanceIntro.finishGuilde();
                return;
            }
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

    @Override
    public void onLoadImageResult(Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }

        this.bmOriginal = bitmap.copy(bitmap.getConfig(), false);
        if (this.presenter != null) {
            this.presenter.applyFilter(AppUtils.getFilterOption(this), bitmap);
        }
    }

    @Override
    public void onFilterResult(Bitmap bitmap) {
        this.hideOverlay();
        if (bitmap == null) {
            return;
        }
        this.binding.ivEnhancePreview.setImageBitmap(bitmap);
        this.bmFilter = bitmap.copy(bitmap.getConfig(), false);
        System.gc();
    }

    @Override
    public void onEnhanceDispose(Disposable disposable) {
        if (this.disposable == null) {
            this.disposable = new CompositeDisposable();
        }
        this.disposable.add(disposable);
    }

    @Override
    public void onSaveError() {
        this.makeToast(R.string.enhance_error_save);
        this.binding.ivEnhanceTickIcon.setEnabled(true);
    }

    @Override
    public void onSaveEnhanceSucces() {
        if (this.isFromDetailPage) {
            this.startDetailPage();
            return;
        }


        if (this.callActivity == StartType.START_FROM_DETAIL_DOC) {
            this.startDetailDocument();
            return;
        }

        this.startSaveActivity();
    }

    @Override
    public void onDocumentSucces(@NotNull DocumentItem documentItem) {
        Intent iDetailDocument = new Intent(this, DetailDocumentActivity.class);
        iDetailDocument.putExtra(Constants.EXTRA_DOCUMENT_ID, documentItem.getId());
        this.startActivity(iDetailDocument);
        this.finish();
    }

    @Override
    protected void onDestroy() {
        if (this.bmOriginal != null) {
            this.bmOriginal.recycle();
        }

        if (this.bmFilter != null) {
            this.bmFilter.recycle();
        }

        if (this.disposable != null) {
            this.disposable.dispose();
        }

        this.deleteTempCropFile();

        System.gc();
        super.onDestroy();
    }

    private void showFilterTool(boolean isShow) {
        this.binding.layoutFilter.setVisibility(isShow ? View.VISIBLE : View.INVISIBLE);
        this.binding.ivTrigon.setVisibility(isShow ? View.VISIBLE : View.INVISIBLE);
        this.isShowFilterTools = !this.isShowFilterTools;
    }

    private void startDetailPage() {
        Intent iDetailPage = new Intent(this, DetailPageActivity.class);
        iDetailPage.putExtra(Constants.EXTRA_DOCUMENT_ID, this.pageItem.getParentId());
        iDetailPage.putExtra(Constants.EXTRA_SELECTED_PAGE_POSTION, this.pageItem.getPosition() - 1);
        this.startActivity(iDetailPage);
        this.finish();
    }

    private void showDiscardDialog() {
        boolean isFromDocument = this.mAppPref.isDocument();

        CustomisedDialog dialog = new CustomisedDialog(this)
                .setCancelable(false)
                .setTitle(isFromDocument ? R.string.save_discard_page_dialog_title : R.string.save_discard_save_dialog_title)
                .setMessage(isFromDocument ? R.string.save_show_dialog_discard_confirm : R.string.save_discard_save_dialog_message)
                .setButtonCancelText(R.string.camera_negative_dialog_option)
                .setButtonAllowText(R.string.camera_positive_dialog_option);
        dialog.setListener(new CustomisedDialog.DialogOnClickListener() {
            @Override
            public void onCancel() {
                dialog.dimiss();
            }

            @Override
            public void onAccept() {
                dialog.dimiss();
                AppUtils.startActivity(EnhanceActivity.this);
            }
        });
        dialog.show();
    }

    private void deleteTempCropFile() {
        Uri cropUri = this.getCropUri();
        if (cropUri != null) {
            FileUtils.deleteFile(cropUri);
        }
    }

    private void startDetailDocument() {
        // TODO: 11/16/2020  push auto save
        if (this.saveDialog != null) {
            this.saveDialog.dimiss();
        }
        Intent intent = new Intent(this, DetailDocumentActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(intent);
        this.finish();
    }

    @Override
    public void onCheckedChange(boolean isChecked) {
        this.mAppPref.setAutoSaveDefaultName(isChecked);
    }

    @Override
    public void onCancel() {
        if (this.saveDialog != null) {
            this.saveDialog.dimiss();
            this.binding.ivEnhanceTickIcon.setEnabled(true);
        }
    }

    @Override
    public void onAccept() {
        if (this.presenter == null) {
            return;
        }

        String documentName;
        if (this.isAutoSave) {
            documentName = FileUtils.createNameFolder(this);
        } else {
            documentName = this.saveDialog == null ? FileUtils.createNameFolder(this) : this.saveDialog.getInputContent();
        }

        if (TextUtils.isEmpty(documentName)) {
            this.makeToast(R.string.save_alert_input_empty_name);
            return;
        }

        if (FileUtils.isInvalidFDocName(documentName)) {
            this.makeToast(R.string.save_alert_input_invalid_name);
            return;
        }

        String finalDocumentName = documentName;
        AppDatabases.getInstance(this)
                .documentDao()
                .isExistsDocumentName(documentName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Boolean>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        onEnhanceDispose(d);
                    }

                    @Override
                    public void onSuccess(@NonNull Boolean isExists) {
                        if (isExists) {
                            makeToast(R.string.save_alert_input_duplicate_name);
                            return;
                        }
                        presenter.saveSingleDocument(EnhanceActivity.this, Math.max(mAppPref.getSaveId(), 1), finalDocumentName, bmFilter, pageItem);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        e.printStackTrace();
                    }
                });

    }
}