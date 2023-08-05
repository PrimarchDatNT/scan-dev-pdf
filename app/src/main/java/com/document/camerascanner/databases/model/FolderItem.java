package com.document.camerascanner.databases.model;


import androidx.room.ColumnInfo;
import androidx.room.Entity;

@Entity(tableName = "tblFolder")
public class FolderItem extends BaseEntity {

    @ColumnInfo(name = "child_count")
    private int childCount;

    public FolderItem() {
        this.createdTime = System.currentTimeMillis();
    }

    public int getChildCount() {
        return this.childCount;
    }

    public void setChildCount(int childCount) {
        this.childCount = childCount;
    }
}
