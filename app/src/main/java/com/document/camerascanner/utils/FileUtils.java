package com.document.camerascanner.utils;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.document.camerascanner.BuildConfig;
import com.document.camerascanner.R;
import com.document.camerascanner.databases.model.DocumentItem;
import com.document.camerascanner.databases.model.PageItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FileUtils {

    @NonNull
    public static String createNameFolder(@NonNull Context context) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMdd_HHmmss", Locale.getDefault());
        String newFolderName = dateFormat.format(System.currentTimeMillis());
        return context.getString(R.string.home_created_folder_name, newFolderName);
    }

    @SuppressLint("WrongConstant")
    public static void sendfile(Context context, @NonNull List<String> listInput) {
        ArrayList<Uri> listUri = new ArrayList<>();
        for (String createUri : listInput) {
            listUri.add(createUri(context, createUri));
        }

        Intent intent = new Intent();
        intent.setAction("android.intent.action.SEND_MULTIPLE");
        intent.putParcelableArrayListExtra("android.intent.extra.STREAM", listUri);
        intent.setType("*/*");
        intent.addFlags(3);

        try {
            Intent iShare = Intent.createChooser(intent, context.getString(R.string.pdf_settings_send_to));
            ((Activity) context).startActivityForResult(iShare, Constants.SHARE_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static Uri createUri(Context context, String str) {
        if (Build.VERSION.SDK_INT < 24) {
            return Uri.fromFile(new File(str));
        }

        try {
            String provideName = BuildConfig.APPLICATION_ID + ".provider";
            return FileProvider.getUriForFile(context, provideName, new File(str));
        } catch (IllegalArgumentException e) {
            return new Uri.Builder().build();
        }
    }

    public static String getRealUri(Context context, Uri uri) {
        try {
            if (uri == null) {
                return "";
            }

            if (uri.getPath() == null) {
                return "";
            }

            File file = new File(uri.getPath());
            String[] filePath = file.getPath().split(":");
            String imgID = filePath[filePath.length - 1];
            Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    null, MediaStore.Images.Media._ID + "=?", new String[]{imgID}, null);

            if (cursor == null) {
                return "";
            }

            cursor.moveToFirst();
            String imagePath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            cursor.close();
            return imagePath;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static boolean isInvalidFDocName(@NonNull String dirName) {
        for (int i = 0; i < dirName.length(); i++) {
            if (dirName.charAt(i) == '|'
                    || dirName.charAt(i) == '?'
                    || dirName.charAt(i) == ':'
                    || dirName.charAt(i) == '/'
                    || dirName.charAt(i) == '\\'
                    || dirName.charAt(i) == '['
                    || dirName.charAt(i) == ']'
                    || dirName.charAt(i) == '+'
                    || dirName.charAt(i) == '*'
                    || dirName.charAt(i) == '"'
                    || dirName.charAt(i) == '<'
                    || dirName.charAt(i) == '>'
                    || dirName.charAt(i) == '\''
                    || dirName.charAt(i) == '©'
                    || dirName.charAt(i) == '®'
                    || dirName.charAt(i) == '™') {
                return true;
            }
        }
        return false;
    }

    public static void startImportImage(@NonNull Activity activity) {
        String nameManufacturer = Build.MANUFACTURER;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        if (nameManufacturer.contains("Xiaomi")) {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
                intent = new Intent(Intent.ACTION_GET_CONTENT);
            }
        }
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/png", "image/jpeg"});
        activity.startActivityForResult(intent, Constants.CODE_PICK_IMAGE_INTENT);
    }

    public static boolean isFileImage(@NonNull File file) {
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".png") || fileName.endsWith(".jpg");
    }

    public static void copyFile(File fileInput, File fileOutput) {
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            inputStream = new FileInputStream(fileInput);
            outputStream = new FileOutputStream(fileOutput);
            int length;
            byte[] buffer = new byte[1024];

            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean isEnhanced(@NonNull File file) {
        String fileName = file.getName();
        String nameSpecies = fileName.split("_")[0];
        if (TextUtils.isEmpty(nameSpecies)) {
            return false;
        }

        return nameSpecies.equals(Constants.IMAGE_ENHANCE);
    }

    @NonNull
    public static String createNameFileNamesake(String pathTo, @NonNull File file, @NonNull Context context, long timeCurrent) {
        File fileTo = new File(pathTo);
        String nameFile = file.getName();
        String nameOrigin = nameFile.substring(0, nameFile.lastIndexOf("_"));
        String dotFile = nameFile.substring(nameFile.lastIndexOf(".") + 1);
        String nameOri = nameFile.substring(0, nameFile.lastIndexOf("."));
        String newName = context.getString(R.string.all_name_current_time_ori, nameOrigin, timeCurrent, dotFile);

        File newNameFile = new File(fileTo, newName);
        if (!newNameFile.exists()) {
            return newName;
        }

        if (file.exists()) {
            int i = 1;
            while (true) {
                newName = context.getString(R.string.all_namecurrenttime_copy, nameOri, timeCurrent, i, dotFile);
                File newFile = new File(fileTo, newName);
                if (!newFile.exists()) {
                    return newName;
                }
                i++;
            }
        }
        return nameFile;
    }


    public static String createNameFileDuplicate(Context context, File fileGoal, DocumentItem documentItem, PageItem pageItem) {
        if (documentItem == null) {
            return Constants.IMAGE_ENHANCE + "_" + System.currentTimeMillis();
        }
        String dotFile = pageItem.getEnhanceUri().substring(pageItem.getEnhanceUri().lastIndexOf(".") + 1);
        String fileName = context.getString(R.string.all_namecurrenttime, documentItem.getName(), pageItem.getPosition(), dotFile);
        File file = new File(fileGoal, fileName);
        if (file.exists()) {
            int i = 0;
            while (true) {
                String newName = context.getString(R.string.all_namecurrenttime_copy, documentItem.getName(), pageItem.getPosition(), i, dotFile);
                File fileExists = new File(fileGoal, newName);
                if (!fileExists.exists()) {
                    return newName;
                }
                i++;
            }
        }
        return fileName;
    }


    public static String createNameFilePdf(Context context, String pathTo, File file) {
        File fileTo = new File(pathTo);
        String nameFile = file.getName();
        if (file.isDirectory()) {
            String newName = nameFile + Constants.PDF_EXTENSION;
            File filePdf = new File(fileTo, newName);
            if (!filePdf.exists()) {
                return newName;
            }
            int i = 1;
            while (true) {
                newName = context.getString(R.string.all_namesake_folder, nameFile, i, "pdf");
                filePdf = new File(fileTo, newName);
                if (!filePdf.exists()) {
                    return newName;
                }
                i++;
            }
        }

        nameFile = nameFile.substring(0, nameFile.lastIndexOf("."));
        String newName = nameFile + Constants.PDF_EXTENSION;
        File filePdf = new File(fileTo, newName);
        if (!filePdf.exists()) {
            return newName;
        }
        int i = 1;
        while (true) {
            newName = context.getString(R.string.all_namesake_folder, nameFile, i, "pdf");
            filePdf = new File(fileTo, newName);
            if (!filePdf.exists()) {
                return newName;
            }
            i++;
        }
    }

    public static void deleteFile(@NonNull Uri uri) {
        if (uri.getPath() == null) {
            return;
        }

        File fdelete = new File(uri.getPath());
        if (fdelete.exists()) {
            System.out.println(fdelete.delete() ? "file Deleted :" : "file not Deleted");
        }
    }

    public static void deletePageItem(PageItem pageItem) {
        if (pageItem == null) {
            return;
        }

        if (pageItem.getOrgUri() != null && !TextUtils.isEmpty(pageItem.getOrgUri())) {
            FileUtils.deleteFile(Uri.parse(pageItem.getOrgUri()));

            String cropFile = pageItem.getOrgUri().replaceAll(Constants.IMAGE_ORIGINAL, Constants.IMAGE_CROP);
            FileUtils.deleteFile(Uri.parse(cropFile));
        }

        if (pageItem.getEnhanceUri() != null && !TextUtils.isEmpty(pageItem.getEnhanceUri())) {
            FileUtils.deleteFile(Uri.parse(pageItem.getEnhanceUri()));
        }
    }

    public static void openFolder(Context context, String location) {

//        Uri uriSelect = Uri.parse(Environment.getExternalStorageDirectory() + "/Scano");
        Uri uri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", new File(Environment.getExternalStorageDirectory(), "Scano"));
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "resource/folder");
        context.startActivity(Intent.createChooser(intent, "Select the app to open"));
    }

}
