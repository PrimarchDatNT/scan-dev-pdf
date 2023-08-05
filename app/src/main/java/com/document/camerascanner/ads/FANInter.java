package com.document.camerascanner.ads;

import android.app.Activity;
import android.util.Log;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.InterstitialAd;
import com.facebook.ads.InterstitialAdListener;

public class FANInter {

    private static final String TAG = "BaseAdsListener";

    private static boolean isAllowShow;

    private static InterstitialAd fbInterstitial;

    public static void load(Activity activity) {
        isAllowShow = true;
        fbInterstitial = new InterstitialAd(activity, AdConstant.FAN_IT_HOME);
        InterstitialAd.InterstitialAdLoadConfigBuilder interstitialAdLoadConfigBuilder = fbInterstitial.buildLoadAdConfig();

        interstitialAdLoadConfigBuilder.withAdListener(new InterstitialAdListener() {
            @Override
            public void onInterstitialDisplayed(Ad ad) {
                Log.i(TAG, "onInterstitialDisplayed: ");
            }

            @Override
            public void onInterstitialDismissed(Ad ad) {
                Log.i(TAG, "onInterstitialDismissed: ");
            }

            @Override
            public void onError(Ad ad, AdError adError) {
                Log.i(TAG, "onError: " + adError.getErrorCode() + ", " + adError.getErrorMessage());
            }

            @Override
            public void onAdLoaded(Ad ad) {
                Log.i(TAG, "onAdLoaded: ");
            }

            @Override
            public void onAdClicked(Ad ad) {
                Log.i(TAG, "onAdClicked: ");
            }

            @Override
            public void onLoggingImpression(Ad ad) {
                Log.i(TAG, "onLoggingImpression: ");
            }
        });

        fbInterstitial.loadAd();
    }

    public static void setAllowShow(boolean allowShow) {
        isAllowShow = allowShow;
    }

    public static boolean show() {
        if (!isAllowShow) {
            return false;
        }

        if (fbInterstitial != null && fbInterstitial.isAdLoaded()) {
            fbInterstitial.show();
            return true;
        }
        return false;
    }

    public static boolean isAdLoaded() {
        return fbInterstitial != null && fbInterstitial.isAdLoaded();
    }

    public static void destroy() {
        if (fbInterstitial != null) {
            fbInterstitial.destroy();
        }
    }
}
