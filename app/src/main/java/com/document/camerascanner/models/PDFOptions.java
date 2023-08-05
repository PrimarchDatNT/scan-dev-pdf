package com.document.camerascanner.models;

import com.itextpdf.text.PageSize;
import com.itextpdf.text.Rectangle;

public class PDFOptions {

    private boolean isMargin;
    private boolean isPortrait;
    private boolean isPageNumber;

    private String fileName;
    private Rectangle pageSize;

    public PDFOptions() {
        this.pageSize = PageSize.A4;
        this.isMargin = false;
        this.isPageNumber = true;
        this.isPortrait = true;
    }

    public String getFileName() {
        return this.fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Rectangle getPageSize() {
        return this.pageSize;
    }

    public void setPageSize(Rectangle pageSize) {
        this.pageSize = pageSize;
    }

    public boolean isMargin() {
        return this.isMargin;
    }

    public void setMargin(boolean margin) {
        this.isMargin = margin;
    }

    public boolean isPageNumber() {
        return this.isPageNumber;
    }

    public void setPageNumber(boolean pageNumber) {
        this.isPageNumber = pageNumber;
    }

    public boolean isPortrait() {
        return isPortrait;
    }

    public void setPortrait(boolean portrait) {
        this.isPortrait = portrait;
    }

}
