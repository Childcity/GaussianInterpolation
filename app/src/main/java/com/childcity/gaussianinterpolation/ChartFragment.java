package com.childcity.gaussianinterpolation;

import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.EntryXComparator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

public class ChartFragment extends Fragment implements OnChartValueSelectedListener {
    private static final String TAG = "ChartFragment";
    static final float DEFAULT_DRAW_STEP = 0.1f;
    private static final int seekBarStep = 200;
    private final ChartFragment self = this;

    private InterpolationViewModel interpolationViewModel;
    private GraphicDrawerTask graphicDrawerTask;
    private LineChart chart;
    private float step = DEFAULT_DRAW_STEP;
    private CheckBox isDrawValue;

    static ChartFragment newInstance() {
        return new ChartFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chart, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        chart = (LineChart) Objects.requireNonNull(getView()).findViewById(R.id.chart);
        isDrawValue = Objects.requireNonNull(getView()).findViewById(R.id.on_value_visible);
        isDrawValue.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                graphicDrawerTask = new GraphicDrawerTask(self, false);
                graphicDrawerTask.execute();
            }
        });

        FragmentActivity activity = Objects.requireNonNull(getActivity());
        interpolationViewModel = ViewModelProviders.of(activity).get(InterpolationViewModel.class);

        setupStepSeekBar();

        final LiveData<Boolean> inputDataChanged =  interpolationViewModel.getInputDataChangedFlag();
        inputDataChanged.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isChanged) {
                // setup and draw chart
                graphicDrawerTask = new GraphicDrawerTask(self, true);
                graphicDrawerTask.execute();
            }
        });
    }

    Uri saveAsImage(){
        String title = String.format(Locale.US,"interpolation_chart_%.4s.png",  Math.abs(new Random(new Date().getTime()).nextInt()));
        String subFolderPath = "Gorodetskiy";

        if(chart == null || (! chart.saveToGallery(title, subFolderPath, "Интерполяционный график",
                Bitmap.CompressFormat.PNG, 100)))
        {
            return null;
        }
        //

        return new Uri.Builder()
                .appendPath(Environment.getExternalStorageDirectory().getPath())
                .appendPath("DCIM")
                .appendPath(subFolderPath)
                .appendPath(title)
                .build();
    }

    private LineData getChartData() {
        final float xLeft = interpolationViewModel.getXmin();
        final float xRight = interpolationViewModel.getXmax();
        final IntrpltAlgorithm intrpltAlg = interpolationViewModel.IntrplAlgorithm;

        List<Entry> inputPointsEntries = new ArrayList<>();
        List<Entry> lagrangeEntries = intrpltAlg.test(IntrpltAlgorithm.LAGRANGE) ? new ArrayList<Entry>() : null;
        List<Entry> gaussianNormalEntries = intrpltAlg.test(IntrpltAlgorithm.GAUSSIAN_NORMAL) ? new ArrayList<Entry>() : null;

        if(lagrangeEntries != null || (gaussianNormalEntries != null))
            for (float i = xLeft; i <= xRight; i += step) {
                if(lagrangeEntries != null) {
                    float lagrangeY = interpolationViewModel.getLagrangePoint(i);
                    lagrangeEntries.add(new Entry(i, lagrangeY));
                }
                if(gaussianNormalEntries != null) {
                    float gaussianNormalY = interpolationViewModel.getGaussianNormalPoint(i);
                    gaussianNormalEntries.add(new Entry(i, gaussianNormalY));
                }
            }

        // add Entry for parametric methods
        List<Entry> gaussianParametricEntries = intrpltAlg.test(IntrpltAlgorithm.GAUSSIAN_PARAMETRIC) ? new ArrayList<Entry>() : null;
        List<Entry> gaussianSummaryEntries = intrpltAlg.test(IntrpltAlgorithm.GAUSSIAN_SUMMARY) ? new ArrayList<Entry>() : null;

        if(gaussianParametricEntries != null) {
            // t[ 0.0 ... (n-1)]
            for (float t = 0f; t <= (interpolationViewModel.getInputPointCount() - 1f); t += step) {
                PointF gaussianParametricPoint = interpolationViewModel.getGaussianParametricPoint(t);
                gaussianParametricEntries.add(new Entry(gaussianParametricPoint.x, gaussianParametricPoint.y));
                //Log.e("ParametricPoint", gaussianParametricPoint.toString());
            }
        }

        if(gaussianSummaryEntries != null) {
            int lastPointsIndex = interpolationViewModel.getInputPointCount() - 1;
            float maxT = interpolationViewModel.getGussSummaryXInputPoint(lastPointsIndex).x;
            //Log.e("maxT", maxT+"");
            // t[ 0.0 ... (maxT)]
            for (float t = 0f; t <= maxT; t += step) {
                PointF gaussianSummaryPoint = interpolationViewModel.getGaussianSummaryPoint(t);
                gaussianSummaryEntries.add(new Entry(gaussianSummaryPoint.x, gaussianSummaryPoint.y));
                //Log.e("SummaryPoint", gaussianSummaryPoint.toString());
            }

//            for (int t = 0; t <= (interpolationViewModel.getInputPointCount() - 1); t += 1) {
//                PointF gaussianSummaryPoint = interpolationViewModel.getGaussianSummaryPoint(interpolationViewModel.getGussSummaryXInputPoint(t).x);
//                gaussianSummaryEntries.add(new Entry(gaussianSummaryPoint.x, gaussianSummaryPoint.y));
//                Log.e("SummaryPoint", gaussianSummaryPoint.toString());
//            }
        }


        for (int i = 0; i < interpolationViewModel.getInputPointCount(); i++) {
            PointF point = interpolationViewModel.getInputPointAt(i);
            inputPointsEntries.add(new Entry(point.x, point.y, getResources().getDrawable(R.drawable.ic_star_black_16dp)));

//            Entry inputEntry = new Entry(point.x, point.y);
//            if(lagrangeEntries != null) lagrangeEntries.add(inputEntry);
//            if(gaussianNormalEntries != null) gaussianNormalEntries.add(inputEntry);
//            if(gaussianParametricEntries != null) gaussianParametricEntries.add(inputEntry);
//            if(gaussianSummaryEntries != null) gaussianSummaryEntries.add(inputEntry);
        }

        List<ILineDataSet> dataSets = new ArrayList<>();

        LineDataSet inputPoints = createLineDataSet(inputPointsEntries, "", Color.TRANSPARENT, false, isDrawValue.isChecked());
        dataSets.add(inputPoints);

        if(lagrangeEntries != null){
            LineDataSet lagrange = createLineDataSet(lagrangeEntries, "по Лагранжу", Color.DKGRAY, true, isDrawValue.isChecked());
            dataSets.add(lagrange);
        }

        if(gaussianNormalEntries != null) {
            LineDataSet gaussianNormal = createLineDataSet(gaussianNormalEntries, "по Гауссу", Color.GREEN, true, isDrawValue.isChecked());
            dataSets.add(gaussianNormal);
        }

        if(interpolationViewModel.getInputPointCount() > 1){
            if(gaussianParametricEntries != null) {
                LineDataSet gaussianParametric = createLineDataSet(gaussianParametricEntries, "по Гауссу (параметрический)", Color.BLUE, true, isDrawValue.isChecked());
                dataSets.add(gaussianParametric);
            }

            if(gaussianSummaryEntries != null){
                LineDataSet gaussianSummary = createLineDataSet(gaussianSummaryEntries, "по Гауссу (сумарный)", Color.MAGENTA, true, isDrawValue.isChecked());
                dataSets.add(gaussianSummary);
            }
        }

        return new LineData(dataSets);
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {
        Log.e(TAG, "onValueSelected: " + h.getX() + h.getY());
    }

    @Override
    public void onNothingSelected() {}


    private LineDataSet createLineDataSet(List<Entry> entries, String label, int color, boolean drawCircle, boolean drawValues){
        Collections.sort(entries, new EntryXComparator());

//        for (int i = 0; i < entries.size(); i++) {
//            if (entries.get(i).getX() < interpolationViewModel.getInputPointAt(0).x){
//                entries.remove(i--);
//            }
//        }

        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        //dataSet.setDrawIcons(true);
        //dataSet.enableDashedLine(10f, 5f, 0f); // draw dashed line
        //dataSet.setMode(LineDataSet.Mode.LINEAR); // algorithm of drawing (HORIZONTAL_BEZIER)

        dataSet.setValueTextSize(10f); // text size of values on line
        dataSet.setColor(color);
        dataSet.setLineWidth(0.001f);
        dataSet.setDrawValues(drawValues);

        // setup circle
        {
            dataSet.setDrawCircles(drawCircle);
            dataSet.setDrawCircleHole(false);
            dataSet.setCircleRadius(2);
            dataSet.setCircleColor(color == Color.TRANSPARENT ? Color.BLUE : color);
        }

        settingUpHighlighter(dataSet);

        return dataSet;
    }

    private void settingUpChart(){

        chart.getDescription().setText("");
        chart.setPinchZoom(true); //zoom 1:1
        chart.setTouchEnabled(true);
        chart.setClickable(true);

        // setting up legend
        {
            Legend legend = chart.getLegend();
            legend.setForm(Legend.LegendForm.CIRCLE);
            legend.setWordWrapEnabled(true);
        }

        settingUpAxis();

        // highlighting a some point
        {
            //chart.highlightValue(-0.5f, 0, false);
        }

        chart.setOnChartValueSelectedListener(this);

        // set height of chart view
//        {
//            ViewGroup.LayoutParams mParams = chart.getLayoutParams();
//            mParams.height = Objects.requireNonNull(getActivity()).getWindow().getDecorView().getWidth();
//        }
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

        leftY.setGranularity(0.01f);
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
        final SeekBar stepSeek = Objects.requireNonNull(getView()).findViewById(R.id.step_bar);
        final EditText stepText = Objects.requireNonNull(getView()).findViewById(R.id.step_value);

        stepText.setText(String.valueOf(step));
        stepSeek.setProgress((int) (step * seekBarStep));

        stepSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                float progress = i / (float) seekBarStep;
                if(step != progress) {
                    stepText.setText(String.valueOf(progress));
                }
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

                if(newStep > 0f)
                    step = newStep;
                else step = DEFAULT_DRAW_STEP;

                if(step != stepSeek.getProgress() / seekBarStep){
                    stepSeek.setProgress((int) (step * seekBarStep));
                }

                graphicDrawerTask = new GraphicDrawerTask(self, false);
                graphicDrawerTask.execute();
            }
        });

    }
    //static volatile int changeLock = 0;

    static class GraphicDrawerTask extends AsyncTask<Void, Void, LineData> {
        private boolean isSettingUpChart;
        private ChartFragment fragment;

        GraphicDrawerTask(ChartFragment fragment, boolean isSettingUpChart){
            this.isSettingUpChart = isSettingUpChart;
            this.fragment = fragment;
        }

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            ProgressBar progressBar = Objects.requireNonNull(fragment.getActivity()).findViewById(R.id.toolbar_progress_bar);
            progressBar.setVisibility(View.VISIBLE);
            fragment.interpolationViewModel.prepareParams();
        }

        @Override
        protected LineData doInBackground(Void... voids) {
            LineData lineData = new LineData();
            try {
                lineData = fragment.getChartData();
            } catch (Exception e) {
                Log.d(TAG, Log.getStackTraceString(e));
            }
            return lineData;
        }

        @Override
        protected void onPostExecute(LineData dataSets){
            super.onPostExecute(dataSets);

            fragment.chart.clear();
            fragment.chart.setData(dataSets);
            if(isSettingUpChart)
                fragment.settingUpChart();
            fragment.chart.invalidate();
            fragment.chart.animateX(800);

            if(fragment.getActivity() != null){
                ProgressBar progressBar = fragment.getActivity().findViewById(R.id.toolbar_progress_bar);
                progressBar.setVisibility(View.INVISIBLE);
            }

            fragment = null;
        }
    }
}
