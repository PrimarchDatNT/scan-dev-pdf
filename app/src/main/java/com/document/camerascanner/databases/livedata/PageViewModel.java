package com.document.camerascanner.databases.livedata;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.document.camerascanner.databases.model.DocumentItem;
import com.document.camerascanner.databases.model.PageItem;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import io.reactivex.disposables.Disposable;

public class PageViewModel extends BaseViewModel<PageItem, PageRepository> {

    public PageViewModel(@NonNull Application application) {
        super(application);
    }

    @Override
    public @NotNull PageRepository getRepository(Application application) {
        return new PageRepository(application);
    }

    public LiveData<DocumentItem> getDocument(int id) {
        return this.repository.getPageParent(id);
    }

    public LiveData<List<PageItem>> getPageByParent(int parrentId) {
        return this.repository.getAllPageByParent(parrentId);
    }

    @Contract(value = " -> new", pure = true)
    private BaseRepository.@NotNull Callback getCallback(){
        return new BaseRepository.Callback() {
            @Override
            public void onSubDispose(Disposable d) {
                subcscribeDispose(d);
            }

            @Override
            public void onExcuteDataSucces() {
            }

            @Override
            public void onExcuteError() {
            }
        };
    }

    public void renameDocument(DocumentItem documentItem) {
        this.repository.renamePageParent(documentItem, this.getCallback());
    }

    public void deletePageParent(DocumentItem documentItem){
        this.repository.deletePageParent(documentItem, this.getCallback());
    }

    @Override
    public void insert(PageItem pageItem) {
        this.repository.insertItem(pageItem, this.getCallback());
    }

    @Override
    public void multipleInsert(List<PageItem> listItem) {
        this.repository.insertMultipleItem(listItem, this.getCallback());
    }

    @Override
    public void update(PageItem item) {
        this.repository.updateItem(item, this.getCallback());
    }

    @Override
    public void multipleUpdate(List<PageItem> listItem) {
        this.repository.updateMultipleItem(listItem, this.getCallback());
    }

    @Override
    public void delete(PageItem item) {
        this.repository.deleteItem(item, this.getCallback());
    }

    @Override
    public void multipleDelete(List<PageItem> listItem) {
        this.repository.deleteMultipleItem(listItem, this.getCallback());
    }

}
