package com.childcity.gaussianinterpolation;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;

import android.graphics.Color;
import android.graphics.PointF;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.EntryXComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ChartFragment extends Fragment implements OnChartValueSelectedListener {
    private static final String TAG = "ChartFragment";

    private InterpolationViewModel interpolationViewModel;
    private LineChart chart;
    private float step = 0.01f;
    private Switch isDrawValue;

    static ChartFragment newInstance() {
        return new ChartFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.chart_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        isDrawValue = Objects.requireNonNull(getView()).findViewById(R.id.on_value_visible);
        isDrawValue.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                chart.clear();
                addChartData();
                chart.invalidate();
            }
        });

        FragmentActivity activity = Objects.requireNonNull(getActivity());
        interpolationViewModel = ViewModelProviders.of(activity).get(InterpolationViewModel.class);

        {
            // setup and draw chart
            chart = (LineChart) Objects.requireNonNull(getView()).findViewById(R.id.chart);
            addChartData();
            settingUpChart();
            chart.invalidate(); // refresh
        }

        setupStepSeekBar();
    }

    private void addChartData() {
        final float xLeft = interpolationViewModel.getInputPointAt(0).x;
        final float xRight = interpolationViewModel.getInputPointAt(interpolationViewModel.getInputPointCount() - 1).x;
        final IntrpltAlgorithm intrpltAlg = interpolationViewModel.IntrplAlgorithm;

        List<Entry> inputPointsEntries = new ArrayList<>();
        List<Entry> lagrangeEntries = intrpltAlg.test(IntrpltAlgorithm.LAGRANGE) ? new ArrayList<Entry>() : null;
        List<Entry> gaussianNormalEntries = intrpltAlg.test(IntrpltAlgorithm.GAUSSIAN_NORMAL) ? new ArrayList<Entry>() : null;

        if(lagrangeEntries != null || (gaussianNormalEntries != null))
            for (float i = xLeft; i < (xRight + step); i += step) {
                float lagrangeY = interpolationViewModel.getLagrangePoint(i);
                float gaussianNormalY = interpolationViewModel.getGaussianNormalPoint(i);
                if(lagrangeEntries != null) lagrangeEntries.add(new Entry(i, lagrangeY));
                if(gaussianNormalEntries != null) gaussianNormalEntries.add(new Entry(i, gaussianNormalY));
            }

        // add Entry for parametric methods
        List<Entry> gaussianParametricEntries = intrpltAlg.test(IntrpltAlgorithm.GAUSSIAN_PARAMETRIC) ? new ArrayList<Entry>() : null;
        List<Entry> gaussianSummaryEntries = intrpltAlg.test(IntrpltAlgorithm.GAUSSIAN_SUMMARY) ? new ArrayList<Entry>() : null;

        if(gaussianParametricEntries != null || (gaussianSummaryEntries != null))
            for (float t = 0f; t < (interpolationViewModel.getInputPointCount() + step); t += step) {
                PointF gaussianParametricPoint = interpolationViewModel.getGaussianParametricPoint(t);
                PointF gaussianSummaryPoint = interpolationViewModel.getGaussianSummaryPoint(t);

                if(gaussianParametricEntries != null) gaussianParametricEntries.add(new Entry(gaussianParametricPoint.x, gaussianParametricPoint.y));
                if(gaussianSummaryEntries != null) gaussianSummaryEntries.add(new Entry(gaussianSummaryPoint.x, gaussianSummaryPoint.y));
            }

        for (int i = 0; i < interpolationViewModel.getInputPointCount(); i++) {
            PointF point = interpolationViewModel.getInputPointAt(i);
            inputPointsEntries.add(new Entry(point.x, point.y, getResources().getDrawable(R.drawable.ic_star_black_16dp)));
        }

        if(lagrangeEntries != null) Collections.sort(lagrangeEntries, new EntryXComparator());
        if(gaussianNormalEntries != null)Collections.sort(gaussianNormalEntries, new EntryXComparator());
        if(gaussianParametricEntries != null) Collections.sort(gaussianParametricEntries, new EntryXComparator());
        if(gaussianSummaryEntries != null) Collections.sort(gaussianSummaryEntries, new EntryXComparator());

        List<ILineDataSet> dataSets = new ArrayList<>();

        LineDataSet inputPoints = createLineDataSet(inputPointsEntries, "", Color.TRANSPARENT, false, false);
        dataSets.add(inputPoints);

        if(lagrangeEntries != null){
            LineDataSet lagrange = createLineDataSet(lagrangeEntries, "по Лагранжу", Color.RED, false, isDrawValue.isChecked());
            dataSets.add(lagrange);
        }

        if(gaussianNormalEntries != null) {
            LineDataSet gaussianNormal = createLineDataSet(gaussianNormalEntries, "по Гауссу", Color.GREEN, false, isDrawValue.isChecked());
            dataSets.add(gaussianNormal);
        }

        if(gaussianParametricEntries != null) {
            LineDataSet gaussianParametric = createLineDataSet(gaussianParametricEntries, "по Гауссу (параметрический)", Color.BLUE, false, isDrawValue.isChecked());
            dataSets.add(gaussianParametric);
        }

        if(gaussianSummaryEntries != null){
            LineDataSet gaussianSummary = createLineDataSet(gaussianSummaryEntries, "по Гауссу (сумарный)", Color.MAGENTA, false, isDrawValue.isChecked());
            dataSets.add(gaussianSummary);
        }

        chart.setData(new LineData(dataSets));
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {
        Log.e(TAG, "" + h.getX() + h.getY());
    }

    @Override
    public void onNothingSelected() {}


    private LineDataSet createLineDataSet(List<Entry> entries, String label, int color, boolean drawCircle, boolean drawValues){
        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        //dataSet.setDrawIcons(true);
        //dataSet.enableDashedLine(10f, 5f, 0f); // draw dashed line
        //dataSet.setMode(LineDataSet.Mode.LINEAR); // algorithm of drawing (HORIZONTAL_BEZIER)

        dataSet.setValueTextSize(10f); // text size of values on line
        dataSet.setDrawCircles(drawCircle);
        dataSet.setColor(color);
        dataSet.setDrawValues(drawValues);

        settingUpHighlighter(dataSet);

        return dataSet;
    }

    private void settingUpChart(){

        chart.getDescription().setText("");
        chart.setPinchZoom(true); //zoom 1:1
        chart.setTouchEnabled(true);
        chart.setClickable(true);

        settingUpAxis();

        {
            // highlighting a some point
            //chart.highlightValue(-0.5f, 0, false);
        }

        chart.setOnChartValueSelectedListener(this);

        {
            // set height of chart view
            ViewGroup.LayoutParams mParams = chart.getLayoutParams();
            mParams.height = Objects.requireNonNull(getActivity()).getWindow().getDecorView().getWidth();
        }
    }

    private void settingUpAxis() {


        // Axis
        chart.getAxisRight().setEnabled(false);
        XAxis xAxis = chart.getXAxis();
        xAxis.enableGridDashedLine(10f, 20f, 0f);
        xAxis.setPosition(XAxis.XAxisPosition.TOP);

        YAxis leftY = chart.getAxisLeft();
        leftY.enableGridDashedLine(10f, 20f, 0f);
        leftY.setDrawZeroLine(true);
        leftY.setZeroLineWidth(2);


        float min = Math.min(xAxis.getAxisMinimum(), leftY.getAxisMinimum()) - 2 * step;
        float max = Math.max(xAxis.getAxisMaximum(), leftY.getAxisMaximum()) + 2 * step;

        xAxis.setAxisMinimum(min);
        xAxis.setAxisMaximum(max);
        leftY.setAxisMinimum(min);
        leftY.setAxisMaximum(max);

//        xAxis.setAxisMinimum(xAxis.getAxisMinimum() - 2 * step);
//        xAxis.setAxisMaximum(xAxis.getAxisMaximum() + 2 * step);
//        leftY.setAxisMinimum(leftY.getAxisMinimum() - 2 * step);
//        leftY.setAxisMaximum(leftY.getAxisMaximum() + 2 * step);

        //leftY.setGranularity(0.2f);
    }

    private void settingUpHighlighter(LineDataSet dataSet) {
        // Point highlighting settings
        dataSet.setHighlightEnabled(true); // allow highlighting for DataSet
        // set this to false to disable the drawing of highlight indicator (lines)
        dataSet.setDrawHighlightIndicators(true);
        dataSet.setHighLightColor(Color.BLACK);
    }

//    private void setFilledArea(){ //set filling the chart
//        // set the filled area
//        dataSet.setDrawFilled(true);
//        dataSet.setFillFormatter(new IFillFormatter() {
//            @Override
//            public float getFillLinePosition(ILineDataSet dataSet, LineDataProvider dataProvider) {
//                return chart.getAxisLeft().getAxisMinimum();
//            }
//        });

//          // set color of filled area
//            if (Utils.getSDKInt() >= 18) {
//                // drawables only supported on api level 18 and above
//                Drawable drawable = ContextCompat.getDrawable(this, R.drawable.fade_red);
//                set1.setFillDrawable(drawable);
//            } else {
//                set1.setFillColor(Color.BLACK);
//            }
//    }


//    private void setLimitingLines(){
//        LimitLine llXAxis = new LimitLine(0.2f, "Index 10");
//        llXAxis.setLineWidth(4f);
//        llXAxis.enableDashedLine(10f, 10f, 0f);
//        llXAxis.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
//        llXAxis.setTextSize(10f);
//        //llXAxis.setTypeface(tfRegular);
//
//        LimitLine ll1 = new LimitLine(1.50f, "Upper Limit");
//        ll1.setLineWidth(4f);
//        ll1.enableDashedLine(10f, 10f, 0f);
//        ll1.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
//        ll1.setTextSize(10f);
//        //ll1.setTypeface(tfRegular);
//
//        LimitLine ll2 = new LimitLine(-30f, "Lower Limit");
//        ll2.setLineWidth(4f);
//        ll2.enableDashedLine(10f, 10f, 0f);
//        ll2.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
//        ll2.setTextSize(10f);
//        //ll2.setTypeface(tfRegular);
//
//        // draw limit lines behind data instead of on top
//        leftY.setDrawLimitLinesBehindData(true);
//        xAxis.setDrawLimitLinesBehindData(true);
//
//        // add limit lines
//        leftY.addLimitLine(ll1);
//        leftY.addLimitLine(ll2);
//        xAxis.addLimitLine(llXAxis);
//    }

    private void setupStepSeekBar(){
        SeekBar stepSeek = Objects.requireNonNull(getView()).findViewById(R.id.step_bar);
        final EditText stepText = Objects.requireNonNull(getView()).findViewById(R.id.editText);

        stepSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                stepText.setText(String.valueOf(i/200f));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        stepText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable editable) {
                String text = editable.toString();
                if(text.isEmpty())
                    return;

                float newStep = Float.parseFloat(text);

                if(newStep <= 0.0f)
                    return;

                step = newStep;
                chart.clear();
                addChartData();
                chart.invalidate();
            }
        });
    }
}
