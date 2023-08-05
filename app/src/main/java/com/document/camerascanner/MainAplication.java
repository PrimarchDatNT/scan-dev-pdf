package com.document.camerascanner;

import android.app.Application;
import android.content.Context;

import androidx.multidex.MultiDex;

import com.document.camerascanner.ads.AdsUtil;
import com.facebook.stetho.Stetho;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.firebase.FirebaseApp;

import org.opencv.osgi.OpenCVNativeLoader;

public class MainAplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        new OpenCVNativeLoader().init();
        Stetho.initializeWithDefaults(this);
        FirebaseApp.initializeApp(this);
        this.initAdMob();
    }

    private void initAdMob() {
        RequestConfiguration requestConfiguration = new RequestConfiguration.Builder()
                .setTestDeviceIds(AdsUtil.getTestDevices(this))
                .build();
        MobileAds.setRequestConfiguration(requestConfiguration);
        MobileAds.initialize(this);
    }

}
