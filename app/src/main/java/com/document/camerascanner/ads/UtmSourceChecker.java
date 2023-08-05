package com.document.camerascanner.ads;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.document.camerascanner.prefs.AppPref;

public class UtmSourceChecker implements InstallReferrerStateListener {

    @SuppressLint("StaticFieldLeak")
    private static UtmSourceChecker instance;

    private final Context context;

    private InstallReferrerClient referrerClient;
    private OnCheckInstallCallback callback;

    private UtmSourceChecker(Context context) {
        this.context = context;
    }

    public static boolean isAllowShowAd(Context context) {
        return AppPref.getInstance(context).isInstallUtmAllowShow() && isStoreVersion(context);
    }

    public static boolean isStoreVersion(Context context) {
        try {
            return !TextUtils.isEmpty(context.getPackageManager().getInstallerPackageName(context.getPackageName()));
        } catch (Throwable th) {
            return false;
        }
    }

    public static UtmSourceChecker getInstance(Context context) {
        if (instance == null) {
            instance = new UtmSourceChecker(context);
        }
        return instance;
    }

    public void rf(OnCheckInstallCallback callback) {
        this.callback = callback;

        if (AppPref.getInstance(this.context).isInstallUtmChecked()) {
            this.logInfo("Install referrer checked");
            if (callback != null) {
                callback.onSuccess();
                return;
            }
            return;
        }

        this.referrerClient = InstallReferrerClient.newBuilder(this.context).build();
        this.referrerClient.startConnection(this);
    }

    public void destroyRf() {
        if (this.referrerClient != null) {
            this.referrerClient.endConnection();
        }
    }

    private void logInfo(String message) {
        Log.i("InstallChecker", message);
    }

    public void onInstallReferrerSetupFinished(int i) {
        if (i == 0) {
            AppPref instance = AppPref.getInstance(context);

            try {
                String installReferrer = this.referrerClient.getInstallReferrer().getInstallReferrer();
                if (installReferrer.contains("google")) {
                    instance.setInstallUtmAllowShow();
                    this.logInfo("is install google-play");
                }

                this.logInfo(installReferrer + " isStore: " + isStoreVersion(this.context));
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            instance.setInstallUtmChecked();

            if (this.callback != null) {
                this.callback.onSuccess();
            }
        }
        this.logInfo("Code: " + i);
    }

    public void onInstallReferrerServiceDisconnected() {
        this.logInfo("onInstallReferrerServiceDisconnected");
    }

    public interface OnCheckInstallCallback {

        void onSuccess();
    }
}