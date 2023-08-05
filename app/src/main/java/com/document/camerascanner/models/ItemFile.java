package com.document.camerascanner.models;

import androidx.annotation.NonNull;

import com.document.camerascanner.utils.VNCharacterUtils;

import java.io.File;
import java.util.Comparator;
import java.util.List;

public class ItemFile {

    private boolean isFolder;
    private boolean isSelect;
    private boolean isDocument;
    private int childCount = 0;
    private int position;
    private long sizeItem;
    private long latModified;

    private String fileName;
    private String pathItem;
    private String pathSource;
    private List<String> urisChild;


    public ItemFile() {
    }

    public ItemFile(String fileName, long latModified, int childCount, boolean isFolder, String pathItem, long sizeItem, String pathSource) {
        this.fileName = fileName;
        this.latModified = latModified;
        this.childCount = childCount;
        this.isFolder = isFolder;
        this.pathItem = pathItem;
        this.sizeItem = sizeItem;
        this.pathSource = pathSource;
    }

    public String getFileName() {
        return this.fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getLatModified() {
        return this.latModified;
    }

    public void setLatModified(long latModified) {
        this.latModified = latModified;
    }

    public int getChildCount() {
        return this.childCount;
    }

    public void setChildCount(int childCount) {
        this.childCount = childCount;
    }

    public boolean isFolder() {
        return this.isFolder;
    }

    public void setFolder(boolean folder) {
        this.isFolder = folder;
    }

    public String getPathItem() {
        return this.pathItem;
    }

    public void setPathItem(String pathItem) {
        this.pathItem = pathItem;
    }

    public long getSizeItem() {
        return this.sizeItem;
    }

    public void setSizeItem(long sizeItem) {
        this.sizeItem = sizeItem;
    }

    public boolean isSelect() {
        return this.isSelect;
    }

    public void setSelect(boolean select) {
        this.isSelect = select;
    }

    public String getPathSource() {
        return this.pathSource;
    }

    public void setPathSource(String pathSource) {
        this.pathSource = pathSource;
    }

    public boolean isDocument() {
        return this.isDocument;
    }

    public void setDocument(boolean document) {
        this.isDocument = document;
    }

    public List<String> getUrisChild() {
        return this.urisChild;
    }

    public void setUrisChild(List<String> urisChild) {
        this.urisChild = urisChild;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    @NonNull
    @Override
    public String toString() {
        return "ItemFile{" +
                "isFolder=" + isFolder +
                ", isSelect=" + isSelect +
                ", isDocument=" + isDocument +
                ", childCount=" + childCount +
                ", position=" + position +
                ", sizeItem=" + sizeItem +
                ", latModified=" + latModified +
                ", fileName='" + fileName + '\'' +
                ", pathItem='" + pathItem + '\'' +
                ", pathSource='" + pathSource + '\'' +
                ", urisChild=" + urisChild +
                '}';
    }

    public void setInfoForDocument(String pathSource, File file) {
        this.setFileName(file.getName());
        this.setSelect(false);
        this.setDocument(true);
        this.setChildCount(0);
        this.setPathItem(file.getPath());
        this.setPathSource(pathSource);
        this.setLatModified(file.lastModified());
        this.setSizeItem(file.length());
    }

    public static Comparator<ItemFile> nameSort = (left, right) -> {
        String fileNameLeft = VNCharacterUtils.removeAccent(left.getFileName());
        fileNameLeft = fileNameLeft.replace(" ", "").toLowerCase();
        String fileNameRight = VNCharacterUtils.removeAccent(right.getFileName());
        fileNameRight = fileNameRight.replace(" ", "").toLowerCase();
        return fileNameLeft.compareTo(fileNameRight);
    };

    public static Comparator<ItemFile> dataSizeSort = (left, right) -> {
        long dataSizeLeft = left.getSizeItem();
        long dataSizeRight = right.getSizeItem();
        int dataSizeKbLeft = (int) (dataSizeLeft);
        int dataSizeKbRight = (int) (dataSizeRight);
        if (dataSizeKbLeft == dataSizeKbRight) {
            return right.getChildCount() - left.getChildCount();
        }
        return dataSizeKbLeft - dataSizeKbRight;
    };

    public static Comparator<ItemFile> dataSizeSortFolder = (left, right) -> {
        File fileLeft = new File(left.getPathItem());
        File fileRight = new File(right.getPathItem());
        File[] listChildLeft = fileLeft.listFiles();
        File[] listChildRight = fileRight.listFiles();
        long sizeLeft = fileLeft.length();
        long sizeRight = fileRight.length();
        if (listChildLeft != null) {
            for (File file : listChildLeft) {
                File[] child = file.listFiles();
                if (child != null) {
                    for (File file1 : child) {
                        sizeLeft += file1.length();
                    }
                }
                sizeLeft += file.length();
            }
        }

        if (listChildRight != null) {
            for (File file : listChildRight) {
                File[] child = file.listFiles();
                if (child != null) {
                    for (File file1 : child) {
                        sizeRight += file1.length();
                    }
                }
                sizeRight += file.length();
            }
        }

        return (int) (sizeRight - sizeLeft);
    };

    public static Comparator<ItemFile> dateSort = (left, right) -> {
        int dateLeft = (int) (left.getLatModified());
        int dateRight = (int) (right.getLatModified());
        return dateLeft - dateRight;
    };

    public static Comparator<ItemFile> dataSortDown = (left, right) -> {
        int dataLeft = (int) (left.getLatModified());
        int dataRight = (int) (right.getLatModified());
        return dataRight - dataLeft;
    };

    public static Comparator<ItemFile> createdSort = (left, right) -> {
        try {
            String nameFileLeft = left.getFileName();
            String timeCreatedLeft = nameFileLeft.split("_")[1];
            boolean isConInvaliLeft = timeCreatedLeft.contains("(");
            if (isConInvaliLeft) {
                timeCreatedLeft = timeCreatedLeft.split("\\(")[0];
            } else {
                timeCreatedLeft = timeCreatedLeft.split("\\.")[0];
            }
            long createLeft = Long.parseLong(timeCreatedLeft);

            String nameFileRight = right.getFileName();
            String timeCreatedRight = nameFileRight.split("_")[1];
            boolean isContaiInvaliRight = timeCreatedRight.contains("(");
            if (isContaiInvaliRight) {
                timeCreatedRight = timeCreatedRight.split("\\(")[0];
            } else {
                timeCreatedRight = timeCreatedRight.split("\\.")[0];
            }
            long createRight = Long.parseLong(timeCreatedRight);

            int comparLeft = (int) (createLeft);
            int comparRight = (int) (createRight);

            return comparLeft - comparRight;
        } catch (Exception e) {
            e.printStackTrace();
        }

        //default
        int dateLeft = (int) (left.getLatModified());
        int dateRight = (int) (right.getLatModified());
        return dateLeft - dateRight;
//        int dateLeft = (int) (left.getLatModified());
//        int dateRight = (int) (right.getLatModified());
//        return dateLeft - dateRight;

    };

    public static Comparator<String> createSortWithPath = (left, right) -> {
        File fileLeft = new File(left);
        File fileRight = new File(right);
        try {
            String nameFileLeft = fileLeft.getName();
            String timeCreatedLeft = nameFileLeft.split("_")[1];
            boolean isConInvaliLeft = timeCreatedLeft.contains("(");
            if (isConInvaliLeft) {
                timeCreatedLeft = timeCreatedLeft.split("\\(")[0];
            } else {
                timeCreatedLeft = timeCreatedLeft.split("\\.")[0];
            }
            long createLeft = Long.parseLong(timeCreatedLeft);

            String nameFileRight = fileRight.getName();
            String timeCreatedRight = nameFileRight.split("_")[1];
            boolean isContaiInvaliRight = timeCreatedRight.contains("(");
            if (isContaiInvaliRight) {
                timeCreatedRight = timeCreatedRight.split("\\(")[0];
            } else {
                timeCreatedRight = timeCreatedRight.split("\\.")[0];
            }
            long createRight = Long.parseLong(timeCreatedRight);

            int comparLeft = (int) (createLeft);
            int comparRight = (int) (createRight);

            return comparLeft - comparRight;
        } catch (Exception e) {
            e.printStackTrace();
        }


        //default
        return 1;
//        File fileLeft = new File(left);
//        File fileRight = new File(right);
//        int timeCreatedLeft = (int) fileLeft.lastModified();
//        int timeCreatedRight = (int) fileRight.lastModified();
//        return timeCreatedLeft - timeCreatedRight;
    };
}
