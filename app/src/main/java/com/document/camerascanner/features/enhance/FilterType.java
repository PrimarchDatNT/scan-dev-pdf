package com.document.camerascanner.features.enhance;

import androidx.annotation.IntDef;

@IntDef({FilterType.ORIGNAL,
        FilterType.GREY_SCALE,
        FilterType.MAGIC_COLOR,
        FilterType.BLACK_AND_WHITE,
        FilterType.BLACK_AND_WHITE_2,
        FilterType.NO_SHADOW})
public @interface FilterType {
    int ORIGNAL = 0;
    int GREY_SCALE = 1;
    int MAGIC_COLOR = 2;
    int BLACK_AND_WHITE = 3;
    int BLACK_AND_WHITE_2 = 4;
    int NO_SHADOW = 5;
}
