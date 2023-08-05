package com.document.camerascanner.features.main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.document.camerascanner.R;
import com.document.camerascanner.ads.AdConstant;
import com.document.camerascanner.ads.AdViewType;
import com.document.camerascanner.ads.FANInter;
import com.document.camerascanner.ads.NativeAds;
import com.document.camerascanner.databases.AppDatabases;
import com.document.camerascanner.databases.model.BaseEntity;
import com.document.camerascanner.databases.model.DocumentItem;
import com.document.camerascanner.databases.model.FolderItem;
import com.document.camerascanner.databases.model.PageItem;
import com.document.camerascanner.databinding.ActivityMainBinding;
import com.document.camerascanner.databinding.ViewMainNavigationBottomBinding;
import com.document.camerascanner.databinding.ViewMainQuickActionBottomBinding;
import com.document.camerascanner.databinding.ViewOverlayBinding;
import com.document.camerascanner.features.detailshow.documents.DetailDocumentActivity;
import com.document.camerascanner.features.detailshow.folders.DetailFolderActivity;
import com.document.camerascanner.features.detect.DetectActivity;
import com.document.camerascanner.features.intro.MainIntro;
import com.document.camerascanner.features.main.dialog.FileDialog;
import com.document.camerascanner.features.main.dialog.MoveFileDialog;
import com.document.camerascanner.features.main.interfaces.MPresenter;
import com.document.camerascanner.features.main.interfaces.MainView;
import com.document.camerascanner.features.pdfsettings.PdfSettingsActivity;
import com.document.camerascanner.features.review.SupportInAppReview;
import com.document.camerascanner.features.save.SaveActivity;
import com.document.camerascanner.features.settings.SettingsActivity;
import com.document.camerascanner.features.share.ShareDialogNew;
import com.document.camerascanner.features.share.ShareSuccessDialog;
import com.document.camerascanner.features.update.SupportInAppUpdate;
import com.document.camerascanner.features.update.UpdateDialog;
import com.document.camerascanner.features.view.CustomisedDialog;
import com.document.camerascanner.prefs.AppPref;
import com.document.camerascanner.prefs.ConstantsPrefs;
import com.document.camerascanner.utils.AppUtils;
import com.document.camerascanner.utils.Constants;
import com.document.camerascanner.utils.FileUtils;
import com.document.camerascanner.utils.PermissionUtils;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAdView;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.RuntimePermissions;


@RuntimePermissions
public class MainActivity extends AppCompatActivity implements MainView, FileAdapter.CallBackClickItem, NativeAds.CallBackNativeAd, TextWatcher {

    private boolean isShowActivity;
    private boolean isLongClick = false;
    private boolean isImporting = false;
    private boolean isShownSpotlight;
    private boolean isClickCreateFolder;
    private boolean isLoadAdSuccess = false;
    private boolean isLoadFolderSuccess = false;
    private boolean isLoadDocumentSuccess = false;
    private int countBackPress = 0;
    private int countTickFolder = 0;
    private int countTickDocument = 0;
    private int actionClickImport = 0;

    private AppPref appPref;
    private NativeAds nativeAds;
    private FileAdapter folderAdapter;
    private FileAdapter documentAdapter;
    private List<FolderItem> listDataFolder;
    private List<FolderItem> listDataFolderDefault;
    private List<DocumentItem> listDataDocument;
    private List<DocumentItem> listDataDocumentDefault;

    private FirebaseAnalytics event;
    private MPresenter mainPresenter;
    private UnifiedNativeAdView adView;
    private UnifiedNativeAd unifiedNativeAd;
    private ActivityMainBinding mainBinding;
    private ViewMainNavigationBottomBinding featureBinding;
    private ViewMainQuickActionBottomBinding quickActionBinding;

