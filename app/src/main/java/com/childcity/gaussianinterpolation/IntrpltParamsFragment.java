package com.childcity.gaussianinterpolation;


import android.annotation.SuppressLint;
import android.graphics.PointF;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Objects;

import static android.widget.RadioGroup.*;

public class IntrpltParamsFragment extends Fragment {
    private static final String TAG = "IntrpltParamsFragment";
    private InterpolationViewModel interpolationViewModel;
    private LayoutInflater ltInflater;
    private LinearLayout pointsLay;

    public IntrpltParamsFragment() {
        // Required empty public constructor
    }

    static IntrpltParamsFragment newInstance() {
        return new IntrpltParamsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_intrplt_params, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        FragmentActivity activity = Objects.requireNonNull(getActivity());
        interpolationViewModel = ViewModelProviders.of(activity).get(InterpolationViewModel.class);

        ltInflater = getLayoutInflater();
        pointsLay =  Objects.requireNonNull(getView()).findViewById(R.id.input_points);

        final Button addPointButton =  Objects.requireNonNull(getView()).findViewById(R.id.add_point);
        addPointButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                addInputPoint(new PointF(0, 0));
            }
        });

        initView();
    }

    @Override
    public void onPause() {
        for (int i = 0; i < pointsLay.getChildCount() - 1 /*-1 because last is '+' button*/; i++) {
            Log.e(TAG, "onPause. i=" + i);
            LinearLayout pointsLayItem = (LinearLayout) pointsLay.getChildAt(i);
            TextView xTV = (TextView) pointsLayItem.getChildAt(0);
            TextView yTV = (TextView) pointsLayItem.getChildAt(1);
            PointF newPoint = new PointF(Float.valueOf("" + xTV.getText()), Float.valueOf("" + yTV.getText()));
            if(i < interpolationViewModel.getInputPointCount()){
                interpolationViewModel.editInputPoint(i, newPoint);
            }else {
                interpolationViewModel.addInputPoint(newPoint);
            }
        }
        super.onPause();
    }

    @SuppressLint("SetTextI18n")
    private void addInputPoint(PointF point){
        LinearLayout pointsLayItem = (LinearLayout) ltInflater.inflate(R.layout.input_points_item, pointsLay, false);
        TextView xTV = (TextView) pointsLayItem.getChildAt(0);
        TextView yTV = (TextView) pointsLayItem.getChildAt(1);

        xTV.setText("" + point.x); yTV.setText("" + point.y);
        pointsLay.addView(pointsLayItem, pointsLay.getChildCount() - 1);

        Button delButton = (Button) pointsLayItem.getChildAt(2);
        delButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Button delButtonToDelete = (Button) v;
                int index = 0;
                for (; index < pointsLay.getChildCount(); index++) {
                    Button delButton = (Button) ((LinearLayout) pointsLay.getChildAt(index)).getChildAt(2);
                    if(delButtonToDelete.equals(delButton)){
                        pointsLay.removeViewAt(index);
                        break;
                    }
                }

                if(pointsLay.getChildCount() > 1 && index < pointsLay.getChildCount()){
                    interpolationViewModel.removeInputPointAt(index);
                }
            }
        });

    }

    private void initView(){
        // set interpolation method

        final Switch lagrangeSwitch=  Objects.requireNonNull(getView()).findViewById(R.id.lagrange_mod);
        final Switch gaussianNormalSwitch=  Objects.requireNonNull(getView()).findViewById(R.id.gaussian_normal_mod);
        final Switch gaussianParametricSwitch=  Objects.requireNonNull(getView()).findViewById(R.id.gaussian_parametric_mod);

        lagrangeSwitch.setChecked(true);
        gaussianNormalSwitch.setChecked(true);
        gaussianParametricSwitch.setChecked(true);

        // Adding points to Front
        for (int i = 0; i < interpolationViewModel.getInputPointCount(); i++) {
            addInputPoint(interpolationViewModel.getInputPointAt(i));
        }
    }
}
