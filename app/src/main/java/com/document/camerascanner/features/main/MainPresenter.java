package com.document.camerascanner.features.main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.Toast;

import com.document.camerascanner.R;
import com.document.camerascanner.databases.AppDatabases;
import com.document.camerascanner.databases.model.BaseEntity;
import com.document.camerascanner.databases.model.DocumentItem;
import com.document.camerascanner.databases.model.FolderItem;
import com.document.camerascanner.databases.model.PageItem;
import com.document.camerascanner.features.camera.CameraActivity;
import com.document.camerascanner.features.main.interfaces.MPresenter;
import com.document.camerascanner.features.main.interfaces.MainView;
import com.document.camerascanner.features.pdfsettings.PdfSettingsActivity;
import com.document.camerascanner.prefs.AppPref;
import com.document.camerascanner.utils.Constants;
import com.document.camerascanner.utils.DbUtils;
import com.document.camerascanner.utils.FileUtils;
import com.document.camerascanner.utils.ImageUtils;
import com.document.camerascanner.utils.PdfUtils;
import com.document.camerascanner.utils.SortUtils;
import com.document.camerascanner.utils.VNCharacterUtils;
import com.google.firebase.analytics.FirebaseAnalytics;

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
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


public class MainPresenter implements MPresenter {

    private final Context context;
    private final MainView mainView;
    private final AppDatabases appDatabases;

    private final HashMap<Integer, FolderItem> mapSelectedFolder;
    private final HashMap<Integer, DocumentItem> mapSelectedDocument;

    private boolean isSuccessDeleteFolder;
    private boolean isSuccessDeleteDocument;

    private CompositeDisposable disposable;

    public MainPresenter(MainView mainView, Context context) {
        this.mainView = mainView;
        this.context = context;
        this.appDatabases = AppDatabases.getInstance(context);
        this.mapSelectedFolder = new HashMap<>();
        this.mapSelectedDocument = new HashMap<>();
    }

    private void addDisposable(Disposable disposable) {
        if (this.disposable == null) {
            this.disposable = new CompositeDisposable();
        }

        if (disposable == null) {
            return;
        }

        this.disposable.add(disposable);
    }

    @Override
    public void getNewFolderDefault(Context context, boolean isMoveDocument) {
        DbUtils.createNameFolderDefault(context, new DbUtils.CallBackFolderNameDefault() {
            @Override
            public void onDisposable(Disposable disposable) {
                addDisposable(disposable);
            }

            @Override
            public void onSuccess(String folderName) {
                if (mainView != null) {
                    mainView.onShowNameFolder(folderName, isMoveDocument);
                }
            }

            @Override
            public void onError() {

            }
        });
    }

    @Override
    public void createNewFolder(String folderName, boolean isMoveDocument) {
        DbUtils.createNewFolder(this.context, folderName, new DbUtils.CreateFolderCallback() {
            @Override
            public void onDisposable(Disposable disposable) {
                addDisposable(disposable);
            }

            @Override
            public void onSuccess(FolderItem folderItem) {
                if (mainView != null) {
                    mainView.onSuccessCreatedFolder(folderItem, isMoveDocument);
                }
            }

            @Override
            public void onError() {
                if (mainView != null) {
                    mainView.onErrorCreatedFolder();
                }
            }
        });
    }

    @Override
    public void listSortAll(List<FolderItem> listFolder, List<DocumentItem> listDocument, int typeSort) {
        this.listSortFolder(listFolder, typeSort);
        this.listSortDocument(listDocument, typeSort);
    }

    private void listSortFolder(List<FolderItem> list, int typeSort) {
        if (list == null) {
            return;
        }

        for (FolderItem itemFile : list) {
            itemFile.setSelected(false);
        }

        switch (typeSort) {
            case TypeSort.SORT_DATA:
                Collections.sort(list, SortUtils.sortByData);
                break;
            case TypeSort.SORT_DATE:
                Collections.sort(list, SortUtils.sortByDateCreated);
                break;
            case TypeSort.SORT_NAME:
                Collections.sort(list, SortUtils.sortByName);
                break;
        }

        if (this.mainView != null) {
            this.mainView.onShowFolderSort(list);
        }
    }

