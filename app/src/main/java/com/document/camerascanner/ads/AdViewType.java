package com.document.camerascanner.ads;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.document.camerascanner.R;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAdView;

public class AdViewType {

    private final Context context;

    private Button btCta;
    private ImageView ivAds;
    private TextView tvTitle;

    public AdViewType(Context context) {
        this.context = context;
    }

    public UnifiedNativeAdView getAdViewList(UnifiedNativeAd unifiedNativeAd) {
        LayoutInflater inflater = LayoutInflater.from(this.context);
        @SuppressLint("InflateParams") UnifiedNativeAdView adView = (UnifiedNativeAdView) inflater.inflate(R.layout.view_native_ads_list, null);
        this.initView(adView);
        this.addViewAd(unifiedNativeAd, adView);
        return adView;
    }

    public void initView(@NonNull ViewGroup viewGroup) {
        this.btCta = viewGroup.findViewById(R.id.bt_cta_ads);
        this.ivAds = viewGroup.findViewById(R.id.iv_ads);
        this.tvTitle = viewGroup.findViewById(R.id.tv_title_ads);
    }

    public void addViewAd(@NonNull UnifiedNativeAd unifiedNativeAd, @NonNull UnifiedNativeAdView adView) {
        adView.setHeadlineView(this.tvTitle);
        adView.setIconView(this.ivAds);
        adView.setCallToActionView(this.btCta);

        ((TextView) adView.getHeadlineView()).setText(unifiedNativeAd.getHeadline());

        if (unifiedNativeAd.getIcon() != null) {
            ((ImageView) adView.getIconView()).setImageDrawable(unifiedNativeAd.getIcon().getDrawable());
        }

        if ((unifiedNativeAd.getCallToAction()) != null) {
            ((Button) adView.getCallToActionView()).setText(unifiedNativeAd.getCallToAction());
        }

        adView.setNativeAd(unifiedNativeAd);
    }
}
