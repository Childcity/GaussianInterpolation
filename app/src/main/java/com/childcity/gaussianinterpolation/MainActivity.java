package com.childcity.gaussianinterpolation;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.material.navigation.NavigationView;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    InterpolationViewModel interpolationViewModel = null;
    private int lastFrag = 1;

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
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        interpolationViewModel = ViewModelProviders.of(this).get(InterpolationViewModel.class);

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

        // Display the fragment as the main content.
        NavigationView navigationView = findViewById(R.id.nav_view);
        if (lastFrag == 0) {
            if(getSupportFragmentManager().findFragmentByTag("chart_tag") == null) {
                navigationView.getMenu().findItem(R.id.nav_chart).setChecked(true);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container_main, ChartFragment.newInstance(), "chart_tag")
                        .commit();
            }
        } else if (lastFrag == 1) {
            if(getSupportFragmentManager().findFragmentByTag("intrplt_params_tag") == null){
                navigationView.getMenu().findItem(R.id.nav_input).setChecked(true);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container_main, IntrpltParamsFragment.newInstance(), "intrplt_params_tag")
                        .commit();
            }

        }


        super.onStart();
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
                    .show(getSupportFragmentManager(), "chart_info_tag");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_chart) {
            lastFrag = 0;
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container_main, ChartFragment.newInstance(), "chart_tag")
                    .commit();
        }else if (id == R.id.nav_input) {
            lastFrag = 1;
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container_main, IntrpltParamsFragment.newInstance(), "intrplt_params_tag")
                    .commit();
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
}
