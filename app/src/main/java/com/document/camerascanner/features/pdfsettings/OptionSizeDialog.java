package com.document.camerascanner.features.pdfsettings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.document.camerascanner.R;
import com.document.camerascanner.databinding.DialogPdfSizeBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class OptionSizeDialog extends BottomSheetDialogFragment {

    private final int type;

    private ImageView[] arrIv;
    private DialogPdfSizeBinding binding;
    private DialogOptionSizeListener listener;

    public OptionSizeDialog(int type) {
        this.type = type;
    }

    public void setListener(DialogOptionSizeListener listener) {
        this.listener = listener;
    }

    @Override
    public int getTheme() {
        return R.style.BottomDialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.binding = DialogPdfSizeBinding.inflate(inflater, container, false);
        return this.binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.arrIv = new ImageView[]{this.binding.ivLetter, this.binding.ivA4, this.binding.ivLegal, this.binding.ivA3, this.binding.ivA5, this.binding.ivBc};
        this.updateSelectPosition(this.type);

        this.binding.clLetter.setOnClickListener(view1 -> {
            if (this.listener != null) {
                this.listener.onClickLetter();
            }
            this.dismiss();
        });

        this.binding.clA4.setOnClickListener(view1 -> {
            if (this.listener != null) {
                this.listener.onClickA4();
            }
            this.dismiss();
        });

        this.binding.clLegal.setOnClickListener(view1 -> {
            if (this.listener != null) {
                this.listener.onClickLegal();
            }
            this.dismiss();
        });

        this.binding.clA3.setOnClickListener(view1 -> {
            if (this.listener != null) {
                this.listener.onClickA3();
            }
            this.dismiss();
        });

        this.binding.clA5.setOnClickListener(view1 -> {
            if (this.listener != null) {
                this.listener.onClickA5();
            }
            this.dismiss();
        });

        this.binding.clBc.setOnClickListener(view1 -> {
            if (this.listener != null) {
                this.listener.onClickBussinesCard();
            }
            this.dismiss();
        });
    }

    public void updateSelectPosition(int position) {
        if (this.arrIv == null) {
            return;
        }

        for (int i = 0; i < this.arrIv.length; i++) {
            if (this.arrIv[i] != null) {
                this.arrIv[i].setSelected(i == position - 1);
            }
        }
    }

    public interface DialogOptionSizeListener {

        void onClickLetter();

        void onClickA4();

        void onClickLegal();

        void onClickA3();

        void onClickA5();

        void onClickBussinesCard();
    }
}
