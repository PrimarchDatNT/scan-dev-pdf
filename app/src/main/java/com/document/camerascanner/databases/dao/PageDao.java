package com.document.camerascanner.databases.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import com.document.camerascanner.databases.model.PageItem;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;

@Dao
public interface PageDao extends BaseDao<PageItem> {

    @Query("UPDATE tblPage SET enchance_uri=:enhanceUri WHERE id=:idPage")
    Completable updateEnhancePageItem(int idPage, String enhanceUri);

    @Query("SELECT * FROM tblPage WHERE parent_id=:idDocument")
    List<PageItem> getListPageItemByIdDocumentNoRx(int idDocument);

    @Query("SELECT * FROM tblPage WHERE parent_id==1")
    Single<List<PageItem>> getListTempPage();

    @Query("SELECT * FROM tblPage WHERE id=:idPage")
    PageItem getPageItemByIdNoRx(int idPage);

    @Query("SELECT * FROM tblPage WHERE parent_id IN (:listId)")
    List<PageItem> getListPageByDocumentIdNoRx(List<Integer> listId);

    @Query("SELECT * FROM tblPage WHERE id IN(:listId)")
    List<PageItem> getListPageByListIdNoRx(List<Integer> listId);

    @Query("SELECT * FROM tblPage WHERE parent_id =:id ORDER BY positon")
    List<PageItem> getListPageByDocumentIdNoRx(int id);

    @Query("SELECT * FROM tblpage WHERE parent_id ==1")
    List<PageItem> getPreviousTempPage();

    @Query("DELETE FROM tblPage WHERE parent_id==1")
    void deleteTempPage();

    @Query("SELECT * FROM tblPage WHERE parent_id =:folderId ORDER BY positon")
    LiveData<List<PageItem>> getListPagebyDocumentId(int folderId);

    @Query("SELECT enchance_uri FROM tblPage\n" +
            "INNER JOIN tblDocument \n" +
            "WHERE tblDocument.id IN(:listId) AND tblPage.parent_id= tblDocument.id;")
    List<String> getAllShareUriByIdDocuments(List<Integer> listId);

}
