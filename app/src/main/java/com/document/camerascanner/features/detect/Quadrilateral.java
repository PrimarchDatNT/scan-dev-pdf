package com.document.camerascanner.features.detect;

import org.jetbrains.annotations.NotNull;
import org.opencv.core.Point;

import java.util.Arrays;

public class Quadrilateral {

    private final Point[] coordinators;
    private double confident;

    public Quadrilateral(Point[] coordinators) {
        this.coordinators = coordinators;
    }

    public double getConfident() {
        return this.confident;
    }

    public void setConfident(double confident) {
        this.confident = confident;
    }

    public Point[] getCoordinators() {
        return this.coordinators;
    }

    @Override
    public @NotNull String toString() {
        return "Quadrilateral{" +
                "listCoordinator=" + Arrays.toString(coordinators) +
                ", confident=" + confident +
                '}';
    }
}
