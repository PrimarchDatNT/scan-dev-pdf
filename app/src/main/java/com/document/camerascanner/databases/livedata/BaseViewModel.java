package com.document.camerascanner.databases.livedata;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import java.util.List;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public abstract class BaseViewModel<E, R> extends AndroidViewModel {

    private final CompositeDisposable compositeDisposable;

    protected R repository;

    public BaseViewModel(@NonNull Application application) {
        super(application);
        this.repository = this.getRepository(application);
        this.compositeDisposable = new CompositeDisposable();
    }

    public void subcscribeDispose(Disposable disposable){
        if (this.compositeDisposable != null) {
            this.compositeDisposable.add(disposable);
        }
    }

    @Override
    protected void onCleared() {
        if (this.compositeDisposable != null) {
            this.compositeDisposable.dispose();
            this.compositeDisposable.clear();
        }
    }

    protected abstract R getRepository(Application application);

    public abstract void insert(E item);

    public abstract void multipleInsert(List<E> listItem);

    public abstract void update(E item);

    public abstract void multipleUpdate(List<E> listItem);

    public abstract void delete(E item);

    public abstract void multipleDelete(List<E> listItem);

}
