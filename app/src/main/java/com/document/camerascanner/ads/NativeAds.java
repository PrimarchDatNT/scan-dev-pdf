package com.document.camerascanner.ads;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.document.camerascanner.utils.AppUtils;
import com.google.ads.mediation.facebook.FacebookAdapter;
import com.google.ads.mediation.facebook.FacebookExtras;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;

import java.util.ArrayList;
import java.util.List;

public class NativeAds extends AbstractAd<NativeAds.NativeListener> {

    private CallBackNativeAd callBack;
    private List<Object> nativeAdList;

    public NativeAds(Context context, String idAd, CallBackNativeAd callBack, String adsName) {
        super(context, idAd, adsName);
        this.callBack = callBack;
    }

    public void loadAds() {
        if (AppUtils.isConnectingInternet(this.getContext())) {
            if (!UtmSourceChecker.isAllowShowAd(this.getContext())) {
                return;
            }

            super.loadAds();
            this.nativeAdList = new ArrayList<>();

            Bundle extras = new FacebookExtras()
                    .setNativeBanner(true)
                    .build();

            AdLoader adLoader = new AdLoader.Builder(this.getContext(), this.adUnit)
                    .withAdListener(this.listener)
                    .forUnifiedNativeAd(ad -> this.nativeAdList.add(ad)).build();

            this.adRequest = new AdRequest.Builder()
                    .addNetworkExtrasBundle(FacebookAdapter.class, extras)
                    .build();

            adLoader.loadAd(this.adRequest);
        }
    }

    @Override
    protected String getAdsType() {
        return AdsType.NATIVE_ADS;
    }

    @Override
    protected NativeListener initCallback() {
        return new NativeListener();
    }

    public interface CallBackNativeAd {

        void onLoaded(List<Object> unifiedNativeAd);
    }

    public class NativeListener extends AbstractAdListener<NativeAds> {

        public NativeListener() {
            super(NativeAds.this);
        }

        @Override
        public void onAdLoaded() {
            super.onAdLoaded();
            if (callBack != null) {
                callBack.onLoaded(nativeAdList);
            }
        }

        @Override
        public void onAdFailedToLoad(@NonNull LoadAdError errors) {
            super.onAdFailedToLoad(errors);
        }
    }

}
