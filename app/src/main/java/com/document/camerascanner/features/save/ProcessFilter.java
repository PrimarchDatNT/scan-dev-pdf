package com.document.camerascanner.features.save;

import android.content.Context;


import com.document.camerascanner.databases.model.DocumentItem;
import com.document.camerascanner.databases.model.PageItem;
import com.document.camerascanner.features.enhance.FilterType;

import java.util.List;

import io.reactivex.disposables.Disposable;

public interface ProcessFilter {

    interface Presenter {

        void loadTempImage(Context context);

        void loadImage(Context context, @FilterType int filterType, List<PageItem> pageItems);

        void onStartSave(Context context, DocumentItem documentItem, List<PageItem> listItem);
    }

    interface View {

        void onShowTempImage(List<PageItem> listItem);

        void onFilterResult(PageItem item);

        void onProcessDispose(Disposable disposable);

        void onFilterSuccess();

        void onSaveSuccess(int id);
    }

}
