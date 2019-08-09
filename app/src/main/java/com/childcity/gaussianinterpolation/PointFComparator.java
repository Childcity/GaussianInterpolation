package com.childcity.gaussianinterpolation;

import android.graphics.PointF;

import java.util.Comparator;

final class PointFComparator implements Comparator<PointF> {
    @Override
    public int compare(PointF left, PointF right) {
        return Float.compare(left.x, right.x);
    }
}
