package com.childcity.gaussianinterpolation;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private NavigationView navigationView;
    private InterpolationViewModel interpolationViewModel;
    private static Map<String, Fragment> fragments;
    private static int lastFrag = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        FloatingActionButton fab = findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        interpolationViewModel = ViewModelProviders.of(this).get(InterpolationViewModel.class);

        if(fragments == null){
            fragments = new HashMap<>();
            fragments.put(ChartFragment.class.getName(), ChartFragment.newInstance());
            fragments.put(IntrpltParamsFragment.class.getName(), IntrpltParamsFragment.newInstance());
        }

        try {
            //Reading Settings
            SharedPreferences activityPreferences = getPreferences(MODE_PRIVATE);
            interpolationViewModel.Alpha = activityPreferences.getString("Alpha", interpolationViewModel.Alpha);
            interpolationViewModel.setInputPoints(activityPreferences.getString("InputPoints", ""));
            interpolationViewModel.IntrplAlgorithm = new IntrpltAlgorithm(activityPreferences.getInt("IntrpltAlgorithm", IntrpltAlgorithm.LAGRANGE));
        }catch (Exception e){
            Log.e("LoadParams", Arrays.toString(e.getStackTrace()));
        }
    }

    @Override
    protected void onStart() {

        if (lastFrag == 0) {
            if(getSupportFragmentManager().findFragmentByTag(ChartFragment.class.getName()) == null){
                navigationView.getMenu().findItem(R.id.nav_chart).setChecked(true);
                loadFragment(ChartFragment.class.getName());
            }
        } else if (lastFrag == 1) {
            if(getSupportFragmentManager().findFragmentByTag(IntrpltParamsFragment.class.getName()) == null) {
                navigationView.getMenu().findItem(R.id.nav_input).setChecked(true);
                loadFragment(IntrpltParamsFragment.class.getName());
            }
        }

        super.onStart();
    }

    private void loadFragment(String className) {
        //Log.e("main", fragment.getClass().getName());
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container_main, fragments.get(className), className)
                .commit();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_info) {
            new InterpolationInfoDialog(interpolationViewModel.getXmin(), interpolationViewModel.getXmax(),
                    interpolationViewModel.getTMin(), interpolationViewModel.getTMaxParametric(), interpolationViewModel.getTMaxSummary(),
                    interpolationViewModel.getNormalAlpha(), interpolationViewModel.getParametricAlpha(), interpolationViewModel.getSummaryAlpha())
                    .show(getSupportFragmentManager(), InterpolationInfoDialog.class.getName());
            return true;
        }else if(id == R.id.action_save || id == R.id.action_load){
            if(! FileController.CheckPermissions(MainActivity.this)){
                Toast.makeText(MainActivity.this, "Разрешите право на чтение/запись для работы этой опции!", Toast.LENGTH_LONG).show();
            }else if(id == R.id.action_save){
                FileController.SaveAsCSV(MainActivity.this);
            }else //noinspection ConstantConditions
                if(id == R.id.action_load){
                    FileController.LoadAsCSV(MainActivity.this);
                }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_chart) {
            lastFrag = 0;
            loadFragment(ChartFragment.class.getName());
        }else if (id == R.id.nav_input) {
            lastFrag = 1;
            loadFragment(IntrpltParamsFragment.class.getName());
        }else if (id == R.id.nav_share) {
//            final Intent share = new Intent(Intent.ACTION_SEND);
//            share.setType("image/png");
//            //Log.e("asas", "" + chartImageUri.getPath());
//            File imagePath = new File(getExternalFilesDir(Environment.DIRECTORY_DCIM), "Gorodetskiy");
//            File newFile = new File(imagePath, "interpolation_chart_2536.png");
//
//            //if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
//                Uri chartImageUri = FileProvider.getUriForFile(MainActivity.this,
//                        "com.childcity.gaussianinterpolation.fileprovider",
//                        newFile);
//            //}
//
//            Log.e("asas", "" + chartImageUri.toString());
//
//            share.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//            //share.setDataAndType(chartImageUri,"image/png");
//            share.putExtra(Intent.EXTRA_STREAM, chartImageUri);
//            startActivity(Intent.createChooser(share, "Share image using"));
            Uri chartImageUri = ((ChartFragment)fragments.get(ChartFragment.class.getName())).saveAsImage();
            if(chartImageUri == null){
                Toast.makeText(this, "Невозможно сохранить изображение.", Toast.LENGTH_LONG).show();
            } else {
                Log.e("asas", "" + chartImageUri.getPath());

            File imagePath = new File(getExternalFilesDir(Environment.DIRECTORY_DCIM), "Gorodetskiy");
            File newFile = new File(imagePath, "interpolation_chart_2536.png");
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                    chartImageUri = FileProvider.getUriForFile(MainActivity.this,
                            "com.childcity.gaussianinterpolation.fileprovider",
                            newFile);
                }

                Log.e("asas", "" + chartImageUri.getPath());
                Log.e("asas", "" + chartImageUri.toString());
                Log.e("asas", "" + chartImageUri.getLastPathSegment());

                final Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("image/png");
                share.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                share.putExtra(Intent.EXTRA_STREAM, chartImageUri);
                startActivity(Intent.createChooser(share, "Share image using"));
            }
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onStop() {
        SharedPreferences activityPreferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = activityPreferences.edit();

        editor.putInt("IntrpltAlgorithm", interpolationViewModel.IntrplAlgorithm.toInt());
        editor.putString("InputPoints", interpolationViewModel.getInputPoints());
        editor.putString("Alpha", interpolationViewModel.Alpha);

        editor.apply();
        super.onStop();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        FileController.processResult(MainActivity.this, interpolationViewModel, requestCode, resultCode, data);
    }
}
