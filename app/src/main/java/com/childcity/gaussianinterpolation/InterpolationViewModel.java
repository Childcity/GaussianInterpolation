package com.childcity.gaussianinterpolation;

import android.annotation.SuppressLint;
import android.app.Application;
import android.graphics.Point;
import android.graphics.PointF;
import com.childcity.gaussianinterpolation.SystemSolver.GaussJordanElimination;
import com.childcity.gaussianinterpolation.SystemSolver.InfiniteSolutionException;
import com.childcity.gaussianinterpolation.SystemSolver.NoSolutionException;

import android.nfc.Tag;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import androidx.arch.core.util.Function;
import androidx.lifecycle.AndroidViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InterpolationViewModel extends AndroidViewModel {
    private List<PointF> inputPoints;
    private List<List<PointF>> gaussParametricInputPoints;

    private double[] gaussNormalBasis = null;
    private double[][] gaussParametricBasis = null; // 2 dimension, because one matrix for X~, and one for Y~

    private double alpha; //default: pi(n-1) / (Xmax - Xmin)^2
    // , xmax, xmin  - максимальне і мінімальне значення аргумента х, тобто значення кінців відрізку.

    IntrpltAlgorithm IntrplAlgorithm = new IntrpltAlgorithm(IntrpltAlgorithm.LAGRANGE);

    public InterpolationViewModel(Application application) {
        super(application);

        inputPoints = new ArrayList<>(Arrays.asList(
                // Points from sqrt(x)
                new PointF(0,0), new PointF(1,1),
                new PointF(2.9f,1.7f), new PointF(20,4.5f),
                new PointF(101,10f)
        ));

        sortInputPointsByX();

        float Xmax = inputPoints.get(inputPoints.size() - 1).x;
        float Xmin = inputPoints.get(0).x;
        alpha = Math.PI * (inputPoints.size() - 1) / Math.pow(Xmax - Xmin, 2); // set default alpha

        prepareParams();
    }


    private interface ITCounter{
        float getXr(int i, float previousT);
    }

    private static double[] FindGaussianBasis(List<PointF> inputPoints, double alpha, Application app){
        int n = inputPoints.size();

        double[][] Ab = new double[n][n + 1]; // n + 1 because the result (Y) will be here

        // Create Basis matrix. Xl -> Xn, Xr -> X[1..n] in formula
        for (int i = 0; i < n; i++) {
            double xL = inputPoints.get(i).x;

            for (int j = 0; j < n; j++) {
                double xR = inputPoints.get(j).x;
                Ab[i][j] = Math.exp(-alpha * Math.pow(xL - xR, 2));
            }

            Ab[i][n] = inputPoints.get(i).y;
        }

        //Toast.makeText(getApplication(),"Test Toast!!!",Toast.LENGTH_LONG).show();

        try {
            return GaussJordanElimination.SolveSystem(Ab, n);
        }catch (NoSolutionException e){
            Toast.makeText(app, "Решения для базисов ф-и Гаусса НЕ найдено!", Toast.LENGTH_LONG).show();
            return new double[n];
        }catch (InfiniteSolutionException e){
            Toast.makeText(app, "Решения для базисов ф-и Гаусса содержит бесконечность!", Toast.LENGTH_LONG).show();
            return new double[n];
        }
    }

    private static float GetGaussianPoint(List<PointF> inputPoints,
                                                    double[] gaussParametricBasis, double alpha, float Xl,
                                                    ITCounter tCounter) {
        double G = 0;

        float Xr = 0f;
        for (int i = 0; i < inputPoints.size(); i++){
            Xr = tCounter.getXr(i, Xr);
            G += gaussParametricBasis[i] * Math.exp(-alpha * Math.pow(Xl - Xr, 2));
        }

        return (float) G;
    }

    private PointF getGaussianParametricPoint(float T, ITCounter tCounter){
        float Xt = GetGaussianPoint(gaussParametricInputPoints.get(0), /*xBasis*/ gaussParametricBasis[0], alpha, T, tCounter);
        float Yt = GetGaussianPoint(gaussParametricInputPoints.get(1), /*yBasis*/ gaussParametricBasis[1], alpha, T, tCounter);
        return new PointF(Xt, Yt);
    }

    float getLagrangePoint(float X){
        double L;

        L = 0;

        for (int i = 0; i < inputPoints.size(); ++i)
        {
            double l = 1;

            for (int j = 0; j < inputPoints.size(); ++j)
                if (i != j)
                    l *= (X - inputPoints.get(j).x) / (inputPoints.get(i).x - inputPoints.get(j).x);

            L += inputPoints.get(i).y * l;
        }

        return (float) L;
    }

    float getGaussianNormalPoint(float X){
        return GetGaussianPoint(inputPoints, /*yBasis*/ gaussNormalBasis, alpha, X, new ITCounter() {
            @Override
            public float getXr(int i, float previousT) {
                return inputPoints.get(i).x;
            }
        });
    }

    PointF getGaussianParametricPoint(float T){
        return getGaussianParametricPoint(T, new ITCounter() {
            @Override
            public float getXr(int i, float previousT) {
                return i;
            }
        });
    }

    PointF getGaussianSummaryPoint(float T){
        return getGaussianParametricPoint(T, new ITCounter() {
            @Override
            public float getXr(int i, float previousT) {
                if(i == 0)
                    return previousT;

                PointF prevPoint = inputPoints.get(i - 1);
                PointF currPoint = inputPoints.get(i);
                return previousT + countDistance(prevPoint, currPoint);
            }
        });
    }



    void addInputPoint(PointF point){
        if(inputPoints.contains(point))
            return;

        inputPoints.add(point);
        prepareParams();
    }

    void editInputPoint(int index, PointF point){
        if(inputPoints.contains(point))
            return;

        inputPoints.get(index).set(point);
        prepareParams();
    }

    void removeInputPointAt(int index){
        if(index < inputPoints.size()) {
            inputPoints.remove(index);
            prepareParams();
        }
    }

    PointF getInputPointAt(int index){
        PointF point = inputPoints.get(index);
        return new PointF(point.x, point.y);
    }

    int getInputPointCount(){
        return inputPoints.size();
    }

    boolean containInputPoint(PointF point){
        return inputPoints.contains(point);
    }

    private void sortInputPointsByX(){
        Collections.sort(inputPoints, new Comparator<PointF>() {
            @Override
            public int compare(PointF left, PointF right) {
                return Float.compare(left.x, right.x);
            }
        });
    }




    Set<String> getInputPoints(){
        Set<String> points = new HashSet<>();
        for (PointF point: inputPoints ) {
            points.add(point.x + ":" + point.y);
        }
        return points;
    }

    void setInputPoints(Set<String> points){
        if(points.isEmpty())
            return;

        inputPoints.clear();
        for (String point : points) {
            String[] coord = point.split(":");
            inputPoints.add(new PointF(Float.valueOf(coord[0]), Float.valueOf(coord[1])));
        }

        sortInputPointsByX();

        float Xmax = inputPoints.get(inputPoints.size() - 1).x;
        float Xmin = inputPoints.get(0).x;
        alpha = Math.PI * (inputPoints.size() - 1) / Math.pow(Xmax - Xmin, 2); // set default alpha

        prepareParams();
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private void prepareParams(){
        sortInputPointsByX();
        gaussNormalBasis = FindGaussianBasis(inputPoints, alpha, getApplication());

        List<PointF> xArray = new ArrayList<>(); // X(t)
        List<PointF> yArray = new ArrayList<>(); // Y(t)

        for (int t = 0; t < inputPoints.size(); t++) {
            xArray.add(new PointF(t, inputPoints.get(t).x)); // fill X(t)
            yArray.add(new PointF(t, inputPoints.get(t).y)); // fill Y(t)
        }

        if(gaussParametricInputPoints != null)
            gaussParametricInputPoints.clear();

        gaussParametricInputPoints = new ArrayList<>();
        gaussParametricInputPoints.add(xArray);
        gaussParametricInputPoints.add(yArray);

        gaussParametricBasis = new double[2][];
        gaussParametricBasis[0] = FindGaussianBasis(xArray, alpha, getApplication());
        gaussParametricBasis[1] = FindGaussianBasis(yArray, alpha, getApplication());

    }

    private float countDistance(PointF a, PointF b){
        double x1 = a.x, x2 = b.x, y1 = a.y, y2 = b.y;
        return (float) Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1));
    }

}
