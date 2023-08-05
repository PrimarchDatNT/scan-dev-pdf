package com.document.camerascanner.features.pdfsettings;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.document.camerascanner.R;
import com.document.camerascanner.ads.AdConstant;
import com.document.camerascanner.ads.AdsBanner;
import com.document.camerascanner.databinding.ActivityPdfSettingsBinding;
import com.document.camerascanner.features.review.SupportInAppReview;
import com.document.camerascanner.prefs.AppPref;
import com.document.camerascanner.utils.Constants;
import com.document.camerascanner.utils.FileUtils;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Rectangle;

import java.util.List;

public class PdfSettingsActivity extends AppCompatActivity implements PdfController.PdfView, CompoundButton.OnCheckedChangeListener,
        SupportInAppReview.OnReviewListener {

    private int typeSize = 0;

    private AppPref appPref;
    private AdsBanner adsBanner;
    private FirebaseAnalytics event;
    private PdfPresenter pdfPresenter;
    private ActivityPdfSettingsBinding binding;
    private SupportInAppReview supportInAppReview;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.init();
    }

    private void init() {
        this.initConfig();
        this.initView();
        this.initAds();
        this.typeSize = this.appPref.getPageSizePdf();
        this.event = FirebaseAnalytics.getInstance(this);
        this.event.logEvent("PDF_OPEN", null);
    }

    private void initConfig() {
        this.appPref = AppPref.getInstance(this);
        this.pdfPresenter = new PdfPresenter(this);
        this.supportInAppReview = new SupportInAppReview(this, this);
    }

    private void initAds() {
        this.adsBanner = new AdsBanner(this, AdConstant.BN_PDF_SETTING, AdConstant.PDF_SETTING, this.binding.adViewBanner);
    }

    private void initView() {
        this.binding = ActivityPdfSettingsBinding.inflate(this.getLayoutInflater());
        this.setContentView(this.binding.getRoot());

        this.binding.tb.setNavigationOnClickListener(view -> this.onBackPressed());
        this.binding.swPdfPageMargin.setOnCheckedChangeListener(this);
        this.binding.swPdfPageNumber.setOnCheckedChangeListener(this);

        boolean isPageNumber = this.appPref.isPageNumber();
        boolean isPageMargin = this.appPref.isMarginPdf();

        this.binding.tvContentPdfOrientation.setText(this.appPref.isPageOrientationPortrait()
                ? R.string.pdf_settings_portrait : R.string.pdf_settings_landscape);

        this.binding.tvContentPdfPageNumber.setText(isPageNumber
                ? R.string.pdf_settings_show_page_number : R.string.pdf_settings_hide_page_number);

        this.binding.swPdfPageNumber.setChecked(isPageNumber);

        this.binding.tvContentPdfPageMargin.setText(isPageMargin
                ? R.string.pdf_settings_add_margin_page : R.string.pdf_settings_no_margin_page);

        this.binding.swPdfPageMargin.setChecked(isPageMargin);
        this.pdfPresenter.init(this);
    }

    private void showDialogPageSize() {
        OptionSizeDialog dialog = new OptionSizeDialog(this.typeSize);
        dialog.setListener(new OptionSizeDialog.DialogOptionSizeListener() {
            @Override
            public void onClickLetter() {
                event.logEvent("PDF_CLICK_SIZE_LETTER", null);
                onClickOptionSize(PageSize.LETTER, R.string.pdf_settings_size_letter);
                typeSize = SizeType.LETTER;
            }

            @Override
            public void onClickA4() {
                event.logEvent("PDF_CLICK_SIZE_A4", null);
                onClickOptionSize(PageSize.A4, R.string.pdf_settings_size_a4);
                typeSize = SizeType.A4;
                setTypeInPresenter(typeSize);
            }

            @Override
            public void onClickLegal() {
                event.logEvent("PDF_CLICK_SIZE_LEGAL", null);
                onClickOptionSize(PageSize.LEGAL, R.string.pdf_settings_size_legal);
                typeSize = SizeType.LEGAL;
                setTypeInPresenter(typeSize);
            }

            @Override
            public void onClickA3() {
                event.logEvent("PDF_CLICK_SIZE_A3", null);
                onClickOptionSize(PageSize.A3, R.string.pdf_settings_size_a3);
                typeSize = SizeType.A3;
                setTypeInPresenter(typeSize);
            }

            @Override
            public void onClickA5() {
                event.logEvent("PDF_CLICK_SIZE_A5", null);
                onClickOptionSize(PageSize.A5, R.string.pdf_settings_size_a5);
                typeSize = SizeType.A5;
                setTypeInPresenter(typeSize);
            }

            @Override
            public void onClickBussinesCard() {
                event.logEvent("PDF_CLICK_SIZE_CARD", null);
                onClickOptionSize(PageSize.POSTCARD, R.string.pdf_settings_size_bussiness_card);
                typeSize = SizeType.BUSINESS_CARD;
                setTypeInPresenter(typeSize);
            }
        });
        dialog.show(this.getSupportFragmentManager(), "OPTION_SIZE");
    }

    private void setTypeInPresenter(int typeSize) {
        if (this.pdfPresenter != null) {
            this.pdfPresenter.setTypeSize(typeSize);
        }
    }

    public void setPageNumber(View view) {
        this.event.logEvent("PDF_CLICK_NUMBER", null);
        this.binding.swPdfPageNumber.setChecked(!this.binding.swPdfPageNumber.isChecked());
    }

    public void setPageMargin(View view) {
        this.event.logEvent("PDF_CLICK_MARGIN", null);
        this.binding.swPdfPageMargin.setChecked(!this.binding.swPdfPageMargin.isChecked());
    }

    public void setPageSize(View view) {
        this.event.logEvent("PDF_CLICK_SIZE", null);
        this.showDialogPageSize();
    }

    public void setPageOrientation(View view) {
        this.event.logEvent("PDF_CLICK_ORIENTATION", null);
        if (this.pdfPresenter != null) {
            this.pdfPresenter.setPageOrientation();
        }
    }

    public void onTickClick(View view) {
        this.event.logEvent("PDF_CLICK_CONFIRM", null);
        this.pdfPresenter.convertImageToPdf(this);
        this.binding.ivTickDone.setEnabled(false);
        this.binding.pbConvert.setVisibility(View.VISIBLE);
        this.binding.view.setVisibility(View.VISIBLE);
    }

    private void onClickOptionSize(Rectangle pageSize, int resId) {
        if (this.pdfPresenter != null) {
            this.pdfPresenter.setPageSize(pageSize);
        }
        this.binding.tvContentPdfPageSize.setText(resId);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.SHARE_REQUEST_CODE) {
            if (this.supportInAppReview != null) {
                this.supportInAppReview.showReview();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.adsBanner != null) {
            this.adsBanner.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (this.adsBanner != null) {
            this.adsBanner.pause();
        }
    }

    @Override
    protected void onDestroy() {
        if (this.adsBanner != null) {
            this.adsBanner.destroyAds();
        }

        if (this.pdfPresenter != null) {
            this.pdfPresenter.disposableAll();
        }

        System.gc();
        super.onDestroy();
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onCheckedChanged(@NonNull CompoundButton compoundButton, boolean isSelect) {
        switch (compoundButton.getId()) {
            case R.id.sw_pdf_page_margin:
                this.pdfPresenter.setPageMargin(isSelect);
                break;

            case R.id.sw_pdf_page_number:
                this.pdfPresenter.setPageNumber(this, isSelect);
                break;
        }
    }

    @Override
    public void showPageOrientation(int orientation) {
        this.binding.tvContentPdfOrientation.setText(orientation);
        boolean isPortrait = orientation == R.string.pdf_settings_portrait;
        this.binding.ivOrientation.setImageResource(isPortrait ? R.drawable.pdf_settings_vector_portrait : R.drawable.pdf_settings_vector_landscape);
    }

    @Override
    public void showPageSizeInit(int pageSize) {
        this.binding.tvContentPdfPageSize.setText(pageSize);
    }

    @Override
    public void showContentPageNumber(String content) {
        this.binding.tvContentPdfPageNumber.setText(content);
    }

    @Override
    public void onSuccessGetListImage() {
        this.binding.ivTickDone.setEnabled(true);
    }

    @Override
    public void onSuccessConvertPdf(List<String> filesPdf) {
        this.binding.view.setVisibility(View.GONE);
        this.binding.pbConvert.setVisibility(View.GONE);
        this.binding.tvPercent.setVisibility(View.GONE);
        this.binding.ivTickDone.setEnabled(true);
        FileUtils.sendfile(this, filesPdf);
    }

    @Override
    public void showNoMarginPage() {
        this.binding.tvContentPdfPageMargin.setText(R.string.pdf_settings_no_margin_page);
    }

    @Override
    public void showAddMarginPage() {
        this.binding.tvContentPdfPageMargin.setText(R.string.pdf_settings_add_margin_page);
    }

    @Override
    public void showPercent(int percent) {
        this.binding.tvPercent.setText(this.getString(R.string.all_percent, percent));
    }

    @Override
    public void onReviewComplete() {
    }

}
