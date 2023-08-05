package com.document.camerascanner.features.main.interfaces;

import android.content.Context;
import android.content.Intent;

import com.document.camerascanner.databases.model.BaseEntity;
import com.document.camerascanner.databases.model.DocumentItem;
import com.document.camerascanner.databases.model.FolderItem;

import java.util.List;

public interface MPresenter {

    void getNewFolderDefault(Context context, boolean isMoveDocument);

    void createNewFolder(String folderName, boolean isMoveDocument);

    void listSortAll(List<FolderItem> listFolder, List<DocumentItem> listDocument, int typeSort);

    void deleteFile();

    void resetHmFolderSelect();

    void resetHmDocumentSelect();

    void mergeFile(String name);

    void mergeDocument(DocumentItem documentGoal);

    void mergeFolder(FolderItem folderItemGoal);

    void moveFiles(BaseEntity baseEntity);

    void getListFolderDB();

    void getListDocumentDB();

    void getListFileFromImport(Intent data);

    void getTitleSelect();

    void searchFile(List<FolderItem> listFolder, List<DocumentItem> listDocument, CharSequence charSequence);

    void setInfoMerge();

    void renameFolder(BaseEntity baseEntity);

    void setItemToHmDocumentItem(DocumentItem documentItem);

    void setListItemToHmDocumentItem(List<DocumentItem> documentItems);

    void setItemToHmFolderItem(FolderItem folderItem);

    void setListItemToHmFolderItem(List<FolderItem> folderItems);

    void startCamera();

    void savePdf(String title, int quality);

    void saveJpg(String title, int quality);

    void sharePdf(String title, int quality);

    void shareJpg(String title);

    void openPdfSettings();

    void setDisposableAll();
}
