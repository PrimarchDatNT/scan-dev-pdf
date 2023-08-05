package com.document.camerascanner.features.save;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.document.camerascanner.databases.AppDatabases;
import com.document.camerascanner.databases.dao.PageDao;
import com.document.camerascanner.databases.model.DocumentItem;
import com.document.camerascanner.databases.model.PageItem;
import com.document.camerascanner.features.enhance.FilterType;
import com.document.camerascanner.features.enhance.NativeFilter;
import com.document.camerascanner.features.save.ProcessFilter.Presenter;
import com.document.camerascanner.utils.AppUtils;
import com.document.camerascanner.utils.Constants;
import com.document.camerascanner.utils.ImageUtils;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class FilterPresenter implements Presenter {

    private final ProcessFilter.View callback;

    public FilterPresenter(ProcessFilter.View callback) {
        this.callback = callback;
    }

    @Override
    public void loadTempImage(Context context) {
        AppDatabases.getInstance(context)
                .pageDao()
                .getListTempPage()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<PageItem>>() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        if (callback != null) {
                            callback.onProcessDispose(d);
                        }
                    }

                    @Override
                    public void onSuccess(@io.reactivex.annotations.NonNull List<PageItem> list) {
                        if (callback != null) {
                            callback.onShowTempImage(list);
                        }
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

    @Override
    public void loadImage(Context context, int filterType, List<PageItem> pageItems) {
        AppDatabases appDatabases = AppDatabases.getInstance(context);
        int[] screen = AppUtils.getScreenSize((Activity) context);

        Observable.fromIterable(pageItems)
                .flatMap((Function<PageItem, ObservableSource<PageItem>>) pageItem ->
                        Observable.just(pageItem)
                                .subscribeOn(Schedulers.computation())
                                .map((Function<PageItem, PageItem>) inputItem -> {
                                    PageItem resultItem = new PageItem();
                                    resultItem.setId(inputItem.getId());
                                    resultItem.setPosition(inputItem.getPosition());
                                    resultItem.setParentId(inputItem.getParentId());
                                    resultItem.setOrgUri(inputItem.getOrgUri());
                                    resultItem.setCacheVertex(inputItem.getCacheVertex());
                                    resultItem.setLoading(false);

                                    Uri uri = Uri.parse(inputItem.getOrgUri());
                                    Bitmap bitmap = ImageUtils.decodeOrginalBitmap(uri, screen[0], screen[1]);
                                    String enhancePath = "";

                                    if (bitmap != null) {
                                        enhancePath = this.saveToStorage(uri, this.applyFilter(filterType, bitmap));
                                    }

                                    if (enhancePath != null && !TextUtils.isEmpty(enhancePath)) {
                                        resultItem.setEnhanceUri(enhancePath);
                                        resultItem.setSize(new File(enhancePath).length());
                                    }

                                    appDatabases.pageDao().updateEntityNoRx(resultItem);

                                    return resultItem;
                                }))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<PageItem>() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        if (callback != null) {
                            callback.onProcessDispose(d);
                        }
                    }

                    @Override
                    public void onNext(@io.reactivex.annotations.NonNull PageItem pageItem) {
                        if (callback != null) {
                            callback.onFilterResult(pageItem);
                        }
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        if (callback != null) {
                            callback.onFilterSuccess();
                        }
                    }
                });
    }

    @Override
    public void onStartSave(Context context, @NotNull DocumentItem documentItem, List<PageItem> listItem) {
        AppDatabases appDatabases = AppDatabases.getInstance(context);

        int documentItemId = documentItem.getId();
        if (documentItemId > 0) {
            this.insertPages(appDatabases.pageDao(), documentItemId, documentItem.getChildCount(), listItem);
            return;
        }

        appDatabases.documentDao()
                .insertEntity(documentItem)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Long>() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        if (callback != null) {
                            callback.onProcessDispose(d);
                        }
                    }

                    @Override
                    public void onSuccess(@io.reactivex.annotations.NonNull Long id) {
                        insertPages(appDatabases.pageDao(), id.intValue(), 0, listItem);
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

    private Bitmap applyFilter(@FilterType int filterType, @NonNull Bitmap bitmap) {
        NativeFilter nativeFilter = new NativeFilter();

        if (filterType == FilterType.MAGIC_COLOR) {
            return nativeFilter.applyMagicColor(bitmap);
        }

        if (filterType == FilterType.GREY_SCALE) {
            return nativeFilter.applyGrayFilter(bitmap);
        }

        if (filterType == FilterType.BLACK_AND_WHITE) {
            return nativeFilter.applyBnW1Filter(bitmap);
        }

        if (filterType == FilterType.BLACK_AND_WHITE_2) {
            return nativeFilter.applyBnW2Filter(bitmap);
        }

        if (filterType == FilterType.NO_SHADOW) {
            return nativeFilter.applyNoShadowFilter(bitmap);
        }

        return bitmap;
    }

    private String saveToStorage(@NonNull Uri uri, Bitmap bitmap) {
        if (uri.getPath() == null) {
            return null;
        }

        File fileRoot = new File(uri.getPath());
        if (!fileRoot.exists()) {
            return null;
        }

        String rootName = fileRoot.getPath();
        String enhancePath = rootName.replaceAll(Constants.IMAGE_ORIGINAL, Constants.IMAGE_ENHANCE);

        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(enhancePath));
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return enhancePath;
    }

    private void insertPages(PageDao pageDao, int parentId, int pageCount, List<PageItem> listItem) {
        Completable.create(emitter -> {
            for (PageItem pageItem : listItem) {
                pageItem.setParentId(parentId);
                pageItem.setPosition(pageCount + pageItem.getPosition());
            }

            pageDao.insertListEntityNoRx(listItem);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        if (callback != null) {
                            callback.onProcessDispose(d);
                        }
                    }

                    @Override
                    public void onComplete() {
                        if (callback != null) {
                            callback.onSaveSuccess(parentId);
                        }
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

}