    private void listSortDocument(List<DocumentItem> list, int typeSort) {
        if (list == null) {
            return;
        }

        for (DocumentItem itemFile : list) {
            itemFile.setSelected(false);
        }

        switch (typeSort) {
            case TypeSort.SORT_DATA:
                Collections.sort(list, SortUtils.sortByData);
                break;
            case TypeSort.SORT_DATE:
                Collections.sort(list, SortUtils.sortByDateCreated);
                break;
            case TypeSort.SORT_NAME:
                Collections.sort(list, SortUtils.sortByName);
                break;
        }

        if (this.mainView != null) {
            this.mainView.onShowDocumentsSort(list);
        }
    }

    private void composeDispose(@io.reactivex.annotations.NonNull Disposable d) {
        if (this.disposable == null) {
            this.disposable = new CompositeDisposable();
        }
        this.disposable.add(d);
    }

    private void resetStatusDelete() {
        this.isSuccessDeleteDocument = false;
        this.isSuccessDeleteFolder = false;
    }

    @Override
    public void deleteFile() {
        List<FolderItem> listFolder = new ArrayList<>(this.mapSelectedFolder.values());
        DbUtils.deleteFolderDatabases(this.context, listFolder, new DbUtils.DBUtilsCallback() {
            @Override
            public void onDisposable(Disposable disposable) {
                addDisposable(disposable);
            }

            @Override
            public void onSuccess() {
                isSuccessDeleteFolder = true;
                resetHmFolderSelect();
                if (mainView != null && isSuccessDeleteDocument) {
                    mainView.onDeleteSuccess();
                    mainView.resetQuickAction();
                    resetStatusDelete();
                }
            }

            @Override
            public void onError() {
            }
        });

        List<DocumentItem> listDocument = new ArrayList<>(this.mapSelectedDocument.values());
        DbUtils.deleteDocumentDatabases(context, listDocument, new DbUtils.DBUtilsCallback() {
            @Override
            public void onDisposable(Disposable disposable) {
                addDisposable(disposable);
            }

            @Override
            public void onSuccess() {
                isSuccessDeleteDocument = true;
                resetHmDocumentSelect();

                if (mainView != null && isSuccessDeleteFolder) {
                    mainView.onDeleteSuccess();
                    mainView.resetQuickAction();
                    resetStatusDelete();
                }
            }

            @Override
            public void onError() {
            }
        });
    }

    @Override
    public void resetHmFolderSelect() {
        if (this.mapSelectedFolder != null) {
            this.mapSelectedFolder.clear();
        }
    }

    @Override
    public void resetHmDocumentSelect() {
        if (this.mapSelectedDocument != null) {
            this.mapSelectedDocument.clear();
        }
    }

    @Override
    public void mergeFile(String name) {
        if (this.mapSelectedDocument.size() != 0) {
            DbUtils.createNewDocument(context, name, Constants.ID_FOLDER_MAIN, new DbUtils.CreateDocumentCallback() {
                @Override
                public void onDisposable(Disposable disposable) {
                    addDisposable(disposable);
                }

                @Override
                public void onSuccess(DocumentItem documentItem) {
                    mergeDocument(documentItem);
                }

                @Override
                public void onError() {
                }
            });
        }

        if (this.mapSelectedFolder.size() != 0) {
            DbUtils.createNewFolder(this.context, name, new DbUtils.CreateFolderCallback() {
                @Override
                public void onDisposable(Disposable disposable) {
                    addDisposable(disposable);
                }

                @Override
                public void onSuccess(FolderItem folderItem) {
                    mergeFolder(folderItem);
                }

                @Override
                public void onError() {
                }
            });
        }
    }

    @Override
    public void mergeDocument(DocumentItem documentGoal) {
        List<DocumentItem> listDocument = new ArrayList<>(this.mapSelectedDocument.values());
        DbUtils.mergeDocumentDatabases(this.context, listDocument, documentGoal, new DbUtils.DBUtilsCallback() {
            @Override
            public void onDisposable(Disposable disposable) {
                addDisposable(disposable);
            }

            @Override
            public void onSuccess() {
                resetHmDocumentSelect();
                if (mainView != null) {
                    mainView.resetQuickAction();
                }
            }

            @Override
            public void onError() {
            }
        });
    }

    @Override
    public void mergeFolder(FolderItem folderItemGoal) {
        List<FolderItem> listFolder = new ArrayList<>(this.mapSelectedFolder.values());
        DbUtils.mergeFolderDatabases(this.context, listFolder, folderItemGoal, new DbUtils.DBUtilsCallback() {
            @Override
            public void onDisposable(Disposable disposable) {
                addDisposable(disposable);
            }

            @Override
            public void onSuccess() {
                resetHmDocumentSelect();
                if (mainView != null) {
                    mainView.resetQuickAction();
                }
            }

            @Override
            public void onError() {
            }
        });
    }

