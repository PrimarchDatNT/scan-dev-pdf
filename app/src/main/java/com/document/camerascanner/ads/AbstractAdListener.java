package com.document.camerascanner.ads;

import android.util.Log;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.LoadAdError;
import com.google.firebase.analytics.FirebaseAnalytics;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;

public abstract class AbstractAdListener<T extends AbstractAd<?>> extends AdListener {

    private static final String TAG = "BaseAdsListener";

    private static final String FORMAT_PATTERN = "%s-%s-%s";

    protected T mAds;

    public AbstractAdListener(T mAds) {
        this.mAds = mAds;
    }

    @NonNull
    private String getTags() {
        return String.format(FORMAT_PATTERN, TAG, this.mAds.getAdsName(), this.mAds.getAdsType());
    }

    @Override
    @CallSuper
    public void onAdClosed() {
        Log.i(this.getTags(), "onAdClosed");
    }

    @Override
    @CallSuper
    public void onAdFailedToLoad(@NonNull LoadAdError errors) {
        final String tag = this.getTags();
        final AdError adError = errors.getCause();

        if (adError == null) {
            Log.i(tag, "onAdFailedToLoad");
            return;
        }
        Log.i(tag, String.format("onAdFailedToLoad: errorCode: %d|errorMessage: %s", adError.getCode(), adError.getMessage()));
    }

    @Override
    public void onAdFailedToLoad(int i) {
        super.onAdFailedToLoad(i);
        final String tag = this.getTags();
        Log.i(tag, String.format("onAdFailedToLoad: errorCode: %d", i));
    }

    @Override
    @CallSuper
    public void onAdLeftApplication() {
        Log.i(this.getTags(), "onAdLeftApplication");
    }

    @Override
    @CallSuper
    public void onAdOpened() {
        Log.i(this.getTags(), "onAdOpened");
        FirebaseAnalytics.getInstance(this.mAds.getContext())
                .logEvent(String.format("ad_%s_open", this.mAds.getAdsName()).toUpperCase(), null);
    }

    @Override
    @CallSuper
    public void onAdLoaded() {
        Log.i(this.getTags(), "onAdLoaded");
    }

    @Override
    @CallSuper
    public void onAdClicked() {
        Log.i(this.getTags(), "onAdClicked");
        FirebaseAnalytics.getInstance(this.mAds.getContext())
                .logEvent(String.format("AD_%s_CLICK", this.mAds.getAdsName()).toUpperCase(), null);
    }

    @Override
    @CallSuper
    public void onAdImpression() {
        Log.i(this.getTags(), "onAdImpression");
    }

}
