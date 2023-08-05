package com.document.camerascanner.prefs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.document.camerascanner.features.enhance.FilterType;
import com.document.camerascanner.features.main.TypeSort;
import com.document.camerascanner.features.settings.DefaultFilterOpt;
import com.document.camerascanner.features.share.TypeQuality;

public class AppPref {

    private static AppPref instance;

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    @SuppressLint("CommitPrefEdits")
    private AppPref(@NonNull Context context) {
        this.pref = context.getSharedPreferences(ConstantsPrefs.CAMSCANNER_PREF, Context.MODE_PRIVATE);
        this.editor = this.pref.edit();
    }

    public static AppPref getInstance(Context context) {
        if (instance == null) {
            instance = new AppPref(context);
        }
        return instance;
    }

    public void acceptPolicy() {
        this.editor.putBoolean(ConstantsPrefs.K_ACCEPT_POLICY, true).apply();
    }

    public boolean isAcceptedPolicy() {
        return this.pref.getBoolean(ConstantsPrefs.K_ACCEPT_POLICY, false);
    }

    public void setStatusTypeShowGrid(boolean typeShowGrid) {
        this.editor.putBoolean(ConstantsPrefs.KEY_STATUS_TYPE_SHOW_GRID, typeShowGrid).apply();
    }

    public boolean isTypeShowGrid() {
        return this.pref.getBoolean(ConstantsPrefs.KEY_STATUS_TYPE_SHOW_GRID, false);
    }

    public void setCameraMode(boolean value) {
        this.editor.putBoolean(ConstantsPrefs.K_CAMERA_MODE, value).apply();
    }

    public boolean isBatchMode() {
        return this.pref.getBoolean(ConstantsPrefs.K_CAMERA_MODE, false);
    }

    public boolean getFlashMode() {
        return this.pref.getBoolean(ConstantsPrefs.K_CAMERA_FLASH_MODE, false);
    }

    @FilterType
    public int getCurrentFilter() {
        return this.pref.getInt(ConstantsPrefs.K_CURRENT_FILTER, FilterType.MAGIC_COLOR);
    }

    public void setCurrentFilter(int currentFilter) {
        this.editor.putInt(ConstantsPrefs.K_CURRENT_FILTER, currentFilter).apply();
    }

    public void setDefaulFilterOption(@DefaultFilterOpt int option) {
        this.editor.putInt(ConstantsPrefs.KEY_DEFAULT_FILTER_OPTION, option).apply();
    }

    public int getDefaultFilterOption() {
        return this.pref.getInt(ConstantsPrefs.KEY_DEFAULT_FILTER_OPTION, DefaultFilterOpt.BLACK_AND_WHITE_2);
    }

    public boolean isShowSessionReview() {
        return this.pref.getBoolean(ConstantsPrefs.KEY_SESSION_SHOW_REVIEW, false);
    }

    public void setSessionShowReview(boolean isShow) {
        this.editor.putBoolean(ConstantsPrefs.KEY_SESSION_SHOW_REVIEW, isShow).apply();
    }

    public boolean isCallShowReview() {
        return this.pref.getBoolean(ConstantsPrefs.KEY_CALL_SHOW_REVIEW, false);
    }

    public void setCallShowReview(boolean isShow) {
        this.editor.putBoolean(ConstantsPrefs.KEY_CALL_SHOW_REVIEW, isShow).apply();
    }

    public int getSortType() {
        return this.pref.getInt(ConstantsPrefs.KEY_TYPE_SORT, TypeSort.SORT_NAME);
    }

    public void setSortType(int sortType) {
        this.editor.putInt(ConstantsPrefs.KEY_TYPE_SORT, sortType).apply();
    }

    public int getSortTypeFolder() {
        return this.pref.getInt(ConstantsPrefs.KEY_TYPE_SORT_FOLDER, TypeSort.SORT_NAME);
    }

    public void setSortTypeFolder(int sortTypeFolder) {
        this.editor.putInt(ConstantsPrefs.KEY_TYPE_SORT_FOLDER, sortTypeFolder).apply();
    }

    public void setQualityType(int qualityType) {
        this.editor.putInt(ConstantsPrefs.KEY_TYPE_QUALITY, qualityType).apply();
    }

    public int getQualityType() {
        return this.pref.getInt(ConstantsPrefs.KEY_TYPE_QUALITY, TypeQuality.HIGH);
    }

    public String getSessionDocName() {
        return this.pref.getString(ConstantsPrefs.KEY_SESSION_DOCUMENT_NAME, "");
    }

    public void setSessionDocName(String docName) {
        this.editor.putString(ConstantsPrefs.KEY_SESSION_DOCUMENT_NAME, docName).apply();
    }

