package com.document.camerascanner.features.save.dialog;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;

import com.document.camerascanner.databinding.DialogSaveFileBinding;
import com.document.camerascanner.features.view.BaseDialog;

public class SaveSingleDialog extends BaseDialog {

    private boolean isCancelable;

    private DialogOnClickListener listener;

    private DialogSaveFileBinding binding;

    public SaveSingleDialog(Context context) {
        super(context);
    }

    public SaveSingleDialog setDocumentName(String documentName) {
        if (documentName != null && !TextUtils.isEmpty(documentName)) {
            this.binding.etSaveDocument.setText(documentName);
            this.binding.etSaveDocument.setSelection(documentName.length());
        }
        return this;
    }

    public String getInputContent(){
        String content = "";
        if (this.binding.etSaveDocument.getText() != null) {
            content = this.binding.etSaveDocument.getText().toString();
        }
        return content;
    }

    public void setListener(DialogOnClickListener listener) {
        this.listener = listener;
    }

    @Override
    protected boolean isCancelable() {
        return this.isCancelable;
    }


    public SaveSingleDialog setCancelable(boolean isCancelable) {
        this.isCancelable = isCancelable;
        return this;
    }

    @Override
    protected View getView() {
        this.binding = DialogSaveFileBinding.inflate(LayoutInflater.from(this.getContext()));
        return this.binding.getRoot();
    }

    @Override
    public void onDialogCancel() {
    }

    @Override
    public void onShowing() {
        this.binding.cbAutoSave.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (this.listener != null) {
                this.listener.onCheckedChange(isChecked);
            }
        });

        this.binding.tvNegativeButton.setOnClickListener(v -> {
            if (this.listener != null) {
                this.listener.onCancel();
            }
        });

        this.binding.tvPositiveButton.setOnClickListener(view -> {
            if (this.listener != null) {
                this.listener.onAccept();
            }
        });
    }

    public interface DialogOnClickListener {

        void onCheckedChange(boolean isChecked);

        void onCancel();

        void onAccept();
    }
}
