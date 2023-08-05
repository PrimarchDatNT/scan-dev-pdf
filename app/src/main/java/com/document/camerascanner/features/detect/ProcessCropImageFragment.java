package com.document.camerascanner.features.detect;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.document.camerascanner.databases.model.PageItem;
import com.document.camerascanner.databinding.FragmentProcessCropImageBinding;
import com.document.camerascanner.features.view.PolygonView;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.Contract;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class ProcessCropImageFragment extends Fragment implements ImageDetect.View, PolygonView.OnVertextPointChangeListener {

    public boolean isClickMax = false;

    private float widthOffset = 1.0f;
    private float heightOffset = 1.0f;

    private Context mContext;
    private Bitmap cropBitmap;
    private PointF[] cachePoint;
    private PageItem pageItem;
    private ProccesCropCallback callback;
    private CompositeDisposable mDisposable;
    private ImageDetectPresenter detectPresenter;
    private FragmentProcessCropImageBinding binding;

    @NonNull
    public static ProcessCropImageFragment newInstance(PageItem pageItem) {
        ProcessCropImageFragment fragment = new ProcessCropImageFragment();
        fragment.pageItem = pageItem;
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.mContext = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.detectPresenter = new ImageDetectPresenter(this.getActivity(), this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.binding = FragmentProcessCropImageBinding.inflate(inflater, container, false);
        return this.binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.binding.viewPolygon.setVertextListener(this);
        if (this.detectPresenter != null) {
            this.detectPresenter.processUri(this.mContext, Uri.parse(this.pageItem.getOrgUri()));
        }
    }

    @NonNull
    @Contract("_ -> new")
    private int[] getAdjustSize(@NonNull Bitmap bitmap) {
        float maxScaleW = this.binding.getRoot().getWidth();
        float maxScaleH = this.binding.getRoot().getHeight();
        float srcW = bitmap.getWidth();
        float srcH = bitmap.getHeight();

        float orgRatio = srcW / srcH;
        float withRatio = maxScaleW / srcW;
        float heightRatio = maxScaleH / srcH;

        if (orgRatio < 1) {
            float temptH = srcH * withRatio;
            if (temptH > maxScaleH) {
                srcH = maxScaleH;
                srcW = (srcW * heightRatio);
            } else {
                srcW = maxScaleW;
                srcH = (srcH * withRatio);
            }
        } else {
            srcW = maxScaleW;
            srcH = (srcH * withRatio);
        }

        return new int[]{(int) srcW, (int) srcH};
    }

    @Override
    public void onShowResult(@NonNull Bitmap bitmap) {
        this.cropBitmap = bitmap.copy(bitmap.getConfig(), false);

        this.binding.ivResultImg.setImageBitmap(bitmap);
        this.binding.ivResultImg.setDrawingCacheEnabled(true);
        Bitmap bmPresent = this.binding.ivResultImg.getDrawingCache();

        final int maxW = this.binding.getRoot().getWidth();
        final int maxH = this.binding.getRoot().getHeight();

        int[] size = this.getAdjustSize(bitmap);

        if (this.callback != null) {
            this.callback.onInitBitmapSucces(size[0], size[1]);
        }

        final int realW = size[0];
        final int realH = size[1];

        int maxVertextX = 0;
        int maxVertextY = 0;
        int minVertextX = 0;
        int minVertextY = 0;

        if (realW == maxW) {
            minVertextY = maxH / 2 - realH / 2;
            maxVertextX = maxW;
            maxVertextY = maxH / 2 + realH / 2;
        }

        if (realH == maxH) {
            minVertextX = maxW / 2 - realW / 2;
            minVertextY = 0;
            maxVertextX = maxW / 2 + realW / 2;
            maxVertextY = maxH;
        }

        this.widthOffset = (float) bitmap.getWidth() / realW;
        this.heightOffset = (float) bitmap.getHeight() / realH;

        this.binding.viewPolygon.setVertexCord(minVertextX, minVertextY, maxVertextX, maxVertextY);
        this.binding.viewPolygon.setMainBitmap(bmPresent);
        this.binding.viewPolygon.setInitedMatrix(false);
        this.binding.viewPolygon.setMainMatrix(this.binding.ivResultImg.getImageMatrix());

        if (this.isApplyCachePoint()) {
            return;
        }

        if (this.detectPresenter != null) {
            this.detectPresenter.detectRect(bmPresent);
        }
    }

    @NonNull
    private PointF[] getOrgMapPoint(PointF[] input) {
        PointF[] output = new PointF[]{new PointF(0, 0),
                new PointF(this.cropBitmap.getWidth(), 0),
                new PointF(0, this.cropBitmap.getHeight()),
                new PointF(this.cropBitmap.getWidth(), this.cropBitmap.getHeight())};

        int[] offset = this.getAdjustSize(this.cropBitmap);
        if (input != null) {
            for (int i = 0; i < input.length; i++) {
                output[i].x = (input[i].x - this.binding.getRoot().getWidth() / 2.0f + offset[0] / 2.0f) * this.widthOffset;
                output[i].y = (input[i].y - this.binding.getRoot().getHeight() / 2.0f + offset[1] / 2.0f) * this.heightOffset;
            }
        }
        return output;
    }

    private boolean isApplyCachePoint() {
        String cachePath = this.getSelectCachePath();
        if (TextUtils.isEmpty(cachePath)) {
            return false;
        }

        List<PointF> cachePoint;
        try {
            cachePoint = new Gson().fromJson(this.pageItem.getCacheVertex(), new TypeToken<List<PointF>>() {
            }.getType());
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            return false;
        }

        if (cachePoint == null) {
            return false;
        }

        this.binding.viewPolygon.setVisibility(View.VISIBLE);
        this.binding.viewPolygon.setPoints(cachePoint);
        this.cachePoint = this.binding.viewPolygon.getSelectPoint();
        this.binding.pbProcessDetect.setVisibility(View.INVISIBLE);
        return true;
    }

    public void setCallback(ProccesCropCallback callback) {
        this.callback = callback;
    }

    public void warpImage() {
        if (!this.binding.viewPolygon.isValidShape()) {
            if (this.callback != null) {
                this.callback.onDataPass(null);
            }
            return;
        }

        if (this.detectPresenter != null) {
            PointF[] selectPoint = this.binding.viewPolygon.getSelectPoint();
            if (this.binding.viewPolygon.getMaxVertextPoint().equals(Arrays.asList(selectPoint))) {
                this.detectPresenter.processWarpTransform(this.cropBitmap, Uri.parse(this.pageItem.getOrgUri()));
            } else {
                this.detectPresenter.processWarpTransform(this.cropBitmap, this.getOrgMapPoint(selectPoint), Uri.parse(this.pageItem.getOrgUri()));
            }
            this.writeSelectCache();
        }
    }

    @NonNull
    private String getSelectCachePath() {
        if (this.pageItem == null || TextUtils.isEmpty(this.pageItem.getCacheVertex())) {
            return "";
        }

        return this.pageItem.getCacheVertex();
    }

    private void writeSelectCache() {
        PointF[] cachePoint = this.binding.viewPolygon.getSelectPoint();
        if (cachePoint == null || this.pageItem == null) {
            return;
        }

        this.pageItem.setCacheVertex(new Gson().toJson(cachePoint));
    }

    public void setCropAll() {
        this.isClickMax = !this.isClickMax;
        if (!this.isClickMax) {
            if (this.cachePoint == null) {
                this.cachePoint = this.binding.viewPolygon.getSelectPoint();
            }
            this.binding.viewPolygon.setPoints(Arrays.asList(this.cachePoint));
            return;
        }

        this.binding.viewPolygon.setMaxVertextPoint();
    }

    @Override
    public void onDispose(Disposable disposable) {
        if (this.mDisposable == null) {
            this.mDisposable = new CompositeDisposable();
        }
        this.mDisposable.add(disposable);
    }

    @Override
    public void onDetectError() {
        this.binding.viewPolygon.setMaxVertextPoint();
        this.updateDetectStage();
    }

    @Override
    public void onShowDetectedPointF(@NonNull Map<Integer, PointF> pointFMap) {
        if (pointFMap.size() != 4) {
            this.binding.viewPolygon.setMaxVertextPoint();
        } else {
            this.binding.viewPolygon.setPoints(pointFMap);
        }

        this.updateDetectStage();
    }

    private void updateDetectStage() {
        this.cachePoint = this.binding.viewPolygon.getSelectPoint();
        this.binding.viewPolygon.setVisibility(View.VISIBLE);
        this.binding.pbProcessDetect.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onWarpError() {
        if (this.callback != null) {
            this.callback.onDataError();
        }
    }

    @Override
    public void onWarpTransformResult() {
        if (this.callback != null) {
            this.callback.onDataPass(this.pageItem);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.binding = null;
    }

    @Override
    public void onDetach() {
        if (this.cropBitmap != null) {
            this.cropBitmap.recycle();
        }

        if (this.mDisposable != null) {
            this.mDisposable.dispose();
        }

        super.onDetach();
    }

    @Override
    public void onVertextMove() {
        this.isClickMax = false;
        if (this.callback != null) {
            this.callback.onMoveSelectCrop();
        }
    }

    @Override
    public void onVertextStop() {
        this.cachePoint = this.binding.viewPolygon.getSelectPoint();
    }

    public interface ProccesCropCallback {

        void onInitBitmapSucces(int width, int height);

        void onMoveSelectCrop();

        void onDataPass(PageItem pageItem);

        void onDataError();
    }

}