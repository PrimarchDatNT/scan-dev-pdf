package com.document.camerascanner.features.camera.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.document.camerascanner.R;
import com.document.camerascanner.databinding.FragmentCameraDetailImageBinding;
import com.document.camerascanner.features.intro.PreviewIntro;
import com.document.camerascanner.prefs.AppPref;
import com.document.camerascanner.prefs.ConstantsPrefs;
import com.document.camerascanner.utils.FileUtils;

import java.util.List;

public class PreviewCameraFragment extends Fragment implements GridAdapter.CallBackPosition {

    private List<Uri> uriList;
    private PreviewCameraCallback callback;

    private boolean isShowIntro;
    private boolean isShowGrid;
    private int positionCurrent = -1;

    private PreviewIntro previewIntro;
    private FragmentCameraDetailImageBinding binding;
    private GridAdapter gridAdapter;
    private SlideAdapter slideAdapter;

    public PreviewCameraFragment(List<Uri> list, PreviewCameraCallback previewCameraCallback) {
        this.uriList = list;
        this.callback = previewCameraCallback;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.binding = FragmentCameraDetailImageBinding.inflate(inflater, container, false);
        return this.binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.initIntro();

        this.binding.ivBack.setOnClickListener(v -> {
            if (this.isShowIntro) {
                return;
            }

            if (this.callback != null) {
                this.callback.onBackDetail();
            }
        });

        this.binding.ivTickDone.setOnClickListener(v -> {
            if (this.isShowIntro) {
                return;
            }

            if (this.callback != null) {
                this.callback.onTickDone();
            }
        });

        this.binding.ivRecapture.setOnClickListener(v -> {
            if (this.isShowIntro) {
                return;
            }

            if (this.positionCurrent == -1) {
                Toast.makeText(this.getContext(), R.string.camera_preview_select_empty, Toast.LENGTH_SHORT).show();
                return;
            }
            if (this.callback != null) {
                this.callback.onReCapture(positionCurrent);
            }
        });

        this.initView();
    }

    private void initIntro() {
        if (!AppPref.getInstance(this.getActivity()).isShowSpotlight(ConstantsPrefs.KEY_SHOW_SPOTLIGHT_CAMERA_PREVIEW)) {
            this.isShowIntro = false;
            if (this.callback != null) {
                this.callback.onShowPreviewIntro(false);
            }
            return;
        }

        this.isShowIntro = true;
        if (this.callback != null) {
            this.callback.onShowPreviewIntro(true);
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            this.previewIntro = new PreviewIntro(this.getActivity(), this.binding.getRoot())
                    .setReCaptureAnchor(this.binding.ivRecaptureSpotlight)

                    .setConfirmAnchor(this.binding.ivTickDoneSpolight)
                    .setDeleteAnchor(this.binding.ivDeleteSpotlight)
                    .setTypeShowAnchor(this.binding.ivTypePreview)
                    .setCallback(() -> {
                        this.isShowIntro = false;
                        if (this.callback != null) {
                            this.callback.onShowPreviewIntro(false);
                        }
                    });

            this.previewIntro.showGuilde();
        }, 500);
    }

    public void finishIntro() {
        if (this.previewIntro != null) {
            this.previewIntro.finishGuilde();
        }
    }

    private void initView() {
        this.slideAdapter = new SlideAdapter(getContext(), this.uriList);
        this.binding.vpSlidePage.setAdapter(slideAdapter);
        this.binding.vpSlidePage.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                positionCurrent = position;
                setStatePosition();
            }
        });
        this.setStatePosition();

        this.gridAdapter = new GridAdapter(getContext(), this.uriList);
        gridAdapter.setCallBackPosition(this);
        this.binding.rvPreviewGrid.setAdapter(gridAdapter);
        this.binding.ivTypePreview.setOnClickListener(this.onClickChangeShow);
        this.binding.ivDelete.setOnClickListener(this.onClickDeleteView);
    }

    private void setStatePosition() {
        this.binding.tvPosition.setText(getContext().getString(R.string.camera_preview_position, this.positionCurrent + 1, this.uriList.size()));
        this.binding.tvPosition.setVisibility(this.isShowGrid ? View.GONE : View.VISIBLE);
    }

    View.OnClickListener onClickChangeShow = v -> {
        this.positionCurrent = -1;
        this.gridAdapter.setPositionCurrent(this.positionCurrent);
        this.isShowGrid = !this.isShowGrid;
        this.binding.vpSlidePage.setVisibility(this.isShowGrid ? View.GONE : View.VISIBLE);
        this.binding.rvPreviewGrid.setVisibility(this.isShowGrid ? View.VISIBLE : View.GONE);
        this.binding.ivTypePreview.setImageResource(this.isShowGrid ? R.drawable.camera_vector_preview_slide : R.drawable.camera_vector_preview_grid);
        this.binding.ivDelete.setVisibility(this.isShowGrid ? View.GONE : View.VISIBLE);
        this.binding.ivDeleteSpotlight.setVisibility(this.isShowGrid ? View.GONE : View.VISIBLE);
        this.positionCurrent = this.isShowGrid ? -1 : this.binding.vpSlidePage.getCurrentItem();
        this.setStatePosition();

    };

    View.OnClickListener onClickDeleteView = v -> {
        if (this.positionCurrent == -1) {
            return;
        }
        FileUtils.deleteFile(this.uriList.get(this.positionCurrent));
        this.uriList.remove(this.positionCurrent);
        this.gridAdapter.notifyDataSetChanged();
        this.slideAdapter.setUriList(this.uriList);
        this.positionCurrent = this.binding.vpSlidePage.getCurrentItem();
        this.setStatePosition();
        if (callback != null) {
            callback.onDeletePage();
        }
    };

    @Override
    public void callBackPosition(int position) {
        this.positionCurrent = position;
        this.gridAdapter.setPositionCurrent(this.positionCurrent);
    }

    @Override
    public void callBackPositionDelete(int position) {
        this.positionCurrent = -1;
        this.gridAdapter.setPositionCurrent(positionCurrent);
        FileUtils.deleteFile(this.uriList.get(position));
        this.uriList.remove(position);
        this.gridAdapter.notifyDataSetChanged();
        this.slideAdapter.setUriList(this.uriList);
        this.setStatePosition();
        if (this.callback != null) {
            this.callback.onDeletePage();
        }
    }

    public interface PreviewCameraCallback {

        void onShowPreviewIntro(boolean isShow);

        void onBackDetail();

        void onReCapture(int position);

        void onTickDone();

        void onDeletePage();
    }


}
