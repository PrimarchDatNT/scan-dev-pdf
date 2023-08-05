package com.document.camerascanner.features.share;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.document.camerascanner.R;
import com.document.camerascanner.databinding.DialogShareNewBinding;
import com.document.camerascanner.prefs.AppPref;
import com.document.camerascanner.utils.AppUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class ShareDialogNew extends BottomSheetDialogFragment {

    private final String title;
    private int quality;
    private final Context context;
    private final AppPref appPref;
    private DialogShareNewBinding binding;
    private DialogShareListener listener;


    public ShareDialogNew(Context context, String title) {
        this.context = context;
        this.appPref = AppPref.getInstance(context);
        this.title = title;
    }

    public void setListener(DialogShareListener dialogShareListener) {
        this.listener = dialogShareListener;
    }

    @Override
    public int getTheme() {
        return R.style.BottomDialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.binding = DialogShareNewBinding.inflate(inflater, container, false);
        this.initView();
        this.initConfig();
        return this.binding.getRoot();
    }

    private void initView() {
        this.binding.etRename.setText(this.title);
        this.binding.tvRename.setText(this.title);
        if (TextUtils.isEmpty(this.title)) {
            hideRename();
        }
        this.quality = this.appPref.getQualityType();
        switch (this.quality) {
            case TypeQuality.LOW:
                this.binding.rbLow.setChecked(true);
                break;
            case TypeQuality.MEDIUM:
                this.binding.rbMedium.setChecked(true);
                break;
            case TypeQuality.HIGH:
                this.binding.rbHight.setChecked(true);
                break;
            case TypeQuality.MAX:
                this.binding.rbMax.setChecked(true);
                break;
        }

    }

    public void hideRename() {
        this.binding.etRename.setVisibility(View.GONE);
        this.binding.tvRename.setVisibility(View.GONE);
        this.binding.ivRename.setVisibility(View.GONE);
        this.binding.etRename.setText(null);
    }

    public void initConfig() {
        this.binding.ivRename.setOnClickListener(v -> {
            this.binding.tvRename.setVisibility(View.GONE);
            this.binding.etRename.setVisibility(View.VISIBLE);
            this.binding.etRename.requestFocus();
            this.binding.etRename.setSelection(this.binding.tvRename.length());
            AppUtils.showKeyboard(this.context);
        });

        this.binding.rbHight.setOnClickListener(v -> this.quality = TypeQuality.HIGH);

        this.binding.rbMax.setOnClickListener(v -> this.quality = TypeQuality.MAX);

        this.binding.rbMedium.setOnClickListener(v -> this.quality = TypeQuality.MEDIUM);

        this.binding.rbLow.setOnClickListener(v -> this.quality = TypeQuality.LOW);


        this.binding.tvPdfSettings.setOnClickListener(v -> {
            if (this.listener != null) {
                this.listener.onClickToPdfSettings();
            }
        });

        this.binding.llSaveJpg.setOnClickListener(v -> {
            if (this.isEmptyText()) {
                return;
            }
            if (this.listener != null) {
                this.listener.onClickSaveJpg();
                this.dismiss();
            }
        });

        this.binding.llSavePdf.setOnClickListener(v -> {
            if (this.isEmptyText()) {
                return;
            }
            if (this.listener != null) {
                this.listener.onClickSavePdf();
                this.dismiss();
            }
        });

        this.binding.llSharePdf.setOnClickListener(v -> {
            if (this.isEmptyText()) {
                return;
            }
            if (this.listener != null) {
                this.listener.onClickSharePdf();
                this.dismiss();
            }
        });

        this.binding.llShareJpg.setOnClickListener(v -> {
            if (this.isEmptyText()) {
                return;
            }
            if (this.listener != null) {
                this.listener.onClickShareJpg();
                this.dismiss();
            }
        });
    }

    private boolean isEmptyText() {
        if (TextUtils.isEmpty(this.binding.etRename.getText()) && !TextUtils.isEmpty(this.title)) {
            Toast.makeText(this.context, R.string.home_folder_et_empty, Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (this.listener != null) {
            this.listener.onDismissDialog();
        }
    }

    public int getQuality() {
        return this.quality;
    }

    public String getTitle() {
        return this.binding.etRename.getText().toString();
    }

    public interface DialogShareListener {

        void onClickShareJpg();

        void onClickSharePdf();

        void onClickSavePdf();

        void onClickSaveJpg();

        void onClickToPdfSettings();

        void onDismissDialog();

    }
}
