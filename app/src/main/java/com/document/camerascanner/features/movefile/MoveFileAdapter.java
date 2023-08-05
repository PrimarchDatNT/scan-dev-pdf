package com.document.camerascanner.features.movefile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.document.camerascanner.R;
import com.document.camerascanner.databases.model.BaseEntity;
import com.document.camerascanner.databases.model.FolderItem;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MoveFileAdapter extends RecyclerView.Adapter<MoveFileAdapter.MoveFileViewHolder> {

    private final Context context;

    private final List<BaseEntity> listFolder;

    private CallBackSelectFolder callback;

    public MoveFileAdapter(Context context, @NotNull List<BaseEntity> listFolder) {
        this.context = context;
        this.listFolder = listFolder;
    }

    public void setCallBackSelectFolder(CallBackSelectFolder callBackSelectFolder) {
        this.callback = callBackSelectFolder;
    }

    @NonNull
    @Override
    public MoveFileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(this.context);
        View view = inflater.inflate(R.layout.item_move_file, parent, false);
        return new MoveFileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MoveFileViewHolder holder, int position) {
        BaseEntity item = this.listFolder.get(position);
        if (item == null) {
            return;
        }
        holder.onBind(item, position);
    }

    @Override
    public int getItemCount() {
        return this.listFolder.size();
    }

    public interface CallBackSelectFolder {

        void onClickItem(int position);

        void onClickCreateNew();
    }

    public class MoveFileViewHolder extends RecyclerView.ViewHolder {

        private final View ivLine;

        private final TextView tvItemMove;

        private final ImageView ivItemMove;

        private final ImageView ivItemSelect;

        private final ConstraintLayout clContainer;

        public MoveFileViewHolder(@NonNull View itemView) {
            super(itemView);
            this.ivLine = itemView.findViewById(R.id.iv_line);
            this.tvItemMove = itemView.findViewById(R.id.tv_item_folder);
            this.ivItemSelect = itemView.findViewById(R.id.iv_item_select);
            this.ivItemMove = itemView.findViewById(R.id.iv_item_move);
            this.clContainer = itemView.findViewById(R.id.cl_container_folder);
        }

        public void onBind(BaseEntity item, int position) {
            if (item == null) {
                return;
            }

            boolean isFolder = item instanceof FolderItem;

            if (position == 0) {
                this.tvItemMove.setText(isFolder ? R.string.all_new_folder_title : R.string.detail_new_document);
                this.ivItemMove.setImageResource(isFolder ? R.drawable.detail_folder_vector_newfolder : R.drawable.all_vector_new_document);

                this.ivLine.setVisibility(View.GONE);

                this.clContainer.setOnClickListener(view -> {
                    if (callback != null) {
                        callback.onClickCreateNew();
                    }
                });
                return;
            }

            this.ivItemMove.setImageResource(isFolder ? R.drawable.move_vector_folder_small : R.drawable.all_vector_document);
            this.ivItemSelect.setVisibility(item.isSelected() ? View.VISIBLE : View.GONE);
            this.ivLine.setVisibility(View.VISIBLE);
            this.tvItemMove.setText(item.getName());

            this.clContainer.setOnClickListener(view -> {
                if (callback != null) {
                    callback.onClickItem(position);
                }
            });

        }
    }
}
