package com.document.camerascanner.features.detailshow.documents.helper;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.document.camerascanner.features.detailshow.documents.interfaces.ItemTouchListener;

public class ItemTouchHelperCallBack extends ItemTouchHelper.Callback {

    private final ItemTouchListener listener;

    public ItemTouchHelperCallBack(ItemTouchListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return super.isItemViewSwipeEnabled();
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return super.isLongPressDragEnabled();
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int dragFlag = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
        int swipeFlag = 0;
        return makeMovementFlags(dragFlag, swipeFlag);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        if (this.listener != null) {
            this.listener.onMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        }
        return false;
    }
}