    private MainIntro mainIntro;
    private ShareDialogNew shareDialogNew;
    private SupportInAppReview inAppReview;
    private SupportInAppUpdate inAppUpdate;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.init();
    }

    private void init() {
        this.initConfig();
        this.initView();
        this.initAds();
        this.checkUpdate();
        this.initSpotLight();
    }

    private void initConfig() {
        this.appPref = AppPref.getInstance(this);
        this.event = FirebaseAnalytics.getInstance(this);
        this.event.logEvent("MAIN_OPEN", null);
        this.mainPresenter = new MainPresenter(this, this);
        this.listDataFolder = new ArrayList<>();
        this.listDataDocument = new ArrayList<>();
        this.listDataFolderDefault = new ArrayList<>();
        this.listDataDocumentDefault = new ArrayList<>();
        this.inAppReview = new SupportInAppReview(this, () -> System.out.println("Review succces"));
    }

    private void initView() {
        this.mainBinding = ActivityMainBinding.inflate(this.getLayoutInflater());
        this.setContentView(this.mainBinding.getRoot());
        this.featureBinding = ViewMainNavigationBottomBinding.bind(this.mainBinding.flMainFeature);
        this.quickActionBinding = ViewMainQuickActionBottomBinding.bind(this.mainBinding.flQuickAction);
        ViewOverlayBinding.bind(this.mainBinding.clLoading);

        this.mainBinding.etSearch.setOnClickListener(v -> this.event.logEvent("MAIN_CLICK_SEARCH_BAR", null));
        this.mainBinding.etSearch.addTextChangedListener(this);

        this.folderAdapter = new FileAdapter(this, new ArrayList<>());
        this.folderAdapter.setCallBackClickItem(this);
        this.mainBinding.rvFolder.setAdapter(this.folderAdapter);
        this.mainBinding.rvFolder.setLayoutManager(new LinearLayoutManager(this));

        this.documentAdapter = new FileAdapter(this, new ArrayList<>());
        this.documentAdapter.setCallBackClickItem(this);
        this.mainBinding.rvFiles.setAdapter(this.documentAdapter);
        this.setLayoutManage();
    }

    private void initAds() {
        this.nativeAds = new NativeAds(this, AdConstant.NT_MAIN, this, AdConstant.MAIN);
        this.nativeAds.loadAds();
    }

    private void initSpotLight() {
        if (!this.appPref.isShowSpotlight(ConstantsPrefs.KEY_SHOW_SPOTLIGHT_MAIN_ACTIVITY)) {
            this.isShownSpotlight = false;
            this.mainBinding.untouched.setVisibility(View.GONE);
            return;
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            this.mainIntro = new MainIntro(this, this.mainBinding.getRoot())
                    .setCaptureAnchor(this.featureBinding.ivCamera)
                    .setImportAnchor(this.featureBinding.clImport)
                    .setDatasAnchor(this.mainBinding.nestedScrollView)
                    .setUtilBarAnchor(this.mainBinding.utilBarContainer)
                    .setSettingAnchor(this.featureBinding.llSettingsContainer)
                    .setCallback(this::setSpotlightUnTouch);

            this.mainIntro.showGuilde();
        }, 500);
    }

    private void checkUpdate() {
        this.inAppUpdate = new SupportInAppUpdate(this, this.mainBinding.getRoot());
        this.inAppUpdate.configInAppUpdate();

        this.inAppUpdate.addOnSuccessListener(appUpdateInfo -> {
            int currentVersion = AppUtils.getAppVersionCode(this);
            if (appUpdateInfo.availableVersionCode() > currentVersion) {
                this.showDialogUpdate(appUpdateInfo);
            } else {
                this.showFANInter();
            }
        });

        this.inAppUpdate.addOnErrorsListener(e -> {
            e.printStackTrace();
            this.showFANInter();
        });
    }

    private void showDialogUpdate(AppUpdateInfo appUpdateInfo) {
        UpdateDialog dialog = new UpdateDialog();
        dialog.setListener(() -> {
            this.inAppUpdate.startUpdate(appUpdateInfo);
            dialog.dismiss();
        });
        dialog.show(this.getSupportFragmentManager(), "UPDATE");
    }

    private void showFANInter() {
        if (FANInter.isAdLoaded()) {
            new Handler(Looper.getMainLooper()).postDelayed(FANInter::show, 400);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (this.mainPresenter != null) {
            this.mainPresenter.searchFile(this.listDataFolderDefault, this.listDataDocumentDefault, s);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    protected void onResume() {
        this.isShowActivity = true;
        super.onResume();

        if (this.inAppUpdate != null) {
            this.inAppUpdate.checkUpdateDownLoaded();
        }
        if (this.actionClickImport == 1) {
            this.actionClickImport = 0;
            return;
        }
        this.getListData();
    }

    private void setLayoutManage() {
        boolean isGrid = this.appPref.isTypeShowGrid();
        this.mainBinding.ivShow.setImageResource(isGrid ? R.drawable.home_vector_show_list : R.drawable.home_vector_show_grid);
        this.mainBinding.rvFiles.setLayoutManager(isGrid ? new GridLayoutManager(this, 3) : new LinearLayoutManager(this));

        if (this.documentAdapter != null) {
            this.documentAdapter.setTypeShowGrid(isGrid);
        }
    }

    private void getListData() {
        if (this.mainPresenter != null) {
            this.mainPresenter.getListFolderDB();
            this.mainPresenter.getListDocumentDB();
        }
    }

    public void updateEmptyStage() {
        if (this.listDataFolder == null || this.listDataDocument == null) {
            this.mainBinding.llContainerEmpty.setVisibility(View.VISIBLE);
            this.mainBinding.ivSelect.setImageResource(R.drawable.all_vector_select);
            this.mainBinding.flQuickAction.setVisibility(View.GONE);
            this.mainBinding.flMainFeature.setVisibility(View.VISIBLE);
            this.documentAdapter.setSelect(false);
            this.folderAdapter.setSelect(false);
            this.countTickFolder = 0;
            this.countTickDocument = 0;
        } else {
            this.mainBinding.llContainerEmpty.setVisibility(View.GONE);
        }

        this.hideOverlay();
        this.mainBinding.tvFiles.setVisibility(this.listDataDocument.isEmpty() ? View.GONE : View.VISIBLE);
        this.mainBinding.tvFolders.setVisibility(this.listDataFolder.isEmpty() ? View.GONE : View.VISIBLE);
        this.mainBinding.llContainerEmpty.setVisibility(this.listDataDocument.isEmpty() && this.listDataFolder.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void statusTiTle() {
        this.mainBinding.tvFolders.setVisibility(this.listDataFolder == null || this.listDataFolder.isEmpty() ? View.GONE : View.VISIBLE);
        this.mainBinding.tvFiles.setVisibility(this.listDataDocument == null || this.listDataDocument.isEmpty() ? View.GONE : View.VISIBLE);
    }

    public void onClickNewFolder(View view) {
        this.event.logEvent("MAIN_CLICK_NEW_FOLDER", null);
        this.showCreateFolderDialog(false);
    }

    public void onClickShowType(View view) {
        this.event.logEvent("MAIN_CLICK_SHOW_TYPE", null);
        if (this.listDataDocument.isEmpty()) {
            return;
        }

        this.appPref.setStatusTypeShowGrid(!this.appPref.isTypeShowGrid());
        this.setLayoutManage();
        this.documentAdapter.notifyDataSetChanged();
    }

    public void onClickSortType(View view) {
        this.event.logEvent("MAIN_CLICK_SORTING", null);
        if (this.listDataDocument.isEmpty() && this.listDataFolder.isEmpty()) {
            return;
        }
        this.showDialogSort();
    }

    public void onClickSelectMain(View view) {
        this.event.logEvent("MAIN_CLICK_SELECT", null);
        this.setSelect(false);
        this.setStatusSelect();
        this.setStatusQuickAction();
    }

    public void onClickSettings(View view) {
        this.event.logEvent("MAIN_CLICK_SETTINGS", null);
        this.startActivity(new Intent(this, SettingsActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    public void onClickImport(View view) {
        this.event.logEvent("MAIN_CLICK_IMPORT", null);
        this.actionClickImport = 0;
        MainActivityPermissionsDispatcher.needsPermissionWithPermissionCheck(this);
    }

    public void onClickCameraMain(View view) {
        this.event.logEvent("MAIN_CLICK_CAMREA", null);
        if (this.mainPresenter != null) {
            this.mainPresenter.startCamera();
        }
    }

    public void onClickShareItem(View view) {
        if (!PermissionUtils.checkPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            MainActivityPermissionsDispatcher.needsPermissionWithPermissionCheck(this);
            this.actionClickImport = 1;
            return;
        }
        this.event.logEvent("MAIN_CLICK_SHARE", null);
        if (this.mainPresenter != null) {
            this.mainPresenter.getTitleSelect();
        }
    }

    public void onClickMoveItem(View view) {
        this.event.logEvent("MAIN_CLICK_MOVE", null);
        this.showDialogMoveFile();
    }

    public void onClickMergeItem(View view) {
        this.event.logEvent("MAIN_CLICK_MERGE", null);
        if (this.mainPresenter != null) {
            this.mainPresenter.setInfoMerge();
        }
    }

    public void onClickDeleteItem(View view) {
        this.event.logEvent("MAIN_CLICK_DELETE", null);
        this.showDialogDelete();
    }

    public void onClickSelectAll(View view) {
        this.event.logEvent("MAIN_CLICK_SELECT_ALL", null);
        this.setTitleSelect();
    }

    private void sortData(@TypeSort int type) {
        if (this.mainPresenter != null) {
            this.mainPresenter.listSortAll(this.listDataFolder, this.listDataDocument, type);
        }
        this.appPref.setSortType(type);
    }

    @SuppressLint("NonConstantResourceId")
    private void showDialogSort() {
        PopupMenu popupMenu = new PopupMenu(this, this.mainBinding.ivSort);
        popupMenu.inflate(R.menu.menu_sort);
        popupMenu.show();
        popupMenu.setOnMenuItemClickListener(menuItem -> {
            switch (menuItem.getItemId()) {
                case R.id.menu_sort_name:
                    this.event.logEvent("MAIN_CLICK_SORT_NAME", null);
                    this.sortData(TypeSort.SORT_NAME);
                    break;

                case R.id.menu_sort_data_created:
                    this.event.logEvent("MAIN_CLICK_SORT_DATE", null);
                    this.sortData(TypeSort.SORT_DATE);
                    break;

                case R.id.menu_sort_size:
                    this.event.logEvent("MAIN_CLICK_SORT_SIZE", null);
                    this.sortData(TypeSort.SORT_DATA);
                    break;
            }

            return false;
        });
    }

    private void showShareDialog(String title) {
        this.shareDialogNew = new ShareDialogNew(this, title);
        this.shareDialogNew.setListener(new ShareDialogNew.DialogShareListener() {
            @Override
            public void onClickShareJpg() {
                if (mainPresenter != null) {
                    mainPresenter.shareJpg(shareDialogNew.getTitle());
                }
            }

            @Override
            public void onClickSharePdf() {
                event.logEvent("MAIN_CLICK_SHARE_GALLERY", null);
                if (mainPresenter != null) {
                    showOverlay();
                    mainPresenter.sharePdf(shareDialogNew.getTitle(), shareDialogNew.getQuality());
                }
            }

            @Override
            public void onClickSavePdf() {
                if (mainPresenter != null) {
                    showOverlay();
                    mainPresenter.savePdf(shareDialogNew.getTitle(), shareDialogNew.getQuality());
                }
            }

            @Override
            public void onClickSaveJpg() {
                if (mainPresenter != null) {
                    showOverlay();
                    mainPresenter.saveJpg(shareDialogNew.getTitle(), shareDialogNew.getQuality());
                }
            }

            @Override
            public void onClickToPdfSettings() {
                actionClickImport = 1;
                startActivity(new Intent(MainActivity.this, PdfSettingsActivity.class));
            }

            @Override
            public void onDismissDialog() {

            }
        });

        this.shareDialogNew.show(getSupportFragmentManager(), "SHARE");
    }

    private void showCreateFolderDialog(boolean isMoveDocument) {
        if (this.mainPresenter != null) {
            this.mainPresenter.getNewFolderDefault(this, isMoveDocument);
        }
    }

    private void showDialogRename(@NonNull BaseEntity baseEntity) {
        AppUtils.showKeyboard(this);
        FileDialog dialog = new FileDialog(this)
                .setCancelable(false)
                .setShowPresent(false)
                .setTitle(R.string.home_rename)
                .setContent(baseEntity.getName());
        dialog.setListener(new FileDialog.FileDialogListener() {
            @Override
            public void onClickCancel() {
                AppUtils.closeKeyboard(MainActivity.this);
                dialog.dimiss();
            }

            @Override
            public void onClickConfirm() {
                if (mainPresenter != null) {
                    String title = dialog.getInputContent();
                    BaseEntity baseEntityTemp = baseEntity instanceof FolderItem ? new FolderItem() : new DocumentItem();
                    baseEntityTemp.setName(title);
                    baseEntityTemp.setCreatedTime(baseEntity.getCreatedTime());
                    baseEntityTemp.setId(baseEntity.getId());
                    baseEntityTemp.setParentId(baseEntity.getParentId());
                    baseEntityTemp.setSelected(baseEntity.isSelected());
                    baseEntityTemp.setSize(baseEntity.getSize());
                    mainPresenter.renameFolder(baseEntityTemp);
                }
                AppUtils.closeKeyboard(MainActivity.this);
                dialog.dimiss();
            }

            @Override
            public void onDismissDialog() {
            }
        });

        dialog.getEdittext().setSelection(baseEntity.getName().length());
        dialog.show();
    }

    private void showDialogMoveFile() {
        AppDatabases appDatabases = AppDatabases.getInstance(this);
        List<BaseEntity> baseEntities = new ArrayList<>(appDatabases.folderDao().getListFolderNoRx());
        baseEntities.add(0, new FolderItem());
        MoveFileDialog dialog = new MoveFileDialog(baseEntities);
        dialog.setTitle(R.string.home_move);
        dialog.setListener(new MoveFileDialog.MoveFileDialogListener() {
            @Override
            public void onCreateFolder() {
                isClickCreateFolder = true;
                showCreateFolderDialog(true);
            }

            @Override
            public void onClickItem(int position, BaseEntity baseEntity) {
                if (mainPresenter != null) {
                    showOverlay();
                    new Handler(Looper.getMainLooper()).postDelayed(() -> mainPresenter.moveFiles(baseEntity), 10);
                }
                dialog.dismiss();
                isClickCreateFolder = false;
            }

            @Override
            public void onDismissDialog() {
                if (!isLongClick) {
                    if (!isClickCreateFolder) {
                        resetStatusAll();
                    }
                }
            }
        });
        dialog.show(this.getSupportFragmentManager(), "MOVE_FILE");
    }

    private void showOverlay() {
        this.mainBinding.clLoading.setVisibility(View.VISIBLE);
    }

    public void hideOverlay() {
        if (!this.isImporting) {
            this.mainBinding.clLoading.setVisibility(View.GONE);
        }
    }

    private void makeToast(int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
    }

    private @NotNull String titleDelete() {
        StringBuilder stringBuilder = new StringBuilder();
        boolean isHaveFolder = false;

        if (this.countTickFolder > 0) {
            stringBuilder.append(this.getResources().getQuantityString(R.plurals.count_folder_main, this.countTickFolder, this.countTickFolder)).append(" ");
            isHaveFolder = true;
        }

        if (this.countTickDocument > 0) {
            if (isHaveFolder) {
                stringBuilder.append(this.getString(R.string.home_and)).append(" ");
            }

            stringBuilder.append(this.getResources().getQuantityString(R.plurals.count_document_main, this.countTickDocument, this.countTickDocument));
        }

        return stringBuilder.toString();
    }

    private void showDialogDelete() {
        CustomisedDialog dialog = new CustomisedDialog(this)
                .setButtonAllowText(R.string.home_accept_delete)
                .setButtonCancelText(R.string.home_deni_delete)
                .setCancelable(false)
                .setTitle(R.string.home_delete_item)
                .setMessage(this.getString(R.string.home_content_delete, this.titleDelete()));
        dialog.setListener(new CustomisedDialog.DialogOnClickListener() {
            @Override
            public void onCancel() {
                dialog.dimiss();
                resetStatusAll();
            }

            @Override
            public void onAccept() {
                dialog.dimiss();
                if (mainPresenter != null) {
                    mainPresenter.deleteFile();
                    mainBinding.clLoading.setVisibility(View.VISIBLE);
                }
            }
        });
        dialog.show();
    }

    private void setStatusSelect() {
        if (this.listDataFolder.size() != 0 || this.listDataDocument.size() != 0) {
            this.folderAdapter.setSelect(!this.folderAdapter.isSelect());
            this.documentAdapter.setSelect(!this.documentAdapter.isSelect());

            if (!this.folderAdapter.isSelect() && !this.documentAdapter.isSelect()) {
                this.updateMultiSelectStage(true);
                this.resetStatusAll();
            } else {
                this.updateMultiSelectStage(false);
            }
        }
    }

    private void updateMultiSelectStage(boolean isExitSelect) {
        this.mainBinding.flMainFeature.setVisibility(isExitSelect ? View.VISIBLE : View.GONE);
        this.mainBinding.flQuickAction.setVisibility(isExitSelect ? View.GONE : View.VISIBLE);
        this.mainBinding.ivSelect.setSelected(!isExitSelect);
        this.isLongClick = !isExitSelect;
        this.mainBinding.tvDone.setVisibility(isExitSelect ? View.GONE : View.VISIBLE);
    }

    private void setTitleSelect() {
        if (TextUtils.equals(this.mainBinding.tvDone.getText(), this.getString(R.string.all_select))) {
            this.setSelectAll(true);
            this.mainBinding.tvDone.setText(R.string.all_unselect);

            if (this.mainPresenter != null) {
                this.mainPresenter.setListItemToHmDocumentItem(this.listDataDocument);
                this.mainPresenter.setListItemToHmFolderItem(this.listDataFolder);
            }
        } else {
            this.mainBinding.tvDone.setText(R.string.all_select);
            this.setSelectAll(false);
            this.resetStatusAll();
        }
    }

    private void setSelectAll(boolean isSelectAll) {
        this.countTickFolder = isSelectAll ? this.listDataFolder.size() : 0;
        this.countTickDocument = isSelectAll ? this.listDataDocument.size() : 0;
        this.setSelect(isSelectAll);
        this.setStatusQuickAction();
    }

    private void setSelect(boolean isSelect) {
        for (FolderItem folderItem : this.listDataFolder) {
            folderItem.setSelected(isSelect);
        }

        for (DocumentItem documentItem : this.listDataDocument) {
            documentItem.setSelected(isSelect);
        }

        if (!isSelect) {
            this.mainPresenter.resetHmDocumentSelect();
            this.mainPresenter.resetHmFolderSelect();
        }

        if (this.isLongClick) {
            this.folderAdapter.notifyDataSetChanged();
            this.documentAdapter.notifyDataSetChanged();
        }
    }

    private void activeItem(boolean isActive, @NotNull ImageView ivItem, @NotNull TextView tvTitle, @NotNull ViewGroup container) {
        ivItem.setSelected(isActive);
        tvTitle.setTextColor(ActivityCompat.getColor(this, isActive ? R.color.color_text_orange : R.color.color_text_gray));
        container.setEnabled(isActive);
    }

    private void resetStatusAll() {
        this.countTickDocument = 0;
        this.countTickFolder = 0;
        this.setSelect(false);
        this.setStatusQuickAction();
    }

    private void setStatusQuickAction() {
        if (this.listDataFolder == null || this.listDataDocument == null) {
            return;
        }

        boolean isSelectAll = this.countTickDocument == this.listDataDocument.size() && this.countTickFolder == this.listDataFolder.size();
        this.mainBinding.tvDone.setText(isSelectAll ? R.string.all_unselect : R.string.all_select);

        if (this.countTickDocument == 0 && this.countTickFolder == 0) {
            this.setStatus(false, false, false);
            this.setStatusShowIcRename(true);
            return;
        }

        if (this.countTickDocument == 1 && this.countTickFolder == 0) {
            this.setStatus(true, false, true);
            this.setStatusShowIcRename(true);
            return;
        }

        if (this.countTickDocument > 1 && this.countTickFolder == 0) {
            this.setStatus(true, true, true);
            this.setStatusShowIcRename(false);
            return;
        }

        if (this.countTickDocument > 0 && this.countTickFolder > 0) {
            this.setStatus(true, false, false);
            this.setStatusShowIcRename(false);
            return;
        }

        if (this.countTickFolder > 1 && this.countTickDocument == 0) {
            this.setStatus(true, true, false);
            this.setStatusShowIcRename(false);
        }

        if (this.countTickFolder == 1) {
            this.setStatus(true, false, false);
            this.setStatusShowIcRename(true);
        }
    }

    private void setStatus(boolean isDelete, boolean isMerge, boolean isMove) {
        this.activeItem(isDelete, this.quickActionBinding.ivDelete, this.quickActionBinding.tvDelete, this.quickActionBinding.llContainerDelete);
        this.activeItem(isMerge, this.quickActionBinding.ivMerg, this.quickActionBinding.tvMerga, this.quickActionBinding.llContainerMerge);
        this.activeItem(isMove, this.quickActionBinding.ivMove, this.quickActionBinding.tvMove, this.quickActionBinding.llContainerMove);
        this.activeItem(isDelete, this.quickActionBinding.ivSendTo, this.quickActionBinding.tvSendTo, this.quickActionBinding.llContainerSend);
    }

    private void setStatusShowIcRename(boolean isMutipleSelect) {
        this.folderAdapter.setOnMultipleSelect(isMutipleSelect);
        this.documentAdapter.setOnMultipleSelect(isMutipleSelect);
    }

    private void setSpotlightUnTouch() {
        this.mainBinding.untouched.setClickable(false);
        this.mainBinding.untouched.setFocusable(false);
        this.mainBinding.untouched.setVisibility(View.GONE);
    }

    private void setSelect(int position, BaseEntity baseEntity) {
        if (baseEntity instanceof DocumentItem) {
            if (this.listDataDocument == null || position > this.listDataDocument.size() - 1) {
                return;
            }

            this.listDataDocument.get(position).setSelected(true);
            this.mainPresenter.setItemToHmDocumentItem(this.listDataDocument.get(position));
            return;
        }

        if (this.listDataFolder == null || position > this.listDataFolder.size() - 1) {
            return;
        }

        this.listDataFolder.get(position).setSelected(true);
        this.mainPresenter.setItemToHmFolderItem(this.listDataFolder.get(position));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constants.CODE_PERMISSION_STORAGE) {
            if (this.actionClickImport == 0) {
                MainActivityPermissionsDispatcher.needsPermissionWithPermissionCheck(this);
            }
            return;
        }

        if (requestCode == Constants.SHARE_REQUEST_CODE) {
            if (this.inAppReview != null) {
                this.inAppReview.showReview();
            }
            return;
        }

        if (requestCode == Constants.CODE_PICK_IMAGE_INTENT) {
            if (data == null) {
                this.isImporting = false;
                this.hideOverlay();
                return;
            }

            if (this.mainPresenter != null) {
                this.showOverlay();
                this.mainPresenter.getListFileFromImport(data);
            }
        }
    }

    @Override
    public void onShowNameFolder(String nameFolder, boolean isMoveDocument) {
        FileDialog dialog = new FileDialog(this)
                .setCancelable(false)
                .setShowPresent(false)
                .setContent(nameFolder)
                .setTitle(R.string.home_created_folder);
        dialog.setListener(new FileDialog.FileDialogListener() {
            @Override
            public void onClickCancel() {
                dialog.dimiss();
                AppUtils.closeKeyboard(MainActivity.this);
                if (isMoveDocument) {
                    resetStatusAll();
                }
            }

            @Override
            public void onClickConfirm() {
                if (mainPresenter != null) {
                    mainPresenter.createNewFolder(dialog.getInputContent(), isMoveDocument);
                }
                AppUtils.closeKeyboard(MainActivity.this);
                dialog.dimiss();
            }

            @Override
            public void onDismissDialog() {
                AppUtils.closeKeyboard(MainActivity.this);
            }
        });

        dialog.getEdittext().requestFocus();
        AppUtils.showKeyboard(this);
        dialog.show();
    }

    @Override
    public void onErrorCreatedFolder() {
        this.makeToast(R.string.home_error_create_folder);
    }

    @Override
    public void onSuccessCreatedFolder(FolderItem folderItem, boolean isMoveDocument) {
        if (!isMoveDocument) {
            this.getListData();
            return;
        }

        if (this.mainPresenter != null) {
            this.showOverlay();
            new Handler(Looper.getMainLooper()).postDelayed(() ->
                    this.mainPresenter.moveFiles(folderItem), 100);
        }
    }

    @Override
    public void onDeleteSuccess() {
        this.getListData();
    }

    @Override
    public void showListFolder(List<FolderItem> list) {
        if (list == null) {
            return;
        }

        if (this.folderAdapter == null) {
            return;
        }

        this.listDataFolder.clear();
        this.listDataFolder = new ArrayList<>(list);
        this.listDataFolderDefault = new ArrayList<>(list);
        this.countTickFolder = 0;
        this.isLoadFolderSuccess = true;
        this.folderAdapter.setListFile(new ArrayList<>(this.listDataFolder));

        if (this.isLoadAdSuccess) {
            this.showAds();
        }

        this.updateEmptyStage();
        this.setStatusQuickAction();

        if (this.mainPresenter != null) {
            this.mainPresenter.listSortAll(this.listDataFolder, this.listDataDocument, this.appPref.getSortType());
        }
    }

    @Override
    public void showListDocument(List<DocumentItem> list) {
        if (list == null) {
            return;
        }

        if (this.appPref.isCallShowReview() && this.inAppReview != null) {
            this.appPref.setCallShowReview(false);
            this.inAppReview.showReview();
        }

        if (this.documentAdapter == null) {
            return;
        }

        this.listDataDocument = new ArrayList<>(list);
        this.listDataDocumentDefault = new ArrayList<>(list);
        this.isLoadDocumentSuccess = true;
        this.documentAdapter.setListFile(new ArrayList<>(this.listDataDocument));
        this.countTickDocument = 0;

        if (this.isLoadAdSuccess) {
            this.showAds();
        }

        this.updateEmptyStage();
        this.setStatusQuickAction();

        if (this.mainPresenter != null) {
            this.mainPresenter.listSortAll(this.listDataFolder, this.listDataDocument, this.appPref.getSortType());
        }
    }

    @Override
    public void onSingleImportSuccess(PageItem pageItem) {
        this.hideOverlay();

        if (this.isShowActivity) {
            this.isImporting = false;
            Intent iDetect = new Intent(this, DetectActivity.class);
            iDetect.putExtra(Constants.EXTRA_PAGE_ITEM, pageItem);
            this.startActivity(iDetect);
        }
    }

    @Override
    public void onMultipleImportSuccess() {
        this.hideOverlay();

        if (this.isShowActivity) {
            this.isImporting = false;
            this.startActivity(new Intent(this, SaveActivity.class));
        }
    }

    @Override
    public void onErrorImport() {
        this.isImporting = false;
        this.hideOverlay();
    }

    @Override
    public void resetQuickAction() {
        this.hideOverlay();
        this.getListData();
        this.countTickFolder = 0;
        this.countTickDocument = 0;
        this.documentAdapter.setSelect(true);
        this.folderAdapter.setSelect(true);
        this.setStatusSelect();
        this.setStatusQuickAction();
    }

    @Override
    public void showDialogSendTo(String title) {
        this.showShareDialog(title);
    }

    @Override
    public void showListSearch(List<FolderItem> listFolder, List<DocumentItem> listDocument) {
        this.listDataFolder = new ArrayList<>(listFolder);
        this.listDataDocument = new ArrayList<>(listDocument);
        this.folderAdapter.setListFile(new ArrayList<>(this.listDataFolder));
        this.documentAdapter.setListFile(new ArrayList<>(this.listDataDocument));
        this.statusTiTle();
    }

    @Override
    public void showListDefault() {
        this.listDataDocument = new ArrayList<>(this.listDataDocumentDefault);
        this.listDataFolder = new ArrayList<>(this.listDataFolderDefault);
        this.folderAdapter.setListFile(new ArrayList<>(this.listDataFolder));
        this.documentAdapter.setListFile(new ArrayList<>(this.listDataDocumentDefault));
        this.statusTiTle();
    }

    @Override
    public void showDialogMerge(String title, String pathRepresent) {
        Context mContext = MainActivity.this;
        AppUtils.showKeyboard(this);

        FileDialog dialog = new FileDialog(mContext)
                .setCancelable(false)
                .setShowPresent(true)
                .setImgPreseter(pathRepresent)
                .setTitle(R.string.home_merge)
                .setContent(title);
        dialog.setListener(new FileDialog.FileDialogListener() {
            @Override
            public void onClickCancel() {
                dialog.dimiss();
                AppUtils.closeKeyboard(mContext);
            }

            @Override
            public void onClickConfirm() {
                if (mainPresenter == null) {
                    dialog.dimiss();
                    AppUtils.closeKeyboard(mContext);
                    return;
                }

                boolean isMergeDoc = countTickDocument > 0;
                String nameCurrent = dialog.getInputContent();
                if (TextUtils.isEmpty(nameCurrent)) {
                    makeToast(isMergeDoc ? R.string.save_alert_input_empty_name : R.string.home_folder_et_empty);
                    return;
                }

                showOverlay();
                mainPresenter.mergeFile(nameCurrent);
                dialog.dimiss();
                AppUtils.closeKeyboard(mContext);
            }

            @Override
            public void onDismissDialog() {
            }
        });

        dialog.getEdittext().setSelection(title.length());
        dialog.show();
    }

    @Override
    public void onSuccessRenameFolder() {
        this.getListData();
    }

    @Override
    public void onSuccessSaveGallery(List<String> filesJpg) {
        if (this.shareDialogNew != null) {
            this.shareDialogNew.dismiss();
        }

        this.hideOverlay();
        this.documentAdapter.setSelect(true);
        this.folderAdapter.setSelect(true);
        this.setStatusSelect();
        this.setStatusQuickAction();

        ShareSuccessDialog shareSuccessDialog = new ShareSuccessDialog(this, filesJpg, getString(R.string.jpg_success));
        shareSuccessDialog.show(getSupportFragmentManager(), "SUCCESS_JPG");

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (this.inAppReview != null) {
                this.inAppReview.showReview();
            }
        }, 500);
    }

    @Override
    public void onShowFolderSort(List<FolderItem> listFolder) {
        if (this.listDataFolder != null) {
            this.listDataFolder = new ArrayList<>(listFolder);
            this.listDataFolderDefault = new ArrayList<>(listFolder);
            this.folderAdapter.setListFile(new ArrayList<>(this.listDataFolder));
            this.resetStatusAll();
        }
    }

    @Override
    public void onSuccessSavePdf(List<String> filesPdf) {
        hideOverlay();
        if (this.mainPresenter != null) {
            this.mainPresenter.resetHmDocumentSelect();
            this.mainPresenter.resetHmFolderSelect();
        }
        ShareSuccessDialog shareSuccessDialog = new ShareSuccessDialog(this, filesPdf, getString(R.string.pdf_success));
        shareSuccessDialog.show(getSupportFragmentManager(), "SHARE_SUCCRSS");
    }

    @Override
    public void onShowDocumentsSort(List<DocumentItem> listDocument) {
        if (this.listDataDocument != null) {
            this.listDataDocument = new ArrayList<>(listDocument);
            this.listDataDocumentDefault = new ArrayList<>(listDocument);
            this.documentAdapter.setListFile(new ArrayList<>(this.listDataDocument));
            this.resetStatusAll();
        }
    }

    @Override
    public void onClickSelectFolder(int position, boolean isSelect) {
        this.listDataFolder.get(position).setSelected(isSelect);
        this.listDataFolderDefault.get(position).setSelected(isSelect);
        this.folderAdapter.notifyItemChanged(position);
        this.countTickFolder = isSelect ? this.countTickFolder + 1 : this.countTickFolder - 1;
        this.setStatusQuickAction();

        if (this.mainPresenter != null) {
            this.mainPresenter.setItemToHmFolderItem(this.listDataFolder.get(position));
        }
    }

    @Override
    public void onClickSelectDocument(int position, boolean isSelect) {
        this.listDataDocument.get(position).setSelected(isSelect);
        this.listDataDocumentDefault.get(position).setSelected(isSelect);
        this.documentAdapter.notifyItemChanged(position);
        this.countTickDocument = isSelect ? this.countTickDocument + 1 : this.countTickDocument - 1;
        this.setStatusQuickAction();

        if (this.mainPresenter != null) {
            this.mainPresenter.setItemToHmDocumentItem(this.listDataDocument.get(position));
        }
    }

    @Override
    public void onClickOpenFolder(int position) {
        this.event.logEvent("MAIN_ITEM_CLICK_OPEN", null);
        FolderItem folderItem = this.listDataFolder.get(position);
        Intent intent = new Intent(this, DetailFolderActivity.class);
        intent.putExtra(Constants.EXTRA_FOLDER_ID, folderItem.getId());
        this.appPref.setSaveId(folderItem.getId());
        this.appPref.setSaveInDocument(false);
        this.startActivity(intent);
    }

    @Override
    public void onclickOpenDocument(int position) {
        this.event.logEvent("MAIN_ITEM_CLICK_OPEN", null);
        Intent intent = new Intent(this, DetailDocumentActivity.class);
        DocumentItem documentItem = this.listDataDocument.get(position);
        intent.putExtra(Constants.EXTRA_DOCUMENT_ID, documentItem.getId());
        this.startActivity(intent);
    }

    @Override
    public void onLongClick(int position, boolean isDocument, boolean isSelect) {
        this.event.logEvent("MAIN_ITEM_LONG_CLICK", null);

        if (this.isLongClick) {
            this.isLongClick = false;
            this.folderAdapter.setSelect(true);
            this.documentAdapter.setSelect(true);
            this.setStatusSelect();
            this.setStatusQuickAction();
            return;
        }

        this.isLongClick = true;
        this.folderAdapter.setSelect(false);
        this.documentAdapter.setSelect(false);

        if (isDocument) {
            this.listDataDocument.get(position).setSelected(true);
            this.mainPresenter.setItemToHmDocumentItem(this.listDataDocument.get(position));
            this.countTickDocument++;
        } else {
            this.listDataFolder.get(position).setSelected(true);
            this.mainPresenter.setItemToHmFolderItem(this.listDataFolder.get(position));
            this.countTickFolder++;
        }

        this.setStatusSelect();
        this.setStatusQuickAction();
    }

    @Override
    public void onClickRename(Object itemFile) {
        this.showDialogRename((BaseEntity) itemFile);
    }

    @Override
    public void deleteFile(int position, Object object) {
        this.setSelect(position, (BaseEntity) object);
        this.showDialogDelete();
    }

    @Override
    public void shareFile(int position, Object object) {
        this.setSelect(position, (BaseEntity) object);

        if (!PermissionUtils.checkPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            MainActivityPermissionsDispatcher.needsPermissionWithPermissionCheck(this);
            this.actionClickImport = 1;
            return;
        }

        if (this.mainPresenter != null) {
            this.mainPresenter.getTitleSelect();
        }
    }

    @Override
    public void moveFile(int position, Object object) {
        this.setSelect(position, (BaseEntity) object);
        this.showDialogMoveFile();
    }

    @Override
    public void onBackPressed() {
        if (this.isShownSpotlight) {
            if (this.appPref.isShowSpotlight(ConstantsPrefs.KEY_SHOW_SPOTLIGHT_MAIN_ACTIVITY)) {
                if (this.mainIntro != null) {
                    this.isShownSpotlight = false;
                    this.mainIntro.finishGuilde();
                }

                this.countBackPress = 0;
                return;
            }
        }

        this.appPref.setShowSpotlight(ConstantsPrefs.KEY_SHOW_SPOTLIGHT_MAIN_ACTIVITY, false);
        this.setSpotlightUnTouch();

        if (this.mainBinding.etSearch.getText().length() > 0) {
            this.mainBinding.etSearch.setText("");
            this.countBackPress = 0;
            return;
        }

        if (this.mainBinding.clLoading.getVisibility() == View.VISIBLE) {
            this.makeToast(R.string.all_alert_file_processing);
            return;
        }

        if (this.isLongClick) {
            this.isLongClick = false;
            this.folderAdapter.setSelect(true);
            this.documentAdapter.setSelect(true);
            this.setStatusSelect();
            this.setStatusQuickAction();
            this.countBackPress = 0;
            return;
        }

        this.countBackPress++;
        if (this.countBackPress == 1) {
            this.makeToast(R.string.home_exit_title);
        }

        if (this.countBackPress == 2) {
            this.finish();
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> this.countBackPress = 0, 2000);
    }

    @OnPermissionDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void showDenied() {
    }

    @OnNeverAskAgain(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void showNeverAskForStorage() {
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
                PermissionUtils.startSettingsPermissionStorage(MainActivity.this);
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

        if (this.actionClickImport == 1) {
            if (this.mainPresenter != null) {
                this.mainPresenter.getTitleSelect();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.isShowActivity = false;
        this.mainBinding.etSearch.setText("");
    }

    @Override
    protected void onDestroy() {
        if (this.mainPresenter != null) {
            this.mainPresenter.setDisposableAll();
        }

        if (this.unifiedNativeAd != null) {
            this.unifiedNativeAd.destroy();
        }

        if (this.nativeAds != null) {
            this.nativeAds.destroyAds();
        }

        FANInter.destroy();
        System.gc();
        this.appPref.setLastTimeShowAdsInter(0);
        super.onDestroy();
    }

    @Override
    public void onLoaded(List<Object> listNativeAds) {
        this.isLoadAdSuccess = true;

        if (this.isLoadDocumentSuccess && this.isLoadFolderSuccess) {
            if (listNativeAds == null) {
                return;
            }
            this.unifiedNativeAd = (UnifiedNativeAd) listNativeAds.get(0);
            this.showAds();
        }
    }

    private void showAds() {
        if (this.unifiedNativeAd == null) {
            return;
        }

        if (this.listDataFolderDefault.isEmpty() && this.listDataDocumentDefault.isEmpty()) {
            this.mainBinding.cvAdsFolder.removeAllViews();
            this.mainBinding.cvAdsDoccument.removeAllViews();
            return;
        }

        if (this.adView == null) {
            this.adView = new AdViewType(this).getAdViewList(this.unifiedNativeAd);
        }

        if (this.listDataFolderDefault.isEmpty()) {
            this.mainBinding.cvAdsFolder.removeAllViews();
            this.mainBinding.cvAdsDoccument.removeAllViews();
            this.mainBinding.cvAdsDoccument.addView(this.adView);
            return;
        }

        this.mainBinding.cvAdsDoccument.removeAllViews();
        this.mainBinding.cvAdsFolder.removeAllViews();
        this.mainBinding.cvAdsFolder.addView(this.adView);
    }

}
