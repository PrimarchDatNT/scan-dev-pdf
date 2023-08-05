package com.document.camerascanner.features.settings;

import androidx.annotation.IntDef;

@IntDef({DefaultFilterOpt.ORIGNAL,
        DefaultFilterOpt.GREY_SCALE,
        DefaultFilterOpt.MAGIC_COLOR,
        DefaultFilterOpt.BLACK_AND_WHITE,
        DefaultFilterOpt.BLACK_AND_WHITE_2,
        DefaultFilterOpt.NO_SHADOW,
        DefaultFilterOpt.LAST_USE_FILTER})
public @interface DefaultFilterOpt {
    int ORIGNAL = 0;
    int GREY_SCALE = 1;
    int MAGIC_COLOR = 2;
    int BLACK_AND_WHITE = 3;
    int BLACK_AND_WHITE_2 = 4;
    int NO_SHADOW = 5;
    int LAST_USE_FILTER = 6;
}
