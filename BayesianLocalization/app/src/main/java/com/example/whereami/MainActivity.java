package com.example.whereami;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private WifiManager wifiManager;
    WifiReceiver receiverWifi;
    SQLiteDatabase db;
    GridLayout leftGridLayout;
    HashMap<String,Network> networks;
    BigDecimal[] prior, posterior;
    boolean sensingFinished;
    TextView senseLabel;
    Button sense;

    String[] permissions = new String[]{
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_whereami_icon_white);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        checkPermissions();

        db = openOrCreateDatabase("database.db", MODE_PRIVATE, null);
        Util.createDatabases(db);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(getApplicationContext(), "Turning WiFi ON...", Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }

        networks = Util.readData(this);
        senseLabel = (TextView) findViewById(R.id.textview_label_sense);
        leftGridLayout = (GridLayout) findViewById(R.id.gridlayout_left_cells);
        String[] cells = this.getResources().getStringArray(R.array.cell_array);

        resetSensing();

        Button training = (Button) findViewById(R.id.button_training);
        training.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(view.getContext(),TrainingActivity.class));
            }
        });

        sense = (Button) findViewById(R.id.button_sense);
        sense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(sensingFinished) {
                    leftGridLayout.removeAllViews();
                    resetSensing();
                    sense.setText(getResources().getString(R.string.button_sense_title));
                }else {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                    } else {

                        Toast.makeText(MainActivity.this, "Sensing...", Toast.LENGTH_SHORT).show();

                        if(sensingFinished) {
                            resetSensing();
                        }

                        posterior = Util.BayesianLocalization(prior, wifiManager, networks);

                        // When user chooses to sense networks the posterior is used to see if a cell has a high localization probability
                        // If this is the case
                        if (posterior[Util.findMaxBigDecimal(posterior)].compareTo(new BigDecimal(0.95)) >= 0) {
                            Arrays.fill(prior, new BigDecimal(0.125));
                            senseLabel.setText(getResources().getString(R.string.textview_sense_results));
                            sense.setText(getResources().getString(R.string.button_sense_reset));
                            sensingFinished = true;
                        } else {
                            senseLabel.setText(getResources().getString(R.string.textview_sense_again));
                            prior = posterior;
                        }

                        createCellGrid(leftGridLayout, cells, posterior);

                    }
                }
            }
        });
    }

    public void createCellGrid(GridLayout gridLayout, String[] cellNames, BigDecimal[] results) {
        gridLayout.removeAllViews();

        float [] results_formatted = new float[8];

        for(int i = 0; i < results.length; i++) {
            results_formatted[i] = results[i].setScale(4,BigDecimal.ROUND_HALF_UP).floatValue();
        }

        Log.i("RESULTS:",Arrays.toString(results_formatted));

        int idx = Util.findMaxValue(results_formatted);

        for(int i = 0; i < 8; i++) {
            TextView textView = new TextView(this);
            textView.setTextColor(0xFFF8F9FA);

            if(i == idx) {
                textView.setTextColor(0xFF343A40);
                textView.setBackgroundResource(R.drawable.cell_open);
            }else{
                textView.setBackgroundResource(R.drawable.cell_closed);
            }
            textView.setGravity(Gravity.CENTER_VERTICAL);
            textView.setHeight(60);
            textView.setText(cellNames[i] + " - " + String.format("%.4f", results_formatted[i]));
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
        resetSensing();
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

     void resetSensing() {
        prior = new BigDecimal[8];
        Arrays.fill(prior, new BigDecimal(0.125));
        sensingFinished = false;
        senseLabel.setText(getResources().getString(R.string.textview_sense_to_see));
     }

    private void getWifi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "Location turned off", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
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

    private boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            result = ContextCompat.checkSelfPermission(this, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 100);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "Permissions granted", Toast.LENGTH_SHORT).show();
            }
            return;
        }
    }
}
