package com.childcity.gaussianinterpolation.SystemSolver;

import android.util.Log;

import java.text.DecimalFormat;

public class GaussJordanElimination {
    private static final String TAG = "GaussJordanElimination";
    private static final DecimalFormat logFormatter = new DecimalFormat("#.0000");

    // a - matrix Ab
    // n - order of Matrix(n)
    /*
        Example b:
        double a[][] = {{ 0, 2, 1, 4 },
                    { 1, 1, 2, 6 },
                    { 2, 1, 1, 7 }};
        b = 4, 6, 7
     */
    public static double[] SolveSystem(double[][] a, int n){
        int flag = 0;

        // Performing Matrix transformation
        flag = PerformOperation(a, n);

        if (flag == 1)
            flag = CheckConsistency(a, n, flag);

        // Printing Final Matrix
        Log.d(TAG, "Final Augmented Matrix is: ");
        PrintMatrix(a, n);

        // Return Solutions(if exist)
        return GetSolution(a, n, flag);
    }

    // function to reduce matrix to reduced
    // row echelon form.
    private static int PerformOperation(double[][] a, int n)
    {
        int i, j, k = 0, c, flag = 0, m = 0;
        double pro = 0;

        // Performing elementary operations
        for (i = 0; i < n; i++)
        {
            if (a[i][i] == 0)
            {
                c = 1;
                while (a[i + c][i] == 0 && (i + c) < n)
                    c++;
                if ((i + c) == n)
                {
                    flag = 1;
                    break;
                }
                for (j = i, k = 0; k <= n; k++)
                {
                    double temp =a[j][k];
                    a[j][k] = a[j+c][k];
                    a[j+c][k] = temp;
                }
            }

            for (j = 0; j < n; j++)
            {

                // Excluding all i == j
                if (i != j)
                {

                    // Converting Matrix to reduced row
                    // echelon form(diagonal matrix)
                    double p = a[j][i] / a[i][i];

                    for (k = 0; k <= n; k++)
                        a[j][k] = a[j][k] - (a[i][k]) * p;
                }
            }
        }
        return flag;
    }

    // Function to get the desired result
    // if unique solutions exists, otherwise
    // prints no solution or infinite solutions
    // depending upon the input given.
    private static double[] GetSolution(double[][] a, int n, int flag) throws InfiniteSolutionException, NoSolutionException {
        if (flag == 2) {
            throw new InfiniteSolutionException();
        } else if (flag == 3) {
            throw new NoSolutionException();
        }

        double[] solution = new double[n];
        StringBuilder line = new StringBuilder("Result is: ");

        // Getting the solution by dividing constants by
        // their respective diagonal elements
        for (int i = 0; i < n; i++){
            solution[i] = a[i][n] / a[i][i];
            line.append(logFormatter.format(solution[i])).append(" ");
        }

        Log.println(Log.INFO, TAG, line.toString());

        return solution;
    }

    // To check whether infinite solutions
    // exists or no solution exists
    private static int CheckConsistency(double[][] a, int n, int flag)
    {
        int i, j;
        double sum;

        // flag == 2 for infinite solution
        // flag == 3 for No solution
        flag = 3;
        for (i = 0; i < n; i++)
        {
            sum = 0;
            for (j = 0; j < n; j++)
                sum = sum + a[i][j];
            if (sum == a[i][j])
                flag = 2;
        }
        return flag;
    }

    // Function to print the matrix
    private static void PrintMatrix(double[][] a, int n)
    {
        for (int i = 0; i < n; i++)
        {
            StringBuilder line = new StringBuilder();
            for (int j = 0; j <= n; j++)
                line.append(logFormatter.format(a[i][j])).append(" ");
            Log.println(Log.INFO, TAG, line.toString());
        }
    }
}