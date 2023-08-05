package com.document.camerascanner.features.share;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.document.camerascanner.R;
import com.document.camerascanner.databinding.DialogPdfSuccessBinding;
import com.document.camerascanner.utils.ImageUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;
import java.util.List;

public class ShareSuccessDialog extends BottomSheetDialogFragment {

    private final Context context;
    private DialogPdfSuccessBinding binding;
    private final List<String> listPdf;
    private String locationRoot;
    private String title;
    private boolean isPdfShare;

    public ShareSuccessDialog(Context context, List<String> listPdf, String title) {
        this.context = context;
        this.listPdf = listPdf;
        this.title = title;
        this.isPdfShare = TextUtils.equals(this.title, context.getString(R.string.pdf_success));
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setTitle(int title) {
        this.title = this.context.getString(title);
    }

    @Override
    public int getTheme() {
        return R.style.BottomDialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.binding = DialogPdfSuccessBinding.inflate(inflater, container, false);
        this.initView();
        this.setListener();
        return this.binding.getRoot();
    }

    public void initView() {
        if (this.listPdf.size() == 0) {
            dismiss();
            return;

        }
        if (this.listPdf.size() == 1) {
            this.binding.tvLocation.setText(this.context.getString(R.string.location_pdf, this.listPdf.get(0)));
            return;
        }
        this.binding.tvSuccess.setText(this.title);

        this.locationRoot = this.listPdf.get(0);
        this.locationRoot = this.locationRoot.substring(0, this.locationRoot.lastIndexOf("/"));
        this.binding.tvLocation.setText(this.context.getString(R.string.location_pdf, locationRoot));
        this.binding.tvOpen.setEnabled(false);
        this.binding.tvShowInFolder.setText(this.isPdfShare ? R.string.pdf_show_pdf : R.string.jpg_show_jpg);
    }

    public void setListener() {
        this.binding.tvOpen.setOnClickListener(v -> {
            Uri uri = FileProvider.getUriForFile(this.context, this.context.getApplicationContext().getPackageName() + ".provider", new File(listPdf.get(0)));
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            this.context.startActivity(Intent.createChooser(intent, "Select the app to open"));
            this.dismiss();
        });

        this.binding.tvShowInFolder.setOnClickListener(v -> {
            ImageUtils.shareToShare(this.context, this.listPdf);
            this.dismiss();
        });
    }

}
