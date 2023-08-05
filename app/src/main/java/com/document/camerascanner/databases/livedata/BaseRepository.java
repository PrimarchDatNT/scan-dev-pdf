package com.document.camerascanner.databases.livedata;

import android.app.Application;

import com.document.camerascanner.databases.AppDatabases;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public abstract class BaseRepository<E> {

    private final AppDatabases appDatabases;

    public BaseRepository(Application application) {
        this.appDatabases = AppDatabases.getInstance(application);
    }

    protected Completable applySchedulers(@NotNull Completable completable) {
        return completable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    protected AppDatabases getAppDatabases() {
        return this.appDatabases;
    }

    public abstract CompletableObserver subcribeObsever(Callback callback);

    public abstract void insertItem(E item, Callback callback);

    public abstract void insertMultipleItem(List<E> listItem, Callback callback);

    public abstract void updateItem(E item, Callback callback);

    public abstract void updateMultipleItem(List<E> listItem, Callback callback);

    public abstract void deleteItem(E item, Callback callback);

    public abstract void deleteMultipleItem(List<E> listItem, Callback callback);

    public interface Callback {

        void onSubDispose(Disposable d);

        void onExcuteDataSucces();

        void onExcuteError();
    }

}
