package com.document.camerascanner.features.main.interfaces;

import com.document.camerascanner.databases.model.DocumentItem;
import com.document.camerascanner.databases.model.FolderItem;
import com.document.camerascanner.databases.model.PageItem;

import java.util.List;

public interface MainView {

    void onShowNameFolder(String nameFolder, boolean isMoveDocument);

    void onErrorCreatedFolder();

    void onSuccessCreatedFolder(FolderItem folderItem, boolean isMoveDocument);

    void onDeleteSuccess();

    void showListFolder(List<FolderItem> list);

    void showListDocument(List<DocumentItem> list);

    void onSingleImportSuccess(PageItem pageItem);

    void onMultipleImportSuccess();

    void onErrorImport();

    void resetQuickAction();

    void showDialogSendTo(String sizeList);

    void showListSearch(List<FolderItem> listFolder, List<DocumentItem> listDocument);

    void showListDefault();

    void showDialogMerge(String title, String pathRepresent);

    void onSuccessRenameFolder();

    void onSuccessSaveGallery(List<String> filesJpg);

    void onShowFolderSort(List<FolderItem> listFolder);

    void onSuccessSavePdf(List<String> filesPdf);

    void onShowDocumentsSort(List<DocumentItem> listDocument);

}
