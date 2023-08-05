package com.document.camerascanner.features.detailshow.documents;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.MediaStoreSignature;
import com.document.camerascanner.R;
import com.document.camerascanner.databases.model.PageItem;
import com.document.camerascanner.databinding.ItemDetailShowBinding;

import java.io.File;
import java.util.List;

public class DetailDocumentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;

    private boolean isMultipleSelect;
    private boolean isDragPage;

    private List<PageItem> itemFileList;
    private OnClickImageListener listener;

    public DetailDocumentAdapter(Context context) {
        this.context = context;
    }

    public void setItemFileList(List<PageItem> itemFileList) {
        this.itemFileList = itemFileList;
        this.notifyDataSetChanged();
    }

    public void setOnClickImageListener(OnClickImageListener onClickImageListener) {
        this.listener = onClickImageListener;
    }

    public void setMultipleSelect(boolean multipleSelect) {
        this.isMultipleSelect = multipleSelect;
        this.notifyDataSetChanged();
    }

    public void setDragPage(boolean dragPage) {
        isDragPage = dragPage;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(this.context);
        return new DetailViewHolder(ItemDetailShowBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        PageItem item = this.itemFileList.get(position);
        if (item == null) {
            return;
        }

        ((DetailViewHolder) holder).onBind(item, position);
    }

    @Override
    public int getItemCount() {
        return this.itemFileList == null ? 0 : this.itemFileList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    public interface OnClickImageListener {

        void onClickImageView(int position, boolean isSelect);

        void onLongClickImageView(int position);
    }

    class DetailViewHolder extends RecyclerView.ViewHolder {

        private final ItemDetailShowBinding binding;

        public DetailViewHolder(@NonNull ItemDetailShowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        @SuppressLint("SetTextI18n")
        public void onBind(PageItem item, int positon) {
            if (item == null) {
                return;
            }

            this.binding.ivSelect.setVisibility(isMultipleSelect ? View.VISIBLE : View.GONE);
            this.binding.ivSelect.setImageResource(isMultipleSelect && item.isSelected() ? R.drawable.all_vector_item_select
                    : R.drawable.all_vector_item_unselect);

            String enhanceUri = item.getEnhanceUri();
            if (!TextUtils.isEmpty(enhanceUri)) {
                File file = new File(enhanceUri);
                Glide.with(context)
                        .load(file)
                        .signature(new MediaStoreSignature("", file.lastModified(), 0))
                        .into(this.binding.ivDetailShow);
            }

            this.binding.tvStt.setText(String.valueOf(item.getPosition()));

            this.binding.clContainerItem.setOnClickListener(view -> {
                if (listener != null) {
                    listener.onClickImageView(positon, !item.isSelected());
                }
            });

            this.binding.getRoot().setOnLongClickListener(view -> {
                if (listener != null && !isDragPage) {
                    listener.onLongClickImageView(positon);
                }
                return false;
            });
        }
    }

}
