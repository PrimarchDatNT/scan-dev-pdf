package com.document.camerascanner.features.update;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.document.camerascanner.databinding.DialogUpdateBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class UpdateDialog extends BottomSheetDialogFragment {

    private DialogUpdateBinding binding;
    private DialogUpdateListener listener;

    public void setListener(DialogUpdateListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.binding = DialogUpdateBinding.inflate(inflater, container, false);
        return this.binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.binding.btnUpdate.setOnClickListener(view1 -> {
            if (this.listener != null) {
                this.listener.onClickUpdate();
            }
        });
    }

    public interface DialogUpdateListener {
        void onClickUpdate();
    }
}
