package com.document.camerascanner.features.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.document.camerascanner.R;
import com.document.camerascanner.utils.ImageUtils;


public class DrawingView extends View {

    private final float FOCUS_CIRCLE_RADIUS = 40;
    private final float CIRCLE_OUTSIDE_STROKE_WIDTH = ImageUtils.dp2px(1.5f);

    private final Paint paint;

    private boolean isTouch;
    private boolean isFocusDone = false;
    private float touchCenterX;
    private float touchCenterY;
    private float circleFocusRadius = FOCUS_CIRCLE_RADIUS;

    public DrawingView(Context context) {
        this(context, null);
    }

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.paint = new Paint();
        this.paint.setAntiAlias(true);
        this.paint.setColor(Color.WHITE);
        this.paint.setStyle(Paint.Style.STROKE);
        this.isTouch = false;
    }

    public void onTouchToFocus(float x, float y) {
        this.isTouch = true;
        this.isFocusDone = false;
        this.touchCenterX = x;
        this.touchCenterY = y;
        this.circleFocusRadius = FOCUS_CIRCLE_RADIUS;
    }

    public void onFocusDone() {
        this.isFocusDone = true;
    }

    public void setTouch(boolean touch) {
        this.isTouch = touch;
    }

    public void setTouchCircleRadius(float radiusChangeValue) {
        this.circleFocusRadius = FOCUS_CIRCLE_RADIUS - radiusChangeValue;
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (this.isTouch) {
            if (this.isFocusDone) {
                this.paint.setStrokeWidth(CIRCLE_OUTSIDE_STROKE_WIDTH);
                canvas.drawCircle(this.touchCenterX, this.touchCenterY, ImageUtils.dp2px(this.circleFocusRadius), this.paint);
                this.paint.setColor(ContextCompat.getColor(getContext(), R.color.transparent));
                this.paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(this.touchCenterX, this.touchCenterY, ImageUtils.dp2px(this.circleFocusRadius) - CIRCLE_OUTSIDE_STROKE_WIDTH, this.paint);
            } else {
                this.paint.setColor(Color.WHITE);
                this.paint.setStyle(Paint.Style.STROKE);
                this.paint.setStrokeWidth(CIRCLE_OUTSIDE_STROKE_WIDTH);
                canvas.drawCircle(this.touchCenterX, this.touchCenterY, ImageUtils.dp2px(this.circleFocusRadius), this.paint);
            }
        }
    }
}
