package com.document.camerascanner.features.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.document.camerascanner.R;
import com.document.camerascanner.databinding.DialogFilterModeBinding;
import com.document.camerascanner.prefs.AppPref;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class OptionsFilterDialog extends BottomSheetDialogFragment {

    private ImageView[] imageViewList;
    private DialogFilterModeBinding binding;
    private DialogOptionFilterListener filterListener;

    public void setFilterListener(DialogOptionFilterListener filterListener) {
        this.filterListener = filterListener;
    }

    @Override
    public int getTheme() {
        return R.style.BottomDialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.binding = DialogFilterModeBinding.inflate(inflater, container, false);
        return this.binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.imageViewList = new ImageView[]{this.binding.ivOriginalMode,
                this.binding.ivGrayScaleModeButton,
                this.binding.ivMagicColorButton,
                this.binding.ivBlackAndWhiteButton,
                this.binding.ivBnw2,
                this.binding.ivNoShadow,
                this.binding.ivLastUsedFilterButton};

        this.updateSelectedPosition(AppPref.getInstance(this.getContext()).getDefaultFilterOption());

        this.binding.clOriginal.setOnClickListener(view1 -> {
            if (this.filterListener != null) {
                this.filterListener.onClickOriginal();
            }
            this.dismiss();
        });

        this.binding.clBlackAndWhite.setOnClickListener(view1 -> {
            if (this.filterListener != null) {
                this.filterListener.onClickBlackAndWhite();
            }
            this.dismiss();
        });

        this.binding.clGrayScale.setOnClickListener(view1 -> {
            if (this.filterListener != null) {
                this.filterListener.onClickGrayScale();
            }
            this.dismiss();
        });

        this.binding.clMagicColor.setOnClickListener(view1 -> {
            if (this.filterListener != null) {
                this.filterListener.onClickMagicColor();
            }
            this.dismiss();
        });

        this.binding.clBnws2.setOnClickListener(view1 -> {
            if (this.filterListener != null) {
                this.filterListener.onClickBlackAndWhite2();
            }
            this.dismiss();
        });

        this.binding.clNoShadow.setOnClickListener(view1 -> {
            if (this.filterListener != null) {
                this.filterListener.onClickNoShadow();
            }
            this.dismiss();
        });

        this.binding.clLastUsedFilter.setOnClickListener(view1 -> {
            if (this.filterListener != null) {
                this.filterListener.onClickLastUsedFilter();
            }
            this.dismiss();
        });
    }

    private void updateSelectedPosition(int position) {
        if (this.imageViewList == null) {
            return;
        }

        for (int i = 0; i < this.imageViewList.length; i++) {
            if (this.imageViewList[i] != null) {
                this.imageViewList[i].setSelected(i == position);
            }
        }
    }

    interface DialogOptionFilterListener {

        void onClickOriginal();

        void onClickBlackAndWhite();

        void onClickBlackAndWhite2();

        void onClickNoShadow();

        void onClickMagicColor();

        void onClickGrayScale();

        void onClickLastUsedFilter();
    }
}
