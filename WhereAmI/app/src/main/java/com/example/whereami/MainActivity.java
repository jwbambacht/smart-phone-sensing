package com.example.whereami;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private WifiManager wifiManager;
    private AccelerometerListener accelerometer;
    private final int MY_PERMISSIONS_ACCESS_COARSE_LOCATION = 1;
    WifiReceiver receiverWifi;
    SharedPreferences settingsSharedPreferences;
    SQLiteDatabase db;
    GridLayout leftGridLayout, rightGridLayout;
    TextView senseToSee, cellLabel, activityLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_whereami_icon_white);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        db = openOrCreateDatabase("database.db", MODE_PRIVATE, null);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(getApplicationContext(), "Turning WiFi ON...", Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }

        SensorManager mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = new AccelerometerListener(mSensorManager);

        settingsSharedPreferences = getApplicationContext().getSharedPreferences("SETTINGS", 0);

        Button training = (Button) findViewById(R.id.button_training);
        training.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(view.getContext(),TrainingActivity.class));
            }
        });

        String[] cells = this.getResources().getStringArray(R.array.cell_array);
        String[] activities = this.getResources().getStringArray(R.array.activity_array);

        senseToSee = (TextView) findViewById(R.id.textview_label_sense);
        cellLabel = (TextView) findViewById(R.id.textview_label_cell);
        activityLabel = (TextView) findViewById(R.id.textview_label_activity);
        leftGridLayout = (GridLayout) findViewById(R.id.gridlayout_left_cells);
        rightGridLayout = (GridLayout) findViewById(R.id.gridlayout_right_cells);

        cellLabel.setVisibility(View.INVISIBLE);
        activityLabel.setVisibility(View.INVISIBLE);

        Button sense = (Button) findViewById(R.id.button_sense);
        sense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                List<Sample> allSamples = Util.loadSamples(db);

                // Require at least 9 training samples before we can sense. This means that we have a minimum value for k of 3
                if(allSamples.size() < 9) {
                    Toast.makeText(MainActivity.this,R.string.error_not_enough_samples, Toast.LENGTH_SHORT).show();
                    return;
                }

                // Use HashMap to find and update network easily and fast
                HashMap<String, Integer> networks = new HashMap<String, Integer>();

                String cellResult = null;
                String activityResult = null;

                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_ACCESS_COARSE_LOCATION);
                } else {

                    Toast.makeText(MainActivity.this,"Sensing...", Toast.LENGTH_SHORT).show();

                    // Find and return networks
                    networks = Util.findNetworks(wifiManager);
                    // Get the feature from accelerometer
                    float[] activityFeature = accelerometer.getFeature(AccelerometerListener.MIN_MAX);

                    // Apply the KNN algorithm in order to obtain the cell prediction
                    cellResult = Util.KNN(networks,allSamples,settingsSharedPreferences, cells);
                    activityResult = Util.activity(activityFeature, allSamples,settingsSharedPreferences, activities);

                    senseToSee.setText(R.string.textview_sense_results);
                    cellLabel.setVisibility(View.VISIBLE);
                    activityLabel.setVisibility(View.VISIBLE);

                    createCellGrid(leftGridLayout, cells, Util.getPrecision(settingsSharedPreferences), cellResult);
                    createCellGrid(rightGridLayout, activities, activities.length,activityResult);
                }
            }
        });
    }

    public void createCellGrid(GridLayout gridLayout, String[] cellNames, int max, String result) {
        gridLayout.removeAllViews();

        for(int i = 0; i < max; i++) {
            TextView textView = new TextView(this);
            textView.setTextColor(0xFFF8F9FA);
            if(cellNames[i].equals(result)) {
                textView.setTextColor(0xFF343A40);
                textView.setBackgroundResource(R.drawable.cell_open);
            }else{
                textView.setBackgroundResource(R.drawable.cell_closed);
            }
            textView.setGravity(Gravity.CENTER_VERTICAL);
            textView.setHeight(50);
            textView.setText(cellNames[i]);
            textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

            gridLayout.addView(textView);

            GridLayout.LayoutParams param = new GridLayout.LayoutParams();
            param.height = GridLayout.LayoutParams.WRAP_CONTENT;
            param.width = GridLayout.LayoutParams.MATCH_PARENT;
            param.setGravity(Gravity.CENTER);
            param.bottomMargin = 20;
            textView.setLayoutParams(param);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent i = new Intent(this, SettingsActivity.class);
                this.startActivity(i);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        leftGridLayout.removeAllViews();
        rightGridLayout.removeAllViews();
        senseToSee.setText(R.string.textview_sense_to_see);
        cellLabel.setVisibility(View.INVISIBLE);
        activityLabel.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        receiverWifi = new WifiReceiver(wifiManager);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(receiverWifi, intentFilter);
        getWifi();
    }

    private void getWifi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "Location turned off", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_ACCESS_COARSE_LOCATION);
            } else {
                wifiManager.startScan();
            }
        } else {
            Toast.makeText(MainActivity.this, "Scanning networks", Toast.LENGTH_SHORT).show();
            wifiManager.startScan();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiverWifi);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_ACCESS_COARSE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "permission granted", Toast.LENGTH_SHORT).show();
                    wifiManager.startScan();
                } else {

                    Toast.makeText(MainActivity.this, "permission not granted", Toast.LENGTH_SHORT).show();
                    return;
                }
                break;
        }
    }
}
