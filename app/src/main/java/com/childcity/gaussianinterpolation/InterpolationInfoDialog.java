package com.childcity.gaussianinterpolation;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.util.Locale;

public class InterpolationInfoDialog extends DialogFragment {
    private float _xMin, _xMax, _tMin, _tMaxParamGauss, _tMaxSummaryGauss;
    private double _normalAlpha; //default: pi(n-1) / (Xmax - Xmin)^2
    private double _parametricAlpha;
    private double _summaryAlpha;

    InterpolationInfoDialog(float xMin, float xMax, float tMin, float tMaxParamGauss, float tMaxSummaryGauss, double normalAlpha, double parametricAlpha, double summaryAlpha){
        _xMin = xMin;
        _xMax = xMax;
        _tMin = tMin;
        _tMaxParamGauss = tMaxParamGauss;
        _tMaxSummaryGauss = tMaxSummaryGauss;
        _normalAlpha = normalAlpha;
        _parametricAlpha = parametricAlpha;
        _summaryAlpha = summaryAlpha;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String text = String.format(Locale.US,
                "X_min = %.4f\n" +
                        "X_max = %.4f\n" +
                        "--------------------\n" +
                        "T_min = %.4f\n" +
                        "T_max = %.4f (параметрический)\n" +
                        "T_max = %.4f (суммарный)\n" +
                        "--------------------\n" +
                        "alpha = %.4f\n" +
                        "alpha = %.4f (параметрический)\n" +
                        "alpha = %.4f (суммарный)\n",
                _xMin, _xMax, _tMin, _tMaxParamGauss, _tMaxSummaryGauss, _normalAlpha, _parametricAlpha, _summaryAlpha);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(text)
                .setPositiveButton(android.R.string.ok, null
                );
        // Create the AlertDialog object and return it
        return builder.create();
    }
}