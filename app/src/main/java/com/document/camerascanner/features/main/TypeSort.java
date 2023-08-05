package com.document.camerascanner.features.main;

import androidx.annotation.IntDef;

@IntDef({TypeSort.SORT_NAME,
        TypeSort.SORT_DATA,
        TypeSort.SORT_DATE})
public @interface TypeSort {

    int SORT_NAME = 1;

    int SORT_DATA = 2;

    int SORT_DATE = 3;
}
