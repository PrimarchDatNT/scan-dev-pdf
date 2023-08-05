package com.document.camerascanner.ads;


import androidx.annotation.StringDef;

@StringDef({AdsType.NATIVE_ADS, AdsType.INTER_ADS, AdsType.BANNER_ADS})
public @interface AdsType {
    String NATIVE_ADS = "nt";

    String INTER_ADS = "it";

    String BANNER_ADS = "bn";
}
