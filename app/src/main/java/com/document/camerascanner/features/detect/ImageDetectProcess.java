package com.document.camerascanner.features.detect;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.document.camerascanner.utils.AppUtils;
import com.document.camerascanner.utils.Constants;
import com.document.camerascanner.utils.DbUtils;
import com.document.camerascanner.utils.ImageUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class ImageDetectProcess {

    private static final int MAX_HEIGHT_RESIZE = 550;

    private final Context context;

    private float orgRatioX;
    private float orgRatioY;

    private ImageDetectCallback callback;

    public ImageDetectProcess(Context context) {
        this.context = context;
    }

    public void setCallback(ImageDetectCallback callback) {
        this.callback = callback;
    }

    public void convertUriToBitmap(Context context, Uri uri) {
        Single.create((SingleOnSubscribe<Bitmap>) emitter -> {
            try {
                int[] reqSize = AppUtils.getScreenSize((Activity) context);
                Bitmap orginalBitmap = ImageUtils.decodeOrginalBitmap(uri, reqSize[0], reqSize[1]);
                if (orginalBitmap == null) {
                    emitter.onError(new NullPointerException());
                    return;
                }
                emitter.onSuccess(orginalBitmap);
            } catch (Exception e) {
                emitter.onError(e);
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Bitmap>() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        if (callback != null) {
                            callback.onSubsribeDispose(d);
                        }
                    }

                    @Override
                    public void onSuccess(@io.reactivex.annotations.NonNull Bitmap bitmap) {
                        if (callback != null) {
                            callback.onProcessUriSuccess(bitmap);
                        }
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

    public void processDetectRect(Bitmap bitmap) {
        Single.create((SingleOnSubscribe<Map<Integer, PointF>>) emitter -> {
            try {
                Bitmap bmTempt = this.getTemptBitmap(bitmap);
                List<PointF> listDetectPointF = this.getDetectedPointF(bmTempt, bitmap.getWidth(), bitmap.getHeight());
                if (listDetectPointF == null) {
                    emitter.onError(new NullPointerException());
                    return;
                }
                emitter.onSuccess(this.getOrderedPoints(listDetectPointF));
            } catch (Exception e) {
                e.printStackTrace();
                emitter.onError(e);
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Map<Integer, PointF>>() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        if (callback != null) {
                            callback.onSubsribeDispose(d);
                        }
                    }

                    @Override
                    public void onSuccess(@io.reactivex.annotations.NonNull Map<Integer, PointF> pointFMap) {
                        if (callback != null) {
                            callback.onDetectSuscces(pointFMap);
                        }
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        if (callback != null) {
                            callback.onDetectError();
                        }
                    }
                });
    }

    public void wrapTransformImage(Bitmap bitmap, Uri uri) {
        Completable.create(emitter -> {
            this.getCropImageUri(bitmap, uri);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        if (callback != null) {
                            callback.onSubsribeDispose(d);
                        }
                    }

                    @Override
                    public void onComplete() {
                        if (callback != null) {
                            callback.onWarpTransformSucces();
                        }
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        e.printStackTrace();
                        if (callback != null) {
                            callback.onWarpError();
                        }
                    }
                });
    }

    public void wrapTransformImage(Bitmap bitmap, @NonNull PointF[] points, Uri uri) {
        Completable.create(emitter -> {
            try {
                Mat src = new Mat();
                Utils.bitmapToMat(bitmap, src);

                Point ul = new Point(points[0].x, points[0].y);
                Point ur = new Point(points[1].x, points[1].y);
                Point ll = new Point(points[2].x, points[2].y);
                Point lr = new Point(points[3].x, points[3].y);

                double d1 = Math.sqrt((ul.x - ur.x) * (ul.x - ur.x) + (ul.y - ur.y) * (ul.y - ur.y));
                double d2 = Math.sqrt((ul.x - ll.x) * (ul.x - ll.x) + (ul.y - ll.y) * (ul.y - ll.y));
                double d3 = Math.sqrt((lr.x - ur.x) * (lr.x - ur.x) + (lr.y - ur.y) * (lr.y - ur.y));
                double d4 = Math.sqrt((ll.x - ur.x) * (ll.x - ur.x) + (ll.y - ur.y) * (ll.y - ur.y));

                double rW = Math.min(d1, d4);
                double rH = Math.min(d2, d3);

                int rW1 = (int) rW;
                int rH1 = (int) rH;

                rW1 = Math.min(rW1, bitmap.getWidth());
                rH1 = Math.min(rH1, bitmap.getHeight());

                Mat resultMat = new Mat(rW1, rH1, src.type());
                Mat srcMat = new MatOfPoint2f(ul, ur, lr, ll);
                Mat dstMat = new MatOfPoint2f(new Point(0, 0), new Point(src.width(), 0),
                        new Point(src.width(), src.height()), new Point(0, src.height()));

                Mat M = Imgproc.getPerspectiveTransform(srcMat, dstMat);
                Imgproc.warpPerspective(src, resultMat, M, src.size());
                Utils.matToBitmap(resultMat, bitmap);

                src.release();
                resultMat.release();
                srcMat.release();
                dstMat.release();
                M.release();
                this.getCropImageUri(Bitmap.createScaledBitmap(bitmap, rW1, rH1, false), uri);
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        if (callback != null) {
                            callback.onSubsribeDispose(d);
                        }
                    }

                    @Override
                    public void onComplete() {
                        if (callback != null) {
                            callback.onWarpTransformSucces();
                        }
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        e.printStackTrace();
                        if (callback != null) {
                            callback.onWarpError();
                        }
                    }
                });
    }

    private void getCropImageUri(@NonNull Bitmap bitmap, @NonNull Uri uri) {
        if (uri.getPath() == null) {
            return;
        }

        String orgPath = uri.getPath();
        String cropPath = orgPath.replaceAll(Constants.IMAGE_ORIGINAL, Constants.IMAGE_CROP);
        File cropFile = new File(cropPath);

        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, new FileOutputStream(cropFile.getPath()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<PointF> getDetectedPointF(Bitmap bitmap, int maxW, int maxH) {
        List<PointF> listDetectRs = new ArrayList<>();
        Mat src = new Mat();
        Utils.bitmapToMat(bitmap, src);

        this.orgRatioX = (float) maxW / src.width();
        this.orgRatioY = (float) maxH / src.height();

//        Point[] points = this.findRect(src);
        Point[] points = this.findRectV2(src);
        src.release();

        if (points == null) {
            return null;
        }

        this.setContourEdgePoints(listDetectRs, points);
        return listDetectRs;
    }

    private Point[] findRect(@NonNull Mat src, Mat src2) {
        Mat blurred = new Mat();
        Imgproc.medianBlur(src2, blurred, 9);
        Mat gray0 = new Mat(blurred.size(), CvType.CV_8U);
        Mat gray = new Mat();
//
        List<MatOfPoint> contours = new ArrayList<>();
        List<MatOfPoint2f> squares = new ArrayList<>();

        for (int c = 0; c < 3; c++) {
            int[] ch = {c, 0};
//            Core.mixChannels(blurredChannel, gray0Channel, new MatOfInt(ch));

//            int thresholdLevel = 2;
            for (int l = 0; l < 2; l++) {
                Mat mapL;
                if (l == 0) {
                    mapL = src.clone();

                } else {
                    mapL = new Mat();
//                    Imgproc.adaptiveThreshold(gray0, mapL, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 8);
                    Imgproc.Canny(mapL, mapL, 50, 120, 3);
                }

                Mat ct = new Mat(mapL.size(), mapL.type(), new Scalar(0));
                Imgproc.findContours(mapL, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
//                Imgproc.drawContours(ct, contours, -1, new Scalar(255, 255, 255), 5);

                for (int i = 0; i < contours.size(); i++) {
                    MatOfPoint2f tempt = new MatOfPoint2f(contours.get(i).toArray());
                    MatOfPoint2f approxCurve = new MatOfPoint2f();
                    Imgproc.approxPolyDP(tempt, approxCurve, Imgproc.arcLength(tempt, true) * 0.02, true);

                    if (approxCurve.total() == 4 && Imgproc.contourArea(tempt) > 150) {
                        double maxCosine = 0;
                        List<Point> curves = approxCurve.toList();
                        for (int j = 2; j < 5; j++) {
                            double cosine = Math.abs(this.getAngle(curves.get(j % 4), curves.get(j - 2), curves.get(j - 1)));
                            maxCosine = Math.max(maxCosine, cosine);
                        }

                        if (maxCosine < 0.5) {
                            squares.add(approxCurve);
                        }
                    }
                }
            }
        }

        double largestArea = -1;
        int largestIndex = 0;
        for (int i = 0; i < squares.size(); i++) {
            double a = Imgproc.contourArea(squares.get(i));
            if (a > largestArea) {
                largestArea = a;
                largestIndex = i;
            }
        }

        blurred.release();
        gray.release();
        gray0.release();

        if (squares.size() != 0) {
            return this.sortPoints(squares.get(largestIndex).toArray());
        } else {
            return null;
        }
    }

    public List<int[]> generateCombination(int n) {
        List<int[]> combinations = new ArrayList<>();
        this.helper(combinations, new int[4], 0, n - 1, 0);
        return combinations;
    }

    private void helper(List<int[]> combinations, int @NotNull [] data, int start, int end, int i) {
        if (i == data.length) {
            int[] combination = data.clone();
            combinations.add(combination);
        } else if (start <= end) {
            data[i] = start;
            this.helper(combinations, data, start + 1, end, i + 1);
            this.helper(combinations, data, start + 1, end, i);
        }
    }

    public boolean isValidShape(Point[] points) {
        for (Point point : points) {
            if (point == null) {
                return false;
            }
        }

        Point point1 = points[0];
        Point point2 = points[1];
        Point point3 = points[2];
        Point point4 = points[3];

        double a1 = this.calculateAngle(point1.x, point1.y, point2.x, point2.y, point3.x, point3.y);
        double a2 = this.calculateAngle(point3.x, point3.y, point1.x, point1.y, point4.x, point4.y);
        double a3 = this.calculateAngle(point2.x, point2.y, point1.x, point1.y, point4.x, point4.y);
        double a4 = this.calculateAngle(point4.x, point4.y, point2.x, point2.y, point3.x, point3.y);

        if (65 > a1 || a1 > 115) {
            return false;
        }

        if (65 > a2 || a2 > 115) {
            return false;
        }

        if (65 > a3 || a3 > 115) {
            return false;
        }

        if (65 > a4 || a4 > 115) {
            return false;
        }

        double quadAllAngle = a1 + a2 + a3 + a4;
        long round = Math.round(quadAllAngle);
        Log.d("datnt", "isValidShape: " + round);
        return round == 360;
    }

    private double calculateAngle(double p1X, double p1Y, double p2X, double p2Y, double p3X, double p3Y) {
        float vectorX12 = (float) (p2Y - p1Y);
        float vectorY12 = (float) (p1X - p2X);

        float vectorX13 = (float) (p3Y - p1Y);
        float vectorY13 = (float) (p1X - p3X);

        double scalar = vectorX12 * vectorX13 + vectorY12 * vectorY13;
        double absV12 = Math.sqrt(vectorX12 * vectorX12 + vectorY12 * vectorY12);
        double absV13 = Math.sqrt(vectorX13 * vectorX13 + vectorY13 * vectorY13);

        double cosAlpha = scalar / (absV12 * absV13);
        double angleRad = Math.acos(cosAlpha);

        return (angleRad * 180) / Math.PI;
    }

    private double calculateConfident(MatOfPoint corners, Point[] coordinators) {
        Point point1 = coordinators[0];
        Point point2 = coordinators[1];
        Point point3 = coordinators[2];
        Point point4 = coordinators[3];

        int cornerCount = 0;
        int[] cornersData = new int[(int) (corners.total() * corners.channels())];
        corners.get(0, 0, cornersData);

        if (this.isConert(corners, point1)) {
            cornerCount += 10;
        }

        if (this.isConert(corners, point2)) {
            cornerCount += 10;
        }

        if (this.isConert(corners, point3)) {
            cornerCount += 10;
        }

        if (this.isConert(corners, point4)) {
            cornerCount += 10;
        }

        Log.d("datnt", "cornert: " + cornerCount);


        double arena = ((point1.x * point2.y + point2.x * point3.y + point3.x * point4.y + point4.x * point1.y)
                - (point2.x * point1.y + point3.x * point2.y + point4.x * point3.y + point1.x * point4.y)) / 2;

        if (cornerCount == 40) {
            arena += 1000;
        }

        return ((cornerCount + 1) * arena) / 5;
    }

    private Point[] findRectV2(Mat src) {
        Mat grayMat = new Mat();
        Mat cannyEdges = new Mat();
        Mat lines = new Mat();
        Mat eClone = new Mat();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(10, 10));

        Imgproc.cvtColor(src, eClone, Imgproc.COLOR_BGR2GRAY, 4);
        Imgproc.cvtColor(src, grayMat, Imgproc.COLOR_BGR2GRAY, 4);
        Imgproc.GaussianBlur(grayMat, grayMat, new Size(5, 5), 0);

        Imgproc.erode(grayMat, grayMat, kernel);
        Imgproc.dilate(grayMat, grayMat, kernel);
        Imgproc.dilate(grayMat, grayMat, kernel);
        Imgproc.erode(grayMat, grayMat, kernel);

        Imgproc.Canny(grayMat, cannyEdges, 50, 200);

        MatOfPoint corners = new MatOfPoint();
        double qualityLevel = 0.01;
        double minDistance = 15;
        int blockSize = 5;
        int gradientSize = 3;
        double k = 0.04;

        /// Copy the source image
        Mat copy = src.clone();

        /// Apply corner detection
        Imgproc.goodFeaturesToTrack(eClone, corners, 35, qualityLevel, minDistance, new Mat(),
                blockSize, gradientSize, true, k);

        /// Draw corners detected
        int[] cornersData = new int[(int) (corners.total() * corners.channels())];
        corners.get(0, 0, cornersData);
        int radius = 1;
        for (int i = 0; i < corners.rows(); i++) {
            Imgproc.circle(copy, new Point(cornersData[i * 2], cornersData[i * 2 + 1]), radius, new Scalar(255, 255, 255), 2);
        }

        Imgcodecs.imwrite(DbUtils.getSaveDir(context).getPath() + "/" + "4.jpg", copy);

        List<Point> listInterSection = this.houghTransform(cannyEdges, lines);

        if (listInterSection.isEmpty() || listInterSection.size() < 4) {
            return this.findRectByContour(grayMat, cannyEdges, lines);
        }

        if (listInterSection.size() == 4) {
            Point[] points = new Point[4];
            for (int i = 0; i < listInterSection.size(); i++) {
                points[i] = listInterSection.get(i);
            }
            return points;
        }

        List<Quadrilateral> quadrilaterals = new ArrayList<>();
        List<int[]> combinations = this.generateCombination(listInterSection.size());

        for (int[] combination : combinations) {
            Point[] sortPoint = this.sortPointsV2(new Point[]{listInterSection.get(combination[0]),
                    listInterSection.get(combination[1]),
                    listInterSection.get(combination[2]),
                    listInterSection.get(combination[3])});

            if (this.isValidShape(sortPoint)) {
                quadrilaterals.add(new Quadrilateral(this.sortPoints(sortPoint)));
            }

//            Point[] sortPoint = new Point[]{listInterSection.get(combination[0]),
//                    listInterSection.get(combination[1]),
//                    listInterSection.get(combination[2]),
//                    listInterSection.get(combination[3])};
//
//            quadrilaterals.add(new Quadrilateral(sortPoint));

//            Point[] sortPoint = this.sortPoints(new Point[]{listInterSection.get(combination[0]),
//                    listInterSection.get(combination[1]),
//                    listInterSection.get(combination[2]),
//                    listInterSection.get(combination[3])});
//
//            quadrilaterals.add(new Quadrilateral(sortPoint));
        }

        Log.d("datnt", "good quad " + quadrilaterals.size());

        if (!quadrilaterals.isEmpty()) {
            for (Quadrilateral quadrilateral : quadrilaterals) {
                double confident = this.calculateConfident(corners, quadrilateral.getCoordinators());
                quadrilateral.setConfident(confident);
            }

            double maxConfident = 0;
            int maxPos = 0;
            for (int i = 0; i < quadrilaterals.size(); i++) {
                double area = quadrilaterals.get(i).getConfident();
                if (area > maxConfident) {
                    maxConfident = area;
                    maxPos = i;
                }
            }

            Mat mapL = new Mat(cannyEdges.size(), cannyEdges.type(), new Scalar(0));
            Point[] points = quadrilaterals.get(maxPos).getCoordinators();
            Imgproc.line(mapL, points[0], points[1], new Scalar(255, 255, 255), 1);
            Imgproc.line(mapL, points[1], points[2], new Scalar(255, 255, 255), 1);
            Imgproc.line(mapL, points[2], points[3], new Scalar(255, 255, 255), 1);
            Imgproc.line(mapL, points[3], points[0], new Scalar(255, 255, 255), 1);
            Imgcodecs.imwrite(DbUtils.getSaveDir(context).getPath() + "/" + "5.jpg", mapL);
            Log.d("datnt", "point: " + Arrays.toString(points));
            return points;
        }

        return this.findRectByContour(grayMat, cannyEdges, lines);
    }

    @Nullable
    private Point[] findRectByContour(Mat grayMat, Mat cannyEdges, Mat lines) {
        List<MatOfPoint> contours = new ArrayList<>();
        Mat heracy = new Mat();
        Imgproc.findContours(cannyEdges, contours, heracy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        heracy.release();

        Collections.sort(contours, (MatOfPoint lhs, MatOfPoint rhs) -> Double.compare(Imgproc.contourArea(rhs), Imgproc.contourArea(lhs)));

        double maxArea = 0;
        int maxPos = 0;
        MatOfPoint2f largest;
        for (int i = 0; i < contours.size(); i++) {
            double area = Imgproc.contourArea(contours.get(i));
            if (area > maxArea) {
                maxArea = area;
                maxPos = i;
            }
        }

        if (maxArea < 10) {
            return null;
        }

        MatOfPoint2f c = new MatOfPoint2f();
        MatOfPoint2f approx = new MatOfPoint2f();
        contours.get(maxPos).convertTo(c, CvType.CV_32FC2);
        Imgproc.approxPolyDP(c, approx, Imgproc.arcLength(c, true) * 0.02, true);
        largest = approx;

        Point[] points;
        points = this.sortPoints(largest.toArray());

        largest.release();
        grayMat.release();
        cannyEdges.release();
        lines.release();

        return points;
    }

    private List<Point> houghTransform(Mat cannyEdges, Mat lines) {
        Mat mapL = new Mat(cannyEdges.size(), cannyEdges.type(), new Scalar(0));
        Mat mapP = new Mat(cannyEdges.size(), cannyEdges.type(), new Scalar(0));

        Imgproc.HoughLines(cannyEdges, lines, 1, Math.PI / 180, 50);
/*
        for (int i = 0; i < lines.rows(); i++) {
            double[] data = lines.get(i, 0);
            double rho = data[0];
            double theta = data[1];
            double a = Math.cos(theta);
            double b = Math.sin(theta);
            double x0 = a * rho;
            double y0 = b * rho;
            Point pt1 = new Point();
            Point pt2 = new Point();
            pt1.x = Math.round(x0 + 1000 * (-b));
            pt1.y = Math.round(y0 + 1000 * (a));
            pt2.x = Math.round(x0 - 1000 * (-b));
            pt2.y = Math.round(y0 - 1000 * (a));
            Imgproc.line(mapL, pt1, pt2, new Scalar(255, 255, 255), 1);
        }
*/

//        Imgcodecs.imwrite(DbUtils.getSaveDir(context).getPath() + "/" + System.currentTimeMillis() + "_2.jpg", mapL);

//        List<double[]> filterdLines = new ArrayList<>();
        ArrayList<Point> listIntersecPoint = new ArrayList<>();
        for (int i = 0; i < lines.rows(); i++) {
            for (int j = i + 1; j < lines.rows(); j++) {
                double[] line1 = lines.get(i, 0);
                double[] line2 = lines.get(j, 0);

                if (line1 != null && line2 != null) {
                    double[] lineP1 = this.getLineP(line1);
                    double[] lineP2 = this.getLineP(line2);
                    Point pt = this.findIntersection(lineP1, lineP2);

                    if (pt.x >= 0 && pt.y >= 0 && pt.x <= cannyEdges.cols() && pt.y < cannyEdges.rows()) {
                        Imgproc.circle(cannyEdges, pt, 1, new Scalar(255, 255, 255), 2);

                        if (!this.isExists(listIntersecPoint, pt) && this.isValidAngle(lineP1, lineP2)) {
                            Imgproc.line(mapL, new Point(lineP1[0], lineP1[1]), new Point(lineP1[2], lineP1[3]), new Scalar(255, 255, 255), 1);
                            Imgproc.line(mapL, new Point(lineP2[0], lineP2[1]), new Point(lineP2[2], lineP2[3]), new Scalar(255, 255, 255), 1);
                            Imgproc.circle(mapP, pt, 1, new Scalar(255, 255, 255), 2);
                            listIntersecPoint.add(pt);
                        }
                    }
                }
            }
        }

        Imgcodecs.imwrite(DbUtils.getSaveDir(context).getPath() + "/" + "2.jpg", mapL);
        Imgcodecs.imwrite(DbUtils.getSaveDir(context).getPath() + "/" + "1.jpg", cannyEdges);
        Imgcodecs.imwrite(DbUtils.getSaveDir(context).getPath() + "/" + "3.jpg", mapP);
        Log.d("datnt", "corrr: " + listIntersecPoint.size());
        return listIntersecPoint;
    }

    private double[] getLineP(double[] data) {
        double rho = data[0];
        double theta = data[1];
        double a = Math.cos(theta);
        double b = Math.sin(theta);
        double x0 = a * rho;
        double y0 = b * rho;
        Point pt1 = new Point();
        Point pt2 = new Point();
        pt1.x = Math.round(x0 + 1000 * (-b));
        pt1.y = Math.round(y0 + 1000 * (a));
        pt2.x = Math.round(x0 - 1000 * (-b));
        pt2.y = Math.round(y0 - 1000 * (a));
        return new double[]{pt1.x, pt1.y, pt2.x, pt2.y};
    }

    private boolean isConert(MatOfPoint corners, Point point) {
        int[] cornersData = new int[(int) (corners.total() * corners.channels())];
        corners.get(0, 0, cornersData);

        for (int i = 0; i < corners.rows(); i++) {
            if (Math.sqrt(Math.pow(cornersData[i * 2] - point.x, 2) + Math.pow(cornersData[i * 2 + 1] - point.y, 2)) < 10) {
                return true;
            }
        }
        return false;
    }

    private boolean isExists(ArrayList<Point> corners, Point pt) {
        for (int i = 0; i < corners.size(); i++) {
            if (Math.sqrt(Math.pow(corners.get(i).x - pt.x, 2) + Math.pow(corners.get(i).y - pt.y, 2)) < 25) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidAngle(double[] line1, double[] line2) {
        double vectorX12 = line1[2] - line1[0];
        double vectorY12 = line1[3] - line1[1];

        double vectorX13 = line2[2] - line2[0];
        double vectorY13 = line2[3] - line2[1];

        double scalar = vectorX12 * vectorX13 + vectorY12 * vectorY13;
        double absV12 = Math.sqrt(vectorX12 * vectorX12 + vectorY12 * vectorY12);
        double absV13 = Math.sqrt(vectorX13 * vectorX13 + vectorY13 * vectorY13);

        double cosAlpha = scalar / (absV12 * absV13);
        double angleRad = Math.acos(cosAlpha);
        double angle = (angleRad * 180) / Math.PI;

//        Log.d("datnt", "isValidAngle: " + angle);
        return 65 < angle && angle < 105;
    }

    private boolean isDuplicate(ArrayList<Point> corners, Point pt) {
        for (int i = 0; i < corners.size(); i++) {
            if (corners.get(i).x == pt.x && corners.get(i).y == pt.y) {
                return true;
            }
        }
        return false;
    }

    private boolean isStrongAngle(double[] line1, double[] line2) {
        double vectorX12 = line1[2] - line1[0];
        double vectorY12 = line1[3] - line1[1];

        double vectorX13 = line2[2] - line2[0];
        double vectorY13 = line2[3] - line2[1];

        double scalar = vectorX12 * vectorX13 + vectorY12 * vectorY13;
        double absV12 = Math.sqrt(vectorX12 * vectorX12 + vectorY12 * vectorY12);
        double absV13 = Math.sqrt(vectorX13 * vectorX13 + vectorY13 * vectorY13);

        double cosAlpha = scalar / (absV12 * absV13);
        double angleRad = Math.acos(cosAlpha);
        double angle = (angleRad * 180) / Math.PI;
        return 85 < angle && angle < 95d;
    }

    private Point findIntersection(double[] line1, double[] line2) {
        double start_x1 = line1[0];
        double start_y1 = line1[1];
        double end_x1 = line1[2];
        double end_y1 = line1[3];
        double start_x2 = line2[0];
        double start_y2 = line2[1];
        double end_x2 = line2[2];
        double end_y2 = line2[3];
        double denominator = ((start_x1 - end_x1) * (start_y2 - end_y2)) - ((start_y1 - end_y1) * (start_x2 - end_x2));

        if (denominator != 0) {
            Point pt = new Point();
            pt.x = ((start_x1 * end_y1 - start_y1 * end_x1) * (start_x2 - end_x2) - (start_x1 - end_x1)
                    * (start_x2 * end_y2 - start_y2 * end_x2)) / denominator;
            pt.y = ((start_x1 * end_y1 - start_y1 * end_x1) * (start_y2 - end_y2) - (start_y1 - end_y1)
                    * (start_x2 * end_y2 - start_y2 * end_x2)) / denominator;
            return pt;
        }

        return new Point(-1, -1);
    }

    private void setContourEdgePoints(List<PointF> listDetectPointM, Point[] points) {
        if (points == null) {
            return;
        }

        listDetectPointM.add(new PointF(Double.valueOf(points[0].x).floatValue() * this.orgRatioX,
                Double.valueOf(points[0].y).floatValue() * this.orgRatioY));

        listDetectPointM.add(new PointF(Double.valueOf(points[1].x).floatValue() * this.orgRatioX,
                Double.valueOf(points[1].y).floatValue() * this.orgRatioY));

        listDetectPointM.add(new PointF(Double.valueOf(points[2].x).floatValue() * this.orgRatioX,
                Double.valueOf(points[2].y).floatValue() * this.orgRatioY));

        listDetectPointM.add(new PointF(Double.valueOf(points[3].x).floatValue() * this.orgRatioX,
                Double.valueOf(points[3].y).floatValue() * this.orgRatioY));
    }

    @NonNull
    private Point[] sortPoints(Point[] src) {
        Point[] result = {null, null, null, null};
        List<Point> srcPoints = new ArrayList<>(Arrays.asList(src));

        Comparator<Point> sumComparator = (lhs, rhs) -> Double.compare(lhs.y + lhs.x, rhs.y + rhs.x);
        Comparator<Point> diffComparator = (lhs, rhs) -> Double.compare(lhs.y - lhs.x, rhs.y - rhs.x);

        result[0] = Collections.min(srcPoints, sumComparator);
        result[2] = Collections.max(srcPoints, sumComparator);
        result[1] = Collections.min(srcPoints, diffComparator);
        result[3] = Collections.max(srcPoints, diffComparator);

        return result;
    }

    private Point[] sortPointsV2(Point[] src) {
        Point[] result = new Point[4];
        Point centerPoint = new Point();

        centerPoint.x = (src[0].x + src[1].x + src[2].x + src[3].x) / 4;
        centerPoint.y = (src[0].y + src[1].y + src[2].y + src[3].y) / 4;

        for (Point point : src) {
            if (point.x < centerPoint.x && point.y < centerPoint.y) {
                result[0] = point;
            } else if (point.x > centerPoint.x && point.y < centerPoint.y) {
                result[1] = point;
            } else if (point.x < centerPoint.x && point.y > centerPoint.y) {
                result[2] = point;
            } else if (point.x > centerPoint.x && point.y > centerPoint.y) {
                result[3] = point;
            }
        }

        return result;
    }

    private Bitmap getTemptBitmap(@NonNull Bitmap bitmap) {
        double ratio = (double) bitmap.getHeight() / MAX_HEIGHT_RESIZE;
        int width = (int) (bitmap.getWidth() / ratio);
        return Bitmap.createScaledBitmap(bitmap, width, MAX_HEIGHT_RESIZE, false);
    }

    public Map<Integer, PointF> getOrderedPoints(List<PointF> points) {
        if (points == null) {
            return null;
        }

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

    private double getAngle(@NonNull Point pt1, @NonNull Point pt2, @NonNull Point pt0) {
        double dx1 = pt1.x - pt0.x;
        double dy1 = pt1.y - pt0.y;
        double dx2 = pt2.x - pt0.x;
        double dy2 = pt2.y - pt0.y;
        return (dx1 * dx2 + dy1 * dy2) / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10);
    }

    public interface ImageDetectCallback {

        void onSubsribeDispose(Disposable dispose);

        void onProcessUriSuccess(Bitmap bitmap);

        void onDetectSuscces(Map<Integer, PointF> mapPoint);

        void onDetectError();

        void onWarpTransformSucces();

        void onWarpError();
    }

}
