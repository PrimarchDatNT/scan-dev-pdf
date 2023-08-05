package com.document.camerascanner.features.camera;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceOrientedMeteringPointFactory;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.document.camerascanner.R;
import com.document.camerascanner.databases.AppDatabases;
import com.document.camerascanner.databases.model.PageItem;
import com.document.camerascanner.databinding.ActivityCameraBinding;
import com.document.camerascanner.databinding.ViewOverlayBinding;
import com.document.camerascanner.features.camera.fragments.PreviewCameraFragment;
import com.document.camerascanner.features.detect.DetectActivity;
import com.document.camerascanner.features.intro.CameraIntro;
import com.document.camerascanner.features.save.SaveActivity;
import com.document.camerascanner.features.view.CustomisedDialog;
import com.document.camerascanner.features.view.DrawingView;
import com.document.camerascanner.prefs.AppPref;
import com.document.camerascanner.prefs.ConstantsPrefs;
import com.document.camerascanner.utils.AppUtils;
import com.document.camerascanner.utils.Constants;
import com.document.camerascanner.utils.FileUtils;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class CameraActivity extends AppCompatActivity implements ImageCapture.OnImageSavedCallback, PreviewCameraFragment.PreviewCameraCallback {

    private boolean isTakePhoto = false;
    private boolean isShownSpotlight;
    private boolean isShowGrid = true;
    private boolean isReCapture;
    private boolean isShowPreivewIntro;
    private int mCount = -1;
    private int correctDirection = 1;
    private int captureOrientation = Surface.ROTATION_0;
    private int positionReCapture = 0;

    private AppPref appPref;
    private File filePhoto;
    private List<Uri> uriList;
    private CameraIntro cameraIntro;
    private ImageCapture imageCapture;
    private FirebaseAnalytics event;
    private DrawingView drawingView;
    private ActivityCameraBinding binding;
    private AppDatabases appDatabases;
    private CompositeDisposable disposable;
    private PreviewCameraFragment previewFragment;
    private OrientationEventListener orientationEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.init();
    }

    private void init() {
        this.initConfig();
        this.initView();
        this.initPermission();
        this.initOrientation();
    }

    private void initOrientation() {
        this.orientationEventListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int i) {
                if (i > 0 && i <= 45) {
                    captureOrientation = Surface.ROTATION_0;
                    if (correctDirection != 1) {
                        correctDirection = 1;
                        rotateFeatureIcon(0);
                    }
                    return;
                }

                if (i > 45 && i <= 135) {
                    if (correctDirection == 2) {
                        return;
                    }

                    captureOrientation = Surface.ROTATION_270;
                    correctDirection = 2;
                    rotateFeatureIcon(-90);
                    return;
                }

                if (i > 135 && i <= 225) {
                    captureOrientation = Surface.ROTATION_180;
                    return;
                }

                if (i > 225 && i <= 315) {
                    if (correctDirection == 4) {
                        return;
                    }
                    captureOrientation = Surface.ROTATION_90;
                    correctDirection = 4;
                    rotateFeatureIcon(90);
                    return;
                }

                if (i > 315 && i <= 360) {
                    captureOrientation = Surface.ROTATION_0;
                }

            }
        };

        if (this.orientationEventListener.canDetectOrientation()) {
            this.orientationEventListener.enable();
        } else {
            this.orientationEventListener.disable();
        }
    }

    private void rotateFeatureIcon(int angle) {
        this.binding.ivFlash.animate().setDuration(200).rotation(angle).start();
        this.binding.ivCapturedImageHolder.animate().setDuration(200).rotation(angle).start();
        this.binding.ivTickIcon.animate().setDuration(200).rotation(angle).start();
        this.binding.ivDiscardAll.animate().setDuration(200).rotation(angle).start();
        this.binding.ivSingleCapture.animate().setDuration(200).rotation(angle).start();
        this.binding.ivMultiCapture.animate().setDuration(200).rotation(angle).start();
    }

    private void initSpotlight() {
        if (!this.appPref.isShowSpotlight(ConstantsPrefs.KEY_SHOW_SPOTLIGHT_CAMERA_ACTIVITY)) {
            this.isShownSpotlight = false;
            return;
        }

        this.isShownSpotlight = true;

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            this.cameraIntro = new CameraIntro(this, this.binding.getRoot())
                    .setCameraAnchor(this.binding.ivCamera)
                    .setFlashAnchor(this.binding.ivFlash)
                    .setMultiCaptureAnchor(this.binding.ivMultiCapture)
                    .setSingleCaptureAnchor(this.binding.ivSingleCapture)
                    .setGridAnchor(this.binding.ivGrid)
                    .setCallback(() -> this.isShownSpotlight = false);

            this.cameraIntro.showGuide();
        }, 500);
    }

    private void initConfig() {
        this.uriList = new ArrayList<>();
        this.appPref = AppPref.getInstance(this);
        this.event = FirebaseAnalytics.getInstance(this);
        this.appDatabases = AppDatabases.getInstance(this);
        this.isShownSpotlight = true;
        this.event.logEvent("CAMERA_OPEN", null);
    }

    private void initView() {
        this.binding = ActivityCameraBinding.inflate(this.getLayoutInflater());
        ViewOverlayBinding.bind(this.binding.clLoading);
        this.setContentView(this.binding.getRoot());
        this.binding.ivGrid.setSelected(this.isShowGrid);
    }

    @NeedsPermission(Manifest.permission.CAMERA)
    public void initPermission() {
        if (this.allPermissionsGranted()) {
            this.startCamera();
            this.initSpotlight();
        } else {
            CameraActivityPermissionsDispatcher.initPermissionWithPermissionCheck(this);
        }
    }

    private void showCaptureProcess(boolean onProcess) {
        this.binding.ivCaptureProcess.setColorFilter(ContextCompat.getColor(this, onProcess ? R.color.color_white : R.color.colorAccent));
        this.binding.ivCaptureProcess.postDelayed(() ->
                this.binding.ivCaptureProcess.setVisibility(onProcess ? View.VISIBLE : View.INVISIBLE), onProcess ? 0 : 550);
    }

    private void takePhoto() {
        if (this.imageCapture == null) {
            return;
        }

        this.showCaptureProcess(true);

        try {
            this.filePhoto = new File(this.getExternalFilesDir(Constants.ALL_TEMP),
                    this.getString(R.string.all_name_original, System.currentTimeMillis(), Constants.IMGAGE_EXTENSION));
            this.imageCapture.setTargetRotation(this.captureOrientation);
            this.imageCapture.takePicture(new ImageCapture.OutputFileOptions.Builder(this.filePhoto).build(),
                    ContextCompat.getMainExecutor(this), this);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public void onClickConfirmCapture(View view) {
        if (this.isShownSpotlight) {
            return;
        }

        if (this.uriList == null || this.uriList.isEmpty()) {
            return;
        }

        if (this.uriList.size() == 1) {
            this.event.logEvent("CAMERA_CAPTURE_SINGLE", null);
            this.startDetectActivity(this.uriList.get(0));
        } else {
            this.event.logEvent("CAMERA_CAPTURE_MULTIPLE", null);
            this.startSaveActivity();
        }
    }

    private void startSaveActivity() {
        Completable.create(emitter -> {
            List<PageItem> listTempPage = new ArrayList<>();
            for (int i = 0; i < this.uriList.size(); i++) {
                listTempPage.add(this.getTempPageItem(i + 1, new File(uriList.get(i).getPath())));
            }
            this.appDatabases.pageDao().insertListEntityNoRx(listTempPage);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        composeDispose(d);
                    }

                    @Override
                    public void onComplete() {
                        Intent intent = new Intent(CameraActivity.this, SaveActivity.class);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

    private void startResultActivity() {
        Completable.create(emitter -> {
            for (Uri uri : this.uriList) {
                FileUtils.deleteFile(uri);
            }
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        composeDispose(d);
                    }

                    @Override
                    public void onComplete() {
                        AppUtils.startActivity(CameraActivity.this);
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

    @Override
    public void onBackPressed() {
        if (this.isShownSpotlight) {
            if (this.appPref.isShowSpotlight(ConstantsPrefs.KEY_SHOW_SPOTLIGHT_CAMERA_ACTIVITY)) {
                if (this.cameraIntro != null) {
                    this.cameraIntro.finishGuilde();
                    return;
                }
            }
        }

        if (this.uriList.isEmpty()) {
            this.startResultActivity();
            return;
        }

        if (this.isShowPreivewIntro) {
            if (this.previewFragment != null) {
                this.previewFragment.finishIntro();
            }
            return;
        }

        if (this.previewFragment != null && this.previewFragment.isAdded() && this.previewFragment.isVisible()) {
            this.removeFragment();
            return;
        }

        this.showDiscardDialog();
    }

    private void showDiscardDialog() {
        CustomisedDialog discardDialog = new CustomisedDialog(this)
                .setTitle(R.string.camera_discard_capture_dialog_title)
                .setMessage(R.string.camera_prompt_discard_message)
                .setButtonCancelText(R.string.camera_negative_dialog_option)
                .setButtonAllowText(R.string.camera_positive_dialog_option);
        discardDialog.setListener(new CustomisedDialog.DialogOnClickListener() {
            @Override
            public void onCancel() {
                discardDialog.dimiss();
            }

            @Override
            public void onAccept() {
                startResultActivity();
            }
        });
        discardDialog.show();
    }

    @SuppressLint("ClickableViewAccessibility")
    public void startCamera() {
        this.drawingView = new DrawingView(this);
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();

                int flashMode = this.binding.ivFlash.isSelected() ? ImageCapture.FLASH_MODE_ON : ImageCapture.FLASH_MODE_OFF;
                this.imageCapture = new ImageCapture.Builder()
                        .setFlashMode(flashMode)
                        .build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                try {
                    cameraProvider.unbindAll();
                    Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, this.imageCapture);

                    this.binding.cameraPreview.setOnTouchListener((view, motionEvent) -> {
                        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                            return true;
                        }

                        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                            SurfaceOrientedMeteringPointFactory pointFactory = new SurfaceOrientedMeteringPointFactory(view.getWidth(), view.getHeight());
                            MeteringPoint point = pointFactory.createPoint(motionEvent.getX(), motionEvent.getY());
                            camera.getCameraControl().startFocusAndMetering(new FocusMeteringAction.Builder(point).build());
                            this.showFocusView(motionEvent.getX(), motionEvent.getY());
                            return true;
                        }

                        return false;
                    });

                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, this.imageCapture);
                    preview.setSurfaceProvider(this.binding.cameraPreview.getSurfaceProvider());
                } catch (Exception exception) {
                    exception.printStackTrace();
                }

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!this.allPermissionsGranted()) {
            this.appPref.setShowSpotlight(ConstantsPrefs.KEY_SHOW_SPOTLIGHT_CAMERA_ACTIVITY, true);
            return;
        }

        this.updateStage();
    }

    private void updateStage() {
        this.isTakePhoto = false;
        this.binding.ivFlash.setSelected(this.appPref.getFlashMode());
        this.binding.ivSingleCapture.setSelected(!this.appPref.isBatchMode());
        this.binding.ivMultiCapture.setSelected(this.appPref.isBatchMode());
        this.binding.clLoading.setVisibility(View.GONE);
    }

    private boolean allPermissionsGranted() {
        for (String requiredPermission : new String[]{Manifest.permission.CAMERA}) {
            if (ContextCompat.checkSelfPermission(this, requiredPermission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @SuppressLint("NeedOnRequestPermissionsResult")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        CameraActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @OnPermissionDenied(Manifest.permission.CAMERA)
    public void onCameraPermissionDenied() {
        this.appPref.setShowSpotlight(ConstantsPrefs.KEY_SHOW_SPOTLIGHT_CAMERA_ACTIVITY, true);
        this.finish();
    }

    @OnNeverAskAgain(Manifest.permission.CAMERA)
    public void onTakePhotoNeverAskAgain() {
        this.askForPermission();
    }

    public void askForPermission() {
        CustomisedDialog permissionDialog = new CustomisedDialog(this)
                .setTitle(R.string.camera_request_permission_title)
                .setMessage(getString(R.string.camera_request_permission_message, "Camera", "Camera", "Camera", "Camera"))
                .setMessage(this.getString(R.string.camera_request_permission_message, "Camera", "Camera", "Camera", "Camera"))
                .setButtonAllowText(R.string.button_allow_text)
                .setButtonCancelText(R.string.all_cancel);
        permissionDialog.setListener(new CustomisedDialog.DialogOnClickListener() {
            @Override
            public void onCancel() {
                permissionDialog.dimiss();
                finish();
            }

            @Override
            public void onAccept() {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                permissionDialog.dimiss();
                startActivityForResult(intent, Constants.CODE_PERMISSION_STORAGE);
            }
        });
        permissionDialog.show();
    }

    private void showFocusView(float x, float y) {
        if (this.drawingView == null) {
            return;
        }

        this.drawingView.onTouchToFocus(x, y);
        this.drawingView.invalidate();

        ValueAnimator forcusAnimator = ValueAnimator.ofFloat(0, 6);
        forcusAnimator.addUpdateListener(valueAnimator -> {
            float animatedValue = (float) valueAnimator.getAnimatedValue();
            this.drawingView.setTouchCircleRadius(animatedValue);
            this.drawingView.invalidate();
        });

        forcusAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                drawingView.onFocusDone();
                new Handler().postDelayed(() -> {
                    if (drawingView != null) {
                        drawingView.setTouch(false);
                        drawingView.invalidate();
                    }
                }, 700);
            }
        });

        forcusAnimator.setDuration(400);
        forcusAnimator.start();
        this.cancelFocusView();
        this.binding.cameraPreview.addView(this.drawingView);
    }

    private void cancelFocusView() {
        try {
            this.binding.cameraPreview.removeView(this.drawingView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClickShowGrid(View view) {
        this.isShowGrid = !this.isShowGrid;
        this.binding.ivGrid.setSelected(this.isShowGrid);
        this.binding.gridLayerCamera.setVisibility(this.isShowGrid ? View.VISIBLE : View.GONE);
    }

    public void onClickPreViewImage(View view) {
        boolean isBatchMode = this.appPref.isBatchMode();
        if (!isBatchMode) {
            return;
        }

        this.previewFragment = new PreviewCameraFragment(this.uriList, this);
        this.binding.frContainer.setVisibility(View.VISIBLE);
        this.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fr_container, this.previewFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
    }

    public void onClickTakePhoto(View view) {
        if (this.isShownSpotlight) {
            return;
        }

        this.event.logEvent("CAMERA_CLICK_CAMERA", null);
        if (this.isTakePhoto) {
            return;
        }
        this.takePhoto();
        this.isTakePhoto = true;
    }

    public void onClickSingleMode(View view) {
        if (this.isShownSpotlight) {
            return;
        }
        this.event.logEvent("CAMERA_CLICK_SINGLE_MODE", null);
        this.updateView(true);
        this.showToast(R.string.camera_single_mode_on);
    }

    public void onClickMultiCaptureMode(View view) {
        if (this.isShownSpotlight) {
            return;
        }
        this.event.logEvent("CAMERA_CLICK_BATCH_MODE", null);
        this.updateView(false);
        this.showToast(R.string.camera_batch_mode_on);
    }

    private void updateView(boolean isSingleMode) {
        this.binding.ivSingleCapture.setSelected(isSingleMode);
        this.binding.ivMultiCapture.setSelected(!isSingleMode);
        this.appPref.setCameraMode(!isSingleMode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        CameraActivityPermissionsDispatcher.initPermissionWithPermissionCheck(this);
    }

    public void onClickFlashMode(View view) {
        if (this.isShownSpotlight) {
            return;
        }

        this.event.logEvent("CAMERA_CLICK_FLASH_MODE", null);
        this.mCount++;

        switch (this.mCount) {
            case ImageCapture.FLASH_MODE_ON:
                this.updateCaptureMode(R.drawable.camera_vector_enable_flash_icon, ImageCapture.FLASH_MODE_ON, R.string.camera_flash_mode_on);
                break;

            case ImageCapture.FLASH_MODE_OFF:
                this.updateCaptureMode(R.drawable.camera_vector_flash_icon, ImageCapture.FLASH_MODE_OFF, R.string.camera_flash_mode_off);
                break;

            case ImageCapture.FLASH_MODE_AUTO:
                this.updateCaptureMode(R.drawable.camera_vector_auto_flash, ImageCapture.FLASH_MODE_AUTO, R.string.camera_auto_flash_mode);
                break;
        }

        if (this.mCount >= 2) {
            this.mCount = -1;
        }
    }

    private void updateCaptureMode(int flashRes, int flashMode, int toastRes) {
        this.binding.ivFlash.setImageResource(flashRes);
        this.imageCapture.setFlashMode(flashMode);
        this.showToast(toastRes);
    }

    private void showToast(int resId) {
        Toast toastFlashOn = Toast.makeText(this, resId, Toast.LENGTH_SHORT);
        toastFlashOn.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 300);
        toastFlashOn.show();
    }

    public void discardAll(View view) {
        if (this.isShownSpotlight) {
            return;
        }

        this.event.logEvent("CAMERA_CLICK_DISCARD_ALL", null);
        CustomisedDialog discardDialog = new CustomisedDialog(this)
                .setButtonAllowText(R.string.camera_positive_dialog_option)
                .setButtonCancelText(R.string.camera_negative_dialog_option)
                .setTitle(R.string.camera_discard_dialog_title)
                .setMessage(R.string.camera_prompt_discard_message);

        discardDialog.setListener(new CustomisedDialog.DialogOnClickListener() {
            @Override
            public void onCancel() {
                discardDialog.dimiss();
            }

            @Override
            public void onAccept() {
                Completable.create(emitter -> {
                    for (Uri uri : uriList) {
                        FileUtils.deleteFile(uri);
                    }
                    emitter.onComplete();
                }).subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new CompletableObserver() {
                            @Override
                            public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                                composeDispose(d);
                            }

                            @Override
                            public void onComplete() {
                                uriList.clear();
                                isReCapture = false;
                                binding.ivTickIcon.setVisibility(View.GONE);
                                binding.ivDiscardAll.setVisibility(View.GONE);
                                binding.ivImageCounter.setVisibility(View.GONE);
                                binding.ivCapturedImageHolder.setVisibility(View.GONE);
                                binding.ivMultiCapture.setVisibility(View.VISIBLE);
                                binding.ivSingleCapture.setVisibility(View.VISIBLE);
                                discardDialog.dimiss();
                            }

                            @Override
                            public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                                e.printStackTrace();
                            }
                        });
            }
        });
        discardDialog.show();
    }

    @Override
    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
        if (this.isReCapture) {
            this.uriList.add(this.positionReCapture, Uri.fromFile(this.filePhoto));
            File file = new File(this.uriList.get(this.positionReCapture + 1).getPath());
            System.out.println(file.delete());
            this.uriList.remove(this.positionReCapture + 1);
            this.isReCapture = false;
            Toast.makeText(this, R.string.recapture_status, Toast.LENGTH_SHORT).show();
        } else {
            this.uriList.add(Uri.fromFile(this.filePhoto));
        }

        this.showCaptureProcess(false);

        if (!this.appPref.isBatchMode()) {
            this.event.logEvent("CAMERA_CAPTURE_SINGLE", null);
            this.startDetectActivity(this.uriList.get(0));
            return;
        }

        this.isTakePhoto = false;
        this.binding.ivCapturedImageHolder.setVisibility(View.VISIBLE);
        this.binding.ivTickIcon.setVisibility(View.VISIBLE);
        this.binding.ivMultiCapture.setVisibility(View.GONE);
        this.binding.ivSingleCapture.setVisibility(View.GONE);
        this.binding.ivImageCounter.setVisibility(View.VISIBLE);
        this.binding.ivDiscardAll.setVisibility(View.VISIBLE);
        this.binding.ivImageCounter.setText(String.valueOf(this.uriList.size()));

        Glide.with(this)
                .asBitmap()
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .load(this.filePhoto.getPath())
                .error(R.drawable.all_place_holder)
                .override(300, 300)
                .into(this.binding.ivCapturedImageHolder);
    }

    private void startDetectActivity(Uri uri) {
        if (uri == null) {
            return;
        }

        Single.create((SingleOnSubscribe<List<PageItem>>) emitter -> {
            PageItem tempPageItem = this.getTempPageItem(1, new File(uri.getPath()));
            this.appDatabases.pageDao().insertEntityNoRx(tempPageItem);
            emitter.onSuccess(this.appDatabases.pageDao().getPreviousTempPage());
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<PageItem>>() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        composeDispose(d);
                    }

                    @Override
                    public void onSuccess(@io.reactivex.annotations.NonNull List<PageItem> list) {
                        Intent intent = new Intent(CameraActivity.this, DetectActivity.class);
                        intent.putExtra(Constants.EXTRA_PAGE_ITEM, list.get(0));
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

    @NotNull
    private PageItem getTempPageItem(int position, @NotNull File captureFile) {
        PageItem pageItem = new PageItem();
        pageItem.setParentId(1);
        pageItem.setLoading(true);
        pageItem.setPosition(position);
        pageItem.setOrgUri(captureFile.getPath());
        return pageItem;
    }

    private void composeDispose(Disposable disposable) {
        if (this.disposable == null) {
            this.disposable = new CompositeDisposable();
        }
        this.disposable.add(disposable);
    }

    @Override
    public void onError(@NonNull ImageCaptureException exception) {
        this.showToast(R.string.camera_caputure_error);
        exception.printStackTrace();
    }

    @Override
    protected void onDestroy() {
        if (this.disposable != null) {
            this.disposable.dispose();
        }

        this.orientationEventListener.disable();
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        System.gc();
        super.onDestroy();
    }

    private void removeFragment() {
        if (this.previewFragment == null) {
            return;
        }
        this.getSupportFragmentManager()
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .remove(this.previewFragment)
                .commit();
    }

    @Override
    public void onShowPreviewIntro(boolean isShow) {
        this.isShowPreivewIntro = isShow;
    }

    @Override
    public void onBackDetail() {
        this.removeFragment();
    }

    @Override
    public void onReCapture(int position) {
        this.removeFragment();
        this.isReCapture = true;
        this.positionReCapture = position;
    }

    @Override
    public void onTickDone() {
        this.binding.ivTickIcon.performClick();
    }

    @Override
    public void onDeletePage() {
        this.binding.ivImageCounter.setText(String.valueOf(this.uriList.size()));
    }
}