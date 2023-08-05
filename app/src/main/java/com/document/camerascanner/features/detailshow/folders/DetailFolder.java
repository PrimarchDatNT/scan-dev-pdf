package com.document.camerascanner.features.detailshow.folders;


import android.content.Context;
import android.content.Intent;

import com.document.camerascanner.databases.model.DocumentItem;
import com.document.camerascanner.databases.model.FolderItem;

import java.util.List;

public interface DetailFolder {

    interface View {

        void onSuccessNameFolderDefault(String folderName);

        void onGetFolderInfoSuccess(FolderItem folderItem);

        void onShowListDocumentInFolder(List<DocumentItem> listDocuments);

        void onShowSuccessDelete();

        void onShowDialogMove(List<FolderItem> list);

        void onSuccessMove();

        void onShowSize(String size);

        void onShowRenameFolder(FolderItem folderItem);

        void onShowRenameDocument();

        void onShowDialogMerge(String title, String pathRepresent);

        void onShowSuccessMerge();

        void onSuccessSavePdf(List<String> listPdf);

        void onSuccessSharePdf();

        void onShowOnSuccessToGallery(List<String> listJpg);

        void onSuccessImport();

    }

    interface Presenter {

        void getFolderNameDefault(Context context);

        void getFolderInfo(Context context, int id);

        void getFolderChildren(Context context, FolderItem folderItem);

        void resetHmDocumentSelect();

        void setListHmDocumentSelect(List<DocumentItem> documentSelect);

        void setItemToHmDocumentItem(DocumentItem documentItem);

        void deleteDocuments(Context context);

        void moveDocuments(Context context, FolderItem folderItemGoal);

        void moveOutDocuments(Context context);

        void showDialogMove(Context context, FolderItem folderItemRoot);

        void showDialogMerge(Context context);

        void generalMergeDocuments(Context context, String nameDocument, int folderId);

        void mergeDocuments(Context context, DocumentItem documentItemGoal);

        void calculatorSize(Context context);

        void sharePdf(Context context);

        void shareJpgGallery(Context context, String title, int quality);

        void shareJpg(Context context);

        void renameFolder(Context context, FolderItem folderItem, String newName);

        void renameDocument(Context context, String newName, DocumentItem documentItem);

        void startCamera(Context context, FolderItem folderItem);

        void getDataImportImage(Context context, Intent data, FolderItem folderItem);

        void newFolder(Context context, String newName);

        void sortDocument(Context context, List<DocumentItem> documentItems, int typeSort);

        void onGeneralSaveAndSharePdf(Context context, String title, int quality, boolean isSave);

        void disposableAll();
    }


}
