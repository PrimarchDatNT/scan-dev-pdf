package com.document.camerascanner.features.update;

import android.app.Activity;
import android.content.IntentSender;
import android.view.View;

import com.document.camerascanner.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.Task;

public class SupportInAppUpdate {

    public static final int UPDATE_REQUEST_CODE = 63879;

    private final int mType;

    private final View mView;
    private final Activity mActivity;

    private AppUpdateManager updateManager;
    private Task<AppUpdateInfo> updateInfoTask;

    public SupportInAppUpdate(Activity activity, View view) {
        this.mActivity = activity;
        this.mView = view;
        this.mType = AppUpdateType.FLEXIBLE;
    }

    public void configInAppUpdate() {
        this.updateManager = AppUpdateManagerFactory.create(this.mActivity);
        this.updateInfoTask = this.updateManager.getAppUpdateInfo();
    }

    public void addOnSuccessListener(OnSuccessListener onSuccessListener) {
        if (onSuccessListener == null || this.updateInfoTask == null) {
            return;
        }
        this.updateInfoTask.addOnSuccessListener(onSuccessListener::onSuccess);
    }

    public void addOnErrorsListener(OnErrorListener onErrorListener) {
        if (this.updateInfoTask == null || onErrorListener == null) {
            return;
        }
        this.updateInfoTask.addOnFailureListener(onErrorListener::onErrors);
    }

    public void startUpdate(AppUpdateInfo appUpdateInfo) {
        if (this.updateInfoTask != null) {
            try {
                this.updateManager.startUpdateFlowForResult(appUpdateInfo, this.mType, this.mActivity, UPDATE_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        }
    }

    public void checkUpdateDownLoaded() {
        if (this.updateManager != null) {
            this.updateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    this.popupSnackbarForCompleteUpdate();
                }

                if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    try {
                        this.updateManager.startUpdateFlowForResult(appUpdateInfo, this.mType, this.mActivity, UPDATE_REQUEST_CODE);
                    } catch (IntentSender.SendIntentException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private void popupSnackbarForCompleteUpdate() {
        if (this.mView == null) {
            return;
        }

        try {
            Snackbar snackbar = Snackbar.make(this.mView, this.mActivity.getString(R.string.update_download_success), Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction(this.mActivity.getString(R.string.update_success_cta), view -> this.updateManager.completeUpdate());
            snackbar.setActionTextColor(this.mActivity.getResources().getColor(R.color.colorAccent));
            snackbar.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface OnSuccessListener {
        void onSuccess(AppUpdateInfo appUpdateInfo);
    }

    public interface OnErrorListener {
        void onErrors(Exception e);
    }
}
