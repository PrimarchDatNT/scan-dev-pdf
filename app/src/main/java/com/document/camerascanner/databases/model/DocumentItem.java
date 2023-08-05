package com.document.camerascanner.databases.model;


import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;

import org.jetbrains.annotations.NotNull;

import java.util.List;


@Entity(tableName = "tblDocument", foreignKeys = @ForeignKey(entity = FolderItem.class, parentColumns = "id", childColumns = "parent_id", onDelete = ForeignKey.CASCADE))
public class DocumentItem extends BaseEntity {
    @ColumnInfo(name = "child_count")
    private int childCount;

    @Ignore
    private String thumbnail;

    @Ignore
    private List<PageItem> listPage;

    @Ignore
    private int position;

    public DocumentItem() {
        this.createdTime = System.currentTimeMillis();
    }

    public int getChildCount() {
        return this.childCount;
    }

    public void setChildCount(int childCount) {
        this.childCount = childCount;
    }

    public String getThumbnail() {
        return this.thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public int getPosition() {
        return this.position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    @Override
    public @NotNull String toString() {
        return "DocumentItem{" +
                "childCount=" + childCount +
                ", thumbnail='" + thumbnail + '\'' +
                ", position=" + position +
                ", id=" + id +
                ", parentId=" + parentId +
                ", size=" + size +
                ", createdTime=" + createdTime +
                ", name='" + name + '\'' +
                ", isSelected=" + isSelected +
                '}';
    }

    public List<PageItem> getListPage() {
        return this.listPage;
    }

    public void setListPage(List<PageItem> listPage) {
        this.listPage = listPage;
    }
}
