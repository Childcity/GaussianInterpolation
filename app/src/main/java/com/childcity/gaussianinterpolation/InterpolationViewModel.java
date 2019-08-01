package com.childcity.gaussianinterpolation;

import android.app.Application;
import android.graphics.PointF;
import com.childcity.gaussianinterpolation.SystemSolver.GaussJordanElimination;
import com.childcity.gaussianinterpolation.SystemSolver.InfiniteSolutionException;
import com.childcity.gaussianinterpolation.SystemSolver.NoSolutionException;

import android.util.Log;
import android.widget.Toast;

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

    private double[] gaussBasis = null; // ý - basis of gaussian function
    private double alpha; //default: pi(n-1) / (Xmax - Xmin)^2
                             // , xmax, xmin  - максимальне і мінімальне значення аргумента х, тобто значення кінців відрізку.

    IntrpltAlgorithm IntrplAlgorithm = IntrpltAlgorithm.LAGRANGE;

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
        double G = 0;

        for (int i = 0; i < inputPoints.size(); ++i)
        {
            float Xi = inputPoints.get(i).x;
            G += gaussBasis[i] * Math.exp(-alpha * Math.pow(X - Xi, 2));
        }

        return (float) G;
    }

    float getGaussianParametricPoint(float X){
        return getGaussianNormalPoint(X);
    }

    private void findGaussianBasis(){
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
            gaussBasis = GaussJordanElimination.SolveSystem(Ab, n);
        }catch (NoSolutionException e){
            Toast.makeText(getApplication(), "Решения для базисов ф-и Гаусса НЕ найдено!", Toast.LENGTH_LONG).show();
            gaussBasis = new double[n];
        }catch (InfiniteSolutionException e){
            Toast.makeText(getApplication(), "Решения для базисов ф-и Гаусса содержит бесконечность!", Toast.LENGTH_LONG).show();
            gaussBasis = new double[n];
        }
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

    private void prepareParams(){
        sortInputPointsByX();
        findGaussianBasis();
    }

}
