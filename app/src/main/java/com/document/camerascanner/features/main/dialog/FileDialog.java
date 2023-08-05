package com.document.camerascanner.features.main.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.document.camerascanner.R;
import com.document.camerascanner.databinding.DialogOperationFileBinding;
import com.document.camerascanner.features.view.BaseDialog;

public class FileDialog extends BaseDialog {

    private boolean isCacelable;
    private boolean isShowPresent;

    private FileDialogListener listener;
    private DialogOperationFileBinding binding;

    public FileDialog(Context context) {
        super(context);
    }

    public void setListener(FileDialogListener listener) {
        this.listener = listener;
    }

    public String getInputContent() {
        String content = "";
        if (this.binding.etContent.getText() != null) {
            content = this.binding.etContent.getText().toString();
        }
        return content;
    }

    public FileDialog setImgPreseter(String path) {
        Glide.with(this.getContext())
                .asBitmap()
                .load(path)
                .thumbnail(0.75f)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .error(R.drawable.home_vector_folderlagre)
                .into(this.binding.ivDilaogPresent);
        return this;
    }

    public EditText getEdittext() {
        return this.binding.etContent;
    }

    public FileDialog setShowPresent(boolean showPresent) {
        this.isShowPresent = showPresent;
        return this;
    }

    public FileDialog setTitle(int textId) {
        this.binding.tvTitle.setText(textId);
        return this;
    }

    public FileDialog setTitle(CharSequence title) {
        this.binding.tvTitle.setText(title);
        return this;
    }

    public FileDialog setContent(int textId) {
        this.binding.etContent.setText(textId);
        this.binding.etContent.requestFocus();
        return this;
    }

    public FileDialog setContent(CharSequence text) {
        this.binding.etContent.setText(text);
        this.binding.etContent.requestFocus();
        return this;
    }

    @Override
    protected boolean isCancelable() {
        return this.isCacelable;
    }

    public FileDialog setCancelable(boolean isCancelable) {
        this.isCacelable = isCancelable;
        return this;
    }

    @Override
    protected View getView() {
        this.binding = DialogOperationFileBinding.inflate(LayoutInflater.from(this.getContext()));
        return this.binding.getRoot();
    }

    @Override
    public void onDialogCancel() {
        if (this.listener != null) {
            this.listener.onDismissDialog();
        }
    }

    @Override
    public void onShowing() {
        this.binding.ivDilaogPresent.setVisibility(this.isShowPresent ? View.VISIBLE : View.GONE);

        this.binding.ivCancel.setOnClickListener(view -> {
            if (this.listener != null) {
                this.listener.onClickCancel();
            }
        });

        this.binding.ivConfirm.setOnClickListener(view -> {
            if (this.listener != null) {
                this.listener.onClickConfirm();
            }
        });
    }

    public interface FileDialogListener {

        void onClickCancel();

        void onClickConfirm();

        void onDismissDialog();
    }
}
