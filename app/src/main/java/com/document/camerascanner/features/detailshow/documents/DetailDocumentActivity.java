package com.document.camerascanner.features.detailshow.documents;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;

import com.document.camerascanner.R;
import com.document.camerascanner.ads.AdConstant;
import com.document.camerascanner.ads.AdViewType;
import com.document.camerascanner.ads.NativeAds;
import com.document.camerascanner.databases.AppDatabases;
import com.document.camerascanner.databases.livedata.PageViewModel;
import com.document.camerascanner.databases.model.BaseEntity;
import com.document.camerascanner.databases.model.DocumentItem;
import com.document.camerascanner.databases.model.PageItem;
import com.document.camerascanner.databinding.ActivityDetailShowImagesBinding;
import com.document.camerascanner.databinding.ViewDetailDocumentActionBinding;
import com.document.camerascanner.databinding.ViewOverlayBinding;
import com.document.camerascanner.features.camera.CameraActivity;
import com.document.camerascanner.features.detailshow.detailpage.DetailPageActivity;
import com.document.camerascanner.features.detailshow.documents.helper.ItemTouchHelperCallBack;
import com.document.camerascanner.features.detailshow.documents.interfaces.DetailDocument;
import com.document.camerascanner.features.detect.DetectActivity;
import com.document.camerascanner.features.main.dialog.FileDialog;
import com.document.camerascanner.features.main.dialog.MoveFileDialog;
import com.document.camerascanner.features.pdfsettings.PdfSettingsActivity;
import com.document.camerascanner.features.review.SupportInAppReview;
import com.document.camerascanner.features.save.SaveActivity;
import com.document.camerascanner.features.share.ShareDialogNew;
import com.document.camerascanner.features.share.ShareSuccessDialog;
import com.document.camerascanner.features.view.CustomisedDialog;
import com.document.camerascanner.prefs.AppPref;
import com.document.camerascanner.utils.AppUtils;
import com.document.camerascanner.utils.Constants;
import com.document.camerascanner.utils.DbUtils;
import com.document.camerascanner.utils.FileUtils;
import com.document.camerascanner.utils.PermissionUtils;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAdView;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class DetailDocumentActivity extends AppCompatActivity implements DetailDocumentAdapter.OnClickImageListener,
        DetailDocument.View, SupportInAppReview.OnReviewListener, NativeAds.CallBackNativeAd {

    private boolean isRename;
    private boolean isMoving;
    private boolean isMoveFile;
    private boolean isDragPage;
    private boolean isMultipleSelect;
    private boolean isTouchFirst;
    private int positionFirstMove = 0;
    private int positionLastMove = 0;

    //0: import img. 1: share gallery
    private int actionClickImport = 0;

    private AppPref appPref;
    private NativeAds nativeAds;
    private UnifiedNativeAd unifiedNativeAd;
    private List<PageItem> listData;
    private List<Integer> listSelectItem;
    private FirebaseAnalytics event;
    private ShareDialogNew shareDialogNew;
    private SupportInAppReview inAppReview;
    private DetailDocumentAdapter mAdapter;
    private DetailDocument.Presenter presenter;
    private ActivityDetailShowImagesBinding binding;
    private ViewDetailDocumentActionBinding docActionBinding;

    private PageViewModel pageViewModel;
    private DocumentItem documentItem;
    private ItemTouchHelper itemTouchHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.init();
    }

    private void init() {
        this.initConfig();
        this.initView();
        this.initData();
        this.initAds();
    }

    private void initConfig() {
        this.event = FirebaseAnalytics.getInstance(this);
        this.event.logEvent("DETAIL_DOC_OPEN", null);
        this.pageViewModel = new ViewModelProvider(this).get(PageViewModel.class);
        this.appPref = AppPref.getInstance(this);
        this.listData = new ArrayList<>();
        this.listSelectItem = new ArrayList<>();
        this.presenter = new DetailDocumentPresenter(this);
        this.inAppReview = new SupportInAppReview(this, this);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initView() {
        this.binding = ActivityDetailShowImagesBinding.inflate(this.getLayoutInflater());
        this.docActionBinding = ViewDetailDocumentActionBinding.bind(this.binding.ilQuickAction);
        ViewOverlayBinding.bind(this.binding.ilOverlay);
        this.setContentView(this.binding.getRoot());

        this.mAdapter = new DetailDocumentAdapter(this);
        this.mAdapter.setOnClickImageListener(this);

        this.binding.rvDetailShow.setLayoutManager(new GridLayoutManager(this, 3));
        this.binding.rvDetailShow.setAdapter(this.mAdapter);
        this.binding.ivBack.setOnClickListener(view -> this.onBackPressed());

        ItemTouchHelper.Callback callback = new ItemTouchHelperCallBack((oldPosition, newPosition) -> {
            this.changePositionDrag(this.listData, oldPosition, newPosition);

            if (!this.isTouchFirst) {
                this.positionFirstMove = oldPosition;
                this.isTouchFirst = true;
            }

            this.positionLastMove = newPosition;
            this.mAdapter.notifyItemMoved(oldPosition, newPosition);
            this.isMoving = true;
        });

        this.itemTouchHelper = new ItemTouchHelper(callback);
    }

    public boolean isRenaming() {
        if (this.isRename) {
            this.makeToast(R.string.detail_renaming);
            return true;
        }
        return false;
    }


    private void initData() {
        Intent intent = this.getIntent();
        int id = -1;
        if (intent != null) {
            id = intent.getIntExtra(Constants.EXTRA_DOCUMENT_ID, -1);
        }

        if (id > 0) {
            this.initModel(id);
            return;
        }

        id = this.appPref.getSaveId();
        if (id < 0) {
            this.finish();
        }

        this.initModel(id);
    }

    private void initModel(int id) {
        this.appPref.removePref();

        this.pageViewModel.getDocument(id).observe(this, document -> {
            if (document == null) {
                this.finish();
                return;
            }

            this.documentItem = document;
            this.binding.tvTitle.setText(this.documentItem.getName());
            this.binding.tvPageCount.setText(this.getResources()
                    .getQuantityString(R.plurals.count_page, this.documentItem.getChildCount(), this.documentItem.getChildCount()));

            if (this.listData == null || this.listData.isEmpty()) {
                this.pageViewModel.getPageByParent(this.documentItem.getId()).observe(this, list -> {
                    this.listData = new ArrayList<>(list);
                    this.listSelectItem.clear();
                    this.binding.ilOverlay.setVisibility(View.GONE);
                    this.isMoving = false;
                    this.isTouchFirst = false;

                    if (this.listData.isEmpty()) {
                        this.pageViewModel.deletePageParent(this.documentItem);
                        return;
                    }

                    this.mAdapter.setItemFileList(this.listData);

                    if (this.isMultipleSelect) {
                        this.hideOverlay();
                        this.resetStatus();
                    }
                });
            }

        });
    }

    private void initAds() {
        this.nativeAds = new NativeAds(this, AdConstant.NT_DETAIL_DOCUMENT, this, AdConstant.DETAIL_DOCUMENT);
        this.nativeAds.loadAds();
    }


    private void changePositionDrag(List<PageItem> listData, int oldPosition, int newPosition) {
        if (oldPosition < newPosition) {
            for (int i = oldPosition; i < newPosition; i++) {
                Collections.swap(listData, i, i + 1);
            }
        } else {
            for (int i = oldPosition; i > newPosition; i--) {
                Collections.swap(listData, i, i - 1);
            }
        }
    }

    private void showSendToDialog(String title) {
        final DetailDocumentActivity context = DetailDocumentActivity.this;

        this.shareDialogNew = new ShareDialogNew(this, title);
        this.shareDialogNew.setListener(new ShareDialogNew.DialogShareListener() {
            @Override
            public void onClickShareJpg() {
                event.logEvent("DETAIL_DOC_SHARE_IMAGE", null);
                if (presenter != null) {
                    presenter.shareToJpgShare(context, listData);
                    hideOverlay();
                    setSelectAll(false);
                    resetStatus();
                }
            }

            @Override
            public void onClickSharePdf() {
                event.logEvent("DETAIL_DOC_SHARE_IMAGE", null);
                if (presenter != null) {
                    showOverlay();
                    presenter.saveAndSharePdf(context, shareDialogNew.getTitle(), shareDialogNew.getQuality(), listData, listSelectItem, documentItem, !isMultipleSelect, false);
                    setSelectAll(false);
                    resetStatus();
                }
            }

            @Override
            public void onClickSavePdf() {
                event.logEvent("DETAIL_DOC_SHARE_IMAGE", null);
                if (presenter != null) {
                    showOverlay();
                    presenter.saveAndSharePdf(context, shareDialogNew.getTitle(), shareDialogNew.getQuality(), listData, listSelectItem, documentItem, !isMultipleSelect, true);
                }
            }

            @Override
            public void onClickSaveJpg() {
                event.logEvent("DETAIL_DOC_SHARE_GALLERY", null);
                if (listData != null && presenter != null) {
                    showOverlay();
                    presenter.shareToGallery(context, shareDialogNew.getTitle(), shareDialogNew.getQuality(), documentItem, listData);
                }
            }

            @Override
            public void onClickToPdfSettings() {
                event.logEvent("DETAIL_DOC_SHARE_IMAGE", null);
//                if (presenter != null) {
//                    presenter.generalSharePdf(context, listData, listSelectItem, documentItem, !isMultipleSelect);
//                }

                startActivity(new Intent(DetailDocumentActivity.this, PdfSettingsActivity.class));
//                hideOverlay();
//                setSelectAll(false);
//                resetStatus();
            }

            @Override
            public void onDismissDialog() {
                if (!isMultipleSelect) {
                    setSelectAll(false);
                }
            }
        });

        this.shareDialogNew.show(getSupportFragmentManager(), "SHARE");
    }

    private void showCreateDocumentDialog() {
        this.event.logEvent("DETAIL_DOC_CLICK_CREATE_FOLDER_DIALOG", null);
        Context mContext = DetailDocumentActivity.this;

        FileDialog dialog = new FileDialog(mContext)
                .setCancelable(false)
                .setShowPresent(false)
                .setContent(FileUtils.createNameFolder(this))
                .setTitle(R.string.all_new_document);

        dialog.setListener(new FileDialog.FileDialogListener() {
            @Override
            public void onClickCancel() {
                dialog.dimiss();
                AppUtils.closeKeyboard(mContext);
            }

            @Override
            public void onClickConfirm() {
                String inputContent = dialog.getInputContent();
                if (TextUtils.isEmpty(inputContent)) {
                    makeToast(R.string.save_alert_input_empty_name);
                    return;
                }

                if (FileUtils.isInvalidFDocName(inputContent)) {
                    makeToast(R.string.save_alert_input_invalid_name);
                    return;
                }

                AppDatabases.getInstance(mContext)
                        .documentDao()
                        .isExistsDocumentName(inputContent)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new SingleObserver<Boolean>() {
                            @Override
                            public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                                if (presenter != null) {
                                    presenter.addDisposable(d);
                                }
                            }

                            @Override
                            public void onSuccess(@io.reactivex.annotations.NonNull Boolean isExists) {
                                if (isExists) {
                                    makeToast(R.string.home_folder_exits);
                                } else {
                                    if (presenter != null) {
                                        presenter.createDocumentForCopyAndMove(mContext, inputContent);
                                    }

                                    dialog.dimiss();
                                    AppUtils.closeKeyboard(mContext);
                                }
                            }

                            @Override
                            public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                                e.printStackTrace();
                            }
                        });
            }

            @Override
            public void onDismissDialog() {
            }
        });

        dialog.getEdittext().requestFocus();
        AppUtils.showKeyboard(this);
        dialog.show();
    }

    private void showDialogDelete() {
        this.event.logEvent("DETAIL_DOC_CLICK_DELETE_DIALOG", null);
        String title = this.getString(R.string.all_title_discard,
                this.getResources().getQuantityString(R.plurals.count_page, this.listSelectItem.size(), this.listSelectItem.size()));

        String message = this.getString(R.string.all_messege_delete,
                this.getResources().getQuantityString(R.plurals.count_page, this.listSelectItem.size(), this.listSelectItem.size()));

        CustomisedDialog dialog = new CustomisedDialog(this)
                .setButtonAllowText(R.string.home_accept_delete)
                .setButtonCancelText(R.string.home_deni_delete)
                .setCancelable(false)
                .setTitle(title)
                .setMessage(message);
        dialog.setListener(new CustomisedDialog.DialogOnClickListener() {
            @Override
            public void onCancel() {
                dialog.dimiss();
                resetStatus();
            }

            @Override
            public void onAccept() {
                dialog.dimiss();
                showOverlay();
                pageViewModel.multipleDelete(listData);
            }
        });
        dialog.show();
    }

    public void onClickDragPage(View view) {
        if (this.isRenaming()) {
            return;
        }
        this.isDragPage = !this.isDragPage;
        Toast.makeText(this, this.isDragPage ? R.string.detail_document_drag_enable : R.string.detail_document_drag_disable, Toast.LENGTH_SHORT).show();
        this.mAdapter.setDragPage(this.isDragPage);
        this.setListenerDragPage(this.isDragPage);
        this.binding.ivDragPage.setSelected(this.isDragPage);
    }

    @SuppressLint("ClickableViewAccessibility")
    public void setListenerDragPage(boolean isDragPage) {
        this.itemTouchHelper.attachToRecyclerView(isDragPage ? this.binding.rvDetailShow : null);
        this.binding.rvDetailShow.setOnTouchListener((view, motionEvent) -> {
            if (!isDragPage) {
                return false;
            }

            if (motionEvent.getAction() == 1 && this.isMoving && (this.positionFirstMove != this.positionLastMove)) {
                this.binding.ilOverlay.setVisibility(View.VISIBLE);

                for (int i = 0; i < listData.size(); i++) {
                    this.listData.get(i).setPosition(i + 1);
                }

                this.pageViewModel.multipleUpdate(this.listData);
            }
            return false;
        });
    }

    public void showHideDragPageView() {
        if (this.isMultipleSelect) {
            this.binding.ivDragPage.setVisibility(View.GONE);
            return;
        }
        this.binding.ivDragPage.setVisibility(View.VISIBLE);
    }

    public void onClickSelect(View view) {
        this.event.logEvent("DETAIL_DOC_CLICK_SELECT", null);
        if (this.isRenaming()) {
            return;
        }

        this.isMultipleSelect = true;
        this.mAdapter.setMultipleSelect(true);
        this.statusSelect();
        this.setStatusQuickAction();
        this.showHideDragPageView();
    }

    public void onClickCamera(View view) {
        this.event.logEvent("DETAIL_DOC_CLICK_CAMERA", null);
        if (this.isRenaming()) {
            return;
        }

        Completable.create(emitter -> {
            try {
                DbUtils.clearPreviousSession(AppDatabases.getInstance(this));
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        if (presenter != null) {
                            presenter.addDisposable(d);
                        }
                    }

                    @Override
                    public void onComplete() {
                        appPref.setSaveId(documentItem.getId());
                        appPref.setSaveInDocument(true);
                        startActivity(new Intent(DetailDocumentActivity.this, CameraActivity.class));
                        finish();
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

    public void onClickSelectAll(View view) {
        this.event.logEvent("DETAIL_DOC_CLICK_SELECT_ALL", null);
        String title = this.binding.tvSelectAll.getText().toString();
        boolean isSelectAll = TextUtils.equals(title, this.getString(R.string.all_select));

        for (PageItem itemFile : this.listData) {
            itemFile.setSelected(isSelectAll);
        }

        this.listSelectItem.clear();

        if (isSelectAll) {
            for (int i = 0; i < this.listData.size(); i++) {
                this.listSelectItem.add(i);
            }
        }

        this.binding.tvSelectAll.setText(isSelectAll ? R.string.all_unselect : R.string.all_select);
        this.mAdapter.notifyDataSetChanged();
        this.setStatusQuickAction();
    }

    public void setSelectAll(boolean isSelectAll) {
        for (PageItem itemFile : this.listData) {
            itemFile.setSelected(isSelectAll);
        }

        if (!isSelectAll) {
            if (this.listSelectItem != null) {
                this.listSelectItem.clear();
            }
        }

        this.mAdapter.notifyDataSetChanged();
    }

    public void onClickImportImage(View view) {
        this.event.logEvent("DETAIL_DOC_CLICK_IMPORT", null);
        if (this.isRenaming()) {
            return;
        }
        this.actionClickImport = 0;
        DetailDocumentActivityPermissionsDispatcher.needsPermissionWithPermissionCheck(this);
    }

    public void onClickShareDoc(View view) {
        this.event.logEvent("DETAIL_DOC_CLICK_SHARE", null);
        if (this.isRenaming()) {
            return;
        }

        this.setSelectAll(true);

        if (!PermissionUtils.checkPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            DetailDocumentActivityPermissionsDispatcher.needsPermissionWithPermissionCheck(this);
            this.actionClickImport = 1;
            return;
        }

        if (this.presenter != null) {
            this.presenter.calculatorSize(this, this.listData);
        }
    }

    private void resetSelect() {
        if (this.listData == null) {
            return;
        }

        for (PageItem itemFile : this.listData) {
            if (itemFile == null) {
                continue;
            }
            itemFile.setSelected(false);
        }

        this.listSelectItem.clear();
        this.mAdapter.setMultipleSelect(false);
        this.binding.tvSelectAll.setText(R.string.all_select);
    }

    private void statusSelect() {
        this.binding.ilQuickAction.setVisibility(this.isMultipleSelect ? View.VISIBLE : View.GONE);
        this.binding.ilNavigationBottom.setVisibility(this.isMultipleSelect ? View.GONE : View.VISIBLE);
        this.binding.ivRename.setVisibility(this.isMultipleSelect ? View.GONE : View.VISIBLE);
        this.binding.tvSelectAll.setVisibility(this.isMultipleSelect ? View.VISIBLE : View.GONE);
        this.mAdapter.notifyDataSetChanged();
    }

    public void onClickShareItem(View view) {
        this.event.logEvent("DETAIL_DOC_CLICK_ITEM_SHARE", null);


        if (!PermissionUtils.checkPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            DetailDocumentActivityPermissionsDispatcher.needsPermissionWithPermissionCheck(this);
            this.actionClickImport = 1;
            return;
        }

        if (this.presenter != null) {
            this.presenter.calculatorSize(this, this.listData);
        }
    }

    public void onClickMoveItem(View view) {
        this.event.logEvent("DETAIL_DOC_CLICK_ITEM_MOVE", null);
        this.isMoveFile = true;

        if (this.presenter != null) {
            this.presenter.getListTargetDocument(this, this.documentItem.getId());
        }
    }

    public void onClickCopyItem(View view) {
        this.event.logEvent("DETAIL_DOC_CLICK_ITEM_COPY", null);
        this.isMoveFile = false;

        if (this.presenter != null) {
            this.presenter.getListTargetDocument(this, this.documentItem.getId());
        }
    }

    public void onClickDeleteItem(View view) {
        this.event.logEvent("DETAIL_DOC_CLICK_ITEM_DELETE", null);
        this.showDialogDelete();
    }

    public void statusTextSelect() {
        this.setStatusQuickAction();

        if (this.listSelectItem.size() == this.listData.size()) {
            this.binding.tvSelectAll.setText(R.string.all_unselect);
            return;
        }
        this.binding.tvSelectAll.setText(R.string.all_select);
    }

    private void setCopyStatus(boolean status) {
        this.docActionBinding.ivCopy.setImageResource(status ? R.drawable.detail_vector_copy_enable : R.drawable.detail_vector_copy);
        this.setGeneralStatus(this.docActionBinding.tvCopy, this.docActionBinding.llContainerCopy, status);
    }

    private void setSendToStatus(boolean status) {
        this.docActionBinding.ivSendTo.setImageResource(status ? R.drawable.home_vector_send_to_enable : R.drawable.home_vector_send_to);
        this.setGeneralStatus(this.docActionBinding.tvSendTo, this.docActionBinding.llContainerSend, status);
    }

    private void setDeleteStatus(boolean status) {
        this.docActionBinding.ivDelete.setImageResource(status ? R.drawable.home_vector_delete_enable : R.drawable.home_vector_delete);
        this.setGeneralStatus(this.docActionBinding.tvDelete, this.docActionBinding.llContainerDelete, status);
    }

    private void setMoveStatus(boolean status) {
        this.docActionBinding.ivMove.setImageResource(status ? R.drawable.home_vector_move_enable : R.drawable.home_vector_move);
        this.setGeneralStatus(this.docActionBinding.tvMove, this.docActionBinding.llContainerMove, status);
    }

    private void setGeneralStatus(@NonNull TextView text, @NonNull ViewGroup viewGroup, boolean status) {
        text.setTextColor(ActivityCompat.getColor(this, status ? R.color.color_text_orange : R.color.color_text_gray));
        viewGroup.setEnabled(status);
    }

    private void statusEditRename(boolean isShow) {
        this.binding.ivRename.setVisibility(isShow ? View.GONE : View.VISIBLE);
        this.binding.ivTickDone.setVisibility(isShow ? View.VISIBLE : View.GONE);
        this.binding.ivClean.setVisibility(isShow ? View.VISIBLE : View.GONE);
        this.binding.tvTitle.setVisibility(isShow ? View.GONE : View.VISIBLE);
        this.binding.etNameFolder.setVisibility(isShow ? View.VISIBLE : View.GONE);
        this.binding.etNameFolder.requestFocus();
        this.binding.etNameFolder.setText(this.binding.tvTitle.getText());
        this.binding.etNameFolder.setSelection(this.binding.tvTitle.getText().length());

        if (isShow) {
            AppUtils.showKeyboard(this);
        } else {
            AppUtils.closeKeyboard(this);
        }

        if (!isShow) {
            this.resetAllSelect();
            this.mAdapter.notifyDataSetChanged();
        }
    }

    private void setStatusQuickAction() {
        boolean isStatus = this.listSelectItem.size() != 0;
        this.setMoveStatus(isStatus);
        this.setCopyStatus(isStatus);
        this.setSendToStatus(isStatus);
        this.setDeleteStatus(isStatus);
    }

    private void resetAllSelect() {
        for (PageItem pageItem : this.listData) {
            if (pageItem == null) {
                continue;
            }
            pageItem.setSelected(false);
        }
    }

    private void makeToast(int resId) {
        Toast.makeText(this, this.getString(resId), Toast.LENGTH_SHORT).show();
    }

    public void onClickRename(View view) {
        this.event.logEvent("DETAIL_DOC_CLICK_RENAME", null);
        this.isRename = true;
        this.statusEditRename(true);
    }

    public void onClickTickRename(View view) {
        Editable text = this.binding.etNameFolder.getText();
        if (text == null || TextUtils.isEmpty(text)) {
            this.makeToast(R.string.home_folder_et_empty);
            return;
        }

        String allSpace = String.valueOf(text).replaceAll(" ", "");
        if (TextUtils.isEmpty(allSpace)) {
            this.makeToast(R.string.home_folder_et_empty);
            return;
        }

        final String documentName = text.toString();

        if (TextUtils.equals(documentName, this.documentItem.getName())) {
            this.isRename = false;
            this.statusEditRename(false);
            return;
        }

        if (FileUtils.isInvalidFDocName(documentName)) {
            this.makeToast(R.string.save_alert_input_invalid_name);
            return;
        }

        AppDatabases.getInstance(this)
                .documentDao()
                .isExistsDocumentName(documentName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Boolean>() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        if (presenter != null) {
                            presenter.addDisposable(d);
                        }
                    }

                    @Override
                    public void onSuccess(@io.reactivex.annotations.NonNull Boolean isExists) {
                        if (isExists) {
                            makeToast(R.string.home_folder_exits);
                        } else {
                            documentItem.setName(documentName);
                            pageViewModel.renameDocument(documentItem);
                            isRename = false;
                            statusEditRename(false);
                        }
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

    public void onClickRemoveName(View view) {
        this.event.logEvent("DETAIL_DOC_CLICK_CLEAR", null);
        this.binding.etNameFolder.setText("");
    }

    public void resetStatus() {
        this.isMultipleSelect = false;
        this.resetSelect();
        this.statusSelect();
        this.showHideDragPageView();
    }

    @Override
    public void onClickImageView(int position, boolean isSelect) {
        if (this.isRenaming()) {
            return;
        }

        if (!this.isMultipleSelect) {
            this.event.logEvent("DETAIL_DOC_CLICK_ITEM_OPEN", null);
            this.appPref.setSaveInDocument(true);
            this.appPref.setSaveId(this.documentItem.getId());

            Intent intent = new Intent(this, DetailPageActivity.class);
            intent.putExtra(Constants.EXTRA_POSITION_CURRENT_PAGE, position);
            intent.putExtra(Constants.EXTRA_DOCUMENT_ID, this.documentItem.getId());
            this.startActivity(intent);
            this.finish();
            return;
        }

        if (isSelect) {
            this.listSelectItem.add(position);
        } else {
            for (int i = 0; i < this.listSelectItem.size(); i++) {
                if (this.listSelectItem.get(i) == position) {
                    this.listSelectItem.remove(i);
                    break;
                }
            }
        }

        this.listData.get(position).setSelected(isSelect);
        this.mAdapter.notifyItemChanged(position);
        this.statusTextSelect();
        this.setStatusQuickAction();
    }

    public void hideOverlay() {
        this.binding.ilOverlay.setVisibility(View.GONE);
    }

    public void showOverlay() {
        this.binding.ilOverlay.setVisibility(View.VISIBLE);
    }

    private void saveStateChangeActivity() {
        this.appPref.setSaveId(this.documentItem.getId());
        this.appPref.setSaveInDocument(true);
        this.hideOverlay();
    }

    @Override
    public void onLongClickImageView(int position) {
        if (this.listData == null) {
            return;
        }

        if (position >= this.listData.size()) {
            return;
        }

        if (this.isRenaming()) {
            return;
        }

        if (this.isMultipleSelect) {
            this.resetStatus();
            return;
        }


        this.event.logEvent("DETAIL_DOC_ITEM_LONG_CLICK", null);
        this.isMultipleSelect = true;
        this.mAdapter.setMultipleSelect(true);
        this.listData.get(position).setSelected(true);
        this.listSelectItem.add(position);
        this.mAdapter.notifyDataSetChanged();
        this.statusSelect();
        this.setStatusQuickAction();
        this.statusTextSelect();
        this.showHideDragPageView();
    }

    @Override
    public void onShowTargetDocuments(@NotNull List<DocumentItem> listDocument) {
        listDocument.add(0, new DocumentItem());
        MoveFileDialog moveFileDialog = new MoveFileDialog(new ArrayList<>(listDocument));
        moveFileDialog.setTitle(this.isMoveFile ? R.string.all_move_file_title : R.string.all_copy_file_title);
        moveFileDialog.setListener(new MoveFileDialog.MoveFileDialogListener() {
            @Override
            public void onCreateFolder() {
                showCreateDocumentDialog();
            }

            @Override
            public void onClickItem(int position, BaseEntity baseEntity) {
                if (isMoveFile) {
                    movePage((DocumentItem) baseEntity);
                } else {
                    showOverlay();
                    copyPage((DocumentItem) baseEntity);
                }
                moveFileDialog.dismiss();
            }

            @Override
            public void onDismissDialog() {
            }
        });
        moveFileDialog.show(this.getSupportFragmentManager(), this.isMoveFile ? "MOVE_PAGE" : "COPY_PAGE");
    }

    @Override
    public void onCopyImageSucces(List<PageItem> listPage) {
        this.pageViewModel.multipleInsert(listPage);
        this.makeToast(R.string.all_detail_copy_complete);
        this.hideOverlay();
    }

    @Override
    public void onSucessCreateDocument(DocumentItem documentItem) {
        if (this.isMoveFile) {
            this.movePage(documentItem);
        } else {
            this.copyPage(documentItem);
        }
    }

    private void copyPage(DocumentItem documentItem) {
        List<PageItem> listMove = new ArrayList<>();
        for (int i = 0; i < this.listSelectItem.size(); i++) {
            PageItem pageItem = this.listData.get(this.listSelectItem.get(i));
            pageItem.setParentId(documentItem.getId());
            pageItem.setId(0);
            pageItem.setPosition(i + 1 + documentItem.getChildCount());
            listMove.add(pageItem);
        }

        if (this.presenter != null) {
            this.presenter.copyPages(this, listMove);
        }
    }

    private void movePage(DocumentItem documentItem) {
        List<PageItem> listMove = new ArrayList<>();

        for (int i = 0; i < this.listSelectItem.size(); i++) {
            PageItem pageItem = this.listData.get(this.listSelectItem.get(i));
            pageItem.setParentId(documentItem.getId());
            pageItem.setPosition(i + 1 + documentItem.getChildCount());
            listMove.add(pageItem);
        }

        this.listData.removeAll(listMove);

        for (int i = 0; i < this.listData.size(); i++) {
            PageItem pageItem = this.listData.get(i);
            pageItem.setPosition(i + 1);
            this.listData.set(i, pageItem);
        }

        this.listData.addAll(listMove);
        this.pageViewModel.multipleUpdate(this.listData);
    }

    @Override
    public void onSuccessShareToGallery(List<String> listFile) {
        if (this.shareDialogNew != null) {
            this.shareDialogNew.dismiss();
        }

        this.hideOverlay();
        this.setSelectAll(false);
        this.resetStatus();

        ShareSuccessDialog shareSuccessDialog = new ShareSuccessDialog(this, listFile, getString(R.string.jpg_success));
        shareSuccessDialog.show(getSupportFragmentManager(), "SUCCESS_JPG");

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (this.inAppReview != null) {
                this.inAppReview.showReview();
            }
        }, 500);
    }

    @Override
    public void onUpdateSizeFileSelect(String title) {
        this.showSendToDialog(this.documentItem.getName());
    }

    @Override
    public void onSingleImportSuccess(PageItem pageItem) {
        this.saveStateChangeActivity();
        Intent iDetect = new Intent(this, DetectActivity.class);
        iDetect.putExtra(Constants.EXTRA_PAGE_ITEM, pageItem);
        this.startActivity(iDetect);
    }

    @Override
    public void onMultipleImportSuccess() {
        this.saveStateChangeActivity();
        this.startActivity(new Intent(this, SaveActivity.class));
    }

    @Override
    public void onSuccessSavePdf(List<String> listPdf) {
        if (this.shareDialogNew != null) {
            this.shareDialogNew.dismiss();
        }

        this.hideOverlay();
        this.setSelectAll(false);
        this.resetStatus();
        ShareSuccessDialog shareSuccessDialog = new ShareSuccessDialog(this, listPdf, getString(R.string.pdf_success));
        shareSuccessDialog.show(getSupportFragmentManager(), "SHARE_SUCCESS");
    }

    @Override
    public void onSuccessSharePdf() {
        if (this.shareDialogNew != null) {
            this.shareDialogNew.dismiss();
        }

        this.hideOverlay();
        this.setSelectAll(false);
        this.resetStatus();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.CODE_PERMISSION_STORAGE) {
            if (this.actionClickImport == 0) {
                if (PermissionUtils.checkPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    FileUtils.startImportImage(this);
                    return;
                }

                DetailDocumentActivityPermissionsDispatcher.needsPermissionWithPermissionCheck(this);
            }
            return;
        }

        if (requestCode == Constants.SHARE_REQUEST_CODE) {
            if (this.inAppReview != null) {
                this.inAppReview.showReview();
            }
            return;
        }

        if (resultCode != RESULT_OK || data == null) {
            this.hideOverlay();
            return;
        }

        if (requestCode == Constants.CODE_PICK_IMAGE_INTENT) {
            if (this.presenter != null) {
                this.presenter.insertTempPage(this, data);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (this.binding.ilOverlay.getVisibility() == View.VISIBLE) {
            this.makeToast(R.string.all_alert_file_processing);
            return;
        }

        if (this.isRename) {
            this.isRename = false;
            this.statusEditRename(false);
            return;
        }

        if (this.isMultipleSelect) {
            this.resetStatus();
            return;
        }

        super.onBackPressed();
    }

    @OnPermissionDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void showDenied() {
    }

    @OnNeverAskAgain(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void showNeverAskForStorage() {
        this.showDialogPermission();
    }

    public void showDialogPermission() {
        CustomisedDialog dialog = new CustomisedDialog(this)
                .setCancelable(false)
                .setMessage(this.getString(R.string.storage_request_permission_message, "Storage", "Storage", "Storage", "Storage"))
                .setTitle(R.string.storage_request_permission_title)
                .setButtonAllowText(R.string.button_allow_text)
                .setButtonCancelText(R.string.all_cancel);
        dialog.setListener(new CustomisedDialog.DialogOnClickListener() {
            @Override
            public void onCancel() {
                dialog.dimiss();
            }

            @Override
            public void onAccept() {
                dialog.dimiss();
                PermissionUtils.startSettingsPermissionStorage(DetailDocumentActivity.this);
            }
        });
        dialog.show();
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void needsPermission() {
        if (this.actionClickImport == 0) {
            this.showOverlay();
            FileUtils.startImportImage(this);
            return;
        }

        if (this.actionClickImport == 1) {
            this.actionClickImport = 0;
            if (this.presenter != null) {
                this.presenter.calculatorSize(this, this.listData);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        DetailDocumentActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @Override
    protected void onDestroy() {
        if (this.presenter != null) {
            this.presenter.disposableAll();
        }

        if (this.nativeAds != null) {
            this.nativeAds.destroyAds();
        }

        if (this.unifiedNativeAd != null) {
            this.unifiedNativeAd.destroy();
        }

        System.gc();
        super.onDestroy();
    }

    @Override
    public void onReviewComplete() {
    }

    @Override
    public void onLoaded(List<Object> listAds) {
        if (listAds != null && listAds.get(0) != null) {
            this.binding.llAdsContainer.removeAllViews();
            this.unifiedNativeAd = (UnifiedNativeAd) listAds.get(0);
            UnifiedNativeAdView adView = new AdViewType(this).getAdViewList(this.unifiedNativeAd);
            this.binding.llAdsContainer.addView(adView);
        }
    }

}
