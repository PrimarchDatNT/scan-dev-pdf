package com.document.camerascanner.features.view;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;

import com.document.camerascanner.databinding.DialogCustomizeBinding;

public class CustomisedDialog extends BaseDialog {

    private DialogCustomizeBinding binding;

    private DialogOnClickListener listener;

    private boolean isCancelable;

    public CustomisedDialog(Context context) {
        super(context);
    }

    @Override
    protected boolean isCancelable() {
        return this.isCancelable;
    }

    public CustomisedDialog setCancelable(boolean isCancelable) {
        this.isCancelable = isCancelable;
        return this;
    }

    @Override
    protected View getView() {
        this.binding = DialogCustomizeBinding.inflate(LayoutInflater.from(this.getContext()));
        return this.binding.getRoot();
    }

    @Override
    public void onDialogCancel() {
    }

    @Override
    public void onShowing() {
        this.binding.tvPositiveButton.setVisibility(TextUtils.isEmpty(this.binding.tvPositiveButton.getText()) ? View.INVISIBLE : View.VISIBLE);
        this.binding.tvNegativeButton.setVisibility(TextUtils.isEmpty(this.binding.tvNegativeButton.getText()) ? View.INVISIBLE : View.VISIBLE);

        this.binding.tvNegativeButton.setOnClickListener(view -> {
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

    public CustomisedDialog setTitle(int textId) {
        this.binding.tvTitle.setText(textId);
        return this;
    }

    public CustomisedDialog setTitle(CharSequence title) {
        this.binding.tvTitle.setText(title);
        return this;
    }

    public CustomisedDialog setMessage(int textId) {
        this.binding.tvMessage.setText(textId);
        return this;
    }

    public CustomisedDialog setMessage(CharSequence message) {
        this.binding.tvMessage.setText(message);
        return this;
    }

    public CustomisedDialog setButtonAllowText(int textId) {
        this.binding.tvPositiveButton.setText(textId);
        return this;
    }

    public CustomisedDialog setButtonAllowText(CharSequence allowText) {
        this.binding.tvPositiveButton.setText(allowText);
        return this;
    }

    public CustomisedDialog setButtonCancelText(int textId) {
        this.binding.tvNegativeButton.setText(textId);
        return this;
    }

    public CustomisedDialog setButtonCancelText(CharSequence cancelText) {
        this.binding.tvNegativeButton.setText(cancelText);
        return this;
    }

    public void setListener(DialogOnClickListener listener) {
        this.listener = listener;
    }

    public interface DialogOnClickListener {
        void onCancel();

        void onAccept();
    }
}
