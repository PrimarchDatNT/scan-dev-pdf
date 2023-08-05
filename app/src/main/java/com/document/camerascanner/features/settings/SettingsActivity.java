package com.document.camerascanner.features.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.document.camerascanner.R;
import com.document.camerascanner.databinding.ActivitySettingsBinding;
import com.document.camerascanner.features.about.PolicyViewerActivity;
import com.document.camerascanner.features.pdfsettings.OptionSizeDialog;
import com.document.camerascanner.features.pdfsettings.SizeType;
import com.document.camerascanner.features.review.SupportInAppReview;
import com.document.camerascanner.features.share.TypeQuality;
import com.document.camerascanner.prefs.AppPref;
import com.google.firebase.analytics.FirebaseAnalytics;

public class SettingsActivity extends AppCompatActivity {

    private boolean isPortrait = false;
    private boolean isShowMargin = false;
    private boolean isShowPageNumber = false;
    private boolean isAutoSave = false;

    private AppPref appPref;
    private FirebaseAnalytics event;
    private ActivitySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.init();
        this.event = FirebaseAnalytics.getInstance(this);
        this.event.logEvent("SETTINGS_OPEN", null);
    }

    private void init() {
        this.appPref = AppPref.getInstance(this);
        this.isPortrait = this.appPref.isPageOrientationPortrait();
        this.isShowPageNumber = this.appPref.isPageNumber();
        this.isShowMargin = this.appPref.isMarginPdf();
        this.isAutoSave = this.appPref.isAutoSaveDefaultName();
        this.initView();
    }

    private void initView() {
        this.binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        this.setContentView(this.binding.getRoot());

        this.binding.tvContentPdfPageNumber.setText(this.isShowPageNumber ? R.string.pdf_settings_show_page_number
                : R.string.settings_do_no_show_page_number);

        this.binding.swAutoSave.setChecked(this.isAutoSave);
        this.binding.swAutoSave.setOnCheckedChangeListener((compoundButton, b) -> this.updateAutoSave(b));
        this.binding.tvContentAutoSave.setText(this.isAutoSave ? R.string.settings_auto_save_enable : R.string.settings_auto_save_disable);

        this.binding.swPdfPageNumber.setChecked(this.isShowPageNumber);
        this.binding.swPdfPageNumber.setOnCheckedChangeListener((compoundButton, b) -> this.updatePageNumber(b));
        this.binding.swPdfPageMargin.setChecked(this.isShowMargin);
        this.binding.swPdfPageMargin.setOnCheckedChangeListener((compoundButton, b) -> this.updateMargin(b));

        this.binding.tvContentPdfPageMargin.setText(this.isShowMargin ? R.string.settings_show_margin_on_pdf_page
                : R.string.pdf_settings_no_margin_page);

        this.binding.tvSettingsPdfOrientation.setText(this.isPortrait ? R.string.pdf_settings_portrait
                : R.string.pdf_settings_landscape);

        this.binding.ivOrientation.setImageResource(this.isPortrait ? R.drawable.pdf_settings_vector_portrait
                : R.drawable.pdf_settings_vector_landscape);

        int option = this.appPref.getDefaultFilterOption();

        switch (option) {
            case DefaultFilterOpt.ORIGNAL:
                this.setFilterOption(R.string.enhance_filter_mode_original);
                break;

            case DefaultFilterOpt.BLACK_AND_WHITE:
                this.setFilterOption(R.string.enhance);
                break;

            case DefaultFilterOpt.GREY_SCALE:
                this.setFilterOption(R.string.enhance_filter_mode_grey_scale);
                break;

            case DefaultFilterOpt.MAGIC_COLOR:
                this.setFilterOption(R.string.enhance_filter_mode_magic_color);
                break;

            case DefaultFilterOpt.BLACK_AND_WHITE_2:
                this.setFilterOption(R.string.enhance_filter_mode_bnw2);
                break;

            case DefaultFilterOpt.NO_SHADOW:
                this.setFilterOption(R.string.enhance_filter_mode_no_shadow);
                break;

            case DefaultFilterOpt.LAST_USE_FILTER:
                this.setFilterOption(R.string.settings_last_used_filter);
                break;
        }

        int currentPageSize = this.appPref.getPageSizePdf();
        switch (currentPageSize) {
            case SizeType.A3:
                this.binding.tvSettingsPdfPageSize.setText(R.string.pdf_settings_size_a3);
                break;
            case SizeType.A4:
                this.binding.tvSettingsPdfPageSize.setText(R.string.pdf_settings_size_a4);
                break;
            case SizeType.A5:
                this.binding.tvSettingsPdfPageSize.setText(R.string.pdf_settings_size_a5);
                break;
            case SizeType.BUSINESS_CARD:
                this.binding.tvSettingsPdfPageSize.setText(R.string.pdf_settings_size_bussiness_card);
                break;
            case SizeType.LEGAL:
                this.binding.tvSettingsPdfPageSize.setText(R.string.pdf_settings_size_legal);
                break;
            case SizeType.LETTER:
                this.binding.tvSettingsPdfPageSize.setText(R.string.pdf_settings_size_letter);
                break;
        }

        int quality = this.appPref.getQualityType();
        switch (quality) {
            case TypeQuality.HIGH:
                this.binding.tvContentQuality.setText(R.string.all_share_quality_high);
                break;
            case TypeQuality.LOW:
                this.binding.tvContentQuality.setText(R.string.all_share_quality_low);
                break;
            case TypeQuality.MAX:
                this.binding.tvContentQuality.setText(R.string.all_share_quality_max);
                break;
            case TypeQuality.MEDIUM:
                this.binding.tvContentQuality.setText(R.string.all_share_quality_medium);
                break;

        }
    }

    private void updatePageNumber(boolean b) {
        this.appPref.setPageNumber(b);
        this.binding.tvContentPdfPageNumber.setText(b ? R.string.pdf_settings_show_page_number
                : R.string.settings_do_no_show_page_number);
    }

    private void updateMargin(boolean b) {
        this.appPref.setMarginPdf(b);
        this.binding.tvContentPdfPageMargin.setText(b ? R.string.settings_show_margin_on_pdf_page
                : R.string.pdf_settings_no_margin_page);
    }

    private void updateAutoSave(boolean b) {
        this.appPref.setAutoSaveDefaultName(b);
        this.binding.tvContentAutoSave.setText(b ? R.string.settings_auto_save_enable : R.string.settings_auto_save_disable);
    }

    public void onClickChangeFilter(View view) {
        this.event.logEvent("SETTINGS_CLICK_FILTER", null);
        this.showFilterOptions();
    }

    public void onClickAutoSave(View view) {
        this.event.logEvent("SETTINGS_CLICK_AUTO_SAVE", null);
        this.binding.swAutoSave.setChecked(!this.binding.swAutoSave.isChecked());
    }

    public void onClickShowQuality(View view) {
        OptionsQualityDialog optionsQualityDialog = new OptionsQualityDialog(this);
        optionsQualityDialog.setCallBackSelectQuality(quality -> this.binding.tvContentQuality.setText(quality));
        optionsQualityDialog.show(getSupportFragmentManager(), "DIALOG_QUALITY");
    }

    private void showFilterOptions() {
        OptionsFilterDialog dialog = new OptionsFilterDialog();
        dialog.setFilterListener(new OptionsFilterDialog.DialogOptionFilterListener() {
            @Override
            public void onClickOriginal() {
                event.logEvent("SETTINGS_CLICK_FILTER_ORIGINAL", null);
                updateFilterOption(R.string.enhance_filter_mode_original, DefaultFilterOpt.ORIGNAL);
            }

            @Override
            public void onClickBlackAndWhite() {
                event.logEvent("SETTINGS_CLICK_FILTER_BLACK_AND_WHITE", null);
                updateFilterOption(R.string.enhance, DefaultFilterOpt.BLACK_AND_WHITE);
            }

            @Override
            public void onClickBlackAndWhite2() {
                event.logEvent("SETTINGS_CLICK_FILTER_BLACK_AND_WHITE_2", null);
                updateFilterOption(R.string.enhance_filter_mode_bnw2, DefaultFilterOpt.BLACK_AND_WHITE_2);
            }

            @Override
            public void onClickNoShadow() {
                event.logEvent("SETTINGS_CLICK_FILTER_NO_SHADOW", null);
                updateFilterOption(R.string.enhance_filter_mode_no_shadow, DefaultFilterOpt.NO_SHADOW);
            }

            @Override
            public void onClickMagicColor() {
                event.logEvent("SETTINGS_CLICK_FILTER_MAGIC_COLOR", null);
                updateFilterOption(R.string.enhance_filter_mode_magic_color, DefaultFilterOpt.MAGIC_COLOR);
            }

            @Override
            public void onClickGrayScale() {
                event.logEvent("SETTINGS_CLICK_FILTER_GRAY_SCALE", null);
                updateFilterOption(R.string.enhance_filter_mode_grey_scale, DefaultFilterOpt.GREY_SCALE);
            }

            @Override
            public void onClickLastUsedFilter() {
                updateFilterOption(R.string.settings_last_used_filter, DefaultFilterOpt.LAST_USE_FILTER);
            }
        });
        dialog.show(this.getSupportFragmentManager(), "DIALOG_FILTER");
    }

    private void updateFilterOption(int resId, @DefaultFilterOpt int option) {
        this.setFilterOption(resId);
        this.appPref.setDefaulFilterOption(option);
    }

    private void setFilterOption(int resId) {
        this.binding.tvSettingsDefaultFilter.setText(resId);
    }

    public void onClickChangePageOrientation(View view) {
        this.event.logEvent("SETTINGS_CLICK_ORIENTATION", null);
        this.isPortrait = !this.isPortrait;
        String orientation = this.isPortrait ? "PORTRAIT" : "LANDSCAPE";
        this.event.logEvent("SETTINGS_ORIENTATION_" + orientation, null);
        this.appPref.setPageOrientationPortrait(this.isPortrait);
        this.binding.ivOrientation.setImageResource(this.isPortrait ? R.drawable.pdf_settings_vector_portrait
                : R.drawable.pdf_settings_vector_landscape);
        this.binding.tvSettingsPdfOrientation.setText(this.isPortrait ? R.string.pdf_settings_portrait
                : R.string.pdf_settings_landscape);
    }

    public void showPageNumber(View view) {
        this.event.logEvent("SETTINGS_CLICK_NUMBER", null);
        boolean isCheck = this.binding.swPdfPageNumber.isChecked();
        this.event.logEvent("SETTINGS_CLICK_NUMBER_" + (isCheck ? "ON" : "OFF"), null);
        this.binding.swPdfPageNumber.setChecked(!isCheck);
        this.updatePageNumber(!isCheck);
    }

    public void onClickShowPageSize(View view) {
        this.event.logEvent("SETTINGS_CLICK_SIZE", null);
        this.showPageSizeOptions();
    }

    private void showPageSizeOptions() {
        OptionSizeDialog dialog = new OptionSizeDialog(AppPref.getInstance(this).getPageSizePdf());
        dialog.setListener(new OptionSizeDialog.DialogOptionSizeListener() {
            @Override
            public void onClickLetter() {
                event.logEvent("SETTINGS_CLICK_SIZE_LETTER", null);
                onClickOptionSize(SizeType.LETTER, R.string.pdf_settings_size_letter);
            }

            @Override
            public void onClickA4() {
                event.logEvent("SETTINGS_CLICK_SIZE_A4", null);
                onClickOptionSize(SizeType.A4, R.string.pdf_settings_size_a4);
            }

            @Override
            public void onClickLegal() {
                event.logEvent("SETTINGS_CLICK_SIZE_LEGAL", null);
                onClickOptionSize(SizeType.LEGAL, R.string.pdf_settings_size_legal);
            }

            @Override
            public void onClickA3() {
                event.logEvent("SETTINGS_CLICK_SIZE_A3", null);
                onClickOptionSize(SizeType.A3, R.string.pdf_settings_size_a3);
            }

            @Override
            public void onClickA5() {
                event.logEvent("SETTINGS_CLICK_SIZE_A5", null);
                onClickOptionSize(SizeType.A5, R.string.pdf_settings_size_a5);
            }

            @Override
            public void onClickBussinesCard() {
                event.logEvent("SETTINGS_CLICK_SIZE_CARD", null);
                onClickOptionSize(SizeType.BUSINESS_CARD, R.string.pdf_settings_size_bussiness_card);
            }
        });
        dialog.show(this.getSupportFragmentManager(), "OPTION_SIZE");
    }

    public void onClickOptionSize(@SizeType int sizeType, int resId) {
        this.appPref.setPageSizePdf(sizeType);
        this.binding.tvSettingsPdfPageSize.setText(resId);
    }

    public void onClickShowPageMargin(View view) {
        this.event.logEvent("SETTINGS_CLICK_MARGIN", null);
        boolean isCheck = this.binding.swPdfPageMargin.isChecked();
        this.event.logEvent("SETTINGS_CLICK_MARGIN_" + (isCheck ? "ON" : "OFF"), null);
        this.binding.swPdfPageMargin.setChecked(!isCheck);
        this.updateMargin(!isCheck);
    }

    public void onClickRateUs(View view) {
        this.event.logEvent("SETTINGS_CLICK_RATE", null);
        SupportInAppReview.openMarket(this);
    }

    public void onClickPrivacyPolicy(View view) {
        this.event.logEvent("SETTINGS_CLICK_POLICY", null);
        this.openPolicy();
    }

    private void openPolicy() {
        Intent iPolicy = new Intent(this, PolicyViewerActivity.class);
        iPolicy.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(iPolicy);
    }

    public void onClickBack(View view) {
        this.onBackPressed();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
    }

}