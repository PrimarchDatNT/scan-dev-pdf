package com.document.camerascanner.features.detailshow.folders;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.MediaStoreSignature;
import com.document.camerascanner.R;
import com.document.camerascanner.databases.model.DocumentItem;
import com.document.camerascanner.databinding.ItemFolderGridBinding;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.File;
import java.util.List;

public class DetailFolderAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private final FirebaseAnalytics event;

    private boolean isSelect;
    private boolean isClickItems;

    private List<DocumentItem> listItem;
    private OnItemClickListener listener;

    public DetailFolderAdapter(Context context) {
        this.event = FirebaseAnalytics.getInstance(context);
        this.context = context;
    }

    public void setListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public boolean isSelect() {
        return this.isSelect;
    }

    public void setSelect(boolean select) {
        this.isSelect = select;
    }

    public void setListItem(List<DocumentItem> listItem) {
        this.listItem = listItem;
        this.notifyDataSetChanged();
    }

    public void setClickItems(boolean clickItems) {
        this.isClickItems = clickItems;
        this.notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(this.context);
        return new DetailFolderViewHolder(ItemFolderGridBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DocumentItem item = this.listItem.get(position);
        if (item == null) {
            return;
        }

        DetailFolderViewHolder detailFolderViewHolder = (DetailFolderViewHolder) holder;
        detailFolderViewHolder.onBind(item);
    }

    @Override
    public int getItemCount() {
        return this.listItem == null ? 0 : this.listItem.size();
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    public class DetailFolderViewHolder extends RecyclerView.ViewHolder {

        private final ItemFolderGridBinding binding;

        public DetailFolderViewHolder(@NonNull ItemFolderGridBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        @SuppressLint("NonConstantResourceId")
        public void onBind(DocumentItem item) {
            if (item == null) {
                return;
            }

            this.binding.tvTitle.setText(item.getName());
            int count = item.getChildCount();
            this.binding.tvPageCount.setText(context.getResources().getQuantityString(R.plurals.count_page, count, count));
            if (count != 0) {
                File file = new File(item.getThumbnail());
                Glide.with(context)
                        .load(file)
                        .error(R.drawable.home_vector_folderlagre)
                        .signature(new MediaStoreSignature("", file.lastModified(), 0))
                        .into(this.binding.ivRepresent);
            }

            if (isSelect) {
                this.binding.ivSelect.setVisibility(View.VISIBLE);
                if (isClickItems) {
                    this.binding.ivOption.setImageResource(R.drawable.home_vector_edit);
                    this.binding.ivOption.setVisibility(View.VISIBLE);
                } else {
                    this.binding.ivOption.setVisibility(View.GONE);
                }
            } else {
                this.binding.ivSelect.setVisibility(View.GONE);
                this.binding.ivOption.setVisibility(View.VISIBLE);
                this.binding.ivOption.setImageResource(R.drawable.home_vector_option);
            }

            this.binding.ivSelect.setImageResource(item.isSelected() ? R.drawable.all_vector_item_select : R.drawable.all_vector_item_unselect);

            this.binding.getRoot().setOnClickListener(view -> {
                if (listener != null) {
                    listener.onItemClick(getAdapterPosition(), !item.isSelected());
                }
            });

            this.binding.getRoot().setOnLongClickListener(view -> {
                if (listener != null) {
                    listener.onItemLongClick(getAdapterPosition());
                }
                return false;
            });

            this.binding.ivOption.setOnClickListener(view -> {
                event.logEvent("DETAIL_FOLDER_CLICK_ITEM_MENU", null);

                if (!isSelect) {
                    PopupMenu popupMenu = new PopupMenu(context, this.binding.ivOption);
                    popupMenu.inflate(R.menu.options_menu);
                    popupMenu.show();
                    popupMenu.setOnMenuItemClickListener(menuItem -> {
                        if (listener == null) {
                            return false;
                        }

                        switch ((menuItem.getItemId())) {
                            case R.id.menu_rename:
                            case R.id.menu_folder_rename:
                                event.logEvent("DETAIL_FOLDER_CLICK_MENU_RENAME", null);
                                listener.onClickRename(item);
                                break;

                            case R.id.menu_move:
                                event.logEvent("DETAIL_FOLDER_CLICK_MENU_MOVE", null);
                                listener.onMoveFolder(getAdapterPosition());
                                break;

                            case R.id.menu_share:
                            case R.id.menu_folder_share:
                                event.logEvent("DETAIL_FOLDER_CLICK_MENU_SHARE", null);
                                listener.onShareFolder(getAdapterPosition());
                                break;

                            case R.id.menu_delete:
                            case R.id.menu_folder_delete:
                                event.logEvent("DETAIL_FOLDER_CLICK_MENU_DELETE", null);
                                listener.onDelete(getAdapterPosition());
                                break;
                        }
                        return false;
                    });
                } else {
                    event.logEvent("DETAIL_FOLDER_CLICK_ITEM_RENAME", null);
                    listener.onClickRename(item);
                }
            });
        }
    }

    public interface OnItemClickListener {

        void onItemClick(int position, boolean isSelect);

        void onItemLongClick(int position);

        void onClickRename(DocumentItem itemFile);

        void onDelete(int position);

        void onShareFolder(int position);

        void onMoveFolder(int position);
    }

}
