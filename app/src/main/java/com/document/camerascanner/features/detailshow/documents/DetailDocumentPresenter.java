package com.document.camerascanner.features.detailshow.documents;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.text.format.Formatter;

import com.document.camerascanner.R;
import com.document.camerascanner.databases.AppDatabases;
import com.document.camerascanner.databases.model.DocumentItem;
import com.document.camerascanner.databases.model.PageItem;
import com.document.camerascanner.features.detailshow.documents.interfaces.DetailDocument;
import com.document.camerascanner.features.pdfsettings.PdfSettingsActivity;
import com.document.camerascanner.utils.Constants;
import com.document.camerascanner.utils.DbUtils;
import com.document.camerascanner.utils.FileUtils;
import com.document.camerascanner.utils.PdfUtils;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class DetailDocumentPresenter implements DetailDocument.Presenter {

    private final DetailDocument.View callBack;

    private CompositeDisposable compositeDisposable;

    public DetailDocumentPresenter(DetailDocument.View callBack) {
        this.callBack = callBack;
        this.compositeDisposable = new CompositeDisposable();
    }

    @Override
    public void addDisposable(Disposable disposable) {
        if (this.compositeDisposable == null) {
            this.compositeDisposable = new CompositeDisposable();
        }
        this.compositeDisposable.add(disposable);
    }

    @Override
    public void copyPages(Context context, List<PageItem> listCopy) {
        Single.create((SingleOnSubscribe<List<PageItem>>) emitter -> {
            File fileSource = context.getExternalFilesDir(Constants.ALL_TEMP);

            for (int i = 0; i < listCopy.size(); i++) {
                PageItem pageItem = listCopy.get(i);
                long currentTime = System.currentTimeMillis();

                if (!TextUtils.isEmpty(pageItem.getOrgUri())) {
                    String fileName = new File(pageItem.getOrgUri()).getName();
                    String dotFile = fileName.substring(fileName.lastIndexOf("."));
                    String nameOri = context.getString(R.string.all_name_current_time_ori, Constants.IMAGE_ORIGINAL, currentTime, dotFile);
                    FileUtils.copyFile(new File(pageItem.getOrgUri()), new File(fileSource, nameOri));
                    pageItem.setOrgUri(new File(fileSource, nameOri).getPath());
                }

                if (!TextUtils.isEmpty(pageItem.getEnhanceUri())) {
                    String fileName = new File(pageItem.getEnhanceUri()).getName();
                    String dotFile = fileName.substring(fileName.lastIndexOf("."));
                    String nameEnhance = context.getString(R.string.all_name_current_time_ori, Constants.IMAGE_ENHANCE, currentTime, dotFile);
                    FileUtils.copyFile(new File(pageItem.getEnhanceUri()), new File(fileSource, nameEnhance));
                    pageItem.setEnhanceUri(new File(fileSource, nameEnhance).getPath());
                    pageItem.setSize(new File(fileSource, nameEnhance).length());
                }
            }

            emitter.onSuccess(listCopy);
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<PageItem>>() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        addDisposable(d);
                    }

                    @Override
                    public void onSuccess(@io.reactivex.annotations.NonNull List<PageItem> list) {
                        if (callBack != null) {
                            callBack.onCopyImageSucces(list);
                        }
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

    @Override
    public void getListTargetDocument(Context context, int currentId) {
        AppDatabases.getInstance(context)
                .documentDao()
                .getTargetDocumentsRX(currentId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<DocumentItem>>() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        addDisposable(d);
                    }

                    @Override
                    public void onSuccess(@io.reactivex.annotations.NonNull List<DocumentItem> list) {
                        if (callBack != null) {
                            callBack.onShowTargetDocuments(list);
                        }
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

    @Override
    public void generalSharePdf(Context context, List<PageItem> list, List<Integer> listSelected, DocumentItem documentItem, boolean isPdfAll) {
        if (isPdfAll) {
            this.openPdfSettings(context, documentItem);
        } else {
            this.openPdfSettings(context, list, listSelected);
        }
    }

    private void openPdfSettings(Context context, DocumentItem documentItem) {
        if (context == null) {
            return;
        }

        ArrayList<Integer> listFile = new ArrayList<>();
        listFile.add(documentItem.getId());
        Intent intent = new Intent(context, PdfSettingsActivity.class);
        intent.putExtra(Constants.EXTRA_SHARE_DOCUMENT, listFile);
        context.startActivity(intent);
    }

    private void openPdfSettings(Context context, List<PageItem> list, List<Integer> listSelected) {
        if (context == null) {
            return;
        }

        ArrayList<Integer> listFile = new ArrayList<>();
        for (Integer integer : listSelected) {
            listFile.add(list.get(integer).getId());
        }

        Intent intent = new Intent(context, PdfSettingsActivity.class);
        intent.putExtra(Constants.EXTRA_IMAGE_SELECT, listFile);
        context.startActivity(intent);
    }

    @Override
    public void shareToJpgShare(Context context, @NotNull List<PageItem> list) {
        ArrayList<String> listShare = new ArrayList<>();
        for (PageItem itemFile : list) {
            if (!itemFile.isSelected()) {
                continue;
            }

            File file = new File(itemFile.getEnhanceUri());
            if (FileUtils.isFileImage(file) && FileUtils.isEnhanced(file)) {
                listShare.add(file.getPath());
            }
        }

        FileUtils.sendfile(context, listShare);
    }

    @Override
    public void shareToGallery(Context context, String title, int quality, DocumentItem documentItem, List<PageItem> list) {
        if (context == null) {
            return;
        }

        List<PageItem> pageSelect = new ArrayList<>();
        for (PageItem pageItem : list) {
            if (pageItem.isSelected()) {
                pageSelect.add(pageItem);
            }
        }

        documentItem.setName(title);

        DbUtils.sharePageToGallery(context, documentItem, title, quality, pageSelect, new DbUtils.StatusShareGallery() {
            @Override
            public void onDisposable(Disposable disposable) {
                addDisposable(disposable);
            }

            @Override
            public void onSuccess(List<String> listFile) {
                if (callBack != null) {
                    callBack.onSuccessShareToGallery(listFile);
                }
            }

            @Override
            public void onError() {

            }
        });
    }

    @Override
    public void calculatorSize(Context context, List<PageItem> list) {
        if (list == null) {
            return;
        }

        long size = 0;
        for (PageItem itemFile : list) {
            if (itemFile.isSelected()) {
                size += itemFile.getSize();
            }
        }

        if (this.callBack == null) {
            return;
        }

        this.callBack.onUpdateSizeFileSelect(Formatter.formatFileSize(context, size));
    }

    @Override
    public void insertTempPage(Context context, Intent data) {
        AppDatabases appDatabases = AppDatabases.getInstance(context);

        Single.create((SingleOnSubscribe<List<PageItem>>) emitter -> {
            DbUtils.clearPreviousSession(appDatabases);
            File fileSource = context.getExternalFilesDir(Constants.ALL_TEMP);

            if (data.getData() == null) {
                ClipData clipData = data.getClipData();
                if (clipData == null) {
                    return;
                }

                List<PageItem> listTemp = new ArrayList<>();
                for (int index = 0; index < clipData.getItemCount(); index++) {
                    ClipData.Item item = clipData.getItemAt(index);
                    String realPath = FileUtils.getRealUri(context, item.getUri());
                    listTemp.add(DbUtils.createTempPage(context, fileSource, realPath, index + 1));
                }

                appDatabases.pageDao().insertListEntityNoRx(listTemp);
                emitter.onSuccess(appDatabases.pageDao().getPreviousTempPage());
                return;
            }

            Uri uri = data.getData();
            String realPath = FileUtils.getRealUri(context, uri);
            appDatabases.pageDao().insertEntityNoRx(DbUtils.createTempPage(context, fileSource, realPath, 1));

            emitter.onSuccess(appDatabases.pageDao().getPreviousTempPage());
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<PageItem>>() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        addDisposable(d);
                    }

                    @Override
                    public void onSuccess(@io.reactivex.annotations.NonNull List<PageItem> listTemp) {
                        if (listTemp.size() == 1) {
                            if (callBack != null) {
                                callBack.onSingleImportSuccess(listTemp.get(0));
                            }
                            return;
                        }

                        if (callBack != null) {
                            callBack.onMultipleImportSuccess();
                        }
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

    @Override
    public void createDocumentForCopyAndMove(Context context, String nameDocument) {
        AppDatabases appDatabases = AppDatabases.getInstance(context);

        Single.create((SingleOnSubscribe<DocumentItem>) emitter -> {
            DocumentItem documentItem = new DocumentItem();
            documentItem.setName(nameDocument);
            documentItem.setParentId(1);
            Long id = appDatabases.documentDao().insertEntityNoRx(documentItem);
            documentItem.setId(id.intValue());
            emitter.onSuccess(documentItem);
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<DocumentItem>() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        addDisposable(d);
                    }

                    @Override
                    public void onSuccess(@io.reactivex.annotations.NonNull DocumentItem documentItem) {
                        if (callBack != null) {
                            callBack.onSucessCreateDocument(documentItem);
                        }
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

    @Override
    public void saveAndSharePdf(Context context, String title, int quality, List<PageItem> list, List<Integer> listSelected, DocumentItem documentItem, boolean isPdfAll, boolean isSave) {

        documentItem.setName(title);
        List<DocumentItem> documentItems = new ArrayList<>();
        if (isPdfAll) {
            documentItem.setListPage(list);
        } else {
            List<PageItem> pageSelect = new ArrayList<>();
            for (Integer integer : listSelected) {
                pageSelect.add(list.get(integer));
            }
            documentItem.setListPage(pageSelect);
        }
        documentItems.add(documentItem);

        PdfUtils.convertPdf(context, quality, title, documentItems, new PdfUtils.PdfStatus() {
            @Override
            public void onDisposable(Disposable disposable) {
                addDisposable(disposable);
            }

            @Override
            public void onSuccessConvertPdf(List<String> listPdf) {
                if (isSave) {
                    if (callBack != null) {
                        callBack.onSuccessSavePdf(listPdf);
                        return;
                    }
                }

                if (callBack != null) {
                    callBack.onSuccessSharePdf();
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
    public void disposableAll() {
        if (this.compositeDisposable != null) {
            this.compositeDisposable.dispose();
        }
    }

}
