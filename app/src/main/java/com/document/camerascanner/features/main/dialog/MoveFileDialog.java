package com.document.camerascanner.features.main.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.document.camerascanner.R;
import com.document.camerascanner.databases.model.BaseEntity;
import com.document.camerascanner.databinding.DialogHomeMoveFileBinding;
import com.document.camerascanner.features.movefile.MoveFileAdapter;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public class MoveFileDialog extends BottomSheetDialogFragment {

    private final List<BaseEntity> listData;

    private int titleResId;

    private MoveFileDialogListener listener;
    private DialogHomeMoveFileBinding binding;

    public MoveFileDialog(List<BaseEntity> listData) {
        this.listData = listData;
    }

    public void setListener(MoveFileDialogListener listener) {
        this.listener = listener;
    }

    public void setTitle(int resId) {
        this.titleResId = resId;
    }

    @Override
    public int getTheme() {
        return R.style.BottomDialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.binding = DialogHomeMoveFileBinding.inflate(inflater, container, false);
        return this.binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.binding.tvTitle.setText(this.titleResId);

        MoveFileAdapter moveFileAdapter = new MoveFileAdapter(this.getContext(), this.listData);

        moveFileAdapter.setCallBackSelectFolder(new MoveFileAdapter.CallBackSelectFolder() {
            @Override
            public void onClickItem(int position) {
                if (listener != null) {
                    listener.onClickItem(position, listData.get(position));
                }
            }

            @Override
            public void onClickCreateNew() {
                if (listener != null) {
                    listener.onCreateFolder();
                    dismiss();
                }
            }
        });

        this.binding.rvFolderMove.setLayoutManager(new LinearLayoutManager(this.getContext()));
        this.binding.rvFolderMove.setAdapter(moveFileAdapter);
    }

    @Override
    public void onDetach() {
        if (this.listener != null) {
            this.listener.onDismissDialog();
        }
        super.onDetach();
    }

    public interface MoveFileDialogListener {

        void onCreateFolder();

        void onClickItem(int position, BaseEntity baseEntity);

        void onDismissDialog();
    }
}
