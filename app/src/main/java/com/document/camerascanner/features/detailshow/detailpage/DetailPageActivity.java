package com.document.camerascanner.features.detailshow.detailpage;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.document.camerascanner.R;
import com.document.camerascanner.ads.AdConstant;
import com.document.camerascanner.ads.InterAds;
import com.document.camerascanner.databases.livedata.PageViewModel;
import com.document.camerascanner.databases.model.DocumentItem;
import com.document.camerascanner.databases.model.PageItem;
import com.document.camerascanner.databinding.ActivityDetailPageBinding;
import com.document.camerascanner.features.detect.DetectActivity;
import com.document.camerascanner.features.pdfsettings.PdfSettingsActivity;
import com.document.camerascanner.features.review.SupportInAppReview;
import com.document.camerascanner.features.share.ShareDialogNew;
import com.document.camerascanner.features.share.ShareSuccessDialog;
import com.document.camerascanner.features.view.CustomisedDialog;
import com.document.camerascanner.prefs.AppPref;
import com.document.camerascanner.utils.AppUtils;
import com.document.camerascanner.utils.Constants;
import com.document.camerascanner.utils.DbUtils;
import com.document.camerascanner.utils.FileUtils;
import com.document.camerascanner.utils.PdfUtils;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import permissions.dispatcher.PermissionUtils;


public class DetailPageActivity extends AppCompatActivity implements SupportInAppReview.OnReviewListener {

    private int currentPosition = 0;

