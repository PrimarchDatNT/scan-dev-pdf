package com.document.camerascanner.splash;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.document.camerascanner.R;
import com.document.camerascanner.ads.AdConstant;
import com.document.camerascanner.ads.FANInter;
import com.document.camerascanner.ads.InterAds;
import com.document.camerascanner.ads.UtmSourceChecker;
import com.document.camerascanner.databases.AppDatabases;
import com.document.camerascanner.databases.model.DocumentItem;
import com.document.camerascanner.databases.model.FolderItem;
import com.document.camerascanner.databinding.ActivitySplashBinding;
import com.document.camerascanner.features.about.PolicyViewerActivity;
import com.document.camerascanner.features.main.MainActivity;
import com.document.camerascanner.prefs.AppPref;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.jetbrains.annotations.NotNull;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class SplashActivity extends AppCompatActivity implements UtmSourceChecker.OnCheckInstallCallback {

    private AppPref mPref;
    private InterAds interAd;
    private AppDatabases appDatabases;
    private ActivitySplashBinding binding;
    private CompositeDisposable disposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.init();
    }

    private void init() {
        FirebaseAnalytics.getInstance(this).logEvent("SPLASH_OPEN", null);
        UtmSourceChecker.getInstance(this).rf(this);

        this.binding = ActivitySplashBinding.inflate(this.getLayoutInflater());
        this.setContentView(this.binding.getRoot());
        this.mPref = AppPref.getInstance(this);
        this.mPref.setSessionShowReview(false);
        this.mPref.setCallShowReview(false);
        this.appDatabases = AppDatabases.getInstance(this);
        this.initDataBases();

        boolean isAccept = this.mPref.isAcceptedPolicy();
        this.binding.tvSplashCta.setVisibility(isAccept ? View.GONE : View.VISIBLE);
        this.binding.tvSplashPolicy.setVisibility(isAccept ? View.GONE : View.VISIBLE);
        if (isAccept) {
            this.initAnimation();
            return;
        }
        FirebaseAnalytics.getInstance(this).logEvent("SPLASH_FIRST_OPEN", null);
        this.initPolicyContent();
    }

    private void initAnimation() {
        this.binding.lottieView.addAnimatorListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                showAd();
            }
        });
    }

    private void initPolicyContent() {
        SpannableStringBuilder builder = new SpannableStringBuilder(this.getString(R.string.about_app_policy_first_part).trim() + " ");
        builder.append(this.getString(R.string.settings_privacy_policy).trim());
        builder.setSpan(new ClickableSpan() {
                            public void onClick(@NotNull View view) {
                                FirebaseAnalytics.getInstance(SplashActivity.this).logEvent("SPLASH_CLICK_POLICY", null);
                                openPolicy();
                            }
                        }, builder.length() - this.getString(R.string.settings_privacy_policy).length()
                , builder.length(), 0);

        this.binding.tvSplashPolicy.setMovementMethod(LinkMovementMethod.getInstance());
        this.binding.tvSplashPolicy.setHighlightColor(0);
        this.binding.tvSplashPolicy.setText(builder, TextView.BufferType.SPANNABLE);
    }

    private void initDataBases() {
        if (this.mPref.isCreatedRootFolder()) {
            return;
        }

        FolderItem folderMain = new FolderItem();
        folderMain.setId(1);
        folderMain.setParentId(0);
        folderMain.setName("Main");

        this.appDatabases.folderDao()
                .insertEntity(folderMain)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Long>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        composeDispose(d);
                    }

                    @Override
                    public void onSuccess(@NonNull Long folderId) {
                        mPref.setCreatedRootFolder();
                        if (!mPref.isCreatedTempDocument()) {
                            DocumentItem documentTemp = new DocumentItem();
                            documentTemp.setId(1);
                            documentTemp.setParentId(folderId.intValue());

                            appDatabases.documentDao()
                                    .insertEntity(documentTemp)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(new SingleObserver<Long>() {
                                        @Override
                                        public void onSubscribe(@NonNull Disposable d) {
                                            composeDispose(d);
                                        }

                                        @Override
                                        public void onSuccess(@NonNull Long docId) {
                                            if (docId >0) {
                                                mPref.setCreatedTempDocument();
                                            }
                                        }

                                        @Override
                                        public void onError(@NonNull Throwable e) {
                                            e.printStackTrace();
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

    private void initAd() {
        this.interAd = new InterAds(this, AdConstant.IT_SPLASH, AdConstant.SPLASH);
        this.interAd.setListener(new InterAds.InterAdsListener(this.interAd) {
            @Override
            public void onAdClosed() {
                super.onAdClosed();
                gotoMain();
            }
        });

//        this.interAd.loadAds();
//        FANInter.load(this);
    }

    private void showAd() {
        if (this.interAd != null && this.interAd.isLoaded()) {
            this.interAd.showAds();
            FANInter.setAllowShow(false);
            return;
        }
        this.gotoMain();
    }

    public void onClickAcceptPolicy(View view) {
        FirebaseAnalytics.getInstance(this).logEvent("SPLASH_CLICK_ACCEPT", null);
        this.mPref.acceptPolicy();
        this.gotoMain();
    }

    private void composeDispose(Disposable d) {
        if (this.disposable == null) {
            this.disposable = new CompositeDisposable();
        }
        this.disposable.add(d);
    }

    private void openPolicy() {
        Intent iPolicy = new Intent(this, PolicyViewerActivity.class);
        iPolicy.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(iPolicy);
    }

    private void gotoMain() {
        this.startActivity(new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
        this.finish();
    }

    @Override
    public void onSuccess() {
        if (UtmSourceChecker.isAllowShowAd(this)) {
            this.initAd();
        }
    }

    @Override
    protected void onDestroy() {
        UtmSourceChecker.getInstance(this).destroyRf();
        if (this.disposable != null) {
            this.disposable.dispose();
        }
        super.onDestroy();
    }
}