package com.document.camerascanner.databases.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import com.document.camerascanner.databases.model.DocumentItem;

import java.util.List;

import io.reactivex.Single;

@Dao
public interface DocumentDao extends BaseDao<DocumentItem> {

    @Query("SELECT * FROM tblDocument WHERE id =:id")
    Single<DocumentItem> getDocumentById(int id);

    @Query("SELECT * FROM tblDocument WHERE id =:id")
    DocumentItem getDocumentByIdNoRx(int id);

    @Query("SELECT * FROM tblDocument WHERE id != 1")
    List<DocumentItem> getListDocument();

    @Query("SELECT * FROM tblDocument WHERE id IN(:listId)")
    List<DocumentItem> getDocumentsByIds(List<Integer> listId);

    @Query("SELECT * FROM tblDocument WHERE id != 1 AND id !=:currentId ")
    Single<List<DocumentItem>> getTargetDocumentsRX(int currentId);

    @Query("SELECT * FROM tblDocument WHERE parent_id=1 AND id != 1")
    Single<List<DocumentItem>> getListDocumentRoot();

    @Query("SELECT * FROM tblDocument WHERE parent_id=:id")
    Single<List<DocumentItem>> getListDocumentByIdFolder(int id);

    @Query("SELECT EXISTS (SELECT * FROM tblDocument WHERE name =:name)")
    Single<Boolean> isExistsDocumentName(String name);

    @Query("SELECT EXISTS (SELECT * FROM tblDocument WHERE name =:name)")
    Boolean isExistsDocumentNameNoRx(String name);

    @Query("SELECT enchance_uri FROM tblPage WHERE parent_id =:idDocument AND positon = 1")
    String getThumbnailDocument(int idDocument);

    @Query("SELECT * FROM tblDocument WHERE parent_id=:id")
    List<DocumentItem> getListDocumentByIdFolderNoRx(int id);

    @Query("SELECT * FROM tblDocument WHERE parent_id IN(:idFolders)")
    List<DocumentItem> getDocumentsInIdFolders(List<Integer> idFolders);

    @Query("SELECT id FROM tblDocument WHERE parent_id IN (:idFolders)")
    List<Integer> getDocumentIdsByFolders(List<Integer> idFolders);

    @Query("UPDATE tblDocument SET child_count = 0 WHERE id=1")
    void resetTempDocument();

    @Query("UPDATE tblDocument SET child_count = (SELECT child_count FROM tblDocument WHERE id=1) WHERE id =:id")
    void initDocumentPageCount(int id);

    @Query("SELECT * FROM tblDocument WHERE parent_id =:folderId")
    LiveData<List<DocumentItem>> getListDoucumentbyFolderId(int folderId);

    @Query("DELETE FROM tblDocument WHERE id=:id")
    void deleteEntityByIdNoRx(int id);

    @Query("SELECT * FROM tblDocument WHERE id =:id")
    LiveData<DocumentItem> getPageParentById(int id);
}
