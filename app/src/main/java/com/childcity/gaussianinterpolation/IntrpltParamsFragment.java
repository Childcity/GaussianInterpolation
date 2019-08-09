package com.childcity.gaussianinterpolation;


import android.annotation.SuppressLint;
import android.graphics.PointF;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;

import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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

    private CheckBox lagrangeSwitch;
    private CheckBox gaussianNormalSwitch;
    private CheckBox gaussianParametricSwitch;
    private CheckBox gaussianSummarySwitch;

    // for changeable points
    private float YPositionBeforeMoving;
    private float yDelta;
    private int actionMoveCount;
    private int lastAction;

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
                addInputPoint(new PointF(0f, 0f));
            }
        });

        initView();
    }

    @Override
    public void onPause() {
        for (int i = 0; i < pointsLay.getChildCount() - 1 /*-1 because last is '+' button*/; i++) {
            //Log.e(TAG, "onPause. i=" + i);
            final LinearLayout pointsLayItem = (LinearLayout) pointsLay.getChildAt(i);
            final TextView xTV = (TextView) pointsLayItem.getChildAt(0);
            final TextView yTV = (TextView) pointsLayItem.getChildAt(1);

            PointF newPoint = new PointF(Float.valueOf("" + xTV.getText()), Float.valueOf("" + yTV.getText()));
            if(i < interpolationViewModel.getInputPointCount()){
                interpolationViewModel.editInputPoint(i, newPoint);
            }else {
                interpolationViewModel.addInputPoint(newPoint);
            }
            //Log.e(TAG, newPoint.toString());
        }

        IntrpltAlgorithm intrpltAlg = interpolationViewModel.IntrplAlgorithm;

        intrpltAlg.changeFlag(IntrpltAlgorithm.LAGRANGE, lagrangeSwitch.isChecked());
        intrpltAlg.changeFlag(IntrpltAlgorithm.GAUSSIAN_NORMAL, gaussianNormalSwitch.isChecked());
        intrpltAlg.changeFlag(IntrpltAlgorithm.GAUSSIAN_PARAMETRIC, gaussianParametricSwitch.isChecked());
        intrpltAlg.changeFlag(IntrpltAlgorithm.GAUSSIAN_SUMMARY, gaussianSummarySwitch.isChecked());

        super.onPause();
    }

    @SuppressLint({"SetTextI18n", "ClickableViewAccessibility"})
    private void addInputPoint(PointF point){
        final LinearLayout pointsLayItem = (LinearLayout) ltInflater.inflate(R.layout.input_points_item, pointsLay, false);
        final TextView xTV = (TextView) pointsLayItem.getChildAt(0);
        final TextView yTV = (TextView) pointsLayItem.getChildAt(1);

        xTV.setText("" + point.x); yTV.setText("" + point.y);
        pointsLay.addView(pointsLayItem, pointsLay.getChildCount() - 1);

        Button delButton = (Button) pointsLayItem.getChildAt(2);
        final Function<View, Void> delButtonOnClick = new Function<View, Void>() {
            @Override
            public Void apply(View v) {
                Button delButtonToDelete = (Button) v;
                int index = -1;
                while (++index < pointsLay.getChildCount()) {
                    Button delButton = (Button) ((LinearLayout) pointsLay.getChildAt(index)).getChildAt(2);
                    if(delButtonToDelete.equals(delButton)){
                        pointsLay.removeViewAt(index);
                        break;
                    }
                }

                if(pointsLay.getChildCount() > 1 && index < pointsLay.getChildCount()){
                    interpolationViewModel.removeInputPointAt(index);
                }
                return null;
            }
        };

        delButton.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                final float Y = event.getRawY();
                final float yPosition = Y + yDelta;
                float moveDelta = Math.abs(yPosition - YPositionBeforeMoving);

                final float pointsLayItemH = pointsLayItem.getHeight();
                final float piecePointsLayItemH = pointsLayItem.getHeight() / 3f;

                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        lastAction = MotionEvent.ACTION_DOWN;
                        yDelta = pointsLayItem.getY() - Y;
                        break;
                    case MotionEvent.ACTION_UP:
                        if(actionMoveCount < 5){ //selete Point, if user quick touch
                            delButtonOnClick.apply(view);
                        }else if(moveDelta < pointsLayItemH - piecePointsLayItemH){
                            pointsLayItem.setY(YPositionBeforeMoving);
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if(lastAction != MotionEvent.ACTION_MOVE){
                            // reset YPositionBeforeMoving
                            YPositionBeforeMoving = yPosition;
                            moveDelta = 0f;
                            actionMoveCount = 0;
                        }

                        final boolean directionUp = yPosition - YPositionBeforeMoving > 0;

                        if(moveDelta < (pointsLayItemH + piecePointsLayItemH))
                            pointsLayItem.setY(yPosition);

                        if(moveDelta > (pointsLayItemH - piecePointsLayItemH) && moveDelta < (pointsLayItemH + piecePointsLayItemH)){
                            // find index of current movable item
                            for (int index = 0; index < pointsLay.getChildCount() - 1; index++) { // '- 1' because the last is '+' button
                                LinearLayout currLay = (LinearLayout)pointsLay.getChildAt(index);
                                if(currLay.equals(pointsLayItem)){ // current index is an index of 'current movable item' in pointsLay
                                    LinearLayout prevLay = index != 0 ? (LinearLayout) pointsLay.getChildAt(index - 1) : null;
                                    LinearLayout nextLay = index != (pointsLay.getChildCount()-2) ? (LinearLayout) pointsLay.getChildAt(index + 1) : null;

                                    if(nextLay != null || prevLay != null){
                                        pointsLayItem.setY(YPositionBeforeMoving);
                                        pointsLay.removeView(pointsLayItem);

                                        if(directionUp && nextLay != null){
                                            pointsLay.addView(pointsLayItem, index + 1);
                                            interpolationViewModel.swapInputPointAt(index, index + 1);
                                        }else if(prevLay != null){
                                            pointsLay.addView(pointsLayItem, index - 1);
                                            interpolationViewModel.swapInputPointAt(index, index - 1);
                                        }

                                        pointsLay.animate();
                                    }

                                    pointsLay.invalidate();
                                    break;
                                }
                            }
                        }

                        lastAction = MotionEvent.ACTION_MOVE;
                        actionMoveCount++;
                        break;
                } //END switch

                pointsLay.invalidate();
                return true;
            }
        });
    }

    private void initView(){
        // set interpolation method

        lagrangeSwitch =  Objects.requireNonNull(getView()).findViewById(R.id.lagrange_mod);
        gaussianNormalSwitch =  Objects.requireNonNull(getView()).findViewById(R.id.gaussian_normal_mod);
        gaussianParametricSwitch =  Objects.requireNonNull(getView()).findViewById(R.id.gaussian_parametric_mod);
        gaussianSummarySwitch =  Objects.requireNonNull(getView()).findViewById(R.id.gaussian_summary_mod);

        IntrpltAlgorithm intrpltAlg = interpolationViewModel.IntrplAlgorithm;
        lagrangeSwitch.setChecked(intrpltAlg.test(IntrpltAlgorithm.LAGRANGE));
        gaussianNormalSwitch.setChecked(intrpltAlg.test(IntrpltAlgorithm.GAUSSIAN_NORMAL));
        gaussianParametricSwitch.setChecked(intrpltAlg.test(IntrpltAlgorithm.GAUSSIAN_PARAMETRIC));
        gaussianSummarySwitch.setChecked(intrpltAlg.test(IntrpltAlgorithm.GAUSSIAN_SUMMARY));

        // Adding points to Front
        for (int i = 0; i < interpolationViewModel.getInputPointCount(); i++) {
            addInputPoint(interpolationViewModel.getInputPointAt(i));
        }

        TextView textView =  Objects.requireNonNull(getView()).findViewById(R.id.textView5);
        textView.setText(Html.fromHtml("Справка: <br>" +
                "<b>size</b> - количество точек<br>" +
                "<b>Xmax</b> - максимальное значение икса<br>" +
                "<b>Xmin</b> - минимальное значение икса"));


    }
    }
