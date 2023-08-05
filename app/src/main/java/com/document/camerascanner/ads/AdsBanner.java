package com.document.camerascanner.ads;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.document.camerascanner.utils.AppUtils;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;

public class AdsBanner extends AbstractAd<AdsBanner.BannerCallback> {

    private AdView adView;
    private ViewGroup layoutContainer;

    public AdsBanner(Context context, String idAd, String adsName, ViewGroup container) {
        super(context, idAd, adsName);
        this.layoutContainer = container;
        this.loadAds();
    }

    @Override
    public void loadAds() {
        if (!AppUtils.isConnectingInternet(this.getContext())) {
            return;
        }

        if (!UtmSourceChecker.isAllowShowAd(this.getContext())) {
            return;
        }

        super.loadAds();
        this.layoutContainer.post(this::loadBanner);
    }

    private void loadBanner() {
        this.adView = new AdView(getContext());
        this.adView.setAdUnitId(this.adUnit);
        this.adView.setAdListener(this.listener);
        this.layoutContainer.removeAllViews();
        this.layoutContainer.addView(this.adView);

        AdSize adSize = this.getAdSize();
        this.adView.setAdSize(adSize);
        if (this.adRequest != null) {
            this.adView.loadAd(this.adRequest);
        }
    }

    private AdSize getAdSize() {
        if (!(this.getContext() instanceof Activity)) {
            return AdSize.SMART_BANNER;
        }

        Display display = ((Activity) this.getContext()).getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float density = outMetrics.density;
        float adWidthPixels = this.layoutContainer.getWidth();
        if (adWidthPixels == 0) {
            adWidthPixels = outMetrics.widthPixels;
        }

        int adWidth = (int) (adWidthPixels / density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this.getContext(), adWidth);
    }

    @Override
    protected String getAdsType() {
        return AdsType.BANNER_ADS;
    }

    @Override
    protected BannerCallback initCallback() {
        return new BannerCallback();
    }

    public void resume() {
        if (this.adView != null) {
            this.adView.resume();
        }
    }

    public void pause() {
        if (this.adView != null) {
            this.adView.pause();
        }
    }

    @Override
    public void destroyAds() {
        super.destroyAds();
        if (this.adView != null) {
            this.adView.destroy();
        }
    }

    public class BannerCallback extends AbstractAdListener<AdsBanner> {

        public BannerCallback() {
            super(AdsBanner.this);
        }

        @Override
        public void onAdFailedToLoad(@NonNull LoadAdError errors) {
            super.onAdFailedToLoad(errors);
            layoutContainer.setVisibility(View.GONE);
        }
    }


}
