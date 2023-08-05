package com.document.camerascanner.features.about;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import com.document.camerascanner.databinding.ActivityPolicyViewerBinding;
import com.google.firebase.analytics.FirebaseAnalytics;

public class PolicyViewerActivity extends AppCompatActivity {

    private static final String EMPTY_URI = "about:blank";

    private static final String PRIVACY_URI = "file:///android_asset/policy.html";

    private ActivityPolicyViewerBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.init();
        FirebaseAnalytics.getInstance(this).logEvent("POLICY_OPEN", null);
    }

    private void init() {
        this.initView();
        this.loadData();
    }

    private void initView() {
        this.binding = ActivityPolicyViewerBinding.inflate(this.getLayoutInflater());
        this.setContentView(this.binding.getRoot());
        this.initWebview();
    }

    private void loadData() {
        try {
            this.binding.wvPolicyContent.loadUrl(PRIVACY_URI);
        } catch (Exception e) {
            this.binding.wvPolicyContent.loadUrl(EMPTY_URI);
            e.printStackTrace();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebview() {
        this.binding.wvPolicyContent.setInitialScale(63);
        WebSettings mWebSettings = this.binding.wvPolicyContent.getSettings();
        mWebSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        mWebSettings.setAppCacheEnabled(false);
        mWebSettings.setBlockNetworkImage(true);
        mWebSettings.setNeedInitialFocus(false);
        mWebSettings.setTextZoom((int) (200.0f * (this.getResources().getConfiguration().fontScale)));
        mWebSettings.setBuiltInZoomControls(false);
        mWebSettings.setDefaultTextEncodingName("UTF-8");
        mWebSettings.setUseWideViewPort(true);
        mWebSettings.setJavaScriptEnabled(true);
        mWebSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);

        this.binding.wvPolicyContent.setWebViewClient(new WebViewClient() {
            public void onPageStarted(WebView webView, String url, Bitmap favicon) {
                super.onPageStarted(webView, url, favicon);
                updateLoadingLayout(true);
            }

            public void onPageFinished(WebView webView, String url) {
                super.onPageFinished(webView, url);
                updateLoadingLayout(false);

                webView.setVisibility(View.VISIBLE);
                webView.loadUrl("javascript:(function() { document.getElementById('footer').style.display='none'; document.getElementById('header').style.display='none';})()");
            }

            @TargetApi(23)
            public void onReceivedError(WebView webView, WebResourceRequest request, WebResourceError error) {
                updateEmpty(webView);
            }

            public void onReceivedError(WebView webView, int errorCode, String description, String failingUrl) {
                super.onReceivedError(webView, errorCode, description, failingUrl);
                updateEmpty(webView);
            }

            public void onReceivedSslError(WebView webView, final SslErrorHandler errorHandler, SslError sslError) {
                showErrorDialog(errorHandler);
            }

            @TargetApi(21)
            public boolean shouldOverrideUrlLoading(WebView webView, WebResourceRequest resourceRequest) {
                return this.shouldOverrideUrlLoading(binding.wvPolicyContent, resourceRequest.getUrl().toString());
            }

            public boolean shouldOverrideUrlLoading(WebView webView, String url) {
                Uri uri = Uri.parse(url);
                String scheme = uri.getScheme();
                if (!TextUtils.equals(scheme, "http") && !TextUtils.equals(scheme, "https")) {
                    return super.shouldOverrideUrlLoading(webView, url);
                } else {
                    startActivity((new Intent("android.intent.action.VIEW", uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    return true;
                }
            }
        });
    }

    private void updateEmpty(WebView webView) {
        try {
            webView.loadUrl(EMPTY_URI);
            webView.setVisibility(View.GONE);
            this.updateLoadingLayout(false);
        } catch (Exception e) {
            e.printStackTrace();
            this.finish();
        }
    }

    private void updateLoadingLayout(boolean isLoading) {
        this.binding.llLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void showErrorDialog(SslErrorHandler errorHandler) {
        new AlertDialog.Builder(this)
                .setTitle("SSL Certificate Error")
                .setMessage("SSL Certificate error. Do you want to continue anyway?")
                .setNegativeButton("Cancel", (dialog, which) -> {
                    errorHandler.cancel();
                    dialog.dismiss();
                })
                .setPositiveButton("Continue", (dialog, which) -> {
                    errorHandler.proceed();
                    dialog.dismiss();
                }).show();
    }

    public void onClickBack(View view) {
        this.onBackPressed();
    }
}