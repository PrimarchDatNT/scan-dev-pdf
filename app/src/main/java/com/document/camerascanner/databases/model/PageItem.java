package com.document.camerascanner.databases.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;

import org.jetbrains.annotations.NotNull;

@Entity(tableName = "tblPage", foreignKeys = @ForeignKey(entity = DocumentItem.class, parentColumns = "id", childColumns = "parent_id", onDelete = ForeignKey.CASCADE))
public class PageItem extends BaseEntity {

    @ColumnInfo(name = "orginal_uri")
    private String orgUri;

    @ColumnInfo(name = "enchance_uri")
    private String enhanceUri;

    @ColumnInfo(name = "cache_vertex")
    private String cacheVertex;

    @ColumnInfo(name = "positon")
    private int position;

    @Ignore
    private boolean isLoading;

    public String getOrgUri() {
        return this.orgUri;
    }

    public void setOrgUri(String orgUri) {
        this.orgUri = orgUri;
    }

    public String getEnhanceUri() {
        return this.enhanceUri;
    }

    public void setEnhanceUri(String enhanceUri) {
        this.enhanceUri = enhanceUri;
    }

    public String getCacheVertex() {
        return this.cacheVertex;
    }

    public void setCacheVertex(String cacheVertex) {
        this.cacheVertex = cacheVertex;
    }

    public int getPosition() {
        return this.position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public boolean isLoading() {
        return this.isLoading;
    }

    public void setLoading(boolean loading) {
        this.isLoading = loading;
    }

    @Override
    public @NotNull String toString() {
        return "PageItem{" +
                "orgUri='" + orgUri + '\'' +
                ", enhanceUri='" + enhanceUri + '\'' +
                ", cacheVertex='" + cacheVertex + '\'' +
                ", position=" + position +
                ", isLoading=" + isLoading +
                ", id=" + id +
                ", parentId=" + parentId +
                ", size=" + size +
                ", createdTime=" + createdTime +
                ", name='" + name + '\'' +
                ", isSelected=" + isSelected +
                '}';
    }
}
