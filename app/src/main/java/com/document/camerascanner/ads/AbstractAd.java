package com.document.camerascanner.ads;

import android.content.Context;

import androidx.annotation.CallSuper;

import com.google.android.gms.ads.AdRequest;

import java.lang.ref.WeakReference;

public abstract class AbstractAd<T extends AbstractAdListener<?>> {

    protected T listener;
    protected String adUnit;
    protected String adsUnitName;
    protected AdRequest adRequest;
    protected WeakReference<Context> mCtx;

    public AbstractAd(Context context, String adUnit, String adsName) {
        this.mCtx = new WeakReference<>(context);
        this.adUnit = adUnit;
        this.adsUnitName = adsName;
        this.listener = this.initCallback();
    }

    public Context getContext() {
        return this.mCtx.get();
    }

    public String getAdsName() {
        return this.adsUnitName;
    }

    @CallSuper
    public void loadAds() {
        if (this.adRequest != null) {
            return;
        }

        this.adRequest = new AdRequest.Builder().build();
    }

    protected abstract String getAdsType();

    protected abstract T initCallback();

    public void destroyAds() {
        this.listener = null;
        this.adRequest = null;
    }


}
