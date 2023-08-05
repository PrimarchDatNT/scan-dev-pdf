package com.document.camerascanner.features.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.MediaStoreSignature;
import com.document.camerascanner.R;
import com.document.camerascanner.databases.model.DocumentItem;
import com.document.camerascanner.databases.model.FolderItem;
import com.document.camerascanner.features.main.adapter.PresentType;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;

    private boolean isSelect;
    private boolean isTypeShowGrid;
    private boolean isOnMultipleSelect = false;

    private List<Object> listItem;
    private CallBackClickItem listener;

    public FileAdapter(Context context, List<Object> listItem) {
        this.context = context;
        this.listItem = listItem;
    }

    public void setTypeShowGrid(boolean typeShowGrid) {
        this.isTypeShowGrid = typeShowGrid;
        this.notifyDataSetChanged();
    }

    public void setCallBackClickItem(CallBackClickItem callBackClickItem) {
        this.listener = callBackClickItem;
    }

    public void setOnMultipleSelect(boolean onMultipleSelect) {
        this.isOnMultipleSelect = onMultipleSelect;
        this.notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(this.context);

        if (viewType == PresentType.LIST) {
            return new ViewHolder(inflater.inflate(R.layout.item_folder_list, parent, false));
        }

        return new ViewHolder(inflater.inflate(R.layout.item_folder_grid, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = this.listItem.get(position);
        if (item == null) {
            return;
        }

        ViewHolder viewHolder = (ViewHolder) holder;
        viewHolder.onBindView(item, position);
    }

    @Override
    public int getItemViewType(int position) {
        if (this.isTypeShowGrid) {
            return PresentType.GRID;
        }

        return PresentType.LIST;
    }

    @Override
    public int getItemCount() {
        return this.listItem == null ? 0 : this.listItem.size();
    }

    public boolean isSelect() {
        return this.isSelect;
    }

    public void setSelect(boolean select) {
        this.isSelect = select;
        this.notifyDataSetChanged();
    }

    public void setListFile(List<Object> list) {
        this.listItem = list;
        this.notifyDataSetChanged();
    }

    public interface CallBackClickItem {

        void onClickSelectDocument(int position, boolean isSelect);

        void onClickSelectFolder(int position, boolean isSelect);

        void onClickOpenFolder(int position);

        void onclickOpenDocument(int position);

        void onLongClick(int position, boolean isDocument, boolean isSelect);

        void onClickRename(Object itemFile);

        void deleteFile(int position, Object object);

        void shareFile(int position, Object object);

        void moveFile(int position, Object object);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final AppCompatImageView ivSelect;

        private final AppCompatImageView ivRepresent;

        private final AppCompatImageView ivOptions;

        private final AppCompatTextView tvTitle;

        private final AppCompatTextView tvPageCount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.ivSelect = itemView.findViewById(R.id.iv_select);
            this.ivRepresent = itemView.findViewById(R.id.iv_represent);
            this.tvTitle = itemView.findViewById(R.id.tv_title);
            this.tvPageCount = itemView.findViewById(R.id.tv_page_count);
            this.ivOptions = itemView.findViewById(R.id.iv_option);
        }

        @SuppressLint("NonConstantResourceId")
        public void onBindView(Object item, int position) {
            if (item == null) {
                return;
            }

            if (isSelect) {
                this.ivSelect.setVisibility(View.VISIBLE);

                if (isOnMultipleSelect) {
                    this.ivOptions.setImageResource(R.drawable.home_vector_edit);
                    this.ivOptions.setVisibility(View.VISIBLE);
                } else {
                    this.ivOptions.setVisibility(View.GONE);
                }
            } else {
                this.ivSelect.setVisibility(View.GONE);
                this.ivOptions.setVisibility(View.VISIBLE);
                this.ivOptions.setImageResource(R.drawable.home_vector_option);
            }

            if (item instanceof FolderItem) {
                this.bindItem(position, (FolderItem) item);
            }

            if (item instanceof DocumentItem) {
                this.bindItem(position, (DocumentItem) item);
            }

            this.ivOptions.setOnClickListener(view -> {
                if (!isSelect) {
                    FirebaseAnalytics.getInstance(context).logEvent("MAIN_ITEM_CLICK_MENU", null);
                    PopupMenu popupMenu = new PopupMenu(context, this.ivOptions);

                    if (item instanceof DocumentItem) {
                        popupMenu.inflate(R.menu.options_menu);
                    } else {
                        popupMenu.inflate(R.menu.menu_folder);
                    }

                    popupMenu.setOnMenuItemClickListener(menuItem -> {
                        if (listener == null) {
                            return false;
                        }

                        switch (menuItem.getItemId()) {
                            case R.id.menu_rename:
                            case R.id.menu_folder_rename:
                                FirebaseAnalytics.getInstance(context).logEvent("MAIN_MENU_CLICK_RENAME", null);
                                if (listener != null) {
                                    listener.onClickRename(item);
                                }
                                break;

                            case R.id.menu_move:
                                FirebaseAnalytics.getInstance(context).logEvent("MAIN_MENU_CLICK_MOVE", null);
                                if (listener != null) {
                                    listener.moveFile(position, item);
                                }
                                break;

                            case R.id.menu_share:
                            case R.id.menu_folder_share:
                                FirebaseAnalytics.getInstance(context).logEvent("MAIN_MENU_CLICK_SHARE", null);
                                if (listener != null) {
                                    listener.shareFile(position, item);
                                }
                                break;

                            case R.id.menu_delete:
                            case R.id.menu_folder_delete:
                                FirebaseAnalytics.getInstance(context).logEvent("MAIN_MENU_CLICK_DELETE", null);
                                if (listener != null) {
                                    listener.deleteFile(position, item);
                                }
                                break;
                        }
                        return false;
                    });

                    popupMenu.show();
                } else {
                    FirebaseAnalytics.getInstance(context).logEvent("MAIN_ITEM_CLICK_RENAME", null);
                    if (listener != null) {
                        listener.onClickRename(item);
                    }
                }
            });
        }

        private void bindItem(int position, @NotNull FolderItem folderItem) {
            this.ivSelect.setImageResource(folderItem.isSelected() ? R.drawable.all_vector_item_select : R.drawable.all_vector_item_unselect);
            this.tvTitle.setText(folderItem.getName());
            this.tvPageCount.setText(context.getResources().getQuantityString(R.plurals.count_document,
                    folderItem.getChildCount(), folderItem.getChildCount()));

            if (getItemViewType() == 1) {
                this.ivRepresent.getLayoutParams().width = 70;
            }

            this.itemView.setOnClickListener(view -> {
                if (isSelect) {
                    listener.onClickSelectFolder(position, !folderItem.isSelected());
                    return;
                }
                listener.onClickOpenFolder(position);
            });

            this.itemView.setOnLongClickListener(view -> {
                if (listener != null) {
                    listener.onLongClick(position, false, folderItem.isSelected());
                }
                return false;
            });
        }

        private void bindItem(int position, @NotNull DocumentItem documentItem) {
            this.ivSelect.setImageResource(documentItem.isSelected() ? R.drawable.all_vector_item_select : R.drawable.all_vector_item_unselect);
            this.tvTitle.setText(documentItem.getName());

            this.tvPageCount.setText(context.getResources()
                    .getQuantityString(R.plurals.count_page, documentItem.getChildCount(), documentItem.getChildCount()));

            if (documentItem.getChildCount() == 0) {
                this.ivRepresent.setImageResource(R.drawable.home_vector_folderlagre);
                return;
            }

            File file = new File(documentItem.getThumbnail());
            Glide.with(context)
                    .load(file)
                    .signature(new MediaStoreSignature("", file.lastModified(), 0))
                    .error(R.drawable.home_vector_folderlagre)
                    .into(this.ivRepresent);

            this.itemView.setOnClickListener(view -> {
                if (isSelect) {
                    listener.onClickSelectDocument(position, !documentItem.isSelected());
                    return;
                }
                listener.onclickOpenDocument(position);
            });

            this.itemView.setOnLongClickListener(view -> {
                if (listener != null) {
                    listener.onLongClick(position, true, documentItem.isSelected());
                }
                return false;
            });

        }
    }

}