    public int getSaveId() {
        return this.pref.getInt(ConstantsPrefs.KEY_SAVE_ID, -1);
    }

    public void setSaveId(int saveId) {
        this.editor.putInt(ConstantsPrefs.KEY_SAVE_ID, saveId).apply();
    }

    public boolean isDocument() {
        return this.pref.getBoolean(ConstantsPrefs.KEY_IS_DOCUMENT, false);
    }

    public void setSaveInDocument(boolean isDocument) {
        this.editor.putBoolean(ConstantsPrefs.KEY_IS_DOCUMENT, isDocument).apply();
    }

    public void removePref() {
        this.editor.remove(ConstantsPrefs.KEY_SAVE_ID).apply();
        this.editor.remove(ConstantsPrefs.KEY_IS_DOCUMENT).apply();
    }

    public boolean isMarginPdf() {
        return this.pref.getBoolean(ConstantsPrefs.KEY_MARGIN_PDF, false);
    }

    public void setMarginPdf(boolean isMargin) {
        this.editor.putBoolean(ConstantsPrefs.KEY_MARGIN_PDF, isMargin).apply();
    }

    public int getPageSizePdf() {
        return this.pref.getInt(ConstantsPrefs.KEY_PAGE_SIZE_PDF, 2);
    }

    public void setPageSizePdf(int typeSize) {
        this.editor.putInt(ConstantsPrefs.KEY_PAGE_SIZE_PDF, typeSize).apply();
    }

    public boolean isPageNumber() {
        return this.pref.getBoolean(ConstantsPrefs.KEY_PAGE_NUMBER_PDF, true);
    }

    public void setPageNumber(boolean isPageNumber) {
        this.editor.putBoolean(ConstantsPrefs.KEY_PAGE_NUMBER_PDF, isPageNumber).apply();
    }

    public boolean isPageOrientationPortrait() {
        return this.pref.getBoolean(ConstantsPrefs.KEY_ORIENTATION_PDF, true);
    }

    public void setPageOrientationPortrait(boolean isPortrait) {
        this.editor.putBoolean(ConstantsPrefs.KEY_ORIENTATION_PDF, isPortrait).apply();
    }

    public boolean isShowAdsInter() {
        return System.currentTimeMillis() - this.pref.getLong(ConstantsPrefs.KEY_LAST_TIME_SHOW_ADS_INTER, 0) > 120000;
    }

    public void setLastTimeShowAdsInter(long number) {
        this.editor.putLong(ConstantsPrefs.KEY_LAST_TIME_SHOW_ADS_INTER, number).apply();
    }

    public void setShowSpotlight(String firstTimeKey, boolean isShown) {
        this.editor.putBoolean(firstTimeKey, isShown).apply();
    }

    public boolean isShowSpotlight(String key) {
        return this.pref.getBoolean(key, true);
    }

    public void setInstallUtmAllowShow() {
        this.editor.putBoolean(ConstantsPrefs.K_INSTALL_UTM_SOURCE_SHOW, true).apply();
    }

    public boolean isInstallUtmAllowShow() {
        return this.pref.getBoolean(ConstantsPrefs.K_INSTALL_UTM_SOURCE_SHOW, false);
    }

    public void setInstallUtmChecked() {
        this.editor.putBoolean(ConstantsPrefs.K_INSTALL_UTM_SOURCE_CHECK, true).apply();
    }

    public boolean isInstallUtmChecked() {
        return this.pref.getBoolean(ConstantsPrefs.K_INSTALL_UTM_SOURCE_CHECK, false);
    }

    public void setCreatedRootFolder() {
        this.editor.putBoolean(ConstantsPrefs.K_IS_CREATE_ROOT_FOLDER, true).apply();
    }

    public boolean isCreatedRootFolder() {
        return this.pref.getBoolean(ConstantsPrefs.K_IS_CREATE_ROOT_FOLDER, false);
    }

    public void setCreatedTempDocument() {
        this.editor.putBoolean(ConstantsPrefs.K_IS_CREATE_TEMP_DOCUMENT, true).apply();
    }

    public boolean isCreatedTempDocument() {
        return this.pref.getBoolean(ConstantsPrefs.K_IS_CREATE_TEMP_DOCUMENT, false);
    }

    public void setAutoSaveDefaultName(boolean isAutoSave) {
        this.editor.putBoolean(ConstantsPrefs.KEY_AUTO_SAVE_WITH_DEFAULT_NAME, isAutoSave).apply();
    }

    public boolean isAutoSaveDefaultName() {
        return this.pref.getBoolean(ConstantsPrefs.KEY_AUTO_SAVE_WITH_DEFAULT_NAME, false);
    }

}
