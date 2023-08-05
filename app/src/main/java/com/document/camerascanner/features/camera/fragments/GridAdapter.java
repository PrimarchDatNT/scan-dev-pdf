package com.document.camerascanner.features.camera.fragments;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.MediaStoreSignature;
import com.document.camerascanner.R;
import com.document.camerascanner.databinding.ItemSaveDocumentBinding;

import java.io.File;
import java.util.List;

public class GridAdapter extends RecyclerView.Adapter<GridAdapter.GridViewHolder> {

    private Context context;
    private List<Uri> uris;
    private int positionCurrent = -1;

    private CallBackPosition callBackPosition;

    public GridAdapter(Context context, List<Uri> uris) {
        this.context = context;
        this.uris = uris;
    }

    public void setCallBackPosition(CallBackPosition callBackPosition) {
        this.callBackPosition = callBackPosition;
    }

    public void setPositionCurrent(int positionCurrent) {
        this.positionCurrent = positionCurrent;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GridViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new GridViewHolder(ItemSaveDocumentBinding.inflate(LayoutInflater.from(this.context), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull GridViewHolder holder, int position) {
        Uri uri = this.uris.get(position);
        holder.onBind(uri, position);
    }

    @Override
    public int getItemCount() {
        return this.uris == null ? 0 : this.uris.size();
    }

    class GridViewHolder extends RecyclerView.ViewHolder {

        private final ItemSaveDocumentBinding binding;

        public GridViewHolder(@NonNull ItemSaveDocumentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void onBind(Uri uri, int position) {
            if (uri == null) {
                return;
            }

            this.binding.pbLoadingFilter.setVisibility(View.GONE);
            this.binding.tvNumberCounter.setText(String.valueOf(position + 1));

            boolean isPositionCurrent = positionCurrent == position;
            this.binding.ivSelect.setVisibility(isPositionCurrent ? View.VISIBLE : View.GONE);
            this.binding.viewBackground.setVisibility(isPositionCurrent ? View.VISIBLE : View.GONE);

            File file = new File(uri.getPath());
            Glide.with(context)
                    .load(file)
                    .error(R.drawable.all_loading_place_holder)
                    .signature(new MediaStoreSignature(null, file.lastModified(), 0))
                    .into(this.binding.ivDocumentItem);

            this.binding.ivDocumentItem.setOnClickListener(v -> {
                if (callBackPosition != null) {
                    callBackPosition.callBackPosition(position);
                }
            });

            this.binding.ivDeleteIcon.setOnClickListener(v -> {
                if (callBackPosition != null) {
                    callBackPosition.callBackPositionDelete(position);
                }
            });
        }
    }

    public interface CallBackPosition {
        void callBackPosition(int position);

        void callBackPositionDelete(int position);
    }
}
