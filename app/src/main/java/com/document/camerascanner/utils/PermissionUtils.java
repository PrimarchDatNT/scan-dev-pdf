package com.document.camerascanner.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;

import org.jetbrains.annotations.NotNull;

public class PermissionUtils {

/*    public static void setPermission(Activity activity, String permission, int code) {
        ActivityCompat.requestPermissions(activity, new String[]{permission}, code);
    }

    public static void setPermissionStorage(Activity activity, int code) {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, code);
    }

    public boolean checkAllPermissionApp(Context context) {
        return checkPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) && checkPermission(context, Manifest.permission.CAMERA);
    }*/

    public static boolean checkPermission(Context context, String permission) {
        return ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static void startSettingsPermissionStorage(@NotNull Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
        intent.setData(uri);
        Activity activity = (Activity) context;
        activity.startActivityForResult(intent, Constants.CODE_PERMISSION_STORAGE);
    }


}
