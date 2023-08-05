package com.document.camerascanner.databases.dao;

import androidx.room.Dao;
import androidx.room.Query;

import com.document.camerascanner.databases.model.FolderItem;
import com.document.camerascanner.databases.model.PageItem;

import java.util.List;

import io.reactivex.Single;

@Dao
public interface FolderDao extends BaseDao<FolderItem> {

    @Query("SELECT * FROM tblFolder WHERE id != 1")
    Single<List<FolderItem>> getListFolder();

    @Query("SELECT * FROM tblFolder WHERE id =:id")
    Single<FolderItem> getFolderInfo(int id);

    @Query("SELECT * FROM tblFolder WHERE id!=1")
    List<FolderItem> getListFolderNoRx();

    @Query("SELECT EXISTS (SELECT * FROM tblFolder WHERE name =:name)")
    Single<Boolean> isExistsFolderName(String name);

    @Query("SELECT EXISTS (SELECT * FROM tblFolder WHERE name =:name)")
    Boolean isExistsFolderNameNoRx(String name);

    @Query("DELETE FROM tblFolder WHERE id=:id")
    void deleteFolderByIdNoRx(int id);

    @Query("DELETE FROM tblFolder WHERE id IN(:listId)")
    void deleteFoldersByIdNoRx(List<Integer> listId);

    @Query("SELECT * FROM tblFolder WHERE id=:folderId")
    FolderItem getFolderItemById(int folderId);

    @Query("SELECT * FROM tblPage\n" +
            "INNER JOIN tblFolder\n" +
            "INNER JOIN tblDocument ON tblDocument.parent_id = tblFolder.id\n" +
            "WHERE tblFolder.id IN(:listId) AND tblPage.parent_id= tblDocument.id;")
    List<PageItem> getAllPageByFolder(List<Integer> listId);


}
