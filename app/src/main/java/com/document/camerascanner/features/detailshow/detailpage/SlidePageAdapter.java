package com.document.camerascanner.features.detailshow.detailpage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.signature.MediaStoreSignature;
import com.document.camerascanner.R;
import com.document.camerascanner.databinding.ItemViewPagerDetailBinding;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.security.MessageDigest;
import java.util.List;

public class SlidePageAdapter extends RecyclerView.Adapter<SlidePageAdapter.PageViewHolder> {

    private final Context context;

    private List<PagePreview> listPage;

    public SlidePageAdapter(Context context) {
        this.context = context;
    }

    public void setListPage(List<PagePreview> listPage) {
        this.listPage = listPage;
        this.notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(this.context);
        return new PageViewHolder(ItemViewPagerDetailBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        PagePreview itemFile = this.listPage.get(position);
        holder.onBind(itemFile);
    }

    @Override
    public int getItemCount() {
        return this.listPage == null ? 0 : this.listPage.size();
    }

    protected static class RotateTransformation extends BitmapTransformation {

        private final float rotateRotationAngle;

        public RotateTransformation(float rotateRotationAngle) {
            this.rotateRotationAngle = rotateRotationAngle;
        }

        @Override
        protected Bitmap transform(@NotNull BitmapPool pool, @NotNull Bitmap toTransform, int outWidth, int outHeight) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotateRotationAngle);
            return Bitmap.createBitmap(toTransform, 0, 0, toTransform.getWidth(), toTransform.getHeight(), matrix, true);
        }

        @Override
        public void updateDiskCacheKey(@NotNull MessageDigest messageDigest) {
            messageDigest.update(("rotate" + rotateRotationAngle).getBytes());
        }
    }

    protected class PageViewHolder extends RecyclerView.ViewHolder {

        private final ItemViewPagerDetailBinding binding;

        public PageViewHolder(@NonNull ItemViewPagerDetailBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void onBind(PagePreview item) {
            if (item == null) {
                return;
            }

            if (TextUtils.isEmpty(item.getImgUri())) {
                return;
            }

            File file = new File(item.getImgUri());
            Glide.with(context)
                    .load(item.getImgUri())
                    .error(R.drawable.all_loading_place_holder)
                    .transform(new RotateTransformation(item.getRoation()))
                    .signature(new MediaStoreSignature(null, file.lastModified(), 0))
                    .into(this.binding.ivPage);

        }
    }
}
