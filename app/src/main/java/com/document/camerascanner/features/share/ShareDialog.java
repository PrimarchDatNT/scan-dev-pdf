package com.document.camerascanner.features.share;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.document.camerascanner.R;
import com.document.camerascanner.databinding.DialogShareBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class ShareDialog extends BottomSheetDialogFragment {

    private final String size;

    private DialogShareBinding binding;
    private DialogShareListener listener;

    public ShareDialog(String size) {
        this.size = size;
    }

    public void setListener(DialogShareListener listener) {
        this.listener = listener;
    }

    @Override
    public int getTheme() {
        return R.style.BottomDialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.binding = DialogShareBinding.inflate(inflater, container, false);
        return this.binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.binding.tvSizePdf.setText(this.size);
        this.binding.tvSizeJpg.setText(this.size);

        this.binding.llJpgSaveGallery.setOnClickListener(view1 -> {
            if (this.listener != null) {
                this.listener.onClickExportGallery();
            }
        });

        this.binding.llJpgShare.setOnClickListener(view12 -> {
            if (this.listener != null) {
                this.listener.onClickShare();
            }
            this.dismiss();
        });

        this.binding.llToPdf.setOnClickListener(view13 -> {
            if (this.listener != null) {
                this.listener.onClickToPdf();
            }
            this.dismiss();
        });
    }

    @Override
    public void onDetach() {
        if (this.listener != null) {
            this.listener.onDismissDialog();
        }
        super.onDetach();
    }

    public interface DialogShareListener {

        void onClickExportGallery();

        void onClickShare();

        void onClickToPdf();

        void onDismissDialog();
    }
}
