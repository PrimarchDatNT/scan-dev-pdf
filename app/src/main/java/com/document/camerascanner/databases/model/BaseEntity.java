package com.document.camerascanner.databases.model;

import androidx.room.ColumnInfo;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public class BaseEntity implements Serializable {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    protected int id;

    @ColumnInfo(name = "parent_id")
    protected int parentId;

    @ColumnInfo(name = "size")
    protected long size;

    @ColumnInfo(name = "create_time")
    protected long createdTime;

    @ColumnInfo(name = "name")
    protected String name;

    @Ignore
    protected boolean isSelected;

    public boolean isSelected() {
        return this.isSelected;
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getParentId() {
        return this.parentId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    public long getSize() {
        return this.size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getCreatedTime() {
        return this.createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public @NotNull String toString() {
        return "BaseEntity{" +
                "id=" + id +
                ", parentId=" + parentId +
                ", size=" + size +
                ", createdTime=" + createdTime +
                ", name='" + name + '\'' +
                ", isSelected=" + isSelected +
                '}';
    }

}
