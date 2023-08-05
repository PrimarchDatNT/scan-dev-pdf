package com.document.camerascanner.utils;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;

import com.document.camerascanner.databases.AppDatabases;
import com.document.camerascanner.databases.model.PageItem;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImageUtils {

    public static Bitmap rotate(Bitmap bitmap, int angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static void importImage(Context context, @NotNull Intent data) {
        AppDatabases appDatabases = AppDatabases.getInstance(context);
        File fileSource = context.getExternalFilesDir(Constants.ALL_TEMP);
        DbUtils.clearPreviousSession(appDatabases);

        if (data.getData() == null) {
            ClipData clipData = data.getClipData();
            if (clipData == null) {
                return;
            }

            List<PageItem> listPage = new ArrayList<>();

            for (int index = 0; index < clipData.getItemCount(); index++) {
                ClipData.Item item = clipData.getItemAt(index);
                String realPath = FileUtils.getRealUri(context, item.getUri());
                listPage.add(DbUtils.createTempPage(context, fileSource, realPath, index + 1));
            }

            appDatabases.pageDao().insertListEntityNoRx(listPage);
            return;
        }

        Uri uri = data.getData();
        String realPath = FileUtils.getRealUri(context, uri);
        appDatabases.pageDao().insertEntityNoRx(DbUtils.createTempPage(context, fileSource, realPath, 1));
    }

    public static @Nullable Bitmap decodeOrginalBitmap(@NonNull Uri uri, int reqW, int reH) {
        if (uri.getPath() == null) {
            return null;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(uri.getPath(), options);

        options.inSampleSize = calculateInSampleSize(options, reqW, reH);
        options.inJustDecodeBounds = false;

        Bitmap bitmap = BitmapFactory.decodeFile(uri.getPath(), options);
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(uri.getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        int orientation = 0;
        if (exif != null) {
            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        }

        int angle;
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                angle = 90;
                break;

            case ExifInterface.ORIENTATION_ROTATE_180:
                angle = 180;
                break;

            case ExifInterface.ORIENTATION_ROTATE_270:
                angle = 270;
                break;

            default:
                angle = 0;
                break;
        }

        Matrix matrix = new Matrix();
        matrix.postRotate(angle);

        if (bitmap == null) {
            return null;
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private static int calculateInSampleSize(@NonNull BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

/*        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }*/

        if (height > reqHeight || width > reqWidth) {

            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            inSampleSize = Math.min(heightRatio, widthRatio);
        }

        return inSampleSize;
    }

    public static void shareToShare(Context context, @NonNull List<String> listDocument) {
        ArrayList<String> itemsShare = new ArrayList<>(listDocument);
        FileUtils.sendfile(context, itemsShare);
    }

    public static float dp2px(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().getDisplayMetrics());
    }

}
