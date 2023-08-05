package com.document.camerascanner.ads;

import android.content.Context;

import com.google.android.gms.ads.InterstitialAd;

public class InterAds extends AbstractAd<InterAds.InterAdsListener> {

    private boolean isShowed = false;

    private InterstitialAd interstitialAds;

    public InterAds(Context context, String adUnit, String adsName) {
        super(context, adUnit, adsName);
    }

    @Override
    protected String getAdsType() {
        return AdsType.INTER_ADS;
    }

    public boolean isShowed() {
        return this.isShowed;
    }

    public boolean isLoaded() {
        return this.interstitialAds != null && this.interstitialAds.isLoaded();
    }

    public void showAds() {
        if (this.isLoaded()) {
            this.interstitialAds.show();
        }
    }

    @Override
    public void loadAds() {
        if (!UtmSourceChecker.isAllowShowAd(this.mCtx.get())) {
            return;
        }

        this.isShowed = false;
        super.loadAds();
        this.initInterAds();
        this.interstitialAds.loadAd(this.adRequest);
    }

    private void initInterAds() {
        if (this.interstitialAds != null) {
            return;
        }

        this.interstitialAds = new InterstitialAd(this.getContext());
        this.interstitialAds.setAdUnitId(this.adUnit);
        this.interstitialAds.setAdListener(this.listener);
    }

    public void setListener(InterAdsListener listener) {
        this.listener = listener;
    }

    @Override
    protected InterAdsListener initCallback() {
        return new InterAdsListener(this);
    }

    public static class InterAdsListener extends AbstractAdListener<InterAds> {

        public InterAdsListener(InterAds mAds) {
            super(mAds);
        }

        @Override
        public void onAdLoaded() {
            super.onAdLoaded();
            mAds.isShowed = true;
        }
    }
}
