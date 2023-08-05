package com.document.camerascanner.features.enhance;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;

import com.document.camerascanner.databases.AppDatabases;
import com.document.camerascanner.databases.model.DocumentItem;
import com.document.camerascanner.databases.model.PageItem;
import com.document.camerascanner.utils.AppUtils;
import com.document.camerascanner.utils.Constants;
import com.document.camerascanner.utils.ImageUtils;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class EnhancePresenter implements Enhance.Presenter {

    private final Enhance.View callback;

    public EnhancePresenter(Enhance.View callback) {
        this.callback = callback;
    }

    @Override
    public void loadEnhanceImage(Context context, Uri uri) {
        Single.create((SingleOnSubscribe<Bitmap>) emitter -> {
            try {
                int[] reqSize = AppUtils.getScreenSize((Activity) context);
                emitter.onSuccess(ImageUtils.decodeOrginalBitmap(uri, reqSize[0], reqSize[1]));
            } catch (Exception e) {
                emitter.onError(e);
            }

        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Bitmap>() {
                    @Override
                    public void onSubscribe(@NotNull Disposable d) {
                        if (callback != null) {
                            callback.onEnhanceDispose(d);
                        }
                    }

                    @Override
                    public void onSuccess(@NotNull Bitmap bitmap) {
                        if (callback != null) {
                            callback.onLoadImageResult(bitmap);
                        }
                    }

                    @Override
                    public void onError(@NotNull Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

    @Override
    public void applyFilter(int type, Bitmap bitmap) {
        Single.create((SingleOnSubscribe<Bitmap>) emitter -> {
            NativeFilter nativeFilter = new NativeFilter();
            switch (type) {
                case FilterType.MAGIC_COLOR:
                    emitter.onSuccess(nativeFilter.applyMagicColor(bitmap.copy(bitmap.getConfig(), false)));
                    break;

                case FilterType.GREY_SCALE:
                    emitter.onSuccess(nativeFilter.applyGrayFilter(bitmap.copy(bitmap.getConfig(), false)));
                    break;

                case FilterType.BLACK_AND_WHITE:
                    emitter.onSuccess(nativeFilter.applyBnW1Filter(bitmap.copy(bitmap.getConfig(), false)));
                    break;

                case FilterType.BLACK_AND_WHITE_2:
                    emitter.onSuccess(nativeFilter.applyBnW2Filter(bitmap.copy(bitmap.getConfig(), false)));
                    break;

                case FilterType.NO_SHADOW:
                    emitter.onSuccess(nativeFilter.applyNoShadowFilter(bitmap.copy(bitmap.getConfig(), false)));
                    break;

                case FilterType.ORIGNAL:
                    emitter.onSuccess(bitmap);
                    break;
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Bitmap>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        if (callback != null) {
                            callback.onEnhanceDispose(d);
                        }
                    }

                    @Override
                    public void onSuccess(@NonNull Bitmap bitmap) {
                        if (callback != null) {
                            callback.onFilterResult(bitmap);
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

    public void saveSingleDocument(Context context, int folderId, String documentName, Bitmap bitmap, PageItem pageItem) {
        if (pageItem == null || bitmap == null || documentName == null || TextUtils.isEmpty(documentName)) {
            if (this.callback != null) {
                this.callback.onSaveError();
            }
            return;
        }

        Single.create((SingleOnSubscribe<DocumentItem>) emitter -> {
            try {
                AppDatabases appDatabases = AppDatabases.getInstance(context);
                DocumentItem documentItem = new DocumentItem();
                documentItem.setName(documentName);
                documentItem.setParentId(folderId);
                int documentId = appDatabases.documentDao().insertEntityNoRx(documentItem).intValue();

                String fileName = pageItem.getOrgUri().replaceAll(Constants.IMAGE_ORIGINAL, Constants.IMAGE_ENHANCE);
                File enhancedFile = new File(fileName);
                String enhancedFilePath = enhancedFile.getPath();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(enhancedFilePath));
                pageItem.setEnhanceUri(enhancedFilePath);
                pageItem.setLoading(false);
                pageItem.setPosition(1);
                pageItem.setParentId(documentId);
                pageItem.setSize(new File(enhancedFilePath).length());
                appDatabases.pageDao().updateEntityNoRx(pageItem);

                documentItem.setId(documentId);
                emitter.onSuccess(documentItem);
            } catch (Exception exception) {
                emitter.onError(exception);
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<DocumentItem>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        if (callback != null) {
                            callback.onEnhanceDispose(d);
                        }
                    }

                    @Override
                    public void onSuccess(@NonNull DocumentItem documentItem) {
                        if (callback != null) {
                            callback.onDocumentSucces(documentItem);
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        e.printStackTrace();
                        if (callback != null) {
                            callback.onSaveError();
                        }
                    }
                });
    }

    @Override
    public void saveEnhace(Context context, boolean isFromDetailDoc, PageItem pageItem, Bitmap bitmap) {
        if (pageItem == null || bitmap == null) {
            if (this.callback != null) {
                this.callback.onSaveError();
            }
            return;
        }

        Completable.create(emitter -> {
            try {
                AppDatabases appDatabases = AppDatabases.getInstance(context);
                String fileName = pageItem.getOrgUri().replaceAll(Constants.IMAGE_ORIGINAL, Constants.IMAGE_ENHANCE);
                File enhancedFile = new File(fileName);
                String enhancedFilePath = enhancedFile.getPath();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(enhancedFilePath));
                pageItem.setEnhanceUri(enhancedFilePath);
                pageItem.setLoading(false);

                if (isFromDetailDoc) {
                    int pageCount = appDatabases.documentDao().getDocumentByIdNoRx(pageItem.getParentId()).getChildCount();
                    pageItem.setPosition(pageCount + 1);
                }

                pageItem.setSize(new File(enhancedFilePath).length());
                appDatabases.pageDao().updateEntityNoRx(pageItem);

                emitter.onComplete();
            } catch (Exception exception) {
                emitter.onError(exception);
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NotNull Disposable d) {
                        if (callback != null) {
                            callback.onEnhanceDispose(d);
                        }
                    }

                    @Override
                    public void onComplete() {
                        if (callback != null) {
                            callback.onSaveEnhanceSucces();
                        }
                    }

                    @Override
                    public void onError(@NotNull Throwable e) {
                        e.printStackTrace();
                        if (callback != null) {
                            callback.onSaveError();
                        }
                    }
                });
    }

}
