package com.childcity.gaussianinterpolation;

import android.annotation.SuppressLint;
import android.app.Application;
import android.graphics.Point;
import android.graphics.PointF;

import com.childcity.gaussianinterpolation.MathParser.MatchParser;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class InterpolationViewModel extends AndroidViewModel {
    static MatchParser Parser = new MatchParser();
    private List<PointF> inputPoints;
    private List<List<PointF>> gaussParametricInputPoints;
    private List<List<PointF>> gaussSummaryInputPoints;

    private double[] gaussNormalBasis = null;
    private double[][] gaussParametricBasis = null; // 2 dimension, because one matrix for X~, and one for Y~
    private double[][] gaussSummaryBasis = null; // 2 dimension, because one matrix for X~, and one for Y~

    private float Xmax; // , xmax, xmin  - максимальне і мінімальне значення аргумента х, тобто значення кінців відрізку.
    private float Xmin;
    private float TMin;
    private float TMaxParametric;
    private float TMaxSummary;

    private double normalAlpha; //default: pi(n-1) / (Xmax - Xmin)^2
    private double parametricAlpha;
    private double summaryAlpha;


    String Alpha;
    IntrpltAlgorithm IntrplAlgorithm = new IntrpltAlgorithm(IntrpltAlgorithm.LAGRANGE);

    public InterpolationViewModel(Application application) {
        super(application);

        Alpha = "pi * (size - 1) / pow((Xmax - Xmin), 2)";

        // set default Points (first app run)
        inputPoints = new ArrayList<>(Arrays.asList(
                // Points from sqrt(x)
                new PointF(0,0), new PointF(1,1),
                new PointF(2.9f,1.7f), new PointF(20,4.5f),
                new PointF(101,10f)
        ));
    }

    float getXmax() {
        return Xmax;
    }

    float getXmin() {
        return Xmin;
    }

    public float getTMin() {
        return TMin;
    }

    public float getTMaxParametric() {
        return TMaxParametric;
    }

    public float getTMaxSummary() {
        return TMaxSummary;
    }

    public double getNormalAlpha() {
        return normalAlpha;
    }

    public double getParametricAlpha() {
        return parametricAlpha;
    }

    public double getSummaryAlpha() {
        return summaryAlpha;
    }


    private interface ITCounter{
        float getXr(int i, float previousT);
    }

    // Method, that build basis matrix and Solve system.
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
        }catch (Exception e){
            Toast.makeText(app, e.toString(), Toast.LENGTH_LONG).show();
            Log.e("Basis", e.toString() + "\n" + Arrays.toString(e.getStackTrace()).replace(",", ",\n"));
            return new double[n];
        }
    }

    private static float GetGaussianPoint(List<PointF> inputPoints,
                                                    double[] parametricBasis, double alpha, float Xl,
                                                    ITCounter tCounter) {
        double G = 0;

        for (int i = 0; i < inputPoints.size(); i++){
            float Xr = inputPoints.get(i).x;
            G += parametricBasis[i] * Math.exp(-alpha * Math.pow(Xl - Xr, 2));
        }

        return (float) G;
    }

    private static PointF getGaussianParametricPoint(List<List<PointF>> parametricInputPoints, double[][] parametricBasis, double alpha, float T){
        ITCounter tCounter = new ITCounter() {
            @Override
            public float getXr(int i, float previousT) {
                return i;
            }
        };
        float Xt = GetGaussianPoint(parametricInputPoints.get(0), /*xBasis*/ parametricBasis[0], alpha, T, tCounter);
        float Yt = GetGaussianPoint(parametricInputPoints.get(1), /*yBasis*/ parametricBasis[1], alpha, T, tCounter);
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
        return GetGaussianPoint(inputPoints, /*yBasis*/ gaussNormalBasis, normalAlpha, X, new ITCounter() {
            @Override
            public float getXr(int i, float previousT) {
                return inputPoints.get(i).x;
            }
        });
    }

    PointF getGaussianParametricPoint(float T){
        return getGaussianParametricPoint(gaussParametricInputPoints, gaussParametricBasis, parametricAlpha, T);
    }

    PointF getGaussianSummaryPoint(float T){
        return getGaussianParametricPoint(gaussSummaryInputPoints, gaussSummaryBasis, summaryAlpha, T);
    }



    void addInputPoint(PointF point){
        inputPoints.add(point);
    }

    void editInputPoint(int index, PointF point){
        inputPoints.get(index).set(point);
    }

    void removeInputPointAt(int index){
        if(index < inputPoints.size() && index >= 0) {
            inputPoints.remove(index);
        }else {
            Log.e("removeInputPointAt", "index is out of bounds!");
        }
    }

    void swapInputPointAt(int index1, int index2){
        if(index2 < inputPoints.size() && index1 < inputPoints.size() && index2 >= 0) {
            Collections.swap(inputPoints, index1, index2);
        }else {
            Log.e("swapInputPointAt", "index1 & index2 is out of bounds!");
        }
    }

    PointF getInputPointAt(int index){
        PointF point = inputPoints.get(index);
        return new PointF(point.x, point.y);
    }

    PointF getGussSummaryXInputPoint(int index){
        PointF point = gaussSummaryInputPoints.get(0).get(index);
        return new PointF(point.x, point.y);
    }

    int getInputPointCount(){
        return inputPoints.size();
    }



    String getInputPoints(){
        StringBuilder sb = new StringBuilder();
        for (PointF point: inputPoints ) {
            sb.append(point.x).append(':').append(point.y).append(";");
        }
        return sb.toString();
    }

    void setInputPoints(String points){
        if(points.equals("") || points.length() == 0)
            return;

        inputPoints.clear();
        for (String point : points.split(";")) {
            String[] coord = point.split(":");
            inputPoints.add(new PointF(Float.valueOf(coord[0]), Float.valueOf(coord[1])));
        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    void prepareParams(){
        Parser.setVariable("size", (double) inputPoints.size());
        Parser.setVariable("pi", Math.PI);

        Xmax = Collections.max(inputPoints, new PointFComparator()).x;
        Xmin = Collections.min(inputPoints, new PointFComparator()).x;


        try {
            Parser.setVariable("Xmin", (double) Xmin);
            Parser.setVariable("Xmax", (double) Xmax);
            normalAlpha = Parser.Parse(Alpha);
            Log.e(Alpha, ""+normalAlpha);
        }catch(Exception e){
            Toast.makeText(getApplication(), "Параметр alpha задан неверно: " + e.getMessage(), Toast.LENGTH_LONG).show();
            normalAlpha = (Math.PI * (inputPoints.size() - 1)) / ((Xmax - Xmin) * (Xmax - Xmin)); // set default alpha
        }


        //////////////////////////NormalBasis

        if(IntrplAlgorithm.test(IntrpltAlgorithm.GAUSSIAN_NORMAL))
            gaussNormalBasis = FindGaussianBasis(inputPoints, normalAlpha, getApplication());

        /////////////////////////ParametricBasis

        List<PointF> xArray = new ArrayList<>(); // X(t)
        List<PointF> yArray = new ArrayList<>(); // Y(t)

        for (int i = 0; i < inputPoints.size(); i++) {
            xArray.add(new PointF(i, inputPoints.get(i).x)); // fill X(t)
            yArray.add(new PointF(i, inputPoints.get(i).y)); // fill Y(t)
        }

        TMin = xArray.get(0).x;
        TMaxParametric = xArray.get(inputPoints.size() - 1).x;

        try {
            Parser.setVariable("Xmin", (double) TMin);
            Parser.setVariable("Xmax", (double) TMaxParametric);
            parametricAlpha = Parser.Parse(Alpha);
            Log.e(Alpha, ""+parametricAlpha);
        }catch(Exception e){
            //Toast.makeText(getApplication(), "Параметр alpha задан неверно: " + e.getMessage(), Toast.LENGTH_LONG).show();
            parametricAlpha = (Math.PI * (inputPoints.size() - 1)) / ((TMaxParametric - TMin) * (TMaxParametric - TMin)); // set default alpha
        }

        if(gaussParametricInputPoints != null)
            gaussParametricInputPoints.clear();

        gaussParametricInputPoints = new ArrayList<>();
        gaussParametricInputPoints.add(xArray);
        gaussParametricInputPoints.add(yArray);

        if(IntrplAlgorithm.test(IntrpltAlgorithm.GAUSSIAN_PARAMETRIC)){
            gaussParametricBasis = new double[2][];
            gaussParametricBasis[0] = FindGaussianBasis(xArray, parametricAlpha, getApplication());
            gaussParametricBasis[1] = FindGaussianBasis(yArray, parametricAlpha, getApplication());
        }

        //////////////////////////////// SummaryBasis

        xArray = new ArrayList<>(); // X(t)
        yArray = new ArrayList<>(); // Y(t)

        float previousT = 0f;
        xArray.add(new PointF(previousT, inputPoints.get(0).x)); // fill X(t)
        yArray.add(new PointF(previousT, inputPoints.get(0).y)); // fill Y(t)

        for (int i = 1; i < inputPoints.size(); i++) {
            PointF prevPoint = inputPoints.get(i - 1);
            PointF currPoint = inputPoints.get(i);
            previousT = previousT + countDistance(prevPoint, currPoint);

            xArray.add(new PointF(previousT, inputPoints.get(i).x)); // fill X(t)
            yArray.add(new PointF(previousT, inputPoints.get(i).y)); // fill Y(t)
        }

        TMaxSummary = xArray.get(inputPoints.size() - 1).x;

        try {
            Parser.setVariable("Xmin", (double) TMin);
            Parser.setVariable("Xmax", (double) TMaxSummary);
            summaryAlpha = Parser.Parse(Alpha);
            Log.e(Alpha, ""+summaryAlpha);
        }catch(Exception e){
            //Toast.makeText(getApplication(), "Параметр alpha задан неверно: " + e.getMessage(), Toast.LENGTH_LONG).show();
            summaryAlpha = (Math.PI * (inputPoints.size() - 1)) / ((TMaxSummary - TMin) * (TMaxSummary - TMin)); // set default alpha
        }

        if(gaussSummaryInputPoints != null)
            gaussSummaryInputPoints.clear();

        gaussSummaryInputPoints = new ArrayList<>();
        gaussSummaryInputPoints.add(xArray);
        gaussSummaryInputPoints.add(yArray);


        if(IntrplAlgorithm.test(IntrpltAlgorithm.GAUSSIAN_SUMMARY)){
            gaussSummaryBasis = new double[2][];
            gaussSummaryBasis[0] = FindGaussianBasis(xArray, summaryAlpha, getApplication());
            gaussSummaryBasis[1] = FindGaussianBasis(yArray, summaryAlpha, getApplication());
        }

    }

    private float countDistance(PointF a, PointF b){
        double x1 = a.x, x2 = b.x,
                y1 = a.y, y2 = b.y;
        return (float) Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1));
    }

}
