package com.document.camerascanner.features.review;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.document.camerascanner.prefs.AppPref;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.Task;

public class SupportInAppReview {

    private final Activity activity;
    private final OnReviewListener callback;
    private final ReviewManager reviewManager;

    private ReviewInfo reviewInfo;

    public SupportInAppReview(Activity activity, OnReviewListener callback) {
        this.activity = activity;
        this.callback = callback;
        this.reviewManager = ReviewManagerFactory.create(this.activity);

        boolean haventShowRate = AppPref.getInstance(this.activity).isShowSessionReview();
        if (!haventShowRate) {
            Task<ReviewInfo> reviewInfoTask = this.reviewManager.requestReviewFlow();
            reviewInfoTask.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    this.reviewInfo = task.getResult();
                }
            });
        }
    }

    public static boolean isInstalledPackage(String packageName, @NonNull Context context) {
        try {
            if (context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_UNINSTALLED_PACKAGES) != null)
                return true;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void openMarket(@NonNull Context context) {
        String packageName = context.getPackageName();
        if (TextUtils.isEmpty(packageName)) {
            return;
        }

        Intent iOpenMarket = new Intent(Intent.ACTION_VIEW).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (isInstalledPackage("com.android.vending", context)) {
            iOpenMarket.setClassName("com.android.vending", "com.google.android.finsky.activities.LaunchUrlHandlerActivity");
            iOpenMarket.setData(Uri.parse("market://details?id=" + packageName));
        } else if (isInstalledPackage("com.google.market", context)) {
            iOpenMarket.setClassName("com.google.market", "com.google.android.finsky.activities.LaunchUrlHandlerActivity");
            iOpenMarket.setData(Uri.parse("market://details?id=" + packageName));
        } else {
            iOpenMarket.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
        }

        try {
            context.startActivity(iOpenMarket);
        } catch (Exception e) {
            Intent intent = new Intent(Intent.ACTION_VIEW).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
            context.startActivity(intent);
            e.printStackTrace();
        }
    }

    public void showReview() {
        boolean haventShowRate = AppPref.getInstance(this.activity).isShowSessionReview();

        if (this.reviewManager != null && this.reviewInfo != null && !haventShowRate) {
            Task<Void> flow = this.reviewManager.launchReviewFlow(this.activity, this.reviewInfo);

            flow.addOnCompleteListener(voidTask -> {
                AppPref.getInstance(this.activity).setSessionShowReview(true);
                if (this.callback != null) {
                    this.callback.onReviewComplete();
                }
            });
        }
    }

    public interface OnReviewListener {
        void onReviewComplete();
    }
}
