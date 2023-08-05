package com.document.camerascanner.features.enhance;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import com.document.camerascanner.databases.model.DocumentItem;
import com.document.camerascanner.databases.model.PageItem;

import io.reactivex.disposables.Disposable;

public interface Enhance {

    interface Presenter {

        void loadEnhanceImage(Context context, Uri uri);

        void applyFilter(int type, Bitmap bitmap);

        void saveEnhace(Context context, boolean isFromDetailDoc, PageItem pageItem, Bitmap bitmap);
    }

    interface View {

        void onLoadImageResult(Bitmap bitmap);

        void onFilterResult(Bitmap bitmap);

        void onEnhanceDispose(Disposable disposable);

        void onSaveError();

        void onSaveEnhanceSucces();

        void onDocumentSucces(DocumentItem documentItem);
    }
}
