package com.document.camerascanner.features.main.adapter;

import androidx.annotation.IntDef;

@IntDef({PresentType.LIST, PresentType.GRID})
public @interface PresentType {

    int LIST = 1;

    int GRID = 2;
}
