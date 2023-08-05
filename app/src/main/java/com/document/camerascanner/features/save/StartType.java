package com.document.camerascanner.features.save;

import androidx.annotation.IntDef;

@IntDef({StartType.START_FROM_MAIN,
        StartType.START_FROM_DETAIL_DOC,
        StartType.START_FROM_DETAIL_FOLDER})
public @interface StartType {
    int START_FROM_MAIN = 1;

    int START_FROM_DETAIL_DOC = 2;

    int START_FROM_DETAIL_FOLDER = 3;
}
