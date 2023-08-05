package com.document.camerascanner.features.detailshow.folders;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.document.camerascanner.R;
import com.document.camerascanner.databases.AppDatabases;
import com.document.camerascanner.databases.model.DocumentItem;
import com.document.camerascanner.databases.model.FolderItem;
import com.document.camerascanner.databases.model.PageItem;
import com.document.camerascanner.features.camera.CameraActivity;
import com.document.camerascanner.features.detect.DetectActivity;
import com.document.camerascanner.features.main.TypeSort;
import com.document.camerascanner.features.pdfsettings.PdfSettingsActivity;
import com.document.camerascanner.features.save.SaveActivity;
import com.document.camerascanner.prefs.AppPref;
import com.document.camerascanner.utils.Constants;
import com.document.camerascanner.utils.DbUtils;
import com.document.camerascanner.utils.FileUtils;
import com.document.camerascanner.utils.ImageUtils;
import com.document.camerascanner.utils.PdfUtils;
import com.document.camerascanner.utils.SortUtils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


public class DetailFolderPresenter implements DetailFolder.Presenter {

    private final DetailFolder.View callback;

    private final HashMap<Integer, DocumentItem> mapSelectedDocument;

    private CompositeDisposable compositeDisposable;

    public DetailFolderPresenter(DetailFolder.View callback) {
        this.callback = callback;
        this.compositeDisposable = new CompositeDisposable();
        this.mapSelectedDocument = new HashMap<>();
    }

    private void addDisposable(Disposable disposable) {
        if (this.compositeDisposable == null) {
            this.compositeDisposable = new CompositeDisposable();
        }

        this.compositeDisposable.add(disposable);
    }

    @Override
    public void getFolderNameDefault(Context context) {
        DbUtils.createNameFolderDefault(context, new DbUtils.CallBackFolderNameDefault() {
            @Override
            public void onDisposable(Disposable disposable) {
                addDisposable(disposable);
            }

            @Override
            public void onSuccess(String folderName) {
                if (callback != null) {
                    callback.onSuccessNameFolderDefault(folderName);
                }
            }

            @Override
            public void onError() {

            }
        });
    }

    @Override
    public void getFolderInfo(Context context, int id) {
        AppDatabases.getInstance(context).folderDao().getFolderInfo(id).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<FolderItem>() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        addDisposable(d);
                    }

