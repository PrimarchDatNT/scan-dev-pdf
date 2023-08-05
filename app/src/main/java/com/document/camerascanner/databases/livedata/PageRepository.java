package com.document.camerascanner.databases.livedata;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.document.camerascanner.databases.dao.DocumentDao;
import com.document.camerascanner.databases.dao.PageDao;
import com.document.camerascanner.databases.model.DocumentItem;
import com.document.camerascanner.databases.model.PageItem;
import com.document.camerascanner.utils.FileUtils;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;

public final class PageRepository extends BaseRepository<PageItem> {

    private final PageDao pageDao;

    private final DocumentDao documentDao;

    public PageRepository(Application application) {
        super(application);
        this.pageDao = this.getAppDatabases().pageDao();
        this.documentDao = this.getAppDatabases().documentDao();
    }

    @Contract(value = "_ -> new", pure = true)
    @Override
    public @NotNull CompletableObserver subcribeObsever(Callback callback) {
        return new CompletableObserver() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                if (callback != null) {
                    callback.onSubDispose(d);
                }
            }

            @Override
            public void onComplete() {
                if (callback != null) {
                    callback.onExcuteDataSucces();
                }
            }

            @Override
            public void onError(@NonNull Throwable e) {
                e.printStackTrace();
                callback.onExcuteError();
            }
        };
    }

    public LiveData<List<PageItem>> getAllPageByParent(int id) {
        return this.pageDao.getListPagebyDocumentId(id);
    }

    public LiveData<DocumentItem> getPageParent(int id) {
        return this.documentDao.getPageParentById(id);
    }

    public void renamePageParent(DocumentItem documentItem, Callback callback) {
        this.applySchedulers(this.documentDao.updateEntity(documentItem)).subscribe(this.subcribeObsever(callback));
    }

    @Override
    public void insertItem(PageItem item, Callback callback) {
        this.applySchedulers(this.pageDao.updateEntity(item)).subscribe(this.subcribeObsever(callback));
    }

    @Override
    public void insertMultipleItem(List<PageItem> listItem, Callback callback) {
        this.applySchedulers(this.pageDao.insertListEntity(listItem)).subscribe(this.subcribeObsever(callback));
    }

    @Override
    public void updateItem(PageItem item, Callback callback) {
        this.applySchedulers(this.pageDao.updateEntity(item)).subscribe(this.subcribeObsever(callback));
    }

    @Override
    public void updateMultipleItem(List<PageItem> listItem, Callback callback) {
        this.applySchedulers(this.pageDao.updateListEntity(listItem)).subscribe(this.subcribeObsever(callback));
    }

    @Override
    public void deleteItem(PageItem item, Callback callback) {
        this.applySchedulers(this.pageDao.deleteEntity(item)).subscribe(this.subcribeObsever(callback));
    }

    public void deletePageParent(DocumentItem documentItem, Callback callback) {
        this.applySchedulers(this.documentDao.deleteEntity(documentItem)).subscribe(this.subcribeObsever(callback));
    }

    @Override
    public void deleteMultipleItem(List<PageItem> listItem, Callback callback) {
        Completable deleteImage = new Completable() {
            @Override
            protected void subscribeActual(CompletableObserver observer) {
                for (int i = 0; i < listItem.size(); i++) {
                    PageItem pageItem = listItem.get(i);

                    if (pageItem.isSelected()) {
                        FileUtils.deletePageItem(pageItem);
                    }
                }
            }
        };

        Completable deleteInDB = new Completable() {
            @Override
            protected void subscribeActual(CompletableObserver observer) {
                List<PageItem> listDelete = new ArrayList<>();

                for (int i = 0; i < listItem.size(); i++) {
                    PageItem pageItem = listItem.get(i);
                    if (pageItem.isSelected()) {
                        listDelete.add(pageItem);
                    }
                }

                listItem.removeAll(listDelete);

                if (listItem.isEmpty()) {
                    documentDao.deleteEntityByIdNoRx(listDelete.get(0).getParentId());
                    return;
                }

                for (int i = 0; i < listItem.size(); i++) {
                    PageItem pageItem = listItem.get(i);
                    pageItem.setPosition(i + 1);
                    listItem.set(i, pageItem);
                }

                pageDao.deleteListEntityNoRx(listDelete);
                pageDao.updateListEntityNoRx(listItem);
            }
        };

        this.applySchedulers(Completable.mergeArray(deleteImage, deleteInDB)).subscribe(this.subcribeObsever(callback));
    }
}
