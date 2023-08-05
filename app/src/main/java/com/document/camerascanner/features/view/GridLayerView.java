package com.document.camerascanner.features.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class GridLayerView extends View {

    private static final float LINE_STICK = 1.5f;
    private Paint mPaint;

    public GridLayerView(Context context) {
        super(context);
        this.init();
    }

    public GridLayerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.init();
    }

    public GridLayerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.init();
    }

    private void init() {
        this.mPaint = new Paint();
        this.mPaint.setColor(0xFFFFFFFF);
        this.mPaint.setAntiAlias(true);
        this.mPaint.setStrokeWidth(LINE_STICK);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = this.getWidth();
        int height = this.getHeight();

        canvas.drawLine((float) width / 3, 0, (float) width / 3, (float) height, this.mPaint);
        canvas.drawLine((float) (2 * width / 3), 0, (float) (2 * width / 3), (float) height, this.mPaint);

        canvas.drawLine(0, (float) height / 3, width, (float) height / 3, this.mPaint);
        canvas.drawLine(0, (float) 2 * height / 3, width, (float) 2 * height / 3, this.mPaint);
    }
}
