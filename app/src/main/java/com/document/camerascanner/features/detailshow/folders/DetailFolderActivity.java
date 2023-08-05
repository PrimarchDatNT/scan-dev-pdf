package com.document.camerascanner.features.detailshow.folders;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.document.camerascanner.R;
import com.document.camerascanner.ads.AdConstant;
import com.document.camerascanner.ads.AdViewType;
import com.document.camerascanner.ads.NativeAds;
import com.document.camerascanner.databases.model.DocumentItem;
import com.document.camerascanner.databases.model.FolderItem;
import com.document.camerascanner.databinding.ActivityDetailShowImagesBinding;
import com.document.camerascanner.databinding.DialogHomeMoveFileBinding;
import com.document.camerascanner.databinding.ViewDetailNavigationBottomBinding;
import com.document.camerascanner.databinding.ViewMainQuickActionBottomBinding;
import com.document.camerascanner.features.detailshow.adapter.MoveDocumentAdapter;
import com.document.camerascanner.features.detailshow.documents.DetailDocumentActivity;
import com.document.camerascanner.features.main.TypeSort;
import com.document.camerascanner.features.main.dialog.FileDialog;
import com.document.camerascanner.features.pdfsettings.PdfSettingsActivity;
import com.document.camerascanner.features.review.SupportInAppReview;
import com.document.camerascanner.features.share.ShareDialogNew;
import com.document.camerascanner.features.share.ShareSuccessDialog;
import com.document.camerascanner.features.view.CustomisedDialog;
import com.document.camerascanner.prefs.AppPref;
import com.document.camerascanner.utils.AppUtils;
import com.document.camerascanner.utils.Constants;
import com.document.camerascanner.utils.FileUtils;
import com.document.camerascanner.utils.PermissionUtils;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAdView;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class DetailFolderActivity extends AppCompatActivity implements DetailFolderAdapter.OnItemClickListener, DetailFolder.View,
        MoveDocumentAdapter.CallBackSelectFolder, SupportInAppReview.OnReviewListener, NativeAds.CallBackNativeAd {

    private boolean isRename = false;
    private boolean isLongClick = false;
    private boolean isLoadAdSuccess = false;
    private boolean isLoadDataSuccess = false;
    private boolean isImporting;
    private int countItemSelect = 0;
    //0: import img. 1: share gallery
    private int actionClickImport = 0;

    private NativeAds nativeAds;
    private FirebaseAnalytics event;
    private List<DocumentItem> listData;
    private List<FolderItem> listAllFolder;
    private UnifiedNativeAd unifiedNativeAd;
    private ShareDialogNew shareDialogNew;
    private AlertDialog moveFileDialog;
    private DetailFolderAdapter mAdapter;
    private DetailFolder.Presenter detailPresenter;
    private SupportInAppReview inAppReview;
    private ActivityDetailShowImagesBinding binding;
    private ViewDetailNavigationBottomBinding naviagtionBinding;
    private ViewMainQuickActionBottomBinding quickActionBinding;
    private FolderItem folderItemRoot;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.init();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (this.actionClickImport == 1) {
            this.actionClickImport = 0;
            return;
        }

        if (this.isLoadDataSuccess) {
            this.detailPresenter.getFolderChildren(this, this.folderItemRoot);
        }
    }

    private void init() {
        this.initView();
        this.initData();
        this.initAds();
    }

    private void initData() {
        AppPref appPref = AppPref.getInstance(this);

        Intent intent = this.getIntent();
        int idFolder = -1;
        if (intent != null) {
            idFolder = intent.getIntExtra(Constants.EXTRA_FOLDER_ID, -1);
        }

        if (idFolder == -1) {
            idFolder = appPref.getSaveId();
            appPref.removePref();
        }

        if (idFolder == -1) {
            this.finish();
        }

        this.listData = new ArrayList<>();
        this.listAllFolder = new ArrayList<>();
        this.detailPresenter = new DetailFolderPresenter(this);
        this.inAppReview = new SupportInAppReview(this, this);

        this.detailPresenter.getFolderInfo(this, idFolder);
    }

    private void initView() {
        this.event = FirebaseAnalytics.getInstance(this);
        this.event.logEvent("DETAIL_FOLDER_OPEN", null);
        this.binding = ActivityDetailShowImagesBinding.inflate(this.getLayoutInflater());
        this.setContentView(this.binding.getRoot());
        this.naviagtionBinding = ViewDetailNavigationBottomBinding.bind(this.binding.ilNavigationBottom);
        this.quickActionBinding = ViewMainQuickActionBottomBinding.bind(this.binding.ilQuickActionFolder);
        this.mAdapter = new DetailFolderAdapter(this);
        this.mAdapter.setListener(this);
        this.binding.rvDetailShow.setLayoutManager(new GridLayoutManager(this, 3));
        this.binding.rvDetailShow.setAdapter(this.mAdapter);
        this.binding.ivDragPage.setImageResource(R.drawable.home_vector_sort);
        this.binding.ivDragPage.setPadding(50, 50, 50, 50);
    }

    private void initAds() {
        this.nativeAds = new NativeAds(this, AdConstant.NT_DETAIL_FOLDER, this, AdConstant.DETAIL_FOLDER);
        this.nativeAds.loadAds();
    }

    @Override
    public void onSuccessNameFolderDefault(String folderName) {
        this.showCreateFolderDialog(folderName);
    }

    @Override
    public void onGetFolderInfoSuccess(FolderItem folderItem) {
        this.folderItemRoot = folderItem;
        this.binding.tvTitle.setText(this.folderItemRoot.getName());
        this.detailPresenter.getFolderChildren(this, this.folderItemRoot);
    }

    public void onClickBackPress(View view) {
        this.onBackPressed();
    }

    private void showDialogDelete() {
        String title = !this.isLongClick ? this.getString(R.string.all_title_single_discard) : this.getString(R.string.all_title_discard,
                this.getResources().getQuantityString(R.plurals.count_document, this.countItemSelect, this.countItemSelect));

        String message = !this.isLongClick ? this.getString(R.string.all_message_single_delete) : this.getString(R.string.all_messege_delete,
                this.getResources().getQuantityString(R.plurals.count_document, this.countItemSelect, this.countItemSelect));

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
                binding.ilOverlay.setVisibility(View.VISIBLE);
                if (detailPresenter != null) {
                    detailPresenter.deleteDocuments(DetailFolderActivity.this);
                }
            }
        });
        dialog.show();
    }

    private void showMergeDialog(String title, String pathFile) {
        AppUtils.showKeyboard(this);

        FileDialog dialog = new FileDialog(this)
                .setCancelable(false)
                .setShowPresent(true)
                .setImgPreseter(pathFile)
                .setTitle(R.string.home_merge)
                .setContent(title);
        dialog.setListener(new FileDialog.FileDialogListener() {
            @Override
            public void onClickCancel() {
                dialog.dimiss();
                AppUtils.closeKeyboard(DetailFolderActivity.this);
            }

            @Override
            public void onClickConfirm() {
                showOverlay();
                detailPresenter.generalMergeDocuments(DetailFolderActivity.this, dialog.getInputContent(), folderItemRoot.getId());
                dialog.dimiss();
                AppUtils.closeKeyboard(DetailFolderActivity.this);

            }

            @Override
            public void onDismissDialog() {
            }
        });
        dialog.getEdittext().setSelection(title.length());
        dialog.show();
    }

    private void showDialogMoveFile() {
        this.event.logEvent("DETAIL_FOLDER_SHOW_MOVE_DIALOG", null);

        DialogHomeMoveFileBinding dialogBinding = DialogHomeMoveFileBinding.inflate(LayoutInflater.from(this));
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setView(dialogBinding.getRoot());
        this.moveFileDialog = builder.show();
        MoveDocumentAdapter moveFileAdapter = new MoveDocumentAdapter(this, this.listAllFolder);
        moveFileAdapter.setCallBackSelectFolder(this);
        dialogBinding.rvFolderMove.setLayoutManager(new LinearLayoutManager(this));
        dialogBinding.rvFolderMove.setAdapter(moveFileAdapter);

        if (this.moveFileDialog.getWindow() == null) {
            return;
        }

        WindowManager.LayoutParams layoutParam = this.getLayoutParam(this.moveFileDialog);
        if (layoutParam != null) {
            this.moveFileDialog.getWindow().setAttributes(layoutParam);
        }

        this.moveFileDialog.setOnDismissListener(dialogInterface -> {
            if (!this.isLongClick) {
                this.resetStatus();
            }
        });
    }

    private void showShareDialog(String title) {
        this.event.logEvent("DETAIL_FOLDER_SHOW_SHARE_DIALOG", null);
        this.shareDialogNew = new ShareDialogNew(this, title);
        this.shareDialogNew.setListener(new ShareDialogNew.DialogShareListener() {
            @Override
            public void onClickShareJpg() {
                event.logEvent("DETAIL_FOLDER_SHARE_IMAGE", null);
                if (listData != null) {
                    detailPresenter.shareJpg(DetailFolderActivity.this);
                }
            }

            @Override
            public void onClickSharePdf() {
                if (detailPresenter != null) {
                    showOverlay();
                    detailPresenter.onGeneralSaveAndSharePdf(DetailFolderActivity.this, shareDialogNew.getTitle(), shareDialogNew.getQuality(), false);
                }
            }

            @Override
            public void onClickSavePdf() {
                if (detailPresenter != null) {
                    showOverlay();
                    detailPresenter.onGeneralSaveAndSharePdf(DetailFolderActivity.this, shareDialogNew.getTitle(), shareDialogNew.getQuality(), true);
                }
            }

            @Override
            public void onClickSaveJpg() {
                event.logEvent("DETAIL_FOLDER_SHARE_GALLERY", null);
                if (listData != null) {
                    showOverlay();
                    detailPresenter.shareJpgGallery(DetailFolderActivity.this, shareDialogNew.getTitle(), shareDialogNew.getQuality());
                }
                shareDialogNew.dismiss();
            }

            @Override
            public void onClickToPdfSettings() {
                event.logEvent("DETAIL_FOLDER_SHARE_PDF", null);
//                if (listData != null) {
//                    detailPresenter.sharePdf(DetailFolderActivity.this);
//                }
                actionClickImport = 1;
                startActivity(new Intent(DetailFolderActivity.this, PdfSettingsActivity.class));
            }

            @Override
            public void onDismissDialog() {
                if (!isLongClick) {
                    resetStatusAll();
                }
            }
        });
        this.shareDialogNew.show(getSupportFragmentManager(), "SHARE");
    }

    private void showDialogRenameDocument(@NonNull DocumentItem documentItem) {
        AppUtils.showKeyboard(this);
        FileDialog dialog = new FileDialog(this)
                .setCancelable(false)
                .setShowPresent(false)
                .setTitle(R.string.home_rename)
                .setContent(documentItem.getName());
        dialog.setListener(new FileDialog.FileDialogListener() {
            @Override
            public void onClickCancel() {
                dialog.dimiss();
                AppUtils.closeKeyboard(DetailFolderActivity.this);
            }

            @Override
            public void onClickConfirm() {
                if (detailPresenter != null) {
                    detailPresenter.renameDocument(DetailFolderActivity.this, dialog.getEdittext().getText().toString(), documentItem);
                }
                dialog.dimiss();
                AppUtils.closeKeyboard(DetailFolderActivity.this);
            }

            @Override
            public void onDismissDialog() {
            }
        });
        dialog.getEdittext().setSelection(documentItem.getName().length());
        dialog.show();
    }

    private WindowManager.LayoutParams getLayoutParam(@NonNull AlertDialog alertDialog) {
        if (alertDialog.getWindow() == null) {
            return null;
        }
        WindowManager.LayoutParams layoutParams = alertDialog.getWindow().getAttributes();
        layoutParams.gravity = Gravity.BOTTOM;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        alertDialog.getWindow().setBackgroundDrawable(null);
        return layoutParams;
    }

    private void showOverlay() {
        this.binding.ilOverlay.setVisibility(View.VISIBLE);
    }

    private void hideOverlay() {
        if (!this.isImporting) {
            this.binding.ilOverlay.setVisibility(View.GONE);
        }
    }

    private void makeToast(int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
    }

    private void resetStatusAll() {
        if (this.listData == null) {
            return;
        }

        for (DocumentItem listDocument : this.listData) {
            listDocument.setSelected(false);
        }
    }

    private void selectAllList() {
        if (this.listData == null) {
            return;
        }

        for (DocumentItem listDocument : this.listData) {
            listDocument.setSelected(true);
        }
        this.detailPresenter.setListHmDocumentSelect(this.listData);
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
            this.countItemSelect = 0;
            this.resetStatusAll();
            this.mAdapter.notifyDataSetChanged();
        }
    }

    private void hideDialogMove() {
        if (this.moveFileDialog != null) {
            this.moveFileDialog.dismiss();
        }
    }

    public boolean isRenaming() {
        if (this.isRename) {
            this.makeToast(R.string.detail_renaming);
            return true;
        }
        return false;
    }

    @SuppressLint("NonConstantResourceId")
    public void onClickDragPage(View view) {
        if (this.isRenaming()) {
            return;
        }
        PopupMenu popupMenu = new PopupMenu(this, this.binding.ivDragPage);
        popupMenu.inflate(R.menu.menu_sort);
        popupMenu.setOnMenuItemClickListener(menuItem -> {
            if (this.detailPresenter == null) {
                return false;
            }

            switch (menuItem.getItemId()) {
                case R.id.menu_sort_name:
                    this.detailPresenter.sortDocument(this, this.listData, TypeSort.SORT_NAME);
                    break;

                case R.id.menu_sort_data_created:
                    this.detailPresenter.sortDocument(this, this.listData, TypeSort.SORT_DATE);
                    break;

                case R.id.menu_sort_size:
                    this.detailPresenter.sortDocument(this, this.listData, TypeSort.SORT_DATA);
                    break;
            }
            return false;
        });
        popupMenu.show();
    }

    public void onClickImportImage(View view) {
        this.event.logEvent("DETAIL_FOLDER_CLICK_IMPORT", null);
        if (this.isRenaming()) {

            return;
        }
        this.actionClickImport = 0;
        DetailFolderActivityPermissionsDispatcher.needsPermissionWithPermissionCheck(this);
    }

    public void onClickShareDoc(View view) {
        this.event.logEvent("DETAIL_FOLDER_CLICK_SHARE", null);
        if (this.isRenaming()) {
            return;
        }

        if (this.listData.isEmpty()) {
            return;
        }

        this.selectAllList();

        if (!PermissionUtils.checkPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            DetailFolderActivityPermissionsDispatcher.needsPermissionWithPermissionCheck(this);
            this.actionClickImport = 1;
            return;
        }

        if (this.detailPresenter != null) {
            this.detailPresenter.calculatorSize(this);
        }
    }

    public void onClickSelect(View view) {
        this.event.logEvent("DETAIL_FOLDER_CLICK_SELECT", null);
        if (this.isRenaming()) {
            return;
        }

        if (this.listData.isEmpty()) {
            return;
        }

        this.isLongClick = true;
        this.mAdapter.setSelect(true);
        this.detailPresenter.resetHmDocumentSelect();
        this.statusSelect();
        this.setStatusQuickAction();
    }

    public void onClickCamera(View view) {
        this.event.logEvent("DETAIL_FOLDER_CLICK_CAMERA", null);
        if (this.isRenaming()) {
            return;
        }
        this.detailPresenter.startCamera(this, this.folderItemRoot);
    }

    public void onClickRename(View view) {
        this.event.logEvent("DETAIL_FOLDER_CLICK_RENAME", null);
        this.isRename = true;
        this.statusEditRename(true);
    }

    public void onClickTickRename(View view) {
        Editable text = this.binding.etNameFolder.getText();
        if (text == null || TextUtils.isEmpty(text)) {
            this.makeToast(R.string.home_folder_et_empty);
            return;
        }

        if (this.detailPresenter != null) {
            this.detailPresenter.renameFolder(this, this.folderItemRoot, text.toString());
        }
    }

    public void onClickRemoveName(View view) {
        this.binding.etNameFolder.setText("");
    }

    public void onClickShareItem(View view) {
        this.event.logEvent("DETAIL_FOLDER_CLICK_ITEM_SHARE", null);
        if (this.listData == null) {
            return;
        }

        if (!PermissionUtils.checkPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            DetailFolderActivityPermissionsDispatcher.needsPermissionWithPermissionCheck(this);
            this.actionClickImport = 1;
            return;
        }

        if (this.detailPresenter != null) {
            this.detailPresenter.calculatorSize(this);
        }
    }

    public void onClickDeleteItem(View view) {
        this.event.logEvent("DETAIL_FOLDER_CLICK_ITEM_DELETE", null);
        this.showDialogDelete();
    }

    public void onClickMoveItem(View view) {
        this.event.logEvent("DETAIL_FOLDER_CLICK_ITEM_MOVE", null);
        if (this.detailPresenter != null) {
            this.detailPresenter.showDialogMove(this, this.folderItemRoot);
        }
    }

    public void onClickMergeItem(View view) {
        this.event.logEvent("DETAIL_FOLDER_CLICK_ITEM_MERGE", null);
        if (this.listData == null) {
            return;
        }

        if (this.detailPresenter != null) {
            this.detailPresenter.showDialogMerge(this);
        }
    }

    public void onClickSelectAll(View view) {
        this.event.logEvent("DETAIL_FOLDER_CLICK_SELECT_ALL", null);
        if (this.listData == null) {
            return;
        }

        String title = this.binding.tvSelectAll.getText().toString();
        boolean isSelectAll = TextUtils.equals(title, this.getString(R.string.all_select));
        for (DocumentItem itemFile : this.listData) {
            itemFile.setSelected(isSelectAll);
        }

        this.countItemSelect = isSelectAll ? this.listData.size() : 0;
        if (isSelectAll) {
            this.detailPresenter.setListHmDocumentSelect(this.listData);
        } else {
            this.detailPresenter.resetHmDocumentSelect();
        }
        this.binding.tvSelectAll.setText(isSelectAll ? R.string.all_unselect : R.string.all_select);
        this.mAdapter.notifyDataSetChanged();
        this.setStatusQuickAction();
    }

    private void resetSelect() {
        if (this.listData == null) {
            return;
        }

        for (DocumentItem itemFile : this.listData) {
            itemFile.setSelected(false);
        }

        this.detailPresenter.resetHmDocumentSelect();
        this.countItemSelect = 0;
        this.mAdapter.setSelect(false);
        this.binding.tvSelectAll.setText(R.string.all_select);
        this.mAdapter.notifyDataSetChanged();
    }

    private void statusSelect() {
        boolean isSelect = this.mAdapter.isSelect();
        this.binding.ilQuickActionFolder.setVisibility(isSelect ? View.VISIBLE : View.GONE);
        this.binding.ilNavigationBottom.setVisibility(isSelect ? View.GONE : View.VISIBLE);
        this.binding.ivRename.setVisibility(isSelect ? View.GONE : View.VISIBLE);
        this.binding.tvSelectAll.setVisibility(isSelect ? View.VISIBLE : View.GONE);
        this.binding.ivDragPage.setVisibility(isSelect ? View.GONE : View.VISIBLE);
        this.mAdapter.notifyDataSetChanged();
    }

    private void setMergeStatus(boolean status) {
        this.quickActionBinding.ivMerg.setImageResource(status ? R.drawable.home_vector_merge_enable : R.drawable.home_vector_merge);
        this.setGeneralStatus(this.quickActionBinding.tvMerga, this.quickActionBinding.llContainerMerge, status);
    }

    private void setSendToStatus(boolean status) {
        this.quickActionBinding.ivSendTo.setImageResource(status ? R.drawable.home_vector_send_to_enable : R.drawable.home_vector_send_to);
        this.setGeneralStatus(this.quickActionBinding.tvSendTo, this.quickActionBinding.llContainerSend, status);
    }

    private void setDeleteStatus(boolean status) {
        this.quickActionBinding.ivDelete.setImageResource(status ? R.drawable.home_vector_delete_enable : R.drawable.home_vector_delete);
        this.setGeneralStatus(this.quickActionBinding.tvDelete, this.quickActionBinding.llContainerDelete, status);
    }

    private void setMoveStatus(boolean status) {
        this.quickActionBinding.ivMove.setImageResource(status ? R.drawable.home_vector_move_enable : R.drawable.home_vector_move);
        this.setGeneralStatus(this.quickActionBinding.tvMove, this.quickActionBinding.llContainerMove, status);
    }

    private void setGeneralStatus(@NotNull TextView text, @NotNull ViewGroup viewGroup, boolean status) {
        text.setTextColor(ActivityCompat.getColor(this, status ? R.color.color_text_orange : R.color.color_text_gray));
        viewGroup.setEnabled(status);
    }

    private void setStatusQuickAction() {
        if (this.countItemSelect == 0) {
            this.updateStatus(false, false, true);
            return;
        }

        if (this.countItemSelect == 1) {
            this.updateStatus(true, false, true);
            return;
        }

        this.updateStatus(true, true, false);
    }

    private void updateStatus(boolean isMove, boolean isMerge, boolean isRename) {
        this.setMoveStatus(isMove);
        this.setMergeStatus(isMerge);
        this.setSendToStatus(isMove);
        this.setDeleteStatus(isMove);
        this.setStatusShowIcRename(isRename);
    }

    public void setStatusShowIcRename(boolean isShow) {
        this.mAdapter.setClickItems(isShow);
    }

    public void statusTextSelect(boolean isSelect) {
        this.countItemSelect = isSelect ? this.countItemSelect + 1 : this.countItemSelect - 1;
        if (this.listData == null) {
            return;
        }

        this.setStatusQuickAction();

        if (this.countItemSelect == this.listData.size()) {
            this.binding.tvSelectAll.setText(R.string.all_unselect);
            return;
        }

        this.binding.tvSelectAll.setText(R.string.all_select);
    }

    public void resetStatus() {
        this.isLongClick = false;
        this.resetSelect();
        this.statusSelect();
        this.binding.ivDragPage.setVisibility(View.VISIBLE);
    }

    private void getListData() {
        this.listData = new ArrayList<>();
        if (this.detailPresenter == null) {
            return;
        }
        this.detailPresenter.getFolderChildren(this, this.folderItemRoot);
    }

    private void showCreateFolderDialog(String folderName) {
        this.event.logEvent("DETAIL_FOLDER_CLICK_CREATE_FOLDER_DIALOG", null);
        FileDialog dialog = new FileDialog(this)
                .setCancelable(false)
                .setShowPresent(false)
                .setContent(folderName)
                .setTitle(R.string.home_created_folder);
        dialog.setListener(new FileDialog.FileDialogListener() {
            @Override
            public void onClickCancel() {
                AppUtils.closeKeyboard(DetailFolderActivity.this);
                dialog.dimiss();
            }

            @Override
            public void onClickConfirm() {
                if (detailPresenter != null) {
                    detailPresenter.newFolder(DetailFolderActivity.this, dialog.getEdittext().getText().toString());
                }

                AppUtils.closeKeyboard(DetailFolderActivity.this);
                dialog.dimiss();
            }

            @Override
            public void onDismissDialog() {
                AppUtils.closeKeyboard(DetailFolderActivity.this);
            }
        });

        dialog.getEdittext().requestFocus();
        AppUtils.showKeyboard(this);
        dialog.show();
    }

    public void hideDialogShare() {
        if (this.shareDialogNew != null) {
            this.shareDialogNew.dismiss();
        }
    }

    @Override
    public void onItemClick(int position, boolean isSelect) {
        if (this.isRenaming()) {
            return;
        }

        if (this.listData == null || position >= this.listData.size()) {
            return;
        }

        if (!this.isLongClick) {
            event.logEvent("DETAIL_FOLDER_CLICK_ITEM_OPEN", null);
            Intent intent = new Intent(this, DetailDocumentActivity.class);
            intent.putExtra(Constants.EXTRA_DOCUMENT_ID, this.listData.get(position).getId());
            this.startActivity(intent);
            return;
        }

        this.listData.get(position).setSelected(isSelect);

        if (this.detailPresenter != null) {
            this.detailPresenter.setItemToHmDocumentItem(this.listData.get(position));
        }

        this.mAdapter.notifyDataSetChanged();
        this.statusTextSelect(isSelect);
    }

    @Override
    public void onItemLongClick(int position) {
        if (this.isRenaming()) {
            return;
        }

        if (this.listData == null || position >= this.listData.size()) {
            return;
        }

        if (this.isLongClick) {
            this.resetStatus();
            return;
        }

        this.event.logEvent("DETAIL_FOLDER_ITEM_LONG_CLICK", null);
        this.isLongClick = true;
        this.mAdapter.setSelect(true);
        this.listData.get(position).setSelected(true);

        if (this.detailPresenter != null) {
            this.detailPresenter.resetHmDocumentSelect();
            this.detailPresenter.setItemToHmDocumentItem(this.listData.get(position));
        }

        this.mAdapter.notifyDataSetChanged();
        this.statusSelect();
        this.setStatusQuickAction();
        this.statusTextSelect(true);
    }

    @Override
    public void onClickRename(DocumentItem documentItem) {
        if (this.isRenaming()) {
            return;
        }
        this.showDialogRenameDocument(documentItem);
    }

    @Override
    public void onDelete(int position) {
        if (this.listData == null || position >= this.listData.size()) {
            return;
        }

        if (this.isRenaming()) {
            return;
        }

        this.listData.get(position).setSelected(true);

        if (this.detailPresenter != null) {
            this.detailPresenter.resetHmDocumentSelect();
            this.detailPresenter.setItemToHmDocumentItem(this.listData.get(position));
        }

        this.showDialogDelete();
    }

    @Override
    public void onShareFolder(int position) {
        if (this.listData == null || position >= this.listData.size()) {
            return;
        }

        if (this.isRenaming()) {
            return;
        }

        if (!PermissionUtils.checkPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            DetailFolderActivityPermissionsDispatcher.needsPermissionWithPermissionCheck(this);
            this.actionClickImport = 1;
            return;
        }

        this.listData.get(position).setSelected(true);

        if (this.detailPresenter != null) {
            this.detailPresenter.resetHmDocumentSelect();
            this.detailPresenter.setItemToHmDocumentItem(this.listData.get(position));
        }

        if (this.detailPresenter != null) {
            this.detailPresenter.calculatorSize(this);
        }
    }

    @Override
    public void onMoveFolder(int position) {
        if (this.listData == null || position >= this.listData.size()) {
            return;
        }

        if (this.isRenaming()) {
            return;
        }

        this.listData.get(position).setSelected(true);

        if (this.detailPresenter != null) {
            this.detailPresenter.resetHmDocumentSelect();
            this.detailPresenter.setItemToHmDocumentItem(this.listData.get(position));
        }

        if (this.detailPresenter != null) {
            this.detailPresenter.showDialogMove(this, this.folderItemRoot);
        }
    }

    @Override
    public void onShowListDocumentInFolder(List<DocumentItem> listDocuments) {
        if (listDocuments == null) {
            return;
        }

        this.naviagtionBinding.ivShare.setEnabled(true);
        this.hideOverlay();
        this.listData = new ArrayList<>(listDocuments);
        this.isLoadDataSuccess = true;
        this.mAdapter.setListItem(this.listData);

        if (this.isLoadAdSuccess) {
            this.showAds();
        }

        this.binding.tvPageCount.setText(this.getResources().getQuantityString(R.plurals.count_document,
                this.listData.size(), this.listData.size()));
        this.isLongClick = false;
        this.resetSelect();
        this.setStatusQuickAction();
        this.statusSelect();
    }

    @Override
    public void onShowSuccessDelete() {
        this.hideOverlay();
        this.getListData();
        this.resetStatus();
    }

    @Override
    public void onShowDialogMove(List<FolderItem> list) {
        if (list == null) {
            return;
        }

        this.listAllFolder = new ArrayList<>(list);
        this.showDialogMoveFile();
    }

    @Override
    public void onSuccessMove() {
        this.hideOverlay();
        this.getListData();
        this.resetStatus();
    }

    @Override
    public void onShowSize(String title) {
        this.showShareDialog(title);
    }

    @Override
    public void onShowRenameFolder(@NotNull FolderItem folderItem) {
        this.folderItemRoot = folderItem;
        this.binding.tvTitle.setText(folderItem.getName());
        this.isRename = false;
        this.statusEditRename(false);
        this.getListData();
    }

    @Override
    public void onShowRenameDocument() {
        this.getListData();
    }

    @Override
    public void onShowDialogMerge(String title, String pathRepresent) {
        this.event.logEvent("DETAIL_FOLDER_CLICK_MERGE_DIALOG", null);
        this.showMergeDialog(title, pathRepresent);
    }

    @Override
    public void onShowSuccessMerge() {
        this.hideOverlay();
        this.getListData();
        this.resetStatus();
    }

    @Override
    public void onSuccessSavePdf(List<String> listPdf) {
        this.hideDialogShare();
        this.hideOverlay();
        this.resetStatus();
        ShareSuccessDialog shareSuccessDialog = new ShareSuccessDialog(this, listPdf, getString(R.string.pdf_success));
        shareSuccessDialog.show(getSupportFragmentManager(), "SHARE_SUCCES");
    }

    @Override
    public void onSuccessSharePdf() {
        hideDialogShare();
        this.hideOverlay();
        this.resetStatus();
    }

    @Override
    public void onShowOnSuccessToGallery(List<String> listJpg) {
        if (this.shareDialogNew != null) {
            this.shareDialogNew.dismiss();
        }

        this.hideOverlay();
        this.resetStatus();

        ShareSuccessDialog shareSuccessDialog = new ShareSuccessDialog(this, listJpg, getString(R.string.jpg_success));
        shareSuccessDialog.show(getSupportFragmentManager(), "SUCCESS_JPG");

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (this.inAppReview != null) {
                this.inAppReview.showReview();
            }
        }, 500);
    }

    @Override
    public void onSuccessImport() {
        this.isImporting = false;
        this.hideOverlay();
    }

    @Override
    public void callBackSelectFolder(int position) {
        if (this.listAllFolder == null || this.listData == null) {
            return;
        }

        if (position >= this.listAllFolder.size()) {
            return;
        }

        this.showOverlay();

        if (this.detailPresenter != null) {
            this.detailPresenter.moveDocuments(this, this.listAllFolder.get(position));
        }

        this.hideDialogMove();
    }

    @Override
    public void callbackMoveOut(int position) {
        this.showOverlay();
        this.hideDialogMove();

        if (this.listData != null) {
            if (this.detailPresenter != null) {
                this.detailPresenter.moveOutDocuments(this);
            }
        }
    }

    @Override
    public void callbackNewFolder(int position) {
        this.hideDialogMove();
        if (detailPresenter != null) {
            detailPresenter.getFolderNameDefault(this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constants.CODE_PERMISSION_STORAGE) {
            if (this.actionClickImport == 0) {
                DetailFolderActivityPermissionsDispatcher.needsPermissionWithPermissionCheck(this);
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
            this.isImporting = false;
            this.hideOverlay();
            return;
        }

        if (requestCode == Constants.CODE_PICK_IMAGE_INTENT) {
            if (this.detailPresenter != null) {
                this.detailPresenter.getDataImportImage(this, data, this.folderItemRoot);
            }
        }
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
                .setMessage(getString(R.string.storage_request_permission_message, "Storage", "Storage", "Storage", "Storage"))
                .setTitle(R.string.storage_request_permission_title)
                .setCancelable(false)
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
                PermissionUtils.startSettingsPermissionStorage(DetailFolderActivity.this);
            }
        });
        dialog.show();
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void needsPermission() {
        if (this.actionClickImport == 0) {
            this.showOverlay();
            this.isImporting = true;
            FileUtils.startImportImage(this);
            return;
        }

        if (this.actionClickImport == 1 && this.detailPresenter != null) {
            this.detailPresenter.calculatorSize(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        DetailFolderActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
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

        if (this.isLongClick) {
            this.resetStatus();
            return;
        }

        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (this.detailPresenter != null) {
            this.detailPresenter.disposableAll();
        }

        if (this.unifiedNativeAd != null) {
            this.unifiedNativeAd.destroy();
        }

        if (this.nativeAds != null) {
            this.nativeAds.destroyAds();
        }

        System.gc();
        super.onDestroy();
    }

    @Override
    public void onReviewComplete() {
    }

    @Override
    public void onLoaded(List<Object> listAds) {
        this.isLoadAdSuccess = true;
        if (this.isLoadDataSuccess && !this.listData.isEmpty()) {
            if (listAds == null) {
                return;
            }

            this.unifiedNativeAd = (UnifiedNativeAd) listAds.get(0);
            this.showAds();
        }
    }

    private void showAds() {
        if (this.unifiedNativeAd == null) {
            return;
        }

        this.binding.llAdsContainer.removeAllViews();
        UnifiedNativeAdView adView = new AdViewType(this).getAdViewList(this.unifiedNativeAd);
        this.binding.llAdsContainer.addView(adView);
    }
}