    @Override
    public void moveFiles(BaseEntity baseEntity) {
        List<DocumentItem> documentItems = new ArrayList<>(this.mapSelectedDocument.values());
        DbUtils.moveDocumentDatabases(this.context, documentItems, (FolderItem) baseEntity, new DbUtils.DBUtilsCallback() {
            @Override
            public void onDisposable(Disposable disposable) {
                addDisposable(disposable);
            }

            @Override
            public void onSuccess() {
                if (mainView != null) {
                    mainView.resetQuickAction();
                    resetHmDocumentSelect();
                    resetHmFolderSelect();
                }
            }

            @Override
            public void onError() {
            }
        });
    }

    @Override
    public void getListFolderDB() {
        this.appDatabases.folderDao()
                .getListFolder()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<FolderItem>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        addDisposable(d);

                    }

                    @Override
                    public void onSuccess(@NonNull List<FolderItem> folderItems) {
                        mainView.showListFolder(folderItems);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

    @Override
    public void getListDocumentDB() {
        this.appDatabases.documentDao()
                .getListDocumentRoot()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<DocumentItem>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        addDisposable(d);
                    }

                    @Override
                    public void onSuccess(@NonNull List<DocumentItem> list) {
                        for (DocumentItem documentItem : list) {
                            String thumbnailUri = appDatabases.documentDao().getThumbnailDocument(documentItem.getId());
                            documentItem.setThumbnail(thumbnailUri);
                        }
                        mainView.showListDocument(list);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

    @Override
    public void getListFileFromImport(Intent data) {
        Single.create((SingleOnSubscribe<List<PageItem>>) emitter -> {
            ImageUtils.importImage(this.context, data);
            emitter.onSuccess(this.appDatabases.pageDao().getPreviousTempPage());
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<PageItem>>() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        composeDispose(d);
                    }

                    @Override
                    public void onSuccess(@io.reactivex.annotations.NonNull List<PageItem> listTemp) {
                        AppPref.getInstance(context).removePref();

                        if (listTemp.size() == 1) {
                            FirebaseAnalytics.getInstance(context).logEvent("MAIN_IMPORT_SINGLE", null);

                            if (mainView != null) {
                                mainView.onSingleImportSuccess(listTemp.get(0));
                            }
                            return;
                        }

                        FirebaseAnalytics.getInstance(context).logEvent("MAIN_IMPORT_MULTIPLE", null);
                        if (mainView != null) {
                            mainView.onMultipleImportSuccess();
                        }
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        e.printStackTrace();
                        if (mainView != null) {
                            mainView.onErrorImport();
                        }
                    }
                });
    }

    @Override
    public void getTitleSelect() {
        String title = "";
        if ((this.mapSelectedFolder.size() == 1 || this.mapSelectedDocument.size() == 1)
                && (this.mapSelectedDocument.size() + this.mapSelectedFolder.size() == 1)) {
            if (this.mapSelectedFolder.size() == 1) {
                title = new ArrayList<>(this.mapSelectedFolder.values()).get(0).getName();
            } else {
                title = new ArrayList<>(this.mapSelectedDocument.values()).get(0).getName();
            }
        }

        if (this.mainView != null) {
            this.mainView.showDialogSendTo(title);
        }
    }

    @Override
    public void searchFile(List<FolderItem> listFolder, List<DocumentItem> listDocument, @NotNull CharSequence charSequence) {
        List<FolderItem> listFolderSearch = new ArrayList<>();
        List<DocumentItem> listDocumentSearch = new ArrayList<>();

        if (charSequence.toString().length() == 0) {
            if (this.mainView != null) {
                this.mainView.showListDefault();
            }
        } else {
            String nameSearch = VNCharacterUtils.removeAccent(charSequence.toString()).replace(" ", "").toLowerCase();

            for (FolderItem folderItem : listFolder) {
                String nameFile = VNCharacterUtils.removeAccent(folderItem.getName()).replace(" ", "").toLowerCase();

                if (nameFile.contains(nameSearch)) {
                    listFolderSearch.add(folderItem);
                }
            }

            for (DocumentItem documentItem : listDocument) {
                String nameFile = VNCharacterUtils.removeAccent(documentItem.getName()).replace(" ", "").toLowerCase();

                if (nameFile.contains(nameSearch)) {
                    listDocumentSearch.add(documentItem);
                }
            }

            for (int index = 0; index < listDocumentSearch.size(); index++) {
                listDocumentSearch.get(index).setPosition(index);
            }

            if (this.mainView != null) {
                this.mainView.showListSearch(listFolderSearch, listDocumentSearch);
            }
        }
    }

    @Override
    public void setInfoMerge() {
        boolean isDocument = this.mapSelectedDocument.size() > 0;
        String title = FileUtils.createNameFolder(this.context);

        if (isDocument) {
            if (this.mainView != null) {
                List<DocumentItem> list = new ArrayList<>(this.mapSelectedDocument.values());
                this.mainView.showDialogMerge(title, list.get(0).getThumbnail());
            }
        } else {
            if (this.mainView != null) {
                this.mainView.showDialogMerge(title, "");
            }
        }
    }

    @Override
    public void renameFolder(@NotNull BaseEntity baseEntity) {
        if (TextUtils.isEmpty(baseEntity.getName().replace(" ", ""))) {
            Toast.makeText(context, R.string.home_folder_et_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        if (FileUtils.isInvalidFDocName(baseEntity.getName())) {
            Toast.makeText(this.context, R.string.save_alert_input_invalid_name, Toast.LENGTH_SHORT).show();
            return;
        }

        if (baseEntity instanceof FolderItem) {
            FolderItem folderItem = this.appDatabases.folderDao().getFolderItemById(baseEntity.getId());
            folderItem.setName(baseEntity.getName());

            this.appDatabases.folderDao()
                    .updateEntity(folderItem)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new CompletableObserver() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {
                            addDisposable(d);

                        }

                        @Override
                        public void onComplete() {
                            if (mainView != null) {
                                mainView.onSuccessRenameFolder();
                            }
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            e.printStackTrace();
                        }
                    });
        }

        if (baseEntity instanceof DocumentItem) {
            DocumentItem documentItem = this.appDatabases.documentDao().getDocumentByIdNoRx(baseEntity.getId());
            documentItem.setName(baseEntity.getName());

            this.appDatabases.documentDao()
                    .updateEntity(documentItem)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new CompletableObserver() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {
                            addDisposable(d);
                        }

                        @Override
                        public void onComplete() {
                            if (mainView != null) {
                                mainView.onSuccessRenameFolder();
                            }
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            e.printStackTrace();
                        }
                    });
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
    public void setListItemToHmDocumentItem(List<DocumentItem> documentItems) {
        if (this.mapSelectedDocument != null) {
            this.mapSelectedDocument.clear();
            for (DocumentItem documentItem : documentItems) {
                this.mapSelectedDocument.put(documentItem.getId(), documentItem);
            }
        }
    }

    @Override
    public void setItemToHmFolderItem(@NotNull FolderItem folderItem) {
        if (!this.mapSelectedFolder.containsKey(folderItem.getId())) {
            this.mapSelectedFolder.put(folderItem.getId(), folderItem);
            return;
        }

        this.mapSelectedFolder.remove(folderItem.getId());
    }

    @Override
    public void setListItemToHmFolderItem(List<FolderItem> folderItems) {
        if (this.mapSelectedFolder != null) {
            this.mapSelectedFolder.clear();

            for (FolderItem folderItem : folderItems) {
                this.mapSelectedFolder.put(folderItem.getId(), folderItem);
            }
        }
    }

    @Override
    public void startCamera() {
        Completable.create(emitter -> {
            AppPref.getInstance(context).removePref();
            DbUtils.clearPreviousSession(this.appDatabases);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        composeDispose(d);
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
    public void savePdf(String title, int quality) {
        List<FolderItem> folderItems = DbUtils.getListFolderItem(new ArrayList<>(this.mapSelectedFolder.values()));
        List<DocumentItem> documentItems = DbUtils.getListDocumentItem(new ArrayList<>(this.mapSelectedDocument.values()));
        DbUtils.convertToDocumentItem(this.context, folderItems, documentItems,
                new DbUtils.ConvertToDocumentItem() {
                    @Override
                    public void onDisposable(Disposable disposable) {

                    }

                    @Override
                    public void onSuccess(List<DocumentItem> documentItems) {
                        shareAndSavePdf(title, quality, documentItems, true);
                    }

                    @Override
                    public void onError() {

                    }
                });
    }

    private void shareAndSavePdf(String title, int quality, List<DocumentItem> documentItems, boolean isSavePdf) {
        PdfUtils.convertPdf(context, quality, title, documentItems, new PdfUtils.PdfStatus() {
            @Override
            public void onDisposable(Disposable disposable) {
                addDisposable(disposable);
            }

            @Override
            public void onSuccessConvertPdf(List<String> listPdf) {
                if (isSavePdf) {
                    if (mainView != null) {
                        mainView.onSuccessSavePdf(listPdf);
                    }
                    return;
                }
                FileUtils.sendfile(context, listPdf);
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
    public void saveJpg(String title, int quality) {
        List<FolderItem> folderItems = DbUtils.getListFolderItem(new ArrayList<>(this.mapSelectedFolder.values()));
        List<DocumentItem> documentItems = DbUtils.getListDocumentItem(new ArrayList<>(this.mapSelectedDocument.values()));
        DbUtils.shareJpgToGallery(this.context, folderItems, documentItems, title, quality, new DbUtils.StatusShareGallery() {
            @Override
            public void onDisposable(Disposable disposable) {
                addDisposable(disposable);
            }

            @Override
            public void onSuccess(List<String> listFile) {
                if (mainView != null) {
                    mainView.onSuccessSaveGallery(listFile);
                }
            }

            @Override
            public void onError() {

            }
        });
    }

    @Override
    public void sharePdf(String title, int quality) {
        List<FolderItem> folderItems = DbUtils.getListFolderItem(new ArrayList<>(this.mapSelectedFolder.values()));
        List<DocumentItem> documentItems = DbUtils.getListDocumentItem(new ArrayList<>(this.mapSelectedDocument.values()));

        DbUtils.convertToDocumentItem(this.context, folderItems, documentItems,
                new DbUtils.ConvertToDocumentItem() {
                    @Override
                    public void onDisposable(Disposable disposable) {
                        addDisposable(disposable);
                    }

                    @Override
                    public void onSuccess(List<DocumentItem> documentItems) {
                        shareAndSavePdf(title, quality, documentItems, false);
                    }

                    @Override
                    public void onError() {

                    }
                });
    }

    @Override
    public void shareJpg(String title) {
        Single.create((SingleOnSubscribe<ArrayList<String>>) emitter -> {
            List<DocumentItem> listAllDocumentSelect = new ArrayList<>();
            List<FolderItem> listFolder = DbUtils.getListFolderItem(new ArrayList<>(this.mapSelectedFolder.values()));

            for (FolderItem folderItem : listFolder) {
                listAllDocumentSelect.addAll(this.appDatabases.documentDao().getListDocumentByIdFolderNoRx(folderItem.getId()));
            }

            listAllDocumentSelect.addAll(new ArrayList<>(this.mapSelectedDocument.values()));
            List<PageItem> pageItemList = new ArrayList<>();
            for (DocumentItem documentItem : listAllDocumentSelect) {
                pageItemList.addAll(this.appDatabases.pageDao().getListPageByDocumentIdNoRx(documentItem.getId()));
            }

            ArrayList<String> listPath = new ArrayList<>();
            for (PageItem pageItem : pageItemList) {
                listPath.add(pageItem.getEnhanceUri());
            }

            emitter.onSuccess(listPath);
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<ArrayList<String>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        addDisposable(d);
                    }

                    @Override
                    public void onSuccess(@NonNull ArrayList<String> strings) {
                        ImageUtils.shareToShare(context, strings);
                        resetHmDocumentSelect();
                        resetHmFolderSelect();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

    @Override
    public void openPdfSettings() {
        List<FolderItem> folderItemsShare = new ArrayList<>(this.mapSelectedFolder.values());
        List<DocumentItem> documentItemsShare = new ArrayList<>(this.mapSelectedDocument.values());
        DbUtils.convertToDocumentItemsId(this.context, folderItemsShare, documentItemsShare, new DbUtils.ConvertToDocumentCallback() {
            @Override
            public void onDisposable(Disposable disposable) {
                addDisposable(disposable);
            }

            @Override
            public void onSuccess(ArrayList<Integer> listId) {
                Intent intent = new Intent(context, PdfSettingsActivity.class);
                intent.putIntegerArrayListExtra(Constants.EXTRA_SHARE_DOCUMENT, listId);
                ((Activity) context).startActivityForResult(intent, Constants.SHARE_REQUEST_CODE);
            }

            @Override
            public void onError() {
            }
        });
    }

    @Override
    public void setDisposableAll() {
        if (this.disposable != null) {
            this.disposable.dispose();
        }
    }
}
