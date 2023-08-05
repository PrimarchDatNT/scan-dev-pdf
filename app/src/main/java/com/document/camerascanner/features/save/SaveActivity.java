package com.document.camerascanner.features.save;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.document.camerascanner.R;
import com.document.camerascanner.databases.AppDatabases;
import com.document.camerascanner.databases.model.DocumentItem;
import com.document.camerascanner.databases.model.PageItem;
import com.document.camerascanner.databinding.ActivitySaveBinding;
import com.document.camerascanner.databinding.ViewOverlaySpotlightTopBinding;
import com.document.camerascanner.features.detailshow.documents.DetailDocumentActivity;
import com.document.camerascanner.features.detect.DetectActivity;
import com.document.camerascanner.features.view.CustomisedDialog;
import com.document.camerascanner.prefs.AppPref;
import com.document.camerascanner.prefs.ConstantsPrefs;
import com.document.camerascanner.utils.AppUtils;
import com.document.camerascanner.utils.Constants;
import com.document.camerascanner.utils.DbUtils;
import com.document.camerascanner.utils.FileUtils;
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

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class SaveActivity extends AppCompatActivity implements ProcessFilter.View {

    private boolean isShownSpotlight;
    private boolean isFiltered;
    private boolean isProcessingSave;
    private boolean isFromDetailDoc;
    private int startType;

    private AppPref appPref;
    private Spotlight spotlight;
    private List<Target> listTarget;
    private List<PageItem> listPageItem;
    private ActivitySaveBinding binding;
    private CompositeDisposable mDisposable;
    private PageItemAdapter mAdapter;
    private FilterPresenter filterPresenter;
    private AppDatabases appDatabases;
    private FirebaseAnalytics event;
    private DocumentItem documentItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.init();
    }

    private void init() {
        this.initData();
        this.initView();
        this.initSpotlight();
    }

    private void initData() {
        this.event = FirebaseAnalytics.getInstance(this);
        this.event.logEvent("SAVE_OPEN", null);
        this.filterPresenter = new FilterPresenter(this);
        this.appDatabases = AppDatabases.getInstance(this);
        this.appPref = AppPref.getInstance(this);
        this.listPageItem = new ArrayList<>();
        this.isShownSpotlight = true;
        this.startType = AppUtils.getCallActivity(this.appPref.isDocument(), this.appPref.getSaveId() < 1);
    }

    private void initView() {
        this.binding = ActivitySaveBinding.inflate(this.getLayoutInflater());
        this.setContentView(this.binding.getRoot());
        this.isFromDetailDoc = this.startType == StartType.START_FROM_DETAIL_DOC;

        String saveName = "";
        if (this.isFromDetailDoc) {
            this.appDatabases.documentDao()
                    .getDocumentById(this.appPref.getSaveId())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<DocumentItem>() {
                        @Override
                        public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                            onProcessDispose(d);
                        }

                        @Override
                        public void onSuccess(@io.reactivex.annotations.NonNull DocumentItem item) {
                            showDocumentName(item.getName());
                            documentItem = item;
                        }

                        @Override
                        public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                            e.printStackTrace();
                        }
                    });


        } else {
            saveName = this.appPref.getSessionDocName();
            if (TextUtils.isEmpty(saveName)) {
                saveName = FileUtils.createNameFolder(this);
            }
        }

        this.showDocumentName(saveName);
        this.binding.etFileName.setOnClickListener(view -> this.event.logEvent("SAVE_CLICK_TEXT_FIELD", null));
        this.binding.etFileName.setInputType(this.isFromDetailDoc ? InputType.TYPE_NULL : InputType.TYPE_CLASS_TEXT);

        if (this.isFromDetailDoc) {
            this.binding.etFileName.setBackground(null);
        }

        this.binding.ivDenyButton.setVisibility(this.isFromDetailDoc ? View.GONE : View.VISIBLE);
        this.initRecycler();
    }

    private void showDocumentName(String saveName) {
        this.binding.etFileName.setText(saveName);
        this.binding.etFileName.setSelection(saveName.length());
    }

    private void initRecycler() {
        this.mAdapter = new PageItemAdapter(this);
        this.mAdapter.setDeleteListener(this::showDeleteDialog);
        this.mAdapter.setItemClickListener(this::onPageItemClick);
        this.binding.rvDocumentItem.setLayoutManager(new GridLayoutManager(this, 3));
        this.binding.rvDocumentItem.setAdapter(this.mAdapter);
        this.initPageItem();
    }

    private void initSpotlight() {
        if (!this.appPref.isShowSpotlight(ConstantsPrefs.KEY_SHOW_SPOTLIGHT_SAVE_ACTIVITY)) {
            this.binding.etFileName.setFocusable(true);
            this.isShownSpotlight = false;
            return;
        }

        this.listTarget = new ArrayList<>();
        ViewOverlaySpotlightTopBinding topBinding = ViewOverlaySpotlightTopBinding.inflate(LayoutInflater.from(this));
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            float[] fieldNameSize = getTargetSize(this.binding.etFileName);
            RoundedRectangle fieldNameShape = new RoundedRectangle((float) (fieldNameSize[1] * 1.6),
                    (float) (fieldNameSize[0] * 1.8), 30, 500L, new DecelerateInterpolator(2f));

            RippleEffect rippleEffect = new RippleEffect(80f, 100f, (30), 124, new DecelerateInterpolator(2f), 90);
            this.createTargetSpotlight(topBinding.getRoot(), topBinding.tvOverlayMessageTop, fieldNameSize, fieldNameShape, rippleEffect,
                    R.string.save_target_rename_name_field, topBinding.closeTarget, this.getString(R.string.spotlight_got_it));

            float[] removeButtonSize = this.getTargetSize(this.binding.ivDenyButton);
            Circle circleShape = new Circle(62f, 500L, new DecelerateInterpolator(2f));
            this.createTargetSpotlight(topBinding.getRoot(), topBinding.tvOverlayMessageTop, removeButtonSize, circleShape, rippleEffect,
                    R.string.save_target_remove_name_folder, topBinding.closeTarget, this.getString(R.string.spotlight_got_it));

            float[] tickSize = getTargetSize(this.binding.ivTickIcon);
            this.createTargetSpotlight(topBinding.getRoot(), topBinding.tvOverlayMessageTop, tickSize, circleShape, rippleEffect,
                    R.string.save_target_tick, topBinding.closeTarget, this.getString(R.string.spotlight_got_it));

            this.spotlight = new Spotlight.Builder(this)
                    .setTargets(this.listTarget)
                    .setBackgroundColor(R.color.spotlightBackground)
                    .setDuration(1000L)
                    .setAnimation(new DecelerateInterpolator(2f))
                    .setOnSpotlightListener(new OnSpotlightListener() {
                        @Override
                        public void onStarted() {
                            topBinding.getRoot().setClickable(true);
                        }

                        @Override
                        public void onEnded() {
                            binding.etFileName.setFocusable(true);
                        }
                    })
                    .setContainer(this.binding.getRoot())
                    .build();

            this.spotlight.start();

            topBinding.closeTarget.setOnClickListener(new OnSingleClickListener() {
                @Override
                public void onSingleClick(View v) {
                    if (listTarget.size() == 1) {
                        finshSpot();
                        return;
                    }

                    if (spotlight != null) {
                        listTarget.remove(0);
                        spotlight.next();
                    }
                }
            });

            topBinding.closeSpotlight.setOnClickListener(new OnSingleClickListener() {
                @Override
                public void onSingleClick(View v) {
                    finshSpot();
                }
            });
        }, 500);
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

    private void createTargetSpotlight(View root, TextView textView, @NonNull float[] size, Shape shape, RippleEffect rippleEffect, int stringResource, TextView tvBtn, String tvResId) {
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
                            textView.setText(stringResource);
                            tvBtn.setText(tvResId);
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

    private void finshSpot() {
        this.isShownSpotlight = false;
        this.appPref.setShowSpotlight(ConstantsPrefs.KEY_SHOW_SPOTLIGHT_SAVE_ACTIVITY, false);

        if (this.spotlight != null) {
            this.spotlight.finish();
        }
    }

    private void initPageItem() {
        if (this.filterPresenter != null) {
            this.filterPresenter.loadTempImage(this);
        }
    }

    private void onPageItemClick(int position) {
        if (this.isShownSpotlight) {
            return;
        }

        if (this.isFiltered) {
            if (this.binding.etFileName.isEnabled()) {
                Editable text = this.binding.etFileName.getText();
                if (text == null) {
                    return;
                }
                this.appPref.setSessionDocName(text.toString());
            }
            this.startDetectActivity(position);
        } else {
            Toast.makeText(this, R.string.save_alert_filter_on_process, Toast.LENGTH_SHORT).show();
        }
    }

    private void startDetectActivity(int position) {
        Intent iCropImge = new Intent(this, DetectActivity.class);
        iCropImge.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        iCropImge.putExtra(Constants.EXTRA_IS_FROM_SAVE, true);
        iCropImge.putExtra(Constants.EXTRA_PAGE_ITEM, this.listPageItem.get(position));
        this.startActivity(iCropImge);
        this.finish();
    }

    private void deteleteItem(int position) {
        Completable.create(emitter -> {
            PageItem removeItem = this.listPageItem.get(position);
            FileUtils.deletePageItem(removeItem);
            this.listPageItem.remove(position);
            this.appDatabases.pageDao().deleteEntityNoRx(removeItem);

            List<PageItem> listUpdate = new ArrayList<>();
            for (int i = position; i < this.listPageItem.size(); i++) {
                PageItem item = this.listPageItem.get(i);
                item.setPosition(i + 1);
                this.listPageItem.set(i, item);
                listUpdate.add(item);
            }

            this.appDatabases.pageDao().updateListEntityNoRx(listUpdate);

            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        onProcessDispose(d);
                    }

                    @Override
                    public void onComplete() {
                        mAdapter.notifyItemRemoved(position);
                        mAdapter.notifyItemRangeChanged(position, listPageItem.size());
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

    private void showErrorNameDialog(int resId) {
        CustomisedDialog dialog = new CustomisedDialog(this)
                .setCancelable(true)
                .setTitle(R.string.save_alert_input_error_dialog_title)
                .setMessage(resId)
                .setButtonAllowText(R.string.save_alert_input_error_dialog_positive_cta);
        dialog.setListener(new CustomisedDialog.DialogOnClickListener() {
            @Override
            public void onCancel() {
                dialog.dimiss();
            }

            @Override
            public void onAccept() {
                dialog.dimiss();
            }
        });
        dialog.show();
    }

    private void checkValidAndSaveDocument() {
        Editable text = this.binding.etFileName.getText();
        if (text == null || TextUtils.isEmpty(text)) {
            this.showErrorNameDialog(R.string.save_alert_input_empty_name);
            return;
        }

        String documentName = text.toString();

        if (FileUtils.isInvalidFDocName(documentName)) {
            this.showErrorNameDialog(R.string.save_alert_input_invalid_name);
            return;
        }

        if (this.startType == StartType.START_FROM_MAIN) {
            this.appDatabases.documentDao()
                    .isExistsDocumentName(documentName)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<Boolean>() {
                        @Override
                        public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                            onProcessDispose(d);
                        }

                        @Override
                        public void onSuccess(@io.reactivex.annotations.NonNull Boolean isExists) {
                            if (isExists) {
                                showErrorNameDialog(R.string.save_alert_input_duplicate_name);
                            } else {
                                DocumentItem documentItem = new DocumentItem();
                                documentItem.setParentId(1);
                                documentItem.setName(documentName);
                                startSave(documentItem);
                            }
                        }

                        @Override
                        public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                            e.printStackTrace();
                        }
                    });
            return;
        }

        if (this.startType == StartType.START_FROM_DETAIL_DOC) {
            this.startSave(this.documentItem);
            return;
        }

        if (this.startType == StartType.START_FROM_DETAIL_FOLDER) {
            DbUtils.createNewDocument(this, documentName, this.appPref.getSaveId(), new DbUtils.CreateDocumentCallback() {
                @Override
                public void onDisposable(Disposable disposable) {
                    onProcessDispose(disposable);
                }

                @Override
                public void onSuccess(DocumentItem documentItem) {
                    startSave(documentItem);
                }

                @Override
                public void onError() {
                }
            });
        }
    }

    private void startSave(DocumentItem documentItem) {
        this.isProcessingSave = true;

        this.binding.ivDenyButton.setVisibility(View.GONE);
        this.binding.etFileName.setEnabled(false);
        this.binding.ivTickIcon.setEnabled(false);
        this.filterPresenter.onStartSave(this, documentItem, this.listPageItem);
    }

    public void onClickRename(View view) {
        this.event.logEvent("SAVE_CLICK_RENAME", null);
        if (this.isShownSpotlight) {
            return;
        }

        if (this.binding.etFileName.getText() != null) {
            this.binding.etFileName.getText().clear();
        }
    }

    public void onClickSaveData(android.view.View view) {
        this.event.logEvent("SAVE_CLICK_SAVE", null);
        if (this.isShownSpotlight) {
            return;
        }

        if (this.isFiltered) {
            this.checkValidAndSaveDocument();
        } else {
            Toast.makeText(this, R.string.save_alert_multiple_filter_processing, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (this.appPref.isShowSpotlight(ConstantsPrefs.KEY_SHOW_SPOTLIGHT_SAVE_ACTIVITY)) {
            this.finshSpot();
            return;
        }

        if (this.isProcessingSave) {
            Toast.makeText(this, R.string.save_all_alert_saving_file, Toast.LENGTH_SHORT).show();
            return;
        }

        this.showExitDialog();
    }

    private void showDeleteDialog(int postion) {
        if (this.isShownSpotlight) {
            return;
        }

        if (this.isFiltered) {
            if (this.listPageItem.size() > 1) {
                CustomisedDialog dialog = new CustomisedDialog(this)
                        .setCancelable(false)
                        .setTitle(R.string.save_delete_dialog_title)
                        .setMessage(R.string.save_delete_dialog_message)
                        .setButtonCancelText(R.string.save_delete_dialog_negative_cta)
                        .setButtonAllowText(R.string.save_delete_dialog_positive_cta);
                dialog.setListener(new CustomisedDialog.DialogOnClickListener() {
                    @Override
                    public void onCancel() {
                        dialog.dimiss();
                    }

                    @Override
                    public void onAccept() {
                        dialog.dimiss();
                        deteleteItem(postion);
                    }
                });
                dialog.show();
            } else {
                this.showExitDialog();
            }
        } else {
            Toast.makeText(this, R.string.save_alert_multiple_filter_processing, Toast.LENGTH_SHORT).show();
        }
    }

    private void showExitDialog() {
        int discardMessage;
        if (this.isFromDetailDoc) {
            discardMessage = this.listPageItem.size() > 1 ? R.string.discard_change_detail_page_confirm : R.string.save_show_dialog_discard_confirm;
        } else {
            discardMessage = R.string.save_discard_save_dialog_message;
        }

        CustomisedDialog dialog = new CustomisedDialog(this)
                .setCancelable(false)
                .setTitle(this.isFromDetailDoc ? R.string.save_discard_page_dialog_title : R.string.save_discard_save_dialog_title)
                .setMessage(discardMessage)
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
                startResultActivity();
            }
        });
        dialog.show();
    }

    private void startResultActivity() {
        this.appPref.setSessionDocName("");
        AppUtils.startActivity(this);
    }

    @Override
    public void onShowTempImage(@NonNull List<PageItem> listItem) {
        for (PageItem pageItem : listItem) {
            pageItem.setLoading(TextUtils.isEmpty(pageItem.getEnhanceUri()));
        }

        for (PageItem pageItem : listItem) {
            if (pageItem.isLoading()) {
                this.isFiltered = false;
                break;
            } else {
                this.isFiltered = true;
            }
        }

        this.listPageItem = new ArrayList<>(listItem);
        this.mAdapter.setListItem(this.listPageItem);

        if (this.filterPresenter != null && !this.isFiltered) {
            this.filterPresenter.loadImage(this, AppUtils.getFilterOption(this), this.listPageItem);
        }
    }

    @Override
    public void onFilterResult(PageItem item) {
        if (item == null) {
            return;
        }

        int position = item.getPosition() - 1;
        this.listPageItem.set(position, item);
        this.mAdapter.notifyItemChanged(position);
    }

    @Override
    public void onProcessDispose(Disposable disposable) {
        if (this.mDisposable == null) {
            this.mDisposable = new CompositeDisposable();
        }
        this.mDisposable.add(disposable);
    }

    @Override
    protected void onDestroy() {
        if (this.mDisposable != null) {
            this.mDisposable.dispose();
        }

        System.gc();
        super.onDestroy();
    }

    @Override
    public void onFilterSuccess() {
        this.isFiltered = true;
    }

    @Override
    public void onSaveSuccess(int id) {
        this.appPref.setCurrentFilter(AppUtils.getFilterOption(this));
        this.appPref.setCallShowReview(true);
        this.isProcessingSave = false;
        this.appPref.setSessionDocName("");
        Intent intent = new Intent(this, DetailDocumentActivity.class);
        intent.putExtra(Constants.EXTRA_DOCUMENT_ID, id);
        this.startActivity(intent);
        this.finish();
    }

}