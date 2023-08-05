package com.document.camerascanner.features.detailshow.documents.interfaces;

import android.content.Context;
import android.content.Intent;

import com.document.camerascanner.databases.model.DocumentItem;
import com.document.camerascanner.databases.model.PageItem;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import io.reactivex.disposables.Disposable;

public interface DetailDocument {

    interface Presenter {

        void addDisposable(Disposable disposable);

        void copyPages(Context context, List<PageItem> listCopy);

        void getListTargetDocument(Context context, int currentId);

        void generalSharePdf(Context context, List<PageItem> list, List<Integer> listSelected, DocumentItem documentItem, boolean isPdfAll);

        void shareToGallery(Context context, String title, int quality, DocumentItem documentItem, List<PageItem> list);

        void shareToJpgShare(Context context, @NotNull List<PageItem> list);

        void calculatorSize(Context context, List<PageItem> list);

        void insertTempPage(Context context, Intent data);

        void createDocumentForCopyAndMove(Context context, String nameDocument);

        void saveAndSharePdf(Context context, String title, int quality, List<PageItem> list, List<Integer> listSelected, DocumentItem documentItem, boolean isPdfAll, boolean isSave);

        void disposableAll();
    }

    interface View {

        void onShowTargetDocuments(List<DocumentItem> listDocument);

        void onCopyImageSucces(List<PageItem> listPage);

        void onSucessCreateDocument(DocumentItem documentItem);

        void onSuccessShareToGallery(List<String> listFile);

        void onUpdateSizeFileSelect(String size);

        void onSingleImportSuccess(PageItem pageItem);

        void onMultipleImportSuccess();

        void onSuccessSavePdf(List<String> listPdf);

        void onSuccessSharePdf();

    }
}
