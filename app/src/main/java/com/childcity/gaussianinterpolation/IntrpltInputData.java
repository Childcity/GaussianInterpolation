package com.childcity.gaussianinterpolation;

import android.graphics.PointF;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class IntrpltInputData {
    String Alpha;
    List<PointF> inputPoints;
    IntrpltAlgorithm IntrplAlgorithm;

    float Xmax; // , xmax, xmin  - максимальне і мінімальне значення аргумента х, тобто значення кінців відрізку.
    float Xmin;

    IntrpltInputData() {
        Alpha = "pi * (size - 1) / pow(Xmax - Xmin, 2)";

        IntrplAlgorithm = new IntrpltAlgorithm(IntrpltAlgorithm.LAGRANGE);

        // set default Points (first app run)
        inputPoints = new ArrayList<>(Arrays.asList(
                // Points from sqrt(x)
                new PointF(0,0), new PointF(1,1),
                new PointF(2.9f,1.7f), new PointF(20,4.5f),
                new PointF(101,10f)
        ));
    }
}
