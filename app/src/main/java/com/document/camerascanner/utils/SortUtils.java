package com.document.camerascanner.utils;

import com.document.camerascanner.databases.model.BaseEntity;
import com.document.camerascanner.databases.model.PageItem;
import com.document.camerascanner.features.detect.Quadrilateral;

import org.opencv.core.Point;

import java.util.Comparator;

public class SortUtils {

    public static Comparator<BaseEntity> sortByDateCreated = (left, right) -> {
        int dataLeft = (int) left.getCreatedTime();
        int dataRight = (int) right.getCreatedTime();
        return dataRight - dataLeft;
    };

    public static Comparator<BaseEntity> sortByName = (left, right) -> {
        String nameLeft = left.getName();
        String nameRight = right.getName();

        nameLeft = VNCharacterUtils.removeAccent(nameLeft.toLowerCase()).replace(" ", "");
        nameRight = VNCharacterUtils.removeAccent(nameRight.toLowerCase()).replace(" ", "");
        return nameLeft.compareTo(nameRight);
    };

    public static Comparator<BaseEntity> sortByData = (left, right) -> {
        int dataLeft = (int) left.getSize();
        int dataRight = (int) right.getSize();
        return dataRight - dataLeft;
    };

    public static Comparator<PageItem> sortByPosition = (left, right) -> {
        int dataLeft = left.getPosition();
        int dataRight = right.getPosition();
        return dataLeft - dataRight;
    };

    public static Comparator<PageItem> sortByParrentId = (left, right) -> {
        int dataLeft = left.getParentId();
        int dataRight = right.getParentId();
        return dataLeft - dataRight;
    };

    public static Comparator<Point> sortByYAxis = (left, right) -> {
        int dataLeft = (int) left.y;
        int dataRight = (int) right.y;
        return dataLeft - dataRight;
    };

    public static Comparator<Point> sortByXAxis = (left, right) -> {
        int dataLeft = (int) left.x;
        int dataRight = (int) right.x;
        return dataLeft - dataRight;
    };


    public static Comparator<Quadrilateral> sortByConfident = (left, right) -> {
        int dataLeft = (int) left.getConfident();
        int dataRight = (int) right.getConfident();
        return dataLeft - dataRight;
    };


}
