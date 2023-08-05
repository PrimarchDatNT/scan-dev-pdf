package com.document.camerascanner.features.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.document.camerascanner.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PolygonView extends FrameLayout {

    protected Context mContext;

    private boolean isRevert = false;
    private boolean mIsTouching = false;
    private boolean isInitedMatrix = false;
    private int mLoupeRadius;
    private int maxVertextX;
    private int maxVertextY;
    private int minVertextX;
    private int minVertextY;

    private Bitmap mBitmap;
    private Matrix matrix;
    private PointF zoomPT;
    private Paint textPaint;
    private Paint borderPaint;
    private Paint circelPaint;
    private Paint selectionRectPaint;

    private View pointer1;
    private View pointer2;
    private View pointer3;
    private View pointer4;
    private View midPointer13;
    private View midPointer12;
    private View midPointer34;
    private View midPointer24;
    private OnVertextPointChangeListener vertextListener;

    public PolygonView(Context context) {
        super(context);
        this.mContext = context;
        this.init();
    }

    public PolygonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        this.init();
    }

    public PolygonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        this.init();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init() {
        final PointF[] vertextPoints = new PointF[]{new PointF(0, 0),
                new PointF(this.getWidth(), 0),
                new PointF(0, this.getHeight()),
                new PointF(this.getWidth(), this.getHeight())};

        this.pointer1 = this.getEdgePointer((int) vertextPoints[0].x, (int) vertextPoints[0].y);
        this.pointer2 = this.getEdgePointer((int) vertextPoints[1].x, (int) vertextPoints[1].y);
        this.pointer3 = this.getEdgePointer((int) vertextPoints[2].x, (int) vertextPoints[2].y);
        this.pointer4 = this.getEdgePointer((int) vertextPoints[3].x, (int) vertextPoints[3].y);

        this.midPointer13 = this.getEdgePointer((int) (this.pointer1.getX() + this.pointer3.getX()) / 2,
                (int) (this.pointer1.getX() + this.pointer3.getX()) / 2);
        this.midPointer12 = this.getEdgePointer((int) (this.pointer1.getX() + this.pointer2.getX()) / 2,
                (int) (this.pointer1.getX() + this.pointer2.getX()) / 2);
        this.midPointer34 = this.getEdgePointer((int) (this.pointer4.getX() + this.pointer3.getX()) / 2,
                (int) (this.pointer4.getX() + this.pointer3.getX()) / 2);
        this.midPointer24 = this.getEdgePointer((int) (this.pointer2.getX() + this.pointer4.getX()) / 2,
                (int) (this.pointer2.getX() + this.pointer4.getX()) / 2);

        this.pointer1.setOnTouchListener(new TouchListenerImpl());
        this.pointer2.setOnTouchListener(new TouchListenerImpl());
        this.pointer3.setOnTouchListener(new TouchListenerImpl());
        this.pointer4.setOnTouchListener(new TouchListenerImpl());

        this.midPointer13.setOnTouchListener(new MidPointTouchListenerImpl(this.pointer1, this.pointer3));
        this.midPointer12.setOnTouchListener(new MidPointTouchListenerImpl(this.pointer1, this.pointer2));
        this.midPointer34.setOnTouchListener(new MidPointTouchListenerImpl(this.pointer3, this.pointer4));
        this.midPointer24.setOnTouchListener(new MidPointTouchListenerImpl(this.pointer2, this.pointer4));

        this.addView(this.pointer1);
        this.addView(this.pointer2);
        this.addView(this.midPointer13);
        this.addView(this.midPointer12);
        this.addView(this.midPointer34);
        this.addView(this.midPointer24);
        this.addView(this.pointer3);
        this.addView(this.pointer4);
        this.initPaint();

        this.matrix = new Matrix();
        this.zoomPT = new PointF();
        this.mLoupeRadius = (int) TypedValue.applyDimension(1, 40.0f, this.getResources().getDisplayMetrics());
    }

    private void initPaint() {
        this.selectionRectPaint = new Paint();
        this.selectionRectPaint.setColor(ContextCompat.getColor(this.mContext, R.color.colorAccent));
        this.selectionRectPaint.setStrokeWidth(6);
        this.selectionRectPaint.setAntiAlias(true);
    }

    public void setPoints(@NonNull Map<Integer, PointF> pointFMap) {
        if (pointFMap.size() == 4) {
            this.setPointsCoordinates(pointFMap);
            this.invalidate();
        }
    }

    public void setPoints(@NonNull List<PointF> listPonitF) {
        if (listPonitF.size() == 4) {
            this.setPointsCoordinates(this.getOrderedPoints(listPonitF));
            this.invalidate();
        }
    }

    public void setMaxVertextPoint() {
        this.setPoints(this.getOrderedPoints(this.getMaxVertextPoint()));
    }

    public void setMainBitmap(Bitmap bitmap) {
        this.mBitmap = bitmap;
    }

    public List<PointF> getMaxVertextPoint() {
        List<PointF> listPointF = new ArrayList<>();
        listPointF.add(new PointF(this.minVertextX, this.minVertextY));
        listPointF.add(new PointF(this.maxVertextX, this.minVertextY));
        listPointF.add(new PointF(this.minVertextX, this.maxVertextY));
        listPointF.add(new PointF(this.maxVertextX, this.maxVertextY));
        return listPointF;
    }

    public PointF[] getSelectPoint() {
        final float offset = this.pointer1.getWidth() / 2.0f;

        PointF[] result = new PointF[]{new PointF(this.pointer1.getX() + offset, this.pointer1.getY() + offset),
                new PointF(this.pointer2.getX() + offset, this.pointer2.getY() + offset),
                new PointF(this.pointer3.getX() + offset, this.pointer3.getY() + offset),
                new PointF(this.pointer4.getX() + offset, this.pointer4.getY() + offset)};

        PointF interPoint = this.calculateIntersections(result);

        int p1Index = 0;
        int p2Index = 1;
        int p3Index = 2;
        int p4Index = 3;

        for (int i = 0; i < result.length; i++) {
            if (result[i].x < interPoint.x && result[i].y < interPoint.y) {
                p1Index = i;
            }

            if (result[i].x > interPoint.x && result[i].y < interPoint.y) {
                p2Index = i;
            }

            if (result[i].x < interPoint.x && result[i].y > interPoint.y) {
                p3Index = i;
            }

            if (result[i].x > interPoint.x && result[i].y > interPoint.y) {
                p4Index = i;
            }
        }

        return new PointF[]{result[p1Index], result[p2Index], result[p3Index], result[p4Index]};
    }

    public void setVertexCord(int minVertextX, int minVertextY, int maxVertextX, int maxVertextY) {
        this.minVertextX = minVertextX;
        this.minVertextY = minVertextY;
        this.maxVertextX = maxVertextX;
        this.maxVertextY = maxVertextY;
    }

    private void setPointsCoordinates(@NonNull Map<Integer, PointF> pointFMap) {
        PointF pointF0 = pointFMap.get(0);

        if (pointF0 != null) {
            this.pointer1.setX(pointF0.x - (float) this.pointer1.getWidth() / 2);
            this.pointer1.setY(pointF0.y - (float) this.pointer1.getHeight() / 2);
        } else {
            this.pointer1.setX(this.minVertextX);
            this.pointer1.setY(this.minVertextY);
        }

        PointF pointF1 = pointFMap.get(1);
        if (pointF1 != null) {
            this.pointer2.setX(pointF1.x - (float) this.pointer2.getWidth() / 2);
            this.pointer2.setY(pointF1.y - (float) this.pointer2.getHeight() / 2);
        } else {
            this.pointer2.setX(this.maxVertextX);
            this.pointer2.setY(this.minVertextY);
        }

        PointF pointF2 = pointFMap.get(2);
        if (pointF2 != null) {
            this.pointer3.setX(pointF2.x - (float) this.pointer3.getWidth() / 2);
            this.pointer3.setY(pointF2.y - (float) this.pointer3.getHeight() / 2);
        } else {
            this.pointer3.setX(this.minVertextX);
            this.pointer3.setY(this.maxVertextY);
        }

        PointF pointF3 = pointFMap.get(3);
        if (pointF3 != null) {
            this.pointer4.setX(pointF3.x - (float) this.pointer4.getWidth() / 2);
            this.pointer4.setY(pointF3.y - (float) this.pointer4.getHeight() / 2);
        } else {
            this.pointer4.setX(this.maxVertextX);
            this.pointer4.setY(this.maxVertextY);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        this.drawSelectRectBorder(canvas);
        this.setMidPosition();
        this.drawUnSelectBackground(canvas);
        this.drawOverPoiter(canvas);

        if (this.mIsTouching) {
            this.drawCircleChanged(canvas);
        }
    }

    private void drawOverPoiter(@NonNull Canvas canvas) {
        Paint circleP = new Paint();
        circleP.setColor(Color.WHITE);
        circleP.setAntiAlias(true);

        Paint vertexPointP = new Paint();
        vertexPointP.setColor(ContextCompat.getColor(this.mContext, R.color.colorAccent));
        vertexPointP.setAntiAlias(true);

        Paint borderP = new Paint();
        borderP.setColor(ContextCompat.getColor(this.mContext, R.color.colorAccent));
        borderP.setDither(true);
        borderP.setAntiAlias(true);
        borderP.setStyle(Paint.Style.STROKE);
        borderP.setStrokeWidth(4);
        borderP.setStrokeCap(Paint.Cap.SQUARE);

        final float offset = this.pointer1.getWidth() / 2.0f;
        PointF cp1 = new PointF(this.pointer1.getX() + offset, this.pointer1.getY() + offset);
        PointF cp2 = new PointF(this.pointer2.getX() + offset, this.pointer2.getY() + offset);
        PointF cp3 = new PointF(this.pointer3.getX() + offset, this.pointer3.getY() + offset);
        PointF cp4 = new PointF(this.pointer4.getX() + offset, this.pointer4.getY() + offset);

        PointF midCp1 = new PointF(this.midPointer13.getX() + offset, this.midPointer13.getY() + offset);
        PointF midCp2 = new PointF(this.midPointer24.getX() + offset, this.midPointer24.getY() + offset);
        PointF midCp3 = new PointF(this.midPointer34.getX() + offset, this.midPointer34.getY() + offset);
        PointF midCp4 = new PointF(this.midPointer12.getX() + offset, this.midPointer12.getY() + offset);

        final float rad = offset - 40.0f;
        canvas.drawCircle(cp1.x, cp1.y, rad, vertexPointP);
        canvas.drawCircle(cp2.x, cp2.y, rad, vertexPointP);
        canvas.drawCircle(cp3.x, cp3.y, rad, vertexPointP);
        canvas.drawCircle(cp4.x, cp4.y, rad, vertexPointP);

        canvas.drawCircle(midCp1.x, midCp1.y, rad, circleP);
        canvas.drawCircle(midCp2.x, midCp2.y, rad, circleP);
        canvas.drawCircle(midCp3.x, midCp3.y, rad, circleP);
        canvas.drawCircle(midCp4.x, midCp4.y, rad, circleP);

        canvas.drawCircle(midCp1.x, midCp1.y, rad, borderP);
        canvas.drawCircle(midCp2.x, midCp2.y, rad, borderP);
        canvas.drawCircle(midCp3.x, midCp3.y, rad, borderP);
        canvas.drawCircle(midCp4.x, midCp4.y, rad, borderP);

        Paint paint = new Paint();
        paint.setColor(Color.BLUE);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(20);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    private void drawSelectRectBorder(@NonNull Canvas canvas) {
        canvas.drawLine(this.pointer1.getX() + ((float) this.pointer1.getWidth() / 2),
                this.pointer1.getY() + ((float) this.pointer1.getHeight() / 2),
                this.pointer3.getX() + ((float) this.pointer3.getWidth() / 2),
                this.pointer3.getY() + ((float) this.pointer3.getHeight() / 2), this.selectionRectPaint);

        canvas.drawLine(this.pointer1.getX() + ((float) this.pointer1.getWidth() / 2),
                this.pointer1.getY() + ((float) this.pointer1.getHeight() / 2),
                this.pointer2.getX() + ((float) this.pointer2.getWidth() / 2),
                this.pointer2.getY() + ((float) this.pointer2.getHeight() / 2), this.selectionRectPaint);

        canvas.drawLine(this.pointer2.getX() + ((float) this.pointer2.getWidth() / 2),
                this.pointer2.getY() + ((float) this.pointer2.getHeight() / 2),
                this.pointer4.getX() + ((float) this.pointer4.getWidth() / 2),
                this.pointer4.getY() + ((float) this.pointer4.getHeight() / 2), this.selectionRectPaint);

        canvas.drawLine(this.pointer3.getX() + ((float) this.pointer3.getWidth() / 2),
                this.pointer3.getY() + ((float) this.pointer3.getHeight() / 2),
                this.pointer4.getX() + ((float) this.pointer4.getWidth() / 2),
                this.pointer4.getY() + ((float) this.pointer4.getHeight() / 2), this.selectionRectPaint);
    }

    private void setMidPosition() {
        this.midPointer13.setX((this.pointer1.getX() + this.pointer3.getX()) / 2.0f);
        this.midPointer13.setY((this.pointer1.getY() + this.pointer3.getY()) / 2.0f);

        this.midPointer24.setX((this.pointer2.getX() + this.pointer4.getX()) / 2.0f);
        this.midPointer24.setY((this.pointer2.getY() + this.pointer4.getY()) / 2.0f);

        this.midPointer34.setX((this.pointer3.getX() + this.pointer4.getX()) / 2.0f);
        this.midPointer34.setY((this.pointer3.getY() + this.pointer4.getY()) / 2.0f);

        this.midPointer12.setX((this.pointer1.getX() + this.pointer2.getX()) / 2.0f);
        this.midPointer12.setY((this.pointer1.getY() + this.pointer2.getY()) / 2.0f);
    }

    @NonNull
    @SuppressLint("ClickableViewAccessibility")
    private View getEdgePointer(int x, int y) {
        View pView = new View(this.mContext);
        LayoutParams layoutParams = new LayoutParams(120, 120);
        pView.setLayoutParams(layoutParams);
        pView.setX(x);
        pView.setY(y);
        return pView;
    }

    public boolean isValidShape() {
        double quadAllAngle = this.calculateAngle(this.pointer1.getX(), this.pointer1.getY(), this.pointer2.getX(), this.pointer2.getY(), this.pointer3.getX(), this.pointer3.getY())
                + this.calculateAngle(this.pointer3.getX(), this.pointer3.getY(), this.pointer1.getX(), this.pointer1.getY(), this.pointer4.getX(), this.pointer4.getY())
                + this.calculateAngle(this.pointer2.getX(), this.pointer2.getY(), this.pointer1.getX(), this.pointer1.getY(), this.pointer4.getX(), this.pointer4.getY())
                + this.calculateAngle(this.pointer4.getX(), this.pointer4.getY(), this.pointer2.getX(), this.pointer2.getY(), this.pointer3.getX(), this.pointer3.getY());
        return Math.round(quadAllAngle) == 360;
    }

    private PointF calculateIntersections(@NonNull PointF[] pts) {
        final float vectorX03 = pts[3].y - pts[0].y;
        final float vectorY03 = pts[0].x - pts[3].x;
        final float offset03 = vectorX03 * pts[0].x + vectorY03 * pts[0].y;

        final float vectorX12 = pts[2].y - pts[1].y;
        final float vectorY12 = pts[1].x - pts[2].x;
        final float offset12 = vectorX12 * pts[1].x + vectorY12 * pts[1].y;

        float xM;
        float yM;

        if (vectorX03 == 0) {
            xM = 0;
            yM = offset03 / vectorY03;
            return new PointF(xM, yM);
        }

        if (vectorY03 == 0) {
            yM = 0;
            xM = offset03 / vectorX03;
            return new PointF(xM, yM);
        }

        if (vectorX12 == 0) {
            xM = 0;
            yM = offset12 / vectorY12;
            return new PointF(xM, yM);
        }

        if (vectorY12 == 0) {
            yM = 0;
            xM = offset12 / vectorX12;
            return new PointF(xM, yM);
        }

        xM = (offset03 - ((vectorY03 * offset12) / vectorY12)) / (vectorX03 - vectorX12 * (vectorY03 / vectorY12));
        yM = (offset03 - vectorX03 * xM) / vectorY03;

        return new PointF(xM, yM);
    }

    private double calculateAngle(float p1X, float p1Y, float p2X, float p2Y, float p3X, float p3Y) {
        float vectorX12 = p2Y - p1Y;
        float vectorY12 = p1X - p2X;

        float vectorX13 = p3Y - p1Y;
        float vectorY13 = p1X - p3X;

        double scalar = vectorX12 * vectorX13 + vectorY12 * vectorY13;
        double absV12 = Math.sqrt(vectorX12 * vectorX12 + vectorY12 * vectorY12);
        double absV13 = Math.sqrt(vectorX13 * vectorX13 + vectorY13 * vectorY13);

        double cosAlpha = scalar / (absV12 * absV13);
        double angleRad = Math.acos(cosAlpha);

        return (angleRad * 180) / Math.PI;
    }

    public void setMainMatrix(Matrix mainMatrix) {
        this.matrix = mainMatrix;
    }

    public void setInitedMatrix(boolean initedMatrix) {
        this.isInitedMatrix = initedMatrix;
    }

    private void drawUnSelectBackground(@NonNull Canvas canvas) {
        final Paint unSelectP = new Paint();
        unSelectP.setColor(ContextCompat.getColor(this.mContext, R.color.color_black_60));
        unSelectP.setAntiAlias(true);

        final float pointRadius = this.pointer1.getWidth() / 2.0f;

        Path selectRectPath = new Path();
        selectRectPath.setFillType(Path.FillType.EVEN_ODD);
        selectRectPath.moveTo(this.pointer1.getX() + pointRadius, this.pointer1.getY() + pointRadius);
        selectRectPath.lineTo(this.pointer3.getX() + pointRadius, this.pointer3.getY() + pointRadius);
        selectRectPath.lineTo(this.pointer4.getX() + pointRadius, this.pointer4.getY() + pointRadius);
        selectRectPath.lineTo(this.pointer2.getX() + pointRadius, this.pointer2.getY() + pointRadius);
        selectRectPath.close();

        Path backgroundPath = new Path();
        backgroundPath.setFillType(Path.FillType.EVEN_ODD);
        backgroundPath.addRect(this.minVertextX, this.minVertextY, this.maxVertextX, this.maxVertextY, Path.Direction.CW);
        backgroundPath.addPath(selectRectPath);

        canvas.drawPath(backgroundPath, unSelectP);
    }

    private void drawCircleChanged(@NonNull Canvas canvas) {
        canvas.save();
        this.matrix = new Matrix();
        this.matrix.reset();
        this.matrix.postScale(2.0f, 2.0f, this.zoomPT.x + this.pointer1.getWidth(), this.zoomPT.y + this.pointer1.getHeight());
        this.circelPaint.getShader().setLocalMatrix(this.matrix);

        if (this.isRevert) {
            float width = ((float) this.getWidth()) - this.zoomPT.x;
            canvas.translate(width - ((float) this.mLoupeRadius), (-this.zoomPT.y) + ((float) this.mLoupeRadius));
        } else {
            canvas.translate((-this.zoomPT.x) + ((float) this.mLoupeRadius), (-this.zoomPT.y) + ((float) this.mLoupeRadius));
        }

        canvas.drawCircle(this.zoomPT.x, this.zoomPT.y, (float) this.mLoupeRadius, this.circelPaint);
        canvas.drawCircle(this.zoomPT.x, this.zoomPT.y, (float) this.mLoupeRadius, this.borderPaint);
        Rect rect = new Rect();
        this.textPaint.getTextBounds("+", 0, 1, rect);
        canvas.drawText("+", this.zoomPT.x, this.zoomPT.y + ((float) (rect.height() / 2)), this.textPaint);
        canvas.restore();
    }

    public void initMatrix() {
        this.isInitedMatrix = true;
        Shader.TileMode tileMode = Shader.TileMode.CLAMP;
        BitmapShader mShader = new BitmapShader(this.mBitmap, tileMode, tileMode);

        this.circelPaint = new Paint();
        this.circelPaint.setShader(mShader);

        this.textPaint = new Paint();
        this.textPaint.setColor(ContextCompat.getColor(this.mContext, R.color.colorAccent));
        this.textPaint.setTextAlign(Paint.Align.CENTER);
        this.textPaint.setTypeface(Typeface.DEFAULT);
        this.textPaint.setAntiAlias(true);
        this.textPaint.setTextSize(80.0f);

        this.borderPaint = new Paint();
        this.borderPaint.setColor(ContextCompat.getColor(this.mContext, R.color.colorAccent));
        this.borderPaint.setDither(true);
        this.borderPaint.setAntiAlias(true);
        this.borderPaint.setStyle(Paint.Style.STROKE);
        this.borderPaint.setStrokeWidth(4);
        this.borderPaint.setStrokeCap(Paint.Cap.SQUARE);
    }

    public Map<Integer, PointF> getOrderedPoints(@NonNull List<PointF> points) {
        PointF centerPoint = new PointF();
        int size = points.size();

        for (PointF pointF : points) {
            centerPoint.x += pointF.x / size;
            centerPoint.y += pointF.y / size;
        }

        Map<Integer, PointF> orderedPoints = new HashMap<>();
        for (PointF pointF : points) {
            int index = -1;
            if (pointF.x < centerPoint.x && pointF.y < centerPoint.y) {
                index = 0;
            } else if (pointF.x > centerPoint.x && pointF.y < centerPoint.y) {
                index = 1;
            } else if (pointF.x < centerPoint.x && pointF.y > centerPoint.y) {
                index = 2;
            } else if (pointF.x > centerPoint.x && pointF.y > centerPoint.y) {
                index = 3;
            }
            orderedPoints.put(index, pointF);
        }

        return orderedPoints;
    }

    public void setVertextListener(OnVertextPointChangeListener vertextListener) {
        this.vertextListener = vertextListener;
    }

    public interface OnVertextPointChangeListener {

        void onVertextMove();

        void onVertextStop();
    }

    private class MidPointTouchListenerImpl implements OnTouchListener {

        private final View mainPointer1;
        private final View mainPointer2;

        PointF downPT = new PointF(); // Record Mouse Position When Pressed Down
        PointF startPT = new PointF(); // Record Start Position of 'img'

        public MidPointTouchListenerImpl(View mainPointer1, View mainPointer2) {
            this.mainPointer1 = mainPointer1;
            this.mainPointer2 = mainPointer2;
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, @NonNull MotionEvent event) {
            int eid = event.getAction();
            final int offset = pointer1.getWidth() / 2;

            switch (eid) {
                case MotionEvent.ACTION_MOVE:
                    PointF mv = new PointF(event.getX() - downPT.x, event.getY() - downPT.y);

                    if (Math.abs(mainPointer1.getX() - mainPointer2.getX()) > Math.abs(mainPointer1.getY() - mainPointer2.getY())) {
                        if (((mainPointer2.getY() + mv.y + v.getHeight() < maxVertextY + offset) && (mainPointer2.getY() + mv.y > minVertextY - offset))) {
                            v.setY(startPT.y + mv.y);
                            startPT = new PointF(v.getX(), v.getY());
                            mainPointer2.setY(Math.abs(mainPointer2.getY() + mv.y));
                        } else {
                            break;
                        }

                        if (((mainPointer1.getY() + mv.y + v.getHeight() < maxVertextY + offset) && (mainPointer1.getY() + mv.y > minVertextY - offset))) {
                            v.setY(startPT.y + mv.y);
                            startPT = new PointF(v.getX(), v.getY());
                            mainPointer1.setY(Math.abs(mainPointer1.getY() + mv.y));
                        } else {
                            break;
                        }
                    } else {
                        if ((mainPointer2.getX() + mv.x + v.getWidth() < maxVertextX + offset) && (mainPointer2.getX() + mv.x > minVertextX - offset)) {
                            v.setX(startPT.x + mv.x);
                            startPT = new PointF(v.getX(), v.getY());
                            mainPointer2.setX(mainPointer2.getX() + mv.x);
                        } else {
                            break;
                        }

                        if ((mainPointer1.getX() + mv.x + v.getWidth() < maxVertextX + offset) && (mainPointer1.getX() + mv.x > minVertextX - offset)) {
                            v.setX(startPT.x + mv.x);
                            startPT = new PointF(v.getX(), v.getY());
                            mainPointer1.setX(mainPointer1.getX() + mv.x);
                        } else {
                            break;
                        }
                    }
                    break;

                case MotionEvent.ACTION_DOWN:
                    downPT.x = event.getX();
                    downPT.y = event.getY();
                    startPT = new PointF(v.getX(), v.getY());
                    vertextListener.onVertextMove();
                    break;

                case MotionEvent.ACTION_UP:
                    int color = ContextCompat.getColor(mContext, isValidShape() ? R.color.colorAccent : R.color.color_orange);
                    selectionRectPaint.setColor(color);
                    vertextListener.onVertextStop();
                    break;
                default:
                    break;
            }

            invalidate();
            return true;
        }
    }

    private class TouchListenerImpl implements OnTouchListener {

        PointF downPT = new PointF(); // Record Mouse Position When Pressed Down
        PointF startPT = new PointF(); // Record Start Position of 'img'

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            final int offset = pointer1.getWidth() / 2;

            if (!isInitedMatrix) {
                initMatrix();
            }

            zoomPT = new PointF(v.getX(), v.getY());
            int mWidthDivider = maxVertextX / 2;
            int mHeightDivider = maxVertextY / 2;
            if (v.getX() < ((float) mWidthDivider) || v.getY() < ((float) mHeightDivider)) {
                if (v.getY() < ((float) (mLoupeRadius * 2))) {
                    if (v.getX() < ((float) mWidthDivider)) {
                        isRevert = true;
                    } else if (v.getX() > ((float) mWidthDivider)) {
                        isRevert = false;
                    } else {
                        isRevert = false;
                    }
                } else {
                    isRevert = false;
                }
            } else {
                isRevert = false;
            }

            int eid = event.getAction();
            switch (eid) {
                case MotionEvent.ACTION_MOVE:
                    mIsTouching = true;
                    PointF mv = new PointF(event.getX() - downPT.x, event.getY() - downPT.y);

                    if (((startPT.x + mv.x + v.getWidth()) <= maxVertextX + offset
                            && (startPT.y + mv.y + v.getHeight() <= maxVertextY + offset))
                            && ((startPT.x + mv.x) >= minVertextX - offset && startPT.y + mv.y >= minVertextY - offset)) {
                        v.setX(startPT.x + mv.x);
                        v.setY(startPT.y + mv.y);
                        startPT = new PointF(v.getX(), v.getY());
                    }
                    break;

                case MotionEvent.ACTION_DOWN:
                    mIsTouching = true;
                    downPT.x = event.getX();
                    downPT.y = event.getY();
                    startPT = new PointF(v.getX(), v.getY());
                    vertextListener.onVertextMove();
                    break;

                case MotionEvent.ACTION_UP:
                    mIsTouching = false;
                    int color = ContextCompat.getColor(mContext, isValidShape() ? R.color.colorAccent : R.color.color_orange);
                    selectionRectPaint.setColor(color);
                    vertextListener.onVertextStop();
                    break;

                default:
                    break;
            }

            invalidate();
            return true;
        }
    }

}
