package com.document.camerascanner.features.save;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.document.camerascanner.R;
import com.document.camerascanner.databases.model.PageItem;
import com.document.camerascanner.databinding.ItemSaveDocumentBinding;

import java.util.List;

public class PageItemAdapter extends RecyclerView.Adapter<PageItemAdapter.PageItemViewHolder> {

    private final Context context;

    private List<PageItem> listItem;
    private OnDeleteItemListener deleteListener;
    private OnItemClickListener itemClickListener;

    public PageItemAdapter(Context context) {
        this.context = context;
    }

    public void setListItem(List<PageItem> listItem) {
        this.listItem = listItem;
        this.notifyDataSetChanged();
    }

    public void setDeleteListener(OnDeleteItemListener deleteListener) {
        this.deleteListener = deleteListener;
    }

    public void setItemClickListener(OnItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    @NonNull
    @Override
    public PageItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new PageItemViewHolder(ItemSaveDocumentBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull PageItemViewHolder holder, int position) {
        PageItem item = this.listItem.get(position);
        if (item == null) {
            return;
        }

        holder.bindView(item);
        holder.binding.ivDeleteIcon.setOnClickListener(view -> {
            if (this.deleteListener != null) {
                this.deleteListener.onClickDelete(position);
            }
        });

        holder.binding.clItemSaveDocument.setOnClickListener(view -> {
            if (this.itemClickListener != null) {
                this.itemClickListener.onItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return this.listItem == null ? 0 : this.listItem.size();
    }

    public interface OnItemClickListener {

        void onItemClick(int postion);
    }

    public interface OnDeleteItemListener {

        void onClickDelete(int position);
    }

    public class PageItemViewHolder extends RecyclerView.ViewHolder {

        private final ItemSaveDocumentBinding binding;

        public PageItemViewHolder(@NonNull ItemSaveDocumentBinding viewBinding) {
            super(viewBinding.getRoot());
            this.binding = viewBinding;
        }

        public void bindView(PageItem pageItem) {
            if (pageItem == null) {
                return;
            }

            Glide.with(context)
                    .asBitmap()
                    .skipMemoryCache(true)
                    .apply(RequestOptions.placeholderOf(R.drawable.all_loading_place_holder))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .error(R.drawable.all_place_holder)
                    .thumbnail(0.65f)
                    .override(binding.getRoot().getWidth(), binding.getRoot().getHeight())
                    .load(pageItem.isLoading() ? pageItem.getOrgUri() : pageItem.getEnhanceUri())
                    .into(binding.ivDocumentItem);

            this.binding.tvNumberCounter.setText(String.valueOf(pageItem.getPosition()));
            this.binding.pbLoadingFilter.setVisibility(pageItem.isLoading() ? View.VISIBLE : View.GONE);
            this.binding.ivDeleteIcon.setVisibility(pageItem.isLoading() ? View.INVISIBLE : View.VISIBLE);

        }
    }
}
