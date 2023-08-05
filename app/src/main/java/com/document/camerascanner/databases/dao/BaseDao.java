package com.document.camerascanner.databases.dao;


import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Update;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;

public interface BaseDao<T> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Long insertEntityNoRx(T entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Single<Long> insertEntity(T entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertListEntity(List<T> listEntity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertListEntityNoRx(List<T> listEntity);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateEntityNoRx(T entity);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    Completable updateEntity(T entity);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    Completable updateListEntity(List<T> listEntity);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateListEntityNoRx(List<T> listEntity);

    @Delete
    void deleteEntityNoRx(T entity);

    @Delete
    Completable deleteEntity(T entity);

    @Delete
    Completable deleteListEntity(List<T> listEntity);

    @Delete
    void deleteListEntityNoRx(List<T> listEntity);


}
