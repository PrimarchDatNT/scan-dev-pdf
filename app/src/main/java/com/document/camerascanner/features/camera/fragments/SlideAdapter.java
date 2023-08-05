package com.document.camerascanner.features.camera.fragments;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.MediaStoreSignature;
import com.document.camerascanner.R;
import com.document.camerascanner.databinding.ItemViewPagerDetailBinding;

import java.io.File;
import java.util.List;

public class SlideAdapter extends RecyclerView.Adapter<SlideAdapter.SlideViewHolder> {

    private final Context context;
    private List<Uri> uriList;

    public SlideAdapter(Context context, List<Uri> uriList) {
        this.context = context;
        this.uriList = uriList;
    }

    public void setUriList(List<Uri> uriList) {
        this.uriList = uriList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SlideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SlideViewHolder(ItemViewPagerDetailBinding.inflate(LayoutInflater.from(this.context), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull SlideViewHolder holder, int position) {
        Uri uri = this.uriList.get(position);
        holder.onBind(uri);
    }

    @Override
    public int getItemCount() {
        return this.uriList == null ? 0 : this.uriList.size();
    }

    class SlideViewHolder extends RecyclerView.ViewHolder {

        private final ItemViewPagerDetailBinding binding;

        public SlideViewHolder(@NonNull ItemViewPagerDetailBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void onBind(Uri uri) {
            if (uri == null) {
                return;
            }
            File file = new File(uri.getPath());
            Glide.with(context)
                    .load(file)
                    .error(R.drawable.all_loading_place_holder)
                    .signature(new MediaStoreSignature(null, file.lastModified(), 0))
                    .into(this.binding.ivPage);
        }
    }
}