                    @Override
                    public void onSuccess(@io.reactivex.annotations.NonNull FolderItem folderItem) {
                        if (callback != null) {
                            callback.onGetFolderInfoSuccess(folderItem);
                        }
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        e.printStackTrace();
                        ((Activity) context).finish();
                    }
                });
    }

    @Override
    public void getFolderChildren(Context context, @NotNull FolderItem folderItem) {
        AppDatabases appDatabases = AppDatabases.getInstance(context);

        Single.create((SingleOnSubscribe<List<DocumentItem>>) emitter -> {
            List<DocumentItem> listDocument;

            listDocument = appDatabases.documentDao().getListDocumentByIdFolderNoRx(folderItem.getId());
            for (DocumentItem documentItem : listDocument) {
                documentItem.setThumbnail(appDatabases.documentDao().getThumbnailDocument(documentItem.getId()));
            }

            AppPref appPref = AppPref.getInstance(context);
            int typeSort = appPref.getSortTypeFolder();
            if (typeSort == TypeSort.SORT_DATA) {
                Collections.sort(listDocument, SortUtils.sortByData);
            } else if (typeSort == TypeSort.SORT_DATE) {
                Collections.sort(listDocument, SortUtils.sortByDateCreated);
            } else if (typeSort == TypeSort.SORT_NAME) {
                Collections.sort(listDocument, SortUtils.sortByName);
            }

            emitter.onSuccess(listDocument);
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<DocumentItem>>() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        addDisposable(d);
                    }

                    @Override
                    public void onSuccess(@io.reactivex.annotations.NonNull List<DocumentItem> list) {
                        if (callback != null) {
                            callback.onShowListDocumentInFolder(list);
                        }
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

    @Override
    public void resetHmDocumentSelect() {
        this.mapSelectedDocument.clear();
    }

    @Override
    public void setListHmDocumentSelect(@NotNull List<DocumentItem> documentSelect) {
        this.resetHmDocumentSelect();
        for (DocumentItem documentItem : documentSelect) {
            this.mapSelectedDocument.put(documentItem.getId(), documentItem);
        }
    }

    @Override
    public void setItemToHmDocumentItem(@NotNull DocumentItem documentItem) {
        if (!this.mapSelectedDocument.containsKey(documentItem.getId())) {
            this.mapSelectedDocument.put(documentItem.getId(), documentItem);
            return;
        }
        this.mapSelectedDocument.remove(documentItem.getId());
    }

    @Override
    public void deleteDocuments(Context context) {
        List<DocumentItem> documentItems = new ArrayList<>(this.mapSelectedDocument.values());
        DbUtils.deleteDocumentDatabases(context, documentItems, new DbUtils.DBUtilsCallback() {
            @Override
            public void onDisposable(Disposable disposable) {
                addDisposable(disposable);
            }

            @Override
            public void onSuccess() {
                if (callback != null) {
                    callback.onShowSuccessDelete();
                }
            }

            @Override
            public void onError() {
            }
        });
    }

    @Override
    public void moveDocuments(Context context, FolderItem folderItemGoal) {
        List<DocumentItem> documentItems = new ArrayList<>(this.mapSelectedDocument.values());
        DbUtils.moveDocumentDatabases(context, documentItems, folderItemGoal, new DbUtils.DBUtilsCallback() {
            @Override
            public void onDisposable(Disposable disposable) {
                addDisposable(disposable);
            }

            @Override
            public void onSuccess() {
                if (callback != null) {
                    callback.onSuccessMove();
                }
            }

            @Override
            public void onError() {
            }
        });
    }

    @Override
    public void moveOutDocuments(Context context) {
        List<DocumentItem> documentItems = new ArrayList<>(this.mapSelectedDocument.values());
        AppDatabases appDatabases = AppDatabases.getInstance(context);
        FolderItem folderMain = appDatabases.folderDao().getFolderItemById(Constants.ID_FOLDER_MAIN);
        DbUtils.moveDocumentDatabases(context, documentItems, folderMain, new DbUtils.DBUtilsCallback() {
            @Override
            public void onDisposable(Disposable disposable) {
                addDisposable(disposable);
            }

            @Override
            public void onSuccess() {
                if (callback != null) {
                    callback.onSuccessMove();
                }
            }

            @Override
            public void onError() {
            }
        });
    }

    @Override
    public void showDialogMove(Context context, FolderItem folderItemRoot) {
        AppDatabases appDatabases = AppDatabases.getInstance(context);
        appDatabases.folderDao().getListFolder().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<FolderItem>>() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        addDisposable(d);
                    }

                    @Override
                    public void onSuccess(@io.reactivex.annotations.NonNull List<FolderItem> folderItems) {
                        for (int index = folderItems.size() - 1; index >= 0; index--) {
                            if (folderItems.get(index).getId() == folderItemRoot.getId()) {
                                folderItems.remove(index);
                                break;
                            }
                        }

                        if (callback != null) {
                            callback.onShowDialogMove(folderItems);
                        }
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

    @Override
    public void showDialogMerge(Context context) {
        String title = FileUtils.createNameFolder(context);
        List<DocumentItem> documentItems = new ArrayList<>(this.mapSelectedDocument.values());
        String path = documentItems.get(0).getThumbnail();

        if (this.callback != null) {
            this.callback.onShowDialogMerge(title, path);
        }
    }

    @Override
    public void generalMergeDocuments(Context context, String nameDocument, int folderId) {
        DbUtils.createNewDocument(context, nameDocument, folderId, new DbUtils.CreateDocumentCallback() {
            @Override
            public void onDisposable(Disposable disposable) {
                addDisposable(disposable);
            }

            @Override
            public void onSuccess(DocumentItem documentItem) {
                mergeDocuments(context, documentItem);
            }

            @Override
            public void onError() {
            }
        });
    }

    @Override
    public void mergeDocuments(Context context, DocumentItem documentItemGoal) {
        List<DocumentItem> documentItems = new ArrayList<>(this.mapSelectedDocument.values());
        DbUtils.mergeDocumentDatabases(context, documentItems, documentItemGoal, new DbUtils.DBUtilsCallback() {
            @Override
            public void onDisposable(Disposable disposable) {
                addDisposable(disposable);
            }

            @Override
            public void onSuccess() {
                if (callback != null) {
                    callback.onShowSuccessMerge();
                }
            }

            @Override
            public void onError() {
            }
        });
    }

    @Override
    public void calculatorSize(Context context) {
        String title = "";
        if (this.mapSelectedDocument.size() == 1) {
            title = new ArrayList<>(this.mapSelectedDocument.values()).get(0).getName();
        }

        if (this.callback != null) {
            this.callback.onShowSize(title);
        }
    }

    @Override
    public void sharePdf(Context context) {
        List<DocumentItem> documentItems = new ArrayList<>(this.mapSelectedDocument.values());
        DbUtils.convertToDocumentItemsId(context, new ArrayList<>(), documentItems, new DbUtils.ConvertToDocumentCallback() {
            @Override
            public void onDisposable(Disposable disposable) {
                addDisposable(disposable);
            }

            @Override
            public void onSuccess(ArrayList<Integer> arrayList) {
                Intent intent = new Intent(context, PdfSettingsActivity.class);
                intent.putExtra(Constants.EXTRA_SHARE_DOCUMENT, arrayList);
                context.startActivity(intent);
            }

            @Override
            public void onError() {

            }
        });
    }

    @Override
    public void shareJpgGallery(Context context, String title, int quality) {
        List<DocumentItem> documentItems = new ArrayList<>(this.mapSelectedDocument.values());
        DbUtils.shareJpgToGallery(context, new ArrayList<>(), documentItems, title, quality, new DbUtils.StatusShareGallery() {
            @Override
            public void onDisposable(Disposable disposable) {
                addDisposable(disposable);
            }

            @Override
            public void onSuccess(List<String> listFile) {
                if (callback != null) {
                    callback.onShowOnSuccessToGallery(listFile);
                }
            }

            @Override
            public void onError() {

            }
        });
    }

    @Override
    public void shareJpg(Context context) {
        AppDatabases appDatabases = AppDatabases.getInstance(context);

        Single.create((SingleOnSubscribe<ArrayList<String>>) emitter -> {
            List<DocumentItem> documentItems = new ArrayList<>(this.mapSelectedDocument.values());
            List<Integer> listId = new ArrayList<>();

            for (DocumentItem documentItem : documentItems) {
                listId.add(documentItem.getId());
            }

            List<String> listPath;
            listPath = appDatabases.pageDao().getAllShareUriByIdDocuments(listId);

            emitter.onSuccess(new ArrayList<>(listPath));
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<ArrayList<String>>() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        addDisposable(d);
                    }

                    @Override
                    public void onSuccess(@io.reactivex.annotations.NonNull ArrayList<String> listUri) {
                        ImageUtils.shareToShare(context, listUri);
                        resetHmDocumentSelect();
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

    @Override
    public void renameFolder(Context context, FolderItem folderItem, @NonNull String newName) {
        if (newName.length() <= 0) {
            Toast.makeText(context, context.getString(R.string.home_folder_et_empty), Toast.LENGTH_SHORT).show();
            return;
        }

        if (FileUtils.isInvalidFDocName(newName)) {
            Toast.makeText(context, R.string.save_alert_input_invalid_name, Toast.LENGTH_SHORT).show();
            return;
        }

        folderItem.setName(newName);

        AppDatabases.getInstance(context)
                .folderDao()
                .updateEntity(folderItem)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        addDisposable(d);
                    }

                    @Override
                    public void onComplete() {
                        if (callback != null) {
                            callback.onShowRenameFolder(folderItem);
                        }
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

    @Override
    public void renameDocument(Context context, String newName, DocumentItem documentItem) {
        if (TextUtils.isEmpty(newName)) {
            Toast.makeText(context, R.string.home_folder_et_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        if (FileUtils.isInvalidFDocName(newName)) {
            Toast.makeText(context, R.string.save_alert_input_invalid_name, Toast.LENGTH_SHORT).show();
            return;
        }

        documentItem.setName(newName);
        AppDatabases.getInstance(context)
                .documentDao()
                .updateEntity(documentItem)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        addDisposable(d);
                    }

                    @Override
                    public void onComplete() {
                        if (callback != null) {
                            callback.onShowRenameDocument();
                        }
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

    @Override
    public void startCamera(Context context, FolderItem folderItem) {
        Completable.create(emitter -> {
            AppPref appPref = AppPref.getInstance(context);
            DbUtils.clearPreviousSession(AppDatabases.getInstance(context));
            appPref.setSaveId(folderItem.getId());
            appPref.setSaveInDocument(false);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        addDisposable(d);
                    }

                    @Override
                    public void onComplete() {
                        context.startActivity(new Intent(context, CameraActivity.class));
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

    @Override
    public void getDataImportImage(Context context, Intent data, FolderItem folderItem) {
        Single.create((SingleOnSubscribe<List<PageItem>>) emitter -> {
            AppDatabases appDatabases = AppDatabases.getInstance(context);
            DbUtils.clearPreviousSession(appDatabases);
            ImageUtils.importImage(context, data);
            emitter.onSuccess(appDatabases.pageDao().getPreviousTempPage());
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<PageItem>>() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        addDisposable(d);
                    }

                    @Override
                    public void onSuccess(@io.reactivex.annotations.NonNull List<PageItem> pageItems) {
                        Intent intent;
                        if (pageItems.size() == 1) {
                            intent = new Intent(context, DetectActivity.class);
                            intent.putExtra(Constants.EXTRA_PAGE_ITEM, pageItems.get(0));
                        } else {
                            intent = new Intent(context, SaveActivity.class);
                        }

                        AppPref appPref = AppPref.getInstance(context);
                        appPref.setSaveId(folderItem.getId());
                        appPref.setSaveInDocument(false);
                        context.startActivity(intent);

                        if (callback != null) {
                            callback.onSuccessImport();
                        }
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

    @Override
    public void newFolder(Context context, String newName) {
        DbUtils.createNewFolder(context, newName, new DbUtils.CreateFolderCallback() {
            @Override
            public void onDisposable(Disposable disposable) {
                addDisposable(disposable);
            }

            @Override
            public void onSuccess(FolderItem folderItem) {
                moveDocuments(context, folderItem);
            }

            @Override
            public void onError() {
            }
        });
    }

    @Override
    public void sortDocument(Context context, List<DocumentItem> documentItems, int typeSort) {
        if (documentItems == null) {
            return;
        }

        AppPref appPref = AppPref.getInstance(context);
        appPref.setSortTypeFolder(typeSort);

        switch (typeSort) {
            case TypeSort.SORT_DATA:
                Collections.sort(documentItems, SortUtils.sortByData);
                break;
            case TypeSort.SORT_DATE:
                Collections.sort(documentItems, SortUtils.sortByDateCreated);
                break;
            case TypeSort.SORT_NAME:
                Collections.sort(documentItems, SortUtils.sortByName);
                break;
        }

        if (this.callback != null) {
            this.callback.onShowListDocumentInFolder(documentItems);
        }
    }

    @Override
    public void onGeneralSaveAndSharePdf(Context context, String title, int quality, boolean isSave) {
        List<DocumentItem> documentItems = DbUtils.getListDocumentItem(new ArrayList<>(this.mapSelectedDocument.values()));

        DbUtils.convertToDocumentItem(context, new ArrayList<>(), documentItems, new DbUtils.ConvertToDocumentItem() {
            @Override
            public void onDisposable(Disposable disposable) {
                addDisposable(disposable);
            }

            @Override
            public void onSuccess(List<DocumentItem> documentItems) {
                shareAndSavePdf(context, title, quality, documentItems, isSave);
            }

            @Override
            public void onError() {

            }
        });
    }

    public void shareAndSavePdf(Context context, String title, int quality, List<DocumentItem> documentItems, boolean isSave) {
        PdfUtils.convertPdf(context, quality, title, documentItems, new PdfUtils.PdfStatus() {
            @Override
            public void onDisposable(Disposable disposable) {
                addDisposable(disposable);
            }

            @Override
            public void onSuccessConvertPdf(List<String> listPdf) {
                if (isSave) {
                    if (callback != null) {
                        callback.onSuccessSavePdf(listPdf);
                        return;
                    }
                }

                if (callback != null) {
                    FileUtils.sendfile(context, listPdf);
                    callback.onSuccessSharePdf();
                }
            }

            @Override
            public void onProcessPercent(int percent) {

            }

            @Override
            public void onError() {

            }
        });
    }

    @Override
    public void disposableAll() {
        if (this.compositeDisposable != null) {
            this.compositeDisposable.dispose();
        }
    }
}
