package com.childcity.gaussianinterpolation;

import android.content.SharedPreferences;
import android.os.Bundle;
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

import java.util.HashSet;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    InterpolationViewModel interpolationViewModel = null;

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

        {
            //Reading Settings
            SharedPreferences activityPreferences = getPreferences(MODE_PRIVATE);
            interpolationViewModel.setInputPoints(activityPreferences.getStringSet("InputPoints", new HashSet<String>()));
            interpolationViewModel.IntrplAlgorithm = IntrpltAlgorithm.valueOf(activityPreferences.getString("IntrpltAlgorithm", "LAGRANGE"));
        }
    }

    @Override
    protected void onStart() {

        // Display the fragment as the main content.
//        getSupportFragmentManager().beginTransaction()
//                .replace(R.id.container_main, ChartFragment.newInstance(), "chart_tag")
//                .commit();

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
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container_main, ChartFragment.newInstance(), "chart_tag")
                    .commit();
        }else if (id == R.id.nav_gallery) {
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

        editor.putString("IntrpltAlgorithm", interpolationViewModel.IntrplAlgorithm.name());
        editor.putStringSet("InputPoints", interpolationViewModel.getInputPoints());

        editor.apply();
        super.onStop();
    }
}