    private AppPref appPref;
    private InterAds interAd;
    private List<PageItem> listData;
    private List<PagePreview> listItem;
    private CompositeDisposable compositeDisposable;
    private FirebaseAnalytics event;
    private ShareDialogNew shareDialogNew;
    private SupportInAppReview inAppReview;
    private SlidePageAdapter slidePageAdapter;
    private ActivityDetailPageBinding binding;
    private DocumentItem documentItem;
    private PageViewModel pageViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.init();
    }

    private void init() {
        this.initView();
        this.initConfig();
        this.initAd();
    }

    private void initView() {
        this.binding = ActivityDetailPageBinding.inflate(this.getLayoutInflater());
        this.setContentView(this.binding.getRoot());
    }

    private void initConfig() {
        this.event = FirebaseAnalytics.getInstance(this);
        this.appPref = AppPref.getInstance(this);
        this.pageViewModel = new ViewModelProvider(this).get(PageViewModel.class);
        this.inAppReview = new SupportInAppReview(this, this);
        this.compositeDisposable = new CompositeDisposable();
        this.listData = new ArrayList<>();
        this.listItem = new ArrayList<>();
        this.slidePageAdapter = new SlidePageAdapter(this);

        this.initData();
    }

    private void initData() {
        Intent intent = this.getIntent();
        int id = -1;
        if (intent != null) {
            id = intent.getIntExtra(Constants.EXTRA_DOCUMENT_ID, -1);
            this.currentPosition = intent.getIntExtra(Constants.EXTRA_POSITION_CURRENT_PAGE, -1);
        }

        if (id > 0) {
            this.initModel(id);
        } else {
            this.finish();
        }
    }

    private void initModel(int id) {
        this.pageViewModel.getDocument(id).observe(this, document -> {
            if (document == null) {
                this.finish();
                return;
            }

            this.documentItem = document;
            this.binding.tvTitle.setText(this.getString(R.string.detail_page_current_page, this.currentPosition, this.listData.size()));

            if (this.listData == null || this.listData.isEmpty()) {
                this.pageViewModel.getPageByParent(this.documentItem.getId()).observe(this, list -> {
                    this.listData = new ArrayList<>(list);

                    if (this.listData.isEmpty()) {
                        this.pageViewModel.deletePageParent(this.documentItem);
                        this.finish();
                        return;
                    }

                    this.initListItem();
                });
            }

        });
    }

    private void initListItem() {
        if (this.listItem.isEmpty()) {
            for (int i = 0; i < this.listData.size(); i++) {
                PagePreview pagePreview = new PagePreview(0, this.listData.get(i).getEnhanceUri());
                this.listItem.add(pagePreview);
            }
        } else {
            for (int i = 0; i < this.listData.size(); i++) {

                PageItem pageItem = this.listData.get(i);
                PagePreview pagePreview = this.listItem.get(i);

                if (!TextUtils.equals(pageItem.getEnhanceUri(), pagePreview.getImgUri())) {
                    pagePreview.setImgUri(pageItem.getEnhanceUri());
                    this.listItem.set(i, pagePreview);
                }

            }
        }

        this.slidePageAdapter.setListPage(this.listItem);
        this.binding.vpSlidePage.setAdapter(this.slidePageAdapter);
        this.binding.vpSlidePage.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                setStatusCurrent(position);
            }
        });
        this.binding.vpSlidePage.setCurrentItem(this.currentPosition);
    }

    private void setStatusCurrent(int position) {
        this.currentPosition = position;
        this.binding.tvTitle.setText(this.getString(R.string.detail_page_current_page, this.currentPosition + 1, this.listData.size()));
    }

    private void initAd() {
        this.interAd = new InterAds(this, AdConstant.IT_DETAIL_PAGE, AdConstant.DETAIL_PAGE);
        this.interAd.setListener(new InterAds.InterAdsListener(this.interAd) {
            @Override
            public void onAdClosed() {
                super.onAdClosed();
                startResult();
            }
        });
        this.interAd.loadAds();
    }

    private void showAd() {
        if (this.interAd != null && this.interAd.isLoaded() && this.appPref.isShowAdsInter()) {
            this.appPref.setLastTimeShowAdsInter(System.currentTimeMillis());
            this.interAd.showAds();
            return;
        }
        this.startResult();
    }

    private void startResult() {
        AppUtils.startActivity(this);
    }

    private void showDialogDelete() {
        CustomisedDialog dialog = new CustomisedDialog(this)
                .setButtonAllowText(R.string.home_accept_delete)
                .setButtonCancelText(R.string.home_deni_delete)
                .setCancelable(false)
                .setTitle(R.string.save_delete_dialog_title)
                .setMessage(R.string.save_delete_dialog_message);
        dialog.setListener(new CustomisedDialog.DialogOnClickListener() {
            @Override
            public void onCancel() {
                dialog.dimiss();
            }

            @Override
            public void onAccept() {
                dialog.dimiss();
                listData.get(currentPosition).setSelected(true);
                listItem.remove(currentPosition);
                pageViewModel.multipleDelete(listData);
            }
        });
        dialog.show();
    }

    public void onBackClick(android.view.View view) {
        this.onBackPressed();
    }

    public void onClickShareItem(android.view.View view) {
        this.event.logEvent("DETAIL_PAGE_CLICK_SHARE", null);
        this.showShareDialog(this.documentItem.getName() + "_" + (this.currentPosition + 1));
    }

    public void onClickRotate(android.view.View view) {
        this.event.logEvent("DETAIL_PAGE_CLICK_ROTATE", null);
        this.listItem.get(this.currentPosition).setRoation();
        this.slidePageAdapter.notifyItemChanged(this.currentPosition);
    }

    public void onClickCrop(android.view.View view) {
        this.event.logEvent("DETAIL_PAGE_CLICK_CROP", null);
        Intent intent = new Intent(this, DetectActivity.class);
        intent.putExtra(Constants.EXTRA_PAGE_ITEM, this.listData.get(this.currentPosition));
        intent.putExtra(Constants.EXTRA_IS_FROM_DETAIL_PAGE, true);
        this.startActivity(intent);
    }

    public void onClickDeleteItem(android.view.View view) {
        this.event.logEvent("DETAIL_PAGE_CLICK_DELETE", null);
        this.showDialogDelete();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.SHARE_REQUEST_CODE) {
            if (this.inAppReview != null) {
                this.inAppReview.showReview();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != Constants.CODE_PERMISSION_STORAGE) {
            return;
        }

        if (PermissionUtils.verifyPermissions(grantResults)) {
            return;
        }

        if (!PermissionUtils.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            this.showDialogPermission();
        }
    }

    private void addDisposable(Disposable disposable) {
        if (this.compositeDisposable == null) {
            this.compositeDisposable = new CompositeDisposable();
        }
        this.compositeDisposable.add(disposable);
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
                com.document.camerascanner.utils.PermissionUtils.startSettingsPermissionStorage(DetailPageActivity.this);
            }
        });
        dialog.show();
    }

    private void showShareDialog(String title) {
        this.event.logEvent("DETAIL_PAGE_SHOW_SHARE_DIALOG", null);
        this.shareDialogNew = new ShareDialogNew(this, title);
        this.shareDialogNew.setListener(new ShareDialogNew.DialogShareListener() {
            @Override
            public void onClickShareJpg() {
                shareImage();
            }

            @Override
            public void onClickSharePdf() {
                onShareAnsSavePdf(shareDialogNew.getTitle(), shareDialogNew.getQuality(), false);
            }

            @Override
            public void onClickSavePdf() {
                onShareAnsSavePdf(shareDialogNew.getTitle(), shareDialogNew.getQuality(), true);
            }

            @Override
            public void onClickSaveJpg() {
                event.logEvent("DETAIL_PAGE_SHARE_GALLERY", null);
                exportToGallery(shareDialogNew.getTitle(), shareDialogNew.getQuality());
            }

            @Override
            public void onClickToPdfSettings() {
                event.logEvent("DETAIL_PAGE_SHARE_PDF", null);
                startActivity(new Intent(DetailPageActivity.this, PdfSettingsActivity.class));
//                sharePDF();
            }

            @Override
            public void onDismissDialog() {

            }
        });
        this.shareDialogNew.show(getSupportFragmentManager(), "SHARE_DIALOG");
    }

    private void exportToGallery(String title, int quality) {
        List<PageItem> pageItems = new ArrayList<>();
        pageItems.add(this.listData.get(this.currentPosition));
        DocumentItem document = new DocumentItem();
        document.setName(this.documentItem.getName());
        document.setId(this.documentItem.getId());
        document.setParentId(this.documentItem.getParentId());
        DbUtils.sharePageToGallery(this, document, title, quality, pageItems, new DbUtils.StatusShareGallery() {
            @Override
            public void onDisposable(Disposable disposable) {
                addDisposable(disposable);
            }

            @Override
            public void onSuccess(List<String> listFile) {
                if (shareDialogNew != null) {
                    shareDialogNew.dismiss();
                }

                onSuccessShareDialog(listFile, getString(R.string.jpg_success));

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (inAppReview != null) {
                        inAppReview.showReview();
                    }
                }, 500);
            }

            @Override
            public void onError() {

            }
        });
    }

    private void onSuccessShareDialog(List<String> files, String title) {
        ShareSuccessDialog shareSuccessDialog = new ShareSuccessDialog(this, files, title);
        shareSuccessDialog.show(getSupportFragmentManager(), "SHARE_SUCCESS");
    }

    private void shareImage() {
        ArrayList<String> listShare = new ArrayList<>();
        listShare.add(this.listData.get(this.currentPosition).getEnhanceUri());
        FileUtils.sendfile(this, listShare);
    }

    private void sharePDF() {
        ArrayList<Integer> listId = new ArrayList<>();
        listId.add(this.listData.get(this.currentPosition).getId());
        Intent intent = new Intent(this, PdfSettingsActivity.class);
        intent.putExtra(Constants.EXTRA_IMAGE_SELECT, listId);
        this.startActivity(intent);
    }

    private void onShareAnsSavePdf(String title, int quality, boolean isSave) {
        DocumentItem document = new DocumentItem();
        document.setName(documentItem.getName());
        List<PageItem> listPage = new ArrayList<>();
        listPage.add(this.listData.get(this.currentPosition));
        document.setListPage(listPage);
        List<DocumentItem> documentsConvert = new ArrayList<>();
        documentsConvert.add(document);
        PdfUtils.convertPdf(this, quality, title, documentsConvert, new PdfUtils.PdfStatus() {
            @Override
            public void onDisposable(Disposable disposable) {
                addDisposable(disposable);
            }

            @Override
            public void onSuccessConvertPdf(List<String> listPdf) {
                if (shareDialogNew != null) {
                    shareDialogNew.dismiss();
                }

                if (isSave) {
                    onSuccessShareDialog(listPdf, getString(R.string.pdf_success));
                } else {
                    FileUtils.sendfile(DetailPageActivity.this, listPdf);
                }

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (inAppReview != null) {
                        inAppReview.showReview();
                    }
                }, 500);
            }

            @Override
            public void onProcessPercent(int percent) {

            }

            @Override
            public void onError() {

            }
        });
    }

    @Override
    public void onBackPressed() {
        this.showAd();
    }

    @Override
    public void onReviewComplete() {
    }

    @Override
    protected void onDestroy() {
        if (this.compositeDisposable != null) {
            this.compositeDisposable.dispose();
        }

        if (this.interAd != null) {
            this.interAd.destroyAds();
        }

        System.gc();
        super.onDestroy();
    }

}
