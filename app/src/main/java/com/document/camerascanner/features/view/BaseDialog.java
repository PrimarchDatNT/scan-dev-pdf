package com.document.camerascanner.features.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.Window;

public abstract class BaseDialog {

    protected final View view;

    private final Context context;
    private final AlertDialog.Builder mDialogBuilder;

    private AlertDialog mDialog;

    public BaseDialog(Context context) {
        this.context = context;
        this.mDialogBuilder = new AlertDialog.Builder(context);
        this.mDialogBuilder.setCancelable(isCancelable());
        this.mDialogBuilder.setOnCancelListener(dialogInterface -> this.onDialogCancel());
        this.view = this.getView();
    }

    protected abstract boolean isCancelable();

    public BaseDialog show() {
        ((Activity) this.getContext()).runOnUiThread(() -> {
            this.mDialogBuilder.setView(this.view);
            if (this.mDialog == null) {
                this.mDialog = this.mDialogBuilder.create();
                this.mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                this.mDialog.setCanceledOnTouchOutside(isCancelable());
                this.mDialog.setCancelable(isCancelable());
            }

            if (this.mDialog.getWindow() != null) {
                this.mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            if (!((Activity) this.context).isFinishing()) {
                this.mDialog.show();
                this.onShowing();
            }
        });
        return this;
    }

    public void dimiss() {
        if (this.mDialog != null) {
            this.mDialog.dismiss();
        }
    }

    protected abstract View getView();

    public abstract void onDialogCancel();

    public Context getContext() {
        return this.context;
    }

    public abstract void onShowing();
}
