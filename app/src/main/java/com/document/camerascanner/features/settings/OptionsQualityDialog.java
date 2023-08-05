package com.document.camerascanner.features.settings;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.document.camerascanner.R;
import com.document.camerascanner.databinding.DialogOptionQualityBinding;
import com.document.camerascanner.features.share.TypeQuality;
import com.document.camerascanner.prefs.AppPref;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class OptionsQualityDialog extends BottomSheetDialogFragment {

    private final Context context;
    private DialogOptionQualityBinding binding;
    private CallBackSelectQuality callBackSelectQuality;

    public OptionsQualityDialog(Context context) {
        this.context = context;
    }

    public void setCallBackSelectQuality(CallBackSelectQuality callBackSelectQuality) {
        this.callBackSelectQuality = callBackSelectQuality;
    }

    @Override
    public int getTheme() {
        return R.style.BottomDialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.binding = DialogOptionQualityBinding.inflate(inflater, container, false);
        initView();
        return this.binding.getRoot();
    }

    private void initView() {
        AppPref appPref = AppPref.getInstance(this.context);
        int quality = appPref.getQualityType();
        switch (quality) {
            case TypeQuality.HIGH:
                this.binding.rbHight.setChecked(true);
                break;
            case TypeQuality.LOW:
                this.binding.rbLow.setChecked(true);
                break;
            case TypeQuality.MAX:
                this.binding.rbMax.setChecked(true);
                break;
            case TypeQuality.MEDIUM:
                this.binding.rbMedium.setChecked(true);
                break;
        }

        this.binding.rbMedium.setOnClickListener(v -> {
            appPref.setQualityType(TypeQuality.MEDIUM);
            this.callBackQuality(R.string.all_share_quality_medium);
        });

        this.binding.rbMax.setOnClickListener(v -> {
            appPref.setQualityType(TypeQuality.MAX);
            this.callBackQuality(R.string.all_share_quality_max);
        });

        this.binding.rbLow.setOnClickListener(v -> {
            appPref.setQualityType(TypeQuality.LOW);
            this.callBackQuality(R.string.all_share_quality_low);
        });

        this.binding.rbHight.setOnClickListener(v -> {
            appPref.setQualityType(TypeQuality.HIGH);
            this.callBackQuality(R.string.all_share_quality_high);
        });

    }

    public void callBackQuality(int qualityInt) {
        if (callBackSelectQuality != null) {
            callBackSelectQuality.callbackQuality(getString(qualityInt));
            this.dismiss();
        }
    }

    public interface CallBackSelectQuality {
        void callbackQuality(String quality);
    }
}
