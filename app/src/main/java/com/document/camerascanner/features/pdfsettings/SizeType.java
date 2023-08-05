package com.document.camerascanner.features.pdfsettings;

import androidx.annotation.IntDef;

@IntDef({SizeType.LETTER,
        SizeType.A4,
        SizeType.LEGAL,
        SizeType.A3,
        SizeType.A5,
        SizeType.BUSINESS_CARD})
public @interface SizeType {
    int LETTER = 1;
    int A4 = 2;
    int LEGAL = 3;
    int A3 = 4;
    int A5 = 5;
    int BUSINESS_CARD = 6;
}
