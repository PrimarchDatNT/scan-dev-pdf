package com.document.camerascanner.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.document.camerascanner.R;
import com.document.camerascanner.databases.AppDatabases;
import com.document.camerascanner.databases.model.DocumentItem;
import com.document.camerascanner.databases.model.FolderItem;
import com.document.camerascanner.databases.model.PageItem;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class DbUtils {

    @NonNull
    public static PageItem createTempPage(@NonNull Context context, File fileSource, String realPath, int position) {
        File file = new File(realPath);
        String fileName = file.getName();
        String dotFile = fileName.substring(fileName.lastIndexOf("."));
        long createTime = System.currentTimeMillis();
        fileName = context.getString(R.string.all_name_original, createTime, dotFile);
        File fileCopy = new File(fileSource, fileName);
        FileUtils.copyFile(file, fileCopy);

        PageItem pageItem = new PageItem();
        pageItem.setName(file.getName());
        pageItem.setParentId(1);
        pageItem.setLoading(true);
        pageItem.setPosition(position);
        pageItem.setOrgUri(fileCopy.getPath());
        return pageItem;
    }

    public static void clearPreviousSession(AppDatabases appDatabases) {
        if (appDatabases == null) {
            return;
        }

        List<PageItem> listPreviousTemp = appDatabases.pageDao().getPreviousTempPage();
        if (listPreviousTemp == null) {
            return;
        }

        for (PageItem pageItem : listPreviousTemp) {
            if (!TextUtils.isEmpty(pageItem.getOrgUri())) {
                FileUtils.deleteFile(Uri.parse(pageItem.getOrgUri()));

                String cropFile = pageItem.getOrgUri().replaceAll(Constants.IMAGE_ORIGINAL, Constants.IMAGE_CROP);
                FileUtils.deleteFile(Uri.parse(cropFile));
            }

            if (!TextUtils.isEmpty(pageItem.getEnhanceUri())) {
                FileUtils.deleteFile(Uri.parse(pageItem.getEnhanceUri()));
            }
        }

        appDatabases.pageDao().deleteTempPage();
    }

    public static void createNameFolderDefault(Context context, CallBackFolderNameDefault callBackFolderNameDefault) {
        Single.create((SingleOnSubscribe<String>) emitter -> {
            AppDatabases appDatabases = AppDatabases.getInstance(context);
            List<FolderItem> folderItems = appDatabases.folderDao().getListFolderNoRx();
            String nameFolder = context.getString(R.string.all_name_folder_default);

            boolean isExists = false;
            for (FolderItem folderItem : folderItems) {
                if (folderItem.getName().equals(nameFolder)) {
                    isExists = true;
                    break;
                }
            }
            if (isExists) {
                int i = 1;
                while (true) {
                    nameFolder = context.getString(R.string.all_name_folder_default_duplicate, i);
                    int size = folderItems.size();
                    int count = 0;
                    for (FolderItem folderItem : folderItems) {
                        if (!folderItem.getName().equals(nameFolder)) {
                            count++;
                        }
                    }
                    if (count == size) {
                        break;
                    }
                    i++;
                }
            }
            emitter.onSuccess(nameFolder);
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<String>() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        if (callBackFolderNameDefault != null) {
                            callBackFolderNameDefault.onDisposable(d);
                        }
                    }

                    @Override
                    public void onSuccess(@io.reactivex.annotations.NonNull String s) {
                        if (callBackFolderNameDefault != null) {
                            callBackFolderNameDefault.onSuccess(s);
                        }
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        if (callBackFolderNameDefault != null) {
                            callBackFolderNameDefault.onError();
                        }
                    }
                });

    }

    public static void createNewFolder(Context context, @NotNull String folderName, CreateFolderCallback createFolderCallback) {
        if (folderName.isEmpty()) {
            Toast.makeText(context, R.string.home_folder_et_empty, Toast.LENGTH_SHORT).show();
            createFolderCallback.onError();
            return;
        }

        if (FileUtils.isInvalidFDocName(folderName)) {
            Toast.makeText(context, R.string.save_alert_input_invalid_name, Toast.LENGTH_SHORT).show();
            createFolderCallback.onError();
            return;
        }

        FolderItem folderItem = new FolderItem();
        folderItem.setName(folderName);
        folderItem.setChildCount(0);
        folderItem.setParentId(0);

        AppDatabases appDatabases = AppDatabases.getInstance(context);

        Single.create((SingleOnSubscribe<FolderItem>) emitter -> {
            boolean isExistsName = appDatabases.folderDao().isExistsFolderNameNoRx(folderName);

            if (isExistsName) {
                emitter.onError(new Throwable());
                return;
            }

            Long id = appDatabases.folderDao().insertEntityNoRx(folderItem);
            if (id == null) {
                emitter.onError(new Throwable());
                return;
            }

            emitter.onSuccess(appDatabases.folderDao().getFolderItemById(id.intValue()));
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<FolderItem>() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        if (createFolderCallback != null) {
                            createFolderCallback.onDisposable(d);
                        }
                    }

                    @Override
                    public void onSuccess(@io.reactivex.annotations.NonNull FolderItem folderItem) {
                        Toast.makeText(context, R.string.home_folder_created_success, Toast.LENGTH_SHORT).show();
                        if (createFolderCallback != null) {
                            createFolderCallback.onSuccess(folderItem);
                        }
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        e.printStackTrace();
                        Toast.makeText(context, R.string.home_folder_exits, Toast.LENGTH_SHORT).show();
                        if (createFolderCallback != null) {
                            createFolderCallback.onError();
                        }
                    }
                });
    }

    public static void createNewDocument(Context context, String documentName, int folderId, CreateDocumentCallback callback) {
        if (TextUtils.isEmpty(documentName)) {
            Toast.makeText(context, R.string.home_folder_et_empty, Toast.LENGTH_SHORT).show();
            callback.onError();
            return;
        }

        if (FileUtils.isInvalidFDocName(documentName)) {
            Toast.makeText(context, R.string.save_alert_input_invalid_name, Toast.LENGTH_SHORT).show();
            callback.onError();
            return;
        }

        DocumentItem documentItem = new DocumentItem();
        documentItem.setParentId(folderId);
        documentItem.setChildCount(0);
        documentItem.setSize(0);
        documentItem.setName(documentName);

        AppDatabases appDatabases = AppDatabases.getInstance(context);
        Single.create((SingleOnSubscribe<DocumentItem>) emitter -> {
            Boolean isExists = appDatabases.documentDao().isExistsDocumentNameNoRx(documentName);
            if (isExists) {
                emitter.onError(new Throwable());
                return;
            }

            Long code = appDatabases.documentDao().insertEntityNoRx(documentItem);
            if (code == null) {
                emitter.onError(new Throwable());
                return;
            }

            emitter.onSuccess(appDatabases.documentDao().getDocumentByIdNoRx(code.intValue()));
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<DocumentItem>() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        if (callback != null) {
                            callback.onDisposable(d);
                        }
                    }

                    @Override
                    public void onSuccess(@io.reactivex.annotations.NonNull DocumentItem documentItem) {
                        if (callback != null) {
                            callback.onSuccess(documentItem);
                        }
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        e.printStackTrace();
                        if (callback != null) {
                            callback.onError();
                        }
                    }
                });
    }

    public static void deleteFolderDatabases(@NonNull Context context, List<FolderItem> folderItems, DBUtilsCallback dbUtilsCallback) {
        AppDatabases appDatabases = AppDatabases.getInstance(context);

        Completable.create(emitter -> {
            List<Integer> listId = new ArrayList<>();
            for (FolderItem folderItem : folderItems) {
                listId.add(folderItem.getId());
            }

            List<PageItem> listPage = appDatabases.folderDao().getAllPageByFolder(listId);
            for (PageItem pageItem : listPage) {
                FileUtils.deletePageItem(pageItem);
            }

            appDatabases.folderDao().deleteFoldersByIdNoRx(listId);

            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        if (dbUtilsCallback != null) {
                            dbUtilsCallback.onDisposable(d);
                        }
                    }

                    @Override
                    public void onComplete() {
                        if (dbUtilsCallback != null) {
                            dbUtilsCallback.onSuccess();
                        }
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        e.printStackTrace();
                        if (dbUtilsCallback != null) {
                            dbUtilsCallback.onError();
                        }
                    }
                });
    }

    public static void deleteDocumentDatabases(@NotNull Context context, List<DocumentItem> documentItems, DBUtilsCallback dbUtilsCallback) {
        AppDatabases appDatabases = AppDatabases.getInstance(context);
        Completable.create(emitter -> {
            List<PageItem> pageItems;
            List<Integer> listDocumentId = new ArrayList<>();

            for (DocumentItem documentItem : documentItems) {
                listDocumentId.add(documentItem.getId());
            }

            pageItems = appDatabases.pageDao().getListPageByDocumentIdNoRx(listDocumentId);
            for (PageItem pageItem : pageItems) {
                FileUtils.deletePageItem(pageItem);
            }

            appDatabases.documentDao().deleteListEntityNoRx(documentItems);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        if (dbUtilsCallback != null) {
                            dbUtilsCallback.onDisposable(d);
                        }
                    }

                    @Override
                    public void onComplete() {
                        if (dbUtilsCallback != null) {
                            dbUtilsCallback.onSuccess();
                        }
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        e.printStackTrace();
                        if (dbUtilsCallback != null) {
                            dbUtilsCallback.onError();
                        }
                    }
                });
    }

    public static void mergeFolderDatabases(@NotNull Context context, List<FolderItem> listFolder, FolderItem folderGoal, DBUtilsCallback dbUtilsCallback) {
        AppDatabases appDatabases = AppDatabases.getInstance(context);

        Completable.create(emitter -> {
            List<Integer> idFolders = new ArrayList<>();
            List<DocumentItem> documentItems;

            for (FolderItem folderItem : listFolder) {
                idFolders.add(folderItem.getId());
            }

            documentItems = appDatabases.documentDao().getDocumentsInIdFolders(idFolders);
            for (DocumentItem documentItem : documentItems) {
                documentItem.setParentId(folderGoal.getId());
            }

            appDatabases.documentDao().updateListEntityNoRx(documentItems);
            appDatabases.folderDao().deleteListEntityNoRx(listFolder);

            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        if (dbUtilsCallback != null) {
                            dbUtilsCallback.onDisposable(d);
                        }
                    }

                    @Override
                    public void onComplete() {
                        if (dbUtilsCallback != null) {
                            dbUtilsCallback.onSuccess();
                        }
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        if (dbUtilsCallback != null) {
                            dbUtilsCallback.onError();
                        }
                    }
                });
    }

    public static void mergeDocumentDatabases(@NotNull Context context, List<DocumentItem> listDocument, DocumentItem documentGoal, DBUtilsCallback dbUtilsCallback) {
        AppDatabases appDatabases = AppDatabases.getInstance(context);
        Completable.create(emitter -> {
            List<PageItem> listAllPage;
            List<Integer> listId = new ArrayList<>();
            Collections.sort(listDocument, SortUtils.sortByDateCreated);

            for (DocumentItem documentItem : listDocument) {
                listId.add(documentItem.getId());
            }

            listAllPage = appDatabases.pageDao().getListPageByDocumentIdNoRx(listId);

            Collections.sort(listAllPage, SortUtils.sortByPosition);
            Collections.sort(listAllPage, SortUtils.sortByParrentId);

            for (int index = 0; index < listAllPage.size(); index++) {
                PageItem pageItem = listAllPage.get(index);
                pageItem.setPosition(index + 1);
                pageItem.setParentId(documentGoal.getId());
            }

            appDatabases.pageDao().updateListEntityNoRx(listAllPage);
            appDatabases.documentDao().deleteListEntityNoRx(listDocument);

            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        if (dbUtilsCallback != null) {
                            dbUtilsCallback.onDisposable(d);
                        }
                    }

                    @Override
                    public void onComplete() {
                        if (dbUtilsCallback != null) {
                            dbUtilsCallback.onSuccess();
                        }
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        if (dbUtilsCallback != null) {
                            dbUtilsCallback.onError();
                        }
                    }
                });
    }

    public static void moveDocumentDatabases(Context context, List<DocumentItem> documentItems, FolderItem folderItemGoal, DBUtilsCallback dbUtilsCallback) {
        AppDatabases appDatabases = AppDatabases.getInstance(context);

        Completable.create(emitter -> {
            for (DocumentItem documentItem : documentItems) {
                documentItem.setParentId(folderItemGoal.getId());
            }

            appDatabases.documentDao().updateListEntityNoRx(documentItems);
            folderItemGoal.setChildCount(folderItemGoal.getChildCount() + documentItems.size());

            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        if (dbUtilsCallback != null) {
                            dbUtilsCallback.onDisposable(d);
                        }
                    }

                    @Override
                    public void onComplete() {
                        if (dbUtilsCallback != null) {
                            dbUtilsCallback.onSuccess();
                        }
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        if (dbUtilsCallback != null) {
                            dbUtilsCallback.onError();
                        }
                    }
                });
    }


    public static void shareJpgToGallery(Context context, List<FolderItem> folderItems, List<DocumentItem> documentItems, String title, int quality, StatusShareGallery statusShareGallery) {
        AppDatabases appDatabases = AppDatabases.getInstance(context);
        List<Integer> idFolders = new ArrayList<>();
        List<Integer> idDocuments = new ArrayList<>();
        HashMap<Integer, DocumentItem> mapDocument = new HashMap<>();

        Single.create((SingleOnSubscribe<List<PageItem>>) emitter -> {
            for (FolderItem folderItem : folderItems) {
                idFolders.add(folderItem.getId());
            }

            List<DocumentItem> documentsInFolder = appDatabases.documentDao().getDocumentsInIdFolders(idFolders);
            documentsInFolder.addAll(documentItems);

            for (DocumentItem documentItem : documentsInFolder) {
                mapDocument.put(documentItem.getId(), documentItem);
                idDocuments.add(documentItem.getId());
            }

            List<PageItem> pageItems = new ArrayList<>(appDatabases.pageDao().getListPageByDocumentIdNoRx(idDocuments));
            emitter.onSuccess(pageItems);
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<PageItem>>() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        if (statusShareGallery != null) {
                            statusShareGallery.onDisposable(d);
                        }
                    }

                    @Override
                    public void onSuccess(@io.reactivex.annotations.NonNull List<PageItem> pageItems) {
                        saveJpg(context, mapDocument, pageItems, title, quality, statusShareGallery);
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        if (statusShareGallery != null) {
                            statusShareGallery.onError();
                        }
                    }
                });
    }

    private static void saveJpg(Context context, HashMap<Integer, DocumentItem> mapDocument, List<PageItem> pageItems, String title, int quality, StatusShareGallery statusShareGallery) {
        File dir = getSaveDir(context);
        List<String> listShare = new ArrayList<>();
        Observable.fromIterable(pageItems)
                .flatMap((Function<PageItem, ObservableSource<String>>) pageItem ->
                        Observable.just(pageItem)
                                .subscribeOn(Schedulers.computation())
                                .map((Function<PageItem, String>) inputItem -> {
                                    String newName;
                                    if (TextUtils.isEmpty(title)) {
                                        newName = FileUtils.createNameFileDuplicate(context, dir, mapDocument.get(pageItem.getParentId()), pageItem);
                                    } else {
                                        DocumentItem documentItemParent = mapDocument.get(pageItem.getParentId());
                                        if (documentItemParent == null) {
                                            documentItemParent = new DocumentItem();
                                            documentItemParent.setName(FileUtils.createNameFolder(context));
                                        }
                                        DocumentItem document = new DocumentItem();
                                        document.setName(documentItemParent.getName());
                                        newName = FileUtils.createNameFileDuplicate(context, dir, document, pageItem);
                                    }
                                    File fileOutput = new File(dir, newName);
                                    FileOutputStream outputStream = new FileOutputStream(fileOutput);
                                    Bitmap bitmap = BitmapFactory.decodeFile(pageItem.getEnhanceUri());
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
                                    outputStream.close();
                                    AppUtils.addImageToContentProvider(context, Uri.parse(fileOutput.getPath()));
                                    return fileOutput.getPath();
                                }))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        if (statusShareGallery != null) {
                            statusShareGallery.onDisposable(d);
                        }
                    }

                    @Override
                    public void onNext(@io.reactivex.annotations.NonNull String s) {
                        listShare.add(s);
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        if (statusShareGallery != null) {
                            statusShareGallery.onError();
                        }
                    }

                    @Override
                    public void onComplete() {
                        if (statusShareGallery != null) {
                            statusShareGallery.onSuccess(listShare);
                        }
                    }
                });
    }

    public static void sharePageToGallery(@NotNull Context context, DocumentItem documentItem, String title, int quality, List<PageItem> pageItems, StatusShareGallery statusShareGallery) {
        HashMap<Integer, DocumentItem> mapDocument = new HashMap<>();
        mapDocument.put(documentItem.getId(), documentItem);
        saveJpg(context, mapDocument, pageItems, title, quality, statusShareGallery);
    }

    @NotNull
    public static File getSaveDir(@NotNull Context context) {
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File(sdCard.getAbsolutePath() + "/" + context.getString(R.string.app_name));
        if (!dir.exists()) {
            System.out.println(dir.mkdir());
        }
        return dir;
    }

    public static void convertToDocumentItemsId(@NotNull Context context, List<FolderItem> folderItems, List<DocumentItem> documentItems, ConvertToDocumentCallback callback) {
        AppDatabases appDatabases = AppDatabases.getInstance(context);

        Single.create((SingleOnSubscribe<ArrayList<Integer>>) emitter -> {
            ArrayList<Integer> listId = new ArrayList<>();
            for (DocumentItem documentItem : documentItems) {
                listId.add(documentItem.getId());
            }

            if (!folderItems.isEmpty()) {
                List<Integer> listFolderId = new ArrayList<>();

                for (FolderItem folderItem : folderItems) {
                    listFolderId.add(folderItem.getId());
                }

                listId.addAll(appDatabases.documentDao().getDocumentIdsByFolders(listFolderId));
            }

            emitter.onSuccess(listId);
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<ArrayList<Integer>>() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        if (callback != null) {
                            callback.onDisposable(d);
                        }
                    }

                    @Override
                    public void onSuccess(@io.reactivex.annotations.NonNull ArrayList<Integer> listId) {
                        if (callback != null) {
                            callback.onSuccess(listId);
                        }
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        e.printStackTrace();
                        if (callback != null) {
                            callback.onError();
                        }
                    }
                });
    }

    public static void convertToDocumentItem(Context context, List<FolderItem> folderItems, List<DocumentItem> documentItems, ConvertToDocumentItem convertToDocumentItem) {
        Single.create((SingleOnSubscribe<List<DocumentItem>>) emitter -> {
            AppDatabases appDatabases = AppDatabases.getInstance(context);
            List<Integer> idFolders = new ArrayList<>();
            for (FolderItem folderItem : folderItems) {
                idFolders.add(folderItem.getId());
            }
            List<DocumentItem> listDocumentInFolder = appDatabases.documentDao().getDocumentsInIdFolders(idFolders);
            documentItems.addAll(listDocumentInFolder);
            List<Integer> listIdDocument = new ArrayList<>();
            for (DocumentItem documentItem : documentItems) {
                listIdDocument.add(documentItem.getId());
            }

            List<PageItem> listAllPage = appDatabases.pageDao().getListPageByDocumentIdNoRx(listIdDocument);

            for (PageItem pageItem : listAllPage) {
                for (DocumentItem documentItem : documentItems) {

                    if (pageItem.getParentId() == documentItem.getId()) {
                        List<PageItem> pageItemList = documentItem.getListPage();

                        if (pageItemList == null) {
                            pageItemList = new ArrayList<>();
                        }
                        pageItemList.add(pageItem);
                        documentItem.setListPage(pageItemList);
                    }
                }
            }

            emitter.onSuccess(documentItems);
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<DocumentItem>>() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        if (convertToDocumentItem != null) {
                            convertToDocumentItem.onDisposable(d);
                        }
                    }

                    @Override
                    public void onSuccess(@io.reactivex.annotations.NonNull List<DocumentItem> documentItems) {
                        if (convertToDocumentItem != null) {
                            convertToDocumentItem.onSuccess(documentItems);
                        }
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        if (convertToDocumentItem != null) {
                            convertToDocumentItem.onError();
                        }
                    }
                });

    }

    public static List<DocumentItem> getListDocumentItem(List<DocumentItem> documentItems) {
        List<DocumentItem> documentItemList = new ArrayList<>();
        for (DocumentItem documentItem : documentItems) {
            DocumentItem documentTamp = new DocumentItem();
            documentTamp.setId(documentItem.getId());
            documentTamp.setName(documentItem.getName());
            documentTamp.setParentId(documentItem.getParentId());
            documentTamp.setListPage(documentItem.getListPage());
            documentTamp.setCreatedTime(documentItem.getCreatedTime());
            documentTamp.setSize(documentItem.getSize());
            documentTamp.setSelected(documentItem.isSelected());
            documentItemList.add(documentTamp);
        }

        return documentItemList;
    }

    public static List<FolderItem> getListFolderItem(List<FolderItem> folderItems) {
        List<FolderItem> folderItemList = new ArrayList<>();
        for (FolderItem folderItem : folderItems) {
            FolderItem folderItemTamp = new FolderItem();
            folderItemTamp.setId(folderItem.getId());
            folderItemTamp.setParentId(folderItem.getParentId());
            folderItemTamp.setChildCount(folderItem.getChildCount());
            folderItemTamp.setName(folderItem.getName());
            folderItemTamp.setCreatedTime(folderItem.getCreatedTime());
            folderItemTamp.setSize(folderItem.getSize());
            folderItemTamp.setSelected(folderItem.isSelected());
            folderItemList.add(folderItemTamp);
        }
        return folderItemList;
    }

    public interface DBUtilsCallback {

        void onDisposable(Disposable disposable);

        void onSuccess();

        void onError();
    }

    public interface CallBackFolderNameDefault {
        void onDisposable(Disposable disposable);

        void onSuccess(String folderName);

        void onError();
    }

    public interface StatusShareGallery {

        void onDisposable(Disposable disposable);

        void onSuccess(List<String> listFile);

        void onError();
    }

    public interface CreateFolderCallback {

        void onDisposable(Disposable disposable);

        void onSuccess(FolderItem folderItem);

        void onError();
    }

    public interface CreateDocumentCallback {

        void onDisposable(Disposable disposable);

        void onSuccess(DocumentItem documentItem);

        void onError();
    }

    public interface ConvertToDocumentCallback {

        void onDisposable(Disposable disposable);

        void onSuccess(ArrayList<Integer> arrayList);

        void onError();

    }

    public interface ConvertToDocumentItem {
        void onDisposable(Disposable disposable);

        void onSuccess(List<DocumentItem> documentItems);

        void onError();
    }

}
