package com.document.camerascanner.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;

import com.document.camerascanner.features.detailshow.documents.DetailDocumentActivity;
import com.document.camerascanner.features.detailshow.folders.DetailFolderActivity;
import com.document.camerascanner.features.main.MainActivity;
import com.document.camerascanner.features.save.StartType;
import com.document.camerascanner.features.settings.DefaultFilterOpt;
import com.document.camerascanner.prefs.AppPref;

public class AppUtils {

    public static void showKeyboard(@NonNull Context context) {
        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    public static void closeKeyboard(@NonNull Context context) {
        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }

    public static int getAppVersionCode(@NonNull Context mContext) {
        int versionCode = 0;
        try {
            PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
            versionCode = packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionCode;
    }

    public static int getFilterOption(Context context) {
        AppPref pref = AppPref.getInstance(context);
        @DefaultFilterOpt int option = pref.getDefaultFilterOption();
        return option == DefaultFilterOpt.LAST_USE_FILTER ? pref.getCurrentFilter() : option;
    }

    public static int getCallActivity(boolean isDocument, boolean isEmptySavePref) {
        if (isEmptySavePref) {
            return StartType.START_FROM_MAIN;
        }

        if (isDocument) {
            return StartType.START_FROM_DETAIL_DOC;
        }

        return StartType.START_FROM_DETAIL_FOLDER;
    }

    public static void startActivity(Context context) {
        AppPref appPref = AppPref.getInstance(context);
        boolean isDocument = appPref.isDocument();
        int startType = getCallActivity(isDocument, appPref.getSaveId() < 1);

        Class<?> nextActivity = MainActivity.class;
        switch (startType) {
            case StartType.START_FROM_MAIN:
                nextActivity = MainActivity.class;
                break;
            case StartType.START_FROM_DETAIL_FOLDER:
                nextActivity = DetailFolderActivity.class;
                break;
            case StartType.START_FROM_DETAIL_DOC:
                nextActivity = DetailDocumentActivity.class;
                break;
        }

        Intent intent = new Intent(context, nextActivity);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        ((Activity) context).finish();
    }

    public static boolean isConnectingInternet(@NonNull Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = null;
        if (cm != null) {
            info = cm.getActiveNetworkInfo();
        }
        return info != null && info.isConnected();
    }

    public static int[] getScreenSize(@NonNull Activity activity) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;
        return new int[]{width, height};
    }

    public static void addImageToContentProvider(Context context, Uri uri) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(uri);
        context.sendBroadcast(mediaScanIntent);
    }

    public static void openGallery(Context context) {
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setType("image/*");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

}
