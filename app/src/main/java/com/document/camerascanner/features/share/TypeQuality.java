package com.document.camerascanner.features.share;


import androidx.annotation.IntDef;

@IntDef({TypeQuality.LOW,
        TypeQuality.MEDIUM,
        TypeQuality.HIGH,
        TypeQuality.MAX})
public @interface TypeQuality {

    int LOW = 30;

    int MEDIUM = 50;

    int HIGH = 70;

    int MAX = 100;

}
