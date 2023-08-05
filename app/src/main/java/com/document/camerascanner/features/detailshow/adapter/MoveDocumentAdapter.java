package com.document.camerascanner.features.detailshow.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.document.camerascanner.R;
import com.document.camerascanner.databases.model.FolderItem;
import com.document.camerascanner.databinding.ItemMoveFileBinding;

import java.util.List;

public class MoveDocumentAdapter extends RecyclerView.Adapter<MoveDocumentAdapter.MoveDocumentViewHolder> {

    private final Context context;
    private final List<FolderItem> listFile;

    private boolean isAdapterFolders = true;

    private CallBackSelectFolder listener;

    public MoveDocumentAdapter(Context context, List<FolderItem> list) {
        this.context = context;
        this.listFile = list;
        this.listFile.add(0, null);
        this.listFile.add(null);
    }

    public void setCallBackSelectFolder(CallBackSelectFolder callBackSelectFolder) {
        this.listener = callBackSelectFolder;
    }

    public void setAdapterFolders(boolean isFolder) {
        this.isAdapterFolders = isFolder;
        this.notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MoveDocumentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(this.context);
        return new MoveDocumentViewHolder(ItemMoveFileBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MoveDocumentViewHolder holder, int position) {
        FolderItem folderItem = this.listFile.get(position);
        holder.onBind(folderItem, position);
    }

    @Override
    public int getItemCount() {
        if (this.listFile == null) {
            return 0;
        }

        if (this.isAdapterFolders) {
            return this.listFile.size();
        }

        return this.listFile.size() - 1;
    }

    public interface CallBackSelectFolder {

        void callBackSelectFolder(int position);

        void callbackMoveOut(int position);

        void callbackNewFolder(int position);
    }

    class MoveDocumentViewHolder extends RecyclerView.ViewHolder {

        private final ItemMoveFileBinding binding;

        public MoveDocumentViewHolder(@NonNull ItemMoveFileBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void onBind(FolderItem folderItem, int position) {
            if (position == 0) {

                this.binding.ivLine.setVisibility(View.GONE);
                this.binding.ivItemMove.setImageResource(isAdapterFolders ? R.drawable.detail_folder_vector_newfolder
                        : R.drawable.all_vector_new_document);
                this.binding.tvItemFolder.setText(isAdapterFolders ? R.string.all_new_folder_title : R.string.detail_new_document);
                this.binding.ivItemSelect.setVisibility(View.GONE);

                this.binding.getRoot().setOnClickListener(view -> {
                    if (listener != null) {
                        listener.callbackNewFolder(position);
                    }
                });
                return;
            }

            if (position < listFile.size() - 1) {
                if (folderItem == null) {
                    return;
                }

                this.binding.ivItemSelect.setVisibility(folderItem.isSelected() ? View.VISIBLE : View.GONE);
                this.binding.ivLine.setVisibility(View.VISIBLE);
                this.binding.tvItemFolder.setText(folderItem.getName());

                this.binding.getRoot().setOnClickListener(view -> {
                    if (listener != null) {
                        listener.callBackSelectFolder(position);
                    }
                });

                this.binding.ivItemMove.setImageResource(isAdapterFolders ? R.drawable.move_vector_folder_small : R.drawable.all_vector_document);
                return;
            }

            this.binding.getRoot().setOnClickListener(view -> {
                if (listener != null) {
                    listener.callbackMoveOut(position);
                }
            });

            this.binding.ivLine.setVisibility(View.VISIBLE);
            this.binding.ivItemMove.setImageResource(R.drawable.folder_detail_vector_move_out);
            this.binding.tvItemFolder.setText(context.getString(R.string.all_detail_folder_move_out));
            this.binding.ivItemSelect.setVisibility(View.GONE);

            if (position == listFile.size() - 1) {
                this.binding.getRoot().setVisibility(isAdapterFolders ? View.VISIBLE : View.GONE);
            }

        }
    }
}
